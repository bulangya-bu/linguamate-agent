<script setup>
import { computed, nextTick, shallowRef, watch } from 'vue';
import { learningApi } from '@/api/modules';
import AppIcon from './AppIcon.vue';
import GrowthRecordDetail from './growth/GrowthRecordDetail.vue';
import GrowthRecordList from './growth/GrowthRecordList.vue';
import { evidenceRecords, evidenceTabs } from './growth/evidenceRecords';
import '@/styles/growth-evidence.css';

const props = defineProps({
  open: Boolean,
  initialTab: { type: String, default: 'words' },
});
const emit = defineEmits(['close', 'navigate']);

const pageSize = 5;
const dialog = shallowRef(null);
const detailPane = shallowRef(null);
const data = shallowRef(null);
const loading = shallowRef(false);
const error = shallowRef('');
const retry = shallowRef(0);
const tab = shallowRef('words');
const page = shallowRef(0);
const selectedId = shallowRef(null);

const collections = computed(() => evidenceRecords(data.value));
const category = computed(() => evidenceTabs.find(item => item.id === tab.value));
const records = computed(() => collections.value[tab.value]);
const pageCount = computed(() => Math.ceil(records.value.length / pageSize));
const pageRecords = computed(() => records.value.slice(page.value * pageSize, (page.value + 1) * pageSize));
const selected = computed(() => pageRecords.value.find(item => item.id === selectedId.value) ?? pageRecords.value[0]);
const articleId = computed(() => tab.value === 'reading' ? selected.value?.articleId : undefined);
const action = computed(() => articleId.value ? '打开原文' : category.value.action);

watch(() => props.open, async open => {
  if (open) resetSelection(evidenceTabs.find(item => item.id === props.initialTab)?.id ?? 'words');
  await nextTick();
  if (open !== props.open || !dialog.value) return;
  if (open && !dialog.value.open) dialog.value.showModal();
  if (!open && dialog.value.open) dialog.value.close();
}, { immediate: true });

watch([() => props.open, retry], async ([open], _, onCleanup) => {
  if (!open) return;
  const request = new AbortController();
  onCleanup(() => request.abort());
  loading.value = true;
  error.value = '';
  data.value = null;
  try {
    const result = await learningApi.growth(request.signal);
    if (!request.signal.aborted) data.value = result;
  } catch (failure) {
    if (!request.signal.aborted) error.value = failure.message;
  } finally {
    if (!request.signal.aborted) loading.value = false;
  }
}, { immediate: true });

watch([tab, selected], () => {
  if (detailPane.value) detailPane.value.scrollTop = 0;
}, { flush: 'post' });

function resetSelection(id) {
  tab.value = id;
  changePage(0);
}

function changePage(index) {
  page.value = index;
  selectedId.value = null;
}

function selectTab(id) {
  if (tab.value !== id) resetSelection(id);
}

function moveTab(event) {
  const activeIndex = evidenceTabs.indexOf(category.value);
  const count = evidenceTabs.length;
  const index = {
    ArrowLeft: (activeIndex - 1 + count) % count,
    ArrowRight: (activeIndex + 1) % count,
    Home: 0,
    End: count - 1,
  }[event.key];
  if (index == null) return;
  event.preventDefault();
  selectTab(evidenceTabs[index].id);
  event.currentTarget.children[index].focus();
}

function practice() {
  emit('navigate', category.value.route, articleId.value);
  emit('close');
}

function backdropClose(event) {
  if (event.target !== dialog.value) return;
  const bounds = dialog.value.getBoundingClientRect();
  if (event.clientX < bounds.left || event.clientX > bounds.right
    || event.clientY < bounds.top || event.clientY > bounds.bottom) emit('close');
}
</script>

<template>
  <dialog
    ref="dialog"
    class="growth-evidence"
    :data-category="tab"
    aria-labelledby="growth-title"
    @cancel.prevent="emit('close')"
    @click="backdropClose"
  >
    <header class="growth-heading">
      <div>
        <span class="growth-heading-icon"><AppIcon name="journal" /></span>
        <h2 id="growth-title">成长证据</h2>
      </div>
      <button class="panel-close" type="button" aria-label="关闭成长证据" autofocus @click="emit('close')">
        <AppIcon name="close" />
      </button>
    </header>

    <nav class="growth-tabs" role="tablist" aria-label="成长证据类型" @keydown="moveTab">
      <button
        v-for="item in evidenceTabs"
        :id="'growth-tab-' + item.id"
        :key="item.id"
        type="button"
        role="tab"
        :data-bookmark="item.id"
        :aria-selected="tab === item.id"
        :tabindex="tab === item.id ? 0 : -1"
        aria-controls="growth-content"
        @click="selectTab(item.id)"
      >
        <AppIcon :name="item.icon" />
        <span>{{ item.label }}</span>
        <span v-if="data" class="growth-tab-count">近期 {{ collections[item.id].length }} {{ item.unit }}</span>
      </button>
    </nav>

    <div
      id="growth-content"
      class="growth-content"
      :class="{ 'has-records': selected }"
      role="tabpanel"
      :aria-labelledby="'growth-tab-' + tab"
      :aria-busy="loading"
    >
      <div v-if="loading" class="growth-state" role="status">正在加载记录…</div>
      <div v-else-if="error" class="growth-state" role="alert">
        <p>{{ error }}</p><button type="button" @click="retry++">重新加载</button>
      </div>
      <template v-else-if="selected">
        <GrowthRecordList
          :records="pageRecords"
          :active-id="selected.id"
          :category="category.label"
          :page="page"
          :page-count="pageCount"
          @select="selectedId = $event"
          @page="changePage"
        />
        <section id="growth-detail" ref="detailPane" class="growth-detail-pane" tabindex="0" aria-label="记录详情">
          <GrowthRecordDetail :key="tab + '-' + selected.id" :record="selected" :type="tab" />
        </section>
      </template>
      <div v-else class="growth-state">
        <AppIcon :name="category.icon" />
        <h3>暂无{{ category.label }}记录</h3>
      </div>
    </div>

    <footer class="growth-footer">
      <button type="button" @click="practice">{{ action }}<AppIcon name="arrow" /></button>
    </footer>
  </dialog>
</template>
