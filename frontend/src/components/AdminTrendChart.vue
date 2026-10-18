<template>
  <div class="admin-trend-interactive">
    <div class="admin-trend-caption">
      <span><i></i>练词作答次数</span>
      <p aria-live="polite">
        <time>{{ selected?.date?.replaceAll("-", ".") || "—" }}</time
        ><strong>{{ selected?.words || 0 }}<small>次</small></strong>
      </p>
    </div>
    <div ref="canvas" class="admin-trend-canvas">
      <svg
        :viewBox="`0 0 ${size.width} ${size.height}`"
        role="group"
        aria-label="近七天练词作答趋势，可聚焦日期并用左右方向键查看"
      >
        <g v-for="tick in scale.ticks" :key="tick">
          <line
            x1="42"
            :x2="size.width - 12"
            :y1="y(tick)"
            :y2="y(tick)"
            class="admin-trend-grid"
          />
          <text
            x="31"
            :y="y(tick) + 4"
            text-anchor="end"
            class="admin-trend-axis"
          >
            {{ tick }}
          </text>
        </g>
        <g
          v-for="(point, index) in points"
          :key="point.date"
          class="admin-trend-day"
          :class="{ selected: selected?.date === point.date }"
        >
          <rect
            :x="x(index) + 4"
            y="6"
            :width="Math.max(4, column - 8)"
            :height="size.height - 33"
            rx="7"
            class="admin-trend-highlight"
          />
          <g
            class="admin-trend-bar"
            :style="{ '--bar-base': `${size.height - 33}px` }"
          >
            <SketchShape
              v-if="point.words > 0"
              kind="rectangle"
              :args="[
                x(index) + (column - barWidth) / 2,
                y(point.words),
                barWidth,
                height(point.words),
              ]"
              :seed="(Number(point.date.replaceAll('-', '')) % 1000000) + 1"
              :options="{
                stroke:
                  index === points.length - 1
                    ? 'var(--green-ink)'
                    : 'var(--blue-ink)',
                fill:
                  index === points.length - 1
                    ? 'var(--admin-hatch-green)'
                    : 'var(--admin-hatch-blue)',
                hachureGap: 4,
                fillWeight: 1.7,
                strokeWidth: 1.2,
                roughness: 0.55,
              }"
            />
          </g>
          <text
            :x="x(index) + column / 2"
            :y="Math.max(15, y(point.words) - 10)"
            text-anchor="middle"
            class="admin-trend-value"
          >
            {{ point.words || 0 }}
          </text>
          <text
            :x="x(index) + column / 2"
            :y="size.height - 9"
            text-anchor="middle"
            class="admin-trend-axis"
          >
            {{ point.date.slice(5).replace("-", ".") }}
          </text>
          <rect
            :ref="(element) => (days[index] = element)"
            :x="x(index)"
            y="0"
            :width="column"
            :height="size.height"
            fill="transparent"
            tabindex="0"
            role="button"
            :aria-label="`${point.date}，练词作答 ${point.words || 0} 次`"
            :aria-pressed="selected?.date === point.date"
            @mouseenter="selectedDate = point.date"
            @click="selectedDate = point.date"
            @focus="selectedDate = point.date"
            @keydown.enter.prevent="selectedDate = point.date"
            @keydown.space.prevent="selectedDate = point.date"
            @keydown.right.prevent="focusDay(index + 1)"
            @keydown.left.prevent="focusDay(index - 1)"
          />
        </g>
      </svg>
      <p
        v-if="!points.some((point) => point.words > 0)"
        class="admin-trend-empty"
      >
        近 7 天暂无练词记录
      </p>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from "vue";
import SketchShape from "./SketchShape.vue";
import { chartScale } from "@/learning-journal";
const props = defineProps({ points: { type: Array, default: () => [] } });
const canvas = ref(null),
  days = ref([]),
  selectedDate = ref(""),
  size = ref({ width: 700, height: 280 });
const selected = computed(
  () =>
    props.points.find((point) => point.date === selectedDate.value) ||
    props.points.at(-1),
);
const scale = computed(() =>
  chartScale(props.points.map((point) => ({ newWords: point.words }))),
);
const column = computed(
  () => (size.value.width - 56) / Math.max(1, props.points.length),
);
const barWidth = computed(() => Math.min(43, column.value * 0.36));
const height = (value) =>
  (Math.max(0, Number(value) || 0) / scale.value.maximum) *
  Math.max(1, size.value.height - 62);
const y = (value) => size.value.height - 33 - height(value);
const x = (index) => 42 + index * column.value;
async function focusDay(index) {
  const safe = Math.max(0, Math.min(props.points.length - 1, index));
  selectedDate.value = props.points[safe]?.date;
  await nextTick();
  days.value[safe]?.focus();
}
let observer;
onMounted(() => {
  observer = new ResizeObserver(([entry]) => {
    size.value = {
      width: Math.max(200, entry.contentRect.width),
      height: Math.max(120, entry.contentRect.height),
    };
  });
  observer.observe(canvas.value);
});
onBeforeUnmount(() => observer?.disconnect());
</script>
