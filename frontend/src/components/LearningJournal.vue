<template>
  <div class="learning-journal" :aria-busy="loading">
    <div v-if="error" class="journal-load-state" role="alert">
      <AppIcon name="journal" />
      <h2>档案暂时没有打开</h2>
      <p>{{ error }}</p>
      <button type="button" @click="load">重新加载</button>
    </div>
    <div v-else-if="!data" class="journal-load-state" role="status">
      <AppIcon name="journal" />
      <p>正在加载学习档案…</p>
    </div>
    <template v-else>
      <section class="journal-overview" aria-labelledby="journal-overview-heading">
        <header class="journal-heading">
          <div>
            <h2 id="journal-overview-heading">你的学习记录</h2>
            <p>{{ data.nickname || "学习者" }} · 加入的第 {{ data.membershipDays }} 天</p>
          </div>
          <div class="journal-heading-actions">
            <time class="journal-date-stamp" :datetime="data.today" :title="data.timeZone">
              <AppIcon name="calendar" />{{ fullDate(data.today) }}
            </time>
            <button
              class="journal-refresh"
              type="button"
              aria-label="刷新学习档案"
              title="刷新学习档案"
              :disabled="loading"
              @click="load"
            >
              <AppIcon name="review" />
            </button>
            <button class="journal-evidence-link" type="button" @click="$emit('evidence')">
              成长证据 <span aria-hidden="true">↗</span>
            </button>
          </div>
        </header>
        <dl class="journal-metrics">
          <div>
            <dt>今日首次作答</dt>
            <dd><strong>{{ today.newWords || 0 }}</strong> 词</dd>
          </div>
          <div>
            <dt>今日复习</dt>
            <dd><strong>{{ today.reviewWords || 0 }}</strong> 词</dd>
          </div>
          <div>
            <dt>累计学过</dt>
            <dd>
              <strong>{{ data.totals.studiedWords }}</strong> 词
              <span class="journal-metric-note">已掌握 {{ data.totals.masteredWords }} 词</span>
            </dd>
          </div>
          <div>
            <dt>累计登录</dt>
            <dd><strong>{{ data.loginDays }}</strong> 天</dd>
          </div>
        </dl>
      </section>
      <div class="journal-body">
        <div class="journal-observations">
          <section
            class="journal-trend"
            aria-labelledby="journal-trend-heading"
          >
            <header class="journal-section-heading">
              <div>
                <h2 id="journal-trend-heading">词汇学习趋势</h2>
              </div>
              <div class="journal-periods" role="group" aria-label="趋势周期">
                <button
                  v-for="days in [7, 30]"
                  :key="days"
                  type="button"
                  :class="{ active: period === days }"
                  :aria-pressed="period === days"
                  @click="period = days"
                >
                  近 {{ days }} 天
                </button>
              </div>
            </header>
            <div class="journal-chart-meta">
              <div class="journal-legend">
                <span><i></i>新学</span><span><i></i>复习</span>
              </div>
              <p aria-live="polite">
                <time>{{ shortDate(chartDay.date) }}</time
                ><span
                  >新学 <b>{{ chartDay.newWords }}</b> 词</span
                ><span
                  >复习 <b>{{ chartDay.reviewWords }}</b> 词</span
                >
              </p>
            </div>
            <div ref="chartWrap" class="journal-chart-wrap">
              <svg
                class="journal-trend-svg"
                :viewBox="`0 0 ${plotSize.width} ${plotSize.height}`"
                role="group"
                aria-label="每天新学和复习单词数量，聚焦日期查看数据"
              >
                <g v-for="tick in scale.ticks" :key="tick">
                  <line
                    x1="37"
                    :x2="plotSize.width - 12"
                    :y1="chartY(tick)"
                    :y2="chartY(tick)"
                    class="journal-grid-line"
                  />
                  <text
                    x="31"
                    :y="chartY(tick) + 4"
                    text-anchor="end"
                    class="journal-axis-label"
                  >
                    {{ tick }}
                  </text>
                </g>
                <g
                  v-for="(day, index) in trend"
                  :key="day.date"
                  class="journal-chart-day"
                  :class="{ selected: chartDay.date === day.date }"
                >
                  <rect
                    :x="columnX(index)"
                    y="10"
                    :width="columnWidth - 2"
                    :height="plotSize.height - 34"
                    class="journal-selected-column"
                    rx="4"
                  />
                  <g class="journal-chart-ink">
                    <SketchShape
                      v-if="day.newWords"
                      kind="rectangle"
                      :args="[
                        columnX(index) + columnWidth * 0.2,
                        chartY(day.newWords),
                        barWidth,
                        chartHeight(day.newWords),
                      ]"
                      :seed="dateSeed(day.date)"
                      :options="{
                        stroke: 'var(--j-green-ink)',
                        fill: 'var(--j-green)',
                        fillStyle: 'hachure',
                        hachureGap: 4,
                        fillWeight: 1.6,
                        roughness: 0.5,
                      }"
                    />
                    <SketchShape
                      v-if="day.reviewWords"
                      kind="rectangle"
                      :args="[
                        columnX(index) + columnWidth * 0.55,
                        chartY(day.reviewWords),
                        barWidth,
                        chartHeight(day.reviewWords),
                      ]"
                      :seed="dateSeed(day.date) + 1"
                      :options="{
                        stroke: 'var(--j-blue-ink)',
                        fill: 'var(--j-blue)',
                        fillStyle: 'hachure',
                        hachureAngle: -35,
                        hachureGap: 4,
                        fillWeight: 1.4,
                        roughness: 0.5,
                      }"
                    />
                  </g>
                  <text
                    v-if="
                      period === 7 ||
                      index % 5 === 0 ||
                      index === trend.length - 1
                    "
                    :x="columnX(index) + columnWidth * 0.5"
                    :y="plotSize.height - 6"
                    text-anchor="middle"
                    class="journal-axis-label"
                  >
                    {{ shortDate(day.date) }}
                  </text>
                  <rect
                    :ref="(element) => (dayRefs[index] = element)"
                    :x="columnX(index)"
                    y="8"
                    :width="columnWidth - 2"
                    :height="plotSize.height - 10"
                    fill="transparent"
                    tabindex="0"
                    role="button"
                    :aria-label="`${fullDate(day.date)}，新学 ${day.newWords} 词，复习 ${day.reviewWords} 词`"
                    @mouseenter="selectedDate = day.date"
                    @focus="selectedDate = day.date"
                    @click="selectedDate = day.date"
                    @keydown.enter.prevent="selectedDate = day.date"
                    @keydown.right.prevent="focusDay(index + 1)"
                    @keydown.left.prevent="focusDay(index - 1)"
                  />
                </g>
              </svg>
              <p
                v-if="!trend.some((day) => day.newWords || day.reviewWords)"
                class="journal-chart-empty"
              >
                近 {{ period }} 天暂无练词记录
              </p>
            </div>
            <div class="journal-trend-footer">
              <span
                >近 {{ period }} 天新学 <b>{{ periodNew }}</b> 词</span
              ><button type="button" @click="$emit('navigate', 'vocabulary')">
                去练词<AppIcon name="arrow" />
              </button>
            </div>
          </section>
          <JournalCalendar
            :days="data.days"
            :today="data.today"
            :selected-date="selectedDate"
            @select="selectedDate = $event"
          />
        </div>
        <JournalOutcomes
          :journal="data"
          @navigate="emit('navigate', $event)"
          @evidence="emit('evidence', $event)"
        />
      </div>
    </template>
  </div>
</template>

<script setup>
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
  watch,
} from "vue";
import AppIcon from "./AppIcon.vue";
import JournalCalendar from "./JournalCalendar.vue";
import JournalOutcomes from "./JournalOutcomes.vue";
import SketchShape from "./SketchShape.vue";
import { learningApi } from "@/api/modules";
import {
  chartScale,
  shortDate,
  trendDays,
} from "@/learning-journal";
import "@/styles/learning-journal.css";
const emit = defineEmits(["navigate", "evidence"]);
const data = ref(null),
  loading = ref(false),
  error = ref(""),
  period = ref(7),
  selectedDate = ref(""),
  dayRefs = ref([]);
const chartWrap = ref(null),
  plotSize = ref({ width: 720, height: 230 });
let observer;
const trend = computed(() =>
  trendDays(data.value?.days, data.value?.today, period.value),
);
const today = computed(
  () => data.value?.days.find((day) => day.date === data.value.today) || {},
);
const chartDay = computed(
  () =>
    trend.value.find((day) => day.date === selectedDate.value) || today.value,
);
const periodNew = computed(() =>
  trend.value.reduce((sum, day) => sum + day.newWords, 0),
);
const scale = computed(() => chartScale(trend.value));
const columnWidth = computed(
  () => (plotSize.value.width - 54) / Math.max(1, trend.value.length),
);
const barWidth = computed(() =>
  Math.max(3, Math.min(21, columnWidth.value * 0.25)),
);
function chartHeight(value) {
  return (
    (value / scale.value.maximum) * Math.max(40, plotSize.value.height - 48)
  );
}
function chartY(value) {
  return plotSize.value.height - 26 - chartHeight(value);
}
function columnX(index) {
  return 39 + index * columnWidth.value;
}
function dateSeed(date) {
  return (Number(date.replaceAll("-", "")) % 1000000) + 1;
}
function fullDate(date) {
  return date?.replaceAll("-", ".") || "";
}
async function load() {
  if (loading.value) return;
  loading.value = true;
  error.value = "";
  try {
    data.value = await learningApi.journal();
    selectedDate.value = data.value.today;
  } catch (exception) {
    error.value = exception.message || "稍后再试一次。";
  } finally {
    loading.value = false;
  }
}
async function focusDay(index) {
  const safe = Math.max(0, Math.min(trend.value.length - 1, index));
  selectedDate.value = trend.value[safe].date;
  await nextTick();
  dayRefs.value[safe]?.focus();
}
watch(chartWrap, (element) => {
  observer?.disconnect();
  if (!element) return;
  observer = new ResizeObserver(([entry]) => {
    plotSize.value = {
      width: Math.max(200, entry.contentRect.width),
      height: Math.max(100, entry.contentRect.height),
    };
  });
  observer.observe(element);
});
onMounted(load);
onBeforeUnmount(() => observer?.disconnect());
</script>
