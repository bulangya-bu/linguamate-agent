<template>
  <main
    ref="pageRef"
    class="auth-page auth-page--garden"
    :class="{
      'is-register': !isLogin,
      'is-motion-paused': !sceneActive,
      'is-form-active': formActive,
    }"
  >
    <section
      ref="storyRef"
      class="auth-story"
      data-cursor-trail="paper"
      @pointermove="move"
      @pointerleave="reset"
      @pointercancel="reset"
      @focusin="freeze"
    >
      <AuthAurora v-if="!isPhone" />
      <AmbientBackground v-if="!isPhone" contained :quiet="formActive" />
      <AppBrand to="/login" variant="auth" />
      <span v-if="isPhone" class="auth-pocket-paper" aria-hidden="true"><i></i></span>
      <template v-else><AuthGarden /><WordAtmosphere /></template>
    </section>
    <section
      class="auth-form-side"
      @focusin="formActive = true"
      @focusout="
        formActive = $event.currentTarget.contains($event.relatedTarget)
      "
    >
      <ThemeToggle class="auth-theme-toggle" />
      <div class="auth-card">
        <div class="auth-card-head">
          <div class="auth-entry-mark" aria-hidden="true">
            <AppIcon name="spark" /><span></span>
          </div>
          <h2>{{ isLogin ? "账号登录" : "创建账号" }}</h2>
        </div>
        <div class="auth-mode-switch" aria-label="账号入口">
          <span class="auth-mode-indicator" aria-hidden="true"></span>
          <RouterLink
            class="auth-mode-pill"
            :class="{ 'is-active': isLogin }"
            :aria-current="isLogin ? 'page' : undefined"
            :aria-disabled="busy || undefined"
            @click.capture="guardModeSwitch"
            :to="{ path: '/login', query: returnQuery }"
            >登录</RouterLink
          ><RouterLink
            class="auth-mode-pill"
            :class="{ 'is-active': !isLogin }"
            :aria-current="!isLogin ? 'page' : undefined"
            :aria-disabled="busy || undefined"
            @click.capture="guardModeSwitch"
            :to="{ path: '/register', query: returnQuery }"
            >创建账号</RouterLink
          >
        </div>
        <div class="auth-form-stage">
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
                  name="username"
                  required
                  autocomplete="username"
                  :disabled="busy"
                  spellcheck="false"
                  autocapitalize="none" /></span
            ></label>
            <label class="auth-field"
              ><span>密码</span
              ><span class="password-field auth-control"
                ><AppIcon class="auth-field-icon" name="lock" /><input
                  name="password"
                  required
                  :type="showPassword ? 'text' : 'password'"
                  autocomplete="current-password"
                  :disabled="busy" /><button
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
                  name="username"
                  required
                  minlength="3"
                  maxlength="32"
                  autocomplete="username"
                  :disabled="busy"
                  spellcheck="false"
                  autocapitalize="none"
                  placeholder="3–32 位" /></span
            ></label>
            <label class="auth-field"
              ><span>昵称<small>选填</small></span
              ><span class="auth-control"
                ><AppIcon class="auth-field-icon" name="smile" /><input
                  name="nickname"
                  maxlength="32"
                  autocomplete="nickname"
                  :disabled="busy" /></span
            ></label>
            <label class="auth-field"
              ><span>密码</span
              ><span class="password-field auth-control"
                ><AppIcon class="auth-field-icon" name="lock" /><input
                  name="password"
                  required
                  minlength="6"
                  maxlength="64"
                  :type="showPassword ? 'text' : 'password'"
                  autocomplete="new-password"
                  @input="clearConfirmationError"
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
            <label class="auth-field">
              <span>确认密码</span>
              <span class="auth-control">
                <AppIcon class="auth-field-icon" name="lock" />
                <input
                  name="passwordConfirmation"
                  required
                  minlength="6"
                  maxlength="64"
                  :type="showPassword ? 'text' : 'password'"
                  autocomplete="new-password"
                  :aria-invalid="confirmationInvalid || undefined"
                  :aria-describedby="confirmationInvalid ? 'auth-form-status' : undefined"
                  :disabled="busy"
                  @input="clearConfirmationError"
                />
              </span>
            </label>
            <button class="auth-primary" type="submit" :disabled="busy">
              {{ busy ? "正在创建..." : "开启我的学习旅程"
              }}<AppIcon v-if="!busy" name="arrow" /><span
                v-else
                class="quiz-ai-spinner"
              ></span>
            </button>
          </form>
          <p
            id="auth-form-status"
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
import AppBrand from "@/components/AppBrand.vue";
import { brandedPageTitle } from "@/brand";
import ThemeToggle from "@/components/ThemeToggle.vue";
import AuthGarden from "@/components/AuthGarden.vue";
import AuthAurora from "@/components/AuthAurora.vue";
import AmbientBackground from "@/components/AmbientBackground.vue";
import WordAtmosphere from "@/components/WordAtmosphere.vue";
import { useArtMotion } from "@/composables/useArtMotion";
import { usePhoneLayout } from "@/composables/usePhoneLayout";
import { useAuthParallax } from "@/composables/useAuthParallax";
import {
  computed,
  onBeforeUnmount,
  onMounted,
  ref,
  watch,
} from "vue";
import { useRoute, useRouter } from "vue-router";
import { authApi } from "@/api/modules";
import { ApiError } from "@/api/client";

const route = useRoute();
const router = useRouter();
const pageRef = ref(null);
const storyRef = ref(null);
const isPhone = usePhoneLayout();
const formActive = ref(false);
const { playing } = useArtMotion(pageRef);
const sceneActive = computed(() => playing.value && !formActive.value);
const { move, reset, freeze } = useAuthParallax(storyRef, playing, formActive);
const busy = ref(false);
const showPassword = ref(false);
const status = ref("");
const statusType = ref("");
const confirmationInvalid = ref(false);
let authViewVersion = 0;

const isLogin = computed(() => route.meta.mode !== "register");
const returnQuery = computed(() => typeof route.query.next === "string" ? { next: route.query.next } : {});

function goAfterLogin(user) {
  if (user.role === "ADMIN") return router.replace("/admin");
  const next = returnQuery.value.next;
  if (next?.startsWith("/") && !next.startsWith("//") && !next.includes("\\")) {
    const target = router.resolve(next);
    if (target.meta.section) return router.replace(target.fullPath);
  }
  return router.replace("/");
}

async function checkExistingSession() {
  const version = authViewVersion;
  try {
    const user = await authApi.me({ redirectOnUnauthorized: false, redirectOnForbidden: false });
    if (user && version === authViewVersion && !busy.value) await goAfterLogin(user);
  } catch {
    // Not logged in is the expected state for this page.
  }
}

function guardModeSwitch(event) {
  if (busy.value) event.preventDefault();
}

function setAuthError(message) {
  status.value = message;
  statusType.value = "error";
}

function clearConfirmationError() {
  if (!confirmationInvalid.value) return;
  confirmationInvalid.value = false;
  status.value = "";
  statusType.value = "";
}

async function submitAuth(request, payload, action) {
  if (busy.value) return;
  const version = authViewVersion;
  busy.value = true;
  status.value = `正在${action}…`;
  statusType.value = "";
  try {
    const user = await request(payload);
    if (version !== authViewVersion) return;
    if (!user?.id) {
      setAuthError(`${action}未完成，请重试`);
      return;
    }
    await goAfterLogin(user);
  } catch (error) {
    if (version !== authViewVersion) return;
    const userMessage = error instanceof ApiError && error.status < 500
      ? error.message
      : "暂时无法连接服务，请稍后重试";
    setAuthError(userMessage);
  } finally {
    busy.value = false;
  }
}

function submitLogin(event) {
  if (busy.value) return;
  const form = new FormData(event.currentTarget);
  const username = form.get("username").trim();
  if (!username) {
    setAuthError("请输入用户名");
    return;
  }
  return submitAuth(authApi.login, { username, password: form.get("password") }, "登录");
}

function submitRegister(event) {
  if (busy.value) return;
  const element = event.currentTarget;
  const form = new FormData(element);
  const username = form.get("username").trim();
  const password = form.get("password");
  if (username.length < 3) {
    setAuthError("用户名长度应为 3–32 位");
    element.elements.username.focus();
    return;
  }
  if (password !== form.get("passwordConfirmation")) {
    confirmationInvalid.value = true;
    setAuthError("两次输入的密码不一致");
    element.elements.passwordConfirmation.focus();
    return;
  }
  return submitAuth(authApi.register, { username, nickname: form.get("nickname").trim(), password }, "注册");
}

function syncAuthTitle() {
  document.title = brandedPageTitle(isLogin.value ? "登录" : "注册");
}

watch(isLogin, () => {
  authViewVersion += 1;
  showPassword.value = false;
  confirmationInvalid.value = false;
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
  authViewVersion += 1;
  document.body.classList.remove("auth-body");
});
</script>
