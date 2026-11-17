<script setup>
import { computed } from "vue";
import AppIcon from "@/components/AppIcon.vue";
import {
  phonetic,
  quizExampleText,
  quizExampleCn,
  reviewCloze,
  highlightTerm,
} from "../vocabulary/wordPresentation";
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
  return currentReviewResult.value ? "下一题" : "检查答案";
});

function handleReviewAction(event) {
  if (!currentReviewResult.value) return;
  event.preventDefault();
  advanceReview();
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
        <span class="review-state-icon"><AppIcon name="check" /></span>
        <h3>本组复习已完成</h3>
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
            再复习一组
          </button>
          <button
            class="quiz-primary"
            type="button"
            @click="goSection('vocabulary')"
          >
            继续学新词<AppIcon name="arrow" />
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
              <h2>例句填空</h2>
              <div class="review-prompt-meta">
                <span v-if="phonetic(currentReviewWord)">{{ phonetic(currentReviewWord) }}</span>
                <span v-if="currentReviewWord.partOfSpeech">{{ currentReviewWord.partOfSpeech }}</span>
                <span>错误 {{ currentReviewWord.wrongCount || 0 }} 次</span>
              </div>
            </div>
            <div
              v-if="!currentReviewResult"
              class="review-cloze"
              v-html="reviewCloze(currentReviewWord)"
            ></div>
            <div v-else class="review-filled-example">
              <p
                v-html="
                  highlightTerm(
                    quizExampleText(
                      currentReviewWord,
                      currentReviewResult,
                    ),
                    currentReviewWord.sourceWord ||
                      currentReviewResult.expectedWord ||
                      currentReviewWord.word,
                  )
                "
              ></p>
              <span lang="zh-CN">{{
                quizExampleCn(
                  currentReviewWord,
                  currentReviewResult,
                )
              }}</span>
            </div>
            <button
              v-if="currentReviewWord.sourceTitle"
              class="review-source"
              type="button"
              @click="returnToArticle(currentReviewWord.sourceArticleId)"
            >
              <AppIcon name="news" /><span>来自阅读 · {{ currentReviewWord.sourceTitle }}</span><AppIcon name="arrow" />
            </button>
          </div>
          <div
            v-if="currentReviewResult"
            class="review-result"
            role="status"
            :class="currentReviewResult.correct ? 'is-correct' : 'is-wrong'"
          >
            <strong class="review-result-status">
              <AppIcon :name="currentReviewResult.correct ? 'check' : 'review'" />
              {{ currentReviewResult.correct ? "通过" : "继续巩固" }}
            </strong>
            <dl class="review-result-answers">
              <div>
                <dt>正确单词</dt>
                <dd>{{ currentReviewResult.expectedWord || currentReviewWord.word }}</dd>
              </div>
              <div>
                <dt>正确释义</dt>
                <dd>{{ currentReviewResult.expectedMeaning }}</dd>
              </div>
            </dl>
          </div>
          <form
            v-else
            id="review-answer-form"
            class="review-challenge-form"
            @submit.prevent="submitReview"
          >
            <div class="review-input-grid">
              <label>
                <span>{{
                  currentReviewWord.sourceWord &&
                  currentReviewWord.sourceWord.toLowerCase() !==
                    currentReviewWord.word.toLowerCase()
                    ? "英文拼写（单词原形）"
                    : "英文拼写"
                }}</span>
                <input
                  v-model.trim="vocabularyState.reviewForm.spellingAnswer"
                  autocomplete="off"
                  autocapitalize="none"
                  spellcheck="false"
                  placeholder="输入完整英文单词"
                  :disabled="reviewLocked"
                />
              </label>
              <label>
                <span>中文释义</span>
                <input
                  v-model.trim="vocabularyState.reviewForm.meaningAnswer"
                  autocomplete="off"
                  placeholder="输入一个释义关键词"
                  :disabled="reviewLocked"
                />
              </label>
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
              @click="handleReviewAction"
            >
              {{ reviewActionLabel }}<AppIcon :name="finishingReview ? 'check' : 'arrow'" />
            </button>
          </div>
        </div>
      </template>
      <div v-else class="panel review-state review-empty-state">
        <span class="review-state-icon"><AppIcon name="check" /></span>
        <h3>
          {{
            vocabularyState.reviewMode === "today"
              ? "当前到期任务已完成"
              : "当前没有待强化的错词"
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
            提前练习错词
          </button>
          <button
            class="quiz-primary"
            type="button"
            @click="goSection('vocabulary')"
          >
            去学新词<AppIcon name="arrow" />
          </button>
        </div>
      </div>
    </div>
  </section>
</template>
