<script setup>
import { computed, nextTick, shallowRef, toRef, watch } from "vue";
import AppIcon from "./AppIcon.vue";
import { learningApi } from "@/api/modules";
import { hasStudyActivity, journalMonthLayout, shiftJournalMonth } from "@/learning-journal";

const props = defineProps({
  days: { type: Array, required: true },
  today: { type: String, required: true },
  month: { type: String, required: true },
  selectedDate: { type: String, required: true },
});
const emit = defineEmits(["browse", "select"]);
const weekdays = ["一", "二", "三", "四", "五", "六", "日"];
const dayFormat = new Intl.DateTimeFormat("zh-CN", {
  month: "long", day: "numeric", weekday: "short", timeZone: "UTC",
});

const selection = toRef(props, "selectedDate");
const month = toRef(props, "month");
const currentMonth = computed(() => props.today.slice(0, 7));
const monthLabel = computed(() => month.value.slice(0, 4) + "年" + Number(month.value.slice(5)) + "月");
const layout = computed(() => journalMonthLayout(month.value));
const history = shallowRef([]);
const state = shallowRef("ready");
const retry = shallowRef(0);
const dayButtons = [];
const previousMonthButton = shallowRef(null);
const focusedDate = shallowRef("");
const tabDate = computed(() => focusedDate.value || selection.value ||
  (month.value === currentMonth.value ? props.today : layout.value.dates[0]));

watch([month, selection], () => { focusedDate.value = ""; });

watch([month, () => props.days, currentMonth, retry], async ([target], _, onCleanup) => {
  history.value = [];
  if (target === currentMonth.value) {
    state.value = "ready";
    return;
  }

  state.value = "loading";
  const request = new AbortController();
  onCleanup(() => request.abort());
  try {
    const result = await learningApi.journalCalendar(target, request.signal);
    if (request.signal.aborted) return;
    if (result?.month !== target || !Array.isArray(result.days)) {
      state.value = "error";
      return;
    }
    history.value = result.days;
    state.value = "ready";
  } catch {
    if (!request.signal.aborted) state.value = "error";
  }
}, { immediate: true });

const entries = computed(() => {
  const source = month.value === currentMonth.value ? props.days : history.value;
  const records = new Map(source.map(day => [day.date, day]));
  return layout.value.dates.map(date => records.get(date)
    ?? { date, newWords: 0, reviewWords: 0, studyEvents: 0 });
});
const lastAvailable = computed(() => entries.value.findLastIndex(day => day.date <= props.today));
const activeDays = computed(() => entries.value.filter(day => day.date <= props.today && hasStudyActivity(day)).length);
const selectedDay = computed(() => entries.value.find(day => day.date === selection.value));
const selectedLabel = computed(() => selectedDay.value
  ? dayFormat.format(new Date(selectedDay.value.date + "T00:00:00Z")) : "");
const selectedActivity = computed(() => activityLabels(selectedDay.value));

function activityLabels(day) {
  if (!day) return [];
  const labels = [];
  if (day.newWords > 0) labels.push("新学 " + day.newWords + " 词");
  if (day.reviewWords > 0) labels.push("复习 " + day.reviewWords + " 词");
  if (day.aiChatTurns > 0) labels.push("AI 对话 " + day.aiChatTurns + " 轮");
  if (day.aiEssayReviews > 0) labels.push("AI 批改 " + day.aiEssayReviews + " 次");
  if (day.readingsFinished > 0) labels.push("读完 " + day.readingsFinished + " 篇");
  if (!labels.length && day.studyEvents > 0) labels.push(day.studyEvents + " 条学习记录");
  return labels;
}

async function browse(target, event) {
  if (target > currentMonth.value || target < "0001-01") return;
  emit("browse", target);
  if (target === currentMonth.value && event.detail === 0) {
    await nextTick();
    previousMonthButton.value?.focus({ preventScroll: true });
  }
}

function selectDay(day) {
  if (day.date !== selection.value && state.value === "ready" && day.date <= props.today) {
    emit("select", day.date);
  }
}

function focusDay(target) {
  dayButtons[Math.max(0, Math.min(lastAvailable.value, target))]?.focus({ preventScroll: true });
}

function dayLabel(day) {
  const date = day.date.replaceAll("-", ".");
  if (day.date > props.today) return date + "，未来日期";
  if (state.value !== "ready") return date;
  const labels = activityLabels(day);
  return date + "，" + (labels.length ? labels.join("，") : "暂无学习记录");
}
</script>

<template>
  <section class="archive-calendar" aria-labelledby="journal-calendar-heading">
    <header class="calendar-heading">
      <h2 id="journal-calendar-heading">学习日历</h2>
      <span v-if="state === 'ready' && selectedDay" class="calendar-active-total">本月学习 <b>{{ activeDays }}</b> 天</span>
    </header>

    <div class="calendar-month-row">
      <time class="calendar-month" :datetime="month" aria-live="polite">{{ monthLabel }}</time>
      <div class="calendar-paging" role="group" aria-label="切换月份">
        <button
          v-if="month !== currentMonth" class="calendar-return" type="button"
          @click="browse(currentMonth, $event)"
        >回到本月</button>
        <button ref="previousMonthButton" type="button" aria-label="上个月" title="上个月" :disabled="month === '0001-01'"
          @click="browse(shiftJournalMonth(month, -1), $event)">
          <AppIcon name="arrow" class="previous-arrow" />
        </button>
        <button type="button" aria-label="下个月" title="下个月" :disabled="month >= currentMonth"
          @click="browse(shiftJournalMonth(month, 1), $event)">
          <AppIcon name="arrow" />
        </button>
      </div>
    </div>

    <div class="calendar-grid" role="group" aria-label="当月学习日历" :aria-busy="state === 'loading'">
      <span v-for="weekday in weekdays" :key="weekday" class="calendar-weekday" aria-hidden="true">周{{ weekday }}</span>
      <button
        v-for="(day, index) in entries" :key="day.date"
        :ref="element => dayButtons[index] = element"
        type="button" class="calendar-day" :class="{ 'has-study': hasStudyActivity(day) }"
        :style="index === 0 ? { gridColumnStart: layout.offset + 1 } : undefined"
        :tabindex="day.date === tabDate && state === 'ready' ? 0 : -1"
        :aria-pressed="day.date === selection && state === 'ready'"
        :aria-current="day.date === today ? 'date' : undefined"
        :disabled="state !== 'ready' || day.date > today"
        :aria-label="dayLabel(day)" :title="dayLabel(day)"
        @focus="focusedDate = day.date" @click="selectDay(day)"
        @keydown.left.prevent="focusDay(index - 1)" @keydown.right.prevent="focusDay(index + 1)"
        @keydown.up.prevent="focusDay(index - 7)" @keydown.down.prevent="focusDay(index + 7)"
        @keydown.home.prevent="focusDay(0)" @keydown.end.prevent="focusDay(lastAvailable)"
      ><span>{{ index + 1 }}</span></button>
    </div>

    <footer class="calendar-detail" aria-live="polite">
      <span v-if="state === 'loading'">正在读取本月记录…</span>
      <template v-else-if="state === 'error'">
        <span>本月记录加载失败</span>
        <button type="button" @click="retry++">重试</button>
      </template>
      <template v-else-if="selectedDay">
        <time :datetime="selection">{{ selectedLabel }}<span v-if="selection === today">今天</span></time>
        <p v-if="selectedActivity.length">
          <span v-for="label in selectedActivity" :key="label">{{ label }}</span>
        </p>
        <p v-else>当天暂无学习记录</p>
      </template>
      <span v-else-if="activeDays">本月学习 {{ activeDays }} 天</span>
      <span v-else>{{ Number(month.slice(5)) }} 月暂无学习记录</span>
    </footer>
  </section>
</template>

<style scoped>
.archive-calendar {
  min-width: 0;
  display: flex;
  flex-direction: column;
  padding: var(--j-panel-padding, 22px);
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 18px;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
}

.calendar-heading, .calendar-month-row, .calendar-paging {
  display: flex;
  align-items: center;
  gap: 10px;
}
.calendar-heading, .calendar-month-row { justify-content: space-between; }
.calendar-heading h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 680;
  letter-spacing: -.03em;
  line-height: 1.4;
}
.calendar-active-total {
  display: inline-flex;
  align-items: baseline;
  gap: 3px;
  color: var(--muted);
  font-size: 13px;
}
.calendar-active-total::before {
  content: "";
  align-self: center;
  width: 6px;
  height: 6px;
  margin-right: 3px;
  border-radius: 50%;
  background: var(--green-ink);
}
.calendar-active-total b { color: var(--green-ink); }
.calendar-paging .calendar-return, .calendar-detail button {
  width: auto;
  padding: 3px;
  color: var(--green-ink);
  background: transparent;
  border: 0;
  font-size: 13px;
  font-weight: 650;
}
.calendar-month-row { margin: 15px 0 8px; min-height: 28px; }
.calendar-month { font-size: 15px; font-weight: 650; }
.calendar-paging {
  display: grid;
  grid-template-columns: 60px 28px 28px;
  justify-items: end;
  gap: 4px;
}
.calendar-paging button:nth-last-child(2) { grid-column: 2; }
.calendar-paging button:last-child { grid-column: 3; }
.calendar-paging button {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  padding: 0;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: transparent;
  color: var(--muted);
}
.calendar-paging button:hover:not(:disabled) { color: var(--ink); background: var(--surface-hover, #edf3f2); }
.calendar-paging button:disabled { opacity: .4; }
.calendar-paging svg { width: 15px; height: 15px; }
.previous-arrow { transform: rotate(180deg); }

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  grid-template-rows: 24px repeat(6, 30px);
  gap: 2px 3px;
  align-content: start;
  margin-bottom: 12px;
}
.calendar-weekday {
  align-self: center;
  color: var(--muted);
  font-size: 12px;
  text-align: center;
}
.calendar-day {
  display: grid;
  place-items: center;
  min-width: 0;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--muted);
  font-size: 14px;
  font-weight: 500;
  line-height: 1;
}
.calendar-day span {
  position: relative;
  display: grid;
  place-items: center;
  width: min(34px, 100%);
  height: 28px;
  border: 1px solid transparent;
  border-radius: 9px;
}
.calendar-day.has-study span {
  color: var(--green-ink);
  background: color-mix(in srgb, var(--green) 18%, var(--surface));
  font-weight: 650;
}
.calendar-day.has-study span::after {
  content: "";
  position: absolute;
  bottom: 2px;
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: currentColor;
}
.calendar-day[aria-current="date"] { color: var(--ink); font-weight: 700; }
.calendar-day:hover:not(:disabled) span { background-color: color-mix(in srgb, var(--muted) 12%, var(--surface)); }
.calendar-day[aria-pressed="true"] span { border-color: var(--green-ink); color: var(--green-ink); }
.calendar-day:disabled { opacity: 1; color: color-mix(in srgb, var(--muted) 60%, var(--surface)); }
.calendar-day:focus-visible { outline: none; }
.calendar-day:focus-visible span, .calendar-paging button:focus-visible, .calendar-return:focus-visible,
.calendar-detail button:focus-visible { outline: 2px solid var(--green-ink); outline-offset: 1px; }
@media (prefers-reduced-motion: no-preference) {
  .calendar-day span { animation: calendar-date-in 140ms ease-out; }
}
@keyframes calendar-date-in {
  from { opacity: .5; }
  to { opacity: 1; }
}

.calendar-detail {
  display: flex;
  align-content: flex-start;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 3px 10px;
  box-sizing: border-box;
  /* Reserve the date line and two lines of activities, including loading and retry states. */
  min-block-size: calc(3lh + 18px);
  margin-top: auto;
  border-block-start: 1px solid var(--line);
  padding-top: 10px;
  font-size: 13px;
  line-height: 1.5;
  color: var(--muted);
}
.calendar-detail time { font-size: 13px; font-weight: 650; color: var(--ink); }
.calendar-detail time span { margin-left: 6px; color: var(--green-ink); font-size: 12px; }
.calendar-detail p { display: flex; flex-wrap: wrap; gap: 1px 9px; margin: 0; line-height: inherit; }
</style>
