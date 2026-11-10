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

function strictReviewParts(result = {}) {
  return [
    result.spellingCorrect ? "拼写正确" : "拼写待修正",
    result.meaningCorrect ? "释义正确" : "释义待加强",
  ];
}

function reviewCloze(word) {
  const example =
    word?.sourceSentence ||
    word?.exampleEn ||
    `Use ${word?.word || "this word"} in a complete sentence.`;
  const term = word?.sourceWord || word?.word;
  if (!term) return escapeHtml(example);
  return String(example)
    .split(new RegExp(`(\\b${escapeRegExp(term)}\\b)`, "gi"))
    .map((piece) =>
      piece.toLowerCase() === term.toLowerCase()
        ? '<mark class="review-blank">_____</mark>'
        : escapeHtml(piece),
    )
    .join("");
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
  strictReviewParts,
  reviewCloze,
  highlightTerm,
  escapeHtml,
  escapeRegExp,
  modelLabel,
};
