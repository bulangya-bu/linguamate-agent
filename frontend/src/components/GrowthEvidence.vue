<template>
  <dialog
    ref="dialog"
    class="growth-evidence"
    aria-labelledby="growth-title"
    @cancel.prevent="$emit('close')"
    @click="backdropClose"
  >
    <header class="growth-heading">
      <h2 id="growth-title">成长证据</h2>
      <button class="panel-close" type="button" aria-label="关闭成长证据" @click="$emit('close')">
        <AppIcon name="close" />
      </button>
    </header>
    <nav class="growth-tabs" aria-label="成长证据类型">
      <button
        v-for="item in tabs"
        :key="item.id"
        :class="{ active: tab === item.id }"
        :aria-pressed="tab === item.id"
        @click="tab = item.id"
      >
        {{ item.label }}<span>{{ counts[item.id] || 0 }}</span>
      </button>
    </nav>
    <div v-if="loading" class="growth-state" role="status">
      正在加载记录…
    </div>
    <div v-else-if="error" class="growth-state" role="alert">
      <p>{{ error }}</p>
      <button type="button" @click="load">重新加载</button>
    </div>
    <div v-else class="growth-records">
      <template v-if="tab === 'words'"
        ><article
          v-for="word in data?.words"
          :key="word.wordId"
          class="growth-word-record"
        >
          <header>
            <h3>{{ word.word }}</h3>
            <span>{{ word.meaning }}</span>
          </header>
          <ol>
            <li v-for="attempt in word.attempts" :key="attempt.id">
              <span
                class="growth-attempt-dot"
                :class="{ correct: attempt.correct }"
                >{{ attempt.correct ? "✓" : "↺" }}</span
              >
              <div>
                <span
                  >{{ date(attempt.createdAt) }} ·
                  {{
                    attempt.type === "STRICT_REVIEW"
                      ? "拼写与释义复习"
                      : "词语练习"
                  }}</span
                >
                <p>{{ attempt.answer || "旧记录未保留答案" }}</p>
                <small>{{ attempt.correct ? "本次答对" : "本次答错" }}</small>
              </div>
            </li>
          </ol>
        </article></template
      >
      <template v-else-if="tab === 'expressions'"
        ><article
          v-for="item in data?.expressions"
          :key="item.id"
          class="growth-expression"
        >
          <header>
            <h3>{{ item.title }}</h3>
            <time>{{ date(item.createdAt) }}</time>
          </header>
          <span class="growth-label">我的原句</span>
          <p class="growth-quote">{{ item.answer }}</p>
          <template v-if="item.feedback?.source === 'model'"
            ><span class="growth-label"
              >AI 表达参考 · {{ item.feedback.modelName }}</span
            >
            <p class="growth-suggestion">{{ item.feedback.improved }}</p>
            <p class="growth-explanation">
              {{ item.feedback.explanation }}
            </p></template
          >
          <p v-else class="growth-explanation">
            {{
              item.feedback?.source === "local"
                ? "本地练习反馈"
                : "暂无改写记录"
            }}
          </p>
          <details v-if="item.reply">
            <summary>查看当时的回复</summary>
            <p>{{ item.reply }}</p>
          </details>
        </article></template
      >
      <template v-else-if="tab === 'writing'"
        ><article
          v-for="item in data?.writing"
          :key="item.id"
          class="growth-writing"
        >
          <header>
            <h3>{{ item.title || "我的英文习作" }}</h3>
            <time>{{ date(item.createdAt) }}</time>
          </header>
          <div class="growth-writing-pair">
            <section v-if="item.before">
              <span class="growth-label">上一次提交的原稿</span>
              <p>{{ item.before }}</p>
            </section>
            <section>
              <span class="growth-label">{{
                item.before ? "我再次提交的改写" : "我的原稿"
              }}</span>
              <p>{{ item.after }}</p>
            </section>
          </div>
          <details v-if="item.suggestion">
            <summary>
              查看当时的{{
                ["model", "AI批改"].includes(item.source) ? " AI" : "本地"
              }}参考稿
            </summary>
            <p>{{ item.suggestion }}</p>
          </details>
          <p v-if="item.comparable || item.before || item.model" class="growth-explanation">
            <template v-if="item.comparable">同标准评分 {{ item.previousScore }} → {{ item.score }} 分</template>
            <template v-else-if="item.before">两次评分不可比</template>
            <span v-if="item.model">{{ item.comparable || item.before ? " · " : "" }}{{ item.model }}</span>
          </p>
        </article></template
      >
      <div v-if="!counts[tab]" class="growth-state">
        <AppIcon name="journal" />
        <h3>暂无记录</h3>
      </div>
    </div>
    <footer>
      <button type="button" @click="practice">
        {{
          {
            words: "继续练词",
            expressions: "继续对话",
            writing: "打开写作工坊",
          }[tab]
        }}<AppIcon name="arrow" />
      </button>
    </footer>
  </dialog>
</template>
<script setup>
import { computed, nextTick, ref, watch } from "vue";
import { learningApi } from "@/api/modules";
import AppIcon from "./AppIcon.vue";
import "@/styles/growth-evidence.css";
const props = defineProps({
    open: Boolean,
    initialTab: { type: String, default: "words" },
  }),
  emit = defineEmits(["close", "navigate"]);
const dialog = ref(null),
  data = ref(null),
  loading = ref(false),
  error = ref(""),
  tab = ref("words");
const tabs = [
  { id: "words", label: "词语轨迹" },
  { id: "expressions", label: "表达记录" },
  { id: "writing", label: "原稿与改写" },
];
const counts = computed(() => ({
  words: data.value?.words.length,
  expressions: data.value?.expressions.length,
  writing: data.value?.writing.length,
}));
watch(
  () => props.open,
  async (open) => {
    await nextTick();
    if (open) {
      tab.value = props.initialTab;
      if (!dialog.value.open) dialog.value.showModal();
      load();
    } else if (dialog.value.open) dialog.value.close();
  },
  { immediate: true },
);
async function load() {
  loading.value = true;
  error.value = "";
  try {
    data.value = await learningApi.growth();
  } catch (e) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
}
function date(value) {
  return value?.replace("T", " ").slice(0, 16) || "";
}
function practice() {
  emit(
    "navigate",
    {
      words: "vocabulary",
      expressions: "chat",
      writing: "writing",
    }[tab.value],
  );
  emit("close");
}
function backdropClose(event) {
  if (event.target !== dialog.value) return;
  const r = dialog.value.getBoundingClientRect();
  if (
    event.clientX < r.left ||
    event.clientX > r.right ||
    event.clientY < r.top ||
    event.clientY > r.bottom
  )
    emit("close");
}
</script>
