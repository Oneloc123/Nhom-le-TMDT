import { esc, icon } from "./components.js";

const navigation = [
  { key: "dashboard", label: "Dashboard", icon: "grid", href: "/admin" },
  { key: "users", label: "Người dùng", icon: "users", href: "/admin/users" },
  { key: "sellers", label: "Seller / Xưởng in", icon: "shop", href: "/admin/sellers" },
  { key: "products", label: "Sản phẩm", icon: "cube", href: "/admin/products" },
  { key: "orders", label: "Đơn hàng", icon: "box", href: "/admin/orders" }
];

const titles = Object.fromEntries(navigation.map(item => [item.key, item.label]));

export function currentRoute(contextPath = "") {
  let path = window.location.pathname;
  if (contextPath && path.startsWith(contextPath)) path = path.slice(contextPath.length);
  if (path === "/admin" || path === "/admin/") return "dashboard";
  const slug = path.split("/").filter(Boolean).pop();
  return titles[slug] ? slug : "not-found";
}

export function routePath(key) {
  return navigation.find(item => item.key === key)?.href || "/admin";
}

export function renderLayout(route, content, contextPath = "", collapsed = false) {
  const title = titles[route] || "Không tìm thấy trang";
  const sidebar = navigation.map(item => `<a class="nav-item ${route === item.key ? "active" : ""}"
      href="${contextPath}${item.href}" title="${esc(item.label)}" ${route === item.key ? 'aria-current="page"' : ""}>
      <span class="nav-icon">${icon(item.icon)}</span><span class="nav-text">${esc(item.label)}</span></a>`).join("");
  return `<div class="admin-shell ${collapsed ? "sidebar-collapsed" : ""}">
    <aside class="admin-sidebar" id="admin-sidebar">
      <a class="brand" href="${contextPath}/admin"><span class="brand-mark"><i></i><i></i><i></i></span>
        <span class="brand-copy"><strong>print3d<span>.</span></strong><small>MARKETPLACE ADMIN</small></span></a>
      <div class="workspace-switch"><span class="workspace-cube">${icon("cube")}</span>
        <span class="workspace-copy"><small>Quản trị vận hành</small><strong>Admin MVP</strong></span></div>
      <nav class="sidebar-nav" aria-label="Điều hướng chính"><div class="nav-group">
        <div class="nav-label">Quản lý marketplace</div>${sidebar}</div></nav>
      <div class="sidebar-bottom"><div class="sidebar-admin">${icon("settings")}<span>Admin workspace</span><span class="online-dot"></span></div></div>
    </aside>
    <main class="admin-main">
      <header class="admin-header"><div class="header-left"><div class="breadcrumbs"><span>Print3D</span>${icon("chevron")}<strong>${esc(title)}</strong></div></div></header>
      <div class="page-content">${content}<footer class="page-footer"><span>Print3D Marketplace</span><span>Admin MVP</span></footer></div>
    </main>
    <div id="modal-root"></div><div id="toast-root" aria-live="polite" aria-atomic="true"></div>
  </div>`;
}

export const routeTitles = titles;
