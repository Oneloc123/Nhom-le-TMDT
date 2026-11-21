package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/** Deterministic customer return/refund rules, shared by controllers and tests. */
public final class ReturnRefundPolicy {
    public static final int RETURN_WINDOW_DAYS = 7;
    public static final String POLICY_VERSION = "PK-RR-2026-11";
    public static final Set<String> REQUEST_TYPES = Set.of("RETURN_AND_REFUND", "REFUND_ONLY");
    public static final Set<String> REASONS = Set.of(
            "DEFECTIVE", "DAMAGED", "WRONG_ITEM", "MISSING_ITEM", "CHANGED_MIND", "OTHER");
    public static final Set<String> EVIDENCE_REQUIRED_REASONS = Set.of(
            "DEFECTIVE", "DAMAGED", "WRONG_ITEM", "MISSING_ITEM");
    public static final Set<String> CUSTOM_ITEM_ALLOWED_REASONS = Set.of(
            "DEFECTIVE", "DAMAGED", "WRONG_ITEM", "MISSING_ITEM");
    public static final Set<String> REFUND_METHODS = Set.of(
            "ORIGINAL_PAYMENT", "BANK_TRANSFER", "COD_MANUAL", "CASH", "OTHER");

    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            "SUBMITTED", Set.of("UNDER_REVIEW", "REJECTED", "WITHDRAWN"),
            "UNDER_REVIEW", Set.of("AWAITING_RETURN_SHIPMENT", "REFUND_PROCESSING", "REJECTED"),
            "AWAITING_RETURN_SHIPMENT", Set.of("RETURN_SHIPPED", "RETURN_RECEIVED", "REJECTED"),
            "RETURN_SHIPPED", Set.of("RETURN_RECEIVED", "REJECTED"),
            "RETURN_RECEIVED", Set.of("REFUND_PROCESSING", "REJECTED"),
            "REFUND_PROCESSING", Set.of("REFUNDED", "REFUND_FAILED"),
            "REFUND_FAILED", Set.of("REFUND_PROCESSING"),
            "REFUNDED", Set.of(),
            "REJECTED", Set.of(),
            "WITHDRAWN", Set.of()
    );

    private ReturnRefundPolicy() { }

    public static boolean isWithinReturnWindow(LocalDateTime deliveredAt, LocalDateTime now) {
        if (deliveredAt == null || now == null || deliveredAt.isAfter(now)) return false;
        return !now.isAfter(deliveredAt.plusDays(RETURN_WINDOW_DAYS));
    }

    public static boolean isValidRequestType(String requestType) {
        return requestType != null && REQUEST_TYPES.contains(requestType);
    }

    public static boolean isValidReason(String reasonCode) {
        return reasonCode != null && REASONS.contains(reasonCode);
    }

    public static boolean requiresEvidence(String reasonCode, boolean customMade) {
        return customMade || EVIDENCE_REQUIRED_REASONS.contains(reasonCode);
    }

    public static boolean isReasonAllowedForCustomItem(String reasonCode) {
        return CUSTOM_ITEM_ALLOWED_REASONS.contains(reasonCode);
    }

    public static boolean isQuantityAvailable(int requestedQuantity, int availableQuantity) {
        return requestedQuantity > 0 && requestedQuantity <= availableQuantity;
    }

    public static BigDecimal requestedAmount(BigDecimal unitPrice, int quantity) {
        if (unitPrice == null || unitPrice.signum() < 0 || quantity < 1) {
            throw new IllegalArgumentException("Không thể tính số tiền yêu cầu hoàn trả.");
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    public static boolean canTransition(String from, String to) {
        return from != null && to != null
                && TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    public static String label(String status) {
        if (status == null) return "Không xác định";
        return switch (status) {
            case "SUBMITTED" -> "Đã gửi yêu cầu";
            case "UNDER_REVIEW" -> "Đang xem xét";
            case "AWAITING_RETURN_SHIPMENT" -> "Chờ gửi hàng trả";
            case "RETURN_SHIPPED" -> "Khách đã gửi hàng trả";
            case "RETURN_RECEIVED" -> "Đã nhận hàng trả";
            case "REFUND_PROCESSING" -> "Đang xử lý hoàn tiền";
            case "REFUNDED" -> "Đã ghi nhận hoàn tiền";
            case "REFUND_FAILED" -> "Hoàn tiền chưa thành công";
            case "REJECTED" -> "Từ chối yêu cầu";
            case "WITHDRAWN" -> "Đã rút yêu cầu";
            default -> status;
        };
    }
}
