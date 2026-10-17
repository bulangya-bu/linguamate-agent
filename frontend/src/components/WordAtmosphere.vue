<template>
  <div
    ref="fieldRef"
    class="word-atmosphere"
    :class="[
      'word-atmosphere--' + variant,
      { 'is-exploring': activeWord !== null, 'is-sleeping': sleeping },
    ]"
    role="group"
    aria-label="漂浮词语，悬停或聚焦查看中文"
  >
    <svg class="word-light-trail" viewBox="0 0 900 210" fill="none" aria-hidden="true">
      <defs>
        <linearGradient :id="gradientId" x1="40" y1="140" x2="870" y2="40" gradientUnits="userSpaceOnUse">
          <stop stop-color="#a7dcf5" stop-opacity="0" />
          <stop offset=".35" stop-color="#98d9f1" stop-opacity=".35" />
          <stop offset=".72" stop-color="#b9e59d" stop-opacity=".4" />
          <stop offset="1" stop-color="#c5e9f3" stop-opacity="0" />
        </linearGradient>
      </defs>
      <path d="M-40 179C117-21 260 232 435 97S714 16 958 101" :stroke="'url(#' + gradientId + ')'" stroke-width="46" />
      <path d="M-40 156C117-44 260 209 435 74S714-7 958 78" stroke="white" stroke-opacity=".7" stroke-width="1.5" />
      <path d="M-40 200C117 0 260 253 435 118S714 37 958 122" stroke="#90cce2" stroke-opacity=".22" />
    </svg>
    <span class="word-light-dust word-light-dust--one" aria-hidden="true"></span>
    <span class="word-light-dust word-light-dust--two" aria-hidden="true"></span>
    <div
      v-for="(word, index) in words"
      :key="index"
      class="word-motion"
      :class="['word-motion--' + index, { 'is-active': activeWord === index }]"
      :style="motionStyles[index]"
    >
      <button
        class="word-shard"
        :class="['word-shard--' + slotTones[index], { 'is-revealed': activeWord === index, 'is-changing': changingSlot === index }]"
        type="button"
        :aria-expanded="activeWord === index"
        :aria-label="activeWord === index ? word.en + '：' + word.cn : word.en + '，查看中文释义'"
        @mouseenter="activeWord = index"
        @focus="activeWord = index"
        @mouseleave="leaveWord($event, index)"
        @blur="leaveWord($event, index)"
        @click="activeWord = index"
        @keydown.esc.stop="activeWord = null"
      >
        <span class="word-shard-glint" aria-hidden="true"></span>
        <span class="word-shard-en">{{ word.en }}</span>
        <span class="word-shard-cn" :aria-hidden="activeWord !== index">{{ word.cn }}</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';

const props = defineProps({ variant: { type: String, default: 'auth' } });
const activeWord = ref(null);
const fieldRef = ref(null);
const sleeping = ref(false);
const focused = ref(false);
const formActive = ref(false);
const reducedMotion = ref(false);
const desktopVisible = ref(false);
const changingSlot = ref(null);
const gradientId = 'word-light-' + props.variant;
const wordPool = [
  { en: 'wonder', cn: '好奇 · 惊奇' },
  { en: 'serendipity', cn: '不期而遇的美好' },
  { en: 'unfold', cn: '展开 · 显现' },
  { en: 'bloom', cn: '绽放 · 成长' },
  { en: 'discover', cn: '发现 · 探索' },
  { en: 'curiosity', cn: '好奇心' },
  { en: 'wander', cn: '漫步 · 探索' },
  { en: 'radiance', cn: '光辉 · 神采' },
  { en: 'resonate', cn: '共鸣 · 回响' },
  { en: 'embrace', cn: '拥抱 · 接纳' },
  { en: 'whisper', cn: '低语 · 轻声说' },
  { en: 'glimmer', cn: '微光 · 希望' },
  { en: 'thrive', cn: '茁壮成长' },
  { en: 'kindred', cn: '志趣相投的' },
  { en: 'solace', cn: '慰藉 · 安慰' },
  { en: 'lucid', cn: '清晰的 · 明澈的' },
  { en: 'luminous', cn: '明亮的 · 发光的' },
  { en: 'horizon', cn: '地平线 · 眼界' },
  { en: 'rhythm', cn: '节奏 · 韵律' },
  { en: 'moment', cn: '片刻 · 瞬间' },
  { en: 'gentle', cn: '温柔的 · 轻柔的' },
  { en: 'savor', cn: '品味 · 细细体会' },
  { en: 'spark', cn: '火花 · 灵感' },
  { en: 'muse', cn: '灵感 · 沉思' },
  { en: 'brave', cn: '勇敢的' },
  { en: 'grace', cn: '优雅 · 风度' },
  { en: 'gleam', cn: '微光 · 闪烁' },
  { en: 'flourish', cn: '蓬勃生长' },
  { en: 'inspire', cn: '启发 · 鼓舞' },
  { en: 'breeze', cn: '微风 · 轻松自在' },
  { en: 'serene', cn: '宁静的 · 安然的' },
  { en: 'gratitude', cn: '感激 · 感恩' },
  { en: 'empathy', cn: '共情 · 同理心' },
  { en: 'belong', cn: '归属 · 属于' },
  { en: 'weave', cn: '编织 · 交织' },
  { en: 'deepen', cn: '加深 · 深入' },
  { en: 'renew', cn: '重焕生机' },
  { en: 'dwell', cn: '居住 · 沉思' },
  { en: 'haven', cn: '避风港' },
  { en: 'ripple', cn: '涟漪 · 波纹' },
  { en: 'create', cn: '创造' },
  { en: 'balance', cn: '平衡 · 均衡' },
  { en: 'journey', cn: '旅程 · 历程' },
  { en: 'cherish', cn: '珍惜 · 爱护' },
  { en: 'envision', cn: '想象 · 展望' },
  { en: 'radiant', cn: '光芒四射的' },
  { en: 'patience', cn: '耐心' },
  { en: 'clarity', cn: '清晰 · 明晰' },
  { en: 'insight', cn: '洞察 · 领悟' },
  { en: 'vivid', cn: '生动的 · 鲜明的' },
];
const slotTones = ['blue', 'green', 'green', 'blue'];
const slotMaxLength = [7, 12, 7, 5];
const historyKey = 'linguamate:ambient-words:' + props.variant;
let recentWords = [];
try {
  const stored = JSON.parse(sessionStorage.getItem(historyKey) || '[]');
  if (Array.isArray(stored)) recentWords = stored.filter(word => typeof word === 'string').slice(-12);
} catch { /* The atmosphere also works when browser storage is unavailable. */ }

function chooseWord(index, visibleWords) {
  const available = wordPool.filter(word => word.en.length <= slotMaxLength[index] && !visibleWords.includes(word.en));
  const fresh = available.filter(word => !recentWords.includes(word.en));
  const candidates = fresh.length ? fresh : available;
  return candidates[Math.floor(Math.random() * candidates.length)];
}

function rememberWord(word) {
  recentWords = [...recentWords, word.en].slice(-12);
  try { sessionStorage.setItem(historyKey, JSON.stringify(recentWords)); } catch { /* Optional history. */ }
}

const initialWords = [];
slotMaxLength.forEach((_, index) => {
  const word = chooseWord(index, initialWords.map(item => item.en));
  initialWords.push(word);
  rememberWord(word);
});
const words = ref(initialWords);

// Each word has a different, bounded path; transforms keep drift off the layout thread.
const random = (min, max) => min + Math.random() * (max - min);
const motionStyles = initialWords.map((_, index) => ({
  '--drift-x': random(8, 17).toFixed(1) + 'px',
  '--drift-y': random(7, 13).toFixed(1) + 'px',
  '--drift-back-x': random(-14, -6).toFixed(1) + 'px',
  '--drift-back-y': random(-12, -5).toFixed(1) + 'px',
  '--word-tilt': (index % 2 ? random(3, 7) : random(-7, -3)).toFixed(1) + 'deg',
  '--drift-duration': random(21, 34).toFixed(1) + 's',
  '--drift-delay': random(-28, -2).toFixed(1) + 's',
}));

function leaveWord(event, index) {
  if (event.type === 'mouseleave' && event.currentTarget.matches(':focus-visible')) return;
  if (event.type === 'blur' && event.currentTarget.matches(':hover')) return;
  if (activeWord.value === index) activeWord.value = null;
}

function syncVisibility() {
  sleeping.value = document.hidden;
}

function syncFocus(event) {
  const target = event.type === 'focusout' ? event.relatedTarget : event.target;
  focused.value = Boolean(target && fieldRef.value?.contains(target));
  formActive.value = props.variant === 'auth' && target instanceof Element
    && Boolean(target.closest('.auth-form-side'));
}

let swapTimer;
let fadeTimer;
let slotQueue = [];
let motionPreference;
let desktopQuery;
const canChangeWords = computed(() => activeWord.value === null && !focused.value && !formActive.value
  && !sleeping.value && !reducedMotion.value && desktopVisible.value);

function cancelWordChange() {
  clearTimeout(swapTimer);
  clearTimeout(fadeTimer);
  changingSlot.value = null;
}

function scheduleWordChange() {
  clearTimeout(swapTimer);
  if (!canChangeWords.value) return;
  swapTimer = setTimeout(() => {
    if (!canChangeWords.value) return;
    if (!slotQueue.length) {
      slotQueue = [0, 1, 2, 3];
      for (let i = slotQueue.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [slotQueue[i], slotQueue[j]] = [slotQueue[j], slotQueue[i]];
      }
    }
    const index = slotQueue[slotQueue.length - 1];
    changingSlot.value = index;
    fadeTimer = setTimeout(() => {
      if (!canChangeWords.value) return;
      const word = chooseWord(index, words.value.map(item => item.en));
      words.value[index] = word;
      rememberWord(word);
      slotQueue.pop();
      changingSlot.value = null;
      scheduleWordChange();
    }, 300);
  }, random(11000, 17000));
}

function syncPreferences() {
  reducedMotion.value = motionPreference.matches;
  desktopVisible.value = desktopQuery.matches;
}

watch(canChangeWords, (canChange) => {
  cancelWordChange();
  if (canChange) scheduleWordChange();
}, { flush: 'sync' });

onMounted(() => {
  syncVisibility();
  motionPreference = window.matchMedia('(prefers-reduced-motion: reduce)');
  desktopQuery = window.matchMedia('(min-width: 981px)');
  syncPreferences();
  syncFocus({ type: 'focusin', target: document.activeElement });
  document.addEventListener('focusin', syncFocus);
  document.addEventListener('focusout', syncFocus);
  document.addEventListener('visibilitychange', syncVisibility);
  motionPreference.addEventListener('change', syncPreferences);
  desktopQuery.addEventListener('change', syncPreferences);
});
onBeforeUnmount(() => {
  cancelWordChange();
  document.removeEventListener('visibilitychange', syncVisibility);
  document.removeEventListener('focusin', syncFocus);
  document.removeEventListener('focusout', syncFocus);
  motionPreference?.removeEventListener('change', syncPreferences);
  desktopQuery?.removeEventListener('change', syncPreferences);
});
</script>

<style scoped>
.word-atmosphere { display: none; }
@media (min-width: 981px) {
  .word-atmosphere {
    position: absolute;
    z-index: 1;
    display: block;
    inset: 0;
    pointer-events: none;
    user-select: none;
    isolation: isolate;
    animation: word-light-arrive 1.2s ease both;
  }
  .word-light-trail {
    position: absolute;
    z-index: -1;
    inset: -5% -6%;
    width: 112%;
    height: 110%;
    overflow: visible;
    opacity: .65;
  }
  .word-motion {
    position: absolute;
    animation: word-light-drift var(--drift-duration) ease-in-out var(--drift-delay) infinite;
  }
  .word-atmosphere.is-exploring .word-motion,
  .word-atmosphere.is-sleeping .word-motion { animation-play-state: paused; }
  .word-motion.is-active { z-index: 3; }
  .word-shard {
    --word-ink: #507f99;
    --word-tint: #e1f2fa;
    position: relative;
    display: grid;
    place-items: center;
    width: 130px;
    height: 64px;
    padding: 0 20px;
    overflow: hidden;
    border: 1px solid #ffffffb8;
    border-radius: 17px 6px 17px 6px;
    color: var(--word-ink);
    background: linear-gradient(135deg, #ffffffe6, #ffffff47 58%, var(--word-tint));
    box-shadow: 0 4px 18px #69a1b009, inset 0 0 0 1px #ffffff28;
    backdrop-filter: blur(6px);
    pointer-events: auto;
    cursor: pointer;
    transform: rotate(var(--word-tilt));
    opacity: .6;
    transition: opacity 350ms ease, background 350ms ease, box-shadow 350ms ease, border-color 350ms ease, transform 500ms var(--ease);
  }
  .word-shard--green { --word-ink: #64854a; --word-tint: #edf7df; }
  .word-motion--1 .word-shard { width: 166px; }
  .word-motion--3 .word-shard { width: 116px; }
  .word-shard-glint {
    position: absolute;
    top: 0;
    left: 13%;
    width: 50%;
    height: 1px;
    background: #ffffff;
    box-shadow: 0 1px 7px #ffffff;
  }
  .word-shard-en {
    font: italic 24px/1.2 Georgia, serif;
    letter-spacing: -.35px;
    transition: transform 400ms var(--ease), color 300ms ease, opacity 260ms ease;
  }
  .word-shard-cn {
    position: absolute;
    top: 38px;
    right: 10px;
    left: 10px;
    color: #506b7c;
    font-size: 11px;
    font-weight: 450;
    letter-spacing: .4px;
    white-space: nowrap;
    opacity: 0;
    transform: translateY(5px);
    transition: opacity 260ms ease, transform 400ms var(--ease);
  }
  .word-shard.is-revealed {
    border-color: #c5e4ef;
    background: #ffffffef;
    box-shadow: 0 10px 30px #4e91ad14, inset 0 -2px 0 var(--word-tint);
    opacity: 1;
    transform: rotate(0deg) translateY(-2px);
  }
  .word-shard.is-revealed .word-shard-en { color: #386980; transform: translateY(-10px); }
  .word-shard--green.is-revealed .word-shard-en { color: #5a7e3d; }
  .word-shard.is-revealed .word-shard-cn { opacity: 1; transform: translateY(0); }
  .word-shard.is-changing .word-shard-en,
  .word-shard.is-changing .word-shard-cn { opacity: 0; transform: translateY(3px); }
  .word-light-dust {
    position: absolute;
    width: 4px;
    height: 4px;
    border-radius: 50%;
    background: #9ecdbb;
    box-shadow: 0 0 0 5px #dff2e945;
    opacity: .55;
  }
  .word-light-dust--one { top: 32%; left: 36%; }
  .word-light-dust--two { top: 75%; left: 63%; width: 3px; height: 3px; background: #a3cce2; }
  .word-atmosphere--dashboard { left: clamp(530px, 42%, 760px); right: 18px; }
  .word-atmosphere--dashboard .word-motion--0 { left: 5%; top: 24px; }
  .word-atmosphere--dashboard .word-motion--1 { left: 48%; top: 24px; }
  .word-atmosphere--dashboard .word-motion--2 { left: 26%; bottom: 24px; }
  .word-atmosphere--dashboard .word-motion--3 { left: 72%; bottom: 24px; }
  .word-atmosphere--auth { z-index: 3; }
  .word-atmosphere--auth .word-motion--0 { left: 65%; top: 12%; }
  .word-atmosphere--auth .word-motion--1 { left: 63%; top: 51%; }
  .word-atmosphere--auth .word-motion--2 { left: 11%; top: 70%; }
  .word-atmosphere--auth .word-motion--3 { left: 37%; top: 85%; }
  .word-atmosphere--auth .word-light-trail { top: 46%; left: -24%; width: 154%; height: 48%; opacity: .5; transform: rotate(-18deg); }
  .word-atmosphere--auth .word-light-dust--one { top: 59%; left: 40%; }
  .word-atmosphere--auth .word-light-dust--two { top: 80%; left: 80%; }
  :global(.auth-page:has(.auth-form-side:focus-within) .word-motion) { animation-play-state: paused; }
  :global(.page-dashboard .focus-copy) { width: min(52%, 540px); }
  :global(.page-dashboard .focus-art) { display: none; }
}
@keyframes word-light-drift {
  0%, 100% { transform: translate3d(0, 0, 0); }
  30% { transform: translate3d(var(--drift-x), var(--drift-back-y), 0); }
  65% { transform: translate3d(var(--drift-back-x), var(--drift-y), 0); }
}
@keyframes word-light-arrive {
  from { opacity: 0; transform: translateY(5px); }
  to { opacity: 1; transform: translateY(0); }
}
@media (prefers-reduced-motion: reduce) {
  .word-atmosphere, .word-motion { animation: none; }
  .word-shard, .word-shard-en, .word-shard-cn { transition: none; }
}
</style>
