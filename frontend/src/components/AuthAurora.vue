<template>
  <div class="auth-aurora" aria-hidden="true">
    <div class="auth-aurora-depth">
      <span
        class="auth-aurora-veil auth-aurora-veil--blue auth-scene-loop"
      ></span>
      <span
        class="auth-aurora-veil auth-aurora-veil--green auth-scene-loop"
      ></span>
      <svg
        class="auth-aurora-ribbons"
        viewBox="0 0 900 1000"
        fill="none"
        preserveAspectRatio="none"
      >
        <defs>
          <linearGradient
            id="auth-ribbon-tint"
            x1="100"
            y1="900"
            x2="850"
            y2="460"
            gradientUnits="userSpaceOnUse"
          >
            <stop stop-color="#81c7df" stop-opacity="0" />
            <stop offset=".32" stop-color="#7fc7df" stop-opacity=".25" />
            <stop offset=".7" stop-color="#a4d486" stop-opacity=".3" />
            <stop offset="1" stop-color="#a3d5e4" stop-opacity="0" />
          </linearGradient>
          <linearGradient
            id="auth-ribbon-glint"
            x1="120"
            y1="900"
            x2="900"
            y2="420"
            gradientUnits="userSpaceOnUse"
          >
            <stop stop-color="#72b6d6" stop-opacity="0" />
            <stop offset=".5" stop-color="#91c77c" stop-opacity=".8" />
            <stop offset=".8" stop-color="#6ebddc" stop-opacity=".8" />
            <stop offset="1" stop-color="#6ebddc" stop-opacity="0" />
          </linearGradient>
        </defs>
        <g class="auth-ribbon-sway auth-scene-loop">
          <path
            class="auth-ribbon-body"
            d="M-150 870C120 920 280 765 414 640S672 390 1030 493"
            stroke="url(#auth-ribbon-tint)"
            stroke-width="66"
          />
          <path
            class="auth-ribbon-edge"
            d="M-150 839C120 889 280 734 414 609S672 359 1030 462"
            stroke="url(#auth-ribbon-tint)"
          />
          <path
            class="auth-ribbon-edge"
            d="M-150 901C120 951 280 796 414 671S672 421 1030 524"
            stroke="url(#auth-ribbon-tint)"
          />
          <path
            class="auth-ribbon-flow auth-scene-loop"
            pathLength="1000"
            d="M-150 839C120 889 280 734 414 609S672 359 1030 462"
            stroke="url(#auth-ribbon-glint)"
            stroke-width="1.5"
            stroke-linecap="round"
          />
          <path
            class="auth-ribbon-flow auth-ribbon-flow--late auth-scene-loop"
            pathLength="1000"
            d="M-150 901C120 951 280 796 414 671S672 421 1030 524"
            stroke="url(#auth-ribbon-glint)"
            stroke-width="1.2"
            stroke-linecap="round"
          />
        </g>
        <path
          class="auth-ribbon-thread"
          d="M250 1060C130 850 400 700 710 760S1007 563 851 285"
          stroke="url(#auth-ribbon-tint)"
          stroke-width="1"
        />
        <path
          class="auth-ribbon-flow auth-ribbon-flow--thread auth-scene-loop"
          pathLength="1000"
          d="M250 1060C130 850 400 700 710 760S1007 563 851 285"
          stroke="url(#auth-ribbon-glint)"
          stroke-width="1.6"
          stroke-linecap="round"
        />
      </svg>
      <span
        v-for="(point, index) in points"
        :key="index"
        class="auth-firefly auth-scene-loop"
        :class="{ 'is-star': index % 3 === 0 }"
        :style="{
          left: point[0] + '%',
          top: point[1] + '%',
          '--fly-delay': index * -2.7 + 's',
          '--fly-time': 7 + (index % 4) * 2 + 's',
        }"
      ></span>
    </div>
  </div>
</template>

<script setup>
const points = [
  [13, 46],
  [39, 61],
  [83, 35],
  [63, 19],
  [21, 87],
  [70, 93],
  [87, 64],
  [38, 79],
];
</script>
