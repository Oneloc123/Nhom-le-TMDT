package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import vn.edu.hcmuaf.fit.tmdt_nhomle.config.AppSecrets;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gửi mail qua SMTP app password. Nếu chưa cấu hình, fallback in ra console (DEV).
 * Mọi OTP dùng {@link #randomCode()} — khi {@link AppSecrets#DEMO_OTP} != rỗng
 * thì trả về đúng giá trị đó (mặc định "111111" cho demo).
 */
public final class MailService {

    private static final Map<String, String> emailBox = new ConcurrentHashMap<>();
    private static final SecureRandom RANDOM = new SecureRandom();

    private MailService() { }

    /** Sinh OTP — ưu tiên DEMO_OTP nếu được cấu hình. */
    public static String randomCode() {
        if (AppSecrets.DEMO_OTP != null && !AppSecrets.DEMO_OTP.isBlank()) {
            return AppSecrets.DEMO_OTP.trim();
        }
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    /** Cho phép tầng xác thực kiểm tra nhanh OTP demo. */
    public static boolean isDemoOtp(String code) {
        return AppSecrets.DEMO_OTP != null && !AppSecrets.DEMO_OTP.isBlank()
                && AppSecrets.DEMO_OTP.trim().equals(code);
    }

    // ============ Đăng ký ============
    public static String sendVerificationCode(String email, String fullName) {
        String code = randomCode();
        sendVerificationCode(email, fullName, code);
        return code;
    }

    public static void sendVerificationCode(String email, String fullName, String code) {
        deliver(email,
                "Xác thực tài khoản PrintKraft 3D",
                "Xin chào " + safeName(fullName) + ",\n\n"
                        + "Mã xác thực tài khoản PrintKraft 3D của bạn là: " + code + "\n"
                        + "Mã có hiệu lực 15 phút.\n\n"
                        + "Nếu bạn không yêu cầu, hãy bỏ qua email này.");
    }

    public static void sendWelcome(String email, String fullName) {
        deliver(email,
                "Chào mừng đến PrintKraft 3D",
                "Xin chào " + safeName(fullName) + ",\n\n"
                        + "Tài khoản của bạn đã được kích hoạt thành công.\n"
                        + "Hãy đăng nhập và bắt đầu hành trình sáng tạo cùng PrintKraft 3D.");
    }

    // ============ Quên mật khẩu ============
    public static String sendPasswordReset(String email, String fullName) {
        String otp = randomCode();
        sendPasswordReset(email, fullName, otp);
        return otp;
    }

    public static void sendPasswordReset(String email, String fullName, String otp) {
        deliver(email,
                "Khôi phục mật khẩu PrintKraft 3D",
                "Xin chào " + safeName(fullName) + ",\n\n"
                        + "Bạn đã yêu cầu khôi phục mật khẩu. Mã OTP: " + otp + "\n"
                        + "Mã có hiệu lực 15 phút.\n\n"
                        + "Nếu không phải bạn, hãy bỏ qua email này.");
    }

    // ============ Đổi mật khẩu ============
    public static void sendPasswordChangedAlert(String email, String fullName) {
        deliver(email,
                "Mật khẩu PrintKraft 3D vừa được thay đổi",
                "Xin chào " + safeName(fullName) + ",\n\n"
                        + "Mật khẩu tài khoản PrintKraft 3D của bạn vừa được thay đổi thành công.\n"
                        + "Nếu không phải bạn thực hiện, hãy liên hệ quản trị viên ngay lập tức.");
    }

    // ============ Đăng nhập ============
    public static void sendLoginAlert(String email, String fullName, String ip, String userAgent) {
        deliver(email,
                "Đăng nhập mới vào PrintKraft 3D",
                "Xin chào " + safeName(fullName) + ",\n\n"
                        + "Tài khoản của bạn vừa được đăng nhập.\n"
                        + "IP       : " + (ip == null ? "?" : ip) + "\n"
                        + "Thiết bị : " + (userAgent == null ? "?" : userAgent) + "\n\n"
                        + "Nếu không phải bạn, hãy đổi mật khẩu ngay.");
    }

    // ============ Gửi thật qua SMTP ============
    private static void deliver(String email, String subject, String body) {
        String plain = "[MAIL] To=" + email + " | Subject=" + subject;
        emailBox.put(email, body);

        if (!AppSecrets.mailConfigured()) {
            System.out.println("[MAIL-DEV] " + plain
                    + " | Body=" + body.replace('\n', ' ')
                    + "  (chưa cấu hình SMTP — dùng AppSecrets để bật)");
            return;
        }
        try {
            Properties p = new Properties();
            p.put("mail.smtp.auth", "true");
            p.put("mail.smtp.host", AppSecrets.MAIL_HOST);
            p.put("mail.smtp.port", String.valueOf(AppSecrets.MAIL_PORT));
            p.put("mail.smtp.starttls.enable", String.valueOf(AppSecrets.MAIL_STARTTLS));
            p.put("mail.smtp.ssl.enable", String.valueOf(AppSecrets.MAIL_SSL_ENABLE));
            p.put("mail.smtp.ssl.trust", AppSecrets.MAIL_HOST);
            p.put("mail.smtp.connectiontimeout", "10000");
            p.put("mail.smtp.timeout", "10000");

            Session session = Session.getInstance(p, new Authenticator() {
                @Override protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                            AppSecrets.MAIL_USERNAME,
                            AppSecrets.MAIL_APP_PASSWORD.replace(" ", "")); // App Password Gmail có thể có khoảng trắng
                }
            });
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(AppSecrets.MAIL_USERNAME, AppSecrets.MAIL_FROM_NAME, "UTF-8"));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email, false));
            msg.setSubject(subject, "UTF-8");
            msg.setText(body, "UTF-8");
            Transport.send(msg);
            System.out.println("[MAIL-SENT] " + plain);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            System.getLogger(MailService.class.getName()).log(System.Logger.Level.WARNING,
                    "Gửi mail thất bại — giữ bản DEV: " + plain, e);
        }
    }

    public static String peek(String email) { return emailBox.get(email); }

    private static String safeName(String n) { return n == null ? "" : n; }
    /** Gửi mã OTP đăng nhập (2FA). Có hiệu lực 5 phút. */
    public static void sendLoginOtp(String email, String fullName, String otp) {
        deliver(email,
                "Mã đăng nhập PrintKraft 3D",
                "Xin chào " + safeName(fullName) + ",\n\n"
                        + "Mã đăng nhập của bạn là: " + otp + "\n"
                        + "Mã có hiệu lực 5 phút.\n\n"
                        + "Nếu không phải bạn, hãy đổi mật khẩu ngay lập tức.");
    }
    /** Thông báo tài khoản vừa bị xoá. */
    public static void sendAccountDeletionAlert(String email, String fullName) {
        deliver(email,
                "Tài khoản PrintKraft 3D đã bị xoá",
                "Xin chào " + safeName(fullName) + ",\n\n"
                        + "Tài khoản PrintKraft 3D của bạn đã được xoá theo yêu cầu.\n"
                        + "Mọi dữ liệu cá nhân sẽ được ẩn khỏi hệ thống. Đơn hàng đã hoàn tất vẫn được lưu\n"
                        + "cho mục đích kế toán theo quy định.\n\n"
                        + "Nếu không phải bạn thực hiện, hãy liên hệ quản trị viên ngay.");
    }

    /** Gửi OTP để xác nhận xoá tài khoản. */
    public static void sendDeleteAccountOtp(String email, String fullName, String otp) {
        deliver(email,
                "Xác nhận xoá tài khoản PrintKraft 3D",
                "Xin chào " + safeName(fullName) + ",\n\n"
                        + "Bạn (hoặc ai đó có quyền truy cập tài khoản của bạn) vừa yêu cầu XOÁ tài khoản.\n"
                        + "Mã xác nhận: " + otp + "\n"
                        + "Mã có hiệu lực 15 phút. Nếu KHÔNG phải bạn, hãy bỏ qua email này — tài khoản sẽ an toàn.");
    }
}