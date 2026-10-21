package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Nhật ký sự kiện bảo mật/soát xét lưu trong bảng {@code user_security_events}.
 *
 * <p>NGUYÊN TẮC: chỉ lưu dữ liệu phi bí mật — không bao giờ ghi mật khẩu,
 * password hash, mã OTP, reset token, session id hay OAuth secret. Mô tả sự kiện
 * là chuỗi đã được người gọi kiểm tra (xem các hằng EVENT_*).</p>
 */
public final class SecurityEventDao {

    public static final String EVENT_REGISTERED = "REGISTERED";
    public static final String EVENT_EMAIL_VERIFIED = "EMAIL_VERIFIED";
    public static final String EVENT_LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String EVENT_LOGIN_FAILED = "LOGIN_FAILED";
    public static final String EVENT_LOGIN_OTP = "LOGIN_OTP";
    public static final String EVENT_PASSWORD_RESET = "PASSWORD_RESET";
    public static final String EVENT_PASSWORD_CHANGED = "PASSWORD_CHANGED";
    public static final String EVENT_LOGOUT = "LOGOUT";
    public static final String EVENT_ADMIN_ROLE_CHANGED = "ADMIN_ROLE_CHANGED";
    public static final String EVENT_ADMIN_STATUS_CHANGED = "ADMIN_STATUS_CHANGED";
    public static final String EVENT_ADMIN_PROFILE_EDITED = "ADMIN_PROFILE_EDITED";
    public static final String EVENT_ADMIN_SOFT_DELETE = "ADMIN_SOFT_DELETE";
    public static final String EVENT_ADMIN_RESTORE = "ADMIN_RESTORE";

    private SecurityEventDao() {
    }

    /** Ghi một sự kiện. Mọi tham số đều đi qua PreparedStatement. */
    public static void record(Long userId, Long actorUserId, String eventType,
                              String description, String ipAddress) {
        if (eventType == null || eventType.isBlank() || description == null || description.isBlank()) {
            return;
        }
        String sql = "INSERT INTO user_security_events"
                + " (user_id, actor_user_id, event_type, description, ip_address, created_at)"
                + " VALUES (?, ?, ?, ?, ?, NOW())";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (userId == null || userId <= 0) {
                ps.setObject(1, null);
            } else {
                ps.setLong(1, userId);
            }
            if (actorUserId == null || actorUserId <= 0) {
                ps.setObject(2, null);
            } else {
                ps.setLong(2, actorUserId);
            }
            ps.setString(3, truncate(eventType, 60));
            ps.setString(4, truncate(description, 255));
            ps.setString(5, truncate(ipAddress, 45));
            // created_at do DB ghi (NOW() trong câu INSERT) — không binding thừa.
            ps.executeUpdate();
        } catch (SQLException e) {
            // Nhật ký không được làm hỏng luồng chính — chỉ ghi log kỹ thuật.
            System.getLogger(SecurityEventDao.class.getName()).log(System.Logger.Level.WARNING,
                    "Không thể ghi sự kiện bảo mật: " + eventType, e);
        }
    }

    public static void record(Long userId, Long actorUserId, String eventType, String description) {
        record(userId, actorUserId, eventType, description, null);
    }

    /** Các sự kiện gần nhất của một tài khoản (dùng cho trang chi tiết admin). */
    public static List<Map<String, Object>> recentForUser(long userId, int limit) {
        int safeLimit = Math.min(Math.max(1, limit), 100);
        String sql = "SELECT e.event_id eventId, e.event_type eventType, e.description,"
                + " e.ip_address ipAddress, e.actor_user_id actorUserId,"
                + " actor.full_name actorName, e.created_at createdAt"
                + " FROM user_security_events e"
                + " LEFT JOIN users actor ON actor.user_id = e.actor_user_id"
                + " WHERE e.user_id = ? ORDER BY e.created_at DESC, e.event_id DESC LIMIT ?";
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, safeLimit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("eventId", rs.getLong("eventId"));
                    row.put("eventType", rs.getString("eventType"));
                    row.put("description", rs.getString("description"));
                    row.put("ipAddress", rs.getString("ipAddress"));
                    row.put("actorName", rs.getString("actorName"));
                    java.sql.Timestamp createdAt = rs.getTimestamp("createdAt");
                    row.put("createdAt", createdAt == null ? null : createdAt.toString());
                    rows.add(row);
                }
            }
            return rows;
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
