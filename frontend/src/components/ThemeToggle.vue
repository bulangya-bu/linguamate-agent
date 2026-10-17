<template>
  <button
    class="theme-toggle"
    type="button"
    :aria-label="isDark ? '切换到白天模式' : '切换到黑夜模式'"
    :title="isDark ? '切换到白天模式' : '切换到黑夜模式'"
    :aria-pressed="isDark"
    @click="toggleTheme"
  >
    <span class="theme-toggle-symbol" aria-hidden="true">
      <AppIcon name="moon" class="theme-toggle-moon" :class="{ 'is-visible': !isDark }" />
      <AppIcon name="sun" class="theme-toggle-sun" :class="{ 'is-visible': isDark }" />
    </span>
  </button>
</template>

<script setup>
import AppIcon from "@/components/AppIcon.vue";
import { useTheme } from "@/composables/useTheme";
const { isDark, toggleTheme } = useTheme();
</script>

<style scoped>
.theme-toggle {
  display: inline-grid;
  flex-shrink: 0;
  place-items: center;
  width: 38px;
  height: 38px;
  padding: 0;
  border: 1px solid transparent;
  border-radius: 12px;
  color: var(--muted);
}
.theme-toggle:hover { color: var(--blue-ink); border-color: var(--line); background: var(--theme-toggle-hover, #edf5fa); }
.theme-toggle-symbol { position: relative; display: block; width: 20px; height: 20px; }
.theme-toggle-symbol > svg { position: absolute; inset: 0; width: 20px; height: 20px; opacity: 0; transform: rotate(-60deg) scale(.5); transition: transform 350ms var(--ease), opacity 180ms ease; }
.theme-toggle-symbol > .is-visible { opacity: 1; transform: rotate(0) scale(1); }
.theme-toggle-sun { color: #e4b76b; }
@media (prefers-reduced-motion: reduce) {
  .theme-toggle-symbol > svg { transition: none; }
}
</style>
