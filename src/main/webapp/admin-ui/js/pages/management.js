import { badge, dateLabel, dateTimeLabel, esc, icon, money, panel, table } from "../components.js";
import { queryString } from "../api.js";

const rowId = row => String(row.id ?? "");
const shown = value => value == null || value === "" ? "—" : String(value);
const statusTone = value => {
  const status = String(value || "").toUpperCase();
  if (["ACTIVE", "VERIFIED", "APPROVED", "COMPLETED", "ĐÃ HOÀN THÀNH", "ĐÃ XÁC NHẬN"].includes(status)) return "success";
  if (["PENDING", "PENDING_REVIEW", "PROCESSING", "ĐANG XỬ LÝ", "CHỜ THANH TOÁN", "ĐANG GIAO HÀNG"].includes(status)) return "warning";
  if (["LOCKED", "BLOCKED", "REJECTED", "CANCELLED", "INACTIVE", "ĐÃ HỦY"].includes(status)) return "danger";
  return "neutral";
};
const badgeFor = value => badge(shown(value), statusTone(value));

function heading(eyebrow, title, description) {
  return `<div class="page-heading"><div><div class="eyebrow">${esc(eyebrow)}</div><h1>${esc(title)}</h1><p>${esc(description)}</p></div></div>`;
}

function filterForm(route, contextPath, state, filters, placeholder = "Tìm theo mã, tên hoặc email…") {
  const current = state.filters || {};
  const selects = filters.map(filter => `<label class="select-box"><span>${esc(filter.label)}</span><select name="${esc(filter.key)}">
    <option value="All">Tất cả</option>${filter.options.map(option => {
      const value = typeof option === "string" ? option : option.value;
      const label = typeof option === "string" ? option : option.label;
      return `<option value="${esc(value)}" ${String(current[filter.key] || "All") === String(value) ? "selected" : ""}>${esc(label)}</option>`;
    }).join("")}</select></label>`).join("");
  const view = current.view ? `<input type="hidden" name="view" value="${esc(current.view)}">` : "";
  return `<form class="table-toolbar" method="get" action="${contextPath}${route}">
    <label class="search-box">${icon("search")}<input type="search" name="q" value="${esc(current.q || "")}" placeholder="${esc(placeholder)}" autocomplete="off"></label>
    ${selects}${view}<input type="hidden" name="page" value="1"><button class="button secondary" type="submit">Lọc</button>
    <a class="button secondary" href="${contextPath}${route}${current.view ? `?view=${encodeURIComponent(current.view)}` : ""}">Xóa lọc</a></form>`;
}

function pagination(page, pages, total, state, contextPath, route) {
  if (pages <= 1) return `<div class="table-footer"><span>${new Intl.NumberFormat("vi-VN").format(total || 0)} kết quả</span></div>`;
  const params = { ...(state.filters || {}) };
  const links = [];
  const first = Math.max(1, page - 2), last = Math.min(pages, first + 4);
  for (let value = first; value <= last; value++) {
    params.page = value;
    links.push(`<a class="page-btn ${value === page ? "active" : ""}" href="${contextPath}${route}${queryString(params)}" ${value === page ? 'aria-current="page"' : ""}>${value}</a>`);
  }
  params.page = Math.max(1, page - 1);
  const prev = `<a class="page-btn" href="${contextPath}${route}${queryString(params)}" aria-label="Trang trước" ${page <= 1 ? 'aria-disabled="true" tabindex="-1"' : ""}>‹</a>`;
  params.page = Math.min(pages, page + 1);
  const next = `<a class="page-btn" href="${contextPath}${route}${queryString(params)}" aria-label="Trang sau" ${page >= pages ? 'aria-disabled="true" tabindex="-1"' : ""}>›</a>`;
  return `<div class="table-footer"><span>Trang ${page}/${pages} · ${new Intl.NumberFormat("vi-VN").format(total || 0)} kết quả</span><div class="pagination">${prev}${links.join("")}${next}</div></div>`;
}

function pageTable(title, subtitle, columns, data, filters, state, route, contextPath, emptyTitle) {
  return panel(title, subtitle, `${table(columns, data?.items || [], emptyTitle, "Thử đổi từ khóa hoặc bộ lọc.")}${pagination(data?.page || 1, data?.pages || 1, data?.total || 0, state, contextPath, route)}`);
}

function detailButton(type, row, label = "Xem chi tiết") {
  return `<button class="icon-button" data-action="detail" data-type="${type}" data-id="${esc(rowId(row))}" title="${esc(label)}" aria-label="${esc(label)}">${icon("eye")}</button>`;
}

export function renderUsers(data, state, contextPath = "") {
  const rows = data?.items || [];
  const columns = [
    { label: "Tài khoản", render: row => `<span class="cell-primary">${esc(row.fullName || "Chưa có tên")}</span><small class="cell-muted">#${esc(rowId(row))} · ${esc(row.email || "")}</small>` },
    { label: "Điện thoại", render: row => esc(shown(row.phone)) },
    { label: "Vai trò", render: row => badgeFor(row.role) },
    { label: "Trạng thái", render: row => badgeFor(row.status) },
    { label: "Đơn hàng", key: "orderCount" },
    { label: "Ngày tạo", render: row => dateLabel(row.createdAt) },
    { label: "Thao tác", render: row => `<div class="row-actions">${detailButton("users", row)}<button class="icon-button" data-action="edit-user" data-id="${esc(rowId(row))}" title="Sửa hồ sơ" aria-label="Sửa hồ sơ">${icon("settings")}</button>
      ${row.status === "ACTIVE" && String(row.id) !== String(state.actor?.id) ? `<button class="icon-button danger" data-action="user-status" data-id="${esc(rowId(row))}" data-status="LOCKED" title="Khóa tài khoản" aria-label="Khóa tài khoản">${icon("close")}</button>` : ""}
      ${row.status === "LOCKED" ? `<button class="icon-button" data-action="user-status" data-id="${esc(rowId(row))}" data-status="ACTIVE" title="Kích hoạt lại" aria-label="Kích hoạt lại">${icon("check")}</button>` : ""}</div>` }
  ];
  return `${heading("TÀI KHOẢN", "Người dùng", "Tra cứu hồ sơ và quản lý trạng thái tài khoản đã lưu trong hệ thống.")}
    ${filterForm("/admin/users", contextPath, state, [
      { key: "role", label: "Vai trò", options: ["ADMIN", "CUSTOMER"] },
      { key: "status", label: "Trạng thái", options: ["ACTIVE", "LOCKED", "PENDING"] }
    ], "Tìm ID, họ tên, email hoặc số điện thoại…")}
    ${pageTable("Danh sách người dùng", "Không hiển thị dữ liệu địa chỉ hoặc nguồn đăng nhập vì schema chưa lưu các trường này.", columns, data, [], state, "/admin/users", contextPath, "Không tìm thấy người dùng")}`;
}

function sellerActions(row) {
  const id = esc(rowId(row));
  const verified = row.verificationStatus === "VERIFIED";
  const state = row.status;
  const controls = [];
  if (state === "ACTIVE") {
    controls.push(`<button class="button secondary small" data-action="seller-status" data-id="${id}" data-status="INACTIVE">Tạm ngưng</button>`);
    controls.push(`<button class="button danger small" data-action="seller-status" data-id="${id}" data-status="BLOCKED">Chặn</button>`);
  } else {
    if (verified) controls.push(`<button class="button secondary small" data-action="seller-status" data-id="${id}" data-status="ACTIVE">Kích hoạt</button>`);
    if (state !== "INACTIVE") controls.push(`<button class="button secondary small" data-action="seller-status" data-id="${id}" data-status="INACTIVE">Ngừng</button>`);
    if (state !== "BLOCKED") controls.push(`<button class="button danger small" data-action="seller-status" data-id="${id}" data-status="BLOCKED">Chặn</button>`);
  }
  if (row.verificationStatus === "PENDING") controls.push(`<button class="button small" data-action="seller-verify" data-id="${id}">Duyệt xác minh</button>`);
  return controls.join("");
}

export function renderSellers(data, state, contextPath = "") {
  const columns = [
    { label: "Xưởng in", render: row => `<span class="cell-primary">${esc(row.name || "Chưa đặt tên")}</span><small class="cell-muted">#${esc(rowId(row))} · ${esc([row.district, row.province].filter(Boolean).join(", ") || "Chưa có khu vực")}</small>` },
    { label: "Chủ xưởng", render: row => `<span>${esc(row.ownerName || "Chưa liên kết tài khoản")}</span><small class="cell-muted">${esc(row.ownerEmail || "")}</small>` },
    { label: "Liên hệ", render: row => esc(row.contactPhone || row.ownerPhone || "—") },
    { label: "Xác minh", render: row => badgeFor(row.verificationStatus) },
    { label: "Trạng thái", render: row => badgeFor(row.status) },
    { label: "Sản phẩm", key: "productCount" },
    { label: "Đơn hàng", key: "orderCount" },
    { label: "", render: row => `<div class="row-actions">${detailButton("sellers", row)}${sellerActions(row)}</div>` }
  ];
  return `${heading("SELLER / XƯỞNG IN", "Xưởng in", "Danh sách đầy đủ gồm xưởng hoạt động, ngừng hoạt động và bị chặn.")}
    ${filterForm("/admin/sellers", contextPath, state, [
      { key: "status", label: "Trạng thái", options: ["ACTIVE", "INACTIVE", "BLOCKED"] },
      { key: "verification", label: "Xác minh", options: ["PENDING", "VERIFIED", "REJECTED"] }
    ], "Tìm tên xưởng, chủ xưởng hoặc khu vực…")}
    ${pageTable("Tất cả xưởng in", "Khu vực và thông tin liên hệ được lấy từ print_shops và users.", columns, data, [], state, "/admin/sellers", contextPath, "Không tìm thấy xưởng in")}`;
}

function moderationLabel(row) {
  if (row.moderationStatus === "PENDING_REVIEW") return "Chờ duyệt";
  if (row.moderationStatus === "REJECTED") return "Đã từ chối";
  return "Đã duyệt";
}

function productPublication(row) {
  if (row.moderationStatus !== "APPROVED") return "Chưa xuất bản";
  return row.active ? "Đang hoạt động" : "Đã ẩn";
}

function productActions(row) {
  const id = esc(rowId(row));
  if (row.moderationStatus === "PENDING_REVIEW") return `<button class="button small" data-action="product-moderate" data-id="${id}" data-status="approve">Duyệt</button><button class="button danger small" data-action="product-reject" data-id="${id}">Từ chối</button>`;
  if (row.moderationStatus === "APPROVED" && row.active) return `<button class="button secondary small" data-action="product-moderate" data-id="${id}" data-status="hide">Ẩn</button>`;
  if (row.moderationStatus === "APPROVED" && !row.active) return `<button class="button small" data-action="product-moderate" data-id="${id}" data-status="unhide">Hiện lại</button>`;
  return "";
}

export function renderProducts(data, state, contextPath = "") {
  const categories = data?.options?.set0 || [];
  const shops = data?.options?.set1 || [];
  const columns = [
    { label: "Sản phẩm", render: row => `<span class="cell-primary">${esc(row.name || "Chưa đặt tên")}</span><small class="cell-muted">#${esc(rowId(row))} · ${esc(row.brand || "")}</small>` },
    { label: "Danh mục", render: row => esc(row.category || "Chưa phân loại") },
    { label: "Xưởng", render: row => esc(row.shops || "Chưa có chào bán") },
    { label: "Giá thấp nhất", render: row => row.minPrice == null ? "—" : money(row.minPrice) },
    { label: "Kiểm duyệt", render: row => badge(moderationLabel(row), statusTone(row.moderationStatus)) },
    { label: "Hoạt động", render: row => badge(productPublication(row), row.active ? "success" : "neutral") },
    { label: "Thao tác", render: row => `<div class="row-actions">${detailButton("products", row)}${productActions(row)}</div>` }
  ];
  return `${heading("CATALOG CONTROL", "Sản phẩm", "Trạng thái kiểm duyệt tách biệt với trạng thái hoạt động. Duyệt sản phẩm không tự bật sản phẩm đã ẩn.")}
    ${filterForm("/admin/products", contextPath, state, [
      { key: "status", label: "Kiểm duyệt / hiển thị", options: [
        { value: "PENDING_REVIEW", label: "Chờ duyệt" }, { value: "APPROVED", label: "Đã duyệt" },
        { value: "REJECTED", label: "Đã từ chối" }, { value: "HIDDEN", label: "Đã ẩn" }
      ] },
      { key: "category", label: "Danh mục", options: categories.map(item => ({ value: item.id, label: item.name })) },
      { key: "shop", label: "Xưởng", options: shops.map(item => ({ value: item.id, label: item.name })) }
    ], "Tìm tên, mã hoặc mô tả sản phẩm…")}
    ${pageTable("Sản phẩm và dịch vụ in", "Giá lấy từ shop_products; Admin chỉ duyệt và quản lý hiển thị, không sửa nội dung seller.", columns, data, [], state, "/admin/products", contextPath, "Không tìm thấy sản phẩm")}`;
}

export function renderOrders(data, state, contextPath = "") {
  const view = state.filters?.view === "quotes" ? "quotes" : "orders";
  const route = "/admin/orders";
  const tabBar = `<div class="section-tabs"><a class="${view === "orders" ? "active" : ""}" href="${contextPath}${route}">Đơn hàng</a><a class="${view === "quotes" ? "active" : ""}" href="${contextPath}${route}?view=quotes">Yêu cầu báo giá</a></div>`;
  if (view === "quotes") return `${heading("ORDER OPERATIONS", "Yêu cầu báo giá", "Theo dõi yêu cầu in theo số lượng; báo giá do seller phụ trách.")}${tabBar}${renderQuotes(data, state, contextPath, true)}`;
  const statuses = data?.options?.set0 || [];
  const payments = data?.options?.set1 || [];
  const columns = [
    { label: "Mã đơn", render: row => `<span class="cell-primary">${esc(row.code || `#${row.id}`)}</span><small class="cell-muted">${dateTimeLabel(row.placedAt)}</small>` },
    { label: "Khách hàng", render: row => `<span>${esc(row.customerName || "Chưa có tên")}</span><small class="cell-muted">${esc(row.customerEmail || "")}</small>` },
    { label: "Số dòng hàng", key: "itemCount" },
    { label: "Tổng đơn", render: row => money(row.totalAmount) },
    { label: "Thanh toán", render: row => badgeFor(row.paymentStatus) },
    { label: "Trạng thái đơn", render: row => badgeFor(row.status) },
    { label: "", render: row => detailButton("orders", row, "Xem chi tiết và lịch sử") }
  ];
  return `${heading("ORDER OPERATIONS", "Đơn hàng", "Theo dõi toàn bộ đơn; thay đổi trạng thái theo quy tắc nghiệp vụ và lưu lịch sử.")}${tabBar}
    ${filterForm(route, contextPath, state, [
      { key: "status", label: "Trạng thái đơn", options: statuses.map(item => ({ value: item.value, label: item.value })) },
      { key: "payment", label: "Thanh toán", options: payments.map(item => ({ value: item.value, label: item.value })) }
    ], "Tìm mã đơn, tên, email hoặc số điện thoại khách…")}
    ${pageTable("Tất cả đơn hàng", "Trạng thái thanh toán chỉ đọc từ dữ liệu hiện có; cập nhật trạng thái đơn không thay đổi thanh toán.", columns, data, [], state, route, contextPath, "Không tìm thấy đơn hàng")}`;
}

export function renderQuotes(data, state, contextPath = "", embedded = false) {
  const statuses = data?.options?.set0 || [];
  const columns = [
    { label: "Mã yêu cầu", render: row => `<span class="cell-primary">#${esc(rowId(row))}</span><small class="cell-muted">${esc(row.companyName || "")}</small>` },
    { label: "Khách hàng", render: row => `<span>${esc(row.customerName || "Chưa có tên")}</span><small class="cell-muted">${esc(row.customerEmail || "")}</small>` },
    { label: "Sản phẩm / dịch vụ", render: row => esc(row.productName || "Không liên kết sản phẩm") },
    { label: "Xưởng ưu tiên", render: row => esc(row.shopName || "Chưa chỉ định") },
    { label: "Số lượng", key: "quantity" },
    { label: "Cần trước", render: row => dateLabel(row.requiredBy) },
    { label: "Báo giá đã lưu", render: row => row.quotedAmount == null ? "Chưa có" : money(row.quotedAmount) },
    { label: "Trạng thái", render: row => badgeFor(row.status) }
  ];
  return `${!embedded ? heading("ORDER OPERATIONS", "Yêu cầu báo giá", "Yêu cầu báo giá thật từ quote_requests. Admin theo dõi, không tự định giá thay seller.") : ""}
    ${filterForm("/admin/orders", contextPath, { filters: { ...(state.filters || {}), view: "quotes" } }, [
      { key: "status", label: "Trạng thái", options: statuses.map(item => ({ value: item.value, label: item.value })) }
    ], "Tìm mã yêu cầu, khách hàng, công ty hoặc sản phẩm…")}
    ${pageTable("Yêu cầu báo giá", "Chỉ hiển thị thông tin đã được lưu trong quote_requests.", columns, data, [], state, "/admin/orders", contextPath, "Không tìm thấy yêu cầu báo giá")}`;
}

function detailFields(fields) {
  return `<div class="detail-grid">${fields.map(([label, value, wide]) => `<div class="detail-field ${wide ? "wide" : ""}"><small>${esc(label)}</small><strong>${value}</strong></div>`).join("")}</div>`;
}

function miniRows(items, fields, emptyText) {
  if (!items?.length) return `<p class="muted-note">${esc(emptyText)}</p>`;
  return `<div class="mini-list">${items.map(item => `<div class="mini-list-row"><span><strong>${esc(fields.title(item))}</strong><small>${esc(fields.subtitle(item) || "")}</small></span>${fields.value ? `<span>${fields.value(item)}</span>` : ""}</div>`).join("")}</div>`;
}

export function renderEntityDetail(type, item, contextPath = "") {
  if (type === "users") {
    const orderRows = miniRows(item.recentOrders, {
      title: row => row.code || `#${row.id}`, subtitle: row => `${row.status || ""} · ${dateTimeLabel(row.placedAt)}`,
      value: row => money(row.totalAmount)
    }, "Tài khoản chưa có đơn hàng.");
    return `<div class="detail-intro">${badgeFor(item.status)}<span><strong>${esc(item.fullName || "Chưa có tên")}</strong><small>#${esc(item.id)} · ${esc(item.role)}</small></span></div>
      ${detailFields([["Email", esc(item.email)], ["Điện thoại", esc(shown(item.phone))], ["Ngày tạo", dateTimeLabel(item.createdAt)]])}
      <h3 class="mini-heading">Cập nhật hồ sơ</h3><form class="stack-form" data-form="user-profile" data-id="${esc(item.id)}">
        <label class="form-field">Họ tên<input name="fullName" maxlength="120" required value="${esc(item.fullName || "")}"></label>
        <label class="form-field">Số điện thoại<input name="phone" maxlength="25" value="${esc(item.phone || "")}"></label>
        <div class="modal-actions"><button class="button" type="submit">Lưu hồ sơ</button></div></form>
      <h3 class="mini-heading">Đơn hàng gần đây</h3>${orderRows}`;
  }
  if (type === "sellers") {
    const actions = sellerActions(item);
    const products = miniRows(item.products, {
      title: row => row.name, subtitle: row => `${row.material || ""} ${row.printingMethod ? `· ${row.printingMethod}` : ""}`,
      value: row => `${row.price == null ? "—" : money(row.price)} · ${badgeFor(row.moderationStatus)}${row.active ? badge("Đang bán", "success") : badge("Đã ẩn", "neutral")}`
    }, "Xưởng chưa có sản phẩm.");
    return `<div class="detail-intro"><span><strong>${esc(item.name || "Chưa đặt tên")}</strong><small>#${esc(item.id)} · ${esc([item.district, item.province].filter(Boolean).join(", ") || "Chưa có khu vực")}</small></span>${badgeFor(item.status)}</div>
      ${detailFields([["Chủ xưởng", esc(item.ownerName || "Chưa liên kết tài khoản")], ["Email chủ xưởng", esc(shown(item.ownerEmail))],
        ["Điện thoại chủ xưởng", esc(shown(item.ownerPhone))], ["Liên hệ xưởng", esc(shown(item.contactPhone))],
        ["Xác minh", badgeFor(item.verificationStatus)], ["Ngày tạo", dateTimeLabel(item.createdAt)], ["Mô tả", esc(shown(item.description)), true], ["Năng lực khai báo", esc(shown(item.specialties)), true]])}
      <div class="row-actions detail-actions">${actions}</div><h3 class="mini-heading">Sản phẩm của xưởng</h3>${products}`;
  }
  if (type === "products") {
    const offers = (item.offers || []).map(offer => `<div class="mini-list-row"><span><strong>${esc(offer.shopName || "Xưởng")}</strong><small>${esc(offer.ownerName || "Chưa có chủ xưởng")} · ${esc(offer.material || "")} · ${esc(offer.printingMethod || "")}</small></span>
      <span>${offer.price == null ? "—" : money(offer.price)}<small class="cell-muted">Số lượng tối thiểu: ${esc(offer.minimumQuantity ?? "—")} · ${offer.active ? "Chào bán" : "Tắt"}</small></span></div>`).join("");
    const actions = productActions(item);
    return `<div class="detail-intro"><span><strong>${esc(item.name || "Chưa đặt tên")}</strong><small>#${esc(item.id)} · ${esc(item.category || "Chưa phân loại")}</small></span>${badge(moderationLabel(item), statusTone(item.moderationStatus))}${badge(productPublication(item), item.active ? "success" : "neutral")}</div>
      ${detailFields([["Thương hiệu", esc(shown(item.brand))], ["Đơn vị", esc(shown(item.baseUnit))], ["Có thể tùy chỉnh", item.customizable ? "Có" : "Không"], ["Ngày tạo", dateTimeLabel(item.createdAt)], ["Mô tả", esc(shown(item.description)), true]])}
      ${item.rejectionReason ? `<h3 class="mini-heading">Lý do từ chối</h3><p class="muted-note">${esc(item.rejectionReason)}</p>` : ""}
      ${item.reviewedAt ? `<p class="muted-note">Được xử lý lúc ${esc(dateTimeLabel(item.reviewedAt))}${item.reviewerName ? ` bởi ${esc(item.reviewerName)}` : ""}.</p>` : ""}
      <h3 class="mini-heading">Chào bán của xưởng</h3>${offers ? `<div class="mini-list">${offers}</div>` : `<p class="muted-note">Chưa có chào bán.</p>`}
      ${actions ? `<div class="row-actions detail-actions">${actions}</div>` : ""}`;
  }
  if (type === "orders") {
    const items = miniRows(item.items, { title: row => row.productName || "Sản phẩm", subtitle: row => `${row.shopName || "Chưa có xưởng"} · ${row.quantity} × ${money(row.unitPrice)}`, value: row => money(row.lineTotal) }, "Đơn chưa có dòng sản phẩm.");
    const payments = miniRows(item.payments, { title: row => row.method || "Phương thức chưa rõ", subtitle: row => `${row.provider || ""} · ${row.status || ""} · ${dateTimeLabel(row.createdAt)}`, value: row => money(row.amount) }, "Chưa có bản ghi thanh toán.");
    const shipments = miniRows(item.shipments, { title: row => row.shopName || "Vận chuyển", subtitle: row => `${row.carrier || "Chưa có hãng vận chuyển"} · ${row.trackingNumber || "Chưa có mã theo dõi"}`, value: row => badgeFor(row.status) }, "Chưa có thông tin vận chuyển.");
    const updates = miniRows(item.productionUpdates, { title: row => `${row.stage || "Cập nhật sản xuất"} · ${row.progressPercent ?? 0}%`, subtitle: row => `${row.shopName || "Xưởng"} · ${dateTimeLabel(row.createdAt)} · ${row.message || ""}` }, "Chưa có cập nhật sản xuất.");
    const history = miniRows(item.history, { title: row => `${row.oldStatus || "Khởi tạo"} → ${row.newStatus}`, subtitle: row => `${row.changedBy || "Hệ thống"} · ${dateTimeLabel(row.changedAt)}${row.note ? ` · ${row.note}` : ""}` }, "Chưa ghi nhận lịch sử.");
    const options = (item.allowedTransitions || []).map(status => `<option value="${esc(status)}">${esc(status)}</option>`).join("");
    const statusForm = options ? `<h3 class="mini-heading">Cập nhật trạng thái đơn</h3><form class="stack-form" data-form="order-status" data-id="${esc(item.id)}">
      <label class="form-field">Trạng thái mới<select name="status" required>${options}</select></label>
      <label class="form-field">Ghi chú lịch sử<textarea name="note" maxlength="500" placeholder="Ghi chú cho lịch sử thay đổi (không bắt buộc)"></textarea></label>
      <div class="modal-actions"><button class="button" type="submit">Cập nhật trạng thái</button></div></form>` : `<p class="muted-note">Đơn đang ở trạng thái kết thúc; không có chuyển trạng thái hợp lệ.</p>`;
    return `<div class="detail-intro"><span><strong>${esc(item.code || `#${item.id}`)}</strong><small>${dateTimeLabel(item.placedAt)}</small></span>${badgeFor(item.status)}${badgeFor(item.paymentStatus)}</div>
      ${detailFields([["Khách hàng", esc(item.customerName || "Chưa có tên")], ["Email", esc(shown(item.customerEmail))], ["Điện thoại", esc(shown(item.customerPhone))],
        ["Người nhận", esc(shown(item.recipientName))], ["Điện thoại nhận", esc(shown(item.recipientPhone))], ["Địa chỉ giao", esc(shown(item.shippingAddress)), true],
        ["Tạm tính", money(item.subtotal)], ["Vận chuyển", money(item.shippingFee)], ["Tổng đơn", money(item.totalAmount)], ["Trạng thái giao", esc(shown(item.shippingStatus))]])}
      <h3 class="mini-heading">Sản phẩm trong đơn</h3>${items}<h3 class="mini-heading">Thanh toán</h3>${payments}
      <h3 class="mini-heading">Vận chuyển</h3>${shipments}<h3 class="mini-heading">Tiến độ sản xuất</h3>${updates}
      <h3 class="mini-heading">Lịch sử trạng thái</h3>${history}${statusForm}`;
  }
  return `<div class="empty-state"><strong>Không tìm thấy dữ liệu</strong></div>`;
}
