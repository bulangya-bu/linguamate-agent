import assert from "node:assert/strict";
import test from "node:test";
import { reviewPrompt } from "../src/features/vocabulary/wordPresentation.js";

test("a real scratch example hides the whole past tense and restores the original text", () => {
  const exampleEn = "John yawned and scratched his leg.";
  const prompt = reviewPrompt({ word: "scratch", exampleEn, exampleInflections: ["scratched"] });
  assert.deepEqual(prompt.parts, ["John yawned and ", "scratched", " his leg."]);
  assert.equal(prompt.parts.join(""), exampleEn);
  assert.equal(prompt.inflected, true);
});

test("article sentences retain the saved surface form and mask every occurrence", () => {
  const prompt = reviewPrompt({
    word: "go", sourceWord: "went", sourceSentence: "We WENT home to go to bed.",
    exampleEn: "They go to school.", exampleCn: "他们去上学。",
  });
  assert.deepEqual(prompt.parts, ["We ", "WENT", " home to ", "go", " to bed."]);
  assert.equal(prompt.translation, "");
});

test("the dictionary example is used when the saved sentence does not match a whole word", () => {
  const prompt = reviewPrompt({
    word: "art", sourceSentence: "The artist arrived.", exampleEn: "I study art.", exampleCn: "我学艺术。",
  });
  assert.deepEqual(prompt.parts, ["I study ", "art", "."]);
  assert.equal(prompt.inflected, false);
  assert.equal(prompt.translation, "我学艺术。");
});

test("unmatched and generic examples switch to a spelling clue without displaying the answer", () => {
  for (const exampleEn of ["They visited the scratchpad.", 'The word "scratch" is useful in English practice.', ""]) {
    const prompt = reviewPrompt({ word: "scratch", exampleEn });
    assert.equal(prompt.parts, undefined);
    assert.match(prompt.hint, /＿/);
    assert.doesNotMatch(prompt.hint, /scratch/i);
  }
  assert.equal(reviewPrompt({ word: "I" }).hint, "字母表第 9 位");
});
