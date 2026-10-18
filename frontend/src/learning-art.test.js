import test from "node:test";
import assert from "node:assert/strict";
import { discoveryWords, shuffledWords } from "./learning-art.js";

test("discovery pages visit the whole bilingual deck before repeating", () => {
  const deck = shuffledWords("", () => 0.41);
  assert.equal(deck.length, discoveryWords.length);
  assert.equal(new Set(deck.map((word) => word.en)).size, deck.length);
  assert.ok(deck.every((word) => word.en && word.cn));
  assert.notDeepEqual(deck, discoveryWords);
});
test("a new deck cannot immediately repeat the previous page", () => {
  const first = shuffledWords("", () => 0.99999)[0].en;
  assert.notEqual(shuffledWords(first, () => 0.99999)[0].en, first);
});
