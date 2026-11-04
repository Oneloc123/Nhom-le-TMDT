package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.UserDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;

import java.util.Optional;
import java.util.UUID;

/** Tìm hoặc tạo user từ kết quả OAuth (Google/Facebook). */
public final class OAuthUserService {
    private OAuthUserService() { }

    public static User findOrCreate(String provider, String providerUserId, String email, String fullName) {
        if (email == null || email.isBlank())
            throw new IllegalArgumentException("Tài khoản " + provider + " không trả về email — không thể đăng nhập.");
        Optional<User> existing = UserDao.findByEmail(email);
        if (existing.isPresent()) {
            User u = existing.get();
            if (u.isDeleted()) throw new IllegalArgumentException("Tài khoản đã bị khóa.");
            if (!"ACTIVE".equals(u.getStatus()))
                throw new IllegalArgumentException("Tài khoản chưa kích hoạt hoặc đã bị khóa.");
            // Liên kết provider nếu user local đăng nhập bằng MXH — Google/FB đã xác thực email.
            if (u.getProvider() == null || "local".equals(u.getProvider()))
                UserDao.updateProvider(u.getId(), provider, providerUserId);
            return UserDao.findById(u.getId()).orElse(u);
        }
        // Tạo user mới, không có mật khẩu (random hash để không login password được).
        String randomHash = BcryptPasswordService.hash(UUID.randomUUID().toString());
        String name = (fullName == null || fullName.isBlank()) ? email : fullName;
        return UserDao.createWithHash(email, randomHash, name, null,
                User.ROLE_CUSTOMER, User.STATUS_ACTIVE, provider, providerUserId);
    }
}