import test from 'node:test';
import assert from 'node:assert/strict';
import { evidenceRecords } from './components/growth/evidenceRecords.js';

test('legacy two-part review answers can be shown separately without inventing individual results', () => {
  const data = { words: [{ wordId: 1, attempts: [
    { id: 1, type: 'STRICT_REVIEW', answer: 'preserv | 保护', correct: false },
    { id: 2, type: 'STRICT_REVIEW', spellingAnswer: 'preserve', meaningAnswer: '保护', correct: true },
  ] }] };
  const [word] = evidenceRecords(data).words;
  assert.equal(word.attempts[0].spellingAnswer, 'preserv');
  assert.equal(word.attempts[0].meaningAnswer, '保护');
  assert.equal(word.attempts[0].spellingCorrect, undefined);
  assert.equal(word.corrected, true);
  assert.equal(word.attempts[1], data.words[0].attempts[1]);
});
