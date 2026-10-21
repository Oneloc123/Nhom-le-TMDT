const appRoot = document.getElementById("admin-app");
const contextPath = appRoot?.dataset.contextPath || "";
const csrfToken = document.querySelector('meta[name="csrf-token"]')?.content || "";

export const apiContextPath = contextPath;

export function queryString(values = {}) {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(values)) {
    if (value !== undefined && value !== null && String(value).trim() !== "") params.set(key, String(value));
  }
  const query = params.toString();
  return query ? `?${query}` : "";
}

export async function apiGet(path) {
  return request(path, "GET");
}

export async function apiPost(path, values = {}) {
  const body = new URLSearchParams();
  for (const [key, value] of Object.entries(values)) {
    if (value !== undefined && value !== null) body.set(key, String(value));
  }
  return request(path, "POST", body);
}

async function request(path, method, body) {
  const headers = { Accept: "application/json" };
  if (method === "POST") {
    headers["Content-Type"] = "application/x-www-form-urlencoded;charset=UTF-8";
    headers["X-CSRF-Token"] = csrfToken;
  }
  let response;
  try {
    response = await fetch(`${contextPath}/admin/api${path}`, {
      method, body, headers, credentials: "same-origin", cache: "no-store"
    });
  } catch {
    throw new Error("Không thể kết nối máy chủ. Hãy kiểm tra kết nối rồi thử lại.");
  }
  if (response.status === 401) {
    window.location.assign(`${contextPath}/login`);
    throw new Error("Phiên đăng nhập đã hết hạn.");
  }
  let payload;
  try { payload = await response.json(); }
  catch { throw new Error("Máy chủ trả về phản hồi không hợp lệ."); }
  if (!response.ok) throw new Error(payload.error || "Yêu cầu không thành công.");
  return payload.data;
}
