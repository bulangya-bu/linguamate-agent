import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

function dashboardSource(file) {
  return readFileSync(new URL(`../src/features/dashboard/${file}`, import.meta.url), "utf8");
}

const view = dashboardSource("DashboardOverview.vue");
const nextStep = dashboardSource("DashboardNextStep.vue");
const today = dashboardSource("DashboardToday.vue");
const art = dashboardSource("DashboardStudyArt.vue");
const dashboard = dashboardSource("useDashboard.js");

test("the overview prioritizes the next action and puts study history below daily words", () => {
  assert.match(view, /<DashboardNextStep\b[^>]*:next-action="nextAction"[^>]*@start="openStudyAction\(nextAction\)"/);
  assert.match(view, /<DashboardToday\b[^>]*:tasks="todayTasks"[^>]*@start="openStudyAction"/);
  assert.doesNotMatch(view, /<DashboardToday\b[^>]*:next-action/);
  assert.match(view, /id="dashboard-words"[^>]*>[\s\S]*?<\/section>\s*<DashboardFootprint\b[^>]*:activity="recentStudy"/);
  assert.doesNotMatch(view, /专题练习|今日主题路线|开启今日主题/);
});

test("today's checklist exposes task status and distinguishes unavailable progress", () => {
  assert.match(today, /aria-labelledby="study-todo-title"[^>]*:aria-busy="loading"/);
  assert.match(today, /<ul v-if="tasks"[^>]*aria-label="今日学习任务"/);
  assert.match(today, /v-for="task in tasks"[\s\S]*?:data-status="task.status"/);
  assert.match(today, /done: "已完成", skipped: "无需复习"/);
  assert.match(today, /<button\b[^>]*v-if="task.status === 'pending'"[^>]*@click="\$emit\('start', task.id\)"/);
  assert.match(today, /<svg\b[^>]*v-if="task.status === 'done'"[^>]*class="study-todo-strike"[^>]*aria-hidden="true"/);
  assert.match(today, /role="status"[\s\S]*?今日进度暂时无法读取/);
  assert.doesNotMatch(today, /查看学习档案/);
});

test("daily goals use their corresponding journal activities in checklist order", () => {
  const goals = [...dashboard.matchAll(/id: "([^"]+)", title: "[^"]+", field: "([^"]+)", target: (\d+)/g)];
  assert.deepEqual(goals.map(([, id, field, target]) => [id, field, Number(target)]), [
    ["new-words", "newWords", 3],
    ["chat", "aiChatTurns", 1],
    ["writing", "aiEssayReviews", 1],
    ["reading", "readingsFinished", 1],
  ]);
  assert.match(dashboard, /id: "review"[\s\S]*?\.\.\.STUDY_GOALS\.map/);
});

test("overview controls keep accessible names and decorative animations respect reduced motion", () => {
  assert.match(nextStep, /<button\b[^>]*type="button"[^>]*:disabled="busy"/);
  assert.match(view, /:aria-label="word\.word \+ '，查看单词详情'"/);
  assert.match(art, /aria-hidden="true"/);
  assert.match(art, /@media \(prefers-reduced-motion: reduce\)[\s\S]*?animation: none/);
  assert.match(view, /@media \(prefers-reduced-motion: reduce\)[\s\S]*?animation: none/);
  assert.match(today, /@media \(prefers-reduced-motion: reduce\)[\s\S]*?\.study-todo-strike\s*\{\s*animation: none/);
});
