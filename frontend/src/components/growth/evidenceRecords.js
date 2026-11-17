export const evidenceTabs = [
  { id: 'words', label: '词汇', icon: 'vocabulary', unit: '词', route: 'vocabulary', action: '继续练词' },
  { id: 'expressions', label: '对话', icon: 'chat', unit: '组', route: 'chat', action: '继续对话' },
  { id: 'writing', label: '写作', icon: 'writing', unit: '篇', route: 'writing', action: '打开写作工坊' },
  { id: 'reading', label: '阅读', icon: 'news', unit: '篇', route: 'news', action: '打开阅读空间' },
];

export { formatDate as evidenceDate } from '../../api/client.js';

export function hasModelFeedback(turn) {
  return turn.feedback?.source === 'model' && Boolean(turn.feedback.improved);
}

function conversationRecords(turns) {
  const groups = new Map();
  for (const turn of turns) {
    if (turn.mode === 'assistant') continue;
    const id = turn.conversationId ?? turn.id;
    if (!groups.has(id)) groups.set(id, { id, title: turn.title || '学习对话', turns: [] });
    groups.get(id).turns.push(turn);
  }

  return [...groups.values()].map(group => {
    const latest = group.turns[0];
    const example = group.turns.find(hasModelFeedback) ?? latest;
    const count = group.turns.filter(hasModelFeedback).length;
    return {
      ...group,
      example,
      subtitle: example.answer,
      date: latest.createdAt,
      status: count ? count + ' 轮有参考表达' : group.turns.length + ' 轮',
    };
  }).sort((a, b) => Number(hasModelFeedback(b.example)) - Number(hasModelFeedback(a.example))
    || (b.date || '').localeCompare(a.date || ''));
}

function wordRecord(word) {
  const attempts = (word.attempts ?? []).map(attempt => {
    if (attempt.type !== 'STRICT_REVIEW' || attempt.spellingAnswer != null || attempt.meaningAnswer != null) return attempt;
    const answers = attempt.answer?.split(' | ');
    if (answers?.length !== 2) return attempt;
    return { ...attempt, spellingAnswer: answers[0], meaningAnswer: answers[1] };
  });
  const latest = attempts.at(-1);
  const corrected = latest?.correct && latest.type === 'STRICT_REVIEW'
    && attempts.slice(0, -1).some(attempt => attempt.correct === false);
  return {
    ...word,
    attempts,
    id: word.wordId,
    title: word.word,
    subtitle: word.meaning,
    date: latest?.createdAt,
    status: latest ? (latest.correct ? '最近答对' : '最近答错') : '',
    tone: latest?.correct === true ? 'success' : 'muted',
    corrected,
  };
}

export function evidenceRecords(data) {
  return {
    words: (data?.words ?? []).map(wordRecord),
    expressions: conversationRecords(data?.expressions ?? []),
    writing: (data?.writing ?? []).map(item => ({
      ...item,
      title: item.title || '我的英文习作',
      subtitle: item.after,
      date: item.createdAt,
      status: item.previousId ? '修订稿' : '原稿',
    })),
    reading: (data?.readings ?? []).map(item => ({
      ...item,
      id: item.articleId,
      title: item.title,
      subtitle: item.sourceName || item.summary,
      date: item.finishedAt || item.updatedAt,
      status: item.finished ? '已读完' : (item.readProgress || 0) + '%',
      tone: item.finished ? 'success' : 'muted',
    })),
  };
}
