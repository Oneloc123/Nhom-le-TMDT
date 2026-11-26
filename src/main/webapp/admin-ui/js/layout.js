import { esc, icon } from "./components.js?v=admin-nav-fix-20261126-r3";

const csrfToken = document.querySelector('meta[name="csrf-token"]')?.content || "";

const financeChildren = [
  { key: "finance", label: "Dashboard", href: "/admin/finance", icon: "grid" },
  { key: "finance-cod", label: "COD chờ nền tảng xác nhận đã nhận tiền", href: "/admin/finance/cod", icon: "map" },
  { key: "finance-settlements", label: "Quyết toán theo tháng", href: "/admin/finance/settlements", icon: "chart" },
  { key: "finance-refunds", label: "Điều chỉnh hoàn tiền", href: "/admin/finance/refunds", icon: "check" },
  { key: "finance-ledger", label: "Sổ cái tài chính", href: "/admin/finance/ledger", icon: "file" },
  { key: "finance-allocations", label: "Phân bổ tài chính gần đây", href: "/admin/finance/allocations", icon: "box" }
];

const navigation = [
  { key: "dashboard", label: "Dashboard", icon: "grid", href: "/admin" },
  { key: "users", label: "Người dùng", icon: "users", href: "/admin/users" },
  { key: "sellers", label: "Seller / Xưởng in", icon: "shop", href: "/admin/sellers" },
  { key: "products", label: "Sản phẩm", icon: "cube", href: "/admin/products" },
  { key: "orders", label: "Đơn hàng", icon: "box", href: "/admin/orders" },
  { key: "shipments", label: "Vận đơn", icon: "truck", href: "/admin/shipments" },
  { key: "statistics", label: "Thống kê", icon: "chart", href: "/admin/statistics" },
  { key: "finance", label: "Tài chính & Hoa hồng", icon: "wallet", href: "/admin/finance", children: financeChildren },
  { key: "custom-prints", label: "Yêu cầu in 3D", icon: "cube", href: "/admin/custom-prints" },
  { key: "order-changes", label: "Yêu cầu thay đổi đơn", icon: "box", href: "/admin/order-changes" },
  { key: "reviews", label: "Đánh giá", icon: "check", href: "/admin/reviews" },
  { key: "reconciliation", label: "Đối soát thanh toán", icon: "bell", href: "/admin/reconciliation" },
  { key: "support", label: "Hỗ trợ khách hàng", icon: "support", href: "/admin/tickets" },
  { key: "returns", label: "Đổi trả / Hoàn tiền", icon: "box", href: "/admin/returns" },
  { key: "escalations", label: "Can thiệp đơn hàng", icon: "scale", href: "/admin/escalations" }
];

const titles = {
  ...Object.fromEntries(navigation.map(item => [item.key, item.label])),
  "finance": "Dashboard tài chính",
  ...Object.fromEntries(financeChildren.filter(item => item.key !== "finance").map(item => [item.key, item.label]))
};
const financePaths = Object.fromEntries(financeChildren.map(item => [item.key, item.href]));

export function currentRoute(contextPath = "") {
  let path = window.location.pathname;
  if (contextPath && path.startsWith(contextPath)) path = path.slice(contextPath.length);
  if (path === "/admin" || path === "/admin/") return "dashboard";
  if (path === "/admin/finance" || path === "/admin/finance/") return "finance";
  if (path.startsWith("/admin/finance/")) {
    const segment = path.slice("/admin/finance/".length).split("/")[0];
    const route = ({
      cod: "finance-cod",
      settlements: "finance-settlements",
      refunds: "finance-refunds",
      ledger: "finance-ledger",
      allocations: "finance-allocations"
    })[segment];
    return route || "not-found";
  }
  const slug = path.split("/").filter(Boolean).pop();
  if (slug === "return-refunds") return "returns";
  if (slug === "tickets") return "support";
  return titles[slug] ? slug : "not-found";
}

export function routePath(key) {
  return financePaths[key] || navigation.find(item => item.key === key)?.href || "/admin";
}

function renderFinanceNavigation(route, contextPath) {
  const active = route.startsWith("finance");
  const children = financeChildren.map(item => `<a class="nav-subitem ${route === item.key ? "active" : ""}"
      data-route="${item.key}" href="${esc(contextPath)}${item.href}" ${route === item.key ? 'aria-current="page"' : ""}>
      <span class="nav-subicon">${icon(item.icon)}</span><span>${esc(item.label)}</span></a>`).join("");
  return `<div class="nav-dropdown ${active ? "is-active" : ""}">
    <a class="nav-item ${active ? "active" : ""}" data-route="finance" href="${esc(contextPath)}${financePaths.finance}"
      title="Tài chính &amp; Hoa hồng" aria-haspopup="true" aria-controls="finance-submenu">
      <span class="nav-icon">${icon("wallet")}</span><span class="nav-text">Tài chính &amp; Hoa hồng</span>
      <span class="nav-expand">${icon("down")}</span></a>
    <div class="nav-submenu" id="finance-submenu" aria-label="Các trang Tài chính và Hoa hồng">${children}</div>
  </div>`;
}

export function renderLayout(route, content, contextPath = "", collapsed = false, actor = null, notifications = {}) {
  const title = titles[route] || "Không tìm thấy trang";
  const actorName = actor?.name || "";
  const displayName = actorName || "Tài khoản quản trị";
  const actorRole = actor?.role === "ADMIN" ? "Quản trị viên" : (actor?.role || "");
  const sidebar = navigation.map(item => {
    if (item.key === "finance") return renderFinanceNavigation(route, contextPath);
    const count = Number(notifications?.[({ sellers: "pendingSellers", products: "pendingProducts" })[item.key]] || 0);
    const badge = count > 0 ? `<span class="nav-alert" aria-label="${count} mục chờ duyệt">${count > 99 ? "99+" : count}</span>` : "";
    return `<a class="nav-item ${route === item.key ? "active" : ""}"
      data-route="${item.key}" href="${esc(contextPath)}${item.href}" title="${esc(item.label)}" ${route === item.key ? 'aria-current="page"' : ""}>
      <span class="nav-icon">${icon(item.icon)}</span><span class="nav-text">${esc(item.label)}</span>${badge}</a>`;
  }).join("");
  return `<div class="admin-shell ${collapsed ? "sidebar-collapsed" : ""}">
    <aside class="admin-sidebar" id="admin-sidebar">
      <a class="brand" href="${esc(contextPath)}/admin"><span class="brand-mark"><i></i><i></i><i></i></span>
        <span class="brand-copy"><strong>PrintKraft<span>.</span></strong><small>MARKETPLACE ADMIN</small></span></a>
      <div class="workspace-switch"><span class="workspace-cube">${icon("cube")}</span>
        <span class="workspace-copy"><small>Quản trị vận hành</small><strong>Marketplace</strong></span></div>
      <nav class="sidebar-nav" aria-label="Điều hướng chính"><div class="nav-group">
        <div class="nav-label">Quản lý marketplace</div>${sidebar}</div></nav>
      <div class="sidebar-bottom">
        <div class="sidebar-admin-copy"><strong>${esc(displayName)}</strong>${actorRole ? `<small>${esc(actorRole)}</small>` : ""}</div>
        <form class="logout-form" method="post" action="${esc(contextPath)}/logout">
          <input type="hidden" name="csrfToken" value="${esc(csrfToken)}">
          <button class="logout-button" type="submit" aria-label="Đăng xuất" title="Đăng xuất">${icon("logout")}<span>Đăng xuất</span></button>
        </form></div>
    </aside>
    <main class="admin-main">
      <header class="admin-header"><div class="header-left"><button class="icon-button menu-toggle" type="button" data-action="toggle-sidebar"
          aria-label="${collapsed ? "Đóng" : "Mở"} menu điều hướng" aria-controls="admin-sidebar" aria-expanded="${collapsed}">${icon("menu")}</button>
          <div class="breadcrumbs"><span>PrintKraft</span>${icon("chevron")}<strong>${esc(title)}</strong></div></div>
        <div class="header-actions"><button class="button secondary finance-help-button" type="button" data-action="finance-help">
          ${icon("help")}<span>Hướng dẫn sử dụng</span></button></div></header>
      <div class="page-content">${content}<footer class="page-footer"><span>PrintKraft Marketplace</span><span>Admin</span></footer></div>
    </main>
    <button class="sidebar-scrim" type="button" data-action="toggle-sidebar" aria-label="Đóng menu điều hướng"></button>
    <div id="modal-root"></div><div id="toast-root" aria-live="polite" aria-atomic="true"></div>
  </div>`;
}

export const routeTitles = titles;
