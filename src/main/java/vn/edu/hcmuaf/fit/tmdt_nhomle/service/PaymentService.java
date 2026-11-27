package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.OrderDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.PaymentDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Order;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.OrderStatus;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.PaymentAttempt;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

public class PaymentService {

    public static final String METHOD_COD = "COD";
    public static final String METHOD_VNPAY = "VNPay";
    public static final String METHOD_ZALOPAY = "ZaloPay";
    public static final String METHOD_MOMO = "MoMo";
    public static final String METHOD_CARD = "Thẻ ngân hàng";

    public static final Set<String> SUPPORTED_METHODS =
            Set.of(METHOD_COD, METHOD_VNPAY, METHOD_ZALOPAY);
    public static final Set<String> GATEWAY_METHODS = Set.of(METHOD_MOMO, METHOD_CARD);

    private final VnPayConfig vnPayConfig;
    private final VnPayService vnPayService;
    private final ZaloPayConfig zaloPayConfig;
    private final ZaloPayService zaloPayService;

    public PaymentService() {
        this(VnPayConfig.get(), ZaloPayConfig.get());
    }

    PaymentService(VnPayConfig vnPayConfig) {
        this(vnPayConfig, ZaloPayConfig.get());
    }

    PaymentService(VnPayConfig vnPayConfig, ZaloPayConfig zaloPayConfig) {
        this.vnPayConfig = vnPayConfig;
        this.vnPayService = new VnPayService(vnPayConfig);
        this.zaloPayConfig = zaloPayConfig;
        this.zaloPayService = new ZaloPayService(zaloPayConfig);
    }

    public Order pay(String orderCode, long userId, String method) {
        Order order = OrderDao.findByCodeForUser(orderCode, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
        if (!OrderStatus.PENDING_PAYMENT.equals(order.getStatus())) {
            throw new IllegalArgumentException("Đơn hàng này không ở trạng thái chờ thanh toán.");
        }
        if (method == null || (!SUPPORTED_METHODS.contains(method) && !GATEWAY_METHODS.contains(method))) {
            throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ.");
        }

        if (METHOD_COD.equals(method)) {
            return payCod(order, userId);
        }
        if (METHOD_VNPAY.equals(method)) {
            throw new PaymentException("VNPAY cần được khởi tạo bằng luồng chuyển hướng thanh toán.");
        }
        if (METHOD_ZALOPAY.equals(method)) {
            throw new PaymentException("ZaloPay cần được khởi tạo bằng luồng chuyển hướng thanh toán.");
        }
        recordFailedGatewayAttempt(order, method);
        throw new PaymentException("Cổng thanh toán " + method
                + " chưa được cấu hình trong môi trường này. Vui lòng chọn "
                + "Thanh toán khi nhận hàng (COD).");
    }

    private Order payCod(Order order, long userId) {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String current = OrderDao.lockStatus(conn, order.getOrderId(), userId);
                if (!OrderStatus.PENDING_PAYMENT.equals(current)) {
                    throw new IllegalArgumentException("Đơn hàng này không ở trạng thái chờ thanh toán.");
                }
                OrderDao.insertPayment(conn, order.getOrderId(), METHOD_COD, METHOD_COD,
                        order.getTotalAmount(), "PENDING", null);
                OrderDao.applyStatusChange(conn, order.getOrderId(), userId, OrderStatus.CONFIRMED,
                        OrderStatus.PAYMENT_COD, "Xác nhận đơn COD — thanh toán khi nhận hàng");
                conn.commit();
            } catch (SQLException | IllegalArgumentException | DataAccessException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
        return OrderDao.findByCodeForUser(order.getOrderCode(), userId).orElse(order);
    }

    public String startVnPay(String orderCode, long userId, String clientIp, String returnUrl) {
        vnPayConfig.requireConfigured();
        Order order = OrderDao.findByCodeForUser(orderCode, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
        if (!OrderStatus.PENDING_PAYMENT.equals(order.getStatus())) {
            throw new IllegalArgumentException("Đơn hàng này không ở trạng thái chờ thanh toán.");
        }

        VnPayService.Checkout checkout = vnPayService.createCheckout(order, clientIp, returnUrl);
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String current = OrderDao.lockStatus(conn, order.getOrderId(), userId);
                if (!OrderStatus.PENDING_PAYMENT.equals(current)) {
                    throw new IllegalArgumentException("Đơn hàng này không còn chờ thanh toán.");
                }
                PaymentDao.createPendingVnPay(conn, order, checkout.transactionRef(),
                        checkout.checkoutUrl());
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
        return checkout.checkoutUrl();
    }

    public ZaloPayService.CreateOrderResult startZaloPay(String orderCode, long userId,
                                                         String redirectUrl, String callbackUrl) {
        zaloPayConfig.requireConfigured();
        Order order = OrderDao.findByCodeForUser(orderCode, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
        if (!OrderStatus.PENDING_PAYMENT.equals(order.getStatus())) {
            throw new IllegalArgumentException("Đơn hàng này không ở trạng thái chờ thanh toán.");
        }
        ZaloPayService.CreateOrderResult checkout =
                zaloPayService.createOrderPayment(order, userId, redirectUrl, callbackUrl);
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String current = OrderDao.lockStatus(conn, order.getOrderId(), userId);
                if (!OrderStatus.PENDING_PAYMENT.equals(current)) {
                    throw new IllegalArgumentException("Đơn hàng này không còn chờ thanh toán.");
                }
                PaymentDao.createPendingZaloPay(conn, order, checkout.appTransId(), checkout.orderUrl());
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
        return checkout;
    }

    public ZaloPayCallbackResult processZaloPayCallback(String rawJsonBody) {
        ZaloPayService.CallbackData callback = zaloPayService.parseCallback(rawJsonBody);
        if (!callback.validMac()) return new ZaloPayCallbackResult(0, "mac not equal", false);
        if (callback.appTransId() == null || callback.amount() == null) {
            return new ZaloPayCallbackResult(0, "invalid payload", false);
        }
        PaymentAttempt attempt = PaymentDao.findByZaloPayAppTransId(callback.appTransId()).orElse(null);
        if (attempt == null) return new ZaloPayCallbackResult(0, "transaction not found", false);
        if (attempt.amount() == null || attempt.amount().compareTo(callback.amount()) != 0) {
            return new ZaloPayCallbackResult(0, "invalid amount", false);
        }
        boolean paid = confirmZaloPayPayment(callback.appTransId(), callback.zpTransId(),
                callback.amount(), callback.channel());
        return new ZaloPayCallbackResult(1, "success", paid);
    }

    public ZaloPayReturnResult processZaloPayReturn(Map<String, String[]> parameters) {
        String appTransId = parameter(parameters, "apptransid");
        if (appTransId == null || appTransId.isBlank()) {
            return new ZaloPayReturnResult("error", "Thiếu mã giao dịch ZaloPay.", null, null);
        }
        PaymentAttempt attempt = PaymentDao.findByZaloPayAppTransId(appTransId).orElse(null);
        if (attempt == null) {
            return new ZaloPayReturnResult("error", "Không tìm thấy giao dịch thanh toán.", null, null);
        }
        // Không suy luận ZaloPay đã thanh toán chỉ vì đơn hàng đã được trả
        // bằng một phương thức khác (ví dụ VNPAY).
        if ("PAID".equals(attempt.status())) {
            return new ZaloPayReturnResult("success", "Thanh toán ZaloPay thành công.",
                    attempt, attempt.transactionRef());
        }

        String returnStatus = parameter(parameters, "status");
        try {
            ZaloPayService.QueryOrderResult query = zaloPayService.queryOrder(appTransId);
            if (query.paid()) {
                if (query.amount() == null || attempt.amount().compareTo(query.amount()) != 0) {
                    return new ZaloPayReturnResult("error",
                            "Số tiền ZaloPay xác nhận không khớp với đơn hàng.", attempt, query.zpTransId());
                }
                confirmZaloPayPayment(appTransId, query.zpTransId(), query.amount(), null);
                attempt = PaymentDao.findByZaloPayAppTransId(appTransId).orElse(attempt);
                if ("PAID".equals(attempt.status())) {
                    return new ZaloPayReturnResult("success", "Thanh toán ZaloPay thành công.",
                            attempt, query.zpTransId());
                }
                return new ZaloPayReturnResult("pending",
                        "Giao dịch ZaloPay đã thanh toán nhưng đang cần đối soát trước khi hoàn tất đơn.",
                        attempt, query.zpTransId());
            }
            if (query.isProcessing() || query.returnCode() == 3) {
                return new ZaloPayReturnResult("pending",
                        "ZaloPay đang xử lý giao dịch. Vui lòng chờ hệ thống cập nhật.",
                        attempt, query.zpTransId());
            }
            markZaloPayFailedIfPending(appTransId, query.returnCode(), query.returnMessage());
            return new ZaloPayReturnResult("failed", "Giao dịch ZaloPay chưa hoàn tất hoặc đã bị hủy.",
                    attempt, query.zpTransId());
        } catch (PaymentException queryError) {
            BigDecimal returnedAmount = parseAmount(parameter(parameters, "amount"));
            if (zaloPayService.verifyRedirectChecksum(parameters) && "1".equals(returnStatus)
                    && returnedAmount != null && attempt.amount().compareTo(returnedAmount) == 0) {
                confirmZaloPayPayment(appTransId, null, returnedAmount, null);
                attempt = PaymentDao.findByZaloPayAppTransId(appTransId).orElse(attempt);
                if ("PAID".equals(attempt.status())) {
                    return new ZaloPayReturnResult("success", "Thanh toán ZaloPay thành công.",
                            attempt, attempt.transactionRef());
                }
                return new ZaloPayReturnResult("pending",
                        "Giao dịch cần được đối soát trước khi xác nhận đơn hàng.", attempt, null);
            }
            if (returnStatus != null && !"1".equals(returnStatus)) {
                markZaloPayFailedIfPending(appTransId, -1, "Khách hàng hủy hoặc giao dịch thất bại.");
                return new ZaloPayReturnResult("failed", "Giao dịch ZaloPay chưa thành công hoặc đã bị hủy.",
                        attempt, null);
            }
            return new ZaloPayReturnResult("pending",
                    "Chưa thể xác minh giao dịch với ZaloPay. Vui lòng kiểm tra lịch sử mua hàng.",
                    attempt, null);
        }
    }

    public String reconcileZaloPay(String appTransId) {
        PaymentAttempt attempt = PaymentDao.findByZaloPayAppTransId(appTransId).orElse(null);
        if (attempt == null) return "NOT_FOUND";
        // Chỉ bản ghi giao dịch ZaloPay có trạng thái PAID mới được coi là đã thu tiền.
        if ("PAID".equals(attempt.status())) return "PAID";
        if ("REVIEW_REQUIRED".equals(attempt.status())) return "REVIEW_REQUIRED";
        try {
            ZaloPayService.QueryOrderResult query = zaloPayService.queryOrder(appTransId);
            if (query.paid()) {
                if (query.amount() == null || attempt.amount().compareTo(query.amount()) != 0) {
                    return "REVIEW_REQUIRED";
                }
                confirmZaloPayPayment(appTransId, query.zpTransId(), query.amount(), null);
                PaymentAttempt refreshed = PaymentDao.findByZaloPayAppTransId(appTransId).orElse(attempt);
                if ("PAID".equals(refreshed.status())) return "PAID";
                if ("REVIEW_REQUIRED".equals(refreshed.status())) return "REVIEW_REQUIRED";
                return "PENDING";
            }
            if (query.isProcessing() || query.returnCode() == 3) return "PENDING";
            markZaloPayFailedIfPending(appTransId, query.returnCode(), query.returnMessage());
            return "FAILED";
        } catch (PaymentException error) {
            return "PENDING";
        }
    }

    private boolean confirmZaloPayPayment(String appTransId, String zpTransId,
                                          BigDecimal amount, Integer channel) {
        PaymentAttempt located = PaymentDao.findByZaloPayAppTransId(appTransId).orElse(null);
        if (located == null) return false;
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String lockedOrderStatus = OrderDao.lockStatus(conn, located.orderId(), located.userId());
                if (lockedOrderStatus == null) {
                    conn.rollback();
                    return false;
                }
                PaymentAttempt attempt = PaymentDao.findZaloPayForUpdate(conn, appTransId).orElse(null);
                if (attempt == null || attempt.amount() == null
                        || amount == null || attempt.amount().compareTo(amount) != 0) {
                    conn.rollback();
                    return false;
                }
                if ("PAID".equals(attempt.status())) {
                    conn.rollback();
                    return true;
                }
                boolean activeAttempt = "PENDING".equals(attempt.status())
                        || "SUPERSEDED".equals(attempt.status());
                if (!activeAttempt || !OrderStatus.PENDING_PAYMENT.equals(lockedOrderStatus)
                        || OrderStatus.PAYMENT_PAID.equals(attempt.orderPaymentStatus())) {
                    if (!"REVIEW_REQUIRED".equals(attempt.status())) {
                        PaymentDao.markReviewRequired(conn, attempt.paymentId(), zpTransId, "1",
                                channel == null ? null : String.valueOf(channel), "ZaloPay",
                                "ZaloPay báo thanh toán thành công nhưng đơn không còn chờ thanh toán.");
                        conn.commit();
                    } else {
                        conn.rollback();
                    }
                    return false;
                }
                OrderDao.applyStatusChange(conn, attempt.orderId(), attempt.userId(),
                        OrderStatus.CONFIRMED, OrderStatus.PAYMENT_PAID,
                        "Thanh toán ZaloPay thành công — mã giao dịch "
                                + (zpTransId == null ? appTransId : zpTransId));
                PaymentDao.markPaid(conn, attempt.paymentId(), zpTransId, "1",
                        channel == null ? null : String.valueOf(channel), "ZaloPay");
                FinancialAllocationService.allocatePaidOrder(conn, attempt.orderId(), attempt.paymentId());
                conn.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    private void markZaloPayFailedIfPending(String appTransId, int returnCode, String message) {
        PaymentAttempt located = PaymentDao.findByZaloPayAppTransId(appTransId).orElse(null);
        if (located == null) return;
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                OrderDao.lockStatus(conn, located.orderId(), located.userId());
                PaymentAttempt attempt = PaymentDao.findZaloPayForUpdate(conn, appTransId).orElse(null);
                if (attempt != null && "PENDING".equals(attempt.status())) {
                    PaymentDao.markFailed(conn, attempt.paymentId(), null, String.valueOf(returnCode),
                            null, "ZaloPay", message == null ? "Giao dịch không thành công." : message);
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    private static String parameter(Map<String, String[]> parameters, String key) {
        if (parameters == null) return null;
        String[] values = parameters.get(key);
        return values == null || values.length == 0 ? null : values[0];
    }

    private static BigDecimal parseAmount(String value) {
        if (value == null || value.isBlank()) return null;
        try { return new BigDecimal(value.trim()); }
        catch (NumberFormatException ignored) { return null; }
    }

    public IpnResult processVnPayIpn(VnPayService.Callback callback) {
        if (callback == null || !callback.validSignature()) {
            return new IpnResult("97", "Invalid signature", null, false);
        }
        if (callback.transactionRef() == null || callback.amount() == null) {
            return new IpnResult("01", "Order not found", null, false);
        }

        PaymentAttempt located = PaymentDao.findByTransactionRef(callback.transactionRef())
                .orElse(null);
        if (located == null) {
            return new IpnResult("01", "Order not found", null, false);
        }

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Luôn khóa order trước payment như luồng tạo checkout để tránh deadlock.
                String lockedOrderStatus = OrderDao.lockStatus(conn, located.orderId(), located.userId());
                if (lockedOrderStatus == null) {
                    conn.rollback();
                    return new IpnResult("01", "Order not found", null, false);
                }
                PaymentAttempt attempt = PaymentDao.findForUpdate(conn, callback.transactionRef())
                        .orElse(null);
                if (attempt == null) {
                    conn.rollback();
                    return new IpnResult("01", "Order not found", null, false);
                }
                if (attempt.amount() == null || attempt.amount().compareTo(callback.amount()) != 0) {
                    conn.rollback();
                    return new IpnResult("04", "Invalid amount", attempt, false);
                }
                if ("PAID".equals(attempt.status())) {
                    conn.rollback();
                    return new IpnResult("02", "Order already confirmed", attempt, true);
                }

                if (OrderStatus.PAYMENT_PAID.equals(attempt.orderPaymentStatus())) {
                    if (callback.successful() && !"REVIEW_REQUIRED".equals(attempt.status())) {
                        PaymentDao.markReviewRequired(conn, attempt.paymentId(),
                                callback.providerTransactionNo(), callback.responseCode(),
                                callback.bankCode(), callback.cardType(),
                                "VNPAY báo thêm một giao dịch thành công cho đơn đã thanh toán.");
                        conn.commit();
                    } else {
                        conn.rollback();
                    }
                    return new IpnResult("02", "Order already confirmed", attempt, false);
                }

                if (!callback.successful()) {
                    if ("PENDING".equals(attempt.status())) {
                        String reason = VnPayService.callbackMessage(callback);
                        PaymentDao.markFailed(conn, attempt.paymentId(), callback.providerTransactionNo(),
                                callback.responseCode(), callback.bankCode(), callback.cardType(), reason);
                        conn.commit();
                        return new IpnResult("00", "Confirm Success", attempt, false);
                    }
                    conn.rollback();
                    return new IpnResult("02", "Order already confirmed", attempt, false);
                }

                boolean activeAttempt = "PENDING".equals(attempt.status())
                        || "SUPERSEDED".equals(attempt.status());
                if (!activeAttempt) {
                    if ("REVIEW_REQUIRED".equals(attempt.status())) {
                        conn.rollback();
                        return new IpnResult("02", "Order already confirmed", attempt, false);
                    }
                    PaymentDao.markReviewRequired(conn, attempt.paymentId(),
                            callback.providerTransactionNo(), callback.responseCode(),
                            callback.bankCode(), callback.cardType(),
                            "VNPAY báo thành công cho một lần thanh toán đã đóng.");
                    conn.commit();
                    return new IpnResult("00", "Confirm Success", attempt, false);
                }

                if (!OrderStatus.PENDING_PAYMENT.equals(attempt.orderStatus())) {
                    PaymentDao.markReviewRequired(conn, attempt.paymentId(),
                            callback.providerTransactionNo(), callback.responseCode(),
                            callback.bankCode(), callback.cardType(),
                            "VNPAY báo thành công nhưng đơn không còn ở trạng thái chờ thanh toán.");
                    conn.commit();
                    return new IpnResult("00", "Confirm Success", attempt, false);
                }

                OrderDao.applyStatusChange(conn, attempt.orderId(), attempt.userId(),
                        OrderStatus.CONFIRMED, OrderStatus.PAYMENT_PAID,
                        "Thanh toán VNPAY thành công — mã giao dịch "
                                + callback.providerTransactionNo());
                PaymentDao.markPaid(conn, attempt.paymentId(), callback.providerTransactionNo(),
                        callback.responseCode(), callback.bankCode(), callback.cardType());
                FinancialAllocationService.allocatePaidOrder(conn, attempt.orderId(), attempt.paymentId());
                conn.commit();
                return new IpnResult("00", "Confirm Success", attempt, true);
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public PaymentAttempt findVnPayAttempt(String transactionRef) {
        return PaymentDao.findByTransactionRef(transactionRef).orElse(null);
    }

    public VnPayService.Callback parseVnPayCallback(java.util.Map<String, String[]> parameters) {
        return vnPayService.parseCallback(parameters);
    }

    private void recordFailedGatewayAttempt(Order order, String method) {
        try (Connection conn = Database.getConnection()) {
            OrderDao.insertPayment(conn, order.getOrderId(), method, method,
                    order.getTotalAmount(), "FAILED",
                    "Cổng thanh toán chưa được cấu hình (thiếu credentials nhà cung cấp).");
        } catch (SQLException e) {
            System.getLogger(PaymentService.class.getName()).log(System.Logger.Level.WARNING,
                    "Không ghi được bản ghi thanh toán thất bại cho đơn " + order.getOrderCode(), e);
        }
    }

    public record IpnResult(String responseCode, String message,
                            PaymentAttempt attempt, boolean paid) {
    }

    public record ZaloPayCallbackResult(int returnCode, String returnMessage, boolean paid) { }

    public record ZaloPayReturnResult(String status, String message,
                                      PaymentAttempt attempt, String zpTransId) { }
}
