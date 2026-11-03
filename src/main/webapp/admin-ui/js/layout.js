import { esc, icon, initials } from "./components.js";

const csrfToken = document.querySelector('meta[name="csrf-token"]')?.content || "";

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

export function renderLayout(route, content, contextPath = "", collapsed = false, actor = null) {
  const title = titles[route] || "Không tìm thấy trang";
  const actorName = actor?.name || "";
  const displayName = actorName || "Tài khoản quản trị";
  const actorInitials = actorName ? initials(actorName) : "A";
  const actorRole = actor?.role === "ADMIN" ? "Quản trị viên" : (actor?.role || "");
  const sidebar = navigation.map(item => `<a class="nav-item ${route === item.key ? "active" : ""}"
      href="${esc(contextPath)}${item.href}" title="${esc(item.label)}" ${route === item.key ? 'aria-current="page"' : ""}>
      <span class="nav-icon">${icon(item.icon)}</span><span class="nav-text">${esc(item.label)}</span></a>`).join("");
  return `<div class="admin-shell ${collapsed ? "sidebar-collapsed" : ""}">
    <aside class="admin-sidebar" id="admin-sidebar">
      <a class="brand" href="${esc(contextPath)}/admin"><span class="brand-mark"><i></i><i></i><i></i></span>
        <span class="brand-copy"><strong>PrintKraft<span>.</span></strong><small>MARKETPLACE ADMIN</small></span></a>
      <div class="workspace-switch"><span class="workspace-cube">${icon("cube")}</span>
        <span class="workspace-copy"><small>Quản trị vận hành</small><strong>Marketplace</strong></span></div>
      <nav class="sidebar-nav" aria-label="Điều hướng chính"><div class="nav-group">
        <div class="nav-label">Quản lý marketplace</div>${sidebar}</div></nav>
      <div class="sidebar-bottom"><div class="sidebar-admin"><span class="admin-avatar sidebar-avatar" aria-hidden="true">${esc(actorInitials)}</span>
        <span class="sidebar-admin-copy"><strong>${esc(displayName)}</strong>${actorRole ? `<small>${esc(actorRole)}</small>` : ""}</span></div>
        <form class="logout-form" method="post" action="${esc(contextPath)}/logout">
          <input type="hidden" name="csrfToken" value="${esc(csrfToken)}">
          <button class="logout-button" type="submit" aria-label="Đăng xuất" title="Đăng xuất">${icon("logout")}<span>Đăng xuất</span></button>
        </form></div>
    </aside>
    <main class="admin-main">
      <header class="admin-header"><div class="header-left"><button class="icon-button menu-toggle" type="button" data-action="toggle-sidebar"
          aria-label="${collapsed ? "Đóng" : "Mở"} menu điều hướng" aria-controls="admin-sidebar" aria-expanded="${collapsed}">${icon("menu")}</button>
          <div class="breadcrumbs"><span>PrintKraft</span>${icon("chevron")}<strong>${esc(title)}</strong></div></div>
        <div class="header-actions"><span class="header-divider" aria-hidden="true"></span><div class="admin-profile">
          <span class="admin-avatar" aria-hidden="true">${esc(actorInitials)}</span><span class="admin-profile-copy"><strong>${esc(displayName)}</strong>
            ${actorRole ? `<small>${esc(actorRole)}</small>` : ""}</span></div></div></header>
      <div class="page-content">${content}<footer class="page-footer"><span>PrintKraft Marketplace</span><span>Admin</span></footer></div>
    </main>
    <button class="sidebar-scrim" type="button" data-action="toggle-sidebar" aria-label="Đóng menu điều hướng"></button>
    <div id="modal-root"></div><div id="toast-root" aria-live="polite" aria-atomic="true"></div>
  </div>`;
}

export const routeTitles = titles;
