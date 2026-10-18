<template>
  <button
    ref="root"
    class="auth-garden"
    :class="{ 'is-paused': !playing }"
    type="button"
    aria-label="点击花朵，认识另一个单词"
    @click="nextWord"
  >
    <svg
      class="auth-garden-art"
      viewBox="0 0 480 360"
      fill="none"
      aria-hidden="true"
    >
      <defs>
        <linearGradient
          id="auth-petal-green"
          x1="150"
          y1="50"
          x2="270"
          y2="190"
          gradientUnits="userSpaceOnUse"
        >
          <stop stop-color="#c4e6a9" stop-opacity=".7" />
          <stop offset="1" stop-color="#8cc968" stop-opacity=".2" />
        </linearGradient>
        <linearGradient
          id="auth-petal-blue"
          x1="150"
          y1="50"
          x2="270"
          y2="190"
          gradientUnits="userSpaceOnUse"
        >
          <stop stop-color="#c4e9f9" stop-opacity=".8" />
          <stop offset="1" stop-color="#79c4dc" stop-opacity=".2" />
        </linearGradient>
      </defs>
      <ellipse
        class="garden-ground"
        cx="247"
        cy="314"
        rx="125"
        ry="12"
        fill="currentColor"
        opacity=".05"
      />
      <g class="garden-orbit">
        <ellipse
          cx="245"
          cy="171"
          rx="185"
          ry="93"
          transform="rotate(-26 245 171)"
          stroke="currentColor"
          opacity=".23"
        />
        <circle
          cx="245"
          cy="171"
          r="139"
          stroke="currentColor"
          opacity=".13"
          stroke-dasharray="2 8"
        />
        <circle cx="82" cy="235" r="4" fill="var(--blue)" />
        <circle cx="409" cy="108" r="5" fill="var(--green)" />
      </g>
      <g class="garden-flower">
        <g class="garden-petals">
          <path
            v-for="(angle, index) in [0, 60, 120, 180, 240, 300]"
            :key="angle"
            :transform="`rotate(${angle} 245 171)`"
            d="M245 171C163 148 150 69 191 50C238 28 274 109 245 171Z"
            :fill="`url(#auth-petal-${index % 2 ? 'blue' : 'green'})`"
            :stroke="index % 2 ? '#8fcadd' : '#9bc47e'"
            stroke-opacity=".5"
          />
        </g>
        <circle
          cx="245"
          cy="171"
          r="43"
          fill="var(--surface)"
          stroke="var(--line)"
        />
        <text
          x="245"
          y="184"
          text-anchor="middle"
          font-family="Georgia, serif"
          font-size="43"
          font-style="italic"
          fill="var(--green-ink)"
        >
          Aa
        </text>
      </g>
      <path
        class="garden-spark"
        d="m367 48 4 12 12 4-12 4-4 12-4-12-12-4 12-4Z"
        fill="var(--blue)"
        opacity=".65"
      />
      <path
        d="M93 121v14m-7-7h14M385 270v10m-5-5h10"
        stroke="currentColor"
        stroke-linecap="round"
        opacity=".4"
      />
    </svg>
    <span class="auth-garden-caption">
      <Transition name="garden-word" mode="out-in">
        <span :key="word.en" class="auth-garden-word"
          ><strong>{{ word.en }}</strong
          ><span>{{ word.cn }}</span></span
        >
      </Transition>
      <span class="auth-garden-next" aria-hidden="true"
        ><AppIcon name="arrow"
      /></span>
    </span>
    <span class="auth-garden-hint">轻点花朵，发现一个新词</span>
    <span class="sr-only" role="status" aria-live="polite">{{
      announcement
    }}</span>
  </button>
</template>

<script setup>
import { ref, shallowRef } from "vue";
import AppIcon from "./AppIcon.vue";
import { shuffledWords } from "@/learning-art";
import { useArtMotion } from "@/composables/useArtMotion";

const root = ref(null);
const { playing } = useArtMotion(root);
let deck = shuffledWords();
const word = shallowRef(deck.shift());
const announcement = ref("");
function nextWord() {
  if (!deck.length) deck = shuffledWords(word.value.en);
  word.value = deck.shift();
  announcement.value = `${word.value.en}，${word.value.cn}`;
}
</script>
