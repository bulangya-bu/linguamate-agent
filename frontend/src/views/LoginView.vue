<template>
  <main class="auth-page">
    <div class="auth-bubble-field" aria-hidden="true">
      <span
        v-for="bubble in authBubbles"
        :key="bubble.word"
        class="auth-word-bubble"
        :class="`tone-${bubble.tone}`"
        :style="{
          '--x': `${bubble.x}%`,
          '--y': `${bubble.y}%`,
          '--size': `${bubble.size}px`,
          '--delay': `${bubble.delay}s`,
          '--duration': `${bubble.duration}s`
        }"
      >
        {{ bubble.word }}
      </span>
    </div>
    <section class="auth-shell">
      <div class="auth-brand-panel">
        <div class="auth-brand-lockup">
          <img src="/assets/linguamate-icon.svg" alt="" />
          <div>
            <strong>LinguaMate</strong>
            <span>英语学习助手</span>
          </div>
        </div>
        <p class="eyebrow">LinguaMate Agent</p>
        <h1>{{ isLogin ? "先登录，再开始今天的英语路线" : "创建账号，保存你的学习进度" }}</h1>
        <p class="subtitle">
          {{ isLogin ? "学习记录会绑定到你的账号，管理员账号会进入独立后台。" : "新账号默认进入学习端，等级、收藏、复习和模型接口都会独立保存。" }}
        </p>
        <div class="auth-path" aria-label="学习路径">
          <span v-for="step in featureSteps" :key="step.label" :class="`node-${step.tone}`">
            <small>{{ step.en }}</small>
            {{ step.label }}
          </span>
        </div>
      </div>

      <section class="auth-card panel">
        <div class="auth-card-head">
          <span>{{ isLogin ? "Welcome back" : "New journey" }}</span>
          <h2>{{ isLogin ? "登录账号" : "注册账号" }}</h2>
        </div>
        <div class="auth-mode-switch">
          <RouterLink class="auth-mode-pill" :class="{ 'is-active': isLogin }" to="/login">登录</RouterLink>
          <RouterLink class="auth-mode-pill" :class="{ 'is-active': !isLogin }" to="/register">注册</RouterLink>
        </div>

        <form v-if="isLogin" class="auth-form" @submit.prevent="submitLogin">
          <label class="auth-field">
            <span>用户名</span>
            <input v-model.trim="loginForm.username" autocomplete="username" placeholder="输入用户名" />
          </label>
          <label class="auth-field">
            <span>密码</span>
            <input v-model="loginForm.password" type="password" autocomplete="current-password" placeholder="输入密码" />
          </label>
          <button class="auth-primary" type="submit" :disabled="busy">
            {{ busy ? "正在登录..." : "登录" }}
          </button>
        </form>

        <form v-else class="auth-form" @submit.prevent="submitRegister">
          <label class="auth-field">
            <span>用户名</span>
            <input v-model.trim="registerForm.username" autocomplete="username" placeholder="3-32 位" />
          </label>
          <label class="auth-field">
            <span>昵称</span>
            <input v-model.trim="registerForm.nickname" autocomplete="nickname" placeholder="显示在主页" />
          </label>
          <label class="auth-field">
            <span>密码</span>
            <input v-model="registerForm.password" type="password" autocomplete="new-password" placeholder="至少 6 位" />
          </label>
          <button class="auth-primary" type="submit" :disabled="busy">
            {{ busy ? "正在创建..." : "注册并进入" }}
          </button>
        </form>

        <p class="settings-status auth-status" :class="{ 'vue-error': statusType === 'error' }">{{ status }}</p>
      </section>
    </section>
  </main>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { authApi } from "@/api/modules";

const route = useRoute();
const router = useRouter();
const busy = ref(false);
const status = ref("");
const statusType = ref("");

const loginForm = reactive({
  username: "student",
  password: "student123"
});

const registerForm = reactive({
  username: "",
  nickname: "",
  password: ""
});

const featureSteps = [
  { label: "对话", en: "Speak", tone: "green" },
  { label: "背词", en: "Words", tone: "blue" },
  { label: "写作", en: "Essay", tone: "amber" },
  { label: "阅读", en: "Read", tone: "coral" }
];

const authBubbles = [
  { word: "context", tone: "green", x: 8, y: 18, size: 82, delay: -2, duration: 18 },
  { word: "review", tone: "blue", x: 22, y: 78, size: 72, delay: -7, duration: 20 },
  { word: "essay", tone: "amber", x: 38, y: 12, size: 66, delay: -4, duration: 19 },
  { word: "speak", tone: "coral", x: 52, y: 84, size: 84, delay: -12, duration: 22 },
  { word: "word", tone: "green", x: 68, y: 16, size: 70, delay: -9, duration: 17 },
  { word: "grammar", tone: "blue", x: 82, y: 70, size: 90, delay: -3, duration: 24 },
  { word: "fluent", tone: "amber", x: 92, y: 30, size: 74, delay: -15, duration: 21 },
  { word: "listen", tone: "coral", x: 14, y: 48, size: 64, delay: -11, duration: 18 },
  { word: "phrase", tone: "blue", x: 72, y: 48, size: 62, delay: -6, duration: 20 },
  { word: "focus", tone: "green", x: 44, y: 54, size: 76, delay: -14, duration: 23 }
];

const isLogin = computed(() => route.meta.mode !== "register");

function goAfterLogin(user) {
  router.replace(user?.role === "ADMIN" ? "/admin" : "/");
}

async function checkExistingSession() {
  try {
    const user = await authApi.me({ redirectOnUnauthorized: false });
    if (user) goAfterLogin(user);
  } catch {
    // Not logged in is the expected state for this page.
  }
}

async function submitLogin() {
  busy.value = true;
  status.value = "正在登录...";
  statusType.value = "";
  try {
    const user = await authApi.login({ ...loginForm });
    goAfterLogin(user);
  } catch (error) {
    status.value = error.message || "登录失败";
    statusType.value = "error";
  } finally {
    busy.value = false;
  }
}

async function submitRegister() {
  busy.value = true;
  status.value = "正在创建账号...";
  statusType.value = "";
  try {
    const user = await authApi.register({ ...registerForm });
    goAfterLogin(user);
  } catch (error) {
    status.value = error.message || "注册失败";
    statusType.value = "error";
  } finally {
    busy.value = false;
  }
}

function syncAuthTitle() {
  document.title = isLogin.value ? "LinguaMate 登录" : "LinguaMate 注册";
}

watch(isLogin, () => {
  status.value = "";
  statusType.value = "";
  syncAuthTitle();
});

onMounted(() => {
  document.body.className = "auth-body";
  syncAuthTitle();
  checkExistingSession();
});

onBeforeUnmount(() => {
  document.body.classList.remove("auth-body");
});
</script>
