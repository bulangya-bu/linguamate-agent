import { api, query } from "./client";

export const authApi = {
  me: (config) => api("/api/auth/me", {}, config),
  login: (payload) =>
    api("/api/auth/login", { method: "POST", body: JSON.stringify(payload) }),
  register: (payload) =>
    api("/api/auth/register", {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  logout: () => api("/api/auth/logout", { method: "POST" }),
};

export const aiApi = {
  settings: () => api("/api/ai/settings"),
  test: (payload) =>
    api("/api/ai/test", { method: "POST", body: JSON.stringify(payload) }),
  saveSettings: (payload) =>
    api("/api/ai/settings", { method: "PUT", body: JSON.stringify(payload) }),
  models: (payload) =>
    api("/api/ai/models", { method: "POST", body: JSON.stringify(payload) }),
};

export const learningApi = {
  growth: () => api("/api/report/growth"),
  report: () => api("/api/report/summary"),
  journal: () => api("/api/report/journal"),
  level: () => api("/api/experience"),
  conversations: () => api("/api/conversations"),
  messages: (id) => api(`/api/conversations/${id}/messages`),
  chat: (payload) =>
    api("/api/chat", { method: "POST", body: JSON.stringify(payload) }),
  practiceContext: () => api("/api/chat/practice-context"),
  advisorVocabulary: (params) =>
    api(`/api/learning-advisor/vocabulary${query(params)}`),
  quizCategories: () => api("/api/word-bank/categories"),
  todayWords: (params) => api(`/api/words/today${query(params)}`),
  quiz: (id, payload) =>
    api(`/api/words/${id}/quiz`, {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  wordAiDetail: (id) => api(`/api/words/${id}/ai-detail`),
  toggleFavorite: (id) => api(`/api/words/${id}/favorite`, { method: "POST" }),
  reviews: (mode, limit = 8) => api(`/api/reviews/${mode}${query({ limit })}`),
  submitReview: (id, payload) =>
    api(`/api/reviews/${id}`, {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  newsTopics: () => api("/api/news/topics"),
  newsRecommendations: (params) =>
    api(`/api/news/recommendations${query(params)}`),
  article: (id) => api(`/api/news/${id}`),
  articleProgress: (id, payload) =>
    api(`/api/news/${id}/progress`, {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  lookupWord: (word, articleId) => {
    const base = articleId
      ? `/api/news/${articleId}/lookup`
      : "/api/news/lookup";
    return api(`${base}${query({ word })}`);
  },
  saveDictionaryWord: (word) =>
    api(`/api/dictionary/save${query({ word })}`, { method: "POST" }),
  explainReadingWord: (id, payload) =>
    api(`/api/news/${id}/explain`, {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  saveReadingWord: (id, payload) =>
    api(`/api/news/${id}/save-word`, {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  writingHistory: () => api("/api/writing/history"),
  translateArticle: (payload) =>
    api("/api/news/translate", {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  wordBankCategories: () => api("/api/word-bank/categories"),
  wordBankStats: () => api("/api/word-bank/stats"),
  wordBankWords: (params) => api(`/api/word-bank/words${query(params)}`),
  writingReview: (payload) =>
    api("/api/writing/review", {
      method: "POST",
      body: JSON.stringify(payload),
    }),
};

const missionPost = (path, payload = {}) =>
  api(`/api/missions/${path}`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
export const missionApi = {
  today: () => api("/api/missions/today"),
  start: (key) => missionPost(`${key}/start`),
  get: (id) => api(`/api/missions/sessions/${id}`),
  update: (id, action, payload) =>
    missionPost(`sessions/${id}/${action}`, payload),
};

export const adminApi = {
  summary: () => api("/api/admin/summary"),
  users: (search) => api(`/api/admin/users${query({ search })}`),
  userProfile: (id) => api(`/api/admin/users/${id}/profile`),
  userReadings: (id) => api(`/api/admin/users/${id}/readings`),
  updateUser: (id, payload) =>
    api(`/api/admin/users/${id}`, {
      method: "PUT",
      body: JSON.stringify(payload),
    }),
  updateUserStatus: (id, status) =>
    api(`/api/admin/users/${id}/status`, {
      method: "POST",
      body: JSON.stringify({ status }),
    }),
  resetPassword: (id, password) =>
    api(`/api/admin/users/${id}/reset-password`, {
      method: "POST",
      body: JSON.stringify({ password }),
    }),
  words: (params) => api(`/api/admin/words${query(params)}`),
  createWord: (payload) =>
    api("/api/admin/words", { method: "POST", body: JSON.stringify(payload) }),
  updateWord: (id, payload) =>
    api(`/api/admin/words/${id}`, {
      method: "PUT",
      body: JSON.stringify(payload),
    }),
  deleteWord: (id) => api(`/api/admin/words/${id}`, { method: "DELETE" }),
  knowledge: () => api("/api/knowledge"),
  uploadKnowledge: (payload) =>
    api("/api/knowledge", { method: "POST", body: JSON.stringify(payload) }),
  news: (params) => api(`/api/admin/news${query(params)}`),
  createNews: (payload) =>
    api("/api/admin/news", { method: "POST", body: JSON.stringify(payload) }),
  updateNews: (id, payload) =>
    api(`/api/admin/news/${id}`, {
      method: "PUT",
      body: JSON.stringify(payload),
    }),
  deleteNews: (id) => api(`/api/admin/news/${id}`, { method: "DELETE" }),
};
