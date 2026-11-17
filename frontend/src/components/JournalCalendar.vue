<script setup>
import { computed, shallowRef, watch } from "vue";
import AppIcon from "./AppIcon.vue";
import { learningApi } from "@/api/modules";
import { dayIntensity, journalMonthLayout, shiftJournalMonth } from "@/learning-journal";

const props = defineProps({
  days: { type: Array, required: true },
  today: { type: String, required: true },
  selectedDate: { type: String, required: true },
});
const emit = defineEmits(["select"]);
const weekdays = ["一", "二", "三", "四", "五", "六", "日"];
const dayButtons = [];
const dateFormat = new Intl.DateTimeFormat("zh-CN", {
  month: "long",
  day: "numeric",
  weekday: "short",
  timeZone: "UTC",
});

const currentMonth = computed(() => props.today.slice(0, 7));
const month = shallowRef(currentMonth.value);
const previousMonth = computed(() => shiftJournalMonth(month.value, -1));
const nextMonth = computed(() => shiftJournalMonth(month.value, 1));
const monthLabel = computed(() => month.value.slice(0, 4) + "年" + Number(month.value.slice(5)) + "月");
const layout = computed(() => journalMonthLayout(month.value));
const history = shallowRef(null);
const loading = shallowRef(false);
const failed = shallowRef(false);
const retry = shallowRef(0);
const unavailable = computed(() => loading.value || failed.value);

const entries = computed(() => {
  const records = month.value === currentMonth.value ? props.days : history.value ?? [];
  const dates = new Map(records.map(day => [day.date, day]));
  return layout.value.dates.map(date =>
    dates.get(date) ?? { date, newWords: 0, reviewWords: 0, studyEvents: 0 });
});
const lastDay = computed(() => entries.value.findLastIndex(day => day.date <= props.today));
const selectedDay = computed(() => {
  const fallback = month.value === currentMonth.value ? props.today : layout.value.dates[0];
  return entries.value.find(day => day.date === props.selectedDate && day.date <= props.today)
    ?? entries.value.find(day => day.date === fallback);
});
const hasActivity = computed(() =>
  selectedDay.value && (selectedDay.value.studyEvents || selectedDay.value.newWords || selectedDay.value.reviewWords));

watch(currentMonth, (next, old) => {
  if (month.value === old || month.value > next) showMonth(next);
});

watch([month, () => props.days, retry], async ([target], _, onCleanup) => {
  history.value = null;
  failed.value = false;
  loading.value = target !== currentMonth.value;
  if (!loading.value) return;

  const request = new AbortController();
  onCleanup(() => request.abort());
  try {
    const result = await learningApi.journalCalendar(target, request.signal);
    if (request.signal.aborted) return;
    if (result?.month !== target || !Array.isArray(result.days)) {
      failed.value = true;
      return;
    }
    history.value = result.days;
  } catch {
    if (!request.signal.aborted) failed.value = true;
  } finally {
    if (!request.signal.aborted) loading.value = false;
  }
}, { immediate: true });

function showMonth(target) {
  if (target === month.value || target > currentMonth.value || target < "0001-01") return;
  month.value = target;
  emit("select", target === currentMonth.value ? props.today : target + "-01");
}

function selectDay(day) {
  if (!unavailable.value && day.date <= props.today) emit("select", day.date);
}

function focusDay(index) {
  const target = dayButtons[Math.max(0, Math.min(lastDay.value, index))];
  if (target && !target.disabled) target.focus();
}

function dayLabel(day) {
  const date = day.date.replaceAll("-", ".");
  if (day.date > props.today) return date + "，未来日期";
  if (unavailable.value) return date;
  return date + "，" + day.studyEvents + " 条学习记录，新学 " + day.newWords + " 词，复习 " + day.reviewWords + " 词";
}
</script>

<template>
  <section class="journal-calendar" aria-labelledby="journal-calendar-heading">
    <header class="journal-section-heading">
      <h2 id="journal-calendar-heading">学习日历</h2>
      <div class="journal-month-controls" role="group" aria-label="切换月份">
        <button
          v-if="month !== currentMonth"
          class="journal-return-month"
          type="button"
          @click="showMonth(currentMonth)"
        >回到本月</button>
        <button type="button" aria-label="上个月" title="上个月" :disabled="month === '0001-01'" @click="showMonth(previousMonth)">
          <AppIcon class="journal-previous-arrow" name="arrow" />
        </button>
        <time class="journal-calendar-month" :datetime="month" aria-live="polite">{{ monthLabel }}</time>
        <button type="button" aria-label="下个月" title="下个月" :disabled="month >= currentMonth" @click="showMonth(nextMonth)">
          <AppIcon name="arrow" />
        </button>
      </div>
    </header>

    <div class="journal-day-grid" role="group" aria-label="当月学习日历" :aria-busy="loading">
      <span v-for="label in weekdays" :key="label" class="journal-calendar-weekday" aria-hidden="true">周{{ label }}</span>
      <button
        v-for="(day, index) in entries"
        :key="day.date"
        :ref="element => dayButtons[index] = element"
        type="button"
        :style="{ gridColumn: (layout.offset + index) % 7 + 1, gridRow: Math.floor((layout.offset + index) / 7) + 2 }"
        :data-intensity="dayIntensity(day)"
        :tabindex="selectedDay?.date === day.date && !unavailable ? 0 : -1"
        :aria-pressed="selectedDay?.date === day.date && !unavailable"
        :aria-current="day.date === today ? 'date' : undefined"
        :disabled="unavailable || day.date > today"
        :aria-label="dayLabel(day)"
        :title="dayLabel(day)"
        @mouseenter="selectDay(day)"
        @focus="selectDay(day)"
        @click="selectDay(day)"
        @keydown.left.prevent="focusDay(index - 1)"
        @keydown.right.prevent="focusDay(index + 1)"
        @keydown.up.prevent="focusDay(index - 7)"
        @keydown.down.prevent="focusDay(index + 7)"
      >{{ index + 1 }}</button>
    </div>

    <div class="journal-calendar-footer">
      <div class="journal-selected-note" aria-live="polite">
        <span v-if="loading">正在加载本月记录…</span>
        <template v-else-if="failed">
          <span>本月记录加载失败</span>
          <button class="journal-calendar-retry" type="button" @click="retry++">重新加载</button>
        </template>
        <template v-else-if="selectedDay">
          <div class="journal-selected-date">
            <AppIcon name="calendar" />
            <time :datetime="selectedDay.date">
              {{ dateFormat.format(new Date(selectedDay.date + "T00:00:00Z")) }}
            </time>
            <span v-if="selectedDay.date === today" class="journal-selected-today">今天</span>
          </div>
          <div v-if="hasActivity" class="journal-day-activity">
            <span>新学 <b>{{ selectedDay.newWords }}</b> 词</span>
            <span>复习 <b>{{ selectedDay.reviewWords }}</b> 词</span>
            <span>学习记录 <b>{{ selectedDay.studyEvents }}</b> 条</span>
          </div>
          <span v-else>当天暂无学习记录</span>
        </template>
      </div>
      <div class="journal-calendar-key" role="img" aria-label="学习量由浅到深">
        <span>少</span>
        <i v-for="level in [0, 1, 2, 3, 4]" :key="level" :data-intensity="level" aria-hidden="true"></i>
        <span>多</span>
      </div>
    </div>
  </section>
</template>

<style scoped>
.journal-calendar {
  --j-day-surface: color-mix(in srgb, var(--j-cell) 66%, var(--j-paper));
  --j-day-border: color-mix(in srgb, var(--j-cell-line) 24%, transparent);
  --j-day-active-ink: #3a5036;
  padding-block: 16px;
}

:global(:root[data-theme="dark"] .journal-calendar) {
  --j-day-active-ink: color-mix(in srgb, var(--j-ink) 86%, var(--j-green-ink));
}

.journal-month-controls {
  display: inline-flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

.journal-month-controls > button {
  display: inline-grid;
  place-items: center;
  min-width: 32px;
  min-height: 32px;
  padding: 3px 6px;
  border: 1px solid color-mix(in srgb, var(--j-rule) 62%, transparent);
  border-radius: 9px;
  background: var(--j-day-surface);
  color: var(--j-muted);
  font-size: 13px;
}

.journal-month-controls > button:hover:not(:disabled) {
  border-color: color-mix(in srgb, var(--j-green-ink) 38%, transparent);
  background: color-mix(in srgb, var(--j-green) 13%, var(--j-paper));
  color: var(--j-green-ink);
}
.journal-month-controls > button > svg { width: 15px; height: 15px; }
.journal-previous-arrow { rotate: 180deg; }
.journal-month-controls .journal-return-month { margin-inline-end: 4px; }
.journal-calendar-month {
  min-width: 104px;
  color: color-mix(in srgb, var(--j-ink) 72%, var(--j-muted));
  font-size: 15px;
  font-weight: 600;
  text-align: center;
}

.journal-day-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  grid-template-rows: auto repeat(6, 23px);
  gap: 4px clamp(3px, .45vw, 8px);
  margin: 10px 0 11px;
  min-width: 0;
}

.journal-calendar-weekday {
  padding-bottom: 2px;
  color: var(--j-muted);
  font-size: 12px;
  line-height: 1.4;
  text-align: center;
}

.journal-day-grid > button {
  position: relative;
  min-width: 0;
  padding: 0;
  border: 1px solid var(--j-day-border);
  border-radius: 6px;
  background: var(--j-heat, var(--j-day-surface));
  color: var(--j-day-ink, var(--j-muted));
  font-size: 13px;
  font-weight: 500;
  transition: background-color 150ms ease, border-color 150ms ease;
}

.journal-day-grid > button[aria-current="date"]::after {
  position: absolute;
  inset-block-start: 4px;
  inset-inline-end: 5px;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: currentColor;
  content: "";
}
.journal-day-grid > button[aria-current="date"][data-intensity="0"] {
  background-color: color-mix(in srgb, var(--j-green) 15%, var(--j-paper));
  color: var(--j-green-ink);
}
.journal-day-grid > button:hover:not(:disabled),
.journal-day-grid > button[aria-pressed="true"] {
  border-color: color-mix(in srgb, var(--j-green-ink) 74%, var(--j-paper));
}
.journal-day-grid > button[data-intensity="4"][aria-pressed="true"] {
  border-color: var(--j-day-ink);
}
.journal-day-grid > button:focus-visible {
  outline: 2px solid var(--j-green-ink);
  outline-offset: 1px;
}
.journal-day-grid > button:disabled {
  opacity: 1;
  border-color: transparent;
  background: color-mix(in srgb, var(--j-cell) 24%, var(--j-paper));
  color: color-mix(in srgb, var(--j-muted) 62%, var(--j-paper));
}

[data-intensity="1"] { --j-heat: color-mix(in srgb, var(--j-cell-1) 80%, var(--j-paper)); }
[data-intensity="2"] { --j-heat: color-mix(in srgb, var(--j-cell-2) 66%, var(--j-paper)); }
[data-intensity="3"] { --j-heat: color-mix(in srgb, var(--j-cell-3) 54%, var(--j-paper)); }
[data-intensity="4"] {
  --j-heat: color-mix(in srgb, var(--j-cell-4) 80%, var(--j-paper));
  --j-day-ink: #233a20;
}
.journal-day-grid > button:is([data-intensity="1"], [data-intensity="2"], [data-intensity="3"]) {
  --j-day-ink: var(--j-day-active-ink);
}
.journal-calendar-key > i {
  background: var(--j-heat, var(--j-day-surface));
}

.journal-calendar-footer {
  display: flex;
  align-items: center;
  gap: 5px 12px;
  flex-wrap: wrap;
}
.journal-calendar-key {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin-inline-start: auto;
  color: var(--j-muted);
  font-size: 13px;
}
.journal-calendar-key > i { width: 11px; height: 11px; border: 1px solid var(--j-day-border); border-radius: 4px; }

.journal-selected-note,
.journal-selected-date,
.journal-day-activity {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}
.journal-selected-note {
  flex: 1 1 auto;
  min-height: 38px;
  padding: 7px 10px;
  gap: 5px 15px;
  border: 1px solid color-mix(in srgb, var(--j-rule) 56%, transparent);
  border-radius: 9px;
  background: color-mix(in srgb, var(--j-soft) 45%, var(--j-paper));
  color: var(--j-muted);
  font-size: 14px;
}
.journal-selected-date { gap: 6px; color: var(--j-ink); font-weight: 650; }
.journal-selected-date > svg { width: 16px; height: 16px; color: var(--j-blue-ink); }
.journal-selected-today {
  padding: 1px 5px;
  border-radius: 4px;
  background: color-mix(in srgb, var(--j-green) 13%, var(--j-paper));
  color: var(--j-green-ink);
  font-size: 12px;
}
.journal-day-activity { gap: 4px 12px; }
.journal-day-activity b { color: var(--j-ink); }
.journal-calendar-retry { padding: 0; color: var(--j-green-ink); font-weight: 600; }
</style>
