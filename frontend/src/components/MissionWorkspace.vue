<template>
  <div class="mission-workspace" :aria-busy="busy || loading">
    <div v-if="loading || !mission" class="mission-load">
      <AppIcon name="spark" />
      <h2>{{ loading ? "正在打开你的主题任务…" : "任务暂时没有打开" }}</h2>
      <p v-if="error" role="alert">{{ error }}</p>
      <button v-if="!loading" class="primary-button" @click="load">重试</button>
    </div>
    <template v-else>
      <header class="mission-heading">
        <div>
          <span class="mission-eyebrow"
            >TODAY’S FIELD NOTES <span>{{ mission.studyDate }}</span></span
          >
          <h2>{{ lesson.title }}</h2>
        </div>
        <button type="button" class="text-button" @click="$emit('home')">
          返回概览<AppIcon name="arrow" />
        </button>
      </header>
      <nav class="mission-stagebar" aria-label="主题任务阶段">
        <button
          v-for="(item, i) in missionStages"
          :key="item.id"
          type="button"
          :class="{ active: stage === i, done: progress.done[i] }"
          :aria-current="stage === i ? 'step' : undefined"
          :disabled="busy || i > unlocked"
          @click="stage = i"
        >
          <span>{{ progress.done[i] ? "✓" : `0${i + 1}` }}</span
          ><strong>{{ item.title }}</strong
          ><small>{{
            ["阅读材料", "3 个主题词", "3 轮表达", "记录与回看"][i]
          }}</small>
        </button>
      </nav>
      <div class="mission-columns">
        <aside class="mission-context">
          <div class="mission-context-top">
            <span class="mission-eyebrow">{{
              mission.key === "water" ? "CAMPUS SCIENCE" : "CAMPUS EXPLORER"
            }}</span
            ><span>{{ lesson.level }}</span>
          </div>
          <MissionSceneArt :theme="mission.key" />
          <div class="mission-role">
            <span>你的身份</span>
            <h3>{{ lesson.studentRole }}</h3>
            <p>{{ lesson.subtitle }}</p>
          </div>
          <div class="mission-context-foot">
            <span><AppIcon name="chat" />{{ lesson.role }}</span
            ><span>{{ lesson.duration }}</span>
          </div>
        </aside>
        <section class="mission-page" :key="stage">
          <div v-if="error" class="mission-error" role="alert">
            {{ error
            }}<button type="button" :disabled="busy" @click="refresh">
              同步任务
            </button>
          </div>
          <template v-if="stage === 0">
            <header class="mission-page-heading">
              <span class="mission-eyebrow">01 / READ & NOTICE</span>
              <h3>
                {{
                  mission.key === "water"
                    ? "A small sensor. A daily change."
                    : "A first day. A friendly guide."
                }}
              </h3>
              <p>读完后，试着留意三个主题词。</p>
            </header>
            <div class="mission-page-scroll mission-reading">
              <p v-for="(paragraph, i) in lesson.paragraphs" :key="i">
                <template
                  v-for="(token, j) in paragraph.split(
                    /\b(reduce|sensor|waste|library|directions|available)\b/gi,
                  )"
                  :key="j"
                  ><mark
                    v-if="
                      /^(reduce|sensor|waste|library|directions|available)$/i.test(
                        token,
                      )
                    "
                    >{{ token }}</mark
                  ><template v-else>{{ token }}</template></template
                >
              </p>
              <small>{{ lesson.source }}</small>
            </div>
            <footer class="mission-page-action">
              <span>{{
                progress.done[0] ? "已保留阅读记录" : "准备好，把新词带入语境。"
              }}</span
              ><button
                type="button"
                class="primary-button"
                :disabled="busy"
                @click="read"
              >
                {{ progress.done[0] ? "进入词语练习" : "已读完，练词语"
                }}<AppIcon name="arrow" />
              </button>
            </footer>
          </template>
          <template v-else-if="stage === 1">
            <header class="mission-page-heading">
              <span class="mission-eyebrow">02 / WORDS IN CONTEXT</span>
              <h3>在一句话里，认识一个词。</h3>
              <div class="mission-word-tabs">
                <button
                  v-for="(word, i) in lesson.words"
                  :key="word.word"
                  type="button"
                  :class="{ active: wordIndex === i }"
                  :aria-pressed="wordIndex === i"
                  :disabled="busy"
                  @click="wordIndex = i"
                >
                  {{ i + 1
                  }}<AppIcon v-if="wordResult(i)" name="check" /></button
                ><span>{{ mission.progress.words.length }} / 3 已作答</span>
              </div>
            </header>
            <div class="mission-page-scroll mission-word-question">
              <h4>{{ currentWord.word }}</h4>
              <p>{{ currentWord.sentence }}</p>
              <p v-if="currentWordResult" class="mission-word-translation">
                {{ currentWord.translation }}
              </p>
              <div
                v-if="currentWordResult"
                class="mission-word-feedback"
                :class="{ wrong: !currentWordResult.correct }"
                role="status"
              >
                <AppIcon
                  :name="currentWordResult.correct ? 'check' : 'review'"
                /><span
                  >{{ currentWordResult.correct ? "答对了" : "已加入错词强化"
                  }}<strong>{{ currentWord.meaning }}</strong></span
                >
              </div>
              <div class="mission-word-options">
                <button
                  v-for="option in currentWord.options"
                  :key="option"
                  type="button"
                  :disabled="busy || !!currentWordResult || completed"
                  :class="{
                    correct:
                      currentWordResult && option === currentWord.meaning,
                    wrong:
                      currentWordResult &&
                      !currentWordResult.correct &&
                      option === currentWordResult.answer,
                  }"
                  @click="answerWord(option)"
                >
                  {{ option }}
                </button>
              </div>
            </div>
            <footer class="mission-page-action">
              <span>实际作答会进入词库与学习记录</span
              ><button
                type="button"
                class="primary-button"
                :disabled="!currentWordResult || busy"
                @click="nextWord"
              >
                {{
                  mission.progress.words.length === 3 ? "进入情境" : "下一个词"
                }}<AppIcon name="arrow" />
              </button>
            </footer>
          </template>
          <template v-else-if="stage === 2">
            <header class="mission-page-heading">
              <div class="mission-scene-tabs">
                <button
                  v-for="(item, i) in lesson.steps"
                  :key="item.title"
                  type="button"
                  :class="{ active: sceneIndex === i }"
                  :disabled="busy || i > progress.practiced"
                  @click="sceneIndex = i"
                >
                  <span>0{{ i + 1 }}</span
                  >{{ item.title
                  }}<AppIcon v-if="attemptsFor(i).length" name="check" />
                </button>
              </div>
            </header>
            <div
              ref="conversation"
              class="mission-page-scroll mission-conversation"
            >
              <div class="mission-person">
                <span>{{ mission.key === "water" ? "A" : "J" }}</span>
                <div>
                  <strong>{{ lesson.role }}</strong>
                  <p>{{ currentStep.question }}</p>
                  <small>{{ currentStep.purpose }}</small>
                </div>
              </div>
              <div v-if="hintLevel" class="mission-hints">
                <span>已查看的提示 · {{ hintLevel }}/3</span>
                <p
                  v-for="(hint, i) in currentStep.hints.slice(0, hintLevel)"
                  :key="i"
                >
                  <b>{{ ["词语线索", "句式支架", "示例表达"][i] }}</b
                  >{{ hint }}
                </p>
              </div>
              <article
                v-for="(turn, i) in currentAttempts"
                :key="turn.createdAt + i"
                class="mission-attempt"
              >
                <header>
                  <span>我的第 {{ i + 1 }} 次表达</span
                  ><small>{{
                    turn.hintLevel
                      ? `使用 ${turn.hintLevel} 级提示`
                      : "未查看提示"
                  }}</small>
                </header>
                <p class="mission-student-answer">{{ turn.answer }}</p>
                <div class="mission-coach-feedback">
                  <span class="mission-feedback-source"
                    ><AppIcon name="spark" />{{
                      turn.feedback.source === "model"
                        ? `AI 教练 · ${turn.feedback.modelName}`
                        : "本地练习 · 模型未连接"
                    }}</span
                  >
                  <p>{{ turn.reply }}</p>
                  <template v-if="turn.feedback.source === 'model'"
                    ><strong>{{
                      turn.feedback.revisionType === "correction"
                        ? "修改建议"
                        : "表达参考"
                    }}</strong>
                    <p v-if="turn.feedback.improved" class="mission-revision">
                      {{ turn.feedback.improved }}
                    </p>
                    <p>{{ turn.feedback.explanation }}</p>
                    <div
                      v-if="turn.feedback.phrases?.length"
                      class="mission-phrase-notes"
                    >
                      <span
                        v-for="phrase in turn.feedback.phrases"
                        :key="phrase.text"
                        ><strong>{{ phrase.text }}</strong
                        >{{ phrase.meaningCn }}</span
                      >
                    </div>
                    <p
                      v-if="turn.feedback.nextChallenge"
                      class="mission-next-challenge"
                    >
                      再试一小步 · {{ turn.feedback.nextChallenge }}
                    </p></template
                  >
                  <p v-else class="mission-local-note">
                    当前保留原句与词语核对；连接模型后可获得针对这句表达的反馈。
                    <button
                      type="button"
                      @click="$emit('settings', $event.currentTarget)"
                    >
                      打开模型配置 →
                    </button>
                  </p>
                  <div class="mission-coverage">
                    <span>要点词核对</span
                    ><span
                      v-for="item in turn.coverage"
                      :key="item.label"
                      :class="{ present: item.present }"
                      >{{ item.present ? "✓" : "○" }} {{ item.label
                      }}<small v-if="item.present">{{
                        item.evidence.join(" / ")
                      }}</small></span
                    >
                  </div>
                  <small class="mission-coverage-note"
                    >仅核对词语是否出现，不代表语义、语法或流利度达标。</small
                  >
                  <details v-if="turn.trace?.length">
                    <summary>查看 AI 工作过程</summary>
                    <ol>
                      <li v-for="(trace, j) in turn.trace" :key="j">
                        <strong>{{ trace.phase }} · {{ trace.title }}</strong>
                        {{ trace.detail || trace.content || trace.action }}
                      </li>
                    </ol>
                  </details>
                </div>
              </article>
            </div>
            <div v-if="!completed" class="mission-composer">
              <label :for="`mission-answer-${sceneIndex}`"
                >{{
                  currentAttempts.length
                    ? "再用自己的话，说清楚一点"
                    : "用英语回答当前问题"
                }}<span>{{ englishCount }} 个英文单词</span></label
              ><textarea
                :id="`mission-answer-${sceneIndex}`"
                v-model="draft"
                maxlength="2000"
                rows="2"
                placeholder="Write your idea here…"
                :disabled="busy"
                @keydown.ctrl.enter.prevent="submitTurn"
              />
              <div class="mission-composer-actions">
                <button
                  type="button"
                  class="mission-hint-button"
                  :disabled="busy || hintLevel >= 3"
                  @click="showHint"
                >
                  <AppIcon name="spark" />{{
                    hintLevel >= 3
                      ? "提示已全部显示"
                      : `看看${["词语线索", "句式支架", "示例表达"][hintLevel]}`
                  }}</button
                ><button
                  type="button"
                  class="primary-button"
                  :disabled="
                    busy ||
                    englishCount < 5 ||
                    mission.progress.turns.length >= 12
                  "
                  @click="submitTurn"
                >
                  {{
                    busy
                      ? "正在保存与反馈…"
                      : currentAttempts.length
                        ? "提交新表达"
                        : "发送表达"
                  }}<AppIcon name="arrow" /></button
                ><button
                  v-if="currentAttempts.length"
                  type="button"
                  class="mission-next"
                  :disabled="busy"
                  @click="nextScene"
                >
                  {{ sceneIndex === 2 ? "去复盘" : "下一个目标" }} →
                </button>
              </div>
            </div>
            <footer v-else class="mission-page-action">
              <span>这些是已保存的真实表达</span
              ><button class="primary-button" @click="stage = 3">
                回看收获<AppIcon name="arrow" />
              </button>
            </footer>
          </template>
          <template v-else>
            <header class="mission-page-heading">
              <span class="mission-eyebrow">04 / MAKE IT YOURS</span>
              <h3>
                {{
                  completed
                    ? "这一次，留下了自己的表达。"
                    : "把今天的一点进步，记下来。"
                }}
              </h3>
            </header>
            <div class="mission-page-scroll mission-recap">
              <div class="mission-recap-numbers">
                <span
                  ><strong
                    >{{ mission.progress.words.filter((w) => w.correct).length
                    }}<small>/3</small></strong
                  >词语答对</span
                ><span
                  ><strong>{{ progress.practiced }}<small>/3</small></strong
                  >目标已练习</span
                ><span
                  ><strong>{{ progress.retries }}</strong
                  >次重新表达</span
                ><span
                  ><strong>{{ progress.hintCount }}</strong
                  >条提示已查看</span
                >
              </div>
              <div
                class="mission-recap-line"
                v-for="(item, i) in lesson.steps"
                :key="i"
              >
                <span>0{{ i + 1 }} · {{ item.title }}</span>
                <p>{{ attemptsFor(i).at(-1)?.answer || "尚未提交表达" }}</p>
              </div>
              <label class="mission-reflection"
                >我的收获<textarea
                  v-model="reflection"
                  rows="3"
                  maxlength="1000"
                  :disabled="busy || completed"
                  placeholder="哪一句是你今天真正学会说的？下次想改进什么？（至少 8 个字符）"
                />
              </label>
              <p class="mission-evidence-note">
                保存原句、再次表达与提示使用记录，作为学习过程的证据。
              </p>
            </div>
            <footer class="mission-page-action">
              <span>{{
                completed
                  ? "已存入当前账号的成长证据"
                  : "完成代表提交了练习，不等同于已掌握"
              }}</span
              ><button
                type="button"
                class="primary-button"
                :disabled="busy || (!completed && reflection.trim().length < 8)"
                @click="completed ? $emit('evidence') : finish()"
              >
                {{
                  completed
                    ? "打开成长证据"
                    : busy
                      ? "正在保存…"
                      : "保存这次收获"
                }}<AppIcon name="arrow" />
              </button>
            </footer>
          </template>
        </section>
      </div>
    </template>
  </div>
</template>
<script setup>
import { computed, nextTick, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import AppIcon from "./AppIcon.vue";
import MissionSceneArt from "./MissionSceneArt.vue";
import { missionApi } from "@/api/modules";
import {
  missionStages,
  missionProgress,
  missionDraftKey,
} from "@/mission-progress";
import "@/styles/mission-studio.css";
const props = defineProps({ userId: { type: Number, required: true } });
const emit = defineEmits(["home", "evidence", "updated", "settings"]);
const route = useRoute(),
  router = useRouter();
const mission = ref(null),
  stage = ref(0),
  wordIndex = ref(0),
  sceneIndex = ref(0),
  loading = ref(false),
  busy = ref(false),
  error = ref(""),
  draft = ref(""),
  reflection = ref(""),
  conversation = ref(null);
const lesson = computed(() => mission.value?.lesson),
  progress = computed(() => missionProgress(mission.value)),
  completed = computed(() => mission.value?.status === "COMPLETED");
const unlocked = computed(() => (completed.value ? 3 : progress.value.current));
const currentWord = computed(() => lesson.value?.words[wordIndex.value]);
const wordResult = (index) =>
  mission.value?.progress.words.find((w) => w.index === index);
const currentWordResult = computed(() => wordResult(wordIndex.value));
const currentStep = computed(() => lesson.value?.steps[sceneIndex.value]);
const attemptsFor = (index) =>
  mission.value?.progress.turns.filter((t) => t.step === index) || [];
const currentAttempts = computed(() => attemptsFor(sceneIndex.value));
const hintLevel = computed(
  () => mission.value?.progress.hints[sceneIndex.value] || 0,
);
const englishCount = computed(
  () => (draft.value.match(/[A-Za-z]+(?:'[A-Za-z]+)?/g) || []).length,
);
const draftKey = computed(() =>
  missionDraftKey(props.userId, mission.value?.id, sceneIndex.value),
);
function readDraft(key) {
  try {
    return localStorage.getItem(key) || "";
  } catch {
    return "";
  }
}
function storeDraft(key, value) {
  try {
    value ? localStorage.setItem(key, value) : localStorage.removeItem(key);
  } catch {
    /* Private browsing may block storage; input remains in memory. */
  }
}
watch(
  draft,
  (value) => {
    if (mission.value) storeDraft(draftKey.value, value);
  },
  { flush: "sync" },
);
watch(draftKey, (key) => (draft.value = readDraft(key)));
watch(sceneIndex, async () => {
  await nextTick();
  if (conversation.value) conversation.value.scrollTop = 0;
});
let loadSequence = 0;
watch(() => [route.query.key, route.query.session], load, { immediate: true });
async function load() {
  const sequence = ++loadSequence;
  loading.value = true;
  error.value = "";
  try {
    const value = route.query.session
      ? await missionApi.get(route.query.session)
      : await missionApi.start(route.query.key || "water");
    if (sequence !== loadSequence) return;
    mission.value = value;
    stage.value = value.status === "COMPLETED" ? 3 : progress.value.current;
    wordIndex.value =
      value.progress.words.length < 3
        ? lesson.value.words.findIndex((w, i) => !wordResult(i))
        : 0;
    sceneIndex.value = Math.min(2, progress.value.practiced);
    reflection.value =
      value.progress.reflection ||
      readDraft(missionDraftKey(props.userId, value.id, "reflection"));
    draft.value = readDraft(draftKey.value);
    if (!route.query.session)
      router.replace({ path: "/mission", query: { session: value.id } });
  } catch (e) {
    if (sequence === loadSequence) error.value = e.message;
  } finally {
    if (sequence === loadSequence) loading.value = false;
  }
}
async function refresh() {
  if (busy.value) return;
  try {
    mission.value = await missionApi.get(mission.value.id);
    error.value = "";
    emit("updated");
  } catch (e) {
    error.value = e.message;
  }
}
async function mutate(action, payload = {}) {
  if (busy.value || completed.value) return false;
  const sessionId = mission.value.id,
    sequence = loadSequence;
  busy.value = true;
  error.value = "";
  try {
    const updated = await missionApi.update(sessionId, action, {
      version: mission.value.version,
      ...payload,
    });
    if (sequence !== loadSequence || mission.value.id !== sessionId)
      return false;
    mission.value = updated;
    emit("updated");
    return true;
  } catch (e) {
    if (sequence === loadSequence)
      error.value = e.message || "保存失败，输入内容已保留。";
    return false;
  } finally {
    busy.value = false;
  }
}
async function read() {
  if (progress.value.done[0] || (await mutate("read"))) stage.value = 1;
}
async function answerWord(answer) {
  await mutate("words", { index: wordIndex.value, answer });
}
function nextWord() {
  if (mission.value.progress.words.length === 3) stage.value = 2;
  else wordIndex.value = lesson.value.words.findIndex((w, i) => !wordResult(i));
}
async function showHint() {
  await mutate("hint", { step: sceneIndex.value });
  await scrollBottom();
}
async function submitTurn() {
  if (englishCount.value < 5 || busy.value) return;
  if (await mutate("turns", { step: sceneIndex.value, answer: draft.value })) {
    draft.value = "";
    await scrollBottom();
  }
}
function nextScene() {
  if (sceneIndex.value < 2) sceneIndex.value++;
  else stage.value = 3;
}
async function scrollBottom() {
  await nextTick();
  conversation.value?.scrollTo({
    top: conversation.value.scrollHeight,
    behavior: window.matchMedia("(prefers-reduced-motion: reduce)").matches
      ? "instant"
      : "smooth",
  });
}
watch(reflection, (value) => {
  if (mission.value)
    storeDraft(
      missionDraftKey(props.userId, mission.value.id, "reflection"),
      value,
    );
});
async function finish() {
  if (await mutate("finish", { reflection: reflection.value }))
    storeDraft(
      missionDraftKey(props.userId, mission.value.id, "reflection"),
      "",
    );
}
</script>
