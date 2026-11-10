import { readingTokens, readingParagraphs } from "@/reading-context";
import { ElMessage } from "element-plus";
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from "vue";
import { learningApi } from "@/api/modules";
import {
  splitArticleTopics,
  newsTopicLabel,
  curatedNewsTopics,
} from "../reading/articlePresentation";

export function useReading({
  router,
  loadReport,
  loadWordBankStats,
  isChatBusy,
  discussReading,
}) {
  const state = reactive({
    newsTopics: [],
    newsList: [],
    newsDifficulty: "",
    newsTopic: "",
    currentArticle: null,
    articleBusy: false,
    readingBusy: false,
    readerProgress: 0,
    progressTimer: null,
    lookupCache: {},
    contextCache: {},
    savedOriginals: {},
    tooltipTimer: null,
    tooltip: {
      visible: false,
      loading: false,
      saving: false,
      contextBusy: false,
      context: null,
      sentence: "",
      contextKey: "",
      contextSaved: false,
      lookupKey: "",
      x: 0,
      y: 0,
      data: {},
    },
    translateBusy: false,
    translation: {},
  });

  const readerRef = ref(null);

  const tooltipRef = ref(null);

  const newsLibraryOpen = ref(false);

  const newsLibraryButtonRef = ref(null);

  const newsLibraryCloseRef = ref(null);

  const showAllNewsTopics = ref(false);

  const tooltipHasDefinition = computed(() => {
    const meaning = state.tooltip.data.meaningCn || "";
    return Boolean(meaning.trim()) && !/未收录|查询中|查询失败/.test(meaning);
  });

  const newsTopicOptions = computed(() => {
    const topics = new Map();
    curatedNewsTopics.forEach((topic) => topics.set(topic.value, topic));
    (state.newsTopics || []).flatMap(splitArticleTopics).forEach((topic) => {
      if (!topics.has(topic)) {
        topics.set(topic, { value: topic, label: newsTopicLabel(topic) });
      }
    });
    return Array.from(topics.values());
  });

  const visibleNewsTopics = computed(() => {
    if (showAllNewsTopics.value) return newsTopicOptions.value;
    const topics = newsTopicOptions.value.slice(0, 8);
    const selected = newsTopicOptions.value.find(
      (topic) => topic.value === state.newsTopic,
    );
    if (selected && !topics.includes(selected)) topics.push(selected);
    return topics;
  });

  const readerProgress = computed(
    () => state.currentArticle?.readProgress ?? state.readerProgress ?? 0,
  );

  const articleTokens = computed(() =>
    readingTokens(state.currentArticle?.content || ""),
  );

  const articleParagraphs = computed(() =>
    readingParagraphs(state.currentArticle?.content || ""),
  );

  const readerFocus = ref(false);

  const focusedSentence = ref("");

  function toggleReaderFocus() {
    readerFocus.value = !readerFocus.value;
    focusedSentence.value = readerFocus.value
      ? articleTokens.value.find((token) => token.word)?.sentence || ""
      : "";
  }

  function dimReaderToken(token) {
    return Boolean(
      readerFocus.value &&
      focusedSentence.value &&
      token.sentence &&
      token.sentence !== focusedSentence.value,
    );
  }

  function focusReaderWord(token, event) {
    if (readerFocus.value) focusedSentence.value = token.sentence;
    showTooltip(token.text, event);
  }

  const articleWordCount = computed(
    () =>
      (state.currentArticle?.content?.match(/[A-Za-z][A-Za-z'-]*/g) || [])
        .length,
  );

  async function setNewsDifficulty(value) {
    if (state.newsDifficulty === value) return;
    state.newsDifficulty = value;
    await loadNews();
  }

  async function setNewsTopic(value) {
    if (state.newsTopic === value) return;
    state.newsTopic = value;
    await loadNews();
  }

  async function loadNewsTopics() {
    state.newsTopics = await learningApi.newsTopics();
  }

  let newsRequestId = 0;

  async function loadNews() {
    const requestId = ++newsRequestId;
    const articles = await learningApi.newsRecommendations({
      difficulty: state.newsDifficulty,
      topic: state.newsTopic,
    });
    if (requestId !== newsRequestId) return;
    state.newsList = articles;
    const currentVisible = state.newsList.some(
      (article) => article.id === state.currentArticle?.id,
    );
    if (!state.newsList.length) {
      ++articleRequestId;
      clearTimeout(state.progressTimer);
      state.tooltip.visible = false;
      state.articleBusy = false;
      state.currentArticle = null;
      state.translation = {};
      state.readerProgress = 0;
      return;
    }
    if (!currentVisible) {
      await loadArticle(state.newsList[0].id);
    }
  }

  let articleRequestId = 0;

  async function loadArticle(id) {
    const requestId = ++articleRequestId;
    clearTimeout(state.progressTimer);
    state.tooltip.visible = false;
    state.articleBusy = true;
    try {
      const article = await learningApi.article(id);
      if (requestId !== articleRequestId) return;
      state.currentArticle = article;
      newsLibraryOpen.value = false;
      state.translation = {};
      state.readerProgress = article.readProgress || 0;
      await nextTick();
      if (readerRef.value) readerRef.value.scrollTop = 0;
    } catch (error) {
      if (requestId === articleRequestId)
        ElMessage.error(error.message || "文章加载失败，请重试");
    } finally {
      if (requestId === articleRequestId) state.articleBusy = false;
    }
  }

  function calculateReaderProgress() {
    const el = readerRef.value;
    if (!el || !state.currentArticle) return state.readerProgress;
    const max = el.scrollHeight - el.clientHeight;
    if (max <= 0) return state.readerProgress;
    return Math.min(
      100,
      Math.max(state.readerProgress, Math.round((el.scrollTop / max) * 100)),
    );
  }

  function handleReaderScroll() {
    if (!state.currentArticle || state.articleBusy) return;
    const nextProgress = calculateReaderProgress();
    state.readerProgress = nextProgress;
    clearTimeout(state.progressTimer);
    const articleId = state.currentArticle.id;
    state.progressTimer = setTimeout(
      () =>
        updateReadingProgress(nextProgress, false, articleId).catch(() => {}),
      500,
    );
  }

  async function updateReadingProgress(
    progress,
    finished,
    articleId = state.currentArticle?.id,
  ) {
    if (!articleId) return;
    const result = await learningApi.articleProgress(articleId, {
      readProgress: progress,
      finished,
    });
    if (state.currentArticle?.id === articleId) {
      state.currentArticle.readProgress = result.readProgress;
      state.currentArticle.finished = result.finished;
      state.readerProgress = result.readProgress;
    }
    const listed = state.newsList.find((article) => article.id === articleId);
    if (listed)
      Object.assign(listed, {
        readProgress: result.readProgress,
        finished: result.finished,
      });
    await loadReport();
  }

  async function finishReading() {
    if (!state.currentArticle || state.readingBusy) return;
    clearTimeout(state.progressTimer);
    state.readingBusy = true;
    try {
      await updateReadingProgress(100, true);
      ElMessage.success("本篇阅读已完成");
    } catch (error) {
      ElMessage.error(error.message || "阅读进度保存失败，请重试");
    } finally {
      state.readingBusy = false;
    }
  }

  async function translateArticle() {
    if (!state.currentArticle || state.translateBusy) return;
    if (state.translation.contentCn || state.translation.titleCn) {
      state.translation = {};
      return;
    }
    const articleId = state.currentArticle.id;
    state.translateBusy = true;
    try {
      const translation = await learningApi.translateArticle({
        title: state.currentArticle.title,
        content: state.currentArticle.content,
      });
      if (state.currentArticle?.id === articleId) {
        state.translation = translation;
        await nextTick();
        readerRef.value
          ?.querySelector(".translation-area")
          ?.scrollIntoView({ behavior: "smooth", block: "nearest" });
      }
    } catch (error) {
      if (state.currentArticle?.id === articleId)
        ElMessage.error(error.message || "翻译失败，请稍后重试");
    } finally {
      state.translateBusy = false;
    }
  }

  const lookupStopWords = new Set([
    "a",
    "an",
    "the",
    "and",
    "or",
    "but",
    "to",
    "of",
    "in",
    "on",
    "at",
    "by",
    "for",
    "with",
    "is",
    "are",
    "was",
    "were",
    "be",
    "been",
    "it",
    "its",
    "they",
    "them",
    "this",
    "that",
  ]);

  function normalizeLookupWord(word) {
    return String(word || "")
      .toLowerCase()
      .replace(/^[^a-z]+|[^a-z]+$/g, "");
  }

  function placeTooltip(event) {
    if (event?.currentTarget) {
      const rect = event.currentTarget.getBoundingClientRect();
      state.tooltip.anchor = {
        left: rect.left,
        top: rect.top,
        bottom: rect.bottom,
      };
    }
    const rect = state.tooltip.anchor;
    if (!rect) return;
    const width = tooltipRef.value?.offsetWidth || 340;
    const height = tooltipRef.value?.offsetHeight || 260;
    const margin = 14;
    const x = rect.left - 20;
    const y =
      rect.bottom + height + margin > window.innerHeight
        ? rect.top - height - 10
        : rect.bottom + 10;
    state.tooltip.x = Math.max(
      margin,
      Math.min(x, window.innerWidth - width - margin),
    );
    state.tooltip.y = Math.max(
      margin,
      Math.min(y, window.innerHeight - height - margin),
    );
  }

  async function showTooltip(word, event) {
    if (state.tooltip.saving) return;
    const normalized = normalizeLookupWord(word);
    if (!normalized || lookupStopWords.has(normalized)) return;
    cancelTooltipHide();
    placeTooltip(event);
    const sentence = event?.currentTarget?.dataset?.sentence || "";
    const contextKey = `${state.currentArticle?.id}:${normalized}:${sentence}`;
    state.tooltip.contextKey = contextKey;
    state.tooltip.contextSaved = Boolean(state.savedOriginals[contextKey]);
    state.tooltip.sentence = sentence;
    state.tooltip.context = state.contextCache[contextKey] || null;
    state.tooltip.contextBusy = false;
    state.tooltip.visible = true;
    state.tooltip.lookupKey = normalized;
    state.tooltip.loading = !state.lookupCache[normalized];
    state.tooltip.data = state.lookupCache[normalized] || {
      word: normalized,
      meaningCn: "查询中...",
    };
    nextTick(() => placeTooltip());
    if (state.lookupCache[normalized]) {
      return;
    }
    try {
      const result = await learningApi.lookupWord(
        normalized,
        state.currentArticle?.id,
      );
      state.lookupCache[normalized] = result;
      if (state.tooltip.visible && state.tooltip.lookupKey === normalized) {
        state.tooltip.data = result;
      }
    } catch (error) {
      if (state.tooltip.visible && state.tooltip.lookupKey === normalized) {
        state.tooltip.data = {
          word: normalized,
          meaningCn: "查询失败，请稍后重试",
        };
      }
    } finally {
      if (state.tooltip.lookupKey === normalized) {
        state.tooltip.loading = false;
        await nextTick();
        placeTooltip();
      }
    }
  }

  async function focusLookupActions(word, event) {
    if (lookupStopWords.has(normalizeLookupWord(word))) return;
    await showTooltip(word, event);
    await nextTick();
    tooltipRef.value?.querySelector("button:not(:disabled)")?.focus();
  }

  function scheduleHideTooltip() {
    clearTimeout(state.tooltipTimer);
    if (
      state.tooltip.contextBusy ||
      state.tooltip.saving ||
      tooltipRef.value?.matches(":hover") ||
      tooltipRef.value?.contains(document.activeElement)
    )
      return;
    state.tooltipTimer = setTimeout(() => {
      state.tooltip.visible = false;
    }, 240);
  }

  function cancelTooltipHide() {
    clearTimeout(state.tooltipTimer);
  }

  function handleTooltipFocusOut(event) {
    if (!event.currentTarget.contains(event.relatedTarget))
      scheduleHideTooltip();
  }

  function openPendingLibrary() {
    state.tooltip.visible = false;
    router.push({ path: "/wordbank", query: { scope: "review" } });
  }

  async function saveReaderWord(word) {
    if (
      !word ||
      state.tooltip.loading ||
      state.tooltip.saving ||
      !tooltipHasDefinition.value
    )
      return;
    if (
      state.tooltip.data.savedInReviewBook &&
      (!state.tooltip.sentence || state.tooltip.contextSaved)
    )
      return;
    const normalized = normalizeLookupWord(word);
    const lookupKey = state.tooltip.lookupKey;
    state.tooltip.saving = true;
    try {
      const result =
        state.currentArticle?.id && state.tooltip.sentence
          ? await learningApi.saveReadingWord(state.currentArticle.id, {
              word: state.tooltip.lookupKey,
              sentence: state.tooltip.sentence,
            })
          : await learningApi.saveDictionaryWord(word);
      for (const [key, cached] of Object.entries(state.lookupCache)) {
        if (normalizeLookupWord(cached.word) === normalized)
          state.lookupCache[key] = result;
      }
      state.lookupCache[normalized] = result;
      if (state.tooltip.lookupKey === lookupKey) {
        state.tooltip.data = result;
        if (state.tooltip.sentence) {
          state.savedOriginals[state.tooltip.contextKey] = true;
          state.tooltip.contextSaved = true;
        }
      }
      ElMessage.success(
        state.tooltip.sentence ? "已保留原句，加入待复习" : "已加入待复习",
      );
      await Promise.allSettled([loadReport(), loadWordBankStats()]);
    } catch (error) {
      ElMessage.error(error.message || "加入待复习失败，请重试");
    } finally {
      state.tooltip.saving = false;
      await nextTick();
      placeTooltip();
    }
  }

  async function explainReaderWord() {
    if (
      state.tooltip.contextBusy ||
      !state.tooltip.sentence ||
      !state.currentArticle
    )
      return;
    const key = state.tooltip.contextKey;
    state.tooltip.contextBusy = true;
    cancelTooltipHide();
    try {
      const result = await learningApi.explainReadingWord(
        state.currentArticle.id,
        { word: state.tooltip.lookupKey, sentence: state.tooltip.sentence },
      );
      state.contextCache[key] = result;
      if (state.tooltip.contextKey === key) state.tooltip.context = result;
    } catch (error) {
      ElMessage.error(error.message || "语境解读失败，请重试");
    } finally {
      if (state.tooltip.contextKey === key) {
        state.tooltip.contextBusy = false;
        await nextTick();
        placeTooltip();
      }
    }
  }

  async function discussArticle() {
    if (!state.currentArticle || isChatBusy()) return;
    state.tooltip.visible = false;
    await discussReading(state.currentArticle);
  }

  async function returnToArticle(id) {
    if (!id) return;
    await router.push("/news");
    await loadArticle(id);
  }

  watch(
    () => state.currentArticle?.id,
    () => {
      focusedSentence.value = readerFocus.value
        ? articleTokens.value.find((token) => token.word)?.sentence || ""
        : "";
    },
  );

  watch(newsLibraryOpen, async (open) => {
    await nextTick();
    (open ? newsLibraryCloseRef.value : newsLibraryButtonRef.value)?.focus();
  });

  onBeforeUnmount(() => {
    clearTimeout(state.tooltipTimer);
    clearTimeout(state.progressTimer);
  });

  return {
    state,
    readerRef,
    tooltipRef,
    newsLibraryOpen,
    newsLibraryButtonRef,
    newsLibraryCloseRef,
    showAllNewsTopics,
    tooltipHasDefinition,
    newsTopicOptions,
    visibleNewsTopics,
    readerProgress,
    articleParagraphs,
    readerFocus,
    toggleReaderFocus,
    dimReaderToken,
    focusReaderWord,
    articleWordCount,
    setNewsDifficulty,
    setNewsTopic,
    loadNewsTopics,
    loadNews,
    loadArticle,
    handleReaderScroll,
    finishReading,
    translateArticle,
    normalizeLookupWord,
    showTooltip,
    focusLookupActions,
    scheduleHideTooltip,
    cancelTooltipHide,
    handleTooltipFocusOut,
    openPendingLibrary,
    saveReaderWord,
    explainReaderWord,
    discussArticle,
    returnToArticle,
  };
}
