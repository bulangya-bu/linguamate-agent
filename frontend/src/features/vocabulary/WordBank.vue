<script setup>
import { toRefs } from "vue";
import AppIcon from "@/components/AppIcon.vue";
import {
  phonetic,
  categoryValue,
  categoryLabel,
  wordCardExampleEn,
  meaningfulExample,
  wordCardExampleCn,
} from "../vocabulary/wordPresentation";
const props = defineProps({
  vocabulary: Object,
  activeSection: String,
});
const {
  state: vocabularyState,
  wordBankGrid,
  openLibraryReview,
  wordBankScopes,
  wordBankEmptyText,
  wordBankTotalPages,
  toggleFavorite,
  loadWordBankWords,
  setWordBankScope,
  resetWordBankFilters,
  setWordBankCategory,
  scheduleWordBankSearch,
} = props.vocabulary;
const { activeSection } = toRefs(props);
</script>

<template>
  <section
    id="wordbank"
    class="section"
    :class="{ active: activeSection === 'wordbank' }"
  >
    <section class="panel wordbank-layout">
      <div class="panel-heading">
        <div class="wordbank-scopes" role="group" aria-label="词库分组">
          <button
            v-for="scope in wordBankScopes"
            :key="scope.id"
            type="button"
            :class="{
              'is-active': vocabularyState.wordBank.scope === scope.id,
            }"
            :aria-pressed="vocabularyState.wordBank.scope === scope.id"
            aria-controls="wordbank-results"
            @click="setWordBankScope(scope.id)"
          >
            {{ scope.label
            }}<span class="wordbank-scope-count">{{
              vocabularyState.wordBank.stats[scope.id] ?? "—"
            }}</span>
          </button>
        </div>
        <div class="wordbank-heading-actions">
          <button
            v-if="
              ['due', 'review'].includes(vocabularyState.wordBank.scope) &&
              vocabularyState.wordBank.total
            "
            type="button"
            class="wordbank-start-review"
            @click="openLibraryReview"
          >
            {{
              vocabularyState.wordBank.scope === "due"
                ? "开始到期复习"
                : "开始错词强化"
            }}<AppIcon name="arrow" />
          </button>
          <span class="energy-pill">{{
            vocabularyState.wordBank.loading
              ? "正在加载"
              : vocabularyState.wordBank.total + " 个单词"
          }}</span>
        </div>
      </div>
      <div class="wordbank-filters">
        <div class="wordbank-categories" role="group" aria-label="词库分类">
          <button
            class="review-tab"
            :class="{ 'is-active': !vocabularyState.wordBank.category }"
            type="button"
            @click="setWordBankCategory('')"
          >
            全部
          </button>
          <button
            v-for="category in vocabularyState.wordBank.categories"
            :key="categoryValue(category)"
            class="review-tab"
            :class="{
              'is-active':
                vocabularyState.wordBank.category === categoryValue(category),
            }"
            type="button"
            @click="setWordBankCategory(categoryValue(category))"
          >
            {{ categoryLabel(category) }}
          </button>
        </div>
        <input
          v-model.trim="vocabularyState.wordBank.search"
          class="wordbank-search"
          aria-label="搜索单词"
          autocomplete="off"
          placeholder="搜索英文前缀或中文释义前缀"
          @input="scheduleWordBankSearch"
          @keyup.enter="loadWordBankWords(0)"
        />
        <button
          class="settings-secondary"
          type="button"
          @click="loadWordBankWords(0)"
        >
          搜索
        </button>
      </div>
      <div
        id="wordbank-results"
        ref="wordBankGrid"
        class="word-strip word-bank-grid"
        :class="{ 'is-loading': vocabularyState.wordBank.loading }"
        :aria-busy="vocabularyState.wordBank.loading"
        :inert="vocabularyState.wordBank.loading"
      >
        <article
          v-for="word in vocabularyState.wordBank.words"
          :key="word.id"
          class="word-card vue-clickable"
          tabindex="0"
          role="button"
          @keydown.enter.self="vocabularyState.selectedWord = word"
          @keydown.space.prevent.self="vocabularyState.selectedWord = word"
          @click="vocabularyState.selectedWord = word"
        >
          <div class="word-card-head">
            <div>
              <strong>{{ word.word }}</strong>
              <span class="word-meta"
                >{{ phonetic(word) }} ·
                {{ word.partOfSpeech || word.levelTag }}</span
              >
            </div>
            <button
              class="favorite-button"
              :class="{ 'is-active': word.favorite }"
              type="button"
              :aria-label="(word.favorite ? '取消收藏 ' : '收藏 ') + word.word"
              :aria-pressed="String(Boolean(word.favorite))"
              @click.stop="toggleFavorite(word)"
            >
              <AppIcon name="star" />
            </button>
          </div>
          <p>{{ word.meaningCn }}</p>
          <div v-if="meaningfulExample(word)" class="word-card-example">
            <p>{{ wordCardExampleEn(word) }}</p>
            <span>{{ wordCardExampleCn(word) }}</span>
          </div>
        </article>
        <div
          v-if="
            !vocabularyState.wordBank.loading &&
            !vocabularyState.wordBank.words.length
          "
          class="wordbank-empty-state"
        >
          <AppIcon name="wordbank" />
          <h3>{{ wordBankEmptyText }}</h3>
          <button
            v-if="
              vocabularyState.wordBank.scope !== 'all' ||
              vocabularyState.wordBank.search ||
              vocabularyState.wordBank.category
            "
            class="settings-secondary"
            type="button"
            @click="resetWordBankFilters"
          >
            查看全部单词
          </button>
        </div>
      </div>
      <div class="wordbank-pagination">
        <span class="wordbank-page-info"
          >第 {{ vocabularyState.wordBank.page + 1 }} 页 /
          {{ wordBankTotalPages }} 页</span
        >
        <button
          class="wordbank-page-btn"
          type="button"
          :disabled="
            vocabularyState.wordBank.loading ||
            vocabularyState.wordBank.page <= 0
          "
          @click="loadWordBankWords(vocabularyState.wordBank.page - 1)"
        >
          上一页
        </button>
        <button
          class="wordbank-page-btn"
          type="button"
          :disabled="
            vocabularyState.wordBank.loading ||
            vocabularyState.wordBank.page >= wordBankTotalPages - 1
          "
          @click="loadWordBankWords(vocabularyState.wordBank.page + 1)"
        >
          下一页
        </button>
      </div>
    </section>
  </section>
</template>
