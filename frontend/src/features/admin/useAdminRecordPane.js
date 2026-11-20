import { computed, nextTick, watch } from "vue";
import { useRoute, useRouter } from "vue-router";

// Phone panels have their own history entries, so the system back button also works.
export function useAdminRecordPane(records, prepareEditor) {
  const route = useRoute();
  const router = useRouter();
  const selected = computed(() => records().find(row => String(row.id) === route.query.record));
  const pane = computed(() => {
    if (route.query.pane === "edit" && (!route.query.record || selected.value)) return "edit";
    if (route.query.pane === "detail" && selected.value) return "detail";
    return "list";
  });

  function location(name, row) {
    const query = { ...route.query };
    delete query.pane;
    delete query.record;
    if (name !== "list") query.pane = name;
    if (row?.id != null) query.record = String(row.id);
    return { path: route.path, query };
  }
  const open = (name, row) => router.push(location(name, row));
  const finish = () => router.replace(location("list"));
  function back() {
    const target = pane.value === "edit" && selected.value ? location("detail", selected.value) : location("list");
    if (window.history.state?.back === router.resolve(target).fullPath) router.back();
    else router.replace(target);
  }
  watch([pane, () => selected.value?.id], async () => {
    if (pane.value === "edit") prepareEditor?.(selected.value);
    await nextTick();
    const backButton = document.querySelector('.admin-mobile-back:not([hidden])');
    if (backButton?.getClientRects().length) backButton.focus({ preventScroll: true });
  }, { immediate: true });
  return { pane, selected, open, finish, back };
}
