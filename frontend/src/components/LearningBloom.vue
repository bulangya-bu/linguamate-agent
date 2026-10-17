<template>
  <svg
    class="learning-bloom"
    viewBox="0 0 400 400"
    fill="none"
    aria-hidden="true"
  >
    <circle
      cx="200"
      cy="200"
      r="164"
      stroke="currentColor"
      stroke-opacity=".13"
    />
    <circle
      cx="200"
      cy="200"
      r="122"
      stroke="currentColor"
      stroke-opacity=".1"
      stroke-dasharray="3 9"
    />
    <g class="bloom-petals">
      <path
        v-for="angle in [0, 60, 120, 180, 240, 300]"
        :key="angle"
        :transform="`rotate(${angle} 200 200)`"
        d="M200 200C120 169 111 88 158 68c48-20 76 58 42 132Z"
        fill="currentColor"
        fill-opacity=".22"
        stroke="currentColor"
        stroke-opacity=".25"
      />
    </g>
    <circle cx="200" cy="200" r="39" fill="currentColor" />
    <path
      d="m185 199 10 11 20-23"
      stroke="#f4f2e9"
      stroke-width="4"
      stroke-linecap="round"
      stroke-linejoin="round"
    />
    <circle cx="338" cy="112" r="8" fill="currentColor" />
    <path
      d="M63 270v16m-8-8h16M294 323v12m-6-6h12"
      stroke="currentColor"
      stroke-width="2"
    />
  </svg>
</template>
