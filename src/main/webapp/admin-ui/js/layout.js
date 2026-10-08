import { icon, esc } from "./components.js";

const navGroups = [
  { label: "Không gian", items: [{ key: "dashboard", label: "Dashboard", icon: "grid", route: "/admin" }] },
  { label: "Quản lý", items: [
    { key: "users", label: "Người dùng", icon: "users", route: "/admin/users" },
    { key: "sellers", label: "Seller / Xưởng in", icon: "shop", route: "/admin/sellers" },
    { key: "products", label: "Sản phẩm", icon: "cube", route: "/admin/products", count: 2 },
    { key: "orders", label: "Đơn hàng", icon: "box", route: "/admin/orders" }
  ] },
  { label: "Tài chính", items: [
    { key: "revenue", label: "Doanh thu", icon: "chart", route: "/admin/revenue" },
    { key: "deposits", label: "Tiền cọc Seller", icon: "wallet", route: "/admin/deposits" },
    { key: "commission", label: "Commission", icon: "spark", route: "/admin/commission" }
  ] },
  { label: "Phân tích", items: [
    { key: "analytics", label: "Thống kê", icon: "chart", route: "/admin/analytics" },
    { key: "map", label: "Bản đồ", icon: "map", route: "/admin/map" }
  ] },
  { label: "Hệ thống", items: [{ key: "settings", label: "Cấu hình Marketplace", icon: "settings", route: "/admin/settings" }] }
];

const titles = {
  dashboard: "Dashboard", users: "Người dùng", sellers: "Seller / Xưởng in", products: "Sản phẩm", orders: "Đơn hàng",
  revenue: "Doanh thu", deposits: "Tiền cọc Seller", commission: "Commission", analytics: "Thống kê", map: "Bản đồ", settings: "Cấu hình Marketplace"
};

export function currentRoute(contextPath = "") {
  let path = window.location.pathname;
  if (contextPath && path.startsWith(contextPath)) path = path.slice(contextPath.length);
  if (path === "/admin/" || path === "/admin") return "dashboard";
  const slug = path.split("/").filter(Boolean).pop();
  return titles[slug] ? slug : "not-found";
}

export function routePath(key) {
  const item = navGroups.flatMap(group => group.items).find(entry => entry.key === key);
  return item?.route || "/admin";
}

export function renderLayout(route, content, contextPath = "", collapsed = false) {
  const activeRoute = route === "dashboard" ? "dashboard" : route;
  const sidebar = navGroups.map(group => `<div class="nav-group"><div class="nav-label">${esc(group.label)}</div>${group.items.map(item => `<a class="nav-item ${activeRoute === item.key ? "active" : ""}" href="${contextPath}${item.route}" data-nav="${item.key}" title="${esc(item.label)}"><span class="nav-icon">${icon(item.icon)}</span><span class="nav-text">${esc(item.label)}</span>${item.count ? `<span class="nav-count">${item.count}</span>` : ""}</a>`).join("")}</div>`).join("");
  const title = titles[route] || "Không tìm thấy trang";
  return `<div class="admin-shell ${collapsed ? "sidebar-collapsed" : ""}">
    <aside class="admin-sidebar" id="admin-sidebar">
      <a class="brand" href="${contextPath}/admin" data-nav="dashboard"><span class="brand-mark"><i></i><i></i><i></i></span><span class="brand-copy"><strong>print3d<span>.</span></strong><small>MARKETPLACE ADMIN</small></span></a>
      <button class="workspace-switch" data-action="workspace" aria-label="Đổi workspace"><span class="workspace-cube">${icon("cube")}</span><span class="workspace-copy"><small>Không gian làm việc</small><strong>Marketplace tổng</strong></span>${icon("down")}</button>
      <nav class="sidebar-nav" aria-label="Điều hướng chính">${sidebar}</nav>
      <div class="sidebar-bottom"><div class="sidebar-tip"><span class="tip-cube">${icon("spark")}</span><strong>Xưởng in đang phát triển</strong><p>Thêm công cụ để theo dõi marketplace của bạn.</p><button data-nav="analytics">Xem phân tích ${icon("arrow")}</button><span class="tip-block tip-one"></span><span class="tip-block tip-two"></span></div><div class="sidebar-admin">${icon("settings")}<span>Admin workspace</span><span class="online-dot"></span></div></div>
    </aside>
    <div class="sidebar-scrim" data-action="close-sidebar"></div>
    <main class="admin-main">
      <header class="admin-header">
        <div class="header-left"><button class="icon-button menu-toggle" data-action="menu-toggle" aria-label="Thu gọn menu">${icon("menu")}</button><div class="breadcrumbs"><span>Print3D</span>${icon("chevron")}<strong>${esc(title)}</strong></div></div>
        <div class="header-actions"><form class="header-search" data-form="header-search">${icon("search")}<input type="search" name="query" placeholder="Tìm kiếm nhanh…" aria-label="Tìm kiếm nhanh"><kbd>⌘ K</kbd></form><span class="header-divider"></span><button class="icon-button notification-button" data-action="notifications" aria-label="Thông báo">${icon("bell")}<i></i></button><button class="admin-profile" data-action="profile"><span class="admin-avatar">LT</span><span class="admin-profile-copy"><strong>Linh Trần</strong><small>Platform Admin</small></span>${icon("down")}</button></div>
      </header>
      <div class="page-content">${content}<footer class="page-footer"><span>Print3D Marketplace</span><span>Admin workspace <i class="online-dot"></i></span><span>Dữ liệu demo</span></footer></div>
    </main>
    <div id="modal-root"></div><div id="toast-root" aria-live="polite" aria-atomic="true"></div>
  </div>`;
}

export const routeTitles = titles;
