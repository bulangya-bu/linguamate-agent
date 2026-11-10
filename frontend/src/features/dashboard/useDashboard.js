import { computed, onScopeDispose, ref, shallowRef } from "vue";
import { ElMessage } from "element-plus";
import { learningApi } from "@/api/modules";
import { learningMessageFromHistory } from "@/learning-advisor";
import { trendDays } from "@/learning-journal";

const RECENT_CONVERSATION_LIMIT = 5;
const STUDY_GOALS = [
  { id: "new-words", title: "学 3 个新单词", field: "newWords", target: 3, unit: "词" },
  { id: "chat", title: "和 AI 对话", field: "aiChatTurns", target: 1, unit: "轮" },
  { id: "writing", title: "写作文请 AI 批改", field: "aiEssayReviews", target: 1, unit: "篇" },
  { id: "reading", title: "读完一篇文章", field: "readingsFinished", target: 1, unit: "篇" },
];

export function useDashboard({
  goSection,
  getDueWords,
  getConversations,
  openDueReview,
  openChatPractice,
  resumePractice,
}) {
  const dashboardExpanded = ref(false);
  const evidenceOpen = ref(false);
  const growthOpen = ref(false);
  const homeJournal = shallowRef(null);
  const todayTasksLoading = shallowRef(true);
  const recentPractice = shallowRef(null);
  const studyActionBusy = shallowRef(false);
  let overviewRequest = 0;

  const recentStudy = computed(() => {
    const journal = homeJournal.value;
    if (!journal?.today) return null;
    const days = trendDays(journal.days, journal.today);
    return days.length ? { today: journal.today, days } : null;
  });

  const todayTasks = computed(() => {
    const journal = homeJournal.value;
    const day = journal?.days?.find((item) => item.date === journal.today);
    const dueWords = getDueWords();
    if (!day || dueWords == null) return null;

    const reviewStatus = dueWords > 0
      ? "pending"
      : day.reviewAttempts > 0 ? "done" : "skipped";
    return [
      {
        id: "review",
        title: "复习到期单词",
        detail: dueWords > 0 ? `${dueWords} 词待复习` : "已无到期词",
        status: reviewStatus,
      },
      ...STUDY_GOALS.map(({ id, title, field, target, unit }) => {
        const progress = Math.min(target, day[field] ?? 0);
        return {
          id,
          title,
          detail: `${progress} / ${target} ${unit}`,
          status: progress === target ? "done" : "pending",
        };
      }),
    ];
  });

  const nextAction = computed(() => {
    const tasks = todayTasks.value;
    if (!tasks) return getDueWords() > 0 ? "review" : "new-words";
    const pending = tasks.find((task) => task.status === "pending");
    return pending?.id || (recentPractice.value ? "conversation" : "journal");
  });

  function openGrowth() {
    growthOpen.value = true;
  }

  async function loadHomeJournal(request) {
    todayTasksLoading.value = true;
    try {
      const journal = await learningApi.journal();
      if (request === overviewRequest) homeJournal.value = journal;
    } catch {
      if (request === overviewRequest) homeJournal.value = null;
    } finally {
      if (request === overviewRequest) todayTasksLoading.value = false;
    }
  }

  async function loadRecentPractice(request) {
    // The summary has no mode. Use the same saved-feedback rule as chat history.
    const candidates = (getConversations() || []).slice(0, RECENT_CONVERSATION_LIMIT);
    for (const conversation of candidates) {
      if (request !== overviewRequest) return;
      try {
        const messages = await learningApi.messages(conversation.id);
        if (request !== overviewRequest) return;
        const latestReply = messages?.findLast(
          (message) => message.role === "assistant",
        );
        if (latestReply && learningMessageFromHistory(latestReply).coaching) {
          recentPractice.value = conversation;
          return;
        }
      } catch {
        // A missing conversation does not prevent checking the next candidate.
      }
    }
  }

  async function refreshTodayTasks() {
    const request = ++overviewRequest;
    recentPractice.value = null;
    await loadHomeJournal(request);
    if (request === overviewRequest && nextAction.value === "journal") {
      await loadRecentPractice(request);
    }
  }

  async function openStudyAction(action) {
    if (studyActionBusy.value) return;
    studyActionBusy.value = true;
    try {
      switch (action) {
        case "review":
          await openDueReview();
          break;
        case "chat":
          await openChatPractice();
          break;
        case "writing":
          await goSection("writing");
          break;
        case "reading":
          await goSection("news");
          break;
        case "conversation":
          await resumePractice(recentPractice.value.id);
          break;
        case "journal":
          await goSection("journal");
          break;
        case "new-words":
          await goSection("vocabulary", { scope: "new" });
      }
    } catch (error) {
      ElMessage.error(error.message || "练习暂时无法打开，请重试");
    } finally {
      studyActionBusy.value = false;
    }
  }

  onScopeDispose(() => {
    overviewRequest += 1;
  });

  return {
    dashboardExpanded,
    evidenceOpen,
    growthOpen,
    recentStudy,
    todayTasks,
    todayTasksLoading,
    nextAction,
    recentPractice,
    studyActionBusy,
    openGrowth,
    openStudyAction,
    refreshTodayTasks,
  };
}
