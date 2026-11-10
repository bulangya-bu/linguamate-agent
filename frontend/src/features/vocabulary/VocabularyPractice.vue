<script setup>
import { computed, toRefs } from "vue";
import AppIcon from "@/components/AppIcon.vue";
import QuizInsight from "@/components/QuizInsight.vue";
import {
  categoryValue,
  categoryLabel,
  quizExampleText,
  quizExampleCn,
  meaningfulExample,
  highlightTerm,
  modelLabel,
} from "./wordPresentation";
const props = defineProps({
  vocabulary: Object,
  activeSection: String,
});
const {
  state: vocabularyState,
  quizScope,
  quizRound,
  quizPane,
  quizInsightToggle,
  quizWorkbench,
  currentQuizWord,
  quizResultForCurrent,
  wordDetailForCurrent,
  quizDefinitionGroups,
  quizOptions,
  quizAnsweredCount,
  quizProgress,
  loadWords,
  prevQuiz,
  nextQuiz,
  quizOptionStatus,
  submitQuiz,
  loadWordDetail,
  openQuizInsight,
  closeQuizInsight,
  toggleFavorite,
} = props.vocabulary;
const { activeSection } = toRefs(props);

const promptExample = computed(() => {
  const word = currentQuizWord.value;
  return word?.sourceSentence || (meaningfulExample(word) ? word.exampleEn : "");
});

const answerExample = computed(() => {
  const result = quizResultForCurrent.value;
  if (!result || (!promptExample.value && !result.exampleEn)) return null;
  return {
    en: quizExampleText(currentQuizWord.value, result),
    cn: quizExampleCn(currentQuizWord.value, result),
  };
});

const reviewHint = computed(() =>
  quizResultForCurrent.value?.nextReviewHint?.replace(/^答[对错]了[，,]\s*/, "") || "",
);

const choices = computed(() => quizOptions.value.map((text, index) => {
  const status = quizOptionStatus(text);
  const letter = String.fromCharCode(65 + index);
  const feedback = { correct: "正确释义", wrong: "选错了" }[status];
  return {
    text,
    letter,
    status,
    label: feedback ? `${letter} ${text}，${feedback}` : undefined,
  };
}));
</script>

<template>
  <section
    id="vocabulary"
    class="section"
    :class="{
      active: activeSection === 'vocabulary',
      'has-insight': quizPane === 'insight',
    }"
  >
    <section class="panel">
      <div class="panel-heading">
        <div class="quiz-heading-label">
          <h2 id="quiz-task-heading">选择正确释义</h2>
          <span v-if="quizScope" class="quiz-mode-tag">新词练习</span>
        </div>
        <select
          v-if="!quizScope"
          v-model="vocabularyState.quizCategory"
          aria-label="选择闯关词库"
          :disabled="quizRound.loading || quizRound.busy"
          @change="loadWords"
        >
          <option value="">智能混合</option>
          <option
            v-for="category in vocabularyState.quizCategories"
            :key="categoryValue(category)"
            :value="categoryValue(category)"
          >
            {{ categoryLabel(category) }}
          </option>
        </select>
      </div>
      <div class="word-quiz-shell">
        <div v-if="quizRound.loading" class="quiz-empty" role="status">
          正在加载单词…
        </div>
        <div v-else-if="quizRound.error" class="quiz-complete">
          <p>单词加载失败。</p>
          <button class="quiz-primary" type="button" @click="loadWords">
            重试
          </button>
        </div>
        <div v-else-if="quizRound.completed" class="quiz-complete">
          <h3>本轮单词闯关完成</h3>
          <p>已完成 {{ quizAnsweredCount }} 个单词</p>
          <button
            class="quiz-primary"
            type="button"
            :disabled="quizRound.busy"
            @click="loadWords"
          >
            再来一轮
          </button>
        </div>
        <div
          v-else-if="currentQuizWord"
          ref="quizWorkbench"
          class="quiz-workbench"
          :class="'shows-' + quizPane"
        >
          <article
            id="quiz-practice"
            class="word-quiz-card"
            :class="{ 'is-answered': quizResultForCurrent }"
            aria-labelledby="quiz-task-heading"
          >
            <div class="quiz-progress">
              <span>第 {{ quizRound.index + 1 }} / {{ quizRound.words.length }} 题</span>
              <div class="quiz-progress-track">
                <i :style="{ width: quizProgress + '%' }"></i>
              </div>
            </div>
            <div class="quiz-study-stage">
              <div class="quiz-word-shell">
                <button
                  class="favorite-button quiz-favorite"
                  :class="{ 'is-active': currentQuizWord.favorite }"
                  type="button"
                  :aria-label="
                    (currentQuizWord.favorite ? '取消收藏 ' : '收藏 ') +
                    currentQuizWord.word
                  "
                  :aria-pressed="String(Boolean(currentQuizWord.favorite))"
                  @click="toggleFavorite(currentQuizWord)"
                >
                  <AppIcon name="star" />
                </button>
                <div class="quiz-word" :key="currentQuizWord.id">
                  {{ currentQuizWord.word }}
                </div>
              </div>
              <p
                v-if="promptExample"
                class="quiz-example-en"
                v-html="highlightTerm(promptExample, currentQuizWord.word)"
              ></p>
            </div>
            <div class="quiz-options">
              <button
                v-for="option in choices"
                :key="option.text"
                class="quiz-option"
                :class="option.status && 'is-' + option.status"
                type="button"
                :disabled="quizRound.busy || Boolean(quizResultForCurrent)"
                :aria-label="option.label"
                @click="submitQuiz(option.text)"
              >
                <span class="option-letter">{{ option.letter }}</span>
                <span class="quiz-option-text">{{ option.text }}</span>
                <span class="quiz-option-mark" aria-hidden="true">
                  <AppIcon
                    v-if="option.status"
                    :name="option.status === 'correct' ? 'check' : 'close'"
                  />
                </span>
              </button>
            </div>
          </article>
          <div
            class="quiz-nav"
            :class="quizResultForCurrent && (quizResultForCurrent.correct ? 'is-correct' : 'is-wrong')"
          >
            <div class="quiz-nav-feedback">
              <div class="quiz-feedback-slot" aria-live="polite" aria-atomic="true">
                <div
                  v-if="quizResultForCurrent"
                  class="quiz-feedback-result"
                  :class="quizResultForCurrent.correct ? 'is-correct' : 'is-wrong'"
                >
                  <AppIcon :name="quizResultForCurrent.correct ? 'check' : 'close'" />
                  <div>
                    <strong>{{ quizResultForCurrent.correct ? "答对了" : "答错了" }}</strong>
                    <span v-if="reviewHint" class="quiz-review-notice">{{ reviewHint }}</span>
                  </div>
                </div>
              </div>
              <button
                ref="quizInsightToggle"
                class="quiz-insight-toggle"
                type="button"
                aria-controls="quiz-analysis"
                :aria-expanded="quizPane === 'insight'"
                :disabled="!quizResultForCurrent"
                :class="{ 'is-unavailable': !quizResultForCurrent }"
                @click="quizPane === 'insight' ? closeQuizInsight() : openQuizInsight()"
              >
                <AppIcon name="spark" />
                {{ quizPane === "insight" ? "收起解析" : "深度解析" }}
              </button>
            </div>
            <div class="quiz-nav-actions">
              <button
                class="quiz-secondary"
                type="button"
                @click="prevQuiz"
                :disabled="quizRound.index <= 0"
              >
                <AppIcon name="arrow" />
                上一题
              </button>
              <button
                class="quiz-primary"
                type="button"
                :disabled="!quizResultForCurrent"
                @click="nextQuiz"
              >
                {{ quizRound.index >= quizRound.words.length - 1 ? "完成" : "下一题" }}
                <AppIcon :name="quizRound.index >= quizRound.words.length - 1 ? 'check' : 'arrow'" />
              </button>
            </div>
          </div>
          <QuizInsight
            v-if="quizPane === 'insight'"
            id="quiz-analysis"
            :key="currentQuizWord.id"
            :word="currentQuizWord"
            :detail="quizResultForCurrent ? wordDetailForCurrent : null"
            :source="modelLabel(wordDetailForCurrent?.data)"
            @request="(force) => loadWordDetail(currentQuizWord, force)"
            @close="closeQuizInsight"
          >
            <template #summary>
              <div class="insight-word">{{ currentQuizWord.word }}</div>
              <section class="insight-section quiz-local-summary">
                <h3>词义与例句</h3>
                <dl v-if="quizDefinitionGroups.length" class="quiz-definitions" aria-label="完整词性与释义">
                  <div v-for="group in quizDefinitionGroups" :key="group.partOfSpeech" class="quiz-definition-group">
                    <dt>{{ group.partOfSpeech || "释义" }}</dt>
                    <dd>
                      <p v-for="meaning in group.meanings" :key="meaning">{{ meaning }}</p>
                    </dd>
                  </div>
                </dl>
                <div v-if="answerExample" class="insight-entry">
                  <p class="insight-english" v-html="highlightTerm(answerExample.en, currentQuizWord.word)"></p>
                  <p v-if="answerExample.cn" class="quiz-example-cn">{{ answerExample.cn }}</p>
                </div>
              </section>
            </template>
          </QuizInsight>
        </div>
        <div v-else class="quiz-empty">
          {{ quizScope === "new" ? "当前词库的新单词已学完。" : "当前词库暂无单词。" }}
        </div>
      </div>
    </section>
  </section>
</template>
