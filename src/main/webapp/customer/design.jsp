<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

    <!DOCTYPE html>
    <html lang="vi">

    <head>
        <meta charset="UTF-8">
        <title>PrintKraft — Xưởng In 3D Trực Tuyến</title>
        <meta name="description"
            content="Upload an OBJ model, configure printing, and select a workshop for an estimate or quote.">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <link rel="preconnect" href="https://fonts.googleapis.com">
        <link
            href="https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;500;600;700&family=IBM+Plex+Sans:wght@400;500;600;700&display=swap"
            rel="stylesheet">
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_design.css?v=restore-3d-20261104">

        <script src="https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js"></script>
        <script src="https://cdn.jsdelivr.net/npm/three@0.128.0/examples/js/controls/OrbitControls.js"></script>
        <script src="https://cdn.jsdelivr.net/npm/three@0.128.0/examples/js/loaders/OBJLoader.js"></script>
    </head>

    <body>

        <%@ include file="header.jsp" %>

            <input type="hidden" id="csrf-token" value="${csrfToken}">
            <div class="toast-container" id="toast-container"></div>

            <main class="configurator-container">
                <div class="config-header">
                    <div class="config-title">
                        <span class="eyebrow">IN 3D THEO YÊU CẦU</span>
                        <h1>Xưởng In 3D Trực Tuyến</h1>
                        <p>Tải lên file 3D, chọn công nghệ in & vật liệu, và nhận báo giá tự động ngay lập tức</p>
                    </div>
                    <div class="config-header-actions">
                        <button class="btn-templates" id="btn-open-templates">
                            <i class="fas fa-magic"></i> Mẫu có sẵn
                        </button>
                        <a href="#" class="open-full"><i class="fas fa-expand"></i> Mở toàn màn hình</a>
                    </div>
                </div>

                <div class="config-body">

                    <div class="upload-panel">
                        
                        <div class="drop-zone" id="drop-zone">
                            <div class="drop-zone-icon">
                                <i class="fas fa-cloud-upload-alt"></i>
                            </div>
                            <div class="drop-zone-text">
                                <h3>Kéo & thả file 3D vào đây</h3>
                                <p>hoặc <span class="browse-link" id="browse-link">chọn từ máy tính</span></p>
                                <div class="file-types">
                                    <span class="file-type-badge">.OBJ</span>
                                </div>
                                <p style="margin-top:8px; font-size:11px; color:var(--ink-soft);">Tối đa 100MB mỗi file
                                </p>
                            </div>
                            <input type="file" class="drop-zone-input" id="file-input" accept=".obj">
                        </div>

                        <div class="model-unit-control">
                            <label for="model-unit">Đơn vị tọa độ đang dùng cho OBJ</label>
                            <select id="model-unit"><option value="mm" selected>Milimét (mm)</option><option value="cm">Centimét (cm)</option><option value="in">Inch (in)</option></select>
                            <small>Mặc định là mm. Nếu kích thước bất thường, hãy xác nhận sau khi hệ thống phân tích.</small>
                        </div>
                        <div class="model-analysis-message" id="model-analysis-message" aria-live="polite"></div>
                        <label id="dimensions-confirm-row" class="dimensions-confirm-row" hidden><input type="checkbox" id="dimensions-confirmed"> Tôi đã kiểm tra đơn vị và xác nhận kích thước mô hình là đúng.</label>

                        <div class="upload-progress" id="upload-progress">
                            <div class="progress-bar-track">
                                <div class="progress-bar-fill" id="progress-fill"></div>
                            </div>
                            <div class="progress-text" id="progress-text">Đang tải...</div>
                        </div>

                        <div class="file-info-card" id="file-info-card">
                            <div class="file-info-header">
                                <div class="file-icon-wrapper">
                                    <i class="fas fa-cube"></i>
                                </div>
                                <div class="file-meta">
                                    <div class="file-name" id="file-name">model.obj</div>
                                    <div class="file-size" id="file-size-display">2.4 MB</div>
                                </div>
                                <button class="file-remove-btn" id="file-remove-btn" title="Xóa file">
                                    <i class="fas fa-times"></i>
                                </button>
                            </div>
                        </div>

                        <div class="file-stats" id="file-stats">
                            <div class="stat-item">
                                <div class="stat-label">Thể tích</div>
                                <div class="stat-value" id="stat-volume">— <span class="stat-unit">cm³</span></div>
                            </div>
                            <div class="stat-item">
                                <div class="stat-label">Tam giác</div>
                                <div class="stat-value" id="stat-triangles">—</div>
                            </div>
                            <div class="stat-item">
                                <div class="stat-label">Kích thước X</div>
                                <div class="stat-value" id="stat-size-x">— <span class="stat-unit">mm</span></div>
                            </div>
                            <div class="stat-item">
                                <div class="stat-label">Kích thước Y</div>
                                <div class="stat-value" id="stat-size-y">— <span class="stat-unit">mm</span></div>
                            </div>
                            <div class="stat-item">
                                <div class="stat-label">Kích thước Z</div>
                                <div class="stat-value" id="stat-size-z">— <span class="stat-unit">mm</span></div>
                            </div>
                            <div class="stat-item">
                                <div class="stat-label" title="Thể tích hình hộp bao quanh mô hình, không phải thể tích vật liệu.">Thể tích hộp bao*</div>
                                <div class="stat-value" id="stat-bbox">— <span class="stat-unit">cm³</span></div>
                            </div>
                        </div>
                    </div>

                    <div class="viewer-panel">
                        
                        <div class="viewer-toolbar">
                            <div class="viewer-toolbar-left">
                                <button class="viewer-tool-btn active" id="tool-rotate" title="Xoay">
                                    <i class="fas fa-sync-alt"></i>
                                </button>
                                <button class="viewer-tool-btn" id="tool-pan" title="Kéo">
                                    <i class="fas fa-arrows-alt"></i>
                                </button>
                                <button class="viewer-tool-btn" id="tool-zoom-in" title="Phóng to">
                                    <i class="fas fa-search-plus"></i>
                                </button>
                                <button class="viewer-tool-btn" id="tool-zoom-out" title="Thu nhỏ">
                                    <i class="fas fa-search-minus"></i>
                                </button>
                                <button class="viewer-tool-btn" id="tool-reset" title="Reset">
                                    <i class="fas fa-compress-arrows-alt"></i>
                                </button>
                                <button class="viewer-tool-btn" id="tool-wireframe" title="Wireframe">
                                    <i class="fas fa-border-all"></i>
                                </button>
                            </div>
                            <div class="viewer-toolbar-right">
                                <span class="viewer-mode-tag" id="viewer-mode-tag">Chờ file...</span>
                            </div>
                        </div>

                        <div class="viewer-canvas-wrapper" id="viewer-canvas-wrapper">
                            <canvas id="three-canvas"></canvas>

                            <div class="viewer-empty-state" id="viewer-empty-state">
                                <div class="viewer-empty-icon">
                                    <i class="fas fa-cube"></i>
                                </div>
                                <h3>Trình xem 3D</h3>
                                <p>Tải lên file 3D để xem trước mô hình<br>của bạn ngay tại đây</p>
                            </div>

                            <div class="viewer-loading-overlay" id="viewer-loading">
                                <div class="loading-spinner"></div>
                                <span class="loading-text">Đang tải mô hình 3D...</span>
                            </div>

                            <div class="viewer-axes-label" id="viewer-axes">
                                <span class="axis-x">X</span>
                                <span class="axis-y">Y</span>
                                <span class="axis-z">Z</span>
                            </div>
                        </div>

                        <div class="viewer-footer">
                            <div class="viewer-hint">
                                <span><kbd>LMB</kbd> Xoay</span>
                                <span><kbd>RMB</kbd> Kéo</span>
                                <span><kbd>Ctrl</kbd>+<kbd>Scroll</kbd> Thu phóng</span>
                            </div>
                            <div class="viewer-dimensions" id="viewer-dimensions">—</div>
                        </div>
                    </div>

                    <div class="controls-panel">

                        <div class="control-section">
                            <div class="section-title">
                                <i class="fas fa-cogs"></i>
                                <span>Công nghệ in</span>
                            </div>
                            <div class="option-grid" id="tech-options">
                                <div class="option-card active" data-tech="fdm">
                                    <span class="opt-name">FDM</span>
                                    <span class="opt-desc">Sợi nhựa, phổ biến</span>
                                </div>
                                <div class="option-card" data-tech="sla">
                                    <span class="opt-name">SLA/DLP</span>
                                    <span class="opt-desc">Resin, chi tiết cao</span>
                                </div>
                                <div class="option-card" data-tech="sls">
                                    <span class="opt-name">SLS</span>
                                    <span class="opt-desc">In bột, bền</span>
                                </div>
                            </div>
                        </div>

                        <div class="control-section">
                            <div class="section-title">
                                <i class="fas fa-layer-group"></i>
                                <span>Vật liệu & Màu sắc</span>
                            </div>
                            <div class="styled-select-wrap">
                                <select class="styled-select" id="material-select">
                                    
                                    <optgroup label="Nhựa FDM" id="mat-fdm">
                                        <option value="pla" data-price="1800" selected>PLA — Dễ in, thân thiện môi
                                            trường</option>
                                        <option value="abs" data-price="2200">ABS — Bền, chịu nhiệt tốt</option>
                                        <option value="petg" data-price="2500">PETG — Trong suốt, chống hóa chất
                                        </option>
                                        <option value="tpu" data-price="3500">TPU — Nhựa dẻo, đàn hồi</option>
                                    </optgroup>
                                    
                                    <optgroup label="Resin SLA" id="mat-sla" disabled>
                                        <option value="resin-std" data-price="4000">Resin tiêu chuẩn</option>
                                        <option value="resin-clear" data-price="5500">Resin trong suốt</option>
                                        <option value="resin-flex" data-price="6000">Resin dẻo</option>
                                    </optgroup>
                                    
                                    <optgroup label="Bột SLS" id="mat-sls" disabled>
                                        <option value="nylon-pa12" data-price="7000">Nylon PA12</option>
                                        <option value="nylon-gf" data-price="9500">Nylon sợi thủy tinh</option>
                                    </optgroup>
                                </select>
                            </div>

                            <div class="color-swatches" id="color-swatches">
                                <button class="color-swatch active" data-color="#F5F5F5"
                                    style="background:#F5F5F5; border:1px solid #ddd;" title="Trắng tự nhiên"></button>
                                <button class="color-swatch" data-color="#1B1B2F" style="background:#1B1B2F;"
                                    title="Đen"></button>
                                <button class="color-swatch" data-color="#FF6B6B" style="background:#FF6B6B;"
                                    title="Đỏ"></button>
                                <button class="color-swatch" data-color="#4A90D9" style="background:#4A90D9;"
                                    title="Xanh dương"></button>
                                <button class="color-swatch" data-color="#00B894" style="background:#00B894;"
                                    title="Xanh lá"></button>
                                <button class="color-swatch" data-color="#FFB347" style="background:#FFB347;"
                                    title="Cam"></button>
                                <button class="color-swatch" data-color="#DDA0DD" style="background:#DDA0DD;"
                                    title="Tím nhạt"></button>
                                
                                <label class="color-swatch color-picker-swatch" title="Chọn màu tùy ý"
                                    id="custom-color-label">
                                    <i class="fas fa-palette"></i>
                                    <input type="color" id="custom-color-input" value="#8A78FB"
                                        style="opacity:0;position:absolute;width:0;height:0;">
                                </label>
                            </div>
                        </div>

                        <div class="control-section">
                            <div class="section-title">
                                <i class="fas fa-sliders-h"></i>
                                <span>Độ phân giải (Layer Height)</span>
                            </div>
                            <div class="pill-row" id="resolution-options">
                                <button class="pill" data-res="0.12" data-factor="1.6">0.12mm
                                    <small>Mịn</small></button>
                                <button class="pill active" data-res="0.2" data-factor="1.0">0.2mm <small>Tiêu
                                        chuẩn</small></button>
                                <button class="pill" data-res="0.28" data-factor="0.75">0.28mm
                                    <small>Thô/Nhanh</small></button>
                            </div>
                        </div>

                        <div class="control-section">
                            <div class="section-title">
                                <i class="fas fa-th"></i>
                                <span>Độ đặc (Infill)</span>
                            </div>
                            <div class="infill-options" id="infill-options">
                                <div class="infill-card" data-infill="15" data-factor="0.7">
                                    <div class="infill-visual">
                                        <div class="infill-fill" style="height:15%"></div>
                                    </div>
                                    <span class="infill-label">15%</span>
                                </div>
                                <div class="infill-card active" data-infill="20" data-factor="1.0">
                                    <div class="infill-visual">
                                        <div class="infill-fill" style="height:20%"></div>
                                    </div>
                                    <span class="infill-label">20%</span>
                                </div>
                                <div class="infill-card" data-infill="50" data-factor="1.5">
                                    <div class="infill-visual">
                                        <div class="infill-fill" style="height:50%"></div>
                                    </div>
                                    <span class="infill-label">50%</span>
                                </div>
                                <div class="infill-card" data-infill="100" data-factor="2.2">
                                    <div class="infill-visual">
                                        <div class="infill-fill" style="height:100%"></div>
                                    </div>
                                    <span class="infill-label">100%</span>
                                </div>
                            </div>
                        </div>

                        <div class="control-section">
                            <div class="section-title">
                                <i class="fas fa-copy"></i>
                                <span>Số lượng</span>
                            </div>
                            <div class="qty-row">
                                <div class="qty-controls">
                                    <button class="qty-btn" id="qty-minus"><i class="fas fa-minus"></i></button>
                                    <input type="number" class="qty-input" id="qty-input" value="1" min="1" max="100000">
                                    <button class="qty-btn" id="qty-plus"><i class="fas fa-plus"></i></button>
                                </div>
                                <span class="qty-note">Giảm giá khi mua từ 5 bản</span>
                            </div>
                        </div>

                        <div class="control-section shop-selection-section">
                            <div class="section-title">
                                <i class="fas fa-store"></i>
                                <span>Xưởng in đề xuất</span>
                            </div>
                            <div class="shop-suggest-desc">
                                <span>Hệ thống tự động lọc các xưởng in uy tín phù hợp nhất với cấu hình bạn chọn:</span>
                            </div>
                            <div class="suitable-shops-list" id="suitable-shops-list">
                                <div class="shop-loading-card">
                                    <div class="shop-loading-spinner"><i class="fas fa-spinner fa-spin"></i></div>
                                    <span>Đang tìm xưởng in phù hợp...</span>
                                </div>
                            </div>
                            <label class="shop-manual-search">Tìm xưởng theo tên hoặc khu vực<input id="shop-search" type="search" placeholder="Tên xưởng, quận, tỉnh"></label>
                            <div class="shop-filters" aria-label="Lọc và sắp xếp xưởng">
                                <label>Giá tối thiểu (VND)<input id="shop-filter-min-price" type="number" min="0" step="1000" placeholder="Không giới hạn"></label>
                                <label>Giá tối đa (VND)<input id="shop-filter-price" type="number" min="0" step="1000" placeholder="Không giới hạn"></label>
                                <label>Thời gian tối đa (ngày)<input id="shop-filter-days" type="number" min="1" max="365" step="1" placeholder="Không giới hạn"></label>
                                <label>Đánh giá từ<select id="shop-filter-rating"><option value="0">Mọi mức</option><option value="3">3 sao</option><option value="4">4 sao</option><option value="4.5">4.5 sao</option></select></label>
                                <label>Sắp xếp<select id="shop-sort"><option value="best">Phù hợp nhất</option><option value="price">Giá thấp trước</option><option value="price-desc">Giá cao trước</option><option value="time">Thời gian nhanh trước</option><option value="distance">Gần nhất</option><option value="rating">Đánh giá cao trước</option></select></label>
                            </div>
                            <div id="shop-print-options"></div>
                            <label class="shop-manual-search">Ghi chú cho xưởng<textarea id="customer-note" maxlength="1000" rows="3" placeholder="Yêu cầu đóng gói, bề mặt hoặc lưu ý khác"></textarea></label>
                        </div>

                        <div class="pricing-card" id="pricing-card">
                            <div class="pricing-header">
                                <i class="fas fa-bolt"></i>
                                <span>Báo giá tự động</span>
                            </div>

                            <div class="price-row">
                                <span>Thể tích ước tính</span>
                                <span id="price-volume">— cm³</span>
                            </div>
                            <div class="price-row">
                                <span>Đơn giá vật liệu</span>
                                <span id="price-material-unit">— đ/cm³</span>
                            </div>
                            <div class="price-row">
                                <span>Chi phí vật liệu</span>
                                <span id="price-material">— đ</span>
                            </div>
                            <div class="price-row">
                                <span>Hệ số phân giải</span>
                                <span id="price-res-factor">×1.0</span>
                            </div>
                            <div class="price-row">
                                <span>Hệ số infill</span>
                                <span id="price-infill-factor">×1.0</span>
                            </div>
                            <div class="price-row">
                                <span>Phí khấu hao máy</span>
                                <span id="price-machine">— đ</span>
                            </div>
                            <div class="price-row discount" id="discount-row" style="display:none;">
                                <span>Giảm giá số lượng</span>
                                <span id="price-discount">-0đ</span>
                            </div>

                            <div class="divider"></div>

                            <div class="price-total">
                                <span>Tổng cộng</span>
                                <span class="total-val" id="total-price">—</span>
                            </div>
                            <div class="price-per-unit" id="price-per-unit"></div>

                            <div class="saving-tag" id="saving-tag" style="display:none;">Tiết kiệm khi mua nhiều</div>

                            <button class="btn-order-now" id="btn-order-now" disabled>
                                <i class="fas fa-bolt"></i>
                                Đặt in ngay với xưởng đã chọn
                            </button>
                            <button class="btn-order-now btn-quote-request" id="btn-quote-request" disabled>
                                <i class="fas fa-file-invoice-dollar"></i>
                                Yêu cầu xưởng báo giá
                            </button>
                            <button class="btn-add-cart" id="btn-add-cart" style="display:none;">
                                <i class="fas fa-shopping-cart"></i>
                                Thêm vào giỏ hàng
                            </button>
                        </div>

                    </div>
                </div>
            </main>

            <div class="tpl-overlay" id="tpl-overlay">
                <div class="tpl-modal" id="tpl-modal">
                    <div class="tpl-modal-header">
                        <div>
                            <span class="tpl-modal-eyebrow">QUICK START</span>
                            <h2 class="tpl-modal-title">Chọn mẫu in 3D</h2>
                            <p class="tpl-modal-sub">Áp dụng ngay cài đặt tối ưu cho từng mục đích sử dụng. Bạn vẫn có
                                thể tuỳ chỉnh sau khi chọn.</p>
                        </div>
                        <button class="tpl-close-btn" id="tpl-close-btn"><i class="fas fa-times"></i></button>
                    </div>

                    <div class="tpl-grid">

                        <div class="tpl-card" data-tpl="standard">
                            <div class="tpl-card-preview"
                                style="--tpl-color:#F5F5F5; --tpl-grad:linear-gradient(135deg,#e8e6f5,#f5f4fb);">
                                <div class="tpl-preview-model">
                                    <div class="tpl-model-icon"><i class="fas fa-wrench"></i></div>
                                    <div class="tpl-color-dot" style="background:#F5F5F5; border:2px solid #ddd;"></div>
                                </div>
                                <div class="tpl-badge tpl-badge-popular">Phổ biến</div>
                            </div>
                            <div class="tpl-card-body">
                                <h3>Tiêu chuẩn</h3>
                                <p>Cân bằng giữa chất lượng và tốc độ. Phù hợp đa mục đích.</p>
                                <div class="tpl-specs">
                                    <span><i class="fas fa-layer-group"></i> PLA · Trắng</span>
                                    <span><i class="fas fa-sliders-h"></i> 0.2mm</span>
                                    <span><i class="fas fa-th"></i> 20% infill</span>
                                </div>
                            </div>
                            <button class="tpl-apply-btn">Áp dụng <i class="fas fa-arrow-right"></i></button>
                        </div>

                        <div class="tpl-card" data-tpl="highdetail">
                            <div class="tpl-card-preview"
                                style="--tpl-color:#DDA0DD; --tpl-grad:linear-gradient(135deg,#f0e8ff,#ede0ff);">
                                <div class="tpl-preview-model">
                                    <div class="tpl-model-icon"><i class="fas fa-gem"></i></div>
                                    <div class="tpl-color-dot" style="background:#DDA0DD;"></div>
                                </div>
                                <div class="tpl-badge tpl-badge-premium">Premium</div>
                            </div>
                            <div class="tpl-card-body">
                                <h3>Độ chi tiết cao</h3>
                                <p>In siêu mịn bằng Resin SLA. Dành cho mô hình trang trí, jewelry.</p>
                                <div class="tpl-specs">
                                    <span><i class="fas fa-layer-group"></i> Resin SLA · Tím</span>
                                    <span><i class="fas fa-sliders-h"></i> 0.12mm</span>
                                    <span><i class="fas fa-th"></i> 50% infill</span>
                                </div>
                            </div>
                            <button class="tpl-apply-btn">Áp dụng <i class="fas fa-arrow-right"></i></button>
                        </div>

                        <div class="tpl-card" data-tpl="flexible">
                            <div class="tpl-card-preview"
                                style="--tpl-color:#00B894; --tpl-grad:linear-gradient(135deg,#d4f5ed,#b2edde);">
                                <div class="tpl-preview-model">
                                    <div class="tpl-model-icon"><i class="fas fa-leaf"></i></div>
                                    <div class="tpl-color-dot" style="background:#00B894;"></div>
                                </div>
                            </div>
                            <div class="tpl-card-body">
                                <h3>Linh hoạt / Dẻo</h3>
                                <p>Nhựa TPU đàn hồi. Lý tưởng cho ốp lưng, miếng đệm, đồ chơi.</p>
                                <div class="tpl-specs">
                                    <span><i class="fas fa-layer-group"></i> TPU · Xanh</span>
                                    <span><i class="fas fa-sliders-h"></i> 0.2mm</span>
                                    <span><i class="fas fa-th"></i> 15% infill</span>
                                </div>
                            </div>
                            <button class="tpl-apply-btn">Áp dụng <i class="fas fa-arrow-right"></i></button>
                        </div>

                        <div class="tpl-card" data-tpl="industrial">
                            <div class="tpl-card-preview"
                                style="--tpl-color:#1B1B2F; --tpl-grad:linear-gradient(135deg,#2a2a4a,#383063);">
                                <div class="tpl-preview-model">
                                    <div class="tpl-model-icon" style="color:#FFB347;"><i class="fas fa-cogs"></i></div>
                                    <div class="tpl-color-dot" style="background:#1B1B2F; border:2px solid #444;"></div>
                                </div>
                                <div class="tpl-badge tpl-badge-pro">Pro</div>
                            </div>
                            <div class="tpl-card-body">
                                <h3>Công nghiệp</h3>
                                <p>Bột SLS Nylon bền chắc, chịu lực. Dùng cho linh kiện cơ khí.</p>
                                <div class="tpl-specs">
                                    <span><i class="fas fa-layer-group"></i> Nylon SLS · Đen</span>
                                    <span><i class="fas fa-sliders-h"></i> 0.12mm</span>
                                    <span><i class="fas fa-th"></i> 100% infill</span>
                                </div>
                            </div>
                            <button class="tpl-apply-btn">Áp dụng <i class="fas fa-arrow-right"></i></button>
                        </div>

                        <div class="tpl-card" data-tpl="fastdraft">
                            <div class="tpl-card-preview"
                                style="--tpl-color:#FFB347; --tpl-grad:linear-gradient(135deg,#fff3dc,#ffe4b5);">
                                <div class="tpl-preview-model">
                                    <div class="tpl-model-icon"><i class="fas fa-bolt"></i></div>
                                    <div class="tpl-color-dot" style="background:#FFB347;"></div>
                                </div>
                                <div class="tpl-badge tpl-badge-fast">Nhanh</div>
                            </div>
                            <div class="tpl-card-body">
                                <h3>In nhanh / Thử mẫu</h3>
                                <p>Tốc độ tối đa, chi phí thấp. Phù hợp để kiểm tra form sản phẩm.</p>
                                <div class="tpl-specs">
                                    <span><i class="fas fa-layer-group"></i> PLA · Vàng</span>
                                    <span><i class="fas fa-sliders-h"></i> 0.28mm</span>
                                    <span><i class="fas fa-th"></i> 15% infill</span>
                                </div>
                            </div>
                            <button class="tpl-apply-btn">Áp dụng <i class="fas fa-arrow-right"></i></button>
                        </div>

                        <div class="tpl-card" data-tpl="transparent">
                            <div class="tpl-card-preview"
                                style="--tpl-color:#4A90D9; --tpl-grad:linear-gradient(135deg,#ddeeff,#c5e0ff);">
                                <div class="tpl-preview-model">
                                    <div class="tpl-model-icon"><i class="fas fa-search"></i></div>
                                    <div class="tpl-color-dot"
                                        style="background:rgba(74,144,217,0.3); border:2px solid #4A90D9; backdrop-filter:blur(4px);">
                                    </div>
                                </div>
                            </div>
                            <div class="tpl-card-body">
                                <h3>Trong suốt</h3>
                                <p>Resin trong suốt, in siêu mịn. Hoàn hảo cho lens cover, mẫu trưng bày.</p>
                                <div class="tpl-specs">
                                    <span><i class="fas fa-layer-group"></i> Resin Clear · Xanh</span>
                                    <span><i class="fas fa-sliders-h"></i> 0.12mm</span>
                                    <span><i class="fas fa-th"></i> 50% infill</span>
                                </div>
                            </div>
                            <button class="tpl-apply-btn">Áp dụng <i class="fas fa-arrow-right"></i></button>
                        </div>

                    </div>
                </div>
            </div>

            <%@ include file="footer.jsp" %>

                <script>
                    document.addEventListener("DOMContentLoaded", () => {
                        const contextPath = '<%= request.getContextPath() %>';
                        const csrfToken = document.getElementById('csrf-token')?.value || '';

                        function showToast(msg, type = "info") {
                            const c = document.getElementById("toast-container");
                            const t = document.createElement("div");
                            t.className = "toast " + type;
                            t.innerHTML = '<i class="fas fa-' + (type === 'success' ? 'check-circle' : type === 'error' ? 'exclamation-circle' : 'info-circle') + '"></i>' + msg;
                            c.appendChild(t);
                            setTimeout(() => { t.classList.add("fadeOut"); setTimeout(() => t.remove(), 300); }, 3500);
                        }
                        
                        let currentUploadedFile = null;
                        let activeUploadRequest = null;
                        let selectedShopId = null;
                        let savedRequestId = null;
                        let savedAnalysis = null;
                        let selectedShopData = null;
                        let selectedColorValue = "";
                        let selectedFinishValue = "";
                        let recommendedShops = [];
                        let shopRecommendationVersion = 0;
                        let shopRecommendationController = null;
                        let isLoadingShopRecommendations = false;

                        let modelData = {
                            volume: 0,       
                            triangles: 0,
                            sizeX: 0,
                            sizeY: 0,
                            sizeZ: 0,
                            bboxVol: 0,      
                            loaded: false
                        };

                        const canvasEl = document.getElementById("three-canvas");
                        const wrapperEl = document.getElementById("viewer-canvas-wrapper");
                        const emptyState = document.getElementById("viewer-empty-state");
                        const loadingOverlay = document.getElementById("viewer-loading");
                        const modeTag = document.getElementById("viewer-mode-tag");
                        const dimLabel = document.getElementById("viewer-dimensions");

                        let scene, camera, renderer, controls, currentMesh = null;
                        let wireframeMode = false;
                        let currentColor = "#F5F5F5";

                        function initThree() {
                            scene = new THREE.Scene();
                            scene.background = new THREE.Color(0xF0EFF5);

                            camera = new THREE.PerspectiveCamera(45, wrapperEl.clientWidth / wrapperEl.clientHeight, 0.1, 10000);
                            camera.position.set(100, 100, 200);

                            renderer = new THREE.WebGLRenderer({ canvas: canvasEl, antialias: true });
                            renderer.setPixelRatio(window.devicePixelRatio);
                            renderer.setSize(wrapperEl.clientWidth, wrapperEl.clientHeight);
                            renderer.shadowMap.enabled = true;
                            renderer.shadowMap.type = THREE.PCFSoftShadowMap;
                            renderer.outputEncoding = THREE.sRGBEncoding;

                            const ambientLight = new THREE.AmbientLight(0xffffff, 0.6);
                            scene.add(ambientLight);

                            const dirLight = new THREE.DirectionalLight(0xffffff, 0.8);
                            dirLight.position.set(100, 200, 150);
                            dirLight.castShadow = true;
                            scene.add(dirLight);

                            const fillLight = new THREE.DirectionalLight(0x8A78FB, 0.3);
                            fillLight.position.set(-100, 50, -100);
                            scene.add(fillLight);

                            const groundGeo = new THREE.PlaneGeometry(2000, 2000);
                            const groundMat = new THREE.MeshStandardMaterial({ color: 0xE8E6F0, roughness: 0.9 });
                            const ground = new THREE.Mesh(groundGeo, groundMat);
                            ground.rotation.x = -Math.PI / 2;
                            ground.position.y = -0.5;
                            ground.receiveShadow = true;
                            scene.add(ground);

                            const grid = new THREE.GridHelper(400, 40, 0xCBC8E0, 0xDDD9F0);
                            grid.position.y = 0;
                            scene.add(grid);

                            controls = new THREE.OrbitControls(camera, renderer.domElement);
                            controls.enableDamping = true;
                            controls.dampingFactor = 0.08;
                            controls.minDistance = 10;
                            controls.maxDistance = 2000;
                            controls.target.set(0, 30, 0);
                            
                            controls.enableZoom = false;

                            function dollyCamera(factor) {
                                if (!camera || !controls) return;
                                camera.position.sub(controls.target);
                                camera.position.multiplyScalar(factor);
                                const dist = camera.position.length();
                                if (dist < controls.minDistance) camera.position.setLength(controls.minDistance);
                                if (dist > controls.maxDistance) camera.position.setLength(controls.maxDistance);
                                camera.position.add(controls.target);
                                controls.update();
                            }

                            wrapperEl.addEventListener("wheel", (e) => {
                                const isFullscreen = document.querySelector(".viewer-panel")?.classList.contains("fullscreen-mode");
                                if (isFullscreen || e.ctrlKey || e.metaKey) {
                                    e.preventDefault();
                                    const factor = e.deltaY < 0 ? 0.9 : 1.1;
                                    dollyCamera(factor);
                                }
                            }, { passive: false });

                            function animate() {
                                requestAnimationFrame(animate);
                                controls.update();
                                renderer.render(scene, camera);
                            }
                            animate();

                            const resizeObserver = new ResizeObserver(() => {
                                const w = wrapperEl.clientWidth;
                                const h = wrapperEl.clientHeight;
                                camera.aspect = w / h;
                                camera.updateProjectionMatrix();
                                renderer.setSize(w, h);
                            });
                            resizeObserver.observe(wrapperEl);
                        }

                        initThree();

                        function loadModel(file) {
                            const ext = file.name.split('.').pop().toLowerCase();
                            if (ext !== 'obj') {
                                showToast('Chỉ chấp nhận tệp .obj.', 'error');
                                document.getElementById('file-input').value = '';
                                return;
                            }
                            if (file.size <= 0 || file.size > 100 * 1024 * 1024) {
                                showToast('Tệp OBJ phải lớn hơn 0 và không vượt quá 100 MB.', 'error');
                                document.getElementById('file-input').value = '';
                                return;
                            }

                            currentUploadedFile = file;
                            savedRequestId = null;
                            savedAnalysis = null;
                            selectedShopId = null;
                            selectedShopData = null;
                            resetShopRecommendations();
                            modelData.loaded = false;
                            document.getElementById('file-stats').classList.remove('visible');
                            document.getElementById('model-analysis-message').textContent = 'Máy chủ sẽ kiểm tra OBJ trước khi cho phép đặt in.';
                            dimLabel.textContent = 'Đang phân tích…';
                            if (currentMesh) { scene.remove(currentMesh); currentMesh = null; }

                            const progressEl = document.getElementById('upload-progress');
                            document.getElementById('progress-fill').style.width = '0%';
                            document.getElementById('progress-text').textContent = 'Đang chuẩn bị tải lên…';
                            progressEl.classList.add('visible');
                            loadingOverlay.classList.add('visible');
                            emptyState.classList.add('hidden');
                            modeTag.textContent = 'Đang tải lên…';
                            modeTag.style.color = 'var(--amber)';
                            modeTag.style.background = 'rgba(255,179,71,0.1)';
                            document.getElementById('file-info-card').classList.add('visible');
                            document.getElementById('file-name').textContent = file.name;
                            document.getElementById('file-size-display').textContent = (file.size / (1024 * 1024)).toFixed(2) + ' MB';

                            const reader = new FileReader();
                            reader.onload = function (event) {
                                try {
                                    const loader = new THREE.OBJLoader();
                                    const previewObject = loader.parse(new TextDecoder('utf-8', { fatal: true }).decode(event.target.result));
                                    let meshCount = 0;
                                    previewObject.traverse(child => {
                                        if (!child.isMesh) return;
                                        meshCount++;
                                        child.geometry.computeBoundingBox();
                                        child.geometry.computeVertexNormals();
                                        child.material = new THREE.MeshStandardMaterial({
                                            color: new THREE.Color(currentColor), metalness: 0.15, roughness: 0.55
                                        });
                                        child.castShadow = true;
                                        child.receiveShadow = true;
                                    });
                                    if (!meshCount) throw new Error('OBJ has no previewable mesh.');
                                    const bounds = new THREE.Box3().setFromObject(previewObject);
                                    if (bounds.isEmpty()) throw new Error('OBJ has no previewable geometry.');
                                    const center = new THREE.Vector3();
                                    bounds.getCenter(center);
                                    previewObject.position.sub(center);
                                    previewObject.position.y += (bounds.max.y - bounds.min.y) / 2;
                                    currentMesh = previewObject;
                                    scene.add(currentMesh);
                                    const size = bounds.getSize(new THREE.Vector3());
                                    modelData = { volume: 0, triangles: 0, sizeX: size.x, sizeY: size.y, sizeZ: size.z, loaded: false };
                                    const maxDim = Math.max(size.x, size.y, size.z);
                                    const distance = maxDim * 2.5;
                                    camera.position.set(distance * 0.7, distance * 0.5, distance * 0.9);
                                    controls.target.set(0, size.y / 2, 0);
                                    controls.update();
                                    modeTag.textContent = 'Đã đọc OBJ; đang chờ máy chủ phân tích';
                                } catch (error) {
                                    console.warn('OBJ preview failed; server validation will continue.', error);
                                    document.getElementById('model-analysis-message').textContent = 'Không thể xem trước trong trình duyệt; tệp vẫn được gửi để máy chủ kiểm tra.';
                                    modeTag.textContent = 'Đang chờ máy chủ kiểm tra';
                                }
                                persistDraft(file);
                            };
                            reader.onerror = function () {
                                document.getElementById('model-analysis-message').textContent = 'Không thể xem trước trong trình duyệt; đang gửi tệp để máy chủ kiểm tra.';
                                persistDraft(file);
                            };
                            reader.readAsArrayBuffer(file);
                        }
                        function showServerAnalysis(analysis) {
                            savedAnalysis = analysis;
                            document.getElementById('model-unit').value = analysis.unit || 'mm';
                            document.getElementById('model-analysis-message').textContent = analysis.message || '';
                            const confirmRow = document.getElementById('dimensions-confirm-row');
                            const confirmBox = document.getElementById('dimensions-confirmed');
                            confirmRow.hidden = analysis.status !== 'NEEDS_CONFIRMATION';
                            confirmBox.checked = false;
                            const ready = analysis.status === 'READY' || analysis.status === 'NEEDS_CONFIRMATION';
                            const width = Number(analysis.widthMm) || 0;
                            const depth = Number(analysis.depthMm) || 0;
                            const height = Number(analysis.heightMm) || 0;
                            const measurable = width > 0 && depth > 0 && height > 0;
                            modelData = {
                                volume: Number(analysis.volumeCm3) || 0,
                                triangles: Number(analysis.triangleCount) || 0,
                                sizeX: width, sizeY: depth, sizeZ: height,
                                loaded: ready
                            };
                            if (measurable) {
                                document.getElementById('stat-volume').textContent = ready
                                    ? modelData.volume.toFixed(3) + ' cm³'
                                    : 'Cần xưởng kiểm tra';
                                document.getElementById('stat-triangles').textContent = modelData.triangles.toLocaleString('vi-VN');
                                document.getElementById('stat-size-x').textContent = width.toFixed(2) + ' mm';
                                document.getElementById('stat-size-y').textContent = depth.toFixed(2) + ' mm';
                                document.getElementById('stat-size-z').textContent = height.toFixed(2) + ' mm';
                                document.getElementById('stat-bbox').textContent = ((width * depth * height) / 1000).toFixed(3) + ' cm³';
                                document.getElementById('file-stats').classList.add('visible');
                                dimLabel.textContent = [width, depth, height].map(value => value.toFixed(2)).join(' × ') + ' mm';
                            } else {
                                document.getElementById('stat-volume').textContent = 'Cần xưởng kiểm tra';
                                document.getElementById('file-stats').classList.remove('visible');
                                dimLabel.textContent = 'Cần xưởng kiểm tra';
                            }
                            modeTag.textContent = ready
                                ? (analysis.status === 'NEEDS_CONFIRMATION' ? 'Xác nhận kích thước' : 'Đã phân tích')
                                : 'Cần xưởng báo giá';
                            modeTag.style.color = analysis.status === 'READY' ? 'var(--teal)' : 'var(--amber)';
                            modeTag.style.background = analysis.status === 'READY'
                                ? 'rgba(0,184,148,0.08)' : 'rgba(255,179,71,0.1)';
                            updateQuote();
                            loadSuitableShops();
                        }

                        function persistDraft(file) {
                            if (!file) return;
                            if (activeUploadRequest) activeUploadRequest.abort();
                            const form = new FormData();
                            form.append('action', 'uploadDraft');
                            form.append('file', file);
                            form.append('unit', document.getElementById('model-unit').value || 'mm');
                            form.append('technology', selectedTech.toUpperCase());
                            form.append('material', String(matSelect.value).toUpperCase());
                            form.append('quantity', qtyInput.value || '1');
                            document.getElementById('model-analysis-message').textContent = 'Đang tải tệp và kiểm tra cấu trúc OBJ trên máy chủ…';
                            const xhr = new XMLHttpRequest();
                            activeUploadRequest = xhr;
                            xhr.open('POST', contextPath + '/design');
                            xhr.setRequestHeader('X-CSRF-Token', csrfToken);
                            xhr.setRequestHeader('Accept', 'application/json');
                            xhr.upload.addEventListener('progress', event => {
                                if (!event.lengthComputable || activeUploadRequest !== xhr || currentUploadedFile !== file) return;
                                const percent = Math.min(99, Math.round(event.loaded * 100 / event.total));
                                document.getElementById('progress-fill').style.width = percent + '%';
                                document.getElementById('progress-text').textContent = 'Đang tải lên… ' + percent + '%';
                            });
                            const fail = message => {
                                if (activeUploadRequest !== xhr || currentUploadedFile !== file) return;
                                activeUploadRequest = null;
                                savedRequestId = null;
                                savedAnalysis = null;
                                document.getElementById('model-analysis-message').textContent = message;
                                document.getElementById('progress-text').textContent = 'Tải lên thất bại';
                                loadingOverlay.classList.remove('visible');
                                updateOrderButtonState();
                            };
                            xhr.onload = () => {
                                if (activeUploadRequest !== xhr || currentUploadedFile !== file) return;
                                let data;
                                try { data = JSON.parse(xhr.responseText); }
                                catch (_) { fail('Máy chủ trả về phản hồi không hợp lệ.'); return; }
                                if (xhr.status < 200 || xhr.status >= 300 || !data.success) {
                                    fail(data.message || 'Không thể phân tích mô hình OBJ.');
                                    return;
                                }
                                activeUploadRequest = null;
                                savedRequestId = data.requestId;
                                const requestedUnit = document.getElementById('model-unit').value;
                                document.getElementById('progress-fill').style.width = '100%';
                                document.getElementById('progress-text').textContent = 'Đã tải lên và kiểm tra';
                                showServerAnalysis(data.analysis);
                                if (requestedUnit && requestedUnit !== data.analysis.unit) {
                                    document.getElementById('model-unit').value = requestedUnit;
                                    reanalyzeSavedDraft();
                                }
                                window.setTimeout(() => {
                                    document.getElementById('upload-progress').classList.remove('visible');
                                    loadingOverlay.classList.remove('visible');
                                }, 500);
                            };
                            xhr.onerror = () => fail('Không thể kết nối máy chủ để tải tệp OBJ.');
                            xhr.onabort = () => {
                                if (activeUploadRequest === xhr) {
                                    activeUploadRequest = null;
                                    loadingOverlay.classList.remove('visible');
                                }
                            };
                            xhr.send(form);
                        }
                        function reanalyzeSavedDraft() {
                            if (!savedRequestId) { if (!activeUploadRequest && currentUploadedFile) persistDraft(currentUploadedFile); return; }
                            const body = new URLSearchParams({ action: "reanalyze", requestId: savedRequestId, unit: document.getElementById("model-unit").value });
                            fetch(contextPath + "/design", { method: "POST", headers: { "X-CSRF-Token": csrfToken, "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8", "Accept": "application/json" }, body })
                                .then(async response => { const data = await response.json(); if (!response.ok || !data.success) throw new Error(data.message || "Không thể đổi đơn vị."); return data; })
                                .then(data => showServerAnalysis(data.analysis))
                                .catch(error => { document.getElementById("model-analysis-message").textContent = error.message; });
                        }

                        function loadExistingDraft() {
                            const id=new URLSearchParams(window.location.search).get("requestId");
                            if(!id){loadSuitableShops();return;}
                            document.getElementById("model-analysis-message").textContent="Đang mở tệp đã lưu trong tài khoản của bạn…";
                            fetch(contextPath+"/design?action=draft&requestId="+encodeURIComponent(id),{credentials:"same-origin",cache:"no-store"})
                                .then(async response=>{const data=await response.json();if(!response.ok||!data.success)throw new Error(data.message||"Không thể mở tệp.");return data;})
                                .then(data=>{savedRequestId=data.requestId;savedAnalysis=data.analysis;document.getElementById("file-info-card").classList.add("visible");document.getElementById("file-name").textContent=data.filename;document.getElementById("file-size-display").textContent="Tệp đã lưu an toàn";showServerAnalysis(data.analysis);})
                                .catch(error=>{document.getElementById("model-analysis-message").textContent=error.message;});
                        }

                        const dropZone = document.getElementById("drop-zone");
                        const fileInput = document.getElementById("file-input");
                        const browseLink = document.getElementById("browse-link");

                        browseLink.addEventListener("click", (e) => { e.stopPropagation(); fileInput.click(); });
                        dropZone.addEventListener("click", () => fileInput.click());

                        fileInput.addEventListener("change", (e) => {
                            if (e.target.files.length > 0) loadModel(e.target.files[0]);
                        });

                        dropZone.addEventListener("dragover", (e) => { e.preventDefault(); dropZone.classList.add("drag-over"); });
                        dropZone.addEventListener("dragleave", () => dropZone.classList.remove("drag-over"));
                        dropZone.addEventListener("drop", (e) => {
                            e.preventDefault();
                            dropZone.classList.remove("drag-over");
                            if (e.dataTransfer.files.length > 0) loadModel(e.dataTransfer.files[0]);
                        });
                        document.getElementById("model-unit").addEventListener("change", reanalyzeSavedDraft);
                        document.getElementById("dimensions-confirmed").addEventListener("change", () => {
                            updateOrderButtonState();
                            loadSuitableShops();
                        });
                        document.getElementById("shop-search").addEventListener("input", () => { clearTimeout(window.customShopSearchTimer); window.customShopSearchTimer=setTimeout(loadSuitableShops,250); });
                        ["shop-filter-min-price", "shop-filter-price", "shop-filter-days"].forEach(id =>
                            document.getElementById(id).addEventListener("input", renderShopRecommendations));
                        ["shop-filter-rating", "shop-sort"].forEach(id =>
                            document.getElementById(id).addEventListener("change", renderShopRecommendations));

                        document.getElementById("file-remove-btn").addEventListener("click", () => {
                            if (activeUploadRequest) { activeUploadRequest.abort(); activeUploadRequest = null; }
                            if (currentMesh) { scene.remove(currentMesh); currentMesh = null; }
                            document.getElementById("file-info-card").classList.remove("visible");
                            document.getElementById("file-stats").classList.remove("visible");
                            emptyState.classList.remove("hidden");
                            modeTag.textContent = "Chờ file...";
                            modeTag.style.color = "";
                            modeTag.style.background = "";
                            dimLabel.textContent = "—";
                            modelData.loaded = false;
                            currentUploadedFile = null;
                            savedRequestId = null;
                            savedAnalysis = null;
                            selectedShopId = null;
                            selectedShopData = null;
                            if (document.getElementById("btn-order-now")) document.getElementById("btn-order-now").disabled = true;
                            if (document.getElementById("btn-add-cart")) document.getElementById("btn-add-cart").disabled = true;
                            document.getElementById("total-price").textContent = "—";
                            fileInput.value = "";
                            resetPriceDisplay();
                            loadSuitableShops();
                            showToast("Đã xóa file", "info");
                        });

                        document.getElementById("tool-reset").addEventListener("click", () => {
                            if (!modelData.loaded) return;
                            const maxDim = Math.max(modelData.sizeX, modelData.sizeY, modelData.sizeZ);
                            const dist = maxDim * 2.5;
                            camera.position.set(dist * 0.7, dist * 0.5, dist * 0.9);
                            controls.target.set(0, modelData.sizeY / 2, 0);
                            controls.update();
                        });

                        document.getElementById("tool-zoom-in").addEventListener("click", () => {
                            camera.position.multiplyScalar(0.8);
                            controls.update();
                        });

                        document.getElementById("tool-zoom-out").addEventListener("click", () => {
                            camera.position.multiplyScalar(1.25);
                            controls.update();
                        });

                        document.getElementById("tool-wireframe").addEventListener("click", function () {
                            wireframeMode = !wireframeMode;
                            this.classList.toggle("active", wireframeMode);
                            if (currentMesh) currentMesh.traverse(child => {
                                if (child.isMesh && child.material) child.material.wireframe = wireframeMode;
                            });
                        });

                        let selectedTech = "fdm";
                        const techCards = document.querySelectorAll("#tech-options .option-card");
                        const matSelect = document.getElementById("material-select");

                        techCards.forEach(card => {
                            card.addEventListener("click", () => {
                                techCards.forEach(c => c.classList.remove("active"));
                                card.classList.add("active");
                                selectedTech = card.dataset.tech;

                                document.getElementById("mat-fdm").disabled = selectedTech !== "fdm";
                                document.getElementById("mat-sla").disabled = selectedTech !== "sla";
                                document.getElementById("mat-sls").disabled = selectedTech !== "sls";

                                const activeGroup = document.getElementById("mat-" + selectedTech);
                                activeGroup.disabled = false;
                                const firstOpt = activeGroup.querySelector("option");
                                if (firstOpt) matSelect.value = firstOpt.value;

                                updateQuote();
                                loadSuitableShops();
                            });
                        });

                        matSelect.addEventListener("change", () => {
                            updateQuote();
                            loadSuitableShops();
                        });

                        const swatches = document.querySelectorAll("#color-swatches .color-swatch:not(.color-picker-swatch)");
                        swatches.forEach(sw => {
                            sw.addEventListener("click", () => {
                                document.querySelectorAll("#color-swatches .color-swatch").forEach(s => s.classList.remove("active"));
                                sw.classList.add("active");
                                currentColor = sw.dataset.color;
                                if (currentMesh) {
                                    currentMesh.traverse(child => {
                                        if (child.isMesh && child.material?.color) child.material.color.set(currentColor);
                                    });
                                }
                            });
                        });

                        const customColorInput = document.getElementById("custom-color-input");
                        const customColorLabel = document.getElementById("custom-color-label");
                        customColorInput.addEventListener("input", () => {
                            const hex = customColorInput.value;
                            document.querySelectorAll("#color-swatches .color-swatch").forEach(s => s.classList.remove("active"));
                            customColorLabel.classList.add("active");
                            customColorLabel.style.background = hex;
                            customColorLabel.style.color = "white";
                            currentColor = hex;
                            if (currentMesh) {
                                currentMesh.traverse(child => {
                                    if (child.isMesh && child.material?.color) child.material.color.set(currentColor);
                                });
                            }
                        });
                        customColorLabel.addEventListener("click", () => {
                            customColorInput.click();
                        });

                        const resPills = document.querySelectorAll("#resolution-options .pill");
                        resPills.forEach(pill => {
                            pill.addEventListener("click", () => {
                                resPills.forEach(p => p.classList.remove("active"));
                                pill.classList.add("active");
                                updateQuote();
                                loadSuitableShops();
                            });
                        });

                        const infillCards = document.querySelectorAll("#infill-options .infill-card");
                        infillCards.forEach(card => {
                            card.addEventListener("click", () => {
                                infillCards.forEach(c => c.classList.remove("active"));
                                card.classList.add("active");
                                updateQuote();
                                loadSuitableShops();
                            });
                        });

                        const qtyInput = document.getElementById("qty-input");
                        const qtyMinus = document.getElementById("qty-minus");
                        const qtyPlus = document.getElementById("qty-plus");

                        qtyMinus.addEventListener("click", () => {
                            let v = parseInt(qtyInput.value);
                            if (v > 1) { qtyInput.value = v - 1; updateQuote(); loadSuitableShops(); }
                        });
                        qtyPlus.addEventListener("click", () => {
                            let v = parseInt(qtyInput.value);
                            if (v < 100000) { qtyInput.value = v + 1; updateQuote(); loadSuitableShops(); }
                        });
                        qtyInput.addEventListener("input", () => { updateQuote(); loadSuitableShops(); });
                        qtyInput.addEventListener("change", () => {
                            let v = parseInt(qtyInput.value);
                            if (isNaN(v) || v < 1) qtyInput.value = 1;
                            if (v > 100000) qtyInput.value = 100000;
                            updateQuote();
                            loadSuitableShops();
                        });

                        function getSelectedOption(selector) {
                            const el = document.querySelector(selector + " .active");
                            return el;
                        }

                        function formatVND(n) {
                            return new Intl.NumberFormat('vi-VN').format(Math.round(n)) + "đ";
                        }

                        function resetPriceDisplay() {
                            document.getElementById("price-volume").textContent = "— cm³";
                            document.getElementById("price-material-unit").textContent = "— đ/cm³";
                            document.getElementById("price-material").textContent = "— đ";
                            document.getElementById("price-res-factor").textContent = "×1.0";
                            document.getElementById("price-infill-factor").textContent = "×1.0";
                            document.getElementById("price-machine").textContent = "— đ";
                            document.getElementById("discount-row").style.display = "none";
                            document.getElementById("price-per-unit").textContent = "";
                            document.getElementById("saving-tag").style.display = "none";
                        }

                        function escapeHtml(str) {
                            if (!str) return "";
                            return String(str)
                                .replace(/&/g, "&amp;")
                                .replace(/</g, "&lt;")
                                .replace(/>/g, "&gt;")
                                .replace(/"/g, "&quot;")
                                .replace(/'/g, "&#039;");
                        }

                        function selectedLayerHeight() {
                            const active=document.querySelector("#resolution-options .pill.active");
                            return Number(active?.dataset.res)||0.2;
                        }
                        function getShopPrice(shop) {
                            const price = getShopNumber(shop?.estimatedTotal);
                            return Number.isFinite(price) && price > 0 ? price : null;
                        }
                        function getShopNumber(value) {
                            if (value == null || String(value).trim() === "") return null;
                            const number = Number(value);
                            return Number.isFinite(number) ? number : null;
                        }
                        function compareNullableNumbers(left, right, direction = 1) {
                            if (left == null) return right == null ? 0 : 1;
                            if (right == null) return -1;
                            return direction * (left - right);
                        }
                        function readOptionalFilter(id, label) {
                            const raw = document.getElementById(id).value.trim();
                            if (raw === "") return { value: null, error: null };
                            const value = Number(raw);
                            if (!Number.isFinite(value) || value < 0) {
                                return { value: null, error: label + " phải là số không âm." };
                            }
                            return { value, error: null };
                        }
                        function renderShopOptions(shop) {
                            const container = document.getElementById("shop-print-options");
                            if (!container) return false;
                            if (!shop) {
                                container.replaceChildren();
                                container.hidden = true;
                                return false;
                            }

                            const previousColor = selectedColorValue;
                            const previousFinish = selectedFinishValue;
                            const options = (Array.isArray(shop?.options) ? shop.options : []).filter(option =>
                                option && option.active === true
                                && ["COLOR", "FINISH"].includes(String(option.type || "").toUpperCase())
                                && String(option.value || "").trim() !== "");
                            const colors = options.filter(option => String(option.type).toUpperCase() === "COLOR");
                            const finishes = options.filter(option => String(option.type).toUpperCase() === "FINISH");
                            selectedColorValue = colors.some(option => String(option.value) === previousColor) ? previousColor : "";
                            selectedFinishValue = finishes.some(option => String(option.value) === previousFinish) ? previousFinish : "";
                            container.replaceChildren();

                            const groups = [
                                { type: "COLOR", title: "Màu sắc", entries: colors, value: selectedColorValue },
                                { type: "FINISH", title: "Gia công sau in", entries: finishes, value: selectedFinishValue }
                            ].filter(group => group.entries.length > 0);
                            container.hidden = groups.length === 0;
                            if (groups.length > 0) {
                                const grid = document.createElement("div");
                                grid.className = "shop-print-option-grid";
                                groups.forEach(group => {
                                    const label = document.createElement("label");
                                    label.append(document.createTextNode(group.title));
                                    const select = document.createElement("select");
                                    select.name = group.type === "COLOR" ? "color" : "finish";
                                    const none = document.createElement("option");
                                    none.value = "";
                                    none.textContent = "Không yêu cầu";
                                    select.append(none);
                                    group.entries.forEach(entry => {
                                        const option = document.createElement("option");
                                        option.value = String(entry.value);
                                        option.textContent = String(entry.label || entry.value);
                                        select.append(option);
                                    });
                                    select.value = group.value;
                                    select.addEventListener("change", () => {
                                        if (group.type === "COLOR") selectedColorValue = select.value;
                                        else selectedFinishValue = select.value;
                                        loadSuitableShops();
                                    });
                                    label.append(select);
                                    grid.append(label);
                                });
                                container.append(grid);
                            }
                            return previousColor !== selectedColorValue || previousFinish !== selectedFinishValue;
                        }
                        function resetShopRecommendations() {
                            shopRecommendationVersion++;
                            if (shopRecommendationController) shopRecommendationController.abort();
                            shopRecommendationController = null;
                            recommendedShops = [];
                            isLoadingShopRecommendations = false;
                            selectedShopId = null;
                            selectedShopData = null;
                            selectedColorValue = "";
                            selectedFinishValue = "";
                            renderShopOptions(null);
                            const list = document.getElementById("suitable-shops-list");
                            if (list) list.innerHTML = '<div class="shop-empty-state">Chọn tệp OBJ để tìm xưởng phù hợp.</div>';
                            updateQuote();
                        }
                        function updateOrderButtonState() {
                            const directButton=document.getElementById("btn-order-now");
                            const quoteButton=document.getElementById("btn-quote-request");
                            const dimensionsConfirmed=savedAnalysis?.status!=="NEEDS_CONFIRMATION"
                                ||document.getElementById("dimensions-confirmed").checked;
                            const hasSelectedShop=Boolean(savedRequestId&&selectedShopId&&selectedShopData
                                &&Number(selectedShopData.shopId)===Number(selectedShopId));
                            const hasSelection=Boolean(!isLoadingShopRecommendations&&hasSelectedShop&&dimensionsConfirmed);
                            if(directButton)directButton.disabled=!(hasSelection&&getShopPrice(selectedShopData)!=null);
                            if(quoteButton)quoteButton.disabled=!hasSelection;
                        }
                        function updateQuote() {
                            document.getElementById("price-volume").textContent=modelData.loaded?modelData.volume.toFixed(3)+" cm³":"Chờ phân tích máy chủ";
                            document.getElementById("price-material-unit").textContent="Theo bảng giá của xưởng";
                            document.getElementById("price-material").textContent="—";
                            document.getElementById("price-res-factor").textContent="Theo cấu hình xưởng";
                            document.getElementById("price-infill-factor").textContent="Theo cấu hình xưởng";
                            document.getElementById("price-machine").textContent="Theo cấu hình xưởng";
                            document.getElementById("discount-row").style.display="none";
                            document.getElementById("saving-tag").style.display="none";
                            document.getElementById("price-per-unit").textContent="Giá ước tính, không phải kết quả slicer.";
                            const amount=getShopPrice(selectedShopData);
                            document.getElementById("total-price").textContent=isLoadingShopRecommendations
                                ?"Đang cập nhật danh sách xưởng…"
                                :amount==null?(selectedShopData?"Cần xưởng báo giá":"Chọn xưởng để xem giá"):formatVND(amount);

                            updateOrderButtonState();
                        }
                        function loadSuitableShops() {
                            const list=document.getElementById("suitable-shops-list");
                            if(!savedRequestId){resetShopRecommendations();return;}
                            if(shopRecommendationController)shopRecommendationController.abort();
                            const version=++shopRecommendationVersion;
                            const controller=new AbortController();
                            shopRecommendationController=controller;
                            isLoadingShopRecommendations=true;
                            selectedShopData=null;
                            list.innerHTML='<div class="shop-loading-card"><span>Đang lọc xưởng theo vật liệu, công nghệ, kích thước và bảng giá…</span></div>';
                            updateQuote();
                            const params=new URLSearchParams({action:"suitableShops",requestId:savedRequestId,tech:selectedTech.toUpperCase(),material:String(matSelect.value).toUpperCase(),
                                infill:document.querySelector("#infill-options .infill-card.active")?.dataset.infill||"20",layerHeight:String(selectedLayerHeight()),
                                quantity:qtyInput.value||"1",color:selectedColorValue,finish:selectedFinishValue,q:document.getElementById("shop-search").value||"",
                                dimensionsConfirmed:String(document.getElementById("dimensions-confirmed").checked)});
                            fetch(contextPath+"/design?"+params,{credentials:"same-origin",cache:"no-store",signal:controller.signal})
                                .then(async response=>{
                                    let data;
                                    try{data=await response.json();}catch(_){throw new Error("Máy chủ trả về danh sách xưởng không hợp lệ.");}
                                    if(!response.ok)throw new Error(data.message||data.error||"Không thể tìm xưởng.");
                                    if(!Array.isArray(data))throw new Error(data.message||"Dữ liệu danh sách xưởng không hợp lệ.");
                                    return data;
                                })
                                .then(shops=>{
                                    if(version!==shopRecommendationVersion)return;
                                    shopRecommendationController=null;
                                    isLoadingShopRecommendations=false;
                                    recommendedShops=shops;
                                    renderShopRecommendations();
                                }).catch(error=>{
                                    if(version!==shopRecommendationVersion||error.name==="AbortError")return;
                                    shopRecommendationController=null;
                                    isLoadingShopRecommendations=false;
                                    recommendedShops=[];
                                    selectedShopId=null;
                                    selectedShopData=null;
                                    renderShopOptions(null);
                                    list.innerHTML='<div class="shop-error-state">'+escapeHtml(error.message||"Không thể tải danh sách xưởng.")+'</div>';
                                    updateQuote();
                                });
                        }
                        function renderShopRecommendations() {
                            if(isLoadingShopRecommendations||!savedRequestId)return;
                            const list=document.getElementById("suitable-shops-list");
                            const minPrice=readOptionalFilter("shop-filter-min-price","Giá tối thiểu");
                            const maxPrice=readOptionalFilter("shop-filter-price","Giá tối đa");
                            const maxDays=readOptionalFilter("shop-filter-days","Thời gian tối đa");
                            const minRating=Number(document.getElementById("shop-filter-rating").value);
                            const validationError=minPrice.error||maxPrice.error||maxDays.error
                                ||(minRating<0||minRating>5||!Number.isFinite(minRating)?"Mức đánh giá không hợp lệ.":null)
                                ||(minPrice.value!=null&&maxPrice.value!=null&&minPrice.value>maxPrice.value
                                    ?"Giá tối thiểu không được lớn hơn giá tối đa.":null);
                            if(validationError){
                                selectedShopId=null;selectedShopData=null;renderShopOptions(null);
                                list.innerHTML='<div class="shop-error-state">'+escapeHtml(validationError)+'</div>';
                                updateQuote();return;
                            }
                            let shops=recommendedShops.filter(shop=>{
                                const price=getShopPrice(shop);
                                if((minPrice.value!=null||maxPrice.value!=null)&&price==null)return false;
                                if(minPrice.value!=null&&price<minPrice.value)return false;
                                if(maxPrice.value!=null&&price>maxPrice.value)return false;
                                if(maxDays.value!=null){
                                    const days=getShopNumber(shop.productionDays);
                                    if(days==null||days>maxDays.value)return false;
                                }
                                if(minRating>0){
                                    const rating=getShopNumber(shop.rating);
                                    if(rating==null||rating<minRating)return false;
                                }
                                return true;
                            });
                            const sort=document.getElementById("shop-sort").value;
                            if(sort==="price")shops.sort((a,b)=>compareNullableNumbers(getShopPrice(a),getShopPrice(b)));
                            else if(sort==="price-desc")shops.sort((a,b)=>compareNullableNumbers(getShopPrice(a),getShopPrice(b),-1));
                            else if(sort==="time")shops.sort((a,b)=>compareNullableNumbers(getShopNumber(a.productionDays),getShopNumber(b.productionDays)));
                            else if(sort==="distance")shops.sort((a,b)=>compareNullableNumbers(getShopNumber(a.distanceKm),getShopNumber(b.distanceKm)));
                            else if(sort==="rating")shops.sort((a,b)=>compareNullableNumbers(getShopNumber(a.rating),getShopNumber(b.rating),-1));
                            if(!shops.length){
                                selectedShopId=null;selectedShopData=null;renderShopOptions(null);
                                const filtered=(minPrice.value!=null||maxPrice.value!=null||maxDays.value!=null||minRating>0);
                                list.innerHTML='<div class="shop-empty-state">'+(filtered
                                    ?"Không có xưởng nào đáp ứng bộ lọc đã chọn. Hãy điều chỉnh bộ lọc để xem thêm xưởng."
                                    :"Không có xưởng nào đủ điều kiện kỹ thuật cho cấu hình này.")+'</div>';
                                updateQuote();return;
                            }
                            let selected=shops.find(shop=>Number(shop.shopId)===Number(selectedShopId));
                            if(!selected){selected=shops[0];selectedShopId=Number(selected.shopId);}
                            selectedShopData=selected;
                            list.innerHTML=shops.map(shop=>{
                                const price=getShopPrice(shop);
                                const rating=getShopNumber(shop.rating);
                                const distance=getShopNumber(shop.distanceKm);
                                const name=String(shop.shopName||"X");
                                const days=getShopNumber(shop.productionDays);
                                const maximumQuantity=getShopNumber(shop.maximumQuantity);
                                return '<div class="shop-card '+(Number(shop.shopId)===Number(selectedShopId)?'selected':'')+'" data-shop-id="'+Number(shop.shopId)+'"><div class="shop-card-content"><div class="shop-card-header"><span class="shop-avatar">'+escapeHtml(name.trim().slice(0,1).toUpperCase())+'</span><span class="shop-card-name">'+escapeHtml(name)+'</span><span class="shop-verified-badge">Đã duyệt</span></div><div class="shop-card-meta"><span>'+(rating!=null?'★ '+rating.toFixed(1):'Chưa có đánh giá')+' ('+Number(shop.reviewCount||0)+' đánh giá)</span><span>'+escapeHtml(shop.location||"")+'</span><span>'+(distance!=null?distance.toFixed(1)+' km':'Khoảng cách chưa có dữ liệu')+'</span><span>Dự kiến '+(days!=null?days:'Chưa xác định')+' ngày</span></div><span class="shop-price-estimate">'+(price==null?'Cần xưởng kiểm tra trước khi báo giá':formatVND(price))+'<small>'+escapeHtml(shop.estimateDisclaimer||"")+'</small></span><small>Tối đa: '+escapeHtml(shop.maxBuildDimensions||"chưa cấu hình")+'</small><small>Nhận tối đa '+(maximumQuantity!=null?maximumQuantity:'Chưa cấu hình')+' sản phẩm/yêu cầu</small></div></div>';
                            }).join("");
                            list.querySelectorAll(".shop-card").forEach(card=>card.addEventListener("click",()=>{
                                const next=shops.find(shop=>Number(shop.shopId)===Number(card.dataset.shopId));
                                if(!next)return;
                                selectedShopId=Number(next.shopId);selectedShopData=next;
                                list.querySelectorAll(".shop-card").forEach(item=>item.classList.toggle("selected",item===card));
                                const optionsChanged=renderShopOptions(next);
                                updateQuote();
                                if(optionsChanged)loadSuitableShops();
                            }));
                            const optionsChanged=renderShopOptions(selected);
                            updateQuote();
                            if(optionsChanged)loadSuitableShops();
                        }
                        function submitCustomPrint(quote) {
                            if(!savedRequestId||!selectedShopId){showToast("Hãy tải mô hình và chọn xưởng trước.","error");return;}
                            const button=document.getElementById(quote?"btn-quote-request":"btn-order-now"),old=button.innerHTML;button.disabled=true;button.textContent=quote?"Đang gửi yêu cầu…":"Đang tính lại giá…";
                            const body=new URLSearchParams({action:quote?"submitQuote":"submitDirect",requestId:savedRequestId,shopId:selectedShopId,
                                technology:selectedTech.toUpperCase(),material:String(matSelect.value).toUpperCase(),
                                infill:document.querySelector("#infill-options .infill-card.active")?.dataset.infill||"20",layerHeight:String(selectedLayerHeight()),
                                quantity:qtyInput.value||"1",color:selectedColorValue,finish:selectedFinishValue,
                                dimensionsConfirmed:String(document.getElementById("dimensions-confirmed").checked),note:document.getElementById("customer-note")?.value||""});
                            fetch(contextPath+"/design",{method:"POST",headers:{"X-CSRF-Token":csrfToken,"Content-Type":"application/x-www-form-urlencoded;charset=UTF-8","Accept":"application/json"},body})
                                .then(async response=>{const data=await response.json();if(!response.ok||!data.success)throw new Error(data.message||"Không thể gửi yêu cầu.");return data;})
                                .then(data=>{window.location.assign(data.redirectUrl);})
                                .catch(error=>{showToast(error.message,"error");button.disabled=false;button.innerHTML=old;});
                        }
                        const directButton=document.getElementById("btn-order-now");
                        if(directButton)directButton.addEventListener("click",event=>{event.preventDefault();event.stopImmediatePropagation();submitCustomPrint(false);},true);
                        const quoteButton=document.getElementById("btn-quote-request");
                        if(quoteButton)quoteButton.addEventListener("click",()=>submitCustomPrint(true));

                        const TEMPLATES = {
                            standard: {
                                tech: "fdm",
                                material: "pla",
                                color: "#F5F5F5",
                                resolution: "0.2",
                                infill: "20"
                            },
                            highdetail: {
                                tech: "sla",
                                material: "resin-std",
                                color: "#DDA0DD",
                                resolution: "0.12",
                                infill: "50"
                            },
                            flexible: {
                                tech: "fdm",
                                material: "tpu",
                                color: "#00B894",
                                resolution: "0.2",
                                infill: "15"
                            },
                            industrial: {
                                tech: "sls",
                                material: "nylon-pa12",
                                color: "#1B1B2F",
                                resolution: "0.12",
                                infill: "100"
                            },
                            fastdraft: {
                                tech: "fdm",
                                material: "pla",
                                color: "#FFB347",
                                resolution: "0.28",
                                infill: "15"
                            },
                            transparent: {
                                tech: "sla",
                                material: "resin-clear",
                                color: "#4A90D9",
                                resolution: "0.12",
                                infill: "50"
                            }
                        };

                        function applyTemplate(tplKey) {
                            const tpl = TEMPLATES[tplKey];
                            if (!tpl) return;

                            techCards.forEach(c => c.classList.remove("active"));
                            const techCard = document.querySelector('#tech-options .option-card[data-tech="' + tpl.tech + '"]');
                            if (techCard) techCard.classList.add("active");
                            selectedTech = tpl.tech;

                            ["fdm", "sla", "sls"].forEach(t => {
                                const g = document.getElementById("mat-" + t);
                                if (g) g.disabled = (t !== tpl.tech);
                            });

                            matSelect.value = tpl.material;

                            currentColor = tpl.color;
                            document.querySelectorAll("#color-swatches .color-swatch").forEach(s => s.classList.remove("active"));
                            const matchSwatch = document.querySelector('#color-swatches .color-swatch[data-color="' + tpl.color + '"]');
                            if (matchSwatch) {
                                matchSwatch.classList.add("active");
                            } else {
                                
                                customColorInput.value = tpl.color;
                                customColorLabel.classList.add("active");
                                customColorLabel.style.background = tpl.color;
                                customColorLabel.style.color = "white";
                            }
                            if (currentMesh) currentMesh.traverse(child => {
                                if (child.isMesh && child.material?.color) child.material.color.set(currentColor);
                            });

                            resPills.forEach(p => {
                                p.classList.toggle("active", p.dataset.res === tpl.resolution);
                            });

                            infillCards.forEach(c => {
                                c.classList.toggle("active", String(c.dataset.infill) === String(tpl.infill));
                            });

                            updateQuote();
                            loadSuitableShops();
                        }

                        const openFullBtn = document.querySelector(".open-full");
                        if (openFullBtn) {
                            openFullBtn.addEventListener("click", (e) => {
                                e.preventDefault();
                                const viewer = document.querySelector(".viewer-panel");
                                if (!viewer) return;
                                const isFull = viewer.classList.toggle("fullscreen-mode");
                                if (isFull) {
                                    openFullBtn.innerHTML = '<i class="fas fa-compress"></i> Thu nhỏ màn hình';
                                    controls.enableZoom = true;
                                    document.body.style.overflow = "hidden";
                                } else {
                                    openFullBtn.innerHTML = '<i class="fas fa-expand"></i> Mở toàn màn hình';
                                    controls.enableZoom = false;
                                    document.body.style.overflow = "";
                                }
                            });
                        }

                        document.getElementById("btn-open-templates").addEventListener("click", () => {
                            document.getElementById("tpl-overlay").classList.add("active");
                            document.body.style.overflow = "hidden";
                        });

                        function closeTemplateModal() {
                            const overlay = document.getElementById("tpl-overlay");
                            if (overlay) overlay.classList.remove("active");
                            document.body.style.overflow = "";
                        }
                        document.getElementById("tpl-close-btn").addEventListener("click", closeTemplateModal);
                        document.getElementById("tpl-overlay").addEventListener("click", (e) => {
                            if (e.target === document.getElementById("tpl-overlay")) closeTemplateModal();
                        });

                        document.querySelectorAll(".tpl-card").forEach(card => {
                            const applyBtn = card.querySelector(".tpl-apply-btn");
                            if (applyBtn) {
                                applyBtn.addEventListener("click", (e) => {
                                    e.stopPropagation();
                                    applyTemplate(card.dataset.tpl);
                                    closeTemplateModal();
                                    showToast("Đã áp dụng mẫu: " + card.querySelector("h3").textContent, "success");
                                });
                            }
                            card.addEventListener("click", () => {
                                document.querySelectorAll(".tpl-card").forEach(c => c.classList.remove("selected"));
                                card.classList.add("selected");
                                applyTemplate(card.dataset.tpl);
                                closeTemplateModal();
                                showToast("Đã áp dụng mẫu: " + card.querySelector("h3").textContent, "success");
                            });
                        });

                        // Khởi tạo danh sách xưởng in phù hợp khi tải trang
                        loadExistingDraft();

                    });

                    document.addEventListener("keydown", (e) => {
                        if (e.key === "Escape") {
                            const overlay = document.getElementById("tpl-overlay");
                            if (overlay && overlay.classList.contains("active")) {
                                overlay.classList.remove("active");
                                document.body.style.overflow = "";
                            }
                            const viewer = document.querySelector(".viewer-panel");
                            if (viewer && viewer.classList.contains("fullscreen-mode")) {
                                viewer.classList.remove("fullscreen-mode");
                                const btn = document.querySelector(".open-full");
                                if (btn) btn.innerHTML = '<i class="fas fa-expand"></i> Mở toàn màn hình';
                                document.body.style.overflow = "";
                            }
                            return;
                        }

                        if (e.key === "t" || e.key === "T") {
                            if (document.activeElement.tagName === "INPUT" || document.activeElement.tagName === "TEXTAREA") return;
                            const overlay = document.getElementById("tpl-overlay");
                            if (overlay) {
                                const willOpen = !overlay.classList.contains("active");
                                overlay.classList.toggle("active", willOpen);
                                document.body.style.overflow = willOpen ? "hidden" : "";
                            }
                        }
                    });
                </script>
    </body>

    </html>
