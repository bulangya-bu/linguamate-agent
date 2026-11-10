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
      class="auth-story"
      @pointermove="move"
      @pointerleave="reset"
      @focusin="freeze"
    >
      <AuthAurora />
      <AmbientBackground contained />
      <AppBrand to="/login" variant="auth" />
      <AuthGarden />
      <WordAtmosphere />
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
                    autocapitalize="none" /></span
              ></label>
              <label class="auth-field"
                ><span>密码</span
                ><span class="password-field auth-control"
                  ><AppIcon class="auth-field-icon" name="lock" /><input
                    v-model="loginForm.password"
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
                    v-model.trim="registerForm.username"
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
                    v-model.trim="registerForm.nickname"
                    autocomplete="nickname"
                    :disabled="busy" /></span
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
import AppBrand from "@/components/AppBrand.vue";
import { brandedPageTitle } from "@/brand";
import ThemeToggle from "@/components/ThemeToggle.vue";
import AuthGarden from "@/components/AuthGarden.vue";
import AuthAurora from "@/components/AuthAurora.vue";
import AmbientBackground from "@/components/AmbientBackground.vue";
import WordAtmosphere from "@/components/WordAtmosphere.vue";
import { useArtMotion } from "@/composables/useArtMotion";
import { useAuthParallax } from "@/composables/useAuthParallax";
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
const pageRef = ref(null);
const formActive = ref(false);
const { playing } = useArtMotion(pageRef);
const sceneActive = computed(() => playing.value && !formActive.value);
const { move, reset, freeze } = useAuthParallax(pageRef, sceneActive);
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
  document.title = brandedPageTitle(isLogin.value ? "登录" : "注册");
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
