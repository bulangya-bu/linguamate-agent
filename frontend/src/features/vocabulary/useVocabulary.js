import { wordDefinitionGroups } from "@/word-definitions";
import { ElMessage } from "element-plus";
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from "vue";
import { learningApi } from "@/api/modules";

function createQuizRound() {
  return reactive({
    words: [],
    index: 0,
    answers: {},
    options: [],
    completed: false,
    busy: false,
    loading: false,
    loaded: false,
    error: false,
  });
}

export function useVocabulary({ route, router, activeSection, loadReport }) {
  const state = reactive({
    quizCategory: "",
    quizCategories: [],
    wordDetails: {},
    reviewMode: "today",
    reviewWords: [],
    reviewIndex: 0,
    reviewTotal: 0,
    reviewMistakes: 0,
    reviewForm: { spellingAnswer: "", meaningAnswer: "" },
    reviewBusy: false,
    lastReviewResult: null,
    reviewPendingResult: false,
    reviewCompleted: false,
    wordBank: {
      scope: "all",
      stats: {},
      loading: false,
      categories: [],
      words: [],
      total: 0,
      page: 0,
      size: 48,
      category: "",
      search: "",
    },
    selectedWord: null,
  });

  const regularQuiz = createQuizRound();
  const newWordsQuiz = createQuizRound();
  const quizScope = computed(() =>
    activeSection.value === "vocabulary" && route.query.scope === "new"
      ? "new"
      : "",
  );
  const quizRound = computed(() => (quizScope.value ? newWordsQuiz : regularQuiz));
  const quizPreviewWords = computed(() =>
    regularQuiz.words.length ? regularQuiz.words : newWordsQuiz.words,
  );

  const wordBankGrid = ref(null);

  const quizPane = ref("practice");

  const quizInsightToggle = ref(null);

  const quizWorkbench = ref(null);

  let quizMotionId = 0;

  let quizTransition = null;

  let quizAnimations = [];

  async function openLibraryReview() {
    state.reviewMode = state.wordBank.scope === "review" ? "mistakes" : "today";
    await router.push("/review");
  }

  const wordDialogRef = ref(null);

  const wordBankScopes = [
    { id: "all", label: "全部单词" },
    { id: "due", label: "待复习" },
    { id: "review", label: "错词" },
    { id: "favorites", label: "收藏词" },
    { id: "mastered", label: "已掌握" },
  ];

  const currentQuizWord = computed(() => quizRound.value.words[quizRound.value.index]);

  const quizResultForCurrent = computed(() =>
    currentQuizWord.value ? quizRound.value.answers[currentQuizWord.value.id] : null,
  );

  const wordDetailForCurrent = computed(() =>
    currentQuizWord.value ? state.wordDetails[currentQuizWord.value.id] : null,
  );

  const quizDefinitionGroups = computed(() =>
    wordDefinitionGroups(
      currentQuizWord.value,
      wordDetailForCurrent.value?.status === "ready"
        ? wordDetailForCurrent.value.data
        : null,
    ),
  );

  const currentReviewWord = computed(
    () => state.reviewWords[state.reviewIndex],
  );

  const selectedWordDetail = computed(() =>
    state.selectedWord ? state.wordDetails[state.selectedWord.id] : null,
  );

  const quizOptions = computed(() => quizRound.value.options[quizRound.value.index] || []);

  const quizAnsweredCount = computed(
    () => Object.keys(quizRound.value.answers).length,
  );

  const quizProgress = computed(() => {
    const round = quizRound.value;
    const total = Math.max(1, round.words.length);
    return Math.round(
      ((round.index + (quizResultForCurrent.value ? 1 : 0)) / total) * 100,
    );
  });

  const reviewTotal = computed(() =>
    Math.max(1, state.reviewTotal || state.reviewWords.length),
  );

  const reviewRemaining = computed(() => state.reviewWords.length);

  const reviewProgress = computed(() => {
    const total = reviewTotal.value;
    const passed = Math.max(0, total - reviewRemaining.value);
    return Math.round((passed / total) * 100);
  });

  const wordBankEmptyText = computed(() => {
    if (state.wordBank.search || state.wordBank.category)
      return "没有符合筛选的单词";
    if (state.wordBank.scope === "favorites") return "还没有收藏词";
    if (state.wordBank.scope === "mastered") return "还没有已掌握的单词";
    if (state.wordBank.scope === "review") return "暂时没有需要强化的错词";
    if (state.wordBank.scope === "due") return "当前没有到期复习的单词";
    return "词库暂时为空";
  });

  const wordBankTotalPages = computed(() =>
    Math.max(1, Math.ceil((state.wordBank.total || 0) / state.wordBank.size)),
  );

  const wordDialogVisible = computed({
    get: () => Boolean(state.selectedWord),
    set: (value) => {
      if (!value) state.selectedWord = null;
    },
  });

  function buildQuizOptions(word, index, words) {
    const distractors = [];
    for (
      let offset = 1;
      offset < words.length && distractors.length < 3;
      offset += 1
    ) {
      const candidate = words[(index + offset) % words.length];
      if (
        candidate?.id !== word.id &&
        candidate?.meaningCn &&
        candidate.meaningCn !== word.meaningCn &&
        !distractors.includes(candidate.meaningCn)
      ) {
        distractors.push(candidate.meaningCn);
      }
    }
    [
      "清晰的；明确的",
      "自然的表达方式",
      "复习计划；学习安排",
      "阅读材料；文章",
    ].forEach((item) => {
      if (
        distractors.length < 3 &&
        item !== word.meaningCn &&
        !distractors.includes(item)
      ) {
        distractors.push(item);
      }
    });
    const options = [word.meaningCn, ...distractors.slice(0, 3)].filter(
      Boolean,
    );
    const rotateBy = options.length
      ? Number(word.id || index) % options.length
      : 0;
    return [...options.slice(rotateBy), ...options.slice(0, rotateBy)];
  }

  async function loadQuizCategories() {
    state.quizCategories = await learningApi.quizCategories();
  }

  async function loadWords() {
    const round = quizRound.value;
    if (round.loading || round.busy) return;
    const scope = quizScope.value;
    round.loading = true;
    round.error = false;
    try {
      const words = await learningApi.todayWords({
        limit: 8,
        category: scope ? "" : state.quizCategory,
        scope,
      });
      round.words = words;
      round.index = 0;
      round.answers = {};
      round.options = words.map(buildQuizOptions);
      round.completed = false;
      round.loaded = true;
      if (round === quizRound.value) resetQuizPane();
    } catch {
      round.error = true;
    } finally {
      round.loading = false;
    }
  }

  async function loadQuizForRoute() {
    if (quizScope.value || !regularQuiz.loaded) await loadWords();
  }

  function prevQuiz() {
    if (quizRound.value.index > 0) quizRound.value.index -= 1;
  }

  function nextQuiz() {
    if (!quizResultForCurrent.value) return;
    const round = quizRound.value;
    if (round.index >= round.words.length - 1) {
      round.completed = true;
      return;
    }
    round.index += 1;
  }

  function quizOptionStatus(option) {
    const result = quizResultForCurrent.value;
    if (!result) return "";
    if (option === result.expectedMeaning) return "correct";
    if (option === result.selectedAnswer) return "wrong";
    return "";
  }

  async function submitQuiz(answer) {
    const round = quizRound.value;
    if (!currentQuizWord.value || quizResultForCurrent.value || round.busy)
      return;
    const wordId = currentQuizWord.value.id;
    round.busy = true;
    try {
      const result = await learningApi.quiz(wordId, { answer });
      round.answers[wordId] = { ...result, selectedAnswer: answer };
      applyWordProgress(result.wordId, result);
      await Promise.all([loadReport(), loadReviews()]);
    } catch (error) {
      ElMessage.error(error.message || "提交失败，请重试");
    } finally {
      round.busy = false;
    }
  }

  async function loadWordDetail(word, force = false) {
    if (!word) return;
    if (state.wordDetails[word.id]?.status === "loading") return;
    if (state.wordDetails[word.id]?.status === "ready" && !force) return;
    state.wordDetails[word.id] = { status: "loading" };
    try {
      state.wordDetails[word.id] = {
        status: "ready",
        data: await learningApi.wordAiDetail(word.id),
      };
    } catch (error) {
      state.wordDetails[word.id] = {
        status: "error",
        message: error.message || "模型未配置或暂时不可用，请检查 AI 设置",
      };
      ElMessage.error(state.wordDetails[word.id].message);
    }
  }

  function openQuizInsight() {
    if (!quizResultForCurrent.value) return;
    changeQuizPane("insight");
    loadWordDetail(currentQuizWord.value);
  }

  async function closeQuizInsight() {
    await changeQuizPane("practice");
    if (quizPane.value === "practice") {
      quizInsightToggle.value?.focus({ preventScroll: true });
    }
  }

  function stopQuizMotion() {
    quizMotionId++;
    quizTransition?.skipTransition();
    quizTransition = null;
    quizAnimations.forEach((animation) => animation.cancel());
    quizAnimations = [];
  }

  function resetQuizPane() {
    stopQuizMotion();
    quizPane.value = "practice";
  }

  async function changeQuizPane(pane) {
    stopQuizMotion();
    const motionId = quizMotionId;
    const update = async () => {
      if (motionId !== quizMotionId) return;
      quizPane.value = pane;
      await nextTick();
    };
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
      await update();
      return;
    }
    if (document.startViewTransition) {
      const transition = document.startViewTransition(update);
      quizTransition = transition;
      transition.ready.catch(() => {});
      await transition.finished.catch(() => {});
      if (quizTransition === transition) quizTransition = null;
      return;
    }

    // Keep both directions animated on browsers without View Transitions.
    const surface = quizWorkbench.value;
    const frames = [
      ...(surface?.querySelectorAll(".word-quiz-card, .quiz-nav") || []),
    ]
      .filter((element) => element.getBoundingClientRect().width)
      .map((element) => ({ element, before: element.getBoundingClientRect() }));
    const animate = (element, keyframes, duration = 380) => {
      const animation = element.animate(keyframes, {
        duration,
        easing: "cubic-bezier(.22,1,.36,1)",
      });
      quizAnimations.push(animation);
      return animation.finished.catch(() => {});
    };
    const aside = surface?.querySelector(".quiz-insight");
    if (pane === "practice" && aside) {
      await animate(
        aside,
        [
          { opacity: 1, transform: "none" },
          { opacity: 0, transform: "translateX(16px)" },
        ],
        180,
      );
    }
    if (motionId !== quizMotionId) return;
    await update();
    const motions = frames.flatMap(({ element, before }) => {
      const after = element.getBoundingClientRect();
      if (!after.width || !after.height) return [];
      return [
        animate(element, [
          {
            transformOrigin: "0 0",
            transform: `translate(${before.x - after.x}px, ${before.y - after.y}px) scale(${before.width / after.width}, ${before.height / after.height})`,
          },
          { transformOrigin: "0 0", transform: "none" },
        ]),
      ];
    });
    const incoming = surface?.querySelector(
      pane === "insight" ? ".quiz-insight" : ".word-quiz-card",
    );
    if (
      incoming &&
      (pane === "insight" ||
        !frames.some(({ element }) => element === incoming))
    ) {
      motions.push(
        animate(incoming, [
          { opacity: 0, transform: "translateX(16px)" },
          { opacity: 1, transform: "none" },
        ]),
      );
    }
    await Promise.all(motions);
    if (motionId === quizMotionId) quizAnimations = [];
  }

  function applyWordProgress(wordId, result) {
    patchWord(wordId, (word) => ({
      ...word,
      familiarity: result.familiarity ?? word.familiarity,
      wrongCount: result.wrongCount ?? word.wrongCount,
      exampleEn: result.exampleEn || word.exampleEn,
      exampleCn: result.exampleCn || word.exampleCn,
    }));
  }

  function patchWord(wordId, patcher) {
    const numericId = Number(wordId);
    const patchList = (words) =>
      words.map((word) =>
        Number(word.id) === numericId ? patcher(word) : word,
      );
    regularQuiz.words = patchList(regularQuiz.words);
    newWordsQuiz.words = patchList(newWordsQuiz.words);
    state.reviewWords = patchList(state.reviewWords);
    state.wordBank.words = patchList(state.wordBank.words);
    if (state.selectedWord && Number(state.selectedWord.id) === numericId) {
      state.selectedWord = patcher(state.selectedWord);
    }
  }

  function applyFavoriteUpdate(updated) {
    patchWord(updated.id, (word) => ({ ...word, favorite: updated.favorite }));
    if (
      state.wordBank.scope === "favorites" &&
      !updated.favorite &&
      state.wordBank.words.some((word) => word.id === updated.id)
    ) {
      state.wordBank.words = state.wordBank.words.filter(
        (word) => word.id !== updated.id,
      );
      state.wordBank.total = Math.max(0, state.wordBank.total - 1);
    }
  }

  async function toggleFavorite(word) {
    if (!word) return;
    try {
      const updated = await learningApi.toggleFavorite(word.id);
      applyFavoriteUpdate(updated);
      await loadReport();
      if (activeSection.value === "wordbank")
        await Promise.all([
          loadWordBankStats(),
          loadWordBankWords(state.wordBank.page),
        ]);
    } catch (error) {
      ElMessage.error(error.message || "收藏失败");
    }
  }

  async function setReviewMode(mode) {
    if (state.reviewBusy || mode === state.reviewMode) return;
    state.reviewMode = mode;
    await loadReviews();
  }

  async function loadReviews() {
    state.reviewWords = await learningApi.reviews(state.reviewMode, 8);
    state.reviewIndex = 0;
    state.reviewTotal = state.reviewWords.length;
    state.reviewMistakes = 0;
    state.lastReviewResult = null;
    state.reviewPendingResult = false;
    state.reviewCompleted = false;
    state.reviewForm.spellingAnswer = "";
    state.reviewForm.meaningAnswer = "";
  }

  async function submitReview() {
    if (
      !currentReviewWord.value ||
      state.reviewBusy ||
      state.reviewPendingResult
    )
      return;
    if (!state.reviewForm.spellingAnswer || !state.reviewForm.meaningAnswer) {
      ElMessage.warning("请同时填写英文拼写和中文释义");
      return;
    }
    state.reviewBusy = true;
    try {
      state.lastReviewResult = await learningApi.submitReview(
        currentReviewWord.value.id,
        { ...state.reviewForm },
      );
      applyWordProgress(currentReviewWord.value.id, state.lastReviewResult);
      if (!state.lastReviewResult.correct) state.reviewMistakes++;
      state.reviewPendingResult = true;
      await loadReport();
    } catch (error) {
      ElMessage.error(error.message || "提交失败，答案已保留，请重试");
    } finally {
      state.reviewBusy = false;
    }
  }

  function advanceReview() {
    if (!currentReviewWord.value || !state.reviewPendingResult) return;
    if (state.lastReviewResult?.correct) {
      state.reviewWords.splice(state.reviewIndex, 1);
    } else {
      const [word] = state.reviewWords.splice(state.reviewIndex, 1);
      if (word) state.reviewWords.push(word);
    }
    state.reviewIndex = 0;
    state.lastReviewResult = null;
    state.reviewPendingResult = false;
    state.reviewForm.spellingAnswer = "";
    state.reviewForm.meaningAnswer = "";
    if (!state.reviewWords.length) {
      state.reviewCompleted = true;
    }
  }

  let wordBankSearchTimer;

  let wordBankRequestId = 0;

  async function loadWordBank() {
    const scope = wordBankScopes.some((item) => item.id === route.query.scope)
      ? route.query.scope
      : "all";
    const page = state.wordBank.scope === scope ? state.wordBank.page : 0;
    state.wordBank.scope = scope;
    await Promise.all([
      loadWordBankStats(),
      !state.wordBank.categories.length
        ? learningApi.wordBankCategories().then((categories) => {
            state.wordBank.categories = categories;
          })
        : Promise.resolve(),
    ]);
    await loadWordBankWords(page);
  }

  async function loadWordBankStats() {
    state.wordBank.stats = await learningApi.wordBankStats();
  }

  async function loadWordBankWords(page = 0) {
    const requestId = ++wordBankRequestId;
    state.wordBank.loading = true;
    state.wordBank.page = Math.max(0, page);
    try {
      const result = await learningApi.wordBankWords({
        page: state.wordBank.page,
        size: state.wordBank.size,
        category: state.wordBank.category,
        search: state.wordBank.search,
        scope: state.wordBank.scope,
      });
      if (requestId !== wordBankRequestId) return;
      const lastPage = Math.max(
        0,
        Math.ceil((result.total || 0) / (result.size || state.wordBank.size)) -
          1,
      );
      if (state.wordBank.page > lastPage) {
        await loadWordBankWords(lastPage);
        return;
      }
      state.wordBank.words = result.words || [];
      state.wordBank.total = result.total || 0;
      state.wordBank.page = result.page || 0;
      state.wordBank.size = result.size || state.wordBank.size;
      await nextTick();
      wordBankGrid.value?.scrollTo({ top: 0 });
    } catch (error) {
      if (requestId !== wordBankRequestId) return;
      state.wordBank.words = [];
      state.wordBank.total = 0;
      ElMessage.error(error.message || "词库加载失败");
    } finally {
      if (requestId === wordBankRequestId) state.wordBank.loading = false;
    }
  }

  async function setWordBankScope(scope) {
    if (state.wordBank.scope === scope) return;
    clearTimeout(wordBankSearchTimer);
    await router.push({
      path: "/wordbank",
      query: scope === "all" ? {} : { scope },
    });
  }

  async function resetWordBankFilters() {
    clearTimeout(wordBankSearchTimer);
    state.wordBank.category = "";
    state.wordBank.search = "";
    if (state.wordBank.scope === "all") await loadWordBankWords(0);
    else await setWordBankScope("all");
  }

  async function setWordBankCategory(category) {
    clearTimeout(wordBankSearchTimer);
    state.wordBank.category = category;
    await loadWordBankWords(0);
  }

  function scheduleWordBankSearch() {
    clearTimeout(wordBankSearchTimer);
    wordBankSearchTimer = setTimeout(() => {
      loadWordBankWords(0).catch((error) =>
        ElMessage.error(error.message || "词库搜索失败"),
      );
    }, 350);
  }

  watch(
    [quizRound, () => currentQuizWord.value?.id],
    () => {
      resetQuizPane();
    },
  );

  onBeforeUnmount(() => {
    stopQuizMotion();
    clearTimeout(wordBankSearchTimer);
  });

  return {
    state,
    wordBankGrid,
    quizScope,
    quizRound,
    quizPreviewWords,
    quizPane,
    quizInsightToggle,
    quizWorkbench,
    openLibraryReview,
    wordDialogRef,
    wordBankScopes,
    currentQuizWord,
    quizResultForCurrent,
    wordDetailForCurrent,
    quizDefinitionGroups,
    currentReviewWord,
    selectedWordDetail,
    quizOptions,
    quizAnsweredCount,
    quizProgress,
    reviewTotal,
    reviewRemaining,
    reviewProgress,
    wordBankEmptyText,
    wordBankTotalPages,
    wordDialogVisible,
    loadQuizCategories,
    loadQuizForRoute,
    loadWords,
    prevQuiz,
    nextQuiz,
    quizOptionStatus,
    submitQuiz,
    loadWordDetail,
    openQuizInsight,
    closeQuizInsight,
    toggleFavorite,
    setReviewMode,
    loadReviews,
    submitReview,
    advanceReview,
    loadWordBank,
    loadWordBankStats,
    loadWordBankWords,
    setWordBankScope,
    resetWordBankFilters,
    setWordBankCategory,
    scheduleWordBankSearch,
  };
}
