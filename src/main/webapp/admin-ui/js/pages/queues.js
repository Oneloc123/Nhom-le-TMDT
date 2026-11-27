import { badge, dateTimeLabel, esc, icon, money, panel } from "../components.js";

const definitions = {
  shipments: {
    title: "Vận đơn", eyebrow: "FULFILMENT OPERATIONS",
    description: "Cập nhật hãng vận chuyển, mã theo dõi và trạng thái giao hàng cho từng xưởng/đơn; mọi thay đổi được lưu vào lịch sử và thông báo cho khách.",
    searchPlaceholder: "Mã đơn, mã vận đơn, hãng vận chuyển, xưởng hoặc khách hàng",
    statuses: [["OPEN","Chưa giao xong"],["PREPARING","Chuẩn bị"],["SHIPPED","Đã gửi"],["IN_TRANSIT","Đang vận chuyển"],["DELIVERED","Đã giao"],["FAILED","Giao thất bại"],["RETURNED","Đã hoàn/trả"]]
  },
  "custom-prints": {
    title: "Yêu cầu in 3D", eyebrow: "CUSTOM PRINT OPERATIONS",
    description: "Theo dõi yêu cầu in theo tệp thiết kế, thông số kỹ thuật, xưởng và tiến độ báo giá.",
    searchPlaceholder: "Mã yêu cầu, tệp, khách hàng hoặc email",
    statuses: [["ADMIN_PENDING","Admin chưa rà soát"],["ADMIN_ESCALATED","Admin cần theo dõi"],["PENDING_REVIEW","Chờ xưởng báo giá"],["READY_TO_ORDER","Sẵn sàng đặt trực tiếp"],["DRAFT","Bản nháp"],["QUOTED","Đã báo giá"],["ORDER_CREATED","Đã tạo đơn"],["CUSTOMER_DECLINED","Khách từ chối"],["EXPIRED","Hết hạn"],["CLOSED","Đã đóng"]]
  },
  "order-changes": {
    title: "Yêu cầu thay đổi đơn", eyebrow: "ORDER CHANGE OPERATIONS",
    description: "Rà soát yêu cầu đổi thông tin, thay đổi sản phẩm hoặc hủy đơn; mọi quyết định đều có ghi chú và lịch sử.",
    searchPlaceholder: "Mã yêu cầu, mã đơn, khách hàng hoặc nội dung",
    statuses: [["ADMIN_PENDING","Admin chưa rà soát"],["ADMIN_REVIEWED","Admin đã rà soát"],["PENDING","Chờ xưởng xử lý"],["ACCEPTED","Xưởng đã chấp nhận"],["REJECTED","Đã từ chối"],["APPLIED","Đã áp dụng"],["WITHDRAWN","Đã rút"],["EXPIRED","Hết hạn"]]
  },
  reviews: {
    title: "Kiểm duyệt đánh giá", eyebrow: "REVIEW MODERATION",
    description: "Xem đánh giá khách hàng và kiểm soát việc hiển thị công khai; thao tác ẩn cần có lý do.",
    searchPlaceholder: "Tên sản phẩm, khách hàng hoặc nội dung đánh giá",
    statuses: [["UNPUBLISHED","Đang ẩn"],["PUBLISHED","Đang công khai"]]
  },
  reconciliation: {
    title: "Đối soát thanh toán", eyebrow: "PAYMENT RECONCILIATION",
    description: "Rà soát giao dịch có kết quả chưa rõ ràng, sai lệch trạng thái hoặc yêu cầu hoàn tiền.",
    searchPlaceholder: "Mã giao dịch, mã đơn, khách hàng hoặc tham chiếu",
    statuses: [["OPEN","Cần đối soát"],["ALL","Tất cả giao dịch"],["REVIEW_REQUIRED","Cần kiểm tra"],["REFUND_REQUIRED","Cần hoàn tiền"],["PENDING","Đang chờ"],["FAILED","Thất bại"],["SUPERSEDED","Đã thay thế"]]
  }
};

const statusNames = {
  PENDING_REVIEW:"Chờ xử lý", READY_TO_ORDER:"Sẵn sàng đặt", DRAFT:"Bản nháp", QUOTED:"Đã báo giá",
  ORDER_CREATED:"Đã tạo đơn", CUSTOMER_DECLINED:"Khách từ chối", EXPIRED:"Hết hạn", CLOSED:"Đã đóng",
  PENDING:"Chờ xử lý", ACCEPTED:"Đã chấp nhận", REJECTED:"Đã từ chối", APPLIED:"Đã áp dụng",
  WITHDRAWN:"Đã rút", PUBLISHED:"Công khai", UNPUBLISHED:"Đang ẩn", REVIEW_REQUIRED:"Cần kiểm tra",
  REFUND_REQUIRED:"Cần hoàn tiền", FAILED:"Thất bại", SUPERSEDED:"Đã thay thế",
  ADMIN_PENDING:"Admin chưa rà soát", ADMIN_ESCALATED:"Admin cần theo dõi", ADMIN_REVIEWED:"Admin đã rà soát",
  PAID:"Đã thanh toán", SUCCESS:"Thành công", RECONCILED:"Đã đối soát", FOLLOW_UP:"Cần theo dõi", OPEN:"Cần đối soát",
  PREPARING:"Chuẩn bị", SHIPPED:"Đã gửi", IN_TRANSIT:"Đang vận chuyển", DELIVERED:"Đã giao", RETURNED:"Đã hoàn/trả"
};
const statusTone = status => {
  if (["PUBLISHED","QUOTED","ORDER_CREATED","APPLIED","RECONCILED","PAID","SUCCESS","ADMIN_REVIEWED"].includes(status)) return "success";
  if (["PENDING","PENDING_REVIEW","READY_TO_ORDER","ACCEPTED","REVIEW_REQUIRED","REFUND_REQUIRED","OPEN","FOLLOW_UP","ADMIN_PENDING","ADMIN_ESCALATED"].includes(status)) return "warning";
  if (["REJECTED","FAILED","EXPIRED","CLOSED","WITHDRAWN","CUSTOMER_DECLINED","UNPUBLISHED"].includes(status)) return "danger";
  return "neutral";
};
const stateBadge = value => {
  const key = String(value || "").toUpperCase();
  return badge(statusNames[key] || String(value || "Không rõ"), statusTone(key));
};
const shown = value => value === null || value === undefined || value === "" ? "—" : String(value);
const idOf = row => String(row && (row.id ?? row.code) || "");

function searchToolbar(route, filters, contextPath) {
  const config = definitions[route];
  const currentStatus = filters && filters.status || (route === "reconciliation" ? "OPEN" : "ALL");
  const options = ['<option value="ALL" ' + (currentStatus === "ALL" ? "selected" : "") + '>Tất cả trạng thái</option>'];
  config.statuses.forEach(([value,label]) => {
    options.push('<option value="' + esc(value) + '" ' + (currentStatus === value ? "selected" : "") + '>' + esc(label) + '</option>');
  });
  return '<form class="table-toolbar" method="get" action="' + esc(contextPath) + '/admin/' + route + '">' +
    '<label class="search-box">' + icon("search") + '<input type="search" name="q" maxlength="200" value="' + esc(filters && filters.q || "") + '" placeholder="' + esc(config.searchPlaceholder) + '"></label>' +
    '<label class="select-box"><span>Trạng thái</span><select name="status">' + options.join("") + '</select></label>' +
    '<input type="hidden" name="page" value="1">' +
    '<button class="button secondary" type="submit">Lọc</button><a class="button secondary" href="' + esc(contextPath) + '/admin/' + route + '">Xóa lọc</a></form>';
}

function actionButton(route, row, action, label, tone) {
  return '<button class="button small ' + tone + '" data-action="business-queue-action" data-queue="' + esc(route) + '" data-id="' + esc(idOf(row)) + '" data-next-action="' + esc(action) + '">' + esc(label) + '</button>';
}
function detailButton(route, row) {
  return '<button class="button small secondary" data-action="business-queue-detail" data-queue="' + esc(route) + '" data-id="' + esc(idOf(row)) + '">' + icon("eye") + ' Chi tiết</button>';
}
function rowActions(route, row) {
  let html = detailButton(route,row);
  if (route === "shipments") {
    html += actionButton(route,row,"UPDATE","Cập nhật vận đơn","secondary");
  } else if (route === "custom-prints") {
    if (String(row.status).toUpperCase() === "PENDING_REVIEW" && String(row.adminReviewAction || "").toUpperCase() !== "REVIEWED") {
      html += actionButton(route,row,"REVIEWED","Đã rà soát","secondary") + actionButton(route,row,"ESCALATED","Cần theo dõi","subtle");
    }
  } else if (route === "order-changes") {
    if (String(row.status).toUpperCase() === "PENDING" && String(row.adminReviewAction || "").toUpperCase() !== "REVIEWED") {
      html += actionButton(route,row,"REVIEWED","Đã rà soát","secondary");
      html += actionButton(route,row,"REJECTED","Từ chối","danger");
    }
  } else if (route === "reviews") {
    const published = String(row.isPublished) === "true" || String(row.isPublished) === "1";
    html += published ? actionButton(route,row,"UNPUBLISH","Ẩn đánh giá","danger") : actionButton(route,row,"PUBLISH","Duyệt hiển thị","secondary");
  } else if (route === "reconciliation") {
    html += actionButton(route,row,"RECONCILED","Đã đối soát","secondary") + actionButton(route,row,"FOLLOW_UP","Cần theo dõi","subtle");
    if (String(row.status).toUpperCase() === "REFUND_REQUIRED") {
      html += actionButton(route,row,"MANUAL_REFUND","Ghi nhận hoàn tiền","danger")
        + actionButton(route,row,"MANUAL_REFUND_FAILED","Ghi nhận thất bại","secondary");
    }
  }
  return '<div class="row-actions">' + html + '</div>';
}
function lineValue(value, className) {
  return '<small class="cell-muted">' + esc(shown(value)) + '</small>';
}

function shipmentRow(row) {
  return '<tr><td><strong class="cell-primary">' + esc(row.orderCode || ("Đơn #" + row.orderId)) + '</strong>' +
    lineValue("Vận đơn #" + idOf(row)) + '</td><td><strong>' + esc(row.customerName || "—") + '</strong>' +
    lineValue(row.customerEmail || "") + '</td><td><strong>' + esc(row.shopName || "—") + '</strong>' +
    lineValue(row.carrier || "Chưa có hãng vận chuyển") +
    lineValue(row.carrierProvider === "GHN"
      ? "Đồng bộ GHN · " + (row.carrierStatusCode || "Chờ trạng thái hãng")
      : "Cập nhật thủ công") + '</td><td><span style="white-space:normal;display:block;min-width:130px;max-width:220px">' +
    esc(row.trackingNumber || "Chưa có mã vận đơn") + '</span></td><td>' + stateBadge(row.status) +
    lineValue(row.receiptStatus === "WAITING"
      ? "Chờ khách xác nhận" : row.receiptStatus === "CONFIRMED"
        ? (row.receiptAutoConfirmedAt ? "Tự xác nhận sau 72 giờ" : "Khách đã xác nhận")
        : row.receiptStatus || "Chưa đến bước xác nhận") + '</td>' +
    '<td>' + esc(dateTimeLabel(row.shippedAt)) + '</td><td>' + esc(dateTimeLabel(row.deliveredAt)) + '</td><td>' +
    rowActions("shipments",row) + '</td></tr>';
}
function customRow(row) {
  return '<tr><td><strong class="cell-primary">' + esc(row.code || ("#" + idOf(row))) + '</strong>' + lineValue(row.fileName || "Chưa có tên tệp") + '</td>' +
    '<td><strong>' + esc(row.customerName || "—") + '</strong>' + lineValue(row.customerEmail || "") + '</td>' +
    '<td>' + esc(row.workflowType || "—") + lineValue([row.technology,row.material].filter(Boolean).join(" · ") || "Chưa khai báo thông số") + '</td>' +
    '<td>' + esc(row.shopName || "Chưa chọn xưởng") + '</td><td>' + stateBadge(row.status) + lineValue(row.adminReviewAction ? "Admin: " + (row.adminReviewAction === "REVIEWED" ? "đã rà soát" : "cần theo dõi") : "Chưa rà soát") + '</td><td>' +
    (row.quotedAmount == null ? "—" : money(row.quotedAmount)) + '</td><td>' + esc(dateTimeLabel(row.submittedAt || row.createdAt)) + '</td><td>' + rowActions("custom-prints",row) + '</td></tr>';
}
function changeRow(row) {
  return '<tr><td><strong class="cell-primary">#' + esc(idOf(row)) + '</strong>' + lineValue(row.orderCode || ("Đơn #" + row.orderId)) + '</td>' +
    '<td><strong>' + esc(row.customerName || "—") + '</strong>' + lineValue(row.customerEmail || "") + '</td>' +
    '<td>' + esc(row.requestType || "—") + lineValue(row.productName || "") + '</td>' +
    '<td><span style="white-space:normal;display:block;min-width:180px;max-width:300px">' + esc(row.reason || "—") + '</span></td>' +
    '<td>' + stateBadge(row.status) + lineValue(row.adminReviewAction ? "Admin: " + (row.adminReviewAction === "REVIEWED" ? "đã rà soát" : "cần theo dõi") : "Chưa rà soát") + '</td><td>' + esc(row.shopName || "—") + '</td><td>' + esc(dateTimeLabel(row.createdAt)) + '</td><td>' + rowActions("order-changes",row) + '</td></tr>';
}
function reviewRow(row) {
  const score = Math.max(0,Math.min(5,Number(row.rating)||0));
  const published = String(row.isPublished) === "true" || String(row.isPublished) === "1";
  return '<tr><td><strong class="cell-primary">#' + esc(idOf(row)) + '</strong>' + lineValue(dateTimeLabel(row.createdAt)) + '</td>' +
    '<td><strong>' + esc(row.productName || ("Sản phẩm #" + row.productId)) + '</strong>' + lineValue(row.shopName || "") + '</td>' +
    '<td><strong>' + esc(row.customerName || "Khách hàng") + '</strong>' + lineValue(row.customerEmail || "") + '</td>' +
    '<td><strong>' + "★".repeat(score) + "☆".repeat(5-score) + '</strong></td>' +
    '<td><span style="white-space:normal;display:block;min-width:200px;max-width:340px">' + esc(row.reviewText || "Không có nhận xét") + '</span></td>' +
    '<td>' + stateBadge(published ? "PUBLISHED" : "UNPUBLISHED") + '</td><td>' + rowActions("reviews",row) + '</td></tr>';
}
function reconciliationRow(row) {
  return '<tr><td><strong class="cell-primary">#' + esc(idOf(row)) + '</strong>' + lineValue((row.provider || "—") + " · " + (row.paymentMethod || "—")) + '</td>' +
    '<td><strong>' + esc(row.orderCode || ("Đơn #" + row.orderId)) + '</strong>' + lineValue((row.customerName || "—") + " · " + (row.customerEmail || "")) + '</td>' +
    '<td>' + (row.amount == null ? "—" : money(row.amount)) + '</td><td>' + stateBadge(row.status) + lineValue("Đơn: " + (statusNames[row.orderPaymentStatus] || row.orderPaymentStatus || "—")) + '</td>' +
    '<td><span style="white-space:normal;display:block;min-width:130px;max-width:220px">' + esc(row.providerTransactionNo || row.providerTransactionRef || "Chưa có mã tham chiếu") + '</span></td>' +
    '<td>' + stateBadge(row.lastOutcome || "OPEN") + lineValue(dateTimeLabel(row.lastReviewedAt)) + '</td><td>' + esc(dateTimeLabel(row.createdAt)) + '</td><td>' + rowActions("reconciliation",row) + '</td></tr>';
}
function tableFor(route, rows) {
  let headers, body;
  if (route === "shipments") { headers=["Đơn hàng / vận đơn","Khách hàng","Xưởng / hãng","Mã theo dõi","Trạng thái","Đã gửi lúc","Đã giao lúc","Thao tác"]; body=rows.map(shipmentRow).join(""); }
  else if (route === "custom-prints") { headers=["Yêu cầu / tệp","Khách hàng","Loại / thông số","Xưởng in","Trạng thái","Giá báo","Ngày gửi","Thao tác"]; body=rows.map(customRow).join(""); }
  else if (route === "order-changes") { headers=["Yêu cầu / đơn","Khách hàng","Loại yêu cầu","Lý do","Trạng thái","Xưởng","Ngày gửi","Thao tác"]; body=rows.map(changeRow).join(""); }
  else if (route === "reviews") { headers=["Đánh giá","Sản phẩm / xưởng","Khách hàng","Điểm","Nhận xét","Hiển thị","Thao tác"]; body=rows.map(reviewRow).join(""); }
  else { headers=["Giao dịch","Đơn / khách hàng","Số tiền","Trạng thái","Tham chiếu","Kết quả rà soát","Ngày tạo","Thao tác"]; body=rows.map(reconciliationRow).join(""); }
  if (!rows.length) return '<div class="empty-state"><span class="empty-illustration">' + icon("inbox") + '</span><strong>Không có bản ghi phù hợp</strong><p>Hãy đổi bộ lọc hoặc thử tìm bằng mã giao dịch/yêu cầu.</p></div>';
  return '<div class="table-wrap"><table class="data-table"><thead><tr>' + headers.map(label=>'<th>' + esc(label) + '</th>').join("") + '</tr></thead><tbody>' + body + '</tbody></table></div>';
}

function reviewPagination(data, filters, contextPath) {
  if (!data || Number(data.pages || 1) <= 1) return "";
  const page = Number(data.page || 1);
  const pages = Number(data.pages || 1);
  const total = Number(data.total || 0);
  const size = Number(data.pageSize || 20);
  const link = (value, label, disabled = false) => {
    const params = new URLSearchParams();
    if (filters?.q) params.set("q", filters.q);
    if (filters?.status && filters.status !== "ALL") params.set("status", filters.status);
    params.set("page", String(value));
    params.set("size", String(size));
    return '<a class="page-btn ' + (value === page ? "active" : "") + '" href="' + esc(contextPath + "/admin/reviews?" + params.toString()) + '"' + (disabled ? ' aria-disabled="true" tabindex="-1"' : '') + (value === page ? ' aria-current="page"' : '') + '>' + esc(label) + '</a>';
  };
  const start = Math.max(1, Math.min(page - 2, pages - 4));
  const numbers = [];
  for (let value = start; value <= Math.min(pages, start + 4); value++) numbers.push(link(value, String(value)));
  const from = total ? (page - 1) * size + 1 : 0;
  const to = Math.min(page * size, total);
  return '<div class="table-footer"><span>Hiển thị <strong>' + from + '–' + to + '</strong> trong <strong>' + total + '</strong> đánh giá</span><nav class="pagination" aria-label="Phân trang đánh giá">' + link(Math.max(1,page-1),"‹",page<=1) + numbers.join("") + link(Math.min(pages,page+1),"›",page>=pages) + '</nav></div>';
}


function queuePagination(rows, filters, contextPath, route, pageSize = 10) {
  const total = rows.length;
  const pages = Math.max(1, Math.ceil(total / pageSize));
  const page = Math.min(pages, Math.max(1, Number.parseInt(filters && filters.page, 10) || 1));
  const link = (value, label, disabled = false) => {
    const params = new URLSearchParams();
    if (filters && filters.q) params.set("q", filters.q);
    if (filters && filters.status) params.set("status", filters.status);
    params.set("page", String(value));
    return '<a class="page-btn ' + (value === page ? "active" : "") + '" href="' +
      esc(contextPath + "/admin/" + route + "?" + params.toString()) + '"' +
      (disabled ? ' aria-disabled="true" tabindex="-1"' : "") +
      (value === page ? ' aria-current="page"' : "") + '>' + esc(label) + '</a>';
  };
  const start = Math.max(1, Math.min(page - 2, pages - 4));
  const numbers = [];
  for (let value = start; value <= Math.min(pages, start + 4); value++) numbers.push(link(value, String(value)));
  const from = total ? (page - 1) * pageSize + 1 : 0;
  const to = Math.min(page * pageSize, total);
  return '<div class="table-footer"><span>Hiển thị <strong>' + from + '–' + to +
    '</strong> trong <strong>' + total + '</strong> bản ghi theo bộ lọc</span>' +
    '<nav class="pagination" aria-label="Phân trang hàng đợi">' +
    link(Math.max(1, page - 1), "‹", page <= 1) + numbers.join("") +
    link(Math.min(pages, page + 1), "›", page >= pages) + '</nav></div>';
}

export function renderBusinessQueue(route, data, filters, contextPath) {
  const config = definitions[route];
  if (!config) return '<section class="error-state"><h1>Không tìm thấy hàng đợi</h1></section>';
  const rows = Array.isArray(data) ? data : (Array.isArray(data?.items) ? data.items : []);
  const isServerPaginated = route === "reviews";
  const pageSize = 10;
  const totalPages = Math.max(1, Math.ceil(rows.length / pageSize));
  const requestedPage = Math.max(1, Number.parseInt(filters && filters.page, 10) || 1);
  const visiblePage = isServerPaginated ? rows : rows.slice((Math.min(requestedPage, totalPages) - 1) * pageSize, Math.min(requestedPage, totalPages) * pageSize);
  const notice = route === "shipments"
    ? '<div class="notice-card" style="margin-bottom:14px"><strong>Quy trình vận chuyển:</strong> chọn trạng thái theo thực tế; bắt buộc có hãng và mã vận đơn khi đánh dấu đã gửi/đang giao/đã giao. Không cho phép mở lại vận đơn đã giao hoặc đã hoàn/trả.</div>'
    : route === "custom-prints"
    ? '<div class="notice-card" style="margin-bottom:14px"><strong>Ranh giới nghiệp vụ:</strong> Admin ghi nhận rà soát/chuyển theo dõi. Việc định giá và gửi báo giá vẫn do xưởng in chịu trách nhiệm; trang này không tự thay đổi giá, trạng thái báo giá hoặc tạo đơn.</div>'
    : route === "order-changes"
      ? '<div class="notice-card" style="margin-bottom:14px"><strong>Kiểm soát thay đổi đơn:</strong> Admin có thể ghi nhận đã rà soát hoặc từ chối yêu cầu đang chờ. Không tự áp dụng thay đổi, hủy đơn hay điều chỉnh khoản thanh toán từ màn hình này.</div>'
      : route === "reconciliation"
        ? '<div class="notice-card" style="margin-bottom:14px"><strong>Đối soát an toàn:</strong> “Đã đối soát” chỉ đóng hàng đợi và lưu ghi chú/mã tham chiếu. Thao tác không xác nhận tiền đã thanh toán, không đổi trạng thái đơn và không gọi ngân hàng/cổng thanh toán.</div>'
        : '<div class="notice-card" style="margin-bottom:14px"><strong>Kiểm duyệt minh bạch:</strong> Khi ẩn đánh giá, cần lưu lý do. Bản ghi gốc và lịch sử thao tác vẫn được giữ lại.</div>';
  return '<div class="page-heading"><div><div class="eyebrow">' + esc(config.eyebrow) + '</div><h1>' + esc(config.title) + '</h1><p>' + esc(config.description) + '</p></div></div>' +
    notice + searchToolbar(route,filters,contextPath) + panel("Danh sách nghiệp vụ", route === "reviews" ? (Number(data?.total || rows.length) + ' đánh giá') : (rows.length + ' bản ghi trong kết quả hiện tại · tối đa 300'), tableFor(route,visiblePage) + (route === "reviews" ? reviewPagination(data, filters, contextPath) : queuePagination(rows, filters, contextPath, route, pageSize)));
}

function detailFields(route, item) {
  let fields = [];
  if (route === "shipments") fields = [
    ["Mã vận đơn",item.id],["Mã đơn hàng",item.orderCode],["Khách hàng",item.customerName],["Email",item.customerEmail],
    ["Xưởng in",item.shopName],["Hãng vận chuyển",item.carrier],["Mã theo dõi",item.trackingNumber],
    ["Nhà vận chuyển",item.carrierProvider === "GHN" ? "GHN (đồng bộ API)" : "Thủ công"],
    ["Mã trạng thái GHN",item.carrierStatusCode],["Thời điểm đồng bộ hãng",item.carrierStatusUpdatedAt],
    ["Trạng thái vận đơn",item.status],["Trạng thái xác nhận nhận hàng",item.receiptStatus],
    ["Hạn khách xác nhận",item.receiptDeadlineAt],["Tự động xác nhận lúc",item.receiptAutoConfirmedAt],
    ["Bằng chứng giao hàng (POD)",item.carrierPodUrl],["Trạng thái vận chuyển đơn",item.orderShippingStatus],
    ["Đã gửi lúc",item.shippedAt],["Đã giao lúc",item.deliveredAt]
  ];
  else if (route === "custom-prints") fields = [
    ["Mã yêu cầu",item.code],["Khách hàng",item.customerName],["Email",item.customerEmail],["Điện thoại",item.customerPhone],
    ["Tệp thiết kế",item.fileName],["Dung lượng (byte)",item.fileSizeBytes],["Xưởng in",item.shopName],["Loại luồng",item.workflowType],
    ["Công nghệ",item.technology],["Vật liệu",item.material],["Số lượng",item.quantity],["Màu sắc",item.colorOption],
    ["Gia công sau in",item.postProcessingOption],["Kích thước xác nhận",item.dimensionsConfirmed],["Trạng thái",item.status],
    ["Giá ước tính",item.estimatedTotal == null ? null : money(item.estimatedTotal)],["Giá xưởng báo",item.quotedAmount == null ? null : money(item.quotedAmount)],
    ["Số ngày sản xuất",item.productionDays],["Hiệu lực báo giá",item.quoteValidUntil],["Đơn hàng",item.orderId],["Ghi chú khách hàng",item.customerNote],
    ["Phân tích mô hình",item.analysisStatus],["Kích thước (mm)",[item.widthMm,item.depthMm,item.heightMm].filter(v=>v!=null).join(" × ")],["Thông báo phân tích",item.analysisMessage]
  ];
  else if (route === "order-changes") fields = [
    ["Mã yêu cầu",item.id],["Đơn hàng",item.orderCode],["Trạng thái đơn",item.orderStatus],["Trạng thái thanh toán",item.paymentStatus],
    ["Khách hàng",item.customerName],["Email",item.customerEmail],["Xưởng",item.shopName],["Sản phẩm",item.productName],
    ["Loại yêu cầu",item.requestType],["Trạng thái yêu cầu",item.status],["Lý do",item.reason],["Phản hồi xưởng",item.shopResponse],
    ["Chênh lệch giá",item.priceDelta == null ? null : money(item.priceDelta)],["Phí hủy",item.cancelFee == null ? null : money(item.cancelFee)],
    ["Hạn xử lý",item.expiresAt],["Khách xác nhận",item.customerConfirmedAt],["Dữ liệu đề xuất",item.payloadJson]
  ];
  else if (route === "reviews") fields = [
    ["Mã đánh giá",item.id],["Sản phẩm",item.productName],["Xưởng",item.shopName],["Khách hàng",item.customerName],["Email",item.customerEmail],
    ["Điểm",item.rating],["Đang công khai",String(item.isPublished)==="true" || String(item.isPublished)==="1" ? "Có" : "Không"],["Thời gian",item.createdAt],["Nhận xét",item.reviewText]
  ];
  else fields = [
    ["Mã giao dịch",item.id],["Mã đơn",item.orderCode],["Trạng thái đơn",item.orderStatus],["Trạng thái thanh toán của đơn",item.orderPaymentStatus],
    ["Khách hàng",item.customerName],["Email",item.customerEmail],["Phương thức",item.paymentMethod],["Cổng thanh toán",item.provider],
    ["Số tiền",item.amount == null ? null : money(item.amount)],["Trạng thái giao dịch",item.status],["Mã giao dịch nội bộ",item.providerTransactionRef],
    ["Mã tham chiếu cổng",item.providerTransactionNo],["Mã phản hồi",item.responseCode],["Ngân hàng",item.bankCode],["Loại thẻ",item.cardType],
    ["Lý do thất bại",item.failureReason],["Thanh toán lúc",item.paidAt],["Đối soát gần nhất",item.lastOutcome],
    ["Mã tham chiếu đối soát",item.lastReference],["Ghi chú gần nhất",item.lastNote]
  ];
  return fields.filter(entry=>entry[1]!=null && entry[1]!=="").map(entry=>{
    const value = typeof entry[1] === "object" ? JSON.stringify(entry[1]) : shown(entry[1]);
    return '<div class="detail-field"><small>' + esc(entry[0]) + '</small><strong>' + esc(value) + '</strong></div>';
  }).join("");
}
function eventHistory(events) {
  if (!Array.isArray(events) || !events.length) return '<p class="muted-note">Chưa có lịch sử thao tác.</p>';
  return '<div class="mini-list">' + events.map(event=>{
    const action=event.title || event.action || event.outcome || event.newStatus || "Cập nhật";
    const note=event.note || event.reason || "—";
    const actor=event.adminName || event.actorName || "Admin";
    return '<div class="mini-list-row"><span><strong>' + esc(statusNames[action] || action) + '</strong><small>' +
      esc(dateTimeLabel(event.createdAt)) + ' · ' + esc(actor) + '<br>' + esc(note) +
      (event.providerReference ? '<br>Mã tham chiếu: ' + esc(event.providerReference) : "") + '</small></span></div>';
  }).join("") + '</div>';
}
export function renderBusinessQueueDetail(route, item) {
  if (!item) return '<p class="muted-note">Không tìm thấy dữ liệu.</p>';
  const status = item.status || item.lastOutcome || (item.isPublished ? "PUBLISHED" : "UNPUBLISHED");
  const events = route === "custom-prints"
    ? [...(item.history || []), ...(item.adminEvents || [])].sort((a,b) => String(a.createdAt || "").localeCompare(String(b.createdAt || "")))
    : (item.events || item.adminEvents || item.history || []);
  const manualRefunds = Array.isArray(item.manualRefunds) ? item.manualRefunds : [];
  const manualRefundHistory = manualRefunds.length
    ? '<div class="mini-list">' + manualRefunds.map(refund => '<div class="mini-list-row"><span><strong>' + esc(refund.status === "SUCCESS" ? "THÀNH CÔNG" : "THẤT BẠI") + ' · ' + esc(money(refund.amount)) + ' · ' + esc(refund.method) + '</strong><small>' + esc(refund.reference) + ' · ' + esc(refund.adminName || "Admin") + '<br>' + esc(dateTimeLabel(refund.recordedAt)) + '<br>' + esc(refund.note || "") + '</small></span></div>').join("") + '</div>'
    : '<p class="muted-note">Chưa có lần hoàn tiền thủ công nào.</p>';
  const title = definitions[route] ? definitions[route].title : "Chi tiết nghiệp vụ";
  const extra = route === "custom-prints" && item.analysisMessage ? '<div class="notice-card">' + esc(item.analysisMessage) + '</div>' : "";
  return '<div class="detail-intro"><span><strong>' + esc(title) + ' #' + esc(item.id || item.code || "") + '</strong><small>' +
    esc(item.code || item.orderCode || item.productName || item.providerTransactionRef || "") + '</small></span>' + stateBadge(status) + '</div>' +
    extra + '<div class="detail-grid">' + detailFields(route,item) + '</div><section class="detail-section"><h3>Lịch sử xử lý</h3>' + eventHistory(events) + '</section>' +
    (route === "reconciliation" ? '<section class="detail-section"><h3>Lịch sử hoàn tiền thủ công</h3>' + manualRefundHistory + '</section>' : '');
}
