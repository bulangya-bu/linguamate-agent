import { reactive } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { adminApi } from "@/api/modules";
import { runAction, useAdminSearch } from "./admin-utils";

const emptyWord = () => ({
  id: "",
  word: "",
  meaningCn: "",
  partOfSpeech: "",
  levelTag: "",
  phoneticUs: "",
  phoneticUk: "",
  exampleEn: "",
  exampleCn: "",
});

export function useAdminWords() {
  const state = reactive({
    wordSearch: "",
    wordPage: { words: [], total: 0, page: 0, size: 20 },
    wordForm: emptyWord()
  });

  async function loadWords(page = state.wordPage.page) {
    state.wordPage = await adminApi.words({
      page,
      size: state.wordPage.size,
      search: state.wordSearch,
    });
  }

  function fillWordForm(word) {
    Object.assign(state.wordForm, emptyWord(), word || {});
  }

  function clearWordForm() {
    Object.assign(state.wordForm, emptyWord());
  }

  async function saveWord() {
    const payload = { ...state.wordForm };
    const id = payload.id;
    delete payload.id;
    if (id) await adminApi.updateWord(id, payload);
    else await adminApi.createWord(payload);
    ElMessage.success(id ? "单词已更新" : "单词已新增");
    clearWordForm();
    await loadWords();
  }

  async function deleteWord(word) {
    await ElMessageBox.confirm(`确认删除 ${word.word}？`, "删除单词", {
      type: "warning",
    });
    await adminApi.deleteWord(word.id);
    ElMessage.success("单词已删除");
    await loadWords();
  }

  const debouncedLoadWords = useAdminSearch(() => runAction(() => loadWords(0)));

  return { state, loadWords, fillWordForm, clearWordForm, saveWord, deleteWord, debouncedLoadWords };
}
