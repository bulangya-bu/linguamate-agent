import { readingTokens, readingParagraphs } from "@/reading-context";
import { ElMessage } from "element-plus";
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from "vue";
import { learningApi } from "@/api/modules";
import { usePhoneLayout } from "@/composables/usePhoneLayout";
import { createReadingPositionStore, visibleParagraph } from "./readingPosition";
import {
  splitArticleTopics,
  newsTopicLabel,
  curatedNewsTopics,
} from "../reading/articlePresentation";

export function useReading({
  router,
  getAccountId = () => null,
  loadReport,
  loadWordBankStats,
  isChatBusy,
  discussReading,
}) {
  const isPhone = usePhoneLayout();
  const phoneArticle = computed(() => {
    const id = Number(router.currentRoute.value.query.article);
    return Number.isSafeInteger(id) && id > 0 ? id : null;
  });
  const state = reactive({
    newsTopics: [],
    newsList: [],
    newsListStatus: "loading",
    newsDifficulty: "",
    newsTopic: "",
    currentArticle: null,
    articleBusy: false,
    articleError: "",
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
  const bookmark = ref(null);
  const activeWord = ref("");
  const positionStore = createReadingPositionStore(getAccountId);
  let programmaticScroll = null;
  let keyboardLookupAnchor = null;

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

  const readerProgress = computed(() => state.readerProgress);

  const articleTokens = computed(() =>
    readingTokens(state.currentArticle?.content || ""),
  );

  const articleParagraphs = computed(() =>
    readingParagraphs(state.currentArticle?.content || "").map((paragraph) => {
      const tail = paragraph.at(-1);
      if (!tail || tail.word) return paragraph;
      return [...paragraph.slice(0, -1), { ...tail, text: tail.text.trimEnd() }];
    }),
  );
  const readerWords = computed(() => articleTokens.value.filter(token => token.word && isLookupWord(token.text)));
  const activeReaderWord = computed(() => activeWord.value || readerWords.value[0]?.key);
  const canResumeReading = computed(() => {
    if (!state.currentArticle) return false;
    if (bookmark.value) return bookmark.value.paragraph > 0 || bookmark.value.offset > 0;
    return !state.currentArticle.finished && state.currentArticle.readProgress > 0;
  });

  const readerFocus = ref(false);

  const focusedSentence = ref("");

  function toggleReaderFocus() {
    readerFocus.value = !readerFocus.value;
    focusedSentence.value = readerFocus.value
      ? readerWords.value.find((token) => token.key === activeReaderWord.value)?.sentence || ""
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
    if (isPhone.value) return;
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

  async function loadNews(preferredArticle) {
    const requestId = ++newsRequestId;
    const initialArticleRequest = articleRequestId;
    const target = Number(preferredArticle);
    const hasTarget = Number.isSafeInteger(target) && target > 0;
    if (isPhone.value && hasTarget) {
      await loadArticle(target);
      return;
    }
    const requestedArticle = hasTarget ? loadArticle(target) : null;
    state.newsListStatus = "loading";
    let articles;
    try {
      articles = await learningApi.newsRecommendations({
        difficulty: state.newsDifficulty,
        topic: state.newsTopic,
      });
    } catch (error) {
      if (requestId === newsRequestId) state.newsListStatus = "error";
      if (!isPhone.value) throw error;
      return;
    }
    if (requestId !== newsRequestId || router.currentRoute.value.name !== "news") return;
    state.newsList = articles;
    state.newsListStatus = "ready";
    if (isPhone.value && !hasTarget) return;
    if (hasTarget) {
      await requestedArticle;
      return;
    }
    if (initialArticleRequest !== articleRequestId) return;
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
      bookmark.value = null;
      activeWord.value = "";
      return;
    }
    if (!currentVisible) {
      await loadArticle(state.newsList[0].id);
    } else {
      markVisibleArticleStarted(state.currentArticle.id);
    }
  }

  let articleRequestId = 0;
  const startedArticles = new Set();

  function markVisibleArticleStarted(id) {
    if (!articleVisible(id) || startedArticles.has(id)) return;
    startedArticles.add(id);
    learningApi.startArticle(id).catch(() => {
      startedArticles.delete(id);
    });
  }

  function articleVisible(id) {
    return router.currentRoute.value.name === "news" && (!isPhone.value || phoneArticle.value === Number(id));
  }

  function selectArticle(id) {
    return isPhone.value ? returnToArticle(id) : loadArticle(id);
  }

  async function loadArticle(id) {
    const requestId = ++articleRequestId;
    clearTimeout(state.progressTimer);
    state.tooltip.visible = false;
    keyboardLookupAnchor = null;
    state.articleBusy = true;
    state.articleError = "";
    if (isPhone.value && state.currentArticle?.id !== id) state.currentArticle = null;
    try {
      const article = await learningApi.article(id);
      if (requestId !== articleRequestId || !articleVisible(id)) return;
      state.currentArticle = article;
      newsLibraryOpen.value = false;
      state.translation = {};
      state.readerProgress = article.readProgress || 0;
      bookmark.value = positionStore.get(article.id);
      activeWord.value = "";
      await nextTick();
      if (requestId === articleRequestId && articleVisible(id)) {
        resumeReading();
        markVisibleArticleStarted(article.id);
      }
    } catch (error) {
      if (requestId === articleRequestId && articleVisible(id)) {
        state.articleError = error.message || "文章加载失败，请重试";
        ElMessage.error(state.articleError);
      }
    } finally {
      if (requestId === articleRequestId) state.articleBusy = false;
    }
  }

  function readerTop(el) {
    return isPhone.value ? Math.max(0, -el.getBoundingClientRect().top) : el.scrollTop;
  }

  function readingRange(el) {
    const article = el.querySelector(".reader-content");
    if (!article) return 0;
    const viewport = isPhone.value ? window.innerHeight : el.clientHeight;
    return Math.max(0, article.offsetTop + article.offsetHeight - viewport);
  }

  function scrollReaderTo(top) {
    const el = readerRef.value;
    if (!el?.clientHeight) return;
    if (isPhone.value) {
      const pageTop = top > 0 ? window.scrollY + el.getBoundingClientRect().top + top : 0;
      window.scrollTo({ top: pageTop, behavior: "instant" });
    } else {
      el.scrollTop = top;
    }
    programmaticScroll = readerTop(el);
    const paragraphs = Array.from(el.querySelectorAll(".reader-paragraph"));
    const paragraph = paragraphs[visibleParagraph(paragraphs, programmaticScroll)];
    const firstWord = paragraph?.querySelector("[data-reader-key]");
    activeWord.value = firstWord?.dataset.readerKey || "";
  }

  function startReadingOver() {
    scrollReaderTo(0);
  }

  function resumeReading() {
    const el = readerRef.value;
    if (!el?.clientHeight || !state.currentArticle) return;
    const paragraph = bookmark.value && el.querySelectorAll(".reader-paragraph")[bookmark.value.paragraph];
    if (paragraph && canResumeReading.value) {
      const top = paragraph.offsetTop + paragraph.offsetHeight * bookmark.value.offset;
      scrollReaderTo(Math.min(top, readingRange(el)));
    } else if (canResumeReading.value) {
      scrollReaderTo(readingRange(el) * state.currentArticle.readProgress / 100);
    } else {
      startReadingOver();
    }
  }

  function handleReaderScroll() {
    const el = readerRef.value;
    if (!state.currentArticle || !articleVisible(state.currentArticle.id) || state.articleBusy || !el?.clientHeight) return;
    const scrollTop = readerTop(el);
    const alreadyPositioned = programmaticScroll === scrollTop;
    programmaticScroll = null;
    if (alreadyPositioned) return;
    const max = readingRange(el);
    const top = Math.min(scrollTop, max);
    const articleId = state.currentArticle.id;
    const paragraphs = Array.from(el.querySelectorAll(".reader-paragraph"));
    const index = visibleParagraph(paragraphs, top);
    const paragraph = paragraphs[index];
    if (paragraph && max > 0 && scrollTop <= max + 1) {
      const offset = Math.min(1, Math.max(0, (top - paragraph.offsetTop) / Math.max(1, paragraph.offsetHeight)));
      bookmark.value = { paragraph: index, offset };
      positionStore.set(articleId, bookmark.value);
      if (!el.contains(document.activeElement)) {
        activeWord.value = paragraph.querySelector("[data-reader-key]")?.dataset.readerKey || activeWord.value;
      }
    }
    const nextProgress = max > 0 ? Math.round(top / max * 100) : state.readerProgress;
    if (nextProgress <= state.readerProgress || state.currentArticle.finished) return;
    state.readerProgress = nextProgress;
    clearTimeout(state.progressTimer);
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
      state.readerProgress = Math.max(state.readerProgress, result.readProgress);
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

  function isLookupWord(text) {
    const word = normalizeLookupWord(text);
    return Boolean(word && !lookupStopWords.has(word));
  }

  function focusNavigationWord(token) {
    activeWord.value = token.key;
    if (readerFocus.value) focusedSentence.value = token.sentence;
  }

  function navigateReaderWords(event, token) {
    const step = { ArrowLeft: -1, ArrowRight: 1, Home: -Infinity, End: Infinity }[event.key];
    if (step === undefined || !readerWords.value.length) return;
    event.preventDefault();
    const index = readerWords.value.findIndex(word => word.key === token.key);
    const next = readerWords.value[Math.min(readerWords.value.length - 1, Math.max(0, index + step))];
    readerRef.value?.querySelector(`[data-reader-key="${CSS.escape(next.key)}"]`)?.focus();
  }

  function placeTooltip(event) {
    if (isPhone.value) return;
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
    if (isPhone.value) {
      keyboardLookupAnchor = event?.currentTarget || null;
      activeWord.value = keyboardLookupAnchor?.dataset.readerKey || activeWord.value;
      if (readerFocus.value) focusedSentence.value = keyboardLookupAnchor?.dataset.sentence || "";
    }
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
    if (!isLookupWord(word)) return;
    const anchor = event.currentTarget;
    keyboardLookupAnchor = anchor;
    await showTooltip(word, event);
    await nextTick();
    if (document.activeElement !== anchor || keyboardLookupAnchor !== anchor) return;
    const panel = tooltipRef.value;
    (panel?.querySelector("button:not(:disabled)") || panel)?.focus();
  }

  function closeKeyboardLookup() {
    state.tooltip.visible = false;
    keyboardLookupAnchor?.focus({ preventScroll: true });
    keyboardLookupAnchor = null;
  }

  function scheduleHideTooltip() {
    clearTimeout(state.tooltipTimer);
    if (isPhone.value) return;
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
    const route = router.currentRoute.value;
    if (route.name === "news" && String(route.query.article) === String(id)) return loadArticle(id);
    await router.push({ path: "/news", query: { article: String(id) } });
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

  watch([readerRef, () => router.currentRoute.value.name], ([element, name]) => {
    if (element && name === "news" && !state.articleBusy) resumeReading();
  }, { flush: "post" });

  watch(isPhone, phone => {
    state.tooltip.visible = false;
    if (!phone && router.currentRoute.value.name === "news") loadNews(router.currentRoute.value.query.article).catch(() => {});
  });
  watch(() => isPhone.value && router.currentRoute.value.name === "news" && phoneArticle.value, (active, previous, onCleanup) => {
    if (!active) return;
    window.addEventListener("scroll", handleReaderScroll, { passive: true });
    onCleanup(() => window.removeEventListener("scroll", handleReaderScroll));
  }, { immediate: true });

  onBeforeUnmount(() => {
    clearTimeout(state.tooltipTimer);
    clearTimeout(state.progressTimer);
  });

  return {
    state,
    isPhone,
    phoneArticle,
    selectArticle,
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
    canResumeReading,
    resumeReading,
    startReadingOver,
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
    isLookupWord,
    activeReaderWord,
    focusNavigationWord,
    navigateReaderWords,
    showTooltip,
    focusLookupActions,
    closeKeyboardLookup,
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
