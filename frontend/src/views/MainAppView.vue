<script setup>
import AppIcon from "@/components/AppIcon.vue";
import AppBrand from "@/components/AppBrand.vue";
import { BRAND_NAME, brandedPageTitle } from "@/brand";
import {
  defineAsyncComponent,
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  reactive,
  shallowRef,
  watch,
} from "vue";
import WritingStudio from "@/components/WritingStudio.vue";
import LearningEvidence from "@/components/LearningEvidence.vue";
import UserAccountMenu from "@/components/UserAccountMenu.vue";
import ThemeToggle from "@/components/ThemeToggle.vue";
import { ElMessage } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import { authApi, learningApi } from "@/api/modules";
import { useDashboard } from "@/features/dashboard/useDashboard";
import { useVocabulary } from "@/features/vocabulary/useVocabulary";
import { useReading } from "@/features/reading/useReading";
import { useChat } from "@/features/chat/useChat";
import { useAiSettings } from "@/features/settings/useAiSettings";
import DashboardOverview from "@/features/dashboard/DashboardOverview.vue";
import ChatWorkspace from "@/features/chat/ChatWorkspace.vue";
import VocabularyPractice from "@/features/vocabulary/VocabularyPractice.vue";
import ReviewPractice from "@/features/vocabulary/ReviewPractice.vue";
import ReadingWorkspace from "@/features/reading/ReadingWorkspace.vue";
import WordBank from "@/features/vocabulary/WordBank.vue";
import ModelSettings from "@/features/settings/ModelSettings.vue";
import WordDetailDialog from "@/features/vocabulary/WordDetailDialog.vue";
import ReadingTooltip from "@/features/reading/ReadingTooltip.vue";
import "@/styles/module-illustrations.css";

const route = useRoute();
const router = useRouter();

const session = reactive({
  loading: true,
  currentUser: null,
  report: {},
  level: {},
  writingResult: null,
  logoutBusy: false,
});

const activeSection = computed(() => route.meta.section || "dashboard");

const settings = useAiSettings();
const {
  state: settingsState,
  settingsPanelRef,
  settingsReturnFocus,
  loadAiSummary,
  openSettings,
} = settings;

const dashboard = useDashboard({
  goSection,
  getDueWords: () => session.report.dueWords,
  getConversations: () => chatState.conversations,
  openDueReview,
  openChatPractice,
  resumePractice,
});
const {
  dashboardExpanded,
  evidenceOpen,
  growthOpen,
  openGrowth,
  refreshTodayTasks,
} = dashboard;
const growthInitialTab = shallowRef("words");

const vocabulary = useVocabulary({ route, router, activeSection, loadReport });
const {
  state: vocabularyState,
  wordDialogRef,
  wordDialogVisible,
  quizPreviewWords,
  loadQuizCategories,
  loadQuizForRoute,
  loadReviews,
  loadWordBank,
  loadWordBankStats,
} = vocabulary;

const chat = useChat({ goSection, loadReport, loadReviews, onModelConnected });
const {
  state: chatState,
  openLearningAssistant,
  loadConversations,
  newConversation,
  sendChatPrompt,
  loadCoachContext,
} = chat;

const reading = useReading({
  router,
  loadReport,
  loadWordBankStats,
  isChatBusy,
  discussReading,
});
const {
  state: readingState,
  newsLibraryOpen,
  loadNewsTopics,
  loadNews,
  returnToArticle,
} = reading;

const LearningJournal = defineAsyncComponent(
  () => import("@/components/LearningJournal.vue"),
);

const GrowthEvidence = defineAsyncComponent(
  () => import("@/components/GrowthEvidence.vue"),
);

function navigateJournal(section) {
  if (section === "mastered")
    router.push({ path: "/wordbank", query: { scope: "mastered" } });
  else goSection(section);
}

function openGrowthEvidence(tab = "words") {
  growthInitialTab.value = tab;
  openGrowth();
}

let previousFocus;
let dialogFocusFrame;

function closeOverlays() {
  settingsState.settingsOpen = false;
  vocabularyState.selectedWord = null;
  readingState.tooltip.visible = false;
  newsLibraryOpen.value = false;
}

function trapDialogFocus(event) {
  if (
    event.key === "Escape" &&
    (settingsState.settingsOpen || wordDialogVisible.value)
  ) {
    event.preventDefault();
    closeOverlays();
    return;
  }
  if (event.key !== "Tab") return;
  const dialog = settingsState.settingsOpen
    ? settingsPanelRef.value
    : wordDialogVisible.value
      ? wordDialogRef.value
      : null;
  if (!dialog) return;
  const controls = [
    ...dialog.querySelectorAll(
      "button:not(:disabled), input:not(:disabled), select:not(:disabled), textarea, a[href]",
    ),
  ];
  const first = controls[0],
    last = controls.at(-1);
  if (
    event.shiftKey &&
    (document.activeElement === first ||
      !dialog.contains(document.activeElement))
  ) {
    event.preventDefault();
    last?.focus();
  } else if (
    !event.shiftKey &&
    (document.activeElement === last ||
      !dialog.contains(document.activeElement))
  ) {
    event.preventDefault();
    first?.focus();
  }
}

const todayLabel = new Intl.DateTimeFormat("zh-CN", {
  month: "long",
  day: "numeric",
  weekday: "long",
  timeZone: "Asia/Shanghai",
}).format(new Date());

const navItems = [
  { id: "dashboard", label: "学习概览", path: "/" },
  { id: "chat", label: "对话练习", path: "/chat" },
  { id: "vocabulary", label: "单词练习", path: "/vocabulary" },
  { id: "review", label: "温故知新", path: "/review" },
  { id: "writing", label: "写作工坊", path: "/writing" },
  { id: "news", label: "阅读空间", path: "/news" },
  { id: "wordbank", label: "我的词库", path: "/wordbank" },
  { id: "journal", label: "学习档案", path: "/report" },
];

const sectionMeta = {
  dashboard: "学习概览",
  chat: "对话练习",
  vocabulary: "单词练习",
  review: "温故知新",
  writing: "写作工坊",
  news: "阅读空间",
  wordbank: "我的词库",
  journal: "学习档案",
};

const sectionTitle = computed(
  () => sectionMeta[activeSection.value] || sectionMeta.dashboard,
);

const userName = computed(
  () =>
    session.currentUser?.nickname || session.currentUser?.username || "学习者",
);

const levelPercent = computed(() => {
  const next = Number(session.level.nextLevelXp || 100);
  return Math.min(
    100,
    Math.round((Number(session.level.experience || 0) / next) * 100),
  );
});

function goSection(section, query = {}) {
  const target = navItems.find((item) => item.id === section) || navItems[0];
  return router.push({ path: target.path, query });
}

function openDueReview() {
  if (vocabularyState.reviewBusy) {
    throw new Error("复习结果正在保存，请稍候");
  }
  return goSection("review", { mode: "today" });
}

function loadReviewsForRoute() {
  if (activeSection.value === "review" && route.query.mode === "today") {
    vocabularyState.reviewMode = "today";
  }
  return loadReviews();
}

function openChatPractice() {
  if (chatState.chatBusy) {
    throw new Error("对话正在生成回复，请稍候");
  }
  if (chatState.chatMode !== "practice") {
    newConversation();
    chatState.chatMode = "practice";
  }
  return goSection("chat");
}

async function resumePractice(id) {
  if (chatState.chatBusy) {
    throw new Error("对话正在生成回复，请稍候");
  }
  const source = route.fullPath;
  await chat.handleConversationSelect(id);
  if (route.fullPath === source) await goSection("chat");
}

async function logout() {
  if (session.logoutBusy) return;
  session.logoutBusy = true;
  try {
    await authApi.logout();
    await router.replace("/login");
  } catch (error) {
    ElMessage.error(error.message || "退出登录失败，请重试");
  } finally {
    session.logoutBusy = false;
  }
}

async function loadReport() {
  const [report, level] = await Promise.all([
    learningApi.report(),
    learningApi.level(),
  ]);
  session.report = report || {};
  session.level = level || {};
  if (activeSection.value === "dashboard" && !session.loading) {
    await refreshTodayTasks();
  }
}

async function writingReviewed(result) {
  session.writingResult = result;
  if (result.reviewSource === "AI批改")
    settingsState.aiSummary.label = "连接正常";
  await loadReport();
}

async function boot() {
  document.body.className = "";
  session.currentUser = await authApi.me();
  await Promise.all([
    loadReport(),
    loadQuizCategories().then(loadQuizForRoute),
    loadReviewsForRoute(),
    loadNewsTopics().then(() => loadNews()),
    loadConversations(),
    loadAiSummary(),
    loadCoachContext(),
  ]);
  if (activeSection.value === "wordbank") {
    await loadWordBank();
  }
  session.loading = false;
  if (activeSection.value === "dashboard") refreshTodayTasks();
}

function onModelConnected() {
  settingsState.aiSummary.label = "连接正常";
}
function openEvidence() {
  evidenceOpen.value = true;
}
function isChatBusy() {
  return chatState.chatBusy;
}
async function discussReading(article) {
  newConversation();
  chatState.chatMode = "practice";
  chatState.chatScene = "reading";
  chatState.chatReadingArticle = { id: article.id, title: article.title };
  await router.push("/chat");
  await sendChatPrompt("我读完了这篇文章，想用英语聊聊其中的观点。");
}

watch(
  () => settingsState.settingsOpen || wordDialogVisible.value,
  async (open) => {
    cancelAnimationFrame(dialogFocusFrame);
    if (open) {
      previousFocus =
        settingsState.settingsOpen && settingsReturnFocus.value
          ? settingsReturnFocus.value
          : document.activeElement;
      document.body.style.overflow = "hidden";
      await nextTick();
      // Visibility transitions become focusable after their first painted frame.
      dialogFocusFrame = requestAnimationFrame(() => {
        dialogFocusFrame = requestAnimationFrame(() => {
          const dialog = settingsState.settingsOpen
            ? settingsPanelRef.value
            : wordDialogVisible.value
              ? wordDialogRef.value
              : null;
          dialog?.querySelector("button, input, select")?.focus();
        });
      });
    } else {
      document.body.style.overflow = "";
      await nextTick();
      previousFocus?.focus();
      settingsReturnFocus.value = null;
    }
  },
  { flush: "post" },
);

watch([activeSection, () => route.query.scope, () => route.query.mode], async ([section]) => {
  document.title = brandedPageTitle(sectionTitle.value);
  readingState.tooltip.visible = false;
  if (section === "news") readingState.lookupCache = {};
  if (section === "vocabulary" && !session.loading) await loadQuizForRoute();
  if (section === "review" && !session.loading && !vocabularyState.reviewBusy)
    await loadReviewsForRoute();
  if (section === "wordbank" && !session.loading) await loadWordBank();
  if (section === "chat" && !session.loading) await loadCoachContext();
  if (section === "dashboard" && !session.loading) await refreshTodayTasks();
});

onMounted(() => {
  document.title = brandedPageTitle(sectionTitle.value);
  document.addEventListener("keydown", trapDialogFocus, true);
  boot().catch((error) => {
    console.error(error);
    session.loading = false;
    ElMessage.error(error.message || "加载失败");
  });
});

onBeforeUnmount(() => {
  cancelAnimationFrame(dialogFocusFrame);
  document.removeEventListener("keydown", trapDialogFocus, true);
  document.body.style.overflow = "";
});
</script>

<template>
  <a class="skip-link" href="#main-content">跳转到学习内容</a>
  <div v-if="session.loading" class="app-loading">
    <img src="/assets/linguamate-icon.svg?v=bright" alt="" /><span
      >正在准备你的学习空间</span
    >
  </div>
  <div
    v-else
    class="app-shell"
    :class="[
      'page-' + activeSection,
      {
        'dashboard-expanded': dashboardExpanded,
        'has-writing-result': Boolean(session.writingResult),
      },
    ]"
    @keydown.esc="closeOverlays"
  >
    <aside
      class="side-nav"
      aria-label="学习导航"
      :inert="settingsState.settingsOpen || wordDialogVisible"
    >
      <AppBrand />
      <nav>
        <span
          class="nav-active-indicator"
          :style="{
            '--nav-index': navItems.findIndex(
              (item) => item.id === activeSection,
            ),
            '--nav-section-offset':
              navItems.findIndex((item) => item.id === activeSection) >= 4
                ? 1
                : 0,
          }"
          aria-hidden="true"
        ></span>
        <button
          v-for="(item, index) in navItems"
          :key="item.id"
          class="nav-item"
          :class="{
            active: activeSection === item.id,
            'nav-group-start': index === 4,
          }"
          :aria-current="activeSection === item.id ? 'page' : undefined"
          type="button"
          @click="goSection(item.id)"
        >
          <span class="nav-icon-wrap"><AppIcon :name="item.id" /></span
          ><span>{{ item.label }}</span
          ><span
            v-if="item.id === 'review' && session.report.dueWords"
            class="nav-count"
            >{{ session.report.dueWords }}</span
          >
        </button>
      </nav>
      <div class="sidebar-bottom">
        <div class="sidebar-progress desktop-account-legacy">
          <div>
            <span>学习等级</span
            ><strong>Lv.{{ session.level.level || 1 }}</strong>
          </div>
          <div class="level-xp-bar">
            <div
              class="level-xp-fill"
              :style="{ width: levelPercent + '%' }"
            ></div>
          </div>
          <small
            >{{ session.level.experience || 0 }} /
            {{ session.level.nextLevelXp || 100 }} XP</small
          >
        </div>
        <button
          class="sidebar-model-config"
          type="button"
          aria-label="模型配置"
          @click="openSettings($event.currentTarget)"
        >
          <span class="sidebar-model-icon"><AppIcon name="settings" /></span>
          <span class="sidebar-model-copy"
            ><strong>模型配置</strong
            ><small
              :class="{
                'is-ready': settingsState.aiSummary.label === '连接正常',
              }"
              >{{ settingsState.aiSummary.label }}</small
            ></span
          >
          <AppIcon class="sidebar-model-arrow" name="arrow" />
        </button>
        <RouterLink
          v-if="session.currentUser?.role === 'ADMIN'"
          class="nav-link-button desktop-account-legacy"
          to="/admin"
          >管理工作台 <AppIcon name="arrow"
        /></RouterLink>
        <button
          class="sidebar-user desktop-account-legacy"
          type="button"
          aria-label="打开我的 API 设置"
          @click="openSettings"
        >
          <span class="level-avatar">{{ userName.slice(0, 1) }}</span
          ><span
            ><strong>{{ userName }}</strong></span
          ><AppIcon name="settings" />
        </button>
        <button
          class="nav-logout-button desktop-account-legacy"
          type="button"
          @click="logout"
        >
          <AppIcon name="logout" />退出登录
        </button>
      </div>
    </aside>
    <main
      id="main-content"
      class="workspace"
      tabindex="-1"
      :inert="settingsState.settingsOpen || wordDialogVisible"
    >
      <header class="workspace-bar">
        <div class="workspace-location">
          <span class="workspace-location-icon" aria-hidden="true"
            ><AppIcon :name="activeSection"
          /></span>
          <span class="workspace-context"
            ><span>{{ BRAND_NAME }}</span
            ><strong>{{ sectionTitle }}</strong></span
          >
        </div>
        <div class="workspace-tools">
          <span class="today-date"
            ><AppIcon name="calendar" />{{ todayLabel }}</span
          ><button
            type="button"
            class="connection-button desktop-account-legacy"
            @click="openSettings"
          >
            <span class="status-dot"></span>模型设置<AppIcon name="settings" />
          </button>
          <ThemeToggle />
          <UserAccountMenu
            :user="session.currentUser || {}"
            :busy="session.logoutBusy"
            :level="session.level"
            :portal-path="session.currentUser?.role === 'ADMIN' ? '/admin' : ''"
            portal-label="管理工作台"
            @logout="logout"
          />
        </div>
      </header>
      <header class="topbar">
        <div class="hero-copy">
          <h1>
            {{
              activeSection === "dashboard"
                ? "你好，" + userName + "。"
                : sectionTitle
            }}
          </h1>
        </div>
      </header>
      <DashboardOverview
        :dashboard="dashboard"
        :active-section="activeSection"
        :go-section="goSection"
        :open-learning-assistant="openLearningAssistant"
        :words="quizPreviewWords"
        @word="vocabularyState.selectedWord = $event"
        :report="session.report"
      />

      <section
        id="journal"
        class="section"
        :class="{ active: activeSection === 'journal' }"
      >
        <LearningJournal
          v-if="activeSection === 'journal'"
          @navigate="navigateJournal"
          @evidence="openGrowthEvidence"
        />
      </section>
      <ChatWorkspace
        :chat="chat"
        :active-section="activeSection"
        :open-settings="openSettings"
        :model-name="settingsState.aiSummary.modelName"
      />

      <VocabularyPractice
        :vocabulary="vocabulary"
        :active-section="activeSection"
      />

      <ReviewPractice
        :vocabulary="vocabulary"
        :active-section="activeSection"
        :go-section="goSection"
        :return-to-article="returnToArticle"
        :open-evidence="openEvidence"
        :report="session.report"
      />

      <section
        id="writing"
        class="section"
        :class="{ active: activeSection === 'writing' }"
      >
        <WritingStudio
          v-if="session.currentUser"
          :user-id="session.currentUser.id"
          @reviewed="writingReviewed"
          @settings="openSettings"
        />
      </section>

      <ReadingWorkspace
        :reading="reading"
        :active-section="activeSection"
        :chat-busy="chatState.chatBusy"
      />

      <WordBank :vocabulary="vocabulary" :active-section="activeSection" />
    </main>

    <ModelSettings :settings="settings" />

    <WordDetailDialog :vocabulary="vocabulary" />

    <LearningEvidence
      :open="evidenceOpen"
      :evidence="session.report.evidence || {}"
      @close="evidenceOpen = false"
      @practice="goSection('review')"
    />
    <GrowthEvidence
      :open="growthOpen"
      :initial-tab="growthInitialTab"
      @close="growthOpen = false"
      @navigate="goSection"
    />
    <ReadingTooltip :reading="reading" />
  </div>
</template>

<style scoped>
.sidebar-model-config {
  display: none;
}
@media (min-width: 981px) {
  .sidebar-model-config {
    display: flex;
    align-items: center;
    gap: 10px;
    width: 100%;
    min-height: 56px;
    padding: 9px 11px;
    border: 1px solid #e1eaf0;
    border-radius: 13px;
    background: #f8fbfd;
    color: #476174;
    text-align: left;
    font-size: 13px;
    transition:
      background 200ms ease,
      border-color 200ms ease;
  }
  .sidebar-model-config:hover {
    border-color: #c5dfed;
    background: #f0f8fd;
  }
  .sidebar-model-icon {
    display: grid;
    place-items: center;
    width: 32px;
    height: 32px;
    border-radius: 9px;
    background: #eaf2f8;
    color: #6993ad;
  }
  .sidebar-model-icon > svg {
    width: 19px;
    height: 19px;
    transition: transform 240ms var(--ease);
  }
  .sidebar-model-config:hover .sidebar-model-icon > svg {
    transform: rotate(20deg);
  }
  .sidebar-model-config > small {
    margin-left: auto;
    color: #8b9eab;
    font-size: 10px;
  }
  .sidebar-model-config > small.is-ready {
    color: #638a49;
  }
}
@media (prefers-reduced-motion: reduce) {
  .sidebar-model-config,
  .sidebar-model-icon > svg {
    transition: none;
  }
}
</style>
