package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class UserDao {

    private static final String COLUMNS =
            "user_id, full_name, email, phone, password_hash, role, status, created_at,"
                    + " updated_at, provider, email_verified, last_login_at,"
                    + " failed_login_attempts, locked_until, deleted_at";

    private UserDao() {
    }

    public static Optional<User> findById(long userId) {
        String sql = "SELECT " + COLUMNS + " FROM users WHERE user_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static Optional<User> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        String sql = "SELECT " + COLUMNS + " FROM users WHERE email = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static List<User> findAll() {
        String sql = "SELECT " + COLUMNS + " FROM users ORDER BY user_id";
        List<User> users = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(map(rs));
            }
            return users;
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static User create(String email, String plainPassword, String fullName,
                              String phone, String role, String status) {
        return create(email, plainPassword, fullName, phone, role, status, "local");
    }

    public static User create(String email, String plainPassword, String fullName,
                              String phone, String role, String status, String provider) {
        String hash = vn.edu.hcmuaf.fit.tmdt_nhomle.util.PasswordHasher.hash(plainPassword);
        return insert(email, hash, fullName, phone, role, status, provider);
    }

    /**
     * Tạo tài khoản từ một hash đã tính sẵn (luồng đăng ký demo OTP: hash được
     * tính ở bước nhập form, không lưu mật khẩu plaintext trong phiên).
     */
    public static User createWithHash(String email, String passwordHash, String fullName,
                                      String phone, String role, String status, String provider) {
        return insert(email, passwordHash, fullName, phone, role, status, provider);
    }

    private static User insert(String email, String passwordHash, String fullName, String phone,
                               String role, String status, String provider) {
        String sql = "INSERT INTO users (full_name, email, phone, password_hash, role, status,"
                + " created_at, provider, email_verified) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        boolean verified = !User.STATUS_PENDING.equals(status);
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, fullName);
            ps.setString(2, email.trim());
            ps.setString(3, phone);
            ps.setString(4, passwordHash);
            ps.setString(5, role);
            ps.setString(6, status);
            ps.setObject(7, LocalDateTime.now());
            ps.setString(8, provider == null || provider.isBlank() ? "local" : provider);
            ps.setBoolean(9, verified);
            ps.executeUpdate();
            long id = generatedId(ps);
            return findById(id).orElseThrow();
        } catch (SQLException e) {
            if (isDuplicate(e)) {
                throw new DataAccessException("Email đã được đăng ký: " + email, e);
            }
            throw DataAccessException.of(e);
        }
    }

    /** Cập nhật hồ sơ + updated_at; KHÔNG đụng role/status/email/mật khẩu. */
    public static void updateProfile(User user) {
        String sql = "UPDATE users SET full_name = ?, phone = ?, updated_at = NOW() WHERE user_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getPhone());
            ps.setLong(3, user.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static void updatePassword(long userId, String plainPassword) {
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, vn.edu.hcmuaf.fit.tmdt_nhomle.util.PasswordHasher.hash(plainPassword));
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static void updateRole(long userId, String role) {
        String sql = "UPDATE users SET role = ? WHERE user_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static void updateStatus(long userId, String status) {
        String sql = "UPDATE users SET status = ? WHERE user_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static long countByRole(String role) {
        return count("role = ?", role);
    }

    public static long countByStatus(String status) {
        return count("status = ?", status);
    }

    private static long count(String where, String value) {
        String sql = "SELECT COUNT(*) FROM users WHERE " + where;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    private static long generatedId(PreparedStatement ps) throws SQLException {
        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getLong(1);
            }
        }
        throw new SQLException("Không lấy được user_id vừa tạo.");
    }

    static boolean isDuplicate(SQLException e) {
        return e.getErrorCode() == 1062; // ER_DUP_ENTRY
    }

    /** Ghi nhận đăng nhập thành công: reset bộ đếm lỗi + cập nhật last_login_at. */
    public static void touchLastLogin(long userId) {
        String sql = "UPDATE users SET last_login_at = NOW(), failed_login_attempts = 0,"
                + " locked_until = NULL, updated_at = NOW() WHERE user_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    /**
     * Tăng bộ đếm đăng nhập sai; khi vượt ngưỡng thì khóa tạm tới {@code lockedUntil}
     * (mốc thời gian, có thể null nếu dưới ngưỡng). Trả về số lần sai sau khi ghi.
     */
    public static int recordFailedLogin(long userId, int attemptsBefore, int lockThreshold,
                                        LocalDateTime lockUntil) {
        int attempts = attemptsBefore + 1;
        String sql = "UPDATE users SET failed_login_attempts = ?, locked_until = ?, updated_at = NOW()"
                + " WHERE user_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, attempts);
            if (attempts >= lockThreshold && lockUntil != null) {
                ps.setTimestamp(2, java.sql.Timestamp.valueOf(lockUntil));
            } else {
                ps.setTimestamp(2, null);
            }
            ps.setLong(3, userId);
            ps.executeUpdate();
            return attempts;
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    /** Xóa cờ khóa tạm / bộ đếm lỗi (admin mở khóa hoặc đăng nhập thành công). */
    public static void clearLoginLock(long userId) {
        String sql = "UPDATE users SET failed_login_attempts = 0, locked_until = NULL"
                + " WHERE user_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    /** Đánh dấu xác thực email thành công (luồng OTP demo). */
    public static void markEmailVerified(long userId) {
        String sql = "UPDATE users SET email_verified = 1, updated_at = NOW() WHERE user_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    /**
     * Xoá mềm / khôi phục tài khoản. Trạng thái nghiệp vụ (role/status) giữ
     * nguyên để đơn hàng, thanh toán và audit không bị ảnh hưởng; deleted_at là
     * cờ duy nhất cho mọi truy vấn “đã xoá mềm”.
     */
    public static void setSoftDeleted(long userId, boolean deleted) {
        String sql = deleted
                ? "UPDATE users SET deleted_at = NOW(), updated_at = NOW() WHERE user_id = ?"
                : "UPDATE users SET deleted_at = NULL, updated_at = NOW() WHERE user_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    private static User map(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("user_id"));
        user.setEmail(rs.getString("email"));
        user.setFullName(rs.getString("full_name"));
        user.setPhone(rs.getString("phone"));
        user.setPassword(rs.getString("password_hash")); // đã băm
        user.setRole(rs.getString("role"));
        user.setStatus(rs.getString("status"));
        java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
        user.setCreatedAt(createdAt == null ? "" : createdAt.toLocalDateTime()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        user.setProvider(nullSafe(rs.getString("provider"), "local"));
        user.setEmailVerified(rs.getBoolean("email_verified"));
        user.setUpdatedAt(timestampString(rs.getTimestamp("updated_at")));
        user.setLastLoginAt(timestampString(rs.getTimestamp("last_login_at")));
        user.setFailedLoginAttempts(rs.getInt("failed_login_attempts"));
        user.setLockedUntil(timestampString(rs.getTimestamp("locked_until")));
        user.setDeletedAt(timestampString(rs.getTimestamp("deleted_at")));
        user.setAddress(""); // schema không có cột address trong users
        return user;
    }

    private static String nullSafe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String timestampString(java.sql.Timestamp value) {
        return value == null ? null : value.toLocalDateTime().toString();
    }
}
