<script setup>
import AppIcon from "@/components/AppIcon.vue";
import AppBrand from "@/components/AppBrand.vue";
import AdminSketchIcon from "@/components/AdminSketchIcon.vue";
import UserAccountMenu from "@/components/UserAccountMenu.vue";
import ThemeToggle from "@/components/ThemeToggle.vue";
import { brandedPageTitle } from "@/brand";
import { ElMessage } from "element-plus";
import { onMounted, reactive } from "vue";
import { useRouter } from "vue-router";
import { authApi } from "@/api/modules";
import { runAction } from "@/features/admin/admin-utils";
import "@/styles/admin-studio.css";
import AdminOverviewPanel from "@/features/admin/AdminOverviewPanel.vue";
import { useAdminOverview } from "@/features/admin/useAdminOverview";
import AdminUsersPanel from "@/features/admin/AdminUsersPanel.vue";
import { useAdminUsers } from "@/features/admin/useAdminUsers";
import AdminWordsPanel from "@/features/admin/AdminWordsPanel.vue";
import { useAdminWords } from "@/features/admin/useAdminWords";
import AdminKnowledgePanel from "@/features/admin/AdminKnowledgePanel.vue";
import { useAdminKnowledge } from "@/features/admin/useAdminKnowledge";
import AdminNewsPanel from "@/features/admin/AdminNewsPanel.vue";
import { useAdminNews } from "@/features/admin/useAdminNews";
import AdminAiPanel from "@/features/admin/AdminAiPanel.vue";
import { useAdminAi } from "@/features/admin/useAdminAi";

const sections = [
  { id: "overview", label: "数据概览" },
  { id: "users", label: "用户管理" },
  { id: "words", label: "词库维护" },
  { id: "knowledge", label: "知识文档" },
  { id: "news", label: "阅读内容" },
  { id: "ai", label: "模型连接" },
];

const adminSectionTitles = {
  overview: "管理概览",
  users: "用户管理",
  words: "词库维护",
  knowledge: "知识库管理",
  news: "阅读内容",
  ai: "模型连接",
};

const dateLabel = new Intl.DateTimeFormat("zh-CN", {
  month: "long",
  day: "numeric",
  weekday: "long",
  timeZone: "Asia/Shanghai",
}).format(new Date());

const router = useRouter();

const state = reactive({
logoutBusy: false,
loading: true,
bootError: "",
user: null,
section: "overview"
});

const overview = useAdminOverview();
const users = useAdminUsers();
const words = useAdminWords();
const knowledge = useAdminKnowledge(overview.loadSummary);
const news = useAdminNews(overview.loadSummary);
const ai = useAdminAi();

const controllers = { overview, users, words, knowledge, news, ai };
const panels = { overview:AdminOverviewPanel, users:AdminUsersPanel, words:AdminWordsPanel, knowledge:AdminKnowledgePanel, news:AdminNewsPanel, ai:AdminAiPanel };

const refreshSummary = overview.refreshSummary;

async function showSection(id) {
  if (!Object.hasOwn(controllers, id)) return;
  state.section = id;
  await runAction({ overview:overview.loadSummary, users:users.loadUsers, words:words.loadWords, knowledge:knowledge.loadKnowledge, news:news.loadNews, ai:ai.loadAiSettings }[id]);
}

async function logout() {
  if (state.logoutBusy) return;
  state.logoutBusy = true;
  try {
    await authApi.logout();
    await router.replace("/login");
  } catch (error) {
    ElMessage.error(error.message || "退出登录失败，请重试");
  } finally {
    state.logoutBusy = false;
  }
}

async function boot() {
  document.body.className = "admin-page";
  state.user = await authApi.me();
  if (state.user?.role !== "ADMIN") {
    router.replace("/");
    return;
  }
  await overview.loadSummary();
  state.loading = false;
}

async function retryBoot() {
  state.loading = true;
  state.bootError = "";
  try {
    await boot();
  } catch (error) {
    state.bootError = error.message || "管理端加载失败，请稍后重试";
    state.loading = false;
  }
}

onMounted(() => { document.title = brandedPageTitle("管理工作台"); retryBoot(); });
</script>

<template>
<div v-if="state.loading" class="admin-loading">
    <AdminSketchIcon name="overview" /><span>正在打开管理工作台…</span>
  </div>
  <div v-else-if="state.bootError" class="admin-loading" role="alert">
    <AdminSketchIcon name="refresh" /><span>{{ state.bootError }}</span
    ><button type="button" @click="retryBoot">重新加载</button>
  </div>
  <div v-else class="app-shell admin-shell">
    <aside class="side-nav" aria-label="管理导航">
      <AppBrand to="/admin" />
      <nav aria-label="后台功能">
        <span
          v-if="state.section !== 'ai'"
          class="nav-active-indicator"
          :style="{
            '--nav-index': sections.findIndex(
              (item) => item.id === state.section,
            ),
            '--nav-section-offset':
              sections.findIndex((item) => item.id === state.section) >= 3
                ? 1
                : 0,
          }"
          aria-hidden="true"
        ></span>
        <button
          v-for="(item, index) in sections.filter((item) => item.id !== 'ai')"
          :key="item.id"
          class="nav-item"
          :class="{
            active: state.section === item.id,
            'nav-group-start': index === 3,
          }"
          :aria-current="state.section === item.id ? 'page' : undefined"
          type="button"
          @click="showSection(item.id)"
        >
          <span class="nav-icon-wrap"><AdminSketchIcon :name="item.id" /></span
          ><span>{{ item.label }}</span>
        </button>
      </nav>
      <div class="sidebar-bottom">
        <button
          class="sidebar-model-config"
          :class="{ 'is-selected': state.section === 'ai' }"
          type="button"
          :aria-current="state.section === 'ai' ? 'page' : undefined"
          @click="showSection('ai')"
        >
          <span class="sidebar-model-icon"><AdminSketchIcon name="ai" /></span
          ><span class="sidebar-model-copy"
            ><strong>模型连接</strong></span
          ><AdminSketchIcon name="arrow" class="sidebar-model-arrow" />
        </button>
      </div>
    </aside>

    <main id="admin-main" class="workspace">
      <header class="workspace-bar">
        <div class="workspace-location">
          <span class="workspace-location-icon" aria-hidden="true"
            ><AdminSketchIcon :name="state.section" /></span
          ><span class="workspace-context"
            ><strong>管理空间</strong></span
          >
        </div>
        <div class="workspace-tools">
          <span class="today-date"
            ><AppIcon name="calendar" />{{ dateLabel }}</span
          ><ThemeToggle /><UserAccountMenu
            :user="state.user || {}"
            :busy="state.logoutBusy"
            portal-path="/"
            portal-label="学习空间"
            @logout="logout"
          />
        </div>
      </header>
      <header class="topbar admin-topbar">
        <div>
          <h1>{{ adminSectionTitles[state.section] }}</h1>
        </div>
        <button
          v-if="state.section === 'overview'"
          class="admin-secondary admin-refresh"
          type="button"
          :disabled="overview.state.summaryBusy"
          @click="refreshSummary"
        >
          <AdminSketchIcon
            name="refresh"
            :class="{ 'is-refreshing': overview.state.summaryBusy }"
          />{{ overview.state.summaryBusy ? "正在更新" : "刷新数据" }}</button
        >
      </header>

      <component
        :is="panels[state.section]"
        :controller="controllers[state.section]"
        @navigate="showSection"
      />

    </main>
  </div>
</template>
