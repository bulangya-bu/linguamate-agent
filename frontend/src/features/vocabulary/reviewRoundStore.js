// Each browser tab can run its own review without overwriting another tab's answers.
export function createReviewRoundStore() {
  const fallback = new Map();

  function key(accountId, mode) {
    return `linguamate:review-round:v1:${accountId}:${mode}`;
  }

  function getValue(id) {
    if (fallback.has(id)) return fallback.get(id);
    try {
      return sessionStorage.getItem(id);
    } catch {
      return null;
    }
  }

  function setValue(id, raw) {
    fallback.set(id, raw);
    try {
      if (raw !== null) sessionStorage.setItem(id, raw);
      else sessionStorage.removeItem(id);
    } catch {
      // Keep the round for in-app navigation when storage is unavailable.
    }
  }

  function read(accountId, mode) {
    const raw = getValue(key(accountId, mode));
    if (!raw) return null;
    try {
      const round = JSON.parse(raw);
      if (!Array.isArray(round?.words) || !round.words.length) return null;
      const validWords = round.words.every(word => word?.id != null && typeof word.word === "string");
      const validCounts = Number.isInteger(round.total) && round.total >= round.words.length &&
        Number.isInteger(round.mistakes) && round.mistakes >= 0;
      const validDraft = typeof round.form?.spellingAnswer === "string" &&
        typeof round.form?.meaningAnswer === "string";
      const validFeedback = round.result === null || (
        typeof round.result?.correct === "boolean" &&
        String(round.result.wordId) === String(round.words[0]?.id)
      );
      return validWords && validCounts && validDraft && validFeedback ? round : null;
    } catch {
      return null;
    }
  }

  function write(accountId, mode, round) {
    setValue(key(accountId, mode), round ? JSON.stringify(round) : null);
  }

  function lastMode(accountId) {
    return getValue(key(accountId, "mode")) === "mistakes" ? "mistakes" : "today";
  }

  function rememberMode(accountId, mode) {
    setValue(key(accountId, "mode"), mode);
  }

  return { read, write, lastMode, rememberMode };
}
