package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;

import java.sql.*;

/** Các thao tác bảo mật xác thực cần transaction. */
public final class UserAuthDao {
    private UserAuthDao() { }

    public static boolean saveResetToken(long userId, String tokenHash) {
        String sql = "INSERT INTO user_password_reset_tokens(user_id,token_hash,expires_at,attempts,created_at) "
                + "VALUES(?,?,DATE_ADD(NOW(),INTERVAL 15 MINUTE),0,NOW())";
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                // Khóa hàng user để tránh hai yêu cầu OTP đồng thời vượt qua giới hạn gửi.
                try (PreparedStatement p = c.prepareStatement("SELECT user_id FROM users WHERE user_id=? AND deleted_at IS NULL FOR UPDATE")) {
                    p.setLong(1, userId); try (ResultSet r = p.executeQuery()) { if (!r.next()) { c.rollback(); return false; } }
                }
                try (PreparedStatement p = c.prepareStatement("SELECT COUNT(*) FROM user_password_reset_tokens WHERE user_id=? AND created_at>=DATE_SUB(NOW(),INTERVAL 60 SECOND)")) {
                    p.setLong(1, userId); try (ResultSet r = p.executeQuery()) { if (r.next() && r.getInt(1) > 0) { c.rollback(); return false; } }
                }
                try (PreparedStatement p = c.prepareStatement("UPDATE user_password_reset_tokens SET used_at=NOW() WHERE user_id=? AND used_at IS NULL")) {
                    p.setLong(1, userId); p.executeUpdate();
                }
                try (PreparedStatement p = c.prepareStatement(sql)) {
                    p.setLong(1, userId); p.setString(2, tokenHash); p.executeUpdate();
                }
                c.commit(); return true;
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    /** Lưu mã kích hoạt email dưới dạng hash; có giới hạn gửi lại 60 giây. */
    public static boolean saveEmailVerificationToken(long userId, String tokenHash) {
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement p = c.prepareStatement(
                        "SELECT user_id FROM users WHERE user_id=? AND status='PENDING' AND deleted_at IS NULL FOR UPDATE")) {
                    p.setLong(1, userId);
                    try (ResultSet r = p.executeQuery()) { if (!r.next()) { c.rollback(); return false; } }
                }
                try (PreparedStatement p = c.prepareStatement(
                        "SELECT COUNT(*) FROM user_email_verification_tokens WHERE user_id=? AND created_at>=DATE_SUB(NOW(),INTERVAL 60 SECOND)")) {
                    p.setLong(1, userId);
                    try (ResultSet r = p.executeQuery()) { if (r.next() && r.getInt(1) > 0) { c.rollback(); return false; } }
                }
                try (PreparedStatement p = c.prepareStatement(
                        "UPDATE user_email_verification_tokens SET used_at=NOW() WHERE user_id=? AND used_at IS NULL")) {
                    p.setLong(1, userId); p.executeUpdate();
                }
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO user_email_verification_tokens(user_id,token_hash,expires_at,attempts,created_at) "
                                + "VALUES(?,?,DATE_ADD(NOW(),INTERVAL 15 MINUTE),0,NOW())")) {
                    p.setLong(1, userId); p.setString(2, tokenHash); p.executeUpdate();
                }
                c.commit(); return true;
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    /** Xác minh email và kích hoạt tài khoản trong cùng một transaction. */
    public static boolean verifyEmail(long userId, String tokenHash) {
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                String stored = null; int attempts = 0; boolean validWindow = false;
                try (PreparedStatement p = c.prepareStatement(
                        "SELECT token_hash,attempts,(expires_at>NOW() AND used_at IS NULL) valid_window "
                                + "FROM user_email_verification_tokens WHERE user_id=? ORDER BY verification_token_id DESC LIMIT 1 FOR UPDATE")) {
                    p.setLong(1, userId);
                    try (ResultSet r = p.executeQuery()) {
                        if (r.next()) { stored=r.getString("token_hash"); attempts=r.getInt("attempts"); validWindow=r.getBoolean("valid_window"); }
                    }
                }
                if (!validWindow || attempts >= 5 || stored == null) { c.rollback(); return false; }
                if (!java.security.MessageDigest.isEqual(stored.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        tokenHash.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                    try (PreparedStatement p = c.prepareStatement(
                            "UPDATE user_email_verification_tokens SET attempts=attempts+1 WHERE user_id=? AND used_at IS NULL")) {
                        p.setLong(1, userId); p.executeUpdate();
                    }
                    c.commit(); return false;
                }
                try (PreparedStatement p = c.prepareStatement(
                        "UPDATE users SET status='ACTIVE',updated_at=NOW() WHERE user_id=? AND status='PENDING' AND deleted_at IS NULL")) {
                    p.setLong(1, userId);
                    if (p.executeUpdate() != 1) { c.rollback(); return false; }
                }
                try (PreparedStatement p = c.prepareStatement(
                        "UPDATE user_email_verification_tokens SET used_at=NOW() WHERE user_id=? AND used_at IS NULL")) {
                    p.setLong(1, userId); p.executeUpdate();
                }
                c.commit(); return true;
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    /** Kiểm tra OTP, tăng số lần sai và đổi mật khẩu nguyên tử trong một transaction. */
    public static boolean resetPassword(long userId, String tokenHash, String newPasswordHash) {
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                String stored = null;
                int attempts = 0;
                boolean validWindow = false;
                try (PreparedStatement p = c.prepareStatement(
                        "SELECT token_hash,attempts,(expires_at>NOW() AND used_at IS NULL) valid_window "
                                + "FROM user_password_reset_tokens WHERE user_id=? ORDER BY reset_token_id DESC LIMIT 1 FOR UPDATE")) {
                    p.setLong(1, userId);
                    try (ResultSet r = p.executeQuery()) {
                        if (r.next()) { stored = r.getString("token_hash"); attempts = r.getInt("attempts"); validWindow = r.getBoolean("valid_window"); }
                    }
                }
                if (!validWindow || attempts >= 5 || stored == null) { c.rollback(); return false; }
                if (!java.security.MessageDigest.isEqual(stored.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        tokenHash.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                    try (PreparedStatement p = c.prepareStatement("UPDATE user_password_reset_tokens SET attempts=attempts+1 WHERE user_id=? AND used_at IS NULL")) {
                        p.setLong(1, userId); p.executeUpdate();
                    }
                    c.commit(); return false;
                }
                try (PreparedStatement p = c.prepareStatement(
                        "UPDATE users SET password_hash=?,failed_login_attempts=0,locked_until=NULL,updated_at=NOW() WHERE user_id=? AND deleted_at IS NULL")) {
                    p.setString(1, newPasswordHash); p.setLong(2, userId);
                    if (p.executeUpdate() != 1) { c.rollback(); return false; }
                }
                try (PreparedStatement p = c.prepareStatement("UPDATE user_password_reset_tokens SET used_at=NOW() WHERE user_id=? AND used_at IS NULL")) {
                    p.setLong(1, userId); p.executeUpdate();
                }
                c.commit(); return true;
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }
}
