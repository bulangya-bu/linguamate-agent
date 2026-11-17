<script setup>
import { useRoute } from "vue-router";
import AmbientBackground from "@/components/AmbientBackground.vue";
import FoldedCursor from "@/components/FoldedCursor.vue";

const route = useRoute();
</script>

<template>
  <AmbientBackground v-if="route.name !== 'login' && route.name !== 'register'" />
  <FoldedCursor />
  <RouterView />
</template>

<style>
#app {
  isolation: isolate;
}
</style>
