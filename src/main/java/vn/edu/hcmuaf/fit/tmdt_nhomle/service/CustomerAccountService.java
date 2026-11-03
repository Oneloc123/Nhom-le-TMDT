package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.CustomerAccountDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.UserDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Nghiệp vụ tài khoản khách hàng; controller không thao tác SQL/DAO trực tiếp. */
public final class CustomerAccountService {
    private CustomerAccountService() { }

    public static User findUser(long userId) {
        return UserDao.findById(userId).orElseThrow(() -> new IllegalArgumentException("Tài khoản không còn tồn tại."));
    }

    public static Map<String, Object> dashboard(long userId) { return CustomerAccountDao.dashboard(userId); }
    public static List<Map<String, Object>> listAddresses(long userId) { return CustomerAccountDao.listAddresses(userId); }
    public static Optional<Map<String, Object>> addressById(long userId, long addressId) { return CustomerAccountDao.addressById(userId, addressId); }
    public static void saveAddress(long userId, long addressId, Map<String, String> form) { CustomerAccountDao.saveAddress(userId, addressId, form); }
    public static boolean deleteAddress(long userId, long addressId) { return CustomerAccountDao.deleteAddress(userId, addressId); }
    public static boolean setDefaultAddress(long userId, long addressId) { return CustomerAccountDao.setDefaultAddress(userId, addressId); }
    public static long countOrders(long userId, String status) { return CustomerAccountDao.countOrders(userId, status); }
    public static List<Map<String, Object>> listOrders(long userId, String status, int page, int size) { return CustomerAccountDao.listOrders(userId, status, page, size); }
    public static Optional<Map<String, Object>> orderDetail(long userId, String orderCode) { return CustomerAccountDao.orderDetail(userId, orderCode); }
    public static boolean cancelOrder(long userId, String orderCode, String reason) { return CustomerAccountDao.cancelOrder(userId, orderCode, reason); }
    public static List<Map<String, Object>> listCustomRequests(long userId) { return CustomerAccountDao.listCustomRequests(userId); }
    public static Map<String, Object> createCustomRequest(long userId, String originalName, String storageUrl,
            String mimeType, long fileSize, String sha256, String material, String technology, int quantity,
            java.math.BigDecimal width, java.math.BigDecimal height, java.math.BigDecimal depth, String note) {
        return CustomerAccountDao.createCustomRequest(userId, originalName, storageUrl, mimeType, fileSize,
                sha256, material, technology, quantity, width, height, depth, note);
    }
    public static Optional<Map<String, Object>> ownedDesignFile(long userId, long fileId) { return CustomerAccountDao.ownedDesignFile(userId, fileId); }

    /** Validate dữ liệu profile ở tầng nghiệp vụ trước khi cập nhật. */
    public static User updateProfile(long userId, String name, String email, String phone,
                                     String birthDate, String gender, boolean notifyEmail) {
        String cleanName = name == null ? "" : name.trim();
        String cleanEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        String cleanPhone = phone == null ? "" : phone.trim();
        String cleanBirth = birthDate == null ? "" : birthDate.trim();
        String cleanGender = gender == null ? "" : gender.trim();
        if (cleanName.length() < 2 || cleanName.length() > 120) throw new IllegalArgumentException("Họ tên phải từ 2 đến 120 ký tự.");
        if (cleanEmail.length() > 190 || !cleanEmail.matches("^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+$"))
            throw new IllegalArgumentException("Email không hợp lệ.");
        if (!cleanPhone.isBlank() && !cleanPhone.matches("[0-9+ .()-]{8,25}")) throw new IllegalArgumentException("Số điện thoại không hợp lệ.");
        if (!cleanBirth.isBlank()) {
            try { if (LocalDate.parse(cleanBirth).isAfter(LocalDate.now())) throw new IllegalArgumentException("Ngày sinh không hợp lệ."); }
            catch (DateTimeParseException e) { throw new IllegalArgumentException("Ngày sinh không hợp lệ."); }
        }
        if (!java.util.Set.of("", "Nam", "Nữ", "Khác").contains(cleanGender)) throw new IllegalArgumentException("Giá trị giới tính không hợp lệ.");
        User user = findUser(userId);
        user.setFullName(cleanName); user.setEmail(cleanEmail); user.setPhone(cleanPhone);
        user.setBirthDate(cleanBirth); user.setGender(cleanGender); user.setNotifyEmail(notifyEmail);
        UserDao.updateProfile(user);
        return findUser(userId);
    }

    public static void updateAvatar(long userId, String avatarUrl) {
        if (avatarUrl == null || !avatarUrl.matches("avatars/[a-f0-9]{32}\\.jpg")) throw new IllegalArgumentException("Đường dẫn ảnh đại diện không hợp lệ.");
        UserDao.updateAvatar(userId, avatarUrl);
    }

    public static List<Map<String, Object>> loginHistory(long userId, int limit) { return UserDao.loginHistory(userId, limit); }
}
