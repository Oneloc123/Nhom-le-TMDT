package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.UserDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.OtpHasher;

import java.sql.*;
import java.time.LocalDateTime;

public final class UserAuthService {
    private UserAuthService() { }

    private static final int OTP_TTL_MINUTES    = 15;
    private static final int OTP_MAX_ATTEMPTS   = 5;
    private static final int OTP_RESEND_SECONDS = 60;

    /* ======================= ĐĂNG KÝ ======================= */
    public static User registerPending(String fullName, String email, String phone, String password) {
        String normalized = email.trim().toLowerCase();
        if (UserDao.findByEmail(normalized).isPresent())
            throw new IllegalArgumentException("Email này đã được đăng ký. Hãy đăng nhập hoặc quên mật khẩu.");

        // ★ Mật khẩu thật → BCrypt
        String hash = BcryptPasswordService.hash(password);
        User user = UserDao.createWithHash(normalized, hash, fullName, phone,
                User.ROLE_CUSTOMER, User.STATUS_PENDING, "local", null);

        // ★ OTP → OtpHasher (SHA-256)
        String otp = MailService.randomCode();
        saveOtp(user.getId(), OtpHasher.hash(otp), "REGISTER");
        MailService.sendVerificationCode(normalized, fullName, otp);
        return user;
    }

    /* ======================= XÁC THỰC ĐĂNG KÝ ======================= */
    public static boolean verifyRegistration(String email, String inputCode) {
        if (inputCode == null || inputCode.isBlank()) return false;
        User user = UserDao.findByEmail(email.trim().toLowerCase()).orElse(null);
        if (user == null) return false;
        if (User.STATUS_ACTIVE.equals(user.getStatus())) return true;

        if (MailService.isDemoOtp(inputCode.trim())) {
            markOtpVerified(user.getId(), "REGISTER");
            UserDao.updateStatus(user.getId(), User.STATUS_ACTIVE);
            return true;
        }

        OtpRow row = loadOtp(user.getId(), "REGISTER");
        if (row == null) return false;
        if (row.expiresAt.isBefore(LocalDateTime.now())) return false;
        if (row.attempts >= OTP_MAX_ATTEMPTS) return false;

        // ★ OtpHasher.verify
        if (!OtpHasher.verify(inputCode.trim(), row.hash)) {
            incrementAttempts(user.getId(), "REGISTER");
            return false;
        }
        markOtpVerified(user.getId(), "REGISTER");
        UserDao.updateStatus(user.getId(), User.STATUS_ACTIVE);
        return true;
    }

    /* ======================= GỬI LẠI MÃ ======================= */
    public static boolean resendRegistrationCode(String email) {
        User user = UserDao.findByEmail(email.trim().toLowerCase()).orElse(null);
        if (user == null || !User.STATUS_PENDING.equals(user.getStatus())) return false;

        OtpRow row = loadOtp(user.getId(), "REGISTER");
        if (row != null && row.createdAt.plusSeconds(OTP_RESEND_SECONDS).isAfter(LocalDateTime.now()))
            return false;

        String otp = MailService.randomCode();
        saveOtp(user.getId(), OtpHasher.hash(otp), "REGISTER");
        MailService.sendVerificationCode(user.getEmail(), user.getFullName(), otp);
        return true;
    }

    /* ======================= ĐĂNG NHẬP (password) ======================= */
    public static User authenticate(String email, String password, String ip, String userAgent) {
        if (email == null || email.isBlank() || password == null)
            throw new IllegalArgumentException("Vui lòng nhập email và mật khẩu.");

        User user = UserDao.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Email hoặc mật khẩu không đúng."));

        if (!User.STATUS_ACTIVE.equals(user.getStatus())) {
            if (User.STATUS_PENDING.equals(user.getStatus()))
                throw new IllegalArgumentException("Tài khoản chưa xác thực email. Hãy hoàn tất xác thực.");
            throw new IllegalArgumentException("Tài khoản đã bị khóa.");
        }

        if (UserDao.isLoginLocked(user.getId()))
            throw new IllegalArgumentException("Tài khoản tạm khóa do đăng nhập sai nhiều lần. Thử lại sau 15 phút.");

        String hash = UserDao.findPasswordHashById(user.getId()).orElse(null);
        // ★ Mật khẩu thật → BCrypt
        if (hash == null || !BcryptPasswordService.verify(password, hash)) {
            UserDao.recordFailedLoginAttempt(user.getId());
            UserDao.recordLoginHistory(user.getId(), "local", "FAILED", ip, userAgent);
            throw new IllegalArgumentException("Email hoặc mật khẩu không đúng.");
        }

        UserDao.recordSuccessfulLogin(user.getId(), "local");
        UserDao.recordLoginHistory(user.getId(), "local", "SUCCESS", ip, userAgent);
        user.setPassword(null);
        return user;
    }

    /* ======================= QUÊN MẬT KHẨU ======================= */
    public static void requestPasswordReset(String email) {
        User user = UserDao.findByEmail(email.trim().toLowerCase()).orElse(null);
        if (user == null || !User.STATUS_ACTIVE.equals(user.getStatus())) return;

        OtpRow row = loadOtp(user.getId(), "RESET");
        if (row != null && row.createdAt.plusSeconds(OTP_RESEND_SECONDS).isAfter(LocalDateTime.now()))
            return;

        String otp = MailService.randomCode();
        saveOtp(user.getId(), OtpHasher.hash(otp), "RESET");   // ★
        MailService.sendPasswordReset(user.getEmail(), user.getFullName(), otp);
    }

    public static boolean resetPassword(String email, String otp, String newPassword) {
        User user = UserDao.findByEmail(email.trim().toLowerCase()).orElse(null);
        if (user == null) return false;

        if (!MailService.isDemoOtp(otp)) {
            OtpRow row = loadOtp(user.getId(), "RESET");
            if (row == null) return false;
            if (row.expiresAt.isBefore(LocalDateTime.now())) return false;
            if (row.attempts >= OTP_MAX_ATTEMPTS) return false;
            if (!OtpHasher.verify(otp.trim(), row.hash)) {   // ★
                incrementAttempts(user.getId(), "RESET");
                return false;
            }
        }
        // Mật khẩu mới → BCrypt
        UserDao.updatePasswordHash(user.getId(), BcryptPasswordService.hash(newPassword));
        markOtpVerified(user.getId(), "RESET");
        MailService.sendPasswordChangedAlert(user.getEmail(), user.getFullName());
        return true;
    }

    /* ======================= Helper OTP ======================= */
    private record OtpRow(String hash, int attempts, LocalDateTime expiresAt, LocalDateTime createdAt) { }

    private static void saveOtp(long userId, String otpHash, String purpose) {
        String sql = "INSERT INTO user_otp(user_id,purpose,otp_hash,attempts,expires_at,created_at) "
                + "VALUES(?,?,?,0,DATE_ADD(NOW(),INTERVAL ? MINUTE),NOW()) "
                + "ON DUPLICATE KEY UPDATE otp_hash=VALUES(otp_hash),attempts=0,"
                + "expires_at=VALUES(expires_at),verified_at=NULL,created_at=NOW()";
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, userId); p.setString(2, purpose); p.setString(3, otpHash);
            p.setInt(4, OTP_TTL_MINUTES);
            p.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException("Không thể lưu OTP.", e); }
    }

    private static OtpRow loadOtp(long userId, String purpose) {
        String sql = "SELECT otp_hash,attempts,expires_at,created_at FROM user_otp "
                + "WHERE user_id=? AND purpose=? AND verified_at IS NULL";
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, userId); p.setString(2, purpose);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) return null;
                return new OtpRow(
                        r.getString(1), r.getInt(2),
                        r.getTimestamp(3).toLocalDateTime(),
                        r.getTimestamp(4).toLocalDateTime());
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private static void incrementAttempts(long userId, String purpose) {
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(
                "UPDATE user_otp SET attempts=attempts+1 WHERE user_id=? AND purpose=? AND verified_at IS NULL")) {
            p.setLong(1, userId); p.setString(2, purpose); p.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private static void markOtpVerified(long userId, String purpose) {
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(
                "UPDATE user_otp SET verified_at=NOW() WHERE user_id=? AND purpose=? AND verified_at IS NULL")) {
            p.setLong(1, userId); p.setString(2, purpose); p.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }
    /* ============================================================
       ĐỔI MẬT KHẨU
       - User local: phải verify currentPassword
       - OAuth user: hash == null → cho phép đặt mật khẩu lần đầu (bỏ qua currentPassword)
       ============================================================ */
    public static void changePassword(int userId, String currentPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 8)
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 8 ký tự.");

        User user = UserDao.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));

        if (!User.STATUS_ACTIVE.equals(user.getStatus()))
            throw new IllegalArgumentException("Tài khoản không còn hoạt động.");

        String storedHash = UserDao.findPasswordHashById(userId).orElse(null);
        boolean hasPassword = storedHash != null && !storedHash.isBlank();

        if (hasPassword) {
            // User đã có mật khẩu → bắt buộc verify
            if (currentPassword == null || currentPassword.isBlank())
                throw new IllegalArgumentException("Vui lòng nhập mật khẩu hiện tại.");
            if (!BcryptPasswordService.verify(currentPassword, storedHash))
                throw new IllegalArgumentException("Mật khẩu hiện tại không đúng.");
        }
        // Nếu !hasPassword → đây là OAuth user đặt mật khẩu lần đầu

        if (hasPassword && BcryptPasswordService.verify(newPassword, storedHash))
            throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại.");

        UserDao.updatePasswordHash(userId, BcryptPasswordService.hash(newPassword));
        UserDao.recordSecurityEvent(userId, "PASSWORD_CHANGED",
                "Đổi mật khẩu từ IP " + "(xem log)");

        try {
            MailService.sendPasswordChangedAlert(user.getEmail(), user.getFullName());
        } catch (RuntimeException ignored) { }
    }

    /* ============================================================
       XOÁ TÀI KHOẢN (soft delete)
       - Bắt buộc verify password (nếu user có password)
       - Chặn nếu còn đơn đang xử lý
       - Soft delete: giữ data cho audit + legal, KHÔNG xoá row
       ============================================================ */
    public static void deleteAccount(int userId, String password) {
        User user = UserDao.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));

        if (user.isDeleted())
            throw new IllegalArgumentException("Tài khoản đã bị xoá trước đó.");

        if (UserDao.hasInProgressOrders(userId))
            throw new IllegalArgumentException(
                    "Bạn còn đơn hàng chưa hoàn tất. Vui lòng đợi giao xong hoặc huỷ đơn trước khi xoá tài khoản.");

        String storedHash = UserDao.findPasswordHashById(userId).orElse(null);
        boolean hasPassword = storedHash != null && !storedHash.isBlank();

        if (hasPassword) {
            if (password == null || password.isBlank())
                throw new IllegalArgumentException("Vui lòng nhập mật khẩu để xác nhận xoá tài khoản.");
            if (!BcryptPasswordService.verify(password, storedHash))
                throw new IllegalArgumentException("Mật khẩu không đúng.");
        }
        // OAuth user không có password → nên xác nhận qua OTP email trước đó
        // (xem deleteAccountWithOtp bên dưới)

        UserDao.recordSecurityEvent(userId, "ACCOUNT_DELETED", "Xoá tài khoản bởi chính chủ.");
        UserDao.softDelete(userId);

        try {
            MailService.sendAccountDeletionAlert(user.getEmail(), user.getFullName());
        } catch (RuntimeException ignored) { }
    }

    /* ============================================================
       XOÁ TÀI KHOẢN QUA OTP EMAIL (dùng cho OAuth user hoặc ai muốn an toàn hơn)
       Flow: requestDeleteAccountOtp() → user nhận mail → confirmDeleteAccountOtp()
       ============================================================ */
    public static void requestDeleteAccountOtp(int userId) {
        User user = UserDao.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));
        if (user.isDeleted())
            throw new IllegalArgumentException("Tài khoản đã bị xoá.");
        if (UserDao.hasInProgressOrders(userId))
            throw new IllegalArgumentException(
                    "Bạn còn đơn hàng chưa hoàn tất. Không thể xoá tài khoản lúc này.");

        OtpRow existing = loadOtp(userId, "DELETE");
        if (existing != null
                && existing.createdAt.plusSeconds(OTP_RESEND_SECONDS).isAfter(LocalDateTime.now()))
            throw new IllegalArgumentException("Vui lòng đợi " + OTP_RESEND_SECONDS
                    + " giây trước khi yêu cầu mã mới.");

        String otp = MailService.randomCode();
        saveOtp(userId, OtpHasher.hash(otp), "DELETE");
        MailService.sendDeleteAccountOtp(user.getEmail(), user.getFullName(), otp);
    }

    public static void confirmDeleteAccountOtp(int userId, String otp) {
        User user = UserDao.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));
        if (user.isDeleted())
            throw new IllegalArgumentException("Tài khoản đã bị xoá.");
        if (UserDao.hasInProgressOrders(userId))
            throw new IllegalArgumentException("Bạn còn đơn hàng chưa hoàn tất.");

        if (!MailService.isDemoOtp(otp)) {
            OtpRow row = loadOtp(userId, "DELETE");
            if (row == null)
                throw new IllegalArgumentException("Chưa có mã xoá tài khoản. Hãy yêu cầu mã mới.");
            if (row.expiresAt.isBefore(LocalDateTime.now()))
                throw new IllegalArgumentException("Mã đã hết hạn. Vui lòng yêu cầu mã mới.");
            if (row.attempts >= OTP_MAX_ATTEMPTS)
                throw new IllegalArgumentException("Bạn đã nhập sai quá nhiều lần. Yêu cầu mã mới.");
            if (!OtpHasher.verify(otp.trim(), row.hash)) {
                incrementAttempts(userId, "DELETE");
                throw new IllegalArgumentException("Mã OTP không đúng.");
            }
        }

        markOtpVerified(userId, "DELETE");
        UserDao.recordSecurityEvent(userId, "ACCOUNT_DELETED", "Xoá tài khoản qua OTP email.");
        UserDao.softDelete(userId);

        try {
            MailService.sendAccountDeletionAlert(user.getEmail(), user.getFullName());
        } catch (RuntimeException ignored) { }
    }
}