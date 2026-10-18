<template>
  <div class="writing-studio">
    <section class="ws-manuscript" aria-label="写作编辑区">
      <header class="ws-heading"><span class="ws-kicker">THE WRITING DESK</span><button type="button" :disabled="busy" @click="openHistory"><AppIcon name="review" />写作记录</button></header>
      <div class="ws-document-title"><input v-model="draft.title" maxlength="120" aria-label="作文标题" placeholder="给这段想法起个标题" :disabled="busy" /><span>{{ result && !hasEdits ? '已批改' : revisionOfId ? '改写稿' : '初稿' }}</span></div>
      <div class="ws-controls">
        <label>类型<select v-model="draft.examType" :disabled="busy"><option value="general">通用写作</option><option value="cet4">四级作文</option><option value="cet6">六级作文</option><option value="ielts">雅思写作</option></select></label>
        <label>目标<select v-model="draft.targetLevel" :disabled="busy"><option v-for="level in ['A1','A2','B1','B2','C1','C2']" :key="level">{{ level }}</option></select></label>
        <span class="ws-save-state" role="status">{{ saveStatus }}</span>
      </div>
      <nav v-if="result" class="ws-view-switch" aria-label="作文查看方式"><button v-for="tab in tabs" :key="tab.id" type="button" :class="{ active: view === tab.id }" @click="view = tab.id">{{ tab.label }}</button></nav>
      <div class="ws-paper">
        <textarea v-if="view === 'edit' || !result" ref="editor" v-model="draft.essayText" :disabled="busy" maxlength="8000" spellcheck="true" aria-label="英文作文内容" :placeholder="'Start with one thought.\n在这里写作，或粘贴你的英文初稿。'"></textarea>
        <div v-else-if="view === 'annotate'" ref="annotatedArea" class="ws-annotated" aria-label="原文与批注"><template v-for="(part,index) in annotations" :key="index"><button v-if="part.index !== undefined" type="button" :data-feedback-index="part.index" :class="{ selected: selected === part.index }" @click="selectFeedback(part.index)">{{ part.text }}</button><span v-else>{{ part.text }}</span></template></div>
        <div v-else class="ws-diff" aria-label="润色修改对照"><p class="ws-diff-legend"><span>绿色：新增</span><span>划线：删改</span></p><template v-for="(part,index) in diff" :key="index"><ins v-if="part.type === 'insert'">{{ part.text }}</ins><del v-else-if="part.type === 'delete'">{{ part.text }}</del><span v-else>{{ part.text }}</span></template></div>
      </div>
      <div v-if="!draft.essayText && !result" class="ws-prompts"><span>从一个话题开始</span><button v-for="topic in topics" :key="topic.title" type="button" @click="chooseTopic(topic)">{{ topic.label }}<AppIcon name="arrow" /></button></div>
      <p v-if="topicHint && !result" class="ws-topic-hint">{{ topicHint }}</p>
      <footer class="ws-submit-row"><span>{{ count }}<small> words</small><small v-if="revisionOfId && hasEdits"> · 改写稿</small></span><button class="quiz-primary" type="button" :disabled="busy || !draft.essayText.trim() || (revisionOfId && !hasEdits)" @click="submit"><AppIcon :name="busy ? 'ai' : 'spark'" />{{ busy ? `批改中 · ${elapsed}s` : revisionOfId ? hasEdits ? '提交改写 · 再评一次' : '编辑后再次批改' : '提交 AI 批改' }}</button></footer>
      <p v-if="error" class="ws-error" role="alert">{{ error }} <button type="button" @click="submit">重试</button></p>
    </section>

    <aside class="ws-inspector" aria-label="写作反馈">
      <header class="ws-inspector-heading"><h2><AppIcon name="spark" />{{ historyOpen ? '写作记录' : '表达实验室' }}</h2><button v-if="historyOpen" type="button" @click="historyOpen = false">返回反馈</button><span v-else>{{ result ? 'REVIEW' : 'WRITE · REFINE · RETRY' }}</span></header>
      <div class="ws-inspector-scroll">
        <template v-if="historyOpen"><p v-if="historyBusy" role="status">正在读取记录…</p><p v-else-if="!history.length" class="ws-note">完成第一次批改后，记录会出现在这里。</p><button v-for="item in history" :key="item.id" class="ws-history-item" type="button" @click="loadHistory(item)"><span><strong>{{ item.title }}</strong><small>{{ String(item.createdAt || '').slice(0,10) }} · {{ item.reviewSource || '历史记录' }}</small></span><b>{{ item.totalScore }}<small>分</small></b><AppIcon name="arrow" /></button></template>
        <div v-else-if="busy" class="ws-await" role="status"><span class="ws-await-orbit"><AppIcon name="writing" /></span><h3>正在推敲你的表达</h3><p>语法、措辞、结构与逻辑将一并呈现。</p><span>{{ elapsed }} 秒</span><small v-if="elapsed >= 15">仍在等待模型回复，请保留当前页面。</small></div>
        <template v-else-if="result">
          <div class="ws-score-summary"><div class="ws-total"><strong>{{ result.totalScore }}</strong><span>/ 100</span></div><div><span class="ws-source">{{ result.reviewSource }} · {{ result.modelName }}</span><p>{{ result.summary }}</p></div></div>
          <p v-if="result.reviewSource !== 'AI批改'" class="ws-local-note">本轮使用本地规则批改，评分仅作练习参考。<button type="button" @click="$emit('settings')">检查模型配置</button></p>
          <div class="ws-dimensions"><div v-for="metric in metrics" :key="metric.key"><span>{{ metric.label }}<b>{{ result[metric.key] }}</b></span><i><em :style="{ width: `${result[metric.key]}%` }"></em></i></div></div>
          <div v-if="result.comparison" class="ws-comparison"><span>上次 {{ result.comparison.previousScore }} → 本次 {{ result.comparison.currentScore }}</span><strong v-if="result.comparison.comparable">{{ scoreDelta > 0 ? '+' : '' }}{{ scoreDelta }} 分</strong><small>{{ result.comparison.note }}</small></div>
          <div class="ws-feedback-title"><h3>逐句推敲</h3><span>{{ result.feedback?.length || 0 }} 条建议</span></div>
          <p v-if="!result.feedback?.length" class="ws-note">本轮没有返回逐句建议，可以查看润色对照。</p>
          <article v-for="(item,index) in result.feedback || []" :key="index" :ref="element => feedbackElements[index] = element" class="ws-feedback" :class="{ selected: selected === index }">
            <button class="ws-feedback-select" type="button" @click="selectAnnotation(index)"><span>{{ String(index+1).padStart(2,'0') }}</span><strong>{{ categoryLabel(item.category) }}</strong><AppIcon name="arrow" /></button>
            <p class="ws-original">{{ item.originalSentence }}</p><p v-if="item.correctedSentence" class="ws-corrected">{{ item.correctedSentence }}</p><p class="ws-reason">{{ item.suggestion }}</p>
          </article>
          <div class="ws-retry-block"><span>把建议写成自己的表达</span><p>保留你的思路，试着重新组织这一段。</p><button type="button" :disabled="busy" @click="startRevision">自己改写一次<AppIcon name="arrow" /></button></div>
          <button class="ws-new-document" type="button" @click="newDocument">开始新作文</button>
        </template>
        <div v-else class="ws-intro"><div class="ws-intro-art"><WritingArt /></div><span class="ws-kicker">MAKE IT YOURS</span><h3>好表达，<br />从再写一次开始。</h3><ol><li><b>01</b><span>写下想法<small>保持你的观点与声音</small></span></li><li><b>02</b><span>看见修改<small>原句、理由与润色逐一对照</small></span></li><li><b>03</b><span>自己再写<small>留下两次表达的变化</small></span></li></ol></div>
      </div>
    </aside>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue';
import AppIcon from './AppIcon.vue';
import WritingArt from './WritingArt.vue';
import { learningApi } from '@/api/modules';
import { annotatedText, revisionDiff, wordCount } from '@/writing-workspace';
const props = defineProps({ userId: { type: [Number,String], required: true } });
const emit = defineEmits(['reviewed','settings']);
const draft = reactive({ title: '', examType: 'general', targetLevel: 'B1', essayText: '' });
const result = ref(null), snapshot = ref(''), revisionOfId = ref(null), selected = ref(0), view = ref('edit');
const busy = ref(false), error = ref(''), elapsed = ref(0), saveStatus = ref('草稿保存在本机'), editor = ref(null), feedbackElements = ref([]);
const historyOpen = ref(false), historyBusy = ref(false), history = ref([]), topicHint = ref('');
const annotatedArea = ref(null);
const tabs = [{ id:'edit', label:'编辑草稿' },{ id:'annotate', label:'原文批注' },{ id:'compare', label:'润色对照' }];
const topics = [{ label:'一次小改变', title:'A Small Change', hint:'Describe a small change that made your daily life better. What changed, and why did it matter?' },{ label:'科技与生活', title:'Technology in Everyday Life', hint:'Does technology bring people closer together? Give a specific example to support your view.' },{ label:'学习的意义', title:'Learning Beyond the Classroom', hint:'Describe something you learned outside the classroom and explain how you use it.' }];
const metrics = [{key:'grammarScore',label:'语法'},{key:'vocabularyScore',label:'词汇'},{key:'structureScore',label:'结构'},{key:'logicScore',label:'逻辑'}];
const count = computed(() => wordCount(draft.essayText));
const hasEdits = computed(() => draft.essayText.trim() !== snapshot.value.trim());
const annotations = computed(() => annotatedText(snapshot.value,result.value?.feedback));
const diff = computed(() => revisionDiff(snapshot.value,result.value?.polishedText));
const scoreDelta = computed(() => Math.round((result.value.comparison.currentScore-result.value.comparison.previousScore)*10)/10);
const storageKey = computed(() => `linguamate-writing-${props.userId}`);
let saveTimer, waitTimer;
try {
  const saved = JSON.parse(localStorage.getItem(storageKey.value) || 'null');
  if (saved?.draft) { Object.assign(draft,saved.draft); result.value=saved.result; snapshot.value=saved.snapshot || ''; revisionOfId.value=saved.revisionOfId; saveStatus.value='已恢复本机草稿'; }
} catch { saveStatus.value='草稿暂未恢复'; }
function saveDraft() {
  try { localStorage.setItem(storageKey.value,JSON.stringify({draft:{...draft},result:result.value,snapshot:snapshot.value,revisionOfId:revisionOfId.value})); saveStatus.value='草稿已保存到本机'; }
  catch { saveStatus.value='无法保存草稿，请保留正文'; }
}
watch([() => ({...draft}),result,revisionOfId],() => { clearTimeout(saveTimer); saveTimer=setTimeout(saveDraft,400); },{deep:true});
function categoryLabel(category) { return ({grammar:'语法修正',vocabulary:'词汇提升',structure:'结构调整',logic:'逻辑衔接',expression:'表达提升'})[category] || category || '表达建议'; }
function chooseTopic(topic) { draft.title=topic.title; topicHint.value=topic.hint; editor.value?.focus(); }
async function selectFeedback(index) { selected.value=index; await nextTick(); feedbackElements.value[index]?.scrollIntoView({block:'nearest',behavior:matchMedia('(prefers-reduced-motion: reduce)').matches?'instant':'smooth'}); }
async function selectAnnotation(index) { selected.value=index; view.value='annotate'; await nextTick(); annotatedArea.value?.querySelector(`[data-feedback-index="${index}"]`)?.scrollIntoView({block:'nearest',behavior:matchMedia('(prefers-reduced-motion: reduce)').matches?'instant':'smooth'}); }
async function submit() {
  if (busy.value || !draft.essayText.trim() || (revisionOfId.value && !hasEdits.value)) return;
  busy.value=true; error.value=''; elapsed.value=0; historyOpen.value=false;
  waitTimer=setInterval(()=>elapsed.value++,1000);
  const submitted = {...draft};
  try { const response=await learningApi.writingReview({...submitted,revisionOfId:revisionOfId.value}); snapshot.value=submitted.essayText; result.value=response; view.value='annotate'; selected.value=0; revisionOfId.value=response.essayId; saveDraft(); emit('reviewed',response); }
  catch (exception) { error.value=exception.message || '批改失败，草稿已保留，请重试。'; }
  finally { busy.value=false; clearInterval(waitTimer); }
}
async function startRevision() { revisionOfId.value=result.value.essayId; view.value='edit'; await nextTick(); editor.value?.focus(); }
function newDocument() { Object.assign(draft,{title:'',examType:'general',targetLevel:'B1',essayText:''}); result.value=null; snapshot.value=''; revisionOfId.value=null; view.value='edit'; topicHint.value=''; error.value=''; }
async function openHistory() { historyOpen.value=true; historyBusy.value=true; try { history.value=await learningApi.writingHistory(); } catch(exception) { error.value=exception.message; } finally { historyBusy.value=false; } }
function loadHistory(item) { Object.assign(draft,{title:item.title,examType:item.examType || 'general',targetLevel:item.targetLevel || 'B1',essayText:item.originalText}); result.value=null; snapshot.value=item.originalText || ''; revisionOfId.value=item.id; historyOpen.value=false; view.value='edit'; }
onBeforeUnmount(()=> { clearTimeout(saveTimer); clearInterval(waitTimer); saveDraft(); });
</script>
