<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Thiết kế 3D đã lưu — PrintKraft 3D</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_home.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shop.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account.css">
</head>
<body data-pk-ctx="${pageContext.request.contextPath}">
<%@ include file="header.jsp" %>

<main class="shop-page acc-page"
      data-pk-csrf="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">

    <div class="shop-heading">
        <div>
            <div class="eyebrow-line">Tài khoản</div>
            <h1>Thiết kế 3D đã lưu</h1>
            <p>Lưu, tìm kiếm và mở lại cấu hình custom sản phẩm in 3D của bạn.</p>
        </div>
        <button type="button" class="acc-btn" data-sd-new>+ Lưu thiết kế mới</button>
    </div>

    <%@ include file="account-nav.jsp" %>

    <c:if test="${not empty message}">
        <div class="notice-ok" role="status"><c:out value="${message}"/></div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="notice-err" role="alert"><c:out value="${error}"/></div>
    </c:if>

    <div class="sd-note">
        <b>Lưu ý trung thực:</b> dự án hiện chưa có trình xem/render 3D trong thư viện,
        nên mỗi thiết kế hiển thị toàn bộ thông số đã lưu thay vì mô hình 3D.
        Nút “Tiếp tục tùy chỉnh” mở trang sản phẩm để bạn chỉnh tiếp theo thông số đã lưu.
    </div>

    <div class="sd-toolbar">
        <form class="sd-search" method="get"
              action="${pageContext.request.contextPath}/saved-designs">
            <input type="search" name="q" value="<c:out value='${q}'/>"
                   placeholder="Tìm theo tên thiết kế…" aria-label="Tìm thiết kế">
            <button type="submit" class="sd-btn sd-btn-primary">Tìm</button>
        </form>
        <span class="acc-muted">Tổng cộng: <b><c:out value="${total}"/></b> thiết kế</span>
    </div>

    <c:choose>
        <c:when test="${empty designs}">
            <div class="empty-state">
                <c:choose>
                    <c:when test="${not empty q}">
                        Không tìm thấy thiết kế nào khớp “<c:out value="${q}"/>”.
                        <a href="${pageContext.request.contextPath}/saved-designs">Xóa bộ tìm kiếm</a>
                    </c:when>
                    <c:otherwise>
                        Bạn chưa lưu thiết kế nào. Bắt đầu bằng nút
                        “+ Lưu thiết kế mới”.
                    </c:otherwise>
                </c:choose>
            </div>
        </c:when>
        <c:otherwise>
            <div class="sd-grid">
                <c:forEach var="design" items="${designs}">
                    <article class="sd-card" data-sd-id="${design.designId}">
                        <div class="sd-preview">
                            <c:choose>
                                <c:when test="${not empty design.previewUrl}">
                                    <img src="${pageContext.request.contextPath}/<c:out value='${design.previewUrl}'/>"
                                         alt="Xem trước thiết kế" style="max-width:100%;max-height:100%;object-fit:contain;">
                                </c:when>
                                <c:otherwise>
                                    Chưa có ảnh xem trước<br>
                                    <span class="acc-muted">Thông số được lưu đầy đủ bên dưới</span>
                                </c:otherwise>
                            </c:choose>
                        </div>
                        <div class="sd-body">
                            <div class="sd-title">
                                <h3><c:out value="${design.designName}"/></h3>
                                <span class="sd-tag ${design.status == 'FINAL' ? 'sd-tag-final' : ''}">
                                    <c:out value="${design.status == 'FINAL' ? 'Hoàn tất' : 'Bản nháp'}"/>
                                </span>
                            </div>
                            <div class="sd-meta">
                                <span><b>Sản phẩm:</b>
                                    <c:out value="${empty design.productName ? 'Không gắn sản phẩm' : design.productName}"/></span>
                                <c:if test="${not empty design.material}">
                                    <span><b>Vật liệu:</b> <c:out value="${design.material}"/>
                                        <c:if test="${not empty design.color}">· <b>Màu:</b> <c:out value="${design.color}"/></c:if>
                                    </span>
                                </c:if>
                                <c:if test="${not empty design.dimensions}">
                                    <span><b>Kích thước:</b> <c:out value="${design.dimensions}"/></span>
                                </c:if>
                                <span><b>Số lượng:</b> <c:out value="${design.quantity}"/> ·
                                    <b>Phiên bản:</b> v<c:out value="${design.version}"/></span>
                                <span class="acc-muted">Cập nhật: <c:out value="${design.updatedAt}"/></span>
                            </div>
                            <div class="sd-actions">
                                <button type="button" class="sd-btn" data-sd-view="${design.designId}">Chi tiết</button>
                                <button type="button" class="sd-btn" data-sd-edit="${design.designId}">Sửa</button>
                                <button type="button" class="sd-btn" data-sd-duplicate="${design.designId}">Nhân bản</button>
                                <c:if test="${not empty design.productId}">
                                    <a class="sd-btn sd-btn-primary"
                                       href="${pageContext.request.contextPath}/product-detail?id=${design.productId}">
                                        Tiếp tục tùy chỉnh
                                    </a>
                                </c:if>
                                <button type="button" class="sd-btn sd-btn-danger" data-sd-delete="${design.designId}">Xóa</button>
                            </div>
                        </div>
                    </article>
                </c:forEach>
            </div>

            <%-- Phân trang phía server --%>
            <c:if test="${pages > 1}">
                <nav class="sd-pagination" aria-label="Phân trang thiết kế">
                    <c:choose>
                        <c:when test="${page > 1}">
                            <a href="${pageContext.request.contextPath}/saved-designs?page=${page - 1}&q=<c:out value='${q}'/>">&laquo; Trước</a>
                        </c:when>
                        <c:otherwise><span class="disabled">&laquo; Trước</span></c:otherwise>
                    </c:choose>
                    <span class="current">Trang <c:out value="${page}"/> / <c:out value="${pages}"/></span>
                    <c:choose>
                        <c:when test="${page < pages}">
                            <a href="${pageContext.request.contextPath}/saved-designs?page=${page + 1}&q=<c:out value='${q}'/>">Sau &raquo;</a>
                        </c:when>
                        <c:otherwise><span class="disabled">Sau &raquo;</span></c:otherwise>
                    </c:choose>
                </nav>
            </c:if>
        </c:otherwise>
    </c:choose>
</main>

<%-- ============ Modal: form Lưu / Sửa thiết kế ============ --%>
<div class="acc-modal" id="sdFormModal" role="dialog" aria-modal="true"
     aria-labelledby="sdFormTitle" hidden>
    <div class="acc-modal-backdrop" data-sd-close></div>
    <div class="acc-modal-card">
        <div class="acc-modal-head">
            <h2 id="sdFormTitle">Lưu thiết kế mới</h2>
            <button type="button" class="acc-modal-x" data-sd-close aria-label="Đóng">&times;</button>
        </div>
        <div class="sd-form">
            <div class="sd-row">
                <label>Tên thiết kế *
                    <input type="text" name="designName" id="sdName" maxlength="150"
                           placeholder="Áo thun in logo — bản 2" required>
                </label>
                <label>Sản phẩm
                    <select name="productId" id="sdProduct">
                        <option value="">— Không gắn sản phẩm —</option>
                        <c:forEach var="p" items="${productOptions}">
                            <option value="${p.id}"><c:out value="${p.name}"/></option>
                        </c:forEach>
                    </select>
                </label>
            </div>
            <div class="sd-row">
                <label>Vật liệu
                    <input type="text" name="material" id="sdMaterial" maxlength="60"
                           placeholder="PLA / Resin / ABS">
                </label>
                <label>Màu sắc
                    <input type="text" name="color" id="sdColor" maxlength="60"
                           placeholder="Đỏ / Trắng / Trong">
                </label>
            </div>
            <div class="sd-row">
                <label>Kích thước
                    <input type="text" name="dimensions" id="sdDimensions" maxlength="120"
                           placeholder="100 x 50 x 20 mm">
                </label>
                <label>Số lượng
                    <input type="number" name="quantity" id="sdQuantity" min="1" max="10000" value="1">
                </label>
            </div>
            <label>Cấu hình tùy chọn (JSON)
                <textarea name="configJson" id="sdConfig"
                          placeholder='{"size":"M","text":"Tên của bạn","logo":true}'></textarea>
                <span class="sd-hint">JSON object hợp lệ, tối đa 8000 ký tự — server kiểm tra lại toàn bộ.</span>
            </label>
            <div class="sd-row">
                <label>Ghi chú
                    <input type="text" name="notes" id="sdNotes" maxlength="500"
                           placeholder="Ghi chú cho xưởng in…">
                </label>
                <label>Trạng thái
                    <select name="status" id="sdStatus">
                        <option value="DRAFT">Bản nháp</option>
                        <option value="FINAL">Hoàn tất</option>
                    </select>
                </label>
            </div>
        </div>
        <div class="acc-modal-foot">
            <button type="button" class="sd-btn" data-sd-close>Hủy</button>
            <button type="button" class="sd-btn sd-btn-primary" id="sdSaveBtn">Lưu thiết kế</button>
        </div>
    </div>
</div>

<%-- ============ Modal: chi tiết thiết kế ============ --%>
<div class="acc-modal" id="sdDetailModal" role="dialog" aria-modal="true"
     aria-labelledby="sdDetailTitle" hidden>
    <div class="acc-modal-backdrop" data-sd-close></div>
    <div class="acc-modal-card">
        <div class="acc-modal-head">
            <h2 id="sdDetailTitle">Chi tiết thiết kế</h2>
            <button type="button" class="acc-modal-x" data-sd-close aria-label="Đóng">&times;</button>
        </div>
        <div id="sdDetailBody"></div>
        <div class="acc-modal-foot">
            <button type="button" class="sd-btn" data-sd-close>Đóng</button>
        </div>
    </div>
</div>

<%@ include file="footer.jsp" %>
<script src="${pageContext.request.contextPath}/js/account.js"></script>
</body>
</html>
