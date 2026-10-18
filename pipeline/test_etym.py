#!/usr/bin/env python3
"""Self-check for etym.py:  python3 pipeline/test_etym.py   (no dependencies)."""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from etym import structure_etym

def rows(s, tag):
    return [l.split("\t")[1:] for l in (s or "").split("\n") if l.startswith(tag + "\t")]

NIGHT = ("From Middle English nighte, night, nyght, niȝt, naht, from Old English niht, from Proto-West Germanic "
         "*naht (“night”), from Proto-Germanic *nahts (“night”), from Proto-Indo-European *nókʷts (“night”). "
         "Cognates Cognate with Scots nicht (“night”), Yola neeght, nieght, nyeght (“night”), North Frisian naacht, "
         "Nacht, noach, nåcht (“night”), Saterland Frisian Noacht (“night”), West Frisian nacht (“night”), Cimbrian "
         "and Dutch nacht (“night”), German and Low German Nacht (“night”), Luxembourgish Nuecht (“night”), "
         "Mòcheno nòcht (“night”), Vilamovian naocht (“night”), Yiddish נאַכט (nakht, “night”), Danish nat (“night”).")

out = structure_etym(NIGHT)
print(out, "\n")
L, C = rows(out, "L"), rows(out, "C")
assert [r[0] for r in L] == ["Middle English", "Old English", "Proto-West Germanic", "Proto-Germanic", "Proto-Indo-European"], L
assert L[0][1] == "nighte, night, nyght, niȝt, naht" and L[0][2] == ""
assert L[2][1] == "*naht" and L[2][2] == "night"
assert L[1][3] == "" and len(L) == 5
assert ["Scots", "nicht", "night"] in C
assert ["Yola", "neeght, nieght, nyeght", "night"] in C
assert ["North Frisian", "naacht, Nacht, noach, nåcht", "night"] in C
assert ["Cimbrian and Dutch", "nacht", "night"] in C
assert ["German and Low German", "Nacht", "night"] in C
assert ["Yiddish", "נאַכט (nakht)", "night"] in C
assert len(out) <= 900 and "…" not in out

# flat "Etymology tree" junk in front of the real prose (as seen for "etymology")
TREE = ("Etymology tree Proto-Indo-European *set-? Ancient Greek ἐτεός (eteós)der.? Ancient Greek ἔτυμος (étumos) "
        "Proto-Indo-European *leǵ- *-os Proto-Indo-European *lóǵos Proto-Hellenic *lógos Ancient Greek λόγος (lógos) "
        "Ancient Greek -λογία (-logía)bor. Latin etymologiader. Old French ethimologiebor. "
        "From Middle English ethimologie, from Old French ethimologie, from Latin etymologia, from Ancient Greek "
        "ἐτυμολογία (etumología), from ἔτυμος (étumos, “true”) + -λογία (-logía).")
out = structure_etym(TREE)
print(out, "\n")
L = rows(out, "L")
assert "tree" not in out.lower() and "bor." not in out and "der." not in out
assert [r[0] for r in L] == ["Middle English", "Old French", "Latin", "Ancient Greek", ""], L
assert L[3][1] == "ἐτυμολογία (etumología)"

# tree with nothing usable after it -> dropped, not shown as junk
assert structure_etym("Etymology tree Latin foo der. Old English bar") is None

# relations, notes, compounds
o = structure_etym("Borrowed from Latin Roma, from Etruscan ruma. Doublet of Rome.")
print(o, "\n")
assert rows(o, "L")[0][:1] == ["Latin"] and rows(o, "L")[0][3] == "borrowed"
assert rows(o, "N") == [["Doublet of Rome."]]
o = structure_etym("Compound of blue +‎ berry.")
assert rows(o, "N") and not rows(o, "L")
assert structure_etym("Unknown.") == "N\tUnknown."
assert structure_etym("") is None and structure_etym("   ") is None

# size cap drops cognates first, never cuts a record
small = structure_etym(NIGHT, cap=330)
assert len(small) <= 330 and rows(small, "L") and all(len(l.split("\t")) >= 3 for l in small.split("\n"))
print("ok")
