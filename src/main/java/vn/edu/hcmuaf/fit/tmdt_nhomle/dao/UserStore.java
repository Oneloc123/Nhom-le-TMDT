package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;

import java.util.List;
import java.util.Optional;

public class UserStore {

    private UserStore() {
    }

    public static List<User> findAll() {
        return UserDao.findAll();
    }

    public static Optional<User> findById(int id) {
        return UserDao.findById(id);
    }

    public static Optional<User> findByEmail(String email) {
        return UserDao.findByEmail(email);
    }

    public static User create(String email, String plainPassword, String fullName, String phone,
                              String address, String role, String status, String provider) {
        User user = UserDao.create(email, plainPassword, fullName, phone, role, status, provider);
        user.setAddress(address == null ? "" : address);
        return user;
    }

    /**
     * Tạo tài khoản từ hash PBKDF2 đã tính trước — dùng cho luồng đăng ký OTP
     * demo để không giữ mật khẩu plaintext trong phiên máy chủ.
     */
    public static User createWithHash(String email, String passwordHash, String fullName,
                                      String phone, String address, String role, String status) {
        User user = UserDao.createWithHash(email, passwordHash, fullName, phone,
                role, status, "local");
        user.setAddress(address == null ? "" : address);
        return user;
    }

    public static void touchLastLogin(int userId) {
        UserDao.touchLastLogin(userId);
    }

    public static int recordFailedLogin(int userId, int attemptsBefore, int lockThreshold,
                                        java.time.LocalDateTime lockUntil) {
        return UserDao.recordFailedLogin(userId, attemptsBefore, lockThreshold, lockUntil);
    }

    public static void clearLoginLock(int userId) {
        UserDao.clearLoginLock(userId);
    }

    public static void markEmailVerified(int userId) {
        UserDao.markEmailVerified(userId);
    }

    public static void setSoftDeleted(int userId, boolean deleted) {
        UserDao.setSoftDeleted(userId, deleted);
    }

    public static User update(User u) {
        UserDao.updateProfile(u);
        UserDao.updateRole(u.getId(), u.getRole());
        UserDao.updateStatus(u.getId(), u.getStatus());
        return u;
    }

    public static void updatePassword(int userId, String plainPassword) {
        UserDao.updatePassword(userId, plainPassword);
    }

    public static long countByRole(String role) {
        return UserDao.countByRole(role);
    }

    public static long countByStatus(String status) {
        return UserDao.countByStatus(status);
    }
}
