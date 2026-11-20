<script setup>
import { computed } from 'vue';
import AppIcon from '@/components/AppIcon.vue';

const props = defineProps({
  review: { type: Object, required: true },
  activeIndex: { type: Number, default: -1 },
  linkToText: Boolean,
  editRanges: { type: Array, default: () => [] },
});
const emit = defineEmits(['select', 'settings', 'edit-sentence']);
const dimensions = [
  { key: 'grammarScore', label: '语法' },
  { key: 'vocabularyScore', label: '词汇' },
  { key: 'structureScore', label: '结构' },
  { key: 'logicScore', label: '逻辑' },
];
const categories = {
  grammar: '语法', vocabulary: '词汇', structure: '结构', logic: '衔接', expression: '表达',
};
const scores = computed(() => dimensions
  .map(({ key, label }) => ({ label, value: props.review[key] }))
  .filter(({ value }) => Number.isFinite(value)));
const source = computed(() => {
  if (props.review.reviewSource === 'AI批改') return '在线批改';
  return props.review.reviewSource ? '本地批改' : '';
});
const delta = computed(() => {
  const comparison = props.review.comparison;
  return comparison ? Math.round((comparison.currentScore - comparison.previousScore) * 10) / 10 : 0;
});
</script>

<template>
  <div class="ws-feedback-content">
    <div class="ws-score-summary" v-if="Number.isFinite(review.totalScore)">
      <div class="ws-total"><strong>{{ review.totalScore }}</strong><span> / 100</span></div>
      <div>
        <span v-if="source" class="ws-source" :title="review.modelName || undefined">{{ source }}</span>
        <p v-if="review.summary">{{ review.summary }}</p>
      </div>
    </div>
    <div v-if="scores.length" class="ws-dimensions" aria-label="评分细项">
      <div v-for="{ label, value } in scores" :key="label"><span>{{ label }}</span><b>{{ value }}</b></div>
    </div>
    <div v-if="review.comparison" class="ws-comparison">
      <span>上次 {{ review.comparison.previousScore }} → 本次 {{ review.comparison.currentScore }}</span>
      <strong v-if="review.comparison.comparable">{{ delta > 0 ? '+' : '' }}{{ delta }} 分</strong>
      <small>{{ review.comparison.note }}</small>
    </div>
    <div v-if="Array.isArray(review.feedback)" class="ws-sentence-feedback">
      <h3 v-if="review.feedback.length" class="ws-feedback-title">逐句建议</h3>
      <article v-for="(item, index) in review.feedback" :key="index" :data-feedback-item="index"
        class="ws-feedback" :class="{ selected: activeIndex === index }">
        <div class="ws-feedback-heading">
          <button v-if="linkToText" class="ws-feedback-select" type="button" @click="emit('select', index)">
            <strong>{{ categories[item.category] || item.category || '建议' }}</strong><AppIcon name="arrow" />
          </button>
          <h4 v-else>{{ categories[item.category] || item.category || '建议' }}</h4>
          <button v-if="editRanges[index]" class="ws-text-action" type="button" @click="emit('edit-sentence', index)">改这句</button>
        </div>
        <p v-if="item.originalSentence" class="ws-original">{{ item.originalSentence }}</p>
        <p v-if="item.correctedSentence" class="ws-corrected">{{ item.correctedSentence }}</p>
        <p v-if="item.suggestion" class="ws-reason">{{ item.suggestion }}</p>
      </article>
    </div>
    <button v-if="linkToText && source === '本地批改'" class="ws-text-action ws-configure" type="button"
      @click="emit('settings')">模型配置</button>
  </div>
</template>
