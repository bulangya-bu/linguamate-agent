const TAIL_LIFETIME = 190;
const PAPER_TAIL_LIFETIME = 110;
const PAPER_TAIL_LENGTH = 32;
const SCRAP_LIFETIME = 460;
const MAX_SEGMENTS = 13;
const MAX_SCRAPS = 3;
const MAX_SEGMENT = 48;
const PALETTES = {
  light: { tail: "#66a68a", scraps: ["#7dbb74", "#6daac3", "#cea36b"] },
  dark: { tail: "#a7d8bb", scraps: ["#add58a", "#89c8e0", "#e1bb8e"] },
};

export function createCursorTrail(canvas) {
  const context = canvas.getContext("2d");
  if (!context) return () => {};

  const motion = window.matchMedia("(any-hover: hover) and (any-pointer: fine) and (prefers-reduced-motion: no-preference)");
  const listeners = new AbortController();
  const options = { passive: true, signal: listeners.signal };
  let segments = [];
  let scraps = [];
  let previous;
  let lastScrapAt = -Infinity;
  let scrapIndex = 0;
  let frame = 0;
  let width = 0;
  let height = 0;

  function clear() {
    if (frame) cancelAnimationFrame(frame);
    frame = 0;
    segments = [];
    scraps = [];
    previous = undefined;
    lastScrapAt = -Infinity;
    context.clearRect(0, 0, width, height);
  }

  function resize() {
    width = window.innerWidth;
    height = window.innerHeight;
    const scale = Math.min(window.devicePixelRatio || 1, 2, Math.sqrt(5_000_000 / (width * height)));
    canvas.width = Math.round(width * scale);
    canvas.height = Math.round(height * scale);
    context.setTransform(scale, 0, 0, scale, 0, 0);
    context.lineCap = "round";
    clear();
  }

  function draw(now) {
    context.clearRect(0, 0, width, height);
    segments = segments.filter((segment) => now - segment.time < segment.lifetime);
    scraps = scraps.filter((scrap) => now - scrap.time < SCRAP_LIFETIME);
    const palette = PALETTES[document.documentElement.dataset.theme] ?? PALETTES.light;

    context.strokeStyle = palette.tail;
    // Limit the paper trail by distance along the stroke, even through turns.
    let paperLengthLeft = PAPER_TAIL_LENGTH;
    for (let index = segments.length - 1; index >= 0; index--) {
      const segment = segments[index];
      let visibleLength = segment.length;
      if (segment.paper) {
        visibleLength = Math.min(visibleLength, paperLengthLeft);
        if (!visibleLength) continue;
        paperLengthLeft -= visibleLength;
      }
      const remaining = 1 - Math.max(0, now - segment.time) / segment.lifetime;
      context.globalAlpha = remaining ** 2 * segment.opacity;
      context.lineWidth = segment.paper ? 0.65 + remaining * 0.45 : 0.7 + remaining * 0.8;
      context.beginPath();
      context.moveTo(segment.x - segment.unitX * visibleLength, segment.y - segment.unitY * visibleLength);
      context.lineTo(segment.x, segment.y);
      context.stroke();
    }

    for (const scrap of scraps) {
      const progress = Math.max(0, now - scrap.time) / SCRAP_LIFETIME;
      const drift = 1 - (1 - progress) ** 2;
      context.save();
      context.translate(scrap.x + scrap.dx * drift, scrap.y + scrap.dy * drift + 9 * progress ** 2);
      context.rotate(scrap.angle + scrap.spin * drift);
      context.globalAlpha = (1 - progress) * 0.74;
      context.fillStyle = palette.scraps[scrap.color];
      context.beginPath();
      context.moveTo(-3, -2);
      context.lineTo(2, -2.6);
      context.lineTo(3, 0.6);
      context.lineTo(0.8, 2.1);
      context.lineTo(-2.5, 1.7);
      context.closePath();
      context.fill();
      context.restore();
    }

    context.globalAlpha = 1;
    frame = segments.length || scraps.length ? requestAnimationFrame(draw) : 0;
  }

  function move(event) {
    if (!motion.matches || event.pointerType !== "mouse") return;
    if (event.buttons || (event.target instanceof Element && getComputedStyle(event.target).cursor === "text")) {
      if (previous || frame) clear();
      return;
    }

    const paper = event.target instanceof Element && Boolean(event.target.closest('[data-cursor-trail="paper"]'));
    const lifetime = paper ? PAPER_TAIL_LIFETIME : TAIL_LIFETIME;
    const point = { x: event.clientX, y: event.clientY, time: performance.now(), paper };
    if (!previous || previous.paper !== paper || point.time - previous.time >= lifetime) {
      previous = point;
      return;
    }

    const dx = point.x - previous.x;
    const dy = point.y - previous.y;
    const distance = Math.hypot(dx, dy);
    if (distance < 3) return;

    const unitX = dx / distance;
    const unitY = dy / distance;
    const speed = distance / Math.max(8, point.time - previous.time);
    if (!paper && distance > MAX_SEGMENT) segments = segments.filter(segment => segment.paper);
    segments.push({
      x: point.x, y: point.y, unitX, unitY, paper, lifetime,
      length: Math.min(distance, MAX_SEGMENT),
      time: previous.time,
      opacity: paper ? 0.025 + 0.19 * Math.min(speed / 1.2, 1) : 0.43,
    });
    if (segments.length > MAX_SEGMENTS) segments.shift();

    if (!paper && speed >= 0.72 && point.time - lastScrapAt >= 75) {
      const side = scrapIndex % 2 === 0 ? 1 : -1;
      scraps.push({
        x: point.x - unitX * 12 - unitY * side * 5,
        y: point.y - unitY * 12 + unitX * side * 5,
        dx: -unitX * 18 - unitY * side * 13,
        dy: -unitY * 18 + unitX * side * 13,
        angle: Math.atan2(dy, dx) + side * 0.6,
        spin: side * 1.3,
        color: scrapIndex % PALETTES.light.scraps.length,
        time: point.time,
      });
      if (scraps.length > MAX_SCRAPS) scraps.shift();
      lastScrapAt = point.time;
      scrapIndex += 1;
    }

    previous = point;
    if (!frame) frame = requestAnimationFrame(draw);
  }

  function syncMotion() {
    if (motion.matches) resize();
    else clear();
  }

  motion.addEventListener("change", syncMotion, options);
  window.addEventListener("pointermove", move, options);
  window.addEventListener("resize", syncMotion, options);
  window.addEventListener("blur", clear, options);
  document.documentElement.addEventListener("pointerleave", clear, options);
  document.addEventListener("visibilitychange", clear, options);
  syncMotion();

  return () => {
    listeners.abort();
    clear();
  };
}
