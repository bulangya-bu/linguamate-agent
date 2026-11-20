import { ElMessage } from "element-plus";
import { computed, reactive } from "vue";
import { learningApi } from "@/api/modules";
import {
  learningMessageFromResponse,
  learningMessageFromHistory,
  nextVocabularyPage,
} from "@/learning-advisor";

function createThread(scene = "daily", readingArticle = null) {
  return { id: null, messages: [], trace: [], scene, draft: "", error: "", readingArticle };
}

function createWorkspace() {
  const draft = createThread();
  return { draft, active: draft, saved: new Map() };
}

function historyMode(messages) {
  const latestUser = messages.findLast((message) => message.role === "user");
  if (latestUser?.mode) return latestUser.mode;
  const latestReply = messages.findLast((message) => message.role === "assistant");
  return latestReply?.coaching ? "practice" : "assistant";
}

export function useChat({
  goSection,
  loadReport,
  loadReviews,
  onModelConnected,
}) {
  const state = reactive({
    conversations: [],
    sending: false,
    loadingHistory: false,
    chatMode: "practice",
    coachWords: [],
    get chatBusy() { return this.sending || this.loadingHistory; },
  });
  const workspaces = reactive({ practice: createWorkspace(), assistant: createWorkspace() });
  const knownModes = reactive(new Map());
  const workspace = computed(() => workspaces[state.chatMode]);
  const activeThread = computed(() => workspace.value.active);
  const canStartNew = computed(() => activeThread.value !== workspace.value.draft);
  const visibleConversations = computed(() => state.conversations.flatMap((item) => {
    const mode = knownModes.get(Number(item.id)) ?? item.mode;
    return !mode || mode === state.chatMode ? [{ ...item, mode }] : [];
  }));

  function selectMode(mode) {
    if (!state.chatBusy && (mode === "practice" || mode === "assistant")) state.chatMode = mode;
  }

  function openLearningAssistant() {
    if (state.chatBusy) return;
    selectMode("assistant");
    goSection("chat");
  }

  function beginReadingDiscussion(article) {
    if (state.chatBusy) return false;
    selectMode("practice");
    workspace.value.active = createThread("reading", { id: article.id, title: article.title });
    return true;
  }

  async function handleLearningAction(action) {
    if (action.section === "review") await loadReviews();
    await goSection(action.section, action.query || {});
  }

  async function loadLearningCardPage(message, index, direction) {
    const thread = activeThread.value;
    const card = message.learningCard;
    const page = nextVocabularyPage(card, direction);
    if (page === card.page) return;
    try {
      const result = await learningApi.advisorVocabulary({
        group: card.group,
        page,
        size: card.size,
      });
      if (thread.messages[index] === message) {
        thread.messages[index] = { ...learningMessageFromResponse(result), id: message.id };
      }
    } catch (error) {
      ElMessage.error(error.message || "词库分页加载失败");
    }
  }

  async function loadConversations() {
    state.conversations = (await learningApi.conversations()) || [];
    return state.conversations;
  }

  async function handleConversationSelect(value) {
    const id = Number(value);
    if (state.chatBusy || !Number.isSafeInteger(id) || id < 1) return;
    const summary = state.conversations.find((item) => Number(item.id) === id);
    const savedMode = knownModes.get(id) ?? summary?.mode;
    const saved = workspaces[savedMode]?.saved.get(id);
    if (saved) {
      state.chatMode = savedMode;
      workspace.value.active = saved;
      return;
    }

    state.loadingHistory = true;
    try {
      const messages = (await learningApi.messages(id)).map(learningMessageFromHistory);
      const mode = savedMode === "practice" || savedMode === "assistant" ? savedMode : historyMode(messages);
      const trace = messages.findLast((item) => item.role === "assistant")?.trace || [];
      const thread = { ...createThread(summary?.scene), id, messages, trace };
      knownModes.set(id, mode);
      workspaces[mode].saved.set(id, thread);
      workspaces[mode].active = thread;
      state.chatMode = mode;
    } catch (error) {
      ElMessage.error(error.message || "会话加载失败，请重试");
    } finally {
      state.loadingHistory = false;
    }
  }

  function newConversation() {
    if (!state.chatBusy) workspace.value.active = workspace.value.draft;
  }

  function sendChatPrompt(prompt) {
    return sendChat(prompt);
  }

  async function loadCoachContext() {
    try {
      state.coachWords = await learningApi.practiceContext();
    } catch {
      state.coachWords = [];
    }
  }

  async function sendChat(prompt) {
    const thread = activeThread.value;
    const isPrompt = typeof prompt === "string";
    const message = (isPrompt ? prompt : thread.draft).trim();
    if (!message || state.chatBusy) return;
    const mode = state.chatMode;
    const currentWorkspace = workspace.value;
    const readingArticle = thread.readingArticle;
    const unsent = thread.draft;
    thread.error = "";
    if (!isPrompt) thread.draft = "";
    thread.messages.push({ role: "user", content: message, readingArticle });
    state.sending = true;
    try {
      const result = await learningApi.chat({
        conversationId: thread.id,
        scene: thread.scene,
        mode,
        message,
        readingArticleId: readingArticle?.id,
      });
      thread.id = Number(result.conversationId);
      knownModes.set(thread.id, mode);
      currentWorkspace.saved.set(thread.id, thread);
      if (thread === currentWorkspace.draft) currentWorkspace.draft = createThread(thread.scene);
      thread.readingArticle = null;
      thread.messages.push(learningMessageFromResponse(result));
      if (result.coaching?.source === "model") onModelConnected();
      thread.trace = result.trace || [];
      await Promise.allSettled([loadConversations(), loadReport()]);
    } catch (error) {
      thread.messages.pop();
      thread.draft = isPrompt ? unsent || message : unsent;
      thread.error = error.message || "发送失败，请重试";
    } finally {
      state.sending = false;
    }
  }

  return {
    state,
    activeThread,
    canStartNew,
    visibleConversations,
    selectMode,
    beginReadingDiscussion,
    openLearningAssistant,
    handleLearningAction,
    loadLearningCardPage,
    loadConversations,
    handleConversationSelect,
    newConversation,
    sendChatPrompt,
    loadCoachContext,
    sendChat,
  };
}
