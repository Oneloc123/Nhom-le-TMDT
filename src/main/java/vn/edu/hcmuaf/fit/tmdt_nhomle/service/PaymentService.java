package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.OrderDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Order;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.OrderStatus;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

/**
 * Xử lý trạng thái thanh toán.
 * - COD (thanh toán khi nhận hàng): phương thức không cần cổng — đơn được xác
 *   nhận, bản ghi payments ở trạng thái PENDING, ghi lịch sử đơn.
 * - VNPay / MoMo / thẻ: CHƯA có credentials cổng thanh toán trong dự án này →
 *   ghi bản ghi payments FAILED với lý do rõ ràng và báo lỗi cho người dùng.
 *   KHÔNG giả lập thành công thanh toán (không callback thật = không paid).
 */
public class PaymentService {

    public static final String METHOD_COD = "COD";
    public static final String METHOD_VNPAY = "VNPay";
    public static final String METHOD_MOMO = "MoMo";
    public static final String METHOD_CARD = "Thẻ ngân hàng";

    public static final Set<String> SUPPORTED_METHODS = Set.of(METHOD_COD);
    public static final Set<String> GATEWAY_METHODS = Set.of(METHOD_VNPAY, METHOD_MOMO, METHOD_CARD);

    /** Trả về đơn hàng sau khi cập nhật trạng thái thanh toán. */
    public Order pay(String orderCode, long userId, String method) {
        Order order = OrderDao.findByCodeForUser(orderCode, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
        if (!OrderStatus.PENDING_PAYMENT.equals(order.getStatus())) {
            throw new IllegalArgumentException("Đơn hàng này không ở trạng thái chờ thanh toán.");
        }
        if (method == null || (!SUPPORTED_METHODS.contains(method) && !GATEWAY_METHODS.contains(method))) {
            throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ.");
        }

        if (SUPPORTED_METHODS.contains(method)) {
            return payCod(order, userId);
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

    /** Ghi lại lần thử thanh toán thất bại (cổng chưa cấu hình) để truy vết. */
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
}
