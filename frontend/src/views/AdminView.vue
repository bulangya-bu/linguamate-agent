<template>
  <div v-if="state.loading" class="admin-loading">正在加载管理控制台...</div>
  <div v-else class="app-shell admin-shell">
    <aside class="side-nav" aria-label="管理导航">
      <RouterLink class="brand" to="/admin">
        <img
          class="brand-mark"
          src="/assets/linguamate-icon.svg?v=bright"
          alt="LinguaMate 图标"
        />
        <div>
          <strong>LinguaMate<span class="brand-period">.</span></strong>
          <span>管理工作台</span>
        </div>
      </RouterLink>
      <nav>
        <span
          class="nav-active-indicator"
          :style="{
            '--nav-index': sections.findIndex(
              (item) => item.id === state.section,
            ),
          }"
          aria-hidden="true"
        ></span>
        <button
          v-for="item in sections"
          :key="item.id"
          class="nav-item"
          :class="{ active: state.section === item.id }"
          :aria-current="state.section === item.id ? 'page' : undefined"
          type="button"
          @click="showSection(item.id)"
        >
          <AppIcon :name="item.id" /><span>{{ item.label }}</span>
        </button>
      </nav>
      <div class="nav-account desktop-account-legacy">
        <div class="user-chip">
          <span>{{
            state.user?.nickname || state.user?.username || "管理员"
          }}</span>
          <small>ADMIN</small>
        </div>
        <RouterLink class="nav-link-button" to="/">用户端</RouterLink>
        <button class="nav-link-button danger" type="button" @click="logout">
          退出
        </button>
      </div>
    </aside>

    <main class="workspace">
      <header class="workspace-bar">
        <div class="workspace-location">
          管理空间 <span>/</span
          ><strong>{{
            sections.find((item) => item.id === state.section)?.label
          }}</strong>
        </div>
        <RouterLink class="text-button desktop-account-legacy" to="/"
          >前往学习空间<AppIcon name="arrow"
        /></RouterLink>
        <div class="workspace-tools">
          <UserAccountMenu
            :user="state.user || {}"
            :busy="state.logoutBusy"
            :show-model-settings="false"
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
        <span class="admin-role-badge"
          ><span class="status-dot"></span>管理员</span
        >
      </header>

      <section
        id="overview"
        class="section"
        :class="{ active: state.section === 'overview' }"
      >
        <div class="admin-summary-grid">
          <div class="admin-overview">
            <section class="admin-kpi-band">
              <div class="admin-kpi-primary">
                <span>今日学习概览</span>
                <strong>{{ state.summary.todayWordReviews || 0 }}</strong>
                <small>今日背词次数</small>
              </div>
              <div class="admin-kpi-metrics">
                <article class="admin-kpi-mini">
                  <span>用户总数</span
                  ><strong>{{ state.summary.users || 0 }}</strong>
                </article>
                <article class="admin-kpi-mini">
                  <span>活跃用户</span
                  ><strong>{{ state.summary.activeUsers || 0 }}</strong>
                </article>
                <article class="admin-kpi-mini">
                  <span>今日学习用户</span
                  ><strong>{{ state.summary.todayLearners || 0 }}</strong>
                </article>
              </div>
              <div class="admin-kpi-progress">
                <div>
                  <span>活跃率 {{ activePercent }}%</span>
                  <span
                    >今日覆盖单词
                    {{ state.summary.todayUniqueWords || 0 }}</span
                  >
                </div>
                <i :style="{ width: activePercent + '%' }"></i>
              </div>
            </section>
            <section class="admin-overview-grid">
              <article class="admin-chart-block admin-overview-chart">
                <div class="admin-chart-heading">
                  <strong>近 7 天背词趋势</strong>
                  <span>所有用户</span>
                </div>
                <div class="admin-column-chart">
                  <div
                    v-for="item in state.summary.dailyWordStats || []"
                    :key="item.date"
                    class="admin-column-item"
                  >
                    <div class="admin-column-track">
                      <i :style="{ height: dailyHeight(item.words) + '%' }"></i>
                    </div>
                    <strong>{{ item.words || 0 }}</strong>
                    <span>{{ shortDate(item.date) }}</span>
                  </div>
                </div>
              </article>
              <article class="admin-chart-block admin-overview-chart">
                <div class="admin-chart-heading">
                  <strong>用户状态占比</strong>
                  <span
                    >{{ state.summary.activeUsers || 0 }} /
                    {{ state.summary.users || 0 }}</span
                  >
                </div>
                <div class="admin-pie-layout">
                  <div class="admin-pie" :style="{ '--value': activePercent }">
                    <strong>{{ activePercent }}%</strong>
                    <span>活跃</span>
                  </div>
                  <div class="admin-pie-legend">
                    <p>
                      <i class="tone-green"></i>活跃用户
                      {{ state.summary.activeUsers || 0 }}
                    </p>
                    <p>
                      <i class="tone-muted"></i>非活跃用户 {{ inactiveUsers }}
                    </p>
                    <p>
                      <i class="tone-blue"></i>今日覆盖单词
                      {{ state.summary.todayUniqueWords || 0 }}
                    </p>
                  </div>
                </div>
              </article>
            </section>
          </div>
        </div>
      </section>

      <section
        id="users"
        class="section"
        :class="{ active: state.section === 'users' }"
      >
        <div class="admin-two-column">
          <section class="panel">
            <div class="panel-heading">
              <h2>用户管理</h2>
              <input
                v-model.trim="state.userSearch"
                class="admin-search"
                placeholder="搜索用户名或昵称"
                @input="debouncedLoadUsers"
              />
            </div>
            <div class="admin-table">
              <div class="admin-row admin-row-head">
                <span>用户</span><span>角色</span><span>状态</span
                ><span>操作</span>
              </div>
              <div v-for="user in state.users" :key="user.id" class="admin-row">
                <span
                  ><strong>{{ user.nickname || user.username }}</strong
                  ><small
                    >{{ user.username }} ·
                    {{ formatDate(user.createdAt) }}</small
                  ></span
                >
                <span>{{ user.role }}</span>
                <span
                  ><mark :class="user.status === 'ACTIVE' ? 'ok' : 'danger'">{{
                    user.status
                  }}</mark></span
                >
                <span class="admin-actions">
                  <button type="button" @click="loadUserProfile(user.id)">
                    详情
                  </button>
                  <button type="button" @click="editUser(user)">编辑</button>
                  <button type="button" @click="toggleUserStatus(user)">
                    {{ user.status === "ACTIVE" ? "禁用" : "启用" }}
                  </button>
                  <button type="button" @click="resetUserPassword(user)">
                    重置密码
                  </button>
                </span>
              </div>
            </div>
          </section>
          <section class="panel admin-profile-panel">
            <h2>用户数据详情</h2>
            <div v-if="state.userProfile" class="admin-profile">
              <div class="admin-profile-head">
                <div>
                  <strong>{{
                    state.userProfile.nickname || state.userProfile.username
                  }}</strong>
                  <small
                    >{{ state.userProfile.username }} ·
                    {{ state.userProfile.role }} ·
                    {{ state.userProfile.status }}</small
                  >
                </div>
                <mark
                  :class="state.userProfile.aiConfigured ? 'ok' : 'danger'"
                  >{{
                    state.userProfile.aiConfigured ? "已配置 AI" : "未配置 AI"
                  }}</mark
                >
              </div>
              <div class="admin-profile-grid">
                <div>
                  <strong>Lv.{{ state.userProfile.level || 1 }}</strong
                  ><span>等级</span>
                </div>
                <div>
                  <strong>{{ state.userProfile.experience || 0 }}</strong
                  ><span>当前经验</span>
                </div>
                <div>
                  <strong>{{ state.userProfile.learnedWords || 0 }}</strong
                  ><span>已背单词</span>
                </div>
                <div>
                  <strong>{{ state.userProfile.favoriteWords || 0 }}</strong
                  ><span>收藏单词</span>
                </div>
                <div>
                  <strong>{{ state.userProfile.masteredWords || 0 }}</strong
                  ><span>已掌握</span>
                </div>
                <div>
                  <strong>{{ state.userProfile.reviewWords || 0 }}</strong
                  ><span>错词</span>
                </div>
                <div>
                  <strong>{{ state.userProfile.essays || 0 }}</strong
                  ><span>作文</span>
                </div>
                <div>
                  <strong>{{ state.userProfile.readings || 0 }}</strong
                  ><span>阅读</span>
                </div>
                <div>
                  <strong>{{ state.userProfile.conversations || 0 }}</strong
                  ><span>会话</span>
                </div>
              </div>
              <section class="admin-readings">
                <div class="admin-chart-heading">
                  <strong>阅读记录</strong>
                  <span>最近 {{ state.userReadings.length }} 条</span>
                </div>
                <div class="admin-table compact">
                  <div class="admin-row reading-row admin-row-head">
                    <span>文章</span><span>进度</span><span>查词</span
                    ><span>时间</span>
                  </div>
                  <div
                    v-for="record in state.userReadings"
                    :key="record.id"
                    class="admin-row reading-row"
                  >
                    <span
                      ><strong>{{ record.title }}</strong
                      ><small>{{ record.difficulty }}</small></span
                    >
                    <span
                      ><mark :class="record.finished ? 'ok' : ''"
                        >{{ record.readProgress || 0 }}%</mark
                      ></span
                    >
                    <span>{{ record.lookedUpWords || 0 }}</span>
                    <span>{{
                      formatDate(record.updatedAt || record.createdAt)
                    }}</span>
                  </div>
                </div>
              </section>
            </div>
            <div v-else class="admin-profile empty-state">
              选择一个用户查看学习数据汇总。
            </div>
          </section>
        </div>
      </section>

      <section
        id="words"
        class="section"
        :class="{ active: state.section === 'words' }"
      >
        <div class="admin-two-column">
          <section class="panel">
            <div class="panel-heading">
              <h2>词库维护</h2>
              <input
                v-model.trim="state.wordSearch"
                class="admin-search"
                placeholder="搜索英文或中文"
                @input="debouncedLoadWords"
              />
            </div>
            <div class="admin-table">
              <div class="admin-row admin-row-head">
                <span>单词</span><span>释义</span><span>分类</span
                ><span>操作</span>
              </div>
              <div
                v-for="word in state.wordPage.words"
                :key="word.id"
                class="admin-row"
              >
                <span
                  ><strong>{{ word.word }}</strong
                  ><small>{{ word.partOfSpeech }}</small></span
                >
                <span>{{ word.meaningCn }}</span>
                <span>{{ word.levelTag }}</span>
                <span class="admin-actions">
                  <button type="button" @click="fillWordForm(word)">
                    编辑
                  </button>
                  <button type="button" @click="deleteWord(word)">删除</button>
                </span>
              </div>
              <div class="admin-pagination">
                <button
                  type="button"
                  :disabled="state.wordPage.page <= 0"
                  @click="loadWords(state.wordPage.page - 1)"
                >
                  上一页
                </button>
                <span
                  >第 {{ state.wordPage.page + 1 }} 页 / 共
                  {{ state.wordPage.total }} 条</span
                >
                <button
                  type="button"
                  :disabled="
                    (state.wordPage.page + 1) * state.wordPage.size >=
                    state.wordPage.total
                  "
                  @click="loadWords(state.wordPage.page + 1)"
                >
                  下一页
                </button>
              </div>
            </div>
          </section>
          <section class="panel">
            <h2>新增/编辑单词</h2>
            <form class="admin-form" @submit.prevent="saveWord">
              <input v-model="state.wordForm.word" placeholder="word" />
              <input
                v-model="state.wordForm.meaningCn"
                placeholder="中文释义"
              />
              <input v-model="state.wordForm.partOfSpeech" placeholder="词性" />
              <input
                v-model="state.wordForm.levelTag"
                placeholder="分类，如 cet4-bank"
              />
              <input
                v-model="state.wordForm.phoneticUs"
                placeholder="美式音标"
              />
              <input
                v-model="state.wordForm.phoneticUk"
                placeholder="英式音标"
              />
              <textarea
                v-model="state.wordForm.exampleEn"
                placeholder="英文例句"
              ></textarea>
              <textarea
                v-model="state.wordForm.exampleCn"
                placeholder="中文例句"
              ></textarea>
              <div class="settings-row">
                <button class="settings-primary" type="submit">保存单词</button>
                <button
                  class="settings-secondary"
                  type="button"
                  @click="clearWordForm"
                >
                  清空
                </button>
              </div>
            </form>
          </section>
        </div>
      </section>

      <section
        id="knowledge"
        class="section"
        :class="{ active: state.section === 'knowledge' }"
      >
        <div class="admin-two-column">
          <section class="panel">
            <h2>知识库文档</h2>
            <div class="admin-table">
              <div class="admin-row admin-row-head">
                <span>标题</span><span>类型</span><span>标签</span>
              </div>
              <div
                v-for="doc in state.knowledge"
                :key="doc.id || doc.title"
                class="admin-row"
              >
                <span
                  ><strong>{{ doc.title }}</strong
                  ><small>{{ doc.createdAt }}</small></span
                >
                <span>{{ doc.sourceType }}</span>
                <span>{{ doc.tags }}</span>
              </div>
            </div>
          </section>
          <section class="panel">
            <h2>导入知识</h2>
            <form class="admin-form" @submit.prevent="uploadKnowledge">
              <input v-model="state.knowledgeForm.title" placeholder="标题" />
              <input
                v-model="state.knowledgeForm.sourceType"
                placeholder="类型，如 grammar / rubric"
              />
              <input
                v-model="state.knowledgeForm.tags"
                placeholder="标签，逗号分隔"
              />
              <textarea
                v-model="state.knowledgeForm.content"
                placeholder="知识内容"
              ></textarea>
              <button class="settings-primary" type="submit">导入并切分</button>
            </form>
          </section>
        </div>
      </section>

      <section
        id="news"
        class="section"
        :class="{ active: state.section === 'news' }"
      >
        <div class="admin-two-column">
          <section class="panel">
            <div class="panel-heading">
              <h2>阅读文章</h2>
              <input
                v-model.trim="state.newsSearch"
                class="admin-search"
                placeholder="搜索标题、来源或标签"
                @input="debouncedLoadNews"
              />
            </div>
            <div class="admin-table">
              <div class="admin-row admin-row-head">
                <span>标题</span><span>难度</span><span>来源</span
                ><span>操作</span>
              </div>
              <div
                v-for="article in state.newsPage.articles"
                :key="article.id"
                class="admin-row"
              >
                <span
                  ><strong>{{ article.title }}</strong
                  ><small>{{ article.topicTags }}</small></span
                >
                <span>{{ article.difficulty }}</span>
                <span>{{ article.sourceName }}</span>
                <span class="admin-actions">
                  <button type="button" @click="fillNewsForm(article)">
                    编辑
                  </button>
                  <button type="button" @click="deleteNews(article)">
                    删除
                  </button>
                </span>
              </div>
              <div class="admin-pagination">
                <button
                  type="button"
                  :disabled="state.newsPage.page <= 0"
                  @click="loadNews(state.newsPage.page - 1)"
                >
                  上一页
                </button>
                <span
                  >第 {{ state.newsPage.page + 1 }} 页 / 共
                  {{ state.newsPage.total }} 条</span
                >
                <button
                  type="button"
                  :disabled="
                    (state.newsPage.page + 1) * state.newsPage.size >=
                    state.newsPage.total
                  "
                  @click="loadNews(state.newsPage.page + 1)"
                >
                  下一页
                </button>
              </div>
            </div>
          </section>
          <section class="panel">
            <h2>新增/编辑阅读</h2>
            <form class="admin-form" @submit.prevent="saveNews">
              <input v-model="state.newsForm.title" placeholder="文章标题" />
              <input v-model="state.newsForm.sourceName" placeholder="来源" />
              <input
                v-model="state.newsForm.difficulty"
                placeholder="难度，如 easy / medium / hard"
              />
              <input
                v-model="state.newsForm.topicTags"
                placeholder="标签，逗号分隔"
              />
              <textarea
                v-model="state.newsForm.summary"
                placeholder="摘要"
              ></textarea>
              <textarea
                v-model="state.newsForm.content"
                class="admin-tall-textarea"
                placeholder="正文内容"
              ></textarea>
              <div class="settings-row">
                <button class="settings-primary" type="submit">保存阅读</button>
                <button
                  class="settings-secondary"
                  type="button"
                  @click="clearNewsForm"
                >
                  清空
                </button>
              </div>
            </form>
          </section>
        </div>
      </section>

      <section
        id="ai"
        class="section"
        :class="{ active: state.section === 'ai' }"
      >
        <section class="panel admin-ai-panel">
          <div class="settings-panel-head">
            <div>
              <h2>模型连接</h2>
              <p class="settings-description">仅应用于当前管理员账号</p>
            </div>
            <span class="settings-status">{{ state.ai.status }}</span>
          </div>
          <div class="settings-form">
            <label class="settings-field">
              <span>模型地址</span>
              <input
                v-model="state.ai.baseUrl"
                autocomplete="off"
                placeholder="https://api.openai.com/v1"
              />
            </label>
            <label class="settings-field">
              <span>API 密钥</span>
              <input
                v-model="state.ai.apiKey"
                type="password"
                autocomplete="off"
                placeholder="留空则沿用已保存密钥"
              />
            </label>
            <div class="settings-row">
              <button
                class="settings-secondary"
                type="button"
                :disabled="state.aiBusy"
                @click="fetchModels"
              >
                获取模型
              </button>
              <label class="settings-field admin-model-select">
                <span>模型名</span>
                <select
                  v-model="state.ai.chatModel"
                  :disabled="!state.ai.models.length"
                >
                  <option
                    v-for="model in state.ai.models"
                    :key="model"
                    :value="model"
                  >
                    {{ model }}
                  </option>
                </select>
              </label>
            </div>
            <div class="settings-actions">
              <button
                class="settings-primary"
                type="button"
                :disabled="state.aiBusy"
                @click="saveAiSettings"
              >
                保存并应用
              </button>
            </div>
          </div>
        </section>
      </section>
    </main>
  </div>
</template>

<script setup>
import AppIcon from "@/components/AppIcon.vue";
import UserAccountMenu from "@/components/UserAccountMenu.vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { computed, onMounted, reactive } from "vue";
import { useRouter } from "vue-router";
import { adminApi, aiApi, authApi } from "@/api/modules";
import { formatDate, normalizeModelList } from "@/api/client";

const router = useRouter();

const sections = [
  { id: "overview", label: "概览" },
  { id: "users", label: "用户" },
  { id: "words", label: "词库" },
  { id: "knowledge", label: "知识库" },
  { id: "news", label: "阅读" },
  { id: "ai", label: "AI 设置" },
];

const adminSectionTitles = {
  overview: "学习概览",
  users: "用户管理",
  words: "词库维护",
  knowledge: "知识库管理",
  news: "阅读内容",
  ai: "模型连接",
};
const emptyWord = () => ({
  id: "",
  word: "",
  meaningCn: "",
  partOfSpeech: "",
  levelTag: "",
  phoneticUs: "",
  phoneticUk: "",
  exampleEn: "",
  exampleCn: "",
});
const emptyNews = () => ({
  id: "",
  title: "",
  summary: "",
  content: "",
  sourceName: "",
  difficulty: "",
  topicTags: "",
});

const state = reactive({
  logoutBusy: false,
  loading: true,
  user: null,
  section: "overview",
  summary: {},
  users: [],
  userSearch: "",
  userProfile: null,
  userReadings: [],
  wordSearch: "",
  wordPage: { words: [], total: 0, page: 0, size: 20 },
  wordForm: emptyWord(),
  knowledge: [],
  knowledgeForm: { title: "", sourceType: "", tags: "", content: "" },
  newsSearch: "",
  newsPage: { articles: [], total: 0, page: 0, size: 20 },
  newsForm: emptyNews(),
  ai: { baseUrl: "", apiKey: "", chatModel: "", models: [], status: "未连接" },
  aiBusy: false,
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
const maxDailyWords = computed(() =>
  Math.max(
    1,
    ...(state.summary.dailyWordStats || []).map(
      (item) => Number(item.words) || 0,
    ),
  ),
);

function debounce(fn, wait = 300) {
  let timer;
  return (...args) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(...args), wait);
  };
}

const debouncedLoadUsers = debounce(() => loadUsers());
const debouncedLoadWords = debounce(() => loadWords(0));
const debouncedLoadNews = debounce(() => loadNews(0));

function dailyHeight(words) {
  return Math.max(
    4,
    Math.round(((Number(words) || 0) / maxDailyWords.value) * 100),
  );
}

function shortDate(value) {
  const parts = String(value || "").split("-");
  return parts.length === 3 ? `${Number(parts[1])}/${Number(parts[2])}` : value;
}

async function showSection(id) {
  state.section = id;
  if (id === "overview") await loadSummary();
  if (id === "users") await loadUsers();
  if (id === "words") await loadWords();
  if (id === "knowledge") await loadKnowledge();
  if (id === "news") await loadNews();
  if (id === "ai") await loadAiSettings();
}

async function logout() {
  if (state.logoutBusy) return;
  state.logoutBusy = true;
  try {
    await authApi.logout();
    await router.replace("/login");
  } catch (error) {
    ElMessage.error(error.message || "退出登录失败，请重试");
  } finally { state.logoutBusy = false; }
}

async function loadSummary() {
  state.summary = await adminApi.summary();
}

async function loadUsers() {
  state.users = await adminApi.users(state.userSearch);
}

async function loadUserProfile(id) {
  const [profile, readings] = await Promise.all([
    adminApi.userProfile(id),
    adminApi.userReadings(id),
  ]);
  state.userProfile = profile;
  state.userReadings = readings || [];
}

async function editUser(user) {
  if (user.role === "ADMIN") {
    ElMessage.warning("管理员账号不在这里编辑");
    return;
  }
  const { value } = await ElMessageBox.prompt("输入新的昵称", "编辑用户", {
    inputValue: user.nickname || user.username,
    confirmButtonText: "保存",
    cancelButtonText: "取消",
  });
  await adminApi.updateUser(user.id, { nickname: value });
  ElMessage.success("用户昵称已更新");
  await loadUsers();
  if (state.userProfile?.id === user.id) await loadUserProfile(user.id);
}

async function toggleUserStatus(user) {
  if (user.role === "ADMIN") {
    ElMessage.warning("管理员账号不能禁用");
    return;
  }
  const status = user.status === "ACTIVE" ? "DISABLED" : "ACTIVE";
  await adminApi.updateUserStatus(user.id, status);
  ElMessage.success(status === "ACTIVE" ? "用户已启用" : "用户已禁用");
  await loadUsers();
}

async function resetUserPassword(user) {
  if (user.role === "ADMIN") {
    ElMessage.warning("管理员密码不在这里重置");
    return;
  }
  const { value } = await ElMessageBox.prompt(
    "输入新密码，至少 6 位",
    "重置密码",
    {
      inputValue: "student123",
      confirmButtonText: "重置",
      cancelButtonText: "取消",
    },
  );
  await adminApi.resetPassword(user.id, value);
  ElMessage.success("密码已重置");
}

async function loadWords(page = state.wordPage.page) {
  state.wordPage = await adminApi.words({
    page,
    size: state.wordPage.size,
    search: state.wordSearch,
  });
}

function fillWordForm(word) {
  Object.assign(state.wordForm, emptyWord(), word || {});
}

function clearWordForm() {
  Object.assign(state.wordForm, emptyWord());
}

async function saveWord() {
  const payload = { ...state.wordForm };
  const id = payload.id;
  delete payload.id;
  if (id) await adminApi.updateWord(id, payload);
  else await adminApi.createWord(payload);
  ElMessage.success(id ? "单词已更新" : "单词已新增");
  clearWordForm();
  await loadWords();
}

async function deleteWord(word) {
  await ElMessageBox.confirm(`确认删除 ${word.word}？`, "删除单词", {
    type: "warning",
  });
  await adminApi.deleteWord(word.id);
  ElMessage.success("单词已删除");
  await loadWords();
}

async function loadKnowledge() {
  state.knowledge = await adminApi.knowledge();
}

async function uploadKnowledge() {
  await adminApi.uploadKnowledge({ ...state.knowledgeForm });
  state.knowledgeForm = { title: "", sourceType: "", tags: "", content: "" };
  ElMessage.success("知识已导入");
  await Promise.all([loadKnowledge(), loadSummary()]);
}

async function loadNews(page = state.newsPage.page) {
  state.newsPage = await adminApi.news({
    page,
    size: state.newsPage.size,
    search: state.newsSearch,
  });
}

function fillNewsForm(article) {
  Object.assign(state.newsForm, emptyNews(), article || {});
}

function clearNewsForm() {
  Object.assign(state.newsForm, emptyNews());
}

async function saveNews() {
  const payload = { ...state.newsForm };
  const id = payload.id;
  delete payload.id;
  if (id) await adminApi.updateNews(id, payload);
  else await adminApi.createNews(payload);
  ElMessage.success(id ? "阅读文章已更新" : "阅读文章已新增");
  clearNewsForm();
  await Promise.all([loadNews(), loadSummary()]);
}

async function deleteNews(article) {
  await ElMessageBox.confirm(`确认删除《${article.title}》？`, "删除文章", {
    type: "warning",
  });
  await adminApi.deleteNews(article.id);
  ElMessage.success("文章已删除");
  await Promise.all([loadNews(), loadSummary()]);
}

async function loadAiSettings() {
  const settings = await aiApi.settings();
  state.ai.baseUrl = settings.baseUrl || "";
  state.ai.apiKey = "";
  state.ai.chatModel = settings.chatModel || "";
  state.ai.models = settings.chatModel ? [settings.chatModel] : [];
  state.ai.status = settings.hasApiKey ? "已保存密钥" : "未保存密钥";
}

async function fetchModels() {
  state.aiBusy = true;
  state.ai.status = "正在获取模型...";
  try {
    const models = await aiApi.models({
      baseUrl: state.ai.baseUrl,
      apiKey: state.ai.apiKey,
    });
    state.ai.models = normalizeModelList(models);
    state.ai.chatModel = state.ai.models.includes(state.ai.chatModel)
      ? state.ai.chatModel
      : state.ai.models[0] || state.ai.chatModel;
    state.ai.status = `已获取 ${state.ai.models.length} 个模型`;
  } finally {
    state.aiBusy = false;
  }
}

async function saveAiSettings() {
  state.aiBusy = true;
  state.ai.status = "正在保存...";
  try {
    const settings = await aiApi.saveSettings({
      baseUrl: state.ai.baseUrl,
      apiKey: state.ai.apiKey,
      chatModel: state.ai.chatModel,
    });
    state.ai.apiKey = "";
    state.ai.chatModel = settings.chatModel || state.ai.chatModel;
    state.ai.status = `已应用 ${state.ai.chatModel || "当前模型"}`;
    ElMessage.success("AI 设置已保存");
  } finally {
    state.aiBusy = false;
  }
}

async function boot() {
  document.body.className = "admin-page";
  state.user = await authApi.me();
  if (state.user?.role !== "ADMIN") {
    router.replace("/");
    return;
  }
  await Promise.all([
    loadSummary(),
    loadUsers(),
    loadWords(),
    loadKnowledge(),
    loadNews(),
  ]);
  state.loading = false;
}

onMounted(() => {
  document.title = "LinguaMate 管理端";
  boot().catch((error) => {
    console.error(error);
    state.loading = false;
    ElMessage.error(error.message || "管理端加载失败");
  });
});
</script>
