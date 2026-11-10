<script setup>
import { toRefs } from "vue";
import AppIcon from "@/components/AppIcon.vue";
import {
  phonetic,
  quizExampleText,
  quizExampleCn,
  strictReviewParts,
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
const { activeSection, goSection, returnToArticle, openEvidence, session } =
  toRefs(props);
</script>

<template>
  <section
    id="review"
    class="section"
    :class="{ active: activeSection === 'review' }"
  >
    <section class="panel">
      <div class="panel-heading">
        <div class="review-mode-switch" role="group" aria-label="复习类型">
          <button
            type="button"
            :class="{ active: vocabularyState.reviewMode === 'today' }"
            :disabled="vocabularyState.reviewBusy"
            @click="setReviewMode('today')"
          >
            今日到期<span>{{ report.dueWords || 0 }}</span></button
          ><button
            type="button"
            :class="{ active: vocabularyState.reviewMode === 'mistakes' }"
            :disabled="vocabularyState.reviewBusy"
            @click="setReviewMode('mistakes')"
          >
            错词强化<span>{{ report.reviewWords || 0 }}</span>
          </button>
        </div>
        <button class="evidence-open" type="button" @click="openEvidence()">
          学习变化<AppIcon name="arrow" />
        </button>
      </div>
      <div class="review-workspace">
        <div
          v-if="vocabularyState.reviewCompleted"
          class="review-challenge-card is-complete"
        >
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
              @click="loadReviews"
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
        <div v-else-if="currentReviewWord" class="review-list">
          <article
            class="review-challenge-card"
            :class="{
              'is-correct': vocabularyState.lastReviewResult?.correct,
              'is-wrong':
                vocabularyState.lastReviewResult &&
                !vocabularyState.lastReviewResult.correct,
            }"
          >
            <div class="quiz-progress">
              <span>剩余 {{ reviewRemaining }} / {{ reviewTotal }} 词</span>
              <div class="quiz-progress-track">
                <i :style="{ width: reviewProgress + '%' }"></i>
              </div>
            </div>
            <div class="review-prompt">
              <button
                v-if="currentReviewWord.sourceTitle"
                class="review-source"
                type="button"
                @click="returnToArticle(currentReviewWord.sourceArticleId)"
              >
                <AppIcon name="news" />来自阅读 ·
                {{ currentReviewWord.sourceTitle }}<AppIcon name="arrow" />
              </button>
              <div class="review-prompt-meta">
                <span v-if="phonetic(currentReviewWord)">{{
                  phonetic(currentReviewWord)
                }}</span>
                <span v-if="currentReviewWord.partOfSpeech">{{
                  currentReviewWord.partOfSpeech
                }}</span>
                <span>错误 {{ currentReviewWord.wrongCount || 0 }} 次</span>
              </div>
              <div
                v-if="!vocabularyState.reviewPendingResult"
                class="review-cloze"
                v-html="reviewCloze(currentReviewWord)"
              ></div>
              <div v-else class="review-filled-example">
                <p
                  v-html="
                    highlightTerm(
                      quizExampleText(
                        currentReviewWord,
                        vocabularyState.lastReviewResult,
                      ),
                      currentReviewWord.sourceWord ||
                        vocabularyState.lastReviewResult?.expectedWord ||
                        currentReviewWord.word,
                    )
                  "
                ></p>
                <span>{{
                  quizExampleCn(
                    currentReviewWord,
                    vocabularyState.lastReviewResult,
                  )
                }}</span>
              </div>
            </div>
            <div
              v-if="vocabularyState.lastReviewResult"
              class="review-result"
              :class="
                vocabularyState.lastReviewResult.correct
                  ? 'is-correct'
                  : 'is-wrong'
              "
            >
              <strong>{{
                vocabularyState.lastReviewResult.correct ? "通过" : "继续巩固"
              }}</strong>
              <span>{{
                strictReviewParts(vocabularyState.lastReviewResult).join(" · ")
              }}</span>
              <span
                >正确单词：{{
                  vocabularyState.lastReviewResult.expectedWord ||
                  currentReviewWord.word
                }}</span
              >
              <span
                >正确释义：{{
                  vocabularyState.lastReviewResult.expectedMeaning
                }}</span
              >
              <small>{{
                vocabularyState.lastReviewResult.nextReviewHint
              }}</small>
            </div>
            <form
              class="review-challenge-form"
              :hidden="vocabularyState.reviewPendingResult"
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
                    :disabled="vocabularyState.reviewBusy"
                  />
                </label>
                <label>
                  <span>中文释义</span>
                  <input
                    v-model.trim="vocabularyState.reviewForm.meaningAnswer"
                    autocomplete="off"
                    placeholder="输入一个释义关键词"
                    :disabled="vocabularyState.reviewBusy"
                  />
                </label>
              </div>
              <div class="review-card-actions">
                <button
                  class="quiz-primary"
                  type="submit"
                  :disabled="vocabularyState.reviewBusy"
                >
                  {{ vocabularyState.reviewBusy ? "提交中..." : "提交复习" }}
                </button>
              </div>
            </form>
            <div
              v-if="vocabularyState.reviewPendingResult"
              class="review-card-actions review-next-actions"
            >
              <button class="quiz-primary" type="button" @click="advanceReview">
                下一题
              </button>
            </div>
          </article>
        </div>
        <div v-else class="review-empty-state">
          <AppIcon name="check" />
          <h3>
            {{
              vocabularyState.reviewMode === "today"
                ? "当前到期任务已完成"
                : "当前没有待强化的错词"
            }}
          </h3>
          <button
            v-if="vocabularyState.reviewMode === 'today' && report.reviewWords"
            class="settings-secondary"
            type="button"
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
    </section>
  </section>
</template>
