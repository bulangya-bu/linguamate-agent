import { api, query } from "./client";

export const authApi = {
  me: (config) => api("/api/auth/me", {}, config),
  login: (payload) => api("/api/auth/login", { method: "POST", body: JSON.stringify(payload) }),
  register: (payload) => api("/api/auth/register", { method: "POST", body: JSON.stringify(payload) }),
  logout: () => api("/api/auth/logout", { method: "POST" })
};

export const aiApi = {
  settings: () => api("/api/ai/settings"),
  saveSettings: (payload) => api("/api/ai/settings", { method: "PUT", body: JSON.stringify(payload) }),
  models: (payload) => api("/api/ai/models", { method: "POST", body: JSON.stringify(payload) })
};

export const learningApi = {
  report: () => api("/api/report/summary"),
  level: () => api("/api/experience"),
  conversations: () => api("/api/conversations"),
  messages: (id) => api(`/api/conversations/${id}/messages`),
  chat: (payload) => api("/api/chat", { method: "POST", body: JSON.stringify(payload) }),
  advisorVocabulary: (params) => api(`/api/learning-advisor/vocabulary${query(params)}`),
  quizCategories: () => api("/api/word-bank/categories"),
  todayWords: (params) => api(`/api/words/today${query(params)}`),
  quiz: (id, payload) => api(`/api/words/${id}/quiz`, { method: "POST", body: JSON.stringify(payload) }),
  wordAiDetail: (id) => api(`/api/words/${id}/ai-detail`),
  toggleFavorite: (id) => api(`/api/words/${id}/favorite`, { method: "POST" }),
  reviews: (mode, limit = 8) => api(`/api/reviews/${mode}${query({ limit })}`),
  submitReview: (id, payload) => api(`/api/reviews/${id}`, { method: "POST", body: JSON.stringify(payload) }),
  newsTopics: () => api("/api/news/topics"),
  newsRecommendations: (params) => api(`/api/news/recommendations${query(params)}`),
  article: (id) => api(`/api/news/${id}`),
  articleProgress: (id, payload) => api(`/api/news/${id}/progress`, { method: "POST", body: JSON.stringify(payload) }),
  lookupWord: (word, articleId) => {
    const base = articleId ? `/api/news/${articleId}/lookup` : "/api/news/lookup";
    return api(`${base}${query({ word })}`);
  },
  saveDictionaryWord: (word) => api(`/api/dictionary/save${query({ word })}`, { method: "POST" }),
  translateArticle: (payload) => api("/api/news/translate", { method: "POST", body: JSON.stringify(payload) }),
  wordBankCategories: () => api("/api/word-bank/categories"),
  wordBankWords: (params) => api(`/api/word-bank/words${query(params)}`),
  writingReview: (payload) => api("/api/writing/review", { method: "POST", body: JSON.stringify(payload) })
};

export const adminApi = {
  summary: () => api("/api/admin/summary"),
  users: (search) => api(`/api/admin/users${query({ search })}`),
  userProfile: (id) => api(`/api/admin/users/${id}/profile`),
  userReadings: (id) => api(`/api/admin/users/${id}/readings`),
  updateUser: (id, payload) => api(`/api/admin/users/${id}`, { method: "PUT", body: JSON.stringify(payload) }),
  updateUserStatus: (id, status) => api(`/api/admin/users/${id}/status`, { method: "POST", body: JSON.stringify({ status }) }),
  resetPassword: (id, password) => api(`/api/admin/users/${id}/reset-password`, { method: "POST", body: JSON.stringify({ password }) }),
  words: (params) => api(`/api/admin/words${query(params)}`),
  createWord: (payload) => api("/api/admin/words", { method: "POST", body: JSON.stringify(payload) }),
  updateWord: (id, payload) => api(`/api/admin/words/${id}`, { method: "PUT", body: JSON.stringify(payload) }),
  deleteWord: (id) => api(`/api/admin/words/${id}`, { method: "DELETE" }),
  knowledge: () => api("/api/knowledge"),
  uploadKnowledge: (payload) => api("/api/knowledge", { method: "POST", body: JSON.stringify(payload) }),
  news: (params) => api(`/api/admin/news${query(params)}`),
  createNews: (payload) => api("/api/admin/news", { method: "POST", body: JSON.stringify(payload) }),
  updateNews: (id, payload) => api(`/api/admin/news/${id}`, { method: "PUT", body: JSON.stringify(payload) }),
  deleteNews: (id) => api(`/api/admin/news/${id}`, { method: "DELETE" })
};
