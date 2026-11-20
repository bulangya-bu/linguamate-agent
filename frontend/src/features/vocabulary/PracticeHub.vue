<script setup>
import AppIcon from "@/components/AppIcon.vue";
defineProps({ due: Number });
const activities = [
  { to: "/news", label: "阅读空间", icon: "news" },
  { to: "/writing", label: "写作工坊", icon: "writing" },
  { to: "/wordbank", label: "我的词库", icon: "wordbank" },
];
</script>

<template>
  <div class="practice-hub">
    <div class="practice-pair">
      <RouterLink class="practice-tile" to="/vocabulary?scope=new">
        <AppIcon name="vocabulary" /><strong>学新词</strong><AppIcon class="tile-arrow" name="arrow" />
      </RouterLink>
      <RouterLink class="practice-tile tile-review" to="/review">
        <AppIcon name="review" /><strong>温故知新</strong>
        <small v-if="due > 0">{{ due }} 词待复习</small><AppIcon class="tile-arrow" name="arrow" />
      </RouterLink>
    </div>
    <div class="practice-shelf">
      <RouterLink v-for="item in activities" :key="item.to" :to="item.to">
        <span class="practice-mark"><AppIcon :name="item.icon" /></span>
        <strong>{{ item.label }}</strong><AppIcon class="shelf-arrow" name="arrow" />
      </RouterLink>
    </div>
  </div>
</template>
