const root = document.getElementById("workshop-app");
const contextPath = root?.dataset.contextPath || "";
const csrfToken = document.querySelector('meta[name="csrf-token"]')?.content || "";
const routes = {
  dashboard: { path: "/dashboard", title: "Tổng quan" },
  profile: { path: "/profile", title: "Hồ sơ & năng lực" },
  products: { path: "/products", title: "Sản phẩm & bảng giá" },
  orders: { path: "/orders", title: "Đơn hàng & tiến độ" },
  quotes: { path: "/quotes", title: "Yêu cầu báo giá" },
  messages: { path: "/messages", title: "Tin nhắn theo đơn hàng" },
  changeRequests: { path: "/change-requests", title: "Yêu cầu thay đổi" },
  reviews: { path: "/reviews", title: "Đánh giá" }
};
const icons = {
  dashboard: '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><rect x="3.5" y="3.5" width="7" height="7" rx="1.5"/><rect x="13.5" y="3.5" width="7" height="7" rx="1.5"/><rect x="3.5" y="13.5" width="7" height="7" rx="1.5"/><rect x="13.5" y="13.5" width="7" height="7" rx="1.5"/></svg>',
  profile: '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><circle cx="12" cy="8" r="3.4"/><path d="M5.2 20c.6-3.5 3-5.3 6.8-5.3s6.2 1.8 6.8 5.3"/><circle cx="12" cy="12" r="9"/></svg>',
  products: '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="m12 3 8 4.5v9L12 21l-8-4.5v-9L12 3Z"/><path d="m4.5 7.8 7.5 4.4 7.5-4.4M12 12.2V21"/></svg>',
  orders: '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M4 7.5h16v12H4zM8 7.5V5h8v2.5M4 12h16M9 12v2h6v-2"/></svg>',
  quotes: '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M20 11.5a7.5 7.5 0 0 1-8 7.5 8.6 8.6 0 0 1-3.7-.8L4 20l1.2-3.8A7 7 0 0 1 4 11.5 7.5 7.5 0 0 1 12 4a7.5 7.5 0 0 1 8 7.5Z"/><path d="M8.5 11.5h.01M12 11.5h.01M15.5 11.5h.01"/></svg>',
  messages: '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M20 11.5a7.5 7.5 0 0 1-8 7.5 8.6 8.6 0 0 1-3.7-.8L4 20l1.2-3.8A7 7 0 0 1 4 11.5 7.5 7.5 0 0 1 12 4a7.5 7.5 0 0 1 8 7.5Z"/><path d="M8.5 11.5h.01M12 11.5h.01M15.5 11.5h.01"/></svg>',
  changeRequests: '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M4 7h13l-3-3m6 13H7l3 3"/><path d="M17 4l3 3-3 3M7 14l-3 3 3 3"/></svg>',
  reviews: '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="m12 3 2.7 5.5 6.1.9-4.4 4.3 1 6.1-5.4-2.9-5.4 2.9 1-6.1-4.4-4.3 6.1-.9L12 3Z"/></svg>',
  active: '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M4 7h16M4 12h16M4 17h10"/><circle cx="18" cy="17" r="2.5"/></svg>',
  completed: '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><circle cx="12" cy="12" r="8.5"/><path d="m8.3 12.2 2.4 2.4 5-5.2"/></svg>',
  cancelled: '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><circle cx="12" cy="12" r="8.5"/><path d="m9 9 6 6m0-6-6 6"/></svg>'
};
const routePath = window.location.pathname.replace(/\/$/, "") || "/workshop/dashboard";
const page = Object.keys(routes).find(key => routePath.endsWith(routes[key].path)) || "dashboard";
const urlParams = new URLSearchParams(window.location.search);
const state = { page, shopId: "", shops: [], data: null, actor: null, loading: true, error: "", modal: null, customPrintRequests: [], priceProfiles: [], messageThread: [], notifications: {} };

function esc(value) {
  return String(value ?? "").replace(/[&<>"']/g, character => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
  })[character]);
}
function number(value) { return new Intl.NumberFormat("vi-VN").format(Number(value) || 0); }
function money(value) { return value == null ? "—" : `${new Intl.NumberFormat("vi-VN").format(Number(value) || 0)} ₫`; }
function date(value) {
  if (!value) return "—";
  const parsed = new Date(String(value).replace(" ", "T"));
  return Number.isNaN(parsed.getTime()) ? esc(value) : new Intl.DateTimeFormat("vi-VN", { dateStyle: "medium", timeStyle: "short" }).format(parsed);
}
function pill(text, tone = "") { return `<span class="pill ${tone}">${esc(text || "Chưa có trạng thái")}</span>`; }
function statusTone(text) {
  const value = String(text || "").toLowerCase();
  if (value.includes("hoàn thành") || value.includes("verified") || value.includes("đã báo giá") || value === "approved") return "good";
  if (value.includes("hủy") || value.includes("đóng") || value.includes("rejected") || value.includes("blocked")) return "bad";
  if (value.includes("chờ") || value.includes("pending") || value.includes("thanh toán")) return "warn";
  return "";
}
function shopQuery() { return `shopId=${encodeURIComponent(state.shopId)}`; }
function workshopUrl(route) { return `${contextPath}/workshop${routes[route].path}?${shopQuery()}`; }
function apiUrl(path, includeShop = true) {
  const query = includeShop && state.shopId ? `?${shopQuery()}` : "";
  return `${contextPath}/workshop/api${path}${query}`;
}
async function apiGet(path, includeShop = true) {
  const response = await fetch(apiUrl(path, includeShop), {
    method: "GET", credentials: "same-origin", cache: "no-store", headers: { Accept: "application/json" }
  });
  return readResponse(response);
}
async function apiPost(path, values = {}) {
  const body = new URLSearchParams();
  for (const [key, value] of Object.entries(values)) if (value != null) body.set(key, String(value));
  body.set("shopId", state.shopId);
  const response = await fetch(apiUrl(path, false), {
    method: "POST", credentials: "same-origin", cache: "no-store",
    headers: { Accept: "application/json", "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8", "X-CSRF-Token": csrfToken },
    body
  });
  return readResponse(response);
}
async function readResponse(response) {
  if (response.status === 401) { window.location.assign(`${contextPath}/login`); throw new Error("Phiên đăng nhập đã hết hạn."); }
  let payload;
  try { payload = await response.json(); } catch { throw new Error("Máy chủ trả về phản hồi không hợp lệ."); }
  if (!response.ok) throw new Error(payload.error || payload.message || "Yêu cầu chưa hoàn tất.");
  return payload.data;
}
function formValues(form) { return Object.fromEntries(new FormData(form).entries()); }
function toast(message, isError = false) {
  const host = document.getElementById("workshop-toast");
  if (!host) return;
  host.innerHTML = `<div class="toast ${isError ? "error" : ""}" role="status">${esc(message)}</div>`;
  window.setTimeout(() => { if (host.isConnected) host.innerHTML = ""; }, 3600);
}
function metric(label, value, detail, icon) {
  return `<article class="metric"><span class="metric-mark">${icon}</span><div class="metric-label">${esc(label)}</div><div class="metric-value">${number(value)}</div><div class="metric-detail">${esc(detail)}</div></article>`;
}
function heading(eyebrow, title, description, action = "") {
  return `<div class="heading"><div><div class="eyebrow">${esc(eyebrow)}</div><h1>${esc(title)}</h1><p>${esc(description)}</p></div>${action}</div>`;
}
function empty(text) { return `<div class="empty"><strong>${esc(text)}</strong><span>Khi có dữ liệu phù hợp với xưởng, danh sách sẽ được cập nhật tại đây.</span></div>`; }
function panel(title, description, content) {
  return `<section class="panel"><header class="panel-head"><div><h2>${esc(title)}</h2><p>${esc(description)}</p></div></header>${content}</section>`;
}
function statusBanner(shop) {
  if (shop?.verificationStatus !== "VERIFIED") {
    return `<div class="status-banner">Xưởng đang chờ Admin xác minh. Bạn vẫn có thể hoàn thiện hồ sơ và chuẩn bị bảng giá; khách hàng chỉ thấy xưởng sau khi được xác minh.</div>`;
  }
  if (shop?.status !== "ACTIVE") {
    return `<div class="status-banner">Xưởng đang ở trạng thái ${esc(shop?.status || "không hoạt động")}. Liên hệ Admin để được hỗ trợ mở lại.</div>`;
  }
  return `<div class="status-banner verified">Xưởng đã xác minh và đang hoạt động trên marketplace.</div>`;
}
function layout(content) {
  const activeShop = state.shops.find(shop => String(shop.id) === String(state.shopId)) || {};
  const navLink = ([key, item]) => {
      const count = Number(state.notifications?.[({ orders: "pendingOrders", quotes: "pendingQuotes", messages: "unreadMessages", changeRequests: "pendingChanges" })[key]] || 0)
          + (key === "quotes" ? Number(state.notifications?.pendingCustomPrints || 0) : 0);
      const badge = count > 0 ? `<span class="nav-alert" aria-label="${number(count)} mục cần xử lý">${count > 99 ? "99+" : number(count)}</span>` : "";
      return `<a class="nav-link ${state.page === key ? "active" : ""}" href="${esc(workshopUrl(key))}"><span class="nav-icon">${icons[key]}</span><span class="nav-label-text">${esc(item.title)}</span>${badge}</a>`;
  };
  const nav = [
    navLink(["dashboard", routes.dashboard]),
    '<div class="nav-title">QUẢN LÝ HOẠT ĐỘNG</div>',
    ...["products", "orders", "quotes", "messages", "changeRequests", "reviews"].map(key => navLink([key, routes[key]])),
    '<div class="nav-title">CẤU HÌNH XƯỞNG</div>',
    navLink(["profile", routes.profile])
  ].join("");
  const selector = state.shops.length > 1 ? `<div class="shop-switch"><label for="shop-switcher">Xưởng đang quản lý</label><select id="shop-switcher">${state.shops.map(shop => `<option value="${esc(shop.id)}" ${String(shop.id) === String(state.shopId) ? "selected" : ""}>${esc(shop.name)}</option>`).join("")}</select></div>` : `<div class="shop-switch"><label>Xưởng của bạn</label><strong>${esc(activeShop.name || "Xưởng in")}</strong></div>`;
  const initials = (state.actor?.name || activeShop.name || "X").trim().slice(0, 1).toUpperCase();
  return `<div class="shell"><aside class="sidebar"><a class="brand" href="${esc(workshopUrl("dashboard"))}"><span class="brand-mark">P</span> PrintKraft</a>${selector}<nav class="nav-list">${nav}</nav><div class="sidebar-bottom">Quản lý dữ liệu thuộc xưởng của bạn.<form method="post" action="${contextPath}/logout" class="logout-form"><input type="hidden" name="csrfToken" value="${esc(document.querySelector('meta[name=\"csrf-token\"]')?.content || "")}"><button type="submit">Đăng xuất</button></form></div></aside><main class="main"><header class="topbar"><div class="topbar-title">${esc(routes[state.page].title)}</div><div class="topbar-user"><span>${esc(state.actor?.name || "Chủ xưởng")}</span><span class="avatar">${esc(initials)}</span></div></header><section class="content">${state.data?.shop ? statusBanner(state.data.shop) : ""}${content}</section></main></div>${state.modal ? modalMarkup() : ""}`;
}
function errorView() {
  return `<section class="error-screen"><div class="eyebrow">KHÔNG THỂ TẢI WORKSHOP PORTAL</div><h1>Dữ liệu chưa sẵn sàng</h1><p>${esc(state.error)}</p><button class="primary" data-action="retry">Thử tải lại</button></section>`;
}
function loadingView() { return `<main class="boot-state" role="status">Đang tải dữ liệu xưởng…</main>`; }
function render() {
  if (state.loading && !state.data) { root.innerHTML = loadingView(); return; }
  if (state.error) { root.innerHTML = errorView(); return; }
  root.innerHTML = layout(renderPage());
}

function renderPage() {
  const data = state.data || {};
  if (state.page === "dashboard") return dashboard(data);
  if (state.page === "profile") return profile(data);
  if (state.page === "products") return offers(data) + customPrintPricingPanel(state.priceProfiles);
  if (state.page === "orders") return orders(data);
  if (state.page === "quotes") return quotes(data) + customPrintQuotes(state.customPrintRequests);
  if (state.page === "messages") return messageCenter(data);
  if (state.page === "changeRequests") return changeRequests(data);
  if (state.page === "reviews") return reviews(data);
  return empty("Không tìm thấy trang.");
}

function threadUrl(orderId) {
  return `${workshopUrl("messages")}&orderId=${encodeURIComponent(orderId)}`;
}
function messageCenter(threads) {
  const orderId = urlParams.get("orderId");
  const selected = (threads || []).find(thread => String(thread.orderId) === String(orderId));
  const threadLinks = (threads || []).map(thread => {
    const active = String(thread.orderId) === String(orderId);
    return `<a class="message-thread ${active ? "active" : ""}" href="${esc(threadUrl(thread.orderId))}">
      <div class="message-thread-head"><strong>${esc(thread.orderCode)}</strong>${Number(thread.unread) > 0 ? pill(`${number(thread.unread)} mới`, "warn") : ""}</div>
      <span class="message-thread-customer">${esc(thread.customerName || "Khách hàng")}</span>
      <span class="message-thread-preview">${esc(thread.lastMessage || "Chưa có tin nhắn")}</span>
      <small>${date(thread.lastMessageAt)}</small>
    </a>`;
  }).join("");
  const bubbles = (state.messageThread || []).map(message => `<div class="message-row ${message.mine ? "mine" : ""}">
    <div class="message-bubble">
      <div class="message-meta">${esc(message.mine ? "Bạn" : message.sender || "Khách hàng")} · ${date(message.createdAt)}</div>
      <div class="message-body">${esc(message.body).replace(/\n/g, "<br>")}</div>
    </div>
  </div>`).join("");
  const conversation = orderId
    ? `<section class="panel conversation-panel">
        <header class="panel-head"><div><h2>${esc(selected?.orderCode || `Đơn hàng #${orderId}`)}</h2><p>${esc(selected?.customerName || "Trao đổi riêng theo đơn hàng")}</p></div><a class="back-link" href="${esc(workshopUrl("messages"))}">Tất cả hội thoại</a></header>
        <div class="message-history">${bubbles || empty("Chưa có tin nhắn. Bạn có thể gửi lời chào đầu tiên.")}</div>
        <form class="message-compose" data-form="message" data-order-id="${esc(orderId)}">
          <label class="field"><span>Tin nhắn</span><textarea name="message" rows="3" maxlength="2000" required placeholder="Trao đổi về tiến độ, thông số hoặc giao hàng..."></textarea></label>
          <div class="form-actions"><small class="muted">Tối đa 2.000 ký tự · Không gửi mật khẩu, OTP hoặc thông tin thẻ.</small><button class="primary" type="submit">Gửi tin nhắn</button></div>
        </form>
      </section>`
    : `<section class="panel conversation-panel"><div class="empty"><strong>Chọn một hội thoại</strong><span>Chọn đơn hàng ở danh sách bên trái để xem lịch sử và phản hồi khách hàng.</span></div></section>`;
  return `${heading("CUSTOMER COMMUNICATION", "Tin nhắn theo đơn hàng", "Trao đổi riêng tư giữa xưởng và khách hàng; mỗi hội thoại được gắn với một đơn hàng.") }
    <div class="message-layout">
      <section class="panel message-thread-panel"><header class="panel-head"><div><h2>Hội thoại</h2><p>${number((threads || []).length)} đơn hàng</p></div></header>
        <div class="message-thread-list">${threadLinks || empty("Chưa có đơn hàng để trao đổi.")}</div>
      </section>
      ${conversation}
    </div>`;
}
function changeRequestProposal(payload) {
  try {
    const parsed = JSON.parse(payload || "{}");
    return parsed.proposedValue == null || parsed.proposedValue === "" ? "Không có giá trị đề xuất" : String(parsed.proposedValue);
  } catch {
    return payload || "Không có giá trị đề xuất";
  }
}
function changeRequestType(type) {
  return ({
    CHANGE_MATERIAL: "Đổi vật liệu", CHANGE_COLOR: "Đổi màu", CHANGE_QUANTITY: "Đổi số lượng",
    REPLACE_DESIGN: "Thay file thiết kế", CHANGE_ADDRESS: "Đổi địa chỉ giao",
    CANCEL_ITEM: "Hủy một sản phẩm", CANCEL_ORDER: "Hủy toàn bộ đơn", OTHER: "Yêu cầu khác"
  })[type] || type || "Yêu cầu khác";
}
function changeRequests(items) {
  const selectedStatus = urlParams.get("status") || "ALL";
  const statuses = ["PENDING", "ACCEPTED", "REJECTED", "APPLIED", "WITHDRAWN"];
  const toolbar = `<form class="toolbar" data-filter="change-requests">
    <label class="field">Trạng thái<select name="status"><option value="ALL" ${selectedStatus === "ALL" ? "selected" : ""}>Tất cả</option>${statuses.map(status => `<option value="${status}" ${selectedStatus === status ? "selected" : ""}>${esc(status)}</option>`).join("")}</select></label>
    <button class="secondary" type="submit">Lọc</button>
  </form>`;
  const cards = (items || []).map(item => {
    const proposal = changeRequestProposal(item.payload);
    const response = item.response ? `<p><strong>Phản hồi của xưởng:</strong> ${esc(item.response)}</p>` : "";
    const amounts = item.priceDelta != null || item.cancelFee != null
      ? `<p><strong>Điều chỉnh giá:</strong> ${money(item.priceDelta)} · <strong>Phí hủy:</strong> ${money(item.cancelFee)}</p>`
      : "";
    const form = item.status === "PENDING" ? `<form class="change-request-response" data-form="change-request" data-id="${esc(item.id)}">
      <div class="form-grid">
        <label>Điều chỉnh giá (VND, có thể âm)<input name="priceDelta" type="number" step="0.01" min="-1000000000" max="1000000000" value="0"></label>
        <label>Phí hủy (VND)<input name="cancelFee" type="number" step="0.01" min="0" max="1000000000" value="0"></label>
        <label class="span-2">Phản hồi cho khách<textarea name="note" maxlength="1000" rows="2" placeholder="Nêu rõ điều kiện, chi phí hoặc lý do từ chối"></textarea></label>
      </div>
      <div class="form-actions"><button class="primary small-button" type="submit" value="ACCEPTED">Chấp nhận yêu cầu</button><button class="danger small-button" type="submit" value="REJECTED">Từ chối</button></div>
    </form>` : "";
    return `<article class="order-card change-request-card">
      <div class="order-card-head"><div><h3>${changeRequestType(item.type)} · ${esc(item.orderCode)}</h3><p>Yêu cầu #${esc(item.id)} · ${esc(item.customer || "Khách hàng")}${item.itemId ? ` · Sản phẩm #${esc(item.itemId)}` : ""}</p></div><div>${pill(item.status, statusTone(item.status))}</div></div>
      <p><strong>Lý do:</strong> ${esc(item.reason || "—")}</p>
      <p><strong>Đề xuất:</strong> ${esc(proposal)}</p>
      <p class="muted">Tạo lúc ${date(item.createdAt)}</p>
      ${response}${amounts}${form}
    </article>`;
  }).join("");
  return `${heading("ORDER SERVICE", "Yêu cầu thay đổi", "Tiếp nhận, phản hồi và theo dõi các yêu cầu thay đổi hoặc hủy đơn từ khách hàng.")}${toolbar}
    ${cards || empty("Không có yêu cầu thay đổi phù hợp.")}`;
}

function dashboard(data) {
  const m = data.metrics || {};
  const orderRows = (data.recentOrders || []).map(row => `<tr><td><strong>${esc(row.code)}</strong><span class="subtext">${esc(row.productName)} · SL ${number(row.quantity)}</span></td><td>${pill(row.orderStatus, statusTone(row.orderStatus))}</td><td>${esc(row.latestStage || row.itemStatus || "Chưa bắt đầu")}</td><td>${date(row.placedAt)}</td></tr>`).join("");
  const quoteRows = (data.recentQuotes || []).map(row => `<tr><td><strong>#${esc(row.id)}</strong><span class="subtext">${esc(row.productName || "Yêu cầu dịch vụ in")}</span></td><td>${esc(row.companyName)}</td><td>${number(row.quantity)}</td><td>${pill(row.status, statusTone(row.status))}</td></tr>`).join("");
  return `${heading("WORKSHOP OVERVIEW", "Tổng quan xưởng", "Đơn hàng, báo giá và sản phẩm được giới hạn theo xưởng đang chọn.")}
    <div class="metric-grid">${metric("Báo giá mới", m.newQuotes, "Yêu cầu đang chờ phản hồi", icons.quotes)}${metric("Đơn đang xử lý", m.activeOrders, "Đơn đã xác nhận / đang giao", icons.active)}${metric("Đơn hoàn thành", m.completedOrders, "Theo trạng thái đơn hiện tại", icons.completed)}${metric("Đơn đã hủy", m.cancelledOrders, "Đơn có sản phẩm của xưởng này", icons.cancelled)}${metric("Sản phẩm đang bán", m.publishedProducts, `${number(m.pendingReviewProducts)} chờ kiểm duyệt`, icons.products)}</div>
    <div class="dashboard-grid">${panel("Đơn hàng gần đây", "Chỉ hiển thị các dòng hàng của xưởng này.", orderRows ? `<div class="table-wrap"><table class="data-table"><thead><tr><th>Đơn / sản phẩm</th><th>Trạng thái đơn</th><th>Tiến độ xưởng</th><th>Ngày đặt</th></tr></thead><tbody>${orderRows}</tbody></table></div>` : empty("Chưa có đơn hàng thuộc xưởng."))}
    ${panel("Báo giá gần đây", "Yêu cầu khách hàng đã chỉ định xưởng này.", quoteRows ? `<div class="table-wrap"><table class="data-table"><thead><tr><th>Mã / sản phẩm</th><th>Doanh nghiệp</th><th>Số lượng</th><th>Trạng thái</th></tr></thead><tbody>${quoteRows}</tbody></table></div>` : empty("Chưa có yêu cầu báo giá."))}</div>
    <div class="lower-grid">${panel("Tình trạng cửa hàng", "Trạng thái xác minh do Admin quản lý.", `<div class="detail-grid"><div class="detail-cell"><b>XÁC MINH</b>${pill(data.shop?.verificationStatus, statusTone(data.shop?.verificationStatus))}</div><div class="detail-cell"><b>HOẠT ĐỘNG</b>${pill(data.shop?.status, statusTone(data.shop?.status))}</div><div class="detail-cell"><b>ĐÁNH GIÁ HIỆN TẠI</b>${data.shop?.rating == null ? "Chưa có dữ liệu" : `${esc(data.shop.rating)} / 5`}</div><div class="detail-cell"><b>TỔNG CHÀO BÁN</b>${number(m.offers)}</div></div>`)}${panel("Quy tắc dữ liệu", "Các con số tài chính chưa được tính vì chưa có quy tắc doanh thu theo xưởng.", `<p class="muted">Dashboard không hiển thị doanh thu ước tính. Giá sản phẩm lấy từ chào bán của xưởng; trạng thái thanh toán do luồng thanh toán hiện có quản lý.</p>`)}</div>`;
}
function profile(data) {
  const p = data || {};
  return `${heading("WORKSHOP PROFILE", "Hồ sơ & năng lực", "Cập nhật thông tin khách hàng dùng để tìm hiểu và lựa chọn xưởng.")}${panel("Thông tin xưởng", "Trạng thái xác minh, điểm đánh giá và chủ tài khoản chỉ Admin/hệ thống quản lý.", `<form class="form-grid" data-form="profile">
      <section class="profile-group span-2"><h3>Thông tin & liên hệ</h3><div class="form-grid">
        ${field("Tên xưởng", "name", p.name, 180, true)}${field("Điện thoại liên hệ", "contactPhone", p.contactPhone, 25)}${field("Email liên hệ", "contactEmail", p.contactEmail, 190, false, "email")}${field("URL ảnh đại diện (HTTP/HTTPS)", "logoUrl", p.logoUrl, 500, false, "url")}
      </div></section>
      <section class="profile-group span-2"><h3>Địa điểm hoạt động</h3><div class="form-grid">
        ${field("Địa chỉ hoạt động", "addressLine", p.addressLine, 255)}${field("Tỉnh / thành phố", "province", p.province, 100)}${field("Quận / huyện", "district", p.district, 100)}${field("Thời gian hoạt động", "operatingHours", p.operatingHours, 255)}${field("Vĩ độ (để tính khoảng cách)", "latitude", p.latitude, 20, false, "number", "min=-90 max=90 step=0.0000001")}${field("Kinh độ (để tính khoảng cách)", "longitude", p.longitude, 20, false, "number", "min=-180 max=180 step=0.0000001")}
      </div></section>
      <section class="profile-group span-2"><h3>Năng lực & sản xuất</h3><div class="form-grid">
        ${textarea("Mô tả xưởng", "description", p.description, 4000)}${textarea("Công nghệ, máy in, vật liệu, màu và hoàn thiện hỗ trợ", "capabilities", p.capabilities, 4000)}${field("Định dạng tệp nhận", "acceptedFileFormats", p.acceptedFileFormats, 255)}${field("Kích thước in tối đa (X × Y × Z mm)", "maxBuildDimensions", p.maxBuildDimensions, 120)}${field("Thời gian xử lý dự kiến (ngày)", "estimatedLeadDays", p.estimatedLeadDays, 3, false, "number", "min=0 max=365")}${field("Số lượng in 3D tối đa mỗi yêu cầu", "maximumCustomPrintQuantity", p.maximumCustomPrintQuantity ?? 100000, 6, true, "number", "min=1 max=100000")}${textarea("Giới hạn / ghi chú dịch vụ", "serviceNotes", p.serviceNotes, 4000)}
      </div></section>
      <div class="form-actions span-2"><button class="primary" type="submit">Lưu hồ sơ</button><span class="muted">Thông tin được cập nhật xuống database sau khi server xác nhận.</span></div></form>`)}
      <div style="height:16px"></div>${panel("Chủ sở hữu & trạng thái quản trị", "Chỉ đọc.", `<div class="detail-grid"><div class="detail-cell"><b>CHỦ XƯỞNG</b>${esc(p.ownerName || "—")}</div><div class="detail-cell"><b>EMAIL TÀI KHOẢN</b>${esc(p.ownerEmail || "—")}</div><div class="detail-cell"><b>XÁC MINH</b>${pill(p.verificationStatus, statusTone(p.verificationStatus))}</div><div class="detail-cell"><b>TRẠNG THÁI</b>${pill(p.status, statusTone(p.status))}</div><div class="detail-cell"><b>ĐÁNH GIÁ</b>${p.rating == null ? "Chưa có dữ liệu" : `${esc(p.rating)} / 5`}</div></div>`)} `;
}
function field(label, name, value, max, required = false, type = "text", extra = "") {
  return `<label>${esc(label)}<input type="${type}" name="${name}" value="${esc(value ?? "")}" maxlength="${max}" ${required ? "required" : ""} ${extra}></label>`;
}
function textarea(label, name, value, max) {
  return `<label class="span-2">${esc(label)}<textarea name="${name}" maxlength="${max}" rows="3">${esc(value ?? "")}</textarea></label>`;
}
function categoryOptions(categories, selected = "") {
  return (categories || []).map(category => `<option value="${esc(category.id)}" ${String(category.id) === String(selected) ? "selected" : ""}>${esc(category.name)}</option>`).join("");
}
function offers(data) {
  const items = data.items || [];
  const categories = data.options?.[0] || [];
  const cards = items.map(item => offerCard(item, categories)).join("");
  const params = new URLSearchParams(window.location.search);
  const toolbar = `<form class="toolbar" data-filter="offers"><label class="field">Tìm kiếm<input name="q" value="${esc(params.get("q") || "")}" placeholder="Tên, SKU, vật liệu"></label><label class="field">Trạng thái<select name="status"><option value="ALL">Tất cả</option>${["ACTIVE","INACTIVE","PENDING_REVIEW","APPROVED","REJECTED"].map(s => `<option value="${s}" ${params.get("status") === s ? "selected" : ""}>${esc(s)}</option>`).join("")}</select></label><label class="field">Danh mục<select name="category"><option value="">Tất cả danh mục</option>${categoryOptions(categories, params.get("category"))}</select></label><button class="secondary" type="submit">Lọc</button></form>`;
  const form = `<details class="editor"><summary>+ Thêm sản phẩm và chào bán mới</summary><form class="form-grid" data-form="new-offer">
      ${field("Tên sản phẩm", "name", "", 180, true)}<label>Danh mục<select name="categoryId" required><option value="">Chọn danh mục</option>${categoryOptions(categories)}</select></label>
      ${field("Thương hiệu", "brand", "", 120)}${field("Ảnh sản phẩm (URL HTTP/HTTPS)", "imageUrl", "", 500, false, "url")}${field("SKU (không bắt buộc)", "sku", "", 80)}${textarea("Mô tả sản phẩm", "description", "", 8000)}
      ${field("Công nghệ in", "printingMethod", "", 100, true)}${field("Vật liệu", "material", "", 120, true)}${field("Đơn giá mỗi sản phẩm (VND)", "unitPrice", "", 20, true, "number", "min=0.01 step=0.01")}
      ${field("Số lượng tối thiểu", "minimumQuantity", "1", 7, true, "number", "min=1 max=1000000")}${field("Thời gian sản xuất (ngày)", "productionDays", "", 3, false, "number", "min=0 max=365")}
      <label>Tồn kho<select name="stockStatus"><option value="IN_STOCK">Còn hàng</option><option value="PREORDER">Đặt trước</option><option value="OUT_OF_STOCK">Hết hàng</option></select></label><label class="check-line"><input type="checkbox" name="customizable" value="true"> Có thể tùy chỉnh</label>
      <div class="notice span-2">Sản phẩm mới được bật chào bán riêng của xưởng nhưng giữ is_active độc lập và bắt đầu ở trạng thái chờ Admin duyệt; chưa duyệt sẽ không xuất hiện công khai.</div><div class="form-actions span-2"><button class="primary" type="submit">Tạo sản phẩm chờ duyệt</button></div></form></details>`;
  return `${heading("CATALOG & OFFER", "Sản phẩm & bảng giá", "Giá, tồn kho và thời gian được lưu theo từng chào bán shop_products.")}${panel("Quản lý danh mục sản phẩm", "Sửa giá chào bán không thay đổi giá snapshot của đơn cũ.", `${toolbar}${form}${items.length ? cards : empty("Xưởng chưa có sản phẩm chào bán.")}${pagination(data)}`)}`;
}
function offerCard(item, categories) {
  const moderation = item.moderationStatus;
  const contentEditor = item.canEditContent ? `<details class="editor"><summary>Sửa nội dung sản phẩm (sẽ gửi Admin duyệt lại)</summary><form class="form-grid" data-form="product-content" data-id="${esc(item.productId)}">
    ${field("Ảnh sản phẩm (URL HTTP/HTTPS)", "imageUrl", item.imageUrl, 500, false, "url")}${field("Tên sản phẩm", "name", item.name, 180, true)}<label>Danh mục<select name="categoryId" required>${categoryOptions(categories, item.categoryId)}</select></label>${field("Thương hiệu", "brand", item.brand, 120)}${textarea("Mô tả", "description", item.description, 8000)}<label class="check-line"><input type="checkbox" name="customizable" value="true" ${item.customizable ? "checked" : ""}> Có thể tùy chỉnh</label><div class="form-actions span-2"><button class="secondary" type="submit">Lưu nội dung</button></div></form></details>` : `<p class="subtext">Thông tin sản phẩm dùng chung với xưởng khác; bạn chỉ có thể sửa chào bán của mình.</p>`;
  return `<article class="offer-card"><div class="offer-top"><div><h3>${esc(item.name)}</h3><span class="subtext">${esc(item.category || "Chưa phân loại")} · #${esc(item.productId)} ${item.sku ? `· SKU ${esc(item.sku)}` : ""}</span></div><div class="offer-actions">${pill(moderation, statusTone(moderation))}${pill(item.offerActive ? "Chào bán đang bật" : "Chào bán tạm dừng", item.offerActive ? "good" : "warn")}</div></div><div class="offer-meta"><span>${esc(item.printingMethod || "Chưa chọn công nghệ")}</span><span>·</span><span>${esc(item.material || "Chưa chọn vật liệu")}</span><span>·</span><strong>${money(item.unitPrice)}</strong><span>·</span><span>SL tối thiểu ${number(item.minimumQuantity)}</span><span>·</span><span>${number(item.productionDays)} ngày</span><span>·</span><span>${esc(item.stockStatus || "—")}</span></div>${item.rejectionReason ? `<p class="subtext">Lý do từ chối: ${esc(item.rejectionReason)}</p>` : ""}
    <details class="editor"><summary>Sửa bảng giá / thông tin chào bán</summary><form class="form-grid" data-form="offer" data-id="${esc(item.id)}">${field("SKU", "sku", item.sku, 80)}${field("Công nghệ in", "printingMethod", item.printingMethod, 100, true)}${field("Vật liệu", "material", item.material, 120, true)}${field("Đơn giá", "unitPrice", item.unitPrice, 20, true, "number", "min=0.01 step=0.01")}${field("Số lượng tối thiểu", "minimumQuantity", item.minimumQuantity, 7, true, "number", "min=1 max=1000000")}${field("Thời gian sản xuất (ngày)", "productionDays", item.productionDays, 3, false, "number", "min=0 max=365")}<label>Tình trạng kho<select name="stockStatus">${["IN_STOCK","PREORDER","OUT_OF_STOCK"].map(s => `<option value="${s}" ${item.stockStatus === s ? "selected" : ""}>${s}</option>`).join("")}</select></label><div class="form-actions span-2"><button class="secondary" type="submit">Lưu chào bán</button><button class="${item.offerActive ? "danger" : "primary"}" type="button" data-action="offer-active" data-id="${esc(item.id)}" data-active="${!item.offerActive}">${item.offerActive ? "Tạm dừng chào bán" : "Mở chào bán"}</button></div></form></details>${contentEditor}</article>`;
}
function pagination(data) {
  if (!data || Number(data.pages) < 2) return "";
  return `<div class="pagination"><span>${number(data.total)} bản ghi · trang ${number(data.page)} / ${number(data.pages)}</span><button type="button" data-action="page" data-page="${Math.max(1, Number(data.page) - 1)}" ${Number(data.page) <= 1 ? "disabled" : ""}>Trước</button><button type="button" data-action="page" data-page="${Math.min(Number(data.pages), Number(data.page) + 1)}" ${Number(data.page) >= Number(data.pages) ? "disabled" : ""}>Sau</button></div>`;
}
function orders(data) {
  const params = new URLSearchParams(window.location.search);
  const toolbar = `<form class="toolbar" data-filter="orders"><label class="field">Tìm kiếm<input name="q" value="${esc(params.get("q") || "")}" placeholder="Mã đơn, sản phẩm, khách hàng"></label><label class="field">Trạng thái đơn<select name="status"><option value="ALL">Tất cả</option>${(data.options?.[0] || []).map(option => `<option value="${esc(option.value)}" ${params.get("status") === option.value ? "selected" : ""}>${esc(option.value)}</option>`).join("")}</select></label><button class="secondary" type="submit">Lọc</button></form>`;
  const cards = (data.items || []).map(item => `<article class="order-card"><div class="order-card-head"><div><h3>${esc(item.code)} · ${esc(item.productName)}</h3><p>SL ${number(item.quantity)} · ${money(item.lineTotal)} · đặt ${date(item.placedAt)}</p>${item.designFileName ? `<p>Tệp thiết kế: ${esc(item.designFileName)}</p>` : ""}${item.customPrintTechnology ? `<p>In 3D: ${esc(item.customPrintTechnology)} · ${esc(item.customPrintMaterial)} · ${number(item.customPrintProductionDays)} ngày</p>` : ""}<p>${item.customerName ? `Khách hàng: ${esc(item.customerName)} · ` : ""}${esc(item.latestStage || "Chưa cập nhật tiến độ")}${item.latestProgress != null ? ` · ${number(item.latestProgress)}%` : ""}</p></div><div>${pill(item.orderStatus, statusTone(item.orderStatus))}</div></div><div class="form-actions"><button type="button" class="secondary small-button" data-action="order-detail" data-id="${esc(item.id)}">Chi tiết & tiến độ</button></div></article>`).join("");
  return `${heading("ORDER FULFILLMENT", "Đơn hàng & tiến độ", "Danh sách chỉ gồm dòng hàng được gán cho xưởng này; trạng thái đơn và thanh toán do hệ thống hiện tại quản lý.")}${panel("Đơn hàng được giao cho xưởng", "Không thể xem dòng hàng của xưởng khác.", `${toolbar}${cards || empty("Chưa có dòng đơn hàng thuộc xưởng.")}${pagination(data)}`)}`;
}
function quotes(data) {
  const params = new URLSearchParams(window.location.search);
  const toolbar = `<form class="toolbar" data-filter="quotes"><label class="field">Tìm kiếm<input name="q" value="${esc(params.get("q") || "")}" placeholder="Mã, doanh nghiệp, khách hàng"></label><label class="field">Trạng thái<select name="status"><option value="ALL">Tất cả</option>${(data.options?.[0] || []).map(option => `<option value="${esc(option.value)}" ${params.get("status") === option.value ? "selected" : ""}>${esc(option.value)}</option>`).join("")}</select></label><button class="secondary" type="submit">Lọc</button></form>`;
  const items = (data.items || []).map(item => `<article class="order-card"><div class="order-card-head"><div><h3>Yêu cầu #${esc(item.id)} · ${esc(item.productName || "Dịch vụ in")}</h3><p>${esc(item.companyName)} · ${number(item.quantity)} sản phẩm · hạn ${date(item.requiredBy)}</p><p>Khách hàng: ${esc(item.customerName || "—")} ${item.customerEmail ? `· ${esc(item.customerEmail)}` : ""}</p>${item.responseNote ? `<p>Phản hồi: ${esc(item.responseNote)}</p>` : ""}</div><div>${pill(item.status, statusTone(item.status))}${item.quotedAmount != null ? `<p><strong>${money(item.quotedAmount)}</strong></p>` : ""}</div></div>
    ${item.status === "Chờ xử lý" ? `<form class="quote-response" data-form="quote" data-id="${esc(item.id)}"><input name="amount" type="number" min="0.01" step="0.01" placeholder="Giá báo (VND)"><textarea name="note" maxlength="500" placeholder="Ghi chú hoặc lý do đóng yêu cầu (8–500 ký tự)"></textarea><div class="form-actions"><button class="primary small-button" type="submit" value="QUOTE">Gửi báo giá</button><button class="danger small-button" type="submit" value="CLOSE">Từ chối / đóng</button></div></form>` : ""}</article>`).join("");
  return `${heading("CUSTOMER REQUESTS", "Yêu cầu báo giá", "Chỉ hiện yêu cầu khách hàng đã gửi riêng cho xưởng. Báo giá do xưởng xác nhận, không tự tính từ tệp 3D.")}${panel("Yêu cầu được gửi đến xưởng", "Mỗi yêu cầu chỉ phản hồi một lần; khách hàng sẽ thấy báo giá hoặc lý do đóng.", `${toolbar}${items || empty("Chưa có yêu cầu báo giá dành cho xưởng.")}${pagination(data)}`)}`;
}
function reviews(data) {
  const items = (data || []).map(review => `<article class="review-card"><div class="order-card-head"><div><strong>${esc(review.customerName || "Khách hàng")}</strong><span class="subtext">${esc(review.productName || "Sản phẩm")} · Đơn ${esc(review.orderCode || "—")}</span></div><span class="stars">${"★".repeat(Math.min(5, Number(review.rating) || 0))}${"☆".repeat(Math.max(0, 5 - (Number(review.rating) || 0)))}</span></div><p class="muted">${esc(review.reviewText || "Khách không để lại nhận xét.")}</p></article>`).join("");
  return `${heading("CUSTOMER FEEDBACK", "Đánh giá", "Chỉ hiển thị đánh giá đã được khách hàng gửi và đang công khai; xưởng không thể chỉnh sửa điểm.")}${panel("Đánh giá đã công khai", "Tối đa 50 đánh giá gần nhất.", items || empty("Chưa có đánh giá công khai."))}`;
}

function customPrintPricingPanel(profiles) {
  const check=(name,label,checked=true)=>`<label class="check-line"><input type="checkbox" name="${name}" value="true" ${checked?"checked":""}> ${label}</label>`;
  const form=`<details class="editor"><summary>+ Cấu hình giá in 3D theo vật liệu và công nghệ</summary><form class="form-grid" data-form="custom-price">
    <label>Công nghệ<select name="technology"><option>FDM</option><option>SLA</option><option>SLS</option></select></label><label>Vật liệu<select name="material"><option>PLA</option><option>ABS</option><option>PETG</option><option>TPU</option><option>RESIN-STD</option><option>RESIN-CLEAR</option><option>RESIN-FLEX</option><option>NYLON-PA12</option><option>NYLON-GF</option></select></label>
    ${field("Giá vật liệu (VND/gram)","materialPricePerGram","180",18,true,"number","min=0.0001 step=0.01")}${field("Mật độ (gram/cm³)","materialDensity","1.24",12,true,"number","min=0.01 step=0.01")}${field("Hao hụt vật liệu (hệ số)","materialWasteFactor","0.05",12,false,"number","min=0 step=0.01")}${field("Chi phí máy (VND/giờ)","machineCostPerHour","10000",18,false,"number","min=0 step=1")}${field("Tốc độ đắp (cm³/giờ)","depositionRate","10",12,true,"number","min=0.001 step=0.1")}${field("Độ dày vỏ (mm)","wallThickness","1.2",12,true,"number","min=0.1 step=0.1")}${field("Hệ số vật liệu support","supportFactor","0.05",12,false,"number","min=0 step=0.01")}${field("Hệ số infill","infillUsageFactor","1",12,true,"number","min=0.01 step=0.01")}${field("Layer tham chiếu (mm)","referenceLayerHeight","0.2",12,true,"number","min=0.01 step=0.01")}${field("Hệ số công nghệ","technologyFactor","1",12,true,"number","min=0.01 step=0.01")}${field("Hệ số phức tạp","complexityFactor","1",12,true,"number","min=0.01 step=0.01")}${field("Giờ chuẩn bị","setupHours","0.05",12,false,"number","min=0 step=0.01")}${field("Phí gia công cơ bản (VND)","postProcessingFee","0",18,false,"number","min=0 step=1")}${field("Giá tối thiểu/đơn (VND)","minimumOrderPrice","25000",18,false,"number","min=0 step=1")}${field("Layer nhỏ nhất (mm)","minimumLayerHeight","0.08",12,true,"number","min=0.01 step=0.01")}${field("Layer lớn nhất (mm)","maximumLayerHeight","0.4",12,true,"number","min=0.01 step=0.01")}${field("Infill nhỏ nhất (%)","minimumInfill","0",8,false,"number","min=0 max=100")}${field("Infill lớn nhất (%)","maximumInfill","100",8,false,"number","min=0 max=100")}${field("Thời gian sản xuất (ngày)","productionDays","3",8,true,"number","min=1 max=365")}
    <section class="profile-group span-2"><h3>Bật/tắt thành phần giá</h3><div class="form-grid">${check("useMaterialCost","Tính phí vật liệu")}${check("useMachineCost","Tính phí máy")}${check("useInfillFactor","Áp dụng infill")}${check("useLayerHeightFactor","Áp dụng layer height")}${check("useTechnologyFactor","Áp dụng công nghệ")}${check("useComplexityFactor","Áp dụng độ phức tạp")}${check("usePostProcessingFee","Tính gia công")}${check("useQuantityDiscount","Áp dụng chiết khấu số lượng")}${check("useMinimumPrice","Áp dụng giá tối thiểu")}${check("active","Nhận đặt trực tiếp",true)}</div></section>
    <div class="notice span-2">Ước tính dùng thể tích, bề mặt, vật liệu, thời gian máy và các hệ số trên; không phải kết quả slicer. Tắt thành phần sẽ loại nó khỏi công thức.</div><div class="form-actions span-2"><button class="primary" type="submit">Lưu cấu hình</button></div></form></details>`;
  const cards=(profiles||[]).map(p=>`<article class="order-card"><div class="order-card-head"><div><h3>${esc(p.printing_method)} · ${esc(p.material)}</h3><p>${p.is_active?"Đang nhận đặt trực tiếp":"Đã tắt đặt trực tiếp"} · tối thiểu ${money(p.minimum_order_price)} · ${number(p.production_days)} ngày</p></div>${pill(p.is_active?"ACTIVE":"INACTIVE",p.is_active?"good":"bad")}</div>
    <p class="muted">Giá vật liệu ${money(p.material_price_per_gram)}/gram · máy ${money(p.machine_cost_per_hour)}/giờ · tốc độ ${number(p.deposition_rate_cm3_per_hour)} cm³/giờ · layer ${esc(p.minimum_layer_height_mm)}–${esc(p.maximum_layer_height_mm)} mm.</p>
    <p><b>Tùy chọn:</b> ${(p.options||[]).map(o=>`${esc(o.label)} (${money(o.fee)}${o.active?"":" · tắt"})`).join(", ")||"Chưa cấu hình"}</p>
    <p><b>Chiết khấu:</b> ${(p.discounts||[]).map(d=>`${number(d.minimumQuantity)}+: ${(Number(d.discountRate)*100).toFixed(1)}%`).join(", ")||"Chưa cấu hình"}</p>
    <details class="editor"><summary>Thêm/cập nhật tùy chọn màu hoặc gia công</summary><form class="form-grid" data-form="custom-option" data-id="${esc(p.profile_id)}"><label>Loại<select name="type"><option value="COLOR">Màu</option><option value="FINISH">Gia công sau in</option></select></label>${field("Mã tùy chọn","value","",100,true)}${field("Tên hiển thị","label","",120,true)}${field("Phí (VND)","fee","0",18,true,"number","min=0 step=1")}<label class="check-line"><input type="checkbox" name="active" value="true" checked> Đang hỗ trợ</label><div class="form-actions span-2"><button class="secondary" type="submit">Lưu tùy chọn</button></div></form></details>
    <details class="editor"><summary>Thêm/cập nhật chiết khấu số lượng</summary><form class="form-grid" data-form="custom-discount" data-id="${esc(p.profile_id)}">${field("Áp dụng từ số lượng","minimumQuantity","5",8,true,"number","min=2 max=100000")}${field("Tỷ lệ (0–0.9)","discountRate","0.05",8,true,"number","min=0 max=0.9 step=0.01")}<div class="form-actions span-2"><button class="secondary" type="submit">Lưu chiết khấu</button></div></form></details></article>`).join("");
  return `<div class="custom-print-price-panel">${panel("Bảng giá in 3D theo yêu cầu","Xưởng kiểm soát cấu hình, thông số và các thành phần được tính.",`${form}${cards||empty("Chưa có cấu hình giá in 3D.")}`)}</div>`;
}

function customPrintQuotes(items) {
  const cards=(items||[]).map(item=>{
    const download=`${contextPath}/workshop/api/custom-print-files/requests/${encodeURIComponent(item.id)}?shopId=${encodeURIComponent(state.shopId)}`;
    const responseForm=item.status==="PENDING_REVIEW"?`<form class="quote-response" data-form="custom-print-quote" data-id="${esc(item.id)}">${field("Tổng giá chính thức (VND)","amount","",18,true,"number","min=0.01 step=0.01")}${field("Thời gian sản xuất (ngày)","productionDays","3",3,true,"number","min=1 max=365")}<textarea name="note" maxlength="500" required placeholder="Điều kiện hoặc ghi chú báo giá"></textarea><div class="form-actions"><button class="primary small-button" type="submit" value="QUOTE">Gửi báo giá</button><button class="danger small-button" type="submit" value="CLOSE">Từ chối yêu cầu</button></div></form>`:"";
    return `<article class="order-card"><div class="order-card-head"><div><h3>${esc(item.request_code)} · ${esc(item.original_filename)}</h3><p>${esc(item.technology||"—")} · ${esc(item.material||"—")} · SL ${number(item.quantity)}</p><p>Khách hàng: ${esc(item.customer_name||"—")} · ${esc(item.customer_email||"")} · ${esc(item.customer_phone||"")}</p><p>Mô hình: ${esc(item.width_mm||"—")} × ${esc(item.depth_mm||"—")} × ${esc(item.height_mm||"—")} mm · ${esc(item.analysis_status||"chưa phân tích")} (${esc(item.source_unit||"—")})</p>${item.customer_note?`<p>Ghi chú: ${esc(item.customer_note)}</p>`:""}</div><div>${pill(item.status,statusTone(item.status))}${item.estimated_total!=null?`<p>Ước tính: ${money(item.estimated_total)}</p>`:""}${item.quoted_amount!=null?`<p><strong>Báo giá: ${money(item.quoted_amount)}</strong></p>`:""}</div></div><div class="form-actions"><a class="secondary small-button" href="${download}">Tải file mô hình</a></div>${responseForm}</article>`;
  }).join("");
  return `${heading("CUSTOM 3D PRINT","Yêu cầu in 3D","Yêu cầu trong danh sách được gửi riêng cho xưởng hiện tại; file tải xuống qua endpoint có kiểm tra quyền.")}${panel("Yêu cầu báo giá in 3D","Xem thông số, tải file, gửi tổng giá và thời gian thực hiện.",cards||empty("Chưa có yêu cầu báo giá in 3D."))}`;
}
function modalMarkup() {
  if (!state.modal) return "";
  const item = state.modal;
  const updates = (item.productionUpdates || []).map(update => `<div class="timeline-item"><strong>${esc(update.stage)} · ${number(update.progressPercent)}%</strong><small>${date(update.createdAt)} · ${esc(update.createdBy || "Xưởng")}${update.message ? ` — ${esc(update.message)}` : ""}</small></div>`).join("");
  const statusHistory = (item.orderStatusHistory || []).map(update => `<div class="timeline-item"><strong>${esc(update.oldStatus || "Bắt đầu")} → ${esc(update.newStatus)}</strong><small>${date(update.changedAt)}${update.note ? ` — ${esc(update.note)}` : ""}</small></div>`).join("");
  const canUpdate = ["Đã xác nhận", "Đang xử lý"].includes(item.orderStatus)
      && ["Đã thanh toán", "COD (thanh toán khi nhận hàng)"].includes(item.paymentStatus);
  const etaValue = String(item.estimatedDeliveryAt || "").slice(0, 10);
  const etaInput = item.customPrintTechnology
      ? '<label>Ngày dự kiến hoàn thành<input type="date" name="estimatedDeliveryDate" value="' + esc(etaValue) + '"><small>Để trống nếu không cần thay đổi ngày.</small></label>'
      : "";
  const progressForm = canUpdate
      ? '<form class="progress-form" data-form="progress" data-id="' + esc(item.id) + '"><select name="stage"><option value="ACCEPTED">Tiếp nhận</option><option value="PRINTING">Đang in</option><option value="POST_PROCESSING">Gia công sau in</option><option value="READY_FOR_SHIPPING">Sẵn sàng giao</option></select><input type="number" name="progress" min="0" max="100" placeholder="% tiến độ" required><input name="message" maxlength="500" placeholder="Ghi chú (tuỳ chọn)">' + etaInput + '<button class="primary small-button" type="submit">Cập nhật</button></form>'
      : '<p class="muted">Chỉ cập nhật tiến độ sau khi đơn được xác nhận và đủ điều kiện thanh toán.</p>';
  const fileDownload=item.designFileName?`<a class="secondary small-button" href="${contextPath}/workshop/api/custom-print-files/orders/${encodeURIComponent(item.id)}?shopId=${encodeURIComponent(state.shopId)}">Tải file thiết kế</a>`:"";
  const customPrintDetails=item.customPrintTechnology?`<div class="detail-cell"><b>CÔNG NGHỆ / VẬT LIỆU</b>${esc(item.customPrintTechnology)} · ${esc(item.customPrintMaterial)}</div><div class="detail-cell"><b>INFILL / LAYER</b>${number(item.customPrintInfill)}% · ${esc(item.customPrintLayerHeight)} mm</div><div class="detail-cell"><b>MÀU / GIA CÔNG</b>${esc(item.customPrintColor||"—")} · ${esc(item.customPrintFinish||"—")}</div><div class="detail-cell"><b>THỜI GIAN DỰ KIẾN</b>${number(item.customPrintProductionDays)} ngày</div>`:"";
  return `<div class="modal-backdrop" data-action="modal-backdrop"><section class="modal" role="dialog" aria-modal="true" aria-labelledby="detail-title"><header class="modal-head"><div><h2 id="detail-title">${esc(item.code)} · ${esc(item.productName)}</h2><p class="muted">Chi tiết chỉ thuộc dòng hàng của xưởng hiện tại.</p></div><button type="button" class="close-button" data-action="modal-close" aria-label="Đóng">×</button></header><div class="detail-grid"><div class="detail-cell"><b>TRẠNG THÁI ĐƠN</b>${pill(item.orderStatus, statusTone(item.orderStatus))}</div><div class="detail-cell"><b>THANH TOÁN</b>${pill(item.paymentStatus, statusTone(item.paymentStatus))}</div><div class="detail-cell"><b>SỐ LƯỢNG / ĐƠN GIÁ</b>${number(item.quantity)} × ${money(item.unitPrice)}</div>${customPrintDetails}<div class="detail-cell"><b>KHÁCH HÀNG</b>${esc(item.customerName || "Chưa hiển thị trước khi xác nhận")}</div><div class="detail-cell"><b>NGƯỜI NHẬN</b>${esc(item.recipientName || "—")}</div><div class="detail-cell"><b>ĐIỆN THOẠI</b>${esc(item.recipientPhone || "—")}</div><div class="detail-cell"><b>ĐỊA CHỈ GIAO</b>${esc(item.shippingAddress || "—")}</div><div class="detail-cell"><b>TỆP THIẾT KẾ</b>${esc(item.designFileName || "Không có tệp được lưu")} ${fileDownload}</div></div><h3>Lịch sử trạng thái đơn</h3><div class="timeline">${statusHistory || empty("Chưa có lịch sử trạng thái.")}</div><h3>Lịch sử tiến độ xưởng</h3><div class="timeline">${updates || empty("Chưa có cập nhật sản xuất.")}</div>${progressForm}</section></div>`;
}

async function load() {
  state.loading = true; state.error = ""; render();
  try {
    state.actor = await apiGet("/session", false);
    state.shops = state.actor.shops || [];
    const requested = urlParams.get("shopId");
    state.shopId = state.shops.some(shop => String(shop.id) === requested) ? requested : String(state.shops[0]?.id || "");
    if (!state.shopId) throw new Error("Tài khoản chưa có xưởng liên kết. Hãy gửi đăng ký xưởng để Admin xác minh.");
    const params = new URLSearchParams(); params.set("shopId", state.shopId);
    for (const key of ["q", "status", "category", "page", "size"]) {
      const value = urlParams.get(key);
      if (value && !(state.page === "changeRequests" && key === "status" && value === "ALL")) params.set(key, value);
    }
    const endpoint = ({ dashboard: "/dashboard", profile: "/profile", products: "/offers", orders: "/orders", quotes: "/quotes", messages: "/messages", changeRequests: "/change-requests", reviews: "/reviews" })[state.page];
    const response = await fetch(`${contextPath}/workshop/api${endpoint}?${params}`, { credentials: "same-origin", cache: "no-store", headers: { Accept: "application/json" } });
    state.data = await readResponse(response);
    state.customPrintRequests = [];
    state.priceProfiles = [];
    state.messageThread = [];
    if (state.page === "quotes") {
      const customResponse=await fetch(`${contextPath}/workshop/api/custom-print-requests?shopId=${encodeURIComponent(state.shopId)}&status=${encodeURIComponent(urlParams.get("status")||"ALL")}&q=${encodeURIComponent(urlParams.get("q")||"")}`,{credentials:"same-origin",cache:"no-store",headers:{Accept:"application/json"}});
      state.customPrintRequests=await readResponse(customResponse);
    } else if (state.page === "products") {
      const priceResponse=await fetch(`${contextPath}/workshop/api/custom-print-pricing?shopId=${encodeURIComponent(state.shopId)}`,{credentials:"same-origin",cache:"no-store",headers:{Accept:"application/json"}});
      state.priceProfiles=await readResponse(priceResponse);
    } else if (state.page === "messages" && urlParams.get("orderId")) {
      const selectedOrderId = urlParams.get("orderId");
      state.messageThread = await apiGet(`/messages/${encodeURIComponent(selectedOrderId)}`);
      const selectedThread = (state.data || []).find(thread => String(thread.orderId) === String(selectedOrderId));
      if (selectedThread) selectedThread.unread = 0;
    }
    try { state.notifications = await apiGet("/notification-summary"); }
    catch { state.notifications = {}; }
  } catch (error) { state.error = error.message || "Không thể tải dữ liệu xưởng."; }
  finally { state.loading = false; render(); }
}
let workshopNotificationTimerStarted = false;
async function refreshWorkshopNotifications() {
  if (!state.shopId || state.loading) return;
  try {
    state.notifications = await apiGet("/notification-summary");
    render();
  } catch { /* Keep the portal usable if the badge endpoint is temporarily unavailable. */ }
}
function startWorkshopNotificationPolling() {
  if (workshopNotificationTimerStarted || typeof window.setInterval !== "function") return;
  workshopNotificationTimerStarted = true;
  window.setInterval(refreshWorkshopNotifications, 30000);
}

async function submitMutation(path, values, message) {
  try { await apiPost(path, values); state.modal = null; toast(message); await load().then(startWorkshopNotificationPolling); }
  catch (error) { toast(error.message || "Không thể lưu thay đổi.", true); }
}
function navigateFilter(form) {
  const url = new URL(window.location.href);
  const params = new URLSearchParams();
  params.set("shopId", state.shopId);
  for (const [key, value] of new FormData(form).entries()) if (String(value).trim()) params.set(key, String(value).trim());
  url.search = params.toString(); window.location.assign(url.toString());
}

root.addEventListener("change", event => {
  if (event.target.id === "shop-switcher") {
    const url = new URL(window.location.href); url.searchParams.set("shopId", event.target.value); url.searchParams.delete("page");
    window.location.assign(url.toString());
  }
});
root.addEventListener("click", async event => {
  const control = event.target.closest("[data-action]");
  if (!control) return;
  const action = control.dataset.action;
  if (action === "retry") { await load(); return; }
  if (action === "offer-active") {
    await submitMutation(`/offers/${encodeURIComponent(control.dataset.id)}/active`, { active: control.dataset.active }, "Đã cập nhật trạng thái chào bán.");
  } else if (action === "page") {
    const url = new URL(window.location.href); url.searchParams.set("page", control.dataset.page); url.searchParams.set("shopId", state.shopId); window.location.assign(url.toString());
  } else if (action === "order-detail") {
    try { state.modal = await apiGet(`/orders/${encodeURIComponent(control.dataset.id)}`); render(); }
    catch (error) { toast(error.message || "Không thể tải chi tiết đơn.", true); }
  } else if (action === "modal-close" || (action === "modal-backdrop" && event.target === control)) {
    state.modal = null; render();
  }
});
root.addEventListener("submit", async event => {
  const form = event.target.closest("form");
  if (!form) return;
  if (form.dataset.filter) { event.preventDefault(); navigateFilter(form); return; }
  const type = form.dataset.form;
  if (!type) return;
  event.preventDefault();
  const values = formValues(form);
  if (type === "profile") return submitMutation("/profile", values, "Đã lưu hồ sơ xưởng.");
  if (type === "custom-price") {
    form.querySelectorAll('input[type="checkbox"][name]').forEach(input => { values[input.name] = input.checked ? "true" : "false"; });
    return submitMutation("/custom-print-pricing", values, "Đã lưu cấu hình giá in 3D.");
  }
  if (type === "custom-option") return submitMutation(`/custom-print-pricing/${encodeURIComponent(form.dataset.id)}/options`, values, "Đã lưu tùy chọn in 3D.");
  if (type === "custom-discount") return submitMutation(`/custom-print-pricing/${encodeURIComponent(form.dataset.id)}/discounts`, values, "Đã lưu chiết khấu số lượng.");
  if (type === "new-offer") return submitMutation("/offers", values, "Đã tạo sản phẩm chờ Admin kiểm duyệt.");
  if (type === "offer") return submitMutation(`/offers/${encodeURIComponent(form.dataset.id)}`, values, "Đã lưu bảng giá chào bán.");
  if (type === "product-content") return submitMutation(`/products/${encodeURIComponent(form.dataset.id)}`, values, "Đã lưu nội dung và gửi lại kiểm duyệt.");
  if (type === "quote") {
    const action = event.submitter?.value || "QUOTE";
    if (action === "CLOSE" && String(values.note || "").trim().length < 8) { toast("Lý do đóng yêu cầu cần ít nhất 8 ký tự.", true); return; }
    if (action === "QUOTE" && !(Number(values.amount) > 0)) { toast("Nhập mức báo giá lớn hơn 0.", true); return; }
    return submitMutation(`/quotes/${encodeURIComponent(form.dataset.id)}/respond`, { ...values, action }, action === "QUOTE" ? "Đã gửi báo giá cho khách hàng." : "Đã đóng yêu cầu và lưu lý do.");
  }
  if (type === "custom-print-quote") {
    const action=event.submitter?.value||"QUOTE";
    if(action==="QUOTE"&&!(Number(values.amount)>0)){toast("Nhập tổng giá lớn hơn 0.",true);return;}
    if(action==="CLOSE"&&String(values.note||"").trim().length<8){toast("Nhập lý do từ chối tối thiểu 8 ký tự.",true);return;}
    const route=action==="QUOTE"?"quote":"close";
    return submitMutation(`/custom-print-requests/${encodeURIComponent(form.dataset.id)}/${route}`,values,
      action==="QUOTE"?"Đã gửi báo giá in 3D cho khách.":"Đã từ chối yêu cầu in 3D.");
  }
  if (type === "message") {
    const message = String(values.message || "").trim();
    if (!message) { toast("Vui lòng nhập nội dung tin nhắn.", true); return; }
    return submitMutation(`/messages/${encodeURIComponent(form.dataset.orderId)}`, { message }, "Đã gửi tin nhắn.");
  }
  if (type === "change-request") {
    const action = event.submitter?.value || "REJECTED";
    if (action === "REJECTED" && !String(values.note || "").trim()) {
      toast("Hãy ghi rõ lý do từ chối để khách hàng hiểu quyết định của xưởng.", true); return;
    }
    return submitMutation(`/change-requests/${encodeURIComponent(form.dataset.id)}/respond`,
      { ...values, action }, action === "ACCEPTED" ? "Đã chấp nhận yêu cầu thay đổi." : "Đã từ chối yêu cầu thay đổi.");
  }
  if (type === "progress") {
    if (!Number.isInteger(Number(values.progress)) || Number(values.progress) < 0 || Number(values.progress) > 100) { toast("Tiến độ phải từ 0 đến 100.", true); return; }
    return submitMutation(`/orders/items/${encodeURIComponent(form.dataset.id)}/progress`, values, "Đã lưu cập nhật tiến độ.");
  }
});

load().then(startWorkshopNotificationPolling);
