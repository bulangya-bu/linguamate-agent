<template>
  <button
    ref="root"
    type="button"
    class="module-art reading-art"
    :class="{ 'is-focusing': active, 'art-is-paused': !playing }"
    :aria-label="active ? '关闭句子聚焦' : '开启句子聚焦'"
    :aria-pressed="active"
    @click="$emit('toggle')"
  >
    <svg viewBox="0 0 180 150" fill="none" aria-hidden="true">
      <g class="reading-paper art-loop">
        <path
          d="M37 16H113L134 38V127H37Z"
          fill="var(--art-paper)"
          stroke="var(--art-line)"
          stroke-width="1.3"
        />
        <path
          d="M113 16V38H134"
          fill="var(--art-soft-blue)"
          stroke="var(--art-line)"
        />
        <path
          d="M53 33H91M53 42H80"
          stroke="var(--art-blue)"
          stroke-width="2.5"
          stroke-linecap="round"
        />
        <path
          d="M52 62H116M52 74H116M52 86H116M52 98H105M52 110H93"
          stroke="var(--art-orbit)"
          stroke-width="2"
          stroke-linecap="round"
        />
        <path
          class="reading-mark art-loop"
          d="M50 73H115"
          pathLength="100"
          stroke="var(--art-green)"
          stroke-width="7"
          opacity=".22"
          stroke-linecap="round"
        />
        <path
          d="M101 15H112V45L106 40L101 45Z"
          fill="var(--art-green)"
          opacity=".7"
        />
      </g>
      <g class="reading-lens art-loop">
        <path
          d="M127 105L150 128"
          stroke="var(--art-blue)"
          stroke-width="10"
          stroke-linecap="round"
        />
        <circle
          cx="108"
          cy="84"
          r="30"
          fill="var(--art-paper)"
          fill-opacity=".93"
          stroke="var(--art-blue)"
          stroke-width="2"
        />
        <circle cx="108" cy="84" r="24" stroke="var(--art-orbit)" />
        <path
          d="M92 75H119M92 84H124M92 93H115"
          stroke="var(--art-green)"
          stroke-width="2.5"
          stroke-linecap="round"
        />
        <path
          d="M90 65Q99 58 108 60"
          stroke="var(--art-blue)"
          stroke-opacity=".35"
          stroke-width="2"
          stroke-linecap="round"
        />
      </g>
      <circle
        class="reading-dot art-loop"
        cx="153"
        cy="37"
        r="3.5"
        fill="var(--art-green)"
      />
    </svg>
    <span class="module-art-hint">{{
      active ? "句子聚焦已开启" : "点亮阅读镜"
    }}</span>
  </button>
</template>

<script setup>
import { ref } from "vue";
import { useArtMotion } from "@/composables/useArtMotion";
defineProps({ active: Boolean });
defineEmits(["toggle"]);
const root = ref(null);
const { playing } = useArtMotion(root);
</script>
