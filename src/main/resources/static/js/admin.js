const adminState = {
    user: null,
    selectedUserId: null,
    wordPage: 0,
    newsPage: 0
};

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => Array.from(document.querySelectorAll(selector));

async function api(url, options = {}) {
    const response = await fetch(url, {
        headers: { "Content-Type": "application/json", ...(options.headers || {}) },
        ...options
    });
    const payload = await response.json();
    if (!payload.success) {
        if (response.status === 401) window.location.href = "/login";
        if (response.status === 403) window.location.href = "/";
        throw new Error(payload.message || "请求失败");
    }
    return payload.data;
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}

function escapeAttribute(value) {
    return escapeHtml(value).replaceAll("`", "&#96;");
}

function debounce(fn, wait = 300) {
    let timer;
    return (...args) => {
        clearTimeout(timer);
        timer = setTimeout(() => fn(...args), wait);
    };
}

function formatDate(value) {
    if (!value) return "";
    return String(value).replace("T", " ").slice(0, 16);
}

function encodeRowData(value) {
    return escapeAttribute(encodeURIComponent(JSON.stringify(value)));
}

function notifyAdmin(message) {
    let notice = $("#adminNotice");
    if (!notice) {
        notice = document.createElement("div");
        notice.id = "adminNotice";
        notice.className = "admin-notice";
        document.body.appendChild(notice);
    }
    notice.textContent = message;
    notice.classList.add("is-visible");
    clearTimeout(notice._timer);
    notice._timer = setTimeout(() => notice.classList.remove("is-visible"), 2200);
}

async function requireAdmin() {
    const user = await api("/api/auth/me");
    if (!user) {
        window.location.href = "/login";
        return null;
    }
    if (user.role !== "ADMIN") {
        window.location.href = "/";
        return null;
    }
    adminState.user = user;
    $("#adminUserName").textContent = user.nickname || user.username;
    return user;
}

function showAdminSection(id) {
    $$("[data-admin-section]").forEach((item) => item.classList.toggle("active", item.dataset.adminSection === id));
    $$(".workspace > .section").forEach((section) => section.classList.toggle("active", section.id === id));
    if (id === "overview") loadSummary().catch(console.error);
    if (id === "users") loadUsers().catch(console.error);
    if (id === "words") loadWords().catch(console.error);
    if (id === "knowledge") loadKnowledge().catch(console.error);
    if (id === "news") loadNews().catch(console.error);
    if (id === "ai") loadAiSettings().catch(console.error);
}

async function logout() {
    await api("/api/auth/logout", { method: "POST" });
    window.location.href = "/login";
}

async function loadSummary() {
    const summary = await api("/api/admin/summary");
    const inactiveUsers = Math.max(0, (summary.users || 0) - (summary.activeUsers || 0));
    const activePercent = summary.users ? Math.round((summary.activeUsers / summary.users) * 100) : 0;
    const dailyStats = summary.dailyWordStats || [];
    const maxDailyWords = Math.max(1, ...dailyStats.map((item) => Number(item.words) || 0));
    $("#adminSummaryGrid").innerHTML = `
        <div class="admin-overview">
            <section class="admin-kpi-band">
                <div class="admin-kpi-primary">
                    <span>今日学习概览</span>
                    <strong>${summary.todayWordReviews || 0}</strong>
                    <small>今日背词次数</small>
                </div>
                <div class="admin-kpi-metrics">
                    <article class="admin-kpi-mini">
                        <span>用户总数</span>
                        <strong>${summary.users || 0}</strong>
                    </article>
                    <article class="admin-kpi-mini">
                        <span>活跃用户</span>
                        <strong>${summary.activeUsers || 0}</strong>
                    </article>
                    <article class="admin-kpi-mini">
                        <span>今日学习用户</span>
                        <strong>${summary.todayLearners || 0}</strong>
                    </article>
                </div>
                <div class="admin-kpi-progress">
                    <div>
                        <span>活跃率 ${activePercent}%</span>
                        <span>今日覆盖单词 ${summary.todayUniqueWords || 0}</span>
                    </div>
                    <i style="width:${activePercent}%"></i>
                </div>
            </section>
            <section class="admin-overview-grid">
                <article class="admin-chart-block admin-overview-chart">
                    <div class="admin-chart-heading">
                        <strong>近 7 天背词趋势</strong>
                        <span>所有用户</span>
                    </div>
                    <div class="admin-column-chart">
                        ${dailyStats.map((item) => {
                            const height = Math.max(4, Math.round(((Number(item.words) || 0) / maxDailyWords) * 100));
                            return `
                                <div class="admin-column-item">
                                    <div class="admin-column-track"><i style="height:${height}%"></i></div>
                                    <strong>${item.words || 0}</strong>
                                    <span>${formatSummaryDate(item.date)}</span>
                                </div>
                            `;
                        }).join("")}
                    </div>
                </article>
                <article class="admin-chart-block admin-overview-chart">
                    <div class="admin-chart-heading">
                        <strong>用户状态占比</strong>
                        <span>${summary.activeUsers || 0} / ${summary.users || 0}</span>
                    </div>
                    <div class="admin-pie-layout">
                        <div class="admin-pie" style="--value:${activePercent}">
                            <strong>${activePercent}%</strong>
                            <span>活跃</span>
                        </div>
                        <div class="admin-pie-legend">
                            <p><i class="tone-green"></i>活跃用户 ${summary.activeUsers || 0}</p>
                            <p><i class="tone-muted"></i>非活跃用户 ${inactiveUsers}</p>
                            <p><i class="tone-blue"></i>今日覆盖单词 ${summary.todayUniqueWords || 0}</p>
                        </div>
                    </div>
                </article>
            </section>
        </div>
    `;
}

function formatSummaryDate(value) {
    if (!value) return "";
    const parts = String(value).split("-");
    return parts.length === 3 ? `${Number(parts[1])}/${Number(parts[2])}` : value;
}

async function loadUsers() {
    const search = $("#userSearch").value.trim();
    const users = await api(`/api/admin/users?search=${encodeURIComponent(search)}`);
    $("#userTable").innerHTML = `
        <div class="admin-row admin-row-head">
            <span>用户</span><span>角色</span><span>状态</span><span>操作</span>
        </div>
        ${users.map((user) => `
            <div class="admin-row">
                <span><strong>${escapeHtml(user.nickname || user.username)}</strong><small>${escapeHtml(user.username)} · ${formatDate(user.createdAt)}</small></span>
                <span>${escapeHtml(user.role)}</span>
                <span><mark class="${user.status === "ACTIVE" ? "ok" : "danger"}">${escapeHtml(user.status)}</mark></span>
                <span class="admin-actions">
                    <button data-user-profile="${user.id}">详情</button>
                    <button data-user-edit="${encodeRowData(user)}">编辑</button>
                    <button data-user-status="${user.id}" data-role="${user.role}" data-status="${user.status === "ACTIVE" ? "DISABLED" : "ACTIVE"}">${user.status === "ACTIVE" ? "禁用" : "启用"}</button>
                    <button data-user-reset="${encodeRowData(user)}">重置密码</button>
                </span>
            </div>
        `).join("")}
    `;
}

function chartBar(label, value, max, tone = "green") {
    const safeMax = Math.max(1, Number(max) || 1);
    const safeValue = Math.max(0, Number(value) || 0);
    const width = Math.min(100, Math.round((safeValue / safeMax) * 100));
    return `
        <div class="admin-chart-row">
            <span>${escapeHtml(label)}</span>
            <div class="admin-chart-track"><i class="tone-${tone}" style="width:${width}%"></i></div>
            <strong>${safeValue}</strong>
        </div>
    `;
}

async function loadUserProfile(userId) {
    adminState.selectedUserId = userId;
    const [profile, readings] = await Promise.all([
        api(`/api/admin/users/${userId}/profile`),
        api(`/api/admin/users/${userId}/readings`)
    ]);
    const vocab = profile.vocabularyDistribution || {};
    const reading = profile.readingOverview || {};
    const vocabMax = Math.max(vocab.learnedWords || 0, vocab.masteredWords || 0, vocab.reviewWords || 0, vocab.dueWords || 0, vocab.favoriteWords || 0, 1);
    const completion = reading.readings ? Math.round(((reading.finishedReadings || 0) / reading.readings) * 100) : 0;
    const profileLevelCards = profile.role === "ADMIN" ? "" : `
            <div><strong>Lv.${profile.level}</strong><span>等级</span></div>
            <div><strong>${profile.experience}</strong><span>当前经验</span></div>
            <div><strong>${profile.totalExperience}</strong><span>累计经验</span></div>`;
    $("#userProfilePanel").classList.remove("empty-state");
    $("#userProfilePanel").innerHTML = `
        <div class="admin-profile-head">
            <div>
                <strong>${escapeHtml(profile.nickname || profile.username)}</strong>
                <small>${escapeHtml(profile.username)} · ${escapeHtml(profile.role)} · ${escapeHtml(profile.status)}</small>
            </div>
            <mark class="${profile.aiConfigured ? "ok" : "danger"}">${profile.aiConfigured ? "已配置 AI" : "未配置 AI"}</mark>
        </div>
        <div class="admin-profile-grid">
            ${profileLevelCards}
            <div><strong>${profile.learnedWords}</strong><span>已背单词</span></div>
            <div><strong>${profile.favoriteWords}</strong><span>收藏单词</span></div>
            <div><strong>${profile.masteredWords}</strong><span>已掌握</span></div>
            <div><strong>${profile.reviewWords}</strong><span>错词</span></div>
            <div><strong>${profile.reviewLogs}</strong><span>复习次数</span></div>
            <div><strong>${profile.wrongReviewLogs}</strong><span>答错次数</span></div>
            <div><strong>${profile.essays}</strong><span>作文</span></div>
            <div><strong>${profile.readings}</strong><span>阅读</span></div>
            <div><strong>${profile.finishedReadings}</strong><span>完成阅读</span></div>
            <div><strong>${profile.lookedUpWords}</strong><span>阅读查词</span></div>
            <div><strong>${profile.conversations}</strong><span>会话</span></div>
        </div>
        <div class="admin-profile-charts">
            <section class="admin-chart-block">
                <div class="admin-chart-heading">
                    <strong>词汇分布</strong>
                    <span>${vocab.learnedWords || 0} 个学习记录</span>
                </div>
                ${chartBar("已背", vocab.learnedWords, vocabMax, "blue")}
                ${chartBar("已掌握", vocab.masteredWords, vocabMax, "green")}
                ${chartBar("错词", vocab.reviewWords, vocabMax, "coral")}
                ${chartBar("待复习", vocab.dueWords, vocabMax, "amber")}
                ${chartBar("收藏", vocab.favoriteWords, vocabMax, "violet")}
            </section>
            <section class="admin-chart-block admin-ring-block">
                <div class="admin-ring" style="--value:${completion}">
                    <strong>${completion}%</strong>
                    <span>完成率</span>
                </div>
                <div>
                    <strong>阅读概览</strong>
                    <p>${reading.finishedReadings || 0} / ${reading.readings || 0} 篇完成，累计查词 ${reading.lookedUpWords || 0} 次。</p>
                </div>
            </section>
        </div>
        <section class="admin-readings">
            <div class="admin-chart-heading">
                <strong>阅读记录</strong>
                <span>最近 ${readings.length} 条</span>
            </div>
            <div class="admin-table compact">
                <div class="admin-row reading-row admin-row-head">
                    <span>文章</span><span>进度</span><span>查词</span><span>时间</span>
                </div>
                ${readings.length ? readings.map((record) => `
                    <div class="admin-row reading-row">
                        <span><strong>${escapeHtml(record.title)}</strong><small>${escapeHtml(record.difficulty || "")}</small></span>
                        <span><mark class="${record.finished ? "ok" : ""}">${record.readProgress || 0}%</mark></span>
                        <span>${record.lookedUpWords || 0}</span>
                        <span>${formatDate(record.updatedAt || record.createdAt)}</span>
                    </div>
                `).join("") : `<p class="empty-state">暂无阅读记录。</p>`}
            </div>
        </section>
    `;
}

async function updateUserFromPrompt(user) {
    if (user.role === "ADMIN") {
        notifyAdmin("管理员账号固定唯一，只能查看，不能编辑。");
        return;
    }
    const nickname = window.prompt("输入新的昵称", user.nickname || user.username);
    if (nickname === null) return;
    await api(`/api/admin/users/${user.id}`, {
        method: "PUT",
        body: JSON.stringify({ nickname })
    });
    notifyAdmin("用户昵称已更新");
    await loadUsers();
    await loadUserProfile(user.id);
}

async function updateUserStatus(userId, status, role) {
    if (role === "ADMIN") {
        notifyAdmin("管理员账号不能禁用或启用。");
        return;
    }
    await api(`/api/admin/users/${userId}/status`, {
        method: "POST",
        body: JSON.stringify({ status })
    });
    notifyAdmin(status === "ACTIVE" ? "用户已启用" : "用户已禁用");
    await loadUsers();
}

async function resetUserPassword(user) {
    if (user.role === "ADMIN") {
        notifyAdmin("管理员账号固定唯一，不能在这里重置密码。");
        return;
    }
    const password = window.prompt("输入新密码，至少 6 位", "student123");
    if (!password) return;
    await api(`/api/admin/users/${user.id}/reset-password`, {
        method: "POST",
        body: JSON.stringify({ password })
    });
    notifyAdmin("密码已重置");
}

async function loadWords(page = adminState.wordPage) {
    adminState.wordPage = Math.max(0, page);
    const params = new URLSearchParams({
        page: adminState.wordPage,
        size: 20,
        search: $("#wordSearch").value.trim()
    });
    const result = await api(`/api/admin/words?${params}`);
    adminState.wordPage = result.page;
    $("#wordTable").innerHTML = `
        <div class="admin-row admin-row-head">
            <span>单词</span><span>释义</span><span>分类</span><span>操作</span>
        </div>
        ${result.words.map((word) => `
            <div class="admin-row">
                <span><strong>${escapeHtml(word.word)}</strong><small>${escapeHtml(word.partOfSpeech || "")}</small></span>
                <span>${escapeHtml(word.meaningCn || "")}</span>
                <span>${escapeHtml(word.levelTag || "")}</span>
                <span class="admin-actions">
                    <button data-word-edit="${encodeRowData(word)}">编辑</button>
                    <button data-word-delete="${word.id}">删除</button>
                </span>
            </div>
        `).join("")}
        <div class="admin-pagination">
            <button data-word-page="${result.page - 1}" data-boundary="${result.page <= 0}">上一页</button>
            <span>第 ${result.page + 1} 页 / 共 ${result.total} 条</span>
            <button data-word-page="${result.page + 1}" data-boundary="${(result.page + 1) * result.size >= result.total}">下一页</button>
        </div>
    `;
}

function fillWordForm(word) {
    $("#wordId").value = word?.id || "";
    $("#wordText").value = word?.word || "";
    $("#wordMeaning").value = word?.meaningCn || "";
    $("#wordPart").value = word?.partOfSpeech || "";
    $("#wordTag").value = word?.levelTag || "";
    $("#wordPhoneticUs").value = word?.phoneticUs || "";
    $("#wordPhoneticUk").value = word?.phoneticUk || "";
    $("#wordExampleEn").value = word?.exampleEn || "";
    $("#wordExampleCn").value = word?.exampleCn || "";
}

function readWordForm() {
    return {
        word: $("#wordText").value,
        meaningCn: $("#wordMeaning").value,
        partOfSpeech: $("#wordPart").value,
        levelTag: $("#wordTag").value,
        phoneticUs: $("#wordPhoneticUs").value,
        phoneticUk: $("#wordPhoneticUk").value,
        exampleEn: $("#wordExampleEn").value,
        exampleCn: $("#wordExampleCn").value
    };
}

async function saveWord(event) {
    event.preventDefault();
    const id = $("#wordId").value;
    const url = id ? `/api/admin/words/${id}` : "/api/admin/words";
    const method = id ? "PUT" : "POST";
    await api(url, { method, body: JSON.stringify(readWordForm()) });
    fillWordForm(null);
    notifyAdmin(id ? "单词已更新" : "单词已新增");
    await loadWords();
}

async function loadKnowledge() {
    const docs = await api("/api/knowledge");
    $("#knowledgeList").innerHTML = `
        <div class="admin-row admin-row-head">
            <span>标题</span><span>类型</span><span>标签</span>
        </div>
        ${docs.map((doc) => `
            <div class="admin-row">
                <span><strong>${escapeHtml(doc.title)}</strong><small>${escapeHtml(doc.createdAt || "")}</small></span>
                <span>${escapeHtml(doc.sourceType)}</span>
                <span>${escapeHtml(doc.tags || "")}</span>
            </div>
        `).join("")}
    `;
}

async function uploadKnowledge(event) {
    event.preventDefault();
    await api("/api/knowledge", {
        method: "POST",
        body: JSON.stringify({
            title: $("#knowledgeTitle").value,
            sourceType: $("#knowledgeSourceType").value,
            tags: $("#knowledgeTags").value,
            content: $("#knowledgeContent").value
        })
    });
    $("#knowledgeForm").reset();
    notifyAdmin("知识已导入");
    await loadKnowledge();
    await loadSummary();
}

async function loadNews(page = adminState.newsPage) {
    adminState.newsPage = Math.max(0, page);
    const params = new URLSearchParams({
        page: adminState.newsPage,
        size: 20,
        search: $("#newsSearch").value.trim()
    });
    const result = await api(`/api/admin/news?${params}`);
    adminState.newsPage = result.page;
    $("#newsTable").innerHTML = `
        <div class="admin-row admin-row-head">
            <span>标题</span><span>难度</span><span>来源</span><span>操作</span>
        </div>
        ${result.articles.map((article) => `
            <div class="admin-row">
                <span><strong>${escapeHtml(article.title)}</strong><small>${escapeHtml(article.topicTags || "")}</small></span>
                <span>${escapeHtml(article.difficulty || "")}</span>
                <span>${escapeHtml(article.sourceName || "")}</span>
                <span class="admin-actions">
                    <button data-news-edit="${encodeRowData(article)}">编辑</button>
                    <button data-news-delete="${article.id}">删除</button>
                </span>
            </div>
        `).join("")}
        <div class="admin-pagination">
            <button data-news-page="${result.page - 1}" data-boundary="${result.page <= 0}">上一页</button>
            <span>第 ${result.page + 1} 页 / 共 ${result.total} 条</span>
            <button data-news-page="${result.page + 1}" data-boundary="${(result.page + 1) * result.size >= result.total}">下一页</button>
        </div>
    `;
}

function fillNewsForm(article) {
    $("#newsId").value = article?.id || "";
    $("#newsTitle").value = article?.title || "";
    $("#newsSummary").value = article?.summary || "";
    $("#newsContent").value = article?.content || "";
    $("#newsSource").value = article?.sourceName || "";
    $("#newsDifficulty").value = article?.difficulty || "";
    $("#newsTags").value = article?.topicTags || "";
}

function readNewsForm() {
    return {
        title: $("#newsTitle").value,
        summary: $("#newsSummary").value,
        content: $("#newsContent").value,
        sourceName: $("#newsSource").value,
        difficulty: $("#newsDifficulty").value,
        topicTags: $("#newsTags").value
    };
}

async function saveNews(event) {
    event.preventDefault();
    const id = $("#newsId").value;
    const url = id ? `/api/admin/news/${id}` : "/api/admin/news";
    const method = id ? "PUT" : "POST";
    await api(url, { method, body: JSON.stringify(readNewsForm()) });
    fillNewsForm(null);
    notifyAdmin(id ? "阅读文章已更新" : "阅读文章已新增");
    await loadNews();
    await loadSummary();
}

function normalizeModelList(models) {
    return Array.from(new Set((models || [])
        .map((item) => (typeof item === "string" ? item : item?.id))
        .filter(Boolean)));
}

function setAiStatus(message) {
    $("#aiConnectionStatus").textContent = message;
}

function populateAiModelSelect(models, selectedModel = "") {
    const select = $("#aiModelSelect");
    const modelList = normalizeModelList(models);
    const current = selectedModel || modelList[0] || "";
    select.innerHTML = modelList.length
        ? modelList.map((model) => `<option value="${escapeAttribute(model)}">${escapeHtml(model)}</option>`).join("")
        : current
            ? `<option value="${escapeAttribute(current)}">${escapeHtml(current)}</option>`
            : `<option value="">暂无模型</option>`;
    select.disabled = !modelList.length && !current;
    select.value = current;
}

async function loadAiSettings() {
    const settings = await api("/api/ai/settings");
    $("#aiBaseUrl").value = settings.baseUrl || "";
    $("#aiApiKey").value = "";
    populateAiModelSelect(settings.chatModel ? [settings.chatModel] : [], settings.chatModel || "");
    setAiStatus(settings.hasApiKey ? "已保存密钥" : "未保存密钥");
}

async function fetchAiModels() {
    setAiStatus("正在获取模型...");
    const models = await api("/api/ai/models", {
        method: "POST",
        body: JSON.stringify({
            baseUrl: $("#aiBaseUrl").value,
            apiKey: $("#aiApiKey").value
        })
    });
    populateAiModelSelect(models, $("#aiModelSelect").value);
    setAiStatus(`已获取 ${normalizeModelList(models).length} 个模型`);
    notifyAdmin("模型列表已刷新");
}

async function saveAiSettings() {
    setAiStatus("正在保存...");
    const settings = await api("/api/ai/settings", {
        method: "PUT",
        body: JSON.stringify({
            baseUrl: $("#aiBaseUrl").value,
            apiKey: $("#aiApiKey").value,
            chatModel: $("#aiModelSelect").value
        })
    });
    populateAiModelSelect([settings.chatModel], settings.chatModel);
    $("#aiApiKey").value = "";
    setAiStatus(`已应用 ${settings.chatModel || "当前模型"}`);
    notifyAdmin("AI 设置已保存");
}

function bindEvents() {
    $$("[data-admin-section]").forEach((item) => {
        item.addEventListener("click", () => showAdminSection(item.dataset.adminSection));
    });
    $("#adminLogoutBtn").addEventListener("click", () => logout().catch(console.error));
    $("#userSearch").addEventListener("input", debounce(() => loadUsers().catch(console.error)));
    $("#wordSearch").addEventListener("input", debounce(() => loadWords(0).catch(console.error)));
    $("#newsSearch").addEventListener("input", debounce(() => loadNews(0).catch(console.error)));
    $("#wordForm").addEventListener("submit", saveWord);
    $("#clearWordFormBtn").addEventListener("click", () => {
        fillWordForm(null);
        notifyAdmin("单词表单已清空");
    });
    $("#newsForm").addEventListener("submit", saveNews);
    $("#clearNewsFormBtn").addEventListener("click", () => {
        fillNewsForm(null);
        notifyAdmin("阅读表单已清空");
    });
    $("#knowledgeForm").addEventListener("submit", uploadKnowledge);
    $("#fetchModelsBtn").addEventListener("click", () => fetchAiModels().catch((error) => setAiStatus(error.message)));
    $("#saveAiSettingsBtn").addEventListener("click", () => saveAiSettings().catch((error) => setAiStatus(error.message)));

    document.addEventListener("click", async (event) => {
        const status = event.target.closest("[data-user-status]");
        if (status) {
            await updateUserStatus(status.dataset.userStatus, status.dataset.status, status.dataset.role);
            return;
        }
        const profile = event.target.closest("[data-user-profile]");
        if (profile) {
            await loadUserProfile(profile.dataset.userProfile);
            return;
        }
        const editUser = event.target.closest("[data-user-edit]");
        if (editUser) {
            await updateUserFromPrompt(JSON.parse(decodeURIComponent(editUser.dataset.userEdit)));
            return;
        }
        const reset = event.target.closest("[data-user-reset]");
        if (reset) {
            await resetUserPassword(JSON.parse(decodeURIComponent(reset.dataset.userReset)));
            return;
        }
        const wordPage = event.target.closest("[data-word-page]");
        if (wordPage) {
            const nextPage = Number(wordPage.dataset.wordPage);
            if (nextPage < 0 || wordPage.dataset.boundary === "true") {
                notifyAdmin(nextPage < 0 ? "已经是第一页" : "已经是最后一页");
            } else {
                await loadWords(nextPage);
            }
            return;
        }
        const wordEdit = event.target.closest("[data-word-edit]");
        if (wordEdit) {
            fillWordForm(JSON.parse(decodeURIComponent(wordEdit.dataset.wordEdit)));
            return;
        }
        const wordDelete = event.target.closest("[data-word-delete]");
        if (wordDelete && window.confirm("删除这个单词？")) {
            await api(`/api/admin/words/${wordDelete.dataset.wordDelete}`, { method: "DELETE" });
            notifyAdmin("单词已删除");
            await loadWords();
            return;
        }
        const newsPage = event.target.closest("[data-news-page]");
        if (newsPage) {
            const nextPage = Number(newsPage.dataset.newsPage);
            if (nextPage < 0 || newsPage.dataset.boundary === "true") {
                notifyAdmin(nextPage < 0 ? "已经是第一页" : "已经是最后一页");
            } else {
                await loadNews(nextPage);
            }
            return;
        }
        const newsEdit = event.target.closest("[data-news-edit]");
        if (newsEdit) {
            fillNewsForm(JSON.parse(decodeURIComponent(newsEdit.dataset.newsEdit)));
            return;
        }
        const newsDelete = event.target.closest("[data-news-delete]");
        if (newsDelete && window.confirm("删除这篇新闻？")) {
            await api(`/api/admin/news/${newsDelete.dataset.newsDelete}`, { method: "DELETE" });
            notifyAdmin("阅读文章已删除");
            await loadNews();
            await loadSummary();
        }
    });
}

async function boot() {
    const user = await requireAdmin();
    if (!user) return;
    bindEvents();
    await Promise.all([loadSummary(), loadUsers(), loadWords(), loadKnowledge(), loadNews()]);
}

boot().catch(console.error);
