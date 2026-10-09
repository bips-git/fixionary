#!/usr/bin/env python3
"""Build dictionary.db (schema v2, slim) from Open English WordNet (WN-LMF XML) plus wordfreq ranks.

    python build_db_v2.py --xml ~/fixionary/dict-data/raw/english-wordnet-2025-plus.xml.gz \
                          --out ~/fixionary/dict-data/build/dictionary.db

What changed from v1 (same data, about half the size):
- Definitions and examples live once in `synsets`; `senses` is just (word, order, synset).
- Synonyms are NOT stored: they are the other words that share a synset (the app joins at display time).
- Hyponyms are NOT stored: "narrower terms of X" are the words whose hypernym is X (reverse lookup
  through a partial index), which also lets the app sort them by commonness.
- `relations` keeps only antonym, hypernym and related; it is WITHOUT ROWID, so it needs no separate index.
- Ties in freq_rank prefer the lowercase headword ("book" before "Book").
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

# wordfreq scores a phrase by combining its words, so "dog do" looks far more common than "dogma".
# Multi-word and hyphenated headwords are therefore pushed down by this many zipf points (about 100x rarer per 2.0).
MULTIWORD_PENALTY = 2.0

SENSE_REL_MAP = {"antonym": "antonym", "derivation": "related", "pertainym": "related",
                 "participle": "related", "also": "related"}
SYNSET_REL_MAP = {"hypernym": "hypernym", "instance_hypernym": "hypernym",
                  "similar": "related", "also": "related", "attribute": "related"}

SCHEMA = """
CREATE TABLE words (
  id           INTEGER PRIMARY KEY,
  headword     TEXT NOT NULL,
  headword_lc  TEXT NOT NULL,
  freq_rank    INTEGER NOT NULL
);
CREATE TABLE synsets (
  id          INTEGER PRIMARY KEY,
  pos         TEXT NOT NULL,
  definition  TEXT NOT NULL,
  example     TEXT
);
CREATE TABLE senses (
  word_id     INTEGER NOT NULL,
  sense_order INTEGER NOT NULL,
  synset_id   INTEGER NOT NULL,
  PRIMARY KEY (word_id, sense_order)
) WITHOUT ROWID;
CREATE TABLE relations (
  word_id   INTEGER NOT NULL,
  rel_type  TEXT NOT NULL CHECK (rel_type IN ('antonym','hypernym','related')),
  ord       INTEGER NOT NULL,
  related   TEXT NOT NULL,
  PRIMARY KEY (word_id, rel_type, ord)
) WITHOUT ROWID;
CREATE TABLE pronunciations (word_id INTEGER NOT NULL, ipa TEXT NOT NULL, region TEXT);
CREATE TABLE etymology      (word_id INTEGER PRIMARY KEY, text TEXT NOT NULL);
CREATE TABLE meta           (key TEXT PRIMARY KEY, value TEXT NOT NULL);
"""

INDEXES = """
CREATE INDEX idx_words_lc ON words(headword_lc, freq_rank);
CREATE INDEX idx_senses_synset ON senses(synset_id);
CREATE INDEX idx_hypernym_target ON relations(related) WHERE rel_type = 'hypernym';
"""


def fold(text: str) -> str:
    """Lowercase, accent-fold, collapse whitespace. The app's Fold.kt must match this exactly."""
    text = text.replace("_", " ")
    text = unicodedata.normalize("NFKD", text)
    text = "".join(c for c in text if not unicodedata.combining(c))
    return " ".join(text.lower().split())


def log(msg: str) -> None:
    print(f"[{time.strftime('%H:%M:%S')}] {msg}", flush=True)


def parse_lmf(path: Path):
    entries, synsets, lexicon = {}, {}, {}
    opener = gzip.open if str(path).endswith(".gz") else open
    with opener(path, "rb") as f:
        for _, el in ET.iterparse(f, events=("end",)):
            tag = el.tag
            if tag == "LexicalEntry":
                lemma = el.find("Lemma")
                if lemma is not None and lemma.get("writtenForm"):
                    prons = [((p.text or "").strip(), p.get("variety"))
                             for p in lemma.findall("Pronunciation") if (p.text or "").strip()]
                    senses = [(s.get("id"), s.get("synset"),
                               [(r.get("relType"), r.get("target")) for r in s.findall("SenseRelation")])
                              for s in el.findall("Sense")]
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
    sense_owner, derived = {}, defaultdict(list)
    for eid, (_, _, _, senses) in entries.items():
        for sid, syn, _ in senses:
            sense_owner[sid] = eid
            derived[syn].append(eid)

    def members(syn_id):
        syn = synsets.get(syn_id)
        return syn["members"] if syn and syn["members"] else derived.get(syn_id, [])

    def lemma_of(eid):
        e = entries.get(eid)
        return e[0].replace("_", " ") if e else None

    by_headword = defaultdict(list)
    for eid, (lemma, _, _, _) in entries.items():
        by_headword[lemma.replace("_", " ").strip()].append(eid)

    words = {}
    for headword, eids in by_headword.items():
        prons, pending, order = [], [], 0
        for eid in eids:
            _, pos_code, entry_prons, entry_senses = entries[eid]
            prons.extend(entry_prons)
            for sid, syn_id, srels in entry_senses:
                syn = synsets.get(syn_id)
                if not syn or not syn["definition"]:
                    continue
                pos = POS_NAMES.get(syn["pos"] or pos_code, "other")
                pending.append((POS_ORDER.get(pos, 9), order, pos, syn_id, srels))
                order += 1
        if not pending:
            continue
        pending.sort(key=lambda t: (t[0], t[1]))

        seen, rels, senses, instance_flags = set(), [], [], []

        def add(related, rel_type):
            if not related or fold(related) == fold(headword):
                return
            if (related, rel_type) not in seen:
                seen.add((related, rel_type))
                rels.append((rel_type, related))

        for _, _, pos, syn_id, srels in pending:
            syn = synsets[syn_id]
            instance_flags.append(any(t == "instance_hypernym" for t, _ in syn["rels"]))
            senses.append((syn_id, pos))
            for rtype, target in srels:
                mapped = SENSE_REL_MAP.get(rtype)
                if mapped and target in sense_owner:
                    add(lemma_of(sense_owner[target]), mapped)
            for rtype, target in syn["rels"]:
                mapped = SYNSET_REL_MAP.get(rtype)
                tm = members(target) if mapped else None
                if tm:
                    add(lemma_of(tm[0]), mapped)

        words[headword] = {"senses": senses, "rels": rels, "prons": prons, "is_proper": all(instance_flags)}
    return words


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--xml", required=True, type=Path)
    ap.add_argument("--out", required=True, type=Path)
    args = ap.parse_args()

    try:
        from wordfreq import zipf_frequency
    except ImportError:
        sys.exit("wordfreq is not installed in this environment: pip install wordfreq")

    log(f"parsing {args.xml}")
    lexicon, entries, synsets = parse_lmf(args.xml)
    log(f"parsed {len(entries):,} lexical entries, {len(synsets):,} synsets")
    words = build_words(entries, synsets)
    log(f"merged into {len(words):,} headwords")

    cache = {}

    def zipf(headword):
        lc = fold(headword)
        if lc not in cache:
            z = zipf_frequency(lc, "en")
            cache[lc] = z - MULTIWORD_PENALTY if (" " in lc or "-" in lc) else z
        return cache[lc]

    # Common first; proper-noun-only entries last; ties prefer the lowercase form.
    ranked = sorted(words, key=lambda h: (words[h]["is_proper"], -zipf(h), fold(h), h != h.lower(), h))

    args.out.parent.mkdir(parents=True, exist_ok=True)
    tmp = args.out.with_suffix(".tmp")
    if tmp.exists():
        tmp.unlink()
    db = sqlite3.connect(tmp)
    db.execute("PRAGMA journal_mode=OFF")
    db.execute("PRAGMA synchronous=OFF")
    db.executescript(SCHEMA)

    synset_db, synset_rows = {}, []
    word_rows, sense_rows, pron_rows = [], [], []
    rel_lists = {}
    word_id_by_headword = {}
    for rank, headword in enumerate(ranked, start=1):
        w = words[headword]
        word_id_by_headword[headword] = rank
        word_rows.append((rank, headword, fold(headword), rank))
        for order, (syn_id, pos) in enumerate(w["senses"]):
            if syn_id not in synset_db:
                synset_db[syn_id] = len(synset_db) + 1
                s = synsets[syn_id]
                synset_rows.append((synset_db[syn_id], pos, s["definition"], s["example"]))
            sense_rows.append((rank, order, synset_db[syn_id]))
        rel_lists[rank] = list(w["rels"])
        for ipa, region in dict.fromkeys(w["prons"]):
            pron_rows.append((rank, ipa, region))

    # Antonyms must be symmetric: add the reverse where the other word exists.
    have = {(wid, rel) for wid, lst in rel_lists.items() for t, rel in lst if t == "antonym"}
    added = 0
    for wid, lst in list(rel_lists.items()):
        for t, rel in list(lst):
            other = word_id_by_headword.get(rel) if t == "antonym" else None
            if other is not None and (other, ranked[wid - 1]) not in have:
                have.add((other, ranked[wid - 1]))
                rel_lists[other].append(("antonym", ranked[wid - 1]))
                added += 1
    log(f"added {added:,} reverse antonym rows")

    rel_rows = []
    for wid, lst in rel_lists.items():
        counters = defaultdict(int)
        for t, rel in lst:
            rel_rows.append((wid, t, counters[t], rel))
            counters[t] += 1

    db.executemany("INSERT INTO words VALUES (?,?,?,?)", word_rows)
    db.executemany("INSERT INTO synsets VALUES (?,?,?,?)", synset_rows)
    db.executemany("INSERT INTO senses VALUES (?,?,?)", sense_rows)
    db.executemany("INSERT INTO relations VALUES (?,?,?,?)", rel_rows)
    db.executemany("INSERT INTO pronunciations VALUES (?,?,?)", pron_rows)

    today = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%d")
    meta = {
        "schema_version": "2",
        "build_date": today,
        "dictionary_version": f"oewn-{lexicon.get('version', 'unknown')}-{today}",
        "source.oewn": json.dumps({"name": "Open English WordNet", "version": lexicon.get("version"),
                                   "licence": "CC BY 4.0", "url": "https://en-word.net/",
                                   "used_for": "headwords, definitions, examples, synonyms, antonyms, broader/narrower terms"}),
        "source.wordfreq": json.dumps({"name": "wordfreq", "licence": "code MIT; data CC BY-SA 4.0",
                                       "url": "https://github.com/rspeer/wordfreq", "used_for": "frequency ranking only"}),
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
    log(f"done: {len(word_rows):,} words, {len(synset_rows):,} synsets, {len(sense_rows):,} senses, "
        f"{len(rel_rows):,} relations, {len(pron_rows):,} pronunciations, {args.out.stat().st_size / 1e6:.1f} MB -> {args.out}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
