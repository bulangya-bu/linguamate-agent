const routeSections = new Map([
  ["/chat", "chat"],
  ["/vocabulary", "vocabulary"],
  ["/review", "review"],
  ["/writing", "writing"],
  ["/news", "news"]
]);

export function normalizeLearningAction(action) {
  if (!action || !routeSections.has(action.route)) return null;
  return {
    id: action.id || action.route,
    label: action.label || "前往学习",
    route: action.route,
    section: routeSections.get(action.route),
    query: action.query && typeof action.query === "object" ? action.query : {}
  };
}

export function learningMessageFromResponse(result = {}) {
  return {
    role: "assistant",
    content: result.reply || "",
    learningCard: result.learningCard || null,
    actions: (result.actions || []).map(normalizeLearningAction).filter(Boolean)
  };
}

export function nextVocabularyPage(card, direction) {
  const totalPages = Math.max(0, Number(card?.totalPages || 0));
  if (!totalPages) return 0;
  const current = Math.max(0, Number(card?.page || 0));
  return Math.min(totalPages - 1, Math.max(0, current + Number(direction || 0)));
}
