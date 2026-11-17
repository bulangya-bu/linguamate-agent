<script setup>
import AppIcon from '../AppIcon.vue';
import { evidenceDate } from './evidenceRecords';

defineProps({
  records: { type: Array, required: true },
  activeId: { type: [String, Number], required: true },
  category: { type: String, required: true },
  page: { type: Number, required: true },
  pageCount: { type: Number, required: true },
});
const emit = defineEmits(['select', 'page']);
</script>

<template>
  <nav class="growth-index" :aria-label="category + '记录'">
    <ol class="growth-index-list">
      <li v-for="item in records" :key="item.id">
        <button
          class="growth-index-item"
          type="button"
          :aria-pressed="activeId === item.id"
          aria-controls="growth-detail"
          @click="emit('select', item.id)"
        >
          <span class="growth-index-line">
            <strong>{{ item.title }}</strong>
            <span v-if="item.status" class="growth-chip" :data-tone="item.tone">{{ item.status }}</span>
          </span>
          <span class="growth-index-line growth-index-meta">
            <span>{{ item.subtitle }}</span>
            <time v-if="item.date" :datetime="item.date">{{ evidenceDate(item.date).slice(5, 10) }}</time>
          </span>
        </button>
      </li>
    </ol>
    <div v-if="pageCount > 1" class="growth-pagination">
      <button type="button" aria-label="上一页" :disabled="page === 0" @click="emit('page', page - 1)">
        <AppIcon class="growth-arrow-back" name="arrow" />
      </button>
      <span aria-live="polite">{{ page + 1 }} / {{ pageCount }}</span>
      <button type="button" aria-label="下一页" :disabled="page + 1 === pageCount" @click="emit('page', page + 1)">
        <AppIcon name="arrow" />
      </button>
    </div>
  </nav>
</template>
