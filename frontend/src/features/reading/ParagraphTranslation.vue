<script setup>
import { shallowRef } from "vue";
import { learningApi } from "@/api/modules";

const props = defineProps({ text: { type: String, required: true } });
const expanded = shallowRef(false);
const loading = shallowRef(false);
const translation = shallowRef("");
const error = shallowRef("");

async function toggle() {
  if (expanded.value && !error.value) {
    expanded.value = false;
    return;
  }
  expanded.value = true;
  if (translation.value || loading.value) return;
  loading.value = true;
  error.value = "";
  try {
    const result = await learningApi.translateArticle({ content: props.text });
    translation.value = result?.contentCn?.trim() || "";
    if (!translation.value) error.value = "暂未取得译文，请检查模型连接。";
  } catch (reason) {
    error.value = reason.message || "译文暂时无法加载。";
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="reader-paragraph-translation">
    <button type="button" :aria-expanded="expanded" @click="toggle">{{ error ? "重试翻译" : expanded ? "收起译文" : "译文" }}</button>
    <p v-if="expanded" aria-live="polite">{{ loading ? "翻译中…" : error || translation }}</p>
  </div>
</template>
