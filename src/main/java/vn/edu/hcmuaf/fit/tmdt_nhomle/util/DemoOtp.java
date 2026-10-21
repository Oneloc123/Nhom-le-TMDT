package vn.edu.hcmuaf.fit.tmdt_nhomle.util;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Demo OTP — CHỈ DÙNG CHO MÔI TRƯNG DEMO/KIỂM THỬ.
 *
 * <p>Toàn bộ luồng đăng ký, đăng nhập OTP và khôi phục mật khẩu dùng một mã cố
 * định {@link #CODE} = {@code 111111}. Hệ thống <b>không gửi email thật</b>,
 * không mở kết nối SMTP, không gọi API mail nào. Mã được xác thực ở SERVER
 * (không tin trường ẩn hay so sánh phía JavaScript).
 *
 * <p>Bảo vệ ở mức demo: thách thức bị gắn với email đã chuẩn hóa và mục đích
 * ({@link #PURPOSE_REGISTER}/{@link #PURPOSE_LOGIN}/{@link #PURPOSE_RESET_PASSWORD}),
 * hết hạn sau {@link #TTL_MILLIS}, giới hạn {@link #MAX_ATTEMPTS} lần nhập sai
 * và bị tiêu thụ sau khi xác thực thành công. KHÔNG an toàn cho production —
 * phải thay bằng OTP ngẫu nhiên + kênh gửi thật khi triển khai thật.
 */
public final class DemoOtp {

    /** Mã OTP demo duy nhất được chấp nhận. KHÔNG dùng ngoài môi trường demo. */
    public static final String CODE = "111111";

    /** Thời hiệu lực của một thách thức OTP: 5 phút. */
    public static final long TTL_MILLIS = 5 * 60 * 1000L;

    /** Số lần nhập sai tối đa cho mỗi thách thức. */
    public static final int MAX_ATTEMPTS = 5;

    public static final String PURPOSE_REGISTER = "REGISTER";
    public static final String PURPOSE_LOGIN = "LOGIN";
    public static final String PURPOSE_RESET_PASSWORD = "RESET_PASSWORD";

    /** Tên attribute lưu {@link Challenge} trong HttpSession. */
    public static final String SESSION_ATTR = "demoOtpChallenge";

    /** Ghi chú bắt buộc phải hiển thị trên UI các luồng OTP demo. */
    public static final String DEMO_NOTE =
            "Chế độ demo: mã OTP kiểm thử là 111111; hệ thống chưa gửi email thật.";

    private DemoOtp() {
    }

    /** Chuẩn hóa email để so sánh và lưu trong thách thức (giữ nguyên case host). */
    public static String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        String trimmed = email.trim().toLowerCase(java.util.Locale.ROOT);
        return trimmed;
    }

    /** So sánh không rò rỉ thời gian giữa mã người dùng nhập và mã lưu trữ. */
    public static boolean matches(String input, String expected) {
        if (input == null || expected == null || input.length() != expected.length()) {
            return false;
        }
        return MessageDigest.isEqual(
                input.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }

    /** Kết quả xác thực OTP — dùng được cho cả message lỗi và kiểm thử. */
    public enum VerifyResult {
        OK, MISSING, EXPIRED, MISMATCH, TOO_MANY_ATTEMPTS, ALREADY_USED, BOUND_TO_OTHER
    }

    /**
     * Một thách thức OTP demo: gắn email + mục đích + thời điểm phát hành +
     * bộ đếm lỗi + cờ đã dùng. Lưu trong phiên người dùng (không ghi DB,
     * không in ra log).
     */
    public static final class Challenge implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String email;
        private final String purpose;
        private final long issuedAt;
        private int attempts;
        private boolean used;

        private Challenge(String email, String purpose, long issuedAt) {
            this.email = email;
            this.purpose = purpose;
            this.issuedAt = issuedAt;
        }

        public static Challenge issue(String rawEmail, String purpose) {
            String normalized = normalizeEmail(rawEmail);
            if (normalized.isEmpty()) {
                throw new IllegalArgumentException("Email không được để trống.");
            }
            if (!PURPOSE_REGISTER.equals(purpose) && !PURPOSE_LOGIN.equals(purpose)
                    && !PURPOSE_RESET_PASSWORD.equals(purpose)) {
                throw new IllegalArgumentException("Mục đích OTP không hợp lệ.");
            }
            return new Challenge(normalized, purpose, System.currentTimeMillis());
        }

        public String email() {
            return email;
        }

        public String purpose() {
            return purpose;
        }

        public long issuedAt() {
            return issuedAt;
        }

        public int attempts() {
            return attempts;
        }

        public boolean used() {
            return used;
        }

        public boolean isExpired(long now) {
            return now - issuedAt > TTL_MILLIS;
        }

        /**
         * Xác thực OTP cho đúng email + mục đích. Tăng bộ đếm lỗi khi sai,
         * chỉ trả {@link VerifyResult#OK} đúng một lần (dùng một lần duy nhất).
         */
        public VerifyResult verify(String rawEmail, String expectedPurpose, String code, long now) {
            if (used) {
                return VerifyResult.ALREADY_USED;
            }
            if (isExpired(now)) {
                return VerifyResult.EXPIRED;
            }
            if (!email.equals(normalizeEmail(rawEmail)) || !purpose.equals(expectedPurpose)) {
                // Thách thức thuộc email/mục đích khác — không tiêu thụ lượt thử.
                return VerifyResult.BOUND_TO_OTHER;
            }
            if (attempts >= MAX_ATTEMPTS) {
                return VerifyResult.TOO_MANY_ATTEMPTS;
            }
            if (code == null || code.isBlank()) {
                return VerifyResult.MISSING;
            }
            if (!matches(code.trim(), CODE)) {
                attempts++;
                return attempts >= MAX_ATTEMPTS
                        ? VerifyResult.TOO_MANY_ATTEMPTS
                        : VerifyResult.MISMATCH;
            }
            used = true;
            return VerifyResult.OK;
        }
    }
}
