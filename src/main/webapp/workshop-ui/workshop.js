const root = document.getElementById("workshop-app");
const contextPath = root?.dataset.contextPath || "";
const csrfToken = document.querySelector('meta[name="csrf-token"]')?.content || "";
const routes = {
  dashboard: { path: "/dashboard", title: "Tổng quan" },
  profile: { path: "/profile", title: "Hồ sơ & năng lực" },
  products: { path: "/products", title: "Sản phẩm & bảng giá" },
  orders: { path: "/orders", title: "Đơn hàng & tiến độ" },
  quotes: { path: "/quotes", title: "Yêu cầu báo giá" },
  reviews: { path: "/reviews", title: "Đánh giá" }
};
const icons = {
  dashboard: '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><rect x="3.5" y="3.5" width="7" height="7" rx="1.5"/><rect x="13.5" y="3.5" width="7" height="7" rx="1.5"/><rect x="3.5" y="13.5" width="7" height="7" rx="1.5"/><rect x="13.5" y="13.5" width="7" height="7" rx="1.5"/></svg>',
  profile: '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><circle cx="12" cy="8" r="3.4"/><path d="M5.2 20c.6-3.5 3-5.3 6.8-5.3s6.2 1.8 6.8 5.3"/><circle cx="12" cy="12" r="9"/></svg>',
  products: '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="m12 3 8 4.5v9L12 21l-8-4.5v-9L12 3Z"/><path d="m4.5 7.8 7.5 4.4 7.5-4.4M12 12.2V21"/></svg>',
  orders: '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M4 7.5h16v12H4zM8 7.5V5h8v2.5M4 12h16M9 12v2h6v-2"/></svg>',
  quotes: '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M20 11.5a7.5 7.5 0 0 1-8 7.5 8.6 8.6 0 0 1-3.7-.8L4 20l1.2-3.8A7 7 0 0 1 4 11.5 7.5 7.5 0 0 1 12 4a7.5 7.5 0 0 1 8 7.5Z"/><path d="M8.5 11.5h.01M12 11.5h.01M15.5 11.5h.01"/></svg>',
  reviews: '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="m12 3 2.7 5.5 6.1.9-4.4 4.3 1 6.1-5.4-2.9-5.4 2.9 1-6.1-4.4-4.3 6.1-.9L12 3Z"/></svg>',
  active: '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M4 7h16M4 12h16M4 17h10"/><circle cx="18" cy="17" r="2.5"/></svg>',
  completed: '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><circle cx="12" cy="12" r="8.5"/><path d="m8.3 12.2 2.4 2.4 5-5.2"/></svg>',
  cancelled: '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><circle cx="12" cy="12" r="8.5"/><path d="m9 9 6 6m0-6-6 6"/></svg>'
};
const routePath = window.location.pathname.replace(/\/$/, "") || "/workshop/dashboard";
const page = Object.keys(routes).find(key => routePath.endsWith(routes[key].path)) || "dashboard";
const urlParams = new URLSearchParams(window.location.search);
const state = { page, shopId: "", shops: [], data: null, actor: null, loading: true, error: "", modal: null };

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
  if (!response.ok) throw new Error(payload.error || "Yêu cầu chưa hoàn tất.");
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
  const nav = Object.entries(routes).map(([key, item]) =>
      `<a class="nav-link ${state.page === key ? "active" : ""}" href="${esc(workshopUrl(key))}"><span class="nav-icon">${icons[key]}</span>${esc(item.title)}</a>`).join("");
  const selector = state.shops.length > 1 ? `<div class="shop-switch"><label for="shop-switcher">Xưởng đang quản lý</label><select id="shop-switcher">${state.shops.map(shop => `<option value="${esc(shop.id)}" ${String(shop.id) === String(state.shopId) ? "selected" : ""}>${esc(shop.name)}</option>`).join("")}</select></div>` : `<div class="shop-switch"><label>Xưởng của bạn</label><strong>${esc(activeShop.name || "Xưởng in")}</strong></div>`;
  const initials = (state.actor?.name || activeShop.name || "X").trim().slice(0, 1).toUpperCase();
  return `<div class="shell"><aside class="sidebar"><a class="brand" href="${esc(workshopUrl("dashboard"))}"><span class="brand-mark">P</span> PrintKraft</a>${selector}<div class="nav-title">WORKSHOP PORTAL</div><nav class="nav-list">${nav}</nav><div class="sidebar-bottom">Quản lý dữ liệu thuộc xưởng của bạn.<form method="post" action="${contextPath}/logout" class="logout-form"><input type="hidden" name="csrfToken" value="${esc(document.querySelector('meta[name=\"csrf-token\"]')?.content || "")}"><button type="submit">Đăng xuất</button></form></div></aside><main class="main"><header class="topbar"><div class="topbar-title">${esc(routes[state.page].title)}</div><div class="topbar-user"><span>${esc(state.actor?.name || "Chủ xưởng")}</span><span class="avatar">${esc(initials)}</span></div></header><section class="content">${state.data?.shop ? statusBanner(state.data.shop) : ""}${content}</section></main></div>${state.modal ? modalMarkup() : ""}`;
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
  if (state.page === "products") return offers(data);
  if (state.page === "orders") return orders(data);
  if (state.page === "quotes") return quotes(data);
  if (state.page === "reviews") return reviews(data);
  return empty("Không tìm thấy trang.");
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
        ${field("Địa chỉ hoạt động", "addressLine", p.addressLine, 255)}${field("Tỉnh / thành phố", "province", p.province, 100)}${field("Quận / huyện", "district", p.district, 100)}${field("Thời gian hoạt động", "operatingHours", p.operatingHours, 255)}
      </div></section>
      <section class="profile-group span-2"><h3>Năng lực & sản xuất</h3><div class="form-grid">
        ${textarea("Mô tả xưởng", "description", p.description, 4000)}${textarea("Công nghệ, máy in, vật liệu, màu và hoàn thiện hỗ trợ", "capabilities", p.capabilities, 4000)}${field("Định dạng tệp nhận", "acceptedFileFormats", p.acceptedFileFormats, 255)}${field("Kích thước in tối đa", "maxBuildDimensions", p.maxBuildDimensions, 120)}${field("Thời gian xử lý dự kiến (ngày)", "estimatedLeadDays", p.estimatedLeadDays, 3, false, "number", "min=0 max=365")}${textarea("Giới hạn / ghi chú dịch vụ", "serviceNotes", p.serviceNotes, 4000)}
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
  const cards = (data.items || []).map(item => `<article class="order-card"><div class="order-card-head"><div><h3>${esc(item.code)} · ${esc(item.productName)}</h3><p>SL ${number(item.quantity)} · ${money(item.lineTotal)} · đặt ${date(item.placedAt)}</p><p>${item.customerName ? `Khách hàng: ${esc(item.customerName)} · ` : ""}${esc(item.latestStage || "Chưa cập nhật tiến độ")}${item.latestProgress != null ? ` · ${number(item.latestProgress)}%` : ""}</p></div><div>${pill(item.orderStatus, statusTone(item.orderStatus))}</div></div><div class="form-actions"><button type="button" class="secondary small-button" data-action="order-detail" data-id="${esc(item.id)}">Chi tiết & tiến độ</button></div></article>`).join("");
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
function modalMarkup() {
  if (!state.modal) return "";
  const item = state.modal;
  const updates = (item.productionUpdates || []).map(update => `<div class="timeline-item"><strong>${esc(update.stage)} · ${number(update.progressPercent)}%</strong><small>${date(update.createdAt)} · ${esc(update.createdBy || "Xưởng")}${update.message ? ` — ${esc(update.message)}` : ""}</small></div>`).join("");
  const statusHistory = (item.orderStatusHistory || []).map(update => `<div class="timeline-item"><strong>${esc(update.oldStatus || "Bắt đầu")} → ${esc(update.newStatus)}</strong><small>${date(update.changedAt)}${update.note ? ` — ${esc(update.note)}` : ""}</small></div>`).join("");
  const canUpdate = ["Đã xác nhận", "Đang xử lý"].includes(item.orderStatus)
      && ["Đã thanh toán", "COD (thanh toán khi nhận hàng)"].includes(item.paymentStatus);
  const progressForm = canUpdate ? `<form class="progress-form" data-form="progress" data-id="${esc(item.id)}"><select name="stage"><option value="ACCEPTED">Tiếp nhận</option><option value="PRINTING">Đang in</option><option value="POST_PROCESSING">Gia công sau in</option><option value="READY_FOR_SHIPPING">Sẵn sàng giao</option></select><input type="number" name="progress" min="0" max="100" placeholder="% tiến độ" required><input name="message" maxlength="500" placeholder="Ghi chú (tuỳ chọn)"><button class="primary small-button" type="submit">Cập nhật</button></form>` : `<p class="muted">Chỉ cập nhật tiến độ sau khi đơn được xác nhận và đủ điều kiện thanh toán.</p>`;
  return `<div class="modal-backdrop" data-action="modal-backdrop"><section class="modal" role="dialog" aria-modal="true" aria-labelledby="detail-title"><header class="modal-head"><div><h2 id="detail-title">${esc(item.code)} · ${esc(item.productName)}</h2><p class="muted">Chi tiết chỉ thuộc dòng hàng của xưởng hiện tại.</p></div><button type="button" class="close-button" data-action="modal-close" aria-label="Đóng">×</button></header><div class="detail-grid"><div class="detail-cell"><b>TRẠNG THÁI ĐƠN</b>${pill(item.orderStatus, statusTone(item.orderStatus))}</div><div class="detail-cell"><b>THANH TOÁN</b>${pill(item.paymentStatus, statusTone(item.paymentStatus))}</div><div class="detail-cell"><b>SỐ LƯỢNG / ĐƠN GIÁ</b>${number(item.quantity)} × ${money(item.unitPrice)}</div><div class="detail-cell"><b>KHÁCH HÀNG</b>${esc(item.customerName || "Chưa hiển thị trước khi xác nhận")}</div><div class="detail-cell"><b>NGƯỜI NHẬN</b>${esc(item.recipientName || "—")}</div><div class="detail-cell"><b>ĐIỆN THOẠI</b>${esc(item.recipientPhone || "—")}</div><div class="detail-cell"><b>ĐỊA CHỈ GIAO</b>${esc(item.shippingAddress || "—")}</div><div class="detail-cell"><b>TỆP THIẾT KẾ</b>${esc(item.designFileName || "Không có tệp được lưu")}</div></div><h3>Lịch sử trạng thái đơn</h3><div class="timeline">${statusHistory || empty("Chưa có lịch sử trạng thái.")}</div><h3>Lịch sử tiến độ xưởng</h3><div class="timeline">${updates || empty("Chưa có cập nhật sản xuất.")}</div>${progressForm}</section></div>`;
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
      const value = urlParams.get(key); if (value) params.set(key, value);
    }
    const endpoint = ({ dashboard: "/dashboard", profile: "/profile", products: "/offers", orders: "/orders", quotes: "/quotes", reviews: "/reviews" })[state.page];
    const response = await fetch(`${contextPath}/workshop/api${endpoint}?${params}`, { credentials: "same-origin", cache: "no-store", headers: { Accept: "application/json" } });
    state.data = await readResponse(response);
  } catch (error) { state.error = error.message || "Không thể tải dữ liệu xưởng."; }
  finally { state.loading = false; render(); }
}
async function submitMutation(path, values, message) {
  try { await apiPost(path, values); state.modal = null; toast(message); await load(); }
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
  if (type === "new-offer") return submitMutation("/offers", values, "Đã tạo sản phẩm chờ Admin kiểm duyệt.");
  if (type === "offer") return submitMutation(`/offers/${encodeURIComponent(form.dataset.id)}`, values, "Đã lưu bảng giá chào bán.");
  if (type === "product-content") return submitMutation(`/products/${encodeURIComponent(form.dataset.id)}`, values, "Đã lưu nội dung và gửi lại kiểm duyệt.");
  if (type === "quote") {
    const action = event.submitter?.value || "QUOTE";
    if (action === "CLOSE" && String(values.note || "").trim().length < 8) { toast("Lý do đóng yêu cầu cần ít nhất 8 ký tự.", true); return; }
    if (action === "QUOTE" && !(Number(values.amount) > 0)) { toast("Nhập mức báo giá lớn hơn 0.", true); return; }
    return submitMutation(`/quotes/${encodeURIComponent(form.dataset.id)}/respond`, { ...values, action }, action === "QUOTE" ? "Đã gửi báo giá cho khách hàng." : "Đã đóng yêu cầu và lưu lý do.");
  }
  if (type === "progress") {
    if (!Number.isInteger(Number(values.progress)) || Number(values.progress) < 0 || Number(values.progress) > 100) { toast("Tiến độ phải từ 0 đến 100.", true); return; }
    return submitMutation(`/orders/items/${encodeURIComponent(form.dataset.id)}/progress`, values, "Đã lưu cập nhật tiến độ.");
  }
});

load();
