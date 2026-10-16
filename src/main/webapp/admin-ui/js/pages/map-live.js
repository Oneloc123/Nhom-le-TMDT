import { orders, products, sellers, technologies, transactions } from "../data.js";
import { compactMoney, icon, pageHeading } from "../components.js";

const fallbackPositions = [[53, 23], [63, 54], [69, 78]];
const positionForSeller = (seller, index) => seller.coords?.length === 2
  ? [Math.max(10, Math.min(90, seller.coords[0] / 9)), Math.max(10, Math.min(90, seller.coords[1] / 5.2))]
  : fallbackPositions[index % fallbackPositions.length];

function groupOrdersByCity() {
  const groups = new Map();
  for (const order of orders) {
    const seller = sellers.find(item => item.id === order.sellerId);
    const city = seller?.city || "Other";
    const group = groups.get(city) || { city, count: 0, amount: 0, types: {}, coords: seller?.coords };
    group.count += 1;
    group.amount += Number(order.amount || 0);
    group.types[order.type || "Other"] = (group.types[order.type || "Other"] || 0) + 1;
    if (!group.coords && seller?.coords) group.coords = seller.coords;
    groups.set(city, group);
  }
  return [...groups.values()].map((group, index) => ({
    ...group,
    dominant: Object.entries(group.types).sort((a, b) => b[1] - a[1])[0]?.[0] || "—",
    position: group.coords
      ? [Math.max(12, Math.min(86, group.coords[0] / 9)), Math.max(12, Math.min(88, group.coords[1] / 5.2))]
      : fallbackPositions[index % fallbackPositions.length]
  }));
}

export function renderMap(state) {
  const filter = state.mapFilter || "Sellers";
  const technology = state.technologyFilter || "All";
  const selected = filter === "Printing Technology"
    ? sellers.filter(seller => technology === "All" || (seller.technology || []).includes(technology))
    : sellers;
  const sellerMarkers = filter === "Orders" ? "" : selected.map((seller, index) => {
    const [left, top] = positionForSeller(seller, index);
    const label = filter === "Revenue" ? compactMoney(seller.revenue || 0) : seller.name;
    return `<button class="map-marker" style="left:${left}%;top:${top}%" data-action="map-seller" data-record="${seller.id}" aria-label="View ${seller.name}" title="${seller.name} · ${seller.location}">${icon("shop")}</button>${filter === "Revenue" ? `<span class="map-revenue-label" style="left:${left + 1.5}%;top:${top + 1.5}%">${label}</span>` : ""}`;
  }).join("");
  const orderGroups = groupOrdersByCity();
  const orderMarkers = filter === "Sellers" || filter === "Printing Technology" ? "" : orderGroups.map(group =>
    `<button class="map-marker order" style="left:${group.position[0]}%;top:${group.position[1]}%" data-action="map-orders" data-record="${group.city}" aria-label="${group.count} orders in ${group.city}" title="${group.count} orders · ${group.city}">${icon("box")}</button><span class="map-cluster-label" style="left:${group.position[0] + 1.5}%;top:${group.position[1] + 1.5}%">${group.count}</span>`).join("");
  const filters = ["Sellers", "Orders", "Revenue", "Printing Technology"];
  const filterButtons = filters.map(value => `<button class="map-filter-chip ${filter === value ? "active" : ""}" data-map-filter="${value}">${value}</button>`).join("");
  const select = filter === "Printing Technology" ? `<label class="select-box"><span>Technology</span><select data-map-technology><option value="All">All</option>${technologies.map(item => `<option ${item === technology ? "selected" : ""}>${item}</option>`).join("")}</select>${icon("down")}</label>` : "";
  const active = sellers.filter(seller => seller.status === "Active").length;
  const gmv = transactions.reduce((sum, row) => sum + Number(row.gross || 0), 0);
  const cityRows = groupOrdersByCity().map(group => `<div class="map-stat"><span>${group.city}</span><strong>${group.count} · ${compactMoney(group.amount)}</strong></div>`).join("");
  const technologySummary = [...new Set(sellers.flatMap(seller => seller.technology || []))].join(" · ") || "—";
  const layerOptions = filters.map(value => `<option value="${value}" ${filter === value ? "selected" : ""}>${value}</option>`).join("");
  const map = `<div class="map-layout"><section class="map-panel" aria-label="Seller and order distribution"><div class="map-water"></div><svg class="map-route" viewBox="0 0 900 520" preserveAspectRatio="none" aria-hidden="true"><path d="M490 120 C530 180 560 230 585 300 C610 355 630 405 650 455"/><path d="M455 135 C480 200 505 230 550 285 C590 335 610 385 625 435"/></svg>${sellerMarkers}${orderMarkers}<div class="map-legend"><span><i></i> Sellers</span><span><i></i> Order groups</span></div></section><aside class="map-side-card"><h3>Marketplace footprint</h3><p>Groups and totals are calculated from the loaded database records.</p><div class="map-stat"><span>Active sellers</span><strong>${active} / ${sellers.length}</strong></div><div class="map-stat"><span>Orders</span><strong>${orders.length}</strong></div><div class="map-stat"><span>Transaction GMV</span><strong>${compactMoney(gmv)}</strong></div><div class="map-stat"><span>Seller technologies</span><strong>${technologySummary}</strong></div>${cityRows}<div class="map-hint">${icon("eye")} Select a marker to inspect its records.</div></aside></div>`;
  return `${pageHeading("MARKETPLACE FOOTPRINT", "Seller and order map", "Seller locations and order groups come from the MySQL workspace records.", `<label class="select-box map-select"><span>Layer</span><select data-map-filter-select>${layerOptions}</select>${icon("down")}</label>`)}<div class="map-filter-row">${filterButtons}${select}</div>${map}`;
}
