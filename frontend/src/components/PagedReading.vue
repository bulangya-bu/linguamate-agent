<template>
  <div class="paged-reading" :class="{ 'is-compact': compact }">
    <div ref="viewport" class="paged-viewport">
      <div
        ref="flow"
        class="paged-flow"
        :style="{
          transform: `translateX(${-page * (width + gap)}px)`,
        }"
      >
        <slot />
      </div>
    </div>
    <nav
      class="reading-pages"
      :class="{ 'is-single': count === 1 && !alwaysShowPagination }"
      :aria-label="label + '翻页'"
      :aria-hidden="count === 1 && !alwaysShowPagination ? true : undefined"
    >
      <button
        type="button"
        :aria-label="'上一页' + label"
        :disabled="page === 0"
        @click="page--"
      >
        <AppIcon name="arrow" />
      </button>
      <span role="status" aria-live="polite">{{ page + 1 }} / {{ count }}</span>
      <button
        type="button"
        :aria-label="'下一页' + label"
        :disabled="page >= count - 1"
        @click="page++"
      >
        <AppIcon name="arrow" />
      </button>
    </nav>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, onUpdated, ref } from "vue";
import AppIcon from "./AppIcon.vue";

defineProps({
  label: { type: String, default: "内容" },
  compact: Boolean,
  alwaysShowPagination: Boolean,
});
const viewport = ref(null);
const flow = ref(null);
const width = ref(0);
const page = ref(0);
const count = ref(1);
const gap = 24;
let observer;
let frame;

function measure() {
  cancelAnimationFrame(frame);
  frame = requestAnimationFrame(() => {
    if (!viewport.value?.clientWidth || !viewport.value?.clientHeight) return;
    width.value = viewport.value.clientWidth;
    if (!flow.value) return;
    // CSS columns preserve every line, including a single very long paragraph.
    count.value = Math.max(
      1,
      Math.ceil((flow.value.scrollWidth + gap - 1) / (width.value + gap)),
    );
    page.value = Math.min(page.value, count.value - 1);
  });
}
onMounted(() => {
  observer = new ResizeObserver(measure);
  observer.observe(viewport.value);
  measure();
});
onUpdated(measure);
onBeforeUnmount(() => {
  observer?.disconnect();
  cancelAnimationFrame(frame);
});
</script>

<style scoped>
.paged-reading {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
  min-height: 0;
  height: 100%;
}
.paged-viewport {
  flex: 1;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  overflow: clip;
}
.paged-flow {
  width: 100%;
  height: 100%;
  column-gap: 24px;
  column-count: 1;
  column-fill: auto;
  overflow-wrap: anywhere;
  transition: transform 0.3s var(--ease);
}
.reading-pages {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  gap: 18px;
  min-height: 32px;
  color: var(--muted);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}
.reading-pages.is-single {
  visibility: hidden;
}
.reading-pages button {
  display: grid;
  place-items: center;
  width: 34px;
  height: 32px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #fff;
}
.reading-pages button:hover:not(:disabled) {
  color: var(--blue-ink);
  border-color: #b6dff4;
  background: #eff9ff;
}
.reading-pages svg {
  width: 16px;
  height: 16px;
}
.reading-pages button:first-child svg {
  transform: rotate(180deg);
}
.is-compact {
  gap: 2px;
}
.is-compact .reading-pages {
  min-height: 22px;
  gap: 12px;
  font-size: 11px;
}
.is-compact .reading-pages button {
  width: 30px;
  height: 22px;
  border: 0;
  background: transparent;
}
@media (prefers-reduced-motion: reduce) {
  .paged-flow {
    transition: none;
  }
}
</style>
