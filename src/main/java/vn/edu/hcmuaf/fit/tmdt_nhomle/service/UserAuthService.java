package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.UserAuthDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.UserDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Optional;

/** Nghiệp vụ đăng ký, đăng nhập và khôi phục mật khẩu của khách hàng. */
public final class UserAuthService {
    private UserAuthService() { }

    public static User register(String name, String email, String phone, String password) {
        validateName(name); validateEmail(email); validatePhone(phone); BcryptPasswordService.validate(password);
        if (UserDao.findByEmail(email).isPresent()) throw new IllegalArgumentException("Email này đã được đăng ký.");
        try {
            return UserDao.createWithHash(email.trim().toLowerCase(Locale.ROOT), BcryptPasswordService.hash(password),
                    name.trim(), phone == null ? null : phone.trim(), User.ROLE_CUSTOMER, User.STATUS_ACTIVE, "local", null);
        } catch (DataAccessException ex) { throw ex; }
    }

    /** Đăng ký ở trạng thái PENDING; tài khoản chỉ hoạt động sau khi OTP email hợp lệ. */
    public static User registerPending(String name, String email, String phone, String password) {
        validateName(name); validateEmail(email); validatePhone(phone); BcryptPasswordService.validate(password);
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (UserDao.findByEmail(normalized).isPresent()) throw new IllegalArgumentException("Email này đã được đăng ký.");
        User pending = UserDao.createWithHash(normalized, BcryptPasswordService.hash(password), name.trim(),
                phone == null ? null : phone.trim(), User.ROLE_CUSTOMER, User.STATUS_PENDING, "local", null);
        String otp = MailService.randomCode();
        if (!UserAuthDao.saveEmailVerificationToken(pending.getId(), sha256(otp))) {
            throw new IllegalArgumentException("Không thể tạo mã xác thực lúc này. Vui lòng thử lại sau ít nhất 60 giây.");
        }
        MailService.sendVerificationCode(pending.getEmail(), pending.getDisplayName(), otp);
        return pending;
    }

    public static boolean verifyRegistration(String email, String otp) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (otp == null || !otp.matches("[0-9]{6}")) return false;
        User pending = UserDao.findByEmail(normalized).orElse(null);
        if (pending == null || !User.STATUS_PENDING.equals(pending.getStatus())) return false;
        return UserAuthDao.verifyEmail(pending.getId(), sha256(otp));
    }

    public static boolean resendRegistrationCode(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        User pending = UserDao.findByEmail(normalized).orElse(null);
        if (pending == null || !User.STATUS_PENDING.equals(pending.getStatus())) return false;
        String otp = MailService.randomCode();
        if (!UserAuthDao.saveEmailVerificationToken(pending.getId(), sha256(otp))) return false;
        MailService.sendVerificationCode(pending.getEmail(), pending.getDisplayName(), otp);
        return true;
    }

    public static User authenticate(String email, String password, String ip, String userAgent) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        Optional<User> found = UserDao.findByEmail(normalized);
        if (found.isEmpty()) throw new IllegalArgumentException("Email hoặc mật khẩu không chính xác.");
        User user = found.get();
        if (!User.STATUS_ACTIVE.equals(user.getStatus())) throw new IllegalArgumentException("Tài khoản chưa hoạt động hoặc đã bị khóa. Vui lòng liên hệ hỗ trợ.");
        if (UserDao.isLoginLocked(user.getId())) throw new IllegalArgumentException("Tài khoản tạm khóa do đăng nhập sai nhiều lần. Vui lòng thử lại sau 15 phút.");
        if (!BcryptPasswordService.verify(password, user.getPassword())) {
            boolean locked = UserDao.recordFailedLoginAttempt(user.getId());
            UserDao.recordLoginHistory(user.getId(), "local", "FAILED", ip, userAgent);
            throw new IllegalArgumentException(locked
                    ? "Tài khoản tạm khóa 15 phút do đăng nhập sai quá nhiều lần."
                    : "Email hoặc mật khẩu không chính xác.");
        }
        // Nâng cấp tự động hash PBKDF2 cũ sang BCrypt sau lần đăng nhập hợp lệ.
        if (BcryptPasswordService.needsUpgrade(user.getPassword())
                && password.getBytes(StandardCharsets.UTF_8).length <= 72) {
            UserDao.updatePasswordHash(user.getId(), BcryptPasswordService.hash(password));
        }
        UserDao.recordSuccessfulLogin(user.getId(), "local");
        UserDao.recordLoginHistory(user.getId(), "local", "SUCCESS", ip, userAgent);
        return UserDao.findById(user.getId()).orElse(user);
    }

    public static void requestPasswordReset(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        User user = UserDao.findByEmail(normalized).orElse(null);
        if (user == null || !user.isActive()) return;
        String otp = MailService.randomCode();
        if (UserAuthDao.saveResetToken(user.getId(), sha256(otp))) {
            MailService.sendPasswordReset(user.getEmail(), user.getDisplayName(), otp);
        }
    }

    public static boolean resetPassword(String email, String otp, String password) {
        BcryptPasswordService.validate(password);
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        User user = UserDao.findByEmail(normalized).orElse(null);
        if (user == null || !user.isActive() || otp == null || !otp.matches("[0-9]{6}")) return false;
        boolean reset = UserAuthDao.resetPassword(user.getId(), sha256(otp), BcryptPasswordService.hash(password));
        if (reset) {
            UserDao.recordSecurityEvent(user.getId(), "PASSWORD_RESET", "Khách hàng đặt lại mật khẩu qua mã OTP.");
            vn.edu.hcmuaf.fit.tmdt_nhomle.security.UserSessionRegistry.invalidateOthers(user.getId(), null);
        }
        return reset;
    }

    public static boolean verifyPassword(User user, String password) {
        if (user == null) return false;
        String hash = UserDao.findPasswordHashById(user.getId()).orElse(null);
        return BcryptPasswordService.verify(password, hash);
    }

    public static void deleteAccount(User user, String currentPassword) {
        if (!verifyPassword(user, currentPassword)) throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác.");
        if (UserDao.hasInProgressOrders(user.getId())) throw new IllegalArgumentException(
                "Không thể xóa tài khoản khi vẫn còn đơn hàng đang xử lý. Hãy chờ đơn hoàn tất hoặc liên hệ hỗ trợ.");
        UserDao.recordSecurityEvent(user.getId(), "ACCOUNT_SOFT_DELETE", "Khách hàng yêu cầu xóa mềm tài khoản.");
        UserDao.softDelete(user.getId());
        vn.edu.hcmuaf.fit.tmdt_nhomle.security.UserSessionRegistry.invalidateOthers(user.getId(), null);
    }

    public static void changePassword(User user, String currentPassword, String nextPassword) {
        if (user == null) throw new IllegalArgumentException("Phiên đăng nhập không hợp lệ.");
        BcryptPasswordService.validate(nextPassword);
        User saved = UserDao.findById(user.getId()).orElseThrow(() -> new IllegalArgumentException("Tài khoản không còn tồn tại."));
        String currentHash = UserDao.findPasswordHashById(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không còn tồn tại."));
        if (!BcryptPasswordService.verify(currentPassword, currentHash)) throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác.");
        if (BcryptPasswordService.verify(nextPassword, currentHash)) throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại.");
        UserDao.updatePasswordHash(user.getId(), BcryptPasswordService.hash(nextPassword));
        UserDao.recordSecurityEvent(user.getId(), "PASSWORD_CHANGED", "Khách hàng đổi mật khẩu.");
        MailService.sendPasswordChangedAlert(saved.getEmail(), saved.getDisplayName());
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(64);
            for (byte b : digest) out.append(String.format("%02x", b));
            return out.toString();
        } catch (Exception e) { throw new IllegalStateException("Không hỗ trợ SHA-256.", e); }
    }
    private static void validateName(String value) {
        if (value == null || value.trim().length() < 2 || value.trim().length() > 120) throw new IllegalArgumentException("Họ tên cần từ 2 đến 120 ký tự.");
    }
    private static void validateEmail(String value) {
        if (value == null || value.length() > 190 || !value.trim().matches("^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+$"))
            throw new IllegalArgumentException("Địa chỉ email không hợp lệ.");
    }
    private static void validatePhone(String value) {
        if (value != null && !value.isBlank() && !value.trim().matches("[0-9+ .()-]{8,25}")) throw new IllegalArgumentException("Số điện thoại không hợp lệ.");
    }
}
