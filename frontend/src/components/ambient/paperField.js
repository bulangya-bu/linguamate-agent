const TAU = Math.PI * 2;
const MAX_PARTICLES = 48;

const clamp = (value, min, max) => Math.max(min, Math.min(max, value));
const seed = (index) => {
  const value = Math.sin(index * 127.1 + 311.7) * 43758.5453;
  return value - Math.floor(value);
};

function createParticle(index) {
  return {
    index,
    region: null,
    x: 0,
    y: 0,
    vx: 0,
    vy: 0,
    angle: seed(index + 10) * TAU,
    spin: 0,
    phase: seed(index + 20) * TAU,
    speed: 4 + seed(index + 30) * 6,
    turn: (seed(index + 40) - .5) * .28,
    width: 5.5 + seed(index + 50) * 4,
    height: 4 + seed(index + 60) * 2.5,
    opacity: .38 + seed(index + 70) * .18,
    color: index % 7 === 0 ? 2 : index % 2,
    dot: index % 4 === 0,
    reveal: 0,
  };
}

// A canvas owns the free-moving pieces; Vue controls visibility and lifetime.
export function createPaperField(canvas, { contained }) {
  const context = canvas.getContext("2d");
  if (!context) return { setMotion() {}, destroy() {} };

  let bounds;
  let regions = [];
  let palette = [];
  let layoutKey = "";
  let width = 0;
  let height = 0;
  let active = false;
  let reduced = false;
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

  function readLayout() {
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
    const quiz = localRect(document.querySelector(".page-vocabulary #quiz-practice"));
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
    addSpace("top", titleRight, bar.y + bar.height + 7, bar.x + bar.width - 150 - titleRight, 54);
    if (nav) {
      addSpace("left-edge", nav.x + nav.width + 3, height * .23,
        bar.x - nav.x - nav.width - 6, height * .65);
    }
    addSpace("right-edge", bar.x + bar.width + 4, height * .22,
      width - bar.x - bar.width - 8, height * .65);
    if (quiz) {
      addSpace("quiz-left", bar.x + 12, quiz.y + 32,
        quiz.x - bar.x - 30, quiz.height - 64);
      addSpace("quiz-right", quiz.x + quiz.width + 18, quiz.y + 32,
        bar.x + bar.width - quiz.x - quiz.width - 30, quiz.height - 64);
    }
    return spaces;
  }

  function updateParticles() {
    const area = regions.reduce((total, region) => total + region.width * region.height, 0);
    const count = area ? clamp(Math.round(area / 4800), 6, MAX_PARTICLES) : 0;
    particles.length = Math.min(particles.length, count);
    while (particles.length < count) particles.push(createParticle(particles.length));

    particles.forEach((particle) => {
      const previous = particle.region;
      let remaining = seed(particle.index + 90) * area;
      const region = regions.find((item) => {
        remaining -= item.width * item.height;
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
    const layout = readLayout();
    const nextKey = JSON.stringify(layout);
    if (nextKey !== layoutKey) {
      layoutKey = nextKey;
      regions = layout;
      updateParticles();
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

  function draw(delta) {
    context.clearRect(0, 0, width, height);
    const drag = Math.exp(-delta * 1.8);
    const spinDrag = Math.exp(-delta * 2.2);
    particles.forEach((particle) => {
      const region = particle.region;
      const heading = particle.phase + Math.sin(time * .11 + particle.phase) * .7;
      const flowX = Math.cos(heading) * particle.speed;
      const flowY = Math.sin(heading * .83 + .8) * particle.speed * .8;
      particle.vx *= drag;
      particle.vy *= drag;
      particle.spin *= spinDrag;
      particle.x += (flowX + particle.vx) * delta;
      particle.y += (flowY + particle.vy) * delta;
      particle.angle += (particle.turn + particle.spin) * delta;
      particle.reveal = reduced ? 1 : Math.min(1, particle.reveal + delta * 1.5);

      // Pieces fade at the boundary before re-entering from the opposite side.
      particle.x = region.x + ((particle.x - region.x) % region.width + region.width) % region.width;
      particle.y = region.y + ((particle.y - region.y) % region.height + region.height) % region.height;
      const edge = Math.min(particle.x - region.x, region.x + region.width - particle.x,
        particle.y - region.y, region.y + region.height - particle.y);
      const visibility = clamp(edge / 8, 0, 1) * particle.reveal;
      if (!visibility) return;

      context.save();
      context.translate(particle.x, particle.y);
      context.rotate(particle.angle);
      context.fillStyle = palette[particle.color];
      context.strokeStyle = palette[particle.color];
      context.globalAlpha = particle.opacity * visibility;
      context.beginPath();
      if (particle.dot) {
        context.arc(0, 0, particle.width * .22, 0, TAU);
      } else {
        context.roundRect(-particle.width / 2, -particle.height / 2,
          particle.width, particle.height, 1.5);
      }
      context.fill();
      if (!particle.dot) {
        context.globalAlpha *= .45;
        context.lineWidth = .65;
        context.stroke();
      }
      context.restore();
    });
  }

  function tick(now) {
    if (!active) return;
    frame = requestAnimationFrame(tick);
    if (lastFrame && now - lastFrame < 30) return;
    const delta = lastFrame ? Math.min((now - lastFrame) / 1000, .05) : 1 / 30;
    lastFrame = now;
    time += delta;
    draw(delta);
  }

  function movePointer(event) {
    if (!active || event.pointerType === "touch" || !bounds) return;
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

    const start = velocitySample;
    const dx = x - start.x;
    const dy = y - start.y;
    const distance = Math.hypot(dx, dy);
    if (distance < .5) return;
    velocitySample = { x, y, at: now };
    const speed = distance / sampleDuration * 1000;
    const radius = 58 + Math.min(speed / 30, 36);
    const directionX = dx / distance;
    const directionY = dy / distance;

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
      const steps = Math.ceil((to - from) / 6);
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
        pushX += (normalX * .65 + directionX * .45) * weight;
        pushY += (normalY * .65 + directionY * .45) * weight;
        rotation += side * weight * .025;
      }
      particle.vx = clamp(particle.vx + pushX, -160, 160);
      particle.vy = clamp(particle.vy + pushY, -160, 160);
      particle.spin = clamp(particle.spin + rotation, -3, 3);
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
  layoutObserver.observe(document.getElementById("app"), {
    childList: true, subtree: true, attributes: true, attributeFilter: ["class"],
  });
  layoutObserver.observe(document.documentElement, {
    attributes: true, attributeFilter: ["data-theme"],
  });
  window.addEventListener("pointermove", movePointer, { passive: true });
  document.addEventListener("pointerleave", leavePointer);
  document.addEventListener("animationend", layoutSettled);
  document.addEventListener("transitionend", layoutSettled);
  window.addEventListener("blur", leavePointer);
  refresh();

  return {
    setMotion,
    destroy() {
      destroyed = true;
      active = false;
      cancelAnimationFrame(frame);
      cancelAnimationFrame(refreshFrame);
      resizeObserver.disconnect();
      layoutObserver.disconnect();
      window.removeEventListener("pointermove", movePointer);
      document.removeEventListener("pointerleave", leavePointer);
      document.removeEventListener("animationend", layoutSettled);
      document.removeEventListener("transitionend", layoutSettled);
      window.removeEventListener("blur", leavePointer);
      context.clearRect(0, 0, width, height);
    },
  };
}
