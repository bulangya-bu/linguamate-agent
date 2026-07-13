import { createRouter, createWebHistory } from "vue-router";
import LoginView from "@/views/LoginView.vue";
import MainAppView from "@/views/MainAppView.vue";
import AdminView from "@/views/AdminView.vue";

const routes = [
  { path: "/login", name: "login", component: LoginView, meta: { mode: "login" } },
  { path: "/register", name: "register", component: LoginView, meta: { mode: "register" } },
  { path: "/admin", name: "admin", component: AdminView },
  { path: "/", name: "dashboard", component: MainAppView, meta: { section: "dashboard" } },
  { path: "/chat", name: "chat", component: MainAppView, meta: { section: "chat" } },
  { path: "/vocabulary", name: "vocabulary", component: MainAppView, meta: { section: "vocabulary" } },
  { path: "/review", name: "review", component: MainAppView, meta: { section: "review" } },
  { path: "/writing", name: "writing", component: MainAppView, meta: { section: "writing" } },
  { path: "/news", name: "news", component: MainAppView, meta: { section: "news" } },
  { path: "/report", name: "report", component: MainAppView, meta: { section: "dashboard" } },
  { path: "/wordbank", name: "wordbank", component: MainAppView, meta: { section: "wordbank" } },
  { path: "/:pathMatch(.*)*", redirect: "/" }
];

export default createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 };
  }
});
