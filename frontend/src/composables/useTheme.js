import { computed, nextTick, shallowRef } from "vue";

const storageKey = "linguamate-theme";
const root = document.documentElement;
const theme = shallowRef(root.dataset.theme === "dark" ? "dark" : "light");
let activeReveal;

function applyTheme(value) {
  theme.value = value === "dark" ? "dark" : "light";
  root.dataset.theme = theme.value;
  root.style.colorScheme = theme.value;
  document.querySelector('meta[name="theme-color"]')?.setAttribute("content", theme.value === "dark" ? "#101923" : "#58cc02");
}

function saveTheme(value) {
  applyTheme(value);
  try { localStorage.setItem(storageKey, value); } catch { /* The switch also works when browser storage is unavailable. */ }
}

function clearReveal() {
  activeReveal = undefined;
  root.classList.remove("is-theme-revealing");
  root.style.removeProperty("--theme-reveal-x");
  root.style.removeProperty("--theme-reveal-y");
  root.style.removeProperty("--theme-reveal-extent");
  root.style.removeProperty("--theme-reveal-edge-scale");
}

function skipReveal() {
  if (!activeReveal) return;
  activeReveal.transition.skipTransition();
  clearReveal();
}

function toggleTheme(button) {
  const current = activeReveal?.target ?? theme.value;
  const target = current === "dark" ? "light" : "dark";
  if (activeReveal || !button || !document.startViewTransition ||
      window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
    skipReveal();
    saveTheme(target);
    return;
  }

  const bounds = button.getBoundingClientRect();
  const x = bounds.left + bounds.width / 2;
  const y = bounds.top + bounds.height / 2;
  const extent = Math.ceil(Math.hypot(Math.max(x, window.innerWidth - x), Math.max(y, window.innerHeight - y))) + 18;
  root.style.setProperty("--theme-reveal-x", `${x}px`);
  root.style.setProperty("--theme-reveal-y", `${y}px`);
  root.style.setProperty("--theme-reveal-extent", `${extent}px`);
  root.style.setProperty("--theme-reveal-edge-scale", extent / 127);
  root.classList.add("is-theme-revealing");

  const transition = document.startViewTransition(async () => {
    if (activeReveal?.transition !== transition) return;
    saveTheme(target);
    await nextTick();
  });
  activeReveal = { transition, target };
  transition.ready.catch(() => {});
  const cleanup = () => {
    if (activeReveal?.transition === transition) clearReveal();
  };
  transition.finished.then(cleanup, cleanup);
}

export function initializeTheme() {
  applyTheme(theme.value);
  window.addEventListener("storage", (event) => {
    if (event.key === storageKey || event.key === null) {
      skipReveal();
      applyTheme(event.newValue);
    }
  });
}

export function useTheme() {
  return {
    isDark: computed(() => theme.value === "dark"),
    toggleTheme,
  };
}
