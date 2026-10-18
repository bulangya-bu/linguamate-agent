import { computed, onBeforeUnmount, onMounted, ref } from "vue";

// Idle motion sleeps in hidden sections and tabs; interaction remains available.
export function useArtMotion(element) {
  const visible = ref(false);
  const tabVisible = ref(!document.hidden);
  const preference = window.matchMedia("(prefers-reduced-motion: reduce)");
  const reducedMotion = ref(preference.matches);
  const playing = computed(
    () => visible.value && tabVisible.value && !reducedMotion.value,
  );
  let observer;
  const updateTab = () => {
    tabVisible.value = !document.hidden;
  };
  const updatePreference = () => {
    reducedMotion.value = preference.matches;
  };
  onMounted(() => {
    observer = new IntersectionObserver(
      ([entry]) => {
        visible.value = entry.isIntersecting;
      },
      { threshold: 0.05 },
    );
    if (element.value) observer.observe(element.value);
    document.addEventListener("visibilitychange", updateTab);
    preference.addEventListener("change", updatePreference);
  });
  onBeforeUnmount(() => {
    observer?.disconnect();
    document.removeEventListener("visibilitychange", updateTab);
    preference.removeEventListener("change", updatePreference);
  });
  return { playing, reducedMotion };
}
