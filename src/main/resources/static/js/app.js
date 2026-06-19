const state = {
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
        index: 0,
        answers: {},
        options: [],
        completed: false
    },
    wordBank: {
        categories: [],
        words: [],
        total: 0,
        page: 0,
        size: 50,
        category: "",
        search: "",
        loaded: false
    }
};

const api = async (url, options = {}) => {
    const response = await fetch(url, {
        headers: { "Content-Type": "application/json", ...(options.headers || {}) },
        ...options
    });
    const payload = await response.json();
    if (!payload.success) {
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
        </article>`;
}

function renderTodayWords() {
    $("#todayWords").innerHTML = state.words.length
        ? state.words.slice(0, 4).map((word) => wordCard(word, true)).join("")
        : `<div class="empty-state">暂无单词，稍后重试。</div>`;
}

async function loadWords() {
    $("#todayWords").innerHTML = `<div class="empty-state">正在准备今日单词...</div>`;
    $("#wordCards").innerHTML = `<div class="quiz-empty">正在加载单词闯关...</div>`;
    try {
        state.words = await api("/api/words/today?limit=8");
    } catch (error) {
        console.error(error);
        state.words = [];
        $("#todayWords").innerHTML = `<div class="empty-state">单词加载失败，请刷新重试。</div>`;
        $("#wordCards").innerHTML = `<div class="quiz-empty">单词加载失败，请刷新重试。</div>`;
        return;
    }
    renderTodayWords();
    state.quiz = {
        index: 0,
        answers: {},
        options: state.words.map((word, index) => buildQuizOptions(word, index)),
        completed: false
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
            input.value = result.correct ? "已答对" : result.expectedMeaning;
            form.closest(".word-card").classList.add(result.correct ? "is-correct" : "is-wrong");
            await loadReviews();
            await loadReport();
        });
    });
}

function buildQuizOptions(word, index) {
    const distractors = [];
    for (let offset = 1; offset < state.words.length && distractors.length < 3; offset += 1) {
        const candidate = state.words[(index + offset) % state.words.length];
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
    if (!state.words.length) {
        container.innerHTML = `<div class="quiz-empty">今天暂时没有单词。</div>`;
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
    const word = state.words[index];
    const answer = state.quiz.answers[word.id];
    const answered = Boolean(answer);
    const options = state.quiz.options[index] || [];
    const progress = Math.round(((index + (answered ? 1 : 0)) / state.words.length) * 100);

    container.innerHTML = `
        <article class="word-quiz-card ${answered ? "is-answered" : ""}">
            <div class="quiz-progress">
                <span>第 ${index + 1} / ${state.words.length} 题</span>
                <div class="quiz-progress-track"><i style="width: ${progress}%"></i></div>
            </div>
            <div class="quiz-study-stage">
                <div class="quiz-word-shell">
                    ${favoriteButton(word, "quiz-favorite")}
                    <div class="quiz-word" aria-label="当前单词">${word.word}</div>
                </div>
                <div class="quiz-example-slot ${answered ? "is-visible" : ""}">
                    ${answered ? renderWordExample(word) : ""}
                </div>
            </div>
            <div class="quiz-options" role="list">
                ${options.map((meaning) => renderQuizOption(meaning, answer, word)).join("")}
            </div>
            <div class="quiz-nav">
                <button class="quiz-secondary" type="button" data-quiz-action="prev" ${index === 0 ? "disabled" : ""}>上一题</button>
                <button class="quiz-primary" type="button" data-quiz-action="${index === state.words.length - 1 ? "finish" : "next"}" ${answered ? "" : "disabled"}>
                    ${index === state.words.length - 1 ? "完成" : "下一题"}
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

function renderWordExample(word) {
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
                state.quiz.index = Math.min(state.words.length - 1, state.quiz.index + 1);
                renderWordQuiz();
            }
            if (action === "finish") {
                state.quiz.completed = true;
                renderWordQuiz();
            }
            if (action === "restart") {
                loadWords().catch((error) => console.error(error));
            }
        });
    });
}

async function answerCurrentWord(selectedMeaning) {
    const word = state.words[state.quiz.index];
    if (!word || state.quiz.answers[word.id]) return;
    const scrollX = window.scrollX;
    const scrollY = window.scrollY;
    const result = await api(`/api/words/${word.id}/quiz`, {
        method: "POST",
        body: JSON.stringify({ answer: selectedMeaning })
    });
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
    state.review.words = state.review.words.map(patch);
    state.wordBank.words = state.wordBank.words.map(patch);
    if (state.review.mode === "favorites" && !updatedWord.favorite) {
        state.review.words = state.review.words.filter((word) => word.id !== updatedWord.id);
    }
    renderTodayWords();
    renderReviewWords();
    renderWordQuiz();
    renderWordBank();
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

async function loadGNews(category) {
    $$(".gnews-tab").forEach((t) => t.classList.toggle("is-active", t.dataset.cat === category));
    try {
        const articles = await api(`/api/news/gsearch?q=${encodeURIComponent(category)}&max=3`);
        const list = $("#gnewsList");
        if (!articles || articles.length === 0) {
            list.innerHTML = `<div class="review-empty">暂无相关新闻</div>`;
            return;
        }
        const prevActive = list.querySelector(".gnews-item.is-active");
        list.innerHTML = articles.map((a, i) => `
            <button class="news-item gnews-item" data-gnews-index="${i}">
                <strong>${escapeHtml(a.title)}</strong>
                <span>${escapeHtml(a.sourceName)} · ${escapeHtml(a.publishedAt)}</span>
                <p>${escapeHtml(a.description)}</p>
            </button>`).join("");
        $$(".gnews-item").forEach((item) => {
            item.addEventListener("click", () => {
                const idx = parseInt(item.dataset.gnewsIndex);
                showGNewsArticle(articles[idx]);
                $$(".gnews-item").forEach((i) => i.classList.remove("is-active"));
                item.classList.add("is-active");
            });
        });
        if (articles[0]) {
            showGNewsArticle(articles[0]);
            $$(".gnews-item")[0]?.classList.add("is-active");
        }
    } catch (error) {
        console.error(error);
        $("#gnewsList").innerHTML = `<div class="review-empty">加载失败</div>`;
    }
}

function showGNewsArticle(article) {
    const snippet = article.content || article.description || "";
    state.currentArticle = { title: article.title, content: snippet };
    const descHtml = makeReaderHtml(snippet);
    const extraHtml = [];
    if (article.titleCn) {
        extraHtml.push(`<p class="gnews-cn">${escapeHtml(article.titleCn)}</p>`);
    }
    if (article.descriptionCn) {
        extraHtml.push(`<p class="gnews-cn">${escapeHtml(article.descriptionCn)}</p>`);
    }
    $("#readerTitle").textContent = article.title;
    $("#readerSummary").innerHTML = `<a href="${escapeAttribute(article.url)}" target="_blank" rel="noopener">${escapeHtml(article.sourceName)}</a> · ${escapeHtml(article.publishedAt)}`;
    $("#readerContent").innerHTML = descHtml + extraHtml.join("");
    clearTranslation();

    fetchFullContent(article.url, snippet.length).catch(() => {});
}

async function fetchFullContent(url, snippetLen) {
    if (!url) return;
    const result = await api("/api/news/fetch-content", {
        method: "POST",
        body: JSON.stringify({ url }),
    });
    if (!result.content || result.content.length <= snippetLen) return;
    state.currentArticle.content = result.content;
    const cnBlocks = Array.from(document.querySelectorAll("#readerContent .gnews-cn")).map(el => el.outerHTML).join("");
    $("#readerContent").innerHTML = makeReaderHtml(result.content) + cnBlocks;
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
    const tokens = ["context", "review", "essay", "listen", "speak", "read", "word", "/i/", "/e/", "/oi/", "/sh/"];
    const nodes = [];
    const trail = [];
    let width = 0;
    let height = 0;
    let ratio = 1;

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

    function seedNodes() {
        const target = Math.max(24, Math.min(58, Math.round((width * height) / 30000)));
        while (nodes.length < target) {
            nodes.push({
                x: Math.random() * width,
                y: Math.random() * height,
                vx: (Math.random() - 0.5) * 0.18,
                vy: (Math.random() - 0.5) * 0.14,
                phase: Math.random() * Math.PI * 2,
                token: tokens[nodes.length % tokens.length]
            });
        }
        nodes.length = target;
    }

    function recordPointer(event) {
        if (cursorOrb && window.matchMedia("(pointer: fine)").matches) {
            document.body.classList.add("has-custom-cursor");
            cursorOrb.style.left = `${event.clientX}px`;
            cursorOrb.style.top = `${event.clientY}px`;
        }
        trail.unshift({ x: event.clientX, y: event.clientY, life: 1 });
        if (trail.length > 42) trail.length = 42;
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
        const maxDistance = width < 760 ? 110 : 150;
        for (let i = 0; i < nodes.length; i += 1) {
            const node = nodes[i];
            node.x += node.vx + Math.cos(time / 2800 + node.phase) * 0.035;
            node.y += node.vy + Math.sin(time / 3200 + node.phase) * 0.03;
            if (node.x < -80) node.x = width + 80;
            if (node.x > width + 80) node.x = -80;
            if (node.y < -60) node.y = height + 60;
            if (node.y > height + 60) node.y = -60;

            for (let j = i + 1; j < nodes.length; j += 1) {
                const other = nodes[j];
                const dx = node.x - other.x;
                const dy = node.y - other.y;
                const distance = Math.hypot(dx, dy);
                if (distance < maxDistance) {
                    ctx.strokeStyle = `rgba(31, 42, 68, ${(1 - distance / maxDistance) * 0.075})`;
                    ctx.lineWidth = 1;
                    ctx.beginPath();
                    ctx.moveTo(node.x, node.y);
                    ctx.lineTo(other.x, other.y);
                    ctx.stroke();
                }
            }
        }

        ctx.font = width < 760 ? "700 11px Segoe UI, Arial" : "800 13px Segoe UI, Arial";
        nodes.forEach((node) => {
            const pulse = 0.072 + Math.sin(time / 1100 + node.phase) * 0.026;
            ctx.fillStyle = `rgba(35, 49, 67, ${pulse})`;
            ctx.fillText(node.token, node.x, node.y);
            ctx.fillStyle = "rgba(88, 204, 2, 0.15)";
            ctx.fillRect(node.x - 9, node.y + 5, 18, 2);
        });
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
        openSettings();
        if (!state.ai.loaded) {
            loadAiSettings().catch((error) => {
                console.error(error);
                setAiStatus("\u8bbe\u7f6e\u52a0\u8f7d\u5931\u8d25");
            });
        }
    });
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
        }
    });
    $("#chatForm").addEventListener("submit", sendChat);
    $("#writingForm").addEventListener("submit", reviewEssay);
    $("#newsDifficulty").addEventListener("change", loadNews);
    $("#gnewsCats").addEventListener("click", (event) => {
        const tab = event.target.closest(".gnews-tab");
        if (tab) loadGNews(tab.dataset.cat);
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
        loadWords(),
        loadReviews(),
        loadNews(),
        loadGNews("technology"),
        loadAiSettings().catch((error) => {
            console.error(error);
            setAiStatus("\u8bbe\u7f6e\u52a0\u8f7d\u5931\u8d25");
        })
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
