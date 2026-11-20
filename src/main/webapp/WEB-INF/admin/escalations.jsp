<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Can thiệp đơn hàng · PrintKraft Admin</title>
    <style>
        :root{color-scheme:light;--paper:#f4f6fa;--surface:#fff;--ink:#172033;--muted:#687386;--line:#e3e8f0;--violet:#5546d8;--sidebar:#172238;--shadow:0 8px 24px rgba(23,32,51,.045)}
        *{box-sizing:border-box}html{min-width:320px;min-height:100%;scroll-behavior:smooth}body{margin:0;background:var(--paper);color:var(--ink);font:13px/1.5 "DM Sans","Segoe UI",Arial,sans-serif;-webkit-font-smoothing:antialiased}a{color:inherit;text-decoration:none}button,input,select,textarea{font:inherit}button{cursor:pointer}button,a{-webkit-tap-highlight-color:transparent}.esc-shell{min-height:100vh;display:grid;grid-template-columns:254px minmax(0,1fr)}
        .esc-sidebar{position:sticky;top:0;height:100vh;overflow:auto;display:flex;flex-direction:column;padding:21px 14px 14px;background:var(--sidebar);border-right:1px solid #ffffff0d;color:#fff}.esc-brand{display:flex;align-items:center;gap:11px;padding:1px 9px 22px}.esc-brand-mark{position:relative;width:36px;height:36px;flex:0 0 36px;border:1px solid #b4abff66;border-radius:11px;background:linear-gradient(145deg,#6b5de1,#4c41b5);box-shadow:0 6px 15px #0c142666;transform:rotate(-5deg)}.esc-brand-mark i{position:absolute;display:block;width:10px;height:10px;border-radius:3px;background:#fff;transform:rotate(7deg)}.esc-brand-mark i:nth-child(1){left:7px;top:7px;opacity:.97}.esc-brand-mark i:nth-child(2){right:6px;top:7px;opacity:.68}.esc-brand-mark i:nth-child(3){left:12px;bottom:5px;opacity:.83}.esc-brand-copy{display:grid;line-height:1.12}.esc-brand-copy strong{font:700 19px/1 "Space Grotesk","Segoe UI",sans-serif;letter-spacing:-.8px}.esc-brand-copy strong span{color:#53c5ca}.esc-brand-copy small{margin-top:6px;color:#aeb9cb;font-size:8px;font-weight:700;letter-spacing:1.1px}.esc-nav{overflow:auto;margin:17px -3px 10px;padding:0 3px}.esc-nav-list{display:grid;gap:0}.esc-nav-label{padding:0 12px 8px;color:#a2aec1;font-size:9px;font-weight:700;letter-spacing:.9px;text-transform:uppercase}.esc-nav-link{position:relative;display:flex;align-items:center;gap:11px;min-height:42px;margin:3px 0;padding:0 11px;border:1px solid transparent;border-radius:10px;color:#c4ccda;font-size:12px;font-weight:550;white-space:nowrap;transition:background .16s,color .16s}.esc-nav-link:hover{color:#fff;background:#ffffff0b}.esc-nav-link.active{color:#fff;background:#ffffff12;border-color:#ffffff16;box-shadow:inset 3px 0 0 #8b80f1}.esc-nav-icon{display:grid;place-items:center;width:19px;flex:0 0 19px;font-size:16px}.esc-sidebar-bottom{margin-top:auto}.esc-logout-form{padding-top:10px;border-top:1px solid #ffffff19}.esc-logout{display:flex;align-items:center;gap:10px;width:100%;min-height:40px;padding:0 10px;border:1px solid transparent;border-radius:9px;background:transparent;color:#d3dbea;font-size:11px;font-weight:620;text-align:left}.esc-logout:hover{background:#ffffff0c;color:white}
        .esc-main{min-width:0;min-height:100vh}.esc-topbar{position:sticky;top:0;z-index:10;height:70px;display:flex;align-items:center;justify-content:space-between;gap:18px;padding:0 34px;border-bottom:1px solid var(--line);background:#ffffffed;backdrop-filter:blur(14px)}.esc-crumbs{display:flex;align-items:center;gap:9px;color:#79859a;font-size:11px}.esc-crumbs strong{color:var(--ink);font-size:12px;font-weight:700}.esc-content{width:min(100%,1600px);margin:0 auto;padding:29px 34px 24px}.esc-heading{display:flex;align-items:flex-end;justify-content:space-between;gap:20px;margin-bottom:22px}.esc-eyebrow{margin-bottom:7px;color:#6557d5;font-size:9px;font-weight:800;letter-spacing:1.35px;text-transform:uppercase}.esc-heading h1{margin:0;color:#172033;font:700 clamp(25px,2.2vw,32px)/1.15 "Space Grotesk","Segoe UI",sans-serif;letter-spacing:-.8px}.esc-heading p{max-width:690px;margin:9px 0 0;color:#758096;font-size:12px;line-height:1.65}.esc-heading-badge{display:inline-flex;align-items:center;gap:8px;padding:9px 12px;border:1px solid var(--line);border-radius:11px;background:#fff;color:#687386;font-size:11px;font-weight:700;white-space:nowrap}.esc-heading-badge span{width:7px;height:7px;border-radius:50%;background:#6b5de1;box-shadow:0 0 0 3px #6b5de11b}
        .esc-alert{margin:0 0 15px;padding:12px 14px;border:1px solid;border-radius:11px;font-size:12px}.esc-alert.ok{border-color:#bcead6;background:#edfbf5;color:#126b4b}.esc-alert.error{border-color:#ffd4d9;background:#fff1f3;color:#a52d3e}.esc-filter-panel,.esc-panel{min-width:0;border:1px solid var(--line);border-radius:15px;background:#fff;box-shadow:var(--shadow)}.esc-filter-panel{margin-bottom:17px;padding:16px}.esc-filter-head{display:flex;align-items:center;justify-content:space-between;gap:12px;margin-bottom:13px}.esc-filter-head h2,.esc-panel-head h2{margin:0;color:#202b40;font-size:13px;font-weight:800}.esc-filter-head p,.esc-panel-head p{margin:4px 0 0;color:#8791a4;font-size:10px}.esc-filter-form{display:grid;grid-template-columns:minmax(180px,1.8fr) minmax(145px,1fr) auto;align-items:end;gap:11px}.esc-field{display:grid;gap:6px;min-width:0}.esc-field label,.esc-field>span{color:#667287;font-size:10px;font-weight:750}.esc-field input,.esc-field select,.esc-field textarea{width:100%;min-width:0;border:1px solid #dfe4ed;border-radius:9px;background:#fff;color:#263149;outline:none}.esc-field input,.esc-field select{height:40px;padding:0 10px}.esc-field textarea{min-height:94px;padding:10px;resize:vertical}.esc-field input:focus,.esc-field select:focus,.esc-field textarea:focus{border-color:#978fe9;box-shadow:0 0 0 3px #6e62d916}.esc-primary,.esc-secondary{display:inline-flex;align-items:center;justify-content:center;gap:7px;min-height:40px;padding:0 14px;border:1px solid transparent;border-radius:9px;font-size:11px;font-weight:750;white-space:nowrap;transition:background .15s}.esc-primary{background:#5546d8;color:#fff;box-shadow:0 5px 13px #5546d81b}.esc-primary:hover{background:#4335b8}.esc-secondary{border-color:#dfe4ed;background:#fff;color:#515d72}.esc-secondary:hover{background:#f8f9fc}
        .esc-grid{display:grid;grid-template-columns:minmax(290px,.88fr) minmax(360px,1.12fr);align-items:start;gap:16px}.esc-panel{overflow:hidden}.esc-panel-head{display:flex;align-items:flex-start;justify-content:space-between;gap:10px;padding:17px 18px;border-bottom:1px solid #edf0f5}.esc-count{display:inline-flex;align-items:center;justify-content:center;min-width:27px;height:25px;padding:0 7px;border:1px solid #e4e1ff;border-radius:8px;background:#f2f0ff;color:#5e50d0;font-size:10px;font-weight:800}.esc-list{display:grid;gap:9px;padding:12px}.esc-card{min-width:0;padding:13px;border:1px solid #e5e9f1;border-radius:12px;background:#fff;transition:border-color .15s}.esc-card:hover{border-color:#c9c3fb}.esc-card.active{border-color:#aaa1f5;background:#faf9ff;box-shadow:inset 3px 0 0 #786be3}.esc-card-top{display:flex;align-items:flex-start;justify-content:space-between;gap:9px}.esc-card-code{color:#665bd0;font-size:10px;font-weight:850;letter-spacing:.3px}.esc-status{display:inline-flex;align-items:center;padding:4px 7px;border-radius:7px;background:#f0f2f6;color:#657086;font-size:9px;font-weight:800}.status-OPEN{background:#fff4e4;color:#9a5d0b}.status-IN_PROGRESS{background:#eaf3ff;color:#2865a7}.status-RESOLVED,.status-CLOSED{background:#e8f8f0;color:#187653}.esc-card h3{margin:8px 0 5px;color:#202b40;font-size:12px;font-weight:800;line-height:1.45;overflow-wrap:anywhere}.esc-card-info{margin:0;color:#687386;font-size:10px;overflow-wrap:anywhere}.esc-card-meta{display:flex;align-items:center;flex-wrap:wrap;gap:6px 11px;margin-top:10px;color:#8993a5;font-size:10px}.esc-card-action{display:inline-flex;align-items:center;gap:5px;margin-top:11px;color:#5b4ed0;font-size:10px;font-weight:800}.esc-empty{padding:36px 15px;color:#8a94a6;font-size:11px;text-align:center}
        .esc-detail{padding:18px}.esc-detail-info{margin:5px 0 0;color:#758096;font-size:11px;overflow-wrap:anywhere}.esc-detail-facts{display:flex;flex-wrap:wrap;gap:7px;margin:14px 0 16px}.esc-fact{padding:6px 8px;border:1px solid #edf0f5;border-radius:8px;background:#fafbfe;color:#69758a;font-size:10px}.esc-reason{margin:0 0 16px;padding:12px;border:1px solid #fde68a;border-radius:10px;background:#fffbeb;color:#78350f;font-size:11px;line-height:1.65;white-space:pre-wrap}.esc-verdict-box{margin:0 0 16px;padding:12px;border:1px solid #bbf7d0;border-radius:10px;background:#f0fdf4;color:#166534;font-size:11px;line-height:1.65;white-space:pre-wrap}.esc-conversation{display:grid;gap:9px;max-height:400px;overflow:auto;margin-bottom:17px;padding:4px 2px 11px;border-bottom:1px solid #edf0f5}.esc-msg{padding:11px 12px;border:1px solid #e7eaf2;border-radius:10px;background:#fafbfe;overflow-wrap:anywhere}.esc-msg.role-ADMIN{border-color:#bfdbfe;background:#eff6ff}.esc-msg.role-SHOP{border-color:#e2e8f0;background:#f8fafc}.esc-msg-head{display:flex;align-items:flex-start;justify-content:space-between;flex-wrap:wrap;gap:4px 10px;color:#374257;font-size:10px;font-weight:800}.esc-msg-head time{color:#929bad;font-size:9px;font-weight:500}.esc-role-tag{display:inline-flex;padding:2px 6px;border-radius:5px;font-size:8px;font-weight:800;letter-spacing:.3px}.esc-role-tag.CUSTOMER{background:#fef3c7;color:#92400e}.esc-role-tag.SHOP{background:#e0e7ff;color:#3730a3}.esc-role-tag.ADMIN{background:#dbeafe;color:#1e40af}.esc-msg p{margin:7px 0 0;color:#566176;font-size:11px;line-height:1.65;white-space:pre-wrap}.esc-form{display:grid;gap:12px}.esc-form .two-cols{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:11px}.esc-actions{display:flex;justify-content:flex-end;gap:8px;flex-wrap:wrap}
        @media(max-width:1120px){.esc-grid{grid-template-columns:minmax(0,1fr)}.esc-filter-form{grid-template-columns:repeat(2,minmax(0,1fr))}.esc-content{padding:24px 22px}}
        @media(max-width:900px){.esc-shell{grid-template-columns:minmax(0,1fr)}.esc-sidebar{position:relative;height:auto;min-height:0;overflow:visible;padding:13px 16px 10px;gap:12px}.esc-brand{padding:0 2px 4px}.esc-sidebar-bottom{display:none}.esc-nav{margin:2px 0 0;padding:0 0 4px;overflow-x:auto}.esc-nav-label{display:none}.esc-nav-list{display:flex;gap:5px;min-width:max-content}.esc-nav-link{min-height:36px;margin:0;padding:0 10px;font-size:10px}.esc-topbar{height:58px;padding:0 20px}.esc-content{padding:22px 20px}}
        @media(max-width:560px){.esc-content{padding:18px 12px}.esc-filter-form{grid-template-columns:minmax(0,1fr)}.esc-panel-head{padding:14px}.esc-list{padding:9px}.esc-detail{padding:13px}.esc-form .two-cols{grid-template-columns:minmax(0,1fr)}}
    </style>
</head>
<body>
<div class="esc-shell">
    <aside class="esc-sidebar" aria-label="Điều hướng quản trị">
        <a class="esc-brand" href="${pageContext.request.contextPath}/admin" aria-label="PrintKraft Admin">
            <span class="esc-brand-mark" aria-hidden="true"><i></i><i></i><i></i></span>
            <span class="esc-brand-copy"><strong>PrintKraft<span>.</span></strong><small>MARKETPLACE ADMIN</small></span>
        </a>
        <nav class="esc-nav" aria-label="Quản lý marketplace">
            <div class="esc-nav-label">Quản lý marketplace</div>
            <div class="esc-nav-list">
                <a class="esc-nav-link" href="${pageContext.request.contextPath}/admin"><span class="esc-nav-icon">▦</span><span>Dashboard</span></a>
                <a class="esc-nav-link" href="${pageContext.request.contextPath}/admin/users"><span class="esc-nav-icon">♙</span><span>Người dùng</span></a>
                <a class="esc-nav-link" href="${pageContext.request.contextPath}/admin/sellers"><span class="esc-nav-icon">⌂</span><span>Seller / Xưởng in</span></a>
                <a class="esc-nav-link" href="${pageContext.request.contextPath}/admin/products"><span class="esc-nav-icon">◇</span><span>Sản phẩm</span></a>
                <a class="esc-nav-link" href="${pageContext.request.contextPath}/admin/orders"><span class="esc-nav-icon">▣</span><span>Đơn hàng</span></a>
                <a class="esc-nav-link" href="${pageContext.request.contextPath}/admin/statistics"><span class="esc-nav-icon">▥</span><span>Thống kê</span></a>
                <a class="esc-nav-link" href="${pageContext.request.contextPath}/admin/tickets"><span class="esc-nav-icon">☏</span><span>Hỗ trợ khách hàng</span></a>
                <a class="esc-nav-link active" href="${pageContext.request.contextPath}/admin/escalations" aria-current="page"><span class="esc-nav-icon">⚖</span><span>Can thiệp đơn hàng</span></a>
            </div>
        </nav>
        <div class="esc-sidebar-bottom">
            <form class="esc-logout-form" method="post" action="${pageContext.request.contextPath}/logout">
                <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                <button class="esc-logout" type="submit"><span aria-hidden="true">↗</span>Đăng xuất</button>
            </form>
        </div>
    </aside>
    <main class="esc-main">
        <header class="esc-topbar">
            <div class="esc-crumbs"><span>PrintKraft</span><span aria-hidden="true"> › </span><strong>Can thiệp đơn hàng</strong></div>
        </header>
        <div class="esc-content">
            <div class="esc-heading">
                <div>
                    <div class="esc-eyebrow">Hòa giải tranh chấp</div>
                    <h1>Can thiệp đơn hàng</h1>
                    <p>Xem và xử lý các vụ tranh chấp giữa khách hàng và xưởng in khi hai bên không tự giải quyết được.</p>
                </div>
                <div class="esc-heading-badge"><span></span>Quản lý can thiệp</div>
            </div>
            <c:if test="${param.assigned == '1'}"><div class="esc-alert ok" role="status">Đã nhận xử lý vụ can thiệp.</div></c:if>
            <c:if test="${param.sent == '1'}"><div class="esc-alert ok" role="status">Tin nhắn đã được gửi vào thread.</div></c:if>
            <c:if test="${param.resolved == '1'}"><div class="esc-alert ok" role="status">Đã phán quyết và đóng vụ can thiệp.</div></c:if>
            <c:if test="${not empty error}"><div class="esc-alert error" role="alert"><c:out value="${error}"/></div></c:if>

            <section class="esc-filter-panel">
                <div class="esc-filter-head"><div><h2>Lọc yêu cầu can thiệp</h2><p>Tìm theo mã đơn, tên khách hàng hoặc xưởng.</p></div></div>
                <form method="get" action="${pageContext.request.contextPath}/admin/escalations" class="esc-filter-form">
                    <label class="esc-field"><span>Từ khóa</span><input type="search" name="q" value="<c:out value='${param.q}'/>" placeholder="Mã đơn, khách hàng, xưởng"></label>
                    <label class="esc-field"><span>Trạng thái</span><select name="status"><option value="">Tất cả</option><option value="OPEN" <c:if test="${param.status == 'OPEN'}">selected</c:if>>Mới mở</option><option value="IN_PROGRESS" <c:if test="${param.status == 'IN_PROGRESS'}">selected</c:if>>Đang xử lý</option><option value="RESOLVED" <c:if test="${param.status == 'RESOLVED'}">selected</c:if>>Đã giải quyết</option><option value="CLOSED" <c:if test="${param.status == 'CLOSED'}">selected</c:if>>Đã đóng</option></select></label>
                    <button class="esc-primary" type="submit">⌕ Lọc</button>
                </form>
            </section>

            <div class="esc-grid">
                <section class="esc-panel">
                    <div class="esc-panel-head"><div><h2>Danh sách can thiệp</h2><p>Các vụ tranh chấp cần xử lý.</p></div><span class="esc-count"><c:out value="${fn:length(escalations)}"/></span></div>
                    <div class="esc-list">
                        <c:choose>
                            <c:when test="${empty escalations}"><div class="esc-empty">Không có yêu cầu can thiệp nào.<br>Hãy thay đổi bộ lọc nếu cần.</div></c:when>
                            <c:otherwise><c:forEach var="e" items="${escalations}">
                                <article class="esc-card <c:if test='${escalation.id == e.id}'>active</c:if>">
                                    <div class="esc-card-top">
                                        <span class="esc-card-code">Đơn <c:out value="${e.orderCode}"/></span>
                                        <span class="esc-status status-<c:out value='${e.status}'/>"><c:out value="${e.status}"/></span>
                                    </div>
                                    <h3><c:out value="${e.shopName}"/> → <c:out value="${e.customerName}"/></h3>
                                    <p class="esc-card-info">Yêu cầu bởi: <c:out value="${e.requesterRole}"/> · <c:out value="${e.customerEmail}"/></p>
                                    <div class="esc-card-meta"><span><c:out value="${e.createdAt}"/></span><c:if test="${not empty e.adminName}"><span>Phụ trách: <c:out value="${e.adminName}"/></span></c:if></div>
                                    <a class="esc-card-action" href="${pageContext.request.contextPath}/admin/escalations?id=${e.id}&status=<c:out value='${param.status}'/>&q=<c:out value='${param.q}'/>">Xử lý <span aria-hidden="true">→</span></a>
                                </article>
                            </c:forEach></c:otherwise>
                        </c:choose>
                    </div>
                </section>

                <section class="esc-panel">
                    <div class="esc-panel-head"><div><h2>Chi tiết và xử lý</h2><p>Xem thread tin nhắn, phản hồi và phán quyết.</p></div></div>
                    <div class="esc-detail">
                        <c:choose>
                            <c:when test="${empty escalation}"><div class="esc-empty">Chọn một vụ can thiệp ở danh sách bên trái để xem chi tiết.</div></c:when>
                            <c:otherwise>
                                <div class="esc-card-code">Đơn hàng #<c:out value="${escalation.orderCode}"/></div>
                                <h3 style="margin:6px 0 0;font-size:17px;line-height:1.4;color:#202b40"><c:out value="${escalation.shopName}"/> ↔ <c:out value="${escalation.customerName}"/></h3>
                                <p class="esc-detail-info">Email: <c:out value="${escalation.customerEmail}"/></p>
                                <div class="esc-detail-facts">
                                    <span class="esc-fact">Trạng thái: <c:out value="${escalation.status}"/></span>
                                    <span class="esc-fact">Yêu cầu bởi: <c:out value="${escalation.requesterRole}"/></span>
                                    <span class="esc-fact">Ngày tạo: <c:out value="${escalation.createdAt}"/></span>
                                    <c:if test="${not empty escalation.adminName}"><span class="esc-fact">Phụ trách: <c:out value="${escalation.adminName}"/></span></c:if>
                                </div>

                                <div class="esc-reason"><b>Lý do yêu cầu can thiệp:</b><br><c:out value="${escalation.reason}"/></div>

                                <c:if test="${not empty escalation.verdict}">
                                    <div class="esc-verdict-box"><b>Phán quyết của Admin:</b><br><c:out value="${escalation.verdict}"/></div>
                                </c:if>

                                <div class="esc-conversation" aria-label="Lịch sử hội thoại đơn hàng">
                                    <c:choose>
                                        <c:when test="${empty threadMessages}"><div class="esc-empty">Chưa có tin nhắn trong thread này.</div></c:when>
                                        <c:otherwise><c:forEach var="m" items="${threadMessages}">
                                            <article class="esc-msg role-<c:out value='${m.senderRole}'/>">
                                                <div class="esc-msg-head">
                                                    <span><c:out value="${m.sender}"/> <span class="esc-role-tag <c:out value='${m.senderRole}'/>"><c:out value="${m.senderRole}"/></span></span>
                                                    <time><c:out value="${m.createdAt}"/></time>
                                                </div>
                                                <p><c:out value="${m.body}"/></p>
                                            </article>
                                        </c:forEach></c:otherwise>
                                    </c:choose>
                                </div>

                                <c:if test="${escalation.status == 'OPEN'}">
                                    <form method="post" action="${pageContext.request.contextPath}/admin/escalations" class="esc-form">
                                        <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                        <input type="hidden" name="escalationId" value="<c:out value='${escalation.id}'/>">
                                        <input type="hidden" name="action" value="assign">
                                        <div class="esc-actions"><button class="esc-primary" type="submit">✋ Nhận xử lý vụ này</button></div>
                                    </form>
                                </c:if>

                                <c:if test="${escalation.status == 'IN_PROGRESS'}">
                                    <form method="post" action="${pageContext.request.contextPath}/admin/escalations" class="esc-form" style="margin-bottom:16px">
                                        <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                        <input type="hidden" name="escalationId" value="<c:out value='${escalation.id}'/>">
                                        <input type="hidden" name="action" value="reply">
                                        <label class="esc-field"><span>Gửi tin nhắn vào thread (cả 2 bên sẽ thấy)</span><textarea name="message" rows="3" maxlength="2000" required placeholder="Nhập tin nhắn cho cả khách hàng và xưởng..."></textarea></label>
                                        <div class="esc-actions"><button class="esc-primary" type="submit">Gửi tin nhắn</button></div>
                                    </form>
                                    <form method="post" action="${pageContext.request.contextPath}/admin/escalations" class="esc-form">
                                        <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                        <input type="hidden" name="escalationId" value="<c:out value='${escalation.id}'/>">
                                        <input type="hidden" name="action" value="resolve">
                                        <label class="esc-field"><span>Phán quyết cuối cùng</span><textarea name="verdict" rows="4" maxlength="5000" required placeholder="Nêu quyết định và hướng giải quyết..."></textarea></label>
                                        <div class="esc-form two-cols">
                                            <label class="esc-field"><span>Trạng thái</span><select name="resolveStatus"><option value="RESOLVED">Đã giải quyết</option><option value="CLOSED">Đóng vụ</option></select></label>
                                        </div>
                                        <div class="esc-actions"><button class="esc-secondary" type="reset">Làm mới</button><button class="esc-primary" type="submit">⚖ Phán quyết</button></div>
                                    </form>
                                </c:if>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </section>
            </div>
        </div>
    </main>
</div>
</body>
</html>
