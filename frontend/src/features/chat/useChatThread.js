import { computed, nextTick, shallowRef, useTemplateRef, watch } from "vue";

const FOLLOW_DISTANCE = 72;

export function useChatThread(props, emit) {
  const thread = useTemplateRef("thread");
  const input = useTemplateRef("input");
  const coachScroll = useTemplateRef("coachScroll");
  const pinnedIndex = shallowRef(null);
  const selectedIndex = computed(
    () => pinnedIndex.value ?? props.messages.findLastIndex((message) => message.coaching),
  );
  const showPendingFeedback = computed(() => props.busy && pinnedIndex.value === null);
  let sendActivator = null;

  function focusInput() {
    input.value?.focus({ preventScroll: true });
  }

  function selectFeedback(index) {
    if (!props.messages[index]?.coaching) return;
    pinnedIndex.value = index;
    if (coachScroll.value) coachScroll.value.scrollTop = 0;
  }

  function prepareSend() {
    pinnedIndex.value = null;
    sendActivator = input.value?.ownerDocument.activeElement;
  }

  function sendMessage() {
    if (props.busy || !props.draft.trim()) return;
    prepareSend();
    emit("send");
  }

  function submitPrompt(prompt) {
    if (props.busy) return;
    prepareSend();
    emit("prompt", prompt);
  }

  function handleKeydown(event) {
    if (event.key === "Enter" && !event.shiftKey && !event.isComposing) {
      event.preventDefault();
      sendMessage();
    }
  }

  watch(
    [() => props.messages, () => props.messages.length, () => props.busy, () => props.mode],
    async ([messages, count, busy, mode], [previous, previousCount, wasBusy, previousMode], onCleanup) => {
      const historyChanged = messages !== previous || mode !== previousMode;
      const sent = count > previousCount && messages[count - 1]?.role === "user";
      const viewport = thread.value;
      const nearBottom = !viewport ||
        viewport.scrollHeight - viewport.clientHeight - viewport.scrollTop <= FOLLOW_DISTANCE;
      const follow = historyChanged || sent || nearBottom;
      const activator = wasBusy && !busy ? sendActivator : null;

      if (historyChanged || sent) pinnedIndex.value = null;
      if (activator) sendActivator = null;

      let current = true;
      onCleanup(() => { current = false; });
      await nextTick();
      if (!current) return;

      if (follow && thread.value) thread.value.scrollTop = thread.value.scrollHeight;
      if (historyChanged && coachScroll.value) coachScroll.value.scrollTop = 0;

      const ownerDocument = input.value?.ownerDocument;
      if (
        activator &&
        nearBottom &&
        viewport?.clientHeight &&
        ownerDocument &&
        (ownerDocument.activeElement === ownerDocument.body ||
          ownerDocument.activeElement === activator)
      ) {
        focusInput();
      }
    },
    { immediate: true },
  );

  watch(selectedIndex, () => {
    if (coachScroll.value) coachScroll.value.scrollTop = 0;
  }, { flush: "post" });

  return {
    thread,
    input,
    coachScroll,
    selectedIndex,
    showPendingFeedback,
    selectFeedback,
    focusInput,
    sendMessage,
    submitPrompt,
    handleKeydown,
  };
}
