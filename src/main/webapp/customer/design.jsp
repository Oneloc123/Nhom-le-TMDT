<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

    <!DOCTYPE html>
    <html lang="vi">

    <head>
        <meta charset="UTF-8">
        <title>PrintKraft — Xưởng In 3D Trực Tuyến</title>
        <meta name="description"
            content="Tải lên file 3D, tùy chỉnh thông số in và nhận báo giá tự động. Hỗ trợ STL, OBJ, STEP.">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <link rel="preconnect" href="https://fonts.googleapis.com">
        <link
            href="https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;500;600;700&family=IBM+Plex+Sans:wght@400;500;600;700&display=swap"
            rel="stylesheet">
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_design.css?v=5">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_header.css?v=2">

        <script src="https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js"></script>
        <script src="https://cdn.jsdelivr.net/npm/three@0.128.0/examples/js/controls/OrbitControls.js"></script>
        <script src="https://cdn.jsdelivr.net/npm/three@0.128.0/examples/js/loaders/STLLoader.js"></script>
        <script src="https://cdn.jsdelivr.net/npm/three@0.128.0/examples/js/loaders/OBJLoader.js"></script>
    </head>

    <body>

        <%@ include file="header.jsp" %>

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
                                    <span class="file-type-badge">.STL</span>
                                    <span class="file-type-badge">.OBJ</span>
                                    <span class="file-type-badge">.STEP</span>
                                </div>
                                <p style="margin-top:8px; font-size:11px; color:var(--ink-soft);">Tối đa 100MB mỗi file
                                </p>
                            </div>
                            <input type="file" class="drop-zone-input" id="file-input" accept=".stl,.obj,.step,.stp">
                        </div>

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
                                    <div class="file-name" id="file-name">model.stl</div>
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
                                <div class="stat-label">Bounding Box</div>
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
                                    <input type="number" class="qty-input" id="qty-input" value="1" min="1" max="100">
                                    <button class="qty-btn" id="qty-plus"><i class="fas fa-plus"></i></button>
                                </div>
                                <span class="qty-note">Giảm giá khi mua từ 5 bản</span>
                            </div>
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

                            <button class="btn-add-cart" id="btn-add-cart" disabled>
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

                        function showToast(msg, type = "info") {
                            const c = document.getElementById("toast-container");
                            const t = document.createElement("div");
                            t.className = "toast " + type;
                            t.innerHTML = '<i class="fas fa-' + (type === 'success' ? 'check-circle' : type === 'error' ? 'exclamation-circle' : 'info-circle') + '"></i>' + msg;
                            c.appendChild(t);
                            setTimeout(() => { t.classList.add("fadeOut"); setTimeout(() => t.remove(), 300); }, 3500);
                        }

                        const TECH_MULTIPLIER = { fdm: 1.0, sla: 1.8, sls: 2.5 };
                        const MACHINE_COST_PER_CM3 = { fdm: 500, sla: 1200, sls: 2000 };
                        
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

                        function computeSTLVolume(geometry) {
                            const pos = geometry.attributes.position;
                            let totalVolume = 0;
                            const v1 = new THREE.Vector3(), v2 = new THREE.Vector3(), v3 = new THREE.Vector3();
                            for (let i = 0; i < pos.count; i += 3) {
                                v1.fromBufferAttribute(pos, i);
                                v2.fromBufferAttribute(pos, i + 1);
                                v3.fromBufferAttribute(pos, i + 2);
                                
                                totalVolume += v1.dot(v2.cross(v3)) / 6.0;
                            }
                            return Math.abs(totalVolume);
                        }

                        function loadModel(file) {
                            const ext = file.name.split('.').pop().toLowerCase();

                            if (!['stl', 'obj', 'step', 'stp'].includes(ext)) {
                                showToast("Định dạng không được hỗ trợ. Chỉ chấp nhận .stl, .obj, .step", "error");
                                return;
                            }

                            const maxSize = 100 * 1024 * 1024; 
                            if (file.size > maxSize) {
                                showToast("File quá lớn! Giới hạn tối đa 100MB.", "error");
                                return;
                            }

                            const progressEl = document.getElementById("upload-progress");
                            const progressFill = document.getElementById("progress-fill");
                            const progressText = document.getElementById("progress-text");
                            progressEl.classList.add("visible");
                            progressFill.style.width = "0%";

                            let progress = 0;
                            const progressInterval = setInterval(() => {
                                progress += Math.random() * 20 + 5;
                                if (progress > 90) progress = 90;
                                progressFill.style.width = progress + "%";
                                progressText.textContent = "Đang xử lý... " + Math.round(progress) + "%";
                            }, 150);

                            loadingOverlay.classList.add("visible");
                            emptyState.classList.add("hidden");
                            modeTag.textContent = "Đang tải...";
                            modeTag.style.color = "var(--amber)";
                            modeTag.style.background = "rgba(255,179,71,0.1)";

                            document.getElementById("file-info-card").classList.add("visible");
                            document.getElementById("file-name").textContent = file.name;
                            const sizeMB = (file.size / (1024 * 1024)).toFixed(2);
                            document.getElementById("file-size-display").textContent = sizeMB + " MB";

                            const reader = new FileReader();
                            reader.onload = function (e) {

                                if (currentMesh) {
                                    scene.remove(currentMesh);
                                    currentMesh = null;
                                }

                                let geometry = null;

                                try {
                                    if (ext === 'stl') {
                                        const loader = new THREE.STLLoader();
                                        geometry = loader.parse(e.target.result);
                                    } else if (ext === 'obj') {
                                        const loader = new THREE.OBJLoader();
                                        const text = new TextDecoder().decode(new Uint8Array(e.target.result));
                                        const obj = loader.parse(text);
                                        
                                        obj.traverse(child => {
                                            if (child.isMesh && !geometry) {
                                                geometry = child.geometry;
                                            }
                                        });
                                        if (!geometry) throw new Error("Không tìm thấy mesh trong file OBJ");
                                    } else if (ext === 'step' || ext === 'stp') {
                                        showToast("File STEP đã nhận. Đang hiển thị dạng bounding box.", "info");
                                        
                                        const size = Math.pow(file.size / 1000, 1 / 3) * 2;
                                        geometry = new THREE.BoxGeometry(size, size, size);
                                    }

                                    if (!geometry) throw new Error("Không thể đọc file");

                                    geometry.computeBoundingBox();
                                    geometry.computeVertexNormals();

                                    const material = new THREE.MeshStandardMaterial({
                                        color: new THREE.Color(currentColor),
                                        metalness: 0.15,
                                        roughness: 0.55,
                                        flatShading: false
                                    });

                                    currentMesh = new THREE.Mesh(geometry, material);
                                    currentMesh.castShadow = true;
                                    currentMesh.receiveShadow = true;

                                    const bbox = geometry.boundingBox;
                                    const center = new THREE.Vector3();
                                    bbox.getCenter(center);
                                    currentMesh.position.sub(center);
                                    currentMesh.position.y += (bbox.max.y - bbox.min.y) / 2;

                                    scene.add(currentMesh);

                                    const sizeX = bbox.max.x - bbox.min.x;
                                    const sizeY = bbox.max.y - bbox.min.y;
                                    const sizeZ = bbox.max.z - bbox.min.z;
                                    const triangles = geometry.attributes.position.count / 3;

                                    let volume = 0;
                                    if (ext === 'stl') {
                                        volume = computeSTLVolume(geometry) / 1000; 
                                    } else {
                                        
                                        volume = (sizeX * sizeY * sizeZ * 0.4) / 1000;
                                    }

                                    const bboxVol = (sizeX * sizeY * sizeZ) / 1000; 

                                    modelData = {
                                        volume: Math.max(volume, 0.1),
                                        triangles,
                                        sizeX, sizeY, sizeZ,
                                        bboxVol,
                                        loaded: true
                                    };

                                    document.getElementById("stat-volume").innerHTML = modelData.volume.toFixed(1) + ' <span class="stat-unit">cm³</span>';
                                    document.getElementById("stat-triangles").textContent = triangles.toLocaleString('vi-VN');
                                    document.getElementById("stat-size-x").innerHTML = sizeX.toFixed(1) + ' <span class="stat-unit">mm</span>';
                                    document.getElementById("stat-size-y").innerHTML = sizeY.toFixed(1) + ' <span class="stat-unit">mm</span>';
                                    document.getElementById("stat-size-z").innerHTML = sizeZ.toFixed(1) + ' <span class="stat-unit">mm</span>';
                                    document.getElementById("stat-bbox").innerHTML = bboxVol.toFixed(1) + ' <span class="stat-unit">cm³</span>';
                                    document.getElementById("file-stats").classList.add("visible");

                                    dimLabel.textContent = sizeX.toFixed(1) + " × " + sizeY.toFixed(1) + " × " + sizeZ.toFixed(1) + " mm";

                                    const maxDim = Math.max(sizeX, sizeY, sizeZ);
                                    const dist = maxDim * 2.5;
                                    camera.position.set(dist * 0.7, dist * 0.5, dist * 0.9);
                                    controls.target.set(0, sizeY / 2, 0);
                                    controls.update();

                                    modeTag.textContent = "Đã tải ✓";
                                    modeTag.style.color = "var(--teal)";
                                    modeTag.style.background = "rgba(0,184,148,0.08)";

                                    document.getElementById("btn-add-cart").disabled = false;

                                    showToast("Mô hình đã được tải thành công!", "success");

                                    updateQuote();

                                } catch (err) {
                                    console.error(err);
                                    showToast("Lỗi khi đọc file: " + err.message, "error");
                                    modeTag.textContent = "Lỗi ✗";
                                    modeTag.style.color = "var(--coral)";
                                    modeTag.style.background = "rgba(255,107,107,0.08)";
                                }

                                clearInterval(progressInterval);
                                progressFill.style.width = "100%";
                                progressText.textContent = "Hoàn tất!";
                                setTimeout(() => {
                                    progressEl.classList.remove("visible");
                                    loadingOverlay.classList.remove("visible");
                                }, 600);
                            };

                            if (ext === 'obj') {
                                reader.readAsArrayBuffer(file);
                            } else {
                                reader.readAsArrayBuffer(file);
                            }
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

                        document.getElementById("file-remove-btn").addEventListener("click", () => {
                            if (currentMesh) { scene.remove(currentMesh); currentMesh = null; }
                            document.getElementById("file-info-card").classList.remove("visible");
                            document.getElementById("file-stats").classList.remove("visible");
                            emptyState.classList.remove("hidden");
                            modeTag.textContent = "Chờ file...";
                            modeTag.style.color = "";
                            modeTag.style.background = "";
                            dimLabel.textContent = "—";
                            modelData.loaded = false;
                            document.getElementById("btn-add-cart").disabled = true;
                            document.getElementById("total-price").textContent = "—";
                            fileInput.value = "";
                            resetPriceDisplay();
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
                            if (currentMesh) currentMesh.material.wireframe = wireframeMode;
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
                            });
                        });

                        matSelect.addEventListener("change", updateQuote);

                        const swatches = document.querySelectorAll("#color-swatches .color-swatch:not(.color-picker-swatch)");
                        swatches.forEach(sw => {
                            sw.addEventListener("click", () => {
                                document.querySelectorAll("#color-swatches .color-swatch").forEach(s => s.classList.remove("active"));
                                sw.classList.add("active");
                                currentColor = sw.dataset.color;
                                if (currentMesh) {
                                    currentMesh.material.color.set(currentColor);
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
                                currentMesh.material.color.set(currentColor);
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
                            });
                        });

                        const infillCards = document.querySelectorAll("#infill-options .infill-card");
                        infillCards.forEach(card => {
                            card.addEventListener("click", () => {
                                infillCards.forEach(c => c.classList.remove("active"));
                                card.classList.add("active");
                                updateQuote();
                            });
                        });

                        const qtyInput = document.getElementById("qty-input");
                        const qtyMinus = document.getElementById("qty-minus");
                        const qtyPlus = document.getElementById("qty-plus");

                        qtyMinus.addEventListener("click", () => {
                            let v = parseInt(qtyInput.value);
                            if (v > 1) { qtyInput.value = v - 1; updateQuote(); }
                        });
                        qtyPlus.addEventListener("click", () => {
                            let v = parseInt(qtyInput.value);
                            if (v < 100) { qtyInput.value = v + 1; updateQuote(); }
                        });
                        qtyInput.addEventListener("input", updateQuote);
                        qtyInput.addEventListener("change", () => {
                            let v = parseInt(qtyInput.value);
                            if (isNaN(v) || v < 1) qtyInput.value = 1;
                            if (v > 100) qtyInput.value = 100;
                            updateQuote();
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

                        function updateQuote() {
                            if (!modelData.loaded) return;

                            const volume = modelData.volume; 

                            const selectedMat = matSelect.options[matSelect.selectedIndex];
                            const matPricePerCm3 = parseFloat(selectedMat.dataset.price || 1800);

                            const activeRes = document.querySelector("#resolution-options .pill.active");
                            const resFactor = parseFloat(activeRes ? activeRes.dataset.factor : 1.0);

                            const activeInfill = document.querySelector("#infill-options .infill-card.active");
                            const infillFactor = parseFloat(activeInfill ? activeInfill.dataset.factor : 1.0);

                            let qty = parseInt(qtyInput.value);
                            if (isNaN(qty) || qty < 1) qty = 1;

                            const techMul = TECH_MULTIPLIER[selectedTech] || 1.0;

                            const machineCostPerCm3 = MACHINE_COST_PER_CM3[selectedTech] || 500;
                            const machineCost = volume * machineCostPerCm3 * resFactor;

                            const materialCost = volume * matPricePerCm3 * infillFactor;

                            let unitPrice = (materialCost + machineCost) * techMul;

                            if (unitPrice < 25000) unitPrice = 25000;

                            let discountRate = 0;
                            if (qty >= 20) discountRate = 0.15;
                            else if (qty >= 10) discountRate = 0.10;
                            else if (qty >= 5) discountRate = 0.05;

                            const totalBeforeDiscount = unitPrice * qty;
                            const discountAmount = totalBeforeDiscount * discountRate;
                            const total = totalBeforeDiscount - discountAmount;

                            document.getElementById("price-volume").textContent = volume.toFixed(1) + " cm³";
                            document.getElementById("price-material-unit").textContent = formatVND(matPricePerCm3) + "/cm³";
                            document.getElementById("price-material").textContent = formatVND(materialCost);
                            document.getElementById("price-res-factor").textContent = "×" + resFactor.toFixed(2);
                            document.getElementById("price-infill-factor").textContent = "×" + infillFactor.toFixed(2);
                            document.getElementById("price-machine").textContent = formatVND(machineCost);

                            if (discountRate > 0) {
                                document.getElementById("discount-row").style.display = "flex";
                                document.getElementById("price-discount").textContent = "-" + formatVND(discountAmount);
                                document.getElementById("saving-tag").style.display = "inline-block";
                                document.getElementById("saving-tag").textContent = "Tiết kiệm " + (discountRate * 100) + "% khi mua " + qty + " bản";
                            } else {
                                document.getElementById("discount-row").style.display = "none";
                                document.getElementById("saving-tag").style.display = "none";
                            }

                            const totalEl = document.getElementById("total-price");
                            totalEl.textContent = formatVND(total);
                            totalEl.style.transform = "scale(1.08)";
                            setTimeout(() => { totalEl.style.transition = "transform 0.3s"; totalEl.style.transform = "scale(1)"; }, 100);

                            if (qty > 1) {
                                document.getElementById("price-per-unit").textContent = "≈ " + formatVND(unitPrice * (1 - discountRate)) + " / bản";
                            } else {
                                document.getElementById("price-per-unit").textContent = "";
                            }
                        }

                        document.getElementById("btn-add-cart").addEventListener("click", () => {
                            if (!modelData.loaded) {
                                showToast("Vui lòng tải lên file 3D trước", "error");
                                return;
                            }
                            showToast("Đã thêm vào giỏ hàng thành công!", "success");

                            const badge = document.getElementById("cartBadge");
                            if (badge) {
                                const current = parseInt(badge.textContent) || 0;
                                badge.textContent = current + 1;
                            }
                        });

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
                            const techCard = document.querySelector(`#tech-options .option-card[data-tech="${tpl.tech}"]`);
                            if (techCard) techCard.classList.add("active");
                            selectedTech = tpl.tech;

                            ["fdm", "sla", "sls"].forEach(t => {
                                const g = document.getElementById("mat-" + t);
                                if (g) g.disabled = (t !== tpl.tech);
                            });

                            matSelect.value = tpl.material;

                            currentColor = tpl.color;
                            document.querySelectorAll("#color-swatches .color-swatch").forEach(s => s.classList.remove("active"));
                            const matchSwatch = document.querySelector(`#color-swatches .color-swatch[data-color="${tpl.color}"]`);
                            if (matchSwatch) {
                                matchSwatch.classList.add("active");
                            } else {
                                
                                customColorInput.value = tpl.color;
                                customColorLabel.classList.add("active");
                                customColorLabel.style.background = tpl.color;
                                customColorLabel.style.color = "white";
                            }
                            if (currentMesh) currentMesh.material.color.set(currentColor);

                            resPills.forEach(p => {
                                p.classList.toggle("active", p.dataset.res === tpl.resolution);
                            });

                            infillCards.forEach(c => {
                                c.classList.toggle("active", String(c.dataset.infill) === String(tpl.infill));
                            });

                            updateQuote();
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