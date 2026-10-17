import { computed, ref } from "vue";

const storageKey = "linguamate-theme";
const theme = ref(document.documentElement.dataset.theme === "dark" ? "dark" : "light");

function applyTheme(value) {
  theme.value = value === "dark" ? "dark" : "light";
  document.documentElement.dataset.theme = theme.value;
  document.documentElement.style.colorScheme = theme.value;
  document.querySelector('meta[name="theme-color"]')?.setAttribute("content", theme.value === "dark" ? "#101923" : "#58cc02");
}

export function initializeTheme() {
  applyTheme(theme.value);
  window.addEventListener("storage", (event) => {
    if (event.key === storageKey || event.key === null) applyTheme(event.newValue);
  });
}

export function useTheme() {
  return {
    isDark: computed(() => theme.value === "dark"),
    toggleTheme() {
      applyTheme(theme.value === "dark" ? "light" : "dark");
      try { localStorage.setItem(storageKey, theme.value); } catch { /* The switch also works when browser storage is unavailable. */ }
    },
  };
}
