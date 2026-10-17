<template>
  <div class="word-atmosphere" aria-hidden="true">
    <span class="word-bubble word-hello">hello.</span>
    <span class="word-bubble word-wonder">wonder</span>
    <span class="word-bubble word-discover">discover</span>
    <span class="word-bubble word-grow">grow</span>
  </div>
</template>

<style scoped>
.word-atmosphere {
  position: absolute;
  z-index: 1;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  user-select: none;
}
.word-bubble {
  position: absolute;
  padding: 10px 22px 12px;
  border: 1px solid #ffffffd9;
  border-radius: 100px;
  color: #47728b;
  background: #ffffff80;
  box-shadow: 0 8px 28px #5b96b008;
  font:
    italic 21px/1.2 Georgia,
    serif;
  letter-spacing: -0.3px;
  opacity: 0.75;
  transform: rotate(var(--tilt));
  animation: word-drift var(--duration) ease-in-out var(--delay) infinite;
}
.word-hello {
  --tilt: 8deg;
  --duration: 22s;
  --delay: -5s;
  top: 18%;
  right: 11%;
  color: #508b32;
  background: #f3fce8b3;
}
.word-wonder {
  --tilt: -7deg;
  --duration: 26s;
  --delay: -13s;
  top: 54%;
  right: 7%;
}
.word-discover {
  --tilt: -6deg;
  --duration: 24s;
  --delay: -9s;
  bottom: 20%;
  left: 11%;
}
.word-grow {
  --tilt: 7deg;
  --duration: 20s;
  --delay: -3s;
  bottom: 8%;
  left: 35%;
  color: #8d7c40;
  background: #fffbe8a6;
}
@keyframes word-drift {
  0%,
  100% {
    transform: translate3d(0, 0, 0) rotate(var(--tilt));
  }
  50% {
    transform: translate3d(5px, -9px, 0) rotate(calc(var(--tilt) + 3deg));
  }
}
:global(.auth-page:focus-within .word-bubble) {
  animation-play-state: paused;
}
@media (max-width: 760px) {
  .word-atmosphere {
    display: none;
  }
}
@media (prefers-reduced-motion: reduce) {
  .word-bubble {
    animation: none;
  }
}
</style>
