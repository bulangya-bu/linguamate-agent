const state = {
    currentUser: null,
    conversationId: null,
    words: [],
    tooltipWord: null,
    currentArticle: null,
    translateBusy: false,
    ai: {
        loaded: false,
        models: [],
        provider: "",
        baseUrl: "",
        chatModel: "",
        hasApiKey: false
    },
    review: {
        mode: "today",
        words: []
    },
    quiz: {
        words: [],
        category: "",
        categories: [],
        index: 0,
        answers: {},
        options: [],
        completed: false,
        aiDetails: {}
    },
    wordAiDetails: {},
    inlineAnsweredWords: {},
    wordBank: {
        categories: [],
        words: [],
        total: 0,
        page: 0,
        size: 50,
        category: "",
        search: "",
        loaded: false,
        selectedWordId: null
    }
};

const api = async (url, options = {}) => {
    const response = await fetch(url, {
        headers: { "Content-Type": "application/json", ...(options.headers || {}) },
        ...options
    });
    const payload = await response.json();
    if (!payload.success) {
        if (response.status === 401) {
            window.location.href = "/login";
            return null;
        }
        throw new Error(payload.message || "请求失败");
    }
    return payload.data;
};

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => Array.from(document.querySelectorAll(selector));
const lookupStopWords = new Set([
    "a", "an", "the", "and", "or", "but", "to", "of", "in", "on", "at", "by", "for", "with",
    "is", "are", "was", "were", "be", "been", "it", "its", "they", "them", "this", "that",
    "when", "while", "then", "than", "also", "can", "may", "do", "does", "did"
]);

function showSection(id) {
    $$(".nav-item").forEach((item) => item.classList.toggle("active", item.dataset.section === id));
    $$(".section").forEach((section) => section.classList.toggle("active", section.id === id));
}

function renderReport(report, level) {
    $("#learnedWords").textContent = report.learnedWords;
    $("#reviewWords").textContent = report.reviewWords;
    $("#favoriteWords").textContent = report.favoriteWords;
    $("#essayCount").textContent = report.essayCount;
    $("#readingCount").textContent = report.readingCount;
    $("#nextActions").innerHTML = report.nextActions.map((item) => `<li>${item}</li>`).join("");
    if (level) {
        const pct = Math.min(100, (level.experience / level.nextLevelXp) * 100);
        $("#levelDisplay").textContent = `Lv.${level.level}`;
        $("#xpFill").style.width = pct + "%";
        $("#xpDisplay").textContent = `${level.experience} / ${level.nextLevelXp}`;
    }
}

function setAiStatus(message) {
    const el = $("#aiConnectionStatus");
    if (el) {
        el.textContent = message;
    }
}

function normalizeModelList(models) {
    return Array.from(new Set((models || [])
        .map((item) => (typeof item === "string" ? item : item?.id))
        .filter(Boolean)));
}

function populateAiModelSelect(models, selectedModel = "") {
    const select = $("#aiModelSelect");
    if (!select) return;
    const modelList = normalizeModelList(models);
    state.ai.models = modelList;
    const current = selectedModel || state.ai.chatModel || modelList[0] || "";
    if (!modelList.length) {
        select.innerHTML = current
            ? `<option value="${escapeAttribute(current)}">${escapeHtml(current)}</option>`
            : `<option value="">暂无模型</option>`;
        select.disabled = true;
        select.value = current;
        return;
    }
    select.disabled = false;
    const displayList = current && !modelList.includes(current) ? [current, ...modelList] : modelList;
    select.innerHTML = displayList.map((model) => `<option value="${escapeAttribute(model)}">${escapeHtml(model)}</option>`).join("");
    select.value = current && displayList.includes(current) ? current : displayList[0];
}

async function loadAiSettings() {
    const settings = await api("/api/ai/settings");
    state.ai.loaded = true;
    state.ai.provider = settings.provider || "";
    state.ai.baseUrl = settings.baseUrl || "";
    state.ai.chatModel = settings.chatModel || "";
    state.ai.hasApiKey = Boolean(settings.hasApiKey);
    const baseUrlInput = $("#aiBaseUrl");
    const apiKeyInput = $("#aiApiKey");
    if (baseUrlInput) baseUrlInput.value = state.ai.baseUrl;
    if (apiKeyInput) apiKeyInput.value = "";
    populateAiModelSelect(state.ai.models, state.ai.chatModel);
    setAiStatus(settings.hasApiKey ? "已保存密钥" : "未保存密钥");
    return settings;
}

function openSettings() {
    const panel = $("#settingsPanel");
    const backdrop = $("#settingsBackdrop");
    if (!panel || !backdrop) return;
    panel.hidden = false;
    backdrop.hidden = false;
    panel.setAttribute("aria-hidden", "false");
    backdrop.setAttribute("aria-hidden", "false");
    window.requestAnimationFrame(() => {
        panel.classList.add("is-open");
        backdrop.classList.add("is-open");
    });
}

function closeSettings() {
    const panel = $("#settingsPanel");
    const backdrop = $("#settingsBackdrop");
    if (!panel || !backdrop) return;
    panel.classList.remove("is-open");
    backdrop.classList.remove("is-open");
    window.setTimeout(() => {
        if (!panel.classList.contains("is-open")) {
            panel.hidden = true;
            backdrop.hidden = true;
            panel.setAttribute("aria-hidden", "true");
            backdrop.setAttribute("aria-hidden", "true");
        }
    }, 180);
}

async function fetchAiModels() {
    const baseUrl = $("#aiBaseUrl")?.value || "";
    const apiKey = $("#aiApiKey")?.value || "";
    setAiStatus("正在获取模型...");
    const models = await api("/api/ai/models", {
        method: "POST",
        body: JSON.stringify({ baseUrl, apiKey })
    });
    populateAiModelSelect(models, $("#aiModelSelect")?.value || state.ai.chatModel);
    const total = normalizeModelList(models).length;
    setAiStatus(total ? `已获取 ${total} 个模型` : "没有找到可用模型");
    return models;
}

async function saveAiSettings() {
    const payload = {
        baseUrl: $("#aiBaseUrl")?.value || "",
        apiKey: $("#aiApiKey")?.value || "",
        chatModel: $("#aiModelSelect")?.value || ""
    };
    setAiStatus("正在保存...");
    const settings = await api("/api/ai/settings", {
        method: "PUT",
        body: JSON.stringify(payload)
    });
    state.ai.provider = settings.provider || "";
    state.ai.baseUrl = settings.baseUrl || "";
    state.ai.chatModel = settings.chatModel || "";
    state.ai.hasApiKey = Boolean(settings.hasApiKey);
    if ($("#aiBaseUrl")) $("#aiBaseUrl").value = state.ai.baseUrl;
    if ($("#aiApiKey")) $("#aiApiKey").value = "";
    if (state.ai.models.length) {
        populateAiModelSelect(state.ai.models, state.ai.chatModel);
    } else {
        populateAiModelSelect([state.ai.chatModel], state.ai.chatModel);
    }
    setAiStatus(`已应用 ${state.ai.chatModel}`);
    return settings;
}

function favoriteButton(word, variant = "") {
    const active = Boolean(word.favorite);
    return `
        <button class="favorite-button ${variant} ${active ? "is-active" : ""}" type="button"
                data-favorite-id="${word.id}" aria-pressed="${active}" title="${active ? "取消收藏" : "收藏单词"}">
            <span aria-hidden="true">★</span>
        </button>`;
}

function formatPhonetic(value) {
    const trimmed = String(value || "").trim();
    if (!trimmed) return "";
    return trimmed.startsWith("/") && trimmed.endsWith("/") ? trimmed : `/${trimmed.replaceAll("/", "")}/`;
}

function wordMeta(word) {
    const parts = [];
    const phonetic = formatPhonetic(word.phoneticUs);
    if (phonetic) parts.push(`<span class="phonetic">${escapeHtml(phonetic)}</span>`);
    if (word.partOfSpeech) parts.push(escapeHtml(word.partOfSpeech));
    if (word.levelTag) parts.push(escapeHtml(word.levelTag));
    return `<span class="word-meta">${parts.join(" · ")}</span>`;
}

function wordCard(word, compact = false) {
    const aiState = state.wordAiDetails[word.id];
    const inlineAnswered = Boolean(state.inlineAnsweredWords[word.id]);
    const quiz = compact ? "" : `
        <form class="mini-quiz" data-word-id="${word.id}">
            <input placeholder="输入英文或中文释义">
            <button type="submit">提交</button>
        </form>`;
    return `
        <article class="word-card ${word.favorite ? "is-favorite" : ""}" data-word-card-id="${word.id}">
            <div class="word-card-head">
                <div>
                    <strong>${word.word}</strong>
                    ${wordMeta(word)}
                </div>
                ${favoriteButton(word)}
            </div>
            <p>${word.meaningCn}</p>
            ${quiz}
            ${inlineAnswered ? renderInlineWordExample(word) : ""}
            ${inlineAnswered ? renderInlineDetailAction(word, aiState) : ""}
            ${inlineAnswered ? renderWordAiDetail(word, aiState, "inline") : ""}
        </article>`;
}

function renderWordBankDialog(word) {
    if (!word) return "";
    const aiState = state.wordAiDetails[word.id];
    const english = word.exampleEn || `Try to use ${word.word} in a complete sentence.`;
    const chinese = word.exampleCn || "试着把这个单词放进一个完整句子里。";
    return `
        <div class="wordbank-dialog-shell" data-wordbank-dialog-word-id="${word.id}">
            <div class="wordbank-dialog-head">
                <div>
                    <p class="eyebrow compact">词库详情</p>
                    <h2 id="wordBankDialogTitle">${escapeHtml(word.word)}</h2>
                    ${wordMeta(word)}
                </div>
                <div class="wordbank-dialog-actions">
                    ${favoriteButton(word, "wordbank-dialog-favorite")}
                    <button id="wordBankDialogClose" class="wordbank-dialog-close" type="button" aria-label="关闭词库详情">×</button>
                </div>
            </div>
            <div class="wordbank-dialog-meaning">
                <span>中文释义</span>
                <p>${escapeHtml(word.meaningCn || "暂无释义")}</p>
            </div>
            <div class="wordbank-dialog-example">
                <span>例句</span>
                <p>${highlightTerm(english, word.word)}</p>
                ${chinese ? `<small>${escapeHtml(chinese)}</small>` : ""}
            </div>
            ${renderWordBankDetailAction(word, aiState)}
            <div id="wordBankDialogAiDetail" class="wordbank-dialog-ai">
                ${renderWordAiDetail(word, aiState, "wordbank")}
            </div>
        </div>`;
}

function renderWordBankDetailAction(word, aiState) {
    const loading = aiState?.status === "loading";
    const ready = aiState?.status === "ready";
    return `
        <div class="wordbank-dialog-detail-actions">
            <button class="quiz-detail-button ${loading ? "is-loading" : ""} ${ready ? "is-done" : ""}"
                    type="button"
                    data-wordbank-ai-detail-word-id="${word.id}"
                    ${loading ? "disabled" : ""}>
                ${loading ? "生成中..." : ready ? "已生成解析" : "深度解析"}
            </button>
        </div>`;
}

function openWordBankDialog(wordId) {
    const word = findWordById(wordId);
    if (!word) return;
    state.wordBank.selectedWordId = Number(wordId);
    refreshWordBankDialog();
    $("#wordBankDialog")?.classList.add("is-open");
    $("#wordBankDialog")?.setAttribute("aria-hidden", "false");
    $("#wordBankDialogBackdrop")?.classList.add("is-open");
}

function closeWordBankDialog() {
    state.wordBank.selectedWordId = null;
    $("#wordBankDialog")?.classList.remove("is-open");
    $("#wordBankDialog")?.setAttribute("aria-hidden", "true");
    $("#wordBankDialogBackdrop")?.classList.remove("is-open");
}

function refreshWordBankDialog() {
    const content = $("#wordBankDialogContent");
    const word = findWordById(state.wordBank.selectedWordId);
    if (!content || !word) return;
    content.innerHTML = renderWordBankDialog(word);
    bindWordBankDialogActions();
}

function bindWordBankDialogActions() {
    $("#wordBankDialogClose")?.addEventListener("click", closeWordBankDialog);
    $("#wordBankDialogContent")?.querySelector("[data-wordbank-ai-detail-word-id]")?.addEventListener("click", (event) => {
        event.stopPropagation();
        loadWordAiDetail(event.currentTarget.dataset.wordbankAiDetailWordId, "wordbank");
    });
    $("#wordBankDialogContent")?.querySelector("[data-ai-detail-retry-id]")?.addEventListener("click", (event) => {
        event.stopPropagation();
        loadWordAiDetail(event.currentTarget.dataset.aiDetailRetryId, "wordbank", true);
    });
}

function renderTodayWords() {
    $("#todayWords").innerHTML = state.words.length
        ? state.words.slice(0, 4).map((word) => wordCard(word, true)).join("")
        : `<div class="empty-state">暂无单词，稍后重试。</div>`;
}

function renderQuizCategorySelect() {
    const select = $("#quizCategorySelect");
    if (!select) return;
    const current = state.quiz.category || "";
    select.innerHTML = `<option value="">默认混合</option>` +
        state.quiz.categories.map((cat) => (
            `<option value="${escapeAttribute(cat.tag)}">${escapeHtml(cat.label)}</option>`
        )).join("");
    select.value = current && state.quiz.categories.some((cat) => cat.tag === current) ? current : "";
}

async function loadQuizCategories() {
    try {
        state.quiz.categories = await api("/api/word-bank/categories");
    } catch (error) {
        console.error(error);
        state.quiz.categories = [];
    }
    renderQuizCategorySelect();
}

async function loadWords({ updateToday = true } = {}) {
    if (updateToday) {
        $("#todayWords").innerHTML = `<div class="empty-state">正在准备今日单词...</div>`;
    }
    $("#wordCards").innerHTML = `<div class="quiz-empty">正在加载单词闯关...</div>`;
    try {
        if (updateToday) {
            state.words = await api("/api/words/today?limit=8");
        }
        const params = new URLSearchParams({ limit: 8 });
        if (state.quiz.category) {
            params.set("category", state.quiz.category);
        }
        state.quiz.words = await api(`/api/words/today?${params}`);
    } catch (error) {
        console.error(error);
        if (updateToday) {
            state.words = [];
            $("#todayWords").innerHTML = `<div class="empty-state">单词加载失败，请刷新重试。</div>`;
        }
        state.quiz.words = [];
        $("#wordCards").innerHTML = `<div class="quiz-empty">单词加载失败，请刷新重试。</div>`;
        return;
    }
    if (updateToday) {
        renderTodayWords();
    }
    state.quiz = {
        ...state.quiz,
        index: 0,
        answers: {},
        options: state.quiz.words.map((word, index) => buildQuizOptions(word, index)),
        completed: false,
        aiDetails: {}
    };
    renderWordQuiz();
}

async function loadReviews() {
    const reviewUrls = {
        today: "/api/reviews/today?limit=6",
        favorites: "/api/reviews/favorites?limit=20",
        mastered: "/api/reviews/mastered?limit=20"
    };
    const url = reviewUrls[state.review.mode] || reviewUrls.today;
    state.review.words = await api(url);
    renderReviewWords();
}

function renderReviewWords() {
    $("[data-review-mode='today']")?.classList.toggle("is-active", state.review.mode === "today");
    $("[data-review-mode='favorites']")?.classList.toggle("is-active", state.review.mode === "favorites");
    $("[data-review-mode='mastered']")?.classList.toggle("is-active", state.review.mode === "mastered");
    const emptyTextMap = {
        today: "今天暂时没有需要复习的单词。",
        favorites: "还没有收藏单词，可以在单词卡右上角点击星标。",
        mastered: "还没有已掌握单词，答对几轮后会出现在这里。"
    };
    const emptyText = emptyTextMap[state.review.mode] || emptyTextMap.today;
    $("#reviewCards").innerHTML = state.review.words.length
        ? state.review.words.map((word) => wordCard(word, true)).join("")
        : `<div class="empty-state review-empty">${emptyText}</div>`;
}

function findWordById(wordId) {
    const numericId = Number(wordId);
    return [...state.words, ...state.quiz.words, ...state.review.words, ...state.wordBank.words]
        .find((item) => item.id === numericId);
}

function bindQuizForms() {
    $$(".mini-quiz").forEach((form) => {
        form.addEventListener("submit", async (event) => {
            event.preventDefault();
            const input = form.querySelector("input");
            const wordId = form.dataset.wordId;
            const result = await api(`/api/words/${wordId}/quiz`, {
                method: "POST",
                body: JSON.stringify({ answer: input.value })
            });
            applyQuizResultToWord(Number(wordId), result);
            input.value = result.correct ? "已答对" : result.expectedMeaning;
            const card = form.closest(".word-card");
            card.classList.add(result.correct ? "is-correct" : "is-wrong");
            form.querySelector("button").disabled = true;
            const updatedWord = findWordById(wordId);
            renderInlineAnswerResult(card, updatedWord);
            await loadReviews();
            await loadReport();
        });
    });
}

function renderInlineAnswerResult(card, word) {
    if (!card || !word) return;
    state.inlineAnsweredWords[word.id] = true;
    if (!card.querySelector(".word-card-example")) {
        card.insertAdjacentHTML("beforeend", renderInlineWordExample(word));
    }
    if (!card.querySelector(".word-card-detail-actions")) {
        card.insertAdjacentHTML("beforeend", renderInlineDetailAction(word, state.wordAiDetails[word.id]));
    }
    bindInlineDetailAction(card);
}

function renderInlineWordExample(word) {
    const english = word.exampleEn || `Try to use ${word.word} in a complete sentence.`;
    const chinese = word.exampleCn || "";
    return `
        <div class="word-card-example">
            <p>${highlightTerm(english, word.word)}</p>
            ${chinese ? `<span>${escapeHtml(chinese)}</span>` : ""}
        </div>`;
}

function renderInlineDetailAction(word, aiState) {
    const loading = aiState?.status === "loading";
    const ready = aiState?.status === "ready";
    return `
        <div class="word-card-detail-actions">
            <button class="quiz-detail-button inline-detail-button ${loading ? "is-loading" : ""} ${ready ? "is-done" : ""}"
                    type="button"
                    data-inline-ai-detail-word-id="${word.id}"
                    ${loading ? "disabled" : ""}>
                ${loading ? "生成中..." : ready ? "已生成解析" : "深度解析"}
            </button>
        </div>`;
}

function bindInlineDetailAction(root) {
    root.querySelector("[data-inline-ai-detail-word-id]")?.addEventListener("click", (event) => {
        loadWordAiDetail(event.currentTarget.dataset.inlineAiDetailWordId, "inline");
    });
}

function buildQuizOptions(word, index) {
    const distractors = [];
    const quizWords = state.quiz.words;
    for (let offset = 1; offset < quizWords.length && distractors.length < 3; offset += 1) {
        const candidate = quizWords[(index + offset) % quizWords.length];
        if (candidate.id !== word.id && candidate.meaningCn !== word.meaningCn && !distractors.includes(candidate.meaningCn)) {
            distractors.push(candidate.meaningCn);
        }
    }
    const fallback = ["清晰的；明确的", "自然的表达方式", "复习计划；学习安排", "阅读材料；文章"];
    fallback.forEach((item) => {
        if (distractors.length < 3 && item !== word.meaningCn && !distractors.includes(item)) {
            distractors.push(item);
        }
    });
    const options = [word.meaningCn, ...distractors.slice(0, 3)];
    const rotateBy = Number(word.id || index) % options.length;
    return [...options.slice(rotateBy), ...options.slice(0, rotateBy)];
}

function renderWordQuiz() {
    const container = $("#wordCards");
    if (!container) return;
    if (!state.quiz.words.length) {
        container.innerHTML = `<div class="quiz-empty">当前分类暂时没有单词。</div>`;
        return;
    }
    if (state.quiz.completed) {
        container.innerHTML = `
            <div class="quiz-complete">
                <div class="quiz-badge">完成</div>
                <h3>本轮单词闯关完成</h3>
                <p>已经完成 ${Object.keys(state.quiz.answers).length} 个单词，可以去复习错词队列继续巩固。</p>
                <button class="quiz-primary" type="button" data-quiz-action="restart">再来一轮</button>
            </div>`;
        bindWordQuizActions();
        return;
    }

    const index = state.quiz.index;
    const word = state.quiz.words[index];
    const answer = state.quiz.answers[word.id];
    const answered = Boolean(answer);
    const options = state.quiz.options[index] || [];
    const progress = Math.round(((index + (answered ? 1 : 0)) / state.quiz.words.length) * 100);
    const aiState = state.quiz.aiDetails[word.id];
    const expanded = Boolean(aiState);

    container.innerHTML = `
        <article class="word-quiz-card ${answered ? "is-answered" : ""} ${expanded ? "has-ai-detail" : ""}">
            <div class="quiz-progress">
                <span>第 ${index + 1} / ${state.quiz.words.length} 题</span>
                <div class="quiz-progress-track"><i style="width: ${progress}%"></i></div>
            </div>
            <div class="quiz-study-stage">
                <div class="quiz-word-shell">
                    ${favoriteButton(word, "quiz-favorite")}
                    <div class="quiz-word" aria-label="当前单词">${word.word}</div>
                </div>
                <div class="quiz-example-slot ${answered ? "is-visible" : ""}">
                    ${answered ? renderWordExample(word, aiState) : ""}
                </div>
            </div>
            ${answered ? renderQuizDetailAction(word, aiState) : ""}
            ${answered ? renderWordAiDetail(word, aiState) : ""}
            <div class="quiz-options" role="list">
                ${options.map((meaning) => renderQuizOption(meaning, answer, word)).join("")}
            </div>
            <div class="quiz-nav">
                <button class="quiz-secondary" type="button" data-quiz-action="prev" ${index === 0 ? "disabled" : ""}>上一题</button>
                <button class="quiz-primary" type="button" data-quiz-action="${index === state.quiz.words.length - 1 ? "finish" : "next"}" ${answered ? "" : "disabled"}>
                    ${index === state.quiz.words.length - 1 ? "完成" : "下一题"}
                </button>
            </div>
        </article>`;
    bindWordQuizActions();
}

function renderQuizOption(meaning, answer, word) {
    const answered = Boolean(answer);
    const selected = answer?.selected === meaning;
    const correct = word.meaningCn === meaning;
    let className = "quiz-option";
    if (answered && correct) className += " is-correct";
    if (answered && selected && !correct) className += " is-wrong";
    if (selected) className += " is-selected";
    return `
        <button class="${className}" type="button" data-meaning="${escapeAttribute(meaning)}" ${answered ? "disabled" : ""}>
            ${meaning}
        </button>`;
}

function renderWordExample(word, aiState) {
    const english = word.exampleEn || `Try to use ${word.word} in a complete sentence.`;
    const chinese = word.exampleCn || "试着把这个单词放进一个完整句子里。";
    const source = word.exampleSource || word.sourceName || "";
    return `
        <aside class="quiz-example-panel" aria-label="单词例句">
            <p class="quiz-example-en">${highlightTerm(english, word.word)}</p>
            <p class="quiz-example-cn">${escapeHtml(chinese)}</p>
            ${source ? `<span class="quiz-example-source">出处：${escapeHtml(source)}</span>` : ""}
        </aside>`;
}

function renderQuizDetailAction(word, aiState) {
    const loading = aiState?.status === "loading";
    const ready = aiState?.status === "ready";
    return `
        <div class="quiz-detail-actions">
            <button class="quiz-detail-button ${loading ? "is-loading" : ""} ${ready ? "is-done" : ""}"
                    type="button"
                    data-ai-detail-word-id="${word.id}"
                    ${loading ? "disabled" : ""}>
                ${loading ? "生成中..." : ready ? "已生成解析" : "深度解析"}
            </button>
        </div>`;
}

function renderWordAiDetail(word, aiState, variant = "") {
    if (!aiState) return "";
    if (aiState.status === "loading") {
        return `
            <section class="quiz-ai-detail ${variant} is-loading">
                <span class="quiz-ai-spinner" aria-hidden="true"></span>
                <span>正在生成 ${escapeHtml(word.word)} 的深度解析...</span>
            </section>`;
    }
    if (aiState.status === "error") {
        return `
            <section class="quiz-ai-detail ${variant} is-error">
                <span>${escapeHtml(aiState.message || "模型未配置或暂时不可用，请检查 AI 设置")}</span>
                <button class="quiz-detail-button retry-detail-button" type="button" data-ai-detail-retry-id="${word.id}">
                    重试
                </button>
            </section>`;
    }
    const detail = aiState.data || {};
    const examples = Array.isArray(detail.examples) ? detail.examples : [];
    const variants = Array.isArray(detail.variants) ? detail.variants : [];
    const usageNotes = Array.isArray(detail.usageNotes) ? detail.usageNotes : [];
    const collocations = Array.isArray(detail.collocations) ? detail.collocations : [];
    return `
        <section class="quiz-ai-detail ${variant}">
            <div class="quiz-ai-head">
                <strong>深度解析 · ${escapeHtml(detail.word || word.word)}</strong>
                <span>${escapeHtml(detail.modelName || "")}${detail.cached ? " · 已缓存" : ""}</span>
            </div>
            <div class="quiz-ai-grid">
                ${renderAiSection("更多例句", examples.map((item) => `
                    <p>${highlightTerm(item.en || "", word.word)}</p>
                    ${item.cn ? `<small>${escapeHtml(item.cn)}</small>` : ""}
                `))}
                ${renderAiSection("词性变种", variants.map((item) => `
                    <p><b>${escapeHtml(item.form || "")}</b> ${escapeHtml(item.type || "")}</p>
                    ${item.meaningCn ? `<small>${escapeHtml(item.meaningCn)}</small>` : ""}
                `))}
                ${renderAiSection("用法提示", usageNotes.map((item) => `<p>${escapeHtml(item)}</p>`))}
                ${renderAiSection("固定搭配", collocations.map((item) => `
                    <p><b>${escapeHtml(item.phrase || "")}</b> ${escapeHtml(item.meaningCn || "")}</p>
                    ${item.exampleEn ? `<small>${highlightTerm(item.exampleEn, word.word)}</small>` : ""}
                `))}
            </div>
        </section>`;
}

function renderAiSection(title, rows) {
    const cleanRows = rows.filter((row) => row && row.trim());
    if (!cleanRows.length) return "";
    return `
        <div class="quiz-ai-section">
            <h3>${escapeHtml(title)}</h3>
            ${cleanRows.map((row) => `<div class="quiz-ai-row">${row}</div>`).join("")}
        </div>`;
}

function highlightTerm(text, term) {
    if (!term) return escapeHtml(text);
    const pieces = String(text).split(new RegExp(`(${escapeRegExp(term)})`, "gi"));
    return pieces.map((piece) => (
        piece.toLowerCase() === term.toLowerCase()
            ? `<mark>${escapeHtml(piece)}</mark>`
            : escapeHtml(piece)
    )).join("");
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;");
}

function escapeRegExp(value) {
    return String(value).replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function bindWordQuizActions() {
    $$(".quiz-option").forEach((option) => {
        option.addEventListener("click", () => answerCurrentWord(option.dataset.meaning));
    });
    $$("[data-quiz-action]").forEach((button) => {
        button.addEventListener("click", () => {
            const action = button.dataset.quizAction;
            if (action === "prev") {
                state.quiz.index = Math.max(0, state.quiz.index - 1);
                renderWordQuiz();
            }
            if (action === "next") {
                state.quiz.index = Math.min(state.quiz.words.length - 1, state.quiz.index + 1);
                renderWordQuiz();
            }
            if (action === "finish") {
                state.quiz.completed = true;
                renderWordQuiz();
            }
            if (action === "restart") {
                loadWords({ updateToday: false }).catch((error) => console.error(error));
            }
        });
    });
    $$("[data-ai-detail-word-id]").forEach((button) => {
        button.addEventListener("click", () => loadWordAiDetail(button.dataset.aiDetailWordId, "quiz"));
    });
    $$("[data-ai-detail-retry-id]").forEach((button) => {
        button.addEventListener("click", () => {
            loadWordAiDetail(button.dataset.aiDetailRetryId, button.closest(".quiz-ai-detail")?.classList.contains("inline") ? "inline" : "quiz", true);
        });
    });
}

async function answerCurrentWord(selectedMeaning) {
    const word = state.quiz.words[state.quiz.index];
    if (!word || state.quiz.answers[word.id]) return;
    const scrollX = window.scrollX;
    const scrollY = window.scrollY;
    const result = await api(`/api/words/${word.id}/quiz`, {
        method: "POST",
        body: JSON.stringify({ answer: selectedMeaning })
    });
    applyQuizResultToWord(word.id, result);
    state.quiz.answers[word.id] = {
        selected: selectedMeaning,
        correct: result.correct,
        expectedMeaning: result.expectedMeaning
    };
    renderWordQuiz();
    window.scrollTo(scrollX, scrollY);
    await loadReviews();
    await loadReport();
}

function applyQuizResultToWord(wordId, result) {
    const patch = (word) => word.id === wordId ? {
        ...word,
        exampleEn: result.exampleEn || word.exampleEn,
        exampleCn: result.exampleCn || word.exampleCn
    } : word;
    state.words = state.words.map(patch);
    state.quiz.words = state.quiz.words.map(patch);
    state.review.words = state.review.words.map(patch);
    state.wordBank.words = state.wordBank.words.map(patch);
}

async function loadWordAiDetail(wordId, source = "quiz", force = false) {
    if (!wordId) return;
    const store = source === "quiz" ? state.quiz.aiDetails : state.wordAiDetails;
    if (source === "inline") {
        state.inlineAnsweredWords[wordId] = true;
    }
    const existing = store[wordId];
    if (!force && (existing?.status === "loading" || existing?.status === "ready")) return;
    store[wordId] = { status: "loading" };
    refreshWordAiDetailUi(wordId, source);
    try {
        const detail = await api(`/api/words/${wordId}/ai-detail`);
        store[wordId] = { status: "ready", data: detail };
    } catch (error) {
        store[wordId] = {
            status: "error",
            message: error.message || "模型未配置或暂时不可用，请检查 AI 设置"
        };
    }
    refreshWordAiDetailUi(wordId, source);
}

function refreshWordAiDetailUi(wordId, source) {
    if (source === "inline") {
        refreshInlineWordAiDetail(wordId);
    } else if (source === "wordbank") {
        refreshWordBankDialog();
    } else {
        renderWordQuiz();
    }
}

async function requireCurrentUser() {
    const user = await api("/api/auth/me");
    if (!user) {
        window.location.href = "/login";
        return null;
    }
    state.currentUser = user;
    renderCurrentUser(user);
    return user;
}

function renderCurrentUser(user) {
    const name = user.nickname || user.username;
    const nameEl = $("#currentUserName");
    const roleEl = $("#currentUserRole");
    const adminLink = $("#adminEntry");
    if (nameEl) nameEl.textContent = name;
    if (roleEl) roleEl.textContent = user.role === "ADMIN" ? "管理员" : "学习者";
    if (adminLink) adminLink.hidden = user.role !== "ADMIN";
    document.body.classList.toggle("is-admin", user.role === "ADMIN");
}

async function logout() {
    await api("/api/auth/logout", { method: "POST" });
    window.location.href = "/login";
}

function refreshInlineWordAiDetail(wordId) {
    const word = findWordById(wordId);
    if (!word) return;
    document.querySelectorAll(`[data-word-card-id="${wordId}"]`).forEach((card) => {
        const example = card.querySelector(".word-card-example");
        if (example) {
            example.outerHTML = renderInlineWordExample(word);
        }
        const detailActions = card.querySelector(".word-card-detail-actions");
        if (detailActions) {
            detailActions.outerHTML = renderInlineDetailAction(word, state.wordAiDetails[wordId]);
        } else {
            card.insertAdjacentHTML("beforeend", renderInlineDetailAction(word, state.wordAiDetails[wordId]));
        }
        bindInlineDetailAction(card);
        card.querySelector(".quiz-ai-detail.inline")?.remove();
        card.insertAdjacentHTML("beforeend", renderWordAiDetail(word, state.wordAiDetails[wordId], "inline"));
        card.querySelector("[data-ai-detail-retry-id]")?.addEventListener("click", (event) => {
            loadWordAiDetail(event.currentTarget.dataset.aiDetailRetryId, "inline", true);
        });
    });
}

async function toggleFavorite(wordId, button) {
    if (!wordId || button?.disabled) return;
    if (button) {
        button.disabled = true;
        button.classList.add("is-animating");
    }
    try {
        const updated = await api(`/api/words/${wordId}/favorite`, { method: "POST" });
        applyFavoriteUpdate(updated);
        await loadReport();
        const refreshed = document.querySelector(`[data-favorite-id="${wordId}"]`);
        if (refreshed) {
            refreshed.classList.add("is-animating");
            window.setTimeout(() => refreshed.classList.remove("is-animating"), 520);
        }
    } catch (error) {
        if (button) {
            button.disabled = false;
            button.classList.remove("is-animating");
        }
        throw error;
    }
}

function applyFavoriteUpdate(updatedWord) {
    const patch = (word) => word.id === updatedWord.id ? { ...word, favorite: updatedWord.favorite } : word;
    state.words = state.words.map(patch);
    state.quiz.words = state.quiz.words.map(patch);
    state.review.words = state.review.words.map(patch);
    state.wordBank.words = state.wordBank.words.map(patch);
    if (state.review.mode === "favorites" && !updatedWord.favorite) {
        state.review.words = state.review.words.filter((word) => word.id !== updatedWord.id);
    }
    renderTodayWords();
    renderReviewWords();
    renderWordQuiz();
    renderWordBank();
    if (state.wordBank.selectedWordId === updatedWord.id) {
        refreshWordBankDialog();
    }
}

function escapeAttribute(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("\"", "&quot;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;");
}

function appendMessage(role, content) {
    const box = document.createElement("div");
    box.className = `message ${role}`;
    box.textContent = content;
    $("#chatMessages").appendChild(box);
    box.scrollIntoView({ behavior: "smooth", block: "end" });
}

function renderTrace(trace) {
    $("#traceList").innerHTML = trace.map((step, index) => `
        <div class="trace-step" style="animation-delay: ${index * 80}ms">
            <strong>${step.phase} · ${step.title}</strong>
            <span>${step.detail}</span>
        </div>`).join("");
}

function formatConvTime(dateStr) {
    if (!dateStr) return "";
    const d = new Date(dateStr);
    if (isNaN(d.getTime())) return "";
    const now = new Date();
    const diff = now - d;
    if (diff < 60000) return "刚刚";
    if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`;
    if (diff < 86400000 && d.getDate() === now.getDate()) {
        return `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
    }
    if (diff < 604800000) {
        return ["周日", "周一", "周二", "周三", "周四", "周五", "周六"][d.getDay()];
    }
    return `${d.getMonth() + 1}/${d.getDate()}`;
}

function sceneLabel(scene) {
    return ({ daily: "日常", exam: "考试", business: "商务", reading: "阅读" })[scene] || scene;
}

async function loadConversations() {
    const list = await api("/api/conversations");
    $("#convList").innerHTML = list.map(c => `
        <div class="conv-item${c.id === state.conversationId ? " is-active" : ""}"
             data-conv-id="${c.id}">
            <span class="conv-title">${escapeHtml(c.title || "新对话")}</span>
            <span class="conv-meta">${sceneLabel(c.scene)} · ${formatConvTime(c.updatedAt)}</span>
        </div>`).join("");
    return list;
}

async function switchConversation(id) {
    state.conversationId = id;
    const messages = await api(`/api/conversations/${id}/messages`);
    $("#chatMessages").innerHTML = messages.map(m =>
        `<div class="message ${m.role}">${escapeHtml(m.content)}</div>`).join("");
    renderTrace([]);
    $$(".conv-item").forEach(el => el.classList.toggle("is-active", +el.dataset.convId === id));
}

function newConversation() {
    state.conversationId = null;
    $("#chatMessages").innerHTML = "";
    renderTrace([]);
    $$(".conv-item").forEach(el => el.classList.remove("is-active"));
    $("#chatInput").focus();
}

$("#convList").addEventListener("click", (e) => {
    const item = e.target.closest(".conv-item");
    if (item) switchConversation(+item.dataset.convId);
});

async function sendChat(event) {
    event.preventDefault();
    const input = $("#chatInput");
    const message = input.value.trim();
    if (!message) return;
    appendMessage("user", message);
    input.value = "";
    appendMessage("assistant", "正在思考...");
    const result = await api("/api/chat", {
        method: "POST",
        body: JSON.stringify({
            conversationId: state.conversationId,
            scene: $("#chatScene").value,
            message
        })
    });
    state.conversationId = result.conversationId;
    const messages = $$("#chatMessages .message");
    messages[messages.length - 1].textContent = result.reply;
    renderTrace(result.trace);
    loadConversations();
}

async function reviewEssay(event) {
    event.preventDefault();
    const essayText = $("#essayText").value.trim();
    if (!essayText) return;
    $("#writingResult").textContent = "正在批改...";
    const result = await api("/api/writing/review", {
        method: "POST",
        body: JSON.stringify({
            title: $("#essayTitle").value,
            examType: $("#examType").value,
            targetLevel: "intermediate",
            essayText
        })
    });
    $("#writingResult").innerHTML = `
        <p class="writing-meta">批改来源：${escapeHtml(result.reviewSource || "AI批改")}</p>
        <p class="writing-meta">使用模型：${escapeHtml(result.modelName || "未配置")}</p>
        <div class="score-grid">
            <div><strong>${Math.round(result.totalScore)}</strong><span>总分</span></div>
            <div><strong>${Math.round(result.grammarScore)}</strong><span>语法</span></div>
            <div><strong>${Math.round(result.vocabularyScore)}</strong><span>词汇</span></div>
            <div><strong>${Math.round(result.logicScore)}</strong><span>逻辑</span></div>
        </div>
        <p>${result.summary}</p>
        <h3>润色版本</h3>
        <p>${result.polishedText}</p>
        <h3>逐句建议</h3>
        ${result.feedback.map((item) => `
            <div class="feedback-item">
                <strong>${item.category}</strong>
                <p>${item.suggestion}</p>
                <p>${item.correctedSentence}</p>
            </div>`).join("")}`;
    renderTrace(result.trace);
    await loadReport();
}

async function loadNews() {
    const difficulty = $("#newsDifficulty").value;
    const list = await api(`/api/news/recommendations?difficulty=${encodeURIComponent(difficulty)}`);
    $("#newsList").innerHTML = list.map((item) => `
        <button class="news-item" data-news-id="${item.id}">
            <strong>${item.title}</strong>
            <span>${item.difficulty} · ${item.topicTags}</span>
            <p>${item.summary}</p>
        </button>`).join("");
    $$(".news-item").forEach((item) => {
        item.addEventListener("click", () => loadArticle(item.dataset.newsId));
    });
    if (list[0]) {
        await loadArticle(list[0].id);
    } else {
        state.currentArticle = null;
        clearTranslation();
        $("#readerTitle").textContent = "暂无文章";
        $("#readerSummary").textContent = "换一个难度后再试试。";
        $("#readerContent").innerHTML = "";
    }
}

async function loadArticle(id) {
    const article = await api(`/api/news/${id}`);
    state.currentArticle = { title: article.title, content: article.content };
    clearTranslation();
    $$(".news-item").forEach((item) => {
        item.classList.toggle("is-active", String(item.dataset.newsId) === String(id));
    });
    $("#readerTitle").textContent = article.title;
    $("#readerSummary").textContent = article.summary;
    $("#readerContent").innerHTML = article.content
        .split(/(\s+)/)
        .map((token) => {
            if (/^[A-Za-z][A-Za-z'-]*[.,;:!?]?$/.test(token)) {
                const clean = token.replace(/[^A-Za-z'-]/g, "");
                const normalized = clean.toLowerCase();
                if (clean.length < 3 || lookupStopWords.has(normalized)) {
                    return token;
                }
                return `<span class="reader-word" data-word="${clean}">${token}</span>`;
            }
            return token;
        })
        .join("");
}

async function showTooltip(event) {
    const word = event.target.dataset.word;
    if (!word || state.tooltipWord === word) return;
    state.tooltipWord = word;
    const result = await api(`/api/dictionary/lookup?word=${encodeURIComponent(word)}`);
    const tooltip = $("#wordTooltip");
    tooltip.innerHTML = `<strong>${escapeHtml(result.word)}</strong> <span class="phonetic">${escapeHtml(formatPhonetic(result.phoneticUs))}</span><br>${escapeHtml(result.meaningCn)}<br>${result.partOfSpeech ? `<span>${escapeHtml(result.partOfSpeech)}</span>` : ""}`;
    tooltip.style.left = `${Math.min(window.innerWidth - 300, event.clientX + 14)}px`;
    tooltip.style.top = `${event.clientY + 18}px`;
    tooltip.classList.add("visible");
}

function hideTooltip() {
    state.tooltipWord = null;
    $("#wordTooltip").classList.remove("visible");
}

function makeReaderHtml(text) {
    return (text || "")
        .split(/(\s+)/)
        .map((token) => {
            if (/^[A-Za-z][A-Za-z'-]*[.,;:!?]?$/.test(token)) {
                const clean = token.replace(/[^A-Za-z'-]/g, "");
                const normalized = clean.toLowerCase();
                if (clean.length < 3 || lookupStopWords.has(normalized)) {
                    return token;
                }
                return `<span class="reader-word" data-word="${clean}">${token}</span>`;
            }
            return token;
        })
        .join("");
}

function clearTranslation() {
    $("#translationOutput").innerHTML = "";
    state.translateBusy = false;
    const btn = $("#translateBtn");
    if (btn) { btn.querySelector("span").textContent = "翻译"; btn.classList.remove("is-done"); }
}

async function translateArticle() {
    if (!state.currentArticle || state.translateBusy) return;
    state.translateBusy = true;
    const btnLabel = $("#translateBtn").querySelector("span");
    btnLabel.textContent = "翻译中...";
    try {
        const result = await api("/api/news/translate", {
            method: "POST",
            body: JSON.stringify(state.currentArticle),
        });
        const html = [];
        if (result.titleCn) {
            html.push(`<h3 class="translation-heading">中文翻译</h3>`);
            html.push(`<p class="translation-title">${escapeHtml(result.titleCn)}</p>`);
        }
        if (result.contentCn) {
            html.push(`<div class="translation-body">${escapeHtml(result.contentCn)}</div>`);
        }
        $("#translationOutput").innerHTML = html.join("");
        const btn = $("#translateBtn");
        if (btn) { btn.querySelector("span").textContent = "已翻译"; btn.classList.add("is-done"); }
    } catch (error) {
        console.error(error);
        state.translateBusy = false;
        const btnLabel = $("#translateBtn").querySelector("span");
        btnLabel.textContent = "翻译";
    }
}

async function loadWordBankCategories() {
    try {
        state.wordBank.categories = await api("/api/word-bank/categories");
    } catch (error) {
        console.error(error);
        state.wordBank.categories = [];
    }
    const tabs = $("#wordBankCategories");
    if (!tabs) return;
    const allActive = state.wordBank.category === "" ? " is-active" : "";
    tabs.innerHTML = `<button class="review-tab${allActive}" type="button" data-category="">全部</button>` +
        state.wordBank.categories.map((cat) => {
            const active = state.wordBank.category === cat.tag ? " is-active" : "";
            return `<button class="review-tab${active}" type="button" data-category="${escapeAttribute(cat.tag)}">${escapeHtml(cat.label)} (${cat.count})</button>`;
        }).join("");
}

async function loadWordBankWords() {
    const { category, search, page, size } = state.wordBank;
    const params = new URLSearchParams({ page, size });
    if (category) params.set("category", category);
    if (search) params.set("search", search);
    try {
        const result = await api(`/api/word-bank/words?${params}`);
        state.wordBank.words = result.words;
        state.wordBank.total = result.total;
        state.wordBank.page = result.page;
        state.wordBank.size = result.size;
        state.wordBank.loaded = true;
    } catch (error) {
        console.error(error);
        state.wordBank.words = [];
        state.wordBank.total = 0;
        state.wordBank.loaded = true;
    }
    renderWordBank();
}

function renderWordBank() {
    const { words, total, page, size, loaded } = state.wordBank;
    const totalPages = Math.max(1, Math.ceil(total / size));

    // Update total badge
    const totalEl = $("#wordBankTotal");
    if (totalEl) totalEl.textContent = loaded ? `共 ${total} 词` : "加载中...";

    // Render word grid
    const grid = $("#wordBankGrid");
    if (!grid) return;
    if (!words.length) {
        grid.innerHTML = `<div class="empty-state">${loaded ? "没有匹配的单词，换个搜索词试试。" : "正在加载词库..."}</div>`;
    } else {
        grid.innerHTML = words.map((word) => wordCard(word, true)).join("");
    }

    // Render pagination
    const pagination = $("#wordBankPagination");
    if (!pagination) return;
    if (totalPages <= 1) {
        pagination.innerHTML = "";
        return;
    }
    let html = `<span class="wordbank-page-info">第 ${page + 1} / ${totalPages} 页</span>`;
    html += `<button class="wordbank-page-btn" type="button" data-page="0" ${page === 0 ? "disabled" : ""}>首页</button>`;
    html += `<button class="wordbank-page-btn" type="button" data-page="${page - 1}" ${page === 0 ? "disabled" : ""}>上一页</button>`;
    html += `<button class="wordbank-page-btn" type="button" data-page="${page + 1}" ${page + 1 >= totalPages ? "disabled" : ""}>下一页</button>`;
    html += `<button class="wordbank-page-btn" type="button" data-page="${totalPages - 1}" ${page + 1 >= totalPages ? "disabled" : ""}>末页</button>`;
    pagination.innerHTML = html;

    // Re-bind mini quizzes
    bindQuizForms();
    bindWordBankCards();
}

function bindWordBankCards() {
    $("#wordBankGrid")?.querySelectorAll("[data-word-card-id]").forEach((card) => {
        card.addEventListener("click", (event) => {
            if (event.target.closest("button, input, form, a, [data-favorite-id]")) return;
            openWordBankDialog(card.dataset.wordCardId);
        });
    });
}

function bindWordBankEvents() {
    // Category tabs
    $("#wordBankCategories")?.addEventListener("click", (event) => {
        const tab = event.target.closest("[data-category]");
        if (!tab) return;
        state.wordBank.category = tab.dataset.category;
        state.wordBank.page = 0;
        loadWordBankCategories().catch(console.error);
        loadWordBankWords().catch(console.error);
    });

    // Search input (debounced)
    let searchTimer;
    $("#wordBankSearch")?.addEventListener("input", (event) => {
        clearTimeout(searchTimer);
        searchTimer = setTimeout(() => {
            state.wordBank.search = event.target.value.trim();
            state.wordBank.page = 0;
            loadWordBankWords().catch(console.error);
        }, 350);
    });

    // Pagination
    $("#wordBankPagination")?.addEventListener("click", (event) => {
        const btn = event.target.closest("[data-page]");
        if (!btn || btn.disabled) return;
        state.wordBank.page = parseInt(btn.dataset.page, 10);
        loadWordBankWords().catch(console.error);
        $("#wordbank").scrollIntoView({ behavior: "smooth", block: "start" });
    });

    $("#wordBankDialogBackdrop")?.addEventListener("click", closeWordBankDialog);
}

async function loadWordBank() {
    await loadWordBankCategories();
    await loadWordBankWords();
    bindWordBankEvents();
}

async function loadReport() {
    const [report, level] = await Promise.all([
        api("/api/report/summary"),
        api("/api/experience")
    ]);
    renderReport(report, level);
}

function initAmbientCanvas() {
    const canvas = $("#ambientCanvas");
    const cursorCanvas = $("#cursorCanvas");
    const cursorOrb = $("#cursorOrb");
    if (!canvas || window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;

    const ctx = canvas.getContext("2d");
    const cursorCtx = cursorCanvas?.getContext("2d");
    const tokens = ["context", "review", "essay", "listen", "speak", "read", "word", "grammar", "story", "phrase", "focus", "fluent"];
    const palette = [
        { core: "rgba(88, 204, 2, 0.18)", glow: "rgba(88, 204, 2, 0.07)", rim: "rgba(88, 204, 2, 0.38)", ink: "rgba(61, 146, 0, 0.68)" },
        { core: "rgba(28, 176, 246, 0.18)", glow: "rgba(28, 176, 246, 0.07)", rim: "rgba(28, 176, 246, 0.36)", ink: "rgba(7, 99, 146, 0.68)" },
        { core: "rgba(255, 200, 0, 0.18)", glow: "rgba(255, 200, 0, 0.07)", rim: "rgba(255, 200, 0, 0.36)", ink: "rgba(106, 82, 0, 0.66)" },
        { core: "rgba(139, 116, 255, 0.14)", glow: "rgba(139, 116, 255, 0.06)", rim: "rgba(139, 116, 255, 0.3)", ink: "rgba(77, 59, 165, 0.62)" }
    ];
    const nodes = [];
    const trail = [];
    let width = 0;
    let height = 0;
    let ratio = 1;
    let hoveredNode = null;
    let lastFrameTime = 0;

    function resize() {
        ratio = Math.min(window.devicePixelRatio || 1, 2);
        width = window.innerWidth;
        height = window.innerHeight;
        canvas.width = Math.floor(width * ratio);
        canvas.height = Math.floor(height * ratio);
        canvas.style.width = `${width}px`;
        canvas.style.height = `${height}px`;
        ctx.setTransform(ratio, 0, 0, ratio, 0, 0);
        if (cursorCanvas && cursorCtx) {
            cursorCanvas.width = Math.floor(width * ratio);
            cursorCanvas.height = Math.floor(height * ratio);
            cursorCanvas.style.width = `${width}px`;
            cursorCanvas.style.height = `${height}px`;
            cursorCtx.setTransform(ratio, 0, 0, ratio, 0, 0);
        }
        seedNodes();
    }

    function desiredNodeCount() {
        const base = Math.round((width * height) / (width < 760 ? 62000 : 56000));
        return Math.max(width < 760 ? 8 : 15, Math.min(width < 760 ? 16 : 32, base));
    }

    function seedNodes() {
        const target = desiredNodeCount();
        while (nodes.length < target) {
            const node = createCanvasNode(nodes.length);
            nodes.push(node);
        }
        nodes.length = target;
        nodes.forEach((node, index) => {
            prepareCanvasNode(node, index, false);
            containCanvasNode(node);
        });
        resolveCanvasCollisions(8);
    }

    function randomSpeed() {
        const min = width < 760 ? 0.2 : 0.28;
        const max = width < 760 ? 0.34 : 0.5;
        return min + Math.random() * (max - min);
    }

    function setRandomVelocity(node) {
        const angle = Math.random() * Math.PI * 2;
        node.speed = randomSpeed();
        node.vx = Math.cos(angle) * node.speed;
        node.vy = Math.sin(angle) * node.speed;
    }

    function createCanvasNode(index) {
        const node = {
            x: 0,
            y: 0,
            vx: 0,
            vy: 0,
            speed: 0,
            phase: Math.random() * Math.PI * 2,
            scale: 1,
            theme: palette[index % palette.length],
            token: tokens[index % tokens.length],
            age: 0,
            life: 12,
            bump: 0
        };
        prepareCanvasNode(node, index, true);
        return node;
    }

    function prepareCanvasNode(node, index, shouldPlace) {
        node.token = tokens[Math.floor(Math.random() * tokens.length)];
        node.theme = palette[(index + Math.floor(Math.random() * palette.length)) % palette.length];
        node.scale = 0.92 + Math.random() * 0.34;
        node.phase = Math.random() * Math.PI * 2;
        node.age = 0;
        node.life = 10 + Math.random() * 8;
        node.bump = 0;
        setRandomVelocity(node);
        if (shouldPlace) {
            placeCanvasNode(node);
        }
    }

    function recordPointer(event) {
        if (cursorOrb && window.matchMedia("(pointer: fine)").matches) {
            document.body.classList.add("has-custom-cursor");
            cursorOrb.style.left = `${event.clientX}px`;
            cursorOrb.style.top = `${event.clientY}px`;
        }
        hoveredNode = findHoveredNode(event.clientX, event.clientY);
        trail.unshift({ x: event.clientX, y: event.clientY, life: 1 });
        if (trail.length > 42) trail.length = 42;
    }

    function nodeFontSize(node) {
        const base = width < 760 ? 12 : 15;
        return Math.round(base * Math.min(1.14, Math.max(0.94, node.scale)));
    }

    function nodeRadius(node) {
        ctx.save();
        ctx.font = `900 ${nodeFontSize(node)}px Segoe UI, Arial`;
        const textWidth = ctx.measureText(node.token).width;
        ctx.restore();
        const minRadius = (width < 760 ? 34 : 42) * node.scale;
        const textRadius = (textWidth / 2) + (width < 760 ? 18 : 23);
        return Math.max(minRadius, textRadius);
    }

    function collisionRadius(node) {
        return nodeRadius(node) + 8;
    }

    function placeCanvasNode(node) {
        const radius = collisionRadius(node);
        let best = null;
        let bestGap = -Infinity;
        for (let attempt = 0; attempt < 120; attempt += 1) {
            const x = radius + Math.random() * Math.max(1, width - radius * 2);
            const y = radius + Math.random() * Math.max(1, height - radius * 2);
            let nearestGap = Infinity;
            const clear = nodes.every((other) => {
                if (other === node) return true;
                const minDistance = radius + collisionRadius(other) + 10;
                const gap = Math.hypot(x - other.x, y - other.y) - minDistance;
                nearestGap = Math.min(nearestGap, gap);
                return gap >= 0;
            });
            if (nearestGap > bestGap) {
                bestGap = nearestGap;
                best = { x, y };
            }
            if (clear) {
                node.x = x;
                node.y = y;
                return;
            }
        }
        node.x = best?.x ?? radius + Math.random() * Math.max(1, width - radius * 2);
        node.y = best?.y ?? radius + Math.random() * Math.max(1, height - radius * 2);
    }

    function findHoveredNode(x, y) {
        for (let i = nodes.length - 1; i >= 0; i -= 1) {
            const node = nodes[i];
            const radius = nodeRadius(node);
            if (Math.hypot(x - node.x, y - node.y) <= radius) {
                return node;
            }
        }
        return null;
    }

    function containCanvasNode(node) {
        const radius = collisionRadius(node);
        if (node.x < radius) {
            node.x = radius;
            node.vx = Math.abs(node.vx);
        } else if (node.x > width - radius) {
            node.x = width - radius;
            node.vx = -Math.abs(node.vx);
        }
        if (node.y < radius) {
            node.y = radius;
            node.vy = Math.abs(node.vy);
        } else if (node.y > height - radius) {
            node.y = height - radius;
            node.vy = -Math.abs(node.vy);
        }
    }

    function limitCanvasVelocity(node) {
        const minSpeed = width < 760 ? 0.18 : 0.25;
        const maxSpeed = width < 760 ? 0.4 : 0.56;
        const speed = Math.hypot(node.vx, node.vy);
        if (!speed) {
            setRandomVelocity(node);
            return;
        }
        if (speed < minSpeed) {
            node.vx = (node.vx / speed) * minSpeed;
            node.vy = (node.vy / speed) * minSpeed;
            node.speed = minSpeed;
            return;
        }
        if (speed > maxSpeed) {
            node.vx = (node.vx / speed) * maxSpeed;
            node.vy = (node.vy / speed) * maxSpeed;
            node.speed = maxSpeed;
            return;
        }
        node.speed = speed;
    }

    function resolveCanvasCollisions(iterations = 2) {
        for (let pass = 0; pass < iterations; pass += 1) {
            for (let i = 0; i < nodes.length; i += 1) {
                for (let j = i + 1; j < nodes.length; j += 1) {
                    const a = nodes[i];
                    const b = nodes[j];
                    const minDistance = collisionRadius(a) + collisionRadius(b) + 2;
                    let dx = a.x - b.x;
                    let dy = a.y - b.y;
                    let distance = Math.hypot(dx, dy);
                    if (!distance) {
                        dx = 1;
                        dy = 0;
                        distance = 1;
                    }
                    if (distance >= minDistance) continue;

                    const nx = dx / distance;
                    const ny = dy / distance;
                    const overlap = minDistance - distance;
                    const aFixed = a === hoveredNode;
                    const bFixed = b === hoveredNode;

                    if (aFixed && !bFixed) {
                        b.x -= nx * overlap;
                        b.y -= ny * overlap;
                    } else if (bFixed && !aFixed) {
                        a.x += nx * overlap;
                        a.y += ny * overlap;
                    } else {
                        a.x += nx * overlap * 0.5;
                        a.y += ny * overlap * 0.5;
                        b.x -= nx * overlap * 0.5;
                        b.y -= ny * overlap * 0.5;
                    }

                    const relativeVelocity = ((a.vx - b.vx) * nx) + ((a.vy - b.vy) * ny);
                    if (relativeVelocity < 0) {
                        const impulse = -relativeVelocity * 1.04;
                        if (!aFixed) {
                            a.vx += impulse * nx;
                            a.vy += impulse * ny;
                        }
                        if (!bFixed) {
                            b.vx -= impulse * nx;
                            b.vy -= impulse * ny;
                        }
                    }
                    a.bump = 1;
                    b.bump = 1;
                    limitCanvasVelocity(a);
                    limitCanvasVelocity(b);
                    containCanvasNode(a);
                    containCanvasNode(b);
                }
            }
        }
    }

    function nodeLifeAlpha(node) {
        const fadeIn = Math.min(1, node.age / 1.2);
        const fadeOut = Math.min(1, Math.max(0, (node.life - node.age) / 1.6));
        return Math.max(0, Math.min(fadeIn, fadeOut));
    }

    function respawnCanvasNode(node, index) {
        prepareCanvasNode(node, index, true);
        resolveCanvasCollisions(4);
    }

    function drawTrail() {
        if (!cursorCtx || trail.length < 2) return;
        cursorCtx.lineCap = "round";
        cursorCtx.lineJoin = "round";
        cursorCtx.save();
        cursorCtx.shadowColor = "rgba(88, 204, 2, 0.48)";
        cursorCtx.shadowBlur = 16;
        for (let i = 0; i < trail.length - 1; i += 1) {
            const point = trail[i];
            const next = trail[i + 1];
            const alpha = Math.max(0, point.life) * 0.82;
            cursorCtx.strokeStyle = `rgba(88, 204, 2, ${alpha})`;
            cursorCtx.lineWidth = 12 * point.life;
            cursorCtx.beginPath();
            cursorCtx.moveTo(point.x, point.y);
            cursorCtx.lineTo(next.x, next.y);
            cursorCtx.stroke();
            cursorCtx.strokeStyle = `rgba(28, 176, 246, ${alpha * 0.62})`;
            cursorCtx.lineWidth = 4 * point.life;
            cursorCtx.beginPath();
            cursorCtx.moveTo(point.x, point.y);
            cursorCtx.lineTo(next.x, next.y);
            cursorCtx.stroke();
        }
        cursorCtx.restore();
        const head = trail[0];
        if (head) {
            cursorCtx.fillStyle = `rgba(255, 200, 0, ${head.life * 0.72})`;
            cursorCtx.beginPath();
            cursorCtx.arc(head.x, head.y, 3.5 + head.life * 2, 0, Math.PI * 2);
            cursorCtx.fill();
        }
    }

    function drawNodes(time) {
        const dt = Math.min(2.2, Math.max(0.5, (time - lastFrameTime) / 16.67 || 1));
        lastFrameTime = time;
        for (let i = 0; i < nodes.length; i += 1) {
            const node = nodes[i];
            if (node !== hoveredNode) {
                node.age += dt / 60;
                if (node.age >= node.life) {
                    respawnCanvasNode(node, i);
                }
                node.x += node.vx * dt;
                node.y += node.vy * dt;
            }
            containCanvasNode(node);
        }
        resolveCanvasCollisions();
        nodes.forEach(limitCanvasVelocity);

        ctx.textAlign = "center";
        ctx.textBaseline = "middle";
        nodes.forEach((node) => {
            const hovered = node === hoveredNode;
            const lifeAlpha = nodeLifeAlpha(node);
            const pulse = hovered ? 1 : 0.78 + Math.sin(time / 1100 + node.phase) * 0.06;
            const bump = Math.max(0, node.bump || 0);
            const bumpScale = 1 + bump * 0.08;
            const radius = nodeRadius(node) * bumpScale;
            const theme = node.theme || palette[0];
            ctx.save();
            ctx.globalAlpha = lifeAlpha * pulse * (width < 760 ? 0.72 : 0.88);
            if (hovered || bump > 0.02) {
                ctx.shadowColor = "rgba(35, 49, 67, 0.12)";
                ctx.shadowBlur = hovered ? 12 : 8;
            }
            const fill = ctx.createRadialGradient(
                node.x - radius * 0.36,
                node.y - radius * 0.42,
                1,
                node.x,
                node.y,
                radius * 1.28
            );
            fill.addColorStop(0, "rgba(255, 255, 255, 0.96)");
            fill.addColorStop(0.2, "rgba(255, 255, 255, 0.5)");
            fill.addColorStop(0.62, theme.core);
            fill.addColorStop(1, theme.glow);
            ctx.fillStyle = fill;
            ctx.strokeStyle = theme.rim;
            ctx.lineWidth = hovered ? 2 : 1.4;
            ctx.beginPath();
            ctx.arc(node.x, node.y, radius, 0, Math.PI * 2);
            ctx.fill();
            ctx.stroke();
            ctx.strokeStyle = "rgba(255, 255, 255, 0.58)";
            ctx.lineWidth = 1;
            ctx.beginPath();
            ctx.arc(node.x - radius * 0.08, node.y - radius * 0.1, radius * 0.67, Math.PI * 1.06, Math.PI * 1.73);
            ctx.stroke();
            ctx.fillStyle = "rgba(255, 255, 255, 0.62)";
            ctx.beginPath();
            ctx.arc(node.x - radius * 0.36, node.y - radius * 0.42, radius * 0.14, 0, Math.PI * 2);
            ctx.fill();
            ctx.strokeStyle = "rgba(255, 255, 255, 0.26)";
            ctx.lineWidth = 1;
            ctx.beginPath();
            ctx.arc(node.x, node.y, radius - 5, Math.PI * 0.15, Math.PI * 0.8);
            ctx.stroke();
            ctx.fillStyle = theme.ink;
            ctx.font = `900 ${nodeFontSize(node)}px Segoe UI, Arial`;
            ctx.fillText(node.token, node.x, node.y);
            ctx.restore();
            node.bump = bump * 0.86;
        });
        ctx.textAlign = "start";
        ctx.textBaseline = "alphabetic";
    }

    function render(time) {
        ctx.clearRect(0, 0, width, height);
        if (cursorCtx) cursorCtx.clearRect(0, 0, width, height);
        drawNodes(time);
        drawTrail();
        for (let i = trail.length - 1; i >= 0; i -= 1) {
            trail[i].life -= 0.025;
            if (trail[i].life <= 0) trail.splice(i, 1);
        }
        window.requestAnimationFrame(render);
    }

    resize();
    window.addEventListener("resize", resize);
    window.addEventListener("pointermove", recordPointer, { passive: true });
    window.requestAnimationFrame(render);
}

function initialSectionFromPath() {
    const sectionByPath = {
        chat: "chat",
        vocabulary: "vocabulary",
        review: "review",
        writing: "writing",
        news: "news",
        wordbank: "wordbank"
    };
    const path = window.location.pathname.replace(/^\/+|\/+$/g, "");
    return sectionByPath[path] || "dashboard";
}

async function boot() {
    const user = await requireCurrentUser();
    if (!user) return;
    initAmbientCanvas();
    $$(".nav-item").forEach((item) => item.addEventListener("click", () => {
        const section = item.dataset.section;
        showSection(section);
        if (section === "wordbank" && !state.wordBank.loaded) {
            loadWordBank().catch(console.error);
        }
    }));
    $$(".lesson-node, .soft-action").forEach((item) => item.addEventListener("click", () => showSection(item.dataset.section)));
    document.addEventListener("click", (event) => {
        const favorite = event.target.closest("[data-favorite-id]");
        if (favorite) {
            event.stopPropagation();
            toggleFavorite(favorite.dataset.favoriteId, favorite).catch((error) => console.error(error));
            return;
        }
        const reviewMode = event.target.closest("[data-review-mode]");
        if (reviewMode) {
            state.review.mode = reviewMode.dataset.reviewMode;
            loadReviews().catch((error) => console.error(error));
        }
    });
    $("#settingsFab")?.addEventListener("click", () => {
        loadAiSettings()
            .then(openSettings)
            .catch((error) => {
                console.error(error);
                setAiStatus(error.message || "加载设置失败");
                openSettings();
            });
    });
    $("#logoutBtn")?.addEventListener("click", () => logout().catch(console.error));
    $("#settingsClose")?.addEventListener("click", closeSettings);
    $("#settingsBackdrop")?.addEventListener("click", closeSettings);
    $("#fetchModelsBtn")?.addEventListener("click", () => {
        fetchAiModels().catch((error) => {
            console.error(error);
            setAiStatus(error.message || "\u83b7\u53d6\u6a21\u578b\u5931\u8d25");
        });
    });
    $("#saveAiSettingsBtn")?.addEventListener("click", () => {
        saveAiSettings().catch((error) => {
            console.error(error);
            setAiStatus(error.message || "\u4fdd\u5b58\u5931\u8d25");
        });
    });
    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            closeSettings();
            closeWordBankDialog();
        }
    });
    $("#chatForm").addEventListener("submit", sendChat);
    $("#writingForm").addEventListener("submit", reviewEssay);
    $("#newsDifficulty").addEventListener("change", loadNews);
    $("#quizCategorySelect")?.addEventListener("change", (event) => {
        state.quiz.category = event.target.value;
        loadWords({ updateToday: false }).catch((error) => console.error(error));
    });
    $("#translateBtn").addEventListener("click", translateArticle);
    $("#readerContent").addEventListener("mouseover", (event) => {
        if (event.target.classList.contains("reader-word")) {
            showTooltip(event).catch(() => hideTooltip());
        }
    });
    $("#readerContent").addEventListener("mouseout", (event) => {
        if (event.target.classList.contains("reader-word")) {
            hideTooltip();
        }
    });
    const initialSection = initialSectionFromPath();
    showSection(initialSection);
    if (initialSection === "wordbank") {
        loadWordBank().catch(console.error);
    }
    await Promise.all([
        loadReport(),
        loadQuizCategories().then(() => loadWords()),
        loadReviews(),
        loadNews()
    ]);
    const conversations = await loadConversations();
    if (!conversations.length) {
        appendMessage("assistant", "\u4f60\u597d\uff0c\u6211\u662f LinguaMate\u3002\u53ef\u4ee5\u966a\u4f60\u7ec3\u5bf9\u8bdd\u3001\u80cc\u5355\u8bcd\u3001\u6279\u6539\u4f5c\u6587\uff0c\u4e5f\u80fd\u6839\u636e\u914d\u7f6e\u597d\u7684\u6a21\u578b\u56de\u7b54\u95ee\u9898\u3002");
        renderTrace([
            { phase: "Observe", title: "\u7b49\u5f85\u8f93\u5165", detail: "\u9009\u62e9\u5de6\u4fa7\u573a\u666f\u6216\u76f4\u63a5\u8f93\u5165\u95ee\u9898\uff0c\u6211\u4f1a\u6574\u7406\u5b66\u4e60\u5efa\u8bae\u3002" },
            { phase: "Think", title: "\u51c6\u5907\u56de\u7b54", detail: "\u5df2\u8f7d\u5165\u672c\u5730\u5b66\u4e60\u5de5\u5177\u548c\u6a21\u578b\u914d\u7f6e\u3002" }
        ]);
    }
}

boot().catch((error) => {
    console.error(error);
});
