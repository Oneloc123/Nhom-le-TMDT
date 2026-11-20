package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.NotificationService;
import java.sql.*;
import java.util.*;

public final class EscalationDao {
    private EscalationDao() {}

    public static long create(long orderId, long shopId, long requestedBy, String requesterRole, String reason) {
        if (reason == null || reason.isBlank() || reason.trim().length() > 2000)
            throw new IllegalArgumentException("Lý do cần từ 1 đến 2.000 ký tự.");
        if (!"CUSTOMER".equals(requesterRole) && !"SHOP".equals(requesterRole))
            throw new IllegalArgumentException("Vai trò người yêu cầu không hợp lệ.");

        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement p = c.prepareStatement(
                        "SELECT escalation_id FROM order_message_escalations WHERE order_id=? AND shop_id=? AND status IN ('OPEN','IN_PROGRESS') LIMIT 1")) {
                    p.setLong(1, orderId); p.setLong(2, shopId);
                    try (ResultSet r = p.executeQuery()) {
                        if (r.next()) throw new IllegalArgumentException("Hội thoại này đã có yêu cầu can thiệp đang mở. Vui lòng chờ Admin xử lý.");
                    }
                }
                if ("CUSTOMER".equals(requesterRole)) {
                    try (PreparedStatement p = c.prepareStatement(
                            "SELECT o.order_id FROM orders o JOIN order_items oi ON oi.order_id=o.order_id WHERE o.order_id=? AND oi.shop_id=? AND o.user_id=? LIMIT 1")) {
                        p.setLong(1, orderId); p.setLong(2, shopId); p.setLong(3, requestedBy);
                        try (ResultSet r = p.executeQuery()) {
                            if (!r.next()) throw new IllegalArgumentException("Đơn hàng không thuộc tài khoản của bạn.");
                        }
                    }
                } else {
                    try (PreparedStatement p = c.prepareStatement(
                            "SELECT ps.shop_id FROM print_shops ps JOIN order_items oi ON oi.shop_id=ps.shop_id WHERE ps.shop_id=? AND ps.owner_user_id=? AND oi.order_id=? LIMIT 1")) {
                        p.setLong(1, shopId); p.setLong(2, requestedBy); p.setLong(3, orderId);
                        try (ResultSet r = p.executeQuery()) {
                            if (!r.next()) throw new IllegalArgumentException("Đơn hàng không thuộc xưởng của bạn.");
                        }
                    }
                }
                try (PreparedStatement p = c.prepareStatement(
                        "SELECT COUNT(*) FROM order_message_escalations WHERE requested_by=? AND created_at>=NOW()-INTERVAL 1 HOUR")) {
                    p.setLong(1, requestedBy);
                    try (ResultSet r = p.executeQuery()) {
                        if (r.next() && r.getInt(1) >= 3) throw new IllegalArgumentException("Bạn đã gửi quá nhiều yêu cầu can thiệp trong 1 giờ.");
                    }
                }
                long id;
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO order_message_escalations(order_id,shop_id,requested_by,requester_role,reason,status,created_at) VALUES(?,?,?,?,?,'OPEN',NOW())",
                        Statement.RETURN_GENERATED_KEYS)) {
                    p.setLong(1, orderId); p.setLong(2, shopId); p.setLong(3, requestedBy);
                    p.setString(4, requesterRole); p.setString(5, reason.trim());
                    p.executeUpdate();
                    try (ResultSet keys = p.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Không tạo được escalation.");
                        id = keys.getLong(1);
                    }
                }
                c.commit();
                notifyAdmins(orderId, shopId, id, requesterRole);
                return id;
            } catch (SQLException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException x) { e.addSuppressed(x); }
                throw e;
            } finally { c.setAutoCommit(true); }
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static Map<String, Object> findActiveByThread(long orderId, long shopId) {
        String sql = "SELECT e.escalation_id,e.status,e.assigned_admin_id,u.full_name admin_name,e.reason,e.verdict,e.created_at,e.resolved_at "
                + "FROM order_message_escalations e LEFT JOIN users u ON u.user_id=e.assigned_admin_id "
                + "WHERE e.order_id=? AND e.shop_id=? AND e.status IN ('OPEN','IN_PROGRESS') ORDER BY e.created_at DESC LIMIT 1";
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, orderId); p.setLong(2, shopId);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) return null;
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", r.getLong("escalation_id"));
                m.put("status", r.getString("status"));
                m.put("assignedAdminId", (Long) r.getObject("assigned_admin_id"));
                m.put("adminName", r.getString("admin_name"));
                m.put("reason", r.getString("reason"));
                m.put("verdict", r.getString("verdict"));
                m.put("createdAt", dateTime(r, "created_at"));
                m.put("resolvedAt", dateTime(r, "resolved_at"));
                return m;
            }
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static Map<String, Object> findById(long escalationId) {
        String sql = "SELECT e.escalation_id,e.order_id,e.shop_id,e.requested_by,e.requester_role,e.reason,"
                + "e.status,e.assigned_admin_id,u.full_name admin_name,e.verdict,e.created_at,e.resolved_at,"
                + "o.order_code,o.user_id customer_id,cu.full_name customer_name,cu.email customer_email,"
                + "COALESCE(ps.shop_name,'Xưởng in') shop_name "
                + "FROM order_message_escalations e "
                + "JOIN orders o ON o.order_id=e.order_id "
                + "JOIN users cu ON cu.user_id=o.user_id "
                + "LEFT JOIN print_shops ps ON ps.shop_id=e.shop_id "
                + "LEFT JOIN users u ON u.user_id=e.assigned_admin_id "
                + "WHERE e.escalation_id=?";
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, escalationId);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) return null;
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", r.getLong("escalation_id"));
                m.put("orderId", r.getLong("order_id"));
                m.put("shopId", r.getLong("shop_id"));
                m.put("requestedBy", r.getLong("requested_by"));
                m.put("requesterRole", r.getString("requester_role"));
                m.put("reason", r.getString("reason"));
                m.put("status", r.getString("status"));
                m.put("assignedAdminId", (Long) r.getObject("assigned_admin_id"));
                m.put("adminName", r.getString("admin_name"));
                m.put("verdict", r.getString("verdict"));
                m.put("createdAt", dateTime(r, "created_at"));
                m.put("resolvedAt", dateTime(r, "resolved_at"));
                m.put("orderCode", r.getString("order_code"));
                m.put("customerId", r.getLong("customer_id"));
                m.put("customerName", r.getString("customer_name"));
                m.put("customerEmail", r.getString("customer_email"));
                m.put("shopName", r.getString("shop_name"));
                return m;
            }
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static List<Map<String, Object>> listForAdmin(String statusFilter, String query) {
        List<Map<String, Object>> rows = new ArrayList<>();
        String sql = "SELECT e.escalation_id,e.order_id,e.shop_id,e.requester_role,e.reason,e.status,"
                + "e.assigned_admin_id,au.full_name admin_name,e.created_at,e.resolved_at,"
                + "o.order_code,o.user_id customer_id,cu.full_name customer_name,cu.email customer_email,"
                + "COALESCE(ps.shop_name,'Xưởng in') shop_name "
                + "FROM order_message_escalations e "
                + "JOIN orders o ON o.order_id=e.order_id "
                + "JOIN users cu ON cu.user_id=o.user_id "
                + "LEFT JOIN print_shops ps ON ps.shop_id=e.shop_id "
                + "LEFT JOIN users au ON au.user_id=e.assigned_admin_id "
                + "WHERE (? IS NULL OR e.status=?) "
                + "AND (? IS NULL OR o.order_code LIKE ? OR cu.full_name LIKE ? OR cu.email LIKE ? OR ps.shop_name LIKE ?) "
                + "ORDER BY CASE e.status WHEN 'OPEN' THEN 0 WHEN 'IN_PROGRESS' THEN 1 ELSE 2 END, e.created_at DESC "
                + "LIMIT 200";
        String s = statusFilter == null || statusFilter.isBlank() ? null : statusFilter.trim().toUpperCase(java.util.Locale.ROOT);
        if (s != null && !Set.of("OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED").contains(s)) s = null;
        String q = query == null || query.isBlank() ? null : query.trim().substring(0, Math.min(120, query.trim().length()));
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, s); p.setString(2, s); p.setString(3, q);
            p.setString(4, q == null ? null : "%" + q + "%"); p.setString(5, q == null ? null : "%" + q + "%");
            p.setString(6, q == null ? null : "%" + q + "%"); p.setString(7, q == null ? null : "%" + q + "%");
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r.getLong("escalation_id"));
                    m.put("orderId", r.getLong("order_id"));
                    m.put("shopId", r.getLong("shop_id"));
                    m.put("requesterRole", r.getString("requester_role"));
                    m.put("reason", r.getString("reason"));
                    m.put("status", r.getString("status"));
                    m.put("assignedAdminId", (Long) r.getObject("assigned_admin_id"));
                    m.put("adminName", r.getString("admin_name"));
                    m.put("createdAt", dateTime(r, "created_at"));
                    m.put("resolvedAt", dateTime(r, "resolved_at"));
                    m.put("orderCode", r.getString("order_code"));
                    m.put("customerName", r.getString("customer_name"));
                    m.put("customerEmail", r.getString("customer_email"));
                    m.put("shopName", r.getString("shop_name"));
                    rows.add(m);
                }
            }
            return rows;
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static void assign(long escalationId, long adminId) {
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(
                "UPDATE order_message_escalations SET assigned_admin_id=?,status='IN_PROGRESS' WHERE escalation_id=? AND status='OPEN'")) {
            p.setLong(1, adminId); p.setLong(2, escalationId);
            if (p.executeUpdate() != 1) throw new IllegalArgumentException("Không thể nhận vụ việc. Có thể đã được giao hoặc đã đóng.");
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static void resolve(long escalationId, long adminId, String verdict, String status) {
        if (verdict == null || verdict.isBlank()) throw new IllegalArgumentException("Phán quyết không được để trống.");
        if (verdict.trim().length() > 5000) throw new IllegalArgumentException("Phán quyết tối đa 5.000 ký tự.");
        if (!"RESOLVED".equals(status) && !"CLOSED".equals(status)) status = "RESOLVED";
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(
                "UPDATE order_message_escalations SET verdict=?,status=?,resolved_at=NOW() WHERE escalation_id=? AND assigned_admin_id=? AND status IN ('OPEN','IN_PROGRESS')")) {
            p.setString(1, verdict.trim()); p.setString(2, status); p.setLong(3, escalationId); p.setLong(4, adminId);
            if (p.executeUpdate() != 1) throw new IllegalArgumentException("Không thể phán quyết. Vụ việc chưa được giao cho bạn hoặc đã đóng.");
        } catch (SQLException e) { throw DataAccessException.of(e); }

        Map<String, Object> esc = findById(escalationId);
        if (esc != null) {
            long customerId = ((Number) esc.get("customerId")).longValue();
            long orderId = ((Number) esc.get("orderId")).longValue();
            long shopId = ((Number) esc.get("shopId")).longValue();
            NotificationService.notifyUser(customerId, "ESCALATION_RESOLVED", "Admin đã phán quyết",
                    "Vụ can thiệp đơn hàng đã được giải quyết.", "/messages?orderId=" + orderId + "&shopId=" + shopId, "ORDER", orderId);
            notifyShopOwner(shopId, "ESCALATION_RESOLVED", "Admin đã phán quyết",
                    "Vụ can thiệp đơn hàng đã được giải quyết.", "/workshop/messages?orderId=" + orderId + "&shopId=" + shopId, "ORDER", orderId);
        }
    }

    public static List<Map<String, Object>> threadForAdmin(long orderId, long shopId, long escalationId) {
        Map<String, Object> esc = findById(escalationId);
        if (esc == null || ((Number) esc.get("orderId")).longValue() != orderId || ((Number) esc.get("shopId")).longValue() != shopId)
            return null;
        List<Map<String, Object>> rows = new ArrayList<>();
        String sql = "SELECT m.message_id,m.sender_user_id,m.sender_role,m.recipient_user_id,m.message_body,m.read_at,m.created_at,"
                + "u.full_name FROM order_messages m LEFT JOIN users u ON u.user_id=m.sender_user_id "
                + "WHERE m.order_id=? AND m.shop_id=? ORDER BY m.created_at,m.message_id";
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, orderId); p.setLong(2, shopId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r.getLong("message_id"));
                    m.put("senderId", r.getLong("sender_user_id"));
                    m.put("senderRole", r.getString("sender_role"));
                    m.put("body", r.getString("message_body"));
                    m.put("sender", r.getString("full_name"));
                    var ts = r.getTimestamp("created_at");
                    m.put("createdAt", ts == null ? "" : ts.toLocalDateTime().toString().replace('T', ' '));
                    rows.add(m);
                }
            }
            return rows;
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static void sendFromAdmin(long orderId, long shopId, long adminUserId, long escalationId, String body) {
        if (body == null || body.isBlank() || body.trim().length() > 2000)
            throw new IllegalArgumentException("Tin nhắn cần từ 1 đến 2.000 ký tự.");
        Map<String, Object> esc = findById(escalationId);
        if (esc == null) throw new IllegalArgumentException("Vụ can thiệp không tồn tại.");
        String status = (String) esc.get("status");
        if ("RESOLVED".equals(status) || "CLOSED".equals(status))
            throw new IllegalArgumentException("Vụ can thiệp đã đóng, không thể gửi thêm tin nhắn.");
        Long assignedId = (Long) esc.get("assignedAdminId");
        if (assignedId == null || assignedId != adminUserId)
            throw new IllegalArgumentException("Bạn chưa được giao xử lý vụ can thiệp này.");
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(
                "INSERT INTO order_messages(order_id,shop_id,sender_user_id,sender_role,recipient_user_id,message_body,escalation_id,created_at) VALUES(?,?,?,'ADMIN',NULL,?,?,NOW())")) {
            p.setLong(1, orderId); p.setLong(2, shopId); p.setLong(3, adminUserId);
            p.setString(4, body.trim()); p.setLong(5, escalationId);
            p.executeUpdate();
        } catch (SQLException e) { throw DataAccessException.of(e); }

        long customerId = ((Number) esc.get("customerId")).longValue();
        NotificationService.notifyUser(customerId, "ESCALATION_MESSAGE", "Admin đã phản hồi",
                "Có tin nhắn mới từ Admin về đơn hàng.", "/messages?orderId=" + orderId + "&shopId=" + shopId, "ORDER", orderId);
        notifyShopOwner(shopId, "ESCALATION_MESSAGE", "Admin đã phản hồi",
                "Có tin nhắn mới từ Admin về đơn hàng.", "/workshop/messages?orderId=" + orderId + "&shopId=" + shopId, "ORDER", orderId);
    }

    private static void notifyAdmins(long orderId, long shopId, long escalationId, String requesterRole) {
        String roleLabel = "CUSTOMER".equals(requesterRole) ? "Khách hàng" : "Xưởng in";
        String sql = "SELECT user_id FROM users WHERE LOWER(role)='admin' AND deleted_at IS NULL AND UPPER(status)='ACTIVE'";
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql); ResultSet r = p.executeQuery()) {
            while (r.next()) {
                NotificationService.notifyUser(r.getLong(1), "ESCALATION_NEW", "Yêu cầu can thiệp mới",
                        roleLabel + " yêu cầu Admin can thiệp đơn hàng.", "/admin/escalations?id=" + escalationId, "ESCALATION", escalationId);
            }
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    private static void notifyShopOwner(long shopId, String type, String title, String message, String url, String relatedType, Long relatedId) {
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(
                "SELECT owner_user_id FROM print_shops WHERE shop_id=?")) {
            p.setLong(1, shopId);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) NotificationService.notifyUser(r.getLong(1), type, title, message, url, relatedType, relatedId);
            }
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    private static String dateTime(ResultSet r, String field) throws SQLException {
        var t = r.getTimestamp(field);
        return t == null ? "" : t.toLocalDateTime().toString().replace('T', ' ');
    }
}
