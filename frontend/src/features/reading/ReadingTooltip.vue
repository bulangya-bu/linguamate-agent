<script setup>
import AppIcon from "@/components/AppIcon.vue";
import { watch } from "vue";
import { phonetic } from "../vocabulary/wordPresentation";
const props = defineProps({
  reading: Object,
});
const {
  state: readingState,
  isPhone,
  tooltipRef,
  tooltipHasDefinition,
  scheduleHideTooltip,
  cancelTooltipHide,
  handleTooltipFocusOut,
  closeKeyboardLookup,
  openPendingLibrary,
  saveReaderWord,
  explainReaderWord,
} = props.reading;

watch([() => readingState.tooltip.visible, isPhone], ([visible, phone]) => {
  if (visible && phone && tooltipRef.value && !tooltipRef.value.open) tooltipRef.value.showModal();
}, { flush: "post" });

function dismissBackdrop(event) {
  if (!isPhone.value || event.target !== tooltipRef.value) return;
  const bounds = tooltipRef.value.getBoundingClientRect();
  if (event.clientY < bounds.top || event.clientX < bounds.left || event.clientX > bounds.right) closeKeyboardLookup();
}
</script>

<template>
  <component :is="isPhone ? 'dialog' : 'div'"
    v-if="readingState.tooltip.visible"
    ref="tooltipRef"
    class="word-tooltip visible"
    role="dialog"
    tabindex="-1"
    :aria-label="readingState.tooltip.data.word + ' 的释义'"
    :style="isPhone ? undefined : {
      left: readingState.tooltip.x + 'px',
      top: readingState.tooltip.y + 'px',
    }"
    @mouseenter="cancelTooltipHide"
    @mouseleave="scheduleHideTooltip"
    @focusin="cancelTooltipHide"
    @focusout="handleTooltipFocusOut"
    @keydown.esc.stop="closeKeyboardLookup"
    @cancel.prevent="closeKeyboardLookup"
    @close="closeKeyboardLookup"
    @click="dismissBackdrop"
  >
    <div class="tooltip-head">
      <strong>{{ readingState.tooltip.data.word }}</strong>
      <span v-if="readingState.tooltip.loading" class="tooltip-status"
        >查询中</span
      >
      <button v-if="isPhone" class="tooltip-phone-close" type="button" aria-label="关闭释义" @click="closeKeyboardLookup"><AppIcon name="close" /></button>
    </div>
    <div
      v-if="
        phonetic(readingState.tooltip.data) ||
        readingState.tooltip.data.partOfSpeech
      "
      class="tooltip-meta"
    >
      <span v-if="phonetic(readingState.tooltip.data)" class="phonetic">{{
        phonetic(readingState.tooltip.data)
      }}</span>
      <span v-if="readingState.tooltip.data.partOfSpeech" class="tooltip-pos">{{
        readingState.tooltip.data.partOfSpeech
      }}</span>
    </div>
    <section v-if="readingState.tooltip.context" class="tooltip-context">
      <span>{{
        readingState.tooltip.context.source === "model"
          ? "本文中的意思 · AI"
          : "通用词典释义"
      }}</span>
      <p>{{ readingState.tooltip.context.meaning }}</p>
      <small>{{ readingState.tooltip.context.explanation }}</small>
      <div v-if="readingState.tooltip.context.phrase" class="tooltip-phrase">
        <b>{{ readingState.tooltip.context.phrase }}</b
        ><span>{{ readingState.tooltip.context.phraseMeaning }}</span>
      </div>
    </section>
    <p v-else>{{ readingState.tooltip.data.meaningCn || "暂无释义" }}</p>
    <blockquote
      v-if="readingState.tooltip.sentence"
      class="tooltip-original-sentence"
    >
      {{ readingState.tooltip.sentence }}
    </blockquote>
    <button
      class="tooltip-explain"
      type="button"
      :disabled="
        readingState.tooltip.loading ||
        readingState.tooltip.contextBusy ||
        !readingState.tooltip.sentence
      "
      @click="explainReaderWord"
    >
      {{
        readingState.tooltip.contextBusy
          ? "查询中…"
          : readingState.tooltip.context
            ? "重新解读"
            : "看语境"
      }}
    </button>
    <small
      v-if="
        !readingState.tooltip.sentence && readingState.tooltip.data.exampleEn
      "
      >{{ readingState.tooltip.data.exampleEn }}</small
    >
    <div class="tooltip-actions">
      <button
        class="tooltip-save-word"
        type="button"
        :class="{ 'is-saved': readingState.tooltip.data.savedInReviewBook }"
        :disabled="
          readingState.tooltip.loading ||
          readingState.tooltip.saving ||
          readingState.tooltip.data.savedInReviewBook ||
          !tooltipHasDefinition
        "
        @click="saveReaderWord(readingState.tooltip.data.word)"
      >
        <AppIcon
          :name="
            readingState.tooltip.data.savedInReviewBook ? 'check' : 'review'
          "
        />
        {{
          readingState.tooltip.loading
            ? "查询中…"
            : readingState.tooltip.saving
              ? "加入中…"
              : readingState.tooltip.data.savedInReviewBook
                ? "已加入待复习"
                : tooltipHasDefinition
                  ? "加入待复习"
                  : "暂无可保存词条"
        }}
      </button>
      <button
        v-if="readingState.tooltip.data.savedInReviewBook"
        type="button"
        class="tooltip-library-link"
        @click="openPendingLibrary"
      >
        查看词库<AppIcon name="arrow" />
      </button>
      <button
        v-if="
          readingState.tooltip.data.savedInReviewBook &&
          readingState.tooltip.sentence &&
          !readingState.tooltip.contextSaved
        "
        type="button"
        class="tooltip-library-link"
        :disabled="readingState.tooltip.saving"
        @click="saveReaderWord(readingState.tooltip.data.word)"
      >
        保留本文原句<AppIcon name="arrow" />
      </button>
    </div>
  </component>
</template>
