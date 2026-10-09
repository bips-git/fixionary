#!/usr/bin/env python3
"""Quality gates for dictionary.db schema v2. The app's queries are mirrored here, so passing means they work.
Usage: gates_v2.py dictionary.db        Exit code 1 if any gate fails."""
import os
import random
import sqlite3
import statistics
import sys
import time

path = sys.argv[1]
db = sqlite3.connect(f"file:{path}?mode=ro", uri=True)
q = lambda sql, *a: db.execute(sql, a).fetchall()
fails = []


def gate(name, ok, detail=""):
    print(f"{'PASS' if ok else 'FAIL'}  {name}  {detail}")
    if not ok:
        fails.append(name)


# ---- the app's queries (keep identical to DictionaryRepository.kt) ----
HIT = """SELECT w.headword,
  (SELECT y.pos FROM senses s JOIN synsets y ON y.id = s.synset_id WHERE s.word_id = w.id ORDER BY s.sense_order LIMIT 1),
  (SELECT y.definition FROM senses s JOIN synsets y ON y.id = s.synset_id WHERE s.word_id = w.id ORDER BY s.sense_order LIMIT 1)
FROM words w"""
SEARCH = HIT + " WHERE w.headword_lc >= ? AND w.headword_lc < ? ORDER BY (w.headword_lc = ?) DESC, w.freq_rank LIMIT 30"
FIND = "SELECT id, headword FROM words WHERE headword_lc = ? ORDER BY (headword = ?) DESC, freq_rank LIMIT 1"
SENSES = "SELECT y.pos, y.definition, y.example FROM senses s JOIN synsets y ON y.id = s.synset_id WHERE s.word_id = ? ORDER BY s.sense_order"
SYNONYMS = """SELECT w2.headword FROM senses s1
  JOIN senses s2 ON s2.synset_id = s1.synset_id AND s2.word_id <> s1.word_id
  JOIN words w2 ON w2.id = s2.word_id
WHERE s1.word_id = ? ORDER BY s1.sense_order, w2.freq_rank"""
RELATIONS = "SELECT rel_type, related FROM relations WHERE word_id = ? ORDER BY rel_type, ord"
NARROWER = """SELECT w.headword FROM relations r JOIN words w ON w.id = r.word_id
WHERE r.rel_type = 'hypernym' AND r.related = ? ORDER BY w.freq_rank LIMIT 30"""
# -----------------------------------------------------------------------


def entry(headword):
    row = q(FIND, headword.lower(), headword)
    if not row:
        return None
    wid, hw = row[0]
    return hw, q(SENSES, wid), q(SYNONYMS, wid), q(RELATIONS, wid), q(NARROWER, hw)


words = q("SELECT COUNT(*) FROM words")[0][0]
senses = q("SELECT COUNT(*) FROM senses")[0][0]
synsets = q("SELECT COUNT(*) FROM synsets")[0][0]
rels = q("SELECT COUNT(*) FROM relations")[0][0]
size_mb = os.path.getsize(path) / 1e6
print(f"words={words:,} synsets={synsets:,} senses={senses:,} relations={rels:,} size={size_mb:.1f} MB")

gate("integrity_check", q("PRAGMA integrity_check")[0][0] == "ok")
gate("size floor (>=100k words, >=150k senses)", words >= 100_000 and senses >= 150_000)
gate("smaller than the 75.8 MB v1 (target < 55 MB)", size_mb < 55, f"({size_mb:.1f} MB)")
gate("no empty definitions", q("SELECT COUNT(*) FROM synsets WHERE TRIM(definition)=''")[0][0] == 0)
gate("no word without senses", q("SELECT COUNT(*) FROM words w WHERE NOT EXISTS (SELECT 1 FROM senses s WHERE s.word_id=w.id)")[0][0] == 0)
gate("no orphans", q("""SELECT (SELECT COUNT(*) FROM senses WHERE word_id NOT IN (SELECT id FROM words))
  + (SELECT COUNT(*) FROM senses WHERE synset_id NOT IN (SELECT id FROM synsets))
  + (SELECT COUNT(*) FROM relations WHERE word_id NOT IN (SELECT id FROM words))""")[0][0] == 0)
gate("every synset is used", q("SELECT COUNT(*) FROM synsets WHERE id NOT IN (SELECT synset_id FROM senses)")[0][0] == 0)
gate("freq_rank unique and contiguous", q("SELECT COUNT(DISTINCT freq_rank), MIN(freq_rank), MAX(freq_rank) FROM words")[0] == (words, 1, words))
gate("only antonym/hypernym/related stored", {r[0] for r in q("SELECT DISTINCT rel_type FROM relations")} <= {"antonym", "hypernym", "related"})
gate("antonyms symmetric", q("""SELECT COUNT(*) FROM relations r JOIN words t ON t.headword = r.related
  WHERE r.rel_type='antonym' AND NOT EXISTS (SELECT 1 FROM relations b WHERE b.word_id = t.id AND b.rel_type='antonym'
  AND b.related = (SELECT headword FROM words WHERE id = r.word_id))""")[0][0] == 0)

plan = " ".join(str(r) for r in q("EXPLAIN QUERY PLAN " + NARROWER, "dog"))
gate("narrower-terms query uses the partial index", "idx_hypernym_target" in plan)

common = "run time house water good make people world school life happy quick big small love book light music city family".split()
bad = [w for w in common if (q("SELECT headword FROM words WHERE headword_lc=? ORDER BY freq_rank LIMIT 1", w) or [[None]])[0][0] != w]
gate("common words found by exact search", not bad, str(bad))
prefixes = [("hous", "house"), ("wat", "water"), ("beaut", "beautiful"), ("comp", "computer"), ("educ", "education"),
            ("inter", "interest"), ("happ", "happy"), ("fami", "family"), ("scho", "school"), ("mus", "music"),
            ("lang", "language"), ("trav", "travel")]
miss = [(p, w) for p, w in prefixes if w not in [r[0] for r in q(SEARCH, p, p + "\uffff", p)]]
gate("prefix search ranks common words in top 30", not miss, str(miss))

gate("known entries have synonyms/narrower", all(
    entry(w) and entry(w)[2] for w in ("happy", "car", "begin")) and bool(entry("dog") and entry("dog")[4]),
    "(happy, car, begin have synonyms; dog has narrower terms)")

rnd = random.Random(7)
heads = [r[0] for r in q("SELECT headword_lc FROM words WHERE freq_rank <= 60000")]
times = []
for p in [h[: rnd.randint(2, 4)] for h in rnd.choices(heads, k=1000)]:
    t = time.perf_counter()
    q(SEARCH, p, p + "\uffff", p)
    times.append((time.perf_counter() - t) * 1000)
times.sort()
gate("search p95 < 5 ms", times[int(len(times) * 0.95)] < 5,
     f"(median {statistics.median(times):.2f} ms, p95 {times[int(len(times) * 0.95)]:.2f} ms over 1000 queries)")

times = []
for h in rnd.sample(heads, min(500, len(heads))):
    t = time.perf_counter()
    entry(h)
    times.append((time.perf_counter() - t) * 1000)
times.sort()
worst = {}
for h in ("person", "thing", "object", "man", "dog", "run"):
    t = time.perf_counter()
    entry(h)
    worst[h] = round((time.perf_counter() - t) * 1000, 1)
gate("word-screen load p95 < 15 ms, heaviest words < 60 ms",
     times[int(len(times) * 0.95)] < 15 and max(worst.values()) < 60,
     f"(p95 {times[int(len(times) * 0.95)]:.2f} ms; heaviest {worst})")

print("\n12 random entries to eyeball:")
pool = [h for h in heads if len(h) > 3][:20000] or heads
for h in rnd.sample(pool, min(12, len(pool))):
    e = entry(h)
    if e:
        hw, sn, syn, rl, nar = e
        print(f"  {hw} ({sn[0][0]}): {sn[0][1][:90]} | syn: {', '.join(r[0] for r in syn[:3]) or '-'} | narrower: {', '.join(r[0] for r in nar[:3]) or '-'}")

print("\nFAILED: " + ", ".join(fails) if fails else "\nALL GATES PASSED")
sys.exit(1 if fails else 0)
