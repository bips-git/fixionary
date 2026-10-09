#!/usr/bin/env python3
"""Build dictionary.db from Open English WordNet (WN-LMF XML) plus wordfreq ranks.

    python 02_build_db.py --xml ~/fixionary/dict-data/raw/english-wordnet-2025-plus.xml.gz \
                          --out ~/fixionary/dict-data/build/dictionary.db

Design notes
- One `words` row per distinct display headword (so "turkey" and "Turkey" are separate rows).
- WordNet is the only source in Phase 3. Wiktionary joins in Phase 5.
- freq_rank: 1 = most common. Entries that are only proper-noun instances rank after all common words.
- Relations store text, not ids, so words missing from the dictionary still display.
"""
import argparse
import datetime
import gzip
import json
import sqlite3
import sys
import time
import unicodedata
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path

POS_NAMES = {
    "n": "noun", "v": "verb", "a": "adjective", "s": "adjective", "r": "adverb",
    "c": "conjunction", "p": "adposition", "x": "other", "u": "unknown",
}
POS_ORDER = {"noun": 0, "verb": 1, "adjective": 2, "adverb": 3}

SENSE_REL_MAP = {
    "antonym": "antonym",
    "derivation": "related", "pertainym": "related", "participle": "related", "also": "related",
}
SYNSET_REL_MAP = {
    "hypernym": "hypernym", "instance_hypernym": "hypernym",
    "hyponym": "hyponym", "instance_hyponym": "hyponym",
    "similar": "related", "also": "related", "attribute": "related",
}
MAX_HYPONYMS_PER_SENSE = 20

SCHEMA = """
CREATE TABLE words (
  id           INTEGER PRIMARY KEY,
  headword     TEXT NOT NULL,
  headword_lc  TEXT NOT NULL,
  freq_rank    INTEGER NOT NULL
);
CREATE TABLE senses (
  id          INTEGER PRIMARY KEY,
  word_id     INTEGER NOT NULL REFERENCES words(id),
  pos         TEXT NOT NULL,
  sense_order INTEGER NOT NULL,
  definition  TEXT NOT NULL,
  example     TEXT,
  label       TEXT
);
CREATE TABLE relations (
  word_id    INTEGER NOT NULL REFERENCES words(id),
  related    TEXT NOT NULL,
  rel_type   TEXT NOT NULL CHECK (rel_type IN ('synonym','antonym','hypernym','hyponym','related')),
  sense_id   INTEGER REFERENCES senses(id)
);
CREATE TABLE pronunciations (word_id INTEGER NOT NULL REFERENCES words(id), ipa TEXT NOT NULL, region TEXT);
CREATE TABLE etymology      (word_id INTEGER PRIMARY KEY REFERENCES words(id), text TEXT NOT NULL);
CREATE TABLE meta           (key TEXT PRIMARY KEY, value TEXT NOT NULL);
"""

INDEXES = """
CREATE INDEX idx_words_lc ON words(headword_lc, freq_rank);
CREATE INDEX idx_senses_word ON senses(word_id, sense_order);
CREATE INDEX idx_rel_word ON relations(word_id, rel_type);
"""


def fold(text: str) -> str:
    """Lowercase, accent-fold, collapse whitespace. The app must normalise queries identically."""
    text = text.replace("_", " ")
    text = unicodedata.normalize("NFKD", text)
    text = "".join(c for c in text if not unicodedata.combining(c))
    return " ".join(text.lower().split())


def log(msg: str) -> None:
    print(f"[{time.strftime('%H:%M:%S')}] {msg}", flush=True)


def parse_lmf(path: Path):
    """Stream the WN-LMF XML. Returns (lexicon_attrs, entries, synsets)."""
    entries = {}   # entry id -> (lemma, pos, [(ipa, variety)], [(sense_id, synset_id, [(relType, target)])])
    synsets = {}   # synset id -> dict
    lexicon = {}
    opener = gzip.open if str(path).endswith(".gz") else open
    with opener(path, "rb") as f:
        for _, el in ET.iterparse(f, events=("end",)):
            tag = el.tag
            if tag == "LexicalEntry":
                lemma = el.find("Lemma")
                if lemma is not None and lemma.get("writtenForm"):
                    prons = [((p.text or "").strip(), p.get("variety"))
                             for p in lemma.findall("Pronunciation") if (p.text or "").strip()]
                    senses = [
                        (s.get("id"), s.get("synset"),
                         [(r.get("relType"), r.get("target")) for r in s.findall("SenseRelation")])
                        for s in el.findall("Sense")
                    ]
                    entries[el.get("id")] = (lemma.get("writtenForm"), lemma.get("partOfSpeech"), prons, senses)
                el.clear()
            elif tag == "Synset":
                defs = [(d.text or "").strip() for d in el.findall("Definition")]
                exs = [(x.text or "").strip() for x in el.findall("Example")]
                synsets[el.get("id")] = {
                    "pos": el.get("partOfSpeech"),
                    "members": (el.get("members") or "").split(),
                    "definition": next((d for d in defs if d), ""),
                    "example": next((x for x in exs if x), None),
                    "rels": [(r.get("relType"), r.get("target")) for r in el.findall("SynsetRelation")],
                }
                el.clear()
            elif tag == "Lexicon":
                lexicon = dict(el.attrib)
    return lexicon, entries, synsets


def build_words(entries, synsets):
    """Merge lexical entries into one record per display headword."""
    sense_owner = {}
    derived_members = defaultdict(list)
    for eid, (_, _, _, senses) in entries.items():
        for sid, syn, _ in senses:
            sense_owner[sid] = eid
            derived_members[syn].append(eid)

    def members(syn_id):
        syn = synsets.get(syn_id)
        if syn and syn["members"]:
            return syn["members"]
        return derived_members.get(syn_id, [])

    def lemma_of(eid):
        e = entries.get(eid)
        return e[0].replace("_", " ") if e else None

    by_headword = defaultdict(list)
    for eid, (lemma, _, _, _) in entries.items():
        by_headword[lemma.replace("_", " ").strip()].append(eid)

    words = {}
    for headword, eids in by_headword.items():
        senses, prons, rels = [], [], []
        pending = []   # (pos_rank, order, eid, sense_id, syn_id, rels)
        order = 0
        for eid in eids:
            _, pos_code, entry_prons, entry_senses = entries[eid]
            prons.extend(entry_prons)
            pos = POS_NAMES.get(pos_code, pos_code or "other")
            for sid, syn_id, srels in entry_senses:
                syn = synsets.get(syn_id)
                if not syn or not syn["definition"]:
                    continue
                pending.append((POS_ORDER.get(pos, 9), order, pos, eid, sid, syn_id, srels))
                order += 1
        if not pending:
            continue
        pending.sort(key=lambda t: (t[0], t[1]))

        instance_flags = []
        for sense_order, (_, _, pos, eid, sid, syn_id, srels) in enumerate(pending):
            syn = synsets[syn_id]
            instance_flags.append(any(t == "instance_hypernym" for t, _ in syn["rels"]))
            senses.append({
                "key": sid, "pos": pos, "order": sense_order,
                "definition": syn["definition"], "example": syn["example"],
            })
            seen = set()

            def add(related, rel_type):
                if not related or fold(related) == fold(headword):
                    return
                k = (related, rel_type)
                if k not in seen:
                    seen.add(k)
                    rels.append((related, rel_type, sid))

            for m in members(syn_id):
                if m != eid:
                    add(lemma_of(m), "synonym")
            for rtype, target in srels:
                mapped = SENSE_REL_MAP.get(rtype)
                if mapped and target in sense_owner:
                    add(lemma_of(sense_owner[target]), mapped)
            hypo = 0
            for rtype, target in syn["rels"]:
                mapped = SYNSET_REL_MAP.get(rtype)
                if not mapped:
                    continue
                if mapped == "hyponym":
                    if hypo >= MAX_HYPONYMS_PER_SENSE:
                        continue
                    hypo += 1
                tm = members(target)
                if tm:
                    add(lemma_of(tm[0]), mapped)

        words[headword] = {
            "senses": senses, "rels": rels, "prons": prons,
            "is_proper": all(instance_flags),
        }
    return words


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--xml", required=True, type=Path)
    ap.add_argument("--out", required=True, type=Path)
    args = ap.parse_args()

    try:
        from wordfreq import zipf_frequency
    except ImportError:
        sys.exit("wordfreq is not installed in this environment: pip install -r requirements.txt")

    log(f"parsing {args.xml}")
    lexicon, entries, synsets = parse_lmf(args.xml)
    log(f"parsed {len(entries):,} lexical entries, {len(synsets):,} synsets")

    words = build_words(entries, synsets)
    log(f"merged into {len(words):,} headwords")

    log("ranking with wordfreq")
    zipf_cache = {}

    def zipf(lc):
        if lc not in zipf_cache:
            zipf_cache[lc] = zipf_frequency(lc, "en")
        return zipf_cache[lc]

    ranked = sorted(
        words,
        key=lambda h: (words[h]["is_proper"], -zipf(fold(h)), fold(h), h != h.lower(), h),
    )

    args.out.parent.mkdir(parents=True, exist_ok=True)
    tmp = args.out.with_suffix(".tmp")
    if tmp.exists():
        tmp.unlink()
    db = sqlite3.connect(tmp)
    db.execute("PRAGMA journal_mode=OFF")
    db.execute("PRAGMA synchronous=OFF")
    db.executescript(SCHEMA)

    word_rows, sense_rows, pron_rows = [], [], []
    rel_rows = []      # (word_id, related, type, sense_db_id)
    word_id_by_headword = {}
    sense_counter = 0
    for rank, headword in enumerate(ranked, start=1):
        w = words[headword]
        word_id_by_headword[headword] = rank
        word_rows.append((rank, headword, fold(headword), rank))
        sense_db = {}
        for s in w["senses"]:
            sense_counter += 1
            sense_db[s["key"]] = sense_counter
            sense_rows.append((sense_counter, rank, s["pos"], s["order"], s["definition"], s["example"], None))
        for related, rtype, skey in w["rels"]:
            rel_rows.append((rank, related, rtype, sense_db.get(skey)))
        seen_p = set()
        for ipa, region in w["prons"]:
            if (ipa, region) not in seen_p:
                seen_p.add((ipa, region))
                pron_rows.append((rank, ipa, region))

    # Antonyms must be symmetric: add the reverse row where the other word exists.
    have = {(r[0], r[1], r[2]) for r in rel_rows if r[2] == "antonym"}
    reverse = []
    for wid, related, rtype, _ in list(rel_rows):
        if rtype != "antonym":
            continue
        other = word_id_by_headword.get(related)
        if other is None:
            continue
        back = ranked[wid - 1]
        if (other, back, "antonym") not in have:
            have.add((other, back, "antonym"))
            reverse.append((other, back, "antonym", None))
    rel_rows.extend(reverse)
    log(f"added {len(reverse):,} reverse antonym rows")

    db.executemany("INSERT INTO words VALUES (?,?,?,?)", word_rows)
    db.executemany("INSERT INTO senses VALUES (?,?,?,?,?,?,?)", sense_rows)
    db.executemany("INSERT INTO relations VALUES (?,?,?,?)", rel_rows)
    db.executemany("INSERT INTO pronunciations VALUES (?,?,?)", pron_rows)

    today = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%d")
    meta = {
        "schema_version": "1",
        "build_date": today,
        "dictionary_version": f"oewn-{lexicon.get('version', 'unknown')}-{today}",
        "source.oewn": json.dumps({
            "name": "Open English WordNet",
            "version": lexicon.get("version"),
            "licence": "CC BY 4.0",
            "url": "https://en-word.net/",
            "used_for": "headwords, definitions, examples, synonyms, antonyms, hypernyms, hyponyms",
        }),
        "source.wordfreq": json.dumps({
            "name": "wordfreq",
            "licence": "code MIT; data CC BY-SA 4.0",
            "url": "https://github.com/rspeer/wordfreq",
            "used_for": "frequency ranking only",
        }),
    }
    db.executemany("INSERT INTO meta VALUES (?,?)", meta.items())
    db.commit()

    log("creating indexes, VACUUM, ANALYZE")
    db.executescript(INDEXES)
    db.execute("PRAGMA journal_mode=DELETE")
    db.commit()
    db.execute("VACUUM")
    db.execute("ANALYZE")
    db.close()

    if args.out.exists():
        args.out.unlink()
    tmp.rename(args.out)
    size_mb = args.out.stat().st_size / 1e6
    log(f"done: {len(word_rows):,} words, {len(sense_rows):,} senses, "
        f"{len(rel_rows):,} relations, {len(pron_rows):,} pronunciations, {size_mb:.1f} MB -> {args.out}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
