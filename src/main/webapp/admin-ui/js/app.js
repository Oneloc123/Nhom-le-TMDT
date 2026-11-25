import { apiContextPath, apiGet, apiPost, queryString } from "./api.js";
import { esc, icon } from "./components.js";
import { currentRoute, renderLayout, routeTitles } from "./layout.js?v=platform-finance-20261125-r1";
import { renderDashboard } from "./pages/dashboard.js";
import { initStatistics, renderStatistics } from "./pages/statistics.js";
import { renderEntityDetail, renderOrders, renderProducts, renderQuotes, renderSellers, renderUsers } from "./pages/management.js";
import { renderReturnRefundDetail, renderReturnRefunds } from "./pages/returns.js";
import { renderBusinessQueue, renderBusinessQueueDetail } from "./pages/queues.js";
import { renderFinance } from "./pages/finance.js";

const root = document.getElementById("admin-app");
const filters = Object.fromEntries(new URLSearchParams(window.location.search));
const state = {
  route: currentRoute(apiContextPath), filters, actor: null, data: null, loading: false,
  error: "", modal: null, pageData: null, sidebarCollapsed: false, notifications: {}
};

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
    case "statistics": return renderStatistics();
    case "finance": return renderFinance(state.data);
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

function render() {
  const safeRoute = routeTitles[state.route] ? state.route : "not-found";
  root.innerHTML = renderLayout(safeRoute, pageContent(), apiContextPath, state.sidebarCollapsed, state.actor, state.notifications);
  if (state.route === "statistics" && !state.loading && !state.error) initStatistics(apiContextPath);
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
    if (state.route === "statistics") {
      state.data = {};
      state.notifications = await apiGet("/notification-summary").catch(() => ({}));
      return;
    }
    if (state.route === "dashboard") endpoint = "/dashboard";
    else if (state.route === "finance") endpoint = "/finance";
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

root.addEventListener("click", async event => {
  const button = event.target.closest("[data-action]");
  if (!button) return;
  const action = button.dataset.action;
  const id = button.dataset.id;
  if (action === "toggle-sidebar") {
    state.sidebarCollapsed = !state.sidebarCollapsed;
    root.querySelector(".admin-shell")?.classList.toggle("sidebar-collapsed", state.sidebarCollapsed);
    const menuToggle = root.querySelector(".menu-toggle");
    menuToggle?.setAttribute("aria-expanded", String(state.sidebarCollapsed));
    menuToggle?.setAttribute("aria-label", `${state.sidebarCollapsed ? "Đóng" : "Mở"} menu điều hướng`);
    (state.sidebarCollapsed ? root.querySelector(".nav-item") : menuToggle)?.focus({ preventScroll: true });
    return;
  }
  if (action === "modal-close") { closeModal(); return; }
  if (action === "retry") { await loadData(); return; }
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
    let values = {};
    let endpoint = "";
    let success = "";
    if (queue === "shipments") {
      const current = (Array.isArray(state.data) ? state.data : []).find(row => String(row.id) === String(id)) || {};
      const carrier = window.prompt("Hãng vận chuyển:", current.carrier || "");
      if (carrier === null) return;
      const trackingNumber = window.prompt("Mã vận đơn:", current.trackingNumber || "");
      if (trackingNumber === null) return;
      const allowed = ["PREPARING","SHIPPED","IN_TRANSIT","DELIVERED","FAILED","RETURNED"];
      const status = window.prompt("Trạng thái: PREPARING, SHIPPED, IN_TRANSIT, DELIVERED, FAILED, RETURNED", current.status || "PREPARING");
      if (status === null) return;
      const normalizedStatus = status.trim().toUpperCase();
      if (!allowed.includes(normalizedStatus)) { showToast("Trạng thái vận đơn không hợp lệ.", "error"); return; }
      if (["SHIPPED","IN_TRANSIT","DELIVERED"].includes(normalizedStatus) && (!carrier.trim() || !trackingNumber.trim())) {
        showToast("Cần nhập hãng vận chuyển và mã vận đơn cho trạng thái này.", "error"); return;
      }
      const note = window.prompt("Ghi chú lịch sử vận chuyển:", "");
      if (note === null) return;
      if (!window.confirm("Lưu cập nhật vận đơn và gửi thông báo cho khách hàng?")) return;
      values = { carrier: carrier.trim(), trackingNumber: trackingNumber.trim(), status: normalizedStatus, note: note.trim() };
      endpoint = "/shipments/" + encodeURIComponent(id) + "/update";
      success = "Đã cập nhật vận đơn, lưu lịch sử và thông báo cho khách hàng.";
    } else if (queue === "custom-prints") {
      const note = window.prompt(nextAction === "ESCALATED" ? "Lý do cần theo dõi (bắt buộc):" : "Ghi chú rà soát (bắt buộc):", "");
      if (note === null) return;
      if (note.trim().length < 3) { showToast("Ghi chú phải có ít nhất 3 ký tự.", "error"); return; }
      values = { action: nextAction, note: note.trim() };
      endpoint = "/custom-prints/" + encodeURIComponent(id) + "/review";
      success = nextAction === "ESCALATED" ? "Đã chuyển yêu cầu in 3D sang theo dõi." : "Đã lưu ghi chú rà soát yêu cầu in 3D.";
    } else if (queue === "order-changes") {
      const note = window.prompt(nextAction === "REJECTED" ? "Lý do từ chối (bắt buộc):" : "Ghi chú rà soát (bắt buộc):", "");
      if (note === null) return;
      if (note.trim().length < 3) { showToast("Ghi chú phải có ít nhất 3 ký tự.", "error"); return; }
      if (nextAction === "REJECTED" && !window.confirm("Từ chối yêu cầu đang chờ xử lý? Thao tác này không tự thay đổi trạng thái đơn.")) return;
      values = { action: nextAction, note: note.trim() };
      endpoint = "/order-changes/" + encodeURIComponent(id) + "/action";
      success = nextAction === "REJECTED" ? "Đã từ chối yêu cầu thay đổi đơn." : "Đã ghi nhận rà soát yêu cầu thay đổi đơn.";
    } else if (queue === "reviews") {
      const reason = window.prompt(nextAction === "UNPUBLISH" ? "Lý do ẩn đánh giá (bắt buộc):" : "Ghi chú duyệt (không bắt buộc):", "");
      if (reason === null) return;
      if (nextAction === "UNPUBLISH" && reason.trim().length < 3) { showToast("Lý do ẩn phải có ít nhất 3 ký tự.", "error"); return; }
      if (!window.confirm(nextAction === "UNPUBLISH" ? "Ẩn đánh giá này khỏi khu vực công khai?" : "Công khai đánh giá này?")) return;
      values = { action: nextAction, reason: reason.trim() };
      endpoint = "/reviews/" + encodeURIComponent(id) + "/moderation";
      success = nextAction === "UNPUBLISH" ? "Đã ẩn đánh giá và lưu lý do." : "Đã công khai đánh giá.";
    } else if (queue === "reconciliation" && (nextAction === "MANUAL_REFUND" || nextAction === "MANUAL_REFUND_FAILED")) {
      const failed = nextAction === "MANUAL_REFUND_FAILED";
      if (!window.confirm(failed
          ? "Chỉ ghi nhận thất bại khi đã kiểm tra kết quả chuyển hoàn bên ngoài. Giao dịch sẽ tiếp tục ở hàng đợi hoàn tiền."
          : "Chỉ tiếp tục nếu bạn đã thực hiện hoàn tiền ngoài hệ thống và có xác nhận chuyển tiền thành công. Ứng dụng sẽ ghi nhận kết quả và cập nhật giao dịch sang REFUNDED. Tiếp tục?")) return;
      const method = window.prompt("Phương thức: ORIGINAL_PAYMENT, BANK_TRANSFER, COD_MANUAL, CASH hoặc OTHER", "BANK_TRANSFER");
      if (method === null) return;
      const allowedMethods = ["ORIGINAL_PAYMENT","BANK_TRANSFER","COD_MANUAL","CASH","OTHER"];
      if (!allowedMethods.includes(method.trim().toUpperCase())) { showToast("Phương thức hoàn tiền không hợp lệ.", "error"); return; }
      const reference = window.prompt("Mã tham chiếu ngân hàng/cổng thanh toán (nếu có):", "");
      if (reference === null || (!failed && reference.trim().length < 3)) { showToast("Cần mã tham chiếu hợp lệ cho thao tác này.", "error"); return; }
      const note = window.prompt(failed ? "Lý do hoàn tiền thất bại (bắt buộc):" : "Ghi chú hoàn tiền (bắt buộc):", "");
      if (note === null || note.trim().length < 3) { showToast(failed ? "Cần lý do thất bại hợp lệ." : "Cần ghi chú xác nhận hoàn tiền.", "error"); return; }
      values = { method: method.trim().toUpperCase(), reference: reference.trim(), note: note.trim() };
      endpoint = "/reconciliation/" + encodeURIComponent(id) + (failed ? "/refund-failed" : "/refund");
      success = failed ? "Đã ghi nhận lần thử hoàn tiền thất bại; nghĩa vụ hoàn tiền vẫn mở." : "Đã ghi nhận hoàn tiền thủ công và cập nhật nhật ký đối soát.";
    } else if (queue === "reconciliation") {
      const note = window.prompt(nextAction === "FOLLOW_UP" ? "Lý do cần theo dõi tiếp (bắt buộc):" : "Ghi chú đối soát (bắt buộc):", "");
      if (note === null) return;
      if (note.trim().length < 3) { showToast("Ghi chú phải có ít nhất 3 ký tự.", "error"); return; }
      const reference = window.prompt("Mã tham chiếu ngân hàng/cổng thanh toán (nếu có):", "");
      if (reference === null) return;
      if (!window.confirm(nextAction === "RECONCILED" ? "Đóng hàng đợi bằng cách ghi nhận đã đối soát? Trạng thái thanh toán gốc sẽ không đổi." : "Ghi nhận giao dịch cần theo dõi tiếp? Trạng thái thanh toán gốc sẽ không đổi.")) return;
      values = { outcome: nextAction, note: note.trim(), reference: reference.trim() };
      endpoint = "/reconciliation/" + encodeURIComponent(id) + "/review";
      success = nextAction === "RECONCILED" ? "Đã ghi nhận kết quả đối soát." : "Đã ghi nhận giao dịch cần theo dõi.";
    }
    if (endpoint) await mutate(endpoint, values, success);
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
    const current = (Array.isArray(state.data) ? state.data : []).find(row => String(row.requestId) === String(id));
    let values = { action: nextAction };
    if (nextAction === "review") {
      values.note = window.prompt("Ghi chú xem xét (không bắt buộc):", "") ?? "";
    } else if (nextAction === "approve") {
      const defaultAmount = current?.requestedAmount == null ? "" : String(current.requestedAmount);
      const amount = window.prompt("Số tiền hoàn tiền được duyệt (VND):", defaultAmount);
      if (amount === null) return;
      if (!amount.trim() || !Number.isFinite(Number(amount)) || Number(amount) <= 0) {
        showToast("Số tiền được duyệt không hợp lệ.", "error");
        return;
      }
      values.approvedAmount = amount.trim();
      values.note = window.prompt("Ghi chú quyết định (không bắt buộc):", "") ?? "";
    } else if (nextAction === "reject") {
      const note = window.prompt("Lý do từ chối (bắt buộc):", "");
      if (note === null) return;
      if (note.trim().length < 3) {
        showToast("Vui lòng nhập lý do từ chối cụ thể.", "error");
        return;
      }
      values.note = note.trim();
    } else if (nextAction === "mark_received") {
      values.note = window.prompt("Ghi chú kiểm tra hàng trả (không bắt buộc):", "") ?? "";
    } else if (nextAction === "start_refund") {
      if (!window.confirm("Chuyển yêu cầu sang bước thực hiện hoàn tiền? Hãy chỉ thao tác khi đã kiểm tra điều kiện hoàn trả.")) return;
      values.note = window.prompt("Ghi chú hoàn tiền (không bắt buộc):", "") ?? "";
    } else if (nextAction === "refund_complete") {
      if (!window.confirm("Chỉ xác nhận sau khi đã thực hiện chuyển tiền/hoàn tiền thành công trên ngân hàng hoặc cổng thanh toán. Tiếp tục?")) return;
      const method = window.prompt("Phương thức: ORIGINAL_PAYMENT, BANK_TRANSFER, COD_MANUAL, CASH hoặc OTHER", "ORIGINAL_PAYMENT");
      if (method === null) return;
      const allowed = ["ORIGINAL_PAYMENT", "BANK_TRANSFER", "COD_MANUAL", "CASH", "OTHER"];
      if (!allowed.includes(method.trim())) {
        showToast("Phương thức hoàn tiền không hợp lệ.", "error");
        return;
      }
      const reference = window.prompt("Mã tham chiếu giao dịch hoàn tiền (bắt buộc):", "");
      if (reference === null || !reference.trim()) {
        showToast("Cần mã tham chiếu để ghi nhận kết quả hoàn tiền.", "error");
        return;
      }
      values.refundMethod = method.trim();
      values.refundReference = reference.trim();
      const refundNote = window.prompt("Ghi chú xác nhận đã chuyển tiền thành công (bắt buộc):", "");
      if (refundNote === null || refundNote.trim().length < 3) {
        showToast("Cần ghi chú xác nhận hoàn tiền từ 3 ký tự.", "error");
        return;
      }
      values.note = refundNote.trim();
    } else if (nextAction === "refund_failed") {
      const note = window.prompt("Lý do hoàn tiền thất bại (bắt buộc):", "");
      if (note === null) return;
      if (note.trim().length < 3) {
        showToast("Vui lòng ghi rõ lý do hoàn tiền thất bại.", "error");
        return;
      }
      values.note = note.trim();
      values.refundMethod = "OTHER";
    } else if (nextAction === "retry_refund") {
      if (!window.confirm("Đưa yêu cầu trở lại hàng đợi hoàn tiền?")) return;
      values.note = window.prompt("Ghi chú thử lại (không bắt buộc):", "") ?? "";
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
    await mutate(`/return-refunds/${encodeURIComponent(id)}/action`, values, messages[nextAction] || "Đã cập nhật yêu cầu.");
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
    const csv = "\uFEFF" + records.map(record => record.map(csvCell).join(",")).join("\r\n");
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
  const form = event.target.closest("form[data-form]");
  if (!form) return;
  const values = Object.fromEntries(new FormData(form).entries());
  const id = form.dataset.id;
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
    const match = String(values.period || "").match(/^(\\d{4})-(\\d{2})$/);
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

if (state.route === "not-found") render();
else loadData().then(startAdminNotificationPolling);
