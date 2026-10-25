import { badge, dateTimeLabel, esc, icon, money, panel, table } from "../components.js";

const count = value => new Intl.NumberFormat("vi-VN").format(Number(value) || 0);
const value = (record, key) => Number(record?.[key]) || 0;

function metric(label, total, detail, iconName, accent) {
  return `<article class="metric-card metric-${accent}"><div class="metric-top"><span>${esc(label)}</span>
    <span class="metric-icon">${icon(iconName)}</span></div><div class="metric-value">${count(total)}</div>
    <div class="metric-bottom"><span>${esc(detail)}</span></div><span class="metric-cube" aria-hidden="true"></span></article>`;
}

export function renderDashboard(data = {}, contextPath = "") {
  const users = data.users || {}, sellers = data.sellers || {}, products = data.products || {};
  const statusRows = (data.orderStatuses || []).map(row => ({ status: row.status || "Chưa xác định", count: row.count }));
  const recent = table([
    { label: "Mã đơn", render: row => `<span class="cell-primary">${esc(row.code || `#${row.id}`)}</span>` },
    { label: "Khách hàng", render: row => `<span>${esc(row.customerName || "Chưa có tên")}</span><small class="cell-muted">${esc(row.customerEmail || "")}</small>` },
    { label: "Tổng tiền đơn", render: row => money(row.totalAmount) },
    { label: "Thanh toán", render: row => badge(row.paymentStatus || "Chưa xác định", "neutral") },
    { label: "Trạng thái đơn", render: row => badge(row.status || "Chưa xác định", "warning") },
    { label: "Thời gian", render: row => dateTimeLabel(row.placedAt) }
  ], data.recentOrders || [], "Chưa có đơn hàng", "Đơn hàng mới sẽ xuất hiện tại đây.");
  const pendingProducts = table([
    { label: "Sản phẩm", render: row => `<span class="cell-primary">${esc(row.name || `#${row.id}`)}</span><small class="cell-muted">${esc(row.category || "Chưa phân loại")}</small>` },
    { label: "Xưởng", render: row => esc(row.shops || "Chưa có chào bán") },
    { label: "Giá thấp nhất", render: row => row.minPrice == null ? "—" : money(row.minPrice) },
    { label: "Gửi lúc", render: row => dateTimeLabel(row.createdAt) },
    { label: "", render: row => `<button class="icon-button" data-action="open-product" data-id="${esc(row.id)}" aria-label="Xem sản phẩm">${icon("eye")}</button>` }
  ], data.pendingProducts || [], "Không có sản phẩm chờ duyệt", "Không có hồ sơ cần Admin kiểm duyệt.");
  const pendingSellers = table([
    { label: "Xưởng in", render: row => `<span class="cell-primary">${esc(row.name || `#${row.id}`)}</span><small class="cell-muted">${esc([row.district, row.province].filter(Boolean).join(", ") || "Chưa có khu vực")}</small>` },
    { label: "Chủ xưởng", render: row => `<span>${esc(row.ownerName || "Chưa liên kết tài khoản")}</span><small class="cell-muted">${esc(row.ownerEmail || "")}</small>` },
    { label: "Gửi lúc", render: row => dateTimeLabel(row.createdAt) },
    { label: "", render: row => `<button class="icon-button" data-action="detail" data-type="sellers" data-id="${esc(row.id)}" aria-label="Xem xưởng">${icon("eye")}</button>` }
  ], data.pendingSellers || [], "Không có xưởng chờ xác minh", "Các xưởng cần xác minh sẽ xuất hiện tại đây.");
  const quotes = table([
    { label: "Yêu cầu", render: row => `<span class="cell-primary">#${esc(row.id)}</span><small class="cell-muted">${esc(row.companyName || "")}</small>` },
    { label: "Khách hàng", render: row => esc(row.customerName || "Chưa có tên") },
    { label: "Sản phẩm / dịch vụ", render: row => esc(row.productName || "Không gắn sản phẩm") },
    { label: "Số lượng", key: "quantity" },
    { label: "Trạng thái", render: row => badge(row.status || "Chưa xác định", "warning") }
  ], data.pendingQuotes || [], "Không có yêu cầu báo giá chờ xử lý", "Các yêu cầu đang mở sẽ xuất hiện tại đây.");
  const statusContent = statusRows.length ? `<div class="admin-status-list">${statusRows.map(row => `<div class="bar-row"><span>${esc(row.status)}</span><span class="bar-track"><i style="width:${Math.min(100, Math.max(0, value({ count: row.count }, "count") / Math.max(1, value(data.orders, "total")) * 100))}%"></i></span><strong>${count(row.count)}</strong></div>`).join("")}</div>` : `<div class="empty-state"><strong>Chưa có trạng thái đơn hàng</strong><p>Dữ liệu sẽ hiển thị sau khi có đơn.</p></div>`;
  return `<div class="page-heading"><div><div class="eyebrow">TỔNG QUAN</div><h1>Dashboard</h1><p>Số liệu vận hành hiện tại lấy từ cơ sở dữ liệu marketplace.</p></div></div>
    <div class="metric-grid">${metric("Người dùng", users.total, `${count(users.active)} đang hoạt động`, "users", "violet")}
      ${metric("Seller / Xưởng in", sellers.total, `${count(sellers.pendingVerification)} chờ xác minh · ${count(sellers.restricted)} bị hạn chế`, "shop", "orange")}
      ${metric("Sản phẩm", products.total, `${count(products.pendingReview)} chờ kiểm duyệt`, "cube", "green")}
      ${metric("Đơn hàng", data.orders?.total, "Tất cả trạng thái nghiệp vụ", "box", "blue")}</div>
    <div class="dashboard-main-grid">${panel("Đơn hàng gần đây", "Các đơn mới nhất theo thời điểm đặt", recent, { action: `<a class="text-link" href="${contextPath}/admin/orders">Tất cả đơn ${icon("chevron")}</a>` })}
      ${panel("Đơn theo trạng thái", "Phân bổ từ dữ liệu orders", statusContent)}</div>
    <div class="dashboard-lower-grid">${panel("Sản phẩm cần kiểm duyệt", "Nội dung do seller gửi, giá lấy từ chào bán hiện có", pendingProducts, { action: `<a class="text-link" href="${contextPath}/admin/products?status=PENDING_REVIEW">Mở danh sách ${icon("chevron")}</a>` })}
      ${panel("Xưởng chờ xác minh", "Hồ sơ đang chờ Admin xác thực", pendingSellers, { action: `<a class="text-link" href="${contextPath}/admin/sellers?verification=PENDING">Mở danh sách ${icon("chevron")}</a>` })}
      ${panel("Yêu cầu báo giá", "Yêu cầu in số lượng lớn đang chờ seller xử lý", quotes, { action: `<a class="text-link" href="${contextPath}/admin/orders?view=quotes">Theo dõi yêu cầu ${icon("chevron")}</a>` })}</div>`;
}
