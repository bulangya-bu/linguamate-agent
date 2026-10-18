export function wordCount(text = '') {
  return (text.match(/[A-Za-z]+(?:['’-][A-Za-z]+)*/g) || []).length;
}

export function annotatedText(text = '', feedback = []) {
  const matches = feedback.flatMap((item, index) => {
    const phrase = item.originalSentence?.trim();
    const start = phrase ? text.indexOf(phrase) : -1;
    return start < 0 ? [] : [{ start, end: start + phrase.length, index }];
  }).sort((a, b) => a.start - b.start || b.end - a.end);
  let cursor = 0;
  const parts = [];
  for (const match of matches) {
    if (match.start < cursor) continue;
    if (match.start > cursor) parts.push({ text: text.slice(cursor, match.start) });
    parts.push({ text: text.slice(match.start, match.end), index: match.index });
    cursor = match.end;
  }
  if (cursor < text.length) parts.push({ text: text.slice(cursor) });
  return parts;
}

// Token-level LCS preserves every character while identifying actual edits.
export function revisionDiff(before = '', after = '') {
  const tokenize = text => text.match(/\s+|[A-Za-z0-9]+(?:['’-][A-Za-z]+)*|[^\sA-Za-z0-9]/g) || [];
  const a = tokenize(before), b = tokenize(after);
  if (a.length * b.length > 2_000_000) return [{ type: 'delete', text: before }, { type: 'insert', text: after }];
  const table = Array.from({ length: a.length + 1 }, () => new Uint16Array(b.length + 1));
  for (let i = a.length - 1; i >= 0; i--) for (let j = b.length - 1; j >= 0; j--)
    table[i][j] = a[i] === b[j] ? table[i + 1][j + 1] + 1 : Math.max(table[i + 1][j], table[i][j + 1]);
  const result = [];
  function push(type, text) {
    if (result.at(-1)?.type === type) result.at(-1).text += text;
    else result.push({ type, text });
  }
  let i = 0, j = 0;
  while (i < a.length || j < b.length) {
    if (i < a.length && j < b.length && a[i] === b[j]) { push('same', a[i++]); j++; }
    else if (j < b.length && (i === a.length || table[i][j + 1] >= table[i + 1][j])) push('insert', b[j++]);
    else push('delete', a[i++]);
  }
  return result;
}
