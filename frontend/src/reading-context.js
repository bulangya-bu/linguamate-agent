export function readingTokens(text = "") {
  const sentences = [...text.matchAll(/[^.!?\n]+(?:[.!?]+|$)/g)].map(
    (match) => ({
      start: match.index,
      end: match.index + match[0].length,
      text: match[0].trim(),
    }),
  );
  return [...text.matchAll(/[A-Za-z][A-Za-z'’-]*|[^A-Za-z]+/g)].map(
    (match, index) => ({
      key: `${index}-${match[0]}`,
      text: match[0],
      word: /^[A-Za-z][A-Za-z'’-]*$/.test(match[0]),
      sentence:
        sentences.find(
          (sentence) =>
            match.index >= sentence.start && match.index < sentence.end,
        )?.text || "",
    }),
  );
}

// Split only between sentences; keep the original characters and lookup context intact.
export function readingParagraphs(text = "") {
  const tokens = readingTokens(text);
  const result = [];
  let paragraph = [],
    sentenceCount = 0;
  for (const token of tokens) {
    paragraph.push(token);
    if (!token.word && /[.!?]/.test(token.text)) sentenceCount++;
    if (!token.word && (/\n\s*\n/.test(token.text) || sentenceCount >= 3)) {
      result.push(paragraph);
      paragraph = [];
      sentenceCount = 0;
    }
  }
  if (paragraph.length) result.push(paragraph);
  return result;
}
