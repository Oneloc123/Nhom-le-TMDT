package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.OrderStore;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Order;

public class PaymentService {
    public static final String METHOD_VNPAY = "VNPay";
    public static final String METHOD_MOMO = "MoMo";
    public static final String METHOD_CARD = "Thẻ ngân hàng";

    public Order pay(String orderNumber, int customerId, String method) {
        Order order = OrderStore.findByNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
        if (order.getCustomerId() != customerId) {
            throw new SecurityException("Bạn không có quyền thanh toán đơn hàng này.");
        }
        if (!METHOD_VNPAY.equals(method) && !METHOD_MOMO.equals(method)
                && !METHOD_CARD.equals(method)) {
            throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ.");
        }
        order.markPaid(method);
        return OrderStore.save(order);
    }
}
