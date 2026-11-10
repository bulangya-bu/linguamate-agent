import { ElMessage } from "element-plus";
import { reactive } from "vue";
import { learningApi } from "@/api/modules";
import {
  learningMessageFromResponse,
  learningMessageFromHistory,
  nextVocabularyPage,
} from "@/learning-advisor";

export function useChat({
  goSection,
  loadReport,
  loadReviews,
  onModelConnected,
}) {
  const state = reactive({
    conversations: [],
    conversationId: null,
    messages: [],
    trace: [],
    chatScene: "daily",
    chatInput: "",
    chatBusy: false,
    chatMode: "practice",
    chatError: "",
    chatReadingArticle: null,
    coachWords: [],
  });

  function openLearningAssistant() {
    state.chatMode = "assistant";
    goSection("chat");
  }

  async function handleLearningAction(action) {
    if (action.section === "review") await loadReviews();
    await goSection(action.section, action.query || {});
  }

  async function loadLearningCardPage(message, index, direction) {
    const card = message.learningCard;
    const page = nextVocabularyPage(card, direction);
    if (page === card.page) return;
    try {
      const result = await learningApi.advisorVocabulary({
        group: card.group,
        page,
        size: card.size,
      });
      state.messages[index] = learningMessageFromResponse(result);
    } catch (error) {
      ElMessage.error(error.message || "词库分页加载失败");
    }
  }

  async function loadConversations() {
    state.conversations = await learningApi.conversations();
    return state.conversations;
  }

  async function openConversation(id) {
    state.chatReadingArticle = null;
    state.conversationId = id;
    const conversation = state.conversations.find(
      (item) => Number(item.id) === Number(id),
    );
    if (conversation?.scene) state.chatScene = conversation.scene;
    state.chatError = "";
    state.messages = (await learningApi.messages(id)).map(
      learningMessageFromHistory,
    );
    const latest = state.messages.findLast((item) => item.role === "assistant");
    state.trace = latest?.trace || [];
    state.chatMode = latest?.coaching ? "practice" : "assistant";
  }

  async function handleConversationSelect(value) {
    if (state.chatBusy) return;
    if (value === "new") {
      newConversation();
      return;
    }
    const id = Number(value);
    if (Number.isFinite(id)) await openConversation(id);
  }

  function newConversation() {
    if (state.chatBusy) return;
    state.chatError = "";
    state.conversationId = null;
    state.messages = [];
    state.trace = [];
    state.chatInput = "";
    state.chatReadingArticle = null;
  }

  function sendChatPrompt(prompt) {
    if (state.chatBusy) return;
    state.chatInput = prompt;
    return sendChat();
  }

  async function loadCoachContext() {
    try {
      state.coachWords = await learningApi.practiceContext();
    } catch {
      state.coachWords = [];
    }
  }

  async function sendChat() {
    if (!state.chatInput.trim() || state.chatBusy) return;
    const message = state.chatInput.trim();
    state.chatError = "";
    state.chatInput = "";
    const readingArticle = state.chatReadingArticle;
    state.messages.push({ role: "user", content: message, readingArticle });
    state.chatBusy = true;
    try {
      const result = await learningApi.chat({
        conversationId: state.conversationId,
        scene: state.chatScene,
        mode: state.chatMode,
        message,
        readingArticleId: readingArticle?.id,
      });
      state.conversationId = result.conversationId;
      state.chatReadingArticle = null;
      state.messages.push(learningMessageFromResponse(result));
      if (result.coaching?.source === "model") onModelConnected();
      state.trace = result.trace || [];
      await Promise.allSettled([loadConversations(), loadReport()]);
    } catch (error) {
      state.messages.pop();
      state.chatInput = message;
      state.chatError = error.message || "暂时没能发送，内容已保留，请重试。";
    } finally {
      state.chatBusy = false;
    }
  }

  return {
    state,
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
