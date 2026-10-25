<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
    <%@ taglib prefix="c" uri="jakarta.tags.core" %>
        <%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
            <!DOCTYPE html>
            <html lang="vi">

            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>${isEdit ? 'Chỉnh sửa đánh giá' : 'Đánh giá sản phẩm đã mua'} - PrintKraft</title>
                <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_home.css">
                <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shop.css">
            </head>

            <body>
                <%@ include file="header.jsp" %>

                    <main class="shop-page">
                        <div class="shop-heading">
                            <div>
                                <div class="eyebrow-line">Đơn hàng #${item.orderCode}</div>
                                <h1>${isEdit ? 'Chỉnh sửa đánh giá sản phẩm' : 'Đánh giá sản phẩm đã mua'}</h1>
                                <p>${isEdit ? 'Bạn có thể cập nhật lại số sao và nhận xét của mình bất cứ lúc nào.' :
                                    'Chia sẻ trải nghiệm thực tế để giúp cộng đồng mua sắm tốt hơn.'}</p>
                            </div>
                            <div class="button-row">
                                <a class="button-secondary"
                                    href="${pageContext.request.contextPath}/purchase-history">Lịch sử mua hàng</a>
                                <a class="button-secondary"
                                    href="${pageContext.request.contextPath}/product-detail?id=${item.productId}">Xem
                                    sản phẩm</a>
                            </div>
                        </div>

                        <c:if test="${not empty error}">
                            <div class="notice-error" style="margin-bottom:24px;">
                                <c:out value="${error}" />
                            </div>
                        </c:if>

                        <c:if test="${isEdit}">
                            <div class="notice-ok"
                                style="margin-bottom:24px; background:#eef2ff; color:#3730a3; border:1px solid #c7d2fe;">
                                ℹ️ Bạn đã từng đánh giá sản phẩm này. Nội dung mới dưới đây sẽ cập nhật lại đánh giá
                                trước đó.
                            </div>
                        </c:if>

                        <div class="review-page-grid">
                            <!-- Cột trái: Thông tin sản phẩm đã mua -->
                            <aside class="review-product-box">
                                <div class="review-product-thumb">
                                    <c:choose>
                                        <c:when test="${not empty item.imageUrl}">
                                            <img src="<c:out value='${item.imageUrl}'/>"
                                                alt="<c:out value='${item.productName}'/>">
                                        </c:when>
                                        <c:otherwise>
                                            <span class="thumb-fallback">🖨️</span>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                                <div class="review-product-name">
                                    <c:out value="${item.productName}" />
                                </div>
                                <div style="margin-bottom: 12px;">
                                    <span class="verified-buyer-tag">✓ Đã mua hàng</span>
                                </div>
                                <div class="review-product-meta">
                                    <div class="meta-row">
                                        <span>Gian hàng</span>
                                        <b>
                                            <c:out
                                                value="${not empty item.shopName ? item.shopName : 'PrintKraft Mall'}" />
                                        </b>
                                    </div>
                                    <div class="meta-row">
                                        <span>Số lượng</span>
                                        <b>
                                            <c:out value="${item.quantity}" /> sản phẩm
                                        </b>
                                    </div>
                                    <div class="meta-row">
                                        <span>Đơn giá</span>
                                        <b>
                                            <fmt:formatNumber value="${item.unitPrice}" type="number"
                                                groupingUsed="true" />đ
                                        </b>
                                    </div>
                                    <div class="meta-row">
                                        <span>Mã đơn hàng</span>
                                        <code><c:out value="${item.orderCode}"/></code>
                                    </div>
                                    <c:if test="${not empty item.placedAtText}">
                                        <div class="meta-row">
                                            <span>Ngày đặt</span>
                                            <span>
                                                <c:out value="${item.placedAtText}" />
                                            </span>
                                        </div>
                                    </c:if>
                                </div>
                            </aside>

                            <!-- Cột phải: Form đánh giá và chọn sao -->
                            <section class="form-panel" style="padding: 28px;">
                                <form class="stack-form" method="post"
                                    action="${pageContext.request.contextPath}/review" id="reviewForm">
                                    <input type="hidden" name="csrfToken"
                                        value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                    <input type="hidden" name="item" value="${item.orderItemId}">

                                    <c:set var="currentRating"
                                        value="${selectedRating != null ? selectedRating : (existingReview != null ? existingReview.rating : (item.existingRating != null ? item.existingRating : 5))}" />
                                    <input type="hidden" name="rating" id="ratingInput" value="${currentRating}">

                                    <!-- Bộ chọn số sao tương tác -->
                                    <div class="star-picker-container">
                                        <div class="star-picker-label">Chất lượng sản phẩm nói chung</div>
                                        <div class="star-picker-stars" id="starPicker">
                                            <button type="button" class="star-btn ${currentRating >= 1 ? 'active' : ''}"
                                                data-rating="1" title="1 sao - Rất tệ">★</button>
                                            <button type="button" class="star-btn ${currentRating >= 2 ? 'active' : ''}"
                                                data-rating="2" title="2 sao - Chưa hài lòng">★</button>
                                            <button type="button" class="star-btn ${currentRating >= 3 ? 'active' : ''}"
                                                data-rating="3" title="3 sao - Bình thường">★</button>
                                            <button type="button" class="star-btn ${currentRating >= 4 ? 'active' : ''}"
                                                data-rating="4" title="4 sao - Hài lòng">★</button>
                                            <button type="button" class="star-btn ${currentRating >= 5 ? 'active' : ''}"
                                                data-rating="5" title="5 sao - Tuyệt vời">★</button>
                                        </div>
                                        <div class="star-picker-hint" id="starHint">Tuyệt vời</div>
                                    </div>

                                    <!-- Gợi ý nhanh các nhận xét phổ biến -->
                                    <div class="feedback-chips-section">
                                        <span class="feedback-chips-label">Gợi ý nhanh (nhấn để thêm vào nội
                                            dung):</span>
                                        <div class="feedback-chips-list">
                                            <span class="feedback-chip" data-text="Chất lượng in rất sắc nét ✨">Chất
                                                lượng in sắc nét ✨</span>
                                            <span class="feedback-chip" data-text="Màu sắc chuẩn thiết kế 🎨">Màu sắc
                                                chuẩn thiết kế 🎨</span>
                                            <span class="feedback-chip" data-text="Chất liệu bền đẹp, chắc chắn 📐">Chất
                                                liệu bền đẹp 📐</span>
                                            <span class="feedback-chip" data-text="Đóng gói cẩn thận, kỹ lưỡng 📦">Đóng
                                                gói cẩn thận 📦</span>
                                            <span class="feedback-chip" data-text="Giao hàng siêu nhanh đúng hẹn ⚡">Giao
                                                hàng nhanh ⚡</span>
                                            <span class="feedback-chip"
                                                data-text="Xưởng in tư vấn và hỗ trợ nhiệt tình 💬">Shop tư vấn nhiệt
                                                tình 💬</span>
                                            <span class="feedback-chip" data-text="Rất đáng tiền, sẽ ủng hộ tiếp 💯">Rất
                                                đáng tiền 💯</span>
                                        </div>
                                    </div>

                                    <!-- Nội dung đánh giá -->
                                    <div class="field">
                                        <label for="reviewText">Nội dung đánh giá chi tiết <span
                                                style="color:var(--coral);">*</span></label>
                                        <c:set var="initText"
                                            value="${enteredText != null ? enteredText : (existingReview != null ? existingReview.reviewText : (item.existingReviewText != null ? item.existingReviewText : ''))}" />
                                        <textarea id="reviewText" name="reviewText" rows="6" maxlength="2000" required
                                            placeholder="Hãy chia sẻ cảm nhận của bạn về độ sắc nét, màu sắc, thời gian in ấn và dịch vụ vận chuyển..."><c:out value="${initText}"/></textarea>
                                        <div class="textarea-footer">
                                            <span>Tối thiểu 3 ký tự, tối đa 2000 ký tự.</span>
                                            <span id="charCounter">0 / 2000 ký tự</span>
                                        </div>
                                    </div>

                                    <div class="button-row" style="margin-top:20px;">
                                        <button class="button-primary" type="submit" id="submitBtn">
                                            ${isEdit ? 'Cập nhật đánh giá' : 'Gửi đánh giá sản phẩm'}
                                        </button>
                                        <a class="button-secondary"
                                            href="${pageContext.request.contextPath}/product-detail?id=${item.productId}">Hủy
                                            & Quay lại</a>
                                    </div>
                                </form>
                            </section>
                        </div>
                    </main>

                    <script>
                        (function () {
                            const starPicker = document.getElementById('starPicker');
                            const starButtons = starPicker.querySelectorAll('.star-btn');
                            const ratingInput = document.getElementById('ratingInput');
                            const starHint = document.getElementById('starHint');
                            const reviewText = document.getElementById('reviewText');
                            const charCounter = document.getElementById('charCounter');
                            const chips = document.querySelectorAll('.feedback-chip');

                            const hints = {
                                1: "★☆☆☆☆ Rất không hài lòng — Thất vọng về sản phẩm/dịch vụ",
                                2: "★★☆☆☆ Chưa hài lòng — Chất lượng chưa đúng kỳ vọng",
                                3: "★★★☆☆ Bình thường — Sản phẩm ở mức tạm được",
                                4: "★★★★☆ Hài lòng — Sản phẩm tốt, đúng mô tả",
                                5: "★★★★★ Tuyệt vời — Rất hài lòng về chất lượng sản phẩm & phục vụ"
                            };

                            function updateStars(rating) {
                                starButtons.forEach(btn => {
                                    const r = parseInt(btn.getAttribute('data-rating'), 10);
                                    if (r <= rating) {
                                        btn.classList.add('active');
                                    } else {
                                        btn.classList.remove('active');
                                    }
                                });
                                starHint.textContent = hints[rating] || "";
                            }

                            // Khởi tạo trạng thái ban đầu
                            const initialRating = parseInt(ratingInput.value, 10) || 5;
                            updateStars(initialRating);

                            starButtons.forEach(btn => {
                                const rating = parseInt(btn.getAttribute('data-rating'), 10);

                                btn.addEventListener('click', function () {
                                    ratingInput.value = rating;
                                    updateStars(rating);
                                });

                                btn.addEventListener('mouseenter', function () {
                                    starButtons.forEach(b => {
                                        const r = parseInt(b.getAttribute('data-rating'), 10);
                                        if (r <= rating) {
                                            b.classList.add('hover-active');
                                        } else {
                                            b.classList.remove('hover-active');
                                        }
                                    });
                                    starHint.textContent = hints[rating] || "";
                                });
                            });

                            starPicker.addEventListener('mouseleave', function () {
                                starButtons.forEach(b => b.classList.remove('hover-active'));
                                const current = parseInt(ratingInput.value, 10) || 5;
                                updateStars(current);
                            });

                            // Cập nhật bộ đếm ký tự
                            function updateCharCount() {
                                const len = reviewText.value.length;
                                charCounter.textContent = len + " / 2000 ký tự";
                            }
                            reviewText.addEventListener('input', updateCharCount);
                            updateCharCount();

                            // Thêm gợi ý từ chip vào textarea
                            chips.forEach(chip => {
                                chip.addEventListener('click', function () {
                                    const textToAdd = chip.getAttribute('data-text');
                                    let current = reviewText.value.trim();
                                    if (current.includes(textToAdd)) {
                                        return;
                                    }
                                    if (current.length > 0) {
                                        reviewText.value = current + ". " + textToAdd;
                                    } else {
                                        reviewText.value = textToAdd;
                                    }
                                    chip.classList.add('used');
                                    updateCharCount();
                                    reviewText.focus();
                                });
                            });
                        })();
                    </script>

                    <%@ include file="footer.jsp" %>
            </body>

            </html>