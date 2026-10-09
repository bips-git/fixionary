#!/usr/bin/env python3
"""Phase 2: fetch Inter and Manrope (SIL Open Font License) from google/fonts, cut the static weights the app
uses, subset them to Latin + IPA + punctuation, and write them to app/src/main/res/font/.

    pip install fonttools
    python pipeline/get_fonts.py

Raw downloads go to dict-data/fonts/ (git-ignored). The subset fonts and the licences (legal/) are committed.
Static cuts are used instead of variable fonts so Compose needs no variable-font API and works the same everywhere.
"""
import json
import re
import sys
import urllib.request
from pathlib import Path

try:
    from fontTools import subset
    from fontTools.ttLib import TTFont
    from fontTools.varLib import instancer
except ImportError:
    sys.exit("fontTools is missing in this environment: pip install fonttools")

ROOT = Path(__file__).resolve().parent.parent
RAW = ROOT / "dict-data" / "fonts"
OUT = ROOT / "app" / "src" / "main" / "res" / "font"
LEGAL = ROOT / "legal"
API = "https://api.github.com/repos/google/fonts/contents/ofl/{}"

# Basic Latin, Latin-1, Latin Extended A/B, IPA extensions and modifier letters, combining marks,
# general punctuation, euro, minus sign, trademark.
UNICODES = (list(range(0x20, 0x7F)) + list(range(0xA0, 0x370)) + list(range(0x2000, 0x2070))
            + [0x20AC, 0x2122, 0x2212])

# (google/fonts folder, source file pattern, wght, output resource name)
JOBS = [
    ("inter", r"^Inter\[.*\]\.ttf$", 400, "inter_regular"),
    ("inter", r"^Inter\[.*\]\.ttf$", 500, "inter_medium"),
    ("inter", r"^Inter\[.*\]\.ttf$", 600, "inter_semibold"),
    ("inter", r"^Inter\[.*\]\.ttf$", 700, "inter_bold"),
    ("inter", r"^Inter-Italic\[.*\]\.ttf$", 400, "inter_italic"),
    ("manrope", r"^Manrope\[.*\]\.ttf$", 600, "manrope_semibold"),
    ("manrope", r"^Manrope\[.*\]\.ttf$", 700, "manrope_bold"),
]


def http(url: str) -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": "fixionary-fonts"})
    with urllib.request.urlopen(req, timeout=60) as r:
        return r.read()


def listing(folder: str) -> list:
    return json.loads(http(API.format(folder)))


def fetch(folder: str, pattern: str) -> Path:
    items = listing(folder)
    names = [i["name"] for i in items]
    hit = next((i for i in items if re.match(pattern, i["name"])), None)
    if hit is None:
        sys.exit(f"no file matching {pattern} in ofl/{folder}; found: {names}")
    dest = RAW / hit["name"]
    if not dest.exists():
        print(f"downloading {hit['name']}")
        dest.write_bytes(http(hit["download_url"]))
    return dest


def cut(src: Path, weight: int, dest: Path) -> None:
    font = TTFont(src)
    limits = {"wght": weight}
    if "opsz" in {a.axisTag for a in font["fvar"].axes}:
        limits["opsz"] = 14           # text size optical cut
    font = instancer.instantiateVariableFont(font, limits)
    opts = subset.Options()
    opts.layout_features = ["kern", "liga", "calt", "ccmp", "locl", "mark", "mkmk", "case"]
    opts.hinting = False
    opts.notdef_outline = True
    sub = subset.Subsetter(opts)
    sub.populate(unicodes=UNICODES)
    sub.subset(font)
    font.save(dest)


def main() -> int:
    RAW.mkdir(parents=True, exist_ok=True)
    OUT.mkdir(parents=True, exist_ok=True)
    LEGAL.mkdir(exist_ok=True)

    for folder, label in (("inter", "Inter"), ("manrope", "Manrope")):
        lic = LEGAL / f"OFL-{label}.txt"
        if not lic.exists():
            item = next((i for i in listing(folder) if i["name"] == "OFL.txt"), None)
            if item:
                lic.write_bytes(http(item["download_url"]))
                print(f"saved {lic.relative_to(ROOT)}")

    total = 0
    for folder, pattern, weight, name in JOBS:
        dest = OUT / f"{name}.ttf"
        cut(fetch(folder, pattern), weight, dest)
        size = dest.stat().st_size
        total += size
        print(f"{name}.ttf  {size / 1024:.0f} KB")
    print(f"total {total / 1024:.0f} KB in {OUT.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
