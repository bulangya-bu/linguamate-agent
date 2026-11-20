<script setup>
import AdminSketchIcon from "@/components/AdminSketchIcon.vue";
defineProps({
  title: { type: String, required: true },
  fields: { type: Array, required: true },
});
defineEmits(["back"]);
</script>

<template>
  <section class="panel admin-mobile-details" :aria-label="title">
    <button class="admin-mobile-back" type="button" @click="$emit('back')">
      <AdminSketchIcon name="arrow" />列表
    </button>
    <h2>{{ title }}</h2>
    <dl>
      <template v-for="[label, value] in fields" :key="label">
        <div v-if="value != null && value !== ''"><dt>{{ label }}</dt><dd>{{ value }}</dd></div>
      </template>
    </dl>
    <footer v-if="$slots.default" class="admin-form-actions"><slot /></footer>
  </section>
</template>
