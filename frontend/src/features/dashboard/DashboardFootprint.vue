<script setup>
import { computed } from "vue";
import { RouterLink } from "vue-router";
import AppIcon from "@/components/AppIcon.vue";
import { shortDate } from "@/learning-journal";

const props = defineProps({
  activity: { type: Object, default: null },
  loading: Boolean,
});

const weekdayFormatter = new Intl.DateTimeFormat("zh-CN", {
  weekday: "short",
  timeZone: "UTC",
});

const studyDays = computed(() =>
  props.activity?.days.filter((day) => day.studyEvents > 0).length ?? 0,
);
</script>

<template>
  <RouterLink
    :to="{ name: 'report' }"
    class="study-footprint"
    aria-labelledby="study-footprint-title study-footprint-destination"
    aria-describedby="study-footprint-summary"
    :aria-busy="loading"
  >
    <div class="study-footprint-heading">
      <span class="study-footprint-icon"><AppIcon name="calendar" /></span>
      <div>
        <h2 id="study-footprint-title">学习足迹</h2>
        <p id="study-footprint-summary">
          {{ activity ? `近 7 天 · 学了 ${studyDays} 天` : "近 7 天" }}
        </p>
      </div>
    </div>

    <ol v-if="activity" class="study-footprint-days" aria-label="近七天学习记录">
      <li
        v-for="day in activity.days"
        :key="day.date"
        class="study-footprint-day"
        :class="{ 'is-active': day.studyEvents > 0, 'is-today': day.date === activity.today }"
        :aria-current="day.date === activity.today ? 'date' : undefined"
      >
        <span class="study-footprint-dot" aria-hidden="true">
          <svg v-if="day.studyEvents > 0" viewBox="0 0 12 12" fill="none">
            <path d="m2.6 6.2 2.2 2.1 4.7-4.8" stroke="currentColor" stroke-width="1.45" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </span>
        <time
          :datetime="day.date"
          :title="`${day.date}，${day.studyEvents > 0 ? '有学习记录' : '暂无学习记录'}`"
        >
          <span class="study-footprint-weekday">{{ weekdayFormatter.format(new Date(day.date)) }}</span>
          <span class="study-footprint-date">{{ day.date === activity.today ? "今天" : shortDate(day.date) }}</span>
        </time>
      </li>
    </ol>
    <p v-else class="study-footprint-unavailable" role="status">
      {{ loading ? "正在读取学习足迹…" : "学习足迹暂时无法读取" }}
    </p>

    <span id="study-footprint-destination" class="study-footprint-destination">
      学习档案 <AppIcon name="arrow" />
    </span>
  </RouterLink>
</template>

<style scoped>
.study-footprint {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 2.1fr) minmax(0, .8fr);
  align-items: center;
  gap: 18px;
  min-width: 0;
  min-height: 72px;
  padding: 10px 17px;
  border: 1px solid var(--line);
  border-radius: 15px;
  color: var(--ink);
  background: color-mix(in srgb, var(--surface), var(--green) 3%);
  transition: border-color 180ms, background 180ms;
}

.study-footprint:hover,
.study-footprint:focus-visible {
  border-color: color-mix(in srgb, var(--line), var(--green-ink) 40%);
  background: color-mix(in srgb, var(--surface), var(--green) 7%);
}

.study-footprint:focus-visible {
  outline-offset: -4px;
}

.study-footprint-heading {
  display: flex;
  align-items: center;
  gap: 10px;
}

.study-footprint-icon {
  display: grid;
  place-items: center;
  flex: none;
  width: 34px;
  height: 34px;
  border-radius: 9px;
  color: var(--green-ink);
  background: color-mix(in srgb, var(--surface), var(--green) 11%);
}

.study-footprint-icon svg,
.study-footprint-destination svg {
  width: 16px;
  height: 16px;
}

.study-footprint-heading h2 {
  margin: 0;
  color: var(--ink);
  font-size: 16px;
  font-weight: 650;
}

.study-footprint-heading p,
.study-footprint-unavailable {
  margin: 2px 0 0;
  color: var(--muted);
  font-size: 12px;
}

.study-footprint-days {
  position: relative;
  isolation: isolate;
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  margin: 0;
  padding: 0;
  list-style: none;
}

.study-footprint-days::before {
  position: absolute;
  z-index: -1;
  inset-inline: calc(100% / 14);
  top: 9px;
  height: 1px;
  background: var(--line);
  content: "";
}

.study-footprint-day {
  display: grid;
  justify-items: center;
  gap: 5px;
  color: var(--muted);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  line-height: 1.2;
}

.study-footprint-day time {
  display: grid;
  justify-items: center;
  gap: 2px;
  white-space: nowrap;
}

.study-footprint-weekday {
  font-weight: 600;
}

.study-footprint-date {
  font-size: 11px;
}

.study-footprint-dot {
  display: grid;
  place-items: center;
  width: 18px;
  height: 18px;
  border: 1px solid color-mix(in srgb, var(--line), var(--muted) 38%);
  border-radius: 50%;
  background: var(--surface);
}

.study-footprint-dot svg {
  width: 12px;
  height: 12px;
}

.study-footprint-day.is-active .study-footprint-dot {
  border-color: var(--green-ink);
  color: var(--green-ink);
  background: color-mix(in srgb, var(--surface), var(--green) 22%);
}

.study-footprint-day.is-today {
  color: var(--green-ink);
  font-weight: 650;
}

.study-footprint-day.is-today .study-footprint-dot {
  outline: 1px solid var(--green-ink);
  outline-offset: 2px;
}

.study-footprint-unavailable {
  text-align: center;
}

.study-footprint-destination {
  display: inline-flex;
  justify-self: end;
  align-items: center;
  gap: 6px;
  color: var(--green-ink);
  font-size: 13px;
  font-weight: 550;
  white-space: nowrap;
}

@media (max-width: 760px) {
  .study-footprint {
    grid-template-columns: 1fr auto;
    gap: 12px;
  }
  .study-footprint-days,
  .study-footprint-unavailable {
    grid-column: 1 / -1;
    grid-row: 2;
  }
}

@media (prefers-reduced-motion: reduce) {
  .study-footprint { transition: none; }
}
</style>
