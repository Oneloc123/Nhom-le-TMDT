import { users, sellers, products, orders, transactions, depositTransactions, reviews } from "../data.js";
import { actionButton, avatarName, badge, dateLabel, dateTimeLabel, esc, icon, money, pageHeading, pager, panel, searchBox, selectBox, statusTone, table } from "../components.js";

const findSeller = id => sellers.find(item => item.id === id);
const findUser = id => users.find(item => item.id === id);
const findProduct = id => products.find(item => item.id === id);
const pill = text => `<span class="tag">${esc(text)}</span>`;
const searchForm = (state, placeholder) => `<form class="search-form" data-form="page-search">${searchBox(state.search || "", placeholder)}<button class="button secondary" type="submit">Tìm</button></form>`;
const toolbar = (state, placeholder, filters = []) => `<div class="table-toolbar"><div class="toolbar-left">${searchForm(state, placeholder)}</div><div class="toolbar-right">${filters.join("")}</div></div>`;

function pageRows(items, state, searchFields, filterField, perPage = 7, extraFilter = () => true) {
  const query = String(state.search || "").trim().toLocaleLowerCase("vi");
  const filters = state.filters || {};
  let result = items.filter(item => (!query || searchFields(item).some(value => String(value || "").toLocaleLowerCase("vi").includes(query))) && (!filters[filterField] || filters[filterField] === "All" || item[filterField] === filters[filterField]) && extraFilter(item));
  if (state.sortKey) {
    const key = state.sortKey;
    result = [...result].sort((a, b) => {
      const av = a[key] ?? "", bv = b[key] ?? "";
      return (typeof av === "number" ? av - bv : String(av).localeCompare(String(bv), "vi")) * (state.sortDir || 1);
    });
  }
  const page = Math.min(Math.max(1, state.page || 1), Math.max(1, Math.ceil(result.length / perPage)));
  return { rows: result.slice((page - 1) * perPage, page * perPage), total: result.length, page };
}

function tablePanel(title, subtitle, columns, result, perPage, emptyTitle) {
  return panel(title, subtitle, `${table(columns, result.rows, emptyTitle)}${pager(result.page, result.total, perPage)}`);
}

export function renderUsers(state) {
  const filters = [selectBox("Trạng thái", "status", ["Active", "Suspended", "Inactive"], state.filters?.status || "All")];
  const result = pageRows(users, state, user => [user.id, user.name, user.email, user.phone], "status");
  const columns = [
    { label: "Khách hàng", render: row => avatarName(row.name, row.color, row.email) },
    { label: "User ID", key: "id", sort: "id" },
    { label: "Điện thoại", key: "phone" },
    { label: "Đơn hàng", render: row => `<span class="cell-number">${row.orders}</span>` },
    { label: "Tổng chi tiêu", render: row => `<span class="cell-primary">${money(row.totalSpent)}</span>` },
    { label: "Ngày tham gia", render: row => dateLabel(row.joined), sort: "joined" },
    { label: "Trạng thái", render: row => badge(row.status, statusTone(row.status)) },
    { label: "Thao tác", render: row => `<div class="row-actions">${actionButton("view", row.id, "Xem hồ sơ")}<button class="icon-button ${row.status === "Active" ? "danger" : ""}" data-action="user-toggle" data-record="${row.id}" aria-label="${row.status === "Active" ? "Tạm khóa" : "Kích hoạt"}" title="${row.status === "Active" ? "Tạm khóa" : "Kích hoạt"}">${icon(row.status === "Active" ? "logout" : "check")}</button></div>` }
  ];
  return `${pageHeading("QUẢN LÝ NGƯỜI DÙNG", "Người dùng", "Theo dõi hồ sơ khách hàng, hoạt động mua sắm và lịch sử giao dịch.")}${toolbar(state, "Tìm tên, email, mã khách hàng…", filters)}${tablePanel("Danh sách khách hàng", `${users.length} tài khoản trong marketplace`, columns, result, 7, "Không tìm thấy khách hàng")}`;
}

export function renderSellers(state) {
  const filters = [selectBox("Trạng thái", "status", ["Active", "Suspended", "Inactive"], state.filters?.status || "All"), selectBox("Khu vực", "city", ["TP. Hồ Chí Minh", "Hà Nội", "Đà Nẵng"], state.filters?.city || "All")];
  const result = pageRows(sellers, state, seller => [seller.id, seller.name, seller.ownerName, seller.location, seller.email], "status", 7, seller => !state.filters?.city || state.filters.city === "All" || seller.city === state.filters.city);
  const columns = [
    { label: "Seller", render: row => avatarName(row.ownerName, row.color, row.id) },
    { label: "Shop name", render: row => `<span class="cell-primary">${esc(row.name)}</span><div class="cell-muted">${esc(row.location)}</div>`, sort: "name" },
    { label: "Sản phẩm", render: row => `<span class="cell-number">${row.products}</span>` },
    { label: "Đơn hàng", render: row => `<span class="cell-number">${row.orders}</span>` },
    { label: "Doanh thu", render: row => `<span class="cell-primary">${money(row.revenue)}</span>`, sort: "revenue" },
    { label: "Đánh giá", render: row => `★ ${row.rating.toFixed(1)} <span class="cell-muted">(${row.reviews})</span>` },
    { label: "Tiền cọc", render: row => `<span class="cell-primary">${money(row.deposit)}</span><div class="cell-muted">Yêu cầu ${money(row.requiredDeposit)}</div>` },
    { label: "Trạng thái", render: row => badge(row.status, statusTone(row.status)) },
    { label: "Thao tác", render: row => `<div class="row-actions">${actionButton("view", row.id, "Xem hồ sơ seller")}<button class="icon-button ${row.status === "Active" ? "danger" : ""}" data-action="seller-toggle" data-record="${row.id}" aria-label="${row.status === "Active" ? "Tạm ngưng" : "Kích hoạt"}" title="${row.status === "Active" ? "Tạm ngưng" : "Kích hoạt"}">${icon(row.status === "Active" ? "logout" : "check")}</button></div>` }
  ];
  return `${pageHeading("QUẢN LÝ ĐỐI TÁC", "Seller / Xưởng in", "Theo dõi năng lực xưởng, hiệu suất kinh doanh và số dư tiền cọc.", `<button class="button secondary" data-action="export" data-export="sellers">${icon("download")} Xuất danh sách</button>`)}${toolbar(state, "Tìm shop, chủ xưởng, địa chỉ…", filters)}${tablePanel("Đối tác marketplace", "Seller hoạt động không cần bước Admin phê duyệt.", columns, result, 7, "Không tìm thấy seller")}`;
}

export function renderProducts(state) {
  const filters = [selectBox("Loại sản phẩm", "type", ["READY-MADE", "CUSTOM PRINTING SERVICE"], state.filters?.type || "All"), selectBox("Trạng thái", "status", ["Pending Review", "Published", "Rejected", "Hidden", "Draft"], state.filters?.status || "All")];
  let items = products.filter(item => {
    const query = String(state.search || "").toLocaleLowerCase("vi");
    return (!query || [item.name, item.id, findSeller(item.sellerId)?.name, item.category].some(value => String(value || "").toLocaleLowerCase("vi").includes(query)))
      && (!state.filters?.type || state.filters.type === "All" || item.type === state.filters.type)
      && (!state.filters?.status || state.filters.status === "All" || item.status === state.filters.status);
  });
  if (state.sortKey) items = [...items].sort((a, b) => String(a[state.sortKey] ?? "").localeCompare(String(b[state.sortKey] ?? ""), "vi") * (state.sortDir || 1));
  const page = Math.min(Math.max(1, state.page || 1), Math.max(1, Math.ceil(items.length / 7)));
  const result = { rows: items.slice((page - 1) * 7, page * 7), total: items.length, page };
  const columns = [
    { label: "Sản phẩm", render: row => `<div class="product-cell"><span class="product-visual tone-${esc(row.tone || "violet")}">${esc(row.imageLabel || "3D PRINT")}</span><span class="product-cell-copy"><strong>${esc(row.name)}</strong><small>${esc(row.id)} · ${esc(findSeller(row.sellerId)?.name || "—")}</small></span></div>` },
    { label: "Loại", render: row => badge(row.type === "READY-MADE" ? "Ready-made" : "Custom print", row.type === "READY-MADE" ? "neutral" : "warning") },
    { label: "Danh mục", key: "category" },
    { label: "Giá", render: row => `<span class="cell-primary">${money(row.price)}</span>` },
    { label: "Trạng thái", render: row => badge(row.status, statusTone(row.status)) },
    { label: "Ngày tạo", render: row => dateLabel(row.created), sort: "created" },
    { label: "Thao tác", render: row => {
      const review = row.status === "Pending Review" ? `<button class="icon-button" data-action="product-approve" data-record="${row.id}" aria-label="Duyệt sản phẩm" title="Duyệt">${icon("check")}</button><button class="icon-button danger" data-action="product-reject" data-record="${row.id}" aria-label="Từ chối sản phẩm" title="Từ chối">${icon("close")}</button>` : row.status === "Published" ? `<button class="icon-button" data-action="product-hide" data-record="${row.id}" aria-label="Ẩn sản phẩm" title="Ẩn">${icon("eye")}</button>` : row.status === "Hidden" ? `<button class="icon-button" data-action="product-unhide" data-record="${row.id}" aria-label="Hiện sản phẩm" title="Hiện">${icon("check")}</button>` : "";
      return `<div class="row-actions">${actionButton("view", row.id, "Xem sản phẩm")}${review}</div>`;
    } }
  ];
  const pending = products.filter(item => item.status === "Pending Review").length;
  return `${pageHeading("CATALOG CONTROL", "Sản phẩm", "Review catalog trước khi public; quản trị viên không chỉnh sửa nội dung của seller.", `<button class="button secondary" data-action="export" data-export="products">${icon("download")} Xuất danh sách</button>`)}<div class="demo-strip">${icon("spark")} ${pending} sản phẩm đang chờ review · Seller tự quản lý nội dung sản phẩm</div>${toolbar(state, "Tìm sản phẩm, shop, danh mục…", filters)}${tablePanel("Catalog marketplace", "Ready-made · Custom printing service", columns, result, 7, "Không tìm thấy sản phẩm")}`;
}

export function renderOrders(state) {
  const filters = [selectBox("Loại đơn", "type", ["READY-MADE", "CUSTOM PRINT"], state.filters?.type || "All"), selectBox("Trạng thái", "status", [...new Set(orders.map(item => item.status))], state.filters?.status || "All")];
  let items = orders.filter(item => {
    const customer = findUser(item.customerId), seller = findSeller(item.sellerId);
    const query = String(state.search || "").toLocaleLowerCase("vi");
    return (!query || [item.id, customer?.name, customer?.email, seller?.name, item.status].some(value => String(value || "").toLocaleLowerCase("vi").includes(query)))
      && (!state.filters?.type || state.filters.type === "All" || item.type === state.filters.type)
      && (!state.filters?.status || state.filters.status === "All" || item.status === state.filters.status);
  });
  if (state.sortKey) items = [...items].sort((a, b) => String(a[state.sortKey] ?? "").localeCompare(String(b[state.sortKey] ?? ""), "vi") * (state.sortDir || 1));
  const page = Math.min(Math.max(1, state.page || 1), Math.max(1, Math.ceil(items.length / 7)));
  const result = { rows: items.slice((page - 1) * 7, page * 7), total: items.length, page };
  const columns = [
    { label: "Order ID", key: "id", sort: "id", render: row => `<span class="cell-primary">${esc(row.id)}</span>` },
    { label: "Khách hàng", render: row => avatarName(findUser(row.customerId)?.name || "Khách hàng", findUser(row.customerId)?.color, findUser(row.customerId)?.email) },
    { label: "Seller", render: row => esc(findSeller(row.sellerId)?.name || "—") },
    { label: "Loại đơn", render: row => badge(row.type === "READY-MADE" ? "Ready-made" : "Custom print", row.type === "READY-MADE" ? "neutral" : "warning") },
    { label: "Giá trị", render: row => `<span class="cell-primary">${money(row.amount)}</span>` },
    { label: "Thanh toán", render: row => badge(row.payment, statusTone(row.payment)) },
    { label: "Trạng thái", render: row => badge(row.status, statusTone(row.status)) },
    { label: "Thời gian", render: row => dateTimeLabel(row.created), sort: "created" },
    { label: "Thao tác", render: row => actionButton("view", row.id, "Xem đơn hàng") }
  ];
  return `${pageHeading("ORDER MONITORING", "Đơn hàng", "Giám sát vòng đời đơn hàng marketplace. Admin chỉ xem, không thay đổi trạng thái.", `<button class="button secondary" data-action="export" data-export="orders">${icon("download")} Xuất danh sách</button>`)}${toolbar(state, "Tìm mã đơn, khách hàng, seller…", filters)}<div class="demo-strip">${icon("eye")} Chế độ giám sát · Toàn bộ cập nhật trạng thái thuộc về seller và khách hàng</div>${tablePanel("Tất cả đơn hàng", `${orders.length} đơn · Ready-made & Custom print`, columns, result, 7, "Không tìm thấy đơn hàng")}`;
}

function detailFields(fields) {
  return `<div class="detail-grid">${fields.map(([label, value, wide]) => `<div class="detail-field ${wide ? "wide" : ""}"><small>${esc(label)}</small><strong>${value}</strong></div>`).join("")}</div>`;
}

function reviewMarkup(review) {
  const customer = findUser(review.userId);
  return `<div class="review-item"><div class="review-head">${avatarName(customer?.name || "Khách hàng", customer?.color, dateLabel(review.date))}<span class="review-stars">${"★".repeat(review.rating)}${"☆".repeat(5 - review.rating)}</span></div><p>${esc(review.text)}</p></div>`;
}

export function renderEntityDetail(type, item) {
  if (type === "users") type = "user";
  if (type === "sellers") type = "seller";
  if (type === "products") type = "product";
  if (type === "orders") type = "order";
  if (type === "user") {
    const userOrders = orders.filter(order => order.customerId === item.id);
    const userTransactions = transactions.filter(transaction => transaction.customerId === item.id);
    const userReviews = reviews.filter(review => review.userId === item.id);
    return `<div class="detail-intro">${avatarName(item.name, item.color, item.id)}${badge(item.status, statusTone(item.status))}</div>${detailFields([["Email", esc(item.email)], ["Điện thoại", esc(item.phone)], ["Ngày tham gia", dateLabel(item.joined)], ["Tổng đơn", String(item.orders)], ["Tổng chi tiêu", money(item.totalSpent)], ["Trạng thái", badge(item.status, statusTone(item.status))]])}<h3 class="mini-heading">Lịch sử đơn hàng <span class="cell-muted">(${userOrders.length})</span></h3>${userOrders.length ? `<div class="mini-list">${userOrders.map(order => `<div class="mini-list-row"><span><strong>${esc(order.id)}</strong><small>${dateLabel(order.created)} · ${order.type === "READY-MADE" ? "Ready-made" : "Custom print"}</small></span><span>${money(order.amount)}</span>${badge(order.status, statusTone(order.status))}</div>`).join("")}</div>` : `<p class="muted-note">Khách hàng chưa có đơn hàng trong dữ liệu demo.</p>`}<h3 class="mini-heading">Lịch sử thanh toán</h3>${userTransactions.length ? `<div class="mini-list">${userTransactions.map(tx => `<div class="mini-list-row"><span><strong>${esc(tx.id)}</strong><small>${dateLabel(tx.date)} · ${esc(tx.status)}</small></span><span>${money(tx.gross)}</span></div>`).join("")}</div>` : `<p class="muted-note">Chưa ghi nhận giao dịch.</p>`}<h3 class="mini-heading">Đánh giá</h3>${userReviews.length ? userReviews.map(reviewMarkup).join("") : `<p class="muted-note">Chưa có đánh giá.</p>`}`;
  }
  if (type === "seller") {
    const sellerProducts = products.filter(product => product.sellerId === item.id);
    const sellerOrders = orders.filter(order => order.sellerId === item.id);
    const sellerReviews = reviews.filter(review => review.sellerId === item.id);
    const sellerDepositHistory = depositTransactions.filter(transaction => transaction.sellerId === item.id);
    const technologyTags = (item.technology || []).map(pill).join("");
    return `<div class="detail-intro">${avatarName(item.ownerName, item.color, item.id)}<span><strong>${esc(item.name)}</strong><small>${esc(item.location)}</small></span>${badge(item.status, statusTone(item.status))}</div><div class="detail-grid">${[["Chủ xưởng", esc(item.ownerName)], ["Email liên hệ", esc(item.email)], ["Điện thoại", esc(item.phone)], ["Địa chỉ", esc(item.address), true], ["Ngày tham gia", dateLabel(item.joined)], ["Khu vực", esc(item.city)]].map(([label, value, wide]) => `<div class="detail-field ${wide ? "wide" : ""}"><small>${label}</small><strong>${value}</strong></div>`).join("")}</div><h3 class="mini-heading">Năng lực xưởng</h3><div class="tag-row">${technologyTags || pill("Chưa khai báo")}</div><h3 class="mini-heading">Hiệu suất</h3><div class="detail-grid">${[["Đơn hàng", item.orders], ["Doanh thu", money(item.revenue)], ["Đánh giá", `★ ${item.rating.toFixed(1)} · ${item.reviews} nhận xét`], ["Sản phẩm", item.products], ["Số dư tiền cọc", money(item.deposit)], ["Mức cọc yêu cầu", money(item.requiredDeposit)]].map(([label, value]) => `<div class="detail-field"><small>${label}</small><strong>${value}</strong></div>`).join("")}</div><h3 class="mini-heading">Lịch sử tiền cọc</h3><p class="muted-note">${item.deposit >= item.requiredDeposit ? "Số dư hiện đáp ứng mức cọc tham khảo." : `Còn thiếu ${money(item.requiredDeposit - item.deposit)} so với mức tham khảo.`} Các giá trị đều là dữ liệu demo.</p>${sellerDepositHistory.length ? `<div class="mini-list">${sellerDepositHistory.map(transaction => `<div class="mini-list-row"><span><strong>${esc(transaction.id)} · ${esc(transaction.type)}</strong><small>${dateLabel(transaction.date)} · ${esc(transaction.note)}</small></span><span class="${transaction.amount < 0 ? "refund-value" : "cell-primary"}">${money(transaction.amount)}</span></div>`).join("")}</div>` : `<p class="muted-note">Chưa có giao dịch tiền cọc.</p>`}<h3 class="mini-heading">Sản phẩm của xưởng</h3>${sellerProducts.length ? `<div class="mini-list">${sellerProducts.map(product => `<div class="mini-list-row"><span><strong>${esc(product.name)}</strong><small>${esc(product.id)} · ${product.type === "READY-MADE" ? "Ready-made" : "Custom print"}</small></span>${badge(product.status, statusTone(product.status))}</div>`).join("")}</div>` : `<p class="muted-note">Chưa có sản phẩm.</p>`}<h3 class="mini-heading">Đơn hàng gần đây · ${sellerOrders.length}</h3>${sellerOrders.slice(0, 3).map(order => `<div class="mini-list-row"><span><strong>${esc(order.id)}</strong><small>${dateLabel(order.created)}</small></span><span>${money(order.amount)}</span>${badge(order.status, statusTone(order.status))}</div>`).join("") || `<p class="muted-note">Chưa có đơn hàng.</p>`}<h3 class="mini-heading">Đánh giá gần đây</h3>${sellerReviews.length ? sellerReviews.map(reviewMarkup).join("") : `<p class="muted-note">Chưa có đánh giá.</p>`}<p class="muted-note map-note">${icon("location")} Vị trí trên bản đồ: ${esc(item.location)} · Vị trí mock, chưa dùng dịch vụ bản đồ.</p>`;
  }
  if (type === "product") {
    const seller = findSeller(item.sellerId);
    const common = `<div class="detail-intro"><span class="product-visual tone-${esc(item.tone || "violet")}">${esc(item.imageLabel || "3D PRINT")}</span><span><strong>${esc(item.name)}</strong><small>${esc(item.id)} · ${esc(seller?.name || "—")}</small></span>${badge(item.status, statusTone(item.status))}</div>${detailFields([["Seller", esc(seller?.name || "—")], ["Loại sản phẩm", esc(item.type)], ["Danh mục", esc(item.category)], ["Giá tham khảo", money(item.price)], ["Ngày tạo", dateLabel(item.created)], ["Mô tả", esc(item.description), true]])}`;
    if (item.type === "READY-MADE") return `${common}<h3 class="mini-heading">Thông tin ready-made</h3>${detailFields([["Kho khả dụng", `${item.stock ?? 0} sản phẩm`], ["Đánh giá", `★ ${(item.rating || 0).toFixed(1)}`], ["Hình ảnh", "Ảnh mock catalog · chưa tải asset sản phẩm thật", true]])}`;
    return `${common}<h3 class="mini-heading">Thông số dịch vụ in</h3><div class="detail-grid">${[["Công nghệ", (item.technologies || []).join(", ")], ["Vật liệu", (item.materials || []).join(", ")], ["Màu sắc", (item.colors || []).join(", ")], ["Chất lượng bề mặt", (item.surface || []).join(", ")], ["Kích thước hỗ trợ", esc(item.dimensions || "—"), true], ["Cách tính giá", esc(item.pricing || "—"), true], ["Ảnh mẫu", (item.examples || []).map(esc).join(" · ") || "Mock preview", true]].map(([label, value, wide]) => `<div class="detail-field ${wide ? "wide" : ""}"><small>${label}</small><strong>${value}</strong></div>`).join("")}</div>${item.status === "Rejected" && item.rejectionReason ? `<h3 class="mini-heading">Lý do từ chối</h3><p class="muted-note">${esc(item.rejectionReason)}</p>` : ""}`;
  }
  if (type === "order") {
    const customer = findUser(item.customerId), seller = findSeller(item.sellerId), product = findProduct(item.productId);
    const base = `<div class="detail-intro"><span><strong>${esc(item.id)}</strong><small>${dateTimeLabel(item.created)} · ${item.type === "READY-MADE" ? "Ready-made" : "Custom print"}</small></span>${badge(item.status, statusTone(item.status))}<span class="spacer"></span>${badge(item.payment, statusTone(item.payment))}</div><div class="two-column"><div><h3 class="mini-heading first">Thông tin khách hàng</h3>${detailFields([["Khách hàng", esc(customer?.name || "—")], ["Email", esc(customer?.email || "—")], ["Điện thoại", esc(customer?.phone || "—")]])}<h3 class="mini-heading">Seller</h3>${detailFields([["Xưởng", esc(seller?.name || "—")], ["Chủ xưởng", esc(seller?.ownerName || "—")], ["Địa chỉ", esc(seller?.location || "—"), true]])}<h3 class="mini-heading">Sản phẩm</h3>${detailFields([["Sản phẩm", esc(product?.name || item.items?.[0]?.name || "—"), true], ["Loại", esc(item.type)], ["Số mặt hàng", `${item.items?.length || 0} dòng`]])}</div><div><h3 class="mini-heading first">Chi tiết thanh toán</h3>${detailFields([["Tạm tính", money(item.breakdown?.subtotal)], ["Phí vận chuyển", money(item.breakdown?.shipping)], ["Giảm giá", money(item.breakdown?.discount)], ["Tổng cộng", `<span class="cell-primary">${money(item.breakdown?.total)}</span>`], ["Thanh toán", esc(item.payment)], ["Phương thức", "Mock · Chuyển khoản / ví"]])}<h3 class="mini-heading">Vận chuyển</h3><div class="detail-field"><small>Trạng thái giao hàng</small><strong>${esc(item.shippingInfo || "Chưa có cập nhật")}</strong></div></div></div><h3 class="mini-heading">Sản phẩm trong đơn</h3><div class="mini-list">${(item.items || []).map(line => `<div class="mini-list-row"><span><strong>${esc(line.name)}</strong><small>Số lượng: ${line.quantity} × ${money(line.unitPrice)}</small></span><span>${money(line.quantity * line.unitPrice)}</span></div>`).join("")}</div>${item.custom ? `<h3 class="mini-heading">Thông tin file in theo yêu cầu</h3>${detailFields([["Tên file", `${icon("file")} ${esc(item.custom.fileName)}`], ["Kích thước X / Y / Z", esc(item.custom.dimensions)], ["Thể tích", esc(item.custom.volume)], ["Số lượng", String(item.custom.quantity)], ["Tỷ lệ", esc(item.custom.scale)], ["Công nghệ", esc(item.custom.technology)], ["Vật liệu", esc(item.custom.material)], ["Màu sắc", esc(item.custom.color)], ["Bề mặt", esc(item.custom.surface)], ["Báo giá seller", item.custom.quote ? money(item.custom.quote) : "Đang chờ seller báo giá"]])}` : ""}<h3 class="mini-heading">Timeline trạng thái <span class="muted-note">(chỉ giám sát)</span></h3><div class="timeline">${(item.timeline || []).map((step, index) => `<div class="timeline-item done">${esc(step)}${index === item.timeline.length - 1 ? `<small> · Cập nhật gần nhất</small>` : ""}</div>`).join("")}</div><div class="read-only-note">${icon("eye")} Admin chỉ theo dõi đơn hàng; trạng thái do khách hàng và seller cập nhật.</div>`;
  }
  return `<div class="empty-state"><strong>Không tìm thấy dữ liệu</strong></div>`;
}

export function lookupEntity(id) {
  return users.find(row => row.id === id) || sellers.find(row => row.id === id) || products.find(row => row.id === id) || orders.find(row => row.id === id) || null;
}

export function collectionForType(type) {
  return ({ users, user: users, sellers, seller: sellers, products, product: products, orders, order: orders })[type] || [];
}
