<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Giỏ hàng | PrintKraft</title>
    <style>
    /* CSS riêng cho giỏ hàng; không phụ thuộc cus_home.css, account.css, shop.css hay user-v2.css. */
    :root{--pk-cart-ink:#202c47;--pk-cart-muted:#78849b;--pk-cart-line:#e5eaf3;--pk-cart-blue:#4569e8;--pk-cart-blue-dark:#2f50c5;--pk-cart-soft:#f3f6ff;--pk-cart-shadow:0 16px 44px rgba(35,54,103,.07)}
    body.pk-cart-body{margin:0;color:var(--pk-cart-ink);background:radial-gradient(ellipse at 80% 0,rgba(81,121,246,.1),transparent 38%),#f5f7fc;font-family:Inter,"Segoe UI",Arial,sans-serif;line-height:1.55}
    body.pk-cart-body *{box-sizing:border-box}
    #cartPage.uv2-page{width:min(1260px,calc(100% - 44px));margin:28px auto 58px;color:var(--pk-cart-ink)}
    #cartPage a{color:inherit;text-decoration:none}
    #cartPage a:hover{color:var(--pk-cart-blue-dark)}
    #cartPage .uv2-breadcrumb{display:flex;align-items:center;gap:9px;flex-wrap:wrap;margin:0 0 17px;color:#9aa5b8;font-size:11.5px}
    #cartPage .uv2-breadcrumb a{color:#66748d;font-weight:650}
    #cartPage .uv2-breadcrumb a:hover{color:var(--pk-cart-blue)}
    #cartPage .uv2-page-heading{position:relative;display:flex;align-items:center;justify-content:space-between;gap:24px;overflow:hidden;margin:0 0 22px;padding:27px 30px;border:1px solid #e1e8fc;border-radius:22px;background:linear-gradient(118deg,#fff 0%,#f5f7ff 64%,#eaf4ff 100%);box-shadow:var(--pk-cart-shadow)}
    #cartPage .uv2-page-heading:after{content:"🛒";position:absolute;right:32px;top:50%;transform:translateY(-50%) rotate(-10deg);opacity:.09;font-size:94px;pointer-events:none}
    #cartPage .uv2-page-heading>div,#cartPage .uv2-page-heading>.uv2-button{position:relative;z-index:1}
    #cartPage .uv2-eyebrow{margin:0 0 7px;color:var(--pk-cart-blue);font-size:9.5px;font-weight:850;letter-spacing:.14em;text-transform:uppercase}
    #cartPage .uv2-page-heading h1{margin:0;color:#1e2d4c;font-size:clamp(26px,3vw,35px);font-weight:850;line-height:1.16;letter-spacing:-.045em}
    #cartPage .uv2-page-heading p:not(.uv2-eyebrow){margin:9px 0 0;color:var(--pk-cart-muted);font-size:12.5px;line-height:1.7}
    #cartPage .uv2-button{display:inline-flex;align-items:center;justify-content:center;gap:7px;min-height:40px;padding:10px 14px;border:1px solid #dce4f4;border-radius:11px;background:#fff;color:#52617b;font:inherit;font-size:11.5px;font-weight:800;text-align:center;cursor:pointer;transition:transform .18s,box-shadow .18s,border-color .18s,background .18s}
    #cartPage .uv2-button:hover{transform:translateY(-1px);border-color:#b9c8fa;box-shadow:0 7px 17px rgba(40,67,150,.08)}
    #cartPage .uv2-button.is-primary{border-color:transparent;background:linear-gradient(135deg,#4773f1,#5c50dc);color:#fff;box-shadow:0 8px 18px rgba(68,99,216,.18)}
    #cartPage .uv2-button.is-primary:hover{color:#fff;box-shadow:0 11px 22px rgba(68,99,216,.25)}
    #cartPage .uv2-account-grid{display:grid;grid-template-columns:224px minmax(0,1fr);align-items:start;gap:20px}
    body.pk-cart-body .uv2-side{position:sticky;top:18px;display:flex;flex-direction:column;gap:3px;min-width:0;padding:14px;border:1px solid var(--pk-cart-line);border-radius:18px;background:rgba(255,255,255,.96);box-shadow:var(--pk-cart-shadow)}
    body.pk-cart-body .uv2-side>b{padding:10px 11px 13px;color:#8995aa;font-size:9.5px;letter-spacing:.13em}
    body.pk-cart-body .uv2-side>a,body.pk-cart-body .uv2-side-logout{display:block;width:100%;min-height:40px;padding:10px 11px;border:0;border-radius:10px;background:transparent;color:#5d6a82;font:inherit;font-size:11.7px;font-weight:700;text-align:left;text-decoration:none;cursor:pointer;transition:background .18s,color .18s}
    body.pk-cart-body .uv2-side>a:hover,body.pk-cart-body .uv2-side>a.is-active{background:#edf2ff;color:var(--pk-cart-blue-dark)}
    body.pk-cart-body .uv2-side form{margin:7px 0 0;padding-top:7px;border-top:1px solid #edf0f6}
    body.pk-cart-body .uv2-side-logout{color:#b05264}
    body.pk-cart-body .uv2-side-logout:hover{background:#fff1f3}
    #cartPage .uv2-content{display:flex;flex-direction:column;gap:15px;min-width:0}
    #cartPage .uv2-alert,#cartPage .uv2-cart-notice{padding:12px 15px;border:1px solid #f1cad0;border-radius:12px;background:#fff3f5;color:#a13a50;font-size:11.5px;font-weight:750}
    #cartPage .uv2-cart-notice:not([hidden]){border-color:#c8ebd9;background:#effbf5;color:#187a50}
    #cartPage .uv2-muted{margin:0;color:var(--pk-cart-muted);font-size:11.5px}
    #cartPage .uv2-panel{min-width:0;padding:22px;border:1px solid var(--pk-cart-line);border-radius:19px;background:#fff;box-shadow:0 12px 34px rgba(35,54,103,.055)}
    #cartPage .uv2-panel-head{display:flex;align-items:center;justify-content:space-between;gap:14px;margin-bottom:14px;padding-bottom:16px;border-bottom:1px solid #edf0f6}
    #cartPage .uv2-panel-head h2{margin:0;color:#263452;font-size:17px;font-weight:850;letter-spacing:-.025em}
    #cartPage .uv2-cart-secure{display:inline-flex;align-items:center;gap:6px;padding:7px 10px;border-radius:99px;background:#effbf5;color:#28805b;font-size:9.5px;font-weight:750}
    #cartPage .uv2-cart-secure:before{content:"✓";display:inline-grid;width:17px;height:17px;place-items:center;border-radius:50%;background:#d8f5e6;color:#168653;font-size:10px}
    #cartPage .cart-list{display:grid;gap:0}
    #cartPage .cart-line{display:grid;grid-template-columns:minmax(0,1fr) auto auto auto;align-items:center;gap:16px;padding:19px 0;border-bottom:1px solid #edf0f6}
    #cartPage .cart-line:last-child{border-bottom:0}
    #cartPage .uv2-cart-item-info{min-width:0}
    #cartPage .uv2-cart-item-info h3{margin:0;color:#273653;font-size:13px;font-weight:850;line-height:1.5;overflow-wrap:anywhere}
    #cartPage .uv2-cart-item-info h3 a{color:inherit}
    #cartPage .uv2-cart-item-info h3 a:hover{color:var(--pk-cart-blue)}
    #cartPage .uv2-cart-item-info p{margin:5px 0 7px;color:#79859b;font-size:10.5px;line-height:1.6;overflow-wrap:anywhere}
    #cartPage .uv2-cart-item-info small{color:#9aa4b6;font-size:9.8px;line-height:1.6}
    #cartPage .uv2-quantity-form{display:flex;align-items:flex-end;gap:7px;margin:0}
    #cartPage .uv2-quantity-label{display:grid;gap:5px;color:#7b879c;font-size:9.8px;font-weight:750;white-space:nowrap}
    #cartPage .quantity-input{width:70px;min-height:38px;padding:7px;border:1px solid #dfe6f1;border-radius:9px;background:#fbfcff;color:#273653;font:inherit;font-size:12px;text-align:center;outline:none}
    #cartPage .quantity-input:focus{border-color:#7f98ee;box-shadow:0 0 0 3px rgba(69,105,232,.1)}
    #cartPage .uv2-cart-line-total{min-width:88px;color:#3158c9;font-size:14px;font-weight:850;text-align:right;white-space:nowrap}
    #cartPage .uv2-cart-remove{margin:0}
    #cartPage .button-danger{min-height:35px;padding:8px 11px;border:1px solid #f3d9de;border-radius:9px;background:#fff7f8;color:#b74a5c;font:inherit;font-size:10.5px;font-weight:800;cursor:pointer;transition:background .18s,border-color .18s}
    #cartPage .button-danger:hover{border-color:#e7aab4;background:#ffedf0}
    #cartPage .uv2-clear-cart{display:flex;justify-content:flex-end;margin-top:15px;padding-top:14px;border-top:1px solid #edf0f6}
    #cartPage .uv2-checkout-layout{display:grid;grid-template-columns:minmax(0,1fr) 300px;align-items:start;gap:17px}
    #cartPage .uv2-cart-total-panel{position:sticky;top:18px;padding:22px}
    #cartPage .uv2-cart-total-panel h2{margin:0 0 13px;color:#263452;font-size:18px;font-weight:850;letter-spacing:-.025em}
    #cartPage .uv2-cart-summary-line{display:flex;align-items:flex-start;justify-content:space-between;gap:14px;padding:15px 0;border-top:1px solid #edf0f6;color:#78849b;font-size:11.5px}
    #cartPage .uv2-cart-summary-line strong{color:#355bd0;font-size:19px;font-weight:900;letter-spacing:-.04em;text-align:right;overflow-wrap:anywhere}
    #cartPage .uv2-cart-shipping-note{margin:0;padding:13px;border:1px solid #e8eefb;border-radius:11px;background:#f8faff;color:#7a879e;font-size:10.5px;line-height:1.7}
    #cartPage .uv2-checkout-button{width:100%;margin-top:15px;min-height:46px}
    #cartPage .uv2-cart-trust-note{display:flex;align-items:flex-start;gap:7px;margin-top:13px;color:#8a95a9;font-size:9.5px;line-height:1.65}
    #cartPage .uv2-cart-trust-note span{display:inline-grid;flex:0 0 17px;width:17px;height:17px;place-items:center;border-radius:50%;background:#e7f8ef;color:#208357;font-size:10px;font-weight:900}
    #cartPage .uv2-cart-empty{display:flex;flex-direction:column;align-items:center;justify-content:center;min-height:320px;padding:40px 24px;text-align:center}
    #cartPage .uv2-empty-icon{display:grid;width:78px;height:78px;place-items:center;margin-bottom:18px;border:1px solid #e2e9ff;border-radius:24px;background:linear-gradient(145deg,#f1f5ff,#eaf5ff);font-size:37px;box-shadow:0 12px 25px rgba(69,105,232,.08)}
    #cartPage .uv2-cart-empty>b{color:#263653;font-size:19px;font-weight:850;letter-spacing:-.025em}
    #cartPage .uv2-cart-empty p{max-width:440px;margin:9px 0 20px;color:#7b879c;font-size:11.5px;line-height:1.75}
    body.pk-cart-body .cart-confirm-modal[hidden]{display:none!important}
    body.pk-cart-body .cart-confirm-modal{position:fixed;inset:0;z-index:2000;display:grid;place-items:center;padding:20px}
    body.pk-cart-body .cart-confirm-backdrop{position:absolute;inset:0;background:rgba(20,30,55,.46);backdrop-filter:blur(4px)}
    body.pk-cart-body .cart-confirm-dialog{position:relative;display:grid;grid-template-columns:48px minmax(0,1fr);gap:14px;width:min(430px,100%);padding:24px;border:1px solid #e6eaf2;border-radius:20px;background:#fff;box-shadow:0 24px 70px rgba(20,30,55,.2)}
    body.pk-cart-body .cart-confirm-icon{display:grid;width:45px;height:45px;place-items:center;border-radius:15px;background:#fff1e9;color:#c66b3b;font-size:21px;font-weight:900}
    body.pk-cart-body .cart-confirm-copy h2{margin:1px 0 6px;color:#253350;font-size:17px}
    body.pk-cart-body .cart-confirm-copy p{margin:0;color:#78849b;font-size:11.5px;line-height:1.7}
    body.pk-cart-body .cart-confirm-actions{grid-column:1/-1;display:flex;justify-content:flex-end;gap:9px;margin-top:6px}
    body.pk-cart-body .button-secondary{min-height:38px;padding:9px 13px;border:1px solid #dfe5ef;border-radius:10px;background:#fff;color:#596780;font:inherit;font-size:11px;font-weight:800;cursor:pointer}
    body.pk-cart-body .cart-confirm-submit{min-width:80px;min-height:38px;background:#cf4d61;color:#fff}
    @media(max-width:1080px){#cartPage .uv2-account-grid{grid-template-columns:195px minmax(0,1fr);gap:14px}#cartPage .uv2-checkout-layout{grid-template-columns:minmax(0,1fr) 260px}#cartPage .uv2-panel{padding:18px}#cartPage .cart-line{grid-template-columns:minmax(0,1fr) auto;gap:12px}#cartPage .uv2-cart-line-total{text-align:left}#cartPage .uv2-cart-remove{justify-self:end}}
    @media(max-width:780px){#cartPage.uv2-page{width:calc(100% - 26px);margin:18px auto 38px}#cartPage .uv2-page-heading{align-items:flex-start;flex-direction:column;padding:23px 21px;border-radius:17px}#cartPage .uv2-page-heading:after{right:16px;font-size:68px}#cartPage .uv2-account-grid{grid-template-columns:minmax(0,1fr)}body.pk-cart-body .uv2-side{position:static;display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:3px;padding:11px}body.pk-cart-body .uv2-side>b,body.pk-cart-body .uv2-side form{grid-column:1/-1}body.pk-cart-body .uv2-side>a{min-width:0;padding:9px;font-size:10.8px}#cartPage .uv2-checkout-layout{grid-template-columns:minmax(0,1fr)}#cartPage .uv2-cart-total-panel{position:static}#cartPage .uv2-cart-secure{font-size:8.5px}}
    @media(max-width:520px){#cartPage .uv2-page-heading{gap:15px}#cartPage .uv2-page-heading>.uv2-button{width:100%}#cartPage .uv2-panel{padding:15px;border-radius:16px}#cartPage .uv2-panel-head{align-items:flex-start;flex-direction:column;gap:9px}#cartPage .cart-line{grid-template-columns:minmax(0,1fr) auto;gap:10px;padding:16px 0}#cartPage .uv2-quantity-form{grid-column:1;align-items:flex-end;flex-wrap:wrap}#cartPage .uv2-cart-line-total{grid-column:1;grid-row:auto;font-size:15px}#cartPage .uv2-cart-remove{grid-column:2;grid-row:2;align-self:end}#cartPage .uv2-clear-cart{justify-content:stretch}#cartPage .uv2-clear-cart .uv2-button{width:100%}#cartPage .uv2-cart-summary-line{align-items:flex-start;flex-direction:column;gap:5px}#cartPage .uv2-cart-summary-line strong{text-align:left}body.pk-cart-body .cart-confirm-dialog{grid-template-columns:40px minmax(0,1fr);gap:11px;padding:19px}body.pk-cart-body .cart-confirm-icon{width:39px;height:39px}}
    </style>
</head>
<body class="pk-cart-body">
<%@ include file="header.jsp" %>

<main class="uv2-page uv2-cart-page" id="cartPage" data-products-url="${pageContext.request.contextPath}/products">
    <div class="uv2-breadcrumb">
        <a href="${pageContext.request.contextPath}/home">Trang chủ</a><span>/</span>
        <a href="${pageContext.request.contextPath}/account">Tài khoản</a><span>/</span><span>Giỏ hàng</span>
    </div>
    <div class="uv2-page-heading">
        <div>
            <p class="uv2-eyebrow">SẢN PHẨM &amp; MUA HÀNG</p>
            <h1>Giỏ hàng của bạn</h1>
            <p>Kiểm tra sản phẩm, xưởng in và số lượng trước khi đặt hàng.</p>
        </div>
        <a class="uv2-button is-light" href="${pageContext.request.contextPath}/products">Tiếp tục mua hàng</a>
    </div>

    <div class="uv2-account-grid">
        <jsp:include page="/WEB-INF/views/user/account-sidebar.jsp"/>
        <div class="uv2-content uv2-cart-content">
            <c:if test="${param.error == 'empty'}">
                <div class="uv2-alert is-error">Giỏ hàng đang trống, hãy chọn sản phẩm trước.</div>
            </c:if>
            <div id="cartNotice" class="notice-ok uv2-cart-notice" role="status" aria-live="polite" ${empty notice ? 'hidden' : ''}>
                <c:out value="${notice}"/>
            </div>
            <c:if test="${not empty sessionScope.currentUser and empty cartItems}">
                <p class="uv2-muted">Giỏ hàng được lưu theo tài khoản của bạn.</p>
            </c:if>

            <c:choose>
                <c:when test="${empty cartItems}">
                    <section class="uv2-panel uv2-empty uv2-cart-empty" id="cartContent">
                        <div class="uv2-empty-icon" aria-hidden="true">🛒</div>
                        <b>Giỏ hàng đang trống</b>
                        <p>Hãy khám phá sản phẩm và lựa chọn xưởng in phù hợp với nhu cầu của bạn.</p>
                        <a class="uv2-button is-primary" href="${pageContext.request.contextPath}/products">Khám phá sản phẩm</a>
                    </section>
                </c:when>
                <c:otherwise>
                    <div class="checkout-layout uv2-checkout-layout" id="cartContent">
                        <section class="summary-panel uv2-panel uv2-cart-items-panel">
                            <div class="uv2-panel-head">
                                <div>
                                    <p class="uv2-eyebrow">SẢN PHẨM ĐÃ CHỌN</p>
                                    <h2 id="cartQuantityHeading"><c:out value="${cartTotalQuantity}"/> sản phẩm</h2>
                                </div>
                                <span class="uv2-cart-secure">Giá được xác nhận khi đặt hàng</span>
                            </div>
                            <div class="cart-list">
                                <c:forEach var="item" items="${cartItems}">
                                    <article class="cart-line uv2-cart-line" data-cart-line="${item.shopProductId}">
                                        <div class="uv2-cart-item-info">
                                            <h3><a href="${pageContext.request.contextPath}/product-detail?id=${item.product.id}"><c:out value="${item.product.name}"/></a></h3>
                                            <p><c:out value="${item.shopName}"/> · <c:out value="${item.product.printMethod}"/> · <c:out value="${item.material}"/></p>
                                            <small><fmt:formatNumber value="${item.unitPrice}" type="number" groupingUsed="true"/>đ/sản phẩm · Số lượng tối thiểu <c:out value="${item.minimumQuantity}"/></small>
                                        </div>
                                        <form class="quantity-form js-cart-mutation uv2-quantity-form" method="post" action="${pageContext.request.contextPath}/cart">
                                            <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                            <input type="hidden" name="action" value="update">
                                            <input type="hidden" name="shopProductId" value="${item.shopProductId}">
                                            <label class="uv2-quantity-label">Số lượng
                                                <input class="quantity-input" type="number" name="quantity"
                                                       min="${item.minimumQuantity > 0 ? item.minimumQuantity : 1}"
                                                       max="100000" value="${item.quantity}" aria-label="Số lượng" required>
                                            </label>
                                            <button class="uv2-button is-light" type="submit">Cập nhật</button>
                                        </form>
                                        <strong class="uv2-cart-line-total" data-line-subtotal><fmt:formatNumber value="${item.subtotal}" type="number" groupingUsed="true"/>đ</strong>
                                        <form class="js-cart-mutation uv2-cart-remove" method="post" action="${pageContext.request.contextPath}/cart"
                                              data-confirm="Xóa sản phẩm này khỏi giỏ hàng?">
                                            <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                            <input type="hidden" name="action" value="remove">
                                            <input type="hidden" name="shopProductId" value="${item.shopProductId}">
                                            <button class="button-danger" type="submit">Xóa</button>
                                        </form>
                                    </article>
                                </c:forEach>
                            </div>
                            <form class="js-cart-mutation uv2-clear-cart" method="post" action="${pageContext.request.contextPath}/cart"
                                  data-confirm="Xóa toàn bộ sản phẩm trong giỏ hàng?" data-confirm-title="Xóa toàn bộ giỏ hàng?">
                                <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                <input type="hidden" name="action" value="clear">
                                <button class="uv2-button is-light" type="submit">Xóa toàn bộ giỏ hàng</button>
                            </form>
                        </section>
                        <aside class="order-panel uv2-panel uv2-cart-total-panel">
                            <p class="uv2-eyebrow">TÓM TẮT ĐƠN HÀNG</p>
                            <h2>Tạm tính</h2>
                            <div class="summary-line uv2-cart-summary-line">
                                <span>Tổng tiền sản phẩm</span>
                                <strong class="cart-total" id="cartSubtotal"><fmt:formatNumber value="${cartSubtotal}" type="number" groupingUsed="true"/>đ</strong>
                            </div>
                            <p class="muted uv2-cart-shipping-note">Phí vận chuyển, thông tin giao nhận và phương thức thanh toán sẽ được xác nhận ở bước đặt hàng.</p>
                            <a class="button-primary uv2-button is-primary uv2-checkout-button" href="${pageContext.request.contextPath}/order">Tiến hành đặt hàng</a>
                            <div class="uv2-cart-trust-note"><span aria-hidden="true">✓</span> Thông tin đơn hàng được kiểm tra trước khi xác nhận.</div>
                        </aside>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</main>

<div class="cart-confirm-modal" id="cartConfirmModal" hidden>
    <div class="cart-confirm-backdrop" data-confirm-cancel></div>
    <section class="cart-confirm-dialog" role="dialog" aria-modal="true"
             aria-labelledby="cartConfirmTitle" aria-describedby="cartConfirmMessage" tabindex="-1">
        <div class="cart-confirm-icon" aria-hidden="true">!</div>
        <div class="cart-confirm-copy">
            <h2 id="cartConfirmTitle">Xóa sản phẩm?</h2>
            <p id="cartConfirmMessage">Xóa sản phẩm này khỏi giỏ hàng?</p>
        </div>
        <div class="cart-confirm-actions">
            <button class="button-secondary" type="button" data-confirm-cancel>Giữ lại</button>
            <button class="button-danger cart-confirm-submit" type="button" id="cartConfirmSubmit">Xóa</button>
        </div>
    </section>
</div>
<%@ include file="footer.jsp" %>
<script defer src="${pageContext.request.contextPath}/js/cart.js?v=20261104-account-layout"></script>
</body>
</html>