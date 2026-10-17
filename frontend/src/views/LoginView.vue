<template>
  <main class="auth-page">
    <section class="auth-story">
      <RouterLink class="brand" to="/login"
        ><img class="brand-mark" src="/assets/linguamate-icon.svg" alt="" />
        <div>
          <strong>LinguaMate<span class="brand-period">.</span></strong
          ><span>让英语成为日常</span>
        </div></RouterLink
      >
      <div class="auth-story-copy">
        <span class="small-label">A SPACE FOR YOUR NEXT CHAPTER</span>
        <h1>A little.<br /><em>Every day.</em></h1>
        <p>每一次开口，每一个新词。<br />慢慢来，也能走很远。</p>
      </div>
      <div class="auth-bloom-wrap">
        <LearningBloom /><span class="auth-orbit-label"
          >SPEAK · DISCOVER · GROW</span
        >
      </div>
      <footer class="auth-story-footer">
        <span>你的英语学习空间</span><span>EST. 2026</span>
      </footer>
    </section>
    <section class="auth-form-side">
      <div class="auth-card">
        <div class="auth-card-head">
          <span class="eyebrow">{{
            isLogin ? "GOOD TO SEE YOU AGAIN" : "YOUR NEXT CHAPTER"
          }}</span>
          <h2>{{ isLogin ? "欢迎回来。" : "从这里开始。" }}</h2>
          <p>
            {{
              isLogin
                ? "今天，也给英语留一点时间。"
                : "创建账号，记录每一次进步。"
            }}
          </p>
        </div>
        <div class="auth-mode-switch" aria-label="账号入口">
          <RouterLink
            class="auth-mode-pill"
            :class="{ 'is-active': isLogin }"
            :aria-current="isLogin ? 'page' : undefined"
            to="/login"
            >登录</RouterLink
          ><RouterLink
            class="auth-mode-pill"
            :class="{ 'is-active': !isLogin }"
            :aria-current="!isLogin ? 'page' : undefined"
            to="/register"
            >创建账号</RouterLink
          >
        </div>
        <form v-if="isLogin" class="auth-form" @submit.prevent="submitLogin">
          <label class="auth-field"
            ><span>用户名</span
            ><input
              v-model.trim="loginForm.username"
              required
              autocomplete="username"
              placeholder="输入用户名"
          /></label>
          <label class="auth-field"
            ><span>密码</span
            ><span class="password-field"
              ><input
                v-model="loginForm.password"
                required
                :type="showPassword ? 'text' : 'password'"
                autocomplete="current-password"
                placeholder="输入密码"
              /><button
                type="button"
                :aria-label="showPassword ? '隐藏密码' : '显示密码'"
                :aria-pressed="showPassword"
                @click="showPassword = !showPassword"
              >
                {{ showPassword ? "隐藏" : "显示" }}
              </button></span
            ></label
          >
          <button class="auth-primary" type="submit" :disabled="busy">
            {{ busy ? "正在进入..." : "进入学习空间"
            }}<AppIcon v-if="!busy" name="arrow" /><span
              v-else
              class="quiz-ai-spinner"
            ></span>
          </button>
        </form>
        <form v-else class="auth-form" @submit.prevent="submitRegister">
          <label class="auth-field"
            ><span>用户名</span
            ><input
              v-model.trim="registerForm.username"
              required
              minlength="3"
              maxlength="32"
              autocomplete="username"
              placeholder="3–32 位，作为登录账号"
          /></label>
          <label class="auth-field"
            ><span>昵称</span
            ><input
              v-model.trim="registerForm.nickname"
              autocomplete="nickname"
              placeholder="希望我们怎么称呼你"
          /></label>
          <label class="auth-field"
            ><span>密码</span
            ><span class="password-field"
              ><input
                v-model="registerForm.password"
                required
                minlength="6"
                :type="showPassword ? 'text' : 'password'"
                autocomplete="new-password"
                placeholder="至少 6 位"
              /><button
                type="button"
                :aria-label="showPassword ? '隐藏密码' : '显示密码'"
                :aria-pressed="showPassword"
                @click="showPassword = !showPassword"
              >
                {{ showPassword ? "隐藏" : "显示" }}
              </button></span
            ></label
          >
          <button class="auth-primary" type="submit" :disabled="busy">
            {{ busy ? "正在创建..." : "开启我的学习旅程"
            }}<AppIcon v-if="!busy" name="arrow" /><span
              v-else
              class="quiz-ai-spinner"
            ></span>
          </button>
        </form>
        <p
          class="settings-status auth-status"
          role="status"
          aria-live="polite"
          :class="{ 'vue-error': statusType === 'error' }"
        >
          {{ status }}
        </p>
        <div class="auth-footnote">
          <AppIcon name="vocabulary" /><span
            >对话 · 词汇 · 写作 · 阅读<br /><small
              >你的学习记录，会在这里好好保存。</small
            ></span
          >
        </div>
      </div>
      <span class="auth-bottom-wordmark">SMALL STEPS. REAL PROGRESS.</span>
    </section>
  </main>
</template>
<script setup>
import AppIcon from "@/components/AppIcon.vue";
import LearningBloom from "@/components/LearningBloom.vue";
import {
  computed,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref,
  watch,
} from "vue";
import { useRoute, useRouter } from "vue-router";
import { authApi } from "@/api/modules";

const route = useRoute();
const router = useRouter();
const busy = ref(false);
const showPassword = ref(false);
const status = ref("");
const statusType = ref("");

const loginForm = reactive({
  username: "student",
  password: "student123",
});

const registerForm = reactive({
  username: "",
  nickname: "",
  password: "",
});

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
  showPassword.value = false;
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
