import { commissionRules, dashboardSeries, depositTransactions, sampleSettings, sellers, transactions, users } from "../data.js";
import { actionButton, avatarName, badge, compactMoney, dateLabel, esc, icon, lineChart, money, pageHeading, pager, panel, statusTone, table } from "../components.js";

const findSeller = id => sellers.find(item => item.id === id);
const findUser = id => users.find(item => item.id === id);
const sum = (rows, key) => rows.reduce((total, row) => total + Number(row[key] || 0), 0);
const recordMetric = (label, value, iconName, accent, note = "MySQL records") => `<article class="metric-card metric-${accent}"><div class="metric-top"><span>${esc(label)}</span><span class="metric-icon">${icon(iconName)}</span></div><div class="metric-value">${value}</div><div class="metric-bottom"><span>${esc(note)}</span></div><span class="metric-cube" aria-hidden="true"></span></article>`;

function transactionTable(rows) {
  return table([
    { label: "Transaction", render: row => `<span class="cell-primary">${esc(row.id)}</span>` },
    { label: "Order", render: row => `<span class="cell-muted">${esc(row.orderId)}</span>` },
    { label: "Customer", render: row => findUser(row.customerId)?.name || "—" },
    { label: "Seller", render: row => findSeller(row.sellerId)?.name || "—" },
    { label: "Gross", render: row => money(row.gross) },
    { label: "Commission", render: row => `<span class="cell-primary">${money(row.commission)}</span>` },
    { label: "Seller revenue", render: row => money(row.sellerRevenue) },
    { label: "Refund", render: row => row.refund ? `<span class="refund-value">${money(row.refund)}</span>` : "—" },
    { label: "Date", render: row => dateLabel(row.date) },
    { label: "Status", render: row => badge(row.status, statusTone(row.status)) }
  ], rows, "No transactions found");
}

export function renderRevenue(state) {
  const query = String(state.search || "").toLocaleLowerCase("vi");
  const rows = transactions.filter(row => !query || [row.id, row.orderId, findUser(row.customerId)?.name, findSeller(row.sellerId)?.name]
    .some(value => String(value || "").toLocaleLowerCase("vi").includes(query)));
  const gmv = sum(transactions, "gross");
  const commission = sum(transactions, "commission");
  const sellerRevenue = sum(transactions, "sellerRevenue");
  const refunds = sum(transactions, "refund");
  const rate = Number(state.commissionRate ?? sampleSettings.commissionRate ?? 10);
  const range = state.range || "30 days";
  const rangeOptions = [["7 days", "7 ngày"], ["30 days", "30 ngày"], ["3 months", "3 tháng"], ["6 months", "6 tháng"], ["1 year", "1 năm"]];
  return `${pageHeading("FINANCE", "Doanh thu", "Giao dịch, GMV và doanh thu tính từ dữ liệu database.", `<label class="select-box range-select"><select data-range>${rangeOptions.map(([value, label]) => `<option value="${value}" ${range === value ? "selected" : ""}>${label}</option>`).join("")}</select>${icon("down")}</label>`)}<div class="demo-strip">${icon("spark")} Số liệu lấy từ ${transactions.length} giao dịch đã nạp vào MySQL</div><div class="revenue-highlight"><div><small>Gross merchandise value · GMV</small><strong>${money(gmv)}</strong><span>${transactions.length} giao dịch</span></div><button class="button" data-nav="commission">Cấu hình commission ${icon("arrow")}</button></div><div class="metric-grid">${recordMetric("Platform revenue", money(commission), "wallet", "violet")}${recordMetric("Seller revenue", money(sellerRevenue), "shop", "green")}${recordMetric("Commission", `${rate}%`, "spark", "orange", "Current database setting")}${recordMetric("Refund", money(refunds), "arrow", "rose")}</div>${panel("Revenue series", "Persisted dashboard series", lineChart(dashboardSeries[range] || dashboardSeries["30 days"] || [], "Marketplace GMV"))}<div class="finance-table-space">${panel("Marketplace transactions", `${transactions.length} records from MySQL`, `<div class="table-toolbar"><div class="toolbar-left"><form class="search-form" data-form="page-search"><label class="search-box">${icon("search")}<input type="search" name="search" value="${esc(state.search || "")}" placeholder="Search transaction, order, customer"></label><button class="button secondary" type="submit">Search</button></form></div><button class="button secondary" data-action="export" data-export="transactions">${icon("download")} Export CSV</button></div><div class="table-scroll">${transactionTable(rows)}</div>`)}</div>`;
}

export function renderDeposits(state) {
  const query = String(state.search || "").toLocaleLowerCase("vi");
  const rows = sellers.map(seller => ({ ...seller, depositStatus: seller.deposit >= seller.requiredDeposit ? "Sufficient" : "Insufficient" }))
    .filter(row => (!query || [row.name, row.ownerName, row.id, row.location].some(value => String(value || "").toLocaleLowerCase("vi").includes(query)))
      && (!state.filters?.depositStatus || state.filters.depositStatus === "All" || row.depositStatus === state.filters.depositStatus));
  const page = Math.min(Math.max(1, state.page || 1), Math.max(1, Math.ceil(rows.length / 7)));
  const visible = rows.slice((page - 1) * 7, page * 7);
  const sufficient = rows.filter(row => row.depositStatus === "Sufficient").length;
  const totalDeposits = sum(sellers, "deposit");
  const columns = [
    { label: "Seller", render: row => avatarName(row.name, row.color, row.id) },
    { label: "Required deposit", render: row => money(row.requiredDeposit) },
    { label: "Current deposit", render: row => `<span class="cell-primary">${money(row.deposit)}</span>` },
    { label: "Status", render: row => badge(row.depositStatus, row.depositStatus === "Sufficient" ? "success" : "warning") },
    { label: "", render: row => actionButton("view", row.id, "View seller") }
  ];
  const txColumns = [
    { label: "Transaction", render: row => `<span class="cell-primary">${esc(row.id)}</span>` },
    { label: "Seller", render: row => findSeller(row.sellerId)?.name || "—" },
    { label: "Type", render: row => badge(row.type, row.type === "Refund" ? "warning" : row.type === "Adjustment" ? "neutral" : "success") },
    { label: "Amount", render: row => money(row.amount) },
    { label: "Date", render: row => dateLabel(row.date) },
    { label: "Note", key: "note" }
  ];
  const filter = `<label class="select-box"><span>Status</span><select data-filter-key="depositStatus"><option value="All">All</option><option value="Sufficient" ${state.filters?.depositStatus === "Sufficient" ? "selected" : ""}>Sufficient</option><option value="Insufficient" ${state.filters?.depositStatus === "Insufficient" ? "selected" : ""}>Insufficient</option></select>${icon("down")}</label>`;
  return `${pageHeading("SELLER BALANCE", "Tiền cọc Seller", "Số dư và lịch sử điều chỉnh được đọc từ dữ liệu MySQL.", `<button class="button secondary" data-action="export" data-export="deposits">${icon("download")} Export</button>`)}<div class="deposit-summary"><div class="deposit-card"><span class="metric-icon">${icon("wallet")}</span><span><small>Reference deposit</small><strong>${money(sampleSettings.requiredDeposit || 0)}</strong></span></div><div class="deposit-card"><span class="metric-icon">${icon("shop")}</span><span><small>Total deposits</small><strong>${money(totalDeposits)}</strong></span></div><div class="deposit-card"><span class="metric-icon">${icon("check")}</span><span><small>Sufficient</small><strong>${sufficient} / ${rows.length}</strong></span></div><div class="deposit-card"><span class="metric-icon">${icon("warning")}</span><span><small>Below requirement</small><strong>${rows.length - sufficient}</strong></span></div></div>${panel("Seller deposits", "Balances and requirements from database records", `<div class="table-toolbar"><form class="search-form" data-form="page-search"><label class="search-box">${icon("search")}<input type="search" name="search" value="${esc(state.search || "")}" placeholder="Search seller"></label><button class="button secondary" type="submit">Search</button></form>${filter}</div><div class="table-scroll">${table(columns, visible, "No sellers found")}</div>${pager(page, rows.length, 7)}`)}<div class="finance-table-space">${panel("Deposit transactions", "Deposit · Refund · Adjustment", `<div class="table-scroll">${table(txColumns, depositTransactions)}</div>`)}</div>`;
}

export function renderCommission(state) {
  const rate = Number(state.commissionRate ?? sampleSettings.commissionRate ?? 10);
  const gmv = sum(transactions, "gross");
  const platformProjection = Math.round(gmv * rate / 100);
  const sellerProjection = gmv - platformProjection;
  const sellerOptions = sellers.map(seller => `<option value="${esc(seller.id)}">${esc(seller.name)} · ${esc(seller.city)}</option>`).join("");
  const rows = state.commissionRules || commissionRules;
  const columns = [
    { label: "Rule", render: row => `<span class="cell-primary">${esc(row.name)}</span><div class="cell-muted">${esc(row.id)}</div>` },
    { label: "Seller scope", render: row => esc(row.scope) },
    { label: "Product type", render: row => esc(row.productType) },
    { label: "Rate", render: row => `<span class="cell-primary">${row.rate}%</span>` },
    { label: "Status", render: row => badge(row.enabled ? "Enabled" : "Disabled", row.enabled ? "success" : "muted") }
  ];
  return `${pageHeading("PLATFORM PRICING", "Commission", "Quy tắc và tỷ lệ commission được lưu trong MySQL.")}<div class="commission-hero"><div class="commission-copy"><div class="eyebrow">Platform commission</div><h2>Revenue split</h2><p>Thay đổi tỷ lệ sẽ cập nhật cấu hình backend và quy tắc marketplace.</p></div><form class="commission-input-card" data-form="commission"><label for="commission-rate">Default platform commission</label><div class="commission-input-wrap"><input id="commission-rate" name="rate" type="number" min="0" max="100" step="0.5" value="${rate}" data-commission-rate><span>%</span></div><div class="commission-projection"><span>Platform share of loaded GMV</span><strong>${money(platformProjection)}</strong></div><div class="commission-projection"><span>Seller share of loaded GMV</span><strong>${money(sellerProjection)}</strong></div><button class="button secondary small" type="submit" style="width:100%;margin-top:12px">Save commission</button></form></section><div class="three-column commission-metrics"><div class="analytics-stat"><small>Current commission</small><strong>${rate}%</strong><span>Database setting</span></div><div class="analytics-stat"><small>Platform share</small><strong>${money(platformProjection)}</strong><span>Calculated from ${transactions.length} transactions</span></div><div class="analytics-stat"><small>Seller share</small><strong>${money(sellerProjection)}</strong><span>Calculated from loaded GMV</span></div></div>${panel("Create commission rule", "Save a new rule to the backend.", `<form class="form-grid commission-rule-form" data-form="commission-rule"><label class="form-field">Seller scope<select name="scope"><option value="All sellers">All sellers</option>${sellerOptions}</select></label><label class="form-field">Product type<select name="productType"><option value="All products">All products</option><option value="READY-MADE">Ready-made</option><option value="CUSTOM PRINTING SERVICE">Custom printing</option></select></label><label class="form-field">Commission rate (%)<input name="rate" type="number" min="0" max="100" step="0.5" value="${rate}" required></label><div class="form-field"><span>Projection on loaded GMV</span><div class="form-hint">Platform ${money(platformProjection)} · Seller ${money(sellerProjection)}</div></div><div class="form-field wide"><button class="button" type="submit">${icon("plus")} Add rule</button></div></form>`)}<div class="finance-table-space">${panel("Commission rules", "Current backend rules", `<div class="table-scroll">${table(columns, rows)}</div>`)}</div>`;
}
