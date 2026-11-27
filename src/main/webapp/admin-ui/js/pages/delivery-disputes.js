import { badge, dateTimeLabel, esc, icon, panel } from "../components.js";

const labels = {
  OPEN: "Mới tiếp nhận",
  UNDER_REVIEW: "Đang xem xét",
  RESOLVED: "Đã giải quyết",
  REJECTED: "Từ chối",
  NOT_RECEIVED: "Khách chưa nhận",
  DAMAGED: "Hàng hư hỏng",
  MISSING: "Thiếu sản phẩm",
  WRONG_ITEM: "Giao sai sản phẩm",
  OTHER: "Vấn đề khác"
};
const tone = status => ({
  OPEN: "warning", UNDER_REVIEW: "warning", RESOLVED: "success", REJECTED: "danger"
})[String(status || "").toUpperCase()] || "neutral";
const statusBadge = status => badge(labels[String(status || "").toUpperCase()] || String(status || "Không rõ"), tone(status));
const value = item => item === null || item === undefined || item === "" ? "—" : String(item);

export function renderDeliveryDisputes(data, filters = {}, contextPath = "") {
  const rows = Array.isArray(data) ? data : [];
  const statuses = [
    ["OPEN", "Mới tiếp nhận"], ["UNDER_REVIEW", "Đang xem xét"],
    ["RESOLVED", "Đã giải quyết"], ["REJECTED", "Từ chối"], ["ALL", "Tất cả"]
  ];
  const current = filters.status || "OPEN";
  const options = statuses.map(([key, label]) =>
    '<option value="' + key + '"' + (current === key ? " selected" : "") + '>' + label + '</option>').join("");
  const toolbar = '<form class="table-toolbar" method="get" action="' + esc(contextPath) + '/admin/delivery-disputes">' +
    '<label class="search-box">' + icon("search") + '<input type="search" name="q" maxlength="160" value="' +
    esc(filters.q || "") + '" placeholder="Mã khiếu nại, đơn hàng, khách hàng, mã vận đơn"></label>' +
    '<label class="select-box"><span>Trạng thái</span><select name="status">' + options + '</select></label>' +
    '<button class="button secondary" type="submit">Lọc</button>' +
    '<a class="button secondary" href="' + esc(contextPath) + '/admin/delivery-disputes">Xóa lọc</a></form>';
  const table = rows.length ? '<div class="table-wrap"><table class="data-table"><thead><tr>' +
    '<th>Khiếu nại / Đơn hàng</th><th>Khách hàng</th><th>Vận đơn / Xưởng</th><th>Vấn đề</th><th>Trạng thái</th><th>Ngày gửi</th><th>Thao tác</th>' +
    '</tr></thead><tbody>' + rows.map(row => {
      const disputeId = esc(row.id);
      const type = labels[String(row.disputeType || "").toUpperCase()] || value(row.disputeType);
      const actions = '<div class="row-actions">' +
        '<button class="button small secondary" data-action="delivery-dispute-detail" data-id="' + disputeId + '">' + icon("eye") + ' Chi tiết</button>' +
        (row.status === "OPEN" ? '<button class="button small" data-action="delivery-dispute-action" data-id="' + disputeId + '" data-next-action="REVIEW">Bắt đầu xử lý</button>' : '') +
        (["OPEN", "UNDER_REVIEW"].includes(row.status) ?
          '<button class="button small secondary" data-action="delivery-dispute-action" data-id="' + disputeId + '" data-next-action="RESOLVE">Giải quyết</button>' +
          '<button class="button small danger" data-action="delivery-dispute-action" data-id="' + disputeId + '" data-next-action="REJECT">Từ chối</button>' : '') +
        '</div>';
      return '<tr><td><strong class="cell-primary">' + esc(value(row.disputeCode)) + '</strong><small class="cell-muted">' +
        esc(value(row.orderCode)) + ' · Kiện #' + disputeId + '</small></td><td><strong>' + esc(value(row.customerName)) +
        '</strong><small class="cell-muted">' + esc(value(row.customerEmail)) + '</small></td><td><strong>' +
        esc(value(row.shopName)) + '</strong><small class="cell-muted">' + esc(value(row.carrier)) + ' · ' +
        esc(value(row.trackingNumber)) + '</small></td><td>' + esc(type) + '</td><td>' + statusBadge(row.status) +
        '</td><td>' + esc(dateTimeLabel(row.createdAt)) + '</td><td>' + actions + '</td></tr>';
    }).join("") + '</tbody></table></div>' :
    '<div class="empty-state"><span class="empty-illustration">' + icon("check") + '</span><strong>Không có khiếu nại giao hàng</strong>' +
    '<p>Khi khách báo chưa nhận hàng hoặc sản phẩm có vấn đề, hồ sơ sẽ xuất hiện tại đây.</p></div>';
  return '<div class="page-heading"><div><div class="eyebrow">DELIVERY DISPUTE OPERATIONS</div><h1>Khiếu nại giao hàng</h1>' +
    '<p>Tiếp nhận phản ánh chưa nhận được hàng, hàng hư hỏng, thiếu hoặc giao sai; mọi quyết định đều được lưu lịch sử.</p></div></div>' +
    '<div class="notice-card" style="margin-bottom:16px"><strong>Kiểm soát hoàn tất đơn:</strong> khiếu nại ở trạng thái Mới tiếp nhận hoặc Đang xem xét sẽ khóa xác nhận nhận hàng và ngăn đơn chuyển sang Hoàn thành. ' +
    'Sau khi Admin giải quyết hoặc từ chối, khách có thể quay lại xác nhận kiện hàng.</div>' +
    toolbar + panel("Hàng đợi khiếu nại", rows.length + " hồ sơ trong kết quả hiện tại · tối đa 300", table);
}

function field(label, valueText) {
  return '<div class="detail-field"><small>' + esc(label) + '</small><strong>' + esc(value(valueText)) + '</strong></div>';
}

export function renderDeliveryDisputeDetail(item) {
  if (!item) return '<p class="muted-note">Không tìm thấy khiếu nại.</p>';
  const events = Array.isArray(item.events) ? item.events : [];
  const history = events.length ? '<div class="mini-list">' + events.map(event =>
    '<div class="mini-list-row"><span><strong>' + esc(labels[event.toStatus] || event.action || event.toStatus || "Cập nhật") +
    '</strong><small>' + esc(dateTimeLabel(event.createdAt)) + ' · ' + esc(event.actorName || "Hệ thống") +
    '<br>' + esc(event.note || "—") + '</small></span></div>').join("") + '</div>' :
    '<p class="muted-note">Chưa có lịch sử xử lý.</p>';
  return '<div class="detail-intro"><span><strong>' + esc(value(item.disputeCode)) + '</strong><small>' +
    esc(value(item.orderCode)) + ' · Vận đơn #' + esc(value(item.shipmentId)) + '</small></span>' + statusBadge(item.status) + '</div>' +
    '<div class="detail-grid">' +
    field("Khách hàng", item.customerName) + field("Email", item.customerEmail) +
    field("Xưởng", item.shopName) + field("Loại khiếu nại", labels[item.disputeType] || item.disputeType) +
    field("Hãng vận chuyển", item.carrier) + field("Mã vận đơn", item.trackingNumber) +
    field("Trạng thái vận đơn", item.shipmentStatus) + field("Xác nhận nhận hàng", item.receiptStatus) +
    field("Thời điểm báo giao", item.deliveredAt) + field("Thời điểm khiếu nại", item.createdAt) +
    field("Admin xử lý", item.adminName) + field("Cập nhật cuối", item.updatedAt) + '</div>' +
    '<section class="detail-section"><h3>Mô tả của khách</h3><p>' + esc(value(item.description)) + '</p></section>' +
    '<section class="detail-section"><h3>Kết quả xử lý</h3><p>' + esc(value(item.resolutionNote)) + '</p></section>' +
    '<section class="detail-section"><h3>Lịch sử thao tác</h3>' + history + '</section>';
}
