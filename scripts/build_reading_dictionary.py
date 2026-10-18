"""Build the bundled reading dictionary from a local ECDICT CSV (MIT).

Download the upstream CSV separately; this script never executes upstream code.
Usage: python scripts/build_reading_dictionary.py output/ecdict.csv
"""
import argparse
import csv
import gzip
import hashlib
import json
import re
from pathlib import Path

SOURCE_URL = "https://raw.githubusercontent.com/skywind3000/ECDICT/master/ecdict.csv"
TOKEN = re.compile(r"[a-z]+(?:['-][a-z]+)*")
HAN = re.compile(r"[\u3400-\u9fff]")
POS = re.compile(r"^(n|v|vt|vi|a|adj|adv|art|pron|prep|conj|int|interj|aux|num|pl|abbr|det|modal)\.\s*")
CONTRACTION = re.compile(r"(?:i|you|he|she|it|we|they|that|there|here|what|who|where|how|when|let|can|could|do|does|did|has|have|had|is|are|was|were|will|would|should|must|might|need|shall|shan|won|ain)n?'(?:t|s|d|ll|re|ve|m)")
INFLECTION = re.compile(r"过去式|过去分词|现在分词|名词复数|复数形式|的复数|第三人称|单数形式")
LOCAL_CONTRACTIONS = {
    "wouldn't": "would not 的缩写；不会；不愿",
    "hasn't": "has not 的缩写；没有",
    "haven't": "have not 的缩写；没有",
}


def exchanges(row):
    return dict(part.split(":", 1) for part in row.get("exchange", "").split("/") if ":" in part)


def glossary(row):
    lines = [line.strip() for line in row["translation"].replace("\\n", "\n").splitlines() if HAN.search(line)]
    everyday = [line for line in lines if not line.startswith("[")]
    chosen = (everyday or lines)[:4]
    positions, glosses = [], []
    for line in chosen:
        match = POS.match(line)
        if match:
            positions.append({"a": "adj", "int": "interj"}.get(match[1], match[1]) + ".")
            line = line[match.end():]
        glosses.append(re.sub(r"\s+", " ", line).strip())
    meaning = "；".join(dict.fromkeys(glosses))
    # The existing Word.meaningCn column holds 255 characters; retain complete short glosses.
    if len(meaning) > 240:
        cut = max(meaning.rfind("；", 100, 238), meaning.rfind("，", 100, 238), meaning.rfind(",", 100, 238))
        meaning = meaning[:cut if cut > 0 else 238].rstrip() + "…"
    return meaning, "/".join(dict.fromkeys(positions))[:60]


def build(csv_path, destination):
    rows = {}
    with csv_path.open(encoding="utf-8-sig", newline="") as source:
        for row in csv.DictReader(source):
            word = (row.get("word") or "").strip().lower()
            if TOKEN.fullmatch(word) and len(word) <= 40 and HAN.search(row.get("translation") or ""):
                rows.setdefault(word, row)
    def selected(word, row):
        return row["tag"] or row["oxford"] == "1" or int(row["collins"] or 0) > 0 or CONTRACTION.fullmatch(word) or any(0 < int(row[key] or 0) <= 50000 for key in ("bnc", "frq"))
    keep = {word for word, row in rows.items() if selected(word, row)}
    # Include the base of every selected inflection even if it lacks frequency metadata.
    for word in list(keep):
        base = exchanges(rows[word]).get("0", "")
        if base in rows:
            keep.add(base)
    entries, forms = [], {}
    for word in sorted(keep):
        row = rows[word]
        changes = exchanges(row)
        meaning, pos = glossary(row)
        raw_lines = [line for line in row["translation"].replace("\\n", "\n").splitlines() if HAN.search(line)]
        pure_form = raw_lines and all(INFLECTION.search(line) for line in raw_lines)
        base = changes.get("0", "") if pure_form else ""
        if pos == "pl." and changes.get("0"):
            base = changes["0"]
        entries.append([word, row["phonetic"].strip()[:110], meaning, pos, base if base in keep else ""])
        for kind, value in changes.items():
            if kind in ("p", "d", "i", "3", "s", "r", "t"):
                for form in value.split(","):
                    if TOKEN.fullmatch(form) and form != word:
                        forms.setdefault(form, word)
    for word, row in rows.items():
        base = exchanges(row).get("0", "")
        if word not in keep and base in keep:
            forms.setdefault(word, base)
    for word, meaning in LOCAL_CONTRACTIONS.items():
        if word not in keep:
            entries.append([word, "", meaning, "aux.", ""])
    entries.sort(key=lambda row: row[0])
    data = json.dumps({"entries": entries, "forms": dict(sorted(forms.items()))}, ensure_ascii=False, separators=(",", ":")).encode()
    destination.mkdir(parents=True, exist_ok=True)
    (destination / "reading-core.json.gz").write_bytes(gzip.compress(data, compresslevel=9, mtime=0))
    metadata = {"source": SOURCE_URL, "license": "MIT", "sourceSha256": hashlib.sha256(csv_path.read_bytes()).hexdigest(),
                "selection": "Exam-tagged, Oxford/Collins, BNC or contemporary frequency <= 50000, common contractions and their bases",
                "entries": len(entries), "inflections": len(forms), "localGrammarSupplements": LOCAL_CONTRACTIONS,
                "resourceSha256": hashlib.sha256((destination / "reading-core.json.gz").read_bytes()).hexdigest()}
    (destination / "source.json").write_text(json.dumps(metadata, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(metadata, ensure_ascii=True))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("csv", type=Path)
    parser.add_argument("--output", type=Path, default=Path("src/main/resources/dictionary"))
    arguments = parser.parse_args()
    build(arguments.csv, arguments.output)
