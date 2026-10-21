package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

import java.util.Map;
import java.util.Set;

/** Validation for seller production updates stored in production_updates. */
public final class WorkshopWorkflow {
    public static final String ACCEPTED = "ACCEPTED";
    public static final String PRINTING = "PRINTING";
    public static final String POST_PROCESSING = "POST_PROCESSING";
    public static final String READY_FOR_SHIPPING = "READY_FOR_SHIPPING";

    private static final Set<String> STAGES =
            Set.of(ACCEPTED, PRINTING, POST_PROCESSING, READY_FOR_SHIPPING);
    private static final Map<String, Set<String>> NEXT = Map.of(
            ACCEPTED, Set.of(PRINTING),
            PRINTING, Set.of(PRINTING, POST_PROCESSING),
            POST_PROCESSING, Set.of(POST_PROCESSING, READY_FOR_SHIPPING),
            READY_FOR_SHIPPING, Set.of());

    private WorkshopWorkflow() { }

    public static boolean canStart(String orderStatus, String paymentStatus) {
        boolean fulfillableOrder = OrderStatus.CONFIRMED.equals(orderStatus)
                || OrderStatus.PROCESSING.equals(orderStatus);
        boolean paymentAllowsWork = OrderStatus.PAYMENT_PAID.equals(paymentStatus)
                || OrderStatus.PAYMENT_COD.equals(paymentStatus);
        return fulfillableOrder && paymentAllowsWork;
    }

    public static boolean isValidUpdate(String previousStage, int previousProgress,
                                        String nextStage, int nextProgress) {
        if (nextStage == null || !STAGES.contains(nextStage)
                || nextProgress < 0 || nextProgress > 100) return false;

        if (previousStage == null) {
            return ACCEPTED.equals(nextStage) && nextProgress == 0;
        }
        if (!NEXT.getOrDefault(previousStage, Set.of()).contains(nextStage)) return false;
        if (nextProgress <= previousProgress) return false;

        return switch (nextStage) {
            case PRINTING -> nextProgress <= 80;
            case POST_PROCESSING -> nextProgress >= 40 && nextProgress <= 95;
            case READY_FOR_SHIPPING -> nextProgress == 100;
            default -> false;
        };
    }

    public static void requireValidUpdate(String orderStatus, String paymentStatus,
                                          String previousStage, int previousProgress,
                                          String nextStage, int nextProgress) {
        if (!canStart(orderStatus, paymentStatus)) {
            throw new IllegalArgumentException("Chỉ cập nhật sản xuất sau khi đơn đã được xác nhận và đủ điều kiện xử lý.");
        }
        if (!isValidUpdate(previousStage, previousProgress, nextStage, nextProgress)) {
            throw new IllegalArgumentException("Bước sản xuất hoặc phần trăm tiến độ không hợp lệ.");
        }
    }
}
