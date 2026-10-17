<template>
  <aside ref="root" class="quiz-insight" aria-label="单词深度解析">
    <header class="insight-heading">
      <div>
        <AppIcon name="spark" />
        <h2>深度解析</h2>
      </div>
      <span
        v-if="detail?.status === 'ready'"
        class="insight-source"
        :title="source"
        >{{ source }}</span
      >
    </header>
    <template v-if="detail?.status === 'ready'">
      <div class="insight-word">{{ word.word }}</div>
      <div
        class="insight-tabs"
        role="tablist"
        aria-label="解析内容"
        @keydown="moveTab"
      >
        <button
          v-for="tab in tabs"
          :id="'insight-tab-' + tab.id"
          :key="tab.id"
          role="tab"
          type="button"
          :aria-selected="active === tab.id"
          :tabindex="active === tab.id ? 0 : -1"
          aria-controls="insight-content"
          @click="active = tab.id"
        >
          {{ tab.label }}
        </button>
      </div>
      <section
        id="insight-content"
        class="insight-content"
        role="tabpanel"
        :aria-labelledby="tabs.length ? 'insight-tab-' + active : undefined"
        :aria-label="tabs.length ? undefined : '补充解析'"
      >
        <PagedReading :key="active" label="解析" always-show-pagination>
          <template v-if="active === 'usage'">
            <p
              v-for="(note, i) in detail.data.usageNotes"
              :key="i"
              class="insight-note"
            >
              {{ note }}
            </p>
          </template>
          <template v-else-if="active === 'examples'">
            <article
              v-for="(example, i) in detail.data.examples"
              :key="i"
              class="insight-entry"
            >
              <p class="insight-english">{{ example.en }}</p>
              <p v-if="example.cn" class="insight-chinese">{{ example.cn }}</p>
            </article>
          </template>
          <template v-else-if="active === 'variants'">
            <article
              v-for="(variant, i) in detail.data.variants"
              :key="i"
              class="insight-entry"
            >
              <p>
                <strong class="insight-term">{{ variant.form }}</strong
                ><span class="insight-pos">{{ variant.type }}</span>
              </p>
              <p v-if="variant.meaningCn" class="insight-chinese">
                {{ variant.meaningCn }}
              </p>
            </article>
          </template>
          <template v-else-if="active === 'collocations'">
            <article
              v-for="(item, i) in detail.data.collocations"
              :key="i"
              class="insight-entry"
            >
              <div class="insight-collocation-heading">
                <p class="insight-term">{{ item.phrase }}</p>
                <p v-if="item.meaningCn" class="insight-chinese">
                  {{ item.meaningCn }}
                </p>
              </div>
              <p v-if="item.exampleEn" class="insight-english">
                {{ item.exampleEn }}
              </p>
            </article>
          </template>
          <p v-else class="insight-note">当前暂无补充内容。</p>
        </PagedReading>
      </section>
    </template>
    <div
      v-else
      class="insight-placeholder"
      :aria-busy="detail?.status === 'loading'"
    >
      <template v-if="detail?.status === 'loading'">
        <span class="quiz-ai-spinner" aria-hidden="true"></span>
        <h3>正在解析 {{ word.word }}</h3>
        <p>正在整理用法、例句与词形</p>
      </template>
      <template v-else-if="detail?.status === 'error'">
        <AppIcon name="ai" />
        <h3>解析暂时不可用</h3>
        <p>{{ detail.message || "请稍后再试" }}</p>
        <button class="settings-secondary" type="button" @click="request(true)">
          重新生成
        </button>
      </template>
      <template v-else>
        <span class="insight-empty-mark"><AppIcon name="vocabulary" /></span>
        <h3>
          {{ answered ? "把这个词，理解得更深。" : "先试一题，再探索更多。" }}
        </h3>
        <p>用法、例句、词形与搭配</p>
        <button
          v-if="answered"
          class="settings-secondary"
          type="button"
          @click="request(false)"
        >
          查看深度解析<AppIcon name="arrow" />
        </button>
      </template>
    </div>
  </aside>
</template>

<script setup>
import { computed, nextTick, ref, watch } from "vue";
import AppIcon from "./AppIcon.vue";
import PagedReading from "./PagedReading.vue";
const props = defineProps({
  word: { type: Object, required: true },
  detail: { type: Object, default: null },
  source: { type: String, default: "" },
  answered: Boolean,
});
const emit = defineEmits(["request"]);
const root = ref(null);
let focusOnReady = false;
function request(force) {
  focusOnReady = root.value?.contains(document.activeElement);
  emit("request", force);
}
const active = ref("usage");
const tabs = computed(() =>
  [
    { id: "usage", label: "用法", key: "usageNotes" },
    { id: "examples", label: "例句", key: "examples" },
    { id: "variants", label: "词形", key: "variants" },
    { id: "collocations", label: "搭配", key: "collocations" },
  ].filter((tab) => props.detail?.data?.[tab.key]?.length),
);
watch(
  tabs,
  (value) => {
    if (!value.some((tab) => tab.id === active.value))
      active.value = value[0]?.id || "empty";
  },
  { immediate: true },
);
watch(
  () => props.detail?.status,
  async (status) => {
    if (status === "ready" && focusOnReady) {
      await nextTick();
      document.getElementById("insight-tab-" + active.value)?.focus();
      focusOnReady = false;
    }
  },
);
async function moveTab(event) {
  if (!["ArrowLeft", "ArrowRight", "Home", "End"].includes(event.key)) return;
  event.preventDefault();
  if (!tabs.value.length) return;
  const index = tabs.value.findIndex((tab) => tab.id === active.value);
  const next =
    event.key === "Home"
      ? 0
      : event.key === "End"
        ? tabs.value.length - 1
        : (index + (event.key === "ArrowRight" ? 1 : -1) + tabs.value.length) %
          tabs.value.length;
  active.value = tabs.value[next].id;
  await nextTick();
  document.getElementById("insight-tab-" + active.value)?.focus();
}
</script>
