import { apiContextPath, apiGet, apiPost, queryString } from "./api.js";
import { esc, icon } from "./components.js";
import { currentRoute, renderLayout, routeTitles } from "./layout.js?v=delivery-disputes-phase1-r1";
import { renderDashboard } from "./pages/dashboard.js?v=admin-dashboard-statistics-20261126-r1";
import { initStatistics } from "./pages/statistics.js?v=admin-dashboard-statistics-20261126-r1";
import { renderEntityDetail, renderOrders, renderProducts, renderQuotes, renderSellers, renderUsers } from "./pages/management.js";
import { renderReturnRefundDetail, renderReturnRefunds } from "./pages/returns.js?v=admin-ux-20261127-r1";
import { renderBusinessQueue, renderBusinessQueueDetail } from "./pages/queues.js?v=admin-ux-20261127-r1";
import { renderFinance } from "./pages/finance.js?v=finance-pages-20261126-r1";
import { renderDeliveryDisputes, renderDeliveryDisputeDetail } from "./pages/delivery-disputes.js?v=delivery-disputes-phase1-r1";

const root = document.getElementById("admin-app");
const SIDEBAR_STORAGE_KEY = "pk_admin_sidebar_collapsed";
function readSidebarCollapsed() {
  try { return window.localStorage?.getItem(SIDEBAR_STORAGE_KEY) === "1"; } catch { return false; }
}
const filters = Object.fromEntries(new URLSearchParams(window.location.search));
const state = {
  route: currentRoute(apiContextPath), filters, actor: null, data: null, loading: false,
  error: "", modal: null, pageData: null, sidebarCollapsed: readSidebarCollapsed(), notifications: {}
};
let statisticsAnchorHandled = false;

function moneyText(value) { return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(Number(value || 0)); }

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
    case "statistics": return renderDashboard(state.data, apiContextPath);
    case "finance":
    case "finance-cod":
    case "finance-settlements":
    case "finance-refunds":
    case "finance-ledger":
    case "finance-allocations": return renderFinance(state.route, state.data, apiContextPath);
    case "users": return renderUsers(state.data, state, apiContextPath);
    case "sellers": return renderSellers(state.data, state, apiContextPath);
    case "products": return renderProducts(state.data, state, apiContextPath);
    case "orders": return renderOrders(state.data, state, apiContextPath);
    case "shipments":
    case "custom-prints":
    case "order-changes":
    case "reviews":
    case "reconciliation": return renderBusinessQueue(state.route, state.data, state.filters, apiContextPath);
    case "returns": return renderReturnRefunds(state.data, state, apiContextPath);
    case "delivery-disputes": return renderDeliveryDisputes(state.data, state.filters, apiContextPath);
    case "support":
    case "escalations": return state.data?.html || loadingState();
    default: return `<section class="not-found-state"><span class="empty-illustration">${icon("cube")}</span>
      <div class="eyebrow">404 · ADMIN ROUTE</div><h1>Không tìm thấy trang</h1><p>Chọn một mục quản trị từ menu để tiếp tục.</p>
      <a class="button" href="${apiContextPath}/admin">Về Dashboard</a></section>`;
  }
}

function modalMarkup() {
  if (!state.modal) return "";
  let body = state.modal.body;
  if (state.modal.type === "create-user") {
    body = `<p class="muted-note">Tài khoản mới sẽ được kích hoạt ngay. Mật khẩu được băm trước khi lưu vào cơ sở dữ liệu.</p>
      <form class="stack-form create-user-form" data-form="create-user">
        <div class="form-grid-two">
          <label class="form-field">Họ và tên<input name="fullName" maxlength="120" required autocomplete="name" placeholder="Nguyễn Văn A"></label>
          <label class="form-field">Số điện thoại<input name="phone" maxlength="25" inputmode="tel" autocomplete="tel" placeholder="09xx xxx xxx"></label>
          <label class="form-field form-field-wide">Email đăng nhập<input type="email" name="email" maxlength="254" required autocomplete="email" placeholder="nguoidung@example.com"></label>
          <label class="form-field form-field-wide">Mật khẩu tạm thời<input type="password" name="password" minlength="8" maxlength="128" required autocomplete="new-password" placeholder="Ít nhất 8 ký tự"><small class="field-hint">Không sử dụng mật khẩu của tài khoản cá nhân.</small></label>
          <label class="form-field form-field-wide">Vai trò<select name="role" required><option value="CUSTOMER">Khách hàng</option><option value="ADMIN">Quản trị viên</option></select></label>
        </div>
        <div class="modal-actions"><button class="button secondary" type="button" data-action="modal-close">Hủy</button><button class="button" type="submit">${icon("plus")} Tạo tài khoản</button></div>
      </form>`;
  }
  if (state.modal.type === "reject-product") {
    body = `<p class="muted-note">Ghi lý do cụ thể để seller biết nội dung cần điều chỉnh.</p>
      <form class="stack-form" data-form="reject-product" data-id="${esc(state.modal.id)}">
        <label class="form-field">Lý do từ chối<textarea name="reason" required minlength="8" maxlength="500"></textarea></label>
        <div class="modal-actions"><button class="button secondary" type="button" data-action="modal-close">Hủy</button>
          <button class="button danger" type="submit">Từ chối sản phẩm</button></div></form>`;
  }
  if (state.modal.type === "seller-verify") {
    const decision = state.modal.decision;
    const approve = decision === "APPROVE";
    const reject = decision === "REJECT";
    const actionLabel = approve ? "Duyệt và kích hoạt xưởng" : reject ? "Từ chối hồ sơ" : "Yêu cầu bổ sung";
    const instruction = approve
      ? "Khi xác nhận, xưởng sẽ chuyển sang VERIFIED và ACTIVE, chủ xưởng có thể truy cập trang quản trị."
      : reject
        ? "Hồ sơ sẽ chuyển sang REJECTED và xưởng tiếp tục ở trạng thái INACTIVE."
        : "Hồ sơ vẫn ở trạng thái PENDING và xưởng chưa được phép truy cập trang quản trị.";
    body = `<div class="stack-form">
      <div class="notice-panel"><strong>${esc(state.modal.shopName || "Xưởng #" + state.modal.id)}</strong><p>${esc(instruction)}</p></div>
      <form class="stack-form" data-form="seller-verify" data-id="${esc(state.modal.id)}" data-decision="${esc(decision)}">
        <label class="form-field">${approve ? "Ghi chú xác minh" : reject ? "Lý do từ chối" : "Thông tin cần bổ sung"}
          <textarea name="reason" required minlength="3" maxlength="1000" rows="4" placeholder="Nhập nội dung ít nhất 3 ký tự…"></textarea>
          <small class="field-hint">Nội dung được lưu vào lịch sử xử lý hồ sơ.</small>
        </label>
        <div class="modal-actions">
          <button class="button secondary" type="button" data-action="modal-close">Hủy</button>
          <button class="button ${reject ? "danger" : ""}" type="submit">${esc(actionLabel)}</button>
        </div>
      </form>
    </div>`;
  }
  return `<div class="modal-backdrop" data-modal-backdrop><section class="modal-card large" role="dialog" aria-modal="true" aria-labelledby="modal-title">
    <header class="modal-head"><div><h2 id="modal-title">${esc(state.modal.title)}</h2><p>${esc(state.modal.subtitle || "Dữ liệu đã lưu trong marketplace")}</p></div>
      <button class="icon-button" data-action="modal-close" aria-label="Đóng">${icon("close")}</button></header>
    <div class="modal-body">${body}</div></section></div>`;
}

function isJspRoute(route) {
  return route === "support" || route === "escalations";
}

function ensureFragmentStyles(doc, route) {
  if (document.head.querySelector(`style[data-admin-jsp-style="${route}"]`)) return;
  for (const styleEl of doc.head.querySelectorAll("style")) {
    const clone = document.createElement("style");
    clone.setAttribute("data-admin-jsp-style", route);
    clone.textContent = styleEl.textContent;
    document.head.appendChild(clone);
  }
}

async function fetchJspFragment(urlStr, route, options = {}) {
  let response;
  try {
    response = await fetch(urlStr, { credentials: "same-origin", cache: "no-store", ...options });
  } catch {
    throw new Error("Không thể kết nối máy chủ. Hãy kiểm tra kết nối rồi thử lại.");
  }
  if (response.status === 401 || new URL(response.url, location.href).pathname.endsWith("/login")) {
    window.location.assign(`${apiContextPath}/login`);
    throw new Error("Phiên đăng nhập đã hết hạn.");
  }
  const html = await response.text();
  const doc = new DOMParser().parseFromString(html, "text/html");
  const selector = route === "support" ? "#support-page-content" : "#escalation-page-content";
  const fragment = doc.querySelector(selector);
  if (!fragment) {
    window.location.assign(response.url || urlStr);
    throw new Error("Không thể đọc nội dung trang quản trị.");
  }
  ensureFragmentStyles(doc, route);
  const metaToken = doc.querySelector('meta[name="csrf-token"]')?.content;
  const localMeta = document.querySelector('meta[name="csrf-token"]');
  if (metaToken && localMeta) localMeta.content = metaToken;
  if (response.url) {
    const finalUrl = new URL(response.url, location.href);
    const nextPath = finalUrl.pathname + finalUrl.search;
    if (nextPath !== location.pathname + location.search) {
      history.replaceState({}, "", nextPath);
      state.filters = Object.fromEntries(finalUrl.searchParams);
    }
  }
  return { html: fragment.outerHTML };
}

function syncSidebarAndHeader(safeRoute) {
  const title = routeTitles[safeRoute] || "Không tìm thấy trang";
  document.title = `${title} · PrintKraft Admin`;
  const breadcrumbTitle = root.querySelector(".breadcrumbs strong");
  if (breadcrumbTitle) breadcrumbTitle.textContent = title;

  const dropdown = root.querySelector(".admin-sidebar .nav-dropdown");
  if (dropdown) dropdown.classList.toggle("is-active", safeRoute.startsWith("finance"));

  const isStatsHash = safeRoute === "dashboard" && window.location.hash === "#statistics";
  for (const link of root.querySelectorAll(".admin-sidebar .nav-item[data-route], .admin-sidebar .nav-subitem[data-route]")) {
    const key = link.dataset.route;
    const active = link.classList.contains("nav-subitem")
      ? key === safeRoute
      : key === "finance"
        ? safeRoute.startsWith("finance")
        : key === "statistics"
          ? isStatsHash
          : key === "dashboard"
            ? (safeRoute === "dashboard" && !isStatsHash)
            : key === safeRoute;
    link.classList.toggle("active", active);
    if (active && !link.closest(".nav-dropdown > .nav-item")) {
      link.setAttribute("aria-current", "page");
    } else {
      link.removeAttribute("aria-current");
    }
  }

  for (const [key, countKey] of [["sellers", "pendingSellers"], ["products", "pendingProducts"]]) {
    const link = root.querySelector(`.admin-sidebar .nav-item[data-route="${key}"]`);
    if (!link) continue;
    const count = Number(state.notifications?.[countKey] || 0);
    const existing = link.querySelector(".nav-alert");
    if (count > 0) {
      const label = count > 99 ? "99+" : String(count);
      if (existing) {
        existing.textContent = label;
        existing.setAttribute("aria-label", `${count} mục chờ duyệt`);
      } else {
        const badge = document.createElement("span");
        badge.className = "nav-alert";
        badge.setAttribute("aria-label", `${count} mục chờ duyệt`);
        badge.textContent = label;
        link.appendChild(badge);
      }
    } else if (existing) {
      existing.remove();
    }
  }

  const adminCopy = root.querySelector(".sidebar-admin-copy");
  if (adminCopy && state.actor) {
    const displayName = state.actor.name || "Tài khoản quản trị";
    const actorRole = state.actor.role === "ADMIN" ? "Quản trị viên" : (state.actor.role || "");
    adminCopy.innerHTML = `<strong>${esc(displayName)}</strong>${actorRole ? `<small>${esc(actorRole)}</small>` : ""}`;
  }
}

function render() {
  const safeRoute = routeTitles[state.route] ? state.route : "not-found";
  const shell = root.querySelector(".admin-shell");
  const pageEl = root.querySelector(".page-content");
  if (!shell || !pageEl) {
    root.innerHTML = renderLayout(safeRoute, pageContent(), apiContextPath, state.sidebarCollapsed, state.actor, state.notifications);
    document.title = `${routeTitles[safeRoute] || "Không tìm thấy trang"} · PrintKraft Admin`;
  } else {
    pageEl.innerHTML = `${pageContent()}<footer class="page-footer"><span>PrintKraft Marketplace</span><span>Admin</span></footer>`;
    syncSidebarAndHeader(safeRoute);
  }
  if ((state.route === "dashboard" || state.route === "statistics") && !state.loading && !state.error) {
    initStatistics(apiContextPath);
    if (!statisticsAnchorHandled) {
      statisticsAnchorHandled = true;
      if (window.location.hash === "#statistics") {
        document.getElementById("statistics")?.scrollIntoView({ block: "start" });
      }
    }
  }
  if (isJspRoute(state.route) && !state.loading && !state.error) {
    const conv = root.querySelector(".support-conversation, .escalation-conversation");
    if (conv) conv.scrollTop = conv.scrollHeight;
  }
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
    if (isJspRoute(state.route)) {
      state.data = await fetchJspFragment(`${location.pathname}${location.search}`, state.route);
      state.notifications = await apiGet("/notification-summary").catch(() => ({}));
      return;
    }
    let endpoint;
    if (state.route === "dashboard" || state.route === "statistics") endpoint = "/dashboard";
    else if (state.route.startsWith("finance")) endpoint = "/finance";
    else if (state.route === "orders" && state.filters.view === "quotes") endpoint = "/quotes";
    else if (state.route === "returns") endpoint = "/return-refunds";
    else endpoint = `/${state.route}`;
    state.data = await apiGet(`${endpoint}${apiQuery()}`);
    try { state.notifications = await apiGet("/notification-summary"); }
    catch { state.notifications = {}; }
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
    if (refresh) await loadData().then(startAdminNotificationPolling);
  } catch (error) {
    showToast(error.message || "Không thể lưu thay đổi.", "error");
  }
}

let adminNotificationTimerStarted = false;
async function refreshAdminNotifications() {
  if (state.loading) return;
  try {
    const next = await apiGet("/notification-summary");
    const changed = JSON.stringify(next) !== JSON.stringify(state.notifications);
    state.notifications = next;
    if (changed) render();
  } catch { /* Badge refresh must not block admin operations. */ }
}
function startAdminNotificationPolling() {
  if (adminNotificationTimerStarted || typeof window.setInterval !== "function") return;
  adminNotificationTimerStarted = true;
  window.setInterval(refreshAdminNotifications, 30000);
}

// ── SPA Navigation Interceptor ────────────────────────────────────────────────
// Intercept clicks on sidebar nav links, finance help links, and JSP sub-links
// so the entire admin workspace behaves as a seamless SPA without full reloads.
root.addEventListener("click", event => {
  // Only intercept plain left-clicks (no modifier keys, no non-left button)
  if (event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
  const link = event.target.closest("a[href]");
  if (!link) return;
  // Ignore links with explicit target, download, or external origin
  if (link.target || link.download) return;
  let url;
  try { url = new URL(link.href, location.href); } catch { return; }
  if (url.origin !== location.origin) return;
  const path = url.pathname;
  const contextPrefix = apiContextPath || "";
  const adminBase = contextPrefix + "/admin";
  if (path !== adminBase && !path.startsWith(adminBase + "/")) return;
  // Do not intercept file/API endpoints if linked directly
  if (path.startsWith(adminBase + "/api/")) return;
  // Same path & query — handle hash transitions (#statistics vs Dashboard top) without refetching
  if (url.pathname === location.pathname && url.search === location.search) {
    event.preventDefault();
    if (url.hash !== location.hash) {
      history.pushState({}, "", url.pathname + url.search + url.hash);
      syncSidebarAndHeader(routeTitles[state.route] ? state.route : "not-found");
      if (url.hash === "#statistics") {
        document.getElementById("statistics")?.scrollIntoView({ behavior: "smooth", block: "start" });
      } else if (!url.hash) {
        window.scrollTo({ top: 0, behavior: "smooth" });
      }
    }
    return;
  }
  event.preventDefault();
  history.pushState({}, "", url.pathname + url.search + url.hash);
  statisticsAnchorHandled = false;
  state.route = currentRoute(apiContextPath);
  state.filters = Object.fromEntries(url.searchParams);
  state.data = null;
  state.error = "";
  state.modal = null;
  loadData();
});

// Handle browser back / forward navigation
window.addEventListener("popstate", () => {
  state.route = currentRoute(apiContextPath);
  state.filters = Object.fromEntries(new URLSearchParams(location.search));
  state.data = null;
  state.error = "";
  state.modal = null;
  loadData();
});
// ── End SPA Navigation Interceptor ────────────────────────────────────────────

root.addEventListener("click", async event => {
  const button = event.target.closest("[data-action]");
  if (!button) return;
  const action = button.dataset.action;
  const id = button.dataset.id;
  if (action === "toggle-sidebar") {
    state.sidebarCollapsed = !state.sidebarCollapsed;
    try { window.localStorage?.setItem(SIDEBAR_STORAGE_KEY, state.sidebarCollapsed ? "1" : "0"); } catch {}
    root.querySelector(".admin-shell")?.classList.toggle("sidebar-collapsed", state.sidebarCollapsed);
    const menuToggle = root.querySelector(".menu-toggle");
    menuToggle?.setAttribute("aria-expanded", String(state.sidebarCollapsed));
    menuToggle?.setAttribute("aria-label", `${state.sidebarCollapsed ? "Đóng" : "Mở"} menu điều hướng`);
    (state.sidebarCollapsed ? root.querySelector(".nav-item") : menuToggle)?.focus({ preventScroll: true });
    return;
  }
  if (action === "modal-close") { closeModal(); return; }
  if (action === "retry") { await loadData(); return; }
  if (action === "finance-help") {
    if (state.route === "support") {
      state.modal = {
        type: "support-help",
        title: "Hướng dẫn xử lý ticket",
        subtitle: "Quy trình phản hồi và theo dõi SLA hỗ trợ khách hàng",
        body: `<div style="display:grid;gap:10px;font-size:12px;color:#46536a;line-height:1.6;">
          <p style="margin:0"><strong>1. Trạng thái &amp; SLA:</strong> Ticket mới mở sẽ tính hạn phản hồi đầu tiên. Chuyển sang <em>Chờ khách hàng</em> khi đang đợi khách bổ sung thông tin để tạm dừng đồng hồ SLA.</p>
          <p style="margin:0"><strong>2. Phản hồi vs Ghi chú nội bộ:</strong> Mặc định nội dung trả lời sẽ hiển thị cho khách. Tích chọn <em>Lưu dạng ghi chú nội bộ</em> nếu chỉ trao đổi giữa các Admin.</p>
          <p style="margin:0"><strong>3. Đóng ticket:</strong> Sau khi xử lý xong, cập nhật sang <em>Đã giải quyết</em> hoặc <em>Đã đóng</em>.</p>
          <div class="modal-actions"><button class="button" type="button" data-action="modal-close">Đã hiểu</button></div>
        </div>`
      };
      render();
      return;
    }

    const routeGuides = {
      dashboard: {
        title: "Hướng dẫn sử dụng · Dashboard",
        subtitle: "Đọc nhanh tình hình vận hành marketplace",
        steps: [
          ["Bắt đầu từ chỉ số tổng quan", "Các thẻ người dùng, xưởng in, sản phẩm và đơn hàng phản ánh số liệu hiện có trong cơ sở dữ liệu."],
          ["Xem biểu đồ theo thời gian", "Trong khu vực Thống kê & phân tích, chọn ngày bắt đầu, ngày kết thúc và chỉ số rồi bấm Cập nhật báo cáo."],
          ["Theo dõi danh sách cần xử lý", "Mở đơn hàng gần đây, sản phẩm chờ duyệt hoặc xưởng chờ xác minh để đi đến nghiệp vụ tương ứng."]
        ],
        warning: "Biểu đồ doanh thu chỉ phản ánh dữ liệu thanh toán mà hệ thống ghi nhận; hãy đối chiếu báo cáo tài chính trước khi dùng cho quyết toán."
      },
      users: {
        title: "Hướng dẫn sử dụng · Người dùng",
        subtitle: "Tra cứu, cập nhật hồ sơ và kiểm soát quyền truy cập",
        steps: [
          ["Tìm kiếm và lọc", "Kết hợp từ khóa với vai trò và trạng thái để giới hạn danh sách; bấm Xóa lọc để quay lại toàn bộ dữ liệu."],
          ["Xem hoặc sửa hồ sơ", "Dùng biểu tượng con mắt để đọc thông tin và lịch sử; dùng biểu tượng cài đặt để cập nhật trường được phép."],
          ["Khóa hoặc mở khóa", "Chỉ khóa khi có lý do quản trị rõ ràng. Không thể tự khóa tài khoản Admin đang đăng nhập."]
        ],
        warning: "Khóa tài khoản không xóa dữ liệu đơn hàng và lịch sử giao dịch."
      },
      sellers: {
        title: "Hướng dẫn sử dụng · Seller / Xưởng in",
        subtitle: "Xác minh hồ sơ và trạng thái hoạt động của xưởng",
        steps: [
          ["Lọc theo xác minh và trạng thái", "Xem riêng hồ sơ đang chờ, đã xác minh, bị từ chối hoặc xưởng đang tạm ngưng."],
          ["Mở chi tiết trước khi quyết định", "Kiểm tra chủ xưởng, thông tin liên hệ, khu vực và lịch sử trước khi duyệt."],
          ["Lưu quyết định có lý do", "Duyệt, từ chối hoặc yêu cầu bổ sung phải có ghi chú để xưởng hiểu bước tiếp theo."]
        ],
        warning: "Trạng thái xác minh hồ sơ và trạng thái hoạt động của xưởng là hai thông tin khác nhau."
      },
      products: {
        title: "Hướng dẫn sử dụng · Sản phẩm",
        subtitle: "Kiểm duyệt nội dung và trạng thái hiển thị",
        steps: [
          ["Lọc theo danh mục, xưởng và kiểm duyệt", "Dùng nhiều bộ lọc cùng lúc để tìm đúng hồ sơ."],
          ["Đọc chi tiết trước khi thao tác", "Kiểm tra nội dung, danh mục, chào bán của xưởng và lý do cần sửa."],
          ["Duyệt, ẩn hoặc từ chối", "Từ chối cần nêu lý do cụ thể; thao tác duyệt không tự động bật sản phẩm đã ẩn."]
        ],
        warning: "Admin quản lý kiểm duyệt và hiển thị, không sửa thay nội dung do xưởng cung cấp."
      },
      orders: {
        title: "Hướng dẫn sử dụng · Đơn hàng",
        subtitle: "Theo dõi trạng thái đơn và thanh toán riêng biệt",
        steps: [
          ["Lọc theo trạng thái đơn và thanh toán", "Mã đơn, email hoặc số điện thoại giúp truy ra đúng hồ sơ."],
          ["Mở chi tiết và lịch sử", "Kiểm tra các dòng hàng, đơn vị bán, giao hàng và lịch sử trạng thái trước khi cập nhật."],
          ["Ghi chú khi đổi trạng thái", "Chọn trạng thái hợp lệ theo quy trình và ghi rõ lý do cho các thay đổi nhạy cảm."]
        ],
        warning: "Cập nhật trạng thái đơn không đồng nghĩa với xác nhận thanh toán hoặc đã hoàn tiền."
      },
      shipments: {
        title: "Hướng dẫn sử dụng · Vận đơn",
        subtitle: "Cập nhật thông tin theo trạng thái vận chuyển thực tế",
        steps: [
          ["Tìm đúng mã đơn hoặc mã vận đơn", "Có thể tìm theo hãng, xưởng, khách hàng hoặc mã theo dõi."],
          ["Cập nhật hãng, mã theo dõi và trạng thái", "Các trạng thái đã gửi, đang giao hoặc đã giao yêu cầu hãng vận chuyển và mã vận đơn."],
          ["Lưu ghi chú đối chiếu", "Ghi lại vấn đề giao hàng hoặc lý do giao thất bại để lưu vào lịch sử."]
        ],
        warning: "Không mở lại vận đơn đã hoàn tất hoặc đã hoàn/trả; chỉ nhập thông tin theo thực tế."
      },
      "custom-prints": {
        title: "Hướng dẫn sử dụng · Yêu cầu in 3D",
        subtitle: "Rà soát hồ sơ, không thay xưởng báo giá",
        steps: [
          ["Mở chi tiết yêu cầu", "Kiểm tra tệp thiết kế, thông số, vật liệu, số lượng, khách hàng và trạng thái phân tích tệp."],
          ["Ghi nhận đã rà soát", "Thêm ghi chú nếu hồ sơ đã được kiểm tra và không cần theo dõi đặc biệt."],
          ["Chuyển cần theo dõi", "Chọn khi có rủi ro hoặc cần bộ phận khác xử lý; ghi rõ lý do."]
        ],
        warning: "Admin không tự định giá, gửi báo giá hay tạo đơn từ màn hình rà soát."
      },
      "order-changes": {
        title: "Hướng dẫn sử dụng · Yêu cầu thay đổi đơn",
        subtitle: "Rà soát yêu cầu đổi thông tin, sản phẩm hoặc hủy đơn",
        steps: [
          ["Đối chiếu đơn gốc", "Kiểm tra trạng thái đơn, thanh toán, sản phẩm, nội dung yêu cầu, phản hồi xưởng và thời hạn."],
          ["Ghi nhận rà soát", "Chỉ đánh dấu đã rà soát sau khi xem nội dung và để lại ghi chú có thể kiểm tra lại."],
          ["Từ chối khi cần thiết", "Nêu rõ lý do. Yêu cầu bị từ chối không tự động hủy đơn, thay đổi đơn hoặc điều chỉnh tiền."]
        ],
        warning: "Không coi việc Admin rà soát là quyết định chấp thuận của xưởng hoặc xác nhận của khách hàng."
      },
      reconciliation: {
        title: "Hướng dẫn sử dụng · Đối soát thanh toán",
        subtitle: "Rà soát giao dịch bất thường và lưu dấu vết xử lý",
        steps: [
          ["Kiểm tra đơn và giao dịch", "Đối chiếu số tiền, cổng thanh toán, mã tham chiếu, trạng thái đơn và trạng thái thanh toán."],
          ["Ghi nhận kết quả", "Chọn Đã đối soát khi đã xác minh thông tin; chọn Cần theo dõi nếu chưa đủ căn cứ."],
          ["Hoàn tiền thủ công đúng quy trình", "Chỉ ghi nhận thành công sau khi hoàn tiền bên ngoài hệ thống và có mã tham chiếu."]
        ],
        warning: "Ghi nhận đối soát thông thường không tự thay đổi trạng thái thanh toán. Hoàn tiền thành công là thao tác tài chính riêng và không thể chỉ dựa vào lời xác nhận chưa kiểm chứng."
      },
      reviews: {
        title: "Hướng dẫn sử dụng · Đánh giá",
        subtitle: "Kiểm duyệt công khai nhưng giữ lại dữ liệu gốc",
        steps: [
          ["Lọc theo trạng thái hiển thị", "Tìm nội dung, sản phẩm, xưởng hoặc khách hàng có liên quan."],
          ["Đọc nội dung và chi tiết", "Kiểm tra điểm, nội dung đánh giá và sản phẩm được đánh giá."],
          ["Ẩn hoặc công khai", "Ẩn cần lý do cụ thể. Không xóa bản ghi gốc để giữ lịch sử quản trị."]
        ],
        warning: "Chỉ thay đổi khả năng hiển thị theo chính sách kiểm duyệt, không chỉnh sửa nội dung đánh giá của khách."
      },
      returns: {
        title: "Hướng dẫn sử dụng · Đổi trả / Hoàn tiền",
        subtitle: "Xử lý theo từng bước và giữ bằng chứng",
        steps: [
          ["Xem hồ sơ và bằng chứng", "Đối chiếu đơn, sản phẩm, số lượng mua/yêu cầu, lý do, hạn đổi trả và hình ảnh đính kèm."],
          ["Duyệt hoặc từ chối", "Nhập đúng số tiền được duyệt; từ chối phải ghi rõ lý do."],
          ["Xác nhận hàng trả và hoàn tiền", "Chỉ chuyển bước khi đã kiểm tra hàng theo chính sách. Ghi mã tham chiếu khi xác nhận giao dịch hoàn tiền."]
        ],
        warning: "Ứng dụng chỉ ghi nhận kết quả hoàn tiền đã thực hiện bên ngoài; không tự chuyển tiền."
      },
      escalations: {
        title: "Hướng dẫn sử dụng · Can thiệp đơn hàng",
        subtitle: "Ghi nhận quyết định quản trị và bảo toàn lịch sử",
        steps: [
          ["Đọc toàn bộ lịch sử", "Xác minh lý do chuyển cấp, trao đổi trước đó và trạng thái đơn hiện tại."],
          ["Ghi chú rõ ràng", "Nêu hành động cần làm, người phụ trách và điều kiện kết thúc nếu có."],
          ["Kiểm tra kết quả", "Sau khi lưu, mở lại hồ sơ để xác nhận nhật ký đã được cập nhật."]
        ],
        warning: "Không dùng can thiệp vận hành để bỏ qua quy tắc thay đổi đơn, thanh toán hoặc hoàn tiền."
      }
    };
    const guide = routeGuides[state.route];
    if (guide) {
      state.modal = {
        type: "admin-guide",
        title: guide.title,
        subtitle: guide.subtitle,
        body: '<div class="workflow-guide"><ol class="workflow-steps">' +
          guide.steps.map((step, index) => '<li><span>' + String(index + 1).padStart(2, "0") + '</span><div><strong>' +
            esc(step[0]) + '</strong><p>' + esc(step[1]) + '</p></div></li>').join("") +
          '</ol><div class="notice-panel"><strong>Lưu ý nghiệp vụ</strong><p>' + esc(guide.warning) +
          '</p></div><div class="modal-actions"><button class="button" type="button" data-action="modal-close">Đã hiểu</button></div></div>'
      };
      render();
      return;
    }
    state.modal = {
      type: "finance-help",
      title: "Hướng dẫn sử dụng · Tài chính & Hoa hồng",
      subtitle: "Cách đọc số liệu và thao tác an toàn trong từng khu vực",
      body: `<div class="finance-help-content">
        <div class="notice-panel"><strong>Chính sách hiện tại của PrintKraft</strong>
          <p>Hoa hồng nền tảng là <b>10%</b> trên giá trị sản phẩm. Phí vận chuyển không thuộc cơ sở tính hoa hồng. Quyết toán theo tháng và theo từng xưởng.</p>
        </div>
        <div class="finance-help-grid">
          <article><span>01</span><div><strong>Dashboard</strong><p>Xem tổng giá trị sản phẩm đã ghi nhận, hoa hồng nền tảng, khoản còn phải trả xưởng và COD đang chờ xác nhận.</p><a href="${apiContextPath}/admin/finance">Mở Dashboard</a></div></article>
          <article><span>02</span><div><strong>COD chờ nền tảng xác nhận đã nhận tiền</strong><p>Chỉ mở thao tác xác nhận sau khi đối chiếu sao kê/chứng từ tiền đã chuyển về PrintKraft. Nhập đúng tên đơn vị vận chuyển và mã tham chiếu COD.</p><a href="${apiContextPath}/admin/finance/cod">Mở danh sách COD</a></div></article>
          <article><span>03</span><div><strong>Quyết toán theo tháng</strong><p>Chọn tháng dương lịch đã kết thúc để tổng hợp tiền của các đơn đã hoàn thành. Khi chuyển tiền bên ngoài hệ thống, nhập mã giao dịch ngân hàng để xác nhận PAID.</p><a href="${apiContextPath}/admin/finance/settlements">Mở quyết toán</a></div></article>
          <article><span>04</span><div><strong>Điều chỉnh hoàn tiền</strong><p>Theo dõi tiền hoàn, phần hoa hồng bị đảo và khoản cần thu hồi từ xưởng. Khoản thu hồi chưa bù đủ sẽ được giữ lại cho kỳ tiếp theo.</p><a href="${apiContextPath}/admin/finance/refunds">Mở điều chỉnh hoàn tiền</a></div></article>
          <article><span>05</span><div><strong>Sổ cái tài chính</strong><p>Tra cứu nhật ký ghi nợ/ghi có, mã tham chiếu và diễn giải. Đây là nơi kiểm tra dấu vết thu tiền, phí vận chuyển, hoa hồng và chi trả.</p><a href="${apiContextPath}/admin/finance/ledger">Mở sổ cái</a></div></article>
          <article><span>06</span><div><strong>Phân bổ tài chính gần đây</strong><p>Kiểm tra từng dòng sản phẩm: giá trị hàng, tỷ lệ hoa hồng được snapshot tại thời điểm ghi nhận và số tiền xưởng nhận.</p><a href="${apiContextPath}/admin/finance/allocations">Mở phân bổ tài chính</a></div></article>
        </div>
        <div class="notice-panel finance-help-warning"><strong>Lưu ý quan trọng</strong>
          <p>Đơn COD chỉ được xác nhận khi tiền thực sự về tài khoản PrintKraft. Không tạo lại kỳ tháng đã khóa. Không xác nhận đã chuyển tiền trước khi giao dịch ngân hàng thành công. Hệ thống không tự thực hiện chuyển khoản.</p>
        </div>
        <div class="modal-actions"><button class="button" type="button" data-action="modal-close">Đã hiểu</button></div>
      </div>`
    };
    render();
    return;
  }

  if (action === "finance-cod-receive") {
    state.modal = {
      type: "finance-cod",
      title: "Xác nhận đã nhận tiền COD",
      subtitle: "Đơn #" + id,
      id,
      body: `<div class="notice-panel"><strong>Chỉ xác nhận khi tiền đã thực sự được chuyển về PrintKraft.</strong>
        <p>Thao tác sẽ đánh dấu khoản COD đã thu và ghi nhận phân bổ hoa hồng 10% cho từng xưởng trong cùng một giao dịch cơ sở dữ liệu.</p></div>
        <form class="stack-form" data-form="finance-cod" data-id="${esc(id)}">
          <label class="form-field">Đơn vị vận chuyển<input name="carrierName" required maxlength="120" placeholder="Ví dụ: Đơn vị vận chuyển"></label>
          <label class="form-field">Mã tham chiếu chuyển COD<input name="carrierReference" required maxlength="120" minlength="3" placeholder="Mã đối soát / mã chuyển tiền"></label>
          <label class="form-field">Ghi chú<textarea name="note" maxlength="500" rows="3" placeholder="Thông tin đối soát bổ sung (không bắt buộc)"></textarea></label>
          <div class="modal-actions"><button class="button secondary" type="button" data-action="modal-close">Hủy</button>
            <button class="button" type="submit">Xác nhận tiền đã về</button></div>
        </form>`
    };
    render();
    return;
  }
  if (action === "finance-create-settlement") {
    const now = new Date();
    const previous = new Date(now.getFullYear(), now.getMonth() - 1, 1);
    const period = `${previous.getFullYear()}-${String(previous.getMonth() + 1).padStart(2, "0")}`;
    state.modal = {
      type: "finance-create-settlement",
      title: "Tạo kỳ quyết toán tháng",
      subtitle: "Chỉ tạo kỳ cho tháng dương lịch đã kết thúc",
      body: `<div class="notice-panel"><strong>Kỳ quyết toán sẽ khóa các khoản phân bổ chưa quyết toán của tháng đã chọn.</strong>
        <p>Hệ thống tổng hợp theo từng xưởng. Không thể tạo kỳ cho tháng hiện tại hoặc tương lai.</p></div>
        <form class="stack-form" data-form="finance-create-settlement">
          <label class="form-field">Tháng quyết toán<input type="month" name="period" required max="${period}" value="${period}"></label>
          <div class="modal-actions"><button class="button secondary" type="button" data-action="modal-close">Hủy</button>
            <button class="button" type="submit">Tổng hợp và khóa kỳ tháng</button></div>
        </form>`
    };
    render();
    return;
  }
  if (action === "finance-settlement-pay") {
    state.modal = {
      type: "finance-settlement-pay",
      title: "Xác nhận đã chuyển tiền cho xưởng",
      subtitle: (button.dataset.shop || "Xưởng") + " · " + moneyText(button.dataset.amount),
      id,
      body: `<div class="notice-panel"><strong>Chỉ xác nhận sau khi chuyển khoản thành công ngoài hệ thống.</strong>
        <p>Hệ thống sẽ chuyển kỳ sang PAID và lưu mã tham chiếu vào sổ cái tài chính.</p></div>
        <form class="stack-form" data-form="finance-settlement-pay" data-id="${esc(id)}">
          <label class="form-field">Mã tham chiếu giao dịch<input name="payoutReference" required minlength="3" maxlength="120" placeholder="Mã giao dịch ngân hàng"></label>
          <label class="form-field">Ghi chú<textarea name="note" maxlength="500" rows="3" placeholder="Nội dung chuyển khoản (không bắt buộc)"></textarea></label>
          <div class="modal-actions"><button class="button secondary" type="button" data-action="modal-close">Hủy</button>
            <button class="button" type="submit">Xác nhận đã chuyển</button></div>
        </form>`
    };
    render();
    return;
  }
  if (action === "detail" || action === "open-product") {
    try { await reloadDetail(action === "open-product" ? "products" : button.dataset.type, id); }
    catch (error) { showToast(error.message || "Không thể tải chi tiết.", "error"); }
    return;
  }

  if (action === "business-queue-detail") {
    const queue = button.dataset.queue;
    try {
      const item = await apiGet("/" + queue + "/" + encodeURIComponent(id));
      state.modal = {
        type: "business-queue-detail",
        title: ({ "custom-prints": "Chi tiết yêu cầu in 3D", "order-changes": "Chi tiết yêu cầu thay đổi đơn",
          reviews: "Chi tiết đánh giá", reconciliation: "Chi tiết đối soát thanh toán" })[queue] || "Chi tiết nghiệp vụ",
        subtitle: item.code || item.orderCode || item.productName || item.providerTransactionRef || ("#" + id),
        body: renderBusinessQueueDetail(queue, item), id, queue
      };
      render();
    } catch (error) {
      showToast(error.message || "Không thể tải chi tiết nghiệp vụ.", "error");
    }
    return;
  }


  if (action === "business-queue-action") {
    const queue = button.dataset.queue;
    const nextAction = button.dataset.nextAction;
    const records = Array.isArray(state.data) ? state.data : (Array.isArray(state.data?.items) ? state.data.items : []);
    const current = records.find(row => String(row.id ?? row.requestId ?? "") === String(id)) || {};
    const titles = {
      shipments: "Cập nhật vận đơn",
      "custom-prints": nextAction === "ESCALATED" ? "Chuyển yêu cầu in 3D sang theo dõi" : "Ghi nhận rà soát yêu cầu in 3D",
      "order-changes": nextAction === "REJECTED" ? "Từ chối yêu cầu thay đổi đơn" : "Rà soát yêu cầu thay đổi đơn",
      reviews: nextAction === "UNPUBLISH" ? "Ẩn đánh giá" : "Công khai đánh giá",
      reconciliation: nextAction === "MANUAL_REFUND" ? "Ghi nhận hoàn tiền thành công" :
        nextAction === "MANUAL_REFUND_FAILED" ? "Ghi nhận hoàn tiền thất bại" :
        nextAction === "FOLLOW_UP" ? "Chuyển giao dịch sang theo dõi" : "Ghi nhận đã đối soát"
    };
    const recordLabel = current.code || current.orderCode || current.productName || current.providerTransactionRef || ("#" + id);
    let fields = "";
    let notice = "";
    let submitLabel = "Lưu ghi nhận";
    let danger = false;
    if (queue === "shipments") {
      const statuses = ["PREPARING", "SHIPPED", "IN_TRANSIT", "DELIVERED", "FAILED", "RETURNED"];
      fields = '<label class="form-field">Hãng vận chuyển<input name="carrier" required maxlength="100" value="' + esc(current.carrier || "") + '" placeholder="Tên đơn vị vận chuyển"></label>' +
        '<label class="form-field">Mã vận đơn<input name="trackingNumber" maxlength="120" value="' + esc(current.trackingNumber || "") + '" placeholder="Mã theo dõi từ hãng"></label>' +
        '<label class="form-field">Trạng thái vận đơn<select name="status" required>' +
        statuses.map(status => '<option value="' + status + '"' + (String(current.status || "PREPARING") === status ? " selected" : "") + '>' + status + '</option>').join("") +
        '</select></label><label class="form-field">Ghi chú lịch sử<textarea name="note" maxlength="500" rows="3" placeholder="Nội dung bổ sung (không bắt buộc)"></textarea></label>';
      notice = '<div class="notice-panel"><strong>Trạng thái phản ánh tình trạng thực tế.</strong><p>Đã gửi, đang vận chuyển và đã giao yêu cầu có hãng vận chuyển và mã vận đơn.</p></div>';
      submitLabel = "Lưu cập nhật vận đơn";
    } else if (queue === "custom-prints") {
      fields = '<label class="form-field">Ghi chú rà soát<textarea name="note" required minlength="3" maxlength="500" rows="4" placeholder="Nêu nội dung đã kiểm tra hoặc lý do cần theo dõi"></textarea></label>';
      notice = '<div class="notice-panel"><strong>Admin chỉ rà soát hồ sơ.</strong><p>Định giá, gửi báo giá và tạo đơn vẫn do quy trình xưởng/khách hàng phụ trách.</p></div>';
      submitLabel = nextAction === "ESCALATED" ? "Chuyển theo dõi" : "Xác nhận đã rà soát";
      danger = nextAction === "ESCALATED";
    } else if (queue === "order-changes") {
      fields = '<label class="form-field">' + (nextAction === "REJECTED" ? "Lý do từ chối" : "Ghi chú rà soát") +
        '<textarea name="note" required minlength="3" maxlength="500" rows="4" placeholder="Nêu căn cứ và nội dung cần lưu lại"></textarea></label>';
      notice = '<div class="notice-panel"><strong>Không tự áp dụng thay đổi hoặc hủy đơn.</strong><p>Việc từ chối chỉ đóng yêu cầu đang chờ và lưu lịch sử; trạng thái đơn/thanh toán không tự đổi.</p></div>';
      submitLabel = nextAction === "REJECTED" ? "Xác nhận từ chối" : "Lưu rà soát";
      danger = nextAction === "REJECTED";
    } else if (queue === "reviews") {
      fields = '<label class="form-field">' + (nextAction === "UNPUBLISH" ? "Lý do ẩn đánh giá" : "Ghi chú kiểm duyệt") +
        '<textarea name="reason" ' + (nextAction === "UNPUBLISH" ? 'required minlength="3"' : "") +
        ' maxlength="500" rows="4" placeholder="Nhập lý do hoặc ghi chú"></textarea></label>';
      notice = '<div class="notice-panel"><strong>Giữ nguyên bản ghi gốc.</strong><p>Thao tác chỉ thay đổi việc hiển thị công khai và tạo lịch sử kiểm duyệt.</p></div>';
      submitLabel = nextAction === "UNPUBLISH" ? "Ẩn đánh giá" : "Công khai đánh giá";
      danger = nextAction === "UNPUBLISH";
    } else if (queue === "reconciliation" && (nextAction === "MANUAL_REFUND" || nextAction === "MANUAL_REFUND_FAILED")) {
      const failed = nextAction === "MANUAL_REFUND_FAILED";
      const methods = [
        ["ORIGINAL_PAYMENT", "Hoàn về phương thức ban đầu"], ["BANK_TRANSFER", "Chuyển khoản ngân hàng"],
        ["COD_MANUAL", "Hoàn COD thủ công"], ["CASH", "Tiền mặt"], ["OTHER", "Khác"]
      ];
      fields = '<label class="form-field">Phương thức hoàn tiền<select name="method" required>' +
        methods.map(item => '<option value="' + item[0] + '"' + (item[0] === "BANK_TRANSFER" ? " selected" : "") + '>' + item[1] + '</option>').join("") +
        '</select></label><label class="form-field">Mã tham chiếu ' + (failed ? "(nếu có)" : "(bắt buộc)") +
        '<input name="reference" maxlength="120" ' + (failed ? "" : 'required minlength="3"') + ' placeholder="Mã ngân hàng/cổng thanh toán"></label>' +
        '<label class="form-field">' + (failed ? "Lý do thất bại" : "Ghi chú xác nhận hoàn tiền") +
        '<textarea name="note" required minlength="3" maxlength="1000" rows="4"></textarea></label>' +
        '<label class="confirm-check"><input type="checkbox" name="externalConfirmed" required> ' +
        (failed ? "Tôi đã kiểm tra kết quả lần thử hoàn tiền bên ngoài và xác nhận thất bại." :
          "Tôi đã thực hiện hoàn tiền bên ngoài hệ thống và xác nhận giao dịch thành công.") + '</label>';
      notice = '<div class="notice-panel"><strong>Đây là ghi nhận tài chính quan trọng.</strong><p>Chỉ ghi nhận kết quả đã kiểm tra bên ngoài hệ thống. Không xác nhận thành công nếu chưa có bằng chứng chuyển tiền.</p></div>';
      submitLabel = failed ? "Ghi nhận thất bại" : "Xác nhận hoàn tiền thành công";
      danger = failed;
    } else if (queue === "reconciliation") {
      fields = '<label class="form-field">' + (nextAction === "FOLLOW_UP" ? "Lý do cần theo dõi tiếp" : "Ghi chú đối soát") +
        '<textarea name="note" required minlength="3" maxlength="1000" rows="4"></textarea></label>' +
        '<label class="form-field">Mã tham chiếu ngân hàng/cổng thanh toán (nếu có)<input name="reference" maxlength="120" placeholder="Mã tham chiếu"></label>';
      notice = '<div class="notice-panel"><strong>Đối soát không tự đổi trạng thái thanh toán.</strong><p>Chọn Đã đối soát chỉ sau khi đối chiếu dữ liệu; nếu chưa rõ, chọn Cần theo dõi.</p></div>';
      submitLabel = nextAction === "FOLLOW_UP" ? "Lưu và tiếp tục theo dõi" : "Xác nhận đã đối soát";
    }
    state.modal = {
      type: "business-queue-action",
      title: titles[queue] || "Xử lý nghiệp vụ",
      subtitle: recordLabel,
      body: '<form class="stack-form" data-form="business-queue-action" data-queue="' + esc(queue) +
        '" data-id="' + esc(id) + '" data-next-action="' + esc(nextAction) + '">' + notice + fields +
        '<div class="modal-actions"><button class="button secondary" type="button" data-action="modal-close">Hủy</button>' +
        '<button class="button ' + (danger ? "danger" : "") + '" type="submit">' + submitLabel + '</button></div></form>'
    };
    render();
    return;
  }
  if (action === "delivery-dispute-detail") {
    try {
      const detail = await apiGet(`/delivery-disputes/${encodeURIComponent(id)}`);
      state.modal = {
        type: "delivery-dispute-detail",
        title: "Chi tiết khiếu nại giao hàng",
        subtitle: detail.disputeCode || `Khiếu nại #${id}`,
        body: renderDeliveryDisputeDetail(detail)
      };
      render();
    } catch (error) {
      showToast(error.message || "Không thể tải chi tiết khiếu nại.", "error");
    }
    return;
  }
  if (action === "delivery-dispute-action") {
    const nextAction = button.dataset.nextAction;
    const actionTitles = {
      REVIEW: "Bắt đầu xem xét khiếu nại",
      RESOLVE: "Giải quyết khiếu nại giao hàng",
      REJECT: "Từ chối khiếu nại giao hàng"
    };
    const submitLabels = {
      REVIEW: "Bắt đầu xem xét",
      RESOLVE: "Xác nhận giải quyết",
      REJECT: "Xác nhận từ chối"
    };
    const noteLabel = nextAction === "REJECT" ? "Lý do từ chối" :
      nextAction === "RESOLVE" ? "Kết quả và phương án giải quyết" : "Ghi chú tiếp nhận";
    const notice = nextAction === "RESOLVE" || nextAction === "REJECT"
      ? '<div class="notice-panel"><strong>Kết thúc bước xử lý khiếu nại.</strong><p>Khách hàng sẽ được thông báo và có thể quay lại xác nhận kiện hàng. Đơn chỉ hoàn tất khi mọi kiện đều được xác nhận và không còn khiếu nại đang mở.</p></div>'
      : '<div class="notice-panel"><strong>Chuyển hồ sơ sang Đang xem xét.</strong><p>Đơn hàng tiếp tục bị khóa hoàn tất cho đến khi khiếu nại được giải quyết hoặc từ chối.</p></div>';
    state.modal = {
      type: "delivery-dispute-action",
      title: actionTitles[nextAction] || "Xử lý khiếu nại",
      subtitle: "Hồ sơ #" + id,
      body: '<form class="stack-form" data-form="delivery-dispute-action" data-id="' + esc(id) +
        '" data-next-action="' + esc(nextAction) + '">' + notice +
        '<label class="form-field">' + noteLabel +
        '<textarea name="note" required minlength="3" maxlength="2000" rows="4" placeholder="Ghi rõ nội dung xử lý để lưu vào lịch sử"></textarea></label>' +
        '<div class="modal-actions"><button class="button secondary" type="button" data-action="modal-close">Hủy</button>' +
        '<button class="button ' + (nextAction === "REJECT" ? "danger" : "") + '" type="submit">' +
        (submitLabels[nextAction] || "Lưu") + '</button></div></form>'
    };
    render();
    return;
  }
  if (action === "return-refund-detail") {
    try {
      const detail = await apiGet(`/return-refunds/${encodeURIComponent(id)}`);
      state.modal = {
        type: "return-refund-detail",
        title: "Chi tiết đổi trả / hoàn tiền",
        subtitle: detail.requestCode || `Yêu cầu #${id}`,
        body: renderReturnRefundDetail(detail, apiContextPath)
      };
      render();
    } catch (error) {
      showToast(error.message || "Không thể tải yêu cầu đổi trả/hoàn tiền.", "error");
    }
    return;
  }


  if (action === "return-refund-action") {
    const nextAction = button.dataset.nextAction;
    const records = Array.isArray(state.data) ? state.data : [];
    const current = records.find(row => String(row.requestId ?? row.id ?? "") === String(id)) || {};
    const noteRequired = ["reject", "refund_failed"].includes(nextAction);
    let fields = "";
    let notice = "";
    let submitLabel = "Lưu cập nhật";
    let danger = false;
    if (nextAction === "approve") {
      fields = '<label class="form-field">Số tiền hoàn tiền được duyệt (VND)<input type="number" name="approvedAmount" min="0.01" step="0.01" required value="' +
        esc(current.requestedAmount == null ? "" : String(current.requestedAmount)) + '"></label>' +
        '<label class="form-field">Ghi chú quyết định<textarea name="note" maxlength="1000" rows="3" placeholder="Không bắt buộc"></textarea></label>';
      notice = '<div class="notice-panel"><strong>Kiểm tra số tiền trước khi duyệt.</strong><p>Số tiền được duyệt không được vượt quá điều kiện và bằng chứng theo chính sách đổi trả.</p></div>';
      submitLabel = "Duyệt yêu cầu";
    } else if (nextAction === "refund_complete") {
      const methods = [["ORIGINAL_PAYMENT", "Hoàn về phương thức ban đầu"], ["BANK_TRANSFER", "Chuyển khoản ngân hàng"], ["COD_MANUAL", "Hoàn COD thủ công"], ["CASH", "Tiền mặt"], ["OTHER", "Khác"]];
      fields = '<label class="form-field">Phương thức hoàn tiền<select name="refundMethod" required>' +
        methods.map(item => '<option value="' + item[0] + '"' + (item[0] === "ORIGINAL_PAYMENT" ? " selected" : "") + '>' + item[1] + '</option>').join("") +
        '</select></label><label class="form-field">Mã tham chiếu giao dịch<input name="refundReference" required minlength="3" maxlength="120" placeholder="Mã ngân hàng/cổng thanh toán"></label>' +
        '<label class="form-field">Ghi chú xác nhận<textarea name="note" required minlength="3" maxlength="1000" rows="3" placeholder="Nhập ghi chú xác nhận hoàn tiền"></textarea></label>' +
        '<label class="confirm-check"><input type="checkbox" name="externalConfirmed" required> Tôi đã hoàn tiền thành công bên ngoài hệ thống và đã đối chiếu mã tham chiếu.</label>';
      notice = '<div class="notice-panel"><strong>Chỉ ghi nhận sau khi giao dịch hoàn tiền thành công.</strong><p>Ứng dụng không tự chuyển tiền; thao tác chỉ lưu kết quả và mã tham chiếu đã kiểm tra.</p></div>';
      submitLabel = "Ghi nhận hoàn tiền";
    } else if (nextAction === "reject") {
      fields = '<label class="form-field">Lý do từ chối<textarea name="note" required minlength="3" maxlength="1000" rows="4" placeholder="Nêu rõ căn cứ từ chối"></textarea></label>';
      submitLabel = "Xác nhận từ chối";
      danger = true;
    } else if (nextAction === "refund_failed") {
      fields = '<label class="form-field">Lý do hoàn tiền thất bại<textarea name="note" required minlength="3" maxlength="1000" rows="4"></textarea></label>';
      submitLabel = "Ghi nhận thất bại";
      danger = true;
    } else if (nextAction === "review") {
      fields = '<label class="form-field">Ghi chú xem xét<textarea name="note" maxlength="1000" rows="3" placeholder="Không bắt buộc"></textarea></label>';
      submitLabel = "Bắt đầu xem xét";
    } else if (nextAction === "mark_received") {
      fields = '<label class="form-field">Ghi chú kiểm tra hàng trả<textarea name="note" maxlength="1000" rows="3" placeholder="Không bắt buộc"></textarea></label>';
      submitLabel = "Xác nhận đã nhận hàng";
    } else if (nextAction === "start_refund") {
      fields = '<label class="form-field">Ghi chú xử lý hoàn tiền<textarea name="note" maxlength="1000" rows="3" placeholder="Không bắt buộc"></textarea></label>';
      notice = '<div class="notice-panel"><strong>Đây mới là bước chuyển sang xử lý.</strong><p>Chưa ghi nhận tiền đã chuyển; chỉ xác nhận hoàn tất sau khi thực hiện giao dịch bên ngoài.</p></div>';
      submitLabel = "Bắt đầu xử lý hoàn tiền";
    } else if (nextAction === "retry_refund") {
      fields = '<label class="form-field">Ghi chú thử lại<textarea name="note" maxlength="1000" rows="3" placeholder="Không bắt buộc"></textarea></label>';
      submitLabel = "Đưa lại vào hàng đợi";
    }
    if (noteRequired && !fields) fields = '<label class="form-field">Ghi chú<textarea name="note" required minlength="3" maxlength="1000" rows="4"></textarea></label>';
    state.modal = {
      type: "return-refund-action",
      title: ({ review: "Bắt đầu xem xét", approve: "Duyệt yêu cầu đổi trả", reject: "Từ chối yêu cầu",
        mark_received: "Xác nhận hàng trả", start_refund: "Bắt đầu hoàn tiền",
        refund_complete: "Ghi nhận hoàn tiền thành công", refund_failed: "Ghi nhận hoàn tiền thất bại",
        retry_refund: "Thử xử lý hoàn tiền lại" })[nextAction] || "Xử lý yêu cầu đổi trả / hoàn tiền",
      subtitle: current.requestCode || current.orderCode || ("Yêu cầu #" + id),
      body: '<form class="stack-form" data-form="return-refund-action" data-id="' + esc(id) +
        '" data-next-action="' + esc(nextAction) + '">' + notice + fields +
        '<div class="modal-actions"><button class="button secondary" type="button" data-action="modal-close">Hủy</button>' +
        '<button class="button ' + (danger ? "danger" : "") + '" type="submit">' + submitLabel + '</button></div></form>'
    };
    render();
    return;
  }
  if (action === "edit-user") {
    try { await reloadDetail("users", id); }
    catch (error) { showToast(error.message || "Không thể tải hồ sơ.", "error"); }
    return;
  }
  if (action === "create-user") {
    state.modal = { type: "create-user", title: "Thêm người dùng", subtitle: "Tạo tài khoản mới cho marketplace", body: "" };
    render();
    document.querySelector('form[data-form="create-user"] input[name="fullName"]')?.focus();
    return;
  }
  if (action === "export-users") {
    const csvCell = value => {
      let text = String(value ?? "");
      // Prevent spreadsheet formula injection from values entered by users.
      if (/^[=+\-@\t\r]/.test(text)) text = `'${text}`;
      return `"${text.replace(/"/g, '""')}"`;
    };
    const records = [
      ["ID", "Họ tên", "Email", "Số điện thoại", "Vai trò", "Trạng thái", "Số đơn hàng", "Ngày tạo"],
      ...(state.data?.items || []).map(row => [row.id, row.fullName, row.email, row.phone, row.role, row.status, row.orderCount, row.createdAt])
    ];
    const csv = "\uFEFF" + records.map(record => record.map(csvCell).join(",")).join("\r");
    const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `nguoi-dung-trang-${state.data?.page || 1}.csv`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.setTimeout(() => URL.revokeObjectURL(url), 0);
    showToast("Đã xuất danh sách đang hiển thị thành CSV.");
    return;
  }
  if (action === "user-status") {
    const next = button.dataset.status;
    const text = next === "LOCKED" ? "Khóa tài khoản này?" : "Kích hoạt lại tài khoản này?";
    if (window.confirm(text)) await mutate(`/users/${encodeURIComponent(id)}/status`, { status: next }, "Đã cập nhật trạng thái tài khoản.");
    return;
  }
  if (action === "seller-status") {
    const next = button.dataset.status;
    if (window.confirm(`Chuyển trạng thái xưởng sang ${next}?`)) await mutate(`/sellers/${encodeURIComponent(id)}/status`, { status: next }, "Đã cập nhật trạng thái xưởng.");
    return;
  }
  if (action === "seller-verify") {
    const decision = button.dataset.decision || "APPROVE";
    const titles = {
      APPROVE: "Xác nhận duyệt xưởng",
      REJECT: "Xác nhận từ chối hồ sơ",
      REQUEST_INFO: "Yêu cầu xưởng bổ sung thông tin"
    };
    state.modal = {
      type: "seller-verify",
      title: titles[decision] || titles.APPROVE,
      subtitle: "Hồ sơ xưởng #" + id,
      id,
      decision,
      shopName: button.dataset.shopName || ""
    };
    render();
    document.querySelector('#modal-root textarea[name="reason"]')?.focus();
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

root.addEventListener("keydown", event => {
  if (event.key !== "Escape" || !state.sidebarCollapsed || state.modal) return;
  state.sidebarCollapsed = false;
  root.querySelector(".admin-shell")?.classList.remove("sidebar-collapsed");
  const menuToggle = root.querySelector(".menu-toggle");
  menuToggle?.setAttribute("aria-expanded", "false");
  menuToggle?.setAttribute("aria-label", "Mở menu điều hướng");
  menuToggle?.focus({ preventScroll: true });
});

root.addEventListener("submit", async event => {
  const jspForm = event.target.closest("form.support-filter-form, form.support-update-form, form.escalation-filter, form.escalation-form");
  if (jspForm && isJspRoute(state.route)) {
    event.preventDefault();
    if (!jspForm.reportValidity()) return;
    const method = (jspForm.getAttribute("method") || "get").toUpperCase();
    const actionUrl = new URL(jspForm.getAttribute("action") || location.href, location.href);
    const bodyParams = new URLSearchParams(new FormData(jspForm));
    if (method === "GET") {
      const nextUrl = actionUrl.pathname + (bodyParams.toString() ? `?${bodyParams.toString()}` : "");
      history.pushState({}, "", nextUrl);
      state.filters = Object.fromEntries(bodyParams);
      await loadData();
    } else {
      state.loading = true;
      render();
      try {
        state.data = await fetchJspFragment(actionUrl.pathname + actionUrl.search, state.route, {
          method: "POST",
          headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
          body: bodyParams
        });
      } catch (error) {
        showToast(error.message || "Không thể gửi yêu cầu.", "error");
      } finally {
        state.loading = false;
        render();
      }
    }
    return;
  }
  const form = event.target.closest("form[data-form]");
  if (!form) return;
  const values = Object.fromEntries(new FormData(form).entries());
  const id = form.dataset.id;

  if (form.dataset.form === "delivery-dispute-action") {
    event.preventDefault();
    if (!form.reportValidity()) return;
    const nextAction = form.dataset.nextAction;
    const note = String(values.note || "").trim();
    if (note.length < 3) {
      showToast("Ghi chú xử lý phải có ít nhất 3 ký tự.", "error");
      return;
    }
    const messages = {
      REVIEW: "Đã chuyển khiếu nại sang Đang xem xét.",
      RESOLVE: "Đã giải quyết khiếu nại và thông báo khách hàng.",
      REJECT: "Đã từ chối khiếu nại và thông báo khách hàng."
    };
    await mutate("/delivery-disputes/" + encodeURIComponent(id) + "/action",
      { action: nextAction, note }, messages[nextAction] || "Đã cập nhật khiếu nại.");
    return;
  }
  if (form.dataset.form === "business-queue-action") {
    event.preventDefault();
    if (!form.reportValidity()) return;
    const queue = form.dataset.queue;
    const nextAction = form.dataset.nextAction;
    const payload = {};
    let endpoint = "";
    let success = "Đã lưu ghi nhận nghiệp vụ.";
    if (queue === "shipments") {
      payload.carrier = String(values.carrier || "").trim();
      payload.trackingNumber = String(values.trackingNumber || "").trim();
      payload.status = String(values.status || "").trim().toUpperCase();
      payload.note = String(values.note || "").trim();
      if (!payload.carrier || (["SHIPPED", "IN_TRANSIT", "DELIVERED"].includes(payload.status) && !payload.trackingNumber)) {
        showToast("Cần nhập hãng vận chuyển và mã vận đơn cho trạng thái này.", "error");
        return;
      }
      endpoint = "/shipments/" + encodeURIComponent(id) + "/update";
      success = "Đã cập nhật vận đơn, lưu lịch sử và thông báo cho khách hàng.";
    } else if (queue === "custom-prints") {
      payload.action = nextAction;
      payload.note = String(values.note || "").trim();
      if (payload.note.length < 3) { showToast("Ghi chú phải có ít nhất 3 ký tự.", "error"); return; }
      endpoint = "/custom-prints/" + encodeURIComponent(id) + "/review";
      success = nextAction === "ESCALATED" ? "Đã chuyển yêu cầu in 3D sang theo dõi." : "Đã lưu ghi chú rà soát yêu cầu in 3D.";
    } else if (queue === "order-changes") {
      payload.action = nextAction;
      payload.note = String(values.note || "").trim();
      if (payload.note.length < 3) { showToast("Ghi chú phải có ít nhất 3 ký tự.", "error"); return; }
      endpoint = "/order-changes/" + encodeURIComponent(id) + "/action";
      success = nextAction === "REJECTED" ? "Đã từ chối yêu cầu thay đổi đơn." : "Đã ghi nhận rà soát yêu cầu thay đổi đơn.";
    } else if (queue === "reviews") {
      payload.action = nextAction;
      payload.reason = String(values.reason || "").trim();
      if (nextAction === "UNPUBLISH" && payload.reason.length < 3) { showToast("Lý do ẩn phải có ít nhất 3 ký tự.", "error"); return; }
      endpoint = "/reviews/" + encodeURIComponent(id) + "/moderation";
      success = nextAction === "UNPUBLISH" ? "Đã ẩn đánh giá và lưu lý do." : "Đã công khai đánh giá.";
    } else if (queue === "reconciliation" && (nextAction === "MANUAL_REFUND" || nextAction === "MANUAL_REFUND_FAILED")) {
      const failed = nextAction === "MANUAL_REFUND_FAILED";
      const method = String(values.method || "").trim().toUpperCase();
      const reference = String(values.reference || "").trim();
      const note = String(values.note || "").trim();
      const allowedMethods = ["ORIGINAL_PAYMENT", "BANK_TRANSFER", "COD_MANUAL", "CASH", "OTHER"];
      if (!allowedMethods.includes(method) || note.length < 3 || (!failed && reference.length < 3)) {
        showToast("Kiểm tra phương thức, ghi chú và mã tham chiếu hoàn tiền.", "error");
        return;
      }
      payload.method = method;
      payload.reference = reference;
      payload.note = note;
      delete payload.externalConfirmed;
      endpoint = "/reconciliation/" + encodeURIComponent(id) + (failed ? "/refund-failed" : "/refund");
      success = failed ? "Đã ghi nhận lần thử hoàn tiền thất bại; nghĩa vụ hoàn tiền vẫn mở." : "Đã ghi nhận hoàn tiền thủ công và cập nhật nhật ký đối soát.";
    } else if (queue === "reconciliation") {
      payload.outcome = nextAction;
      payload.note = String(values.note || "").trim();
      payload.reference = String(values.reference || "").trim();
      if (payload.note.length < 3) { showToast("Ghi chú phải có ít nhất 3 ký tự.", "error"); return; }
      endpoint = "/reconciliation/" + encodeURIComponent(id) + "/review";
      success = nextAction === "RECONCILED" ? "Đã ghi nhận kết quả đối soát." : "Đã ghi nhận giao dịch cần theo dõi.";
    }
    if (endpoint) await mutate(endpoint, payload, success);
    return;
  }
  if (form.dataset.form === "return-refund-action") {
    event.preventDefault();
    if (!form.reportValidity()) return;
    const nextAction = form.dataset.nextAction;
    const payload = { action: nextAction };
    payload.note = String(values.note || "").trim();
    if (nextAction === "approve") {
      const amount = Number(values.approvedAmount);
      if (!Number.isFinite(amount) || amount <= 0) { showToast("Số tiền được duyệt không hợp lệ.", "error"); return; }
      payload.approvedAmount = String(values.approvedAmount).trim();
    } else if (nextAction === "refund_complete") {
      const method = String(values.refundMethod || "").trim().toUpperCase();
      const reference = String(values.refundReference || "").trim();
      const note = String(values.note || "").trim();
      const allowed = ["ORIGINAL_PAYMENT", "BANK_TRANSFER", "COD_MANUAL", "CASH", "OTHER"];
      if (!allowed.includes(method) || reference.length < 3 || note.length < 3) {
        showToast("Kiểm tra phương thức, mã tham chiếu và ghi chú hoàn tiền.", "error");
        return;
      }
      payload.refundMethod = method;
      payload.refundReference = reference;
      payload.note = note;
      delete payload.externalConfirmed;
    } else if (nextAction === "reject" || nextAction === "refund_failed") {
      if (payload.note.length < 3) { showToast("Vui lòng ghi rõ lý do xử lý.", "error"); return; }
      if (nextAction === "refund_failed") payload.refundMethod = "OTHER";
    }
    const messages = {
      review: "Đã chuyển sang xem xét.",
      approve: "Đã lưu quyết định và số tiền được duyệt.",
      reject: "Đã từ chối yêu cầu.",
      mark_received: "Đã ghi nhận hàng trả.",
      start_refund: "Đã chuyển sang xử lý hoàn tiền.",
      refund_complete: "Đã ghi nhận kết quả hoàn tiền bên ngoài.",
      refund_failed: "Đã ghi nhận giao dịch hoàn tiền thất bại.",
      retry_refund: "Đã đưa lại vào xử lý hoàn tiền."
    };
    await mutate("/return-refunds/" + encodeURIComponent(id) + "/action", payload, messages[nextAction] || "Đã cập nhật yêu cầu.");
    return;
  }
  if (form.dataset.form === "finance-cod") {
    event.preventDefault();
    if (!form.reportValidity()) return;
    const carrierName = String(values.carrierName || "").trim();
    const carrierReference = String(values.carrierReference || "").trim();
    if (!carrierName || carrierName.length > 120 || carrierReference.length < 3 || carrierReference.length > 120) {
      showToast("Vui lòng nhập đơn vị vận chuyển và mã tham chiếu hợp lệ.", "error");
      return;
    }
    await mutate(`/finance/cod/${encodeURIComponent(id)}/receive`,
      { carrierName, carrierReference, note: String(values.note || "").trim() },
      "Đã xác nhận tiền COD về nền tảng và ghi nhận phân bổ tài chính.");
    return;
  }
  if (form.dataset.form === "finance-create-settlement") {
    event.preventDefault();
    if (!form.reportValidity()) return;
    const match = String(values.period || "").match(/^(\d{4})-(\d{2})$/);
    if (!match) { showToast("Tháng quyết toán không hợp lệ.", "error"); return; }
    const year = Number(match[1]);
    const month = Number(match[2]);
    const lastDay = new Date(year, month, 0).getDate();
    const periodStart = `${match[1]}-${match[2]}-01`;
    const periodEnd = `${match[1]}-${match[2]}-${String(lastDay).padStart(2, "0")}`;
    await mutate("/finance/settlements/create", { periodStart, periodEnd },
      "Đã tổng hợp và khóa kỳ quyết toán tháng.");
    return;
  }
  if (form.dataset.form === "finance-settlement-pay") {
    event.preventDefault();
    if (!form.reportValidity()) return;
    const payoutReference = String(values.payoutReference || "").trim();
    if (payoutReference.length < 3 || payoutReference.length > 120) {
      showToast("Mã tham chiếu chuyển khoản phải có từ 3 đến 120 ký tự.", "error");
      return;
    }
    await mutate(`/finance/settlements/${encodeURIComponent(id)}/paid`,
      { payoutReference, note: String(values.note || "").trim() },
      "Đã xác nhận chuyển tiền quyết toán cho xưởng.");
    return;
  }
  if (form.dataset.form === "seller-verify") {
    event.preventDefault();
    if (!form.reportValidity()) return;
    const reason = String(values.reason || "").trim();
    if (reason.length < 3 || reason.length > 1000) {
      showToast("Nội dung phải có từ 3 đến 1000 ký tự.", "error");
      return;
    }
    const decision = form.dataset.decision;
    const messages = {
      APPROVE: "Đã duyệt và kích hoạt xưởng.",
      REJECT: "Đã từ chối hồ sơ xưởng.",
      REQUEST_INFO: "Đã yêu cầu xưởng bổ sung thông tin."
    };
    await mutate(`/sellers/${encodeURIComponent(id)}/verification`,
      { action: decision, reason }, messages[decision] || "Đã lưu quyết định xác minh.");
    return;
  }
  if (form.dataset.form === "reject-product") {
    event.preventDefault();
    if (!form.reportValidity()) return;
    if (String(values.reason || "").trim().length < 8) { showToast("Lý do cần ít nhất 8 ký tự.", "error"); return; }
    await mutate(`/products/${encodeURIComponent(id)}/moderation`, { action: "reject", reason: values.reason }, "Đã từ chối sản phẩm và lưu lý do.");
    return;
  }
  if (form.dataset.form === "create-user") {
    event.preventDefault();
    if (!form.reportValidity()) return;
    if (String(values.password || "").length < 8) { showToast("Mật khẩu phải có ít nhất 8 ký tự.", "error"); return; }
    await mutate("/users", values, "Đã tạo tài khoản người dùng.");
    return;
  }
  if (form.dataset.form === "user-profile") {
    event.preventDefault();
    if (!form.reportValidity()) return;
    await mutate(`/users/${encodeURIComponent(id)}/profile`, values, "Đã lưu hồ sơ người dùng.", false);
    try { await reloadDetail("users", id); } catch (error) { showToast(error.message || "Không thể tải hồ sơ.", "error"); }
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

const initialJspFragment = root.querySelector("#support-page-content, #escalation-page-content");
if (initialJspFragment && isJspRoute(state.route)) {
  for (const styleEl of document.head.querySelectorAll("style")) {
    if (!styleEl.hasAttribute("data-admin-jsp-style")) {
      styleEl.setAttribute("data-admin-jsp-style", state.route);
    }
  }
  state.data = { html: initialJspFragment.outerHTML };
  render();
  Promise.all([
    apiGet("/session").catch(() => null),
    apiGet("/notification-summary").catch(() => ({}))
  ]).then(([actor, notifications]) => {
    if (actor) state.actor = actor;
    state.notifications = notifications || {};
    syncSidebarAndHeader(state.route);
    startAdminNotificationPolling();
  });
} else if (state.route === "not-found") {
  render();
} else {
  loadData().then(startAdminNotificationPolling);
}
