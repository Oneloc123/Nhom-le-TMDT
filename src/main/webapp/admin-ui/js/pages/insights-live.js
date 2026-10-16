import { materials, orders, products, sellers, technologies, transactions } from "../data.js";
import { barChart, donutChart, icon, lineChart, money, pageHeading, panel } from "../components.js";

const ranges = [["7D", "7D"], ["30D", "30D"], ["3M", "3M"], ["6M", "6M"], ["1Y", "1Y"], ["CUSTOM", "Custom"]];
const timeOf = value => {
  const parsed = Date.parse(value || "");
  return Number.isFinite(parsed) ? parsed : 0;
};
const tally = values => values.reduce((counts, value) => {
  if (value) counts[value] = (counts[value] || 0) + 1;
  return counts;
}, {});
const chartValues = (rows, dateField, valueField) => {
  const grouped = rows.reduce((result, row) => {
    const day = String(row[dateField] || "").slice(0, 10);
    if (day) result[day] = (result[day] || 0) + Number(row[valueField] || 0);
    return result;
  }, {});
  const values = Object.keys(grouped).sort().slice(-12).map(day => grouped[day]);
  if (!values.length) return [0, 0];
  return values.length === 1 ? [values[0], values[0]] : values;
};
const percentages = counts => {
  const total = Object.values(counts).reduce((sum, value) => sum + value, 0) || 1;
  return Object.entries(counts).map(([label, value]) => ({ label, value: Math.round(value * 100 / total) }));
};

export function renderAnalytics(state) {
  const active = state.analyticsRange || "30D";
  const days = ({ "7D": 7, "30D": 30, "3M": 90, "6M": 180, "1Y": 365 })[active];
  const end = active === "CUSTOM" ? timeOf(state.customEnd || new Date().toISOString().slice(0, 10)) + 86_399_999 : Date.now();
  const start = active === "CUSTOM" ? timeOf(state.customStart || new Date(end - 30 * 86_400_000).toISOString().slice(0, 10)) : end - (days || 30) * 86_400_000;
  const inRange = row => { const date = timeOf(row.date || row.created); return date >= start && date <= end; };
  const periodTransactions = transactions.filter(inRange);
  const periodOrders = orders.filter(inRange);
  const gmv = periodTransactions.reduce((sum, row) => sum + Number(row.gross || 0), 0);
  const orderCounts = tally(orders.map(order => order.type));
  const statusCounts = tally(periodOrders.map(order => order.status));
  const technologyCounts = tally(products.flatMap(product => product.technologies || []));
  const materialCounts = tally(products.flatMap(product => product.materials || []));
  const topSellers = [...sellers].sort((a, b) => Number(b.revenue || 0) - Number(a.revenue || 0)).slice(0, 5)
    .map(seller => ({ label: seller.name, value: Number(seller.revenue || 0), display: seller.revenue ? money(seller.revenue) : "0" }));
  const topProducts = [...products].sort((a, b) => Number(b.rating || 0) - Number(a.rating || 0)).slice(0, 5)
    .map(product => ({ label: product.name, value: Number(product.rating || 0) * 20, display: `★ ${Number(product.rating || 0).toFixed(1)}` }));
  const cancelled = periodOrders.filter(order => ["Cancelled", "Refunded"].includes(order.status)).length;
  const cancellationRate = periodOrders.length ? (cancelled * 100 / periodOrders.length).toFixed(1) : "0.0";
  const aov = periodOrders.length ? Math.round(periodOrders.reduce((sum, order) => sum + Number(order.amount || 0), 0) / periodOrders.length) : 0;
  const filter = `<div class="analytics-filter" role="group" aria-label="Date range">${ranges.map(([value, label]) => `<button class="${active === value ? "active" : ""}" data-analytics-range="${value}">${label}</button>`).join("")}</div>`;
  const customRange = active === "CUSTOM" ? `<div class="custom-date-range"><label>From <input type="date" value="${state.customStart || ""}" data-range-date="start"></label><label>To <input type="date" value="${state.customEnd || ""}" data-range-date="end"></label></div>` : "";
  const stats = `<div class="analytics-stat-strip"><div class="analytics-stat"><small>GMV in range</small><strong>${money(gmv)}</strong><span>${periodTransactions.length} transactions</span></div><div class="analytics-stat"><small>Orders in range</small><strong>${periodOrders.length}</strong><span>MySQL records</span></div><div class="analytics-stat"><small>Cancelled or refunded</small><strong>${cancellationRate}%</strong><span>${cancelled} of ${periodOrders.length} orders</span></div><div class="analytics-stat"><small>Average order value</small><strong>${money(aov)}</strong><span>Calculated from loaded orders</span></div></div>`;
  const orderTypeData = percentages(orderCounts);
  const statusData = percentages(statusCounts);
  const techData = percentages(technologyCounts);
  const materialData = percentages(materialCounts);
  return `${pageHeading("INSIGHTS & REPORTS", "Marketplace analytics", "Metrics are calculated from the loaded orders, transactions, products, and sellers.", filter)}${customRange}<div class="demo-strip">${icon("spark")} Report values are derived from the selected database records.</div>${stats}<div class="analytics-grid"><div class="span-two">${panel("Revenue over time", "Transaction gross by recorded date", lineChart(chartValues(periodTransactions, "date", "gross"), "GMV"))}</div>${panel("Orders over time", "Order amounts by created date", lineChart(chartValues(periodOrders, "created", "amount"), "Orders"))}${panel("Order type", "Distribution of loaded orders", donutChart(orderTypeData, String(orders.length), "orders"))}${panel("Order status", "Distribution in selected range", donutChart(statusData, String(periodOrders.length), "orders"))}${panel("Printing technology", "Technology options listed by products", barChart(technologies.map(label => ({ label, value: techData.find(item => item.label === label)?.value || 0, display: `${techData.find(item => item.label === label)?.value || 0}%` }))))}${panel("Materials", "Materials listed by products", barChart(materials.map(label => ({ label, value: materialData.find(item => item.label === label)?.value || 0, display: `${materialData.find(item => item.label === label)?.value || 0}%` }))))}${panel("Top sellers", "Sorted by stored seller revenue", barChart(topSellers, "value"))}${panel("Top products", "Sorted by stored product rating", barChart(topProducts, "value"))}<div class="analytics-footnote">All counts and totals come from the current MySQL admin workspace records.</div></div>`;
}
