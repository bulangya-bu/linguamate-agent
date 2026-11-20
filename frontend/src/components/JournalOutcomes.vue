<script setup>
import { computed } from "vue";
import AppIcon from "./AppIcon.vue";
import { hasStudyActivity, trendDays } from "@/learning-journal";

const props = defineProps({
  journal: { type: Object, required: true },
  loading: Boolean,
});
const emit = defineEmits(["evidence", "navigate", "refresh"]);
const format = new Intl.NumberFormat("zh-CN").format;
const weekday = new Intl.DateTimeFormat("zh-CN", { weekday: "short", timeZone: "UTC" });
const week = computed(() => trendDays(props.journal.days, props.journal.today).map(day => ({
  date: day.date,
  label: weekday.format(new Date(day.date + "T12:00:00Z")).slice(-1),
  active: hasStudyActivity(day),
})));
const activeThisWeek = computed(() => week.value.filter(day => day.active).length);
const outcomes = computed(() => {
  const t = props.journal.totals;
  return [
    { id: "words", icon: "vocabulary", label: "学过单词", value: t.studiedWords, unit: "词",
      detail: "已掌握 " + format(t.masteredWords) + " 词", route: "vocabulary",
      description: "同一个词多次练习，只计为一个学过的词" },
    { id: "reading", icon: "news", label: "英文阅读", value: t.readingWords ?? t.readings,
      unit: t.readingWords == null ? "篇" : "词", detail: t.readingWords == null ? "已读完的文章" : "读完 " + format(t.readings) + " 篇",
      description: "只统计已读完的文章" },
    { id: "writing", icon: "writing", label: "英文写作", value: t.writtenWords ?? t.essays,
      unit: t.writtenWords == null ? "次" : "词", detail: t.writtenWords == null ? "累计提交次数" : "提交 " + format(t.essays) + " 次",
      route: t.essays ? null : "writing", description: "按每次提交的英文原文统计，含改稿" },
    { id: "expressions", icon: "chat", label: t.practiceTurns == null ? "对话发言" : "对话练习",
      value: t.practiceTurns ?? t.chatTurns, unit: "条",
      detail: t.practiceTurns == null ? "含学习助手消息" : "自己发出的消息", route: "chat" },
  ];
});
function target(item) {
  return item.route && !item.value ? item.route : null;
}
function open(item) {
  const route = target(item);
  if (route) emit("navigate", route);
  else emit("evidence", item.id);
}
</script>

<template>
  <section class="archive-ledger" aria-label="累计学习成果">
    <div class="archive-cover">
      <h2>累计学习</h2>
      <p class="cover-total"><strong>{{ format(journal.activeDays) }}</strong><span>天</span></p>
      <div class="cover-week">
        <p>近 7 天<span>学了 {{ activeThisWeek }} 天</span></p>
        <ol class="cover-days" aria-label="最近七天学习足迹">
          <li v-for="day in week" :key="day.date" :class="{ studied: day.active }"
            :aria-current="day.date === journal.today ? 'date' : undefined"
            :title="day.date + (day.active ? ' 有学习记录' : ' 无学习记录')">
            <time :datetime="day.date" :aria-label="day.date + (day.active ? ' 有学习记录' : ' 无学习记录')">{{ day.label }}</time>
          </li>
        </ol>
      </div>
    </div>
    <div class="archive-ledger-paper">
      <header class="ledger-heading">
        <h2>累计成果</h2>
        <div class="ledger-actions">
          <button type="button" class="ledger-refresh" aria-label="刷新学习档案" title="刷新学习档案"
            :disabled="loading" @click="emit('refresh')"><AppIcon name="review" /></button>
          <button type="button" class="ledger-records" @click="emit('evidence', 'words')">成长证据<AppIcon name="arrow" /></button>
        </div>
      </header>
      <div class="ledger-subjects">
        <button v-for="item in outcomes" :key="item.id" type="button" class="ledger-subject" :data-subject="item.id"
          :title="item.description"
          :aria-label="item.label + '，' + format(item.value) + item.unit + '，' + item.detail + (target(item) ? '，开始练习' : '，查看记录')"
          @click="open(item)">
          <span class="subject-title"><AppIcon :name="item.icon" />{{ item.label }}</span>
          <span class="subject-volume" :class="{ long: format(item.value).length > 6 }"><strong>{{ format(item.value) }}</strong><span>{{ item.unit }}</span></span>
          <span class="subject-detail">{{ item.detail }}<span class="subject-chevron" aria-hidden="true">›</span></span>
        </button>
      </div>
    </div>
  </section>
</template>
