import test from "node:test";
import assert from "node:assert/strict";
import { createReviewRoundStore } from "./features/vocabulary/reviewRoundStore.js";

function mockStorage(t) {
  const values = new Map();
  const previous = Object.getOwnPropertyDescriptor(globalThis, "sessionStorage");
  Object.defineProperty(globalThis, "sessionStorage", {
    configurable: true,
    value: {
      getItem: key => values.get(key) ?? null,
      setItem: (key, value) => values.set(key, value),
      removeItem: key => values.delete(key),
    },
  });
  t.after(() => {
    if (previous) Object.defineProperty(globalThis, "sessionStorage", previous);
    else delete globalThis.sessionStorage;
  });
  return values;
}

function answeredRound() {
  return {
    words: [{ id: 2, word: "adapt" }, { id: 1, word: "preserve" }],
    total: 2,
    mistakes: 1,
    form: { spellingAnswer: "adpt", meaningAnswer: "适应" },
    result: { wordId: 2, correct: false, spellingCorrect: false, meaningCorrect: true },
  };
}

test("refresh restores only the selected account and mode, finishing clears only that round", t => {
  mockStorage(t);
  const store = createReviewRoundStore();
  const round = answeredRound();
  store.write(1, "today", round);
  store.write(1, "mistakes", { ...round, result: null });
  store.rememberMode(1, "mistakes");

  const restored = createReviewRoundStore();
  assert.deepEqual(restored.read(1, "today"), round);
  assert.equal(restored.read(1, "mistakes").result, null);
  assert.equal(restored.read(2, "today"), null);
  assert.equal(restored.lastMode(1), "mistakes");
  assert.equal(restored.lastMode(2), "today");
  restored.write(1, "today", null);
  const reopened = createReviewRoundStore();
  assert.equal(reopened.read(1, "today"), null);
  assert.deepEqual(reopened.read(1, "mistakes").words, round.words);
});

test("a damaged snapshot or another word's feedback starts a fresh round", t => {
  const values = mockStorage(t);
  const store = createReviewRoundStore();
  const round = answeredRound();
  store.write(1, "today", round);
  const id = [...values.keys()][0];
  for (const raw of [
    "{",
    JSON.stringify({ ...round, total: 1 }),
    JSON.stringify({ ...round, result: { wordId: 1, correct: false } }),
  ]) {
    values.set(id, raw);
    assert.equal(createReviewRoundStore().read(1, "today"), null);
  }
});

test("tab navigation still preserves the round when the browser refuses storage", t => {
  mockStorage(t);
  globalThis.sessionStorage.setItem = () => { throw new Error("storage blocked"); };
  globalThis.sessionStorage.getItem = () => { throw new Error("storage blocked"); };
  const store = createReviewRoundStore();
  assert.equal(store.read(1, "today"), null);
  const round = answeredRound();
  store.write(1, "today", round);
  round.form.spellingAnswer = "edited";
  assert.equal(store.read(1, "today").form.spellingAnswer, "adpt");
  store.write(1, "today", null);
  assert.equal(store.read(1, "today"), null);
});
