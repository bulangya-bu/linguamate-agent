const $ = (selector) => document.querySelector(selector);

async function api(url, options = {}) {
    const response = await fetch(url, {
        headers: { "Content-Type": "application/json", ...(options.headers || {}) },
        ...options
    });
    const payload = await response.json();
    if (!payload.success) {
        throw new Error(payload.message || "请求失败");
    }
    return payload.data;
}

function setStatus(message) {
    $("#authStatus").textContent = message || "";
}

function currentMode() {
    return window.location.pathname.replace(/\/+$/, "") === "/register" ? "register" : "login";
}

function renderMode(mode) {
    const login = mode === "login";
    const loginForm = $("#loginForm");
    const registerForm = $("#registerForm");
    document.title = login ? "LinguaMate 登录" : "LinguaMate 注册";
    loginForm.hidden = !login;
    registerForm.hidden = login;
    loginForm.style.display = login ? "grid" : "none";
    registerForm.style.display = login ? "none" : "grid";
    $("#loginModePill").classList.toggle("is-active", login);
    $("#registerModePill").classList.toggle("is-active", !login);
    $("#authHeroTitle").textContent = login ? "先登录，再开始今天的英语路线" : "创建账号，保存你的学习进度";
    $("#authHeroSubtitle").textContent = login
        ? "学习记录会绑定到你的账号，管理员账号会进入独立后台。"
        : "新账号默认进入学习端，等级、收藏、复习和模型接口都会独立保存。";
    setStatus("");
}

function goAfterLogin(user) {
    window.location.href = user.role === "ADMIN" ? "/admin" : "/";
}

async function checkExistingSession() {
    const user = await api("/api/auth/me");
    if (user) {
        goAfterLogin(user);
    }
}

$("#loginForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    setStatus("正在登录...");
    try {
        const user = await api("/api/auth/login", {
            method: "POST",
            body: JSON.stringify({
                username: $("#loginUsername").value,
                password: $("#loginPassword").value
            })
        });
        goAfterLogin(user);
    } catch (error) {
        setStatus(error.message);
    }
});

$("#registerForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    setStatus("正在创建账号...");
    try {
        const user = await api("/api/auth/register", {
            method: "POST",
            body: JSON.stringify({
                username: $("#registerUsername").value,
                nickname: $("#registerNickname").value,
                password: $("#registerPassword").value
            })
        });
        goAfterLogin(user);
    } catch (error) {
        setStatus(error.message);
    }
});

renderMode(currentMode());
checkExistingSession().catch(() => {});
