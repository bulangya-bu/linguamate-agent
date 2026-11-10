<script setup>
import AppIcon from "@/components/AppIcon.vue";
import {
  phonetic,
  wordCardExampleEn,
  wordCardExampleCn,
  highlightTerm,
  modelLabel,
} from "../vocabulary/wordPresentation";
const props = defineProps({
  vocabulary: Object,
});
const {
  state: vocabularyState,
  wordDialogRef,
  selectedWordDetail,
  wordDialogVisible,
  loadWordDetail,
  toggleFavorite,
} = props.vocabulary;
</script>

<template>
  <div
    class="wordbank-dialog-backdrop"
    :class="{ 'is-open': wordDialogVisible }"
    aria-hidden="true"
    @click="vocabularyState.selectedWord = null"
  ></div>
  <aside
    ref="wordDialogRef"
    :inert="!wordDialogVisible"
    class="wordbank-dialog"
    :class="{ 'is-open': wordDialogVisible }"
    role="dialog"
    aria-modal="true"
    aria-labelledby="wordBankDialogTitle"
    :aria-hidden="String(!wordDialogVisible)"
  >
    <div v-if="vocabularyState.selectedWord" class="wordbank-dialog-shell">
      <div class="wordbank-dialog-head">
        <div>
          <h2 id="wordBankDialogTitle">
            {{ vocabularyState.selectedWord.word }}
          </h2>
          <p class="word-meta">
            {{ phonetic(vocabularyState.selectedWord) }} ·
            {{ vocabularyState.selectedWord.partOfSpeech }} ·
            {{ vocabularyState.selectedWord.levelTag }}
          </p>
        </div>
        <div class="wordbank-dialog-actions">
          <button
            class="favorite-button wordbank-dialog-favorite"
            :class="{ 'is-active': vocabularyState.selectedWord.favorite }"
            type="button"
            :aria-label="
              (vocabularyState.selectedWord.favorite ? '取消收藏 ' : '收藏 ') +
              vocabularyState.selectedWord.word
            "
            :aria-pressed="
              String(Boolean(vocabularyState.selectedWord.favorite))
            "
            @click="toggleFavorite(vocabularyState.selectedWord)"
          >
            <AppIcon name="star" />
          </button>
          <button
            class="panel-close"
            type="button"
            aria-label="关闭"
            @click="vocabularyState.selectedWord = null"
          >
            <AppIcon name="close" />
          </button>
        </div>
      </div>
      <div class="wordbank-dialog-meaning">
        <span>中文释义</span>
        <p>{{ vocabularyState.selectedWord.meaningCn }}</p>
      </div>
      <div class="wordbank-dialog-example">
        <span>例句</span>
        <p
          v-html="
            highlightTerm(
              wordCardExampleEn(vocabularyState.selectedWord),
              vocabularyState.selectedWord.word,
            )
          "
        ></p>
        <small>{{ wordCardExampleCn(vocabularyState.selectedWord) }}</small>
      </div>
      <div class="wordbank-dialog-detail-actions">
        <button
          class="quiz-detail-button"
          :class="{
            'is-loading': selectedWordDetail?.status === 'loading',
            'is-done': selectedWordDetail?.status === 'ready',
          }"
          type="button"
          :disabled="selectedWordDetail?.status === 'loading'"
          @click="loadWordDetail(vocabularyState.selectedWord)"
        >
          {{
            selectedWordDetail?.status === "loading"
              ? "生成中..."
              : selectedWordDetail?.status === "ready"
                ? "已生成解析"
                : "深度解析"
          }}
        </button>
      </div>
      <section
        v-if="selectedWordDetail"
        class="quiz-ai-detail wordbank"
        :class="{
          'is-loading': selectedWordDetail.status === 'loading',
          'is-error': selectedWordDetail.status === 'error',
        }"
      >
        <template v-if="selectedWordDetail.status === 'loading'">
          <span class="quiz-ai-spinner" aria-hidden="true"></span>
          <span
            >正在生成
            {{ vocabularyState.selectedWord.word }} 的深度解析...</span
          >
        </template>
        <template v-else-if="selectedWordDetail.status === 'error'">
          <span>{{
            selectedWordDetail.message ||
            "模型未配置或暂时不可用，请检查 AI 设置"
          }}</span>
          <button
            class="quiz-detail-button retry-detail-button"
            type="button"
            @click="loadWordDetail(vocabularyState.selectedWord, true)"
          >
            重试
          </button>
        </template>
        <template v-else>
          <div class="quiz-ai-head">
            <strong
              >深度解析 ·
              {{
                selectedWordDetail.data?.word ||
                vocabularyState.selectedWord.word
              }}</strong
            >
            <span>{{ modelLabel(selectedWordDetail.data) }}</span>
          </div>
          <div class="quiz-ai-grid">
            <section
              v-if="selectedWordDetail.data?.examples?.length"
              class="quiz-ai-section"
            >
              <h3>更多例句</h3>
              <div
                v-for="example in selectedWordDetail.data.examples"
                :key="example.en"
                class="quiz-ai-row"
              >
                <p
                  v-html="
                    highlightTerm(
                      example.en || '',
                      vocabularyState.selectedWord.word,
                    )
                  "
                ></p>
                <small v-if="example.cn">{{ example.cn }}</small>
              </div>
            </section>
            <section
              v-if="selectedWordDetail.data?.variants?.length"
              class="quiz-ai-section"
            >
              <h3>词性变种</h3>
              <div
                v-for="variant in selectedWordDetail.data.variants"
                :key="`${variant.form}-${variant.type}`"
                class="quiz-ai-row"
              >
                <p>
                  <b>{{ variant.form }}</b> {{ variant.type }}
                </p>
                <small v-if="variant.meaningCn">{{ variant.meaningCn }}</small>
              </div>
            </section>
            <section
              v-if="selectedWordDetail.data?.usageNotes?.length"
              class="quiz-ai-section"
            >
              <h3>用法提示</h3>
              <div
                v-for="note in selectedWordDetail.data.usageNotes"
                :key="note"
                class="quiz-ai-row"
              >
                <p>{{ note }}</p>
              </div>
            </section>
            <section
              v-if="selectedWordDetail.data?.collocations?.length"
              class="quiz-ai-section"
            >
              <h3>固定搭配</h3>
              <div
                v-for="item in selectedWordDetail.data.collocations"
                :key="`${item.phrase}-${item.meaningCn}`"
                class="quiz-ai-row"
              >
                <p>
                  <b>{{ item.phrase }}</b> {{ item.meaningCn }}
                </p>
                <small
                  v-if="item.exampleEn"
                  v-html="
                    highlightTerm(
                      item.exampleEn,
                      vocabularyState.selectedWord.word,
                    )
                  "
                ></small>
              </div>
            </section>
          </div>
        </template>
      </section>
    </div>
  </aside>
</template>
