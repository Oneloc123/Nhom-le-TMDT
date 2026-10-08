import { orders, products, sellers, technologies } from "../data.js";
import { avatarName, barChart, badge, compactMoney, donutChart, icon, lineChart, metricCard, money, pageHeading, panel } from "../components.js";

const statusColors = ["", "orange", "green", "blue", "red"];
const ranges = [["7D", "7D"], ["30D", "30D"], ["3M", "3M"], ["6M", "6M"], ["1Y", "1Y"], ["CUSTOM", "Tùy chọn"]];

export function renderAnalytics(state) {
  const active = state.analyticsRange || "30D";
  const multiplier = ({ "7D": 0.24, "30D": 1, "3M": 2.8, "6M": 5.5, "1Y": 10, CUSTOM: 1 })[active] || 1;
  const periodGmv = Math.round(128460000 * multiplier);
  const periodOrders = Math.round(1284 * multiplier);
  const filter = `<div class="analytics-filter" role="group" aria-label="Khoảng thời gian">${ranges.map(([value, label]) => `<button class="${active === value ? "active" : ""}" data-analytics-range="${value}">${label}</button>`).join("")}</div>`;
  const customRange = active === "CUSTOM" ? `<div class="custom-date-range"><label>Từ ngày <input type="date" value="${state.customStart || "2026-09-01"}" data-range-date="start"></label><label>Đến ngày <input type="date" value="${state.customEnd || "2026-10-08"}" data-range-date="end"></label></div>` : "";
  const orderCounts = orders.reduce((acc, order) => { acc[order.type] = (acc[order.type] || 0) + 1; return acc; }, {});
  const typeTotal = orders.length || 1;
  const readyPercent = Math.round((orderCounts["READY-MADE"] || 0) / typeTotal * 100);
  const statusCounts = orders.reduce((acc, order) => { acc[order.status] = (acc[order.status] || 0) + 1; return acc; }, {});
  const statusItems = Object.entries(statusCounts).slice(0, 5).map(([label, value]) => ({ label, value: Math.round(value / typeTotal * 100) }));
  const techCounts = { FDM: 42, SLA: 28, SLS: 19, MJF: 11 };
  const materialCounts = { PLA: 36, PETG: 24, ABS: 15, "Resin tiêu chuẩn": 17, "Nylon PA12": 8 };
  const topSellers = [...sellers].sort((a, b) => b.revenue - a.revenue).slice(0, 5).map(seller => ({ label: seller.name, value: seller.revenue, display: compactMoney(seller.revenue) }));
  const topProducts = [...products].sort((a, b) => (b.rating || 0) - (a.rating || 0)).slice(0, 5).map(product => ({ label: product.name, value: Math.round((product.rating || 4.5) * 20), display: `★ ${(product.rating || 4.5).toFixed(1)}` }));
  const cancellationRate = ((orders.filter(order => ["Cancelled", "Refunded"].includes(order.status)).length / typeTotal) * 100).toFixed(1);
  const aov = Math.round(orders.reduce((sum, order) => sum + order.amount, 0) / typeTotal);
  const statStrip = `<div class="analytics-stat-strip"><div class="analytics-stat"><small>GMV kỳ này</small><strong>${money(periodGmv)}</strong><span>${active === "CUSTOM" ? `${state.customStart || "2026-09-01"} — ${state.customEnd || "2026-10-08"}` : `khoảng ${active}`}</span></div><div class="analytics-stat"><small>Đơn hàng ước tính</small><strong>${new Intl.NumberFormat("vi-VN").format(periodOrders)}</strong><span>mẫu theo khoảng thời gian</span></div><div class="analytics-stat"><small>Tỷ lệ hủy / hoàn</small><strong>${cancellationRate}%</strong><span>mẫu hiện tại ${orders.length} đơn</span></div><div class="analytics-stat"><small>Average order value</small><strong>${money(aov)}</strong><span>ước tính từ sample orders</span></div></div>`;
  return `${pageHeading("INSIGHTS & REPORTS", "Thống kê marketplace", "Nhìn toàn cảnh doanh thu, đơn hàng và năng lực mạng lưới xưởng in.", filter)}${customRange}<div class="demo-strip">${icon("spark")} Biểu đồ, tỷ lệ hủy và AOV được dựng từ dữ liệu mẫu; không phải báo cáo vận hành</div>${statStrip}<div class="analytics-grid"><div class="span-two">${panel("1 · Revenue over time", "GMV và doanh thu platform", lineChart([18, 24, 31, 27, 38, 35, 49, 43, 57, 53, 68, 82], "Doanh thu theo thời gian"))}</div>${panel("2 · Orders over time", "Số đơn hàng theo từng giai đoạn", lineChart([18, 22, 28, 24, 35, 31, 42, 38, 49, 52, 61, 75], "Đơn hàng theo thời gian"))}${panel("3 · Ready-made vs Custom print", "Cơ cấu đơn theo loại", donutChart([{ label: "Ready-made", value: readyPercent }, { label: "Custom print", value: 100 - readyPercent }], `${orders.length}`, "đơn mẫu"))}${panel("4 · Order status distribution", "Phân bổ trạng thái đơn demo", donutChart(statusItems.map(item => ({ label: item.label, value: item.value })), `${orders.length}`, "đơn mẫu"))}${panel("5 · Printing technology", "Tỷ trọng công nghệ in", barChart(Object.entries(techCounts).map(([label, value]) => ({ label, value, display: `${value}%` }))))}${panel("6 · Material distribution", "Vật liệu được chọn trong sample", barChart(Object.entries(materialCounts).map(([label, value]) => ({ label, value, display: `${value}%` }))))}${panel("7 · Top sellers", "Theo GMV marketplace mẫu", barChart(topSellers, "value"))}${panel("8 · Top products", "Theo mức quan tâm / rating", barChart(topProducts, "value"))}<div class="analytics-footnote">${icon("warning")} Tỷ trọng công nghệ và vật liệu mang tính minh họa, chưa kết nối sự kiện sử dụng thật.</div></div>`;
}

function sellerPosition(seller, index) {
  const base = seller.city === "Hà Nội" ? [53, 23] : seller.city === "Đà Nẵng" ? [63, 54] : [69, 78];
  const spread = (index % 3) - 1;
  return [Math.max(12, Math.min(86, base[0] + spread * 2.1)), Math.max(12, Math.min(88, base[1] + spread * 2.8))];
}

export function renderMap(state) {
  const mapFilter = state.mapFilter || "Sellers";
  const technology = state.technologyFilter || "All";
  const selected = mapFilter === "Printing Technology" ? sellers.filter(seller => technology === "All" || seller.technology.includes(technology)) : sellers;
  const sellerMarkers = mapFilter === "Orders" ? "" : selected.map((seller, index) => {
    const [left, top] = sellerPosition(seller, index);
    const title = mapFilter === "Revenue" ? compactMoney(seller.revenue) : seller.name;
    return `<button class="map-marker" style="left:${left}%;top:${top}%" data-action="map-seller" data-record="${seller.id}" aria-label="Xem ${seller.name}" title="${seller.name} · ${seller.location}">${icon("shop")}</button>${mapFilter === "Revenue" ? `<span class="map-revenue-label" style="left:${left + 1.5}%;top:${top + 1.5}%">${title}</span>` : ""}`;
  }).join("");
  const orderGroups = [
    { city: "Hà Nội", count: 5, amount: 2640000, type: "Custom print", position: [57, 30] },
    { city: "Đà Nẵng", count: 3, amount: 1790000, type: "Ready-made", position: [65, 59] },
    { city: "TP. Hồ Chí Minh", count: 8, amount: 4860000, type: "Mixed", position: [72, 82] }
  ];
  const orderMarkers = mapFilter === "Sellers" || mapFilter === "Printing Technology" ? "" : orderGroups.map(group => `<button class="map-marker order" style="left:${group.position[0]}%;top:${group.position[1]}%" data-action="map-orders" data-record="${group.city}" aria-label="${group.count} đơn hàng tại ${group.city}" title="${group.count} đơn · ${group.city}">${icon("box")}</button><span class="map-cluster-label" style="left:${group.position[0] + 1.5}%;top:${group.position[1] + 1.5}%">${group.count}</span>`).join("");
  const filterButtons = ["Sellers", "Orders", "Revenue", "Printing Technology"].map(item => `<button class="map-filter-chip ${mapFilter === item ? "active" : ""}" data-map-filter="${item}">${item === "Sellers" ? "Xưởng in" : item === "Orders" ? "Đơn hàng" : item === "Revenue" ? "Doanh thu" : "Công nghệ"}</button>`).join("");
  const techSelect = mapFilter === "Printing Technology" ? `<label class="select-box"><span>Công nghệ</span><select data-map-technology><option value="All">Tất cả</option>${technologies.map(item => `<option ${item === technology ? "selected" : ""}>${item}</option>`).join("")}</select>${icon("down")}</label>` : "";
  const activeSellers = sellers.filter(seller => seller.status === "Active").length;
  const totalOrders = orders.length;
  return `${pageHeading("MARKETPLACE FOOTPRINT", "Bản đồ hệ sinh thái", "Phân bố seller và đơn hàng theo các trung tâm in 3D tại Việt Nam.", `<label class="select-box map-select"><span>Lớp bản đồ</span><select data-map-filter-select>${["Sellers", "Orders", "Revenue", "Printing Technology"].map(item => `<option value="${item}" ${mapFilter === item ? "selected" : ""}>${item === "Sellers" ? "Xưởng in" : item === "Orders" ? "Đơn hàng" : item === "Revenue" ? "Doanh thu" : "Công nghệ in"}</option>`).join("")}</select>${icon("down")}</label>`)}<div class="demo-strip">${icon("location")} Bản đồ minh họa · vị trí seller được làm mờ theo khu vực, không phải vị trí GPS</div><div class="map-filter-row">${filterButtons}${techSelect}</div><div class="map-layout"><section class="map-panel" aria-label="Bản đồ phân bố seller và đơn hàng"><div class="map-water"></div><span class="map-landmark hanoi">HÀ NỘI</span><span class="map-landmark danang">ĐÀ NẴNG</span><span class="map-landmark hcm">TP. HỒ CHÍ MINH</span><svg class="map-route" viewBox="0 0 900 520" preserveAspectRatio="none" aria-hidden="true"><path d="M490 120 C530 180 560 230 585 300 C610 355 630 405 650 455"/><path d="M455 135 C480 200 505 230 550 285 C590 335 610 385 625 435"/></svg>${sellerMarkers}${orderMarkers}<div class="map-legend"><span><i></i> Seller / xưởng in</span><span><i></i> Cụm đơn hàng</span></div></section><aside class="map-side-card"><h3>Tổng quan mạng lưới</h3><p>Số liệu mock theo 3 cụm thành phố lớn.</p><div class="map-stat"><span>Xưởng in đang hoạt động</span><strong>${activeSellers} / ${sellers.length}</strong></div><div class="map-stat"><span>Đơn hàng trong mẫu</span><strong>${totalOrders}</strong></div><div class="map-stat"><span>GMV mẫu</span><strong>${compactMoney(128460000)}</strong></div><div class="map-stat"><span>Công nghệ nổi bật</span><strong>FDM · SLA</strong></div><div class="map-stat"><span>Khu vực</span><strong>Hà Nội · Đà Nẵng · TP.HCM</strong></div><div class="map-hint">${icon("eye")} Chọn một marker để xem thông tin chi tiết.</div></aside></div>`;
}
