<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Tổng quan tài khoản | PrintKraft</title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/account.css">
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/user-v2.css?v=20261104">
</head>

<body>

<jsp:include page="/customer/header.jsp"/>

<main class="uv2-page uv2-dashboard">

    <div class="uv2-breadcrumb">
        <a href="${pageContext.request.contextPath}/home">Trang chủ</a>
        <span>/</span>
        <span>Tài khoản của tôi</span>
    </div>

    <c:if test="${not empty success}">
        <div class="uv2-alert is-success">
            <c:out value="${success}"/>
        </div>
    </c:if>

    <c:if test="${not empty error}">
        <div class="uv2-alert is-error">
            <c:out value="${error}"/>
        </div>
    </c:if>

    <!-- HERO -->
    <section class="uv2-welcome">

        <div class="uv2-avatar">
            <c:out value="${accountUser.userInitial}"/>
        </div>

        <div>
            <p class="uv2-eyebrow">PRINTKRAFT CUSTOMER SPACE</p>

            <h1>
                Xin chào,
                <c:out value="${accountUser.displayName}"/>!
            </h1>

            <p>
                Chào mừng bạn quay lại PrintKraft.
                Tất cả hoạt động mua sắm và thiết kế 3D
                đều được quản lý tại đây.
            </p>
        </div>

        <a class="uv2-button is-primary"
           href="${pageContext.request.contextPath}/profile">
            ✎ Chỉnh sửa hồ sơ
        </a>

    </section>

    <!-- ACCOUNT CONTENT -->
    <div class="uv2-account-grid">

        <!-- SIDEBAR -->
        <aside class="uv2-side">

            <b>KHÔNG GIAN CÁ NHÂN</b>

            <a class="is-active"
               href="${pageContext.request.contextPath}/account">
                Tổng quan
            </a>

            <a href="${pageContext.request.contextPath}/profile">
                Thông tin cá nhân
            </a>

            <a href="${pageContext.request.contextPath}/addresses">
                Sổ địa chỉ
            </a>

            <a href="${pageContext.request.contextPath}/purchase-history">
                Đơn hàng của tôi
            </a>

            <a href="${pageContext.request.contextPath}/my-designs">
                Yêu cầu in 3D
            </a>

            <a href="${pageContext.request.contextPath}/favorites">
                Sản phẩm yêu thích
            </a>

            <a href="${pageContext.request.contextPath}/messages">
                Tin nhắn xưởng
            </a>

            <form method="post"
                  action="${pageContext.request.contextPath}/logout">

                <input type="hidden"
                       name="csrfToken"
                       value="<c:out value='${csrfToken}'/>">

                <button class="uv2-side-logout" type="submit">
                    Đăng xuất
                </button>

            </form>

        </aside>

        <!-- MAIN CONTENT -->
        <div class="uv2-content">

            <!-- STATISTICS -->
            <section class="uv2-stats">

                <article>
                    <span>Tổng đơn hàng</span>

                    <strong>
                        <c:out value="${dashboard.totalOrders}"/>
                    </strong>

                    <small>Tổng số đơn đã đặt</small>
                </article>

                <article>
                    <span>Đang xử lý</span>

                    <strong>
                        <c:out value="${dashboard.processingOrders}"/>
                    </strong>

                    <small>Đơn hàng chưa hoàn tất</small>
                </article>

                <article>
                    <span>Đã hoàn thành</span>

                    <strong>
                        <c:out value="${dashboard.completedOrders}"/>
                    </strong>

                    <small>Đơn đã giao thành công</small>
                </article>

                <article>
                    <span>Yêu cầu in 3D</span>

                    <strong>
                        <c:out value="${dashboard.customRequests}"/>
                    </strong>

                    <small>Mô hình đã gửi đến xưởng</small>
                </article>

            </section>

            <!-- QUICK ACTIONS -->
            <section class="uv2-shortcuts">

                <a class="uv2-shortcut"
                   href="${pageContext.request.contextPath}/design">

                    <div class="uv2-shortcut-icon">◇</div>

                    <div>
                        <b>Thiết kế và in 3D</b>
                        <small>
                            Gửi mô hình, chọn vật liệu và yêu cầu báo giá.
                        </small>
                    </div>

                </a>

                <a class="uv2-shortcut"
                   href="${pageContext.request.contextPath}/addresses">

                    <div class="uv2-shortcut-icon">⌖</div>

                    <div>
                        <b>Địa chỉ giao hàng</b>
                        <small>
                            Quản lý địa chỉ và lựa chọn nơi nhận hàng.
                        </small>
                    </div>

                </a>

                <a class="uv2-shortcut"
                   href="${pageContext.request.contextPath}/favorites">

                    <div class="uv2-shortcut-icon">♡</div>

                    <div>
                        <b>Sản phẩm yêu thích</b>
                        <small>
                            Xem lại những sản phẩm bạn đã lưu.
                        </small>
                    </div>

                </a>

            </section>

            <!-- RECENT ORDERS -->
            <section class="uv2-panel">

                <div class="uv2-panel-head">

                    <div>
                        <p class="uv2-eyebrow">
                            SHOPPING ACTIVITY
                        </p>

                        <h2>Đơn hàng gần đây</h2>
                    </div>

                    <a href="${pageContext.request.contextPath}/purchase-history">
                        Xem tất cả →
                    </a>

                </div>

                <c:choose>

                    <c:when test="${empty dashboard.latestOrders}">

                        <div class="uv2-empty">

                            <div style="font-size:38px;margin-bottom:12px;">
                                🛍
                            </div>

                            <b>Bạn chưa có đơn hàng nào</b>

                            <p>
                                Khám phá những sản phẩm độc đáo
                                và bắt đầu trải nghiệm mua sắm tại PrintKraft.
                            </p>

                            <a class="uv2-button is-primary"
                               href="${pageContext.request.contextPath}/products">
                                Khám phá sản phẩm →
                            </a>

                        </div>

                    </c:when>

                    <c:otherwise>

                        <div class="uv2-table-wrap">

                            <table class="uv2-table">

                                <thead>
                                <tr>
                                    <th>MÃ ĐƠN HÀNG</th>
                                    <th>NGÀY ĐẶT</th>
                                    <th>TRẠNG THÁI</th>
                                    <th>TỔNG TIỀN</th>
                                    <th></th>
                                </tr>
                                </thead>

                                <tbody>

                                <c:forEach var="o"
                                           items="${dashboard.latestOrders}">

                                    <tr>

                                        <td>
                                            <b>
                                                <c:out value="${o.orderCode}"/>
                                            </b>
                                        </td>

                                        <td>
                                            <c:out value="${o.placedAt}"/>
                                        </td>

                                        <td>
                                            <span class="uv2-status">
                                                <c:out value="${o.status}"/>
                                            </span>
                                        </td>

                                        <td>
                                            <b>
                                                <c:out value="${o.totalAmount}"/> ₫
                                            </b>
                                        </td>

                                        <td>
                                            <a href="${pageContext.request.contextPath}/order-detail?code=${o.orderCode}">
                                                Chi tiết →
                                            </a>
                                        </td>

                                    </tr>

                                </c:forEach>

                                </tbody>

                            </table>

                        </div>

                    </c:otherwise>

                </c:choose>

            </section>

            <!-- DEFAULT ADDRESS -->
            <section class="uv2-panel">

                <div class="uv2-panel-head">

                    <div>
                        <p class="uv2-eyebrow">DELIVERY INFORMATION</p>
                        <h2>Địa chỉ nhận hàng mặc định</h2>
                    </div>

                    <a href="${pageContext.request.contextPath}/addresses">
                        Quản lý →
                    </a>

                </div>

                <c:choose>

                    <c:when test="${empty defaultAddress}">

                        <div class="uv2-empty">

                            <div style="font-size:34px;margin-bottom:10px;">
                                📍
                            </div>

                            <b>Bạn chưa có địa chỉ giao hàng</b>

                            <p>
                                Thêm địa chỉ để thuận tiện hơn
                                trong những lần đặt hàng tiếp theo.
                            </p>

                            <a class="uv2-button is-primary"
                               href="${pageContext.request.contextPath}/addresses">
                                + Thêm địa chỉ
                            </a>

                        </div>

                    </c:when>

                    <c:otherwise>

                        <div class="uv2-address-preview">

                            <b>
                                <c:out value="${defaultAddress.recipient_name}"/>
                                ·
                                <c:out value="${defaultAddress.recipient_phone}"/>
                            </b>

                            <p>
                                <c:out value="${defaultAddress.address_line}"/>,
                                <c:out value="${defaultAddress.ward}"/>,
                                <c:out value="${defaultAddress.district}"/>,
                                <c:out value="${defaultAddress.province}"/>
                            </p>

                            <span>
                                ✓
                                <c:out value="${defaultAddress.label}"/>
                                · Địa chỉ mặc định
                            </span>

                        </div>

                    </c:otherwise>

                </c:choose>

            </section>

        </div>

    </div>

</main>

<jsp:include page="/customer/footer.jsp"/>

</body>
</html>