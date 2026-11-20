import { reactive, ref, watch } from "vue";
import { aiApi } from "@/api/modules";
import { normalizeModelList } from "@/api/client";

import { normalizeAddress, useModelCredential } from "./modelCredential";

export function useAiSettings() {
  const state = reactive({
    settingsOpen: false,
    ai: {
      baseUrl: "",
      apiKey: "",
      chatModel: "",
      models: [],
      status: "未连接",
    },
    aiBusy: false,
    aiTesting: false,
    aiTestStatus: "",
    aiTestPassed: false,
    aiSaved: { baseUrl: "", chatModel: "", tested: false },
    aiSummary: {
      label: "个人配置",
      modelName: "",
      loaded: false,
      loading: false,
    },
  });

  const settingsPanelRef = ref(null);
  const { keyHint, rememberCredential } = useModelCredential(state.ai);

  const settingsReturnFocus = ref(null);

  function updateAiSummary(settings, tested = false) {
    state.aiSummary.label = settings.chatModel ? (tested ? "连接正常" : "未检测") : "未配置";
    state.aiSummary.modelName = settings.chatModel || "";
    state.aiSummary.loaded = true;
  }

  async function loadAiSummary() {
    if (state.aiSummary.loaded || state.aiSummary.loading) return;
    state.aiSummary.loading = true;
    try {
      updateAiSummary(await aiApi.settings());
    } catch {
      state.aiSummary.label = "查看配置";
    } finally {
      state.aiSummary.loading = false;
    }
  }

  async function openSettings(returnTarget) {
    settingsReturnFocus.value =
      returnTarget instanceof HTMLElement ? returnTarget : null;
    state.settingsOpen = true;
    try {
      const settings = await aiApi.settings();
      const tested = state.aiSaved.tested && matchesSavedSettings(settings);
      state.ai.baseUrl = settings.baseUrl || "";
      state.ai.chatModel = settings.chatModel || "";
      state.ai.apiKey = "";
      state.ai.models = settings.chatModel ? [settings.chatModel] : [];
      rememberCredential(settings);
      state.ai.status = settings.hasApiKey ? "已配置密钥" : "未配置密钥";
      state.aiSaved = {
        baseUrl: settings.baseUrl || "",
        chatModel: settings.chatModel || "",
        tested,
      };
      state.aiTestPassed = tested;
      state.aiTestStatus = tested ? "连接正常" : "";
      updateAiSummary(settings, tested);
    } catch (error) {
      state.ai.status = error.message || "加载设置失败";
    }
  }

  function matchesSavedSettings(settings) {
    return settings.baseUrl === state.aiSaved.baseUrl && settings.chatModel === state.aiSaved.chatModel;
  }

  async function fetchModels() {
    state.aiBusy = true;
    state.ai.status = "正在获取…";
    try {
      const models = await aiApi.models({
        baseUrl: state.ai.baseUrl,
        apiKey: state.ai.apiKey,
      });
      state.ai.models = normalizeModelList(models);
      if (!state.ai.chatModel) state.ai.chatModel = state.ai.models[0] || "";
      state.ai.status = state.ai.models.length
        ? `已获取 ${state.ai.models.length} 个模型`
        : "没有找到可用模型";
    } catch (error) {
      state.ai.status =
        error.message || "获取模型失败，可以直接输入模型名后测试连接";
    } finally {
      state.aiBusy = false;
    }
  }

  async function testAiConnection() {
    if (state.aiBusy) return;
    state.aiBusy = true;
    state.aiTesting = true;
    state.aiTestPassed = false;
    state.aiTestStatus = "测试中…";
    try {
      const result = await aiApi.test({
        baseUrl: state.ai.baseUrl,
        apiKey: state.ai.apiKey,
        chatModel: state.ai.chatModel,
      });
      state.aiTestPassed = true;
      state.aiTestStatus = `连接正常 · ${result.latencyMs} ms`;
      if (!state.ai.apiKey && matchesSavedSettings(state.ai)) {
        state.aiSaved.tested = true;
        updateAiSummary(state.ai, true);
      }
    } catch (error) {
      state.aiTestStatus = error.message || "连接失败，请检查配置";
      if (!state.ai.apiKey && matchesSavedSettings(state.ai)) {
        state.aiSaved.tested = false;
        updateAiSummary(state.ai);
      }
    } finally {
      state.aiBusy = false;
      state.aiTesting = false;
    }
  }

  async function saveAiSettings() {
    if (state.aiBusy) return;
    state.aiBusy = true;
    state.ai.status = "保存中…";
    try {
      const tested = state.aiTestPassed || (!state.ai.apiKey && state.aiSaved.tested && matchesSavedSettings(state.ai));
      const settings = await aiApi.saveSettings({
        baseUrl: state.ai.baseUrl,
        apiKey: state.ai.apiKey,
        chatModel: state.ai.chatModel,
      });
      const effectiveAddress = normalizeAddress(state.ai.baseUrl || state.aiSaved.baseUrl);
      const effectiveModel = state.ai.chatModel.trim() || state.aiSaved.chatModel;
      const unchanged = (settings.baseUrl || "") === effectiveAddress
        && (settings.chatModel || "") === effectiveModel;
      const savedTested = tested && unchanged;
      state.ai.apiKey = "";
      state.ai.baseUrl = settings.baseUrl || "";
      state.ai.chatModel = settings.chatModel || "";
      rememberCredential(settings);
      state.ai.status = "已保存";
      updateAiSummary(settings, savedTested);
      state.aiSaved = {
        baseUrl: settings.baseUrl || "",
        chatModel: settings.chatModel || "",
        tested: savedTested,
      };
      state.aiTestPassed = savedTested;
      state.aiTestStatus = savedTested ? "连接正常" : "";
    } catch (error) {
      state.ai.status = error.message || "保存失败，输入内容已保留，请重试";
    } finally {
      state.aiBusy = false;
    }
  }

  watch(
    () => [state.ai.baseUrl, state.ai.apiKey, state.ai.chatModel],
    () => {
      state.aiTestPassed = false;
      state.aiTestStatus = "";
      if (state.ai.status === "已保存") state.ai.status = "有改动";
    },
    { flush: "sync" },
  );

  return {
    state,
    settingsPanelRef,
    settingsReturnFocus,
    keyHint,
    loadAiSummary,
    openSettings,
    fetchModels,
    testAiConnection,
    saveAiSettings,
  };
}
