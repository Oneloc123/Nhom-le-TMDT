import { badge, dateTimeLabel, esc, icon, money, panel } from "../components.js";

const statusLabel = status => ({
  SUBMITTED: "Đã gửi yêu cầu",
  UNDER_REVIEW: "Đang xem xét",
  AWAITING_RETURN_SHIPMENT: "Chờ gửi hàng trả",
  RETURN_SHIPPED: "Khách đã gửi hàng trả",
  RETURN_RECEIVED: "Đã nhận hàng trả",
  REFUND_PROCESSING: "Đang xử lý hoàn tiền",
  REFUNDED: "Đã ghi nhận hoàn tiền",
  REFUND_FAILED: "Hoàn tiền chưa thành công",
  REJECTED: "Từ chối yêu cầu",
  WITHDRAWN: "Đã rút yêu cầu"
})[status] || status || "Không xác định";

const statusTone = status => {
  if (["REFUNDED"].includes(status)) return "success";
  if (["SUBMITTED", "UNDER_REVIEW", "AWAITING_RETURN_SHIPMENT", "RETURN_SHIPPED", "RETURN_RECEIVED", "REFUND_PROCESSING"].includes(status)) return "warning";
  if (["REJECTED", "WITHDRAWN", "REFUND_FAILED"].includes(status)) return "danger";
  return "neutral";
};

const statusBadge = status => badge(statusLabel(status), statusTone(status));
const shown = value => value == null || value === "" ? "—" : String(value);
const recordId = row => String(row.requestId ?? row.id ?? "");

function actionButton(id, action, label, danger = false) {
  return `<button class="button small ${danger ? "danger" : "secondary"}" data-action="return-refund-action" data-id="${esc(id)}" data-next-action="${esc(action)}">${esc(label)}</button>`;
}

function rowActions(row) {
  const id = recordId(row);
  const common = `<button class="button small secondary" data-action="return-refund-detail" data-id="${esc(id)}">${icon("eye")} Chi tiết</button>`;
  let actions = "";
  switch (row.status) {
    case "SUBMITTED":
      actions = actionButton(id, "review", "Bắt đầu xem xét") + actionButton(id, "reject", "Từ chối", true);
      break;
    case "UNDER_REVIEW":
      actions = actionButton(id, "approve", "Duyệt") + actionButton(id, "reject", "Từ chối", true);
      break;
    case "AWAITING_RETURN_SHIPMENT":
    case "RETURN_SHIPPED":
      actions = actionButton(id, "mark_received", "Xác nhận đã nhận hàng trả");
      break;
    case "RETURN_RECEIVED":
      actions = actionButton(id, "start_refund", "Bắt đầu hoàn tiền");
      break;
    case "REFUND_PROCESSING":
      actions = actionButton(id, "refund_complete", "Ghi nhận hoàn tiền") + actionButton(id, "refund_failed", "Ghi nhận thất bại", true);
      break;
    case "REFUND_FAILED":
      actions = actionButton(id, "retry_refund", "Thử xử lý lại");
      break;
    default:
      break;
  }
  return `<div class="row-actions">${common}${actions}</div>`;
}

function filterBar(state, contextPath) {
  const filters = state.filters || {};
  const statuses = [
    ["ALL", "Tất cả trạng thái"], ["SUBMITTED", "Đã gửi yêu cầu"],
    ["UNDER_REVIEW", "Đang xem xét"], ["AWAITING_RETURN_SHIPMENT", "Chờ gửi hàng trả"],
    ["RETURN_SHIPPED", "Đã gửi hàng trả"], ["RETURN_RECEIVED", "Đã nhận hàng trả"],
    ["REFUND_PROCESSING", "Đang xử lý hoàn tiền"], ["REFUND_FAILED", "Hoàn tiền thất bại"],
    ["REFUNDED", "Đã hoàn tiền"], ["REJECTED", "Đã từ chối"], ["WITHDRAWN", "Đã rút yêu cầu"]
  ];
  return `<form class="table-toolbar" method="get" action="${esc(contextPath)}/admin/return-refunds">
    <label class="search-box">${icon("search")}<input type="search" name="q" value="${esc(filters.q || "")}" placeholder="Tìm mã yêu cầu, mã đơn, khách hàng hoặc sản phẩm…"></label>
    <label class="select-box"><span>Trạng thái</span><select name="status">${statuses.map(([value,label]) => `<option value="${value}" ${String(filters.status || "ALL") === value ? "selected" : ""}>${label}</option>`).join("")}</select></label>
    <input type="hidden" name="page" value="1">
    <button class="button secondary" type="submit">Lọc</button><a class="button secondary" href="${esc(contextPath)}/admin/return-refunds">Xóa lọc</a>
  </form>`;
}


function returnPagination(rows, state, contextPath, pageSize = 10) {
  const filters = state.filters || {};
  const total = rows.length;
  const pages = Math.max(1, Math.ceil(total / pageSize));
  const page = Math.min(pages, Math.max(1, Number.parseInt(filters.page, 10) || 1));
  const link = (value, label, disabled = false) => {
    const params = new URLSearchParams();
    if (filters.q) params.set("q", filters.q);
    if (filters.status && filters.status !== "ALL") params.set("status", filters.status);
    params.set("page", String(value));
    return '<a class="page-btn ' + (value === page ? "active" : "") + '" href="' +
      esc(contextPath + "/admin/return-refunds?" + params.toString()) + '"' +
      (disabled ? ' aria-disabled="true" tabindex="-1"' : "") +
      (value === page ? ' aria-current="page"' : "") + '>' + esc(label) + '</a>';
  };
  const start = Math.max(1, Math.min(page - 2, pages - 4));
  const links = [];
  for (let value = start; value <= Math.min(pages, start + 4); value++) links.push(link(value, String(value)));
  const from = total ? (page - 1) * pageSize + 1 : 0;
  const to = Math.min(page * pageSize, total);
  return '<div class="table-footer"><span>Hiển thị <strong>' + from + '–' + to +
    '</strong> trong <strong>' + total + '</strong> hồ sơ theo bộ lọc</span>' +
    '<nav class="pagination" aria-label="Phân trang yêu cầu đổi trả">' +
    link(Math.max(1, page - 1), "‹", page <= 1) + links.join("") +
    link(Math.min(pages, page + 1), "›", page >= pages) + '</nav></div>';
}

export function renderReturnRefunds(data, state, contextPath = "") {
  const rows = Array.isArray(data) ? data : [];
  const pageSize = 10;
  const pages = Math.max(1, Math.ceil(rows.length / pageSize));
  const page = Math.min(pages, Math.max(1, Number.parseInt(state.filters?.page, 10) || 1));
  const visibleRows = rows.slice((page - 1) * pageSize, page * pageSize);
  const content = visibleRows.length ? `<div class="table-wrap"><table class="data-table">
    <thead><tr><th>Yêu cầu / đơn hàng</th><th>Khách hàng</th><th>Sản phẩm</th><th>Số lượng</th><th>Số tiền yêu cầu</th><th>Trạng thái</th><th>Ngày gửi</th><th>Thao tác</th></tr></thead>
    <tbody>${visibleRows.map(row => `<tr>
      <td><strong>${esc(row.requestCode || "#" + recordId(row))}</strong><small class="cell-muted">${esc(row.orderCode || "—")}</small></td>
      <td><strong>${esc(row.customerName || "Chưa có tên")}</strong><small class="cell-muted">${esc(row.customerEmail || "")}</small></td>
      <td>${esc(row.productNameSnapshot || "Sản phẩm")}</td>
      <td>${esc(row.requestedQuantity ?? "—")}</td>
      <td>${money(row.requestedAmount)}${row.approvedAmount == null ? "" : `<small class="cell-muted">Duyệt: ${money(row.approvedAmount)}</small>`}</td>
      <td>${statusBadge(row.status)}</td>
      <td>${esc(dateTimeLabel(row.submittedAt))}</td>
      <td>${rowActions(row)}</td>
    </tr>`).join("")}</tbody></table></div>` : `<div class="empty-state"><span class="empty-illustration">${icon("box")}</span><strong>Chưa có yêu cầu đổi trả / hoàn tiền</strong><p>Yêu cầu mới của khách sẽ xuất hiện tại đây.</p></div>`;
  return `<div class="page-heading"><div><div class="eyebrow">AFTER-SALES OPERATIONS</div><h1>Đổi trả / Hoàn tiền</h1><p>Tiếp nhận hồ sơ theo đơn và từng dòng sản phẩm, duyệt điều kiện, ghi nhận hàng trả và theo dõi kết quả hoàn tiền.</p></div></div>
    <div class="notice-card" style="margin-bottom:16px"><strong>Lưu ý đối soát:</strong> thao tác “Ghi nhận hoàn tiền” chỉ ghi lại kết quả, phương thức và mã tham chiếu giao dịch. Nhân viên phải thực hiện chuyển tiền trên ngân hàng/cổng thanh toán bên ngoài trước khi xác nhận thành công.</div>
    ${filterBar(state, contextPath)}
    ${panel("Danh sách yêu cầu", `${rows.length} yêu cầu được trả về bộ lọc hiện tại`, content + returnPagination(rows, state, contextPath, pageSize))}`;
}

function detailField(label, value) {
  return `<div class="detail-field"><small>${esc(label)}</small><strong>${esc(shown(value))}</strong></div>`;
}

function timeline(events) {
  if (!events?.length) return `<p class="muted-note">Chưa có lịch sử trạng thái.</p>`;
  return `<div class="mini-list">${events.map(event => `<div class="mini-list-row"><span><strong>${esc(statusLabel(event.toStatus))}</strong><small>${esc(dateTimeLabel(event.createdAt))} · ${esc(event.actorName || "Hệ thống")}<br>${esc(event.note || "")}</small></span></div>`).join("")}</div>`;
}

function evidence(items, contextPath) {
  if (!items?.length) return `<p class="muted-note">Khách chưa đính kèm ảnh bằng chứng.</p>`;
  return `<div style="display:flex;gap:12px;flex-wrap:wrap">${items.map(item => {
    const href = `${contextPath}/admin/api/return-refund-evidence/${encodeURIComponent(item.evidenceId)}`;
    return `<a href="${esc(href)}" target="_blank" rel="noopener" style="width:120px;display:grid;gap:5px;color:inherit;text-decoration:none"><img src="${esc(href)}" alt="Ảnh bằng chứng" loading="lazy" style="width:112px;height:88px;object-fit:cover;border:1px solid #dbe3f0;border-radius:8px"><small>${esc(item.originalFilename || "Ảnh bằng chứng")} · ${esc(item.fileSize || "—")} bytes</small></a>`;
  }).join("")}</div>`;
}

function refundTransactions(items) {
  if (!items?.length) return `<p class="muted-note">Chưa ghi nhận lần thử hoàn tiền nào.</p>`;
  return `<div class="mini-list">${items.map(item => `<div class="mini-list-row"><span><strong>${esc(item.status)} · ${money(item.amount)}</strong><small>${esc(item.method)} · ${esc(item.reference || "Không có mã tham chiếu")}<br>${esc(dateTimeLabel(item.recordedAt))} · ${esc(item.recordedByName || "")}<br>${esc(item.note || "")}</small></span></div>`).join("")}</div>`;
}

export function renderReturnRefundDetail(item, contextPath = "") {
  if (!item) return `<p class="muted-note">Không tìm thấy dữ liệu.</p>`;
  const actions = rowActions(item).replace(/data-action="return-refund-detail"/, 'data-action="return-refund-detail"');
  return `<div class="detail-intro"><span><strong>${esc(item.requestCode || "#" + recordId(item))}</strong><small>${esc(item.orderCode || "—")} · ${esc(item.productNameSnapshot || "Sản phẩm")}</small></span>${statusBadge(item.status)}</div>
    <div class="detail-grid">
      ${detailField("Khách hàng", item.customerName)}${detailField("Email", item.customerEmail)}
      ${detailField("Loại yêu cầu", item.requestType)}${detailField("Lý do", item.reasonCode)}
      ${detailField("Số lượng mua", item.purchasedQuantity)}${detailField("Số lượng yêu cầu", item.requestedQuantity)}
      ${detailField("Số tiền yêu cầu", money(item.requestedAmount))}${detailField("Số tiền duyệt", item.approvedAmount == null ? "Chưa duyệt" : money(item.approvedAmount))}
      ${detailField("Gửi lúc", item.submittedAt)}${detailField("Hạn yêu cầu", item.returnDeadlineAt)}
      ${detailField("Đơn vị trả hàng", item.returnCarrier)}${detailField("Mã vận đơn trả", item.returnTrackingNumber)}
      ${detailField("Phương thức hoàn tiền", item.resolutionMethod)}${detailField("Mã tham chiếu hoàn tiền", item.refundReference)}
      ${detailField("Quản trị viên xử lý", item.decidedByUserId)}${detailField("Cập nhật cuối", item.updatedAt)}
    </div>
    <h3 class="mini-heading">Mô tả của khách</h3><p>${esc(item.customerNote || "—")}</p>
    <h3 class="mini-heading">Ghi chú quyết định</h3><p>${esc(item.decisionNote || "—")}</p>
    <h3 class="mini-heading">Bằng chứng</h3>${evidence(item.evidence, contextPath)}
    <h3 class="mini-heading">Lịch sử xử lý</h3>${timeline(item.events)}
    <h3 class="mini-heading">Các lần ghi nhận hoàn tiền</h3>${refundTransactions(item.refundTransactions)}
    <div class="row-actions detail-actions">${actions}</div>`;
}
