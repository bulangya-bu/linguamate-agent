<script setup>
import { computed } from "vue";
import AppIcon from "@/components/AppIcon.vue";
import DashboardFootprint from "./DashboardFootprint.vue";
import DashboardNextStep from "./DashboardNextStep.vue";
import DashboardToday from "./DashboardToday.vue";

const props = defineProps({
  dashboard: { type: Object, required: true },
  activeSection: String,
  goSection: Function,
  openLearningAssistant: Function,
  words: { type: Array, default: () => [] },
  report: { type: Object, default: () => ({}) },
});
defineEmits(["word"]);

const {
  dashboardExpanded,
  recentStudy,
  todayTasks,
  todayTasksLoading,
  nextAction,
  recentPractice,
  studyActionBusy,
  openStudyAction,
} = props.dashboard;

const dailyWords = computed(() => props.words.slice(0, 4));
const nextTask = computed(() => todayTasks.value?.find((task) => task.id === nextAction.value));
</script>

<template>
  <section
    id="dashboard"
    class="section study-overview"
    :class="{ active: activeSection === 'dashboard', 'is-expanded': dashboardExpanded }"
  >
    <DashboardNextStep
      :due-words="report.dueWords || 0"
      :next-action="nextAction"
      :task-title="nextTask?.title"
      :recent-practice="recentPractice"
      :busy="studyActionBusy"
      :active="activeSection === 'dashboard'"
      @start="openStudyAction(nextAction)"
    />

    <button
      class="dashboard-details-toggle"
      type="button"
      :aria-expanded="dashboardExpanded"
      aria-controls="dashboard-tasks dashboard-words dashboard-footprint"
      @click="dashboardExpanded = !dashboardExpanded"
    >
      <span>{{ dashboardExpanded ? "收起学习概况" : "查看学习概况" }}</span>
      <span aria-hidden="true">{{ dashboardExpanded ? "−" : "+" }}</span>
    </button>

    <DashboardToday
      id="dashboard-tasks"
      class="study-summary"
      :tasks="todayTasks"
      :loading="todayTasksLoading"
      :busy="studyActionBusy"
      @start="openStudyAction"
    />

    <section id="dashboard-words" class="study-words" aria-labelledby="study-words-title">
      <div class="study-words-heading">
        <h2 id="study-words-title">今日单词</h2>
        <div class="study-word-actions">
          <button class="study-bank-link" type="button" @click="goSection('wordbank')">
            <AppIcon name="wordbank" />探索词库
          </button>
          <button class="study-assistant" type="button" @click="openLearningAssistant">
            <AppIcon name="spark" />学习助手
          </button>
        </div>
      </div>
      <div v-if="dailyWords.length" class="study-word-list">
        <button
          v-for="word in dailyWords"
          :key="word.id"
          class="study-word"
          type="button"
          :aria-label="word.word + '，查看单词详情'"
          @click="$emit('word', word)"
        >
          <span class="study-word-letter" aria-hidden="true">{{ word.word[0] }}</span>
          <strong>{{ word.word }}</strong>
          <span class="study-word-meaning">{{ word.meaningCn }}</span>
          <AppIcon name="arrow" class="study-word-arrow" />
        </button>
      </div>
      <p v-else class="study-word-empty" role="status">暂无推荐单词</p>
    </section>

    <DashboardFootprint
      id="dashboard-footprint"
      :activity="recentStudy"
      :loading="todayTasksLoading"
    />
  </section>
</template>

<style scoped>
#dashboard.study-overview.active {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  grid-template-rows: auto;
  grid-auto-rows: min-content;
  align-content: start;
  gap: 20px 22px;
  min-width: 0;
  min-height: 0;
  padding: 1px 1px 10px;
  overflow: auto;
}

.study-next,
.study-summary,
.study-words {
  min-width: 0;
}

.study-overview.active > :is(.study-next, .study-summary, .study-words, .study-footprint) {
  animation: study-reveal 570ms var(--ease) both;
}

.study-overview.active > .study-summary { animation-delay: 55ms; }
.study-overview.active > .study-words { animation-delay: 105ms; }
.study-overview.active > .study-footprint { animation-delay: 145ms; }

.study-words-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 9px 14px;
  margin: 4px 0 13px;
}

.study-words-heading h2 {
  margin: 0;
  color: var(--ink);
  font-size: clamp(19px, 1.8vw, 27px);
  font-weight: 650;
  letter-spacing: -.035em;
}

.study-word-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
}

.study-word-actions > button {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-height: 35px;
  padding: 0 10px;
  border: 1px solid transparent;
  border-radius: 9px;
  font-size: 13px;
  font-weight: 500;
}

.study-word-actions svg {
  flex: none;
  width: 16px;
  height: 16px;
}

.study-bank-link {
  color: var(--muted);
}

.study-bank-link:hover {
  color: var(--ink);
  border-color: var(--line);
  background: var(--surface);
}

.study-assistant {
  color: var(--blue-ink);
  background: color-mix(in srgb, var(--surface), var(--blue) 9%);
}

.study-assistant:hover {
  border-color: color-mix(in srgb, var(--line), var(--blue) 29%);
  background: color-mix(in srgb, var(--surface), var(--blue) 14%);
}

.study-word-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 164px), 1fr));
  gap: 11px;
}

.study-word {
  --word-accent: #83b86a;
  position: relative;
  isolation: isolate;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
  min-height: 113px;
  padding: 18px 31px 16px 17px;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 15px;
  color: var(--ink);
  background: var(--surface);
  text-align: left;
}

.study-word:nth-child(2) { --word-accent: #69acbf; }
.study-word:nth-child(3) { --word-accent: #cc9c61; }
.study-word:nth-child(4) { --word-accent: #8b9ac4; }

.study-word::before {
  position: absolute;
  top: 0;
  left: 17px;
  width: 25px;
  height: 3px;
  border-radius: 0 0 3px 3px;
  background: var(--word-accent);
  content: "";
}

.study-word:hover {
  border-color: var(--word-accent);
  background: color-mix(in srgb, var(--surface), var(--word-accent) 6%);
  box-shadow: 0 6px 16px #233e3212;
  transform: translateY(-2px);
}

.study-word:focus-visible {
  outline-offset: -4px;
}

.study-word > strong {
  max-width: 100%;
  overflow: hidden;
  color: var(--ink);
  font: clamp(20px, 2vw, 32px)/1.08 Georgia, serif;
  letter-spacing: -.03em;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.study-word-letter {
  position: absolute;
  z-index: -1;
  right: 6px;
  bottom: -28px;
  color: var(--word-accent);
  font: italic 114px/1 Georgia, serif;
  opacity: .15;
  pointer-events: none;
  text-transform: uppercase;
}

.study-word-meaning {
  display: -webkit-box;
  overflow: hidden;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.55;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.study-word-arrow {
  position: absolute;
  right: 11px;
  bottom: 14px;
  width: 16px;
  height: 16px;
  color: var(--muted);
  transition: transform 200ms var(--ease), color 200ms;
}

.study-word:hover > .study-word-arrow {
  color: var(--green-ink);
  transform: translateX(2px);
}

.study-word-empty {
  margin: 0;
  padding: 22px 16px;
  border: 1px solid var(--line);
  border-radius: 15px;
  color: var(--muted);
  background: var(--surface);
}

@media (min-width: 981px) {
  #dashboard.study-overview.active {
    grid-template-columns: minmax(0, 2.05fr) minmax(245px, 1fr);
  }
  .study-words,
  .study-footprint {
    grid-column: 1 / -1;
  }
  .study-words {
    margin-top: 14px;
  }
}

@media (max-width: 760px) {
  #dashboard.study-overview.active {
    gap: 13px;
  }
  .study-overview:not(.is-expanded) :is(.study-summary, .study-words, .study-footprint) {
    display: none;
  }
  .study-word-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@keyframes study-reveal {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (prefers-reduced-motion: reduce) {
  .study-overview.active > :is(.study-next, .study-summary, .study-words, .study-footprint) {
    animation: none;
  }
  .study-word,
  .study-word-arrow {
    transition: none;
  }
  .study-word:hover,
  .study-word:hover > .study-word-arrow {
    transform: none;
  }
}
</style>
