<template>
  <div class="studio-book" aria-hidden="true">
    <svg viewBox="0 0 540 420" fill="none">
      <defs>
        <linearGradient :id="id + '-left'" x1="80" y1="110" x2="270" y2="320" gradientUnits="userSpaceOnUse">
          <stop stop-color="var(--studio-paper-top, #ffffff)" />
          <stop offset="1" stop-color="var(--studio-paper-fold, #e0efdf)" />
        </linearGradient>
        <linearGradient :id="id + '-right'" x1="280" y1="150" x2="470" y2="260" gradientUnits="userSpaceOnUse">
          <stop stop-color="var(--studio-paper-fold, #e0efdf)" />
          <stop offset=".18" stop-color="var(--studio-paper-top, #ffffff)" />
          <stop offset="1" stop-color="var(--studio-paper-edge, #eaf5fa)" />
        </linearGradient>
        <linearGradient :id="id + '-cover'" x1="90" y1="220" x2="460" y2="330" gradientUnits="userSpaceOnUse">
          <stop stop-color="#84c947" /><stop offset="1" stop-color="#459cb4" />
        </linearGradient>
        <filter :id="id + '-shadow'" x="0" y="0" width="540" height="420" filterUnits="userSpaceOnUse"><feGaussianBlur stdDeviation="13" /></filter>
      </defs>
      <ellipse cx="283" cy="340" rx="165" ry="19" fill="var(--studio-book-shadow, #437b8b)" opacity=".13" :filter="'url(#' + id + '-shadow)'" />
      <g class="studio-book-orbits" stroke="var(--studio-orbit, #9fc8d5)">
        <ellipse cx="275" cy="216" rx="232" ry="112" transform="rotate(-24 275 216)" stroke-opacity=".55" />
        <path d="M89 121C150 18 395 73 452 229" stroke-dasharray="2 8" stroke-opacity=".6" />
        <circle cx="66" cy="257" r="4" fill="var(--blue)" stroke="none" />
        <circle cx="463" cy="142" r="5" fill="var(--green)" stroke="none" />
      </g>
      <g class="studio-book-spread">
        <path d="M74 152C150 124 210 145 270 181C336 140 407 125 472 150L472 280C396 261 332 285 270 328C204 285 139 265 74 285Z" :fill="'url(#' + id + '-cover)'" />
        <path d="M79 143C150 116 212 137 270 174C335 133 404 118 467 141L467 271C395 253 329 278 270 319C207 277 142 256 79 277Z" fill="var(--studio-paper-under, #cfe3d7)" />
        <path d="M82 135C153 110 212 131 270 166C335 126 402 111 464 134L464 264C393 245 329 271 270 312C207 270 144 250 82 269Z" fill="var(--studio-paper-edge, #eaf5fa)" />
        <path d="M84 121C155 99 214 122 270 158V304C210 266 149 244 84 256Z" :fill="'url(#' + id + '-left)'" stroke="var(--studio-paper-stroke, #d6e7e4)" />
        <path d="M270 158C333 119 399 105 462 123V254C393 239 330 265 270 304Z" :fill="'url(#' + id + '-right)'" stroke="var(--studio-paper-stroke, #d6e7e4)" />
        <path d="M270 161V299" stroke="var(--studio-paper-seam, #a9c9b5)" stroke-opacity=".65" stroke-width="2" />
        <text x="113" y="222" transform="rotate(13 113 222)" fill="var(--studio-letter-ink, #3b7b40)" font-family="Georgia, serif" font-style="italic" font-size="80" letter-spacing="-7">Aa</text>
        <path d="M316 166C350 150 391 141 421 145M316 184C350 168 391 159 421 163M316 202C350 186 379 180 400 181" stroke="var(--studio-print, #a6c8d4)" stroke-width="4" stroke-linecap="round" />
        <path d="M316 237C341 224 364 217 386 215" stroke="var(--studio-letter-ink, #3b7b40)" stroke-width="5" stroke-linecap="round" opacity=".65" />
        <path d="m397 227 8 7 15-20" stroke="var(--studio-letter-ink, #3b7b40)" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" />
        <path d="M280 295L280 329L294 315L308 319L308 279" fill="var(--blue)" opacity=".85" />
      </g>
      <path d="m425 72 3 10 10 3-10 3-3 10-3-10-10-3 10-3Z" fill="var(--studio-spark, #78b78a)" />
      <path d="m106 310 2 6 6 2-6 2-2 6-2-6-6-2 6-2Z" fill="var(--blue)" opacity=".65" />
    </svg>
  </div>
</template>

<script setup>
import { useId } from "vue";
const id = "studio-book-" + useId();
</script>

<style scoped>
.studio-book { position: absolute; inset: 0; display: grid; place-items: center; pointer-events: none; }
.studio-book > svg { width: 112%; max-width: 560px; height: auto; max-height: 100%; overflow: visible; }
.studio-book-spread { transform-origin: 270px 220px; filter: drop-shadow(0 15px 12px #254e4d14); animation: studio-page-float 9s ease-in-out infinite; }
.studio-book-orbits { transform-origin: 275px 216px; animation: studio-orbit-breathe 14s ease-in-out infinite; }
@keyframes studio-page-float {
  0%, 100% { transform: translateY(1px) rotate(-8deg); }
  50% { transform: translateY(-10px) rotate(-4deg); }
}
@keyframes studio-orbit-breathe {
  0%, 100% { transform: rotate(-3deg) scale(.97); }
  50% { transform: rotate(3deg) scale(1.03); }
}
@media (prefers-reduced-motion: reduce) {
  .studio-book-spread, .studio-book-orbits { animation: none; }
  .studio-book-spread { transform: rotate(-6deg); }
}
</style>
