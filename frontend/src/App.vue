<script setup>
import { useRoute } from "vue-router";
import AmbientBackground from "@/components/AmbientBackground.vue";

const route = useRoute();
</script>

<template>
  <AmbientBackground v-if="route.name !== 'login' && route.name !== 'register'" />
  <RouterView />
</template>

<style>
#app {
  isolation: isolate;
}
</style>
