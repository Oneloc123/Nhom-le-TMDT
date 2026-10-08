import { users, sellers, products, orders, transactions, commissionRules, sampleSettings } from "./data.js";
import { badge, esc, icon, money } from "./components.js";
import { currentRoute, renderLayout, routePath, routeTitles } from "./layout.js";
import { renderDashboard } from "./pages/dashboard.js";
import { collectionForType, lookupEntity, renderEntityDetail, renderOrders, renderProducts, renderSellers, renderUsers } from "./pages/management.js";
import { renderCommission, renderDeposits, renderRevenue } from "./pages/finance.js";
import { renderAnalytics, renderMap } from "./pages/insights.js";
import { renderSettings } from "./pages/settings.js";

const root = document.getElementById("admin-app");
const contextPath = root.dataset.contextPath || "";
const storageKey = "print3d-admin-demo-v1";
let saved = {};
try { saved = JSON.parse(localStorage.getItem(storageKey) || "{}"); } catch { saved = {}; }

const state = {
  route: currentRoute(contextPath), collapsed: false, search: "", page: 1, filters: {}, sortKey: "", sortDir: 1,
  range: "30 days", analyticsRange: "30D", mapFilter: "Sellers", technologyFilter: "All",
  commissionRate: Number(saved.commissionRate ?? 10),
  commissionRules: Array.isArray(saved.commissionRules) ? saved.commissionRules : [...commissionRules],
  settings: { ...sampleSettings, ...(saved.settings || {}), commissionRate: Number(saved.commissionRate ?? saved.settings?.commissionRate ?? 10) },
  modal: null
};

function persist() {
  try { localStorage.setItem(storageKey, JSON.stringify({ commissionRate: state.commissionRate, commissionRules: state.commissionRules, settings: state.settings })); } catch { /* Local storage may be unavailable; the session still works. */ }
}

function pageContent() {
  try {
    switch (state.route) {
      case "dashboard": return renderDashboard(state);
      case "users": return renderUsers(state);
      case "sellers": return renderSellers(state);
      case "products": return renderProducts(state);
      case "orders": return renderOrders(state);
      case "revenue": return renderRevenue(state);
      case "deposits": return renderDeposits(state);
      case "commission": return renderCommission(state);
      case "analytics": return renderAnalytics(state);
      case "map": return renderMap(state);
      case "settings": return renderSettings(state);
      default: return `<section class="not-found-state"><span class="empty-illustration">${icon("cube")}</span><div class="eyebrow">404 · ADMIN ROUTE</div><h1>Trang này chưa có trong workspace</h1><p>Chọn một module trong menu để tiếp tục.</p><a class="button" href="${contextPath}/admin" data-nav="dashboard">Về Dashboard</a></section>`;
    }
  } catch (error) {
    console.error("Admin page render failed", error);
    return `<section class="error-state"><span class="empty-illustration">${icon("warning")}</span><div class="eyebrow">KHÔNG THỂ TẢI TRANG</div><h1>Đã xảy ra lỗi khi hiển thị dữ liệu</h1><p>Thử tải lại module. Dữ liệu demo của các trang khác vẫn được giữ nguyên.</p><button class="button" data-action="retry-render">Tải lại trang</button></section>`;
  }
}

function render() {
  root.innerHTML = renderLayout(state.route, pageContent(), contextPath, state.collapsed);
}

function resetPageState() {
  state.search = "";
  state.page = 1;
  state.filters = {};
  state.sortKey = "";
  state.sortDir = 1;
}

function navigate(route) {
  const key = routeTitles[route] ? route : "dashboard";
  const path = `${contextPath}${routePath(key)}`;
  if (window.location.pathname !== path) window.history.pushState({}, "", path);
  state.route = key;
  resetPageState();
  if (window.innerWidth <= 1080) state.collapsed = false;
  render();
  window.scrollTo({ top: 0, behavior: "smooth" });
}

function showToast(message, type = "success") {
  const host = document.getElementById("toast-root");
  if (!host) return;
  host.className = "toast-stack";
  host.innerHTML = `<div class="toast ${type === "error" ? "error" : ""}" role="status"><span class="toast-mark">${icon(type === "error" ? "warning" : "check")}</span><span>${esc(message)}</span></div>`;
  window.setTimeout(() => { if (host.isConnected) host.innerHTML = ""; }, 3000);
}

function closeModal() {
  const host = document.getElementById("modal-root");
  if (host) host.innerHTML = "";
  state.modal = null;
}

function showModal(title, content, { subtitle = "Dữ liệu mẫu · bản xem trước", large = false, actions = "" } = {}) {
  const host = document.getElementById("modal-root");
  if (!host) return;
  state.modal = true;
  host.innerHTML = `<div class="modal-backdrop" data-modal-backdrop><section class="modal-card ${large ? "large" : ""}" role="dialog" aria-modal="true" aria-labelledby="modal-title"><header class="modal-head"><div><h2 id="modal-title">${esc(title)}</h2>${subtitle ? `<p>${esc(subtitle)}</p>` : ""}</div><button class="icon-button" data-action="modal-close" aria-label="Đóng hộp thoại">${icon("close")}</button></header><div class="modal-body">${content}${actions ? `<div class="modal-actions">${actions}</div>` : ""}</div></section></div>`;
  host.querySelector("[data-action='modal-close']")?.focus();
}

function entityType(item) {
  if (users.includes(item)) return "user";
  if (sellers.includes(item)) return "seller";
  if (products.includes(item)) return "product";
  if (orders.includes(item)) return "order";
  return "record";
}

function showEntity(item) {
  const type = entityType(item);
  const title = type === "user" ? "Hồ sơ khách hàng" : type === "seller" ? "Hồ sơ seller / xưởng in" : type === "product" ? "Chi tiết sản phẩm" : type === "order" ? "Chi tiết đơn hàng · chỉ xem" : "Chi tiết";
  const large = ["seller", "product", "order"].includes(type);
  showModal(title, renderEntityDetail(type, item), { subtitle: `${item.id} · Dữ liệu demo`, large });
}

function confirmStatus(type, item) {
  const suspending = item.status === "Active";
  const noun = type === "user" ? "tài khoản khách hàng" : "seller";
  const nextStatus = suspending ? "Suspended" : "Active";
  showModal(suspending ? `Tạm ngưng ${noun}?` : `Kích hoạt ${noun}?`, `<div class="confirm-copy"><span class="confirm-icon ${suspending ? "danger" : "success"}">${icon(suspending ? "warning" : "check")}</span><p>${suspending ? `Bạn sẽ tạm ngưng ${type === "user" ? "tài khoản" : "hoạt động của xưởng"} <strong>${esc(type === "user" ? item.name : item.name)}</strong>.` : `Bạn sẽ kích hoạt lại <strong>${esc(type === "user" ? item.name : item.name)}</strong>.`}</p><p class="muted-note">Thay đổi chỉ áp dụng trong phiên demo hiện tại.</p></div>`, { subtitle: "Xác nhận thay đổi local state", actions: `<button class="button secondary" data-action="modal-close">Hủy</button><button class="button ${suspending ? "danger" : ""}" data-action="confirm-status" data-type="${type}" data-record="${item.id}" data-next-status="${nextStatus}">${suspending ? "Tạm ngưng" : "Kích hoạt"}</button>` });
}

function productRejectModal(product) {
  showModal("Từ chối sản phẩm", `<p class="muted-note">Nhập lý do rõ ràng để seller biết cần điều chỉnh mục nào. Admin không sửa nội dung sản phẩm thay seller.</p><form class="reject-form" data-form="reject-product" data-record="${product.id}"><label class="form-field">Lý do từ chối<textarea name="reason" required minlength="8" placeholder="Ví dụ: Vui lòng bổ sung ảnh chụp sản phẩm thực tế và ghi rõ vật liệu in."></textarea></label><div class="modal-actions"><button class="button secondary" type="button" data-action="modal-close">Hủy</button><button class="button danger" type="submit">Gửi lý do &amp; từ chối</button></div></form>`, { subtitle: `${product.name} · ${product.id}` });
  hostFocus();
}

function hostFocus() { document.querySelector("#modal-root textarea")?.focus(); }

function globalSearch(query) {
  const needle = query.trim().toLocaleLowerCase("vi");
  if (!needle) return showToast("Nhập nội dung tìm kiếm trước.", "error");
  const groups = [
    ["Khách hàng", "users", users.filter(row => [row.id, row.name, row.email, row.phone].some(value => String(value).toLocaleLowerCase("vi").includes(needle)))],
    ["Seller", "sellers", sellers.filter(row => [row.id, row.name, row.ownerName, row.location].some(value => String(value).toLocaleLowerCase("vi").includes(needle)))],
    ["Sản phẩm", "products", products.filter(row => [row.id, row.name, row.category].some(value => String(value || "").toLocaleLowerCase("vi").includes(needle)))],
    ["Đơn hàng", "orders", orders.filter(row => [row.id, row.status].some(value => String(value).toLocaleLowerCase("vi").includes(needle)))]
  ];
  const results = groups.flatMap(([label, type, items]) => items.map(item => ({ label, type, item })));
  const content = results.length ? `<div class="global-results">${results.slice(0, 12).map(({ label, type, item }) => `<button class="global-result" data-action="global-result" data-record="${item.id}" data-type="${type}"><span class="global-result-icon">${icon(type === "users" ? "users" : type === "sellers" ? "shop" : type === "products" ? "cube" : "box")}</span><span><strong>${esc(item.name || item.id)}</strong><small>${label} · ${esc(item.id)}</small></span>${icon("chevron")}</button>`).join("")}</div>` : `<div class="empty-state"><span class="empty-illustration">${icon("search")}</span><strong>Không có kết quả phù hợp</strong><p>Thử mã đơn, tên shop, email hoặc tên sản phẩm khác.</p></div>`;
  showModal("Tìm kiếm marketplace", content, { subtitle: `Kết quả demo cho “${query}”`, large: true });
}

function createCsv(kind) {
  const sets = {
    users: [["User ID", "Name", "Email", "Phone", "Orders", "Total spent", "Joined", "Status"], ...users.map(row => [row.id, row.name, row.email, row.phone, row.orders, row.totalSpent, row.joined, row.status])],
    sellers: [["Seller ID", "Shop", "Owner", "Location", "Products", "Orders", "Revenue", "Rating", "Deposit", "Status"], ...sellers.map(row => [row.id, row.name, row.ownerName, row.location, row.products, row.orders, row.revenue, row.rating, row.deposit, row.status])],
    products: [["Product ID", "Name", "Seller", "Type", "Category", "Price", "Status", "Created"], ...products.map(row => [row.id, row.name, row.sellerId, row.type, row.category, row.price, row.status, row.created])],
    orders: [["Order ID", "Customer", "Seller", "Type", "Amount", "Payment", "Status", "Created"], ...orders.map(row => [row.id, row.customerId, row.sellerId, row.type, row.amount, row.payment, row.status, row.created])],
    transactions: [["Transaction ID", "Order ID", "Customer", "Seller", "Gross", "Commission", "Seller revenue", "Refund", "Date", "Status"], ...transactions.map(row => [row.id, row.orderId, row.customerId, row.sellerId, row.gross, row.commission, row.sellerRevenue, row.refund, row.date, row.status])],
    deposits: [["Seller ID", "Shop", "Current deposit", "Required deposit", "Status"], ...sellers.map(row => [row.id, row.name, row.deposit, row.requiredDeposit, row.deposit >= row.requiredDeposit ? "Sufficient" : "Insufficient"])]
  };
  const rows = sets[kind] || [];
  const csv = `\uFEFF${rows.map(row => row.map(cell => `"${String(cell).replaceAll('"', '""')}"`).join(",")).join("\r\n")}`;
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
  const link = document.createElement("a");
  link.href = URL.createObjectURL(blob);
  link.download = `print3d-${kind}-demo.csv`;
  link.click();
  URL.revokeObjectURL(link.href);
  showToast("Đã tạo CSV từ dữ liệu mẫu.");
}

function mapSellerModal(seller) {
  showModal(seller.name, `<div class="detail-intro">${badge(seller.status, seller.status === "Active" ? "success" : "muted")}<span><strong>${esc(seller.location)}</strong><small>${esc(seller.technology.join(" · "))}</small></span></div><div class="detail-grid">${[["Đơn hàng", seller.orders], ["Doanh thu", money(seller.revenue)], ["Đánh giá", `★ ${seller.rating.toFixed(1)} (${seller.reviews})`], ["Sản phẩm", seller.products]].map(([label, value]) => `<div class="detail-field"><small>${label}</small><strong>${value}</strong></div>`).join("")}</div>`, { subtitle: "Vị trí mock trên bản đồ marketplace" });
}

function mapOrdersModal(city) {
  const group = city === "Hà Nội" ? { count: 5, value: 2640000, dominant: "Custom print" } : city === "Đà Nẵng" ? { count: 3, value: 1790000, dominant: "Ready-made" } : { count: 8, value: 4860000, dominant: "Mixed" };
  showModal(`Đơn hàng · ${city}`, `<div class="detail-grid"><div class="detail-field"><small>Số lượng đơn trong cụm</small><strong>${group.count} đơn</strong></div><div class="detail-field"><small>Tổng giá trị mẫu</small><strong>${money(group.value)}</strong></div><div class="detail-field wide"><small>Loại đơn chiếm ưu thế</small><strong>${group.dominant}</strong></div></div><p class="muted-note">Cụm đơn hàng được gom theo khu vực seller phục vụ, không đại diện tọa độ giao hàng thực.</p>`, { subtitle: "Order distribution · mock data" });
}

root.addEventListener("click", event => {
  const nav = event.target.closest("[data-nav]");
  if (nav) { event.preventDefault(); navigate(nav.dataset.nav); return; }
  const pageButton = event.target.closest("[data-page]");
  if (pageButton) { state.page = Number(pageButton.dataset.page); render(); return; }
  const sortButton = event.target.closest("[data-sort]");
  if (sortButton) { const key = sortButton.dataset.sort; state.sortDir = state.sortKey === key ? -state.sortDir : 1; state.sortKey = key; render(); return; }
  const rangeButton = event.target.closest("[data-analytics-range]");
  if (rangeButton) { state.analyticsRange = rangeButton.dataset.analyticsRange; render(); return; }
  const mapFilterButton = event.target.closest("[data-map-filter]");
  if (mapFilterButton) { state.mapFilter = mapFilterButton.dataset.mapFilter; render(); return; }
  const scrollButton = event.target.closest("[data-settings-scroll]");
  if (scrollButton) { document.getElementById(scrollButton.dataset.settingsScroll)?.scrollIntoView({ behavior: "smooth", block: "start" }); return; }
  const modalBackdrop = event.target.closest("[data-modal-backdrop]");
  if (modalBackdrop && event.target === modalBackdrop) { closeModal(); return; }
  const button = event.target.closest("[data-action]");
  if (!button) return;
  const { action, record, type } = button.dataset;
  if (action === "menu-toggle") { state.collapsed = !state.collapsed; render(); return; }
  if (action === "close-sidebar") { state.collapsed = false; render(); return; }
  if (action === "modal-close") { closeModal(); return; }
  if (action === "notifications") { showModal("Thông báo", `<div class="notice-list"><div><span class="notice-dot orange"></span><span><strong>${products.filter(item => item.status === "Pending Review").length} sản phẩm chờ review</strong><small>Catalog · Cần kiểm tra trước khi public</small></span></div><div><span class="notice-dot violet"></span><span><strong>2 seller cần theo dõi tiền cọc</strong><small>Đối tác · Dữ liệu tham khảo trong phiên demo</small></span></div><div><span class="notice-dot green"></span><span><strong>Đơn hàng đang giao được cập nhật</strong><small>Monitoring · Admin chỉ xem trạng thái</small></span></div></div>`, { subtitle: "Cập nhật trong marketplace demo" }); return; }
  if (action === "profile") { showModal("Tài khoản quản trị", `<div class="detail-intro"><span class="admin-avatar profile-large">LT</span><span><strong>Linh Trần</strong><small>Platform Admin · Mock profile</small></span></div><p class="muted-note">Đây chỉ là hồ sơ hiển thị mẫu. Project chưa triển khai authentication hoặc phân quyền thật.</p>`, { subtitle: "Admin workspace" }); return; }
  if (action === "workspace") { showToast("Đang ở workspace Print3D Marketplace."); return; }
  if (action === "retry-render") { render(); return; }
  if (action === "view") { const item = lookupEntity(record); if (item) showEntity(item); return; }
  if (action === "user-toggle") { const item = users.find(row => row.id === record); if (item) confirmStatus("user", item); return; }
  if (action === "seller-toggle") { const item = sellers.find(row => row.id === record); if (item) confirmStatus("seller", item); return; }
  if (action === "confirm-status") {
    const collection = collectionForType(type);
    const item = collection.find(row => row.id === record);
    if (item) { item.status = button.dataset.nextStatus; closeModal(); render(); showToast(`Đã cập nhật trạng thái ${item.id} trong phiên demo.`); }
    return;
  }
  if (action === "product-approve") {
    const product = products.find(row => row.id === record);
    if (product?.status === "Pending Review") { product.status = "Published"; render(); showToast("Sản phẩm đã được public trong phiên demo."); }
    return;
  }
  if (action === "product-reject") { const product = products.find(row => row.id === record); if (product) productRejectModal(product); return; }
  if (action === "product-hide" || action === "product-unhide") {
    const product = products.find(row => row.id === record);
    if (product) { product.status = action === "product-hide" ? "Hidden" : "Published"; render(); showToast(action === "product-hide" ? "Sản phẩm đã được ẩn trong demo." : "Sản phẩm đã được hiển thị trong demo."); }
    return;
  }
  if (action === "map-seller") { const seller = sellers.find(row => row.id === record); if (seller) mapSellerModal(seller); return; }
  if (action === "map-orders") { mapOrdersModal(record); return; }
  if (action === "global-result") { const item = lookupEntity(record); closeModal(); if (item) showEntity(item); return; }
  if (action === "export") { createCsv(button.dataset.export); return; }
  if (action === "settings-toggle") { state.settings.requireProductReview = !state.settings.requireProductReview; persist(); render(); showToast("Đã cập nhật cài đặt review local."); return; }
  if (action === "add-setting-tag") { showModal("Thêm tùy chọn", `<form class="tag-add-form" data-form="settings-tag" data-tag-group="${esc(button.dataset.tagGroup)}"><label class="form-field">Tên tùy chọn<input name="value" required maxlength="40" placeholder="Nhập tên công nghệ, vật liệu hoặc bề mặt"></label><div class="modal-actions"><button class="button secondary" type="button" data-action="modal-close">Hủy</button><button class="button" type="submit">Thêm vào cấu hình</button></div></form>`, { subtitle: "Giá trị chỉ được lưu cục bộ" }); document.querySelector("#modal-root input")?.focus(); return; }
  if (action === "remove-setting-tag") {
    const group = button.dataset.tagGroup;
    state.settings[group] = (state.settings[group] || []).filter(item => item !== button.dataset.tag);
    persist(); render(); showToast("Đã cập nhật danh sách tùy chọn."); return;
  }
});

root.addEventListener("change", event => {
  const target = event.target;
  if (target.matches("[data-filter-key]")) { state.filters[target.dataset.filterKey] = target.value; state.page = 1; render(); return; }
  if (target.matches("[data-range]")) { state.range = target.value; render(); return; }
  if (target.matches("[data-range-date]")) { state[target.dataset.rangeDate === "start" ? "customStart" : "customEnd"] = target.value; render(); return; }
  if (target.matches("[data-dashboard-range]")) { state.range = target.value; render(); return; }
  if (target.matches("[data-map-filter-select]")) { state.mapFilter = target.value; render(); return; }
  if (target.matches("[data-map-technology]")) { state.technologyFilter = target.value; render(); return; }
});

root.addEventListener("input", event => {
  if (event.target.matches("[data-commission-rate]")) {
    const rate = Math.min(100, Math.max(0, Number(event.target.value) || 0));
    const gmv = 28600000;
    const values = document.querySelectorAll(".commission-projection strong");
    if (values[0]) values[0].textContent = money(gmv * rate / 100);
    if (values[1]) values[1].textContent = money(gmv - gmv * rate / 100);
  }
});

root.addEventListener("submit", event => {
  const form = event.target.closest("form[data-form]");
  if (!form) return;
  event.preventDefault();
  const formType = form.dataset.form;
  const data = new FormData(form);
  if (formType === "page-search") { state.search = String(data.get("search") || "").trim(); state.page = 1; render(); return; }
  if (formType === "header-search") { globalSearch(String(data.get("query") || "")); return; }
  if (formType === "reject-product") {
    const product = products.find(row => row.id === form.dataset.record);
    const reason = String(data.get("reason") || "").trim();
    if (!product || reason.length < 8) { showToast("Vui lòng nhập lý do từ 8 ký tự trở lên.", "error"); return; }
    product.status = "Rejected";
    product.rejectionReason = reason;
    closeModal(); render(); showToast("Đã từ chối sản phẩm và lưu lý do trong phiên demo."); return;
  }
  if (formType === "commission") {
    const rate = Number(data.get("rate"));
    if (!Number.isFinite(rate) || rate < 0 || rate > 100) { showToast("Tỷ lệ commission cần nằm từ 0 đến 100%.", "error"); return; }
    state.commissionRate = rate;
    state.settings.commissionRate = rate;
    const allRule = state.commissionRules.find(rule => rule.id === "COM-01");
    if (allRule) allRule.rate = rate;
    persist(); render(); showToast("Đã lưu tỷ lệ commission demo ở local state."); return;
  }
  if (formType === "commission-rule") {
    const scopeId = String(data.get("scope") || "All sellers");
    const seller = sellers.find(row => row.id === scopeId);
    const rate = Number(data.get("rate"));
    if (!Number.isFinite(rate) || rate < 0 || rate > 100) { showToast("Tỷ lệ cần nằm từ 0 đến 100%.", "error"); return; }
    const productType = String(data.get("productType") || "All products");
    state.commissionRules.push({ id: `COM-${String(Date.now()).slice(-5)}`, name: seller ? `Quy tắc · ${seller.name}` : productType === "All products" ? "Quy tắc tùy chỉnh" : `Quy tắc · ${productType === "READY-MADE" ? "Ready-made" : "Custom print"}`, scope: seller ? seller.name : "All sellers", productType, rate, enabled: false });
    persist(); render(); showToast("Đã thêm quy tắc commission demo."); return;
  }
  if (formType === "settings") {
    const commissionRate = Number(data.get("commissionRate"));
    const requiredDeposit = Number(data.get("requiredDeposit"));
    if (!Number.isFinite(commissionRate) || commissionRate < 0 || commissionRate > 100 || !Number.isFinite(requiredDeposit) || requiredDeposit < 0) { showToast("Kiểm tra tỷ lệ commission (0–100%) và tiền cọc không âm.", "error"); return; }
    state.settings.platformName = String(data.get("platformName") || sampleSettings.platformName).trim();
    state.settings.currency = String(data.get("currency") || sampleSettings.currency);
    state.settings.timezone = String(data.get("timezone") || sampleSettings.timezone);
    state.settings.requiredDeposit = requiredDeposit;
    state.settings.commissionRate = commissionRate;
    state.commissionRate = state.settings.commissionRate;
    state.settings.requireProductReview = document.querySelector("[data-action='settings-toggle']")?.getAttribute("aria-checked") === "true";
    persist(); render(); showToast("Đã lưu cấu hình marketplace vào local state."); return;
  }
  if (formType === "settings-tag") {
    const value = String(data.get("value") || "").trim();
    const group = form.dataset.tagGroup;
    if (!value || !["printingTechnologies", "materials", "surfaceQualities"].includes(group)) return;
    state.settings[group] = [...new Set([...(state.settings[group] || []), value])];
    persist(); closeModal(); render(); showToast("Đã thêm tùy chọn vào cấu hình.");
  }
});

window.addEventListener("popstate", () => { state.route = currentRoute(contextPath); resetPageState(); render(); });
window.addEventListener("keydown", event => {
  if (event.key === "Escape" && state.modal) { closeModal(); return; }
  const typing = ["INPUT", "TEXTAREA", "SELECT"].includes(document.activeElement?.tagName);
  if (!typing && event.key === "/") { event.preventDefault(); document.querySelector("#page-search")?.focus(); }
  if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") { event.preventDefault(); document.querySelector(".header-search input")?.focus(); }
  if (event.key === "Tab" && state.modal) {
    const dialog = document.querySelector(".modal-card");
    const focusable = [...(dialog?.querySelectorAll("button:not(:disabled), input:not(:disabled), select:not(:disabled), textarea:not(:disabled), a[href]") || [])];
    if (!focusable.length) return;
    const first = focusable[0], last = focusable.at(-1);
    if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
    else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
  }
});

render();
