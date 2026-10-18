<template>
  <button
    ref="root"
    type="button"
    class="module-art writing-art"
    :class="{ 'art-is-paused': !playing }"
    aria-label="切换英文表达对照示例"
    @click="index = (index + 1) % examples.length"
  >
    <svg viewBox="0 0 250 185" fill="none" aria-hidden="true">
      <ellipse
        cx="123"
        cy="156"
        rx="78"
        ry="8"
        fill="var(--art-orbit)"
        opacity=".12"
      />
      <g transform="rotate(-7 116 91)">
        <path
          d="M54 20H158L184 47V158H54Z"
          fill="var(--art-paper)"
          stroke="var(--art-line)"
          stroke-width="1.4"
        />
        <path
          d="M158 20V47H184"
          fill="var(--art-soft-blue)"
          stroke="var(--art-line)"
          stroke-width="1.2"
        />
        <path
          d="M69 40H119M69 49H104M69 130H152M69 140H129"
          stroke="var(--art-orbit)"
          stroke-width="2"
          stroke-linecap="round"
          opacity=".65"
        />
        <text
          :key="'before' + index"
          x="71"
          y="78"
          class="writing-before"
          fill="var(--muted)"
          font-family="Georgia,serif"
          :font-size="example.before.length > 12 ? 14 : 16"
        >
          {{ example.before }}
        </text>
        <path
          d="M69 73L157 80"
          stroke="var(--art-blue)"
          stroke-width="1.3"
          opacity=".6"
        />
        <text
          :key="'after' + index"
          x="71"
          y="111"
          class="writing-after"
          fill="var(--art-green)"
          font-family="Georgia,serif"
          font-style="italic"
          :font-size="example.after.length > 12 ? 15 : 18"
        >
          {{ example.after }}
        </text>
        <path
          class="writing-ink art-loop"
          d="M70 117Q108 113 158 118"
          pathLength="100"
          stroke="var(--art-green)"
          stroke-width="2.1"
          stroke-linecap="round"
        />
      </g>
      <g class="writing-pen art-loop">
        <path
          d="M169 119L185 57Q187 50 194 52L201 55Q206 57 204 63L186 125L173 139Z"
          fill="var(--art-soft-blue)"
          stroke="var(--art-blue)"
          stroke-width="1.3"
        />
        <path
          d="M173 139L174 121L186 125Z"
          fill="var(--art-paper)"
          stroke="var(--art-blue)"
          stroke-width="1.2"
        />
        <path
          d="M173 139L180 127M184 62L199 67M191 56L195 42"
          stroke="var(--art-blue)"
          stroke-width="1.3"
          stroke-linecap="round"
        />
      </g>
      <path
        class="art-spark art-loop"
        d="M30 91L32 98L39 100L32 102L30 109L28 102L21 100L28 98Z"
        fill="var(--art-green)"
        opacity=".5"
      />
    </svg>
    <span class="module-art-hint"
      >{{ example.note }}<span aria-hidden="true"> ↻</span></span
    >
    <span class="sr-only" aria-live="polite"
      >表达示例：{{ example.before }}，换一种说法：{{
        example.after
      }}。具体用法需结合语境。</span
    >
  </button>
</template>

<script setup>
import { computed, ref } from "vue";
import { useArtMotion } from "@/composables/useArtMotion";
const root = ref(null),
  index = ref(0);
const { playing } = useArtMotion(root);
const examples = [
  { before: "very good", after: "remarkable", note: "让赞美更具体" },
  { before: "I think", after: "I believe", note: "试试另一种开场" },
  { before: "very important", after: "essential", note: "用一个词，说清重点" },
  { before: "help people", after: "support others", note: "为表达换一个角度" },
];
const example = computed(() => examples[index.value]);
</script>
