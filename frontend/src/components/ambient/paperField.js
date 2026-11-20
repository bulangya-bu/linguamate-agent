const TAU = Math.PI * 2;
const MAX_PARTICLES = 48;
const DESKTOP_PARTICLES = 28;
const EXTRA_LEARNING_PAPERS = 2;

const learningLayouts = {
  dashboard: {
    columns: [[".study-next", ".study-summary"]],
    rows: [[".study-next, .study-summary", ".study-words-heading"],
      [".study-word-list, .study-word-empty", ".study-footprint"]],
    bottom: ".study-footprint",
  },
  vocabulary: { center: ".quiz-workbench, .quiz-empty, .quiz-complete" },
  review: { center: ".review-workspace" },
  chat: {
    columns: [[".studio-dialogue", ".studio-coach"]],
    rows: [[".studio-chat-toolbar", ".studio-chat-columns"]],
    bottom: ".studio-chat-columns",
  },
  writing: { columns: [[".ws-manuscript", ".ws-inspector"]], bottom: ".writing-studio" },
  news: { columns: [[".news-library", ".reader-panel"]], bottom: ".news-layout" },
  wordbank: { bottom: ".wordbank-layout" },
  journal: { columns: [[".archive-trend", ".archive-calendar"]], bottom: ".archive-spread" },
};

const clamp = (value, min, max) => Math.max(min, Math.min(max, value));
const seed = (index) => {
  const value = Math.sin(index * 127.1 + 311.7) * 43758.5453;
  return value - Math.floor(value);
};

function createParticle(index) {
  const width = 8 + seed(index + 50) * 7;
  const height = 6 + seed(index + 60) * 4;
  const dot = index % 4 === 0;
  return {
    index,
    region: null,
    x: 0,
    y: 0,
    vx: 0,
    vy: 0,
    angle: seed(index + 10) * TAU,
    spin: 0,
    lift: 0,
    liftTarget: 0,
    phase: seed(index + 20) * TAU,
    speed: 4 + seed(index + 30) * 6,
    turn: (seed(index + 40) - .5) * .28,
    width,
    opacity: .34 + seed(index + 70) * .16,
    color: index % 7 === 0 ? 2 : index % 2,
    paper: dot ? null : createPaperShape(width, height, index % 5 === 1),
    reveal: 0,
  };
}

function createPaperShape(width, height, bookmark) {
  const left = -width / 2;
  const right = width / 2;
  const top = -height / 2;
  const bottom = height / 2;
  const corner = bookmark ? 0 : Math.min(3.3, height * .34);
  const outline = new Path2D();
  outline.moveTo(left, top);
  outline.lineTo(right - corner, top);
  outline.lineTo(right, top + corner);
  outline.lineTo(right, bottom);
  if (bookmark) outline.lineTo(0, bottom - 2.4);
  outline.lineTo(left, bottom);
  outline.closePath();

  let fold;
  if (corner) {
    fold = new Path2D();
    fold.moveTo(right - corner, top);
    fold.lineTo(right - corner, top + corner);
    fold.lineTo(right, top + corner);
    fold.closePath();
  }

  let line;
  if (width >= 11) {
    line = new Path2D();
    line.moveTo(left + 2.4, top + 2.6);
    line.lineTo(right - corner - 1.4, top + 2.6);
  }

  return { outline, fold, line };
}

// A canvas owns the free-moving pieces; Vue controls visibility and lifetime.
export function createPaperField(canvas, { contained }) {
  const context = canvas.getContext("2d");
  if (!context) return { setMotion() {}, setQuiet() {}, destroy() {} };

  let bounds;
  let regions = [];
  let palette = [];
  let paperLight;
  let paperShadow;
  let layoutKey = "";
  let width = 0;
  let height = 0;
  let distribute = false;
  let active = false;
  let reduced = false;
  let quiet = false;
  let driftRate = 1;
  let destroyed = false;
  let frame = 0;
  let refreshFrame = 0;
  let lastFrame = 0;
  let time = 0;
  let pointer = { x: 0, y: 0, at: 0, present: false };
  let velocitySample = { x: 0, y: 0, at: 0 };
  const particles = [];

  function localRect(element) {
    const rect = element?.getBoundingClientRect();
    if (!rect || !rect.width || !rect.height) return null;
    return {
      x: rect.left - bounds.left,
      y: rect.top - bounds.top,
      width: rect.width,
      height: rect.height,
    };
  }

  function readLayout(section) {
    const spaces = [];
    const addSpace = (key, x, y, spaceWidth, spaceHeight) => {
      const right = Math.min(width, x + spaceWidth);
      const bottom = Math.min(height, y + spaceHeight);
      const region = {
        key,
        x: Math.round(Math.max(0, x)),
        y: Math.round(Math.max(0, y)),
        width: Math.round(right - Math.max(0, x)),
        height: Math.round(bottom - Math.max(0, y)),
      };
      if (region.width >= 10 && region.height >= 10) spaces.push(region);
    };

    if (contained) {
      // Use the whitespace around the existing login illustration.
      if (height < 140) {
        addSpace("auth-top", width * .59, 12, width * .32, height - 24);
      } else {
        addSpace("auth-top", 64, height * .15, width * .61 - 64, height * .33);
        addSpace("auth-left", 28, height * .54, width * .12, height * .3);
        addSpace("auth-right", width * .85, height * .28, width * .12, height * .23);
      }
      return spaces;
    }

    const bar = localRect(document.querySelector(".workspace-bar"));
    const nav = localRect(document.querySelector(".side-nav"));
    if (!bar) return spaces;

    if (width <= 760) {
      addSpace("top", width * .38, 151, width * .26, 38);
      return spaces;
    }

    const title = document.querySelector(".topbar h1");
    let titleRight = bar.x + 220;
    if (title) {
      const range = document.createRange();
      range.selectNodeContents(title);
      titleRight = Math.max(titleRight, range.getBoundingClientRect().right - bounds.left + 24);
    }
    const sectionRect = localRect(section);
    const top = bar.y + bar.height + 7;
    const barRight = bar.x + bar.width;
    addSpace("top", titleRight, top, barRight - 24 - titleRight,
      Math.min(44, (sectionRect?.y ?? top + 44) - top));

    const middle = Math.max(top + 32, sectionRect?.y ?? height * .23);
    const lower = height * .62;
    const bottom = height - 4;
    const addRail = (key, x, railWidth, start = middle, end = bottom) => {
      const split = clamp(lower, start, end);
      addSpace(`${key}-middle`, x, start, railWidth, split - start);
      addSpace(`${key}-lower`, x, split, railWidth, end - split);
    };

    if (nav) {
      addRail("left-edge", nav.x + nav.width + 3, bar.x - nav.x - nav.width - 6);
    }
    addRail("right-edge", barRight + 4, width - barRight - 8);

    const layout = learningLayouts[section?.id];
    if (!layout) return spaces;

    const find = (selector) => localRect(section.querySelector(selector));
    const center = layout.center && find(layout.center);
    if (center) {
      const start = Math.max(middle, center.y + 10);
      const end = Math.min(bottom, center.y + center.height);
      const right = center.x + center.width;
      addRail("content-left", bar.x + 8, Math.min(48, center.x - bar.x - 16), start, end);
      const rightWidth = Math.min(48, barRight - right - 16);
      addRail("content-right", barRight - 8 - rightWidth, rightWidth, start, end);
    }

    for (const [index, [leftSelector, rightSelector]] of (layout.columns ?? []).entries()) {
      const left = find(leftSelector);
      const right = find(rightSelector);
      if (!left || !right) continue;
      const x = left.x + left.width + 2;
      const start = Math.max(left.y, right.y, middle);
      const end = Math.min(left.y + left.height, right.y + right.height, bottom);
      addRail(`column-${index}`, x, right.x - x - 2, start, end);
    }

    for (const [index, [aboveSelector, belowSelector]] of (layout.rows ?? []).entries()) {
      const above = [...section.querySelectorAll(aboveSelector)].map(localRect).filter(Boolean);
      const below = find(belowSelector);
      if (!above.length || !below) continue;
      const y = Math.max(middle, ...above.map((rect) => rect.y + rect.height + 3));
      addSpace(`row-${index}`, below.x + 12, y, below.width - 24, below.y - y - 3);
    }

    const last = layout.bottom ? find(layout.bottom) : center;
    if (last) {
      const y = Math.max(middle, last.y + last.height + 2, bottom - 52);
      addSpace("bottom", bar.x + 20, y, bar.width - 40, bottom - y);
    }
    return spaces;
  }

  function updateParticles(learningPage) {
    const weights = regions.map((region) => {
      const area = region.width * region.height;
      return distribute ? Math.min(32000, area) : area;
    });
    const topIndex = distribute ? regions.findIndex((region) => region.key === "top") : -1;
    if (topIndex >= 0 && weights.length > 1) {
      const rest = weights.reduce((total, weight, index) => total + (index === topIndex ? 0 : weight), 0);
      weights[topIndex] = Math.min(weights[topIndex] * .65, rest * .5);
    }
    const area = weights.reduce((total, weight) => total + weight, 0);
    const count = area ? clamp(Math.round(area / 4800), 6, distribute ? DESKTOP_PARTICLES : MAX_PARTICLES) : 0;
    const topWeight = weights[topIndex] ?? 0;
    const extra = distribute && learningPage && area > topWeight ? EXTRA_LEARNING_PAPERS : 0;
    const totalCount = count + extra;
    particles.length = Math.min(particles.length, totalCount);
    while (particles.length < totalCount) particles.push(createParticle(particles.length));

    particles.forEach((particle) => {
      const previous = particle.region;
      const placement = distribute ? (particle.index * .61803398875 + .23) % 1 : seed(particle.index + 90);
      let remaining = particle.index < count ? placement * area : topWeight + placement * (area - topWeight);
      const region = regions.find((item, index) => {
        remaining -= weights[index];
        return remaining <= 0;
      }) || regions[regions.length - 1];
      if (region.key === previous?.key) {
        // A layout change carries each piece with its own region.
        particle.x = region.x + (particle.x - previous.x) / previous.width * region.width;
        particle.y = region.y + (particle.y - previous.y) / previous.height * region.height;
      } else {
        particle.x = region.x + seed(particle.index + 300) * region.width;
        particle.y = region.y + seed(particle.index + 600) * region.height;
        particle.vx = 0;
        particle.vy = 0;
        particle.spin = 0;
        particle.lift = 0;
        particle.liftTarget = 0;
        particle.reveal = reduced ? 1 : 0;
      }
      particle.region = region;
    });
  }

  function refresh() {
    refreshFrame = 0;
    if (destroyed) return;
    bounds = canvas.getBoundingClientRect();
    width = bounds.width;
    height = bounds.height;
    distribute = !contained && width > 760;
    const ratio = Math.min(window.devicePixelRatio || 1, 2,
      Math.sqrt(6_000_000 / Math.max(1, width * height)));
    const pixelWidth = Math.round(width * ratio);
    const pixelHeight = Math.round(height * ratio);
    if (canvas.width !== pixelWidth || canvas.height !== pixelHeight) {
      canvas.width = pixelWidth;
      canvas.height = pixelHeight;
    }
    context.setTransform(ratio, 0, 0, ratio, 0, 0);
    const style = getComputedStyle(canvas);
    palette = ["--paper-blue", "--paper-green", "--paper-apricot"]
      .map((name) => style.getPropertyValue(name).trim());
    paperLight = style.getPropertyValue("--paper-light").trim();
    paperShadow = style.getPropertyValue("--paper-shadow").trim();
    const section = document.querySelector(".workspace > .section.active");
    const learningPage = Object.hasOwn(learningLayouts, section?.id);
    const layout = readLayout(section);
    const nextKey = JSON.stringify([learningPage, layout]);
    if (nextKey !== layoutKey) {
      layoutKey = nextKey;
      regions = layout;
      updateParticles(learningPage);
    }
    draw(0);
  }

  function queueRefresh() {
    if (!destroyed && !refreshFrame) refreshFrame = requestAnimationFrame(refresh);
  }

  function layoutSettled(event) {
    if (!event.propertyName || ["transform", "width", "height", "grid-template-columns"].includes(event.propertyName)) {
      queueRefresh();
    }
  }

  function handleLayoutScroll(event) {
    if (distribute && (event.target === document || event.target.matches?.(".workspace, .workspace > .section"))) {
      queueRefresh();
    }
  }

  function drawPaper(particle, color, visibility) {
    const { paper, opacity, lift } = particle;
    const alpha = (opacity + lift * .19) * visibility;

    context.lineJoin = "round";
    context.lineWidth = .7;

    context.save();
    context.translate(.8 + lift, 1 + lift * 1.7);
    context.globalAlpha = alpha * (.19 + lift * .16);
    context.fillStyle = paperShadow;
    context.fill(paper.outline);
    context.restore();
    context.globalAlpha = alpha;
    context.fillStyle = color;
    context.fill(paper.outline);
    context.globalAlpha = alpha * .7;
    context.strokeStyle = color;
    context.stroke(paper.outline);

    if (paper.fold) {
      context.globalAlpha = alpha * .87;
      context.fillStyle = paperLight;
      context.fill(paper.fold);
    }

    if (paper.line) {
      context.globalAlpha = alpha * .68;
      context.strokeStyle = paperLight;
      context.lineCap = "round";
      context.stroke(paper.line);
    }
  }

  function draw(delta) {
    context.clearRect(0, 0, width, height);
    const drag = Math.exp(-delta * 1.8);
    const spinDrag = Math.exp(-delta * 2.2);
    const liftDrag = Math.exp(-delta * 2.6);
    const liftBlend = 1 - Math.exp(-delta * 18);
    particles.forEach((particle) => {
      const region = particle.region;
      const heading = particle.phase + Math.sin(time * .11 + particle.phase) * .7;
      const flowX = Math.cos(heading) * particle.speed;
      const flowY = Math.sin(heading * .83 + .8) * particle.speed * .8;
      particle.vx *= drag;
      particle.vy *= drag;
      particle.spin *= spinDrag;
      particle.x += (flowX * driftRate + particle.vx) * delta;
      particle.y += (flowY * driftRate + particle.vy) * delta;
      particle.angle += (particle.turn * driftRate + particle.spin) * delta;
      if (contained) {
        particle.lift += (particle.liftTarget - particle.lift) * liftBlend;
        particle.liftTarget *= liftDrag;
      } else {
        particle.lift *= liftDrag;
      }
      particle.reveal = reduced ? 1 : Math.min(1, particle.reveal + delta * 1.5);

      // Pieces fade at the boundary before re-entering from the opposite side.
      particle.x = region.x + ((particle.x - region.x) % region.width + region.width) % region.width;
      particle.y = region.y + ((particle.y - region.y) % region.height + region.height) % region.height;
      const edge = Math.min(particle.x - region.x, region.x + region.width - particle.x,
        particle.y - region.y, region.y + region.height - particle.y);
      const fade = distribute ? Math.min(8, region.width * .4, region.height * .4) : 8;
      const visibility = clamp(edge / fade, 0, 1) * particle.reveal;
      if (!visibility) return;

      context.save();
      context.translate(particle.x, particle.y);
      context.rotate(particle.angle);
      if (!particle.paper) {
        context.fillStyle = palette[particle.color];
        context.globalAlpha = (particle.opacity + particle.lift * .1) * visibility;
        context.beginPath();
        context.arc(0, 0, particle.width * .155, 0, TAU);
        context.fill();
      } else {
        const flutter = Math.sin(time * 1.55 + particle.phase);
        const tilt = contained ? -.38 : .17;
        context.scale(1 + particle.lift * .18, .88 + flutter * .12 + particle.lift * tilt);
        drawPaper(particle, palette[particle.color], visibility);
      }
      context.restore();
    });
  }

  function tick(now) {
    if (!active) return;
    frame = requestAnimationFrame(tick);
    if (!contained && lastFrame && now - lastFrame < 30) return;
    const elapsed = lastFrame ? (now - lastFrame) / 1000 : 1 / 30;
    const delta = Math.min(elapsed, .05);
    lastFrame = now;
    if (contained) {
      const targetRate = quiet ? .16 : 1;
      driftRate += (targetRate - driftRate) * (1 - Math.exp(-elapsed * 4.5));
    }
    time += delta * driftRate;
    draw(delta);
  }

  function movePointer(event) {
    if (!active || quiet || event.pointerType === "touch" || !bounds) return;
    const now = performance.now();
    const x = event.clientX - bounds.left;
    const y = event.clientY - bounds.top;
    if (x < 0 || x > width || y < 0 || y > height) {
      pointer.present = false;
      return;
    }
    if (!pointer.present || now - pointer.at >= 160) velocitySample = { x, y, at: now };
    pointer = { x, y, at: now, present: true };
    const sampleDuration = now - velocitySample.at;
    if (sampleDuration < 8) return;

    const dx = x - velocitySample.x;
    const dy = y - velocitySample.y;
    const travel = Math.hypot(dx, dy);
    if (travel < .5) return;
    velocitySample = { x, y, at: now };
    const speed = travel / sampleDuration * 1000;
    // The login scene keeps slow passes local; a brisk sweep catches a wider strip of paper.
    const sweep = Math.min(speed / 1050, 1);
    const radius = contained ? 38 + 36 * sweep : 60 + Math.min(speed / 27, 35);
    const gain = contained ? .24 + .99 * sweep : 1 + Math.min(speed / 2600, .22);
    const directionX = dx / travel;
    const directionY = dy / travel;
    // A sudden cursor jump should not scatter every piece between the two points.
    const distance = Math.min(travel, 180);
    const start = { x: x - directionX * distance, y: y - directionY * distance };

    particles.forEach((particle) => {
      const along = (particle.x - start.x) * directionX + (particle.y - start.y) * directionY;
      const closest = clamp(along, 0, distance);
      const offsetX = particle.x - (start.x + directionX * closest);
      const offsetY = particle.y - (start.y + directionY * closest);
      const separation = Math.hypot(offsetX, offsetY);
      if (separation >= radius) return;

      // Integrate the nearby part of the swept path in small spatial steps.
      // This keeps the same gesture consistent across mouse sampling rates.
      const from = Math.max(0, along - radius);
      const to = Math.min(distance, along + radius);
      const steps = Math.max(1, Math.ceil((to - from) / 6));
      const stepSize = (to - from) / steps;
      const side = Math.sin(particle.phase) >= 0 ? 1 : -1;
      let pushX = 0;
      let pushY = 0;
      let rotation = 0;
      for (let step = 0; step < steps; step += 1) {
        const position = from + (step + .5) * stepSize;
        const xDistance = particle.x - start.x - directionX * position;
        const yDistance = particle.y - start.y - directionY * position;
        const radialDistance = Math.hypot(xDistance, yDistance);
        const weight = Math.max(0, 1 - radialDistance / radius) ** 2 * stepSize;
        const normalX = radialDistance > 1 ? xDistance / radialDistance : -directionY * side;
        const normalY = radialDistance > 1 ? yDistance / radialDistance : directionX * side;
        pushX += (normalX * .95 + directionX * 1.03) * weight;
        pushY += (normalY * .95 + directionY * 1.03) * weight;
        rotation += side * weight * .044;
      }
      const region = particle.region;
      const vx = particle.vx + pushX * gain;
      const vy = particle.vy + pushY * gain;
      const limit = Math.min(1, 175 / (Math.hypot(vx, vy) || 1));
      // Ease the sweep near a region boundary to keep the hit visible before wrapping.
      particle.vx = clamp(vx * limit, (region.x - particle.x) * 1.65,
        (region.x + region.width - particle.x) * 1.65);
      particle.vy = clamp(vy * limit, (region.y - particle.y) * 1.65,
        (region.y + region.height - particle.y) * 1.65);
      particle.spin = clamp(particle.spin + rotation * gain, -4, 4);
      const lift = clamp(1 - separation / (radius * .68), 0, 1);
      if (contained) particle.liftTarget = Math.max(particle.liftTarget, lift * (.25 + .75 * sweep));
      else particle.lift = Math.max(particle.lift, lift);
    });
  }

  function leavePointer() {
    pointer.present = false;
  }

  function setMotion(playing, prefersReducedMotion) {
    active = playing && !prefersReducedMotion;
    reduced = prefersReducedMotion;
    cancelAnimationFrame(frame);
    frame = 0;
    lastFrame = 0;
    pointer.present = false;
    if (reduced) draw(0);
    if (active) frame = requestAnimationFrame(tick);
  }

  const resizeObserver = new ResizeObserver(queueRefresh);
  resizeObserver.observe(canvas);
  const layoutObserver = new MutationObserver(queueRefresh);
  if (!contained) {
    layoutObserver.observe(document.getElementById("app"), {
      childList: true, subtree: true, attributes: true, attributeFilter: ["class"],
    });
  }
  layoutObserver.observe(document.documentElement, {
    attributes: true, attributeFilter: ["data-theme"],
  });
  window.addEventListener("pointermove", movePointer, { passive: true });
  window.addEventListener("scroll", handleLayoutScroll, { capture: true, passive: true });
  document.addEventListener("pointerleave", leavePointer);
  if (!contained) {
    document.addEventListener("animationend", layoutSettled);
    document.addEventListener("transitionend", layoutSettled);
  }
  window.addEventListener("blur", leavePointer);
  refresh();

  return {
    setMotion,
    setQuiet(value) {
      quiet = Boolean(value);
      pointer.present = false;
    },
    destroy() {
      destroyed = true;
      active = false;
      cancelAnimationFrame(frame);
      cancelAnimationFrame(refreshFrame);
      resizeObserver.disconnect();
      layoutObserver.disconnect();
      window.removeEventListener("pointermove", movePointer);
      window.removeEventListener("scroll", handleLayoutScroll, true);
      document.removeEventListener("pointerleave", leavePointer);
      document.removeEventListener("animationend", layoutSettled);
      document.removeEventListener("transitionend", layoutSettled);
      window.removeEventListener("blur", leavePointer);
      context.clearRect(0, 0, width, height);
    },
  };
}
