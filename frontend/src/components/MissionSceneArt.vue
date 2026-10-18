<template>
  <div class="mission-illustration" :class="{ 'is-water': theme === 'water' }">
    <svg
      v-if="theme === 'water'"
      viewBox="0 0 480 300"
      role="img"
      aria-label="校园节水传感水龙头示意图"
    >
      <defs>
        <linearGradient id="mission-water" x2="1" y2="1">
          <stop stop-color="var(--surface)" />
          <stop offset="1" stop-color="var(--blue-soft)" />
        </linearGradient>
      </defs>
      <ellipse
        cx="245"
        cy="257"
        rx="177"
        ry="17"
        fill="var(--blue)"
        opacity=".09"
      />
      <path
        d="M85 221 Q223 215 389 221 L370 258 Q239 280 104 256 Z"
        fill="url(#mission-water)"
        stroke="var(--blue-ink)"
        stroke-width="1.5"
      />
      <path
        d="M130 219 L130 91 Q130 64 157 64 L264 64 Q285 64 285 88 L285 112 L252 112 L252 98 L169 98 L169 219"
        fill="var(--surface)"
        stroke="var(--blue-ink)"
        stroke-width="2"
      />
      <path
        d="M135 89 Q139 73 158 72 L260 72"
        fill="none"
        stroke="var(--blue)"
        opacity=".5"
        stroke-width="5"
      />
      <rect
        x="251"
        y="112"
        width="35"
        height="9"
        rx="3"
        fill="var(--green-ink)"
      />
      <g class="mission-water-stream" fill="var(--blue)">
        <path
          v-for="i in 4"
          :key="i"
          :transform="`translate(${262 + (i % 2) * 10} ${133 + i * 17})`"
          d="M0 -6 Q-8 3 0 6 Q8 3 0 -6"
          :style="{ animationDelay: i * -0.3 + 's' }"
        />
      </g>
      <g class="mission-hand">
        <path
          d="M375 197 L324 197 Q313 196 306 190 L278 170 Q271 164 265 170 Q260 177 270 184 L285 197 L244 197 Q230 197 229 205 Q228 213 243 213 L283 220 Q307 238 331 225 L376 225"
          fill="var(--surface)"
          stroke="var(--green-ink)"
          stroke-width="1.8"
        />
      </g>
      <path
        d="M286 130 Q309 133 312 153 M292 121 Q326 128 326 153"
        class="mission-signal"
        fill="none"
        stroke="var(--green)"
        stroke-width="2"
        stroke-linecap="round"
      />
      <path
        d="M72 182 Q48 165 66 144 Q93 157 72 182 M388 112 Q367 95 383 73 Q409 87 388 112"
        fill="var(--green-soft)"
        stroke="var(--green-ink)"
        stroke-width="1.3"
      />
      <path
        d="M186 255 Q238 264 284 252"
        fill="none"
        stroke="var(--blue)"
        stroke-width="2"
        stroke-dasharray="4 7"
      />
    </svg>
    <svg
      v-else
      viewBox="0 0 480 300"
      role="img"
      aria-label="虚构校园地图，主门经过花园及学生中心到图书馆"
    >
      <path
        d="M44 51 Q228 20 430 49 L439 255 Q244 277 43 251 Z"
        fill="var(--surface)"
        stroke="var(--blue-ink)"
        opacity=".9"
      />
      <path
        d="M88 239 L88 161 Q88 133 119 133 L248 133 L248 74 L349 74"
        fill="none"
        stroke="var(--green)"
        stroke-width="10"
        opacity=".25"
      />
      <path
        class="mission-map-route"
        d="M88 239 L88 161 Q88 133 119 133 L248 133 L248 74 L349 74"
        fill="none"
        stroke="var(--green-ink)"
        stroke-width="2"
        stroke-dasharray="5 7"
      />
      <g stroke="var(--blue-ink)" fill="var(--blue-soft)" stroke-width="1.4">
        <path d="M321 59 L354 40 L388 59 L388 111 L321 111 Z" />
        <path
          d="M330 65 L330 105 M344 65 L344 105 M359 65 L359 105 M376 65 L376 105 M316 114 L394 114"
        />
        <path
          d="M225 151 L281 151 L281 192 L225 192 Z M222 150 L253 127 L285 150"
        />
        <path
          d="M65 232 L65 211 L112 211 L112 232 M73 232 L73 218 M104 232 L104 218"
        />
      </g>
      <g fill="var(--green-soft)" stroke="var(--green-ink)">
        <circle cx="140" cy="176" r="20" />
        <circle cx="169" cy="172" r="16" />
        <path d="M140 183 L140 204 M169 179 L169 201" />
      </g>
      <g fill="var(--ink)" font-size="12" text-anchor="middle">
        <text x="88" y="250">MAIN GATE</text>
        <text x="155" y="223">GARDEN</text>
        <text x="254" y="210">STUDENT CENTRE</text>
        <text x="356" y="133">LIBRARY</text>
        <text x="371" y="202">CAFE</text>
      </g>
      <path
        d="M349 161 L382 161 L382 182 L349 182 Z M382 163 Q402 161 398 172 Q397 179 382 178"
        fill="var(--surface)"
        stroke="var(--blue-ink)"
      />
      <circle
        class="mission-map-dot"
        cx="88"
        cy="236"
        r="6"
        fill="var(--green)"
      />
    </svg>
    <div class="mission-art-hotspots" role="group" aria-label="探索情境词语">
      <button
        v-for="(point, i) in points"
        :key="point.word"
        type="button"
        :class="{ active: selected === i }"
        :aria-pressed="selected === i"
        @click="selected = i"
      >
        {{ point.word }}
      </button>
    </div>
    <p class="mission-art-caption" aria-live="polite">
      <strong>{{ points[selected].word }}</strong
      ><span>{{ points[selected].meaning }}</span>
    </p>
  </div>
</template>
<script setup>
import { computed, ref, watch } from "vue";
const props = defineProps({ theme: { type: String, default: "water" } });
const selected = ref(0);
const points = computed(() =>
  props.theme === "water"
    ? [
        { word: "sensor", meaning: "传感器 · 检测手的靠近" },
        { word: "flow", meaning: "流动 · 手移开后停止供水" },
        { word: "reduce", meaning: "减少 · 从装置到日常习惯" },
      ]
    : [
        { word: "direction", meaning: "路线指引 · 经过花园，在学生中心左转" },
        { word: "library", meaning: "图书馆 · 在咖啡馆旁，今晚六点关闭" },
        { word: "available", meaning: "可用的 · 图书与安静的学习空间" },
      ],
);
watch(
  () => props.theme,
  () => (selected.value = 0),
);
</script>
