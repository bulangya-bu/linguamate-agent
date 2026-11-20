<script setup>
import { computed, nextTick, ref, watch } from "vue";
import { chartScale, shortDate, trendDays } from "@/learning-journal";
import AppIcon from "@/components/AppIcon.vue";

const props = defineProps({
  days: { type: Array, required: true },
  today: { type: String, required: true },
  selectedDate: { type: String, default: "" },
});
const emit = defineEmits(["navigate", "select"]);
const period = ref(7);
const selection = ref(props.selectedDate);
const hovered = ref("");
const chart = ref(null);
const bars = computed(() =>
  trendDays(props.days, props.today, period.value).map((day) => ({
    date: day.date,
    newWords: day.newWords,
    reviews: day.reviewAttempts,
  })),
);
const newTotal = computed(() => bars.value.reduce((sum, day) => sum + day.newWords, 0));
const reviewTotal = computed(() => bars.value.reduce((sum, day) => sum + day.reviews, 0));
const hasActivity = computed(() => newTotal.value > 0 || reviewTotal.value > 0);
const scale = computed(() => chartScale(bars.value.map(day => ({
  newWords: day.newWords, reviewWords: day.reviews,
}))));
const selected = computed(() => bars.value.find(day => day.date === selection.value)
  ?? bars.value.findLast(day => day.newWords || day.reviews)
  ?? bars.value.at(-1));
const visibleDay = computed(() => bars.value.find(day => day.date === hovered.value) ?? selected.value);

watch(() => props.selectedDate, date => {
  if (bars.value.some(day => day.date === date)) {
    selection.value = date;
    hovered.value = "";
  }
});
watch(period, () => { hovered.value = ""; });

function showLabel(index) {
  return period.value === 7 || index === bars.value.length - 1 || (index % 7 === 0 && index < bars.value.length - 4);
}
function dateLabel(date) {
  const [, month, day] = date.split("-");
  return Number(month) + "月" + Number(day) + "日";
}
function select(day) {
  selection.value = day.date;
  hovered.value = "";
  emit("select", day.date);
}
async function move(index, event) {
  const last = bars.value.length - 1;
  const positions = { ArrowLeft: index - 1, ArrowRight: index + 1, Home: 0, End: last };
  if (!(event.key in positions)) return;
  event.preventDefault();
  const target = Math.max(0, Math.min(last, positions[event.key]));
  select(bars.value[target]);
  await nextTick();
  chart.value?.querySelectorAll("button")[target]?.focus({ preventScroll: true });
}
</script>

<template>
  <section class="archive-trend" aria-labelledby="archive-trend-heading">
    <header class="trend-head">
      <h2 id="archive-trend-heading">词汇练习趋势</h2>
      <div class="trend-period" role="group" aria-label="趋势周期">
        <button v-for="length in [7, 30]" :key="length" type="button"
          :aria-pressed="period === length" @click="period = length">近 {{ length }} 天</button>
      </div>
    </header>
    <div class="trend-totals" :aria-label="`近 ${period} 天合计`">
      <span title="在这段时间内第一次练习的单词数">
        <i class="new-key" aria-hidden="true"></i>新学 <strong>{{ newTotal }}</strong> 词
      </span>
      <span title="在温故知新检查答案的总次数，含重复练习">
        <i class="review-key" aria-hidden="true"></i>复习 <strong>{{ reviewTotal }}</strong> 次
      </span>
    </div>
    <div v-if="hasActivity" class="trend-figure">
      <div class="trend-plot" :class="{ dense: period === 30 }" ref="chart" role="group" aria-label="每日词汇练习，左右方向键切换日期"
        :style="{ '--columns': bars.length }" @pointerleave="hovered = ''">
        <div v-for="tick in scale.ticks" :key="tick" class="trend-rule" aria-hidden="true"
          :style="{ bottom: (100 * tick / scale.maximum) + '%' }"><span>{{ tick }}</span></div>
        <button v-for="(day, index) in bars" :key="day.date" type="button" class="trend-column"
          :class="{ 'is-visible': visibleDay?.date === day.date }"
          :tabindex="selected?.date === day.date ? 0 : -1" :aria-pressed="selected?.date === day.date"
          :aria-label="`${dateLabel(day.date)}，新学 ${day.newWords} 词，复习 ${day.reviews} 次`"
          @pointerenter="hovered = day.date" @click="select(day)" @focus="hovered = ''" @keydown="move(index, $event)">
          <i class="trend-bar new-key" :class="{ empty: !day.newWords }" :style="{ height: (100 * day.newWords / scale.maximum) + '%' }"></i>
          <i class="trend-bar review-key" :class="{ empty: !day.reviews }" :style="{ height: (100 * day.reviews / scale.maximum) + '%' }"></i>
          <span v-if="showLabel(index)" class="trend-x" aria-hidden="true">{{ shortDate(day.date) }}</span>
        </button>
      </div>
    </div>
    <div v-else class="trend-empty" role="status">
      <AppIcon name="vocabulary" /><p>近 {{ period }} 天还没有练词记录</p>
      <button class="trend-practice" type="button" @click="emit('navigate', 'vocabulary')">
        去练词<AppIcon name="arrow" /></button>
    </div>
    <footer v-if="hasActivity && visibleDay" class="trend-foot">
      <p aria-live="polite">
        <time :datetime="visibleDay.date">{{ dateLabel(visibleDay.date) }}</time>
        <span>新学 {{ visibleDay.newWords }} 词</span>
        <span>复习 {{ visibleDay.reviews }} 次</span>
      </p>
    </footer>
  </section>
</template>

<style scoped>
.archive-trend {
  --trend-new: #4c8668;
  --trend-review: #548ca8;
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-width: 0;
  padding: var(--j-panel-padding);
  border: 1px solid var(--line);
  border-radius: 20px;
  color: var(--ink);
  background: var(--surface);
}
:root[data-theme="dark"] .archive-trend { --trend-new: #92bd94; --trend-review: #80adc6; }
.trend-head, .trend-period, .trend-totals, .trend-totals > span, .trend-foot, .trend-foot p, .trend-practice {
  display: flex;
  align-items: center;
}
.trend-head, .trend-foot { justify-content: space-between; gap: 12px; flex-wrap: wrap; }
.trend-head h2 { margin: 0; font-size: clamp(19px, 1.35vw, 23px); line-height: 1.35; letter-spacing: -.03em; }
.trend-period { gap: 2px; padding: 3px; background: color-mix(in srgb, var(--line) 48%, var(--surface)); border-radius: 9px; }
.trend-period button { padding: 5px 10px; line-height: 1.4; border-radius: 7px; color: var(--muted); font-size: 13px; white-space: nowrap; }
.trend-period button[aria-pressed="true"] { background: var(--surface); color: var(--ink); box-shadow: 0 1px 3px #152f3014; font-weight: 650; }
.trend-totals { gap: clamp(18px, 2.2vw, 36px); flex-wrap: wrap; color: var(--muted); font-size: 14px; }
.trend-totals > span { gap: 5px; }
.trend-totals strong { color: var(--ink); font-size: 22px; line-height: 1; font-variant-numeric: tabular-nums; }
.trend-totals i { width: 9px; height: 9px; border-radius: 3px; margin-right: 3px; }
.new-key { background: var(--trend-new); }
.review-key { background: var(--trend-review); }
.trend-figure { flex: 1; display: flex; padding: 10px 6px 26px 31px; }
.trend-plot { flex: 1; display: grid; grid-template-columns: repeat(var(--columns), minmax(0, 1fr)); min-height: 116px; position: relative; }
.trend-rule { position: absolute; left: 0; right: 0; border-top: 1px dashed var(--line); pointer-events: none; }
.trend-rule:first-child { border-top-style: solid; }
.trend-rule span { position: absolute; right: calc(100% + 8px); transform: translateY(-55%); color: var(--muted); font-size: 12px; }
.trend-column {
  position: relative; display: flex; align-items: flex-end; justify-content: center; gap: min(6%, 3px);
  padding: 0; border: 0; border-radius: 5px 5px 0 0; min-width: 0; background: transparent;
}
.trend-column.is-visible { background: color-mix(in srgb, var(--trend-new) 8%, transparent); }
.trend-column:focus-visible, .trend-period button:focus-visible, .trend-practice:focus-visible {
  outline: 2px solid var(--trend-new); outline-offset: 2px;
}
.trend-bar { width: min(25%, 14px); min-height: 1px; border-radius: 3px 3px 0 0; }
.trend-bar.empty { visibility: hidden; }
.trend-x { position: absolute; top: calc(100% + 7px); left: 50%; transform: translateX(-50%); font-size: 12px; color: var(--muted); white-space: nowrap; line-height: 1.2; }
.dense .trend-column:first-of-type .trend-x { left: 0; transform: none; }
.dense .trend-column:last-child .trend-x { left: auto; right: 0; transform: none; }
.trend-empty { flex: 1; min-height: 152px; display: grid; align-content: center; justify-items: center; gap: 8px; color: var(--muted); }
.trend-empty svg { width: 26px; height: 26px; color: var(--trend-new); }
.trend-empty p, .trend-foot p { margin: 0; }
.trend-foot { padding-top: 12px; border-top: 1px solid var(--line); min-height: 37px; }
.trend-foot p { gap: 6px 12px; flex-wrap: wrap; color: var(--muted); font-size: 14px; }
.trend-foot time { color: var(--ink); font-weight: 600; }
.trend-practice { gap: 6px; padding: 4px 0; color: var(--green-ink); font-size: 14px; font-weight: 600; }
.trend-practice svg { width: 16px; height: 16px; }
.trend-practice:hover { text-decoration: underline; text-underline-offset: 4px; }
</style>
