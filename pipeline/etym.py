"""Turn Wiktionary's flat etymology text into the structured form the app renders (database v4).

Stored in the `etymology.text` column, one record per line, fields separated by TAB:

    L <lang> <terms> <gloss> <rel>   one step of the lineage, nearest ancestor first
    N <text>                         a free note ("Doublet of ...", "Compound of ...", "Unknown.")
    C <lang> <terms> <gloss>         one cognate

`terms` are comma separated variants; `rel` is one of  borrowed inherited learned derived via
ultimately probably possibly perhaps  or empty (plain "from"). The app falls back to showing the
text as one paragraph when it finds none of these tags, so an old v3 database still displays.

What this fixes compared to the old clean_etym():
  * "Etymology tree" template output (language lines plus der./bor. tags) is dropped;
  * the "Cognates" block is split off, so it can no longer eat the character budget;
  * the cut is made on whole records, never in the middle of a word ("Dan...").
"""
import re

# ---------------------------------------------------------------- language names
_MODIFIERS = {
    "Old", "Middle", "Proto", "Ancient", "Classical", "Medieval", "Late", "Vulgar", "Modern", "New",
    "Low", "High", "Early", "North", "South", "East", "West", "Northern", "Southern", "Eastern",
    "Western", "Central", "Upper", "Lower", "Biblical", "Koine", "Byzantine", "Imperial", "Standard",
    "Literary", "Anglo-Norman", "Ecclesiastical", "Neo-Latin", "Scientific", "Classic", "Norman",
    "Saterland", "Cimbrian", "Scottish", "American", "Brazilian", "Canadian", "Egyptian",
}
_LANGS = {
    "English", "Scots", "Latin", "Greek", "French", "German", "Dutch", "Norse", "Frisian", "Spanish",
    "Portuguese", "Italian", "Arabic", "Persian", "Hebrew", "Sanskrit", "Hindi", "Urdu", "Bengali",
    "Russian", "Polish", "Czech", "Slovak", "Serbo-Croatian", "Bulgarian", "Ukrainian", "Turkish",
    "Chinese", "Japanese", "Korean", "Welsh", "Irish", "Gaelic", "Breton", "Cornish", "Swedish",
    "Danish", "Norwegian", "Icelandic", "Faroese", "Gothic", "Celtic", "Germanic", "Hellenic",
    "Italic", "Slavic", "Indo-European", "Indo-Iranian", "Indo-Aryan", "Iranian", "Semitic",
    "Aramaic", "Akkadian", "Sumerian", "Coptic", "Basque", "Finnish", "Hungarian", "Estonian",
    "Romanian", "Catalan", "Occitan", "Provençal", "Yiddish", "Malay", "Tamil", "Swahili",
    "Nahuatl", "Quechua", "Tupi", "Lithuanian", "Latvian", "Albanian", "Armenian", "Thai",
    "Vietnamese", "Hawaiian", "Maori", "Afrikaans", "Luxembourgish", "Saxon", "Frankish",
    "Brythonic", "Goidelic", "Phoenician", "Etruscan", "Tocharian", "Hittite", "Avestan",
    "Pashto", "Punjabi", "Marathi", "Gujarati", "Tagalog", "Indonesian", "Javanese", "Berber",
    "Ottoman", "Venetian", "Sicilian", "Galician", "Walloon", "Picard", "Lombard",
}
KNOWN = _MODIFIERS | _LANGS


def _is_cap(tok):
    return tok[:1].isupper()


def split_lang(tokens):
    """Split leading language-name tokens from the terms: ['Old','English','niht'] -> ('Old English','niht')."""
    i, n = 0, len(tokens)
    while i < n - 1:                      # always leave at least one token for the terms
        t = tokens[i]
        if i == 0:
            if not _is_cap(t):
                break
        elif t in KNOWN:
            pass
        elif t == "and" and i + 1 < n - 1 and tokens[i + 1] in KNOWN:
            pass
        else:
            break
        i += 1
    return " ".join(tokens[:i]), " ".join(tokens[i:])


# ---------------------------------------------------------------- text helpers
_PROSE_START = (r"From|Borrowed from|Inherited from|Learned borrowing from|Semi-learned borrowing from|"
                r"Compound of|Back-formation from|Clipping of|Shortening of|Blend of|Univerbation of|"
                r"Calque of|Ultimately|Probably|Possibly|Perhaps|Uncertain|Unknown|Of unknown|"
                r"Derived from|Equivalent to|Cognate with|Via|Related to|Doublet of|Apparently|Originally")
_PROSE_RE = re.compile(rf"(?:^|(?<=\s))(?:{_PROSE_START})\b")


def strip_tree(text):
    """Drop the 'Etymology tree' template output; keep what comes before it and the prose after it."""
    i = text.find("Etymology tree")
    if i < 0:
        return text
    head, rest = text[:i].rstrip(), text[i + len("Etymology tree"):]
    m = _PROSE_RE.search(rest)
    return (head + " " + rest[m.start():]).strip() if m else head


def _depth_at(text, pos):
    seg = text[:pos]
    return (seg.count("(") - seg.count(")")) + (seg.count("“") - seg.count("”"))


def _split_top(text, pattern):
    """Split on `pattern`, but only where we are outside parentheses and quotes."""
    parts, last = [], 0
    for m in re.finditer(pattern, text):
        if _depth_at(text, m.start()) <= 0:
            parts.append(text[last:m.start()])
            last = m.end()
    parts.append(text[last:])
    return [p.strip() for p in parts if p.strip()]


_REL_WORDS = [
    ("semi-learned borrowing from", "learned"), ("learned borrowing from", "learned"),
    ("ultimately borrowed from", "ultimately"), ("ultimately from", "ultimately"),
    ("borrowed from", "borrowed"), ("inherited from", "inherited"), ("derived from", "derived"),
    ("probably from", "probably"), ("possibly from", "possibly"), ("perhaps from", "perhaps"),
    ("via", "via"), ("from", ""),
]
_REL_ALT = "|".join(re.escape(w) for w, _ in _REL_WORDS)
_REL_RE = re.compile("^(" + _REL_ALT + r")\s+", re.I)
_STEP_SPLIT = r",?\s+(?:and\s+)?(?=(?:" + _REL_ALT + r")\s)"


def _take_rel(text):
    m = _REL_RE.match(text)
    if not m:
        return None, text
    word = m.group(1).lower()
    return next(r for w, r in _REL_WORDS if w == word), text[m.end():]


def _extract_gloss(s):
    """Return (text without the quoted gloss, gloss)."""
    m = re.search(r"\s*\(\s*“([^”]*)”\s*\)", s)           # (“night”)
    if m:
        return s[:m.start()] + s[m.end():], m.group(1)
    m = re.search(r",\s*“([^”]*)”\)", s)                   # (nakht, “night”)
    if m:
        return s[:m.start()] + ")" + s[m.end():], m.group(1)
    m = re.search(r"\s*“([^”]*)”", s)
    if m:
        return s[:m.start()] + s[m.end():], m.group(1)
    return s, ""


def _clean(s):
    return re.sub(r"\s+", " ", s).strip(" ,;.:")


def _clip(s, cap):
    if len(s) <= cap:
        return s
    cut = s[:cap]
    k = cut.rfind(". ")
    if k > cap // 2:
        return cut[:k + 1]
    return (cut[:cut.rfind(" ")].rstrip(" ,;") if " " in cut else cut) + "…"


def parse_step(piece):
    """'Middle English nighte, night (“night”)' -> (lang, terms, gloss)."""
    body, gloss = _extract_gloss(piece)
    lang, terms = split_lang(_clean(body).split(" "))
    return lang, _clean(terms), _clean(gloss)


# ---------------------------------------------------------------- cognates
_COG_GROUP = re.compile(r"(?:^|,\s*|\s+and\s+)(.+?“[^”]*”\))")


def parse_cognates(text, limit):
    text = re.sub(r"^(?:Cognates?\s+with|Cognates?:?)\s+", "", text.strip(), flags=re.I)
    pieces, last = [], 0
    for m in _COG_GROUP.finditer(text):
        pieces.append(m.group(1))
        last = m.end()
    tail = text[last:].strip(" ,.;")
    if len(tail) > 2:
        pieces.append(tail)
    out = []
    for piece in pieces:
        body, gloss = _extract_gloss(piece)
        entries = []                                         # [lang, [terms]]
        for chunk in _split_top(body, r",\s+"):
            lang, terms = split_lang(chunk.split(" "))
            if lang:
                entries.append([lang, [terms]])
            elif entries:
                entries[-1][1].append(chunk)
            else:
                entries.append(["", [chunk]])
        for k, (lang, terms) in enumerate(entries):
            out.append((lang, _clean(", ".join(terms)), _clean(gloss) if k == len(entries) - 1 else ""))
    return out[:limit]


# ---------------------------------------------------------------- main entry
def structure_etym(raw, cap=900, max_steps=8, max_cognates=12, note_cap=240):
    """Structured etymology text for the database, or None when nothing usable is left."""
    text = " ".join(raw.replace("\u00a0", " ").split())
    text = strip_tree(text)
    if not text:
        return None

    cog_text = ""
    m = re.search(r"\s*\bCognates\b:?\s+", text)
    if m:
        text, cog_text = text[:m.start()], text[m.end():]

    steps, notes = [], []
    for sent in re.split(r"(?<=[.)”])\s+(?=[A-Z])", text):
        sent = sent.strip()
        if not sent:
            continue
        if re.match(r"Cognates? with\b", sent):
            cog_text = (cog_text + " " + sent).strip()
            continue
        rel0, body = _take_rel(sent) if not steps else (None, sent)
        if rel0 is None:
            notes.append(_clip(sent, note_cap))
            continue
        for k, part in enumerate(_split_top(body, _STEP_SPLIT)):
            rel = rel0
            if k:
                rel, part = _take_rel(part)
                rel = rel or ""
            lang, terms, gloss = parse_step(part)
            if terms:
                steps.append((lang, terms, gloss, rel or ""))

    cogs = parse_cognates(cog_text, max_cognates) if cog_text else []
    steps = steps[:max_steps]
    notes = notes[:2]

    def render():
        lines = ["L\t" + "\t".join(s) for s in steps]
        lines += ["N\t" + n for n in notes]
        lines += ["C\t" + "\t".join(c) for c in cogs]
        return "\n".join(lines)

    out = render()
    while len(out) > cap and cogs:
        cogs.pop()
        out = render()
    while len(out) > cap and notes:
        notes.pop()
        out = render()
    return out or None
