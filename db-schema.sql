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
CREATE INDEX idx_words_lc ON words(headword_lc, freq_rank);
CREATE INDEX idx_senses_synset ON senses(synset_id);
CREATE INDEX idx_hypernym_target ON relations(related) WHERE rel_type = 'hypernym';
CREATE TABLE sqlite_stat1(tbl,idx,stat);
