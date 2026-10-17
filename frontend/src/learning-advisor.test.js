import test from "node:test";
import assert from "node:assert/strict";
import { learningMessageFromResponse, nextVocabularyPage, normalizeLearningAction } from "./learning-advisor.js";

test("learning response keeps structured cards and only allows known routes", () => {
  const message = learningMessageFromResponse({
    reply: "继续完成今天的任务。",
    learningCard: { type: "daily-route", items: [] },
    actions: [
      { id: "review", label: "复习", section: "review", route: "/review", query: { mode: "today" } },
      { id: "unsafe", label: "离开", section: "unsafe", route: "https://example.com", query: {} }
    ]
  });

  assert.equal(message.role, "assistant");
  assert.equal(message.learningCard.type, "daily-route");
  assert.deepEqual(message.actions.map((item) => item.route), ["/review"]);
});

test("action normalization derives its section and pagination stays in range", () => {
  assert.equal(normalizeLearningAction({ label: "写作", route: "/writing" }).section, "writing");
  assert.equal(nextVocabularyPage({ page: 0, totalPages: 3 }, 1), 1);
  assert.equal(nextVocabularyPage({ page: 2, totalPages: 3 }, 1), 2);
  assert.equal(nextVocabularyPage({ page: 0, totalPages: 3 }, -1), 0);
});
