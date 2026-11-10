<script setup>
import { computed } from "vue";
import AppIcon from "@/components/AppIcon.vue";
import DashboardStudyArt from "./DashboardStudyArt.vue";

const props = defineProps({
  dueWords: { type: Number, default: 0 },
  nextAction: { type: String, default: "new-words" },
  taskTitle: { type: String, default: "学习新单词" },
  recentPractice: { type: Object, default: null },
  busy: Boolean,
  active: Boolean,
});
defineEmits(["start"]);

const actionLabels = {
  review: "开始复习",
  "new-words": "开始学新词",
  chat: "开始对话",
  writing: "去写作文",
  reading: "去阅读",
  conversation: "继续上次对话",
  journal: "查看学习档案",
};
const actionLabel = computed(() => actionLabels[props.nextAction]);
const planFinished = computed(() =>
  props.nextAction === "conversation" || props.nextAction === "journal",
);
</script>

<template>
  <section class="study-next" aria-labelledby="study-next-title">
    <div class="study-next-copy">
      <span class="study-next-label"><i></i>{{ planFinished ? "今日进度" : "下一步" }}</span>
      <h2 v-if="nextAction === 'review'" id="study-next-title">
        <span class="study-next-count">{{ dueWords }}</span> 个单词<br />待复习
      </h2>
      <h2 v-else-if="planFinished" id="study-next-title">今日待办<br />已完成</h2>
      <h2 v-else id="study-next-title">{{ taskTitle }}</h2>
      <p v-if="nextAction === 'conversation' && recentPractice?.title" class="study-next-conversation">
        {{ recentPractice.title }}
      </p>
      <button class="study-start" type="button" :disabled="busy" @click="$emit('start')">
        <span>{{ busy ? "正在打开…" : actionLabel }}</span>
        <AppIcon name="arrow" />
      </button>
    </div>
    <div class="study-next-art">
      <DashboardStudyArt v-if="active" />
    </div>
  </section>
</template>

<style scoped>
.study-next {
  position: relative;
  isolation: isolate;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, .95fr);
  align-items: center;
  min-height: clamp(270px, 39dvh, 390px);
  overflow: hidden;
  border: 1px solid #386457;
  border-radius: 25px;
  color: #f5faee;
  background:
    radial-gradient(ellipse at 87% 17%, #377277a6, transparent 63%),
    linear-gradient(118deg, #15342b 0%, #1c493e 57%, #26595d 100%);
  box-shadow: 0 12px 28px #1b493716;
}

.study-next::before {
  position: absolute;
  z-index: -1;
  inset: 0;
  content: "";
  background-image:
    linear-gradient(#a6d5ca16 1px, transparent 1px),
    linear-gradient(90deg, #a6d5ca16 1px, transparent 1px);
  background-size: 35px 35px;
  mask-image: linear-gradient(110deg, transparent 27%, #000 83%);
  pointer-events: none;
}

.study-next-copy {
  z-index: 1;
  min-width: 0;
  padding: 30px 0 30px clamp(26px, 3.3vw, 54px);
}

.study-next-label {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  color: #d0e9d4;
  font-size: 13px;
  font-weight: 550;
  letter-spacing: .1em;
}

.study-next-label i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #c0ed9e;
}

.study-next h2 {
  margin: 14px 0 22px;
  color: inherit;
  font-size: clamp(30px, 3.45vw, 60px);
  font-weight: 650;
  line-height: 1.12;
  letter-spacing: -.045em;
  text-wrap: balance;
}

.study-next-count {
  color: #d3f4b1;
  font-family: Georgia, serif;
  font-size: 1.12em;
  font-weight: 500;
  font-variant-numeric: lining-nums tabular-nums;
}

.study-next-conversation {
  margin: -9px 0 18px;
  overflow: hidden;
  color: #cde2d8;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.study-start {
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  gap: 19px;
  min-height: 48px;
  padding: 0 18px;
  border: 1px solid #dcf8c4;
  border-radius: 12px;
  color: #1b3b2c;
  background: #c8efa4;
  font-weight: 650;
  text-align: left;
  box-shadow: 0 3px 0 #173d34;
}

.study-start:hover:not(:disabled) {
  background: #e0f9ca;
  transform: translateY(-2px);
}

.study-start:focus-visible {
  outline-color: #ddffbb;
}

.study-start:disabled {
  opacity: .7;
  cursor: wait;
}

.study-start > svg {
  flex: none;
  width: 19px;
  height: 19px;
  transition: transform 200ms var(--ease);
}

.study-start:hover > svg {
  transform: translateX(3px);
}

.study-next-art {
  align-self: stretch;
  min-width: 0;
  min-height: 0;
  padding-right: clamp(7px, 1.5vw, 24px);
}

@media (max-width: 760px) {
  .study-next {
    min-height: 220px;
  }
  .study-next-copy {
    padding: 23px 0 23px 20px;
  }
  .study-next h2 {
    font-size: clamp(24px, 6vw, 39px);
  }
  .study-start {
    min-height: 40px;
    gap: 8px;
    padding-inline: 11px;
    font-size: 12px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .study-start,
  .study-start > svg {
    transition: none;
  }
  .study-start:hover:not(:disabled),
  .study-start:hover > svg {
    transform: none;
  }
}
</style>
