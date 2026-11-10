import { onBeforeUnmount } from "vue";
import { ElMessage } from "element-plus";
export { formatDate, normalizeModelList } from "@/api/client";

export const number = (value) => (Number(value) || 0).toLocaleString("zh-CN");
export const difficultyLabel = (value) =>
  ({ easy: "简单", medium: "进阶", hard: "挑战" })[value] || value || "未分类";

export async function runAction(action) {
  try {
    await action();
  } catch (error) {
    if (error === "cancel" || error === "close") return;
    ElMessage.error(error.message || "操作未完成，请稍后重试");
  }
}

export function useAdminSearch(action) {
  let timer;
  onBeforeUnmount(() => clearTimeout(timer));
  return () => { clearTimeout(timer); timer = setTimeout(action, 300); };
}
