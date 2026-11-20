<script setup>
import { computed, nextTick, onBeforeUnmount, shallowRef, useTemplateRef } from 'vue';
import { ElMessageBox } from 'element-plus';
import AppIcon from './AppIcon.vue';
import { learningApi } from '@/api/modules';
import WritingFeedback from '@/features/writing/WritingFeedback.vue';
import { useWritingDraft } from '@/features/writing/useWritingDraft';
import { annotatedText, editableSentenceRanges, revisionDiff, wordCount } from '@/writing-workspace';

const props = defineProps({ userId: { type: [Number, String], required: true } });
const emit = defineEmits(['reviewed', 'settings']);
const { draft, snapshot, result, revisionOfId, saveStatus, hasEdits, hasUnsubmittedWork, setDocument, recordReview } =
  useWritingDraft(props.userId);
const editor = useTemplateRef('editor');
const annotatedArea = useTemplateRef('annotatedArea');
const inspector = useTemplateRef('inspector');
const historyTitle = useTemplateRef('historyTitle');
const historyToggle = useTemplateRef('historyToggle');
const feedbackToggle = useTemplateRef('feedbackToggle');
const mobilePane = shallowRef('manuscript');
const view = shallowRef('edit');
const activeFeedback = shallowRef(0);
const topicHint = shallowRef('');
const busy = shallowRef(false);
const error = shallowRef('');
const historyOpen = shallowRef(false);
const historyBusy = shallowRef(false);
const historyError = shallowRef('');
const history = shallowRef([]);
const historyRecord = shallowRef(null);
const historyFeedback = shallowRef(undefined);
const historyFeedbackError = shallowRef('');
let feedbackRequest;
let disposed = false;

const tabs = [
  { id: 'edit', label: '编辑' },
  { id: 'annotate', label: '批注' },
  { id: 'compare', label: '润色对照' },
];
const topics = [
  { label: '一次小改变', title: 'A Small Change', hint: 'Describe a small change that made your daily life better. What changed, and why did it matter?' },
  { label: '科技与生活', title: 'Technology in Everyday Life', hint: 'Does technology bring people closer together? Give a specific example to support your view.' },
  { label: '学习的意义', title: 'Learning Beyond the Classroom', hint: 'Describe something you learned outside the classroom and explain how you use it.' },
];
const count = computed(() => wordCount(draft.essayText));
const canSubmit = computed(() => !busy.value && draft.essayText.trim() && (!revisionOfId.value || hasEdits.value));
const annotations = computed(() => annotatedText(snapshot.value, result.value?.feedback));
const diff = computed(() => revisionDiff(snapshot.value, result.value?.polishedText));
const title = computed(() => historyOpen.value ? '写作记录' : '写作反馈');
const submitLabel = computed(() => busy.value ? '批改中…' : revisionOfId.value ? '复评' : '批改');
const savedReview = computed(() => ({ ...historyRecord.value, feedback: historyFeedback.value }));
const editRanges = computed(() => {
  const record = historyRecord.value;
  const source = record ? record.originalText : snapshot.value;
  const target = record && record.id !== revisionOfId.value ? source : draft.essayText;
  return editableSentenceRanges(source, target, record ? historyFeedback.value : result.value?.feedback);
});

function chooseTopic(topic) {
  draft.title = topic.title;
  topicHint.value = topic.hint;
  editor.value?.focus();
}

function scrollTo(element) {
  element?.scrollIntoView({
    block: 'nearest',
    behavior: matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth',
  });
}

function closeHistory() {
  historyOpen.value = false;
  feedbackRequest?.abort();
  historyRecord.value = null;
}

async function showFeedback() {
  mobilePane.value = 'feedback';
  await nextTick();
  historyTitle.value?.focus({ preventScroll: true });
}

async function backToManuscript() {
  mobilePane.value = 'manuscript';
  await nextTick();
  feedbackToggle.value?.focus({ preventScroll: true });
}

async function selectFeedback(index) {
  activeFeedback.value = index;
  mobilePane.value = 'feedback';
  closeHistory();
  await nextTick();
  scrollTo(inspector.value?.querySelector('[data-feedback-item="' + index + '"]'));
}

async function selectAnnotation(index) {
  mobilePane.value = 'manuscript';
  activeFeedback.value = index;
  view.value = 'annotate';
  await nextTick();
  scrollTo(annotatedArea.value?.querySelector('[data-feedback-index="' + index + '"]'));
}

async function submit() {
  if (!canSubmit.value) return;
  busy.value = true;
  error.value = '';
  closeHistory();
  const submitted = { ...draft };
  try {
    const response = await learningApi.writingReview({ ...submitted, revisionOfId: revisionOfId.value });
    if (!response || disposed) return;
    recordReview(submitted, response);
    const compact = matchMedia('(max-width: 760px), (max-width: 960px) and (max-height: 540px) and (pointer: coarse)').matches;
    view.value = compact ? 'edit' : 'annotate';
    if (compact) await showFeedback();
    activeFeedback.value = 0;
    emit('reviewed', response);
  } catch (exception) {
    error.value = exception.message || '批改失败，请重试';
  } finally {
    busy.value = false;
  }
}

async function confirmReplacement(action) {
  if (!hasUnsubmittedWork.value) return true;
  try {
    await ElMessageBox.confirm('当前未提交的内容会被清空。', '替换当前草稿？', {
      confirmButtonText: action,
      cancelButtonText: '保留草稿',
      closeOnClickModal: false,
      autofocus: false,
    });
    return true;
  } catch {
    return false;
  }
}

async function editDocument() {
  closeHistory();
  mobilePane.value = 'manuscript';
  error.value = '';
  view.value = 'edit';
  await nextTick();
  editor.value?.focus();
}

async function newDocument() {
  if (!(await confirmReplacement('舍弃并新建'))) return;
  setDocument();
  topicHint.value = '';
  await editDocument();
}

async function startRevision() {
  const previous = historyRecord.value;
  if (previous && previous.id !== revisionOfId.value) {
    if (!(await confirmReplacement('舍弃并改写'))) return false;
    setDocument({
      title: previous.title || '',
      examType: previous.examType || 'general',
      targetLevel: previous.targetLevel || 'B1',
      essayText: previous.originalText || '',
    }, previous.id);
    topicHint.value = '';
  }
  await editDocument();
  return true;
}

async function editSentence(index) {
  const range = editRanges.value[index];
  if (!range || busy.value || !(await startRevision()) || !editor.value) return;
  activeFeedback.value = index;
  editor.value.setSelectionRange(range.start, range.end);
}

async function openHistory() {
  historyOpen.value = true;
  mobilePane.value = 'feedback';
  await nextTick();
  historyTitle.value?.focus({ preventScroll: true });
  if (historyBusy.value) return;
  historyBusy.value = true;
  historyError.value = '';
  try {
    history.value = (await learningApi.writingHistory()) || [];
  } catch (exception) {
    historyError.value = exception.message || '无法读取写作记录';
  } finally {
    historyBusy.value = false;
  }
}

function toggleHistory() {
  if (historyOpen.value) closeHistory();
  else openHistory();
}

async function showHistoryRecord(record) {
  feedbackRequest?.abort();
  const request = new AbortController();
  feedbackRequest = request;
  historyRecord.value = record;
  historyFeedback.value = undefined;
  historyFeedbackError.value = '';
  await nextTick();
  if (request.signal.aborted) return;
  historyTitle.value?.focus();
  try {
    const feedback = await learningApi.writingHistoryFeedback(record.id, request.signal);
    if (!request.signal.aborted) historyFeedback.value = feedback || [];
  } catch (exception) {
    if (!request.signal.aborted) historyFeedbackError.value = exception.message || '无法读取批改反馈';
  }
}

async function backFromHistory() {
  if (historyRecord.value) {
    feedbackRequest?.abort();
    historyRecord.value = null;
    await nextTick();
    historyTitle.value?.focus();
  } else {
    closeHistory();
    mobilePane.value = 'manuscript';
    await nextTick();
    historyToggle.value?.focus();
  }
}

onBeforeUnmount(() => {
  disposed = true;
  feedbackRequest?.abort();
});
</script>

<template>
  <div class="writing-studio" :data-mobile-pane="mobilePane">
    <section class="ws-manuscript" aria-label="写作编辑区">
      <header class="ws-heading">
        <span class="ws-save-state" role="status">{{ saveStatus }}</span>
        <div class="ws-toolbar">
          <button v-if="result" ref="feedbackToggle" class="ws-text-action ws-mobile-action" type="button" @click="showFeedback">反馈</button>
          <button class="ws-text-action" type="button" :disabled="busy" aria-label="新建作文" @click="newDocument">新建</button>
          <button ref="historyToggle" class="ws-text-action" type="button" :disabled="busy"
            :aria-pressed="historyOpen" aria-label="写作记录" @click="toggleHistory">记录</button>
        </div>
      </header>
      <div class="ws-document-title">
        <input v-model="draft.title" maxlength="120" aria-label="作文标题" placeholder="作文标题" :disabled="busy" />
      </div>
      <div class="ws-controls">
        <label>类型
          <select v-model="draft.examType" :disabled="busy">
            <option value="general">通用写作</option><option value="cet4">四级作文</option>
            <option value="cet6">六级作文</option><option value="ielts">雅思作文</option>
          </select>
        </label>
        <label>目标
          <select v-model="draft.targetLevel" :disabled="busy">
            <option v-for="level in ['A1', 'A2', 'B1', 'B2', 'C1', 'C2']" :key="level">{{ level }}</option>
          </select>
        </label>
      </div>
      <div v-if="result" class="ws-view-switch" role="group" aria-label="作文查看方式">
        <button v-for="tab in tabs" :key="tab.id" type="button" :class="{ active: view === tab.id }"
          :aria-pressed="view === tab.id" @click="view = tab.id">{{ tab.label }}</button>
      </div>
      <div class="ws-paper">
        <textarea v-if="view === 'edit' || !result" ref="editor" v-model="draft.essayText" :disabled="busy"
          maxlength="8000" spellcheck="true" aria-label="英文作文内容" placeholder="输入英文正文…" />
        <div v-else-if="view === 'annotate'" ref="annotatedArea" class="ws-annotated" aria-label="原文与批注">
          <template v-for="(part, index) in annotations" :key="index">
            <button v-if="part.index !== undefined" type="button" :data-feedback-index="part.index"
              :class="{ selected: activeFeedback === part.index }" @click="selectFeedback(part.index)">{{ part.text }}</button>
            <span v-else>{{ part.text }}</span>
          </template>
        </div>
        <div v-else class="ws-diff" aria-label="润色修改对照">
          <p class="ws-diff-legend"><span>绿色新增</span><span>划线删改</span></p>
          <template v-for="(part, index) in diff" :key="index">
            <ins v-if="part.type === 'insert'">{{ part.text }}</ins><del v-else-if="part.type === 'delete'">{{ part.text }}</del><span v-else>{{ part.text }}</span>
          </template>
        </div>
      </div>
      <div v-if="!draft.essayText && !result" class="ws-prompts" aria-label="写作题目">
        <button v-for="topic in topics" :key="topic.title" type="button" @click="chooseTopic(topic)">{{ topic.label }}</button>
      </div>
      <p v-if="topicHint && !result" class="ws-topic-hint">{{ topicHint }}</p>
      <p v-if="error" class="ws-error" role="alert">{{ error }}</p>
      <footer class="ws-submit-row">
        <span>{{ count }} <small>词</small></span>
        <button class="ws-primary" type="button" :disabled="!canSubmit" @click="submit">{{ submitLabel }}</button>
      </footer>
    </section>

    <aside class="ws-inspector" :aria-label="title">
      <header class="ws-inspector-heading">
        <h2 ref="historyTitle" tabindex="-1">{{ title }}</h2>
        <button v-if="historyOpen" class="ws-text-action" type="button" @click="backFromHistory">返回</button>
        <button v-else class="ws-text-action ws-mobile-action" type="button" @click="backToManuscript">返回作文</button>
      </header>
      <div ref="inspector" class="ws-inspector-scroll">
        <template v-if="historyOpen && historyRecord">
          <div class="ws-history-document">
            <h3>{{ historyRecord.title }}</h3>
            <time v-if="historyRecord.createdAt" :datetime="historyRecord.createdAt">{{ historyRecord.createdAt.slice(0, 10) }}</time>
            <details v-if="historyRecord.originalText">
              <summary>原文</summary><p lang="en">{{ historyRecord.originalText }}</p>
            </details>
            <details v-if="historyRecord.polishedText">
              <summary>参考稿</summary><p lang="en">{{ historyRecord.polishedText }}</p>
            </details>
          </div>
          <WritingFeedback :review="savedReview" :edit-ranges="editRanges" @edit-sentence="editSentence" />
          <p v-if="historyFeedbackError" class="ws-error" role="alert">
            {{ historyFeedbackError }} <button class="ws-text-action" type="button" @click="showHistoryRecord(historyRecord)">重试</button>
          </p>
          <p v-else-if="historyFeedback === undefined" class="ws-note" role="status">正在读取反馈…</p>
          <p v-else-if="!historyFeedback.length" class="ws-note">暂无逐句建议</p>
        </template>
        <template v-else-if="historyOpen">
          <p v-if="historyBusy" class="ws-note" role="status">正在读取记录…</p>
          <p v-else-if="historyError" class="ws-error" role="alert">
            {{ historyError }} <button class="ws-text-action" type="button" @click="openHistory">重试</button>
          </p>
          <p v-else-if="!history.length" class="ws-note">暂无写作记录</p>
          <button v-for="item in history" v-else :key="item.id" class="ws-history-item" type="button" @click="showHistoryRecord(item)">
            <span><strong>{{ item.title }}</strong><time v-if="item.createdAt" :datetime="item.createdAt">{{ item.createdAt.slice(0, 10) }}</time></span>
            <b v-if="item.totalScore != null">{{ item.totalScore }}</b><AppIcon name="arrow" />
          </button>
        </template>
        <div v-else-if="busy" class="ws-intro" role="status">正在批改…</div>
        <WritingFeedback v-else-if="result" :review="result" :active-index="activeFeedback" link-to-text
          :edit-ranges="editRanges" @edit-sentence="editSentence" @select="selectAnnotation" @settings="emit('settings')" />
        <div v-else class="ws-intro">提交后查看反馈</div>
      </div>
      <footer v-if="historyRecord || (!historyOpen && result && !busy)" class="ws-inspector-footer">
        <button class="ws-primary" type="button" @click="startRevision">改写</button>
      </footer>
    </aside>
  </div>
</template>
