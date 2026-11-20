import { reactive } from "vue";
import { ElMessage } from "element-plus";
import { aiApi } from "@/api/modules";
import { normalizeModelList } from "./admin-utils";
import { useModelCredential } from "../settings/modelCredential";

export function useAdminAi() {
  const state = reactive({
    ai: { baseUrl: "", apiKey: "", chatModel: "", models: [], status: "未连接" },
    aiBusy: false
  });
  const { keyHint, rememberCredential } = useModelCredential(state.ai);

  async function loadAiSettings() {
    const settings = await aiApi.settings();
    state.ai.baseUrl = settings.baseUrl || "";
    state.ai.apiKey = "";
    state.ai.chatModel = settings.chatModel || "";
    state.ai.models = settings.chatModel ? [settings.chatModel] : [];
    rememberCredential(settings);
    state.ai.status = settings.hasApiKey ? "已配置密钥" : "未配置密钥";
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
    } catch (error) {
      state.ai.status = error.message || "获取失败，请检查连接";
      throw error;
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
      state.ai.baseUrl = settings.baseUrl || "";
      state.ai.chatModel = settings.chatModel || state.ai.chatModel;
      rememberCredential(settings);
      state.ai.status = `已应用 ${state.ai.chatModel || "当前模型"}`;
      ElMessage.success("AI 设置已保存");
    } catch (error) {
      state.ai.status = error.message || "保存失败，请重试";
      throw error;
    } finally {
      state.aiBusy = false;
    }
  }

  return { state, keyHint, loadAiSettings, fetchModels, saveAiSettings };
}
