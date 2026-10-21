package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

/** Moderation state is independent from the seller/admin publication switch. */
public final class ProductModeration {
    public static final String PENDING_REVIEW = "PENDING_REVIEW";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";

    private ProductModeration() { }

    public record State(String moderationStatus, boolean active, String rejectionReason) { }

    public static State transition(String moderationStatus, boolean active, String action, String reason) {
        if ("approve".equals(action)) {
            requirePending(moderationStatus);
            return new State(APPROVED, active, null);
        }
        if ("reject".equals(action)) {
            requirePending(moderationStatus);
            String normalized = reason == null ? "" : reason.trim();
            if (normalized.length() < 8 || normalized.length() > 500) {
                throw new IllegalArgumentException("Lý do từ chối cần từ 8 đến 500 ký tự.");
            }
            return new State(REJECTED, active, normalized);
        }
        if (!APPROVED.equals(moderationStatus)) {
            throw new IllegalArgumentException("Chỉ sản phẩm đã duyệt mới được ẩn hoặc hiện.");
        }
        if ("hide".equals(action)) {
            if (!active) throw new IllegalArgumentException("Sản phẩm đã được ẩn.");
            return new State(APPROVED, false, null);
        }
        if ("unhide".equals(action)) {
            if (active) throw new IllegalArgumentException("Sản phẩm đang hoạt động.");
            return new State(APPROVED, true, null);
        }
        throw new IllegalArgumentException("Thao tác kiểm duyệt không hợp lệ.");
    }

    private static void requirePending(String status) {
        if (!PENDING_REVIEW.equals(status)) {
            throw new IllegalArgumentException("Chỉ sản phẩm đang chờ duyệt mới được xử lý.");
        }
    }
}
