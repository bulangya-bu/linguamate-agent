export const missionStages = [
  { id: "read", title: "读懂主题", section: "news" },
  { id: "words", title: "练习词语", section: "vocabulary" },
  { id: "scene", title: "情境表达", section: "chat" },
  { id: "recap", title: "留下收获", section: "journal" },
];
export function missionProgress(mission) {
  const p = mission?.progress || {};
  const turns = p.turns || [];
  const practiced = new Set(turns.map((t) => t.step)).size;
  const done = [
    !!p.readingAt,
    (p.words || []).length === 3,
    practiced === 3,
    !!p.reflectedAt,
  ];
  return {
    done,
    completed: done.filter(Boolean).length,
    practiced,
    current: Math.max(
      0,
      done.findIndex((value) => !value),
    ),
    hintCount: (p.hints || []).reduce((a, b) => a + b, 0),
    retries: turns.length - practiced,
  };
}
export function missionDraftKey(userId, sessionId, step) {
  return `linguamate:mission:${userId}:${sessionId}:${step}`;
}
