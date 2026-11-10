import { ElMessage } from "element-plus";
import { reactive, ref, watch } from "vue";
import { aiApi } from "@/api/modules";
import { normalizeModelList } from "@/api/client";

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
    aiSaved: { baseUrl: "", chatModel: "" },
    aiSummary: {
      label: "个人配置",
      modelName: "",
      loaded: false,
      loading: false,
    },
  });

  const settingsPanelRef = ref(null);

  const settingsReturnFocus = ref(null);

  function updateAiSummary(settings) {
    state.aiSummary.label = settings.chatModel ? "未检测" : "未配置";
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
      state.ai.baseUrl = settings.baseUrl || "";
      state.ai.chatModel = settings.chatModel || "";
      state.ai.apiKey = "";
      state.ai.models = settings.chatModel ? [settings.chatModel] : [];
      state.ai.status = settings.hasApiKey ? "已保存密钥" : "未保存密钥";
      state.aiSaved = {
        baseUrl: settings.baseUrl || "",
        chatModel: settings.chatModel || "",
      };
      updateAiSummary(settings);
    } catch (error) {
      state.ai.status = error.message || "加载设置失败";
    }
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
    state.ai.status = "正在发送实际模型请求…";
    try {
      const result = await aiApi.test({
        baseUrl: state.ai.baseUrl,
        apiKey: state.ai.apiKey,
        chatModel: state.ai.chatModel,
      });
      state.ai.status = `${result.modelName} 连接正常 · ${result.latencyMs} ms（当前表单）`;
      if (
        !state.ai.apiKey &&
        state.ai.baseUrl === state.aiSaved.baseUrl &&
        state.ai.chatModel === state.aiSaved.chatModel
      )
        state.aiSummary.label = "连接正常";
    } catch (error) {
      state.ai.status = error.message || "连接失败，请检查配置后重试";
    } finally {
      state.aiBusy = false;
      state.aiTesting = false;
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
      updateAiSummary(settings);
      state.aiSaved = {
        baseUrl: settings.baseUrl || "",
        chatModel: settings.chatModel || "",
      };
      ElMessage.success("AI 设置已保存");
    } catch (error) {
      state.ai.status = error.message || "保存失败，输入内容已保留，请重试";
    } finally {
      state.aiBusy = false;
    }
  }

  watch(
    () => [state.ai.baseUrl, state.ai.apiKey, state.ai.chatModel],
    () => {
      if (!state.aiBusy && state.ai.status.includes("连接正常"))
        state.ai.status = "配置已修改，请重新测试连接";
    },
  );

  return {
    state,
    settingsPanelRef,
    settingsReturnFocus,
    loadAiSummary,
    openSettings,
    fetchModels,
    testAiConnection,
    saveAiSettings,
  };
}
