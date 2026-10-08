package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Kho người dùng in-memory (mock, không có DB).
 * Có sẵn dữ liệu mẫu: 1 admin + nhiều khách hàng đặt in 3D.
 */
public class UserStore {

    private static final Map<Integer, User> USERS = new LinkedHashMap<>();
    private static final AtomicInteger SEQ = new AtomicInteger(100);

    static {
        add(new User(nextId(), "admin@printkraft.vn", "admin123",
                "Quan tri vien", "0901000001",
                "12 Nguyen Hue, Quan 1, TP.HCM", User.ROLE_ADMIN, User.STATUS_ACTIVE, "local", "2025-01-05"));

        add(new User(nextId(), "minhtuan@gmail.com", "123456",
                "Nguyen Minh Tuan", "0912345678",
                "45 Tran Hung Dao, Quan 5, TP.HCM", User.ROLE_CUSTOMER, User.STATUS_ACTIVE, "local", "2025-03-11"));

        add(new User(nextId(), "trang.pham@gmail.com", "123456",
                "Pham Thi Trang", "0987654321",
                "88 Le Loi, Da Nang", User.ROLE_CUSTOMER, User.STATUS_ACTIVE, "google", "2025-04-02"));

        add(new User(nextId(), "hoinam@yahoo.com", "123456",
                "Le Hoi Nam", "0903333444",
                "27 Le Duan, Hue", User.ROLE_CUSTOMER, User.STATUS_LOCKED, "local", "2025-05-20"));

        add(new User(nextId(), "quynh.anh@outlook.com", "123456",
                "Vu Quynh Anh", "0977888999",
                "160 Xa Lo Ha Noi, TP. Thu Duc", User.ROLE_CUSTOMER, User.STATUS_ACTIVE, "facebook", "2025-06-14"));

        add(new User(nextId(), "viet.hoang@gmail.com", "123456",
                "Do Viet Hoang", "0938123456",
                "9 Pham Ngu Lao, Quan 1, TP.HCM", User.ROLE_CUSTOMER, User.STATUS_PENDING, "local", "2025-07-01"));
    }

    private UserStore() {
    }

    private static int nextId() {
        return SEQ.incrementAndGet();
    }

    private static void add(User u) {
        USERS.put(u.getId(), u);
    }

    public static List<User> findAll() {
        return new ArrayList<>(USERS.values());
    }

    public static Optional<User> findById(int id) {
        return Optional.ofNullable(USERS.get(id));
    }

    public static Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return USERS.values().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email.trim()))
                .findFirst();
    }

    public static User create(String email, String password, String fullName, String phone,
                              String address, String role, String status, String provider) {
        User u = new User(nextId(), email.trim(), password, fullName, phone,
                address, role, status, provider, java.time.LocalDate.now().toString());
        USERS.put(u.getId(), u);
        return u;
    }

    public static User update(User u) {
        USERS.put(u.getId(), u);
        return u;
    }

    public static long countByRole(String role) {
        return USERS.values().stream().filter(u -> u.getRole().equals(role)).count();
    }

    public static long countByStatus(String status) {
        return USERS.values().stream().filter(u -> u.getStatus().equals(status)).count();
    }
}
