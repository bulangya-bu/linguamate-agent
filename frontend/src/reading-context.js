export function readingTokens(text = '') {
  const sentences = [...text.matchAll(/[^.!?\n]+(?:[.!?]+|$)/g)].map(match => ({start:match.index,end:match.index+match[0].length,text:match[0].trim()}));
  return [...text.matchAll(/[A-Za-z][A-Za-z'’-]*|[^A-Za-z]+/g)].map((match,index) => ({
    key:`${index}-${match[0]}`,text:match[0],word:/^[A-Za-z][A-Za-z'’-]*$/.test(match[0]),
    sentence:sentences.find(sentence => match.index >= sentence.start && match.index < sentence.end)?.text || '',
  }));
}
