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

/**
 * Truy cập bảng users theo schema thật.
 * Lưu ý schema: users KHÔNG có cột address/provider — hai trường đó trên model
 * chỉ mang tính tạm thời trong phiên (giới hạn đã ghi trong README).
 * Mật khẩu lưu dạng băm PBKDF2 (xem util.PasswordHasher).
 */
public final class UserDao {

    private static final String COLUMNS =
            "user_id, full_name, email, phone, password_hash, role, status, created_at";

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

    /** Tạo tài khoản mới; plainPassword sẽ được băm trước khi lưu. */
    public static User create(String email, String plainPassword, String fullName,
                              String phone, String role, String status) {
        String sql = "INSERT INTO users (full_name, email, phone, password_hash, role, status, created_at)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, fullName);
            ps.setString(2, email.trim());
            ps.setString(3, phone);
            ps.setString(4, vn.edu.hcmuaf.fit.tmdt_nhomle.util.PasswordHasher.hash(plainPassword));
            ps.setString(5, role);
            ps.setString(6, status);
            ps.setObject(7, LocalDateTime.now());
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

    /** Cập nhật thông tin hiển thị (không đụng tới mật khẩu/vai trò). */
    public static void updateProfile(User user) {
        String sql = "UPDATE users SET full_name = ?, phone = ? WHERE user_id = ?";
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
        // Schema không có cột provider/address — giá trị tạm trong phiên.
        user.setProvider("local");
        user.setAddress("");
        return user;
    }
}
