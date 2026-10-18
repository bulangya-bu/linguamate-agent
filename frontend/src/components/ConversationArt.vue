<template>
  <button
    ref="root"
    type="button"
    class="module-art conversation-art"
    :class="{ 'art-is-paused': !playing }"
    :aria-label="
      mode === 'assistant'
        ? '换一个学习问题并填入输入框'
        : '换一句英语开场并填入对话框'
    "
    @click="suggest"
  >
    <svg viewBox="0 0 270 190" fill="none" aria-hidden="true">
      <path
        class="conversation-thread art-loop"
        d="M37 130C-5 65 96 5 178 35S288 140 215 167C161 189 82 166 85 119"
        stroke="var(--art-orbit)"
        stroke-dasharray="3 7"
      />
      <g class="conversation-question art-loop">
        <path
          d="M34 39Q34 26 48 26H182Q196 26 196 40V86Q196 100 182 100H76L51 118L55 100H48Q34 100 34 86Z"
          fill="var(--art-paper)"
          stroke="var(--art-line)"
          stroke-width="1.3"
        />
        <text
          x="114"
          y="69"
          text-anchor="middle"
          class="art-serif"
          fill="var(--art-blue)"
        >
          {{ current.hello }}
        </text>
        <path d="M58 84H120" stroke="var(--art-orbit)" stroke-linecap="round" />
      </g>
      <g :key="index" class="conversation-reply art-loop">
        <path
          d="M116 102H224Q238 102 238 115V145Q238 158 224 158H211L216 174L193 158H116Q102 158 102 145V116Q102 102 116 102Z"
          fill="var(--art-soft-green)"
          stroke="var(--art-green-line)"
          stroke-width="1.3"
        />
        <text
          x="170"
          y="127"
          text-anchor="middle"
          fill="var(--art-green)"
          font-size="15"
        >
          {{ current.reply }}
        </text>
        <g class="conversation-dots" fill="var(--art-green)">
          <circle class="art-loop" cx="159" cy="143" r="2" />
          <circle class="art-loop" cx="170" cy="143" r="2" />
          <circle class="art-loop" cx="181" cy="143" r="2" />
        </g>
      </g>
      <path
        d="M218 40L220 47L227 49L220 51L218 58L216 51L209 49L216 47Z"
        fill="var(--art-green)"
        opacity=".6"
      />
    </svg>
    <span class="module-art-hint"
      >{{ mode === "assistant" ? "点击，换一个学习问题" : "点击，换一句开场"
      }}<span aria-hidden="true"> ↗</span></span
    >
    <span class="sr-only" aria-live="polite">{{ announcement }}</span>
  </button>
</template>

<script setup>
import { computed, ref } from "vue";
import { useArtMotion } from "@/composables/useArtMotion";
const props = defineProps({
  scene: { type: String, default: "daily" },
  mode: { type: String, default: "practice" },
});
const emit = defineEmits(["suggest"]);
const root = ref(null),
  index = ref(0),
  announcement = ref("");
const { playing } = useArtMotion(root);
const starters = {
  daily: [
    {
      hello: "Hello!",
      reply: "Your turn.",
      prompt: "One good thing about today was ",
    },
    {
      hello: "A little story?",
      reply: "I’m listening.",
      prompt: "Something interesting happened to me recently. ",
    },
  ],
  exam: [
    {
      hello: "Your opinion?",
      reply: "Make it yours.",
      prompt: "In my opinion, learning a new language is important because ",
    },
    {
      hello: "An example?",
      reply: "Tell me more.",
      prompt: "One experience that changed my perspective was ",
    },
  ],
  business: [
    {
      hello: "Let’s connect.",
      reply: "Ideas welcome.",
      prompt: "I would like to suggest an idea for our project. ",
    },
    {
      hello: "Next step?",
      reply: "Let’s plan.",
      prompt: "Could we discuss the next steps for this project? ",
    },
  ],
  reading: [
    {
      hello: "A good read?",
      reply: "Let’s discuss.",
      prompt: "One idea from a recent article that stayed with me was ",
    },
    {
      hello: "What stood out?",
      reply: "Your view?",
      prompt: "The article made me think about ",
    },
  ],
  assistant: [
    {
      hello: "What’s next?",
      reply: "One step.",
      prompt: "根据我的学习记录，今天最值得优先练习什么？",
    },
    {
      hello: "Let’s explore.",
      reply: "Together.",
      prompt: "帮我找出最近反复出错的单词，并建议一种练习方式。",
    },
  ],
};
const choices = computed(
  () =>
    starters[props.mode === "assistant" ? "assistant" : props.scene] ||
    starters.daily,
);
const current = computed(
  () => choices.value[index.value % choices.value.length],
);
function suggest() {
  const starter = current.value;
  emit("suggest", starter.prompt);
  announcement.value = "已准备一条开场句，可在输入框继续编辑。";
  index.value++;
}
</script>
