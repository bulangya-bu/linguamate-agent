const posNames = {
  noun: "n",
  verb: "v",
  adjective: "adj",
  adverb: "adv",
  ad: "adv",
  pronoun: "pron",
  preposition: "prep",
  conjunction: "conj",
};

export function wordDefinitionGroups(word, detail) {
  if (!word) return [];
  const entries = [
    { partOfSpeech: word.partOfSpeech, meaningCn: word.meaningCn },
    ...(word.definitions || []),
    ...(detail?.variants || [])
      .filter(
        (item) =>
          item.form?.trim().toLowerCase() === word.word?.trim().toLowerCase(),
      )
      .map((item) => ({ partOfSpeech: item.type, meaningCn: item.meaningCn })),
  ];
  const groups = new Map();
  for (const entry of entries) {
    const meaning = entry.meaningCn?.trim();
    if (!meaning) continue;
    const rawPos = (entry.partOfSpeech || "")
      .trim()
      .toLowerCase()
      .replace(/\.$/, "");
    const pos = posNames[rawPos] || (rawPos === "unknown" ? "" : rawPos);
    if (!groups.has(pos))
      groups.set(pos, {
        partOfSpeech: pos ? pos + (/^[a-z]+$/.test(pos) ? "." : "") : "",
        meanings: [],
      });
    const group = groups.get(pos);
    const key = meaning.replace(/[\s,，;；]+/g, "");
    if (
      !group.meanings.some((item) => item.replace(/[\s,，;；]+/g, "") === key)
    )
      group.meanings.push(meaning);
  }
  return [...groups.values()];
}
