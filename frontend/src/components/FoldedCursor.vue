<script setup>
import { onBeforeUnmount, onMounted, useTemplateRef } from "vue";
import { createCursorTrail } from "./cursorTrail";

const canvas = useTemplateRef("canvas");
let stopTrail;

onMounted(() => {
  stopTrail = createCursorTrail(canvas.value);
});

onBeforeUnmount(() => stopTrail?.());
</script>

<template>
  <Teleport to="body">
    <canvas ref="canvas" class="folded-cursor-trail" aria-hidden="true" />
  </Teleport>
</template>

<style scoped>
.folded-cursor-trail {
  display: none;
  position: fixed;
  inset: 0;
  z-index: 5000;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

@media (any-hover: hover) and (any-pointer: fine) and (prefers-reduced-motion: no-preference) {
  .folded-cursor-trail {
    display: block;
  }
}
</style>
