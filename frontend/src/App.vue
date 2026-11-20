<script setup>
import { useRoute } from "vue-router";
import { usePhoneLayout } from "@/composables/usePhoneLayout";
import AmbientBackground from "@/components/AmbientBackground.vue";
import FoldedCursor from "@/components/FoldedCursor.vue";

const route = useRoute();
const isPhone = usePhoneLayout();
</script>

<template>
  <AmbientBackground v-if="route.name !== 'login' && route.name !== 'register' && (!isPhone || route.name === 'dashboard')" :quiet="isPhone" />
  <FoldedCursor />
  <RouterView />
</template>

<style>
#app {
  isolation: isolate;
}
</style>
