import { onBeforeUnmount, watch } from "vue";

// Only interpolate while the pointer is moving; idle ambience runs in CSS.
export function useAuthParallax(root, active) {
  let frame = 0;
  let x = 0;
  let y = 0;
  let targetX = 0;
  let targetY = 0;
  function draw() {
    x += (targetX - x) * 0.085;
    y += (targetY - y) * 0.085;
    if (Math.abs(targetX - x) + Math.abs(targetY - y) < 0.02) {
      x = targetX;
      y = targetY;
    }
    root.value?.style.setProperty("--auth-depth-x", `${x.toFixed(3)}px`);
    root.value?.style.setProperty("--auth-depth-y", `${y.toFixed(3)}px`);
    frame = x === targetX && y === targetY ? 0 : requestAnimationFrame(draw);
  }
  function schedule() {
    if (!frame) frame = requestAnimationFrame(draw);
  }
  function move(event) {
    if (!active.value || event.pointerType === "touch") return;
    if (event.target.closest?.(".word-shard, .auth-garden")) {
      freeze();
      return;
    }
    const bounds = event.currentTarget.getBoundingClientRect();
    targetX =
      Math.max(
        -1,
        Math.min(1, ((event.clientX - bounds.left) / bounds.width - 0.5) * 2),
      ) * 16;
    targetY =
      Math.max(
        -1,
        Math.min(1, ((event.clientY - bounds.top) / bounds.height - 0.5) * 2),
      ) * 12;
    schedule();
  }
  function reset() {
    targetX = 0;
    targetY = 0;
    schedule();
  }
  function freeze() {
    cancelAnimationFrame(frame);
    frame = 0;
    targetX = x;
    targetY = y;
  }
  watch(active, (value) => {
    if (value) return;
    cancelAnimationFrame(frame);
    frame = x = y = targetX = targetY = 0;
    root.value?.style.setProperty("--auth-depth-x", "0px");
    root.value?.style.setProperty("--auth-depth-y", "0px");
  });
  onBeforeUnmount(() => cancelAnimationFrame(frame));
  return { move, reset, freeze };
}
