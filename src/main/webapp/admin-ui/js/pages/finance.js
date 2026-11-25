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

function pageHeading(title, description, action = "") {
  return `<div class="page-heading">
    <div><div class="eyebrow">TÀI CHÍNH NỀN TẢNG</div><h1>${esc(title)}</h1><p>${esc(description)}</p></div>
    ${action ? `<div class="heading-action">${action}</div>` : ""}
  </div>`;
}

function policyPanel(policy) {
  return `<div class="notice-panel finance-policy">
    <div><strong>Chính sách đang áp dụng</strong>
      <p>Hoa hồng cố định <b>${esc(policy.commissionRate ?? 10)}%</b> trên giá trị sản phẩm, không tính phí vận chuyển.
      Quyết toán <b>hàng tháng</b>. Tiền COD chỉ được ghi nhận sau khi Admin kiểm tra tiền thực nhận và mã đối soát của đơn vị vận chuyển.</p>
    </div><span class="badge badge-neutral">Đang áp dụng</span>
  </div>`;
}

function dashboard(summary, policy, contextPath) {
  const links = [
    ["finance-cod", "COD chờ xác nhận", "Kiểm tra tiền COD hãng vận chuyển đã chuyển về.", "map", summary.pendingCodCount || 0, "đơn cần đối soát"],
    ["finance-settlements", "Quyết toán theo tháng", "Tổng hợp khoản phải trả và xác nhận chuyển tiền cho xưởng.", "chart", summary.readySettlementCount || 0, "kỳ chờ chi"],
    ["finance-refunds", "Điều chỉnh hoàn tiền", "Theo dõi khoản hoàn, đảo hoa hồng và thu hồi từ xưởng.", "check", summary.openRefundCount || 0, "điều chỉnh đang mở"],
    ["finance-ledger", "Sổ cái tài chính", "Tra cứu các bút toán thu, chi và điều chỉnh.", "file", summary.ledgerEntryCount || 0, "bút toán"],
    ["finance-allocations", "Phân bổ gần đây", "Xem chi tiết doanh thu, hoa hồng và tiền xưởng theo dòng hàng.", "box", summary.allocationCount || 0, "dòng phân bổ"]
  ];
  return `<div class="metric-grid metric-four finance-metrics">
      <article class="metric-card metric-violet"><div class="metric-top">Tổng giá trị sản phẩm <span class="metric-icon">${icon("box")}</span></div>
        <div class="metric-value">${money(summary.grossProductAmount)}</div><div class="metric-bottom">Đã ghi nhận sau thanh toán</div></article>
      <article class="metric-card metric-green"><div class="metric-top">Hoa hồng PrintKraft <span class="metric-icon">${icon("chart")}</span></div>
        <div class="metric-value">${money(summary.commissionAmount)}</div><div class="metric-bottom">Theo tỷ lệ snapshot từng dòng hàng</div></article>
      <article class="metric-card metric-orange"><div class="metric-top">Còn phải trả xưởng <span class="metric-icon">${icon("wallet")}</span></div>
        <div class="metric-value">${money(summary.outstandingSellerAmount)}</div><div class="metric-bottom">Bao gồm khoản chờ quyết toán</div></article>
      <article class="metric-card metric-blue"><div class="metric-top">COD chờ đối soát <span class="metric-icon">${icon("map")}</span></div>
        <div class="metric-value">${money(summary.pendingCodAmount)}</div><div class="metric-bottom">${Number(summary.pendingCodCount || 0)} đơn chưa xác nhận tiền về</div></article>
    </div>
    ${policyPanel(policy)}
    <section class="panel finance-section">
      <div class="panel-heading"><div><h2>Đi đến chức năng tài chính</h2>
        <p>Chọn từng khu vực để xử lý và kiểm tra dữ liệu chi tiết.</p></div></div>
      <div class="finance-shortcuts">
        ${links.map(([route, title, description, iconName, count, countLabel]) => `<a class="finance-shortcut" href="${esc(contextPath)}/admin/finance/${route.replace("finance-", "") === "finance" ? "" : route.replace("finance-", "")}" data-finance-link="${route}">
          <span class="finance-shortcut-icon">${icon(iconName)}</span>
          <span class="finance-shortcut-copy"><strong>${esc(title)}</strong><small>${esc(description)}</small><em>${Number(count || 0)} ${esc(countLabel)}</em></span>
          <span class="finance-shortcut-arrow">${icon("chevron")}</span>
        </a>`).join("")}
      </div>
    </section>`;
}

function codPage(rows) {
  return `<section class="panel finance-section">
    <div class="panel-heading"><div><h2>COD chờ nền tảng xác nhận đã nhận tiền</h2>
      <p>Chỉ xác nhận sau khi đối chiếu sao kê/chứng từ và mã tham chiếu chuyển COD của đơn vị vận chuyển.</p></div>
      <span class="badge badge-warning">${rows.length} đơn</span></div>
    <div class="table-scroll"><table class="data-table">
      <thead><tr><th>Mã đơn</th><th>Xưởng liên quan</th><th>Số tiền COD</th><th>Ngày tạo thanh toán</th><th>Thao tác</th></tr></thead>
      <tbody>${rows.length ? rows.map(row => `<tr>
        <td class="cell-primary">${esc(row.orderCode)}</td><td>${esc(row.shops || "—")}</td>
        <td class="cell-number">${money(row.amount)}</td><td>${dateTime(row.createdAt)}</td>
        <td><button class="button small" data-action="finance-cod-receive" data-id="${esc(row.orderId)}">Xác nhận tiền COD đã về</button></td>
      </tr>`).join("") : emptyRow(5, "Không có đơn COD nào đang chờ đối soát.")}</tbody>
    </table></div>
  </section>`;
}

function settlementsPage(rows) {
  const action = `<button class="button" data-action="finance-create-settlement">${icon("clock")} Tạo kỳ quyết toán tháng</button>`;
  return `<div class="finance-page-action-note"><span>Chỉ tạo kỳ cho tháng dương lịch đã kết thúc. Kỳ đã tạo sẽ không thể chạy lại.</span>${action}</div>
    <section class="panel finance-section">
      <div class="panel-heading"><div><h2>Quyết toán theo tháng</h2>
        <p>Mỗi kỳ được tổng hợp theo từng xưởng. Trạng thái PAID chỉ ghi nhận khi Admin nhập mã chuyển khoản thực tế.</p></div></div>
      <div class="table-scroll"><table class="data-table">
        <thead><tr><th>Kỳ</th><th>Xưởng</th><th>Giá trị ròng</th><th>Tiền hoàn</th><th>Hoa hồng ròng</th><th>Thu hồi xưởng</th><th>Thực nhận</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
        <tbody>${rows.length ? rows.map(row => `<tr>
          <td>${dateOnly(row.periodStart)} – ${dateOnly(row.periodEnd)}</td>
          <td class="cell-primary">${esc(row.shopName)}</td>
          <td>${money(row.grossProductAmount)}</td><td>${money(row.refundAmount)}</td>
          <td>${money(row.commissionAmount)}</td><td>${money(row.sellerRecoveryAmount)}</td>
          <td class="cell-number">${money(row.sellerNetAmount)}</td><td>${badge(row.status)}</td>
          <td>${row.status === "READY" ? `<button class="button small" data-action="finance-settlement-pay" data-id="${esc(row.settlementId)}" data-shop="${esc(row.shopName)}" data-amount="${esc(row.sellerNetAmount)}">Xác nhận đã chuyển</button>` : row.status === "REVIEW_REQUIRED" ? "Cần rà soát số dư" : esc(row.payoutReference || "Đã chi")}</td>
        </tr>`).join("") : emptyRow(9, "Chưa có kỳ quyết toán. Các khoản đủ điều kiện sẽ xuất hiện tại đây sau khi tạo kỳ tháng đã kết thúc.")}</tbody>
      </table></div>
    </section>`;
}

function refundsPage(rows) {
  return `<section class="panel finance-section">
    <div class="panel-heading"><div><h2>Điều chỉnh hoàn tiền</h2>
      <p>Hoàn tiền thành công làm giảm hoa hồng và khoản phải trả xưởng theo tỷ lệ đã snapshot. Khoản thu hồi chưa bù đủ sẽ chuyển sang kỳ quyết toán sau.</p></div>
      <span class="badge badge-neutral">${rows.length} điều chỉnh</span></div>
    <div class="table-scroll"><table class="data-table">
      <thead><tr><th>Mã đơn</th><th>Sản phẩm</th><th>Xưởng</th><th>Giá trị hoàn</th><th>Giảm hoa hồng</th><th>Thu hồi xưởng</th><th>Nguồn</th><th>Trạng thái</th><th>Ngày ghi nhận</th></tr></thead>
      <tbody>${rows.length ? rows.map(row => `<tr>
        <td class="cell-primary">${esc(row.orderCode)}</td><td>${esc(row.productName)}</td><td>${esc(row.shopName)}</td>
        <td>${money(row.refundAmount)}</td><td>${money(row.commissionReversal)}</td><td class="cell-number">${money(row.sellerRecovery)}</td>
        <td>${esc(row.sourceType)}</td><td>${badge(row.status)}</td><td>${dateTime(row.createdAt)}</td>
      </tr>`).join("") : emptyRow(9, "Chưa có bút toán điều chỉnh hoàn tiền.")}</tbody>
    </table></div>
  </section>`;
}

function ledgerPage(rows) {
  return `<section class="panel finance-section">
    <div class="panel-heading"><div><h2>Sổ cái tài chính</h2>
      <p>Nhật ký bút toán bất biến cho thu tiền, phải trả xưởng, hoa hồng, phí vận chuyển, hoàn tiền và lệnh chi.</p></div>
      <span class="badge badge-neutral">${rows.length} bút toán gần đây</span></div>
    <div class="table-scroll"><table class="data-table">
      <thead><tr><th>Thời điểm</th><th>Loại bút toán</th><th>Đơn hàng</th><th>Xưởng</th><th>Tham chiếu</th><th>Ghi nợ</th><th>Ghi có</th><th>Diễn giải</th></tr></thead>
      <tbody>${rows.length ? rows.map(row => `<tr>
        <td>${dateTime(row.createdAt)}</td><td class="cell-primary">${esc(row.entryType)}</td>
        <td>${esc(row.orderCode || "—")}</td><td>${esc(row.shopName || "—")}</td>
        <td class="cell-muted">${esc(row.referenceCode || "—")}</td>
        <td>${money(row.debitAmount)}</td><td class="cell-number">${money(row.creditAmount)}</td>
        <td>${esc(row.description || "—")}</td>
      </tr>`).join("") : emptyRow(8, "Chưa có bút toán tài chính.")}</tbody>
    </table></div>
  </section>`;
}

function allocationsPage(rows) {
  return `<section class="panel finance-section">
    <div class="panel-heading"><div><h2>Phân bổ tài chính gần đây</h2>
      <p>Mỗi dòng sản phẩm lưu riêng giá trị, tỷ lệ hoa hồng được chụp tại thời điểm ghi nhận và khoản phải trả xưởng.</p></div>
      <span class="badge badge-neutral">${rows.length} dòng phân bổ</span></div>
    <div class="table-scroll"><table class="data-table">
      <thead><tr><th>Đơn hàng</th><th>Sản phẩm</th><th>Xưởng</th><th>Giá trị</th><th>Hoa hồng</th><th>Phải trả xưởng</th><th>Trạng thái</th></tr></thead>
      <tbody>${rows.length ? rows.map(row => `<tr>
        <td class="cell-primary">${esc(row.orderCode)}</td><td>${esc(row.productName)}</td><td>${esc(row.shopName)}</td>
        <td>${money(row.grossProductAmount)}</td><td>${money(row.commissionAmount)} <span class="cell-muted">(${esc(row.commissionRate)}%)</span></td>
        <td class="cell-number">${money(row.sellerNetAmount)}</td><td>${badge(row.status)}</td>
      </tr>`).join("") : emptyRow(7, "Chưa có khoản phân bổ tài chính được ghi nhận.")}</tbody>
    </table></div>
  </section>`;
}

export function renderFinance(route = "finance", data = {}, contextPath = "") {
  const summary = data.summary || {};
  const policy = data.policy || {};
  const codRows = Array.isArray(data.pendingCod) ? data.pendingCod : [];
  const settlements = Array.isArray(data.settlements) ? data.settlements : [];
  const refundRows = Array.isArray(data.refundAdjustments) ? data.refundAdjustments : [];
  const ledgerRows = Array.isArray(data.ledger) ? data.ledger : [];
  const allocations = Array.isArray(data.allocations) ? data.allocations : [];

  const pages = {
    finance: {
      title: "Dashboard tài chính",
      description: "Tổng quan dòng tiền, hoa hồng nền tảng và các tác vụ tài chính cần xử lý.",
      content: dashboard(summary, policy, contextPath)
    },
    "finance-cod": {
      title: "COD chờ nền tảng xác nhận đã nhận tiền",
      description: "Đối chiếu chứng từ tiền COD thực tế đã về trước khi ghi nhận thanh toán.",
      content: codPage(codRows)
    },
    "finance-settlements": {
      title: "Quyết toán theo tháng",
      description: "Tổng hợp số tiền phải trả theo từng xưởng và ghi nhận mã chuyển khoản thực tế.",
      content: settlementsPage(settlements)
    },
    "finance-refunds": {
      title: "Điều chỉnh hoàn tiền",
      description: "Theo dõi phần đảo hoa hồng và khoản thu hồi từ số dư phải trả xưởng.",
      content: refundsPage(refundRows)
    },
    "finance-ledger": {
      title: "Sổ cái tài chính",
      description: "Tra cứu lịch sử ghi nợ, ghi có và các mã tham chiếu trong hệ thống.",
      content: ledgerPage(ledgerRows)
    },
    "finance-allocations": {
      title: "Phân bổ tài chính gần đây",
      description: "Kiểm tra cách giá trị từng dòng sản phẩm được chia giữa nền tảng và xưởng.",
      content: allocationsPage(allocations)
    }
  };
  const page = pages[route] || pages.finance;
  return `<section class="finance-page" data-finance-page="${esc(route)}">
    ${pageHeading(page.title, page.description)}
    ${page.content}
  </section>`;
}
