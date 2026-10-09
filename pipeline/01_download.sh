#!/usr/bin/env bash
# Download raw source data into ~/dict-data/raw/. Safe to re-run.
set -euo pipefail

RAW="${DICT_RAW:-$HOME/fixionary/dict-data/raw}"
mkdir -p "$RAW"

# Open English WordNet 2025+ (core words plus curated proper nouns), WN-LMF XML, gzipped, ~12 MB.
# URL taken from https://en-word.net/downloads
URL="https://en-word.net/static/english-wordnet-2025-plus.xml.gz"
OUT="$RAW/english-wordnet-2025-plus.xml.gz"

if [ -s "$OUT" ] && gzip -t "$OUT" 2>/dev/null; then
  echo "already downloaded: $OUT"
else
  curl -fL --retry 3 --retry-delay 2 -o "$OUT" "$URL"
  gzip -t "$OUT"
fi
ls -lh "$OUT"
sha256sum "$OUT"
