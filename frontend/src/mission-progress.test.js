import test from "node:test";
import assert from "node:assert/strict";
import { missionProgress, missionDraftKey } from "./mission-progress.js";
import { readingParagraphs, readingTokens } from "./reading-context.js";
test("daily completion comes only from the current saved mission", () => {
  assert.deepEqual(missionProgress().done, [false, false, false, false]);
  const p = missionProgress({
    progress: {
      readingAt: "2026-10-18",
      words: [1, 2, 3],
      turns: [{ step: 0 }, { step: 0 }, { step: 1 }],
      hints: [1, 2, 0],
    },
  });
  assert.deepEqual(p.done, [true, true, false, false]);
  assert.equal(p.practiced, 2);
  assert.equal(p.retries, 1);
  assert.equal(p.hintCount, 3);
  assert.equal(p.current, 2);
});
test("all three scene steps and a saved reflection are required", () => {
  const p = missionProgress({
    progress: {
      readingAt: "now",
      words: [1, 2, 3],
      turns: [{ step: 0 }, { step: 1 }, { step: 2 }],
      reflectedAt: "now",
    },
  });
  assert.equal(p.completed, 4);
  assert.equal(p.retries, 0);
  assert.notEqual(missionDraftKey(1, 3, 0), missionDraftKey(2, 3, 0));
  assert.notEqual(missionDraftKey(1, 3, 0), missionDraftKey(1, 3, 1));
});
test("reading paragraphs preserve every original token and its sentence lookup context", () => {
  const source =
    "When sensors detect a hand, water flows. It stops. That saves water. A student can report leaks.\n\nIt's a daily habit!";
  const paragraphs = readingParagraphs(source);
  assert.equal(paragraphs.length, 3);
  assert.equal(
    paragraphs
      .flat()
      .map((t) => t.text)
      .join(""),
    source,
  );
  assert.deepEqual(paragraphs.flat(), readingTokens(source));
});
