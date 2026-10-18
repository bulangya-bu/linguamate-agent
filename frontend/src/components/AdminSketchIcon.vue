<template>
  <svg
    class="admin-sketch-icon"
    viewBox="0 0 28 28"
    fill="none"
    aria-hidden="true"
  >
    <SketchShape
      v-for="(path, index) in paths[name] || paths.overview"
      :key="path"
      kind="path"
      :args="[path]"
      :seed="31 + index"
      :options="{
        stroke: 'currentColor',
        strokeWidth: 1.3,
        roughness: 0.65,
        bowing: 0.35,
      }"
    />
  </svg>
</template>

<script setup>
import SketchShape from "./SketchShape.vue";
defineProps({ name: { type: String, default: "overview" } });
const paths = {
  overview: ["M5 4 L5 23 L24 23", "M10 18 L10 12 M15 18 L15 7 M21 18 L21 10"],
  users: [
    "M11 4 C5 4 5 12 11 12 C17 12 17 4 11 4 Z",
    "M3 24 L3 21 C3 14 19 14 19 21 L19 24",
    "M21 5 C27 6 27 12 22 13 M23 17 C27 18 27 20 27 24",
  ],
  words: [
    "M3 6 Q9 4 14 8 Q19 4 25 6 L25 24 Q19 22 14 26 Q9 22 3 24 Z",
    "M14 8 L14 26 M7 10 L10 11 M18 11 L21 10",
  ],
  knowledge: [
    "M6 3 L22 3 Q24 3 24 6 L24 25 L7 25 Q3 25 3 21 L3 6 Q3 3 6 3 Z",
    "M4 20 L24 20 M9 8 L19 8 M9 13 L17 13",
  ],
  news: [
    "M7 4 L25 4 L25 24 L5 24 Q2 24 2 21 L2 9 L7 9 Z",
    "M7 9 L7 22 M11 9 L20 9 M11 13 L20 13 M11 18 L14 18 M18 18 L21 18",
  ],
  ai: [
    "M8 7 L21 7 L21 21 L8 21 Z",
    "M11 11 L17 11 L17 17 L11 17 Z M14 2 L14 7 M14 21 L14 26 M2 14 L8 14 M21 14 L26 14 M5 5 L8 8 M21 21 L24 24 M5 24 L8 21 M21 8 L24 5",
  ],
  arrow: ["M4 14 L24 14 M18 8 L24 14 L18 20"],
  refresh: ["M23 10 C20 1 6 2 4 12 C2 24 19 28 24 18", "M24 3 L24 11 L16 11"],
  search: ["M12 3 C0 3 1 20 12 20 C24 20 24 3 12 3 Z", "M18 18 L26 26"],
  plus: ["M14 5 L14 23 M5 14 L23 14"],
  review: [
    "M5 9 C12 0 25 6 25 15 C25 27 6 28 3 18",
    "M3 3 L3 11 L11 11 M14 8 L14 15 L19 18",
  ],
  check: ["M5 15 L11 21 L24 6"],
  writing: [
    "M4 24 L6 15 L20 2 L26 8 L12 22 Z",
    "M17 5 L23 11 M4 24 L12 22 M17 25 L26 25",
  ],
  chat: [
    "M5 4 L23 4 Q26 4 26 8 L26 18 Q26 22 22 22 L10 22 L3 27 L3 8 Q3 4 5 4 Z",
    "M8 10 L20 10 M8 16 L16 16",
  ],
  shield: [
    "M14 2 Q20 6 25 6 L24 16 Q22 23 14 27 Q5 23 4 16 L3 6 Q8 6 14 2 Z",
    "M8 14 L12 18 L20 10",
  ],
};
</script>
