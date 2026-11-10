import { computed, reactive } from "vue";
import { ElMessage } from "element-plus";
import { adminApi } from "@/api/modules";



export function useAdminOverview() {
  const state = reactive({
    summaryBusy: false,
    summary: {}
  });

  const inactiveUsers = computed(() =>
    Math.max(
      0,
      Number(state.summary.users || 0) - Number(state.summary.activeUsers || 0),
    ),
  );

  const activePercent = computed(() =>
    state.summary.users
      ? Math.round((state.summary.activeUsers / state.summary.users) * 100)
      : 0,
  );

  const weekReviews = computed(() =>
    (state.summary.dailyWordStats || []).reduce(
      (sum, item) => sum + (Number(item.words) || 0),
      0,
    ),
  );

  const overviewMetrics = computed(() => [
    {
      label: "今日练词作答",
      value: state.summary.todayWordReviews,
      unit: "次",
      icon: "overview",
      tone: "is-green",
    },
    {
      label: "今日练词人数",
      value: state.summary.todayLearners,
      unit: "人",
      icon: "users",
      tone: "is-blue",
    },
    {
      label: "今日覆盖单词",
      value: state.summary.todayUniqueWords,
      unit: "词",
      icon: "words",
      tone: "",
    },
    {
      label: "注册账号",
      value: state.summary.users,
      unit: "个",
      icon: "shield",
      tone: "",
    },
  ]);

  const contentAssets = computed(() => [
    {
      id: "words",
      label: "单词词库",
      value: state.summary.words,
      unit: "词",
    },
    {
      id: "news",
      label: "阅读文章",
      value: state.summary.news,
      unit: "篇",
    },
    {
      id: "knowledge",
      label: "知识文档",
      value: state.summary.knowledgeDocuments,
      unit: "篇",
    },
  ]);

  const activityTotals = computed(() => [
    {
      label: "累计会话",
      value: state.summary.conversations,
      unit: "个",
      icon: "chat",
    },
    {
      label: "写作提交",
      value: state.summary.essays,
      unit: "次",
      icon: "writing",
    },
    {
      label: "阅读记录",
      value: state.summary.readings,
      unit: "条",
      icon: "news",
    },
    {
      label: "错词词条",
      value: state.summary.reviewWords,
      unit: "条",
      icon: "review",
    },
  ]);

  async function loadSummary() {
    state.summary = await adminApi.summary();
  }

  async function refreshSummary() {
    if (state.summaryBusy) return;
    state.summaryBusy = true;
    try {
      await loadSummary();
    } catch (error) {
      ElMessage.error(error.message || "数据刷新失败，请重试");
    } finally {
      state.summaryBusy = false;
    }
  }

  return { state, inactiveUsers, activePercent, weekReviews, overviewMetrics, contentAssets, activityTotals, loadSummary, refreshSummary };
}
