<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="csrf-token" content="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
  <title>Can thiệp đơn hàng · PrintKraft Admin</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/admin-ui/assets/admin.css?v=admin-escalations-r1">
  <style>
    .escalation-page{min-width:0}
    .escalation-alert{display:flex;align-items:flex-start;gap:9px;margin:0 0 14px;padding:11px 13px;border:1px solid var(--line);border-radius:10px;background:#fff;color:var(--muted);font-size:11px}
    .escalation-alert.ok{border-color:#bcead6;background:#edfbf5;color:#126b4b}
    .escalation-alert.error{border-color:#ffd4d9;background:#fff1f3;color:#a52d3e}
    .escalation-filter{display:grid;grid-template-columns:minmax(180px,1.5fr) minmax(150px,.8fr) auto;align-items:end;gap:11px;margin-bottom:15px;padding:15px;border:1px solid var(--line);border-radius:14px;background:#fff;box-shadow:var(--shadow-sm)}
    .escalation-field{display:grid;gap:6px;min-width:0;color:var(--muted);font-size:10px;font-weight:700}
    .escalation-field input,.escalation-field select,.escalation-field textarea{width:100%;min-width:0;border:1px solid var(--line-strong);border-radius:9px;background:#fff;color:var(--ink);outline:none}
    .escalation-field input,.escalation-field select{height:38px;padding:0 10px}
    .escalation-field textarea{min-height:86px;padding:10px;resize:vertical}
    .escalation-field :is(input,select,textarea):focus{border-color:#978fe9;box-shadow:0 0 0 3px #6e62d916}
    .escalation-grid{display:grid;grid-template-columns:minmax(290px,.86fr) minmax(360px,1.14fr);align-items:start;gap:14px}
    .escalation-panel{min-width:0;padding:0;overflow:hidden}
    .escalation-panel>.panel-heading{padding:17px 18px 13px;margin:0;border-bottom:1px solid #edf0f5}
    .escalation-list{display:grid;gap:9px;max-height:calc(100vh - 310px);min-height:180px;overflow:auto;padding:12px}
    .escalation-card{display:block;width:100%;padding:13px;border:1px solid var(--line);border-radius:11px;background:#fff;text-align:left;transition:border-color .15s,background .15s}
    .escalation-card:hover{border-color:#c9c3fb;background:#fcfbff}
    .escalation-card.active{border-color:#aaa1f5;background:#faf9ff;box-shadow:inset 3px 0 0 #786be3}
    .escalation-card-top,.escalation-meta,.escalation-detail-head,.escalation-actions{display:flex;align-items:center;justify-content:space-between;gap:8px;flex-wrap:wrap}
    .escalation-code{color:var(--violet);font-size:10px;font-weight:800}
    .escalation-card h3{margin:8px 0 4px;font-size:12px;line-height:1.45}
    .escalation-card p{margin:0;color:var(--muted);font-size:10px;overflow-wrap:anywhere}
    .escalation-meta{justify-content:flex-start;margin-top:9px;color:#929bad;font-size:9px}
    .escalation-status{display:inline-flex;padding:4px 7px;border-radius:7px;background:#f0f2f6;color:#657086;font-size:9px;font-weight:800}
    .escalation-status.OPEN{background:#fff4e4;color:#9a5d0b}
    .escalation-status.IN_PROGRESS{background:#eaf3ff;color:#2865a7}
    .escalation-status.RESOLVED,.escalation-status.CLOSED{background:#e8f8f0;color:#187653}
    .escalation-detail{padding:17px}
    .escalation-detail h2{margin:5px 0 3px;font:600 18px/1.35 "Space Grotesk",sans-serif}
    .escalation-subtitle{margin:0;color:var(--muted);font-size:11px;overflow-wrap:anywhere}
    .escalation-facts{display:flex;flex-wrap:wrap;gap:7px;margin:13px 0}
    .escalation-fact{padding:6px 8px;border:1px solid #edf0f5;border-radius:8px;background:#fafbfe;color:#69758a;font-size:9px}
    .escalation-reason,.escalation-verdict{margin:0 0 13px;padding:12px;border:1px solid #fde68a;border-radius:10px;background:#fffbeb;color:#78350f;font-size:11px;line-height:1.65;white-space:pre-wrap;overflow-wrap:anywhere}
    .escalation-verdict{border-color:#bbf7d0;background:#f0fdf4;color:#166534}
    .escalation-conversation{display:grid;gap:9px;max-height:390px;overflow:auto;margin:13px 0 16px;padding:3px 2px 12px;border-bottom:1px solid #edf0f5}
    .escalation-message{padding:11px 12px;border:1px solid #e7eaf2;border-radius:10px;background:#fafbfe;overflow-wrap:anywhere}
    .escalation-message.ADMIN{border-color:#bfdbfe;background:#eff6ff}
    .escalation-message.SHOP{border-color:#e2e8f0;background:#f8fafc}
    .escalation-message-head{display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:5px 10px;color:#374257;font-size:10px;font-weight:800}
    .escalation-message-head time{color:#929bad;font-size:9px;font-weight:500}
    .escalation-message p{margin:7px 0 0;color:#566176;font-size:11px;line-height:1.65;white-space:pre-wrap}
    .escalation-role{display:inline-flex;padding:2px 6px;border-radius:5px;background:#e0e7ff;color:#3730a3;font-size:8px;font-weight:800}
    .escalation-form{display:grid;gap:11px;padding-top:13px;margin-top:13px;border-top:1px solid #edf0f5}
    .escalation-form h3{margin:0;font-size:12px}
    .escalation-empty{padding:35px 14px;color:#8993a5;font-size:11px;text-align:center}
    .escalation-count{display:inline-flex;align-items:center;justify-content:center;min-width:26px;height:24px;padding:0 7px;border-radius:8px;background:var(--violet-wash);color:var(--violet);font-size:10px;font-weight:800}
    @media(max-width:1100px){.escalation-grid{grid-template-columns:minmax(0,1fr)}.escalation-list{max-height:420px}}
    @media(max-width:620px){.page-content{padding:20px 12px}.escalation-filter{grid-template-columns:minmax(0,1fr)}.escalation-detail{padding:13px}}
  </style>
</head>
<body>
<div id="admin-app">
  <main class="page-content escalation-page" id="escalation-page-content">
    <div class="page-heading">
      <div><div class="eyebrow">DISPUTE RESOLUTION</div><h1>Can thiệp đơn hàng</h1>
        <p>Tiếp nhận tranh chấp giữa khách hàng và xưởng in, trao đổi với các bên và lưu phán quyết xử lý.</p></div>
      <span class="badge badge-warning"><i></i>Quản lý tranh chấp</span>
    </div>

    <c:if test="${param.assigned == '1'}"><div class="escalation-alert ok" role="status">Đã nhận xử lý vụ can thiệp.</div></c:if>
    <c:if test="${param.sent == '1'}"><div class="escalation-alert ok" role="status">Tin nhắn đã được gửi vào hội thoại.</div></c:if>
    <c:if test="${param.resolved == '1'}"><div class="escalation-alert ok" role="status">Đã lưu phán quyết và đóng vụ can thiệp.</div></c:if>
    <c:if test="${not empty error}"><div class="escalation-alert error" role="alert"><c:out value="${error}"/></div></c:if>

    <form method="get" action="${pageContext.request.contextPath}/admin/escalations" class="escalation-filter">
      <label class="escalation-field">Tìm kiếm
        <input type="search" name="q" maxlength="120" value="<c:out value='${param.q}'/>" placeholder="Mã đơn, khách hàng, email hoặc xưởng">
      </label>
      <label class="escalation-field">Trạng thái
        <select name="status">
          <option value="">Tất cả trạng thái</option>
          <option value="OPEN" <c:if test="${param.status == 'OPEN'}">selected</c:if>>Mới mở</option>
          <option value="IN_PROGRESS" <c:if test="${param.status == 'IN_PROGRESS'}">selected</c:if>>Đang xử lý</option>
          <option value="RESOLVED" <c:if test="${param.status == 'RESOLVED'}">selected</c:if>>Đã giải quyết</option>
          <option value="CLOSED" <c:if test="${param.status == 'CLOSED'}">selected</c:if>>Đã đóng</option>
        </select>
      </label>
      <div class="row-actions"><button class="button" type="submit">${''}Lọc</button><a class="button secondary" href="${pageContext.request.contextPath}/admin/escalations">Xóa lọc</a></div>
    </form>

    <div class="escalation-grid">
      <section class="panel escalation-panel">
        <div class="panel-heading"><div><h2>Danh sách can thiệp</h2><p>Các vụ việc được sắp xếp theo mức độ cần xử lý.</p></div><span class="escalation-count"><c:out value="${fn:length(escalations)}"/></span></div>
        <div class="escalation-list">
          <c:choose>
            <c:when test="${empty escalations}"><div class="escalation-empty">Không có yêu cầu can thiệp phù hợp.<br>Hãy thử thay đổi bộ lọc.</div></c:when>
            <c:otherwise><c:forEach var="e" items="${escalations}">
              <a class="escalation-card <c:if test='${not empty escalation and escalation.id == e.id}'>active</c:if>"
                 href="${pageContext.request.contextPath}/admin/escalations?id=${e.id}&amp;status=<c:out value='${param.status}'/>&amp;q=<c:out value='${param.q}'/>">
                <div class="escalation-card-top"><span class="escalation-code">Đơn <c:out value="${e.orderCode}"/></span>
                  <span class="escalation-status <c:out value='${e.status}'/>"><c:choose>
                    <c:when test="${e.status == 'OPEN'}">Mới mở</c:when><c:when test="${e.status == 'IN_PROGRESS'}">Đang xử lý</c:when>
                    <c:when test="${e.status == 'RESOLVED'}">Đã giải quyết</c:when><c:otherwise>Đã đóng</c:otherwise>
                  </c:choose></span></div>
                <h3><c:out value="${e.shopName}"/> ↔ <c:out value="${e.customerName}"/></h3>
                <p>Người yêu cầu: <c:out value="${e.requesterRole}"/> · <c:out value="${e.customerEmail}"/></p>
                <div class="escalation-meta"><span><c:out value="${e.createdAt}"/></span><c:if test="${not empty e.adminName}"><span>Phụ trách: <c:out value="${e.adminName}"/></span></c:if></div>
              </a>
            </c:forEach></c:otherwise>
          </c:choose>
        </div>
      </section>

      <section class="panel escalation-panel">
        <div class="panel-heading"><div><h2>Chi tiết và xử lý</h2><p>Hội thoại, phản hồi và phán quyết của quản trị viên.</p></div></div>
        <div class="escalation-detail">
          <c:choose>
            <c:when test="${empty escalation}"><div class="escalation-empty">Chọn một vụ can thiệp ở danh sách để xem nội dung và thao tác.</div></c:when>
            <c:otherwise>
              <div class="escalation-detail-head"><span class="escalation-code">VỤ CAN THIỆP #<c:out value="${escalation.id}"/></span>
                <span class="escalation-status <c:out value='${escalation.status}'/>"><c:out value="${escalation.status}"/></span></div>
              <h2>Đơn <c:out value="${escalation.orderCode}"/></h2>
              <p class="escalation-subtitle"><c:out value="${escalation.shopName}"/> ↔ <c:out value="${escalation.customerName}"/> · <c:out value="${escalation.customerEmail}"/></p>
              <div class="escalation-facts">
                <span class="escalation-fact">Người yêu cầu: <c:out value="${escalation.requesterRole}"/></span>
                <span class="escalation-fact">Tạo lúc: <c:out value="${escalation.createdAt}"/></span>
                <c:if test="${not empty escalation.adminName}"><span class="escalation-fact">Phụ trách: <c:out value="${escalation.adminName}"/></span></c:if>
              </div>
              <div class="escalation-reason"><strong>Lý do yêu cầu can thiệp</strong><br><c:out value="${escalation.reason}"/></div>
              <c:if test="${not empty escalation.verdict}"><div class="escalation-verdict"><strong>Phán quyết đã lưu</strong><br><c:out value="${escalation.verdict}"/></div></c:if>

              <h3>Hội thoại của đơn hàng</h3>
              <div class="escalation-conversation" aria-label="Lịch sử hội thoại đơn hàng">
                <c:choose><c:when test="${empty threadMessages}"><div class="escalation-empty">Chưa có tin nhắn trong hội thoại.</div></c:when>
                  <c:otherwise><c:forEach var="m" items="${threadMessages}">
                    <article class="escalation-message <c:out value='${m.senderRole}'/>">
                      <div class="escalation-message-head"><span><c:out value="${m.sender}"/> <span class="escalation-role"><c:out value="${m.senderRole}"/></span></span><time><c:out value="${m.createdAt}"/></time></div>
                      <p><c:out value="${m.body}"/></p>
                    </article>
                  </c:forEach></c:otherwise></c:choose>
              </div>

              <c:if test="${escalation.status == 'OPEN'}">
                <form method="post" action="${pageContext.request.contextPath}/admin/escalations" class="escalation-form">
                  <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                  <input type="hidden" name="escalationId" value="<c:out value='${escalation.id}'/>"><input type="hidden" name="action" value="assign">
                  <div class="escalation-actions"><button class="button" type="submit">Nhận xử lý vụ việc</button></div>
                </form>
              </c:if>
              <c:if test="${escalation.status == 'IN_PROGRESS'}">
                <form method="post" action="${pageContext.request.contextPath}/admin/escalations" class="escalation-form">
                  <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                  <input type="hidden" name="escalationId" value="<c:out value='${escalation.id}'/>"><input type="hidden" name="action" value="reply">
                  <label class="escalation-field">Gửi tin nhắn cho cả khách hàng và xưởng<textarea name="message" rows="3" maxlength="2000" required placeholder="Nhập nội dung trao đổi..."></textarea></label>
                  <div class="escalation-actions"><button class="button" type="submit">Gửi tin nhắn</button></div>
                </form>
                <form method="post" action="${pageContext.request.contextPath}/admin/escalations" class="escalation-form">
                  <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                  <input type="hidden" name="escalationId" value="<c:out value='${escalation.id}'/>"><input type="hidden" name="action" value="resolve">
                  <h3>Phán quyết cuối cùng</h3>
                  <label class="escalation-field">Nội dung phán quyết<textarea name="verdict" rows="4" maxlength="5000" required placeholder="Ghi rõ quyết định, căn cứ và hướng giải quyết..."></textarea></label>
                  <label class="escalation-field">Kết quả<select name="resolveStatus"><option value="RESOLVED">Đã giải quyết</option><option value="CLOSED">Đóng vụ việc</option></select></label>
                  <div class="escalation-actions"><button class="button secondary" type="reset">Làm mới</button><button class="button" type="submit">Lưu phán quyết</button></div>
                </form>
              </c:if>
            </c:otherwise>
          </c:choose>
        </div>
      </section>
    </div>
  </main>
</div>
<script type="module">
  import { renderLayout } from "${pageContext.request.contextPath}/admin-ui/js/layout.js?v=admin-layout-workshop-review-r2";
  const contextPath = "${pageContext.request.contextPath}";
  const content = document.getElementById("escalation-page-content").outerHTML;
  const root = document.getElementById("admin-app");
  root.innerHTML = renderLayout("escalations", content, contextPath, false, null, {});
  root.addEventListener("click", event => {
    const toggle = event.target.closest('[data-action="toggle-sidebar"]');
    if (!toggle) return;
    root.querySelector(".admin-shell")?.classList.toggle("sidebar-collapsed");
  });
  const conversation = root.querySelector(".escalation-conversation");
  if (conversation) conversation.scrollTop = conversation.scrollHeight;
</script>
</body>
</html>