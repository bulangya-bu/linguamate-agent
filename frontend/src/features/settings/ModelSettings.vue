<script setup>
import AppIcon from "@/components/AppIcon.vue";

const props = defineProps({
  settings: Object,
});
const {
  state: settingsState,
  settingsPanelRef,
  keyHint,
  openSettings,
  fetchModels,
  testAiConnection,
  saveAiSettings,
} = props.settings;
</script>

<template>
  <button
    class="settings-fab desktop-account-legacy"
    type="button"
    aria-label="我的 API 设置"
    title="我的 API 设置"
    @click="openSettings"
  >
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path
        d="M19.14 12.94c.04-.31.06-.63.06-.94s-.02-.63-.06-.94l2.03-1.58a.5.5 0 0 0 .12-.64l-1.92-3.32a.5.5 0 0 0-.6-.22l-2.39.96a7.1 7.1 0 0 0-1.63-.94l-.36-2.54A.5.5 0 0 0 13.9 1h-3.8a.5.5 0 0 0-.49.41l-.36 2.54c-.58.22-1.12.53-1.63.94l-2.39-.96a.5.5 0 0 0-.6.22L2.71 7.47a.5.5 0 0 0 .12.64l2.03 1.58c-.04.31-.06.63-.06.94s.02.63.06.94L2.83 13.15a.5.5 0 0 0-.12.64l1.92 3.32a.5.5 0 0 0 .6.22l2.39-.96c.5.41 1.05.73 1.63.94l.36 2.54a.5.5 0 0 0 .49.41h3.8a.5.5 0 0 0 .49-.41l.36-2.54c.58-.22 1.12-.53 1.63-.94l2.39.96a.5.5 0 0 0 .6-.22l1.92-3.32a.5.5 0 0 0-.12-.64l-2.03-1.58ZM12 15.2A3.2 3.2 0 1 1 12 8.8a3.2 3.2 0 0 1 0 6.4Z"
      />
    </svg>
  </button>
  <div
    class="settings-backdrop"
    :class="{ 'is-open': settingsState.settingsOpen }"
    aria-hidden="true"
    @click="settingsState.settingsOpen = false"
  ></div>
  <aside
    ref="settingsPanelRef"
    :inert="!settingsState.settingsOpen"
    class="settings-panel"
    :class="{ 'is-open': settingsState.settingsOpen }"
    role="dialog"
    aria-modal="true"
    aria-labelledby="settingsTitle"
    :aria-hidden="String(!settingsState.settingsOpen)"
  >
    <div class="settings-panel-head">
      <div>
        <h2 id="settingsTitle">模型连接</h2>
      </div>
      <button
        class="panel-close"
        type="button"
        aria-label="关闭设置"
        @click="settingsState.settingsOpen = false"
      >
        <AppIcon name="close" />
      </button>
    </div>
    <div class="settings-form">
      <label class="settings-field">
        <span>模型地址</span>
        <input
          v-model="settingsState.ai.baseUrl"
          :disabled="settingsState.aiBusy"
          autocomplete="off"
          placeholder="https://api.openai.com/v1"
        />
      </label>
      <label class="settings-field">
        <span>API 密钥</span>
        <input
          v-model="settingsState.ai.apiKey"
          :disabled="settingsState.aiBusy"
          type="password"
          autocomplete="off"
          :placeholder="keyHint"
        />
      </label>
      <div class="settings-model-row">
        <label class="settings-field">
          <span>模型名</span>
          <input
            v-model.trim="settingsState.ai.chatModel"
            list="available-ai-models"
            placeholder="模型名称"
            :disabled="settingsState.aiBusy"
          />
        </label>
        <datalist id="available-ai-models">
          <option v-for="model in settingsState.ai.models" :key="model" :value="model" />
        </datalist>
        <button
          class="settings-secondary"
          type="button"
          :disabled="settingsState.aiBusy || !settingsState.ai.baseUrl.trim()"
          @click="fetchModels"
        >获取模型</button>
      </div>
      <div class="settings-actions">
        <button
          class="settings-secondary"
          type="button"
          :disabled="settingsState.aiBusy || !settingsState.ai.baseUrl.trim() || !settingsState.ai.chatModel"
          @click="testAiConnection"
        >
          {{ settingsState.aiTesting ? "测试中…" : "测试连接" }}
        </button>
        <button
          class="settings-primary"
          type="button"
          :disabled="settingsState.aiBusy || !settingsState.ai.baseUrl.trim()"
          @click="saveAiSettings"
        >
          保存
        </button>
      </div>
      <div class="settings-feedback" role="status">
        <span>{{ settingsState.ai.status }}</span>
        <span v-if="settingsState.aiTestStatus" :class="{ connected: settingsState.aiTestPassed }">{{ settingsState.aiTestStatus }}</span>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.settings-model-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: end;
  gap: 8px;
}
.settings-model-row > button { align-self: end; min-height: 40px; }
.settings-feedback {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 14px;
  min-height: 1.4em;
  color: var(--muted);
  font-size: 13px;
}
.settings-feedback .connected { color: var(--green-ink); }
</style>
