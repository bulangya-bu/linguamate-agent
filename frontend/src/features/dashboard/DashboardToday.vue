<script setup>
import { computed } from "vue";

const props = defineProps({
  tasks: { type: Array, default: null },
  loading: Boolean,
  busy: Boolean,
});
defineEmits(["start"]);

const taskCount = computed(() =>
  props.tasks?.filter((task) => task.status !== "skipped").length ?? 0,
);
const doneCount = computed(() =>
  props.tasks?.filter((task) => task.status === "done").length ?? 0,
);
const statusLabels = { done: "已完成", skipped: "无需复习" };
</script>

<template>
  <section class="study-todo" aria-labelledby="study-todo-title" :aria-busy="loading">
    <div class="study-todo-heading">
      <h2 id="study-todo-title">今日待办</h2>
      <p v-if="tasks" class="study-todo-count">
        <span>完成</span> {{ doneCount }} / {{ taskCount }}
      </p>
    </div>

    <ul v-if="tasks" class="study-todo-list" aria-label="今日学习任务">
      <li
        v-for="task in tasks"
        :key="task.id"
        class="study-todo-item"
        :data-task="task.id"
        :data-status="task.status"
      >
        <svg class="study-todo-check" viewBox="0 0 28 28" fill="none" aria-hidden="true">
          <path
            d="M4 4.5Q13 3.4 23 4.5L24 23.2Q14 24.4 4.1 23.5L4 4.5Z"
            stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"
          />
          <path
            v-if="task.status === 'done'"
            class="study-todo-tick"
            d="M7.6 13.4Q10.3 16 12.5 18L21.1 8.7"
            stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"
          />
          <path
            v-else-if="task.status === 'skipped'"
            d="M9 14.5Q14 13.9 19 14.2"
            stroke="currentColor" stroke-width="1.7" stroke-linecap="round"
          />
        </svg>
        <div class="study-todo-copy">
          <span>{{ task.title }}</span>
          <small v-if="task.detail && task.status !== 'skipped'">{{ task.detail }}</small>
        </div>
        <button
          v-if="task.status === 'pending'"
          class="study-todo-start"
          type="button"
          :disabled="busy"
          :aria-label="`开始${task.title}`"
          @click="$emit('start', task.id)"
        >
          开始
          <svg viewBox="0 0 12 12" fill="none" aria-hidden="true">
            <path d="M4 2.2Q6.1 4.1 8 6.1L4.2 9.8" stroke="currentColor" stroke-width="1.25" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </button>
        <span v-else class="study-todo-status">{{ statusLabels[task.status] }}</span>
        <svg
          v-if="task.status === 'done'"
          class="study-todo-strike"
          viewBox="0 0 300 22"
          preserveAspectRatio="none"
          fill="none"
          aria-hidden="true"
        >
          <path d="M2 12C22 10.8 29 7 40 5.3S77 2.4 103 4.2 151 3.2 182 4 244 3.6 264 6 285 9.2 298 9.8" />
          <path d="M11 12C27 10.3 31 8.1 43 6.2S78 3.5 104 5 152 4 183 4.7 247 4.7 266 7 281 9.1 289 10" />
        </svg>
      </li>
    </ul>
    <p v-else class="study-todo-unavailable" role="status">
      {{ loading ? "正在读取今日进度…" : "今日进度暂时无法读取" }}
    </p>
  </section>
</template>

<style scoped>
.study-todo {
  --todo-paper: #fbfcf4;
  --todo-rule: #d9e5d6;
  --todo-ink: #5e655d;
  --todo-note: #686f65;
  --todo-accent: #536c50;
  --todo-tape: #cae2ab;
  --todo-hand: "STKaiti", "KaiTi", "Kaiti SC", "Microsoft YaHei", serif;
  position: relative;
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 26px clamp(15px, 1.65vw, 27px) 17px;
  border: 1px solid var(--line);
  border-radius: 21px 24px 24px 21px;
  background: var(--todo-paper);
  box-shadow: 0 8px 23px #304f4610;
  color: var(--todo-ink);
  font: 400 17px/1.25 var(--todo-hand);
  letter-spacing: .02em;
}

:global(:root[data-theme="dark"] .study-todo) {
  --todo-paper: #1b2a2b;
  --todo-rule: #394d45;
  --todo-ink: #b1bab3;
  --todo-note: #a3afa7;
  --todo-accent: #a9bd9c;
  --todo-tape: #58724c;
}

.study-todo::before {
  position: absolute;
  top: 70px;
  bottom: 20px;
  left: 9px;
  width: 1px;
  background: color-mix(in srgb, var(--todo-rule), var(--coral) 20%);
  content: "";
  pointer-events: none;
}

.study-todo::after {
  position: absolute;
  top: 7px;
  left: 43%;
  width: 59px;
  height: 14px;
  background: var(--todo-tape);
  clip-path: polygon(1% 0, 100% 1%, 97% 35%, 100% 58%, 98% 100%, 0 97%, 3% 67%, 0 35%);
  transform: rotate(-5deg);
  opacity: .76;
  content: "";
  pointer-events: none;
}

.study-todo-heading {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  padding-bottom: 9px;
  border-bottom: 1px solid var(--todo-rule);
}

.study-todo-heading h2 {
  margin: 0;
  font-size: clamp(25px, 2vw, 32px);
  font-weight: 400;
  line-height: 1.1;
}

.study-todo-count {
  display: flex;
  align-items: baseline;
  gap: 5px;
  margin: 0;
  color: var(--todo-note);
  font-size: 16px;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.study-todo-count > span {
  font-size: 13px;
}

.study-todo-list {
  display: grid;
  flex: 1;
  grid-auto-rows: 1fr;
  gap: 2px;
  margin: 7px 0 0;
  padding: 0;
  list-style: none;
}

.study-todo-item {
  position: relative;
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr) auto;
  align-items: center;
  gap: 7px;
  min-height: 47px;
  padding: 4px 6px;
  border-bottom: 1px dashed var(--todo-rule);
  color: var(--todo-ink);
}

.study-todo-item:has(.study-todo-start:enabled:is(:hover, :focus-visible)) {
  border-radius: 7px 9px 8px 6px;
  background: color-mix(in srgb, var(--todo-paper), var(--green) 6%);
}

.study-todo-check {
  width: 23px;
  height: 23px;
  color: var(--todo-note);
  transform: rotate(-4deg);
}

.study-todo-tick {
  stroke-dasharray: 21;
  animation: study-todo-ink 350ms var(--ease) both;
}

.study-todo-copy {
  display: grid;
  gap: 1px;
  min-width: 0;
}

.study-todo-copy > span {
  overflow-wrap: anywhere;
}

.study-todo-copy > small {
  color: var(--todo-note);
  font-size: 13px;
  font-variant-numeric: tabular-nums;
}

.study-todo-status,
.study-todo-start {
  color: var(--todo-note);
  font: inherit;
  font-size: 14px;
  letter-spacing: inherit;
  white-space: nowrap;
}

.study-todo-start {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 3px 0 3px 5px;
  border: 0;
  background: transparent;
  color: var(--todo-accent);
}

.study-todo-start > svg {
  width: 12px;
  height: 12px;
}

.study-todo-start::before {
  position: absolute;
  inset: 0;
  border-radius: 7px 9px 8px 6px;
  content: "";
}

.study-todo-start:focus-visible {
  outline: none;
}

.study-todo-start:active:not(:disabled) {
  transform: none;
}

.study-todo-start:focus-visible::before {
  outline: 2px solid var(--todo-accent);
  outline-offset: -2px;
}

.study-todo-item[data-status="done"] {
  --todo-faded-ink: color-mix(in srgb, var(--todo-ink), var(--todo-paper) 10%);
  color: var(--todo-faded-ink);
}

.study-todo-item[data-status="done"] :is(.study-todo-check, .study-todo-status, .study-todo-copy > small) {
  color: var(--todo-faded-ink);
}

.study-todo-strike {
  position: absolute;
  top: 50%;
  left: 2px;
  width: calc(100% - 4px);
  height: 22px;
  color: var(--todo-faded-ink);
  transform: translateY(-50%);
  pointer-events: none;
  animation: study-todo-strike-in 360ms var(--ease) both;
}

.study-todo-strike path {
  stroke: currentColor;
  stroke-width: 1.3;
  stroke-linecap: round;
  opacity: .85;
  vector-effect: non-scaling-stroke;
}

.study-todo-strike path + path {
  stroke-width: .7;
  opacity: .36;
}

.study-todo-item:nth-child(even) > .study-todo-strike {
  rotate: .7deg;
}

.study-todo-unavailable {
  margin: 20px 0;
  color: var(--todo-note);
}

@keyframes study-todo-ink {
  from { stroke-dashoffset: 21; }
  to { stroke-dashoffset: 0; }
}

@keyframes study-todo-strike-in {
  from { clip-path: inset(0 100% 0 0); }
  to { clip-path: inset(0); }
}

@media (prefers-reduced-motion: reduce) {
  .study-todo-tick,
  .study-todo-strike {
    animation: none;
  }
}
</style>
