<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="theme-color" content="#F5F7FB">
    <meta name="csrf-token" content="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
    <title>Hỗ trợ khách hàng · PrintKraft Admin</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&family=Space+Grotesk:wght@500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/admin-ui/assets/admin.css?v=admin-nav-fix-20261126-r4">
    <style>
        .support-page { min-width: 0; }
        .support-alert { display: flex; align-items: center; gap: 10px; margin: 0 0 16px; padding: 12px 15px; border: 1px solid var(--line); border-radius: 12px; background: #fff; font-size: 12px; font-weight: 500; box-shadow: var(--shadow-sm); }
        .support-alert.ok { border-color: #bcead6; background: #edfbf5; color: #126b4b; }
        .support-alert.error { border-color: #ffd4d9; background: #fff1f3; color: #a52d3e; }

        .support-kpi-strip { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 13px; margin-bottom: 16px; }
        .support-kpi { position: relative; overflow: hidden; padding: 15px 17px; border: 1px solid var(--line); border-radius: 14px; background: var(--surface); box-shadow: var(--shadow-sm); display: grid; gap: 6px; }
        .support-kpi-label { color: var(--muted); font-size: 11px; font-weight: 600; }
        .support-kpi-value { font: 700 24px/1.1 "Space Grotesk", sans-serif; color: var(--ink); letter-spacing: -.5px; font-variant-numeric: tabular-nums; }
        .support-kpi-sub { font-size: 10px; color: var(--muted); }
        .support-kpi.is-danger .support-kpi-value { color: var(--red); }
        .support-kpi.is-warn .support-kpi-value { color: var(--orange); }
        .support-kpi.is-violet .support-kpi-value { color: var(--violet); }

        .support-filter-panel { margin-bottom: 16px; padding: 16px 18px; border: 1px solid var(--line); border-radius: 14px; background: var(--surface); box-shadow: var(--shadow-sm); }
        .support-filter-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 13px; }
        .support-filter-head h2 { margin: 0; color: #202c43; font: 600 14px/1.3 "Space Grotesk", sans-serif; }
        .support-filter-head p { margin: 3px 0 0; color: var(--muted); font-size: 11px; }
        .support-filter-form { display: grid; grid-template-columns: minmax(190px, 1.7fr) repeat(3, minmax(145px, 1fr)) auto; align-items: end; gap: 11px; }
        .support-filter-form > * { min-width: 0; }

        .support-field { display: grid; gap: 6px; min-width: 0; }
        .support-field > span { color: #59667b; font-size: 11px; font-weight: 650; }
        .support-field input, .support-field select, .support-field textarea {
            width: 100%; min-width: 0; border: 1px solid var(--line-strong); border-radius: 10px;
            background: #fff; color: var(--ink); font-size: 12px; outline: none; transition: border-color .15s, box-shadow .15s;
        }
        .support-field input, .support-field select { height: 40px; padding: 0 12px; }
        .support-field textarea { min-height: 98px; padding: 11px 12px; resize: vertical; line-height: 1.55; }
        .support-field :is(input, select, textarea):focus { border-color: #978fe9; box-shadow: 0 0 0 3px #6e62d91a; }
        .support-filter-actions { display: flex; align-items: center; gap: 8px; }

        .support-workspace-grid { display: grid; grid-template-columns: minmax(310px, .88fr) minmax(380px, 1.12fr); align-items: start; gap: 15px; }
        .support-panel { min-width: 0; padding: 0; overflow: hidden; }
        .support-panel > .panel-heading { padding: 17px 20px 14px; margin: 0; border-bottom: 1px solid #edf0f5; }
        .support-count { display: inline-flex; align-items: center; justify-content: center; min-width: 28px; height: 26px; padding: 0 9px; border: 1px solid #e1deff; border-radius: 8px; background: var(--violet-wash); color: var(--violet); font-size: 11px; font-weight: 700; }

        .support-ticket-list { display: grid; gap: 10px; max-height: calc(100vh - 300px); min-height: 220px; overflow: auto; padding: 14px; scrollbar-width: thin; scrollbar-color: #d2dae7 transparent; }
        .support-ticket-card {
            display: block; width: 100%; padding: 14px 15px; border: 1px solid var(--line); border-radius: 12px;
            background: #fff; color: inherit; text-decoration: none; transition: border-color .16s, background .16s, transform .16s, box-shadow .16s;
        }
        .support-ticket-card:hover { border-color: #c8c1f8; background: #fcfbff; transform: translateY(-1px); box-shadow: 0 6px 16px rgba(23, 32, 51, .05); }
        .support-ticket-card:focus-visible { outline: 3px solid #6e62d933; outline-offset: 2px; border-color: #8f84eb; }
        .support-ticket-card.active { border-color: #9f95f2; background: #faf9ff; box-shadow: inset 3px 0 0 var(--violet), 0 6px 16px rgba(85, 70, 216, .08); }
        .support-ticket-top { display: flex; align-items: center; justify-content: space-between; gap: 8px; flex-wrap: wrap; }
        .support-ticket-code { display: inline-flex; align-items: center; padding: 3px 8px; border-radius: 7px; background: var(--violet-wash); color: var(--violet); font-size: 11px; font-weight: 700; letter-spacing: .25px; }
        .support-ticket-card h3 { margin: 9px 0 4px; color: var(--ink); font: 600 13px/1.45 "Space Grotesk", "DM Sans", sans-serif; overflow-wrap: anywhere; }
        .support-ticket-customer { margin: 0; color: var(--muted); font-size: 11px; overflow-wrap: anywhere; }
        .support-ticket-meta { display: flex; align-items: center; flex-wrap: wrap; gap: 7px 10px; margin-top: 11px; padding-top: 10px; border-top: 1px dashed #edf0f5; color: var(--muted); font-size: 11px; }

        .support-pill { display: inline-flex; align-items: center; gap: 5px; padding: 4px 9px; border: 1px solid transparent; border-radius: 999px; font-size: 10px; font-weight: 700; white-space: nowrap; }
        .support-pill-dot { width: 6px; height: 6px; border-radius: 50%; background: currentColor; }
        .status-OPEN { background: #fff5e4; border-color: #f3dfb8; color: #9a5d0b; }
        .status-IN_PROGRESS { background: #edf5ff; border-color: #cbdff8; color: #2660a0; }
        .status-WAITING_CUSTOMER { background: #f3f0ff; border-color: #dfd8ff; color: #5546d8; }
        .status-RESOLVED, .status-CLOSED { background: #eaf7f0; border-color: #cbe8d7; color: #176a4b; }

        .priority-chip { display: inline-flex; align-items: center; gap: 6px; font-size: 11px; font-weight: 600; color: #46536a; }
        .priority-chip::before { content: ""; width: 7px; height: 7px; border-radius: 50%; background: #9aa3b4; }
        .priority-HIGH { color: #a73440; }
        .priority-HIGH::before { background: #df596b; }
        .priority-NORMAL { color: #8c5a10; }
        .priority-NORMAL::before { background: #e09c38; }
        .priority-LOW { color: #197453; }
        .priority-LOW::before { background: #2a9a73; }

        .sla-pill { display: inline-flex; align-items: center; padding: 3px 8px; border: 1px solid #e6eaf1; border-radius: 7px; background: #f6f8fb; color: #5a667c; font-size: 10px; font-weight: 700; }
        .sla-OVERDUE { border-color: #f5c8cd; background: #fff0f1; color: #b33543; }
        .sla-DUE_SOON { border-color: #f1ddb2; background: #fff7e7; color: #96600e; }
        .sla-PAUSED { border-color: #cadcf4; background: #edf5ff; color: #3565a3; }
        .sla-RESPONDED, .sla-RESOLVED, .sla-CLOSED { border-color: #bfe6d2; background: #ecfbf3; color: #187753; }
        .sla-ON_TRACK { border-color: #d6e9df; background: #f2faf5; color: #2b7050; }

        .support-detail-body { padding: 20px; }
        .support-detail-head { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 8px; margin-bottom: 10px; }
        .support-detail-badges { display: flex; align-items: center; flex-wrap: wrap; gap: 7px; }
        .support-detail-title { margin: 0 0 5px; color: var(--ink); font: 600 19px/1.35 "Space Grotesk", sans-serif; letter-spacing: -.3px; overflow-wrap: anywhere; }
        .support-detail-customer { margin: 0; color: var(--muted); font-size: 12px; overflow-wrap: anywhere; }

        .support-facts-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 9px; margin: 16px 0; }
        .support-fact-box { padding: 10px 12px; border: 1px solid #edf0f5; border-radius: 10px; background: #fafbfe; display: grid; gap: 3px; }
        .support-fact-box span { color: var(--muted); font-size: 10px; font-weight: 600; }
        .support-fact-box strong { color: var(--ink); font-size: 12px; font-weight: 700; overflow-wrap: anywhere; }

        .support-attachments { margin: 0 0 16px; padding: 12px 14px; border: 1px solid #e3e8f0; border-radius: 12px; background: #f8faff; }
        .support-attachments-title { display: block; margin-bottom: 8px; color: #202c43; font-size: 12px; font-weight: 700; }
        .support-attachment-list { display: flex; flex-wrap: wrap; gap: 8px; }
        .support-attachment-chip {
            display: inline-flex; align-items: center; gap: 7px; padding: 7px 11px; border: 1px solid #dce3f2;
            border-radius: 9px; background: #fff; color: var(--violet); font-size: 11px; font-weight: 600;
            transition: border-color .15s, background .15s;
        }
        .support-attachment-chip:hover { border-color: #9e93f0; background: #faf9ff; }
        .support-attachment-chip small { color: var(--muted); font-weight: 500; }

        .support-section-label { margin: 0 0 10px; color: #202c43; font: 600 13px/1.3 "Space Grotesk", sans-serif; }
        .support-conversation {
            display: grid; gap: 10px; max-height: 360px; overflow: auto; margin-bottom: 18px;
            padding: 12px; border: 1px solid #edf0f5; border-radius: 12px; background: #f9fafd;
            scrollbar-width: thin; scrollbar-color: #d2dae7 transparent;
        }
        .support-message { padding: 12px 14px; border: 1px solid #e5e9f2; border-radius: 11px; background: #fff; overflow-wrap: anywhere; }
        .support-message.role-ADMIN { border-color: #d2dcfa; background: #f4f7ff; }
        .support-message.internal { border-color: #f1dfb8; background: #fffaf0; }
        .support-message-head { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 6px 10px; color: #202c43; font-size: 11px; font-weight: 700; }
        .support-message-role { display: inline-flex; padding: 2px 7px; border-radius: 6px; background: #eef1f7; color: #4a576e; font-size: 10px; font-weight: 700; margin-left: 6px; }
        .support-message.role-ADMIN .support-message-role { background: #e3e8ff; color: #3c43a8; }
        .support-message.internal .support-message-role { background: #fef0c7; color: #93580c; }
        .support-message-head time { color: #8a94a6; font-size: 10px; font-weight: 500; }
        .support-message p { margin: 8px 0 0; color: #3c475d; font-size: 12px; line-height: 1.65; white-space: pre-wrap; }

        .support-update-form { display: grid; gap: 13px; padding-top: 16px; border-top: 1px solid #edf0f5; }
        .support-update-form .two-fields { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
        .support-internal-toggle {
            display: flex; align-items: center; gap: 9px; padding: 10px 12px; border: 1px solid var(--line);
            border-radius: 10px; background: #fafbfe; color: #46536a; font-size: 12px; font-weight: 500; cursor: pointer;
            transition: border-color .15s, background .15s;
        }
        .support-internal-toggle:has(input:checked) { border-color: #f1dfb8; background: #fffaf0; color: #8a560e; }
        .support-internal-toggle input { width: 15px; height: 15px; accent-color: var(--violet); }
        .support-update-actions { display: flex; justify-content: flex-end; gap: 9px; flex-wrap: wrap; }

        .support-empty-box { min-height: 220px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; padding: 28px 18px; text-align: center; color: var(--muted); }
        .support-empty-icon { width: 44px; height: 44px; display: grid; place-items: center; border: 1px solid #e1deff; border-radius: 13px; background: var(--violet-wash); color: var(--violet); }
        .support-empty-box strong { color: var(--ink); font-size: 13px; }
        .support-empty-box p { margin: 0; max-width: 310px; font-size: 12px; line-height: 1.5; }

        @media (max-width: 1180px) {
            .support-kpi-strip { grid-template-columns: repeat(2, minmax(0, 1fr)); }
            .support-filter-form { grid-template-columns: repeat(2, minmax(0, 1fr)); }
            .support-filter-actions { grid-column: 1 / -1; justify-content: flex-start; }
            .support-workspace-grid { grid-template-columns: minmax(0, 1fr); }
            .support-ticket-list { max-height: 430px; }
        }
        @media (max-width: 640px) {
            .support-kpi-strip, .support-filter-form, .support-facts-grid, .support-update-form .two-fields { grid-template-columns: minmax(0, 1fr); }
            .support-detail-body { padding: 14px; }
            .support-filter-actions, .support-update-actions { display: grid; grid-template-columns: 1fr; }
            .support-filter-actions :is(.button, a), .support-update-actions .button { width: 100%; justify-content: center; }
        }
    </style>
</head>
<body>
<c:set var="openCount" value="0"/>
<c:set var="slaAlertCount" value="0"/>
<c:set var="highPriorityCount" value="0"/>
<c:forEach var="t" items="${tickets}">
    <c:if test="${t.status == 'OPEN' or t.status == 'IN_PROGRESS' or t.status == 'WAITING_CUSTOMER'}">
        <c:set var="openCount" value="${openCount + 1}"/>
        <c:if test="${t.priority == 'HIGH'}"><c:set var="highPriorityCount" value="${highPriorityCount + 1}"/></c:if>
    </c:if>
    <c:if test="${t.slaState == 'OVERDUE' or t.slaState == 'DUE_SOON'}">
        <c:set var="slaAlertCount" value="${slaAlertCount + 1}"/>
    </c:if>
</c:forEach>

<div id="admin-app" data-context-path="${pageContext.request.contextPath}">
    <div class="support-page" id="support-page-content">
        <div class="page-heading">
            <div>
                <div class="eyebrow">CHĂM SÓC KHÁCH HÀNG</div>
                <h1>Hỗ trợ khách hàng</h1>
                <p>Tiếp nhận yêu cầu, theo dõi hạn phản hồi SLA và trao đổi trực tiếp với khách hàng trong cùng giao diện quản trị.</p>
            </div>
            <div class="heading-action">
                <span class="badge badge-neutral"><i></i>Quản lý ticket</span>
            </div>
        </div>

        <c:if test="${param.updated == '1'}"><div class="support-alert ok" role="status">Đã cập nhật ticket và gửi phản hồi thành công.</div></c:if>
        <c:if test="${not empty error}"><div class="support-alert error" role="alert"><c:out value="${error}"/></div></c:if>

        <div class="support-kpi-strip">
            <article class="support-kpi is-violet">
                <span class="support-kpi-label">Tổng yêu cầu theo bộ lọc</span>
                <strong class="support-kpi-value"><c:out value="${fn:length(tickets)}"/></strong>
                <span class="support-kpi-sub">Tối đa 200 ticket mới nhất</span>
            </article>
            <article class="support-kpi">
                <span class="support-kpi-label">Đang cần xử lý</span>
                <strong class="support-kpi-value"><c:out value="${openCount}"/></strong>
                <span class="support-kpi-sub">Mới mở, đang xử lý &amp; chờ khách</span>
            </article>
            <article class="support-kpi is-danger">
                <span class="support-kpi-label">Cảnh báo SLA</span>
                <strong class="support-kpi-value"><c:out value="${slaAlertCount}"/></strong>
                <span class="support-kpi-sub">Quá hạn hoặc sắp đến hạn (4h)</span>
            </article>
            <article class="support-kpi is-warn">
                <span class="support-kpi-label">Ưu tiên cao đang mở</span>
                <strong class="support-kpi-value"><c:out value="${highPriorityCount}"/></strong>
                <span class="support-kpi-sub">Cần phản hồi sớm</span>
            </article>
        </div>

        <section class="support-filter-panel">
            <div class="support-filter-head">
                <div>
                    <h2>Lọc yêu cầu hỗ trợ</h2>
                    <p>Tìm nhanh theo mã ticket, tiêu đề, tên hoặc email khách hàng.</p>
                </div>
            </div>
            <form method="get" action="${pageContext.request.contextPath}/admin/tickets" class="support-filter-form">
                <label class="support-field"><span>Từ khóa</span><input type="search" name="q" value="<c:out value='${param.q}'/>" placeholder="Mã ticket, tiêu đề, email khách hàng..."></label>
                <label class="support-field"><span>Trạng thái</span><select name="status">
                    <option value="">Tất cả trạng thái</option>
                    <option value="OPEN" <c:if test="${param.status == 'OPEN'}">selected</c:if>>Mới mở</option>
                    <option value="IN_PROGRESS" <c:if test="${param.status == 'IN_PROGRESS'}">selected</c:if>>Đang xử lý</option>
                    <option value="WAITING_CUSTOMER" <c:if test="${param.status == 'WAITING_CUSTOMER'}">selected</c:if>>Chờ khách hàng</option>
                    <option value="RESOLVED" <c:if test="${param.status == 'RESOLVED'}">selected</c:if>>Đã giải quyết</option>
                    <option value="CLOSED" <c:if test="${param.status == 'CLOSED'}">selected</c:if>>Đã đóng</option>
                </select></label>
                <label class="support-field"><span>Mức ưu tiên</span><select name="priority">
                    <option value="">Tất cả mức ưu tiên</option>
                    <option value="HIGH" <c:if test="${param.priority == 'HIGH'}">selected</c:if>>Cao</option>
                    <option value="NORMAL" <c:if test="${param.priority == 'NORMAL'}">selected</c:if>>Bình thường</option>
                    <option value="LOW" <c:if test="${param.priority == 'LOW'}">selected</c:if>>Thấp</option>
                </select></label>
                <label class="support-field"><span>Trạng thái SLA</span><select name="sla">
                    <option value="ALL" <c:if test="${empty param.sla or param.sla == 'ALL'}">selected</c:if>>Tất cả SLA</option>
                    <option value="OVERDUE" <c:if test="${param.sla == 'OVERDUE'}">selected</c:if>>Quá hạn</option>
                    <option value="DUE_SOON" <c:if test="${param.sla == 'DUE_SOON'}">selected</c:if>>Sắp đến hạn (4 giờ)</option>
                    <option value="ON_TRACK" <c:if test="${param.sla == 'ON_TRACK'}">selected</c:if>>Trong hạn</option>
                    <option value="PAUSED" <c:if test="${param.sla == 'PAUSED'}">selected</c:if>>Tạm dừng chờ khách</option>
                    <option value="RESPONDED" <c:if test="${param.sla == 'RESPONDED'}">selected</c:if>>Đã phản hồi</option>
                </select></label>
                <div class="support-filter-actions">
                    <button class="button" type="submit">Áp dụng lọc</button>
                    <a class="button secondary" href="${pageContext.request.contextPath}/admin/tickets">Xóa lọc</a>
                </div>
            </form>
        </section>

        <div class="support-workspace-grid">
            <section class="panel support-panel">
                <div class="panel-heading">
                    <div>
                        <h2>Danh sách yêu cầu</h2>
                        <p>Sắp xếp theo mức ưu tiên và hạn SLA cần phản hồi.</p>
                    </div>
                    <span class="support-count"><c:out value="${fn:length(tickets)}"/></span>
                </div>
                <div class="support-ticket-list" role="list" aria-label="Danh sách yêu cầu hỗ trợ">
                    <c:choose>
                        <c:when test="${empty tickets}">
                            <div class="support-empty-box">
                                <span class="support-empty-icon" aria-hidden="true">
                                    <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M4 13a8 8 0 0 1 16 0v3a2 2 0 0 1-2 2h-1.5v-5H19a7 7 0 0 0-14 0h1.5v5H5a2 2 0 0 1-2-2v-3Z"/><path d="M18 18v1a3 3 0 0 1-3 3h-3"/></svg>
                                </span>
                                <strong>Không tìm thấy yêu cầu phù hợp</strong>
                                <p>Thử thay đổi trạng thái, mức ưu tiên hoặc xóa từ khóa tìm kiếm để xem tất cả ticket.</p>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="t" items="${tickets}">
                                <a role="listitem" class="support-ticket-card <c:if test='${ticket.id == t.id}'>active</c:if>"
                                   <c:if test="${ticket.id == t.id}">aria-current="true"</c:if>
                                   href="${pageContext.request.contextPath}/admin/tickets?id=${t.id}&amp;status=<c:out value='${param.status}'/>&amp;priority=<c:out value='${param.priority}'/>&amp;sla=<c:out value='${param.sla}'/>&amp;q=<c:out value='${param.q}'/>">
                                    <div class="support-ticket-top">
                                        <span class="support-ticket-code"><c:out value="${t.code}"/></span>
                                        <span class="support-pill status-<c:out value='${t.status}'/>"><i class="support-pill-dot"></i><c:choose>
                                            <c:when test="${t.status == 'OPEN'}">Mới mở</c:when>
                                            <c:when test="${t.status == 'IN_PROGRESS'}">Đang xử lý</c:when>
                                            <c:when test="${t.status == 'WAITING_CUSTOMER'}">Chờ khách hàng</c:when>
                                            <c:when test="${t.status == 'RESOLVED'}">Đã giải quyết</c:when>
                                            <c:when test="${t.status == 'CLOSED'}">Đã đóng</c:when>
                                            <c:otherwise><c:out value="${t.status}"/></c:otherwise>
                                        </c:choose></span>
                                    </div>
                                    <h3><c:out value="${t.subject}"/></h3>
                                    <p class="support-ticket-customer"><c:out value="${t.customer}"/> · <c:out value="${t.email}"/></p>
                                    <div class="support-ticket-meta">
                                        <span class="priority-chip priority-<c:out value='${t.priority}'/>"><c:choose>
                                            <c:when test="${t.priority == 'HIGH'}">Ưu tiên cao</c:when>
                                            <c:when test="${t.priority == 'NORMAL'}">Bình thường</c:when>
                                            <c:when test="${t.priority == 'LOW'}">Ưu tiên thấp</c:when>
                                            <c:otherwise><c:out value="${t.priority}"/></c:otherwise>
                                        </c:choose></span>
                                        <span class="sla-pill sla-<c:out value='${t.slaState}'/>"><c:choose>
                                            <c:when test="${t.slaState == 'OVERDUE'}">Quá hạn SLA</c:when>
                                            <c:when test="${t.slaState == 'DUE_SOON'}">Sắp đến hạn</c:when>
                                            <c:when test="${t.slaState == 'PAUSED'}">Tạm dừng chờ khách</c:when>
                                            <c:when test="${t.slaState == 'RESPONDED'}">Đã phản hồi</c:when>
                                            <c:when test="${t.slaState == 'ON_TRACK'}">Trong hạn</c:when>
                                            <c:when test="${t.slaState == 'RESOLVED'}">Đã giải quyết</c:when>
                                            <c:when test="${t.slaState == 'CLOSED'}">Đã đóng</c:when>
                                            <c:otherwise>Chưa có hạn SLA</c:otherwise>
                                        </c:choose></span>
                                        <c:if test="${not empty t.slaDueAt}"><span>Hạn: <c:out value="${t.slaDueAt}"/></span></c:if>
                                    </div>
                                </a>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </div>
            </section>

            <section class="panel support-panel">
                <div class="panel-heading">
                    <div>
                        <h2>Chi tiết và xử lý</h2>
                        <p>Xem hồ sơ đính kèm, trao đổi với khách hàng và cập nhật tiến độ xử lý.</p>
                    </div>
                </div>
                <div class="support-detail-body">
                    <c:choose>
                        <c:when test="${empty ticket}">
                            <div class="support-empty-box">
                                <span class="support-empty-icon" aria-hidden="true">
                                    <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
                                </span>
                                <strong>Chưa chọn yêu cầu hỗ trợ</strong>
                                <p>Chọn một ticket ở danh sách bên trái để xem nội dung chi tiết, lịch sử hội thoại và phản hồi khách hàng.</p>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <div class="support-detail-head">
                                <span class="support-ticket-code"><c:out value="${ticket.code}"/></span>
                                <div class="support-detail-badges">
                                    <span class="support-pill status-<c:out value='${ticket.status}'/>"><i class="support-pill-dot"></i><c:choose>
                                        <c:when test="${ticket.status == 'OPEN'}">Mới mở</c:when>
                                        <c:when test="${ticket.status == 'IN_PROGRESS'}">Đang xử lý</c:when>
                                        <c:when test="${ticket.status == 'WAITING_CUSTOMER'}">Chờ khách hàng</c:when>
                                        <c:when test="${ticket.status == 'RESOLVED'}">Đã giải quyết</c:when>
                                        <c:when test="${ticket.status == 'CLOSED'}">Đã đóng</c:when>
                                        <c:otherwise><c:out value="${ticket.status}"/></c:otherwise>
                                    </c:choose></span>
                                    <span class="sla-pill sla-<c:out value='${ticket.slaState}'/>"><c:choose>
                                        <c:when test="${ticket.slaState == 'OVERDUE'}">Quá hạn SLA</c:when>
                                        <c:when test="${ticket.slaState == 'DUE_SOON'}">Sắp đến hạn</c:when>
                                        <c:when test="${ticket.slaState == 'PAUSED'}">Tạm dừng chờ khách</c:when>
                                        <c:when test="${ticket.slaState == 'RESPONDED'}">Đã phản hồi</c:when>
                                        <c:when test="${ticket.slaState == 'ON_TRACK'}">Trong hạn</c:when>
                                        <c:when test="${ticket.slaState == 'RESOLVED'}">Đã giải quyết</c:when>
                                        <c:when test="${ticket.slaState == 'CLOSED'}">Đã đóng</c:when>
                                        <c:otherwise>Chưa có hạn SLA</c:otherwise>
                                    </c:choose></span>
                                </div>
                            </div>
                            <h2 class="support-detail-title"><c:out value="${ticket.subject}"/></h2>
                            <p class="support-detail-customer">Khách hàng: <strong><c:out value="${ticket.customer}"/></strong> · <c:out value="${ticket.email}"/></p>

                            <div class="support-facts-grid">
                                <div class="support-fact-box"><span>Loại yêu cầu</span><strong><c:out value="${not empty ticket.category ? ticket.category : 'Chung'}"/></strong></div>
                                <div class="support-fact-box"><span>Mã đơn liên quan</span><strong><c:choose><c:when test="${not empty ticket.orderId}">#<c:out value="${ticket.orderId}"/></c:when><c:otherwise>Không gắn đơn</c:otherwise></c:choose></strong></div>
                                <div class="support-fact-box"><span>Xưởng liên quan</span><strong><c:choose><c:when test="${not empty ticket.shopId}">Shop #<c:out value="${ticket.shopId}"/></c:when><c:otherwise>Không có</c:otherwise></c:choose></strong></div>
                                <div class="support-fact-box"><span>Tạo lúc</span><strong><c:out value="${not empty ticket.createdAt ? ticket.createdAt : '—'}"/></strong></div>
                                <div class="support-fact-box"><span>Hạn SLA</span><strong><c:out value="${not empty ticket.slaDueAt ? ticket.slaDueAt : 'Chưa thiết lập'}"/></strong></div>
                                <div class="support-fact-box"><span>Phản hồi đầu tiên</span><strong><c:out value="${not empty ticket.firstResponseAt ? ticket.firstResponseAt : 'Chưa phản hồi'}"/></strong></div>
                            </div>

                            <c:if test="${not empty ticketAttachments}">
                                <div class="support-attachments">
                                    <span class="support-attachments-title">Tệp khách hàng đính kèm</span>
                                    <div class="support-attachment-list">
                                        <c:forEach var="a" items="${ticketAttachments}">
                                            <a class="support-attachment-chip" href="${pageContext.request.contextPath}/support/attachment?id=${a.id}">
                                                <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6Z"/><path d="M14 2v6h6"/></svg>
                                                <span><c:out value="${a.fileName}"/></span>
                                                <small>(<c:out value="${a.size}"/> B)</small>
                                            </a>
                                        </c:forEach>
                                    </div>
                                </div>
                            </c:if>

                            <h3 class="support-section-label">Lịch sử hội thoại</h3>
                            <div class="support-conversation" aria-label="Lịch sử hội thoại">
                                <c:choose>
                                    <c:when test="${empty ticketMessages}"><div class="support-empty-box" style="min-height:110px"><p>Chưa có tin nhắn trong ticket này.</p></div></c:when>
                                    <c:otherwise>
                                        <c:forEach var="m" items="${ticketMessages}">
                                            <article class="support-message role-<c:out value='${m.role}'/> <c:if test='${m.internal}'>internal</c:if>">
                                                <div class="support-message-head">
                                                    <span><c:out value="${m.sender}"/><span class="support-message-role"><c:choose>
                                                        <c:when test="${m.internal}">Ghi chú nội bộ</c:when>
                                                        <c:when test="${m.role == 'ADMIN'}">Quản trị viên</c:when>
                                                        <c:otherwise>Khách hàng</c:otherwise>
                                                    </c:choose></span></span>
                                                    <time><c:out value="${m.createdAt}"/></time>
                                                </div>
                                                <p><c:out value="${m.body}"/></p>
                                            </article>
                                        </c:forEach>
                                    </c:otherwise>
                                </c:choose>
                            </div>

                            <form method="post" action="${pageContext.request.contextPath}/admin/tickets" class="support-update-form">
                                <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                <input type="hidden" name="ticketId" value="<c:out value='${ticket.id}'/>">
                                <input type="hidden" name="q" value="<c:out value='${param.q}'/>">
                                <input type="hidden" name="sla" value="<c:out value='${param.sla}'/>">
                                <div class="two-fields">
                                    <label class="support-field"><span>Trạng thái</span><select name="status">
                                        <option value="OPEN" <c:if test="${ticket.status == 'OPEN'}">selected</c:if>>Mới mở</option>
                                        <option value="IN_PROGRESS" <c:if test="${ticket.status == 'IN_PROGRESS'}">selected</c:if>>Đang xử lý</option>
                                        <option value="WAITING_CUSTOMER" <c:if test="${ticket.status == 'WAITING_CUSTOMER'}">selected</c:if>>Chờ khách hàng</option>
                                        <option value="RESOLVED" <c:if test="${ticket.status == 'RESOLVED'}">selected</c:if>>Đã giải quyết</option>
                                        <option value="CLOSED" <c:if test="${ticket.status == 'CLOSED'}">selected</c:if>>Đã đóng</option>
                                    </select></label>
                                    <label class="support-field"><span>Mức ưu tiên</span><select name="priority">
                                        <option value="LOW" <c:if test="${ticket.priority == 'LOW'}">selected</c:if>>Thấp</option>
                                        <option value="NORMAL" <c:if test="${ticket.priority == 'NORMAL'}">selected</c:if>>Bình thường</option>
                                        <option value="HIGH" <c:if test="${ticket.priority == 'HIGH'}">selected</c:if>>Cao</option>
                                    </select></label>
                                </div>
                                <label class="support-field"><span>Giao cho Admin ID</span><input type="number" name="assignedAdminId" min="1" value="<c:out value='${ticket.assignedAdminId}'/>" placeholder="Để trống nếu chưa phân công"></label>
                                <label class="support-field"><span>Phản hồi khách hàng hoặc ghi chú</span><textarea name="reply" rows="4" maxlength="5000" placeholder="Nhập nội dung phản hồi gửi khách hàng; để trống nếu chỉ cập nhật trạng thái..."></textarea></label>
                                <label class="support-internal-toggle"><input type="checkbox" name="internal" value="1"> Lưu dạng ghi chú nội bộ (khách hàng sẽ không nhìn thấy nội dung này)</label>
                                <div class="support-update-actions">
                                    <button class="button secondary" type="reset">Làm mới biểu mẫu</button>
                                    <button class="button" type="submit">Lưu cập nhật</button>
                                </div>
                            </form>
                        </c:otherwise>
                    </c:choose>
                </div>
            </section>
        </div>
    </div>
</div>
<script type="module" src="${pageContext.request.contextPath}/admin-ui/js/app.js?v=admin-nav-fix-20261126-r4"></script>
</body>
</html>
