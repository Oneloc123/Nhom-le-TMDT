package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.OrderStatus;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.DeliveryDisputePolicy;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.NotificationService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.ShipmentPolicy;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Customer receipt confirmation and Admin delivery-dispute workflow. */
public final class DeliveryDisputeDao {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_ADMIN_ROWS = 300;

    private DeliveryDisputeDao() { }

    public static List<Map<String, Object>> shipmentsForCustomer(long orderId, long customerId) {
        String sql = "SELECT sh.shipment_id id,sh.order_id orderId,o.order_code orderCode,"
                + "sh.shop_id shopId,COALESCE(s.shop_name,'Xưởng in') shopName,sh.carrier,"
                + "sh.tracking_number trackingNumber,sh.status shipmentStatus,sh.delivered_at deliveredAt,"
                + "COALESCE(sh.receipt_status,'NOT_READY') receiptStatus,"
                + "EXISTS(SELECT 1 FROM delivery_disputes d WHERE d.shipment_id=sh.shipment_id"
                + " AND d.status IN ('OPEN','UNDER_REVIEW')) hasOpenDispute"
                + " FROM shipments sh JOIN orders o ON o.order_id=sh.order_id"
                + " LEFT JOIN print_shops s ON s.shop_id=sh.shop_id"
                + " WHERE sh.order_id=? AND o.user_id=? ORDER BY sh.shipment_id";
        return query(sql, orderId, customerId);
    }

    public static void confirmReceipt(long shipmentId, long customerId) {
        inTransaction(conn -> {
            ShipmentContext context = lockCustomerShipment(conn, shipmentId, customerId);
            if (!"DELIVERED".equals(ShipmentPolicy.normalize(context.shipmentStatus()))) {
                throw new IllegalArgumentException("Vận đơn chưa được ghi nhận giao thành công.");
            }
            if (!"WAITING".equalsIgnoreCase(context.receiptStatus())) {
                throw new IllegalArgumentException("Vận đơn không còn ở trạng thái chờ xác nhận nhận hàng.");
            }
            if (hasActiveDispute(conn, shipmentId)) {
                throw new IllegalArgumentException("Vận đơn đang có khiếu nại. Admin cần xử lý trước khi xác nhận.");
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE shipments SET receipt_status='CONFIRMED',receipt_confirmed_at=NOW(),"
                    + "receipt_confirmed_by_user_id=? WHERE shipment_id=? AND receipt_status='WAITING'")) {
                ps.setLong(1, customerId);
                ps.setLong(2, shipmentId);
                if (ps.executeUpdate() != 1) {
                    throw new IllegalArgumentException("Trạng thái xác nhận đã thay đổi. Vui lòng tải lại trang.");
                }
            }
            insertShipmentEvent(conn, context.orderId(), shipmentId,
                    "Khách hàng xác nhận đã nhận hàng",
                    "Khách hàng xác nhận đã nhận kiện hàng. Tài khoản khách #" + customerId + ".");
            completeOrderIfReady(conn, context.orderId(), customerId, context.orderStatus());
            NotificationService.notifyUser(conn, customerId, "RECEIPT_CONFIRMED",
                    "Đã xác nhận nhận hàng",
                    "PrintKraft đã ghi nhận bạn nhận được kiện hàng của đơn " + context.orderCode() + ".",
                    "/order-tracking?code=" + context.orderCode(), "SHIPMENT", shipmentId);
            return null;
        });
    }

    public static long reportIssue(long shipmentId, long customerId, String rawType, String rawDescription) {
        String type = DeliveryDisputePolicy.normalizeIssueType(rawType);
        String description = cleanRequired(rawDescription, 2000, "Vui lòng mô tả vấn đề với ít nhất 10 ký tự.");
        if (description.length() < 10) {
            throw new IllegalArgumentException("Vui lòng mô tả vấn đề với ít nhất 10 ký tự.");
        }
        return inTransaction(conn -> {
            ShipmentContext context = lockCustomerShipment(conn, shipmentId, customerId);
            if (!"DELIVERED".equals(ShipmentPolicy.normalize(context.shipmentStatus()))) {
                throw new IllegalArgumentException("Chỉ có thể báo vấn đề sau khi vận đơn được ghi nhận giao thành công.");
            }
            if (!"WAITING".equalsIgnoreCase(context.receiptStatus())) {
                throw new IllegalArgumentException("Vận đơn không còn ở trạng thái chờ xác nhận. Hãy liên hệ Admin nếu cần hỗ trợ.");
            }
            if (hasActiveDispute(conn, shipmentId)) {
                throw new IllegalArgumentException("Vận đơn đã có khiếu nại đang được xử lý.");
            }

            String code = generateCode();
            long disputeId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO delivery_disputes(dispute_code,shipment_id,order_id,customer_id,"
                    + "dispute_type,description,status,created_at,updated_at)"
                    + " VALUES(?,?,?,?,?,?,'OPEN',NOW(),NOW())", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, code);
                ps.setLong(2, shipmentId);
                ps.setLong(3, context.orderId());
                ps.setLong(4, customerId);
                ps.setString(5, type);
                ps.setString(6, description);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("Không lấy được mã khiếu nại giao hàng.");
                    disputeId = keys.getLong(1);
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE shipments SET receipt_status='DISPUTED' WHERE shipment_id=? AND receipt_status='WAITING'")) {
                ps.setLong(1, shipmentId);
                if (ps.executeUpdate() != 1) throw new IllegalArgumentException("Trạng thái vận đơn đã thay đổi.");
            }
            insertDisputeEvent(conn, disputeId, customerId, "SUBMITTED", null, "OPEN", description);
            insertShipmentEvent(conn, context.orderId(), shipmentId,
                    "Khách hàng gửi khiếu nại giao hàng",
                    "Mã khiếu nại " + code + " · Loại: " + type + " · " + description);
            NotificationService.notifyUser(conn, customerId, "DELIVERY_DISPUTE_CREATED",
                    "Đã tiếp nhận khiếu nại giao hàng",
                    "Khiếu nại " + code + " của đơn " + context.orderCode() + " đã được tiếp nhận.",
                    "/order-tracking?code=" + context.orderCode(), "DELIVERY_DISPUTE", disputeId);
            return disputeId;
        });
    }

    public static List<Map<String, Object>> listForAdmin(String rawStatus, String rawQuery) {
        String status = rawStatus == null ? "OPEN" : rawStatus.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("ALL", "OPEN", "UNDER_REVIEW", "RESOLVED", "REJECTED").contains(status)) {
            throw new IllegalArgumentException("Bộ lọc trạng thái khiếu nại không hợp lệ.");
        }
        StringBuilder sql = new StringBuilder(adminSelect())
                .append(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (!"ALL".equals(status)) {
            sql.append(" AND d.status=?");
            args.add(status);
        }
        String query = rawQuery == null ? "" : rawQuery.trim();
        if (!query.isEmpty()) {
            if (query.length() > 160) throw new IllegalArgumentException("Từ khóa tìm kiếm tối đa 160 ký tự.");
            sql.append(" AND (d.dispute_code LIKE ? OR o.order_code LIKE ? OR u.full_name LIKE ?"
                    + " OR u.email LIKE ? OR sh.tracking_number LIKE ? OR s.shop_name LIKE ?)");
            String like = "%" + query + "%";
            for (int i = 0; i < 6; i++) args.add(like);
        }
        sql.append(" ORDER BY CASE d.status WHEN 'OPEN' THEN 0 WHEN 'UNDER_REVIEW' THEN 1 ELSE 2 END,"
                + " d.created_at DESC,d.dispute_id DESC LIMIT ").append(MAX_ADMIN_ROWS);
        return query(sql.toString(), args.toArray());
    }

    public static Map<String, Object> detailForAdmin(long disputeId) {
        List<Map<String, Object>> rows = query(adminSelect() + " WHERE d.dispute_id=?", disputeId);
        if (rows.isEmpty()) return null;
        Map<String, Object> detail = rows.getFirst();
        detail.put("events", query(
                "SELECT e.event_id id,e.action,e.from_status fromStatus,e.to_status toStatus,e.note,"
                + "e.created_at createdAt,COALESCE(u.full_name,'Hệ thống') actorName"
                + " FROM delivery_dispute_events e LEFT JOIN users u ON u.user_id=e.actor_user_id"
                + " WHERE e.dispute_id=? ORDER BY e.created_at,e.event_id", disputeId));
        return detail;
    }

    public static void actAsAdmin(long disputeId, long adminId, String rawAction, String rawNote) {
        String action = DeliveryDisputePolicy.normalizeAction(rawAction);
        String note = cleanRequired(rawNote, 2000, "Vui lòng ghi chú xử lý ít nhất 3 ký tự.");
        if (note.length() < 3) throw new IllegalArgumentException("Vui lòng ghi chú xử lý ít nhất 3 ký tự.");
        inTransaction(conn -> {
            long orderId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT order_id FROM delivery_disputes WHERE dispute_id=?")) {
                ps.setLong(1, disputeId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy khiếu nại giao hàng.");
                    orderId = rs.getLong(1);
                }
            }
            String orderCode;
            long customerId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT order_code,user_id FROM orders WHERE order_id=? FOR UPDATE")) {
                ps.setLong(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy đơn hàng.");
                    orderCode = rs.getString("order_code");
                    customerId = rs.getLong("user_id");
                }
            }
            long shipmentId;
            String current;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT shipment_id,status FROM delivery_disputes WHERE dispute_id=? AND order_id=? FOR UPDATE")) {
                ps.setLong(1, disputeId);
                ps.setLong(2, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new IllegalArgumentException("Khiếu nại đã thay đổi hoặc không còn tồn tại.");
                    shipmentId = rs.getLong("shipment_id");
                    current = rs.getString("status");
                }
            }
            String target = DeliveryDisputePolicy.targetStatus(action);
            if (!DeliveryDisputePolicy.canTransition(current, target)) {
                throw new IllegalArgumentException("Không thể xử lý khiếu nại từ " + current + " sang " + target + ".");
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE delivery_disputes SET status=?,resolution_note=?,admin_id=?,"
                    + "resolved_at=CASE WHEN ? IN ('RESOLVED','REJECTED') THEN NOW() ELSE NULL END,updated_at=NOW()"
                    + " WHERE dispute_id=?")) {
                ps.setString(1, target);
                ps.setString(2, note);
                ps.setLong(3, adminId);
                ps.setString(4, target);
                ps.setLong(5, disputeId);
                if (ps.executeUpdate() != 1) throw new IllegalArgumentException("Không cập nhật được khiếu nại.");
            }
            if (DeliveryDisputePolicy.TERMINAL_STATUSES.contains(target)) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE shipments SET receipt_status='WAITING' WHERE shipment_id=? AND receipt_status='DISPUTED'")) {
                    ps.setLong(1, shipmentId);
                    ps.executeUpdate();
                }
            }
            insertDisputeEvent(conn, disputeId, adminId, action, current, target, note);
            insertShipmentEvent(conn, orderId, shipmentId,
                    "Admin cập nhật khiếu nại giao hàng",
                    "Khiếu nại #" + disputeId + ": " + current + " → " + target + " · " + note);
            NotificationService.notifyUser(conn, customerId, "DELIVERY_DISPUTE_UPDATED",
                    "Cập nhật khiếu nại giao hàng",
                    "Khiếu nại giao hàng của đơn " + orderCode + " đã được cập nhật: " + target + ". " + note,
                    "/order-tracking?code=" + orderCode, "DELIVERY_DISPUTE", disputeId);
            return null;
        });
    }

    public static boolean hasActiveDispute(Connection conn, long orderId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM delivery_disputes WHERE order_id=? AND status IN ('OPEN','UNDER_REVIEW') LIMIT 1")) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private static ShipmentContext lockCustomerShipment(Connection conn, long shipmentId, long customerId)
            throws SQLException {
        long orderId;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT sh.order_id FROM shipments sh JOIN orders o ON o.order_id=sh.order_id"
                + " WHERE sh.shipment_id=? AND o.user_id=?")) {
            ps.setLong(1, shipmentId);
            ps.setLong(2, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy vận đơn thuộc tài khoản của bạn.");
                orderId = rs.getLong(1);
            }
        }
        String orderCode;
        String orderStatus;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT order_code,status FROM orders WHERE order_id=? AND user_id=? FOR UPDATE")) {
            ps.setLong(1, orderId);
            ps.setLong(2, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Đơn hàng không tồn tại hoặc không thuộc tài khoản của bạn.");
                orderCode = rs.getString("order_code");
                orderStatus = rs.getString("status");
            }
        }
        String shipmentStatus;
        String receiptStatus;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT status,receipt_status FROM shipments WHERE shipment_id=? AND order_id=? FOR UPDATE")) {
            ps.setLong(1, shipmentId);
            ps.setLong(2, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy vận đơn.");
                shipmentStatus = rs.getString("status");
                receiptStatus = rs.getString("receipt_status");
            }
        }
        return new ShipmentContext(orderId, orderCode, orderStatus, shipmentStatus, receiptStatus);
    }

    private static boolean hasActiveDispute(Connection conn, long shipmentId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM delivery_disputes WHERE shipment_id=? AND status IN ('OPEN','UNDER_REVIEW') LIMIT 1")) {
            ps.setLong(1, shipmentId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private static void completeOrderIfReady(Connection conn, long orderId, long customerId, String currentStatus)
            throws SQLException {
        if (!OrderStatus.canTransition(currentStatus, OrderStatus.COMPLETED)) return;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM shipments WHERE order_id=? AND ("
                + "UPPER(TRIM(COALESCE(status,''))) NOT IN ('DELIVERED','COMPLETED','ĐÃ GIAO')"
                + " OR COALESCE(receipt_status,'NOT_READY') NOT IN ('CONFIRMED','NOT_REQUIRED'))")) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || rs.getLong(1) != 0) return;
            }
        }
        if (hasActiveDispute(conn, orderId)) return;
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE orders SET status=?,completed_at=NOW() WHERE order_id=? AND status=?")) {
            ps.setString(1, OrderStatus.COMPLETED);
            ps.setLong(2, orderId);
            ps.setString(3, currentStatus);
            if (ps.executeUpdate() != 1) return;
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE order_items SET status=? WHERE order_id=?")) {
            ps.setString(1, OrderStatus.COMPLETED);
            ps.setLong(2, orderId);
            ps.executeUpdate();
        }
        OrderDao.insertHistory(conn, orderId, customerId, currentStatus, OrderStatus.COMPLETED,
                "Khách hàng đã xác nhận nhận đủ tất cả kiện hàng.");
    }

    private static String adminSelect() {
        return "SELECT d.dispute_id id,d.dispute_code disputeCode,d.shipment_id shipmentId,"
                + "d.order_id orderId,o.order_code orderCode,d.customer_id customerId,"
                + "u.full_name customerName,u.email customerEmail,d.dispute_type disputeType,"
                + "d.description,d.status,d.resolution_note resolutionNote,d.admin_id adminId,"
                + "a.full_name adminName,sh.carrier,sh.tracking_number trackingNumber,sh.status shipmentStatus,"
                + "sh.receipt_status receiptStatus,sh.delivered_at deliveredAt,s.shop_name shopName,"
                + "d.created_at createdAt,d.updated_at updatedAt,d.resolved_at resolvedAt"
                + " FROM delivery_disputes d JOIN shipments sh ON sh.shipment_id=d.shipment_id"
                + " JOIN orders o ON o.order_id=d.order_id LEFT JOIN users u ON u.user_id=d.customer_id"
                + " LEFT JOIN users a ON a.user_id=d.admin_id LEFT JOIN print_shops s ON s.shop_id=sh.shop_id";
    }

    private static void insertDisputeEvent(Connection conn, long disputeId, long actorId,
                                           String action, String from, String to, String note) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO delivery_dispute_events(dispute_id,actor_user_id,action,from_status,to_status,note,created_at)"
                + " VALUES(?,?,?,?,?,?,NOW())")) {
            ps.setLong(1, disputeId);
            ps.setLong(2, actorId);
            ps.setString(3, action);
            ps.setString(4, from);
            ps.setString(5, to);
            ps.setString(6, note);
            ps.executeUpdate();
        }
    }

    private static void insertShipmentEvent(Connection conn, long orderId, long shipmentId,
                                            String title, String description) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO shipment_events(order_id,shipment_id,event_title,description,occurred_at)"
                + " VALUES(?,?,?,?,NOW())")) {
            ps.setLong(1, orderId);
            ps.setLong(2, shipmentId);
            ps.setString(3, clip(title, 180));
            ps.setString(4, clip(description, 1000));
            ps.executeUpdate();
        }
    }

    private static String generateCode() {
        return "DD-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
                + String.format(Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
    }

    private static String cleanRequired(String raw, int max, String error) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) throw new IllegalArgumentException(error);
        if (value.length() > max) throw new IllegalArgumentException("Nội dung vượt quá " + max + " ký tự.");
        return value;
    }

    private static String clip(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static List<Map<String, Object>> query(String sql, Object... args) {
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                        String label = rs.getMetaData().getColumnLabel(i);
                        Object value = rs.getObject(i);
                        if (value instanceof Timestamp timestamp) value = timestamp.toLocalDateTime().toString();
                        else if (value instanceof Number && label.toLowerCase(Locale.ROOT).endsWith("id")) value = value.toString();
                        row.put(label, value);
                    }
                    result.add(row);
                }
            }
            return result;
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    private static <T> T inTransaction(SqlWork<T> work) {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                T value = work.run(conn);
                conn.commit();
                return value;
            } catch (SQLException | RuntimeException e) {
                try { conn.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    @FunctionalInterface
    private interface SqlWork<T> { T run(Connection conn) throws SQLException; }

    private record ShipmentContext(long orderId, String orderCode, String orderStatus,
                                   String shipmentStatus, String receiptStatus) { }
}
