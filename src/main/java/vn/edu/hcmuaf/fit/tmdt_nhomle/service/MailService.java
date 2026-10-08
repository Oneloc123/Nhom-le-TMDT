package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mô phỏng dịch vụ gửi email (xác thực đăng ký, khôi phục mật khẩu).
 * Không kết nối SMTP thật — in nội dung ra console và lưu vào
 * emailBox để trang demo có thể hiển thị mã xác thực.
 */
public final class MailService {

    /** mailbox tạm: email -> nội dung mail gần nhất */
    private static final Map<String, String> emailBox = new ConcurrentHashMap<>();

    private MailService() {
    }

    /** Gửi mã xác thực kích hoạt tài khoản. Trả về mã 6 số. */
    public static String sendVerificationCode(String email, String fullName) {
        String code = String.format("%06d", (int) (Math.random() * 1_000_000));
        String body = "Xin chao " + fullName + ",\n"
                + "Ma xac thuc tai khoan PrintKraft 3D cua ban la: " + code + "\n"
                + "Ma co hieu luc trong 15 phut.";
        emailBox.put(email, body);
        System.out.println("[MAIL -> " + email + "] " + body.replace('\n', ' '));
        return code;
    }

    /** Gửi link/OTP khôi phục mật khẩu. Trả về OTP 6 số. */
    public static String sendPasswordReset(String email, String fullName) {
        String otp = String.format("%06d", (int) (Math.random() * 1_000_000));
        String body = "Xin chao " + fullName + ",\n"
                + "Ban da yeu cau khoi phuc mat khau PrintKraft 3D. OTP: " + otp + "\n"
                + "Neu ban khong yeu cau, hay bo qua email nay.";
        emailBox.put(email, body);
        System.out.println("[MAIL -> " + email + "] " + body.replace('\n', ' '));
        return otp;
    }

    /** Nội dung mail gần nhất của email (dùng cho demo hiển thị mã). */
    public static String peek(String email) {
        return emailBox.get(email);
    }
}
