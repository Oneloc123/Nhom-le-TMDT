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
        User user = UserDao.create(email, plainPassword, fullName, phone, role, status);
        user.setAddress(address == null ? "" : address);
        user.setProvider(provider == null ? "local" : provider);
        return user;
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
