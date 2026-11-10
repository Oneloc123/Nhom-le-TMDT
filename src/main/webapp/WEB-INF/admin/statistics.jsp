<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="true" %>
<%@ page import="java.time.LocalDate" %>
<%
    String contextPath = request.getContextPath();
    LocalDate statsToday = LocalDate.now();
    LocalDate statsMonthStart = statsToday.withDayOfMonth(1);
    String csrfToken = vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request);
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="theme-color" content="#f5f7fc">
    <title>Thống kê & phân tích · PrintKraft Admin</title>
    <style>
        :root{color-scheme:light;--ink:#172033;--muted:#758096;--line:#e6eaf2;--paper:#fff;--page:#f5f7fc;--purple:#6e62d9;--purple-dark:#5548c7;--green:#149b73;--blue:#3978df;--amber:#db8e28;--red:#d95b69;--shadow:0 12px 34px rgba(31,42,76,.06)}
        *{box-sizing:border-box}html{min-height:100%;scroll-behavior:smooth}body{margin:0;background:var(--page);font-family:Inter,ui-sans-serif,system-ui,-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif;color:var(--ink);font-size:14px;line-height:1.45}button,input,select{font:inherit}button,a{-webkit-tap-highlight-color:transparent}a{color:inherit;text-decoration:none}button{cursor:pointer}.statistics-shell{min-height:100vh;display:grid;grid-template-columns:254px minmax(0,1fr)}
        .statistics-sidebar{background:#172238;color:#c9cde0;padding:21px 14px 14px;display:flex;flex-direction:column;gap:0;min-height:100vh;position:sticky;top:0;height:100vh;overflow:auto;border-right:1px solid #ffffff0d}.brand{display:flex;align-items:center;gap:11px;padding:1px 9px 22px;min-width:222px;color:#fff}.brand-symbol{width:37px;height:37px;border-radius:12px;background:linear-gradient(145deg,#9389ff,#5e52cb);display:flex;align-items:center;justify-content:center;gap:3px;box-shadow:0 7px 18px #6255cc55}.brand-symbol i{display:block;width:4px;border-radius:3px;background:#fff}.brand-symbol i:nth-child(1){height:12px}.brand-symbol i:nth-child(2){height:20px}.brand-symbol i:nth-child(3){height:15px}.brand-name{font-weight:800;font-size:18px;letter-spacing:-.6px}.brand-name span{color:#a49bff}.brand-caption{display:block;color:#9097b4;font-size:9px;font-weight:800;letter-spacing:1.5px;margin-top:2px}.side-workspace{margin:0;padding:10px;border:1px solid #ffffff17;background:#ffffff08;border-radius:12px;display:flex;align-items:center;gap:10px;min-width:224px}.workspace-mark{width:33px;height:33px;border-radius:10px;background:#383653;color:#bdb8ff;display:grid;place-items:center;font-size:18px}.side-workspace small{display:block;color:#939bb5;font-size:10px}.side-workspace strong{display:block;color:#fff;font-size:12px;margin-top:2px}.nav-heading{font-size:9px;line-height:1;font-weight:700;letter-spacing:.9px;color:#a2aec1;padding:0 12px 8px;margin-bottom:0;text-transform:uppercase}.side-nav{display:grid;gap:0}.side-link{position:relative;min-height:42px;margin:3px 0;padding:0 11px;border:1px solid transparent;border-radius:10px;display:flex;align-items:center;gap:11px;color:#c4ccda;font-size:12px;font-weight:550;white-space:nowrap;transition:background .16s,color .16s,border-color .16s}.side-link:hover{background:#ffffff0b;color:#fff}.side-link.active{background:#ffffff12;color:#fff;border-color:#ffffff16;box-shadow:inset 3px 0 0 #8b80f1}.side-icon{width:22px;text-align:center;font-size:16px}.side-bottom{margin-top:auto;border-top:1px solid #ffffff16;padding:15px 7px 0;display:flex;align-items:center;gap:10px}.admin-avatar{width:35px;height:35px;display:grid;place-items:center;border-radius:12px;background:linear-gradient(145deg,#e4dfff,#b8b0ff);color:#433b8d;font-weight:800}.admin-id{min-width:0;flex:1}.admin-id strong{display:block;font-size:12px;color:white}.admin-id small{font-size:10px;color:#9ba3bd}.logout-button{width:34px;height:34px;display:grid;place-items:center;background:transparent;border:1px solid #ffffff18;border-radius:9px;color:#c1c7dc}
        .statistics-main{min-width:0}.topbar{height:72px;padding:0 clamp(20px,3vw,44px);display:flex;align-items:center;justify-content:space-between;gap:18px;border-bottom:1px solid #e9edf4;background:#ffffffc9;backdrop-filter:blur(14px)}.crumb{font-size:12px;color:var(--muted)}.crumb b{color:#353d50;font-weight:700}.topbar-right{display:flex;align-items:center;gap:11px}.top-chip{border:1px solid var(--line);background:white;border-radius:999px;padding:7px 11px;color:#667085;font-size:11px;font-weight:700}.top-avatar{width:33px;height:33px;border-radius:11px;background:#e9e7ff;color:#584dcc;display:grid;place-items:center;font-weight:800}.content{max-width:1680px;padding:28px clamp(20px,3vw,44px) 38px;margin:0 auto}.heading-row{display:flex;justify-content:space-between;align-items:flex-end;gap:20px;margin-bottom:23px}.eyebrow{font-size:10px;text-transform:uppercase;letter-spacing:1.8px;color:var(--purple);font-weight:850;margin-bottom:7px}.page-title{font-size:clamp(24px,2.5vw,32px);line-height:1.15;letter-spacing:-1px;margin:0;font-weight:850;color:#182035}.page-description{margin:9px 0 0;color:var(--muted);max-width:650px;line-height:1.65}.heading-actions{display:flex;gap:9px;align-items:center;flex-shrink:0}.button{display:inline-flex;align-items:center;justify-content:center;gap:8px;border:1px solid transparent;border-radius:10px;padding:10px 14px;min-height:40px;font-size:12px;font-weight:800;transition:transform .15s,background .15s}.button:active{transform:translateY(1px)}.button-primary{background:var(--purple);color:white;box-shadow:0 6px 13px #6e62d929}.button-primary:hover{background:var(--purple-dark)}.button-secondary{background:white;border-color:var(--line);color:#4d566b}.button-secondary:hover{background:#f9faff}.button:disabled{opacity:.5;cursor:wait}
        .filter-panel{padding:17px;background:var(--paper);border:1px solid var(--line);border-radius:16px;box-shadow:var(--shadow);margin-bottom:20px}.filter-top{display:flex;justify-content:space-between;align-items:center;gap:12px;margin-bottom:14px}.section-label{font-size:12px;font-weight:850;letter-spacing:.1px}.filter-helper{font-size:11px;color:var(--muted)}.filters{display:grid;grid-template-columns:repeat(5,minmax(120px,1fr)) auto;gap:11px;align-items:end}.field{display:grid;gap:6px;min-width:0}.field label{font-size:10px;font-weight:800;color:#687389}.field input,.field select{width:100%;height:41px;border:1px solid #dfe4ee;border-radius:9px;background:#fff;color:#263149;padding:0 10px;outline:none;min-width:0}.field input:focus,.field select:focus{border-color:#8d85e8;box-shadow:0 0 0 3px #6e62d916}.filter-buttons{display:flex;gap:7px;flex-wrap:wrap;margin-top:13px;padding-top:13px;border-top:1px solid #eff1f6}.preset-button{border:1px solid #e1e5ef;border-radius:999px;background:#fff;color:#667085;padding:7px 11px;font-size:10px;font-weight:800}.preset-button:hover,.preset-button.active{border-color:#bdb7ff;background:#f4f2ff;color:#594ec4}.status-message{display:none;padding:12px 14px;border-radius:11px;font-size:12px;margin:0 0 18px}.status-message.show{display:block}.status-message.loading{background:#eeedff;color:#5a51c3}.status-message.error{background:#fff0f1;color:#a53243;border:1px solid #ffd9de}.status-message.success{background:#eafaf3;color:#157652}
        .kpi-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:13px;margin-bottom:20px}.kpi-card{position:relative;overflow:hidden;background:white;border:1px solid var(--line);border-radius:15px;padding:17px;box-shadow:0 7px 22px #1f2a4c06;min-width:0}.kpi-head{display:flex;justify-content:space-between;align-items:center;gap:10px;color:#697489;font-size:11px;font-weight:750}.kpi-icon{width:35px;height:35px;display:grid;place-items:center;border-radius:11px;font-size:17px;flex-shrink:0}.kpi-purple .kpi-icon{background:#f0eeff;color:#6e62d9}.kpi-green .kpi-icon{background:#e7f8f1;color:#14865f}.kpi-blue .kpi-icon{background:#eaf2ff;color:#3978df}.kpi-amber .kpi-icon{background:#fff4e3;color:#bd7919}.kpi-rose .kpi-icon{background:#fff0f1;color:#d05668}.kpi-cyan .kpi-icon{background:#e7f8fb;color:#13859b}.kpi-value{font-size:clamp(20px,2.1vw,27px);letter-spacing:-.8px;font-weight:850;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;margin:13px 0 8px}.kpi-bottom{display:flex;justify-content:space-between;align-items:center;gap:7px;color:#8992a4;font-size:10px}.kpi-change{font-weight:850}.kpi-change.up{color:#11875e}.kpi-change.down{color:#d05668}.kpi-change.neutral{color:#7a8497}.kpi-decoration{position:absolute;width:90px;height:90px;border-radius:50%;right:-43px;bottom:-54px;border:16px solid #f6f5ff;pointer-events:none}
        .charts-grid{display:grid;grid-template-columns:minmax(0,1.55fr) minmax(290px,1fr);gap:15px;margin-bottom:15px}.charts-grid.equal{grid-template-columns:repeat(2,minmax(0,1fr))}.panel{min-width:0;background:white;border:1px solid var(--line);border-radius:16px;box-shadow:var(--shadow);padding:18px}.panel-head{display:flex;justify-content:space-between;align-items:flex-start;gap:12px;margin-bottom:15px}.panel-title{font-size:13px;font-weight:850;margin:0;color:#212b42}.panel-subtitle{font-size:10px;color:#8992a4;margin-top:5px;line-height:1.5}.panel-tag{background:#f3f4fa;border-radius:8px;padding:6px 9px;color:#737d91;font-size:10px;font-weight:800;white-space:nowrap}.bar-viewport{width:100%;overflow-x:auto;overflow-y:hidden;padding-bottom:5px}.bar-viewport svg{display:block;max-width:none}.chart-empty{min-height:220px;display:grid;place-items:center;text-align:center;color:#8b94a7;font-size:12px;border:1px dashed #e2e6ef;border-radius:12px;padding:18px}.pie-content{display:grid;grid-template-columns:minmax(145px,.9fr) minmax(0,1.1fr);align-items:center;gap:12px;min-height:230px}.pie-graphic{width:100%;max-width:230px;margin:auto;display:block;overflow:visible}.pie-legend{display:grid;gap:10px;align-content:center;max-height:270px;overflow:auto}.legend-item{display:grid;grid-template-columns:10px minmax(0,1fr);gap:8px;align-items:start;font-size:11px;color:#657086}.legend-swatch{width:9px;height:9px;border-radius:3px;margin-top:3px}.legend-main{min-width:0}.legend-name{display:block;overflow-wrap:anywhere;font-weight:700;color:#4a556c}.legend-meta{display:flex;gap:8px;flex-wrap:wrap;align-items:center;color:#8a93a5;font-size:10px;margin-top:3px}.legend-meta strong{color:#4f5970}.table-wrap{overflow-x:auto}table{width:100%;border-collapse:collapse;min-width:400px}th{text-align:left;text-transform:uppercase;letter-spacing:.7px;font-size:9px;color:#8a93a5;padding:11px 9px;border-bottom:1px solid #edf0f5;font-weight:850}td{padding:12px 9px;border-bottom:1px solid #f0f2f7;font-size:11px;color:#657086}tbody tr:last-child td{border-bottom:0}.product-name{font-weight:800;color:#273149;display:block;max-width:310px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.product-rank{display:inline-grid;place-items:center;width:25px;height:25px;background:#f0efff;color:#665bd0;border-radius:8px;font-weight:850}.revenue-cell{font-weight:850;color:#273149;white-space:nowrap}.inline-bar{height:4px;background:#eff0f7;border-radius:999px;overflow:hidden;margin-top:6px;max-width:220px}.inline-bar span{display:block;height:100%;border-radius:999px;background:linear-gradient(90deg,#8277eb,#5c50ca)}.operations-heading{margin:23px 0 13px;display:flex;align-items:flex-end;justify-content:space-between;gap:10px}.operations-heading h2{font-size:16px;margin:0;letter-spacing:-.3px}.operations-heading p{margin:5px 0 0;color:var(--muted);font-size:11px}.operations-grid{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:11px}.operation-card{background:#fff;border:1px solid var(--line);border-radius:13px;padding:14px;display:flex;gap:10px;align-items:flex-start;min-width:0;transition:border-color .15s,transform .15s}.operation-card:hover{border-color:#c7c2fa;transform:translateY(-1px)}.operation-icon{width:33px;height:33px;flex-shrink:0;border-radius:10px;display:grid;place-items:center;background:#f2f1ff;color:#6257cf;font-size:15px}.operation-copy{min-width:0}.operation-label{display:block;color:#727d92;font-size:10px;font-weight:700;line-height:1.45}.operation-count{display:block;margin-top:5px;font-size:21px;font-weight:850;letter-spacing:-.5px;color:#1e2940}.operation-link{display:block;font-size:10px;color:var(--purple);font-weight:800;margin-top:4px}.footnote{margin-top:17px;color:#8b94a5;font-size:10px;line-height:1.65}.skeleton{background:linear-gradient(90deg,#f3f5fa 20%,#e9edf5 38%,#f3f5fa 60%);background-size:400% 100%;animation:shimmer 1.2s ease infinite;border-radius:7px;color:transparent!important}@keyframes shimmer{0%{background-position:100% 0}100%{background-position:0 0}}.page-footer{padding:20px 0 0;margin-top:23px;border-top:1px solid #e7ebf3;display:flex;justify-content:space-between;color:#9aa2b3;font-size:10px}.mobile-nav-toggle{display:none}
        @media(max-width:1300px){.filters{grid-template-columns:repeat(3,minmax(130px,1fr)) auto}.operations-grid{grid-template-columns:repeat(3,minmax(0,1fr))}.kpi-grid{grid-template-columns:repeat(3,minmax(0,1fr))}}
        @media(max-width:960px){.statistics-shell{grid-template-columns:1fr}.statistics-sidebar{height:auto;min-height:0;position:relative;padding:14px 17px;gap:13px}.brand{padding:0 3px}.side-workspace,.nav-heading,.side-bottom{display:none}.side-nav{display:flex;gap:5px;overflow-x:auto;padding-bottom:2px}.side-link{flex:0 0 auto;min-height:36px;padding:8px 10px;font-size:11px}.side-icon{width:auto}.topbar{height:60px}.charts-grid,.charts-grid.equal{grid-template-columns:1fr}.content{padding-top:22px}.heading-row{align-items:flex-start}.statistics-sidebar{overflow:visible}}
        @media(max-width:640px){.topbar{padding:0 16px}.content{padding:20px 13px 28px}.heading-row{display:block}.heading-actions{margin-top:15px}.heading-actions .button{flex:1}.filters{grid-template-columns:repeat(2,minmax(0,1fr))}.filter-apply{grid-column:1/-1}.filter-top{align-items:flex-start;flex-direction:column}.kpi-grid{grid-template-columns:repeat(2,minmax(0,1fr));gap:9px}.kpi-card{padding:13px}.kpi-value{font-size:21px}.kpi-bottom{display:block}.kpi-bottom span{display:block;margin-top:3px}.operations-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.operation-card{padding:11px;gap:8px}.operation-icon{width:29px;height:29px}.operation-count{font-size:19px}.pie-content{grid-template-columns:1fr;gap:8px}.pie-graphic{max-width:180px}.pie-legend{grid-template-columns:repeat(2,minmax(0,1fr));max-height:none}.panel{padding:14px}.panel-head{gap:8px}.panel-tag{display:none}.filter-panel{padding:13px}.filter-helper{line-height:1.5}.crumb{font-size:11px}.top-chip{display:none}.page-footer{gap:10px}}
        @media print{body{background:#fff}.statistics-shell{display:block}.statistics-sidebar,.topbar,.filter-panel,.heading-actions,.page-footer{display:none!important}.content{max-width:none;padding:0}.kpi-card,.panel,.operation-card{box-shadow:none;break-inside:avoid}.charts-grid,.charts-grid.equal{grid-template-columns:1fr 1fr}.bar-viewport{overflow:visible}.bar-viewport svg{max-width:100%;height:auto}}
    .brand-mark{width:36px;height:36px;position:relative;border:1px solid #b4abff66;border-radius:11px;background:linear-gradient(145deg,#6b5de1,#4c41b5);box-shadow:0 6px 15px #0c142666;transform:rotate(-5deg);flex:0 0 36px}.brand-mark i{display:block;position:absolute;width:10px;height:10px;border-radius:3px;background:#fff;transform:rotate(7deg)}.brand-mark i:nth-child(1){left:7px;top:7px;opacity:.97}.brand-mark i:nth-child(2){right:6px;top:7px;opacity:.68}.brand-mark i:nth-child(3){left:12px;bottom:5px;opacity:.83}.brand-copy{display:grid;line-height:1.12}.brand-copy strong{font:700 19px/1 "Space Grotesk","Segoe UI",sans-serif;letter-spacing:-.8px}.brand-copy strong span{color:#53c5ca}.brand-copy small{margin-top:6px;color:#aeb9cb;font-size:8px;font-weight:700;letter-spacing:1.1px}
    </style>
</head>
<body>
<div class="statistics-shell">
    <aside class="statistics-sidebar">
        <a class="brand" href="<%= contextPath %>/admin" aria-label="PrintKraft Admin">
            <span class="brand-mark" aria-hidden="true"><i></i><i></i><i></i></span>
            <span class="brand-copy"><strong>PrintKraft<span>.</span></strong><small>MARKETPLACE ADMIN</small></span>
        </a>
        <div class="side-workspace"><span class="workspace-mark">▦</span><span><small>Không gian quản trị</small><strong>Marketplace</strong></span></div>
        <nav aria-label="Điều hướng chính">
            <div class="nav-heading">QUẢN LÝ MARKETPLACE</div>
            <div class="side-nav">
                <a class="side-link" href="<%= contextPath %>/admin"><span class="side-icon">▦</span>Dashboard</a>
                <a class="side-link" href="<%= contextPath %>/admin/users"><span class="side-icon">♙</span>Người dùng</a>
                <a class="side-link" href="<%= contextPath %>/admin/sellers"><span class="side-icon">⌂</span>Seller / Xưởng in</a>
                <a class="side-link" href="<%= contextPath %>/admin/products"><span class="side-icon">◇</span>Sản phẩm</a>
                <a class="side-link" href="<%= contextPath %>/admin/orders"><span class="side-icon">▣</span>Đơn hàng</a>
                <a class="side-link active" href="<%= contextPath %>/admin/statistics" aria-current="page"><span class="side-icon">▥</span>Thống kê</a>
                <a class="side-link" href="<%= contextPath %>/admin/tickets"><span class="side-icon">☏</span>Hỗ trợ khách hàng</a>
            </div>
        </nav>
        <div class="side-bottom">
            <span class="admin-avatar">A</span>
            <span class="admin-id"><strong>Không gian quản trị</strong><small>Quản trị viên PrintKraft</small></span>
            <form method="post" action="<%= contextPath %>/logout">
                <input type="hidden" name="csrfToken" value="<%= csrfToken %>">
                <button class="logout-button" type="submit" aria-label="Đăng xuất" title="Đăng xuất">↗</button>
            </form>
        </div>
    </aside>
    <div class="statistics-main">
        <header class="topbar">
            <div class="crumb">PrintKraft <span aria-hidden="true">/</span> <b>Thống kê & phân tích</b></div>
            <div class="topbar-right"><span class="top-chip">● Dữ liệu trực tiếp từ hệ thống</span><span class="top-avatar" aria-hidden="true">A</span></div>
        </header>
        <main class="content">
            <div class="heading-row">
                <div><div class="eyebrow">BUSINESS INTELLIGENCE</div><h1 class="page-title">Thống kê & phân tích</h1><p class="page-description">Theo dõi doanh thu, đơn hàng, khách hàng, sản phẩm, xưởng in và các hàng đợi vận hành trên cùng một màn hình.</p></div>
                <div class="heading-actions"><button class="button button-secondary" type="button" id="exportCsv">⇩ Xuất CSV</button><button class="button button-primary" type="button" id="printReport">▤ In báo cáo</button></div>
            </div>
            <form class="filter-panel" id="filterForm">
                <div class="filter-top"><span class="section-label">Bộ lọc báo cáo</span><span class="filter-helper">Chọn khoảng thời gian và cách gom nhóm để cập nhật tất cả biểu đồ.</span></div>
                <div class="filters">
                    <div class="field"><label for="fromDate">Từ ngày</label><input id="fromDate" name="from" type="date" value="<%= statsMonthStart %>" required></div>
                    <div class="field"><label for="toDate">Đến ngày</label><input id="toDate" name="to" type="date" value="<%= statsToday %>" required></div>
                    <div class="field"><label for="groupBy">Gom nhóm theo</label><select id="groupBy" name="groupBy"><option value="day" selected>Theo ngày</option><option value="month">Theo tháng</option><option value="year">Theo năm</option></select></div>
                    <div class="field"><label for="metric">Chỉ số biểu đồ cột</label><select id="metric" name="metric"><option value="revenue">Doanh thu đã thanh toán</option><option value="orders">Số đơn hàng</option><option value="customers">Khách hàng mới</option><option value="products">Sản phẩm mới</option><option value="shops">Xưởng in mới</option><option value="customRequests">Yêu cầu in 3D</option></select></div>
                    <button class="button button-primary filter-apply" id="applyFilters" type="submit">↻ Cập nhật báo cáo</button>
                </div>
                <div class="filter-buttons" aria-label="Khoảng thời gian nhanh">
                    <button type="button" class="preset-button active" data-preset="month">Tháng này</button>
                    <button type="button" class="preset-button" data-preset="7days">7 ngày qua</button>
                    <button type="button" class="preset-button" data-preset="30days">30 ngày qua</button>
                    <button type="button" class="preset-button" data-preset="year">Từ đầu năm</button>
                </div>
            </form>
            <div id="statusMessage" class="status-message" role="status" aria-live="polite"></div>
            <section class="kpi-grid" id="kpiGrid" aria-label="Chỉ số tổng quan"><article class="kpi-card"><div class="skeleton">Đang tải dữ liệu</div></article></section>
            <section class="charts-grid">
                <article class="panel">
                    <div class="panel-head"><div><h2 class="panel-title" id="trendTitle">Xu hướng doanh thu</h2><p class="panel-subtitle" id="trendSubtitle">So sánh theo ngày trong khoảng thời gian đã chọn.</p></div><span class="panel-tag" id="rangeTag">Tháng này</span></div>
                    <div id="trendChart" class="chart-empty">Đang tải biểu đồ…</div>
                </article>
                <article class="panel">
                    <div class="panel-head"><div><h2 class="panel-title">Cơ cấu trạng thái đơn hàng</h2><p class="panel-subtitle">Tỷ trọng đơn hàng theo trạng thái trong kỳ.</p></div><span class="panel-tag">Đơn hàng</span></div>
                    <div id="orderStatusChart" class="chart-empty">Đang tải biểu đồ…</div>
                </article>
            </section>
            <section class="charts-grid equal">
                <article class="panel">
                    <div class="panel-head"><div><h2 class="panel-title">Phương thức thanh toán</h2><p class="panel-subtitle">Phân bổ giao dịch theo phương thức ghi nhận trong kỳ.</p></div><span class="panel-tag">Thanh toán</span></div>
                    <div id="paymentChart" class="chart-empty">Đang tải biểu đồ…</div>
                </article>
                <article class="panel">
                    <div class="panel-head"><div><h2 class="panel-title">Doanh số theo danh mục</h2><p class="panel-subtitle">Phân bổ số lượng sản phẩm bán ra theo danh mục.</p></div><span class="panel-tag">Sản phẩm</span></div>
                    <div id="categoryChart" class="chart-empty">Đang tải biểu đồ…</div>
                </article>
            </section>
            <section class="panel">
                <div class="panel-head"><div><h2 class="panel-title">Sản phẩm nổi bật trong kỳ</h2><p class="panel-subtitle">Xếp theo doanh thu dòng hàng trong các đơn hàng đã tạo trong khoảng thời gian đã chọn.</p></div><span class="panel-tag">Top sản phẩm</span></div>
                <div class="table-wrap" id="topProductsTable"><div class="chart-empty">Đang tải danh sách sản phẩm…</div></div>
            </section>
            <div class="operations-heading"><div><h2>Hàng đợi vận hành</h2><p>Những nghiệp vụ cần theo dõi hoặc xử lý của quản trị viên.</p></div><span class="panel-tag">Trạng thái hiện tại</span></div>
            <section class="operations-grid" id="operationsGrid"><div class="operation-card"><div class="skeleton">Đang tải hàng đợi</div></div></section>
            <p class="footnote">Doanh thu trong báo cáo được tính từ đơn hàng có trạng thái thanh toán PAID, SUCCESS hoặc COMPLETED (hoặc có giao dịch thanh toán tương ứng đã thành công). Các ô hàng đợi vận hành phản ánh số lượng đang chờ xử lý hiện tại, không bị giới hạn bởi khoảng ngày đã chọn. Số liệu hiển thị phụ thuộc dữ liệu có trong cơ sở dữ liệu.</p>
            <footer class="page-footer"><span>PrintKraft · Marketplace Admin</span><span>Thống kê & phân tích</span></footer>
        </main>
    </div>
</div>
<script>
(() => {
    const contextPath = "<%= contextPath %>";
    const form = document.getElementById('filterForm');
    const fromInput = document.getElementById('fromDate');
    const toInput = document.getElementById('toDate');
    const groupInput = document.getElementById('groupBy');
    const metricInput = document.getElementById('metric');
    const statusBox = document.getElementById('statusMessage');
    const colors = ['#7063dc','#3b8bdb','#1ea781','#eda947','#e56d7e','#27a6b8','#8a6ccf','#8e9bb1'];
    let latestData = null;
    let latestSeries = [];
    const metricInfo = {
        revenue: { title: 'Xu hướng doanh thu đã thanh toán', noun: 'Doanh thu', type: 'money' },
        orders: { title: 'Xu hướng số lượng đơn hàng', noun: 'Đơn hàng', type: 'count' },
        customers: { title: 'Xu hướng khách hàng mới', noun: 'Khách hàng mới', type: 'count' },
        products: { title: 'Xu hướng sản phẩm mới', noun: 'Sản phẩm mới', type: 'count' },
        shops: { title: 'Xu hướng xưởng in mới', noun: 'Xưởng in mới', type: 'count' },
        customRequests: { title: 'Xu hướng yêu cầu in 3D', noun: 'Yêu cầu in 3D', type: 'count' }
    };
    const queueInfo = [
        ['pendingOrders','Đơn hàng cần xử lý','▣','/admin/orders'],
        ['pendingShipments','Lô giao hàng chưa hoàn tất','↗','/admin/orders'],
        ['openSupportTickets','Ticket hỗ trợ đang mở','☏','/admin/tickets'],
        ['pendingCustomRequests','Yêu cầu in 3D chờ báo giá','◇','/admin/orders?view=quotes'],
        ['pendingQuotes','Báo giá B2B cần xử lý','▤','/admin/orders?view=quotes'],
        ['pendingProducts','Sản phẩm chờ duyệt','◈','/admin/products?status=PENDING_REVIEW'],
        ['pendingSellerVerifications','Xưởng chờ xác minh','⌂','/admin/sellers?verification=PENDING'],
        ['pendingChangeRequests','Yêu cầu đổi/hủy đơn','↺','/admin/orders'],
        ['refundRequired','Giao dịch cần hoàn tiền','₫','/admin/orders'],
        ['unpublishedReviews','Đánh giá chờ kiểm duyệt','☆','/admin/products']
    ];
    const numberFormat = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 });
    const moneyFormat = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 });
    function escapeHtml(value) { return String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c])); }
    function num(value) { const n = Number(value ?? 0); return Number.isFinite(n) ? n : 0; }
    function numberLabel(value) { return numberFormat.format(num(value)); }
    function moneyLabel(value) { return moneyFormat.format(num(value)); }
    function compactLabel(value) { const n = num(value); if (n >= 1000000000) return (n / 1000000000).toLocaleString('vi-VN',{maximumFractionDigits:1}) + ' tỷ'; if (n >= 1000000) return (n / 1000000).toLocaleString('vi-VN',{maximumFractionDigits:1}) + ' tr'; if (n >= 1000) return (n / 1000).toLocaleString('vi-VN',{maximumFractionDigits:1}) + ' k'; return numberLabel(n); }
    function showStatus(message, type) { statusBox.textContent = message || ''; statusBox.className = 'status-message' + (message ? ' show' : '') + (type ? ' ' + type : ''); }
    function parseDate(value) { const parts = value.split('-').map(Number); return new Date(parts[0], parts[1] - 1, parts[2]); }
    function dateString(date) { return date.getFullYear() + '-' + String(date.getMonth()+1).padStart(2,'0') + '-' + String(date.getDate()).padStart(2,'0'); }
    function periodKey(date, group) { if (group === 'year') return String(date.getFullYear()); if (group === 'month') return date.getFullYear() + '-' + String(date.getMonth()+1).padStart(2,'0'); return dateString(date); }
    function periodLabel(key, group) { if (group === 'year') return key; if (group === 'month') { const [y,m] = key.split('-'); return m + '/' + y; } const [y,m,d] = key.split('-'); return d + '/' + m; }
    function buildBuckets(fromText, toText, group, values) {
        const start = parseDate(fromText); const finish = parseDate(toText); const map = new Map((values || []).map(row => [String(row.period), num(row.value)]));
        const list = []; const cur = new Date(start.getFullYear(), start.getMonth(), start.getDate());
        if (group === 'month') cur.setDate(1); if (group === 'year') { cur.setMonth(0,1); }
        while (cur <= finish && list.length < 1500) {
            const key = periodKey(cur, group); list.push({ key, label: periodLabel(key, group), value: map.get(key) || 0 });
            if (group === 'year') cur.setFullYear(cur.getFullYear()+1,0,1); else if (group === 'month') cur.setMonth(cur.getMonth()+1,1); else cur.setDate(cur.getDate()+1);
        }
        return list;
    }
    function changeText(current, previous) {
        const c = num(current), p = num(previous); if (p === 0 && c === 0) return {text:'Không đổi', tone:'neutral'}; if (p === 0) return {text:'Mới trong kỳ', tone:'up'};
        const delta = (c-p)/Math.abs(p)*100; return {text:(delta > 0 ? '+' : '') + delta.toLocaleString('vi-VN',{maximumFractionDigits:1}) + '% so với kỳ trước', tone:delta > 0 ? 'up' : delta < 0 ? 'down' : 'neutral'};
    }
    function card(title, value, sub, icon, tone, change) {
        return `<article class="kpi-card kpi-${tone}"><div class="kpi-head"><span>${escapeHtml(title)}</span><span class="kpi-icon">${icon}</span></div><div class="kpi-value" title="${escapeHtml(value)}">${escapeHtml(value)}</div><div class="kpi-bottom"><span class="kpi-change ${change?.tone || 'neutral'}">${escapeHtml(change?.text || '')}</span><span>${escapeHtml(sub)}</span></div><i class="kpi-decoration" aria-hidden="true"></i></article>`;
    }
    function renderKpis(summary, previous, data) {
        const orderChange = changeText(summary.orderCount, previous.orderCount);
        const revenueChange = changeText(summary.revenue, previous.revenue);
        const customerChange = changeText(summary.newCustomers, previous.newCustomers);
        const paidRate = num(summary.orderCount) ? num(summary.paidOrderCount) / num(summary.orderCount) * 100 : 0;
        const cancelRate = num(summary.orderCount) ? num(summary.cancelledOrderCount) / num(summary.orderCount) * 100 : 0;
        const cards = [
            card('Doanh thu đã thanh toán', moneyLabel(summary.revenue), 'So với kỳ liền trước', '₫', 'purple', revenueChange),
            card('Tổng đơn hàng', numberLabel(summary.orderCount), 'Đơn phát sinh trong kỳ', '▣', 'blue', orderChange),
            card('Giá trị đơn trung bình', moneyLabel(summary.averageOrderValue), 'Trên đơn đã thanh toán', '↗', 'green', {text:numberLabel(summary.paidOrderCount)+' đơn đã thanh toán',tone:'neutral'}),
            card('Khách hàng mới', numberLabel(summary.newCustomers), 'Tài khoản khách trong kỳ', '♙', 'cyan', customerChange),
            card('Sản phẩm mới', numberLabel(summary.newProducts), 'Sản phẩm tạo trong kỳ', '◇', 'amber', {text:numberLabel(summary.newShops)+' xưởng in mới',tone:'neutral'}),
            card('Tỷ lệ thanh toán', paidRate.toLocaleString('vi-VN',{maximumFractionDigits:1})+'%', 'Tỷ lệ trên đơn phát sinh', '✓', 'rose', {text:'Tỷ lệ hủy '+cancelRate.toLocaleString('vi-VN',{maximumFractionDigits:1})+'%',tone:'neutral'})
        ];
        document.getElementById('kpiGrid').innerHTML = cards.join('');
    }
    function renderBarChart(rows, group) {
        const holder = document.getElementById('trendChart');
        if (!rows.length) { holder.className='chart-empty'; holder.textContent='Không có dữ liệu trong khoảng thời gian này.'; return; }
        holder.className='bar-viewport';
        const maxValue = Math.max(1, ...rows.map(row => row.value));
        const gap = rows.length > 70 ? 7 : rows.length > 35 ? 10 : rows.length > 18 ? 14 : 21;
        const barWidth = Math.max(5, Math.min(25, gap * .58));
        const left=60, right=15, top=18, bottom=53, height=290;
        const plotHeight=height-top-bottom, plotWidth=rows.length*gap;
        const width=left+plotWidth+right;
        const axes=[];
        for(let i=0;i<=4;i++){ const y=top+plotHeight*i/4; const v=maxValue*(1-i/4); axes.push(`<line x1="${left}" y1="${y}" x2="${left+plotWidth}" y2="${y}" stroke="#e9edf5" stroke-dasharray="3 4"/><text x="${left-10}" y="${y+4}" text-anchor="end" font-size="10" fill="#8993a7">${escapeHtml(metricInput.value==='revenue'?compactLabel(v):numberLabel(Math.round(v)))}</text>`); }
        const every=Math.max(1,Math.ceil(rows.length/14));
        const bars=rows.map((row,index)=>{const x=left+index*gap+(gap-barWidth)/2;const bh=num(row.value)/maxValue*plotHeight;const y=top+plotHeight-bh;const show=index%every===0||index===rows.length-1;return `<g><title>${escapeHtml(row.label)}: ${escapeHtml(metricInput.value==='revenue'?moneyLabel(row.value):numberLabel(row.value))}</title><rect x="${x}" y="${y}" width="${barWidth}" height="${Math.max(0,bh)}" rx="${Math.min(4,barWidth/3)}" fill="#7063dc" opacity="${num(row.value)===0?'.18':'.92'}"/><rect x="${x}" y="${Math.max(top,y)}" width="${barWidth}" height="${Math.min(5,bh)}" rx="2" fill="#a69eff"/>${show?`<text x="${x+barWidth/2}" y="${height-28}" text-anchor="middle" font-size="10" fill="#7d879b">${escapeHtml(row.label)}</text>`:''}</g>`;}).join('');
        holder.innerHTML=`<svg role="img" aria-label="Biểu đồ cột ${escapeHtml(metricInfo[metricInput.value].noun)}" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}">${axes.join('')}${bars}<text x="${left+plotWidth/2}" y="${height-7}" text-anchor="middle" font-size="10" fill="#a0a7b5">${group==='day'?'Ngày':group==='month'?'Tháng':'Năm'}</text></svg>`;
    }
    function sectorPath(cx,cy,r,start,end) {
        if (end-start >= 359.99) return `M ${cx} ${cy-r} A ${r} ${r} 0 1 1 ${cx} ${cy+r} A ${r} ${r} 0 1 1 ${cx} ${cy-r} Z`;
        const rad=Math.PI/180; const x1=cx+r*Math.cos(start*rad), y1=cy+r*Math.sin(start*rad); const x2=cx+r*Math.cos(end*rad), y2=cy+r*Math.sin(end*rad); const large=end-start>180?1:0;
        return `M ${cx} ${cy} L ${x1.toFixed(2)} ${y1.toFixed(2)} A ${r} ${r} 0 ${large} 1 ${x2.toFixed(2)} ${y2.toFixed(2)} Z`;
    }
    function renderPie(holderId, items, valueKey='value', metaKey=null, metaType='count') {
        const holder=document.getElementById(holderId); const clean=(items||[]).map(item=>({label:String(item.label||'Chưa phân loại'),value:Math.max(0,num(item[valueKey])),meta:metaKey?num(item[metaKey]):null})).filter(item=>item.value>0);
        if(!clean.length){holder.className='chart-empty';holder.textContent='Chưa có dữ liệu để phân bổ trong kỳ.';return;}
        holder.className='pie-content';const total=clean.reduce((sum,item)=>sum+item.value,0);let offset=-90;
        const paths=clean.map((item,index)=>{const angle=item.value/total*360;const path=sectorPath(100,100,86,offset,offset+angle);const svg=`<path d="${path}" fill="${colors[index%colors.length]}" stroke="#fff" stroke-width="2"><title>${escapeHtml(item.label)}: ${numberLabel(item.value)}</title></path>`;offset+=angle;return svg;}).join('');
        const legend=clean.map((item,index)=>{const share=item.value/total*100;let extra=numberLabel(item.value)+' ('+share.toLocaleString('vi-VN',{maximumFractionDigits:1})+'%)';if(item.meta!==null)extra+= ' · '+(metaType==='money'?moneyLabel(item.meta):numberLabel(item.meta));return `<div class="legend-item"><span class="legend-swatch" style="background:${colors[index%colors.length]}"></span><span class="legend-main"><span class="legend-name">${escapeHtml(item.label)}</span><span class="legend-meta"><strong>${escapeHtml(extra)}</strong></span></span></div>`;}).join('');
        holder.innerHTML=`<svg class="pie-graphic" viewBox="0 0 200 200" role="img" aria-label="Biểu đồ tròn phân bổ">${paths}</svg><div class="pie-legend">${legend}</div>`;
    }
    function renderProducts(items) {
        const host=document.getElementById('topProductsTable');
        if(!items||!items.length){host.innerHTML='<div class="chart-empty">Chưa có sản phẩm phát sinh đơn hàng trong khoảng thời gian này.</div>';return;}
        const max=Math.max(1,...items.map(item=>num(item.revenue)));
        host.innerHTML=`<table><thead><tr><th>#</th><th>Sản phẩm</th><th>Số lượng</th><th>Doanh thu dòng hàng</th></tr></thead><tbody>${items.map((item,index)=>`<tr><td><span class="product-rank">${index+1}</span></td><td><span class="product-name" title="${escapeHtml(item.name)}">${escapeHtml(item.name||'Sản phẩm')}</span><div class="inline-bar"><span style="width:${Math.max(0,Math.min(100,num(item.revenue)/max*100))}%"></span></div></td><td>${numberLabel(item.quantity)}</td><td class="revenue-cell">${moneyLabel(item.revenue)}</td></tr>`).join('')}</tbody></table>`;
    }
    function renderOperations(operations) {
        document.getElementById('operationsGrid').innerHTML=queueInfo.map(([key,label,icon,href])=>`<a class="operation-card" href="${contextPath}${href}"><span class="operation-icon">${icon}</span><span class="operation-copy"><span class="operation-label">${escapeHtml(label)}</span><strong class="operation-count">${numberLabel(operations[key])}</strong><span class="operation-link">Mở nghiệp vụ →</span></span></a>`).join('');
    }
    function renderAll(data) {
        latestData=data;
        const metric=metricInfo[metricInput.value]||metricInfo.revenue;
        const group=groupInput.value;
        const from=fromInput.value,to=toInput.value;
        const summary=data.summary||{}, previous=data.previous||{};
        renderKpis(summary,previous,data);
        const trendMap=data.trends?.[metricInput.value]||[];
        const trend=buildBuckets(from,to,group,trendMap);
        latestSeries=trend;
        document.getElementById('trendTitle').textContent=metric.title;
        document.getElementById('trendSubtitle').textContent=(group==='day'?'Theo ngày':group==='month'?'Theo tháng':'Theo năm')+' · '+from+' đến '+to;
        document.getElementById('rangeTag').textContent=group==='day'?'Theo ngày':group==='month'?'Theo tháng':'Theo năm';
        renderBarChart(trend,group);
        renderPie('orderStatusChart',data.orderStatuses||[],'value');
        renderPie('paymentChart',data.paymentMethods||[],'value','amount','money');
        renderPie('categoryChart',data.categorySales||[],'value','amount','money');
        renderProducts(data.topProducts||[]);
        renderOperations(data.operations||{});
        showStatus('Đã cập nhật báo cáo: '+from+' → '+to+'.','success');
        document.querySelectorAll('[data-preset]').forEach(button=>button.classList.remove('active'));
    }
    async function loadData() {
        if(!fromInput.value||!toInput.value){showStatus('Vui lòng chọn ngày bắt đầu và ngày kết thúc.','error');return;}
        if(fromInput.value>toInput.value){showStatus('Ngày bắt đầu không được sau ngày kết thúc.','error');return;}
        const apply=document.getElementById('applyFilters');apply.disabled=true;showStatus('Đang lấy số liệu mới nhất từ cơ sở dữ liệu…','loading');
        try {
            const params=new URLSearchParams({from:fromInput.value,to:toInput.value,groupBy:groupInput.value});
            const response=await fetch(`${contextPath}/admin/api/statistics?${params.toString()}`,{credentials:'same-origin',headers:{'Accept':'application/json'}});
            let payload;try{payload=await response.json();}catch{throw new Error('Máy chủ trả về dữ liệu không hợp lệ.');}
            if(!response.ok)throw new Error(payload.error||'Không thể tải báo cáo.');
            renderAll(payload.data||{});
        } catch(error) { showStatus(error.message||'Không thể tải số liệu. Hãy kiểm tra kết nối cơ sở dữ liệu và thử lại.','error'); }
        finally { apply.disabled=false; }
    }
    function applyPreset(preset) {
        const today=parseDate(toInput.value||dateString(new Date()));let start=new Date(today);let group='day';
        if(preset==='month'){start=new Date(today.getFullYear(),today.getMonth(),1);group='day';}
        else if(preset==='7days'){start.setDate(start.getDate()-6);group='day';}
        else if(preset==='30days'){start.setDate(start.getDate()-29);group='day';}
        else if(preset==='year'){start=new Date(today.getFullYear(),0,1);group='month';}
        fromInput.value=dateString(start);toInput.value=dateString(today);groupInput.value=group;
        document.querySelectorAll('[data-preset]').forEach(button=>button.classList.toggle('active',button.dataset.preset===preset));
        loadData();
    }
    function csvCell(value) { let text=String(value??''); if(/^[=+\-@\t\r]/.test(text))text="'"+text;return '"'+text.replace(/"/g,'""')+'"'; }
    function exportCsv() {
        if(!latestData)return;const s=latestData.summary||{},ops=latestData.operations||{};const records=[
            ['Báo cáo thống kê PrintKraft'],['Từ ngày',fromInput.value],['Đến ngày',toInput.value],['Gom nhóm',groupInput.options[groupInput.selectedIndex].text],[],
            ['Chỉ số','Giá trị'],['Doanh thu đã thanh toán',s.revenue||0],['Đơn hàng',s.orderCount||0],['Đơn đã thanh toán',s.paidOrderCount||0],['Giá trị đơn trung bình',s.averageOrderValue||0],['Khách hàng mới',s.newCustomers||0],['Sản phẩm mới',s.newProducts||0],['Xưởng in mới',s.newShops||0],[],
            ['Xu hướng',metricInfo[metricInput.value].noun,'Giá trị'],...latestSeries.map(item=>[item.label,metricInfo[metricInput.value].noun,item.value]),[],
            ['Hàng đợi vận hành','Số lượng'],...queueInfo.map(([key,label])=>[label,ops[key]||0]),[],
            ['Sản phẩm nổi bật','Số lượng','Doanh thu'],...(latestData.topProducts||[]).map(item=>[item.name,item.quantity,item.revenue])
        ];const csv='\uFEFF'+records.map(row=>row.map(csvCell).join(',')).join('\r\n');const blob=new Blob([csv],{type:'text/csv;charset=utf-8;'});const url=URL.createObjectURL(blob);const a=document.createElement('a');a.href=url;a.download=`printkraft-thong-ke-${fromInput.value}-${toInput.value}.csv`;document.body.appendChild(a);a.click();a.remove();setTimeout(()=>URL.revokeObjectURL(url),0);
    }
    form.addEventListener('submit',event=>{event.preventDefault();loadData();});
    document.querySelectorAll('[data-preset]').forEach(button=>button.addEventListener('click',()=>applyPreset(button.dataset.preset)));
    metricInput.addEventListener('change',()=>{if(latestData)renderAll(latestData);});
    document.getElementById('exportCsv').addEventListener('click',exportCsv);
    document.getElementById('printReport').addEventListener('click',()=>window.print());
    loadData();
})();
</script>
</body>
</html>
