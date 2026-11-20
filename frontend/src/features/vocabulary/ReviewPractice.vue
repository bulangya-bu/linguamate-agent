<script setup>
import { computed } from "vue";
import AppIcon from "@/components/AppIcon.vue";
import { phonetic, reviewPrompt } from "./wordPresentation";
const props = defineProps({
  vocabulary: Object,
  activeSection: String,
  goSection: Function,
  returnToArticle: Function,
  openEvidence: Function,
  report: Object,
});
const {
  state: vocabularyState,
  currentReviewWord,
  reviewTotal,
  reviewRemaining,
  reviewProgress,
  setReviewMode,
  loadReviews,
  submitReview,
  advanceReview,
} = props.vocabulary;

const reviewLocked = computed(() => vocabularyState.reviewBusy || vocabularyState.reviewLoading);
const currentReviewResult = computed(() => vocabularyState.lastReviewResult);
const canCheckAnswer = computed(() =>
  Boolean(vocabularyState.reviewForm.spellingAnswer && vocabularyState.reviewForm.meaningAnswer),
);
const finishingReview = computed(() =>
  currentReviewResult.value?.correct && reviewRemaining.value === 1,
);
const reviewActionLabel = computed(() => {
  if (vocabularyState.reviewBusy) return "检查中…";
  if (finishingReview.value) return "完成";
  return currentReviewResult.value ? "下一题" : "检查";
});
const prompt = computed(() => reviewPrompt(currentReviewWord.value));
const answerFields = computed(() => {
  const result = currentReviewResult.value;
  return [
    {
      key: "spellingAnswer",
      label: prompt.value.inflected ? "英文原形" : "英文拼写",
      placeholder: prompt.value.inflected ? "填写词典原形" : "填写完整单词",
      correct: result?.spellingCorrect ?? (result?.correct ? true : null),
      expected: result?.expectedWord || currentReviewWord.value?.word,
    },
    {
      key: "meaningAnswer",
      label: "中文释义",
      placeholder: "填写一个中文义项",
      correct: result?.meaningCorrect ?? (result?.correct ? true : null),
      expected: result?.expectedMeaning,
    },
  ];
});

function handleReviewAction(event) {
  if (!currentReviewResult.value) return;
  event.preventDefault();
  advanceReview();
}

function handleAnswerEnter(event, field) {
  if (!matchMedia('(pointer: coarse)').matches) return;
  if (event.isComposing || event.keyCode === 229 || reviewLocked.value || currentReviewResult.value) return;
  event.preventDefault();
  const input = event.currentTarget;
  if (field.key === 'spellingAnswer') {
    input.form.elements.namedItem('meaningAnswer')?.focus();
  } else if (canCheckAnswer.value) {
    input.blur();
    input.form.requestSubmit();
  } else {
    input.form.elements.namedItem('spellingAnswer')?.focus();
  }
}
</script>

<template>
  <section
    id="review"
    class="section"
    :class="{ active: activeSection === 'review' }"
  >
    <div class="review-workspace">
      <header class="review-toolbar">
        <div class="review-mode-switch" role="group" aria-label="复习类型">
          <button
            type="button"
            :class="{ active: vocabularyState.reviewMode === 'today' }"
            :aria-pressed="vocabularyState.reviewMode === 'today'"
            :disabled="reviewLocked"
            @click="setReviewMode('today')"
          >
            <AppIcon name="calendar" />今日到期<span>{{ report.dueWords || 0 }}</span>
          </button>
          <button
            type="button"
            :class="{ active: vocabularyState.reviewMode === 'mistakes' }"
            :aria-pressed="vocabularyState.reviewMode === 'mistakes'"
            :disabled="reviewLocked"
            @click="setReviewMode('mistakes')"
          >
            <AppIcon name="review" />错词强化<span>{{ report.reviewWords || 0 }}</span>
          </button>
        </div>
        <button class="evidence-open" type="button" @click="openEvidence()">
          学习变化<AppIcon name="arrow" />
        </button>
      </header>
      <div
        v-if="vocabularyState.reviewCompleted"
        class="panel review-state is-complete"
      >
        <h3>复习完成</h3>
        <dl class="review-summary">
          <div>
            <dt>完成词数</dt>
            <dd>{{ vocabularyState.reviewTotal }}</dd>
          </div>
          <div>
            <dt>答错次数</dt>
            <dd>{{ vocabularyState.reviewMistakes }}</dd>
          </div>
        </dl>
        <div class="review-card-actions">
          <button
            class="settings-secondary"
            type="button"
            :disabled="reviewLocked"
            @click="loadReviews()"
          >
            再练一组
          </button>
          <button
            class="quiz-primary"
            type="button"
            @click="goSection('vocabulary')"
          >
            学新词<AppIcon name="arrow" />
          </button>
        </div>
      </div>
      <template v-else-if="currentReviewWord">
        <article class="panel review-challenge-card">
          <div class="quiz-progress">
            <span>剩余 {{ reviewRemaining }} / {{ reviewTotal }} 词</span>
            <div
              class="quiz-progress-track"
              role="progressbar"
              aria-label="本组复习进度"
              :aria-valuenow="reviewTotal - reviewRemaining"
              :aria-valuemax="reviewTotal"
            >
              <i :style="{ width: reviewProgress + '%' }"></i>
            </div>
          </div>
          <div class="review-prompt">
            <div class="review-prompt-heading">
              <h2>{{ prompt.parts ? "例句填空" : "拼写还原" }}</h2>
              <div class="review-prompt-meta">
                <span v-if="phonetic(currentReviewWord)">{{ phonetic(currentReviewWord) }}</span>
                <span v-if="currentReviewWord.partOfSpeech">{{ currentReviewWord.partOfSpeech }}</span>
                <span v-if="currentReviewWord.wrongCount">错 {{ currentReviewWord.wrongCount }} 次</span>
              </div>
            </div>
            <div class="review-filled-example">
              <p v-if="prompt.parts" lang="en">
                <template v-for="(part, index) in prompt.parts" :key="index">
                  <mark v-if="index % 2" :class="{ 'review-blank': !currentReviewResult }">{{ currentReviewResult ? part : "_____" }}</mark>
                  <template v-else>{{ part }}</template>
                </template>
              </p>
              <p v-else>{{ currentReviewResult ? currentReviewWord.word : prompt.hint }}</p>
              <span v-if="currentReviewResult && prompt.translation" lang="zh-CN">{{ prompt.translation }}</span>
            </div>
            <button
              v-if="currentReviewWord.sourceTitle"
              class="review-source"
              type="button"
              @click="returnToArticle(currentReviewWord.sourceArticleId)"
            >
              <AppIcon name="news" /><span>{{ currentReviewResult ? currentReviewWord.sourceTitle : "查看原文" }}</span><AppIcon name="arrow" />
            </button>
          </div>
          <form
            id="review-answer-form"
            class="review-challenge-form"
            @submit.prevent="submitReview"
          >
            <div class="review-input-grid">
              <div v-for="field in answerFields" :key="field.key" class="review-answer-field">
                <label :for="'review-' + field.key">{{ field.label }}</label>
                <input
                  :id="'review-' + field.key"
                  v-model.trim="vocabularyState.reviewForm[field.key]"
                  :name="field.key"
                  :lang="field.key === 'spellingAnswer' ? 'en' : 'zh-CN'"
                  :enterkeyhint="field.key === 'spellingAnswer' ? 'next' : 'done'"
                  autocomplete="off"
                  autocapitalize="none"
                  spellcheck="false"
                  :placeholder="field.placeholder"
                  :readonly="reviewLocked || Boolean(currentReviewResult)"
                  :aria-invalid="currentReviewResult && field.correct === false ? true : undefined"
                  :aria-describedby="currentReviewResult ? 'review-feedback-' + field.key : undefined"
                  :data-result="field.correct == null ? undefined : field.correct ? 'correct' : 'wrong'"
                  @keydown.enter="handleAnswerEnter($event, field)"
                />
                <p :id="'review-feedback-' + field.key" class="review-answer-feedback" aria-live="polite">
                  <template v-if="currentReviewResult">
                    <span v-if="field.correct" class="review-answer-correct"><AppIcon name="check" />正确</span>
                    <span v-else :class="{ 'review-answer-wrong': field.correct === false }">正确答案 <strong>{{ field.expected }}</strong></span>
                  </template>
                </p>
              </div>
            </div>
          </form>
        </article>
        <div class="quiz-nav review-nav" role="group" aria-label="复习切题">
          <div class="quiz-nav-actions">
            <button
              class="quiz-primary"
              :type="currentReviewResult ? 'button' : 'submit'"
              :form="currentReviewResult ? undefined : 'review-answer-form'"
              :disabled="reviewLocked || (!currentReviewResult && !canCheckAnswer)"
              :aria-label="currentReviewResult ? undefined : '检查答案'"
              @click="handleReviewAction"
            >
              {{ reviewActionLabel }}<AppIcon :name="finishingReview ? 'check' : 'arrow'" />
            </button>
          </div>
        </div>
      </template>
      <div v-else class="panel review-state review-empty-state">
        <h3>
          {{
            vocabularyState.reviewMode === "today"
              ? "暂无到期词"
              : "暂无错词"
          }}
        </h3>
        <div class="review-card-actions">
          <button
            v-if="vocabularyState.reviewMode === 'today' && report.reviewWords"
            class="settings-secondary"
            type="button"
            :disabled="reviewLocked"
            @click="setReviewMode('mistakes')"
          >
            练错词
          </button>
          <button
            class="quiz-primary"
            type="button"
            @click="goSection('vocabulary')"
          >
            学新词<AppIcon name="arrow" />
          </button>
        </div>
      </div>
    </div>
  </section>
</template>
