<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>


<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>PrintKraft — In theo yêu cầu</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link href="https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;500;600;700&family=IBM+Plex+Sans:wght@400;500;600&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_home.css">
</head>
<body>

<%@ include file="header.jsp" %>

<main id="app">
    <div class="page active" id="page-home">

        <section class="hero">
            <div>
                <div class="hero-eyebrow">⚡ Giao mẫu thử trong 48 giờ</div>
                <h1>In ấn của bạn, <span class="accent">theo cách của bạn.</span></h1>
                <p>Tải thiết kế lên, chọn chất liệu và số lượng — xưởng in đối tác của chúng tôi lo phần còn lại. Từ 1 chiếc áo đến 10.000 sản phẩm.</p>
                <div class="hero-cta">
                    <a class="btn-primary" href="#studio" onclick="switchTab('studio');return false;">Bắt đầu thiết kế</a>
                    <a class="btn-secondary" href="#san-pham" onclick="switchTab('san-pham');return false;">Xem sản phẩm mẫu</a>
                </div>
                <div class="hero-stats">
                    <div><strong>4.9/5</strong><span>Đánh giá khách hàng</span></div>
                    <div><strong>120+</strong><span>Xưởng in đối tác</span></div>
                    <div><strong>48h</strong><span>Thời gian sản xuất TB</span></div>
                    <div><strong>63</strong><span>Tỉnh thành giao hàng</span></div>
                </div>
            </div>
            <div class="hero-visual">
                <div class="blob b1"></div><div class="blob b2"></div><div class="blob b3"></div>
                <div class="mockup m3">POSTER<br>A2 GLOSS</div>
                <div class="mockup m1">ÁO THUN<br>COTTON 100%</div>
                <div class="mockup m2">CỐC SỨ<br>350ML</div>
                <div class="mockup m4">STICKER<br>DECAL</div>
            </div>
        </section>

        
        <div class="cat-strip">
            <div class="inner">
                <a onclick="switchTab('san-pham')">Áo thun &amp; Hoodie</a>
                <a onclick="switchTab('san-pham')">Cốc &amp; Bình giữ nhiệt</a>
                <a onclick="switchTab('san-pham')">Poster &amp; Canvas</a>
                <a onclick="switchTab('san-pham')">Bao bì &amp; Nhãn dán</a>
                <a onclick="switchTab('san-pham')">Namecard &amp; Văn phòng phẩm</a>
                <a onclick="switchTab('doanh-nghiep')">Quà tặng doanh nghiệp</a>
                <a onclick="switchTab('san-pham')">Túi vải &amp; Phụ kiện</a>
                <a onclick="switchTab('san-pham')">Đồng phục team</a>
            </div>
        </div>

        
        <section class="section">
            <div class="section-head">
                <div>
                    <div class="eyebrow-line">Khám phá</div>
                    <h2>Sản phẩm bán chạy nhất</h2>
                    <p>Những sản phẩm được khách hàng yêu thích và đặt in nhiều nhất trong tháng.</p>
                </div>
                <a class="link" href="#san-pham">Xem tất cả →</a>
            </div>
            <div class="grid">
                <div class="card">
                    <div class="card-thumb" style="background:var(--grad-warm);"><span class="tag">Bán chạy</span><span class="fav">♡</span>Cốc sứ trắng</div>
                    <div class="card-body">
                        <h4>Cốc sứ trắng 2 mặt</h4>
                        <div class="meta">In Sublimation · Từ 1 chiếc</div>
                        <div class="stars">★★★★★ <span style="color:var(--ink-soft)">(412)</span></div>
                        <div class="price-row" style="margin-top:8px;"><span class="price">65.000đ<small>Đã gồm VAT</small></span><button class="add-btn">+</button></div>
                    </div>
                </div>
                <div class="card">
                    <div class="card-thumb" style="background:var(--grad-primary);"><span class="tag">Bán chạy</span><span class="fav">♡</span>Áo thun Oversize</div>
                    <div class="card-body">
                        <h4>Áo thun Oversize Cotton</h4>
                        <div class="meta">In DTG · Từ 1 chiếc</div>
                        <div class="stars">★★★★★ <span style="color:var(--ink-soft)">(218)</span></div>
                        <div class="price-row" style="margin-top:8px;"><span class="price">129.000đ<small>Đã gồm VAT</small></span><button class="add-btn">+</button></div>
                    </div>
                </div>
                <div class="card">
                    <div class="card-thumb" style="background:linear-gradient(135deg,#1B1B2F,#4A4A6A);"><span class="fav">♡</span>Túi vải canvas</div>
                    <div class="card-body">
                        <h4>Túi vải canvas dày</h4>
                        <div class="meta">In lụa · Từ 50 chiếc</div>
                        <div class="stars">★★★★★ <span style="color:var(--ink-soft)">(174)</span></div>
                        <div class="price-row" style="margin-top:8px;"><span class="price">42.000đ<small>Đã gồm VAT</small></span><button class="add-btn">+</button></div>
                    </div>
                </div>
                <div class="card">
                    <div class="card-thumb" style="background:linear-gradient(135deg,#0B7285,#22B8CF);"><span class="fav">♡</span>Sticker cuộn</div>
                    <div class="card-body">
                        <h4>Sticker decal cuộn</h4>
                        <div class="meta">In kỹ thuật số · Từ 100 tem</div>
                        <div class="stars">★★★★★ <span style="color:var(--ink-soft)">(287)</span></div>
                        <div class="price-row" style="margin-top:8px;"><span class="price">3.500đ<small>/tem · Đã gồm VAT</small></span><button class="add-btn">+</button></div>
                    </div>
                </div>
            </div>
        </section>

        
        <section class="section" style="padding-top: 20px;">
            <div class="section-head">
                <div>
                    <div class="eyebrow-line">Xu hướng</div>
                    <h2>Sản phẩm được xem nhiều nhất</h2>
                    <p>Các sản phẩm đang thu hút sự quan tâm từ cộng đồng thiết kế và doanh nghiệp.</p>
                </div>
                <a class="link" href="#san-pham">Xem tất cả →</a>
            </div>
            <div class="grid">
                <div class="card">
                    <div class="card-thumb" style="background:var(--grad-sky);"><span class="fav">♡</span>Hoodie Basic</div>
                    <div class="card-body">
                        <h4>Hoodie Basic Nỉ Bông</h4>
                        <div class="meta">In DTF · Từ 10 chiếc</div>
                        <div class="stars">★★★★☆ <span style="color:var(--ink-soft)">(142)</span></div>
                        <div class="price-row" style="margin-top:8px;"><span class="price">289.000đ<small>Đã gồm VAT</small></span><button class="add-btn">+</button></div>
                    </div>
                </div>
                <div class="card">
                    <div class="card-thumb" style="background:var(--grad-pink);"><span class="fav">♡</span>Bình giữ nhiệt</div>
                    <div class="card-body">
                        <h4>Bình giữ nhiệt in logo</h4>
                        <div class="meta">Khắc laser · Từ 20 chiếc</div>
                        <div class="stars">★★★★☆ <span style="color:var(--ink-soft)">(98)</span></div>
                        <div class="price-row" style="margin-top:8px;"><span class="price">215.000đ<small>Đã gồm VAT</small></span><button class="add-btn">+</button></div>
                    </div>
                </div>
                <div class="card">
                    <div class="card-thumb" style="background:var(--grad-cool); color:#063a2e;"><span class="fav">♡</span>Poster A2</div>
                    <div class="card-body">
                        <h4>Poster A2 giấy mỹ thuật</h4>
                        <div class="meta">In UV · Từ 1 tờ</div>
                        <div class="stars">★★★★☆ <span style="color:var(--ink-soft)">(96)</span></div>
                        <div class="price-row" style="margin-top:8px;"><span class="price">89.000đ<small>Đã gồm VAT</small></span><button class="add-btn">+</button></div>
                    </div>
                </div>
                <div class="card">
                    <div class="card-thumb" style="background:linear-gradient(135deg,#8B5E3C,#C08552);"><span class="fav">♡</span>Namecard</div>
                    <div class="card-body">
                        <h4>Namecard giấy mỹ thuật 300gsm</h4>
                        <div class="meta">In offset · Từ 100 tấm</div>
                        <div class="stars">★★★★★ <span style="color:var(--ink-soft)">(521)</span></div>
                        <div class="price-row" style="margin-top:8px;"><span class="price">1.200đ<small>/tấm · Đã gồm VAT</small></span><button class="add-btn">+</button></div>
                    </div>
                </div>
            </div>
        </section>

        
        <section class="how">
            <div class="section">
                <div class="section-head">
                    <div>
                        <div class="eyebrow-line" style="color:var(--violet-soft);">Quy trình</div>
                        <h2>Đặt hàng chỉ với 3 bước</h2>
                        <p>Không cần kinh nghiệm thiết kế — công cụ của chúng tôi lo phần khó</p>
                    </div>
                </div>
                <div class="steps">
                    <div class="step">
                        <div class="num">1</div>
                        <h4>Tải thiết kế lên</h4>
                        <p>Dùng công cụ kéo-thả hoặc tải file có sẵn. Xem trước mockup ngay lập tức.</p>
                    </div>
                    <div class="step">
                        <div class="num">2</div>
                        <h4>Chọn xưởng in phù hợp</h4>
                        <p>Hệ thống gợi ý xưởng in gần bạn nhất, đánh giá cao và đúng thời hạn.</p>
                    </div>
                    <div class="step">
                        <div class="num">3</div>
                        <h4>Theo dõi &amp; nhận hàng</h4>
                        <p>Cập nhật trạng thái theo thời gian thực, từ sản xuất đến giao hàng.</p>
                    </div>
                </div>
            </div>
        </section>




    </div>
</main>
<%@ include file="footer.jsp" %>
</body>
</html>