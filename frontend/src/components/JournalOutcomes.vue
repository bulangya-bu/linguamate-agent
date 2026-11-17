<script setup>
import { computed, shallowRef } from "vue";
import AppIcon from "./AppIcon.vue";
import { journalReadingsThisWeek, trendDays } from "@/learning-journal";

const props = defineProps({
  journal: { type: Object, required: true },
});
const emit = defineEmits(["navigate", "evidence"]);

const bookmarks = [
  { id: "words", label: "词汇", icon: "vocabulary", count: "studiedWords", route: "vocabulary", start: "去练词" },
  { id: "expressions", label: "对话", icon: "chat", count: "chatTurns", route: "chat", start: "去对话" },
  { id: "writing", label: "写作", icon: "writing", count: "essays", route: "writing", start: "去写作" },
  { id: "reading", label: "阅读", icon: "news" },
];
const bookmark = shallowRef(bookmarks[0]);
const formatCount = new Intl.NumberFormat("zh-CN").format;

function textVolume(words, activity, total, recent) {
  if (words == null) return { main: total, details: [recent] };
  return { main: { label: activity, value: words, unit: "词" }, details: [total, recent] };
}

const pages = computed(() => {
  const { totals, days, today } = props.journal;
  const recent = trendDays(days, today);
  return {
    words: {
      main: { label: "累计学过", value: totals.studiedWords, unit: "词" },
      details: [
        { label: "已掌握", value: totals.masteredWords, unit: "词" },
        { label: "累计复习", value: totals.reviewWords, unit: "词" },
      ],
    },
    expressions: {
      main: { label: "累计对话", value: totals.chatTurns, unit: "轮" },
      details: [
        { label: "近 7 天 AI 辅导", value: recent.reduce((sum, day) => sum + (day.aiChatTurns || 0), 0), unit: "轮" },
      ],
    },
    writing: textVolume(
      totals.writtenWords,
      "累计写作",
      { label: "累计提交", value: totals.essays, unit: "篇" },
      { label: "近 7 天 AI 批改", value: recent.reduce((sum, day) => sum + (day.aiEssayReviews || 0), 0), unit: "篇" },
    ),
    reading: textVolume(
      totals.readingWords,
      "累计阅读",
      { label: "累计读完", value: totals.readings, unit: "篇" },
      { label: "本周读完", value: journalReadingsThisWeek(props.journal), unit: "篇" },
    ),
  };
});
const page = computed(() => pages.value[bookmark.value.id]);
const accuracy = computed(() => props.journal.totals.reviewAccuracy ?? null);
const canViewRecords = computed(() =>
  bookmark.value.id === "reading" || props.journal.totals[bookmark.value.count] > 0);

function moveBookmark(event) {
  const current = bookmarks.indexOf(bookmark.value);
  const destinations = {
    ArrowLeft: (current - 1 + bookmarks.length) % bookmarks.length,
    ArrowRight: (current + 1) % bookmarks.length,
    Home: 0,
    End: bookmarks.length - 1,
  };
  const next = destinations[event.key];
  if (typeof next !== "number") return;
  event.preventDefault();
  bookmark.value = bookmarks[next];
  event.currentTarget.children[next]?.focus();
}

function openPage() {
  if (canViewRecords.value) emit("evidence", bookmark.value.id);
  else emit("navigate", bookmark.value.route);
}
</script>

<template>
  <aside class="journal-outcomes" aria-labelledby="journal-outcomes-heading">
    <header class="journal-section-heading outcomes-heading">
      <h2 id="journal-outcomes-heading">学习成果</h2>
      <AppIcon name="journal" />
    </header>

    <div class="outcomes-book">
      <div class="outcomes-bookmarks" role="tablist" aria-label="成果书签" @keydown="moveBookmark">
        <button
          v-for="item in bookmarks"
          :id="'journal-bookmark-' + item.id"
          :key="item.id"
          class="outcomes-bookmark"
          :data-tone="item.id"
          type="button"
          role="tab"
          :tabindex="bookmark.id === item.id ? 0 : -1"
          :aria-selected="bookmark.id === item.id"
          aria-controls="journal-outcomes-page"
          @click="bookmark = item"
        >
          <AppIcon :name="item.icon" />{{ item.label }}
        </button>
      </div>

      <section
        id="journal-outcomes-page"
        class="outcomes-paper"
        :data-tone="bookmark.id"
        role="tabpanel"
        tabindex="0"
        :aria-labelledby="'journal-bookmark-' + bookmark.id"
      >
        <div :key="bookmark.id" class="outcomes-leaf">
          <AppIcon class="outcomes-watermark" :name="bookmark.icon" />
          <dl class="outcomes-volume">
            <div class="outcomes-main-amount">
              <dt>{{ page.main.label }}</dt>
              <dd><strong>{{ formatCount(page.main.value) }}</strong> {{ page.main.unit }}</dd>
            </div>
            <div v-for="item in page.details" :key="item.label" class="outcomes-sub-amount">
              <dt>{{ item.label }}</dt>
              <dd><strong>{{ formatCount(item.value) }}</strong> {{ item.unit }}</dd>
            </div>
          </dl>
          <div class="outcomes-paper-actions">
            <button type="button" @click="openPage">
              {{ canViewRecords ? "查看记录" : bookmark.start }}
              <AppIcon name="arrow" />
            </button>
          </div>
        </div>
      </section>
    </div>

    <dl class="outcomes-summary">
      <div>
        <dt>复习正确率</dt>
        <dd><strong>{{ accuracy === null ? "—" : accuracy + "%" }}</strong></dd>
        <dd v-if="journal.totals.reviewAttempts" class="outcomes-summary-detail">
          {{ formatCount(journal.totals.reviewAttempts) }} 次作答
        </dd>
      </div>
      <div>
        <dt>累计学习</dt>
        <dd><strong>{{ formatCount(journal.activeDays) }}</strong> 天</dd>
      </div>
      <div>
        <dt>连续登录</dt>
        <dd><strong>{{ formatCount(journal.currentStreak) }}</strong> 天</dd>
        <dd class="outcomes-summary-detail">最长 {{ formatCount(journal.longestStreak) }} 天</dd>
      </div>
    </dl>
  </aside>
</template>

<style scoped>
.journal-outcomes {
  display: grid;
  gap: 16px;
  min-width: 0;
  padding: var(--j-panel-padding);
  border: 1px solid var(--j-rule);
  border-radius: 20px;
  background: linear-gradient(145deg, color-mix(in srgb, var(--j-soft) 68%, var(--j-paper)), var(--j-paper) 72%);
  box-shadow: 0 8px 32px #22364805;
}

.outcomes-heading > svg {
  width: 20px;
  height: 20px;
  color: var(--j-blue-ink);
}

.outcomes-book { min-width: 0; }
.outcomes-bookmarks {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
  padding: 3px 6px 0;
}

[data-tone="words"] { --bookmark-tint: var(--j-green); --bookmark-ink: var(--j-green-ink); }
[data-tone="expressions"] { --bookmark-tint: var(--j-blue); --bookmark-ink: var(--j-blue-ink); }
[data-tone="writing"] { --bookmark-tint: var(--violet); --bookmark-ink: color-mix(in srgb, var(--violet) 38%, var(--j-ink)); }
[data-tone="reading"] { --bookmark-tint: #d9aa6b; --bookmark-ink: color-mix(in srgb, #d9aa6b 36%, var(--j-ink)); }

.outcomes-bookmark {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 43px;
  min-width: 0;
  padding: 7px 3px;
  border: 1px solid color-mix(in srgb, var(--bookmark-tint) 32%, var(--j-rule));
  border-bottom: 0;
  border-radius: 10px 10px 0 0;
  background: color-mix(in srgb, var(--bookmark-tint) 14%, var(--j-paper));
  color: var(--bookmark-ink);
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
}

.outcomes-bookmark svg { width: 16px; height: 16px; }
.outcomes-bookmark:hover { background: color-mix(in srgb, var(--bookmark-tint) 26%, var(--j-paper)); }
.outcomes-bookmark[aria-selected="true"] {
  border-top-width: 3px;
  border-top-color: var(--bookmark-ink);
  background: color-mix(in srgb, var(--bookmark-tint) 27%, var(--j-paper));
}

.outcomes-paper {
  position: relative;
  min-width: 0;
  padding: 18px 17px 13px 28px;
  border: 1px solid color-mix(in srgb, var(--bookmark-tint) 40%, var(--j-rule));
  border-radius: 9px 10px 14px 10px;
  background: linear-gradient(155deg, color-mix(in srgb, var(--bookmark-tint) 10%, var(--j-paper)), var(--j-paper) 66%);
  box-shadow:
    0 3px 0 -1px color-mix(in srgb, var(--bookmark-tint) 25%, var(--j-paper)),
    0 5px 0 -1px var(--j-rule);
}

.outcomes-paper::before {
  position: absolute;
  inset: 0 auto 0 13px;
  width: 3px;
  border-inline: 1px solid color-mix(in srgb, var(--bookmark-tint) 30%, var(--j-paper));
  content: "";
  pointer-events: none;
}

.outcomes-leaf {
  position: relative;
  display: grid;
  grid-template-rows: 1fr auto;
  min-height: 187px;
  animation: outcomes-page-in 200ms var(--ease) both;
}

.outcomes-watermark {
  position: absolute;
  top: 5px;
  right: 9px;
  width: clamp(48px, 5vw, 78px);
  height: auto;
  aspect-ratio: 1;
  color: var(--bookmark-ink);
  opacity: .14;
  transform: rotate(-11deg);
  pointer-events: none;
}

.outcomes-volume {
  position: relative;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  align-content: start;
  gap: 10px 14px;
  margin: 0;
  color: var(--j-muted);
}

.outcomes-volume div { min-width: 0; }

.outcomes-main-amount {
  grid-column: 1 / -1;
  border-bottom: 1px dashed color-mix(in srgb, var(--bookmark-tint) 38%, var(--j-rule));
  padding: 0 0 11px;
}

.outcomes-main-amount dt {
  font-size: 16px;
  font-weight: 600;
}

.outcomes-volume dd {
  margin: 3px 0 0;
  line-height: 1.12;
  overflow-wrap: anywhere;
}

.outcomes-main-amount dd { font-size: 16px; }
.outcomes-main-amount strong {
  color: var(--bookmark-ink);
  font-size: clamp(34px, 3.4vw, 52px);
  font-weight: 720;
  letter-spacing: -.035em;
}

.outcomes-sub-amount { font-size: 14px; }
.outcomes-sub-amount strong {
  color: var(--j-ink);
  font-size: 22px;
  font-weight: 650;
}

.outcomes-paper-actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 7px;
}
.outcomes-paper-actions > button {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-height: 34px;
  padding: 5px 9px;
  border-radius: 7px;
  color: var(--bookmark-ink);
  font-size: 14px;
  font-weight: 650;
}
.outcomes-paper-actions > button:hover { background: color-mix(in srgb, var(--bookmark-tint) 18%, var(--j-paper)); }
.outcomes-paper-actions svg { width: 15px; height: 15px; }

.outcomes-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin: 4px 0 0;
  padding-top: 12px;
  border-top: 1px solid var(--j-rule);
  color: var(--j-muted);
  font-size: 14px;
}
.outcomes-summary > div + div {
  padding-inline-start: 8px;
  border-inline-start: 1px solid var(--j-rule);
}
.outcomes-summary dd { margin: 2px 0 0; overflow-wrap: anywhere; }
.outcomes-summary dd strong {
  color: var(--j-ink);
  font-size: 22px;
  font-weight: 700;
}
.outcomes-summary .outcomes-summary-detail { font-size: 13px; }

@media (max-width: 460px) {
  .outcomes-bookmark svg { display: none; }
  .outcomes-summary { gap: 5px; }
  .outcomes-summary > div + div { padding-inline-start: 5px; }
}

@keyframes outcomes-page-in {
  from { opacity: 0; transform: translateX(6px); }
  to { opacity: 1; transform: translateX(0); }
}
</style>
