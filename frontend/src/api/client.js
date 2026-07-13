export class ApiError extends Error {
  constructor(message, status, payload) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.payload = payload;
  }
}

export function query(params = {}) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      search.set(key, value);
    }
  });
  const text = search.toString();
  return text ? `?${text}` : "";
}

export async function api(url, options = {}, config = {}) {
  const headers = { ...(options.headers || {}) };
  const hasBody = options.body !== undefined && options.body !== null;
  if (hasBody && !(options.body instanceof FormData) && !headers["Content-Type"]) {
    headers["Content-Type"] = "application/json";
  }

  const response = await fetch(url, {
    credentials: "same-origin",
    ...options,
    headers
  });

  let payload = null;
  const text = await response.text();
  if (text) {
    try {
      payload = JSON.parse(text);
    } catch {
      throw new ApiError("服务返回格式异常", response.status, text);
    }
  }

  const ok = response.ok && payload?.success !== false;
  if (!ok) {
    if (response.status === 401 && config.redirectOnUnauthorized !== false) {
      window.location.href = "/login";
      return null;
    }
    if (response.status === 403 && config.redirectOnForbidden !== false) {
      window.location.href = "/";
      return null;
    }
    throw new ApiError(payload?.message || "请求失败", response.status, payload);
  }

  return payload?.data ?? null;
}

export function normalizeModelList(models) {
  return Array.from(new Set((models || [])
    .map((item) => (typeof item === "string" ? item : item?.id))
    .filter(Boolean)));
}

export function formatDate(value) {
  if (!value) return "";
  return String(value).replace("T", " ").slice(0, 16);
}
