package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.OrderStatus;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.PasswordHasher;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** DAO định danh dùng chung: user-facing, admin và AuthFilter. */
public final class UserDao {
    private static final String COLUMNS = "user_id,full_name,email,phone,role,status,created_at,"
            + "auth_provider,provider_user_id,avatar_url,birth_date,gender,notify_email,last_login_at,deleted_at";
    private static final String CREDENTIAL_COLUMNS = COLUMNS.replace("phone,role", "phone,password_hash,role");

    private UserDao() { }

    public static Optional<User> findById(long id) {
        return one("SELECT " + COLUMNS + " FROM users WHERE user_id=? AND deleted_at IS NULL", p -> p.setLong(1, id));
    }

    public static Optional<User> findByEmail(String email) {
        if (email == null || email.isBlank()) return Optional.empty();
        return one("SELECT " + CREDENTIAL_COLUMNS + " FROM users WHERE LOWER(email)=? AND deleted_at IS NULL",
                p -> p.setString(1, normalizeEmail(email)), true);
    }

    /** Chỉ trả hash cho luồng xác thực; tuyệt đối không gắn hash vào đối tượng lưu trong session. */
    public static Optional<String> findPasswordHashById(long userId) {
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(
                "SELECT password_hash FROM users WHERE user_id=? AND deleted_at IS NULL")) {
            p.setLong(1, userId);
            try (ResultSet r = p.executeQuery()) { return r.next() ? Optional.ofNullable(r.getString(1)) : Optional.empty(); }
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static List<User> findAll() {
        String sql = "SELECT " + COLUMNS + " FROM users ORDER BY user_id DESC";
        List<User> result = new ArrayList<>();
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql); ResultSet r = p.executeQuery()) {
            while (r.next()) result.add(map(r));
            return result;
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    /** Tạo tài khoản legacy cho adapter của admin/OAuth; đăng ký khách hàng mới dùng createWithHash. */
    public static User create(String email, String plainPassword, String fullName, String phone, String role, String status) {
        return create(email, plainPassword, fullName, phone, role, status, "local", null);
    }

    public static User create(String email, String plainPassword, String fullName, String phone,
                              String role, String status, String provider, String providerUserId) {
        return createWithHash(email, PasswordHasher.hash(plainPassword), fullName, phone, role, status, provider, providerUserId);
    }

    public static User createWithHash(String email, String passwordHash, String fullName, String phone,
                                      String role, String status, String provider, String providerUserId) {
        String sql = "INSERT INTO users(full_name,email,phone,password_hash,role,status,created_at,auth_provider,provider_user_id,updated_at) "
                + "VALUES(?,?,?,?,?,?,NOW(),?,?,NOW())";
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1, required(fullName, "Họ tên", 120));
            p.setString(2, normalizeEmail(email));
            p.setString(3, clean(phone, 25));
            p.setString(4, passwordHash);
            p.setString(5, defaultValue(role, User.ROLE_CUSTOMER));
            p.setString(6, defaultValue(status, User.STATUS_ACTIVE));
            p.setString(7, normalizeProvider(provider));
            p.setString(8, clean(providerUserId, 190));
            p.executeUpdate();
            try (ResultSet keys = p.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Không lấy được user_id sau khi tạo tài khoản.");
                return findById(keys.getLong(1)).orElseThrow(() -> new SQLException("Không đọc lại được tài khoản vừa tạo."));
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DataAccessException("Email hoặc tài khoản liên kết đã được sử dụng.", e);
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static boolean recordFailedLoginAttempt(long userId) {
        String sql = "UPDATE users SET failed_login_attempts=LEAST(COALESCE(failed_login_attempts,0)+1,20), "
                + "locked_until=CASE WHEN COALESCE(failed_login_attempts,0)+1>=5 THEN DATE_ADD(NOW(),INTERVAL 15 MINUTE) ELSE locked_until END "
                + "WHERE user_id=? AND deleted_at IS NULL";
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, userId); p.executeUpdate();
            return isLoginLocked(userId);
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static boolean isLoginLocked(long userId) {
        String sql = "SELECT locked_until FROM users WHERE user_id=? AND deleted_at IS NULL";
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, userId);
            try (ResultSet r = p.executeQuery()) {
                return r.next() && r.getTimestamp(1) != null && r.getTimestamp(1).toInstant().isAfter(java.time.Instant.now());
            }
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static void recordSuccessfulLogin(long userId, String provider) {
        String sql = "UPDATE users SET last_login_at=NOW(),auth_provider=?,failed_login_attempts=0,locked_until=NULL,updated_at=NOW() "
                + "WHERE user_id=? AND deleted_at IS NULL";
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, normalizeProvider(provider)); p.setLong(2, userId); p.executeUpdate();
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static void recordLoginHistory(long userId, String provider, String result, String ip, String userAgent) {
        String sql = "INSERT INTO user_login_history(user_id,auth_provider,login_result,ip_address,user_agent,created_at) VALUES(?,?,?,?,?,NOW())";
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, userId); p.setString(2, normalizeProvider(provider));
            p.setString(3, "SUCCESS".equalsIgnoreCase(result) ? "SUCCESS" : "FAILED");
            p.setString(4, clean(ip, 45)); p.setString(5, clean(userAgent, 500)); p.executeUpdate();
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static boolean hasInProgressOrders(long userId) {
        String sql = "SELECT COUNT(*) FROM orders WHERE user_id=? AND status NOT IN (?,?)";
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, userId); p.setString(2, OrderStatus.COMPLETED); p.setString(3, OrderStatus.CANCELLED);
            try (ResultSet r = p.executeQuery()) { return r.next() && r.getLong(1) > 0; }
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static List<Map<String, Object>> loginHistory(long userId, int limit) {
        int size = Math.max(1, Math.min(50, limit));
        String sql = "SELECT auth_provider,login_result,ip_address,user_agent,created_at FROM user_login_history "
                + "WHERE user_id=? ORDER BY created_at DESC,login_history_id DESC LIMIT ?";
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, userId); p.setInt(2, size);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("provider", r.getString("auth_provider")); m.put("result", r.getString("login_result"));
                    m.put("ip", r.getString("ip_address")); m.put("userAgent", r.getString("user_agent"));
                    Timestamp ts = r.getTimestamp("created_at");
                    m.put("createdAt", ts == null ? "" : ts.toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                    rows.add(m);
                }
            }
            return rows;
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static void recordSecurityEvent(long userId, String type, String description) {
        String sql = "INSERT INTO user_security_events(user_id,event_type,description,created_at) VALUES(?,?,?,NOW())";
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, userId); p.setString(2, clean(type, 60)); p.setString(3, clean(description, 500)); p.executeUpdate();
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static void updateProfile(User user) {
        if (user == null || user.getId() <= 0) throw new IllegalArgumentException("Tài khoản không hợp lệ.");
        String sql = "UPDATE users SET full_name=?,email=?,phone=?,birth_date=?,gender=?,notify_email=?,updated_at=NOW() "
                + "WHERE user_id=? AND deleted_at IS NULL";
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, required(user.getFullName(), "Họ tên", 120));
            p.setString(2, normalizeEmail(user.getEmail())); p.setString(3, clean(user.getPhone(), 25));
            if (user.getBirthDate() == null || user.getBirthDate().isBlank()) p.setNull(4, Types.DATE);
            else p.setDate(4, java.sql.Date.valueOf(user.getBirthDate()));
            p.setString(5, clean(user.getGender(), 20)); p.setBoolean(6, user.isNotifyEmail()); p.setLong(7, user.getId());
            if (p.executeUpdate() != 1) throw new DataAccessException("Không tìm thấy tài khoản để cập nhật.");
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DataAccessException("Email này đã được một tài khoản khác sử dụng.", e);
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static void softDelete(long userId) {
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(
                "UPDATE users SET deleted_at=NOW(),status='LOCKED',updated_at=NOW() WHERE user_id=? AND deleted_at IS NULL")) {
            p.setLong(1, userId); p.executeUpdate();
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static void updateAvatar(long userId, String avatarUrl) {
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(
                "UPDATE users SET avatar_url=?,updated_at=NOW() WHERE user_id=? AND deleted_at IS NULL")) {
            p.setString(1, clean(avatarUrl, 500)); p.setLong(2, userId); p.executeUpdate();
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    /** Adapter cũ; tài khoản tạo mới từ giao diện dùng BCryptPasswordService. */
    public static void updatePassword(long userId, String plainPassword) {
        updatePasswordHash(userId, PasswordHasher.hash(plainPassword));
    }

    public static void updatePasswordHash(long userId, String passwordHash) {
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(
                "UPDATE users SET password_hash=?,failed_login_attempts=0,locked_until=NULL,updated_at=NOW() WHERE user_id=? AND deleted_at IS NULL")) {
            p.setString(1, passwordHash); p.setLong(2, userId);
            if (p.executeUpdate() != 1) throw new DataAccessException("Không thể cập nhật mật khẩu tài khoản.");
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static void updateRole(long userId, String role) {
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement("UPDATE users SET role=?,updated_at=NOW() WHERE user_id=?")) {
            p.setString(1, clean(role, 50)); p.setLong(2, userId); p.executeUpdate();
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static void updateStatus(long userId, String status) {
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement("UPDATE users SET status=?,updated_at=NOW() WHERE user_id=?")) {
            p.setString(1, clean(status, 50)); p.setLong(2, userId); p.executeUpdate();
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static long countByRole(String role) { return count("SELECT COUNT(*) FROM users WHERE role=? AND deleted_at IS NULL", role); }
    public static long countByStatus(String status) { return count("SELECT COUNT(*) FROM users WHERE status=? AND deleted_at IS NULL", status); }

    private static long count(String sql, String value) {
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, value); try (ResultSet r = p.executeQuery()) { return r.next() ? r.getLong(1) : 0; }
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    private interface Binder { void bind(PreparedStatement p) throws SQLException; }
    private static Optional<User> one(String sql, Binder binder) { return one(sql, binder, false); }
    private static Optional<User> one(String sql, Binder binder, boolean includePassword) {
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            binder.bind(p); try (ResultSet r = p.executeQuery()) { return r.next() ? Optional.of(map(r, includePassword)) : Optional.empty(); }
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    private static User map(ResultSet r) throws SQLException { return map(r, false); }
    private static User map(ResultSet r, boolean includePassword) throws SQLException {
        User u = new User();
        u.setId(Math.toIntExact(r.getLong("user_id")));
        u.setFullName(r.getString("full_name")); u.setEmail(r.getString("email"));
        u.setPhone(r.getString("phone")); if (includePassword) u.setPassword(r.getString("password_hash"));
        u.setRole(r.getString("role")); u.setStatus(r.getString("status"));
        u.setProvider(r.getString("auth_provider")); u.setProviderUserId(r.getString("provider_user_id"));
        u.setAvatarUrl(r.getString("avatar_url")); u.setBirthDate(r.getString("birth_date"));
        u.setGender(r.getString("gender")); u.setNotifyEmail(r.getBoolean("notify_email"));
        Timestamp created = r.getTimestamp("created_at"); u.setCreatedAt(created == null ? "" : created.toLocalDateTime().toLocalDate().toString());
        Timestamp login = r.getTimestamp("last_login_at"); u.setLastLoginAt(login == null ? null : login.toLocalDateTime().toString());
        u.setDeleted(r.getTimestamp("deleted_at") != null);
        return u;
    }

    private static String normalizeEmail(String email) {
        String v = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);

        if (v.length() > 190 ||
                !v.matches("(?i)^[A-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Z0-9-]+(?:\\.[A-Z0-9-]+)+$")) {

            throw new IllegalArgumentException("Địa chỉ email không hợp lệ.");
        }

        return v;
    }
    private static String normalizeProvider(String provider) {
        if (provider == null) return "local";
        return switch (provider.trim().toLowerCase(Locale.ROOT)) { case "google" -> "google"; case "facebook" -> "facebook"; default -> "local"; };
    }
    private static String required(String value, String label, int max) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty() || v.length() > max) throw new IllegalArgumentException(label + " không được để trống và tối đa " + max + " ký tự.");
        return v;
    }
    private static String clean(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String v = value.trim(); return v.length() <= max ? v : v.substring(0, max);
    }
    private static String defaultValue(String value, String fallback) { return value == null || value.isBlank() ? fallback : value.trim(); }
}
