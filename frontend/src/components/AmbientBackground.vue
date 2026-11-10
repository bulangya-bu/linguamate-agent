<script setup>
import { onBeforeUnmount, onMounted, useTemplateRef, watch } from "vue";
import { useArtMotion } from "@/composables/useArtMotion";
import { createPaperField } from "./ambient/paperField";

const props = defineProps({ contained: Boolean });
const layer = useTemplateRef("layer");
const canvas = useTemplateRef("canvas");
const { playing, reducedMotion } = useArtMotion(layer);
let field;

onMounted(() => {
  field = createPaperField(canvas.value, { contained: props.contained });
  field.setMotion(playing.value, reducedMotion.value);
});

watch([playing, reducedMotion], ([active, reduced]) => {
  field?.setMotion(active, reduced);
});

onBeforeUnmount(() => field?.destroy());
</script>

<template>
  <div
    ref="layer"
    class="ambient-background"
    :class="{ 'is-contained': contained, 'is-playing': playing }"
    aria-hidden="true"
  >
    <canvas ref="canvas" class="paper-field" />
  </div>
</template>

<style scoped>
.ambient-background {
  --paper-blue: rgb(62 146 180);
  --paper-green: rgb(119 168 82);
  --paper-apricot: rgb(207 153 88);
  position: fixed;
  inset: 0;
  z-index: -1;
  overflow: hidden;
  pointer-events: none;
}

:global(:root[data-theme="dark"] .ambient-background) {
  --paper-blue: rgb(111 185 211);
  --paper-green: rgb(157 194 122);
  --paper-apricot: rgb(223 181 128);
}

.ambient-background.is-contained {
  position: absolute;
  z-index: 1;
}

.paper-field {
  display: block;
  width: 100%;
  height: 100%;
}
</style>
