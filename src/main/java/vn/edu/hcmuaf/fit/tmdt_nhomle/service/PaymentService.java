package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.OrderDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.PaymentDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Order;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.OrderStatus;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.PaymentAttempt;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

public class PaymentService {

    public static final String METHOD_COD = "COD";
    public static final String METHOD_VNPAY = "VNPay";
    public static final String METHOD_MOMO = "MoMo";
    public static final String METHOD_CARD = "Thẻ ngân hàng";

    public static final Set<String> SUPPORTED_METHODS = Set.of(METHOD_COD, METHOD_VNPAY);
    public static final Set<String> GATEWAY_METHODS = Set.of(METHOD_MOMO, METHOD_CARD);

    private final VnPayConfig vnPayConfig;
    private final VnPayService vnPayService;

    public PaymentService() {
        this(VnPayConfig.get());
    }

    PaymentService(VnPayConfig vnPayConfig) {
        this.vnPayConfig = vnPayConfig;
        this.vnPayService = new VnPayService(vnPayConfig);
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
}
