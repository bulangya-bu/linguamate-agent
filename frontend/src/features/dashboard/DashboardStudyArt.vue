<script setup>
import { useTemplateRef } from "vue";
import { useArtMotion } from "@/composables/useArtMotion";

const art = useTemplateRef("art");
const frontReply = useTemplateRef("frontReply");
const backReply = useTemplateRef("backReply");
const sparks = useTemplateRef("sparks");
const { playing, reducedMotion } = useArtMotion(art);

function playReply() {
  if (reducedMotion.value) return;

  const effects = [
    [frontReply.value, [
      { transform: "translateY(5px) scale(.96)" },
      { transform: "translateY(-9px) rotate(-4deg) scale(1.04)", offset: .38 },
      { transform: "translateY(2px) rotate(1deg)", offset: .74 },
      { transform: "none" },
    ]],
    [backReply.value, [
      { transform: "translateY(3px) scale(.97)" },
      { transform: "translate(13px, -6px) rotate(7deg)", offset: .4 },
      { transform: "translate(-2px, 1px) rotate(-1deg)", offset: .76 },
      { transform: "none" },
    ]],
    [sparks.value, [
      { opacity: .78, transform: "scale(.96)" },
      { opacity: 1, transform: "scale(1.15)", offset: .42 },
      { opacity: 1, transform: "none" },
    ]],
  ];

  for (const [element, keyframes] of effects) {
    for (const animation of element.getAnimations()) animation.cancel();
    element.animate(keyframes, { duration: 460, easing: "cubic-bezier(.22,.7,.25,1)" });
  }
}
</script>

<template>
  <button class="study-art-trigger" type="button" aria-label="轻触卡片" @click="playReply">
    <svg
      ref="art"
      class="study-art"
      :class="{ 'is-playing': playing }"
      viewBox="0 0 396 330"
      fill="none"
      aria-hidden="true"
      focusable="false"
    >
      <path
        d="M28 249C-2 210 67 135 168 94c101-41 202-42 226-2 25 42-47 109-148 151C145 285 55 286 28 249Z"
        stroke="#a6ded0"
        stroke-opacity=".27"
      />
      <path
        class="study-orbit"
        d="M15 229C-3 183 75 125 177 103c102-23 195-9 212 37 18 46-62 103-164 125C123 288 33 275 15 229Z"
        stroke="#c6eeac"
        stroke-opacity=".58"
        stroke-dasharray="2 7"
      />
      <g class="study-sheet-press">
        <g ref="backReply" class="study-sheet-reply">
          <g class="study-sheet-back">
            <rect x="169" y="70" width="142" height="192" rx="15" fill="#0c3439" opacity=".42" transform="rotate(17 240 166) translate(4 9)" />
            <g transform="rotate(17 240 166)">
              <rect x="169" y="70" width="142" height="192" rx="15" fill="#7fc3d2" />
              <rect x="177" y="78" width="126" height="176" rx="10" stroke="#b9e4e9" stroke-opacity=".66" />
              <path d="M273 117h18m-18 12h18m-25 12h25" stroke="#398291" stroke-opacity=".67" stroke-width="2" stroke-linecap="round" />
              <circle cx="285" cy="222" r="11" fill="#a5d9df" />
            </g>
          </g>
        </g>
        <g ref="frontReply" class="study-sheet-reply">
          <g class="study-sheet-front">
            <rect x="90" y="61" width="160" height="206" rx="16" fill="#092b29" opacity=".45" transform="rotate(-11 170 164) translate(3 8)" />
            <g transform="rotate(-11 170 164)">
              <rect x="90" y="61" width="160" height="206" rx="16" fill="#f5fae9" />
              <rect x="98" y="69" width="144" height="190" rx="11" stroke="#d9e8ce" />
              <path d="M211 61h18v39l-9-6-9 6V61Z" fill="#b7e78d" />
              <path d="M111 178h118m-118 15h83m-83 15h96m-96 15h62" stroke="#bfdbc5" stroke-width="2" stroke-linecap="round" />
              <text x="110" y="154" fill="#255647" font-family="Georgia, serif" font-size="76" letter-spacing="-5">Aa</text>
              <circle cx="216" cy="230" r="15" fill="#d6edc2" />
              <path d="m210 230 4 4 8-9" stroke="#42703c" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
            </g>
          </g>
        </g>
      </g>
      <g ref="sparks" class="study-sparks">
        <path d="m306 43 4 13 13 4-13 4-4 13-4-13-13-4 13-4 4-13Z" fill="#c9ed96" />
        <path d="m61 103 2 7 7 2-7 2-2 7-2-7-7-2 7-2 2-7Z" fill="#97dce3" />
        <circle cx="47" cy="225" r="4" fill="#bfe992" />
        <circle cx="336" cy="231" r="5" fill="#8cd7e3" />
        <circle cx="284" cy="292" r="2.5" fill="#d0edb4" />
      </g>
    </svg>
  </button>
</template>

<style scoped>
.study-art-trigger {
  display: block;
  width: 100%;
  height: 100%;
  padding: 0;
  border-radius: 18px;
  touch-action: manipulation;
}

.study-art-trigger:active:not(:disabled) {
  transform: none;
}

.study-art-trigger:focus-visible {
  outline: 2px solid #d3f4b1;
  outline-offset: -8px;
}

.study-art {
  display: block;
  width: 100%;
  height: 100%;
  overflow: visible;
  pointer-events: none;
}

.study-sheet-press,
.study-sheet-reply,
.study-sparks {
  transform-box: fill-box;
  transform-origin: center;
}

.study-sheet-press {
  transition: transform 110ms ease-out;
}

.study-art-trigger:active .study-sheet-press {
  transform: translateY(4px) scale(.96);
}

.study-sheet-front {
  animation: study-sheet-float 7s ease-in-out infinite alternate paused;
}

.study-sheet-back {
  animation: study-sheet-float 8.4s -4s ease-in-out infinite alternate-reverse paused;
}

.study-orbit {
  animation: study-orbit-drift 24s linear infinite paused;
}

.is-playing :is(.study-sheet-front, .study-sheet-back, .study-orbit) {
  animation-play-state: running;
}

@keyframes study-sheet-float {
  from { transform: translate(0, 3px); }
  to { transform: translate(0, -7px); }
}

@keyframes study-orbit-drift {
  to { stroke-dashoffset: -180; }
}

@media (prefers-reduced-motion: reduce) {
  .study-art :is(.study-sheet-front, .study-sheet-back, .study-orbit) {
    animation: none;
  }

  .study-sheet-press {
    transition: none;
  }

  .study-art-trigger:active .study-sheet-press {
    transform: none;
    filter: brightness(1.1);
  }
}
</style>
