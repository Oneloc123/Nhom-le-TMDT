import { esc, icon } from "../components.js";

const money = value => new Intl.NumberFormat("vi-VN", {
  style: "currency", currency: "VND", maximumFractionDigits: 0
}).format(Number(value || 0));

const dateTime = value => {
  if (!value) return "—";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "—" : new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "short", timeStyle: "short"
  }).format(date);
};

const dateOnly = value => {
  if (!value) return "—";
  const date = new Date(String(value).length === 10 ? `${value}T00:00:00` : value);
  return Number.isNaN(date.getTime()) ? "—" : new Intl.DateTimeFormat("vi-VN").format(date);
};

function emptyRow(columns, message) {
  return `<tr><td colspan="${columns}" class="empty-cell">${esc(message)}</td></tr>`;
}

function badge(status) {
  const paid = status === "PAID" || status === "SETTLED";
  return `<span class="badge ${paid ? "badge-success" : status === "READY" || status === "AVAILABLE" ? "badge-warning" : "badge-muted"}"><i></i>${esc(status)}</span>`;
}

export function renderFinance(data = {}) {
  const summary = data.summary || {};
  const policy = data.policy || {};
  const codRows = Array.isArray(data.pendingCod) ? data.pendingCod : [];
  const settlements = Array.isArray(data.settlements) ? data.settlements : [];
  const allocations = Array.isArray(data.allocations) ? data.allocations : [];
  return `
    <section class="finance-page">
      <div class="page-heading">
        <div><div class="eyebrow">TÀI CHÍNH NỀN TẢNG</div>
          <h1>Tài chính &amp; Hoa hồng</h1>
          <p>Đối soát tiền thực thu, theo dõi hoa hồng 10% và quyết toán hàng tháng cho các xưởng.</p>
        </div>
        <div class="heading-action"><button class="button" data-action="finance-create-settlement">${icon("calendar")} Tạo kỳ quyết toán tháng</button></div>
      </div>

      <div class="notice-panel finance-policy">
        <div><strong>Chính sách đang áp dụng</strong>
          <p>Hoa hồng cố định <b>${esc(policy.commissionRate ?? 10)}%</b> trên giá trị sản phẩm, không tính phí vận chuyển.
          Quyết toán <b>hàng tháng</b>. Tiền COD do nền tảng tiếp nhận và chỉ ghi nhận sau khi có mã đối soát từ đơn vị vận chuyển.</p>
        </div>
        <span class="badge badge-neutral">Đang áp dụng</span>
      </div>

      <div class="metric-grid metric-four finance-metrics">
        <article class="metric-card metric-violet"><div class="metric-top">Tổng giá trị sản phẩm <span class="metric-icon">${icon("box")}</span></div>
          <div class="metric-value">${money(summary.grossProductAmount)}</div><div class="metric-bottom">Các khoản đã xác nhận thu tiền</div></article>
        <article class="metric-card metric-green"><div class="metric-top">Hoa hồng PrintKraft <span class="metric-icon">${icon("chart")}</span></div>
          <div class="metric-value">${money(summary.commissionAmount)}</div><div class="metric-bottom">Đã ghi nhận theo tỷ lệ snapshot</div></article>
        <article class="metric-card metric-orange"><div class="metric-top">Còn phải trả xưởng <span class="metric-icon">${icon("wallet")}</span></div>
          <div class="metric-value">${money(summary.outstandingSellerAmount)}</div><div class="metric-bottom">${Number(summary.readySettlementCount || 0)} kỳ đang chờ chuyển tiền</div></article>
        <article class="metric-card metric-blue"><div class="metric-top">COD chờ đối soát <span class="metric-icon">${icon("map")}</span></div>
          <div class="metric-value">${money(summary.pendingCodAmount)}</div><div class="metric-bottom">${Number(summary.pendingCodCount || 0)} đơn chưa xác nhận tiền về</div></article>
      </div>

      <section class="panel finance-section">
        <div class="panel-heading"><div><h2>COD chờ nền tảng xác nhận đã nhận tiền</h2>
          <p>Chỉ xác nhận sau khi đối chiếu chứng từ và mã tham chiếu chuyển tiền của hãng vận chuyển.</p></div>
          <span class="badge badge-warning">${codRows.length} đơn</span></div>
        <div class="table-scroll"><table class="data-table">
          <thead><tr><th>Mã đơn</th><th>Xưởng liên quan</th><th>Số tiền COD</th><th>Ngày tạo thanh toán</th><th>Thao tác</th></tr></thead>
          <tbody>${codRows.length ? codRows.map(row => `<tr>
            <td class="cell-primary">${esc(row.orderCode)}</td><td>${esc(row.shops || "—")}</td>
            <td class="cell-number">${money(row.amount)}</td><td>${dateTime(row.createdAt)}</td>
            <td><button class="button small" data-action="finance-cod-receive" data-id="${esc(row.orderId)}">Xác nhận tiền COD đã về</button></td>
          </tr>`).join("") : emptyRow(5, "Không có đơn COD nào đang chờ đối soát.")}</tbody>
        </table></div>
      </section>

      <section class="panel finance-section">
        <div class="panel-heading"><div><h2>Quyết toán theo tháng</h2>
          <p>Mỗi kỳ được tổng hợp theo từng xưởng. Trạng thái PAID chỉ ghi nhận khi Admin nhập mã chuyển khoản thực tế.</p></div></div>
        <div class="table-scroll"><table class="data-table">
          <thead><tr><th>Kỳ</th><th>Xưởng</th><th>Giá trị ròng</th><th>Tiền hoàn</th><th>Hoa hồng ròng</th><th>Thu hồi xưởng</th><th>Thực nhận</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
          <tbody>${settlements.length ? settlements.map(row => `<tr>
            <td>${dateOnly(row.periodStart)} – ${dateOnly(row.periodEnd)}</td>
            <td class="cell-primary">${esc(row.shopName)}</td>
            <td>${money(row.grossProductAmount)}</td><td>${money(row.refundAmount)}</td>
            <td>${money(row.commissionAmount)}</td><td>${money(row.sellerRecoveryAmount)}</td>
            <td class="cell-number">${money(row.sellerNetAmount)}</td><td>${badge(row.status)}</td>
            <td>${row.status === "READY" ? `<button class="button small" data-action="finance-settlement-pay" data-id="${esc(row.settlementId)}" data-shop="${esc(row.shopName)}" data-amount="${esc(row.sellerNetAmount)}">Xác nhận đã chuyển</button>` : row.status === "REVIEW_REQUIRED" ? "Cần rà soát số dư" : esc(row.payoutReference || "Đã chi")}</td>
          </tr>`).join("") : emptyRow(9, "Chưa có kỳ quyết toán. Các khoản đã thu sẽ xuất hiện tại đây sau khi tạo kỳ tháng đã kết thúc.")}</tbody>
        </table></div>
      </section>

      <section class="panel finance-section">
        <div class="panel-heading"><div><h2>Điều chỉnh hoàn tiền</h2>
          <p>Hoàn tiền thành công làm giảm hoa hồng và khoản phải trả xưởng theo tỷ lệ đã snapshot. Khoản thu hồi chưa bù đủ sẽ chuyển sang kỳ quyết toán sau.</p></div></div>
        <div class="table-scroll"><table class="data-table">
          <thead><tr><th>Mã đơn</th><th>Sản phẩm</th><th>Xưởng</th><th>Giá trị hoàn</th><th>Giảm hoa hồng</th><th>Thu hồi xưởng</th><th>Nguồn</th><th>Trạng thái</th><th>Ngày ghi nhận</th></tr></thead>
          <tbody>${Array.isArray(data.refundAdjustments) && data.refundAdjustments.length ? data.refundAdjustments.map(row => `<tr>
            <td class="cell-primary">${esc(row.orderCode)}</td><td>${esc(row.productName)}</td><td>${esc(row.shopName)}</td>
            <td>${money(row.refundAmount)}</td><td>${money(row.commissionReversal)}</td><td class="cell-number">${money(row.sellerRecovery)}</td>
            <td>${esc(row.sourceType)}</td><td>${badge(row.status)}</td><td>${dateTime(row.createdAt)}</td>
          </tr>`).join("") : emptyRow(9, "Chưa có bút toán điều chỉnh hoàn tiền.")}</tbody>
        </table></div>
      </section>
      <section class="panel finance-section">
        <div class="panel-heading"><div><h2>Phân bổ tài chính gần đây</h2>
          <p>Mỗi dòng sản phẩm lưu riêng giá trị, tỷ lệ hoa hồng được chụp tại thời điểm ghi nhận và khoản phải trả xưởng.</p></div></div>
        <div class="table-scroll"><table class="data-table">
          <thead><tr><th>Đơn hàng</th><th>Sản phẩm</th><th>Xưởng</th><th>Giá trị</th><th>Hoa hồng</th><th>Phải trả xưởng</th><th>Trạng thái</th></tr></thead>
          <tbody>${allocations.length ? allocations.map(row => `<tr>
            <td class="cell-primary">${esc(row.orderCode)}</td><td>${esc(row.productName)}</td><td>${esc(row.shopName)}</td>
            <td>${money(row.grossProductAmount)}</td><td>${money(row.commissionAmount)} <span class="cell-muted">(${esc(row.commissionRate)}%)</span></td>
            <td class="cell-number">${money(row.sellerNetAmount)}</td><td>${badge(row.status)}</td>
          </tr>`).join("") : emptyRow(7, "Chưa có khoản phân bổ tài chính được ghi nhận.")}</tbody>
        </table></div>
      </section>
    </section>`;
}
