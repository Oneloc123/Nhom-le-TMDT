package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import java.util.List;
import java.util.Optional;

/** Adapter giữ chữ ký cũ để admin và OAuth không bị gãy trong đợt thay module khách hàng. */
public final class UserStore {
    private UserStore() { }
    public static List<User> findAll() { return UserDao.findAll(); }
    public static Optional<User> findById(int id) { return UserDao.findById(id); }
    public static Optional<User> findByEmail(String email) {
        return UserDao.findByEmail(email).map(user -> { user.setPassword(null); return user; });
    }
    public static User create(String email, String password, String name, String phone, String address,
                              String role, String status, String provider) {
        User user = UserDao.createWithHash(email, vn.edu.hcmuaf.fit.tmdt_nhomle.service.BcryptPasswordService.hash(password),
                name, phone, role, status, provider, null);
        user.setAddress(address == null ? "" : address);
        return user;
    }
    public static User update(User user) {
        UserDao.updateProfile(user); UserDao.updateRole(user.getId(), user.getRole()); UserDao.updateStatus(user.getId(), user.getStatus());
        return user;
    }
    public static void updatePassword(int userId, String password) {
        UserDao.updatePasswordHash(userId, vn.edu.hcmuaf.fit.tmdt_nhomle.service.BcryptPasswordService.hash(password));
    }
    public static long countByRole(String role) { return UserDao.countByRole(role); }
    public static long countByStatus(String status) { return UserDao.countByStatus(status); }
}
