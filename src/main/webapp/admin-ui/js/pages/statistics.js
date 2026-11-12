const statisticsMarkup = `<div class="stats-page">
            <div class="heading-row">
                <div><div class="eyebrow">BUSINESS INTELLIGENCE</div><h1 class="page-title">Thống kê & phân tích</h1><p class="page-description">Theo dõi doanh thu, đơn hàng, khách hàng, sản phẩm, xưởng in và các hàng đợi vận hành trên cùng một màn hình.</p></div>
                <div class="heading-actions"><button class="button button-secondary" type="button" id="exportCsv">⇩ Xuất CSV</button><button class="button button-primary" type="button" id="printReport">▤ In báo cáo</button></div>
            </div>
            <form class="filter-panel" id="filterForm">
                <div class="filter-top"><span class="section-label">Bộ lọc báo cáo</span><span class="filter-helper">Chọn khoảng thời gian và cách gom nhóm để cập nhật tất cả biểu đồ.</span></div>
                <div class="filters">
                    <div class="field"><label for="fromDate">Từ ngày</label><input id="fromDate" name="from" type="date" value="__MONTH_START__" required></div>
                    <div class="field"><label for="toDate">Đến ngày</label><input id="toDate" name="to" type="date" value="__TODAY__" required></div>
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
            </div>`;

export function renderStatistics() {
    const now = new Date();
    const date = value => value.getFullYear() + "-" + String(value.getMonth() + 1).padStart(2, "0") + "-" + String(value.getDate()).padStart(2, "0");
    const today = date(now);
    const monthStart = date(new Date(now.getFullYear(), now.getMonth(), 1));
    return statisticsMarkup.replace("__MONTH_START__", monthStart).replace("__TODAY__", today);
}

export function initStatistics(contextPathValue = "") {
    const contextPath = contextPathValue;
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

}
