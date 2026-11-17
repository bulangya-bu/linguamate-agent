import { nextTick, reactive, watch } from "vue";
import { learningApi } from "@/api/modules";

export function useReplyTranslations(props, thread) {
  const translations = reactive(new Map());
  const expandedTranslations = reactive(new Set());

  watch(
    [() => props.messages, () => props.mode],
    () => expandedTranslations.clear(),
  );

  function canTranslate(message) {
    return props.mode === "practice" &&
      message.role === "assistant" &&
      message.id != null &&
      Boolean(message.coaching) &&
      /[A-Za-z]/.test(message.content);
  }

  async function revealTranslation(id) {
    await nextTick();
    const viewport = thread.value;
    if (!viewport?.clientHeight || !expandedTranslations.has(id)) return;
    const panel = viewport.ownerDocument.getElementById(`reply-translation-${id}`);
    const actions = panel?.previousElementSibling;
    if (!actions || !viewport.contains(panel)) return;

    const bounds = viewport.getBoundingClientRect();
    const actionBounds = actions.getBoundingClientRect();
    if (actionBounds.bottom <= bounds.top || actionBounds.top >= bounds.bottom) return;
    const overflow = panel.getBoundingClientRect().bottom - bounds.bottom + 8;
    const available = actionBounds.top - bounds.top - 8;
    const offset = Math.min(overflow, available);
    if (offset > 0) viewport.scrollTop += offset;
  }

  async function loadTranslation(id) {
    if (translations.get(id)?.status === "loading") return;
    translations.set(id, { status: "loading" });
    try {
      const result = await learningApi.translateChatReply(id);
      const text = result?.translation?.trim();
      if (!text) throw new Error("暂未获得中文译文，请重试。");
      translations.set(id, { status: "ready", text });
    } catch (error) {
      translations.set(id, {
        status: "error",
        message: error.message || "暂时无法翻译，请稍后重试。",
      });
    }
    await revealTranslation(id);
  }

  function toggleTranslation(id) {
    if (expandedTranslations.delete(id)) return;
    expandedTranslations.add(id);
    if (!translations.has(id)) loadTranslation(id);
    revealTranslation(id);
  }

  return { translations, expandedTranslations, canTranslate, toggleTranslation, loadTranslation };
}
