package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.security.SecureRandom;

/**
 * Kênh gửi mail DEV.
 *
 * <p>Chưa tích hợp SMTP thật (chưa có credentials nhà cung cấp mail trong môi trường
 * phát triển) nên mã xác thực/OTP được in ra server console để kiểm thử luồng đăng ký,
 * đăng nhập và khôi phục mật khẩu. Khi có SMTP, chỉ cần thay phần thân hàm này bằng
 * lần gọi API mail với cấu hình lấy từ biến môi trường (xem db.properties.example)
 * — các lớp khác không đổi. KHÔNG in bí mật này vào trang người dùng.</p>
 */
public final class MailService {

    private static final Map<String, String> emailBox = new ConcurrentHashMap<>();
    private static final SecureRandom RANDOM = new SecureRandom();

    private MailService() {
    }

    /** Sinh mã 6 chữ số bằng SecureRandom (không dùng Math.random cho bí mật). */
    public static String randomCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    public static String sendVerificationCode(String email, String fullName) {
        String code = randomCode();
        sendVerificationCode(email, fullName, code);
        return code;
    }

    /** Mã đã được hash trong DB trước khi đưa cho tầng gửi email. */
    public static void sendVerificationCode(String email, String fullName, String code) {
        String body = "Xin chao " + fullName + ",\n"
                + "Ma xac thuc tai khoan PrintKraft 3D cua ban la: " + code + "\n"
                + "Ma co hieu luc trong 15 phut.";
        emailBox.put(email, body);
        System.out.println("[MAIL-DEV -> " + email + "] " + body.replace('\n', ' '));
    }

    public static String sendPasswordReset(String email, String fullName) {
        String otp = randomCode();
        sendPasswordReset(email, fullName, otp);
        return otp;
    }

    /** Gửi mã đã được lưu hash vào DB trước đó, tránh gửi mã nếu đang bị giới hạn tần suất. */
    public static void sendPasswordReset(String email, String fullName, String otp) {
        String body = "Xin chao " + fullName + ",\n"
                + "Ban da yeu cau khoi phuc mat khau PrintKraft 3D. OTP: " + otp + "\n"
                + "Ma co hieu luc trong 15 phut. Neu ban khong yeu cau, hay bo qua email nay.";
        emailBox.put(email, body);
        System.out.println("[MAIL-DEV -> " + email + "] " + body.replace('\n', ' '));
    }

    public static void sendPasswordChangedAlert(String email, String fullName) {
        String body = "Xin chào " + (fullName == null ? "" : fullName) + ", mật khẩu PrintKraft của bạn vừa được thay đổi. "
                + "Nếu không phải bạn thực hiện, hãy liên hệ quản trị viên ngay.";
        emailBox.put(email, body);
        System.out.println("[MAIL-DEV -> " + email + "] " + body);
    }

    public static String peek(String email) {
        return emailBox.get(email);
    }
}
