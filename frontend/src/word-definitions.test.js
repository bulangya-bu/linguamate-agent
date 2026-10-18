import test from "node:test";
import assert from "node:assert/strict";
import { wordDefinitionGroups } from "./word-definitions.js";
test("overlapping senses add only new meanings within the same part of speech", () => {
  assert.deepEqual(
    wordDefinitionGroups({
      word: "territory",
      partOfSpeech: "n",
      meaningCn: "领土；版图；领域",
      definitions: [
        { partOfSpeech: "n.", meaningCn: "领土，版图，领域，范围" },
        { partOfSpeech: "n", meaningCn: "范围；领土" },
      ],
    }),
    [{ partOfSpeech: "n.", meanings: ["领土；版图；领域", "范围"] }],
  );
});

test("keeps every local-bank part of speech and meaning, without repeating the primary answer", () => {
  assert.deepEqual(
    wordDefinitionGroups({
      word: "revolutionary",
      partOfSpeech: "adj",
      meaningCn: "革命的，革新的",
      definitions: [
        { partOfSpeech: "adj.", meaningCn: "革命的, 革新的" },
        { partOfSpeech: "n", meaningCn: "革命者" },
        { partOfSpeech: "n", meaningCn: "革命支持者" },
      ],
    }),
    [
      { partOfSpeech: "adj.", meanings: ["革命的，革新的"] },
      { partOfSpeech: "n.", meanings: ["革命者", "革命支持者"] },
    ],
  );
});

test("merges extra same-word AI senses into the word card and keeps derived words separate", () => {
  assert.deepEqual(
    wordDefinitionGroups(
      { word: "record", partOfSpeech: "n.", meaningCn: "记录" },
      {
        variants: [
          { form: "Record", type: "noun", meaningCn: "记录" },
          { form: "record", type: "verb", meaningCn: "记录；录制" },
          { form: "recorded", type: "过去式", meaningCn: "已记录" },
        ],
      },
    ),
    [
      { partOfSpeech: "n.", meanings: ["记录"] },
      { partOfSpeech: "v.", meanings: ["记录；录制"] },
    ],
  );
});

test("old cards and missing parts of speech retain their full definition", () => {
  assert.deepEqual(
    wordDefinitionGroups({
      word: "test",
      meaningCn: "测试；试验",
      partOfSpeech: "unknown",
    }),
    [{ partOfSpeech: "", meanings: ["测试；试验"] }],
  );
  assert.deepEqual(wordDefinitionGroups(null), []);
});
