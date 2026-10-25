package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

import java.util.List;
import java.util.Set;

public final class OrderStatus {

    public static final String PENDING_PAYMENT = "Chờ thanh toán";
    public static final String CONFIRMED = "Đã xác nhận";
    public static final String PROCESSING = "Đang xử lý";
    public static final String SHIPPED = "Đang giao hàng";
    public static final String COMPLETED = "Đã hoàn thành";
    public static final String CANCELLED = "Đã hủy";

    public static final String PAYMENT_UNPAID = "Chưa thanh toán";
    public static final String PAYMENT_COD = "COD (thanh toán khi nhận hàng)";
    public static final String PAYMENT_PAID = "Đã thanh toán";
    public static final String PAYMENT_CANCELLED = "Đã hủy";

    public static final String SHIPPING_NONE = "Chưa có";
    public static final String SHIPPING_IN_TRANSIT = "Đang vận chuyển";
    public static final String SHIPPING_DELIVERED = "Đã giao";

    private static final java.util.Map<String, List<String>> TRANSITIONS = java.util.Map.of(
            PENDING_PAYMENT, List.of(CONFIRMED, CANCELLED),
            CONFIRMED, List.of(PROCESSING, CANCELLED),
            PROCESSING, List.of(SHIPPED),
            SHIPPED, List.of(COMPLETED),
            COMPLETED, List.of(),
            CANCELLED, List.of());

    private static final Set<String> CANCELLABLE_BY_CUSTOMER = Set.of(PENDING_PAYMENT, CONFIRMED);

    private OrderStatus() {
    }

    public static boolean canTransition(String from, String to) {
        if (from == null || to == null) {
            return false;
        }
        return TRANSITIONS.getOrDefault(from, List.of()).contains(to);
    }

    public static boolean cancellableByCustomer(String status) {
        return CANCELLABLE_BY_CUSTOMER.contains(status);
    }
}
