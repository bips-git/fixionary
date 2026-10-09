#!/usr/bin/env python3
"""Quality gates for dictionary.db. Exit code 1 if any gate fails.  Usage: 03_quality_gates.py dictionary.db"""
import random
import sqlite3
import statistics
import sys
import time

db_path = sys.argv[1]
db = sqlite3.connect(f"file:{db_path}?mode=ro", uri=True)
q = lambda sql, *a: db.execute(sql, a).fetchall()
fails = []


def gate(name, ok, detail=""):
    print(f"{'PASS' if ok else 'FAIL'}  {name}  {detail}")
    if not ok:
        fails.append(name)


words, senses, rels = (q(f"SELECT COUNT(*) FROM {t}")[0][0] for t in ("words", "senses", "relations"))
print(f"words={words:,} senses={senses:,} relations={rels:,}")
gate("integrity_check", q("PRAGMA integrity_check")[0][0] == "ok")
gate("size floor (>=100k words, >=150k senses)", words >= 100_000 and senses >= 150_000)
gate("no empty definitions", q("SELECT COUNT(*) FROM senses WHERE TRIM(definition)=''")[0][0] == 0)
gate("no word without senses", q("SELECT COUNT(*) FROM words w WHERE NOT EXISTS (SELECT 1 FROM senses s WHERE s.word_id=w.id)")[0][0] == 0)
gate("no orphan senses/relations", q("SELECT (SELECT COUNT(*) FROM senses WHERE word_id NOT IN (SELECT id FROM words)) + (SELECT COUNT(*) FROM relations WHERE word_id NOT IN (SELECT id FROM words))")[0][0] == 0)
gate("freq_rank unique and contiguous", q("SELECT COUNT(DISTINCT freq_rank), MIN(freq_rank), MAX(freq_rank) FROM words")[0] == (words, 1, words))
gate("antonyms symmetric", q("""SELECT COUNT(*) FROM relations r JOIN words t ON t.headword=r.related
    WHERE r.rel_type='antonym' AND NOT EXISTS (SELECT 1 FROM relations b JOIN words o ON o.id=b.word_id
    WHERE b.rel_type='antonym' AND b.word_id=t.id AND b.related=(SELECT headword FROM words WHERE id=r.word_id))""")[0][0] == 0)

common = "run time house water good make people world school life happy quick big small love book light music city family".split()
bad = [w for w in common if (q("SELECT headword FROM words WHERE headword_lc=? ORDER BY freq_rank LIMIT 1", w) or [[None]])[0][0] != w]
gate("common words found by exact search", not bad, str(bad))

prefixes = [("hous", "house"), ("wat", "water"), ("beaut", "beautiful"), ("comp", "computer"), ("educ", "education"),
            ("inter", "interest"), ("happ", "happy"), ("fami", "family"), ("scho", "school"), ("mus", "music"),
            ("lang", "language"), ("trav", "travel")]
SEARCH = "SELECT headword FROM words WHERE headword_lc >= ? AND headword_lc < ? ORDER BY (headword_lc = ?) DESC, freq_rank LIMIT 15"
miss = [(p, w) for p, w in prefixes if w not in [r[0] for r in q(SEARCH, p, p + "\uffff", p)]]
gate("prefix search ranks common words in top 15", not miss, str(miss))

rnd = random.Random(7)
heads = [r[0] for r in q("SELECT headword_lc FROM words WHERE freq_rank <= 60000")]
prefs = [h[: rnd.randint(2, 4)] for h in rnd.choices(heads, k=1000)]
times = []
for p in prefs:
    t = time.perf_counter()
    q(SEARCH, p, p + "\uffff", p)
    times.append((time.perf_counter() - t) * 1000)
times.sort()
p50, p95 = statistics.median(times), times[int(len(times) * 0.95)]
gate("search p95 < 5 ms", p95 < 5, f"(median {p50:.2f} ms, p95 {p95:.2f} ms over 1000 queries)")

print("\n20 random entries to eyeball:")
for wid, hw in rnd.sample(q("SELECT id, headword FROM words WHERE freq_rank <= 40000"), 20):
    s = q("SELECT pos, definition FROM senses WHERE word_id=? ORDER BY sense_order LIMIT 1", wid)[0]
    syn = [r[0] for r in q("SELECT related FROM relations WHERE word_id=? AND rel_type='synonym' LIMIT 3", wid)]
    print(f"  {hw} ({s[0]}): {s[1]}  | syn: {', '.join(syn) or '-'}")

print("\nFAILED: " + ", ".join(fails) if fails else "\nALL GATES PASSED")
sys.exit(1 if fails else 0)
