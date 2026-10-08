import { commissionRules, dashboardSeries, depositTransactions, sellers, transactions, users } from "../data.js";
import { actionButton, avatarName, badge, compactMoney, dateLabel, esc, icon, lineChart, metricCard, money, pageHeading, pager, panel, statusTone, table } from "../components.js";

const findSeller = id => sellers.find(item => item.id === id);
const findUser = id => users.find(item => item.id === id);

function transactionTable(rows) {
  const columns = [
    { label: "Transaction ID", render: row => `<span class="cell-primary">${row.id}</span>` },
    { label: "Order ID", render: row => `<span class="cell-muted">${row.orderId}</span>` },
    { label: "Khách hàng", render: row => findUser(row.customerId)?.name || "—" },
    { label: "Seller", render: row => findSeller(row.sellerId)?.name || "—" },
    { label: "Gross amount", render: row => money(row.gross) },
    { label: "Commission", render: row => `<span class="cell-primary">${money(row.commission)}</span>` },
    { label: "Seller revenue", render: row => money(row.sellerRevenue) },
    { label: "Refund", render: row => row.refund ? `<span class="refund-value">−${money(row.refund)}</span>` : "—" },
    { label: "Ngày", render: row => dateLabel(row.date) },
    { label: "Trạng thái", render: row => badge(row.status, statusTone(row.status)) }
  ];
  return table(columns, rows, "Chưa có giao dịch");
}

export function renderRevenue(state) {
  const range = state.range || "30 days";
  const transactionRows = transactions.filter(row => {
    const query = String(state.search || "").toLocaleLowerCase("vi");
    return !query || [row.id, row.orderId, findUser(row.customerId)?.name, findSeller(row.sellerId)?.name].some(value => String(value || "").toLocaleLowerCase("vi").includes(query));
  });
  const rate = Number(state.commissionRate ?? 10);
  return `${pageHeading("TÀI CHÍNH", "Doanh thu", "Theo dõi GMV, phí nền tảng và dòng tiền seller theo giao dịch.", `<label class="select-box range-select"><select data-range>${[["7 days", "7 ngày"], ["30 days", "30 ngày"], ["3 months", "3 tháng"], ["6 months", "6 tháng"], ["1 year", "1 năm"]].map(([value, label]) => `<option value="${value}" ${range === value ? "selected" : ""}>${label}</option>`).join("")}</select>${icon("down")}</label>`)}<div class="demo-strip">${icon("spark")} Số liệu giao dịch và tỷ lệ hiện tại là dữ liệu mẫu để trình diễn</div><div class="revenue-highlight"><div><small>Gross merchandise value · GMV</small><strong>${money(128460000)}</strong><span>↗ 18,6% so với kỳ trước · ${escPeriod(range)}</span></div><button class="button" data-nav="commission">Cấu hình commission ${icon("arrow")}</button></div><div class="metric-grid">${metricCard({ label: "Platform revenue", value: money(12846000), change: "+18,6%", iconName: "wallet", accent: "violet" })}${metricCard({ label: "Seller revenue", value: money(115614000), change: "+17,2%", iconName: "shop", accent: "green" })}${metricCard({ label: "Commission", value: `${rate}%`, change: "Configurable", footnote: "mock setting", iconName: "spark", accent: "orange" })}${metricCard({ label: "Refund", value: money(270000), change: "−2,1%", iconName: "arrow", accent: "rose" })}</div>${panel("Revenue over time", "GMV và doanh thu platform · VND", `${lineChart(dashboardSeries[range] || dashboardSeries["30 days"], "GMV và platform revenue")}`, { action: `<span class="legend-inline"><i></i> GMV <i class="soft"></i> Platform revenue</span>` })}<div class="finance-table-space">${panel("Giao dịch marketplace", `${transactions.length} giao dịch mẫu · tính commission theo cấu hình demo`, `<div class="table-toolbar"><div class="toolbar-left"><form class="search-form" data-form="page-search"><label class="search-box">${icon("search")}<input type="search" name="search" value="${esc(state.search || "")}" placeholder="Tìm transaction, order, customer…"></label><button class="button secondary" type="submit">Tìm</button></form></div><button class="button secondary" data-action="export" data-export="transactions">${icon("download")} Xuất CSV</button></div><div class="table-scroll">${transactionTable(transactionRows)}</div>`)}</div>`;
}

function escPeriod(value) {
  return ({ "7 days": "7 ngày", "30 days": "30 ngày", "3 months": "3 tháng", "6 months": "6 tháng", "1 year": "1 năm" })[value] || "30 ngày";
}

export function renderDeposits(state) {
  const depositStatus = seller => seller.deposit >= seller.requiredDeposit ? "Sufficient" : "Insufficient";
  let rows = sellers.map(seller => ({ ...seller, depositStatus: depositStatus(seller) }));
  const query = String(state.search || "").toLocaleLowerCase("vi");
  rows = rows.filter(row => (!query || [row.name, row.ownerName, row.id, row.location].some(value => String(value || "").toLocaleLowerCase("vi").includes(query))) && (!state.filters?.depositStatus || state.filters.depositStatus === "All" || row.depositStatus === state.filters.depositStatus));
  const page = Math.min(Math.max(1, state.page || 1), Math.max(1, Math.ceil(rows.length / 7)));
  const visibleRows = rows.slice((page - 1) * 7, page * 7);
  const columns = [
    { label: "Seller", render: row => avatarName(row.name, row.color, row.id) },
    { label: "Mức cọc yêu cầu", render: row => money(row.requiredDeposit) },
    { label: "Tiền cọc hiện tại", render: row => `<span class="cell-primary">${money(row.deposit)}</span>` },
    { label: "Chênh lệch", render: row => row.deposit >= row.requiredDeposit ? badge("Đủ cọc", "success") : `<span class="refund-value">−${money(row.requiredDeposit - row.deposit)}</span>` },
    { label: "Trạng thái", render: row => badge(row.depositStatus === "Sufficient" ? "Sufficient" : "Insufficient", row.depositStatus === "Sufficient" ? "success" : "warning") },
    { label: "Cập nhật gần nhất", render: row => dateLabel(depositTransactions.find(tx => tx.sellerId === row.id)?.date || row.joined) },
    { label: "", render: row => actionButton("view", row.id, "Xem thông tin seller") }
  ];
  const txColumns = [
    { label: "Mã giao dịch", render: row => `<span class="cell-primary">${row.id}</span>` },
    { label: "Seller", render: row => findSeller(row.sellerId)?.name || "—" },
    { label: "Loại giao dịch", render: row => badge(row.type, row.type === "Refund" ? "warning" : row.type === "Adjustment" ? "neutral" : "success") },
    { label: "Số tiền", render: row => `<span class="${row.amount < 0 ? "refund-value" : "cell-primary"}">${money(row.amount)}</span>` },
    { label: "Ngày", render: row => dateLabel(row.date) },
    { label: "Ghi chú", key: "note" }
  ];
  const totalDeposits = sellers.reduce((sum, seller) => sum + seller.deposit, 0);
  const sufficient = sellers.filter(seller => seller.deposit >= seller.requiredDeposit).length;
  return `${pageHeading("SELLER BALANCE", "Tiền cọc Seller", "Theo dõi tiền cọc tham khảo và lịch sử deposit, refund, adjustment.", `<button class="button secondary" data-action="export" data-export="deposits">${icon("download")} Xuất danh sách</button>`)}<div class="demo-strip">${icon("spark")} Chưa có payment integration · Các số dư chỉ phục vụ demo</div><div class="deposit-summary"><div class="deposit-card"><span class="metric-icon">${icon("wallet")}</span><span><small>Mức cọc tham khảo</small><strong>${money(8000000)}</strong></span></div><div class="deposit-card"><span class="metric-icon">${icon("shop")}</span><span><small>Tổng tiền cọc</small><strong>${money(totalDeposits)}</strong></span></div><div class="deposit-card"><span class="metric-icon">${icon("check")}</span><span><small>Đủ mức tham khảo</small><strong>${sufficient} / ${sellers.length}</strong></span></div><div class="deposit-card"><span class="metric-icon">${icon("warning")}</span><span><small>Cần theo dõi</small><strong>${sellers.length - sufficient} seller</strong></span></div></div>${panel("Tình trạng tiền cọc", "Mức cọc hiện tại so với yêu cầu tham khảo của từng xưởng", `<div class="table-toolbar"><form class="search-form" data-form="page-search"><label class="search-box">${icon("search")}<input type="search" name="search" value="${state.search || ""}" placeholder="Tìm seller hoặc xưởng in…"></label><button class="button secondary" type="submit">Tìm</button></form><label class="select-box"><span>Hiển thị</span><select data-filter-key="depositStatus"><option value="All">Tất cả</option><option value="Sufficient" ${state.filters?.depositStatus === "Sufficient" ? "selected" : ""}>Đủ cọc</option><option value="Insufficient" ${state.filters?.depositStatus === "Insufficient" ? "selected" : ""}>Thiếu cọc</option></select>${icon("down")}</label></div><div class="table-scroll">${table(columns, visibleRows, "Không tìm thấy seller")}</div>${pager(page, rows.length, 7)}`)}<div class="finance-table-space">${panel("Lịch sử giao dịch tiền cọc", "Deposit · Refund · Adjustment", `<div class="table-scroll">${table(txColumns, depositTransactions)}</div>`)}</div>`;
}

export function renderCommission(state) {
  const rate = Number(state.commissionRate ?? 10);
  const gmvProjection = 28600000;
  const platformProjection = Math.round(gmvProjection * rate / 100);
  const sellerProjection = gmvProjection - platformProjection;
  const sellerOptions = sellers.map(seller => `<option value="${seller.id}">${seller.name} · ${seller.city}</option>`).join("");
  const rules = state.commissionRules || commissionRules;
  const columns = [
    { label: "Quy tắc", render: row => `<span class="cell-primary">${esc(row.name)}</span><div class="cell-muted">${esc(row.id)}</div>` },
    { label: "Phạm vi seller", render: row => esc(row.scope) },
    { label: "Loại sản phẩm", render: row => row.productType === "All products" ? "Tất cả sản phẩm" : row.productType === "READY-MADE" ? "Ready-made" : "Custom printing" },
    { label: "Tỷ lệ", render: row => `<span class="cell-primary">${row.rate}%</span>` },
    { label: "Trạng thái", render: row => badge(row.enabled ? "Đang dùng" : "Nháp demo", row.enabled ? "success" : "muted") }
  ];
  return `${pageHeading("PLATFORM PRICING", "Commission", "Cấu hình tỷ lệ phí nền tảng theo phạm vi seller và loại sản phẩm.") }<div class="demo-strip">${icon("spark")} Mức 10% là cấu hình mẫu có thể chỉnh sửa, không phải quy tắc nghiệp vụ bắt buộc</div><section class="commission-hero"><div class="commission-copy"><div class="eyebrow">Platform commission</div><h2>Tỷ lệ chia sẻ doanh thu</h2><p>Điều chỉnh tỷ lệ để xem trước cách doanh thu được phân bổ. Thay đổi chỉ nằm trong local state của phiên demo.</p><div class="commission-note">${icon("warning")} Chưa áp dụng lên đơn hàng hoặc seller thật.</div></div><form class="commission-input-card" data-form="commission"><label for="commission-rate">Commission áp dụng toàn platform</label><div class="commission-input-wrap"><input id="commission-rate" name="rate" type="number" min="0" max="100" step="0.5" value="${rate}" data-commission-rate aria-label="Tỷ lệ commission phần trăm"><span>%</span></div><div class="commission-projection"><span>Platform revenue / tháng</span><strong>${money(platformProjection)}</strong></div><div class="commission-projection"><span>Seller revenue / tháng</span><strong>${money(sellerProjection)}</strong></div><button class="button secondary small" type="submit" style="width:100%;margin-top:12px">Lưu cấu hình demo</button></form></section><div class="three-column commission-metrics"><div class="analytics-stat"><small>Current commission</small><strong>${rate}%</strong><span>cấu hình local hiện tại</span></div><div class="analytics-stat"><small>Projected platform revenue</small><strong>${money(platformProjection)}</strong><span>trên GMV mẫu ${money(gmvProjection)}</span></div><div class="analytics-stat"><small>Projected seller revenue</small><strong>${money(sellerProjection)}</strong><span>sau commission demo</span></div></div>${panel("Tạo quy tắc commission demo", "Có thể chọn toàn bộ seller hoặc một seller cụ thể.", `<form class="form-grid commission-rule-form" data-form="commission-rule"><label class="form-field">Phạm vi seller<select name="scope"><option value="All sellers">All sellers</option>${sellerOptions}</select></label><label class="form-field">Loại sản phẩm<select name="productType"><option value="All products">Tất cả loại</option><option value="READY-MADE">Ready-made</option><option value="CUSTOM PRINTING SERVICE">Custom printing</option></select></label><label class="form-field">Tỷ lệ commission (%)<input name="rate" type="number" min="0" max="100" step="0.5" value="${rate}" required></label><div class="form-field"><span>Ước tính trên GMV mẫu</span><div class="form-hint">Platform ${money(platformProjection)} · Seller ${money(sellerProjection)}</div></div><div class="form-field wide"><button class="button" type="submit">${icon("plus")} Thêm quy tắc local</button></div></form>`)}<div class="finance-table-space">${panel("Quy tắc hiện có", "Các quy tắc đang là cấu hình mẫu, chưa có hiệu lực backend.", `<div class="table-scroll">${table(columns, rules)}</div>`)}</div>`;
}
