package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import java.util.Locale;
import java.util.Set;

/** Rules for reasoned, auditable shop verification decisions. */
public final class SellerVerificationPolicy {
    public record Decision(String action, String targetStatus, String reason) { }

    private SellerVerificationPolicy() { }

    /** Maps a verification decision to the shop's operational state. */
    public static String shopStatusFor(String rawAction) {
        String action = rawAction == null ? "" : rawAction.trim().toUpperCase(Locale.ROOT);
        return switch (action) {
            case "APPROVE" -> "ACTIVE";
            case "REJECT", "REQUEST_INFO" -> "INACTIVE";
            default -> throw new IllegalArgumentException("Thao tác xác minh xưởng không hợp lệ.");
        };
    }

    public static Decision decide(String currentStatus, String rawAction, String rawReason) {
        if (!"PENDING".equalsIgnoreCase(currentStatus == null ? "" : currentStatus.trim())) {
            throw new IllegalArgumentException("Chỉ xưởng đang chờ xác minh mới được xử lý.");
        }
        String action = rawAction == null ? "" : rawAction.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("APPROVE", "REJECT", "REQUEST_INFO").contains(action)) {
            throw new IllegalArgumentException("Thao tác xác minh xưởng không hợp lệ.");
        }
        String reason = rawReason == null ? "" : rawReason.trim();
        if (reason.length() < 3 || reason.length() > 1000) {
            throw new IllegalArgumentException("Lý do xử lý phải có từ 3 đến 1000 ký tự.");
        }
        String target = switch (action) {
            case "APPROVE" -> "VERIFIED";
            case "REJECT" -> "REJECTED";
            case "REQUEST_INFO" -> "PENDING";
            default -> throw new IllegalArgumentException("Thao tác xác minh xưởng không hợp lệ.");
        };
        return new Decision(action, target, reason);
    }
}
