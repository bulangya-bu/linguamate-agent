import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const view = readFileSync(new URL("../src/views/MainAppView.vue", import.meta.url), "utf8");
const css = readFileSync(new URL("../src/styles/app.css", import.meta.url), "utf8");

test("today route uses a focused Duolingo-style lesson path", () => {
  assert.match(view, /class="duo-route-shell"/);
  assert.match(view, /class="duo-route-progress"/);
  assert.match(view, /class="duo-step-button"/);
  assert.match(view, /class="duo-quest-panel"/);
  assert.doesNotMatch(view, /class="adventure-map-bg"/);
});

test("lesson buttons expose their destination and state to assistive technology", () => {
  assert.match(view, /:aria-label="routeStepAriaLabel\(step\)"/);
  assert.match(view, /function routeStepAriaLabel\(step\)/);
});

test("today route includes compact and mobile layout rules", () => {
  assert.match(css, /\.duo-step-button:active/);
  assert.match(css, /@media \(max-width: 760px\)[\s\S]*\.duo-route-track/);
  assert.match(css, /@media \(prefers-reduced-motion: reduce\)[\s\S]*\.duo-step-button/);
});
