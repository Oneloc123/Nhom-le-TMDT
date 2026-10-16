import { dashboardSeries, orders, products, sellers, transactions, users } from "../data.js";
import { actionButton, avatarName, badge, compactMoney, dateLabel, donutChart, esc, icon, lineChart, money, pageHeading, panel, statusTone, table } from "../components.js";

const findSeller = id => sellers.find(item => item.id === id);
const findUser = id => users.find(item => item.id === id);
const metric = (label, value, iconName, accent, note = "MySQL records") => `<article class="metric-card metric-${accent}"><div class="metric-top"><span>${esc(label)}</span><span class="metric-icon">${icon(iconName)}</span></div><div class="metric-value">${value}</div><div class="metric-bottom"><span>${esc(note)}</span></div><span class="metric-cube" aria-hidden="true"></span></article>`;
const tally = rows => rows.reduce((counts, row) => {
  const key = row.status || "Unknown";
  counts[key] = (counts[key] || 0) + 1;
  return counts;
}, {});

export function renderDashboard(state) {
  const pending = products.filter(product => product.status === "Pending Review");
  const activeSellers = sellers.filter(seller => seller.status === "Active").length;
  const gmv = transactions.reduce((sum, row) => sum + Number(row.gross || 0), 0);
  const platformRevenue = transactions.reduce((sum, row) => sum + Number(row.commission || 0), 0);
  const statusCounts = tally(orders);
  const typeCounts = orders.reduce((counts, row) => {
    const key = row.type || "Other";
    counts[key] = (counts[key] || 0) + 1;
    return counts;
  }, {});
  const recentOrders = [...orders].sort((a, b) => b.created.localeCompare(a.created)).slice(0, 5);
  const orderColumns = [
    { label: "Order", render: row => `<span class="cell-primary">${row.id}</span><div class="cell-muted">${dateLabel(row.created)}</div>` },
    { label: "Customer", render: row => avatarName(findUser(row.customerId)?.name || row.customerId, findUser(row.customerId)?.color) },
    { label: "Amount", render: row => `<span class="cell-primary">${money(row.amount)}</span>` },
    { label: "Status", render: row => badge(row.status, statusTone(row.status)) },
    { label: "", render: row => actionButton("view", row.id, "View order") }
  ];
  const productColumns = [
    { label: "Product", render: row => `<div class="product-cell"><span class="product-visual tone-${row.tone || "violet"}">${row.imageLabel || "3D PRINT"}</span><span class="product-cell-copy"><strong>${row.name}</strong><small>${findSeller(row.sellerId)?.name || row.sellerId} · ${row.type || ""}</small></span></div>` },
    { label: "Submitted", render: row => dateLabel(row.created) },
    { label: "", render: row => `<div class="row-actions">${actionButton("view", row.id, "View product")}<button class="icon-button" data-action="product-approve" data-record="${row.id}" aria-label="Approve">${icon("check")}</button><button class="icon-button danger" data-action="product-reject" data-record="${row.id}" aria-label="Reject">${icon("close")}</button></div>` }
  ];
  const series = dashboardSeries[state.range] || dashboardSeries["30 days"] || [];
  const revenuePanel = panel("Marketplace revenue", "Persisted chart series and transaction records", `<div class="chart-toolbar"><div><small>GMV in loaded transactions</small><strong>${compactMoney(gmv)}</strong></div><span class="up">${transactions.length} records</span></div>${lineChart(series, "Marketplace revenue")}`);
  const orderPanel = panel("Orders by type", "Calculated from loaded orders", donutChart(Object.entries(typeCounts).map(([label, value]) => ({ label, value })), String(orders.length), "orders"));
  const statusPanel = panel("Order status", "Current database records", `<div class="status-list">${Object.entries(statusCounts).map(([label, count]) => `<div class="status-row"><i class="status-bullet"></i><span>${label}</span><strong>${count}</strong></div>`).join("")}</div>`);
  const sellerPanel = panel("Top sellers", "Sorted by revenue", `<div class="rank-list">${[...sellers].sort((a, b) => Number(b.revenue || 0) - Number(a.revenue || 0)).slice(0, 4).map((seller, index) => `<div class="rank-row"><span class="rank-index">0${index + 1}</span>${avatarName(seller.name, seller.color)}<span class="rank-value">${compactMoney(seller.revenue || 0)}</span></div>`).join("")}</div>`, { action: `<button class="text-link" data-nav="sellers">All sellers ${icon("chevron")}</button>` });
  const newestSellers = [...sellers].sort((a, b) => b.joined.localeCompare(a.joined)).slice(0, 4);
  const sellerPanelNew = panel("Recently joined sellers", "Loaded from the database", `<div class="rank-list">${newestSellers.map(seller => `<div class="rank-row">${avatarName(seller.name, seller.color, seller.city)}<span class="rank-copy"><strong>${seller.ownerName}</strong><small>${dateLabel(seller.joined)}</small></span>${badge(seller.status, statusTone(seller.status))}</div>`).join("")}</div>`);
  const recentPanel = panel("Recent orders", "Latest records", `<div class="table-scroll">${table(orderColumns, recentOrders)}</div>`, { action: `<button class="text-link" data-nav="orders">All orders ${icon("chevron")}</button>` });
  const pendingPanel = panel("Products pending review", `${pending.length} pending records`, `<div class="table-scroll">${table(productColumns, pending)}</div>`, { action: `<button class="text-link" data-nav="products">Open products ${icon("chevron")}</button>` });
  const completed = statusCounts.Completed || 0;
  const completedPercent = orders.length ? Math.round(completed * 100 / orders.length) : 0;

  return `${pageHeading("OVERVIEW", "Admin workspace", "Marketplace figures loaded from MySQL.", `<span class="date-chip">${icon("clock")} ${new Intl.DateTimeFormat("vi-VN", { dateStyle: "medium" }).format(new Date())}</span>`)}<div class="demo-strip">${icon("spark")} MySQL admin workspace · ${users.length} users · ${sellers.length} sellers · ${orders.length} orders</div><div class="metric-grid">${metric("Users", users.length.toLocaleString("vi-VN"), "users", "violet")}${metric("Active sellers", activeSellers.toLocaleString("vi-VN"), "shop", "orange")}${metric("Orders", orders.length.toLocaleString("vi-VN"), "box", "green")}${metric("GMV", compactMoney(gmv), "chart", "blue")}${metric("Platform revenue", compactMoney(platformRevenue), "wallet", "rose")}${metric("Pending review", String(pending.length).padStart(2, "0"), "cube", "yellow")}</div><div class="dashboard-main-grid">${revenuePanel}${orderPanel}</div><div class="dashboard-lower-grid">${statusPanel}${sellerPanel}</div><div class="dashboard-triple-grid">${sellerPanelNew}${panel("Order completion", "Calculated from loaded orders", `<div class="pulse-card"><div class="pulse-orbit"><span>${icon("printer")}</span></div><strong>${completedPercent}%</strong><small>${completed} of ${orders.length} orders are completed</small><div class="pulse-foot"><span><i class="online-dot"></i> ${activeSellers} active sellers</span><span>${transactions.length} transactions</span></div></div>`)}</div><div class="dashboard-lower-grid dashboard-table-grid">${recentPanel}${pendingPanel}</div>`;
}
