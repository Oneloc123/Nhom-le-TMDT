package vn.edu.hcmuaf.fit.tmdt_nhomle.util;

/** Kiểm tra URL quay lại để không tạo lỗ hổng open redirect. */
public final class Redirects {
    private Redirects() {
    }

    public static String safeLocalPath(String value) {
        if (value == null || !value.startsWith("/") || value.startsWith("//")
                || value.contains("\\") || value.contains("\r") || value.contains("\n")) {
            return null;
        }
        return value;
    }
}
