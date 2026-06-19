"""
Build word bank SQL from KyleBing CET-4/CET-6 JSON files.
Downloads JSON from GitHub, extracts: word, usphone, ukphone, Chinese meaning, part of speech.
Generates SQL INSERT statements compatible with the existing data.sql format.
"""
import json
import os
import sys
import urllib.request
import re

# Config
CET4_URL = "https://raw.githubusercontent.com/KyleBing/english-vocabulary/master/json_original/json-full/CET4_3.json"
CET6_URL = "https://raw.githubusercontent.com/KyleBing/english-vocabulary/master/json_original/json-full/CET6_3.json"
OUTPUT_FILE = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "word_bank.sql")
TEMP_DIR = os.path.join(os.path.dirname(__file__), "..", "output", "word_bank")

def download(url, dest, retries=3):
    import time
    for i in range(retries):
        try:
            print(f"Downloading: {url} (attempt {i+1}/{retries})")
            req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
            with urllib.request.urlopen(req, timeout=120) as resp:
                with open(dest, "wb") as f:
                    f.write(resp.read())
            size = os.path.getsize(dest)
            print(f"  -> {dest} ({size:,} bytes)")
            return
        except Exception as e:
            print(f"  Attempt {i+1} failed: {e}")
            if i < retries - 1:
                wait = 3 * (i + 1)
                print(f"  Retrying in {wait}s...")
                time.sleep(wait)
            else:
                raise

def parse_words(filepath):
    """Parse KyleBing full-format JSON, extract (word, usphone, ukphone, meaning_cn, pos)."""
    with open(filepath, "r", encoding="utf-8") as f:
        data = json.load(f)

    words = []
    seen = set()
    for entry in data:
        try:
            content = entry.get("content", {}).get("word", {}).get("content", {})
            word = (entry.get("headWord") or "").strip()
            if not word or len(word) < 2 or len(word) > 30:
                continue
            # Only pure English words (no spaces, no Chinese chars)
            if not re.match(r'^[a-zA-Z\'-]+$', word):
                continue
            word_lower = word.lower()
            if word_lower in seen:
                continue
            seen.add(word_lower)

            usphone = content.get("usphone", "")
            ukphone = content.get("ukphone", "")

            # Get first Chinese translation
            trans_list = content.get("trans", [])
            meaning_cn = ""
            pos = ""
            for t in trans_list:
                if t.get("tranCn"):
                    meaning_cn = t["tranCn"].strip()
                    pos = (t.get("pos") or "").strip()
                    break

            if not meaning_cn:
                continue

            # Clean up phonetics - add / / if not present
            if usphone and not usphone.startswith("/"):
                usphone = "/" + usphone + "/"
            if ukphone and not ukphone.startswith("/"):
                ukphone = "/" + ukphone + "/"

            words.append({
                "word": word_lower,
                "usphone": usphone,
                "ukphone": ukphone,
                "meaning_cn": meaning_cn,
                "pos": pos,
            })
        except Exception:
            continue

    return words

def escape_sql(s):
    """Escape single quotes for SQL."""
    return s.replace("'", "''")

def generate_sql(words, source_label):
    """Generate SQL INSERT statements."""
    lines = []
    lines.append(f"-- {source_label}: {len(words)} words")
    lines.append("INSERT INTO word (word, phonetic_us, phonetic_uk, meaning_cn, part_of_speech, level_tag)")
    lines.append("VALUES")

    values = []
    for w in words:
        vals = "('{}', '{}', '{}', '{}', '{}', 'cet4-6-bank')".format(
            escape_sql(w["word"]),
            escape_sql(w["usphone"]),
            escape_sql(w["ukphone"]),
            escape_sql(w["meaning_cn"]),
            escape_sql(w["pos"]),
        )
        values.append(vals)

    lines.append(",\n".join(values) + ";")
    lines.append("")
    return "\n".join(lines)

def main():
    os.makedirs(TEMP_DIR, exist_ok=True)

    # Download files
    cet4_path = os.path.join(TEMP_DIR, "CET4_3.json")
    cet6_path = os.path.join(TEMP_DIR, "CET6_3.json")

    if not os.path.exists(cet4_path):
        download(CET4_URL, cet4_path)
    else:
        print(f"Using cached: {cet4_path}")

    if not os.path.exists(cet6_path):
        download(CET6_URL, cet6_path)
    else:
        print(f"Using cached: {cet6_path}")

    # Parse
    cet4_words = parse_words(cet4_path)
    print(f"CET-4 words: {len(cet4_words)}")

    cet6_words = parse_words(cet6_path)
    print(f"CET-6 words: {len(cet6_words)}")

    # Deduplicate CET-6 against CET-4
    cet4_set = {w["word"] for w in cet4_words}
    cet6_new = [w for w in cet6_words if w["word"] not in cet4_set]
    print(f"CET-6 new (after dedup): {len(cet6_new)}")

    # Generate SQL
    sql_lines = []
    sql_lines.append("-- ============================================")
    sql_lines.append("-- Local Word Bank: CET-4 + CET-6 core vocabulary")
    sql_lines.append(f"-- Total: {len(cet4_words) + len(cet6_new)} words")
    sql_lines.append("-- Generated by scripts/build_word_bank.py")
    sql_lines.append("-- Source: KyleBing/english-vocabulary (kajweb/dict)")
    sql_lines.append("-- ============================================")
    sql_lines.append("")
    sql_lines.append(generate_sql(cet4_words, "CET-4"))
    sql_lines.append(generate_sql(cet6_new, "CET-6"))

    output = "\n".join(sql_lines)

    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        f.write(output)

    total = len(cet4_words) + len(cet6_new)
    size_kb = len(output.encode("utf-8")) / 1024
    print(f"\nWritten: {OUTPUT_FILE}")
    print(f"Total words: {total}")
    print(f"File size: {size_kb:.0f} KB")
    print("Done!")

if __name__ == "__main__":
    main()
