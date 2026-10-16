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
        String body = "Xin chao " + fullName + ",\n"
                + "Ma xac thuc tai khoan PrintKraft 3D cua ban la: " + code + "\n"
                + "Ma co hieu luc trong 15 phut.";
        emailBox.put(email, body);
        System.out.println("[MAIL-DEV -> " + email + "] " + body.replace('\n', ' '));
        return code;
    }

    public static String sendPasswordReset(String email, String fullName) {
        String otp = randomCode();
        String body = "Xin chao " + fullName + ",\n"
                + "Ban da yeu cau khoi phuc mat khau PrintKraft 3D. OTP: " + otp + "\n"
                + "Neu ban khong yeu cau, hay bo qua email nay.";
        emailBox.put(email, body);
        System.out.println("[MAIL-DEV -> " + email + "] " + body.replace('\n', ' '));
        return otp;
    }

    public static String peek(String email) {
        return emailBox.get(email);
    }
}
