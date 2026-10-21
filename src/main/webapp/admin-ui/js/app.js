import { apiContextPath, apiGet, apiPost, queryString } from "./api.js";
import { esc, icon } from "./components.js";
import { currentRoute, renderLayout, routeTitles } from "./layout.js";
import { renderDashboard } from "./pages/dashboard.js";
import { renderEntityDetail, renderOrders, renderProducts, renderQuotes, renderSellers, renderUsers } from "./pages/management.js";

const root = document.getElementById("admin-app");
const filters = Object.fromEntries(new URLSearchParams(window.location.search));
const state = {
  route: currentRoute(apiContextPath), filters, actor: null, data: null, loading: false,
  error: "", modal: null, pageData: null
};

function pageError(message) {
  return `<section class="error-state"><span class="empty-illustration">${icon("warning")}</span>
    <div class="eyebrow">KHÔNG THỂ TẢI DỮ LIỆU</div><h1>Yêu cầu chưa hoàn tất</h1><p>${esc(message)}</p>
    <button class="button" data-action="retry">Thử lại</button></section>`;
}

function loadingState() {
  return `<section class="loading-state" role="status"><span class="loading-spinner"></span><strong>Đang tải dữ liệu từ hệ thống…</strong></section>`;
}

function pageContent() {
  if (state.loading) return loadingState();
  if (state.error) return pageError(state.error);
  switch (state.route) {
    case "dashboard": return renderDashboard(state.data, apiContextPath);
    case "users": return renderUsers(state.data, state, apiContextPath);
    case "sellers": return renderSellers(state.data, state, apiContextPath);
    case "products": return renderProducts(state.data, state, apiContextPath);
    case "orders": return renderOrders(state.data, state, apiContextPath);
    default: return `<section class="not-found-state"><span class="empty-illustration">${icon("cube")}</span>
      <div class="eyebrow">404 · ADMIN ROUTE</div><h1>Không tìm thấy trang</h1><p>Chọn một trong năm mục quản trị để tiếp tục.</p>
      <a class="button" href="${apiContextPath}/admin">Về Dashboard</a></section>`;
  }
}

function modalMarkup() {
  if (!state.modal) return "";
  let body = state.modal.body;
  if (state.modal.type === "reject-product") {
    body = `<p class="muted-note">Ghi lý do cụ thể để seller biết nội dung cần điều chỉnh.</p>
      <form class="stack-form" data-form="reject-product" data-id="${esc(state.modal.id)}">
        <label class="form-field">Lý do từ chối<textarea name="reason" required minlength="8" maxlength="500"></textarea></label>
        <div class="modal-actions"><button class="button secondary" type="button" data-action="modal-close">Hủy</button>
          <button class="button danger" type="submit">Từ chối sản phẩm</button></div></form>`;
  }
  return `<div class="modal-backdrop" data-modal-backdrop><section class="modal-card large" role="dialog" aria-modal="true" aria-labelledby="modal-title">
    <header class="modal-head"><div><h2 id="modal-title">${esc(state.modal.title)}</h2><p>${esc(state.modal.subtitle || "Dữ liệu đã lưu trong marketplace")}</p></div>
      <button class="icon-button" data-action="modal-close" aria-label="Đóng">${icon("close")}</button></header>
    <div class="modal-body">${body}</div></section></div>`;
}

function render() {
  const safeRoute = routeTitles[state.route] ? state.route : "not-found";
  root.innerHTML = renderLayout(safeRoute, pageContent(), apiContextPath);
  const host = document.getElementById("modal-root");
  if (host) host.innerHTML = modalMarkup();
}

function apiQuery() {
  const params = { ...state.filters };
  delete params.view;
  return queryString(params);
}

async function loadData() {
  state.loading = true;
  state.error = "";
  state.data = null;
  render();
  try {
    if (!state.actor) state.actor = await apiGet("/session");
    let endpoint;
    if (state.route === "dashboard") endpoint = "/dashboard";
    else if (state.route === "orders" && state.filters.view === "quotes") endpoint = "/quotes";
    else endpoint = `/${state.route}`;
    state.data = await apiGet(`${endpoint}${apiQuery()}`);
  } catch (error) {
    state.error = error.message || "Không thể đọc dữ liệu.";
  } finally {
    state.loading = false;
    render();
  }
}

async function reloadDetail(type, id) {
  const detail = await apiGet(`/${type}/${encodeURIComponent(id)}`);
  state.modal = {
    type: "entity", title: detailTitle(type), subtitle: `${detail.name || detail.code || detail.fullName || `#${id}`}`,
    body: renderEntityDetail(type, detail, apiContextPath), id, recordType: type
  };
  render();
}

function detailTitle(type) {
  return ({ users: "Chi tiết người dùng", sellers: "Chi tiết xưởng in", products: "Chi tiết sản phẩm", orders: "Chi tiết đơn hàng" })[type] || "Chi tiết";
}

function showToast(message, type = "success") {
  const host = document.getElementById("toast-root");
  if (!host) return;
  host.innerHTML = `<div class="toast ${type === "error" ? "error" : ""}" role="status"><span class="toast-mark">${icon(type === "error" ? "warning" : "check")}</span><span>${esc(message)}</span></div>`;
  window.setTimeout(() => { if (host.isConnected) host.innerHTML = ""; }, 3500);
}

function closeModal() { state.modal = null; render(); }

async function mutate(path, values, successText, refresh = true) {
  try {
    await apiPost(path, values);
    state.modal = null;
    showToast(successText);
    if (refresh) await loadData();
  } catch (error) {
    showToast(error.message || "Không thể lưu thay đổi.", "error");
  }
}

root.addEventListener("click", async event => {
  const button = event.target.closest("[data-action]");
  if (!button) return;
  const action = button.dataset.action;
  const id = button.dataset.id;
  if (action === "modal-close") { closeModal(); return; }
  if (action === "retry") { await loadData(); return; }
  if (action === "detail" || action === "open-product") {
    try { await reloadDetail(action === "open-product" ? "products" : button.dataset.type, id); }
    catch (error) { showToast(error.message || "Không thể tải chi tiết.", "error"); }
    return;
  }
  if (action === "edit-user") {
    try { await reloadDetail("users", id); }
    catch (error) { showToast(error.message || "Không thể tải hồ sơ.", "error"); }
    return;
  }
  if (action === "user-status") {
    const next = button.dataset.status;
    const text = next === "LOCKED"
      ? "Khóa tài khoản này? Người dùng sẽ không thể đăng nhập cho tới khi được mở khóa."
      : "Mở khóa tài khoản này? Bộ đếm đăng nhập sai sẽ được đặt lại.";
    if (window.confirm(text)) await mutate(`/users/${encodeURIComponent(id)}/status`, { status: next }, "Đã cập nhật trạng thái tài khoản.");
    return;
  }
  if (action === "user-delete") {
    if (window.confirm("Xoá MỀM tài khoản này? Đơn hàng, thanh toán và đánh giá vẫn được giữ nguyên; admin có thể khôi phục sau.")) {
      await mutate(`/users/${encodeURIComponent(id)}/delete`, {}, "Đã xoá mềm tài khoản.");
    }
    return;
  }
  if (action === "user-restore") {
    if (window.confirm("Khôi phục tài khoản này?")) {
      await mutate(`/users/${encodeURIComponent(id)}/restore`, {}, "Đã khôi phục tài khoản.");
    }
    return;
  }
  if (action === "seller-status") {
    const next = button.dataset.status;
    if (window.confirm(`Chuyển trạng thái xưởng sang ${next}?`)) await mutate(`/sellers/${encodeURIComponent(id)}/status`, { status: next }, "Đã cập nhật trạng thái xưởng.");
    return;
  }
  if (action === "seller-verify") {
    if (window.confirm("Xác minh xưởng này?")) await mutate(`/sellers/${encodeURIComponent(id)}/verification`, { action: "verify" }, "Đã xác minh xưởng.");
    return;
  }
  if (action === "product-reject") {
    state.modal = { type: "reject-product", title: "Từ chối sản phẩm", subtitle: `Sản phẩm #${id}`, id };
    render();
    document.querySelector("#modal-root textarea")?.focus();
    return;
  }
  if (action === "product-moderate") {
    const next = button.dataset.status;
    const prompts = { approve: "Duyệt sản phẩm này? Trạng thái hoạt động hiện tại sẽ được giữ nguyên.", hide: "Ẩn sản phẩm khỏi catalog công khai?", unhide: "Hiện lại sản phẩm đã duyệt?" };
    if (window.confirm(prompts[next] || "Xác nhận thay đổi?")) {
      await mutate(`/products/${encodeURIComponent(id)}/moderation`, { action: next }, "Đã cập nhật kiểm duyệt sản phẩm.");
    }
  }
});

root.addEventListener("submit", async event => {
  const form = event.target.closest("form[data-form]");
  if (!form) return;
  const values = Object.fromEntries(new FormData(form).entries());
  const id = form.dataset.id;
  if (form.dataset.form === "reject-product") {
    event.preventDefault();
    if (!form.reportValidity()) return;
    if (String(values.reason || "").trim().length < 8) { showToast("Lý do cần ít nhất 8 ký tự.", "error"); return; }
    await mutate(`/products/${encodeURIComponent(id)}/moderation`, { action: "reject", reason: values.reason }, "Đã từ chối sản phẩm và lưu lý do.");
    return;
  }
  if (form.dataset.form === "user-profile") {
    event.preventDefault();
    if (!form.reportValidity()) return;
    await mutate(`/users/${encodeURIComponent(id)}/profile`, values, "Đã lưu hồ sơ người dùng.", false);
    try { await reloadDetail("users", id); } catch (error) { showToast(error.message || "Không thể tải hồ sơ.", "error"); }
    return;
  }
  if (form.dataset.form === "user-role") {
    event.preventDefault();
    if (!form.reportValidity()) return;
    if (!window.confirm(`Đổi vai trò tài khoản #${id} sang ${values.role}? Thao tác được ghi nhật ký bảo mật.`)) return;
    await mutate(`/users/${encodeURIComponent(id)}/role`, { role: values.role }, "Đã đổi vai trò tài khoản.", false);
    try { await reloadDetail("users", id); await loadData(); }
    catch (error) { showToast(error.message || "Đã lưu nhưng không thể tải lại dữ liệu.", "error"); }
    return;
  }
  if (form.dataset.form === "order-status") {
    event.preventDefault();
    if (!form.reportValidity()) return;
    if (!window.confirm(`Cập nhật đơn sang trạng thái “${values.status}”? Thanh toán sẽ không thay đổi.`)) return;
    await mutate(`/orders/${encodeURIComponent(id)}/status`, values, "Đã cập nhật trạng thái và ghi lịch sử.", false);
    try { await reloadDetail("orders", id); await loadData(); }
    catch (error) { showToast(error.message || "Đã lưu nhưng không thể tải lại dữ liệu.", "error"); }
  }
});

if (state.route === "not-found") render();
else loadData();
