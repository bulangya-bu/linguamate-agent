<template>
  <button
    ref="root"
    type="button"
    class="studio-book"
    :class="{ 'is-flipping': flipping, 'art-is-paused': !playing }"
    aria-label="翻一页，随机认识一个单词"
    :aria-disabled="flipping"
    @click="turnPage"
  >
    <svg viewBox="0 0 540 420" fill="none" aria-hidden="true">
      <defs>
        <linearGradient
          :id="id + '-left'"
          x1="80"
          y1="110"
          x2="270"
          y2="320"
          gradientUnits="userSpaceOnUse"
        >
          <stop stop-color="var(--studio-paper-top, #ffffff)" />
          <stop offset="1" stop-color="var(--studio-paper-fold, #e0efdf)" />
        </linearGradient>
        <linearGradient
          :id="id + '-right'"
          x1="280"
          y1="150"
          x2="470"
          y2="260"
          gradientUnits="userSpaceOnUse"
        >
          <stop stop-color="var(--studio-paper-fold, #e0efdf)" />
          <stop offset=".18" stop-color="var(--studio-paper-top, #ffffff)" />
          <stop offset="1" stop-color="var(--studio-paper-edge, #eaf5fa)" />
        </linearGradient>
        <linearGradient
          :id="id + '-cover'"
          x1="90"
          y1="220"
          x2="460"
          y2="330"
          gradientUnits="userSpaceOnUse"
        >
          <stop stop-color="#84c947" />
          <stop offset="1" stop-color="#459cb4" />
        </linearGradient>
        <filter
          :id="id + '-shadow'"
          x="0"
          y="0"
          width="540"
          height="420"
          filterUnits="userSpaceOnUse"
        >
          <feGaussianBlur stdDeviation="13" />
        </filter>
      </defs>
      <ellipse
        cx="283"
        cy="340"
        rx="165"
        ry="19"
        fill="var(--studio-book-shadow, #437b8b)"
        opacity=".13"
        :filter="'url(#' + id + '-shadow)'"
      />
      <g
        class="studio-book-orbits art-loop"
        stroke="var(--studio-orbit, #9fc8d5)"
      >
        <ellipse
          cx="275"
          cy="216"
          rx="232"
          ry="112"
          transform="rotate(-24 275 216)"
          stroke-opacity=".55"
        />
        <path
          d="M89 121C150 18 395 73 452 229"
          stroke-dasharray="2 8"
          stroke-opacity=".6"
        />
        <circle cx="66" cy="257" r="4" fill="var(--blue)" stroke="none" />
        <circle cx="463" cy="142" r="5" fill="var(--green)" stroke="none" />
      </g>
      <g class="studio-book-spread art-loop">
        <path
          d="M74 152C150 124 210 145 270 181C336 140 407 125 472 150L472 280C396 261 332 285 270 328C204 285 139 265 74 285Z"
          :fill="'url(#' + id + '-cover)'"
        />
        <path
          d="M79 143C150 116 212 137 270 174C335 133 404 118 467 141L467 271C395 253 329 278 270 319C207 277 142 256 79 277Z"
          fill="var(--studio-paper-under, #cfe3d7)"
        />
        <path
          d="M82 135C153 110 212 131 270 166C335 126 402 111 464 134L464 264C393 245 329 271 270 312C207 270 144 250 82 269Z"
          fill="var(--studio-paper-edge, #eaf5fa)"
        />
        <path
          d="M84 121C155 99 214 122 270 158V304C210 266 149 244 84 256Z"
          :fill="'url(#' + id + '-left)'"
          stroke="var(--studio-paper-stroke, #d6e7e4)"
        />
        <path
          d="M270 158C333 119 399 105 462 123V254C393 239 330 265 270 304Z"
          :fill="'url(#' + id + '-right)'"
          stroke="var(--studio-paper-stroke, #d6e7e4)"
        />
        <path
          d="M270 161V299"
          stroke="var(--studio-paper-seam, #a9c9b5)"
          stroke-opacity=".65"
          stroke-width="2"
        />
        <g class="book-page-copy">
          <text
            x="178"
            y="181"
            transform="rotate(13 178 181)"
            fill="var(--studio-print, #a6c8d4)"
            font-family="Georgia,serif"
            font-size="11"
            letter-spacing="2"
          >
            WORD {{ String(page).padStart(2, "0") }}
          </text>
          <text
            x="178"
            y="216"
            text-anchor="middle"
            transform="rotate(13 178 216)"
            fill="var(--studio-letter-ink, #3b7b40)"
            font-family="Georgia,serif"
            font-style="italic"
            :font-size="word.en.length > 9 ? 27 : 35"
            class="book-discovery-word"
          >
            {{ word.en }}
          </text>
          <g transform="rotate(-12 365 204)">
            <text
              x="365"
              y="181"
              text-anchor="middle"
              fill="var(--studio-print, #a6c8d4)"
              font-size="11"
              letter-spacing="2"
            >
              MEANING
            </text>
            <text
              x="365"
              y="212"
              text-anchor="middle"
              fill="var(--studio-letter-ink, #3b7b40)"
              font-size="20"
              class="book-discovery-meaning"
            >
              <tspan
                v-for="(line, index) in meaningLines"
                :key="index"
                x="365"
                :dy="index ? 27 : 0"
              >
                {{ line }}
              </tspan>
            </text>
          </g>
        </g>
        <path
          d="M317 251Q350 236 395 235"
          stroke="var(--studio-print, #a6c8d4)"
          stroke-width="2"
          stroke-linecap="round"
          opacity=".6"
        />
        <path
          class="book-turn-sheet"
          d="M270 158C333 119 399 105 462 123V254C393 239 330 265 270 304Z"
          :fill="'url(#' + id + '-right)'"
          stroke="var(--studio-paper-stroke, #d6e7e4)"
        />
        <path
          class="book-ribbon art-loop"
          d="M280 295L280 329L294 315L308 319L308 279"
          fill="var(--blue)"
          opacity=".85"
        />
      </g>
      <path
        class="book-spark art-loop"
        d="m425 72 3 10 10 3-10 3-3 10-3-10-10-3 10-3Z"
        fill="var(--studio-spark, #78b78a)"
      />
      <path
        class="book-spark book-spark-small art-loop"
        d="m106 310 2 6 6 2-6 2-2 6-2-6-6-2 6-2Z"
        fill="var(--blue)"
        opacity=".65"
      />
    </svg>
    <span class="studio-book-hint"
      >点击翻页<span aria-hidden="true"> ↗</span></span
    >
    <span class="art-sr-only" aria-live="polite">{{ announcement }}</span>
  </button>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, useId, watch } from "vue";
import { useArtMotion } from "@/composables/useArtMotion";
import { shuffledWords } from "@/learning-art";
const id = "studio-book-" + useId();
const root = ref(null),
  flipping = ref(false),
  page = ref(1),
  announcement = ref("");
const { playing, reducedMotion } = useArtMotion(root);
let previous = "";
try {
  previous = sessionStorage.getItem("linguamate:discovery-book:last") || "";
} catch {
  /* Optional session history. */
}
let deck = shuffledWords(previous),
  pending = null,
  timers = [];
const word = ref(deck.shift());
const meaningLines = computed(() => word.value.cn.split("；"));
function remember() {
  try {
    sessionStorage.setItem("linguamate:discovery-book:last", word.value.en);
  } catch {
    /* Reading works without storage. */
  }
}
remember();
function reveal() {
  if (!pending) return;
  word.value = pending;
  pending = null;
  page.value++;
  remember();
  announcement.value = `第 ${page.value} 页：${word.value.en}，${word.value.cn}`;
}
function finish() {
  reveal();
  flipping.value = false;
  timers.forEach(clearTimeout);
  timers = [];
}
function turnPage() {
  if (flipping.value) return;
  if (!deck.length) deck = shuffledWords(word.value.en);
  pending = deck.shift();
  if (reducedMotion.value) {
    finish();
    return;
  }
  flipping.value = true;
  timers = [setTimeout(reveal, 340), setTimeout(finish, 720)];
}
watch(reducedMotion, (reduced) => {
  if (reduced) finish();
});
onBeforeUnmount(() => timers.forEach(clearTimeout));
</script>

<style scoped>
.studio-book {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  padding: 0;
  border: 0;
  border-radius: 24px;
  background: transparent;
  color: var(--studio-accent, var(--green-ink));
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}
.studio-book:focus-visible {
  outline: 2px solid var(--blue);
  outline-offset: -10px;
}
.studio-book > svg {
  width: 112%;
  max-width: 560px;
  height: auto;
  max-height: 100%;
  overflow: visible;
}
.studio-book-spread {
  transform-origin: 270px 220px;
  filter: drop-shadow(0 15px 12px #254e4d14);
  animation: studio-page-float 6.8s ease-in-out infinite;
  animation-delay: -1.1s;
}
.studio-book-orbits {
  transform-origin: 275px 216px;
  animation: studio-orbit-breathe 10s ease-in-out infinite;
  animation-delay: -3s;
}
.studio-book-orbits path {
  animation: studio-orbit-flow 12s linear infinite;
}
.book-ribbon {
  transform-origin: 294px 288px;
  animation: studio-ribbon-sway 4.6s ease-in-out infinite;
}
.book-spark {
  transform-box: fill-box;
  transform-origin: center;
  animation: studio-spark-bloom 3.6s ease-in-out infinite;
}
.book-spark-small {
  animation-delay: -1.4s;
}
.studio-book-hint {
  position: absolute;
  bottom: 9%;
  left: 50%;
  transform: translateX(-50%);
  color: var(--muted);
  font-size: 11px;
  opacity: 0.65;
  white-space: nowrap;
  transition: opacity 180ms;
}
.studio-book:hover .studio-book-hint,
.studio-book:focus-visible .studio-book-hint {
  opacity: 1;
}
.studio-book:hover .art-loop,
.studio-book:focus-visible .art-loop,
.studio-book:hover .studio-book-orbits path,
.studio-book:focus-visible .studio-book-orbits path {
  animation-play-state: paused;
}
.studio-book.art-is-paused .art-loop,
.studio-book.art-is-paused .studio-book-orbits path {
  animation-play-state: paused;
}
.book-turn-sheet {
  opacity: 0;
  transform-origin: 270px 216px;
}
.is-flipping .book-turn-sheet {
  animation: studio-book-turn 720ms cubic-bezier(0.35, 0.05, 0.25, 1) both;
}
.is-flipping .book-page-copy {
  animation: studio-book-copy 720ms ease both;
}
@keyframes studio-book-turn {
  0% {
    opacity: 0;
    transform: scaleX(1);
  }
  12% {
    opacity: 1;
  }
  45% {
    transform: scaleX(0.08);
  }
  72% {
    opacity: 1;
    transform: scaleX(-0.85);
  }
  100% {
    opacity: 0;
    transform: scaleX(-1);
  }
}
@keyframes studio-book-copy {
  0%,
  24% {
    opacity: 1;
  }
  38%,
  52% {
    opacity: 0;
  }
  82%,
  100% {
    opacity: 1;
  }
}
@keyframes studio-page-float {
  0%,
  100% {
    transform: translate(2px, 5px) rotate(-9deg);
  }
  50% {
    transform: translate(-3px, -16px) rotate(-3deg);
  }
}
@keyframes studio-orbit-breathe {
  0%,
  100% {
    transform: rotate(-4deg) scale(0.96);
  }
  50% {
    transform: rotate(4deg) scale(1.04);
  }
}
@keyframes studio-orbit-flow {
  to {
    stroke-dashoffset: -120;
  }
}
@keyframes studio-ribbon-sway {
  0%,
  100% {
    transform: rotate(-7deg);
  }
  50% {
    transform: rotate(8deg) translateY(2px);
  }
}
@keyframes studio-spark-bloom {
  0%,
  100% {
    transform: scale(0.8) rotate(-8deg);
    opacity: 0.55;
  }
  50% {
    transform: scale(1.22) rotate(12deg);
    opacity: 0.95;
  }
}
@media (prefers-reduced-motion: reduce) {
  .studio-book .art-loop,
  .studio-book-orbits path {
    animation: none;
  }
  .studio-book-spread {
    transform: rotate(-6deg);
  }
}
</style>
