<template>
  <RouterLink
    class="brand brand--bilingual"
    :class="{ 'brand--auth': variant === 'auth' }"
    :to="to"
    :aria-label="BRAND_FULL_NAME"
    :title="BRAND_FULL_NAME"
  >
    <img
      class="brand-mark"
      src="/assets/linguamate-icon.svg?v=bright"
      alt=""
    />
    <div class="brand-copy">
      <strong class="brand-name">{{ BRAND_NAME }}</strong>
      <span class="brand-english">（{{ BRAND_ENGLISH_NAME }}）</span>
    </div>
  </RouterLink>
</template>

<script setup>
import { BRAND_NAME, BRAND_ENGLISH_NAME, BRAND_FULL_NAME } from "@/brand";

defineProps({
  to: { type: String, default: "/" },
  variant: { type: String, default: "workspace" },
});
</script>

<style scoped>
.brand.brand--bilingual .brand-copy {
  display: grid;
  gap: 3px;
  min-width: 0;
}
.brand.brand--bilingual .brand-copy > .brand-name {
  display: block;
  color: var(--ink);
  letter-spacing: 0.8px;
  line-height: 1.2;
  white-space: nowrap;
}
.brand.brand--bilingual .brand-copy > .brand-english {
  display: block;
  margin-top: 0;
  color: var(--muted);
  font-weight: 500;
  letter-spacing: 0.5px;
  line-height: 1.3;
  white-space: nowrap;
}
@media (min-width: 981px) {
  .brand.brand--bilingual .brand-copy > .brand-name {
    font-size: 23px;
  }
  .brand.brand--bilingual .brand-copy > .brand-english {
    font-size: 12px;
  }
  .brand.brand--bilingual.brand--auth .brand-mark {
    width: 44px;
    height: 44px;
  }
  .brand.brand--bilingual.brand--auth .brand-copy > .brand-name {
    font-size: 30px;
    letter-spacing: 1.5px;
  }
  .brand.brand--bilingual.brand--auth .brand-copy > .brand-english {
    font-size: 14px;
  }
}
</style>
