#!/usr/bin/env python3
"""Adds a full-text index over definitions to an existing dictionary.db (Phase 6e: search by meaning).

    python3 pipeline/add_meaning_index.py path/to/dictionary.db

Safe to re-run. Uses SQLite FTS4 with external content (the index only, definitions are not
duplicated), the full-text flavour that every Android version ships. Copy the resulting file to
app/src/main/assets/dictionary.db and rebuild the app.
"""
import os
import sqlite3
import sys
import time


def main(path):
    if not os.path.exists(path):
        sys.exit(f"not found: {path}")
    before = os.path.getsize(path)
    db = sqlite3.connect(path)
    t0 = time.time()
    db.execute("DROP TABLE IF EXISTS synset_fts")
    db.execute('CREATE VIRTUAL TABLE synset_fts USING fts4(content="synsets", definition, tokenize=unicode61)')
    db.execute("INSERT INTO synset_fts(synset_fts) VALUES('rebuild')")
    db.execute("INSERT INTO synset_fts(synset_fts) VALUES('optimize')")
    db.execute("INSERT OR REPLACE INTO meta VALUES ('meaning_index', 'fts4 over synsets.definition')")
    db.commit()
    n = db.execute("SELECT COUNT(*) FROM synset_fts WHERE synset_fts MATCH 'the'").fetchone()[0]
    db.execute("VACUUM")
    db.close()
    after = os.path.getsize(path)
    print(f"indexed in {time.time() - t0:.1f}s; {n:,} definitions contain 'the'")
    print(f"size {before / 1e6:.1f} MB -> {after / 1e6:.1f} MB (+{(after - before) / 1e6:.1f} MB)")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    main(sys.argv[1])
