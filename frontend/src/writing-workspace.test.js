import test from 'node:test';
import assert from 'node:assert/strict';
import { annotatedText,revisionDiff,wordCount } from './writing-workspace.js';
import { readingTokens } from './reading-context.js';
import { learningMessageFromHistory } from './learning-advisor.js';
test('annotations preserve original text and handle overlapping or absent suggestions',()=>{
  const text='I has a book. I read it.';
  const parts=annotatedText(text,[{originalSentence:'I has a book.'},{originalSentence:'has a book'},{originalSentence:'missing'}]);
  assert.equal(parts.map(p=>p.text).join(''),text);
  assert.equal(parts.filter(p=>p.index!==undefined).length,1);
  assert.equal(parts.find(p=>p.index!==undefined).index,0);
});
test('revision diff reconstructs both versions including punctuation and whitespace',()=>{
  for(const [before,after] of [['I has a book.\nIt good.','I have a book.\nIt is good.'],['','Hello'],['A',''],['<script>','<strong>']]) {
    const parts=revisionDiff(before,after);
    assert.equal(parts.filter(p=>p.type!=='insert').map(p=>p.text).join(''),before);
    assert.equal(parts.filter(p=>p.type!=='delete').map(p=>p.text).join(''),after);
  }
  assert.equal(wordCount("I'm learning. It's a well-known idea."),6);
});
test('reading tokens retain the exact selected sentence including curly apostrophes',()=>{
  const text='Scientists share ideas. It’s useful!\nTry again.';
  const parts=readingTokens(text);
  assert.equal(parts.map(p=>p.text).join(''),text);
  assert.equal(parts.find(p=>p.text==='share').sentence,'Scientists share ideas.');
  assert.equal(parts.find(p=>p.text==='It’s').sentence,'It’s useful!');
  assert.equal(parts.find(p=>p.text==='again').sentence,'Try again.');
});
test('reading discussion restores its article attachment without putting raw article text into the message',()=>{
  const message=learningMessageFromHistory({role:'user',content:'讨论这篇文章',metadata:JSON.stringify({mode:'practice',readingArticle:{id:123,title:'Reading Science'}})});
  assert.deepEqual(message.readingArticle,{id:123,title:'Reading Science'});
  assert.equal(message.content,'讨论这篇文章');
  assert.equal(learningMessageFromHistory({metadata:'{"readingArticle":{"id":"bad","title":"x"}}'}).readingArticle,null);
});
