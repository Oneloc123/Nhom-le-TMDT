package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

import java.util.Map;
import java.util.Set;
import java.util.Locale;

/** Validation for seller production updates stored in production_updates. */
public final class WorkshopWorkflow {
    public static final String ACCEPTED = "ACCEPTED";
    public static final String PRINTING = "PRINTING";
    public static final String POST_PROCESSING = "POST_PROCESSING";
    public static final String READY_FOR_SHIPPING = "READY_FOR_SHIPPING";

    private static final Set<String> STAGES =
            Set.of(ACCEPTED, PRINTING, POST_PROCESSING, READY_FOR_SHIPPING);
    private static final Map<String, Integer> PROGRESS_BY_STAGE = Map.of(
            ACCEPTED, 0,
            PRINTING, 55,
            POST_PROCESSING, 85,
            READY_FOR_SHIPPING, 100);
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

    private static String normalizeStage(String stage) {
        return stage == null ? "" : stage.trim().toUpperCase(Locale.ROOT);
    }

    /** Returns the fixed customer-visible progress milestone for a production stage. */
    public static int progressForStage(String stage) {
        Integer progress = PROGRESS_BY_STAGE.get(normalizeStage(stage));
        if (progress == null) throw new IllegalArgumentException("Giai đoạn sản xuất không hợp lệ.");
        return progress;
    }

    public static boolean isValidUpdate(String previousStage, int previousProgress, String nextStage) {
        String normalizedNextStage = normalizeStage(nextStage);
        Integer nextProgress = PROGRESS_BY_STAGE.get(normalizedNextStage);
        if (nextProgress == null || !STAGES.contains(normalizedNextStage)) return false;

        if (previousStage == null) {
            return ACCEPTED.equals(normalizedNextStage) && nextProgress == 0;
        }
        String normalizedPreviousStage = normalizeStage(previousStage);
        if (!NEXT.getOrDefault(normalizedPreviousStage, Set.of()).contains(normalizedNextStage)) return false;
        return nextProgress > previousProgress;
    }

    /** Validates the stage and returns its server-controlled progress percentage. */
    public static int requireValidUpdate(String orderStatus, String paymentStatus,
                                         String previousStage, int previousProgress, String nextStage) {
        if (!canStart(orderStatus, paymentStatus)) {
            throw new IllegalArgumentException("Chỉ cập nhật sản xuất sau khi đơn đã được xác nhận và đủ điều kiện xử lý.");
        }
        int nextProgress = progressForStage(nextStage);
        if (!isValidUpdate(previousStage, previousProgress, nextStage)) {
            throw new IllegalArgumentException("Giai đoạn sản xuất không đúng thứ tự hoặc đã được cập nhật.");
        }
        return nextProgress;
    }
}
