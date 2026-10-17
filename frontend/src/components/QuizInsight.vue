<template>
  <aside ref="root" class="quiz-insight" aria-label="单词深度解析">
    <header class="insight-heading">
      <div>
        <AppIcon name="spark" />
        <h2>深度解析</h2>
      </div>
      <div class="insight-actions">
        <span
          v-if="detail?.status === 'ready'"
          class="insight-source"
          :title="source"
          >{{ source }}</span
        >
        <button
          class="insight-close"
          type="button"
          aria-label="收起深度解析"
          @click="emit('close')"
        >
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.8"
            aria-hidden="true"
          >
            <path d="m6 6 12 12M18 6 6 18" />
          </svg>
        </button>
      </div>
    </header>
    <template v-if="detail?.status === 'ready'">
      <section
        ref="content"
        class="insight-content"
        tabindex="0"
        aria-label="完整解析内容"
      >
        <div class="insight-word">{{ word.word }}</div>
        <section v-if="detail.data.usageNotes?.length" class="insight-section">
          <h3>用法</h3>
          <p
            v-for="(note, i) in detail.data.usageNotes"
            :key="i"
            class="insight-note"
          >
            {{ note }}
          </p>
        </section>
        <section
          v-if="detail.data.collocations?.length"
          class="insight-section"
        >
          <h3>搭配</h3>
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
        </section>
        <section v-if="detail.data.examples?.length" class="insight-section">
          <h3>例句</h3>
          <article
            v-for="(example, i) in detail.data.examples"
            :key="i"
            class="insight-entry"
          >
            <p class="insight-english">{{ example.en }}</p>
            <p v-if="example.cn" class="insight-chinese">{{ example.cn }}</p>
          </article>
        </section>
        <section v-if="derivedForms.length" class="insight-section">
          <h3>词形变化</h3>
          <article
            v-for="(variant, i) in derivedForms"
            :key="i"
            class="insight-entry insight-variant"
          >
            <p>
              <strong class="insight-term">{{ variant.form }}</strong
              ><span class="insight-pos">{{ variant.type }}</span>
            </p>
            <p v-if="variant.meaningCn" class="insight-chinese">
              {{ variant.meaningCn }}
            </p>
          </article>
        </section>
        <p v-if="!hasContent" class="insight-note">当前暂无补充内容。</p>
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
        <p>正在整理用法、搭配与例句</p>
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
        <h3>探索 {{ word.word }}</h3>
        <button
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
const props = defineProps({
  word: { type: Object, required: true },
  detail: { type: Object, default: null },
  source: { type: String, default: "" },
});
const emit = defineEmits(["request", "close"]);
const root = ref(null);
const content = ref(null);
const isBaseForm = (variant) =>
  variant.form?.trim().toLowerCase() === props.word.word.trim().toLowerCase();
const derivedForms = computed(
  () =>
    props.detail?.data?.variants?.filter((variant) => !isBaseForm(variant)) ||
    [],
);
let focusOnReady = false;
function request(force) {
  focusOnReady = root.value?.contains(document.activeElement);
  emit("request", force);
}
const hasContent = computed(
  () =>
    derivedForms.value.length ||
    ["usageNotes", "examples", "collocations"].some(
      (key) => props.detail?.data?.[key]?.length,
    ),
);
watch(
  () => props.detail?.status,
  async (status) => {
    if (status === "ready" && focusOnReady) {
      await nextTick();
      content.value?.focus({ preventScroll: true });
      focusOnReady = false;
    }
  },
);
</script>
