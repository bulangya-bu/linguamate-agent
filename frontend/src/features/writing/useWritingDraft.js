import { computed, onBeforeUnmount, onMounted, reactive, shallowRef, watch } from 'vue';

const emptyDraft = { title: '', examType: 'general', targetLevel: 'B1', essayText: '' };

export function useWritingDraft(userId) {
  const storageKey = `linguamate-writing-${userId}`;
  const draft = reactive({ ...emptyDraft });
  const baseline = shallowRef({ ...emptyDraft });
  const result = shallowRef(null);
  const revisionOfId = shallowRef(null);
  const saveStatus = shallowRef('');
  const snapshot = computed(() => baseline.value.essayText);
  const hasEdits = computed(() => draft.essayText.trim() !== snapshot.value.trim());
  const hasUnsubmittedWork = computed(() =>
    Object.keys(emptyDraft).some(key => draft[key] !== baseline.value[key]),
  );
  let saveTimer;

  try {
    const saved = JSON.parse(localStorage.getItem(storageKey) || 'null');
    if (saved?.draft) {
      Object.assign(draft, saved.draft);
      result.value = saved.result ?? null;
      revisionOfId.value = saved.revisionOfId ?? null;
      baseline.value = saved.baseline ?? (saved.snapshot
        ? { ...draft, essayText: saved.snapshot }
        : { ...emptyDraft });
      saveStatus.value = '已保存在本机';
    }
  } catch {
    saveStatus.value = '草稿读取失败';
  }

  function saveDraft() {
    clearTimeout(saveTimer);
    saveTimer = undefined;
    try {
      localStorage.setItem(storageKey, JSON.stringify({
        draft: { ...draft },
        baseline: baseline.value,
        result: result.value,
        revisionOfId: revisionOfId.value,
      }));
      saveStatus.value = '已保存在本机';
    } catch {
      saveStatus.value = '草稿保存失败';
    }
  }

  function flushDraft() {
    if (saveTimer !== undefined) saveDraft();
  }

  function setDocument(document = {}, essayId = null) {
    Object.assign(draft, emptyDraft, document);
    baseline.value = { ...draft };
    revisionOfId.value = essayId;
    result.value = null;
    saveDraft();
  }

  function recordReview(document, review) {
    baseline.value = document;
    result.value = review;
    revisionOfId.value = review.essayId;
    saveDraft();
  }

  watch([draft, baseline, result, revisionOfId], () => {
    clearTimeout(saveTimer);
    saveTimer = setTimeout(saveDraft, 400);
  }, { flush: 'sync' });
  onMounted(() => window.addEventListener('pagehide', flushDraft));
  onBeforeUnmount(() => {
    window.removeEventListener('pagehide', flushDraft);
    flushDraft();
  });

  return { draft, snapshot, result, revisionOfId, saveStatus, hasEdits, hasUnsubmittedWork, setDocument, recordReview };
}
