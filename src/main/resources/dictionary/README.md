# Reading dictionary

`reading-core.json.gz` is an offline lookup supplement built from
[ECDICT](https://github.com/skywind3000/ECDICT), distributed under the included
`ECDICT-LICENSE.txt` (MIT). The source URL, checksums and entry counts are recorded
in `source.json`.

The subset includes exam-tagged vocabulary, Oxford/Collins entries, words ranked
within 50,000 in either source frequency list, common contractions and their bases.
The bundled data contains approximately 58,000 entries and 39,653 inflection mappings. It does
not require a network connection or an AI key at runtime.

Lookups prefer existing Chinese definitions in the application's word bank.
The supplement supplies missing words and known word forms. Pure inflections
such as `went` and `scientists` resolve to their base entries; independent senses
such as the noun `running` are retained. Upstream IPA is stored as British IPA,
without inventing a separate American pronunciation. Glosses are shortened to
fit the existing 255-character database column. The index does not supply example
sentences; existing word-bank examples are retained when available.
Three common negative contractions missing Chinese text upstream have short local
grammar glosses, recorded separately in `source.json`; no IPA is invented for them.

The index is read-only. A word enters the database and the current user's review
queue when that user explicitly saves it. Existing incomplete definitions can be
repaired during that save, without replacing the word ID or other users' progress.
Unresolved placeholder entries cannot be saved as learning material.

To reproduce, download the upstream CSV to a local ignored directory, verify its
SHA-256 against `source.json`, then run from the repository root:

```powershell
python scripts/build_reading_dictionary.py output/ecdict.csv
```

Keep the upstream license alongside any redistributed subset. The full CSV is
not bundled with the application.
