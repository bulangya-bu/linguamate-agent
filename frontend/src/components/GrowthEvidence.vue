<template>
  <dialog
    ref="dialog"
    class="growth-evidence"
    aria-labelledby="growth-title"
    @cancel.prevent="$emit('close')"
    @click="backdropClose"
  >
    <header class="growth-heading">
      <div>
        <span class="mission-eyebrow">A TRACE OF YOUR OWN</span>
        <h2 id="growth-title">每一点进步，都有来处。</h2>
      </div>
      <button type="button" aria-label="关闭成长证据" @click="$emit('close')">
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
      正在寻找你留下的记录…
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
                ? "此条使用本地练习反馈，未进行模型改写。"
                : "此条没有结构化改写记录。"
            }}
          </p>
          <details v-if="item.reply">
            <summary>查看当时的回复</summary>
            <p>{{ item.reply }}</p>
          </details>
        </article>
        <div
          v-if="data?.missions.some((m) => m.progress.turns.length)"
          class="growth-cross-link"
        >
          <span>主题情境中的原句与再次表达，保存在任务记录里。</span
          ><button @click="tab = 'missions'">查看主题任务 →</button>
        </div></template
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
          <p class="growth-explanation">
            {{
              item.comparable
                ? `相同反馈来源与标准：${item.previousScore} → ${item.score} 分`
                : "本条仅展示实际提交，不比较不同来源或标准的分数。"
            }}<span v-if="item.model"> · {{ item.model }}</span>
          </p>
        </article></template
      >
      <template v-else
        ><article
          v-for="item in data?.missions"
          :key="item.id"
          class="growth-mission"
        >
          <header>
            <div>
              <span class="growth-label"
                >{{ item.studyDate }} ·
                {{
                  item.status === "COMPLETED" ? "已完成复盘" : "练习进行中"
                }}</span
              >
              <h3>{{ item.lesson.title }}</h3>
            </div>
            <button
              type="button"
              class="text-button"
              @click="openMission(item)"
            >
              {{ item.status === "COMPLETED" ? "回看任务" : "继续任务"
              }}<AppIcon name="arrow" />
            </button>
          </header>
          <div class="growth-mission-metrics">
            <span>词语 {{ item.progress.words.length }}/3</span
            ><span>表达 {{ missionProgress(item).practiced }}/3</span
            ><span>重新表达 {{ missionProgress(item).retries }} 次</span
            ><span>提示 {{ missionProgress(item).hintCount }} 条</span>
          </div>
          <details
            v-for="(step, i) in item.lesson.steps"
            :key="i"
            :open="i === 0 && item.progress.turns.length > 0"
          >
            <summary>
              {{ step.title
              }}<span
                >{{
                  item.progress.turns.filter((t) => t.step === i).length
                }}
                次表达</span
              >
            </summary>
            <div
              v-for="(turn, j) in item.progress.turns.filter(
                (t) => t.step === i,
              )"
              :key="j"
              class="growth-mission-turn"
            >
              <span class="growth-label"
                >第 {{ j + 1 }} 次 ·
                {{
                  turn.hintLevel
                    ? `使用 ${turn.hintLevel} 级提示`
                    : "未查看提示"
                }}</span
              >
              <p class="growth-quote">{{ turn.answer }}</p>
              <span class="growth-label">{{
                turn.feedback.source === "model"
                  ? `AI 参考 · ${turn.feedback.modelName}`
                  : "本地练习 · 保留原句"
              }}</span>
              <p
                v-if="turn.feedback.source === 'model'"
                class="growth-suggestion"
              >
                {{ turn.feedback.improved }}
              </p>
            </div>
            <p
              v-if="!item.progress.turns.some((t) => t.step === i)"
              class="growth-explanation"
            >
              还没有这一轮的表达。
            </p>
          </details>
          <div v-if="item.progress.reflection" class="growth-reflection">
            <span class="growth-label">我的收获</span>
            <p>{{ item.progress.reflection }}</p>
          </div>
        </article></template
      >
      <div v-if="!counts[tab]" class="growth-state">
        <AppIcon name="journal" />
        <h3>
          {{
            tab === "expressions" &&
            data?.missions.some((m) => m.progress.turns.length)
              ? "主题情境的表达已保存在任务中"
              : "这里等待你的第一条记录"
          }}
        </h3>
        <p>完成一次练习后，自己的答案就会出现在这里。</p>
      </div>
    </div>
    <footer>
      <span>最近记录 · 当前账号的实际答案与提交</span
      ><button type="button" @click="practice">
        {{
          {
            words: "继续练词",
            expressions: "继续对话",
            writing: "打开写作工坊",
            missions: "开始今日主题",
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
import { missionProgress } from "@/mission-progress";
import "@/styles/growth-evidence.css";
const props = defineProps({
    open: Boolean,
    initialTab: { type: String, default: "missions" },
  }),
  emit = defineEmits(["close", "navigate", "mission"]);
const dialog = ref(null),
  data = ref(null),
  loading = ref(false),
  error = ref(""),
  tab = ref("missions");
const tabs = [
  { id: "missions", label: "主题任务" },
  { id: "words", label: "词语轨迹" },
  { id: "expressions", label: "表达记录" },
  { id: "writing", label: "原稿与改写" },
];
const counts = computed(() => ({
  missions: data.value?.missions.length,
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
function openMission(item) {
  emit("mission", item.id);
  emit("close");
}
function practice() {
  emit(
    "navigate",
    {
      words: "vocabulary",
      expressions: "chat",
      writing: "writing",
      missions: "mission",
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
