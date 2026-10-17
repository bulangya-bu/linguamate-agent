<template>
  <svg
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="1.65"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
  >
    <path
      v-for="(path, index) in paths[name] || paths.spark"
      :key="index"
      :d="path"
    />
  </svg>
</template>

<script setup>
defineProps({ name: { type: String, default: "spark" } });
const paths = {
  dashboard: ["M3 10 12 3l9 7v10a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1Z"],
  chat: [
    "M21 11.5a8.5 8.5 0 0 1-8.5 8.5H4l-2 2V11.5A8.5 8.5 0 0 1 10.5 3h2a8.5 8.5 0 0 1 8.5 8.5Z",
    "M7 10h9M7 14h5",
  ],
  vocabulary: [
    "M3 5c4-1 6 0 9 2 3-2 5-3 9-2v15c-4-1-6 0-9 2-3-2-5-3-9-2Z",
    "M12 7v15M6 9h3M15 9h3",
  ],
  review: ["M3 10a9 9 0 1 1 1 7M3 4v6h6", "M12 7v5l3 2"],
  writing: ["m14 4 6 6M4 20l5-1L21 7a2 2 0 0 0-6-6L3 13 2 22ZM12 21h9"],
  news: [
    "M5 3h16v17a1 1 0 0 1-1 1H4a2 2 0 0 1-2-2V8h3Z",
    "M5 8v11M9 7h8M9 11h8M9 15h3M15 15h2",
  ],
  wordbank: ["m12 2 10 5-10 5L2 7ZM2 12l10 5 10-5M2 17l10 5 10-5"],
  settings: [
    "M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8Z",
    "m9 3-1 3-3 1-2 3 2 2-1 3 2 3 3-1 2 3h3l1-3 3-1 2-3-2-2 1-3-2-3-3 1-2-3Z",
  ],
  arrow: ["M4 12h16M14 6l6 6-6 6"],
  moon: ["M20.6 13.1A8.7 8.7 0 0 1 10.9 3.4 8.8 8.8 0 1 0 20.6 13.1Z"],
  sun: ["M16 12a4 4 0 1 1-8 0 4 4 0 0 1 8 0Z", "M12 2v2M12 20v2M2 12h2M20 12h2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"],
  check: ["m5 12 4 4L19 6"],
  star: [
    "m12 3 2.8 5.7 6.3.9-4.55 4.43 1.08 6.27L12 17.34l-5.63 2.96 1.08-6.27L2.9 9.6l6.3-.9Z",
  ],
  spark: ["m12 2 2.5 7.5L22 12l-7.5 2.5L12 22l-2.5-7.5L2 12l7.5-2.5Z"],
  logout: ["M9 3H4v18h5M9 12h13m-5-5 5 5-5 5"],
  users: [
    "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M9 3a4 4 0 1 0 0 8 4 4 0 0 0 0-8ZM17 4a4 4 0 0 1 0 7M22 21v-2a4 4 0 0 0-3-4",
  ],
  overview: ["M3 3v18h18M7 16v-5M12 16V7M17 16v-8"],
  knowledge: [
    "M4 3h13a3 3 0 0 1 3 3v15H6a3 3 0 0 1-3-3V5a2 2 0 0 1 2-2ZM3 17h17M8 7h7M8 11h5",
  ],
  words: ["M3 5c4-1 6 0 9 2 3-2 5-3 9-2v15c-4-1-6 0-9 2-3-2-5-3-9-2ZM12 7v15"],
  ai: [
    "M12 3v3M12 18v3M3 12h3M18 12h3M5.5 5.5l2 2M16.5 16.5l2 2M5.5 18.5l2-2M16.5 7.5l2-2M8 8h8v8H8Z",
  ],
};
</script>
