<script setup>
import { toRefs } from "vue";
import AppIcon from "@/components/AppIcon.vue";
import {
  phonetic,
  categoryValue,
  categoryLabel,
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

async function turnPage(page) {
  await loadWordBankWords(page);
  const grid = wordBankGrid.value;
  if (vocabularyState.wordBank.page === page && activeSection.value === 'wordbank' && grid
      && getComputedStyle(grid).overflowY === 'visible' && grid.getBoundingClientRect().top < 0) {
    grid.scrollIntoView({ block: 'start', behavior: 'instant' });
  }
}
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
                ? "复习全部到期词"
                : "复习全部错词"
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
        <select
          class="wordbank-mobile-category"
          aria-label="词库分类"
          :value="vocabularyState.wordBank.category"
          @change="setWordBankCategory($event.target.value)"
        >
          <option value="">全部分类</option>
          <option v-for="category in vocabularyState.wordBank.categories" :key="categoryValue(category)" :value="categoryValue(category)">
            {{ categoryLabel(category) }}
          </option>
        </select>
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
          placeholder="搜单词或释义"
          @input="scheduleWordBankSearch"
          @keyup.enter="loadWordBankWords(0)"
        />
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
          class="wordbank-entry"
        >
          <button class="wordbank-entry-open" type="button" @click="vocabularyState.selectedWord = word">
            <span class="wordbank-entry-heading">
              <strong>{{ word.word }}</strong>
              <small v-if="word.wrongCount > 0">错 {{ word.wrongCount }} 次</small>
              <small v-else-if="word.familiarity >= 3">已掌握</small>
            </span>
            <span class="word-meta">{{ [phonetic(word), word.partOfSpeech || word.levelTag].filter(Boolean).join(' · ') }}</span>
            <span class="wordbank-entry-meaning">{{ word.meaningCn }}</span>
          </button>
          <button
            class="favorite-button"
            :class="{ 'is-active': word.favorite }"
            type="button"
            :aria-label="(word.favorite ? '取消收藏 ' : '收藏 ') + word.word"
            :aria-pressed="String(Boolean(word.favorite))"
            @click="toggleFavorite(word)"
          >
            <AppIcon name="star" />
          </button>
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
          @click="turnPage(vocabularyState.wordBank.page - 1)"
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
          @click="turnPage(vocabularyState.wordBank.page + 1)"
        >
          下一页
        </button>
      </div>
    </section>
  </section>
</template>

<style scoped>
.wordbank-mobile-category { display: none; }
#wordbank .word-bank-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(245px, 100%), 1fr));
  align-content: start;
  gap: 8px;
}

.wordbank-entry {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  min-width: 0;
  padding: 6px 8px 6px 0;
  border: 1px solid var(--line);
  border-radius: 11px;
  background: var(--surface);
}

.wordbank-entry:hover { border-color: color-mix(in srgb, var(--green-ink) 32%, var(--line)); }
.wordbank-entry-open {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 7px 12px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: var(--ink);
  text-align: left;
}
.wordbank-entry-heading { display: flex; flex-wrap: wrap; align-items: baseline; gap: 4px 8px; }
.wordbank-entry-heading > strong { font: 400 21px/1.1 Georgia, serif; overflow-wrap: anywhere; }
.wordbank-entry-heading > small { color: var(--muted); font-size: 11px; }
.wordbank-entry-meaning {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  font-size: 13px;
  line-height: 1.45;
  overflow-wrap: anywhere;
}
.wordbank-entry > .favorite-button { width: 33px; height: 33px; min-width: 33px; }
.wordbank-entry > .favorite-button > svg { width: 17px; height: 17px; }

@media (max-width: 760px), (max-width: 960px) and (max-height: 540px) and (pointer: coarse) {
  #wordbank .wordbank-layout { width: 100%; padding: 14px; overflow: visible; }
  #wordbank .panel-heading { display: flex; flex-flow: column; align-items: stretch; gap: 10px; }
  .wordbank-scopes { display: grid; grid-template-columns: repeat(5,minmax(0,1fr)); gap: 0; }
  .wordbank-scopes > button { min-height: 44px; padding: 2px 0 4px; font-size: 14px; }
  .wordbank-scopes .wordbank-scope-count { display: none; }
  .wordbank-heading-actions { justify-content: space-between; }
  #wordbank .wordbank-filters { display: flex; flex-wrap: nowrap; align-items: center; gap: 8px; padding-bottom: 12px; }
  .wordbank-filters .wordbank-categories { display: none; }
  #wordbank .wordbank-search { order: -1; flex: 1; min-width: 0; height: 44px; font-size: 16px; }
  #wordbank .wordbank-mobile-category {
    display: block;
    width: 37%;
    min-height: 44px;
    padding: 7px;
    background: var(--surface);
    color: var(--ink);
    font-size: 16px;
  }
  #wordbank .word-bank-grid { display: grid; grid-template-columns: minmax(0,1fr); overflow: visible; max-height: none; padding: 0; }
  #wordbank .word-bank-grid { scroll-margin-top: calc(var(--mobile-header-height, 56px) + 8px); }
  .wordbank-entry { align-items: center; padding-block: 4px; }
  .wordbank-entry-open { gap: 6px; }
  .wordbank-entry-heading > strong { font-size: 23px; }
  #wordbank .wordbank-entry .word-meta { font-size: 14px; line-height: 1.4; }
  .wordbank-entry-meaning { font-size: 16px; }
  .wordbank-entry > .favorite-button { width: 44px; height: 44px; }
  #wordbank .wordbank-pagination { flex-wrap: wrap; min-height: 52px; gap: 6px; font-size: 13px; }
  .wordbank-pagination .wordbank-page-btn { min-height: 44px; }
}
</style>
