const iconPaths = {
  grid: '<rect x="3" y="3" width="7" height="7" rx="1.7"/><rect x="14" y="3" width="7" height="7" rx="1.7"/><rect x="3" y="14" width="7" height="7" rx="1.7"/><rect x="14" y="14" width="7" height="7" rx="1.7"/>',
  users: '<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/>',
  shop: '<path d="M3 10h18l-1.4-6H4.4L3 10Z"/><path d="M5 10v10h14V10M9 20v-6h6v6M3 10a3 3 0 0 0 6 0 3 3 0 0 0 6 0 3 3 0 0 0 6 0"/>',
  cube: '<path d="m12 3 9 5-9 5-9-5 9-5Z"/><path d="M3 8v8l9 5 9-5V8M12 13v8"/>',
  box: '<path d="m12 3 8 4.5v9L12 21l-8-4.5v-9L12 3Z"/><path d="m4.3 7.7 7.7 4.4 7.7-4.4M12 12v9"/>',
  wallet: '<rect x="3" y="5" width="18" height="15" rx="2.5"/><path d="M3 8h15a3 3 0 0 1 3 3v2h-5a2 2 0 0 0 0 4h5M16 14h.01M6 5V4a2 2 0 0 1 2-2h10"/>',
  chart: '<path d="M3 3v18h18M7 14l4-4 3 3 6-7"/><path d="M16 6h4v4"/>',
  map: '<path d="m3 6 6-3 6 3 6-3v15l-6 3-6-3-6 3V6Z"/><path d="M9 3v15m6-12v15"/>',
  settings: '<circle cx="12" cy="12" r="3"/><path d="m19.4 15 .1.1 1.4 1.1-1.4 2.4-1.7-.6a8 8 0 0 1-1.7 1l-.3 1.8h-2.8l-.3-1.8a8 8 0 0 1-1.7-1l-1.7.6-1.4-2.4 1.4-1.1a7 7 0 0 1 0-2l-1.4-1.1 1.4-2.4 1.7.6a8 8 0 0 1 1.7-1l.3-1.8h2.8l.3 1.8a8 8 0 0 1 1.7 1l1.7-.6 1.4 2.4-1.4 1.1a7 7 0 0 1 0 2Z" transform="translate(-1 -1)"/>',
  search: '<circle cx="10.8" cy="10.8" r="6.8"/><path d="m16 16 5 5"/>',
  bell: '<path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4"/>',
  chevron: '<path d="m9 18 6-6-6-6"/>',
  down: '<path d="m7 10 5 5 5-5"/>',
  arrow: '<path d="M7 17 17 7M7 7h10v10"/>',
  menu: '<path d="M4 6h16M4 12h16M4 18h16"/>',
  close: '<path d="m18 6-12 12M6 6l12 12"/>',
  eye: '<path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6-10-6-10-6Z"/><circle cx="12" cy="12" r="2.5"/>',
  filter: '<path d="M4 6h16M7 12h10m-7 6h4"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  download: '<path d="M12 3v12m-5-5 5 5 5-5M5 21h14"/>',
  printer: '<path d="M6 9V3h12v6M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"/><path d="M6 14h12v7H6z"/><path d="M18 12h.01"/>',
  spark: '<path d="m12 3 1.4 5.6L19 10l-5.6 1.4L12 17l-1.4-5.6L5 10l5.6-1.4L12 3Z"/><path d="m19 16 .7 2.3L22 19l-2.3.7L19 22l-.7-2.3L16 19l2.3-.7L19 16Z"/>',
  clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
  check: '<path d="m5 12 4 4L19 6"/>',
  warning: '<path d="m12 3 10 18H2L12 3Z"/><path d="M12 9v4m0 3h.01"/>',
  file: '<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6Z"/><path d="M14 2v6h6M8 13h8m-8 4h8"/>',
  location: '<path d="M20 10c0 5-8 12-8 12S4 15 4 10a8 8 0 1 1 16 0Z"/><circle cx="12" cy="10" r="2.5"/>',
  logout: '<path d="M10 17l5-5-5-5m5 5H3m9-9h7a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-7"/>'
};

export const icon = (name, className = "") => `<svg class="icon ${className}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${iconPaths[name] || iconPaths.cube}</svg>`;
export const esc = (value = "") => String(value).replace(/[&<>"']/g, char => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[char]);
export const money = value => new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(Number(value) || 0);
export const compactMoney = value => `${(Number(value || 0) / 1_000_000).toFixed(1).replace(".", ",")} tr`;
export const dateLabel = value => value ? new Intl.DateTimeFormat("vi-VN", { day: "2-digit", month: "short", year: "numeric" }).format(new Date(value)) : "—";
export const dateTimeLabel = value => value ? new Intl.DateTimeFormat("vi-VN", { day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit" }).format(new Date(value)) : "—";
export const initials = name => String(name || "").split(/\s+/).filter(Boolean).slice(-2).map(word => word[0]).join("").toUpperCase();

export function avatar(name, color = "lilac", size = "") {
  return `<span class="avatar avatar-${esc(color)} ${size}" aria-label="${esc(name)}">${esc(initials(name))}</span>`;
}

export function badge(text, tone = "neutral") {
  const slug = String(tone || "neutral").toLowerCase().replace(/[^a-z0-9]+/g, "-");
  return `<span class="badge badge-${slug}"><i></i>${esc(text)}</span>`;
}

export function statusTone(status = "") {
  const s = status.toLowerCase();
  if (["active", "published", "completed", "settled", "deposit received", "đã thanh toán"].includes(s)) return "success";
  if (["pending review", "pending quote", "customer accepted", "quoted", "processing", "printing", "shipping", "đã đặt cọc"].includes(s)) return "warning";
  if (["suspended", "rejected", "cancelled", "refunded", "đã hoàn tiền"].includes(s)) return "danger";
  if (["hidden", "inactive", "draft", "chưa thanh toán"].includes(s)) return "muted";
  return "neutral";
}

export function pageHeading(eyebrow, title, description, action = "") {
  return `<div class="page-heading"><div><div class="eyebrow">${esc(eyebrow)}</div><h1>${esc(title)}</h1><p>${esc(description)}</p></div>${action ? `<div class="heading-action">${action}</div>` : ""}</div>`;
}

export function panel(title, subtitle, content, options = {}) {
  return `<section class="panel ${options.className || ""}"><div class="panel-heading"><div><h2>${title}</h2>${subtitle ? `<p>${subtitle}</p>` : ""}</div>${options.action || ""}</div>${content}</section>`;
}

export function metricCard({ label, value, change, iconName = "chart", accent = "violet", footnote = "so với kỳ trước" }) {
  const positive = !String(change).trim().startsWith("-");
  return `<article class="metric-card metric-${accent}"><div class="metric-top"><span>${esc(label)}</span><span class="metric-icon">${icon(iconName)}</span></div><div class="metric-value">${value}</div><div class="metric-bottom"><span class="metric-change ${positive ? "up" : "down"}">${positive ? "↗" : "↘"} ${esc(change)}</span><span>${esc(footnote)}</span></div><span class="metric-cube" aria-hidden="true"></span></article>`;
}

export function searchBox(value = "", placeholder = "Tìm theo tên, mã, email…", id = "page-search") {
  return `<label class="search-box" for="${id}">${icon("search")}<input id="${id}" type="search" name="search" value="${esc(value)}" placeholder="${esc(placeholder)}" autocomplete="off"><kbd>/</kbd></label>`;
}

export function selectBox(label, key, options, selected = "All") {
  return `<label class="select-box"><span>${esc(label)}</span><select data-filter-key="${esc(key)}"><option value="All">Tất cả</option>${options.map(item => `<option value="${esc(item)}" ${item === selected ? "selected" : ""}>${esc(item)}</option>`).join("")}</select>${icon("down")}</label>`;
}

export function pager(page, total, perPage = 8) {
  const pages = Math.max(1, Math.ceil(total / perPage));
  return `<div class="table-footer"><span>Hiển thị <strong>${total ? Math.min((page - 1) * perPage + 1, total) : 0}–${Math.min(page * perPage, total)}</strong> trong <strong>${total}</strong> kết quả</span><div class="pagination"><button class="page-btn" data-page="${Math.max(1, page - 1)}" aria-label="Trang trước" ${page <= 1 ? "disabled" : ""}>‹</button>${Array.from({ length: pages }, (_, index) => index + 1).slice(Math.max(0, page - 2), Math.min(pages, Math.max(0, page - 2) + 5)).map(number => `<button class="page-btn ${number === page ? "active" : ""}" data-page="${number}" ${number === page ? 'aria-current="page"' : ""}>${number}</button>`).join("")}<button class="page-btn" data-page="${Math.min(pages, page + 1)}" aria-label="Trang sau" ${page >= pages ? "disabled" : ""}>›</button></div></div>`;
}

export function table(columns, rows, emptyTitle = "Chưa có dữ liệu", emptyText = "Thử điều chỉnh bộ lọc để xem thêm kết quả.") {
  if (!rows.length) return `<div class="empty-state"><span class="empty-illustration">${icon("box")}</span><strong>${esc(emptyTitle)}</strong><p>${esc(emptyText)}</p></div>`;
  return `<div class="table-scroll"><table class="data-table"><thead><tr>${columns.map(column => `<th>${column.sort ? `<button class="sort-button" data-sort="${esc(column.sort)}">${column.label}${icon("down")}</button>` : column.label}</th>`).join("")}</tr></thead><tbody>${rows.map(row => `<tr>${columns.map(column => `<td>${typeof column.render === "function" ? column.render(row) : esc(row[column.key] ?? "—")}</td>`).join("")}</tr>`).join("")}</tbody></table></div>`;
}

export function donutChart(items, center, caption = "đơn hàng") {
  const total = items.reduce((sum, item) => sum + item.value, 0) || 1;
  let offset = 0;
  const colors = ["#7466e8", "#efaa57", "#4dbb9f", "#ed7d73", "#89a2e8"];
  const segments = items.map((item, index) => {
    const length = item.value / total * 100;
    const segment = `<circle class="donut-segment" cx="60" cy="60" r="43" fill="none" stroke="${colors[index % colors.length]}" stroke-width="13" stroke-dasharray="${length} ${100 - length}" stroke-dashoffset="${-offset}" pathLength="100"/>`;
    offset += length;
    return segment;
  }).join("");
  return `<div class="donut-wrap"><svg class="donut-chart" viewBox="0 0 120 120" role="img" aria-label="Phân bổ ${esc(caption)}">${segments}<circle cx="60" cy="60" r="31" fill="#fff"/><text x="60" y="58" text-anchor="middle" class="donut-center">${esc(center)}</text><text x="60" y="72" text-anchor="middle" class="donut-caption">${esc(caption)}</text></svg><div class="donut-legend">${items.map((item, index) => `<div><span class="legend-dot" style="--dot:${colors[index % colors.length]}"></span><span>${esc(item.label)}</span><strong>${item.value}%</strong></div>`).join("")}</div></div>`;
}

export function lineChart(values, label = "Doanh thu") {
  const width = 620, height = 190, padX = 12, padY = 18;
  const maxValue = Math.max(...values);
  const axisMax = Math.ceil(maxValue / 10) * 10 || 10;
  const max = axisMax * 1.12;
  const numberLabel = value => new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 0 }).format(value);
  const points = values.map((value, index) => `${padX + index * (width - padX * 2) / (values.length - 1)},${height - padY - value / max * (height - padY * 2)}`).join(" ");
  const firstX = padX, lastX = width - padX;
  const area = `${firstX},${height - padY} ${points} ${lastX},${height - padY}`;
  return `<div class="line-chart-wrap"><div class="chart-y-labels"><span>${numberLabel(axisMax)}</span><span>${numberLabel(axisMax / 2)}</span><span>0</span></div><svg class="line-chart" viewBox="0 0 ${width} ${height}" preserveAspectRatio="none" role="img" aria-label="${esc(label)} theo thời gian"><defs><linearGradient id="chart-fill" x1="0" x2="0" y1="0" y2="1"><stop offset="0%" stop-color="#7869e9" stop-opacity=".20"/><stop offset="100%" stop-color="#7869e9" stop-opacity="0"/></linearGradient></defs><path class="chart-grid-line" d="M0 18H620M0 95H620M0 172H620"/><polygon points="${area}" fill="url(#chart-fill)"/><polyline points="${points}" fill="none" stroke="#7062df" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round" vector-effect="non-scaling-stroke"/>${values.map((value, index) => { const [x, y] = points.split(" ")[index].split(","); return `<circle cx="${x}" cy="${y}" r="3.3" fill="#fff" stroke="#7062df" stroke-width="2" vector-effect="non-scaling-stroke"/>`; }).join("")}</svg></div><div class="chart-x-labels"><span>01</span><span>05</span><span>10</span><span>15</span><span>20</span><span>25</span><span>30</span></div>`;
}

export function barChart(items, valueKey = "value") {
  const max = Math.max(...items.map(item => item[valueKey]), 1);
  return `<div class="bar-chart">${items.map(item => `<div class="bar-row"><span>${esc(item.label)}</span><div class="bar-track"><i style="width:${Math.max(5, item[valueKey] / max * 100)}%"></i></div><strong>${esc(item.display ?? item[valueKey])}</strong></div>`).join("")}</div>`;
}

export function avatarName(name, color = "lilac", meta = "") {
  return `<div class="person-cell">${avatar(name, color)}<span><strong>${esc(name)}</strong>${meta ? `<small>${esc(meta)}</small>` : ""}</span></div>`;
}

export function actionButton(action, id, label = "Xem chi tiết", iconName = "eye", extra = "") {
  return `<button class="icon-button" data-action="${esc(action)}" data-record="${esc(id)}" ${extra} aria-label="${esc(label)}" title="${esc(label)}">${icon(iconName)}</button>`;
}
