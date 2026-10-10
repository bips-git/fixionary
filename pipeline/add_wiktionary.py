#!/usr/bin/env python3
"""Phase 5b: merge Wiktionary (via the slim file from wikt_audit.py) into a copy of dictionary.db.

    python3 pipeline/add_wiktionary.py --db dict-data/build/dictionary.db \
        --wikt dict-data/build/wikt_en.jsonl.gz --out dict-data/build/dictionary-v3.db

The input database is never modified. WordNet stays the main source; Wiktionary only fills gaps:
  * common words WordNet lacks (the, of, and, ...)  -> new headwords, ranked by word frequency
  * IPA (US / UK) for words that have none          -> pronunciations
  * origins (etymology)                             -> etymology (cleaned, capped length)
  * inflected forms (ran, mice, better ...)         -> new table forms(form, lemma_id, note)
New synsets carry src = 1 so the app can credit Wiktionary. Prints a size report; check it before copying
the result over dict-data/build/dictionary.db and rebuilding.
"""
import argparse, datetime, gzip, json, os, re, sqlite3, sys
from collections import defaultdict

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from build_db_v2 import SCHEMA, INDEXES, MULTIWORD_PENALTY, fold, log  # noqa: E402

PLAIN = re.compile(r"^[a-z]+$")
FORM_OK = re.compile(r"^[a-z][a-z']*$")
POS_MAP = {"noun": "noun", "verb": "verb", "adj": "adjective", "adv": "adverb", "prep": "preposition",
           "conj": "conjunction", "pron": "pronoun", "det": "determiner", "intj": "interjection",
           "num": "numeral", "article": "article", "particle": "particle", "contraction": "contraction"}
POS_ORDER = ["noun", "verb", "adjective", "adverb"]
SKIP_TAGS = {"obsolete", "archaic", "rare", "nonstandard", "misspelling", "eye-dialect", "offensive", "vulgar"}
SKIP_GLOSS = re.compile(r"^(alternative|obsolete|misspelling|nonstandard|abbreviation of|initialism of|acronym of|eye dialect)", re.I)
BAD_FORM_TAGS = {"table-tags", "inflection-template", "class", "romanization", "multiword-construction",
                 "canonical", "no-table-tags", "error-unrecognized-form"}
US_TAGS = {"us", "general-american", "general american", "ga"}
UK_TAGS = {"uk", "received-pronunciation", "received pronunciation", "rp"}
MAX_SENSES = 6
MAX_FORMS_PER_LEMMA = 12


def norm_ipa(s):
    s = s.strip().strip("/[]").strip()
    return f"/{s}/" if s else ""


def pick_ipa(items):
    us = uk = first = None
    for it in items:
        v = norm_ipa(it["ipa"])
        if not v:
            continue
        tags = {t.lower() for t in it.get("t", [])}
        first = first or v
        if tags & US_TAGS and not us: us = v
        if tags & UK_TAGS and not uk: uk = v
    out = []
    if us: out.append((us, "US"))
    if uk and uk != us: out.append((uk, "UK"))
    if not out and first: out.append((first, None))
    return out


def clean_etym(text, cap):
    text = " ".join(text.split())
    if len(text) <= cap:
        return text
    cut = text[:cap]
    k = cut.rfind(". ")
    return cut[:k + 1] if k > cap // 2 else cut.rstrip() + "…"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--db", required=True)
    ap.add_argument("--wikt", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--min-zipf", type=float, default=3.0, help="add Wiktionary-only words at least this common")
    ap.add_argument("--etym-max-rank", type=int, default=120000)
    ap.add_argument("--etym-cap", type=int, default=600, help="max characters of origin text per word")
    ap.add_argument("--forms-max-rank", type=int, default=100000)
    a = ap.parse_args()
    try:
        from wordfreq import zipf_frequency
    except ImportError:
        sys.exit("pip install wordfreq")

    zc = {}
    def zipf(h):
        lc = fold(h)
        if lc not in zc:
            z = zipf_frequency(lc, "en")
            zc[lc] = z - MULTIWORD_PENALTY if (" " in lc or "-" in lc) else z
        return zc[lc]

    src = sqlite3.connect(f"file:{a.db}?mode=ro", uri=True)
    old = src.execute("SELECT id, headword, headword_lc FROM words ORDER BY id").fetchall()
    exact = {h: i for i, h, _ in old}
    lcset = {lc for _, _, lc in old}
    have_pron = {r[0] for r in src.execute("SELECT DISTINCT word_id FROM pronunciations")}
    have_ety = {r[0] for r in src.execute("SELECT word_id FROM etymology")}
    log(f"source database: {len(old):,} headwords")

    # ---- read the slim Wiktionary file -------------------------------------------------------
    dbent = defaultdict(list)          # exact DB headword -> [(ipa items, etymology, forms)]
    cand = {}                          # new word -> {"s": [(pos, gloss)], "ipa": [...], "e": str, "f": [...]}
    formof = []                        # (form, lemma lc, gloss)
    n = 0
    with gzip.open(a.wikt, "rt", encoding="utf-8") as f:
        for line in f:
            n += 1
            e = json.loads(line)
            w, senses = e["w"], e.get("s", [])
            only_form = bool(senses) and all(("fo" in s or "ao" in s) for s in senses)
            if only_form:
                lw = w.lower()
                if w == lw and FORM_OK.match(lw):
                    fo = next((s["fo"][0] for s in senses if "fo" in s), None)
                    if fo:
                        formof.append((lw, fo.lower(), senses[0]["g"]))
                continue
            if w in exact:
                dbent[w].append((e.get("ipa", []), e.get("e"), e.get("f", [])))
            elif PLAIN.match(w) and w not in lcset and e.get("p") != "name" and zipf(w) >= a.min_zipf:
                pos = POS_MAP.get(e.get("p"))
                good = [(pos, s["g"]) for s in senses
                        if pos and not (set(s.get("t", [])) & SKIP_TAGS) and not SKIP_GLOSS.match(s["g"])]
                if not good:
                    continue
                c = cand.setdefault(w, {"s": [], "ipa": [], "e": None, "f": []})
                c["s"] += good
                c["ipa"] += e.get("ipa", [])
                c["e"] = c["e"] or e.get("e")
                c["f"] += e.get("f", [])
    log(f"read {n:,} slim entries; {len(cand):,} new common words; {len(formof):,} form-of pointers")

    # ---- new ranking: keep WordNet order, slot new words in by frequency ----------------------
    new_sorted = sorted(cand, key=lambda w: (-zipf(w), w))
    order, j = [], 0
    for oid, h, _ in old:
        z = zipf(h)
        while j < len(new_sorted) and zipf(new_sorted[j]) > z:
            order.append(("new", new_sorted[j])); j += 1
        order.append(("old", oid))
    order += [("new", w) for w in new_sorted[j:]]
    idmap, new_id = {}, {}
    for rank, (kind, key) in enumerate(order, start=1):
        (idmap if kind == "old" else new_id)[key] = rank

    # ---- build the output database -----------------------------------------------------------
    if os.path.exists(a.out):
        os.remove(a.out)
    db = sqlite3.connect(a.out)
    db.execute("PRAGMA journal_mode=OFF"); db.execute("PRAGMA synchronous=OFF")
    db.executescript(SCHEMA)
    db.execute("ALTER TABLE synsets ADD COLUMN src INTEGER NOT NULL DEFAULT 0")
    db.execute("CREATE TABLE forms (form TEXT NOT NULL, lemma_id INTEGER NOT NULL, note TEXT, "
               "PRIMARY KEY (form, lemma_id)) WITHOUT ROWID")
    db.execute("ATTACH DATABASE ? AS src", (a.db,))
    db.execute("CREATE TEMP TABLE idmap (old INTEGER PRIMARY KEY, new INTEGER)")
    db.executemany("INSERT INTO idmap VALUES (?,?)", idmap.items())
    db.execute("INSERT INTO words SELECT m.new, w.headword, w.headword_lc, m.new FROM src.words w JOIN idmap m ON m.old = w.id")
    db.execute("INSERT INTO synsets (id, pos, definition, example) SELECT id, pos, definition, example FROM src.synsets")
    db.execute("INSERT INTO senses SELECT m.new, s.sense_order, s.synset_id FROM src.senses s JOIN idmap m ON m.old = s.word_id")
    db.execute("INSERT INTO relations SELECT m.new, r.rel_type, r.ord, r.related FROM src.relations r JOIN idmap m ON m.old = r.word_id")
    db.execute("INSERT INTO pronunciations SELECT m.new, p.ipa, p.region FROM src.pronunciations p JOIN idmap m ON m.old = p.word_id")
    db.execute("INSERT INTO etymology SELECT m.new, e.text FROM src.etymology e JOIN idmap m ON m.old = e.word_id")
    db.execute("INSERT INTO meta SELECT key, value FROM src.meta")
    syn_next = (db.execute("SELECT MAX(id) FROM synsets").fetchone()[0] or 0) + 1

    # new words
    word_rows, syn_rows, sense_rows = [], [], []
    for w in new_sorted:
        wid = new_id[w]
        word_rows.append((wid, w, w, wid))
        by_pos = defaultdict(list)
        for pos, g in cand[w]["s"]:
            by_pos[pos].append(g)
        pos_seq = [p for p in POS_ORDER if p in by_pos] + [p for p in by_pos if p not in POS_ORDER]
        order_i = 0
        for pos in pos_seq:
            for g in by_pos[pos]:
                if order_i >= MAX_SENSES: break
                syn_rows.append((syn_next, pos, g, None, 1))
                sense_rows.append((wid, order_i, syn_next))
                syn_next += 1; order_i += 1
    db.executemany("INSERT INTO words VALUES (?,?,?,?)", word_rows)
    db.executemany("INSERT INTO synsets (id, pos, definition, example, src) VALUES (?,?,?,?,?)", syn_rows)
    db.executemany("INSERT INTO senses VALUES (?,?,?)", sense_rows)

    # IPA + origins + raw forms for old and new words
    pron_rows, ety_rows = [], []
    raw_forms = defaultdict(list)      # lemma id -> [(form, note)]
    def take(wid, ipa_items, etym, forms, had_pron, had_ety):
        if not had_pron:
            for ipa, region in pick_ipa(ipa_items):
                pron_rows.append((wid, ipa, region))
        if not had_ety and etym and wid <= a.etym_max_rank:
            ety_rows.append((wid, clean_etym(etym, a.etym_cap)))
        for form, tags in forms:
            if set(tags) & BAD_FORM_TAGS:
                continue
            if FORM_OK.match(form):
                raw_forms[wid].append((form, " ".join(tags)))
    for h, ents in dbent.items():
        wid = idmap[exact[h]]; oid = exact[h]
        ipa_items = [i for e in ents for i in e[0]]
        etym = next((e[1] for e in ents if e[1]), None)
        take(wid, ipa_items, etym, [x for e in ents for x in e[2]], oid in have_pron, oid in have_ety)
    for w in new_sorted:
        c = cand[w]
        take(new_id[w], c["ipa"], c["e"], c["f"], False, False)
    db.executemany("INSERT INTO pronunciations VALUES (?,?,?)", pron_rows)
    db.executemany("INSERT INTO etymology VALUES (?,?)", ety_rows)

    # forms (kept even when the form is itself a headword, e.g. 'ran': the app shows a short "past of run" note)
    # lemma lookups by lowercase, then pointers from form-of entries
    by_lc = {}
    for wid, hw, lc, _ in db.execute("SELECT id, headword, headword_lc, 0 FROM words ORDER BY id DESC"):
        by_lc[lc] = wid               # DESC order -> lowest id wins
    forms = {}
    for lemma_id, items in raw_forms.items():
        if lemma_id > a.forms_max_rank: continue
        lemma_lc = db.execute("SELECT headword_lc FROM words WHERE id=?", (lemma_id,)).fetchone()[0]
        for form, note in items[:MAX_FORMS_PER_LEMMA * 2]:
            if form != lemma_lc:
                forms.setdefault((form, lemma_id), note)
    for form, lemma, gloss in formof:
        lid = by_lc.get(lemma)
        if lid and lid <= a.forms_max_rank and form != lemma:
            forms[(form, lid)] = gloss[:70]
    # at most MAX_FORMS_PER_LEMMA per lemma
    per, rows = defaultdict(int), []
    for (form, lid), note in sorted(forms.items(), key=lambda kv: (kv[0][1], kv[0][0])):
        if per[lid] < MAX_FORMS_PER_LEMMA:
            per[lid] += 1; rows.append((form, lid, note or None))
    db.executemany("INSERT INTO forms VALUES (?,?,?)", rows)

    today = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%d")
    meta = {"schema_version": "3",
            "source.wiktionary": json.dumps({"name": "Wiktionary (English), via kaikki.org / wiktextract",
                                             "licence": "CC BY-SA 4.0", "url": "https://en.wiktionary.org/",
                                             "used_for": "common words missing from WordNet, IPA, origins, word forms"}),
            "wiktionary_merged": today}
    db.executemany("INSERT OR REPLACE INTO meta VALUES (?,?)", meta.items())
    db.execute("UPDATE meta SET value = value || '+wikt-' || ? WHERE key = 'dictionary_version'", (today,))
    db.commit()
    db.execute("DETACH DATABASE src")

    db.executescript(INDEXES)
    db.execute('CREATE VIRTUAL TABLE synset_fts USING fts4(content="synsets", definition, tokenize=unicode61)')
    db.execute("INSERT INTO synset_fts(synset_fts) VALUES('rebuild')")
    db.execute("INSERT INTO synset_fts(synset_fts) VALUES('optimize')")
    db.execute("INSERT OR REPLACE INTO meta VALUES ('meaning_index', 'fts4 over synsets.definition')")
    db.commit()
    db.execute("ANALYZE"); db.execute("VACUUM"); db.close()

    mb = lambda p: f"{os.path.getsize(p)/1e6:.1f} MB"
    log(f"done: {a.out} = {mb(a.out)}  (input {mb(a.db)})")
    print(f"  new headwords      {len(word_rows):,}   (new definitions {len(syn_rows):,})")
    print(f"  IPA rows added     {len(pron_rows):,}")
    print(f"  origins added      {len(ety_rows):,}   ({sum(len(r[1].encode()) for r in ety_rows)/1e6:.1f} MB text)")
    print(f"  forms              {len(rows):,}   (e.g. ran, mice)")
    chk = sqlite3.connect(a.out)
    for q in ("the", "ran", "mice", "better"):
        r = chk.execute("SELECT w.headword, f.note FROM forms f JOIN words w ON w.id=f.lemma_id WHERE f.form=? LIMIT 2", (q,)).fetchall()
        r2 = chk.execute("SELECT id FROM words WHERE headword_lc=?", (q,)).fetchone()
        print(f"  check '{q}': headword id={r2[0] if r2 else None}; form of {r}")


if __name__ == "__main__":
    main()
