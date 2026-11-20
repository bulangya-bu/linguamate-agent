<template>
  <div class="conversation-studio">
    <header class="studio-chat-toolbar">
      <div class="studio-mode-switch" aria-label="对话模式">
        <button v-for="item in modes" :key="item.id" type="button" :class="{ selected: mode === item.id }"
          :aria-pressed="mode === item.id" :disabled="controlsBusy" @click="$emit('mode', item.id)">
          {{ item.label }}
        </button>
      </div>
      <div class="studio-history-controls">
        <label v-if="isPhone" class="studio-phone-history">
          历史
          <select aria-label="历史会话" :value="selectedHistoryId || ''" :disabled="controlsBusy || !conversations.length"
            @change="$emit('history', $event.target.value)">
            <option value="" disabled>历史会话</option>
            <option v-for="item in conversations" :key="item.id" :value="String(item.id)">{{ item.title || '学习对话' }} · {{ historyDetail(item) }}</option>
          </select>
        </label>
        <el-select v-else :model-value="selectedHistoryId" aria-label="历史会话" placeholder="历史"
          class="studio-history" filterable :disabled="controlsBusy" no-data-text="暂无会话" popper-class="chat-select-popper" @change="$emit('history', $event)">
          <el-option v-for="conversation in conversations" :key="conversation.id" :value="`${conversation.id}`" :label="conversation.title || '学习对话'">
            <span class="studio-history-item">
              <strong>{{ conversation.title || '学习对话' }}</strong>
              <small>{{ historyDetail(conversation) }}</small>
            </span>
          </el-option>
        </el-select>
        <button type="button" class="studio-new" aria-label="新对话" :disabled="controlsBusy || !canStartNew" @click="$emit('new')"><span aria-hidden="true">＋</span><span v-if="!isPhone" class="studio-new-label">新对话</span></button>
      </div>
    </header>

    <div class="studio-chat-columns">
      <section class="studio-dialogue" aria-label="英语对话">
        <header class="studio-dialogue-heading">
          <strong>{{ mode === 'practice' ? currentScene.label : '学习助手' }}</strong>
          <span v-if="userTurns" class="studio-turn-count">{{ userTurns }} 轮</span>
          <button v-if="isPhone && (targetWords.length || assistantActions.length || displayedTrace.length)" type="button"
            class="studio-coach-open" aria-haspopup="dialog" @click="openCoach()">
            {{ mode === 'assistant' ? '学习建议' : userTurns ? '本轮反馈' : '薄弱词' }}
          </button>
        </header>

        <div ref="thread" class="studio-thread" role="log" aria-label="对话记录" aria-live="polite" :aria-busy="controlsBusy">
          <p v-if="loadingHistory" class="studio-history-loading" role="status">正在读取会话…</p>
          <div v-if="!messages.length" class="studio-chat-welcome">
            <div class="studio-welcome-copy">
              <h2>{{ mode === 'practice' ? '选个场景' : '想学什么？' }}</h2>
              <div v-if="mode === 'practice'" class="studio-scene-choices" role="group" aria-label="练习场景">
                <button
                  v-for="item in scenes"
                  :key="item.id"
                  type="button"
                  :class="{ selected: scene === item.id }"
                  :aria-pressed="scene === item.id"
                  :disabled="controlsBusy"
                  @click="$emit('scene', item.id)"
                >
                  <strong>{{ item.label }}</strong>
                  <AppIcon v-if="scene === item.id" name="check" />
                </button>
              </div>
              <div v-else class="studio-assistant-prompts">
                <button v-for="prompt in assistantPrompts" :key="prompt" type="button" :disabled="controlsBusy" @click="submitPrompt(prompt)">
                  {{ prompt }}<AppIcon name="arrow" />
                </button>
              </div>
              <button v-if="mode === 'practice'" class="studio-start-practice" type="button" :disabled="controlsBusy" @click="submitPrompt(currentScene.prompt)">
                开始练习<AppIcon name="arrow" />
              </button>
            </div>
            <div v-if="mode === 'practice'" class="studio-welcome-art"><ConversationArt :scene="scene" :mode="mode" :disabled="controlsBusy" @suggest="suggestFromArt" /></div>
          </div>

          <article v-for="(message, index) in messages" :key="message.id ? `saved-${message.id}` : `live-${index}`" class="studio-message" :class="message.role">
            <div v-if="message.role === 'assistant'" class="studio-message-avatar" aria-hidden="true"><AppIcon name="chat" /></div>
            <div class="studio-message-body">
              <span v-if="message.role === 'user'" class="sr-only">你</span>
              <div v-else class="studio-message-byline"><strong>{{ message.coaching ? '教练' : '助手' }}</strong></div>
              <div v-if="message.role === 'user' && message.readingArticle" class="studio-reading-attachment"><AppIcon name="news" /><span>阅读讨论<strong>{{ message.readingArticle.title }}</strong></span></div>
              <p><template v-for="(part, partIndex) in messageTextParts(message)" :key="partIndex"><strong v-if="part.emphasis">{{ part.text }}</strong><template v-else>{{ part.text }}</template></template></p>
              <slot name="message-extra" :message="message" :index="index" :show-actions="mode !== 'assistant' || message !== latestAssistant" />
              <div v-if="message.coaching" class="studio-message-actions">
                <button class="studio-feedback-link" :class="{ selected: selectedIndex === index }" type="button"
                  aria-label="查看本轮反馈" :aria-pressed="isPhone ? undefined : selectedIndex === index"
                  :aria-haspopup="isPhone ? 'dialog' : undefined" @click="openCoach(index)">反馈<AppIcon name="arrow" /></button>
                <button
                  v-if="canTranslate(message)"
                  class="studio-translation-toggle"
                  type="button"
                  :aria-label="expandedTranslations.has(message.id) ? '收起中文' : '查看中文'"
                  :aria-expanded="expandedTranslations.has(message.id)"
                  :aria-controls="expandedTranslations.has(message.id) ? `reply-translation-${message.id}` : undefined"
                  @click="toggleTranslation(message.id)"
                >
                  {{ expandedTranslations.has(message.id) ? '收起' : '中文' }}
                </button>
              </div>
              <div
                v-if="canTranslate(message) && expandedTranslations.has(message.id)"
                :id="`reply-translation-${message.id}`"
                class="studio-translation"
                aria-live="polite"
              >
                <p v-if="translations.get(message.id)?.status === 'loading'" class="studio-translation-state">正在翻译…</p>
                <div v-else-if="translations.get(message.id)?.status === 'error'" class="studio-translation-error">
                  <p>{{ translations.get(message.id).message }}</p>
                  <button type="button" @click="loadTranslation(message.id)">重试</button>
                </div>
                <p v-else lang="zh-CN">{{ translations.get(message.id)?.text }}</p>
              </div>
            </div>
          </article>
          <div v-if="busy" class="studio-thinking" role="status">{{ elapsed >= 15 ? `正在回复 · ${elapsed} 秒` : '正在回复…' }}</div>
        </div>

        <form class="studio-composer" @submit.prevent="sendMessage">
          <p v-if="error" class="studio-chat-error" role="alert">{{ error }}</p>
          <div class="studio-composer-box">
            <textarea ref="input" :value="draft" maxlength="2000" rows="2" aria-label="对话内容"
              aria-describedby="studio-composer-shortcuts" :disabled="controlsBusy"
              :placeholder="mode === 'practice' ? '用英语开口…' : '输入学习问题…'"
              @input="$emit('update:draft', $event.target.value)" @keydown="handleKeydown"></textarea>
            <button type="submit" :disabled="controlsBusy || !draft.trim()" :aria-label="busy ? '正在回复' : '发送消息'">发送<AppIcon name="arrow" /></button>
          </div>
          <span id="studio-composer-shortcuts" class="sr-only">Enter 发送，Shift + Enter 换行。</span>
          <div v-if="draft.length >= 1800" class="studio-composer-footer">{{ draft.length }} / 2000</div>
        </form>
      </section>

      <component :is="isPhone ? 'dialog' : 'aside'" ref="coachPanel" class="studio-coach"
        :aria-label="mode === 'practice' ? '练习反馈' : '学习建议'" @click="dismissBackdrop">
        <header class="studio-coach-heading">
          <h2>{{ mode === 'assistant' ? '下一步' : showPracticeWords ? '薄弱词' : '本轮反馈' }}</h2>
          <span v-if="responseSource" class="studio-model-line" :title="responseSource">{{ responseSource }}</span>
          <button v-if="isPhone" type="button" class="studio-coach-close" aria-label="关闭反馈" @click="closeCoach"><AppIcon name="close" /></button>
        </header>
        <div ref="coachScroll" class="studio-coach-scroll">
          <template v-if="mode === 'practice'">
            <p v-if="showPendingFeedback" class="studio-feedback-pending">正在准备反馈…</p>
            <Transition name="coach-reveal" mode="out-in">
              <div v-if="feedback && !showPendingFeedback" :key="selectedIndex" class="studio-feedback-content">
                <div v-if="feedback.source === 'local'" class="studio-local-notice"><AppIcon name="settings" /><button type="button" @click="openCoachSettings">模型配置<AppIcon name="arrow" /></button></div>
                <section v-if="feedback.source === 'model' && (feedback.improved || feedback.explanation)" class="studio-coach-section">
                  <div v-if="feedback.improved" class="studio-expression">
                    <div class="studio-original">
                      <span>原句</span>
                      <p lang="en"><template v-for="(part, index) in revisionParts.original" :key="index"><mark v-if="part.type === 'delete' && part.text.trim()">{{ part.text }}</mark><template v-else>{{ part.text }}</template></template></p>
                    </div>
                    <div class="studio-revision">
                      <span>建议表达</span>
                      <p lang="en"><template v-for="(part, index) in revisionParts.improved" :key="index"><mark v-if="part.type === 'insert' && part.text.trim()">{{ part.text }}</mark><template v-else>{{ part.text }}</template></template></p>
                    </div>
                  </div>
                  <p v-if="feedback.explanation" class="studio-explanation">{{ feedback.explanation }}</p>
                </section>
                <section v-if="feedback.phrases?.length" class="studio-coach-section">
                  <h3>实用短语</h3>
                  <div v-for="phrase in feedback.phrases" :key="phrase.text" class="studio-phrase"><strong>{{ phrase.text }}</strong><p>{{ phrase.meaningCn }}</p></div>
                </section>
                <section v-if="feedback.nextChallenge" class="studio-coach-section studio-next-challenge"><h3>下一轮</h3><p>{{ feedback.nextChallenge }}</p><button type="button" @click="closeCoach(); focusInput()">回答<AppIcon name="arrow" /></button></section>
              </div>
            </Transition>
            <section v-if="targetWords.length" class="studio-targets" :class="{ 'is-preview': showPracticeWords }">
              <h3 v-if="!showPracticeWords"><AppIcon name="review" />薄弱词</h3>
              <div class="studio-target-list"><span v-for="word in targetWords" :key="word.word" :class="{ used: word.used }" :title="word.meaningCn"><AppIcon v-if="word.used" name="check" />{{ word.word }}<small>{{ word.meaningCn }}</small></span></div>
              <button type="button" @click="closeCoach(); $emit('review')">复习错词<AppIcon name="arrow" /></button>
            </section>
          </template>
          <div v-else class="studio-assistant-guide">
            <slot v-if="assistantActions.length" name="assistant-actions" :actions="assistantActions" />
            <p v-else class="studio-guide-empty">{{ busy ? '正在整理建议…' : '暂无练习建议' }}</p>
          </div>
          <details v-if="displayedTrace.length" class="studio-process"><summary>处理记录<span>{{ displayedTrace.length }} 步</span></summary><ol><li v-for="(step,index) in displayedTrace" :key="index"><span>{{ step.phase }}</span><strong>{{ step.title }}</strong><p v-if="step.phase !== 'Observe' && step.phase !== 'Answer'">{{ step.detail }}</p></li></ol></details>
        </div>
      </component>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, shallowRef, useTemplateRef, watch } from 'vue';
import { usePhoneLayout } from '@/composables/usePhoneLayout';
import AppIcon from './AppIcon.vue';
import ConversationArt from './ConversationArt.vue';
import { useChatThread } from '@/features/chat/useChatThread';
import { useReplyTranslations } from '@/features/chat/useReplyTranslations';
import { revisionDiff } from '@/writing-workspace';
const props = defineProps({ messages: { type: Array, default: () => [] }, conversations: { type: Array, default: () => [] }, conversationId: [Number, String],
  scene: { type: String, default: 'daily' }, mode: { type: String, default: 'practice' }, draft: { type: String, default: '' }, busy: Boolean,
  loadingHistory: Boolean, canStartNew: Boolean, active: Boolean, error: String, contextWords: { type: Array, default: () => [] }, trace: { type: Array, default: () => [] } });
const emit = defineEmits(['mode', 'scene', 'history', 'new', 'send', 'update:draft', 'prompt', 'settings', 'review']);
const {
  thread, input, coachScroll, selectedIndex, showPendingFeedback,
  selectFeedback, focusInput, sendMessage, submitPrompt, handleKeydown,
} = useChatThread(props, emit);
const isPhone = usePhoneLayout();
const coachPanel = useTemplateRef('coachPanel');
function openCoach(index) {
  const round = index ?? props.messages.findLastIndex(message => message.coaching);
  if (round >= 0) selectFeedback(round);
  if (isPhone.value && !coachPanel.value?.open) coachPanel.value?.showModal();
}
function closeCoach() {
  if (coachPanel.value?.open) coachPanel.value.close();
}
function openCoachSettings(event) {
  closeCoach();
  emit('settings', isPhone.value ? document.activeElement : event.currentTarget);
}
function dismissBackdrop(event) {
  if (!isPhone.value || event.target !== coachPanel.value) return;
  const bounds = coachPanel.value.getBoundingClientRect();
  if (event.clientY < bounds.top || event.clientX < bounds.left || event.clientX > bounds.right) closeCoach();
}
watch([() => props.active, () => props.mode, () => props.conversationId, isPhone], closeCoach);
const {
  translations, expandedTranslations, canTranslate, toggleTranslation, loadTranslation,
} = useReplyTranslations(props, thread);
const modes = [{ id: 'practice', label: '对话练习' }, { id: 'assistant', label: '学习助手' }];
const scenes = [
  { id: 'daily', label: '日常交流', prompt: '请和我练习日常英语，从今天的一件小事开始。' },
  { id: 'exam', label: '考试口语', prompt: '请作为英语口语考官，先问我一个观点类问题。' },
  { id: 'business', label: '商务表达', prompt: '请模拟同事，用英语和我聊一个项目，先问我一个问题。' },
  { id: 'reading', label: '阅读讨论', prompt: '请和我用英语讨论阅读，先问我最近读过的内容。' },
];
const assistantPrompts = ['今天还有什么没学？', '不熟悉的单词有哪些？', '推荐一篇英语阅读'];
const controlsBusy = computed(() => props.busy || props.loadingHistory);
const currentScene = computed(() => scenes.find(item => item.id === props.scene) || scenes[0]);
const historyDate = new Intl.DateTimeFormat('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
function historyDetail(conversation) {
  const scene = conversation.mode === 'assistant' ? '学习助手' : scenes.find(item => item.id === conversation.scene)?.label;
  const time = conversation.updatedAt ? new Date(conversation.updatedAt) : null;
  const dateLabel = time && !Number.isNaN(time.getTime()) ? historyDate.format(time) : '';
  return [scene, dateLabel].filter(Boolean).join(' · ');
}
const selectedHistoryId = computed(() => {
  const id = String(props.conversationId ?? '');
  return props.conversations.some(item => String(item.id) === id) ? id : undefined;
});
const userTurns = computed(() => props.messages.filter(item => item.role === 'user').length);
const latestAssistant = computed(() => props.messages.findLast(item => item.role === 'assistant'));
const assistantActions = computed(() => latestAssistant.value?.actions || []);
const feedback = computed(() => props.messages[selectedIndex.value]?.coaching);
const revisionParts = computed(() => {
  const parts = revisionDiff(feedback.value?.original || '', feedback.value?.improved || '');
  return {
    original: parts.filter(part => part.type !== 'insert'),
    improved: parts.filter(part => part.type !== 'delete'),
  };
});
const responseSource = computed(() => {
  if (props.mode === 'practice') {
    return feedback.value?.source === 'local' ? '本地练习' : '';
  }
  return latestAssistant.value?.trace?.some(step => step.title === '使用本地兜底') ? '本地建议' : '';
});
const targetWords = computed(() => feedback.value?.targetWords || props.contextWords);
const showPracticeWords = computed(() => props.mode === 'practice' && !props.messages.length && !props.busy && targetWords.value.length > 0);
const displayedTrace = computed(() => props.mode === 'practice' ? props.messages[selectedIndex.value]?.trace || props.trace : props.trace);
const elapsed = shallowRef(0);
let waitTimer;
watch(() => props.busy, busy => {
  clearInterval(waitTimer);
  if (!busy) return;
  elapsed.value = 0;
  waitTimer = setInterval(() => elapsed.value++, 1000);
});
onBeforeUnmount(() => clearInterval(waitTimer));
function messageTextParts(message) {
  if (message.role === 'user') return [{ text: message.content, emphasis: false }];
  // Interpolate text nodes rather than HTML, including when a model emits Markdown emphasis.
  return String(message.content || '').split(/\*\*([^*\n]+)\*\*/g)
    .map((text, index) => ({ text, emphasis: index % 2 === 1 }));
}
const lastArtPrompt = shallowRef('');
watch([() => props.mode, () => props.conversationId], () => { lastArtPrompt.value = ''; });
async function suggestFromArt(prompt) {
  if (controlsBusy.value) return;
  const existing = props.draft;
  let next = prompt;
  if (existing.trim()) {
    const replacesStarter = lastArtPrompt.value && existing.endsWith(lastArtPrompt.value);
    next = replacesStarter ? existing.slice(0, -lastArtPrompt.value.length) + prompt : `${existing}\n${prompt}`;
  }
  emit('update:draft', next.slice(0, 2000));
  lastArtPrompt.value = prompt;
  await nextTick();
  focusInput();
}
</script>
