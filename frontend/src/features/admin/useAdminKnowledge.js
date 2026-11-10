import { reactive } from "vue";
import { ElMessage } from "element-plus";
import { adminApi } from "@/api/modules";



export function useAdminKnowledge(loadSummary) {
  const state = reactive({
    knowledge: [],
    knowledgeForm: { title: "", sourceType: "", tags: "", content: "" }
  });

  async function loadKnowledge() {
    state.knowledge = await adminApi.knowledge();
  }

  async function uploadKnowledge() {
    await adminApi.uploadKnowledge({ ...state.knowledgeForm });
    state.knowledgeForm = { title: "", sourceType: "", tags: "", content: "" };
    ElMessage.success("知识已导入");
    await Promise.all([loadKnowledge(), loadSummary()]);
  }

  return { state, loadKnowledge, uploadKnowledge };
}
