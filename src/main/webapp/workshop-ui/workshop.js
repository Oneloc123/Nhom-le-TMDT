const root = document.getElementById("workshop-app");
const contextPath = root?.dataset.contextPath || "";
const csrfToken = document.querySelector('meta[name="csrf-token"]')?.content || "";
const routes = {
  dashboard: { path: "/dashboard", title: "Tổng quan" },
  profile: { path: "/profile", title: "Hồ sơ & năng lực" },
  products: { path: "/products", title: "Sản phẩm & bảng giá" },
  orders: { path: "/orders", title: "Đơn hàng & tiến độ" },
  quotes: { path: "/quotes", title: "Yêu cầu báo giá" },
  customPrintRequests: { path: "/custom-print-requests", title: "Yêu cầu in 3D" },
  customPrintRequestDetail: { path: "/custom-print-requests", title: "Chi tiết yêu cầu in 3D" },
  messages: { path: "/messages", title: "Tin nhắn theo đơn hàng" },
  comments: { path: "/comments", title: "Hỏi đáp & Bình luận" },
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
  comments: '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/><path d="M8 10h.01M12 10h.01M16 10h.01"/></svg>',
  changeRequests: '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M4 7h13l-3-3m6 13H7l3 3"/><path d="M17 4l3 3-3 3M7 14l-3 3 3 3"/></svg>',
  customPrintRequests: '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="m12 3 8 4.5v9L12 21l-8-4.5v-9L12 3Z"/><path d="m4.5 7.8 7.5 4.4 7.5-4.4M12 12.2V21"/></svg>',
  reviews: '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="m12 3 2.7 5.5 6.1.9-4.4 4.3 1 6.1-5.4-2.9-5.4 2.9 1-6.1-4.4-4.3 6.1-.9L12 3Z"/></svg>',
  active: '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M4 7h16M4 12h16M4 17h10"/><circle cx="18" cy="17" r="2.5"/></svg>',
  completed: '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><circle cx="12" cy="12" r="8.5"/><path d="m8.3 12.2 2.4 2.4 5-5.2"/></svg>',
  cancelled: '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><circle cx="12" cy="12" r="8.5"/><path d="m9 9 6 6m0-6-6 6"/></svg>'
};
function resolveRoute(pathname = (typeof window !== "undefined" ? window.location?.pathname : "")) {
  const clean = (pathname || "").replace(/\/$/, "") || "/workshop/dashboard";
  if (/\/custom-print-requests\/[1-9]\d*$/.test(clean)) return "customPrintRequestDetail";
  return Object.keys(routes).find(key => key !== "customPrintRequestDetail" && clean.endsWith(routes[key].path)) || "dashboard";
}
function customPrintRequestIdFromPath(pathname = (typeof window !== "undefined" ? window.location?.pathname : "")) {
  const match = String(pathname || "").match(/\/custom-print-requests\/([1-9]\d*)\/?$/);
  return match ? match[1] : "";
}
function customPrintRequestUrl(id) {
  return contextPath + "/workshop/custom-print-requests/" + encodeURIComponent(id) + "?shopId=" + encodeURIComponent(state.shopId);
}
function customPrintFileUrl(id) {
  return contextPath + "/workshop/api/custom-print-files/requests/" + encodeURIComponent(id) + "?shopId=" + encodeURIComponent(state.shopId);
}
let urlParams = new URLSearchParams(typeof window !== "undefined" ? window.location?.search : "");
function syncUrlParams() {
  if (typeof window !== "undefined" && window.location) {
    urlParams = new URLSearchParams(window.location.search);
  }
}
const state = { page: resolveRoute(), shopId: "", shops: [], data: null, actor: null, loading: true, error: "", modal: null, priceProfiles: [], messageThread: [], activeEscalation: null, messageThreadLoading: false, notifications: {}, modelViewer: null };

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
  if (value.includes("hoàn thành") || value.includes("verified") || value.includes("đã báo giá") || value === "approved" || value === "quoted" || value === "order_created") return "good";
  if (value.includes("hủy") || value.includes("đóng") || value.includes("rejected") || value.includes("blocked") || value.includes("declined") || value.includes("expired") || value === "closed") return "bad";
  if (value.includes("chờ") || value.includes("pending") || value.includes("thanh toán") || value.includes("pending_review")) return "warn";
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
      const count = Number(state.notifications?.[({ orders: "pendingOrders", quotes: "pendingQuotes", customPrintRequests: "pendingCustomPrints", messages: "unreadMessages", comments: "unansweredComments", changeRequests: "pendingChanges" })[key]] || 0);
      const badge = count > 0 ? `<span class="nav-alert" aria-label="${number(count)} mục cần xử lý">${count > 99 ? "99+" : number(count)}</span>` : "";
      return `<a class="nav-link ${(state.page === key || (key === "customPrintRequests" && state.page === "customPrintRequestDetail")) ? "active" : ""}" data-nav-key="${key}" href="${esc(workshopUrl(key))}"><span class="nav-icon">${icons[key]}</span><span class="nav-label-text">${esc(item.title)}</span>${badge}</a>`;
  };
  const nav = [
    navLink(["dashboard", routes.dashboard]),
    '<div class="nav-title">QUẢN LÝ HOẠT ĐỘNG</div>',
    ...["products", "orders", "quotes", "customPrintRequests", "messages", "comments", "changeRequests", "reviews"].map(key => navLink([key, routes[key]])),
    '<div class="nav-title">CẤU HÌNH XƯỞNG</div>',
    navLink(["profile", routes.profile])
  ].join("");
  const selector = state.shops.length > 1 ? `<div class="shop-switch"><label for="shop-switcher">Xưởng đang quản lý</label><select id="shop-switcher">${state.shops.map(shop => `<option value="${esc(shop.id)}" ${String(shop.id) === String(state.shopId) ? "selected" : ""}>${esc(shop.name)}</option>`).join("")}</select></div>` : `<div class="shop-switch"><label>Xưởng của bạn</label><strong>${esc(activeShop.name || "Xưởng in")}</strong></div>`;
  const initials = (state.actor?.name || activeShop.name || "X").trim().slice(0, 1).toUpperCase();
  return `<div class="shell"><aside class="sidebar"><a class="brand" href="${esc(workshopUrl("dashboard"))}"><span class="brand-mark">P</span> PrintKraft</a>${selector}<nav class="nav-list">${nav}</nav><div class="sidebar-bottom">Quản lý dữ liệu thuộc xưởng của bạn.<form id="workshop-logout-form" method="post" action="${esc(contextPath)}/logout" class="logout-form"><input type="hidden" name="csrfToken" value="${esc(csrfToken)}"><button type="submit" aria-label="Đăng xuất khỏi tài khoản xưởng">Đăng xuất</button></form></div></aside><main class="main"><header class="topbar"><div class="topbar-title">${esc(routes[state.page].title)}</div><div class="topbar-user"><span>${esc(state.actor?.name || "Chủ xưởng")}</span><span class="avatar">${esc(initials)}</span></div></header><section class="content">${state.data?.shop ? statusBanner(state.data.shop) : ""}${content}</section></main></div>${state.modal ? modalMarkup() : ""}`;
}
function updateNavigationBadges() {
  if (typeof root?.querySelectorAll !== "function") return;
  const metricByKey = {
    orders: "pendingOrders",
    quotes: "pendingQuotes",
    customPrintRequests: "pendingCustomPrints",
    messages: "unreadMessages",
    comments: "unansweredComments",
    changeRequests: "pendingChanges"
  };
  root.querySelectorAll("[data-nav-key]").forEach(link => {
    const metric = metricByKey[link.dataset?.navKey];
    if (!metric) return;
    const count = Number(state.notifications?.[metric] || 0);
    let badge = link.querySelector(".nav-alert");
    if (count <= 0) {
      if (badge) link.removeChild(badge);
      return;
    }
    if (!badge) {
      badge = document.createElement("span");
      badge.className = "nav-alert";
      link.appendChild(badge);
    }
    badge.setAttribute("aria-label", number(count) + " mục cần xử lý");
    badge.textContent = count > 99 ? "99+" : number(count);
  });
}

function errorView() {
  return `<section class="error-screen"><div class="eyebrow">KHÔNG THỂ TẢI WORKSHOP PORTAL</div><h1>Dữ liệu chưa sẵn sàng</h1><p>${esc(state.error)}</p><button class="primary" data-action="retry">Thử tải lại</button></section>`;
}
function loadingView() { return `<main class="boot-state" role="status">Đang tải dữ liệu xưởng…</main>`; }
function render() {
  destroyCustomPrintModelViewer();
  if (state.loading && !state.actor && !state.data) { root.innerHTML = loadingView(); return; }
  if (state.error && !state.actor) { root.innerHTML = errorView(); return; }

  let content;
  if (state.loading && !state.data) {
    content = `<div class="panel" style="padding: 56px 24px; text-align: center; color: var(--muted, #64748b);">
      <div style="font-size: 26px; margin-bottom: 12px; display: inline-block;">⏳</div>
      <div style="font-size: 15px; font-weight: 600; color: var(--title, #1e293b);">Đang tải dữ liệu...</div>
    </div>`;
  } else if (state.error) {
    content = errorView();
  } else {
    content = renderPage();
  }
  root.innerHTML = layout(content);
  if (!state.loading && !state.error && state.page === "customPrintRequestDetail" && state.data?.id) {
    mountCustomPrintModelViewer(state.data.id);
  }
  const historyEl = typeof root?.querySelector === 'function' ? root.querySelector('.message-history') : null;
  if (historyEl) historyEl.scrollTop = historyEl.scrollHeight;
}

function renderPage() {
  const data = state.data || {};
  if (state.page === "dashboard") return dashboard(data);
  if (state.page === "profile") return profile(data);
  if (state.page === "products") return offers(data) + customPrintPricingPanel(state.priceProfiles);
  if (state.page === "orders") return orders(data);
  if (state.page === "quotes") return quotes(data);
  if (state.page === "customPrintRequests") return customPrintRequestList(data);
  if (state.page === "customPrintRequestDetail") return customPrintRequestDetail(data);
  if (state.page === "messages") return messageCenter(data);
  if (state.page === "comments") return commentsView(data);
  if (state.page === "changeRequests") return changeRequests(data);
  if (state.page === "reviews") return reviews(data);
  return empty("Không tìm thấy trang.");
}

function threadUrl(orderId) {
  return `${workshopUrl("messages")}&orderId=${encodeURIComponent(orderId)}`;
}
function messageCenter(threads) {
  const list = Array.isArray(threads) ? threads : (Array.isArray(threads?.items) ? threads.items : []);
  const orderId = urlParams.get("orderId");
  const selected = list.find(thread => String(thread.orderId) === String(orderId));
  const threadLinks = list.map(thread => {
    const active = String(thread.orderId) === String(orderId);
    const escBadge = thread.escalationStatus
      ? `<span class="pill warn" style="background:#fee2e2;color:#991b1b;border:1px solid #fca5a5;font-size:9px;margin-left:auto;">⚖ ${thread.escalationStatus === "OPEN" ? "Chờ Admin" : "Admin can thiệp"}</span>`
      : "";
    return `<a class="message-thread ${active ? "active" : ""}" href="${esc(threadUrl(thread.orderId))}" data-order-id="${esc(thread.orderId)}">
      <div class="message-thread-head"><strong>${esc(thread.orderCode)}</strong>${escBadge}${Number(thread.unread) > 0 ? pill(`${number(thread.unread)} mới`, "warn") : ""}</div>
      <span class="message-thread-customer">${esc(thread.customerName || "Khách hàng")}</span>
      <span class="message-thread-preview">${esc(thread.lastMessage || "Chưa có tin nhắn")}</span>
      <small>${date(thread.lastMessageAt)}</small>
    </a>`;
  }).join("");
  const bubbles = (state.messageThread || []).map(message => {
    const isAdmin = message.senderRole === "ADMIN";
    const isShop = message.senderRole === "SHOP";
    let senderLabel = "Khách hàng";
    if (isAdmin) {
      senderLabel = `🛡️ Admin (${message.sender || "Ban Quản Trị"})`;
    } else if (message.mine) {
      senderLabel = "Bạn (Xưởng in)";
    } else if (isShop) {
      senderLabel = `🏬 Xưởng in (${message.sender})`;
    } else {
      senderLabel = `👤 Khách hàng (${message.sender || "Khách"})`;
    }
    return `<div class="message-row ${message.mine ? "mine" : ""} ${isAdmin ? "is-admin" : ""}">
      <div class="message-bubble" ${isAdmin ? 'style="border:1px solid #93c5fd;background:#eff6ff;"' : ""}>
        <div class="message-meta">${esc(senderLabel)} · ${date(message.createdAt)}</div>
        <div class="message-body">${esc(message.body).replace(/\n/g, "<br>")}</div>
      </div>
    </div>`;
  }).join("");
  const escNotice = state.activeEscalation
    ? `<div class="escalation-banner" style="margin:14px 18px 0;padding:12px 16px;border-radius:12px;background:#eff6ff;border:1px solid #bfdbfe;color:#1e40af;">
        <div style="display:flex;align-items:center;justify-content:space-between;gap:8px;margin-bottom:4px;">
          <strong>⚖ Phiên hòa giải 3 bên · Trạng thái: ${esc(state.activeEscalation.status)}</strong>
          <small>${esc(state.activeEscalation.createdAt || "")}</small>
        </div>
        <p style="margin:0;font-size:12px;line-height:1.5;">
          ${state.activeEscalation.status === "IN_PROGRESS"
            ? `Admin <b>${esc(state.activeEscalation.adminName || "Ban Quản Trị")}</b> đã tham gia hỗ trợ giải quyết giữa khách hàng và xưởng.`
            : "Yêu cầu can thiệp đang chờ Admin tiếp nhận. Mọi trao đổi tại đây sẽ có sự theo dõi của Ban Quản Trị."}
        </p>
        ${state.activeEscalation.reason ? `<small style="display:block;margin-top:4px;color:#2563eb;">Lý do can thiệp: ${esc(state.activeEscalation.reason)}</small>` : ""}
        ${state.activeEscalation.verdict ? `<div style="margin-top:8px;padding:8px 12px;border-radius:8px;background:#fff;border:1px solid #93c5fd;color:#1e3a8a;"><strong>Phán quyết của Admin:</strong> ${esc(state.activeEscalation.verdict)}</div>` : ""}
      </div>`
    : `<details class="escalate-details" style="margin:14px 18px 0;">
        <summary style="cursor:pointer;display:inline-flex;align-items:center;gap:6px;padding:6px 12px;border-radius:8px;border:1px solid #fed7aa;background:#fff7ed;color:#c2410c;font-size:12px;font-weight:600;">
          <span>⚖</span> Gặp tranh chấp với khách? Yêu cầu Admin hòa giải
        </summary>
        <form class="escalate-form" data-form="escalate" data-order-id="${esc(orderId)}" style="margin-top:10px;padding:12px;border:1px solid #fed7aa;border-radius:10px;background:#fffaf0;">
          <label style="display:block;margin-bottom:6px;font-weight:600;font-size:12px;color:#9a3412;">Lý do yêu cầu Admin can thiệp:</label>
          <textarea name="reason" rows="2" maxlength="2000" required placeholder="Nêu rõ lý do tranh chấp hoặc yêu cầu hỗ trợ từ Admin (khách không nhận hàng, tranh chấp yêu cầu in, hủy đơn vô lý...)" style="width:100%;box-sizing:border-box;border:1px solid #fdba74;border-radius:8px;padding:8px;font:inherit;font-size:12px;margin-bottom:8px;"></textarea>
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <small style="color:#78350f;">Admin sẽ vào thread này và trao đổi cùng cả 2 bên.</small>
            <button class="primary" type="submit" style="background:#ea580c;border:none;padding:6px 14px;border-radius:8px;color:#fff;font-weight:600;font-size:12px;">Gửi yêu cầu can thiệp</button>
          </div>
        </form>
      </details>`;
  const historyContent = state.messageThreadLoading
    ? `<div class="empty" style="padding:48px 16px;"><span style="font-size:20px;display:inline-block;margin-bottom:8px;">⏳</span><span>Đang tải tin nhắn...</span></div>`
    : (bubbles || empty("Chưa có tin nhắn. Bạn có thể gửi lời chào đầu tiên."));
  const conversation = orderId
    ? `<section class="panel conversation-panel">
        <header class="panel-head"><div><h2>${esc(selected?.orderCode || `Đơn hàng #${orderId}`)}</h2><p>${esc(selected?.customerName || "Trao đổi riêng theo đơn hàng")}</p></div><a class="back-link" href="${esc(workshopUrl("messages"))}">Tất cả hội thoại</a></header>
        ${escNotice}
        <div class="message-history">${historyContent}</div>
        <form class="message-compose" data-form="message" data-order-id="${esc(orderId)}">
          <label class="field"><span>Tin nhắn</span><textarea name="message" rows="3" maxlength="2000" required placeholder="Trao đổi về tiến độ, thông số hoặc giải trình với Admin/Khách..."></textarea></label>
          <div class="form-actions"><small class="muted">Tối đa 2.000 ký tự · Không gửi mật khẩu, OTP hoặc thông tin thẻ.</small><button class="primary" type="submit">Gửi tin nhắn</button></div>
        </form>
      </section>`
    : `<section class="panel conversation-panel"><div class="empty"><strong>Chọn một hội thoại</strong><span>Chọn đơn hàng ở danh sách bên trái để xem lịch sử và phản hồi khách hàng.</span></div></section>`;
  return `${heading("CUSTOMER COMMUNICATION", "Tin nhắn theo đơn hàng", "Trao đổi riêng tư giữa xưởng và khách hàng; mỗi hội thoại được gắn với một đơn hàng.") }
    <div class="message-layout">
      <section class="panel message-thread-panel"><header class="panel-head"><div><h2>Hội thoại</h2><p>${number(list.length)} đơn hàng</p></div></header>
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
  const list = Array.isArray(items) ? items : (Array.isArray(items?.items) ? items.items : []);
  const selectedStatus = urlParams.get("status") || "ALL";
  const statuses = ["PENDING", "ACCEPTED", "REJECTED", "APPLIED", "WITHDRAWN"];
  const toolbar = `<form class="toolbar" data-filter="change-requests">
    <label class="field">Trạng thái<select name="status"><option value="ALL" ${selectedStatus === "ALL" ? "selected" : ""}>Tất cả</option>${statuses.map(status => `<option value="${status}" ${selectedStatus === status ? "selected" : ""}>${esc(status)}</option>`).join("")}</select></label>
    <button class="secondary" type="submit">Lọc</button>
  </form>`;
  const cards = list.map(item => {
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
  const activeCount = items.filter(item => item.offerActive).length;
  const pendingCount = items.filter(item => item.moderationStatus === "PENDING_REVIEW").length;
  const pausedCount = items.filter(item => !item.offerActive).length;
  const summary = '<div class="offer-summary-grid">' +
    '<div><span>Đang chào bán</span><strong>' + number(activeCount) + '</strong></div>' +
    '<div><span>Chờ Admin duyệt</span><strong>' + number(pendingCount) + '</strong></div>' +
    '<div><span>Đang tạm dừng</span><strong>' + number(pausedCount) + '</strong></div>' +
  '</div>';
  const toolbar = '<form class="toolbar offer-toolbar" data-filter="offers">' +
    '<label class="field">Tìm kiếm<input name="q" value="' + esc(params.get("q") || "") + '" placeholder="Tên sản phẩm, SKU hoặc vật liệu"></label>' +
    '<label class="field">Trạng thái<select name="status"><option value="ALL">Tất cả</option>' +
    ["ACTIVE", "INACTIVE", "PENDING_REVIEW", "APPROVED", "REJECTED"].map(status =>
      '<option value="' + status + '"' + (params.get("status") === status ? " selected" : "") + '>' + status + '</option>'
    ).join("") +
    '</select></label>' +
    '<label class="field">Danh mục<select name="category"><option value="">Tất cả danh mục</option>' +
    categoryOptions(categories, params.get("category")) + '</select></label>' +
    '<button class="secondary" type="submit">Áp dụng bộ lọc</button></form>';
  const form = '<details class="editor editor-create offer-create-editor">' +
    '<summary><span> Thêm sản phẩm và chào bán mới</span><small>Nhập thông tin sản phẩm, công nghệ, vật liệu và giá bán</small></summary>' +
    '<form class="form-grid" data-form="new-offer">' +
      field("Tên sản phẩm", "name", "", 180, true) +
      '<label>Danh mục<select name="categoryId" required><option value="">Chọn danh mục</option>' + categoryOptions(categories) + '</select></label>' +
      field("Thương hiệu", "brand", "", 120) +
      field("Ảnh sản phẩm (URL HTTP/HTTPS)", "imageUrl", "", 500, false, "url") +
      field("SKU (không bắt buộc)", "sku", "", 80) +
      textarea("Mô tả sản phẩm", "description", "", 8000) +
      field("Công nghệ in", "printingMethod", "", 100, true) +
      field("Vật liệu", "material", "", 120, true) +
      field("Đơn giá mỗi sản phẩm (VND)", "unitPrice", "", 20, true, "number", "min=0.01 step=0.01") +
      field("Số lượng tối thiểu", "minimumQuantity", "1", 7, true, "number", "min=1 max=1000000") +
      field("Thời gian sản xuất (ngày)", "productionDays", "", 3, false, "number", "min=0 max=365") +
      '<label>Tồn kho<select name="stockStatus"><option value="IN_STOCK">Còn hàng</option><option value="PREORDER">Đặt trước</option><option value="OUT_OF_STOCK">Hết hàng</option></select></label>' +
      '<label class="check-line"><input type="checkbox" name="customizable" value="true"> Có thể tùy chỉnh</label>' +
      '<div class="notice span-2">Sản phẩm mới bắt đầu ở trạng thái chờ Admin duyệt và chưa xuất hiện công khai cho đến khi được duyệt.</div>' +
      '<div class="form-actions span-2"><button class="primary" type="submit">Tạo sản phẩm chờ duyệt</button></div>' +
    '</form></details>';
  return heading("CATALOG & OFFER", "Sản phẩm & bảng giá", "Quản lý sản phẩm, chào bán, tồn kho và giá riêng của xưởng.") +
    summary +
    panel("Danh mục sản phẩm và chào bán", "Giá và số lượng áp dụng cho đơn mới; đơn hàng cũ giữ nguyên giá snapshot.", toolbar + form +
      '<div class="offer-list">' + (cards || empty("Xưởng chưa có sản phẩm chào bán.")) + '</div>' + pagination(data));
}

function offerCard(item, categories) {
  const moderation = item.moderationStatus;
  const contentEditor = item.canEditContent
    ? '<details class="editor offer-editor"><summary>Sửa thông tin sản phẩm <small>Gửi Admin duyệt lại nội dung</small></summary>' +
      '<form class="form-grid" data-form="product-content" data-id="' + esc(item.productId) + '">' +
        field("Ảnh sản phẩm (URL HTTP/HTTPS)", "imageUrl", item.imageUrl, 500, false, "url") +
        field("Tên sản phẩm", "name", item.name, 180, true) +
        '<label>Danh mục<select name="categoryId" required>' + categoryOptions(categories, item.categoryId) + '</select></label>' +
        field("Thương hiệu", "brand", item.brand, 120) +
        textarea("Mô tả", "description", item.description, 8000) +
        '<label class="check-line"><input type="checkbox" name="customizable" value="true" ' + (item.customizable ? "checked" : "") + '> Có thể tùy chỉnh</label>' +
        '<div class="form-actions span-2"><button class="secondary" type="submit">Lưu nội dung</button></div>' +
      '</form></details>'
    : '<p class="subtext offer-shared-note">Sản phẩm dùng chung với xưởng khác. Xưởng chỉ chỉnh sửa được bảng giá riêng.</p>';
  const stockLabel = ({ IN_STOCK: "Còn hàng", PREORDER: "Đặt trước", OUT_OF_STOCK: "Hết hàng" })[item.stockStatus] || item.stockStatus || "—";
  return '<article class="offer-card">' +
    '<div class="offer-card-header"><div class="offer-title-group"><h3>' + esc(item.name) + '</h3>' +
      '<span class="subtext">' + esc(item.category || "Chưa phân loại") + ' · #' + esc(item.productId) + (item.sku ? ' · SKU ' + esc(item.sku) : '') + '</span></div>' +
      '<div class="offer-actions">' + pill(moderation, statusTone(moderation)) +
      pill(item.offerActive ? "Đang chào bán" : "Tạm dừng", item.offerActive ? "good" : "warn") + '</div></div>' +
    '<div class="offer-price-grid">' +
      '<div class="offer-price-primary"><span>Đơn giá</span><strong>' + money(item.unitPrice) + '</strong></div>' +
      '<div><span>Số lượng tối thiểu</span><strong>' + number(item.minimumQuantity) + '</strong></div>' +
      '<div><span>Sản xuất</span><strong>' + number(item.productionDays) + ' ngày</strong></div>' +
      '<div><span>Tình trạng kho</span><strong>' + esc(stockLabel) + '</strong></div>' +
    '</div>' +
    '<div class="offer-meta"><span><b>Công nghệ:</b> ' + esc(item.printingMethod || "Chưa chọn") + '</span>' +
      '<span><b>Vật liệu:</b> ' + esc(item.material || "Chưa chọn") + '</span></div>' +
    (item.rejectionReason ? '<p class="subtext offer-rejection-note"><b>Lý do từ chối:</b> ' + esc(item.rejectionReason) + '</p>' : '') +
    '<details class="editor offer-editor"><summary>Chỉnh sửa bảng giá / chào bán <small>Cập nhật giá, tồn kho và thời gian sản xuất</small></summary>' +
      '<form class="form-grid" data-form="offer" data-id="' + esc(item.id) + '">' +
        field("SKU", "sku", item.sku, 80) +
        field("Công nghệ in", "printingMethod", item.printingMethod, 100, true) +
        field("Vật liệu", "material", item.material, 120, true) +
        field("Đơn giá (VND)", "unitPrice", item.unitPrice, 20, true, "number", "min=0.01 step=0.01") +
        field("Số lượng tối thiểu", "minimumQuantity", item.minimumQuantity, 7, true, "number", "min=1 max=1000000") +
        field("Thời gian sản xuất (ngày)", "productionDays", item.productionDays, 3, false, "number", "min=0 max=365") +
        '<label>Tình trạng kho<select name="stockStatus">' +
          ["IN_STOCK", "PREORDER", "OUT_OF_STOCK"].map(status => '<option value="' + status + '"' + (item.stockStatus === status ? " selected" : "") + '>' +
            ({ IN_STOCK: "Còn hàng", PREORDER: "Đặt trước", OUT_OF_STOCK: "Hết hàng" })[status] + '</option>').join("") +
        '</select></label>' +
        '<div class="form-actions span-2"><button class="primary" type="submit">Lưu chào bán</button>' +
          '<button class="' + (item.offerActive ? "danger" : "secondary") + '" type="button" data-action="offer-active" data-id="' + esc(item.id) +
          '" data-active="' + (!item.offerActive) + '">' + (item.offerActive ? "Tạm dừng chào bán" : "Mở chào bán") + '</button></div>' +
      '</form></details>' +
    contentEditor +
  '</article>';
}


function pagination(data) {
  if (!data || Number(data.pages) < 2) return "";
  return '<div class="pagination"><span>' + number(data.total) + ' bản ghi · trang ' + number(data.page) + ' / ' + number(data.pages) + '</span>' +
    '<button type="button" data-action="page" data-page="' + Math.max(1, Number(data.page) - 1) + '"' +
    (Number(data.page) <= 1 ? " disabled" : "") + '>Trước</button>' +
    '<button type="button" data-action="page" data-page="' + Math.min(Number(data.pages), Number(data.page) + 1) + '"' +
    (Number(data.page) >= Number(data.pages) ? " disabled" : "") + '>Sau</button></div>';
}

function orders(data) {
  const params = new URLSearchParams(window.location.search);
  const items = Array.isArray(data.items) ? data.items : [];
  const total = Number(data.total ?? items.length);
  const inProgress = items.filter(item => Number(item.latestProgress || 0) > 0 && Number(item.latestProgress || 0) < 100).length;
  const ready = items.filter(item => Number(item.latestProgress || 0) >= 100 || /sẵn sàng giao|hoàn thành/i.test(item.latestStage || "")).length;
  const waiting = items.filter(item => Number(item.latestProgress || 0) <= 0).length;
  const summary = `<div class="orders-summary">
    <div class="orders-summary-card"><span>Tổng dòng đơn hàng</span><strong>${number(total)}</strong><small>Trong kết quả hiện tại</small></div>
    <div class="orders-summary-card"><span>Chưa bắt đầu</span><strong>${number(waiting)}</strong><small>Chưa có cập nhật sản xuất</small></div>
    <div class="orders-summary-card is-active"><span>Đang sản xuất</span><strong>${number(inProgress)}</strong><small>Đã có tiến độ dưới 100%</small></div>
    <div class="orders-summary-card is-ready"><span>Sẵn sàng giao</span><strong>${number(ready)}</strong><small>Đạt mốc 100%</small></div>
  </div>`;
  const toolbar = `<form class="toolbar orders-toolbar" data-filter="orders"><label class="field">Tìm kiếm<input name="q" value="${esc(params.get("q") || "")}" placeholder="Mã đơn, sản phẩm, khách hàng"></label><label class="field">Trạng thái đơn<select name="status"><option value="ALL">Tất cả trạng thái</option>${(data.options?.[0] || []).map(option => `<option value="${esc(option.value)}" ${params.get("status") === option.value ? "selected" : ""}>${esc(option.value)}</option>`).join("")}</select></label><button class="secondary" type="submit">Lọc đơn hàng</button></form>`;
  const cards = items.map(item => {
    const progress = Math.max(0, Math.min(100, Number(item.latestProgress || 0)));
    const stage = item.latestStage || "Chưa cập nhật tiến độ";
    const progressTone = progress >= 100 ? "is-ready" : progress > 0 ? "is-active" : "is-waiting";
    return `<article class="order-card order-card-modern">
      <div class="order-card-head">
        <div class="order-card-title"><span class="order-code">${esc(item.code)}</span><h3>${esc(item.productName)}</h3><p class="order-subtitle">Đặt ngày ${date(item.placedAt)} · ${item.customerName ? `Khách hàng: ${esc(item.customerName)}` : "Chưa có thông tin khách hàng"}</p></div>
        <div class="order-card-status">${pill(item.orderStatus, statusTone(item.orderStatus))}<strong>${money(item.lineTotal)}</strong></div>
      </div>
      <div class="order-card-meta"><span><b>Số lượng</b>${number(item.quantity)} sản phẩm</span><span><b>Tiến độ hiện tại</b>${esc(stage)}</span>${item.customPrintTechnology ? `<span><b>In 3D</b>${esc(item.customPrintTechnology)} · ${esc(item.customPrintMaterial || "—")}</span>` : ""}</div>
      <div class="order-progress-block">
        <div class="order-progress-label"><span>Tiến độ sản xuất</span><strong>${number(progress)}%</strong></div>
        <div class="order-progress-track" role="progressbar" aria-label="Tiến độ sản xuất đơn ${esc(item.code)}" aria-valuemin="0" aria-valuemax="100" aria-valuenow="${progress}"><span class="${progressTone}" style="width:${progress}%"></span></div>
        <small>${progress >= 100 ? "Đã hoàn tất các bước sản xuất" : progress > 0 ? "Đơn hàng đang được xưởng xử lý" : "Đang chờ xưởng tiếp nhận và cập nhật"}</small>
      </div>
      ${item.designFileName ? `<p class="order-file">Tệp thiết kế: ${esc(item.designFileName)}</p>` : ""}
      <div class="order-card-actions"><button type="button" class="secondary small-button" data-action="order-detail" data-id="${esc(item.id)}">Xem chi tiết & cập nhật tiến độ <span aria-hidden="true">→</span></button></div>
    </article>`;
  }).join("");
  return `${heading("ORDER FULFILLMENT", "Đơn hàng & tiến độ", "Theo dõi các dòng đơn hàng được giao cho xưởng, kiểm tra tiến độ sản xuất và cập nhật từng giai đoạn.")}${summary}${panel("Danh sách đơn hàng", "Chỉ hiển thị sản phẩm thuộc xưởng đang quản lý. Trạng thái đơn và thanh toán vẫn do hệ thống quản lý.", `${toolbar}<div class="orders-list">${cards || empty("Chưa có dòng đơn hàng thuộc xưởng.")}</div>${pagination(data)}`)}`;
}
function quotes(data) {
  const params = new URLSearchParams(window.location.search);
  const toolbar = `<form class="toolbar" data-filter="quotes"><label class="field">Tìm kiếm<input name="q" value="${esc(params.get("q") || "")}" placeholder="Mã, doanh nghiệp, khách hàng"></label><label class="field">Trạng thái<select name="status"><option value="ALL">Tất cả</option>${(data.options?.[0] || []).map(option => `<option value="${esc(option.value)}" ${params.get("status") === option.value ? "selected" : ""}>${esc(option.value)}</option>`).join("")}</select></label><button class="secondary" type="submit">Lọc</button></form>`;
  const items = (data.items || []).map(item => `<article class="order-card"><div class="order-card-head"><div><h3>Yêu cầu #${esc(item.id)} · ${esc(item.productName || "Dịch vụ in")}</h3><p>${esc(item.companyName)} · ${number(item.quantity)} sản phẩm · hạn ${date(item.requiredBy)}</p><p>Khách hàng: ${esc(item.customerName || "—")} ${item.customerEmail ? `· ${esc(item.customerEmail)}` : ""}</p>${item.responseNote ? `<p>Phản hồi: ${esc(item.responseNote)}</p>` : ""}</div><div>${pill(item.status, statusTone(item.status))}${item.quotedAmount != null ? `<p><strong>${money(item.quotedAmount)}</strong></p>` : ""}</div></div>
    ${item.status === "Chờ xử lý" ? `<form class="quote-response" data-form="quote" data-id="${esc(item.id)}"><input name="amount" type="number" min="0.01" step="0.01" placeholder="Giá báo (VND)"><textarea name="note" maxlength="500" placeholder="Ghi chú hoặc lý do đóng yêu cầu (8–500 ký tự)"></textarea><div class="form-actions"><button class="primary small-button" type="submit" value="QUOTE">Gửi báo giá</button><button class="danger small-button" type="submit" value="CLOSE">Từ chối / đóng</button></div></form>` : ""}</article>`).join("");
  return `${heading("CUSTOMER REQUESTS", "Yêu cầu báo giá", "Chỉ hiện yêu cầu khách hàng đã gửi riêng cho xưởng. Báo giá do xưởng xác nhận, không tự tính từ tệp 3D.")}${panel("Yêu cầu được gửi đến xưởng", "Mỗi yêu cầu chỉ phản hồi một lần; khách hàng sẽ thấy báo giá hoặc lý do đóng.", `${toolbar}${items || empty("Chưa có yêu cầu báo giá dành cho xưởng.")}${pagination(data)}`)}`;
}
function reviews(data) {
  const list = Array.isArray(data) ? data : (Array.isArray(data?.items) ? data.items : []);
  const items = list.map(review => `<article class="review-card"><div class="order-card-head"><div><strong>${esc(review.customerName || "Khách hàng")}</strong><span class="subtext">${esc(review.productName || "Sản phẩm")} · Đơn ${esc(review.orderCode || "—")}</span></div><span class="stars">${"★".repeat(Math.min(5, Number(review.rating) || 0))}${"☆".repeat(Math.max(0, 5 - (Number(review.rating) || 0)))}</span></div><p class="muted">${esc(review.reviewText || "Khách không để lại nhận xét.")}</p></article>`).join("");
  return `${heading("CUSTOMER FEEDBACK", "Đánh giá", "Chỉ hiển thị đánh giá đã được khách hàng gửi và đang công khai; xưởng không thể chỉnh sửa điểm.")}${panel("Đánh giá đã công khai", "Tối đa 50 đánh giá gần nhất.", items || empty("Chưa có đánh giá công khai."))}`;
}

function commentsView(items) {
  const params = new URLSearchParams(window.location.search);
  const selectedStatus = params.get("status") || "ALL";
  const searchQuery = params.get("q") || "";
  const toolbar = `<form class="toolbar" data-filter="comments">
    <label class="field">Tìm kiếm<input name="q" value="${esc(searchQuery)}" placeholder="Tên sản phẩm, nội dung câu hỏi, khách hàng..."></label>
    <label class="field">Trạng thái<select name="status">
      <option value="ALL" ${selectedStatus === "ALL" ? "selected" : ""}>Tất cả câu hỏi</option>
      <option value="UNANSWERED" ${selectedStatus === "UNANSWERED" ? "selected" : ""}>Chờ phản hồi</option>
      <option value="ANSWERED" ${selectedStatus === "ANSWERED" ? "selected" : ""}>Đã phản hồi</option>
    </select></label>
    <button class="secondary" type="submit">Lọc</button>
  </form>`;

  const list = Array.isArray(items) ? items : (Array.isArray(items?.items) ? items.items : []);
  const unansweredCount = list.filter(item => !item.answered).length;
  const answeredCount = list.filter(item => item.answered).length;

  const summaryMetrics = list.length > 0 ? `<div class="metrics-grid" style="margin-bottom: 20px;">
    ${metric("TỔNG CÂU HỎI", list.length, "Số câu hỏi ghi nhận trên sản phẩm của xưởng", icons.comments)}
    ${metric("CHỜ PHẢN HỒI", unansweredCount, "Câu hỏi cần xưởng giải đáp sớm", icons.quotes)}
    ${metric("ĐÃ GIẢI ĐÁP", answeredCount, "Câu hỏi xưởng đã có phản hồi", icons.completed)}
  </div>` : "";

  const cards = list.map(item => {
    const productUrl = `${contextPath}/product-detail?id=${encodeURIComponent(item.productId)}#comments`;
    const repliesList = (item.replies || []).map(reply => {
      const isShop = reply.isSellerReply;
      const authorBadge = isShop ? `<span class="badge-seller-user" style="font-size:11px; margin-left:6px;">🏪 ${esc(reply.shopName || "Xưởng in")}</span>` : "";
      return `<div class="qa-reply-bubble ${isShop ? "is-shop-reply" : ""}">
        <div class="qa-reply-bubble-meta"><strong>${esc(reply.authorName || "Người dùng")}</strong>${authorBadge} <small class="muted">· ${date(reply.createdAt)}</small></div>
        <div class="qa-reply-bubble-body">${esc(reply.content).replace(/\n/g, "<br>")}</div>
      </div>`;
    }).join("");

    const repliesBlock = (item.replies && item.replies.length > 0)
      ? `<div class="qa-replies-thread"><div class="qa-replies-thread-title">Lịch sử phản hồi (${number(item.replies.length)})</div>${repliesList}</div>`
      : "";

    return `<article class="qa-workshop-card">
      <div class="qa-workshop-header">
        <div class="qa-product-tag">
          <span class="qa-product-icon">📦</span>
          <a class="qa-product-link" href="${esc(productUrl)}" target="_blank" rel="noopener" title="Xem trang sản phẩm">
            <strong>${esc(item.productName || `Sản phẩm #${item.productId}`)}</strong>
            <span class="qa-external-hint">↗ Xem sản phẩm</span>
          </a>
        </div>
        <div>
          ${pill(item.answered ? "Đã phản hồi" : "Chờ phản hồi", item.answered ? "good" : "warn")}
        </div>
      </div>

      <div class="qa-customer-info">
        <div class="qa-customer-avatar">${esc((item.customerName || "K").trim().slice(0, 1).toUpperCase())}</div>
        <div class="qa-customer-meta">
          <span class="qa-customer-name">${esc(item.customerName || "Khách hàng")}</span>
          <span class="qa-timestamp">${date(item.createdAt)}</span>
        </div>
      </div>

      <div class="qa-question-box">
        <div class="qa-question-label">Câu hỏi của khách hàng:</div>
        <div class="qa-question-text">${esc(item.content).replace(/\n/g, "<br>")}</div>
      </div>

      ${repliesBlock}

      <form class="qa-quick-reply-form" data-form="comment-reply" data-id="${esc(item.id)}">
        <div class="qa-chips-row">
          <span class="qa-chips-label">Gợi ý câu trả lời mẫu:</span>
          <div class="qa-chips-scroll">
            <button type="button" class="qa-chip-btn" data-action="fill-reply" data-text="Dạ mẫu này xưởng hỗ trợ in đúng theo kích thước và vật liệu bạn chọn ạ. Bạn có thể gửi file để xưởng tư vấn kỹ hơn nhé!">In theo yêu cầu</button>
            <button type="button" class="qa-chip-btn" data-action="fill-reply" data-text="Thời gian in và hoàn thiện dự kiến từ 2 - 3 ngày làm việc ạ. Sau khi in xong xưởng đóng gói cẩn thận giao ngay!">Thời gian in 2-3 ngày</button>
            <button type="button" class="qa-chip-btn" data-action="fill-reply" data-text="Xưởng sử dụng chất liệu nhựa in chính hãng, độ chi tiết cao và chịu lực tốt bạn yên tâm đặt hàng nhé!">Chất liệu bền đẹp</button>
          </div>
        </div>
        <textarea name="content" rows="3" maxlength="2000" placeholder="Nhập câu trả lời từ xưởng in để giải đáp cho khách hàng..." required></textarea>
        <div class="form-actions">
          <small class="muted">Tối đa 2.000 ký tự · Phản hồi sẽ hiển thị công khai kèm huy hiệu xưởng tại trang sản phẩm.</small>
          <button class="primary small-button" type="submit">Gửi câu trả lời</button>
        </div>
      </form>
    </article>`;
  }).join("");

  return `${heading("CUSTOMER Q&A", "Hỏi đáp & Bình luận sản phẩm", "Theo dõi và phản hồi thắc mắc của khách hàng về quy cách in ấn, vật liệu và dịch vụ của xưởng.")}
    ${summaryMetrics}
    ${panel("Danh sách câu hỏi từ khách hàng", "Khách hàng đặt câu hỏi tại trang chi tiết các sản phẩm do xưởng chào bán.", `${toolbar}${cards || empty("Chưa có câu hỏi nào từ khách hàng.")}`)}`;
}

function customPrintPricingPanel(profiles) {
  const activeProfiles = (profiles || []).filter(profile => profile.is_active).length;
  const check = (name, label, checked = true) =>
    '<label class="check-line"><input type="checkbox" name="' + name + '" value="true" ' + (checked ? "checked" : "") + '> ' + label + '</label>';
  const form = '<details class="editor editor-create price-create-editor">' +
    '<summary><span>Thêm cấu hình giá in 3D</span><small>Thiết lập công thức riêng theo công nghệ và vật liệu</small></summary>' +
    '<form class="form-grid" data-form="custom-price">' +
      '<label>Công nghệ<select name="technology"><option>FDM</option><option>SLA</option><option>SLS</option></select></label>' +
      '<label>Vật liệu<select name="material"><option>PLA</option><option>ABS</option><option>PETG</option><option>TPU</option><option>RESIN-STD</option><option>RESIN-CLEAR</option><option>RESIN-FLEX</option><option>NYLON-PA12</option><option>NYLON-GF</option></select></label>' +
      field("Giá vật liệu (VND/gram)", "materialPricePerGram", "180", 18, true, "number", "min=0.0001 step=0.01") +
      field("Mật độ (gram/cm³)", "materialDensity", "1.24", 12, true, "number", "min=0.01 step=0.01") +
      field("Hao hụt vật liệu (hệ số)", "materialWasteFactor", "0.05", 12, false, "number", "min=0 step=0.01") +
      field("Chi phí máy (VND/giờ)", "machineCostPerHour", "10000", 18, false, "number", "min=0 step=1") +
      field("Tốc độ đắp (cm³/giờ)", "depositionRate", "10", 12, true, "number", "min=0.001 step=0.1") +
      field("Độ dày vỏ (mm)", "wallThickness", "1.2", 12, true, "number", "min=0.1 step=0.1") +
      field("Hệ số vật liệu support", "supportFactor", "0.05", 12, false, "number", "min=0 step=0.01") +
      field("Hệ số infill", "infillUsageFactor", "1", 12, true, "number", "min=0.01 step=0.01") +
      field("Layer tham chiếu (mm)", "referenceLayerHeight", "0.2", 12, true, "number", "min=0.01 step=0.01") +
      field("Hệ số công nghệ", "technologyFactor", "1", 12, true, "number", "min=0.01 step=0.01") +
      field("Hệ số phức tạp", "complexityFactor", "1", 12, true, "number", "min=0.01 step=0.01") +
      field("Giờ chuẩn bị", "setupHours", "0.05", 12, false, "number", "min=0 step=0.01") +
      field("Phí gia công cơ bản (VND)", "postProcessingFee", "0", 18, false, "number", "min=0 step=1") +
      field("Giá tối thiểu/đơn (VND)", "minimumOrderPrice", "25000", 18, false, "number", "min=0 step=1") +
      field("Layer nhỏ nhất (mm)", "minimumLayerHeight", "0.08", 12, true, "number", "min=0.01 step=0.01") +
      field("Layer lớn nhất (mm)", "maximumLayerHeight", "0.4", 12, true, "number", "min=0.01 step=0.01") +
      field("Infill nhỏ nhất (%)", "minimumInfill", "0", 8, false, "number", "min=0 max=100") +
      field("Infill lớn nhất (%)", "maximumInfill", "100", 8, false, "number", "min=0 max=100") +
      field("Thời gian sản xuất (ngày)", "productionDays", "3", 8, true, "number", "min=1 max=365") +
      '<section class="profile-group span-2"><h3>Thành phần áp dụng trong công thức</h3><div class="form-grid">' +
        check("useMaterialCost", "Tính phí vật liệu") +
        check("useMachineCost", "Tính phí máy") +
        check("useInfillFactor", "Áp dụng infill") +
        check("useLayerHeightFactor", "Áp dụng layer height") +
        check("useTechnologyFactor", "Áp dụng công nghệ") +
        check("useComplexityFactor", "Áp dụng độ phức tạp") +
        check("usePostProcessingFee", "Tính gia công") +
        check("useQuantityDiscount", "Áp dụng chiết khấu số lượng") +
        check("useMinimumPrice", "Áp dụng giá tối thiểu") +
        check("active", "Nhận đặt trực tiếp", true) +
      '</div></section>' +
      '<div class="notice span-2">Giá ước tính dựa trên thông số mô hình, vật liệu, thời gian máy và các hệ số đã bật; đây không phải kết quả slicer.</div>' +
      '<div class="form-actions span-2"><button class="primary" type="submit">Lưu cấu hình giá</button></div>' +
    '</form></details>';
  const cards = (profiles || []).map(profile => {
    const options = (profile.options || []).map(option =>
      '<span class="price-chip">' + esc(option.label) + ' · ' + money(option.fee) + (option.active ? "" : " · Tắt") + '</span>'
    ).join("");
    const discounts = (profile.discounts || []).map(discount =>
      '<span class="price-chip">' + number(discount.minimumQuantity) + '+ · ' + (Number(discount.discountRate) * 100).toFixed(1) + '%</span>'
    ).join("");
    return '<article class="price-profile-card">' +
      '<div class="price-profile-head"><div><span class="eyebrow">CẤU HÌNH GIÁ</span><h3>' +
        esc(profile.printing_method) + ' · ' + esc(profile.material) + '</h3></div>' +
        pill(profile.is_active ? "Đang nhận đơn trực tiếp" : "Đã tắt nhận đơn", profile.is_active ? "good" : "bad") + '</div>' +
      '<div class="price-profile-metrics">' +
        '<div><span>Giá vật liệu</span><strong>' + money(profile.material_price_per_gram) + '<small>/gram</small></strong></div>' +
        '<div><span>Chi phí máy</span><strong>' + money(profile.machine_cost_per_hour) + '<small>/giờ</small></strong></div>' +
        '<div><span>Tốc độ đắp</span><strong>' + number(profile.deposition_rate_cm3_per_hour) + '<small>cm³/giờ</small></strong></div>' +
        '<div><span>Thời gian</span><strong>' + number(profile.production_days) + '<small>ngày</small></strong></div>' +
      '</div>' +
      '<div class="price-profile-foot"><span>Layer: ' + esc(profile.minimum_layer_height_mm) + '–' + esc(profile.maximum_layer_height_mm) + ' mm</span>' +
        '<span>Giá tối thiểu: <b>' + money(profile.minimum_order_price) + '</b></span></div>' +
      '<div class="price-profile-tags"><div><b>Tùy chọn màu / gia công</b><div>' + (options || '<span class="muted">Chưa cấu hình</span>') + '</div></div>' +
        '<div><b>Chiết khấu số lượng</b><div>' + (discounts || '<span class="muted">Chưa cấu hình</span>') + '</div></div></div>' +
      '<details class="editor price-profile-editor"><summary>Thêm / cập nhật tùy chọn màu hoặc gia công</summary>' +
        '<form class="form-grid" data-form="custom-option" data-id="' + esc(profile.profile_id) + '">' +
          '<label>Loại<select name="type"><option value="COLOR">Màu</option><option value="FINISH">Gia công sau in</option></select></label>' +
          field("Mã tùy chọn", "value", "", 100, true) +
          field("Tên hiển thị", "label", "", 120, true) +
          field("Phí (VND)", "fee", "0", 18, true, "number", "min=0 step=1") +
          '<label class="check-line"><input type="checkbox" name="active" value="true" checked> Đang hỗ trợ</label>' +
          '<div class="form-actions span-2"><button class="secondary" type="submit">Lưu tùy chọn</button></div>' +
        '</form></details>' +
      '<details class="editor price-profile-editor"><summary>Thêm / cập nhật chiết khấu số lượng</summary>' +
        '<form class="form-grid" data-form="custom-discount" data-id="' + esc(profile.profile_id) + '">' +
          field("Áp dụng từ số lượng", "minimumQuantity", "5", 8, true, "number", "min=2 max=100000") +
          field("Tỷ lệ (0–0.9)", "discountRate", "0.05", 8, true, "number", "min=0 max=0.9 step=0.01") +
          '<div class="form-actions span-2"><button class="secondary" type="submit">Lưu chiết khấu</button></div>' +
        '</form></details>' +
    '</article>';
  }).join("");
  const summary = '<div class="price-summary-grid">' +
    '<div><span>Tổng cấu hình</span><strong>' + number((profiles || []).length) + '</strong></div>' +
    '<div><span>Đang nhận đặt trực tiếp</span><strong>' + number(activeProfiles) + '</strong></div>' +
    '<div><span>Đã tắt nhận đặt</span><strong>' + number((profiles || []).length - activeProfiles) + '</strong></div>' +
  '</div>';
  return '<div class="custom-print-price-panel">' +
    summary +
    panel("Bảng giá in 3D theo yêu cầu", "Mỗi cấu hình gắn với một công nghệ và vật liệu. Chỉnh sửa hệ số sẽ ảnh hưởng các báo giá ước tính mới.", form +
      '<div class="price-profile-list">' + (cards || empty("Chưa có cấu hình giá in 3D.")) + '</div>') +
  '</div>';
}

function customPrintRequestList(data) {
  const items = Array.isArray(data) ? data : (Array.isArray(data?.items) ? data.items : []);
  const params = new URLSearchParams(window.location.search);
  const statuses = [
    ["ALL", "Tất cả trạng thái"],
    ["PENDING_REVIEW", "Chờ xưởng xử lý"],
    ["QUOTED", "Đã báo giá"],
    ["CUSTOMER_DECLINED", "Khách từ chối"],
    ["EXPIRED", "Đã quá hạn"],
    ["ORDER_CREATED", "Đã tạo đơn"],
    ["CLOSED", "Đã đóng"]
  ];
  const toolbar = '<form class="toolbar" data-filter="custom-print-requests">' +
    '<label class="field">Tìm kiếm<input name="q" value="' + esc(params.get("q") || "") + '" placeholder="Mã yêu cầu, tên file hoặc khách hàng"></label>' +
    '<label class="field">Trạng thái<select name="status">' +
    statuses.map(option => '<option value="' + option[0] + '"' + ((params.get("status") || "ALL") === option[0] ? " selected" : "") + '>' + option[1] + '</option>').join("") +
    '</select></label><button class="secondary" type="submit">Lọc yêu cầu</button>' +
    '<a class="back-link secondary small-button" href="' + esc(workshopUrl("customPrintRequests")) + '">Đặt lại</a></form>';
  const pending = items.filter(item => item.status === "PENDING_REVIEW").length;
  const quoted = items.filter(item => item.status === "QUOTED").length;
  const cards = items.map(item => {
    const label = ({
      PENDING_REVIEW: "Chờ xưởng xử lý",
      QUOTED: "Đã báo giá",
      CUSTOMER_DECLINED: "Khách từ chối",
      EXPIRED: "Đã quá hạn",
      ORDER_CREATED: "Đã tạo đơn",
      CLOSED: "Đã đóng"
    })[item.status] || item.status;
    return '<article class="custom-request-card">' +
      '<div class="custom-request-main">' +
        '<div class="custom-request-heading"><div><span class="request-code">' + esc(item.request_code || ("Yêu cầu #" + item.id)) + '</span>' +
        '<h3>' + esc(item.original_filename || "Mô hình OBJ") + '</h3></div>' + pill(label, statusTone(item.status)) + '</div>' +
        '<div class="custom-request-meta">' +
          '<span><b>Khách hàng</b>' + esc(item.customer_name || "—") + '</span>' +
          '<span><b>Cấu hình in</b>' + esc(item.technology || "—") + ' · ' + esc(item.material || "—") + '</span>' +
          '<span><b>Số lượng</b>' + number(item.quantity) + '</span>' +
          '<span><b>Kích thước</b>' + esc(item.width_mm || "—") + ' × ' + esc(item.depth_mm || "—") + ' × ' + esc(item.height_mm || "—") + ' mm</span>' +
          '<span><b>Ngày gửi</b>' + date(item.created_at) + '</span>' +
        '</div>' +
      '</div>' +
      '<div class="custom-request-pricing">' +
        '<span class="request-price-label">Giá khách tham khảo</span><strong>' + money(item.estimated_total) + '</strong>' +
        (item.quoted_amount != null ? '<span class="request-quoted-price">Giá đã báo: ' + money(item.quoted_amount) + '</span>' : '<span class="request-quoted-price">Chưa có báo giá</span>') +
        '<div class="custom-request-actions">' +
          '<a class="primary small-button request-detail-link" href="' + esc(customPrintRequestUrl(item.id)) + '">Xem chi tiết</a>' +
          '<a class="secondary small-button" href="' + esc(customPrintFileUrl(item.id)) + '">Tải OBJ</a>' +
        '</div>' +
      '</div>' +
    '</article>';
  }).join("");
  const summary = '<div class="custom-request-summary">' +
    '<div><span>Tổng yêu cầu đang hiển thị</span><strong>' + number(items.length) + '</strong></div>' +
    '<div><span>Đang chờ xử lý</span><strong>' + number(pending) + '</strong></div>' +
    '<div><span>Đã gửi báo giá</span><strong>' + number(quoted) + '</strong></div>' +
  '</div>';
  return heading("CUSTOM 3D PRINT", "Yêu cầu in 3D", "Quản lý các tệp OBJ và yêu cầu báo giá mà khách hàng gửi riêng đến xưởng.") +
    summary +
    panel("Danh sách yêu cầu của khách hàng", "Chọn một yêu cầu để xem mô hình 3D và phản hồi báo giá ở trang chi tiết.", toolbar + (cards || empty("Chưa có yêu cầu in 3D phù hợp với bộ lọc.")));
}

function customPrintRequestDetail(item) {
  if (!item || !item.id) return heading("CUSTOM 3D PRINT", "Không tìm thấy yêu cầu", "Yêu cầu không tồn tại hoặc không thuộc xưởng đang chọn.") +
    '<a class="back-link secondary small-button" href="' + esc(workshopUrl("customPrintRequests")) + '">Quay lại danh sách</a>';
  const label = ({
    PENDING_REVIEW: "Chờ xưởng xử lý",
    QUOTED: "Đã báo giá",
    CUSTOMER_DECLINED: "Khách từ chối",
    EXPIRED: "Đã quá hạn",
    ORDER_CREATED: "Đã tạo đơn",
    CLOSED: "Đã đóng"
  })[item.status] || item.status;
  const downloadUrl = customPrintFileUrl(item.id);
  const responseForm = item.status === "PENDING_REVIEW"
    ? '<form class="quote-response custom-print-response" data-form="custom-print-quote" data-id="' + esc(item.id) + '">' +
      '<div class="form-grid">' +
        '<label>Tổng giá báo khách (VND)<input name="amount" type="number" min="0.01" step="0.01" required placeholder="Nhập tổng giá"></label>' +
        '<label>Thời gian sản xuất (ngày)<input name="productionDays" type="number" min="1" max="365" value="3" required></label>' +
        '<label class="span-2">Ghi chú / điều kiện báo giá<textarea name="note" maxlength="500" minlength="8" required rows="3" placeholder="Nêu rõ thời gian, điều kiện gia công hoặc lý do từ chối"></textarea></label>' +
      '</div>' +
      '<div class="form-actions"><button class="primary" type="submit" value="QUOTE">Gửi báo giá cho khách</button><button class="danger" type="submit" value="CLOSE">Từ chối yêu cầu</button></div>' +
    '</form>'
    : '<div class="notice custom-print-response-note"><strong>Yêu cầu đã được xử lý.</strong><p>' + esc(item.seller_response_note || "Không có ghi chú phản hồi.") + '</p>' +
      (item.quoted_amount != null ? '<p>Giá báo: <b>' + money(item.quoted_amount) + '</b> · Thời gian: <b>' + number(item.production_days) + ' ngày</b></p>' : '') + '</div>';
  const dimensions = [item.width_mm, item.depth_mm, item.height_mm].every(value => value != null)
    ? esc(item.width_mm) + ' × ' + esc(item.depth_mm) + ' × ' + esc(item.height_mm) + ' mm'
    : 'Chưa có số đo tin cậy';
  const fileSize = Number(item.file_size_bytes) > 0
    ? (Number(item.file_size_bytes) >= 1048576 ? (Number(item.file_size_bytes) / 1048576).toFixed(2) + " MB" : (Number(item.file_size_bytes) / 1024).toFixed(1) + " KB")
    : "—";
  const note = item.customer_note
    ? '<div class="detail-cell detail-cell-wide"><b>GHI CHÚ CỦA KHÁCH HÀNG</b><p>' + esc(item.customer_note).replace(/\n/g, "<br>") + '</p></div>'
    : '';
  const responseNote = item.seller_response_note
    ? '<div class="detail-cell detail-cell-wide"><b>PHẢN HỒI CỦA XƯỞNG</b><p>' + esc(item.seller_response_note).replace(/\n/g, "<br>") + '</p></div>'
    : '';
  return heading("CUSTOM 3D PRINT", "Chi tiết yêu cầu in 3D", "Kiểm tra mô hình khách hàng tải lên, thông số phân tích và gửi phản hồi ngay tại đây.",
      '<a class="back-link secondary small-button" href="' + esc(workshopUrl("customPrintRequests")) + '">← Danh sách yêu cầu</a>') +
    '<section class="panel custom-print-detail-panel">' +
      '<div class="custom-print-detail-heading"><div><span class="request-code">' + esc(item.request_code || ("Yêu cầu #" + item.id)) + '</span><h2>' + esc(item.original_filename || "Mô hình OBJ") + '</h2>' +
      '<p class="muted">Gửi lúc ' + date(item.created_at) + ' · ' + fileSize + '</p></div>' + pill(label, statusTone(item.status)) + '</div>' +
      '<div class="detail-grid custom-print-detail-grid">' +
        '<div class="detail-cell"><b>KHÁCH HÀNG</b>' + esc(item.customer_name || "—") + '</div>' +
        '<div class="detail-cell"><b>EMAIL</b>' + esc(item.customer_email || "—") + '</div>' +
        '<div class="detail-cell"><b>ĐIỆN THOẠI</b>' + esc(item.customer_phone || "—") + '</div>' +
        '<div class="detail-cell"><b>CÔNG NGHỆ / VẬT LIỆU</b>' + esc(item.technology || "—") + ' · ' + esc(item.material || "—") + '</div>' +
        '<div class="detail-cell"><b>SỐ LƯỢNG</b>' + number(item.quantity) + '</div>' +
        '<div class="detail-cell"><b>INFILL / LAYER HEIGHT</b>' + (item.infill_percent == null ? "—" : number(item.infill_percent) + "%") + ' · ' + esc(item.layer_height_mm || "—") + ' mm</div>' +
        '<div class="detail-cell"><b>KÍCH THƯỚC X × Y × Z</b>' + dimensions + '</div>' +
        '<div class="detail-cell"><b>THỂ TÍCH</b>' + (item.volume_cm3 == null ? "—" : esc(item.volume_cm3) + " cm³") + '</div>' +
        '<div class="detail-cell"><b>TRẠNG THÁI PHÂN TÍCH</b>' + esc(item.analysis_status || "—") + '</div>' +
        '<div class="detail-cell"><b>GIÁ ƯỚC TÍNH</b>' + money(item.estimated_total) + '</div>' +
        '<div class="detail-cell"><b>GIÁ ĐÃ BÁO</b>' + money(item.quoted_amount) + '</div>' +
        '<div class="detail-cell"><b>HẠN BÁO GIÁ</b>' + date(item.quote_valid_until) + '</div>' +
        '<div class="detail-cell"><b>MÀU / GIA CÔNG</b>' + esc(item.color_option || "—") + ' · ' + esc(item.post_processing_option || "—") + '</div>' +
        '<div class="detail-cell"><b>ĐƠN VỊ FILE GỐC</b>' + esc(item.source_unit || "—") + '</div>' +
        note + responseNote +
        (item.analysis_message ? '<div class="detail-cell detail-cell-wide"><b>GHI CHÚ PHÂN TÍCH</b><p>' + esc(item.analysis_message) + '</p></div>' : '') +
      '</div>' +
    '</section>' +
    '<section class="panel custom-model-panel">' +
      '<div class="panel-head"><div><h2>Mô hình 3D khách hàng gửi</h2><p>Xoay, kéo, phóng to và kiểm tra hình học OBJ trực tiếp trước khi báo giá.</p></div>' +
      '<a class="secondary small-button" href="' + esc(downloadUrl) + '">Tải file OBJ</a></div>' +
      '<div class="custom-model-stage"><canvas id="custom-print-model-canvas" aria-label="Mô hình 3D OBJ của khách hàng"></canvas>' +
      '<div class="custom-model-status" id="custom-print-model-status" role="status">Đang tải mô hình 3D...</div></div>' +
      '<div class="custom-model-tools">' +
        '<button class="secondary small-button" type="button" data-action="model-reset">Đặt lại góc nhìn</button>' +
        '<button class="secondary small-button" type="button" data-action="model-zoom-in">Phóng to +</button>' +
        '<button class="secondary small-button" type="button" data-action="model-zoom-out">Thu nhỏ −</button>' +
        '<button class="secondary small-button" type="button" data-action="model-wireframe">Bật / tắt Wireframe</button>' +
      '</div>' +
    '</section>' +
    '<section class="panel custom-print-response-panel"><div class="panel-head"><div><h2>Phản hồi yêu cầu</h2><p>Giá và thời gian được gửi cho khách hàng; trạng thái được cập nhật ở máy chủ.</p></div></div>' +
      responseForm +
    '</section>';
}

function destroyCustomPrintModelViewer() {
  const viewer = state.modelViewer;
  state.modelViewer = null;
  if (!viewer) return;
  if (viewer.frame && typeof window.cancelAnimationFrame === "function") window.cancelAnimationFrame(viewer.frame);
  if (viewer.onResize && typeof window.removeEventListener === "function") window.removeEventListener("resize", viewer.onResize);
  if (viewer.controls && typeof viewer.controls.dispose === "function") viewer.controls.dispose();
  if (viewer.renderer && typeof viewer.renderer.dispose === "function") viewer.renderer.dispose();
}

async function mountCustomPrintModelViewer(requestId) {
  const canvas = root.querySelector("#custom-print-model-canvas");
  const status = root.querySelector("#custom-print-model-status");
  if (!canvas || !status) return;
  const THREE = window.THREE;
  if (!THREE || !THREE.OBJLoader || !THREE.OrbitControls) {
    status.textContent = "Không thể khởi tạo trình xem 3D. Hãy tải file OBJ để mở bằng phần mềm hỗ trợ.";
    return;
  }
  let renderer;
  try {
    renderer = new THREE.WebGLRenderer({ canvas: canvas, antialias: true, alpha: true });
    renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
    renderer.setClearColor(0xf1f5f9, 1);
    const scene = new THREE.Scene();
    scene.background = new THREE.Color(0xf1f5f9);
    const camera = new THREE.PerspectiveCamera(42, 1, 0.01, 100000);
    camera.position.set(180, 140, 180);
    scene.add(new THREE.AmbientLight(0xffffff, 1.15));
    const keyLight = new THREE.DirectionalLight(0xffffff, 1.1);
    keyLight.position.set(120, 180, 150);
    scene.add(keyLight);
    const fillLight = new THREE.DirectionalLight(0xffffff, 0.45);
    fillLight.position.set(-120, 60, -100);
    scene.add(fillLight);
    const controls = new THREE.OrbitControls(camera, renderer.domElement);
    controls.enableDamping = true;
    controls.dampingFactor = 0.07;
    controls.target.set(0, 0, 0);
    const viewer = { renderer: renderer, scene: scene, camera: camera, controls: controls, object: null, frame: 0, onResize: null, wireframe: false, initialPosition: new THREE.Vector3(180, 140, 180) };
    state.modelViewer = viewer;
    const resize = () => {
      if (state.modelViewer !== viewer) return;
      const bounds = canvas.getBoundingClientRect();
      const width = Math.max(320, bounds.width || canvas.clientWidth || 640);
      const height = Math.max(280, bounds.height || canvas.clientHeight || 440);
      renderer.setSize(width, height, false);
      camera.aspect = width / height;
      camera.updateProjectionMatrix();
    };
    viewer.onResize = resize;
    resize();
    const response = await fetch(customPrintFileUrl(requestId), {
      method: "GET",
      credentials: "same-origin",
      cache: "no-store",
      headers: { Accept: "text/plain, application/octet-stream" }
    });
    if (state.modelViewer !== viewer) return;
    if (!response.ok) throw new Error(response.status === 404 ? "Không tìm thấy file OBJ hoặc file không thuộc xưởng này." : "Không thể tải file OBJ của khách hàng.");
    const source = await response.text();
    if (state.modelViewer !== viewer) return;
    if (!source.trim()) throw new Error("File OBJ không có dữ liệu hình học.");
    const object = new THREE.OBJLoader().parse(source);
    let meshCount = 0;
    object.traverse(child => {
      if (!child.isMesh) return;
      meshCount += 1;
      child.material = new THREE.MeshStandardMaterial({
        color: 0x8795ad,
        metalness: 0.12,
        roughness: 0.52,
        side: THREE.DoubleSide,
        wireframe: false
      });
      if (child.geometry && typeof child.geometry.computeVertexNormals === "function") child.geometry.computeVertexNormals();
    });
    if (!meshCount) throw new Error("OBJ không chứa mesh có thể hiển thị.");
    const bounds = new THREE.Box3().setFromObject(object);
    const size = bounds.getSize(new THREE.Vector3());
    const center = bounds.getCenter(new THREE.Vector3());
    const maxSize = Math.max(size.x, size.y, size.z);
    if (!Number.isFinite(maxSize) || maxSize <= 0) throw new Error("Không xác định được kích thước mô hình OBJ.");
    object.position.sub(center);
    scene.add(object);
    viewer.object = object;
    const distance = maxSize * 1.8;
    viewer.initialPosition.set(distance, distance * 0.72, distance);
    camera.near = Math.max(maxSize / 10000, 0.001);
    camera.far = Math.max(maxSize * 100, 1000);
    camera.position.copy(viewer.initialPosition);
    camera.updateProjectionMatrix();
    controls.minDistance = Math.max(maxSize * 0.08, 0.1);
    controls.maxDistance = maxSize * 30;
    controls.target.set(0, 0, 0);
    controls.update();
    status.textContent = "Đã tải mô hình · " + meshCount + " thành phần";
    status.classList.add("is-hidden");
    const animate = () => {
      if (state.modelViewer !== viewer) return;
      viewer.frame = window.requestAnimationFrame(animate);
      controls.update();
      renderer.render(scene, camera);
    };
    if (typeof window.requestAnimationFrame === "function") animate();
    if (typeof window.addEventListener === "function") window.addEventListener("resize", resize);
  } catch (error) {
    if (state.modelViewer?.renderer === renderer) destroyCustomPrintModelViewer();
    status.classList.remove("is-hidden");
    status.textContent = error.message || "Không thể hiển thị mô hình 3D.";
  }
}

function handleCustomModelViewerAction(action, control) {
  const viewer = state.modelViewer;
  if (!viewer) return;
  if (action === "model-reset") {
    viewer.camera.position.copy(viewer.initialPosition);
    viewer.controls.target.set(0, 0, 0);
    viewer.controls.update();
  } else if (action === "model-zoom-in" || action === "model-zoom-out") {
    const factor = action === "model-zoom-in" ? 0.82 : 1.22;
    viewer.camera.position.sub(viewer.controls.target).multiplyScalar(factor).add(viewer.controls.target);
    viewer.controls.update();
  } else if (action === "model-wireframe") {
    viewer.wireframe = !viewer.wireframe;
    if (viewer.object) viewer.object.traverse(child => {
      if (child.isMesh && child.material) child.material.wireframe = viewer.wireframe;
    });
    control.textContent = viewer.wireframe ? "Tắt Wireframe" : "Bật / tắt Wireframe";
  }
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
  const latestUpdate = (item.productionUpdates || []).slice(-1)[0];
  const currentStage = latestUpdate?.stage || null;
  const currentProgress = Math.max(0, Math.min(100, Number(latestUpdate?.progressPercent || 0)));
  const stageLabels = { ACCEPTED: "Tiếp nhận", PRINTING: "Đang in", POST_PROCESSING: "Gia công sau in", READY_FOR_SHIPPING: "Sẵn sàng giao" };
  const progressOverview = `<section class="order-detail-progress">
    <div class="order-detail-progress-head"><div><span class="detail-kicker">TIẾN ĐỘ SẢN XUẤT</span><h3>${esc(stageLabels[currentStage] || "Chưa bắt đầu sản xuất")}</h3><p>${latestUpdate ? "Cập nhật gần nhất: " + date(latestUpdate.createdAt) : "Xưởng chưa ghi nhận lần cập nhật nào."}</p></div><strong>${number(currentProgress)}%</strong></div>
    <div class="order-progress-track large" role="progressbar" aria-label="Tiến độ sản xuất hiện tại" aria-valuemin="0" aria-valuemax="100" aria-valuenow="${currentProgress}"><span class="${currentProgress >= 100 ? "is-ready" : currentProgress > 0 ? "is-active" : "is-waiting"}" style="width:${currentProgress}%"></span></div>
    <div class="order-stage-steps"><span class="${currentProgress >= 0 ? "done" : ""}">Tiếp nhận</span><span class="${currentProgress >= 55 ? "done" : ""}">Đang in</span><span class="${currentProgress >= 85 ? "done" : ""}">Gia công</span><span class="${currentProgress >= 100 ? "done" : ""}">Sẵn sàng giao</span></div>
  </section>`;
  const nextStages = {
    ACCEPTED: ["PRINTING"],
    PRINTING: ["POST_PROCESSING"],
    POST_PROCESSING: ["READY_FOR_SHIPPING"],
    READY_FOR_SHIPPING: []
  };
  const nextStage = currentStage == null ? "ACCEPTED" : (nextStages[currentStage] || [])[0];
  const stageMilestones = {
    ACCEPTED: { label: "Tiếp nhận", progress: 0 },
    PRINTING: { label: "Đang in", progress: 55 },
    POST_PROCESSING: { label: "Gia công sau in", progress: 85 },
    READY_FOR_SHIPPING: { label: "Sẵn sàng giao", progress: 100 }
  };
  const stageOptions = Object.entries(stageMilestones).map(([value, stage]) =>
    '<option value="' + value + '"' + (value === nextStage ? " selected" : "") +
    (value === nextStage ? "" : " disabled") + '>' + stage.label + ' · ' + stage.progress + '%</option>'
  ).join("");
  const stageHint = '<small class="progress-stage-hint">Mốc phần trăm được hệ thống tự gán theo giai đoạn: Tiếp nhận 0% · Đang in 55% · Gia công 85% · Sẵn sàng giao 100%.</small>';
  const progressForm = canUpdate && nextStage
      ? '<form class="progress-form" data-form="progress" data-id="' + esc(item.id) + '">' +
        '<label class="progress-stage-field">Giai đoạn sản xuất<select name="stage" required>' + stageOptions + '</select></label>' +
        '<label class="progress-message-field">Ghi chú (không bắt buộc)<input name="message" maxlength="500" placeholder="Ví dụ: Đã hoàn thành 2/3 sản phẩm"></label>' +
        etaInput +
        '<div class="form-actions progress-submit"><button class="primary small-button" type="submit">Cập nhật tiến độ</button></div>' +
        stageHint +
      '</form>'
      : (canUpdate
          ? '<p class="notice success">Đơn hàng đã ở bước sẵn sàng giao. Không còn giai đoạn sản xuất tiếp theo để cập nhật.</p>'
          : '<p class="muted">Chỉ cập nhật tiến độ sau khi đơn được xác nhận và đủ điều kiện thanh toán.</p>');
  const fileDownload=item.designFileName?`<a class="secondary small-button" href="${contextPath}/workshop/api/custom-print-files/orders/${encodeURIComponent(item.id)}?shopId=${encodeURIComponent(state.shopId)}">Tải file thiết kế</a>`:"";
  const customPrintDetails=item.customPrintTechnology?`<div class="detail-cell"><b>CÔNG NGHỆ / VẬT LIỆU</b>${esc(item.customPrintTechnology)} · ${esc(item.customPrintMaterial)}</div><div class="detail-cell"><b>INFILL / LAYER</b>${number(item.customPrintInfill)}% · ${esc(item.customPrintLayerHeight)} mm</div><div class="detail-cell"><b>MÀU / GIA CÔNG</b>${esc(item.customPrintColor||"—")} · ${esc(item.customPrintFinish||"—")}</div><div class="detail-cell"><b>THỜI GIAN DỰ KIẾN</b>${number(item.customPrintProductionDays)} ngày</div>`:"";
  return `<div class="modal-backdrop" data-action="modal-backdrop"><section class="modal" role="dialog" aria-modal="true" aria-labelledby="detail-title"><header class="modal-head"><div><h2 id="detail-title">${esc(item.code)} · ${esc(item.productName)}</h2><p class="muted">Chi tiết chỉ thuộc dòng hàng của xưởng hiện tại.</p></div><button type="button" class="close-button" data-action="modal-close" aria-label="Đóng">×</button></header><div class="detail-grid"><div class="detail-cell"><b>TRẠNG THÁI ĐƠN</b>${pill(item.orderStatus, statusTone(item.orderStatus))}</div><div class="detail-cell"><b>THANH TOÁN</b>${pill(item.paymentStatus, statusTone(item.paymentStatus))}</div><div class="detail-cell"><b>SỐ LƯỢNG / ĐƠN GIÁ</b>${number(item.quantity)} × ${money(item.unitPrice)}</div>${customPrintDetails}<div class="detail-cell"><b>KHÁCH HÀNG</b>${esc(item.customerName || "Chưa hiển thị trước khi xác nhận")}</div><div class="detail-cell"><b>NGƯỜI NHẬN</b>${esc(item.recipientName || "—")}</div><div class="detail-cell"><b>ĐIỆN THOẠI</b>${esc(item.recipientPhone || "—")}</div><div class="detail-cell"><b>ĐỊA CHỈ GIAO</b>${esc(item.shippingAddress || "—")}</div><div class="detail-cell"><b>TỆP THIẾT KẾ</b>${esc(item.designFileName || "Không có tệp được lưu")} ${fileDownload}</div></div>${progressOverview}<div class="order-detail-history"><section><h3>Lịch sử trạng thái đơn</h3><div class="timeline">${statusHistory || empty("Chưa có lịch sử trạng thái.")}</div></section><section><h3>Lịch sử tiến độ xưởng</h3><div class="timeline">${updates || empty("Chưa có cập nhật sản xuất.")}</div></section></div><section class="order-update-panel"><div class="order-update-heading"><div><span class="detail-kicker">WORKSHOP ACTION</span><h3>Cập nhật tiến độ</h3><p>Chọn giai đoạn tiếp theo; hệ thống tự xác định phần trăm hoàn thành.</p></div></div>${progressForm}</section></section></div>`;
}

async function load() {
  state.loading = true; state.error = ""; render();
  try {
    if (!state.actor) {
      state.actor = await apiGet("/session", false);
      state.shops = state.actor.shops || [];
    }
    syncUrlParams();
    const requested = urlParams.get("shopId");
    state.shopId = state.shops.some(shop => String(shop.id) === requested) ? requested : String(state.shops[0]?.id || "");
    if (!state.shopId) throw new Error("Tài khoản chưa có xưởng liên kết. Hãy gửi đăng ký xưởng để Admin xác minh.");
    const params = new URLSearchParams(); params.set("shopId", state.shopId);
    for (const key of ["q", "status", "category", "page", "size"]) {
      const value = urlParams.get(key);
      if (value && !(state.page === "changeRequests" && key === "status" && value === "ALL")) params.set(key, value);
    }
    const detailId = customPrintRequestIdFromPath();
    if (state.page === "customPrintRequestDetail" && !detailId) throw new Error("Mã yêu cầu in 3D không hợp lệ.");
    const endpoints = { dashboard: "/dashboard", profile: "/profile", products: "/offers", orders: "/orders", quotes: "/quotes", customPrintRequests: "/custom-print-requests", messages: "/messages", comments: "/comments", changeRequests: "/change-requests", reviews: "/reviews" };
    const endpoint = state.page === "customPrintRequestDetail" ? "/custom-print-requests/" + encodeURIComponent(detailId) : endpoints[state.page];
    if (!endpoint) throw new Error("Không tìm thấy trang xưởng.");
    const response = await fetch(`${contextPath}/workshop/api${endpoint}?${params}`, { credentials: "same-origin", cache: "no-store", headers: { Accept: "application/json" } });
    state.data = await readResponse(response);
    state.priceProfiles = [];
    state.messageThread = [];
    if (state.page === "products") {
      const priceResponse=await fetch(`${contextPath}/workshop/api/custom-print-pricing?shopId=${encodeURIComponent(state.shopId)}`,{credentials:"same-origin",cache:"no-store",headers:{Accept:"application/json"}});
      state.priceProfiles=await readResponse(priceResponse);
    } else if (state.page === "messages" && urlParams.get("orderId")) {
      const selectedOrderId = urlParams.get("orderId");
      state.messageThread = await apiGet(`/messages/${encodeURIComponent(selectedOrderId)}`);
      try {
        state.activeEscalation = await apiGet(`/messages/${encodeURIComponent(selectedOrderId)}/escalation`);
      } catch {
        state.activeEscalation = null;
      }
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
    updateNavigationBadges();
  } catch {}
}
function startWorkshopNotificationPolling() {
  if (workshopNotificationTimerStarted || typeof window.setInterval !== "function") return;
  workshopNotificationTimerStarted = true;
  window.setInterval(refreshWorkshopNotifications, 30000);
}

async function loadMessageThread(orderId) {
  state.messageThreadLoading = true;
  const list = Array.isArray(state.data) ? state.data : (Array.isArray(state.data?.items) ? state.data.items : []);
  const selectedThread = list.find(thread => String(thread.orderId) === String(orderId));
  if (selectedThread) selectedThread.unread = 0;
  render();
  try {
    const [thread, escalation] = await Promise.all([
      apiGet(`/messages/${encodeURIComponent(orderId)}`),
      apiGet(`/messages/${encodeURIComponent(orderId)}/escalation`).catch(() => null)
    ]);
    state.messageThread = Array.isArray(thread) ? thread : [];
    state.activeEscalation = escalation;
  } catch (err) {
    toast(err.message || "Không thể tải tin nhắn hội thoại.", true);
  } finally {
    state.messageThreadLoading = false;
    render();
  }
}

function navigateTo(urlStr) {
  let url;
  try {
    url = new URL(urlStr, window.location.href);
  } catch {
    if (typeof window !== "undefined" && typeof window.location?.assign === "function") {
      window.location.assign(urlStr);
    }
    return;
  }

  if (url.origin !== window.location.origin || !url.pathname.includes("/workshop")) {
    if (typeof window !== "undefined" && typeof window.location?.assign === "function") {
      window.location.assign(urlStr);
    }
    return;
  }

  const newPage = resolveRoute(url.pathname);
  if (typeof window !== "undefined" && window.history && typeof window.history.pushState === "function") {
    window.history.pushState(null, "", url.href);
  } else if (typeof window !== "undefined" && window.location) {
    window.location.href = url.href;
    return;
  }

  syncUrlParams();
  if (newPage === "messages" && state.page === "messages" && state.data) {
    const selectedOrderId = urlParams.get("orderId");
    if (selectedOrderId) {
      loadMessageThread(selectedOrderId);
    } else {
      state.messageThread = [];
      state.activeEscalation = null;
      render();
    }
    return;
  }

  state.page = newPage;
  state.loading = true;
  state.data = null;
  state.modal = null;
  state.error = "";
  render();
  load();
}

if (typeof window !== "undefined" && typeof window.addEventListener === "function") {
  window.addEventListener("popstate", () => {
    syncUrlParams();
    const newPage = resolveRoute(window.location.pathname);
    if (newPage === "messages" && state.page === "messages" && state.data) {
      const selectedOrderId = urlParams.get("orderId");
      if (selectedOrderId) {
        loadMessageThread(selectedOrderId);
      } else {
        state.messageThread = [];
        state.activeEscalation = null;
        render();
      }
      return;
    }
    state.page = newPage;
    state.loading = true;
    state.data = null;
    state.modal = null;
    state.error = "";
    render();
    load();
  });
}

async function submitMutation(path, values, message) {
  const buttons = root.querySelectorAll(".product-dialog-footer button");
  if (state.productDialog) { state.productDialog.submitting = true; buttons.forEach(button => button.disabled = true); const submit = root.querySelector(".product-dialog-footer button[type='submit']"); if (submit) submit.textContent = "Đang lưu…"; }
  try { await apiPost(path, values); state.modal = null; state.productDialog = null; toast(message); await load().then(startWorkshopNotificationPolling); }
  catch (error) { if (state.productDialog) state.productDialog.submitting = false; buttons.forEach(button => button.disabled = false); toast(error.message || "Không thể lưu thay đổi.", true); }
}
function navigateFilter(form) {
  const url = new URL(window.location.href);
  const params = new URLSearchParams();
  params.set("shopId", state.shopId);
  for (const [key, value] of new FormData(form).entries()) if (String(value).trim()) params.set(key, String(value).trim());
  url.search = params.toString();
  navigateTo(url.toString());
}

root.addEventListener("change", event => {
  if (event.target.id === "shop-switcher") {
    const url = new URL(window.location.href);
    url.searchParams.set("shopId", event.target.value);
    url.searchParams.delete("page");
    navigateTo(url.toString());
  }
});
root.addEventListener("click", async event => {
  const link = event.target?.closest?.("a.nav-link, a.brand, a.back-link, a.message-thread, a.request-detail-link");
  if (link && link.getAttribute) {
    const href = link.getAttribute("href") || "";
    const isSpecialClick = event.ctrlKey || event.metaKey || event.shiftKey || event.altKey || (event.button !== undefined && event.button !== 0);
    if (!isSpecialClick && href && !href.startsWith("#") && !href.startsWith("javascript:")) {
      if (typeof event.preventDefault === "function") event.preventDefault();
      navigateTo(link.href || href);
      return;
    }
  }

  const backdrop = event.target?.closest?.("[data-action='product-dialog-backdrop']");
  if (backdrop && event.target === backdrop) { if (!state.productDialog?.submitting) { state.productDialog = null; render(); } return; }
  const control = event.target?.closest?.("[data-action]");
  if (!control) return;
  const action = control.dataset.action;
  if (action === "product-dialog-create") { state.productDialog = { type: "create" }; render(); root.querySelector(".product-dialog")?.focus(); return; }
  if (action === "product-dialog-offer") { state.productDialog = { type: "offer", id: control.dataset.id }; render(); root.querySelector(".product-dialog")?.focus(); return; }
  if (action === "product-dialog-content") { state.productDialog = { type: "content", id: control.dataset.id }; render(); root.querySelector(".product-dialog")?.focus(); return; }
  if (action === "product-dialog-close") { if (!state.productDialog?.submitting) { state.productDialog = null; render(); } return; }
  if (action === "clear-offer-filter") { const url = new URL(window.location.href); ["q","status","category","page"].forEach(key => url.searchParams.delete(key)); url.searchParams.set("shopId", state.shopId); navigateTo(url.toString()); return; }
  if (action === "retry") { await load(); return;
  if (action === "model-reset" || action === "model-zoom-in" || action === "model-zoom-out" || action === "model-wireframe") {
    handleCustomModelViewerAction(action, control);
    return;
  }
  if (action === "offer-active") {
    await submitMutation(`/offers/${encodeURIComponent(control.dataset.id)}/active`, { active: control.dataset.active }, "Đã cập nhật trạng thái chào bán.");
  } else if (action === "page") {
    const url = new URL(window.location.href);
    url.searchParams.set("page", control.dataset.page);
    url.searchParams.set("shopId", state.shopId);
    navigateTo(url.toString());
  } else if (action === "order-detail") {
    try { state.modal = await apiGet(`/orders/${encodeURIComponent(control.dataset.id)}`); render(); }
    catch (error) { toast(error.message || "Không thể tải chi tiết đơn.", true); }
  } else if (action === "fill-reply") {
    const form = control.closest("form");
    const textarea = form?.querySelector('textarea[name="content"]');
    if (textarea) {
      textarea.value = control.dataset.text || "";
      textarea.focus();
    }
  } else if (action === "modal-close" || (action === "modal-backdrop" && event.target === control)) {
    state.modal = null; render();
  }
});
document.addEventListener("keydown", event => { if (event.key === "Escape" && state.productDialog && !state.productDialog.submitting) { state.productDialog = null; render(); } });\nroot.addEventListener("submit", async event => {
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
    if (typeof form?.reset === "function") form.reset();
    return submitMutation(`/messages/${encodeURIComponent(form.dataset.orderId)}`, { message }, "Đã gửi tin nhắn.");
  }
  if (type === "escalate") {
    const reason = String(values.reason || "").trim();
    if (!reason) { toast("Vui lòng nhập lý do yêu cầu can thiệp.", true); return; }
    return submitMutation(`/messages/${encodeURIComponent(form.dataset.orderId)}/escalate`, { reason }, "Đã gửi yêu cầu Admin can thiệp.");
  }
  if (type === "change-request") {
    const action = event.submitter?.value || "REJECTED";
    if (action === "REJECTED" && !String(values.note || "").trim()) {
      toast("Hãy ghi rõ lý do từ chối để khách hàng hiểu quyết định của xưởng.", true); return;
    }
    return submitMutation(`/change-requests/${encodeURIComponent(form.dataset.id)}/respond`,
      { ...values, action }, action === "ACCEPTED" ? "Đã chấp nhận yêu cầu thay đổi." : "Đã từ chối yêu cầu thay đổi.");
  }
  if (type === "comment-reply") {
    const content = String(values.content || "").trim();
    if (content.length < 2) {
      toast("Nội dung câu trả lời cần tối thiểu 2 ký tự.", true);
      return;
    }
    return submitMutation(`/comments/${encodeURIComponent(form.dataset.id)}/reply`, { content }, "Đã gửi câu trả lời cho khách hàng.");
  }
  if (type === "progress") {
    if (!values.stage) { toast("Vui lòng chọn giai đoạn sản xuất.", true); return; }
    return submitMutation(`/orders/items/${encodeURIComponent(form.dataset.id)}/progress`, values, "Đã lưu cập nhật tiến độ.");
  }
});

load().then(startWorkshopNotificationPolling);
