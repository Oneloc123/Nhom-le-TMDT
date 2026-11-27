package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import java.util.Locale;
import java.util.Set;

/** Validation rules for customer delivery confirmation and Admin dispute review. */
public final class DeliveryDisputePolicy {
    public static final Set<String> ISSUE_TYPES = Set.of(
            "NOT_RECEIVED", "DAMAGED", "MISSING", "WRONG_ITEM", "OTHER");
    public static final Set<String> ACTIVE_STATUSES = Set.of("OPEN", "UNDER_REVIEW");
    public static final Set<String> TERMINAL_STATUSES = Set.of("RESOLVED", "REJECTED");

    private DeliveryDisputePolicy() { }

    public static String normalizeIssueType(String raw) {
        String value = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (!ISSUE_TYPES.contains(value)) {
            throw new IllegalArgumentException("Loại khiếu nại giao hàng không hợp lệ.");
        }
        return value;
    }

    public static String normalizeAction(String raw) {
        String value = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("REVIEW", "RESOLVE", "REJECT").contains(value)) {
            throw new IllegalArgumentException("Thao tác xử lý khiếu nại không hợp lệ.");
        }
        return value;
    }

    public static String targetStatus(String action) {
        return switch (normalizeAction(action)) {
            case "REVIEW" -> "UNDER_REVIEW";
            case "RESOLVE" -> "RESOLVED";
            case "REJECT" -> "REJECTED";
            default -> throw new IllegalArgumentException("Thao tác xử lý khiếu nại không hợp lệ.");
        };
    }

    public static boolean canTransition(String from, String to) {
        if (from == null || to == null) return false;
        String oldStatus = from.trim().toUpperCase(Locale.ROOT);
        String newStatus = to.trim().toUpperCase(Locale.ROOT);
        return switch (oldStatus) {
            case "OPEN" -> Set.of("UNDER_REVIEW", "RESOLVED", "REJECTED").contains(newStatus);
            case "UNDER_REVIEW" -> Set.of("RESOLVED", "REJECTED").contains(newStatus);
            default -> false;
        };
    }

    public static boolean blocksCompletion(String status) {
        return status != null && ACTIVE_STATUSES.contains(status.trim().toUpperCase(Locale.ROOT));
    }
}
