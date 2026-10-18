<template>
  <div class="conversation-studio">
    <header class="studio-chat-toolbar">
      <div class="studio-mode-switch" aria-label="对话模式">
        <button v-for="item in modes" :key="item.id" type="button" :class="{ selected: mode === item.id }"
          :aria-pressed="mode === item.id" :disabled="busy" @click="$emit('mode', item.id)">
          <AppIcon :name="item.icon" />{{ item.label }}
        </button>
      </div>
      <div class="studio-history-controls">
        <el-select :model-value="conversationId ? `${conversationId}` : 'new'" aria-label="历史会话" placeholder="历史会话"
          class="studio-history" filterable :disabled="busy" popper-class="chat-select-popper" @change="$emit('history', $event)">
          <el-option value="new" label="新对话" />
          <el-option v-for="conversation in conversations" :key="conversation.id" :value="`${conversation.id}`" :label="conversation.title || '学习对话'" />
        </el-select>
        <button type="button" class="studio-new" aria-label="新对话" :disabled="busy" @click="$emit('new')"><span aria-hidden="true">＋</span>新对话</button>
      </div>
    </header>

    <div class="studio-chat-columns">
      <section class="studio-dialogue" aria-label="英语对话">
        <header class="studio-dialogue-heading">
          <div><span class="studio-live-dot" :class="{ connected: !busy && (mode === 'practice' ? latestFeedback?.source === 'model' : assistantUsesModel) }"></span>
            <strong>{{ mode === 'practice' ? currentScene.label : '你的学习助手' }}</strong>
            <span>{{ mode === 'practice' ? 'CONVERSATION LAB' : 'LEARNING ASSISTANT' }}</span>
          </div>
          <span class="studio-turn-count">{{ userTurns }} 轮交流</span>
        </header>

        <div ref="thread" class="studio-thread" role="log" aria-label="对话记录" aria-live="polite" :aria-busy="busy">
          <div v-if="!messages.length" class="studio-chat-welcome">
            <div class="studio-welcome-art"><ConversationArt :scene="scene" :mode="mode" :disabled="busy" @suggest="suggestFromArt" /></div>
            <span class="studio-welcome-kicker">{{ mode === 'practice' ? '从一句话，走进真实情境' : '让学习记录，变成下一步行动' }}</span>
            <h2 v-if="mode === 'practice'">把英语，<br />聊成<span>自己的。</span></h2>
            <h2 v-else>今天的学习，<br /><span>一起安排。</span></h2>
            <p>{{ mode === 'practice' ? '你负责表达，AI 陪你接话、推敲，再往前一步。' : '查词、回顾薄弱项，或从今天尚未完成的任务开始。' }}</p>
            <div v-if="mode === 'practice'" class="studio-scene-choices" aria-label="练习场景">
              <button v-for="item in scenes" :key="item.id" type="button" :class="{ selected: scene === item.id }"
                @click="$emit('scene', item.id)"><span>{{ item.number }}</span><strong>{{ item.label }}</strong><small>{{ item.hint }}</small><AppIcon name="arrow" /></button>
            </div>
            <div v-else class="studio-assistant-prompts">
              <button v-for="prompt in assistantPrompts" :key="prompt" type="button" @click="submitPrompt(prompt)">{{ prompt }}<AppIcon name="arrow" /></button>
            </div>
            <button v-if="mode === 'practice'" class="studio-start-practice" type="button" @click="submitPrompt(currentScene.prompt)">
              从这个场景开始<AppIcon name="arrow" />
            </button>
          </div>

          <article v-for="(message, index) in messages" :key="message.id || index" class="studio-message" :class="message.role">
            <div class="studio-message-avatar"><AppIcon v-if="message.role !== 'user'" name="spark" /><span v-else>我</span></div>
            <div class="studio-message-body">
              <div class="studio-message-byline"><strong>{{ message.role === 'user' ? '你' : 'LinguaMate' }}</strong>
                <span v-if="message.coaching">{{ message.coaching.source === 'model' ? 'AI 对话教练' : '本地场景练习' }}</span>
                <span v-else-if="message.role === 'assistant' && message.trace?.some(step => step.title === '调用在线模型')">AI 学习助手</span>
                <span v-else-if="message.role === 'assistant' && message.trace?.some(step => step.title === '使用本地兜底')">本地学习建议</span>
              </div>
              <div v-if="message.role === 'user' && message.readingArticle" class="studio-reading-attachment"><AppIcon name="news" /><span>阅读讨论<strong>{{ message.readingArticle.title }}</strong></span></div>
              <p><template v-for="(part, partIndex) in messageTextParts(message)" :key="partIndex"><strong v-if="part.emphasis">{{ part.text }}</strong><template v-else>{{ part.text }}</template></template></p>
              <slot name="message-extra" :message="message" :index="index" />
              <button v-if="message.coaching" class="studio-feedback-link" :class="{ selected: selectedIndex === index }" type="button"
                :aria-pressed="selectedIndex === index" @click="selectedIndex = index"><AppIcon name="spark" />查看本轮反馈<AppIcon name="arrow" /></button>
            </div>
          </article>
          <div v-if="busy" class="studio-thinking" role="status"><span><i></i><i></i><i></i></span>{{ elapsed >= 15 ? '仍在等待模型回复…' : '正在结合上下文准备回复…' }}<small class="studio-wait-time">{{ elapsed }} 秒</small></div>
        </div>

        <form class="studio-composer" @submit.prevent="$emit('send')">
          <p v-if="error" class="studio-chat-error" role="alert">{{ error }}</p>
          <div v-if="messages.length && mode === 'practice'" class="studio-current-scene"><AppIcon name="chat" />{{ currentScene.label }}<span>新场景可以从「新对话」开始</span></div>
          <div class="studio-composer-box">
            <textarea ref="input" :value="draft" maxlength="2000" rows="2" aria-label="对话内容" :disabled="busy"
              :placeholder="mode === 'practice' ? '用英语说说你的想法，也可以用中文请求提示…' : '今天想解决什么学习问题？'"
              @input="$emit('update:draft', $event.target.value)" @keydown="handleKeydown"></textarea>
            <button type="submit" :disabled="busy || !draft.trim()" :aria-label="busy ? '正在回复' : '发送消息'"><AppIcon :name="busy ? 'ai' : 'arrow'" /><span>{{ busy ? '回复中' : '发送' }}</span></button>
          </div>
          <div class="studio-composer-footer"><span>Enter 发送 · Shift + Enter 换行</span><span>{{ draft.length }} / 2000</span></div>
        </form>
      </section>

      <aside class="studio-coach" aria-label="AI 教练反馈">
        <header class="studio-coach-heading"><span class="studio-coach-mark"><AppIcon name="spark" /></span><div><h2>{{ mode === 'practice' ? '你的 AI 教练' : '学习连接站' }}</h2><p>{{ mode === 'practice' ? '每一轮，都有一点进步' : '从对话走向实际练习' }}</p></div><span class="studio-coach-number">{{ String(userTurns).padStart(2, '0') }}</span></header>
        <div class="studio-model-line"><span :class="{ ready: !busy && (mode === 'practice' ? feedback?.source === 'model' : assistantUsesModel) }"></span><b>{{ mode === 'practice' ? feedback?.modelName || modelName || '等待配置模型' : modelName || '本地学习工具' }}</b><small>{{ mode === 'assistant' ? assistantStatus : feedback ? feedback.source === 'model' ? '本轮已调用' : '本轮本地练习' : '练习时连接' }}</small></div>
        <div class="studio-coach-scroll">
          <template v-if="mode === 'practice'">
            <div v-if="busy" class="studio-feedback-pending"><AppIcon name="ai" /><p>正在准备本轮反馈</p><small>完成后会和回复一起呈现</small></div>
            <Transition name="coach-reveal" mode="out-in">
              <div v-if="feedback && !busy" :key="selectedIndex" class="studio-feedback-content">
                <div v-if="feedback.source === 'local'" class="studio-local-notice"><AppIcon name="settings" /><p>{{ feedback.notice }}</p><button type="button" @click="$emit('settings', $event.currentTarget)">查看模型配置<AppIcon name="arrow" /></button></div>
                <section v-if="feedback.source === 'model'" class="studio-coach-section">
                  <h3><span>01</span>本轮表达<small>{{ feedback.revisionType === 'correction' ? '语法修正' : feedback.revisionType === 'upgrade' ? '表达提升' : '学习建议' }}</small></h3>
                  <template v-if="feedback.improved"><p class="studio-original">{{ feedback.original }}</p><div class="studio-revision"><AppIcon name="arrow" /><p>{{ feedback.improved }}</p></div></template>
                  <p v-if="feedback.explanation" class="studio-explanation">{{ feedback.explanation }}</p>
                  <p v-else-if="!feedback.improved" class="studio-explanation">这一轮没有提供表达修改，可以接着聊下去。</p>
                </section>
                <section v-if="feedback.phrases?.length" class="studio-coach-section">
                  <h3><span>02</span>带走一句</h3>
                  <div v-for="phrase in feedback.phrases" :key="phrase.text" class="studio-phrase"><strong>{{ phrase.text }}</strong><p>{{ phrase.meaningCn }}</p></div>
                </section>
                <section v-if="feedback.nextChallenge" class="studio-coach-section studio-next-challenge"><h3><AppIcon name="arrow" />下一轮挑战</h3><p>{{ feedback.nextChallenge }}</p><button type="button" @click="focusInput">试着回答<AppIcon name="arrow" /></button></section>
              </div>
              <div v-else-if="!busy" key="empty" class="studio-coach-intro"><span class="studio-coach-orbit" aria-hidden="true"><AppIcon name="spark" /><i></i></span><h3>让每次开口，<br />留下真正的收获。</h3><ol><li><b>01</b><span>进入情境<strong>围绕你的回应继续聊</strong></span></li><li><b>02</b><span>推敲表达<strong>区分语法错误与风格提升</strong></span></li><li><b>03</b><span>再试一次<strong>把新表达带进下一轮</strong></span></li></ol></div>
            </Transition>
            <section class="studio-targets"><h3><AppIcon name="review" />把薄弱词用起来<span>{{ targetWords.length }}</span></h3><p v-if="!targetWords.length">完成单词练习后，AI 会在这里带入你的薄弱词。</p>
              <div v-else class="studio-target-list"><span v-for="word in targetWords" :key="word.word" :class="{ used: word.used }" :title="word.meaningCn"><AppIcon v-if="word.used" name="check" />{{ word.word }}<small>{{ word.meaningCn }}</small></span></div>
              <small v-if="targetWords.some(word => word.used)">已在本轮用到 · 掌握情况仍以复习为准</small>
              <button v-if="targetWords.length" type="button" @click="$emit('review')">回到词库复习<AppIcon name="arrow" /></button>
            </section>
          </template>
          <div v-else class="studio-assistant-guide"><AppIcon name="wordbank" /><h3>你的学习，彼此相连。</h3><p>助手会读取真实学习记录，为你找到今天值得继续的一步。</p><button v-for="prompt in assistantPrompts" :key="prompt" type="button" :disabled="busy" @click="submitPrompt(prompt)">{{ prompt }}<AppIcon name="arrow" /></button><button class="studio-back-practice" type="button" :disabled="busy" @click="$emit('mode', 'practice')">带着薄弱词，开始对话<AppIcon name="chat" /></button></div>
          <details class="studio-process"><summary><AppIcon name="ai" />本轮处理记录<span>{{ displayedTrace.length ? `${displayedTrace.length} 步` : '等待练习' }}</span></summary><ol v-if="displayedTrace.length"><li v-for="(step,index) in displayedTrace" :key="index"><span>{{ step.phase }}</span><strong>{{ step.title }}</strong><p v-if="step.phase !== 'Observe' && step.phase !== 'Answer'">{{ step.detail }}</p></li></ol><p v-else>完成对话后显示实际执行的步骤。</p></details>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue';
import AppIcon from './AppIcon.vue';
import ConversationArt from './ConversationArt.vue';
const props = defineProps({ messages: { type: Array, default: () => [] }, conversations: { type: Array, default: () => [] }, conversationId: [Number, String],
  scene: { type: String, default: 'daily' }, mode: { type: String, default: 'practice' }, draft: { type: String, default: '' }, busy: Boolean,
  error: String, modelName: String, contextWords: { type: Array, default: () => [] }, trace: { type: Array, default: () => [] } });
const emit = defineEmits(['mode', 'scene', 'history', 'new', 'send', 'update:draft', 'prompt', 'settings', 'review']);
const modes = [{ id: 'practice', label: '对话练习', icon: 'chat' }, { id: 'assistant', label: '学习助手', icon: 'spark' }];
const scenes = [
  { id: 'daily', number: '01', label: '日常交流', hint: '分享生活与想法', prompt: '请和我练习日常英语，从今天的一件小事开始。' },
  { id: 'exam', number: '02', label: '考试口语', hint: '观点与具体例子', prompt: '请作为英语口语考官，先问我一个观点类问题。' },
  { id: 'business', number: '03', label: '商务表达', hint: '清楚表达与协作', prompt: '请模拟同事，用英语和我聊一个项目，先问我一个问题。' },
  { id: 'reading', number: '04', label: '阅读讨论', hint: '从阅读走向思考', prompt: '请和我用英语讨论阅读，先问我最近读过的内容。' },
];
const assistantPrompts = ['今天还有什么没学？', '不熟悉的单词有哪些？', '推荐一篇英语阅读'];
const currentScene = computed(() => scenes.find(item => item.id === props.scene) || scenes[0]);
const userTurns = computed(() => props.messages.filter(item => item.role === 'user').length);
const selectedIndex = ref(-1);
const latestFeedback = computed(() => [...props.messages].reverse().find(item => item.coaching)?.coaching);
const latestAssistant = computed(() => props.messages.findLast(item => item.role === 'assistant'));
const assistantUsesModel = computed(() => !latestAssistant.value?.coaching && latestAssistant.value?.trace?.some(step => step.title === '调用在线模型'));
const assistantStatus = computed(() => props.busy ? '等待本轮回复' : assistantUsesModel.value ? '本轮已调用'
  : latestAssistant.value?.trace?.some(step => step.title === '使用本地兜底') ? '本轮本地建议' : '问答时连接');
const feedback = computed(() => props.messages[selectedIndex.value]?.coaching);
const targetWords = computed(() => feedback.value?.targetWords || props.contextWords);
const displayedTrace = computed(() => props.mode === 'practice' ? props.messages[selectedIndex.value]?.trace || props.trace : props.trace);
const thread = ref(null);
const input = ref(null);
const elapsed = ref(0);
let waitTimer;
watch(() => props.busy, busy => { clearInterval(waitTimer); if (busy) { elapsed.value=0; waitTimer=setInterval(()=>elapsed.value++,1000); } });
onBeforeUnmount(()=>clearInterval(waitTimer));
function focusInput() { input.value?.focus(); }
function messageTextParts(message) {
  if (message.role === 'user') return [{ text: message.content, emphasis: false }];
  // Interpolate text nodes rather than HTML, including when a model emits Markdown emphasis.
  return String(message.content || '').split(/\*\*([^*\n]+)\*\*/g)
    .map((text, index) => ({ text, emphasis: index % 2 === 1 }));
}
const lastArtPrompt=ref('');
async function suggestFromArt(prompt) { if(props.busy)return;const existing=props.draft;const next=!existing.trim() ? prompt : lastArtPrompt.value && existing.endsWith(lastArtPrompt.value) ? existing.slice(0,-lastArtPrompt.value.length)+prompt : `${existing}\n${prompt}`;emit('update:draft',next.slice(0,2000));lastArtPrompt.value=prompt;await nextTick();focusInput(); }
function submitPrompt(prompt) { if (!props.busy) emit('prompt', prompt); }
function handleKeydown(event) { if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) { event.preventDefault(); if (!props.busy && props.draft.trim()) emit('send'); } }
watch(() => [props.messages.length, props.conversationId, props.busy], async () => {
  selectedIndex.value = props.messages.findLastIndex(item => item.coaching);
  await nextTick();
  if (thread.value) thread.value.scrollTo({ top: thread.value.scrollHeight, behavior: matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth' });
}, { immediate: true });
watch(() => props.busy, async (busy, wasBusy) => {
  if (wasBusy && !busy) { await nextTick(); focusInput(); }
});
</script>
