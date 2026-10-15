package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Lớp facade giữ nguyên API tĩnh cũ (trước đây là kho in-memory) nhưng nay
 * được ghi/đọc từ bảng `users` thật qua {@link UserDao}.
 * Mật khẩu được băm (PBKDF2) khi tạo/đổi — không lưu plaintext.
 *
 * Lưu ý: schema `users` không có cột address/provider; hai trường đó trên
 * model chỉ mang giá trị tạm trong phiên làm việc (xem README — Known limitations).
 */
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

    /** Tạo tài khoản; plainPassword sẽ được băm trước khi lưu. */
    public static User create(String email, String plainPassword, String fullName, String phone,
                              String address, String role, String status, String provider) {
        User user = UserDao.create(email, plainPassword, fullName, phone, role, status);
        user.setAddress(address == null ? "" : address);
        user.setProvider(provider == null ? "local" : provider);
        return user;
    }

    /** Lưu các trường hiển thị (họ tên, SĐT, vai trò, trạng thái). */
    public static User update(User u) {
        UserDao.updateProfile(u);
        UserDao.updateRole(u.getId(), u.getRole());
        UserDao.updateStatus(u.getId(), u.getStatus());
        return u;
    }

    /** Đổi mật khẩu (tự băm). */
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
