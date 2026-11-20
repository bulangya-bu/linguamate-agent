function phonetic(word) {
  const value = word?.phoneticUs || word?.phoneticUk || "";
  if (!value) return "";
  const clean = String(value).replaceAll("/", "");
  return `/${clean}/`;
}

function categoryValue(category) {
  return category?.tag || category?.code || "";
}

function categoryLabel(category) {
  const label = category?.label || categoryValue(category) || "未命名";
  return category?.count ? `${label} (${category.count})` : label;
}

function quizExampleText(word, result = {}) {
  if (word?.sourceSentence) return word.sourceSentence;
  return (
    result?.exampleEn ||
    word?.exampleEn ||
    `Try to use ${word?.word || "this word"} in a complete sentence.`
  );
}

function quizExampleCn(word, result = {}) {
  if (word?.sourceSentence) return "";
  return (
    result?.exampleCn || word?.exampleCn || "试着把这个单词放进一个完整句子里。"
  );
}

function wordCardExampleEn(word) {
  return (
    word?.exampleEn ||
    `The word "${word?.word || "this word"}" is useful in English practice.`
  );
}

function meaningfulExample(word) {
  return (
    Boolean(word?.exampleEn?.trim()) &&
    !/^The word ["“].+["”] is useful in English practice\.$/.test(
      word.exampleEn.trim(),
    )
  );
}

function wordCardExampleCn(word) {
  return (
    word?.exampleCn || `单词“${word?.word || "这个单词"}”在英语练习中很有用。`
  );
}

function spellingHint(headword) {
  const letters = [...headword];
  const letterCount = letters.filter((letter) => /[a-z]/i.test(letter)).length;
  if (letterCount === 1 && /^[a-z]$/i.test(headword)) {
    return `字母表第 ${headword.toUpperCase().charCodeAt(0) - 64} 位`;
  }
  let letterIndex = 0;
  return letters.map((letter) => {
    if (!/[a-z]/i.test(letter)) return letter;
    letterIndex += 1;
    return letterIndex % 3 === 2 || letterCount === 1 ? "＿" : letter;
  }).join("");
}

function reviewPrompt(word) {
  const headword = word?.word?.trim() || "";
  const knownForms = [headword, ...(word?.exampleInflections || [])];
  const translation = word?.exampleCn?.trim() || "";
  const examples = [
    { text: word?.sourceSentence, forms: [...knownForms, word?.sourceWord], translation: "" },
    {
      text: meaningfulExample(word) ? word.exampleEn : "",
      forms: knownForms,
      translation: /^单词“.+”在英语练习中很有用。$/.test(translation) ? "" : translation,
    },
  ];
  for (const { text, forms, translation } of examples) {
    if (!text?.trim()) continue;
    const terms = [...new Set(forms.filter(Boolean))].sort((a, b) => b.length - a.length);
    if (!terms.length) continue;
    const alternatives = terms.map((term) => escapeRegExp(term).replace(/['’]/g, "['’]")).join("|");
    const pattern = new RegExp(`(?<![A-Za-z0-9_-])(${alternatives})(?![A-Za-z0-9_-])`, "gi");
    // Captured entries retain their original spelling for the marked answer after submission.
    const parts = text.split(pattern);
    if (parts.length > 1) {
      return {
        parts,
        translation,
        inflected: parts.some((part, index) => index % 2 === 1 && part.toLowerCase() !== headword.toLowerCase()),
      };
    }
  }
  return { hint: spellingHint(headword) };
}

function highlightTerm(text, term) {
  if (!term) return escapeHtml(text || "");
  const pieces = String(text || "").split(
    new RegExp(`(${escapeRegExp(term)})`, "gi"),
  );
  return pieces
    .map((piece) =>
      piece.toLowerCase() === String(term).toLowerCase()
        ? `<mark>${escapeHtml(piece)}</mark>`
        : escapeHtml(piece),
    )
    .join("");
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;");
}

function escapeRegExp(value) {
  return String(value ?? "").replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function modelLabel(detail) {
  if (!detail?.modelName) return "";
  return detail.modelName === "local-rules" ? "本地词库兜底" : detail.modelName;
}

export {
  phonetic,
  categoryValue,
  categoryLabel,
  quizExampleText,
  quizExampleCn,
  wordCardExampleEn,
  meaningfulExample,
  wordCardExampleCn,
  reviewPrompt,
  highlightTerm,
  escapeHtml,
  escapeRegExp,
  modelLabel,
};
