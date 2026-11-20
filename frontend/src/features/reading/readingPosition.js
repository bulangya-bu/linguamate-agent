export function createReadingPositionStore(getAccountId) {
  const positions = new Map();

  function key(articleId) {
    const accountId = getAccountId();
    return accountId == null ? null : `linguamate:reading-position:${accountId}:${articleId}`;
  }

  function get(articleId) {
    const id = key(articleId);
    if (!id) return null;
    if (positions.has(id)) return positions.get(id);
    try {
      const position = JSON.parse(localStorage.getItem(id));
      if (!position || !Number.isInteger(position.paragraph) || position.paragraph < 0
        || !Number.isFinite(position.offset) || position.offset < 0 || position.offset > 1) return null;
      positions.set(id, position);
      return position;
    } catch {
      return null;
    }
  }

  function set(articleId, position) {
    const id = key(articleId);
    if (!id) return;
    positions.set(id, position);
    try {
      localStorage.setItem(id, JSON.stringify(position));
    } catch {
      // Keep the position for this session if browser storage is unavailable.
    }
  }

  return { get, set };
}

export function visibleParagraph(paragraphs, scrollTop) {
  const index = paragraphs.findIndex(paragraph => paragraph.offsetTop + paragraph.offsetHeight > scrollTop);
  return index === -1 ? paragraphs.length - 1 : index;
}
