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
          </div>
          <span class="studio-turn-count">{{ userTurns }} 轮交流</span>
        </header>

        <div ref="thread" class="studio-thread" role="log" aria-label="对话记录" aria-live="polite" :aria-busy="busy">
          <div v-if="!messages.length" class="studio-chat-welcome">
            <div class="studio-welcome-art"><ConversationArt :scene="scene" :mode="mode" :disabled="busy" @suggest="suggestFromArt" /></div>
            <h2>{{ mode === 'practice' ? '选择练习场景' : '想聊什么？' }}</h2>
            <div v-if="mode === 'practice'" class="studio-scene-choices" aria-label="练习场景">
              <button v-for="item in scenes" :key="item.id" type="button" :class="{ selected: scene === item.id }"
                @click="$emit('scene', item.id)"><strong>{{ item.label }}</strong><AppIcon name="arrow" /></button>
            </div>
            <div v-else class="studio-assistant-prompts">
              <button v-for="prompt in assistantPrompts" :key="prompt" type="button" @click="submitPrompt(prompt)">{{ prompt }}<AppIcon name="arrow" /></button>
            </div>
            <button v-if="mode === 'practice'" class="studio-start-practice" type="button" @click="submitPrompt(currentScene.prompt)">
              开始练习<AppIcon name="arrow" />
            </button>
          </div>

          <article v-for="(message, index) in messages" :key="message.id || index" class="studio-message" :class="message.role">
            <div class="studio-message-avatar"><AppIcon v-if="message.role !== 'user'" name="spark" /><span v-else>我</span></div>
            <div class="studio-message-body">
              <div class="studio-message-byline"><strong>{{ message.role === 'user' ? '你' : BRAND_FULL_NAME }}</strong>
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
          <div v-if="busy" class="studio-thinking" role="status"><span><i></i><i></i><i></i></span>{{ elapsed >= 15 ? '仍在等待模型回复…' : '正在回复…' }}<small class="studio-wait-time">{{ elapsed }} 秒</small></div>
        </div>

        <form class="studio-composer" @submit.prevent="$emit('send')">
          <p v-if="error" class="studio-chat-error" role="alert">{{ error }}</p>
          <div class="studio-composer-box">
            <textarea ref="input" :value="draft" maxlength="2000" rows="2" aria-label="对话内容" :disabled="busy"
              :placeholder="mode === 'practice' ? '输入英语，或用中文请求提示…' : '今天想解决什么学习问题？'"
              @input="$emit('update:draft', $event.target.value)" @keydown="handleKeydown"></textarea>
            <button type="submit" :disabled="busy || !draft.trim()" :aria-label="busy ? '正在回复' : '发送消息'"><AppIcon :name="busy ? 'ai' : 'arrow'" /><span>{{ busy ? '回复中' : '发送' }}</span></button>
          </div>
          <div class="studio-composer-footer"><span>Enter 发送 · Shift + Enter 换行</span><span>{{ draft.length }} / 2000</span></div>
        </form>
      </section>

      <aside class="studio-coach" aria-label="AI 教练反馈">
        <header class="studio-coach-heading"><span class="studio-coach-mark"><AppIcon name="spark" /></span><div><h2>{{ mode === 'practice' ? '本轮反馈' : '学习建议' }}</h2></div></header>
        <div v-if="responseSource" class="studio-model-line"><span :class="{ ready: !busy && (mode === 'practice' ? feedback?.source === 'model' : assistantUsesModel) }"></span><b>{{ responseSource }}</b></div>
        <div class="studio-coach-scroll">
          <template v-if="mode === 'practice'">
            <div v-if="busy" class="studio-feedback-pending"><AppIcon name="ai" /><p>正在准备本轮反馈</p></div>
            <Transition name="coach-reveal" mode="out-in">
              <div v-if="feedback && !busy" :key="selectedIndex" class="studio-feedback-content">
                <div v-if="feedback.source === 'local'" class="studio-local-notice"><AppIcon name="settings" /><button type="button" @click="$emit('settings', $event.currentTarget)">查看模型配置<AppIcon name="arrow" /></button></div>
                <section v-if="feedback.source === 'model' && (feedback.improved || feedback.explanation)" class="studio-coach-section">
                  <h3>本轮表达<small>{{ feedback.revisionType === 'correction' ? '语法修正' : feedback.revisionType === 'upgrade' ? '表达提升' : '学习建议' }}</small></h3>
                  <template v-if="feedback.improved"><p class="studio-original">{{ feedback.original }}</p><div class="studio-revision"><AppIcon name="arrow" /><p>{{ feedback.improved }}</p></div></template>
                  <p v-if="feedback.explanation" class="studio-explanation">{{ feedback.explanation }}</p>
                </section>
                <section v-if="feedback.phrases?.length" class="studio-coach-section">
                  <h3>实用短语</h3>
                  <div v-for="phrase in feedback.phrases" :key="phrase.text" class="studio-phrase"><strong>{{ phrase.text }}</strong><p>{{ phrase.meaningCn }}</p></div>
                </section>
                <section v-if="feedback.nextChallenge" class="studio-coach-section studio-next-challenge"><h3><AppIcon name="arrow" />下一轮挑战</h3><p>{{ feedback.nextChallenge }}</p><button type="button" @click="focusInput">试着回答<AppIcon name="arrow" /></button></section>
              </div>
              <p v-else-if="!busy" key="empty" class="studio-feedback-empty">暂无对话反馈</p>
            </Transition>
            <section class="studio-targets"><h3><AppIcon name="review" />薄弱词<span>{{ targetWords.length }}</span></h3><p v-if="!targetWords.length">暂无薄弱词</p>
              <div v-else class="studio-target-list"><span v-for="word in targetWords" :key="word.word" :class="{ used: word.used }" :title="word.meaningCn"><AppIcon v-if="word.used" name="check" />{{ word.word }}<small>{{ word.meaningCn }}</small></span></div>
              <small v-if="targetWords.some(word => word.used)">✓ 本轮已使用</small>
              <button v-if="targetWords.length" type="button" @click="$emit('review')">回到词库复习<AppIcon name="arrow" /></button>
            </section>
          </template>
          <div v-else class="studio-assistant-guide"><template v-if="messages.length"><button v-for="prompt in assistantPrompts" :key="prompt" type="button" :disabled="busy" @click="submitPrompt(prompt)">{{ prompt }}<AppIcon name="arrow" /></button></template><button class="studio-back-practice" type="button" :disabled="busy" @click="$emit('mode', 'practice')">开始对话练习<AppIcon name="chat" /></button></div>
          <details v-if="displayedTrace.length" class="studio-process"><summary><AppIcon name="ai" />本轮处理记录<span>{{ displayedTrace.length }} 步</span></summary><ol><li v-for="(step,index) in displayedTrace" :key="index"><span>{{ step.phase }}</span><strong>{{ step.title }}</strong><p v-if="step.phase !== 'Observe' && step.phase !== 'Answer'">{{ step.detail }}</p></li></ol></details>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue';
import AppIcon from './AppIcon.vue';
import ConversationArt from './ConversationArt.vue';
import { BRAND_FULL_NAME } from '@/brand';
const props = defineProps({ messages: { type: Array, default: () => [] }, conversations: { type: Array, default: () => [] }, conversationId: [Number, String],
  scene: { type: String, default: 'daily' }, mode: { type: String, default: 'practice' }, draft: { type: String, default: '' }, busy: Boolean,
  error: String, modelName: String, contextWords: { type: Array, default: () => [] }, trace: { type: Array, default: () => [] } });
const emit = defineEmits(['mode', 'scene', 'history', 'new', 'send', 'update:draft', 'prompt', 'settings', 'review']);
const modes = [{ id: 'practice', label: '对话练习', icon: 'chat' }, { id: 'assistant', label: '学习助手', icon: 'spark' }];
const scenes = [
  { id: 'daily', label: '日常交流', prompt: '请和我练习日常英语，从今天的一件小事开始。' },
  { id: 'exam', label: '考试口语', prompt: '请作为英语口语考官，先问我一个观点类问题。' },
  { id: 'business', label: '商务表达', prompt: '请模拟同事，用英语和我聊一个项目，先问我一个问题。' },
  { id: 'reading', label: '阅读讨论', prompt: '请和我用英语讨论阅读，先问我最近读过的内容。' },
];
const assistantPrompts = ['今天还有什么没学？', '不熟悉的单词有哪些？', '推荐一篇英语阅读'];
const currentScene = computed(() => scenes.find(item => item.id === props.scene) || scenes[0]);
const userTurns = computed(() => props.messages.filter(item => item.role === 'user').length);
const selectedIndex = ref(-1);
const latestFeedback = computed(() => [...props.messages].reverse().find(item => item.coaching)?.coaching);
const latestAssistant = computed(() => props.messages.findLast(item => item.role === 'assistant'));
const assistantUsesModel = computed(() => !latestAssistant.value?.coaching && latestAssistant.value?.trace?.some(step => step.title === '调用在线模型'));
const feedback = computed(() => props.messages[selectedIndex.value]?.coaching);
const responseSource = computed(() => {
  if (props.mode === 'practice') {
    if (!feedback.value) return '';
    return feedback.value.source === 'model' ? feedback.value.modelName || 'AI 教练' : '本地练习';
  }
  if (assistantUsesModel.value) return props.modelName || 'AI 学习助手';
  return latestAssistant.value?.trace?.some(step => step.title === '使用本地兜底') ? '本地学习建议' : '';
});
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

<style scoped>
.studio-scene-choices > button { grid-template-columns: 1fr auto; align-items: center; }
.studio-scene-choices > button > svg { grid-column: 2; grid-row: 1; }
.studio-feedback-empty { color: var(--muted); }
</style>
