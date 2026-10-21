package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

public final class AdminAccountPolicy {
    private AdminAccountPolicy() { }

    public static void assertCanDisable(long targetId, long actorId, int activeAdminCount) {
        if (targetId == actorId) {
            throw new IllegalArgumentException("Bạn không thể tự khóa tài khoản Admin đang sử dụng.");
        }
        if (activeAdminCount <= 1) {
            throw new IllegalArgumentException("Không thể khóa Admin đang hoạt động cuối cùng.");
        }
    }
}
