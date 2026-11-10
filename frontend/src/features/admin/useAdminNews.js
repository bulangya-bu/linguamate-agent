import { reactive } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { adminApi } from "@/api/modules";
import { runAction, useAdminSearch } from "./admin-utils";

const emptyNews = () => ({
  id: "",
  title: "",
  summary: "",
  content: "",
  sourceName: "",
  difficulty: "",
  topicTags: "",
});

export function useAdminNews(loadSummary) {
  const state = reactive({
    newsSearch: "",
    newsPage: { articles: [], total: 0, page: 0, size: 20 },
    newsForm: emptyNews()
  });

  async function loadNews(page = state.newsPage.page) {
    state.newsPage = await adminApi.news({
      page,
      size: state.newsPage.size,
      search: state.newsSearch,
    });
  }

  function fillNewsForm(article) {
    Object.assign(state.newsForm, emptyNews(), article || {});
  }

  function clearNewsForm() {
    Object.assign(state.newsForm, emptyNews());
  }

  async function saveNews() {
    const payload = { ...state.newsForm };
    const id = payload.id;
    delete payload.id;
    if (id) await adminApi.updateNews(id, payload);
    else await adminApi.createNews(payload);
    ElMessage.success(id ? "阅读文章已更新" : "阅读文章已新增");
    clearNewsForm();
    await Promise.all([loadNews(), loadSummary()]);
  }

  async function deleteNews(article) {
    await ElMessageBox.confirm(`确认删除《${article.title}》？`, "删除文章", {
      type: "warning",
    });
    await adminApi.deleteNews(article.id);
    ElMessage.success("文章已删除");
    await Promise.all([loadNews(), loadSummary()]);
  }

  const debouncedLoadNews = useAdminSearch(() => runAction(() => loadNews(0)));

  return { state, loadNews, fillNewsForm, clearNewsForm, saveNews, deleteNews, debouncedLoadNews };
}
