<script setup>
import AppIcon from "./AppIcon.vue";

defineProps({ section: { type: String, required: true } });
const tabs = [
  { section: "dashboard", to: "/", label: "首页", icon: "dashboard" },
  { section: "practice", to: "/practice", label: "练习", icon: "vocabulary" },
  { section: "chat", to: "/chat", label: "对话", icon: "chat" },
  { section: "journal", to: "/report", label: "我的", icon: "person" },
];
</script>

<template>
  <nav class="mobile-nav" aria-label="主导航">
    <RouterLink v-for="tab in tabs" :key="tab.section" :to="tab.to"
      :aria-current="section === tab.section ? 'page' : undefined">
      <span class="mobile-nav-symbol"><AppIcon :name="tab.icon" /></span>{{ tab.label }}
    </RouterLink>
  </nav>
</template>
