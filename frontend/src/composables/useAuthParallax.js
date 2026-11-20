import { onBeforeUnmount, watch } from "vue";

// Sample the latest pointer once per frame. Let the scene settle on its own clock.
export function useAuthParallax(story, playing, quiet) {
  let frame = 0;
  let lastTime = 0;
  let x = 0;
  let y = 0;
  let targetX = 0;
  let targetY = 0;
  let pointer = null;

  function paint() {
    story.value?.style.setProperty("--auth-depth-x", x.toFixed(2) + "px");
    story.value?.style.setProperty("--auth-depth-y", y.toFixed(2) + "px");
  }

  function draw(time) {
    if (pointer && story.value) {
      const bounds = story.value.getBoundingClientRect();
      if (bounds.width && bounds.height) {
        targetX = Math.max(-1, Math.min(1, 2 * (pointer.x - bounds.left) / bounds.width - 1)) * 10;
        targetY = Math.max(-1, Math.min(1, 2 * (pointer.y - bounds.top) / bounds.height - 1)) * 7;
      }
      pointer = null;
    }
    const elapsed = Math.max(0, Math.min(time - lastTime, 50));
    lastTime = time;
    const blend = 1 - Math.exp(-elapsed / 105);
    x += (targetX - x) * blend;
    y += (targetY - y) * blend;
    if (Math.abs(targetX - x) + Math.abs(targetY - y) < 0.04) {
      x = targetX;
      y = targetY;
    }
    paint();
    frame = x === targetX && y === targetY ? 0 : requestAnimationFrame(draw);
  }

  function schedule() {
    if (!frame) {
      lastTime = performance.now();
      frame = requestAnimationFrame(draw);
    }
  }

  function move(event) {
    if (!playing.value || quiet.value || event.pointerType === "touch") return;
    if (event.target.closest?.(".word-hit, .auth-garden")) {
      freeze();
      return;
    }
    pointer = { x: event.clientX, y: event.clientY };
    schedule();
  }

  function reset() {
    pointer = null;
    targetX = 0;
    targetY = 0;
    if (playing.value && (x || y)) schedule();
  }

  function freeze() {
    pointer = null;
    cancelAnimationFrame(frame);
    frame = 0;
    targetX = x;
    targetY = y;
  }

  watch(quiet, value => { if (value) reset(); });
  watch(playing, value => {
    if (value) return;
    freeze();
    x = y = targetX = targetY = 0;
    paint();
  });
  onBeforeUnmount(() => cancelAnimationFrame(frame));
  return { move, reset, freeze };
}
