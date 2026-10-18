<template>
  <main
    class="auth-page auth-page--garden"
    :class="{ 'is-register': !isLogin }"
  >
    <section class="auth-story">
      <RouterLink class="brand" to="/login"
        ><img
          class="brand-mark"
          src="/assets/linguamate-icon.svg?v=bright"
          alt=""
        />
        <div>
          <strong>LinguaMate<span class="brand-period">.</span></strong
          ><span>让英语成为日常</span>
        </div></RouterLink
      >
      <div class="auth-story-copy">
        <h1>
          A little.<br /><em
            >Every day.<svg viewBox="0 0 440 25" fill="none" aria-hidden="true">
              <path
                d="M4 17C106 3 261 3 432 10M41 23C168 9 299 9 397 16"
              /></svg
          ></em>
        </h1>
        <p>每一次开口，每一个新词。<br />慢慢来，也能走很远。</p>
      </div>
      <AuthGarden />
      <WordAtmosphere />
    </section>
    <section class="auth-form-side">
      <ThemeToggle class="auth-theme-toggle" />
      <div class="auth-card">
        <div class="auth-card-head">
          <div class="auth-entry-mark" aria-hidden="true">
            <AppIcon name="spark" /><span></span>
          </div>
          <h2>{{ isLogin ? "欢迎回来" : "从这里开始" }}<span>。</span></h2>
          <p>
            {{
              isLogin
                ? "新的一天，和英语再近一点。"
                : "让每一次小小的进步，都被记住。"
            }}
          </p>
        </div>
        <div class="auth-mode-switch" aria-label="账号入口">
          <span class="auth-mode-indicator" aria-hidden="true"></span>
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
        <div class="auth-form-stage">
          <Transition name="auth-form-swap" mode="out-in">
            <form
              v-if="isLogin"
              key="login"
              class="auth-form"
              :aria-busy="busy"
              @submit.prevent="submitLogin"
            >
              <label class="auth-field"
                ><span>用户名</span
                ><span class="auth-control"
                  ><AppIcon class="auth-field-icon" name="person" /><input
                    v-model.trim="loginForm.username"
                    required
                    autocomplete="username"
                    :disabled="busy"
                    spellcheck="false"
                    autocapitalize="none"
                    placeholder="输入用户名" /></span
              ></label>
              <label class="auth-field"
                ><span>密码</span
                ><span class="password-field auth-control"
                  ><AppIcon class="auth-field-icon" name="lock" /><input
                    v-model="loginForm.password"
                    required
                    :type="showPassword ? 'text' : 'password'"
                    autocomplete="current-password"
                    :disabled="busy"
                    placeholder="输入密码" /><button
                    type="button"
                    :disabled="busy"
                    :aria-label="showPassword ? '隐藏密码' : '显示密码'"
                    :aria-pressed="showPassword"
                    @click="showPassword = !showPassword"
                  >
                    <AppIcon
                      :name="showPassword ? 'eye-off' : 'eye'"
                    /></button></span
              ></label>
              <button class="auth-primary" type="submit" :disabled="busy">
                {{ busy ? "正在进入..." : "进入学习空间"
                }}<AppIcon v-if="!busy" name="arrow" /><span
                  v-else
                  class="quiz-ai-spinner"
                ></span>
              </button>
            </form>
            <form
              v-else
              key="register"
              class="auth-form"
              :aria-busy="busy"
              @submit.prevent="submitRegister"
            >
              <label class="auth-field"
                ><span>用户名</span
                ><span class="auth-control"
                  ><AppIcon class="auth-field-icon" name="person" /><input
                    v-model.trim="registerForm.username"
                    required
                    minlength="3"
                    maxlength="32"
                    autocomplete="username"
                    :disabled="busy"
                    spellcheck="false"
                    autocapitalize="none"
                    placeholder="3–32 位，作为登录账号" /></span
              ></label>
              <label class="auth-field"
                ><span>昵称<small>选填</small></span
                ><span class="auth-control"
                  ><AppIcon class="auth-field-icon" name="smile" /><input
                    v-model.trim="registerForm.nickname"
                    autocomplete="nickname"
                    :disabled="busy"
                    placeholder="希望我们怎么称呼你" /></span
              ></label>
              <label class="auth-field"
                ><span>密码</span
                ><span class="password-field auth-control"
                  ><AppIcon class="auth-field-icon" name="lock" /><input
                    v-model="registerForm.password"
                    required
                    minlength="6"
                    :type="showPassword ? 'text' : 'password'"
                    autocomplete="new-password"
                    :disabled="busy"
                    placeholder="至少 6 位" /><button
                    type="button"
                    :disabled="busy"
                    :aria-label="showPassword ? '隐藏密码' : '显示密码'"
                    :aria-pressed="showPassword"
                    @click="showPassword = !showPassword"
                  >
                    <AppIcon
                      :name="showPassword ? 'eye-off' : 'eye'"
                    /></button></span
              ></label>
              <button class="auth-primary" type="submit" :disabled="busy">
                {{ busy ? "正在创建..." : "开启我的学习旅程"
                }}<AppIcon v-if="!busy" name="arrow" /><span
                  v-else
                  class="quiz-ai-spinner"
                ></span>
              </button>
            </form>
          </Transition>
          <p
            class="settings-status auth-status"
            role="status"
            aria-live="polite"
            :class="{ 'vue-error': statusType === 'error' }"
          >
            {{ status }}
          </p>
        </div>
      </div>
    </section>
  </main>
</template>
<script setup>
import AppIcon from "@/components/AppIcon.vue";
import ThemeToggle from "@/components/ThemeToggle.vue";
import AuthGarden from "@/components/AuthGarden.vue";
import WordAtmosphere from "@/components/WordAtmosphere.vue";
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
  if (busy.value) return;
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
  if (busy.value) return;
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
