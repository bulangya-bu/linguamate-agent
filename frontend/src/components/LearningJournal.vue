<script setup>
import { onMounted, shallowRef } from "vue";
import { learningApi } from "@/api/modules";
import AppIcon from "./AppIcon.vue";
import JournalCalendar from "./JournalCalendar.vue";
import JournalOutcomes from "./JournalOutcomes.vue";
import ArchiveTrend from "@/features/journal/ArchiveTrend.vue";
import "@/styles/learning-journal.css";

const emit = defineEmits(["navigate", "evidence"]);
const journal = shallowRef(null);
const browsedMonth = shallowRef("");
const selectedDate = shallowRef("");
const loading = shallowRef(false);
const journalError = shallowRef("");

function browseMonth(month) {
  browsedMonth.value = month;
  selectedDate.value = "";
}

function selectDate(date) {
  browsedMonth.value = date.slice(0, 7);
  selectedDate.value = date;
}

async function load() {
  if (loading.value) return;
  loading.value = true;
  journalError.value = "";
  try {
    const result = await learningApi.journal();
    if (!result?.today || !result.totals || !Array.isArray(result.days)) {
      throw new Error("学习档案数据暂不可用");
    }
    journal.value = result;
  } catch (error) {
    journalError.value = error.message || "学习档案暂时无法加载";
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div class="learning-journal" :aria-busy="loading">
    <div v-if="!journal" class="journal-load-state" :role="journalError ? 'alert' : 'status'">
      <AppIcon name="journal" />
      <p>{{ journalError || "正在打开学习档案…" }}</p>
      <button v-if="journalError" type="button" @click="load">重新加载</button>
    </div>
    <template v-else>
      <JournalOutcomes :journal="journal" :loading="loading"
        @refresh="load" @evidence="emit('evidence', $event)" @navigate="emit('navigate', $event)" />
      <p v-if="journalError" class="archive-refresh-note" role="alert">刷新失败，当前显示上次加载的档案。<button type="button" @click="load">重试</button></p>
      <div class="archive-spread">
        <ArchiveTrend :days="journal.days" :today="journal.today" :selected-date="selectedDate"
          @select="selectDate" @navigate="emit('navigate', $event)" />
        <JournalCalendar :days="journal.days" :today="journal.today" :month="browsedMonth || journal.today.slice(0, 7)"
          :selected-date="selectedDate" @browse="browseMonth" @select="selectDate" />
      </div>
    </template>
  </div>
</template>
