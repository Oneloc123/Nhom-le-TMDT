package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.OrderStatus;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.GhnShippingService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.NotificationService;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class GhnDeliveryDao {
    private static final GhnShippingService GHN = new GhnShippingService();
    private static final System.Logger LOG = System.getLogger(GhnDeliveryDao.class.getName());
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String ACTIVE_CASES = """
            EXISTS (SELECT 1 FROM support_tickets t WHERE t.order_id=o.order_id
                       AND t.status IN ('OPEN','IN_PROGRESS','WAITING_CUSTOMER'))
            OR EXISTS (SELECT 1 FROM return_refund_requests r WHERE r.order_id=o.order_id
                       AND r.status NOT IN ('REFUNDED','REJECTED','WITHDRAWN'))
            """;

    private GhnDeliveryDao() { }

    public record Receipt(String trackingNumber, String deadline, boolean awaitingConfirmation, boolean openCase) {
        public String getTrackingNumber() { return trackingNumber; }
        public String getDeadline() { return deadline; }
        public boolean isAwaitingConfirmation() { return awaitingConfirmation; }
        public boolean isOpenCase() { return openCase; }
    }

    public static void register(Connection conn, long orderId, String province, String district, String ward)
            throws SQLException {
        try (var ps = conn.prepareStatement(
                "INSERT INTO ghn_deliveries(order_id,province,district,ward) VALUES(?,?,?,?)")) {
            ps.setLong(1, orderId);
            ps.setString(2, province);
            ps.setString(3, district);
            ps.setString(4, ward);
            ps.executeUpdate();
        }
    }

    public static void changeDestination(Connection conn, long orderId, String province, String district, String ward)
            throws SQLException {
        if (!isManaged(conn, orderId)) return;
        if (province == null || province.isBlank() || ward == null || ward.isBlank()) {
            throw new IllegalArgumentException("Địa chỉ GHN phải có tỉnh/thành và phường/xã.");
        }
        try (var ps = conn.prepareStatement("SELECT 1 FROM ghn_deliveries WHERE order_id=? AND shipment_id IS NOT NULL")) {
            ps.setLong(1, orderId);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) throw new IllegalArgumentException("Đơn đã tạo vận đơn GHN.");
            }
        }
        try (var ps = conn.prepareStatement(
                "UPDATE ghn_deliveries SET province=?,district=?,ward=? WHERE order_id=? AND shipment_id IS NULL")) {
            ps.setString(1, province);
            ps.setString(2, district);
            ps.setString(3, ward);
            ps.setLong(4, orderId);
            ps.executeUpdate();
        }
    }

    public static boolean isManaged(Connection conn, long orderId) throws SQLException {
        try (var ps = conn.prepareStatement("SELECT 1 FROM ghn_deliveries WHERE order_id=?")) {
            ps.setLong(1, orderId);
            try (var rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    // The caller owns the order row lock and has already checked shop ownership.
    static void createShipment(Connection conn, long orderId, long actorId) throws SQLException {
        String sql = """
                SELECT o.order_code,o.user_id,o.status,o.payment_status,o.total_amount,
                       o.recipient_name_snapshot,o.recipient_phone_snapshot,o.shipping_address_snapshot,
                       g.province,g.district,g.ward,g.shipment_id
                FROM orders o JOIN ghn_deliveries g ON g.order_id=o.order_id WHERE o.order_id=?
                """;
        long customer;
        String orderCode;
        String orderStatus;
        String tracking;
        try (var ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Đơn chưa có địa chỉ báo giá GHN.");
                if (rs.getObject("shipment_id") != null) return;
                orderCode = rs.getString("order_code");
                customer = rs.getLong("user_id");
                orderStatus = rs.getString("status");
                if (!(OrderStatus.CONFIRMED.equals(orderStatus) || OrderStatus.PROCESSING.equals(orderStatus))) {
                    throw new IllegalArgumentException("Đơn chưa sẵn sàng tạo vận đơn.");
                }
                int cod = codAmount(rs.getString("payment_status"), rs.getBigDecimal("total_amount"));
                requireReady(conn, orderId);
                tracking = GHN.createShipment(orderCode, rs.getString("recipient_name_snapshot"),
                        rs.getString("recipient_phone_snapshot"), rs.getString("shipping_address_snapshot"),
                        rs.getString("province"), rs.getString("district"), rs.getString("ward"), cod);
            }
        }

        long shipmentId;
        try (var ps = conn.prepareStatement("""
                INSERT INTO shipments(order_id,carrier,tracking_number,status,updated_at)
                VALUES(?,'GHN',?,'PREPARING',NOW())
                """, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, orderId);
            ps.setString(2, tracking);
            ps.executeUpdate();
            try (var rs = ps.getGeneratedKeys()) {
                if (!rs.next()) throw new SQLException("Không lấy được mã vận đơn nội bộ.");
                shipmentId = rs.getLong(1);
            }
        }
        try (var ps = conn.prepareStatement("""
                UPDATE ghn_deliveries SET shipment_id=?,provider_status='ready_to_pick',next_check_at=NOW()
                WHERE order_id=? AND shipment_id IS NULL
                """)) {
            ps.setLong(1, shipmentId);
            ps.setLong(2, orderId);
            if (ps.executeUpdate() != 1) throw new SQLException("Không gắn được vận đơn vào đơn hàng.");
        }
        setOrderState(conn, orderId, OrderStatus.SHIPPED, "Chờ GHN lấy hàng", false);
        updateItems(conn, orderId, OrderStatus.SHIPPED);
        OrderDao.insertHistory(conn, orderId, actorId, orderStatus, OrderStatus.SHIPPED,
                "Đã tạo vận đơn GHN. Hàng chờ lấy tại Đại học Nông Lâm.");
        addEvent(conn, orderId, shipmentId, "ready_to_pick", "Chờ GHN lấy hàng");
        notifyCustomer(conn, orderId, customer, orderCode, "Đã tạo vận đơn GHN",
                "Mã vận đơn " + tracking + ". Hàng chờ GHN lấy tại Đại học Nông Lâm.");
    }

    static int codAmount(String payment, BigDecimal total) {
        if (OrderStatus.PAYMENT_PAID.equals(payment)) return 0;
        if (OrderStatus.PAYMENT_COD.equals(payment)) {
            try {
                int amount = total.intValueExact();
                if (amount > 0 && amount <= 50_000_000) return amount;
            } catch (ArithmeticException ignored) { }
            throw new IllegalArgumentException("Tổng COD phải là số VND nguyên trong giới hạn GHN.");
        }
        throw new IllegalArgumentException("Đơn cần thanh toán hoặc xác nhận COD trước khi gửi hàng.");
    }

    private static void requireReady(Connection conn, long orderId) throws SQLException {
        String sql = """
                SELECT COUNT(*), COALESCE(SUM(CASE WHEN COALESCE((
                    SELECT pu.stage FROM production_updates pu WHERE pu.order_item_id=oi.order_item_id
                    ORDER BY pu.created_at DESC,pu.production_update_id DESC LIMIT 1
                ),'')='READY_FOR_SHIPPING' THEN 1 ELSE 0 END),0)
                FROM order_items oi WHERE oi.order_id=? AND COALESCE(oi.status,'')<>?
                """;
        try (var ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            ps.setString(2, OrderStatus.CANCELLED);
            try (var rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) == 0 || rs.getInt(1) != rs.getInt(2)) {
                    throw new IllegalArgumentException("Tất cả sản phẩm trong đơn phải sẵn sàng giao trước khi tạo vận đơn GHN.");
                }
            }
        }
        try (var ps = conn.prepareStatement("SELECT 1 FROM shipments WHERE order_id=? LIMIT 1")) {
            ps.setLong(1, orderId);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) throw new IllegalArgumentException("Đơn đã có vận đơn khác. Liên hệ quản trị để kiểm tra.");
            }
        }
    }

    public static Receipt receipt(long orderId) {
        String sql = """
                SELECT s.tracking_number,g.confirm_by,o.status, (%s) AS open_case
                FROM orders o JOIN ghn_deliveries g ON g.order_id=o.order_id
                JOIN shipments s ON s.shipment_id=g.shipment_id WHERE o.order_id=?
                """.formatted(ACTIVE_CASES);
        try (var conn = Database.getConnection(); var ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                var deadline = rs.getTimestamp("confirm_by");
                return new Receipt(rs.getString("tracking_number"),
                        deadline == null ? null : deadline.toLocalDateTime().format(DISPLAY_TIME),
                        deadline != null && OrderStatus.SHIPPED.equals(rs.getString("status")),
                        rs.getBoolean("open_case"));
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static boolean confirmReceipt(long orderId, long userId) {
        return complete(orderId, userId);
    }

    public static void completeDue() {
        String sql = """
                SELECT o.order_id FROM ghn_deliveries g JOIN orders o ON o.order_id=g.order_id
                WHERE g.confirm_by<=NOW() AND o.status=? AND NOT (%s)
                ORDER BY g.confirm_by LIMIT 100
                """.formatted(ACTIVE_CASES);
        List<Long> due = new ArrayList<>();
        try (var conn = Database.getConnection(); var ps = conn.prepareStatement(sql)) {
            ps.setString(1, OrderStatus.SHIPPED);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) due.add(rs.getLong(1));
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
        for (long id : due) {
            try { complete(id, null); }
            catch (RuntimeException e) { LOG.log(System.Logger.Level.WARNING, "Could not close order " + id, e); }
        }
    }

    private static boolean complete(long orderId, Long customerId) {
        return transact(conn -> {
            OrderState order = lockOrder(conn, orderId);
            if (order == null || customerId != null && customerId != order.customerId()) return false;
            if (OrderStatus.COMPLETED.equals(order.status())) return isManaged(conn, orderId);
            if (!OrderStatus.SHIPPED.equals(order.status())) return false;
            String sql = """
                    SELECT 1 FROM ghn_deliveries g JOIN orders o ON o.order_id=g.order_id
                    WHERE o.order_id=? AND g.provider_status='delivered' AND g.confirm_by IS NOT NULL
                        AND (? OR g.confirm_by<=NOW()) AND NOT (%s)
                    """.formatted(ACTIVE_CASES);
            try (var ps = conn.prepareStatement(sql)) {
                ps.setLong(1, orderId);
                ps.setBoolean(2, customerId != null);
                try (var rs = ps.executeQuery()) { if (!rs.next()) return false; }
            }
            setOrderState(conn, orderId, OrderStatus.COMPLETED, OrderStatus.SHIPPING_DELIVERED, true);
            updateItems(conn, orderId, OrderStatus.COMPLETED);
            String note = customerId == null ? "Tự hoàn thành sau 72 giờ, không có khiếu nại đang mở."
                    : "Khách hàng xác nhận đã nhận đủ và đúng hàng.";
            OrderDao.insertHistory(conn, orderId, customerId, order.status(), OrderStatus.COMPLETED, note);
            notifyCustomer(conn, orderId, order.customerId(), order.code(), "Đơn hàng đã hoàn thành", note);
            return true;
        });
    }

    public static void syncActive() {
        record Candidate(long orderId, String tracking) { }
        List<Candidate> active = new ArrayList<>();
        try (var conn = Database.getConnection(); var ps = conn.prepareStatement("""
                SELECT g.order_id,s.tracking_number FROM ghn_deliveries g
                JOIN shipments s ON s.shipment_id=g.shipment_id JOIN orders o ON o.order_id=g.order_id
                WHERE o.status=? AND g.next_check_at<=NOW() ORDER BY g.next_check_at,g.order_id LIMIT 20
                """)) {
            ps.setString(1, OrderStatus.SHIPPED);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) active.add(new Candidate(rs.getLong(1), rs.getString(2)));
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
        for (var shipment : active) {
            if (Thread.currentThread().isInterrupted()) return;
            try {
                if (reserveCheck(shipment.orderId())) sync(shipment.orderId(), GHN.detail(shipment.tracking()));
            } catch (RuntimeException e) {
                LOG.log(System.Logger.Level.WARNING, "Could not sync GHN order " + shipment.orderId()
                        + ": " + e.getClass().getSimpleName());
            }
        }
    }

    private static boolean reserveCheck(long orderId) {
        try (var conn = Database.getConnection(); var ps = conn.prepareStatement("""
                UPDATE ghn_deliveries SET next_check_at=DATE_ADD(NOW(),INTERVAL 2 MINUTE)
                WHERE order_id=? AND next_check_at<=NOW()
                """)) {
            ps.setLong(1, orderId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    private static void sync(long orderId, GhnShippingService.DeliveryStatus update) {
        var milestone = GhnDeliveryStage.from(update.status());
        transact(conn -> {
            OrderState order = lockOrder(conn, orderId);
            if (order == null || !OrderStatus.SHIPPED.equals(order.status()) || milestone == null) return null;
            long shipmentId;
            String previous;
            try (var ps = conn.prepareStatement("""
                    SELECT shipment_id,provider_status,provider_updated_at_ms FROM ghn_deliveries
                    WHERE order_id=? AND next_check_at IS NOT NULL FOR UPDATE
                    """)) {
                ps.setLong(1, orderId);
                try (var rs = ps.executeQuery()) {
                    if (!rs.next() || update.version() <= rs.getLong("provider_updated_at_ms")) return null;
                    shipmentId = rs.getLong("shipment_id");
                    previous = rs.getString("provider_status");
                }
            }
            try (var ps = conn.prepareStatement("""
                    UPDATE ghn_deliveries SET provider_status=?,provider_updated_at_ms=?,
                    next_check_at=IF(?,NULL,next_check_at),
                    confirm_by=IF(?,DATE_ADD(NOW(),INTERVAL 72 HOUR),confirm_by) WHERE order_id=?
                    """)) {
                ps.setString(1, update.status());
                ps.setLong(2, update.version());
                ps.setBoolean(3, milestone.terminal());
                ps.setBoolean(4, milestone.delivered());
                ps.setLong(5, orderId);
                ps.executeUpdate();
            }
            if (milestone.equals(GhnDeliveryStage.from(previous))) return null;
            try (var ps = conn.prepareStatement("""
                    UPDATE shipments SET status=?,updated_at=NOW(),
                        shipped_at=IF(?,COALESCE(shipped_at,NOW()),shipped_at),
                        delivered_at=IF(?,COALESCE(delivered_at,NOW()),delivered_at)
                    WHERE shipment_id=?
                    """)) {
                ps.setString(1, milestone.shipmentStatus());
                ps.setBoolean(2, milestone.inCarrierCustody());
                ps.setBoolean(3, milestone.delivered());
                ps.setLong(4, shipmentId);
                ps.executeUpdate();
            }
            setOrderState(conn, orderId, order.status(), milestone.title(), false);
            addEvent(conn, orderId, shipmentId, update.status(), milestone.title());
            if (milestone.delivered()) {
                notifyCustomer(conn, orderId, order.customerId(), order.code(), "GHN đã giao hàng",
                        "Vui lòng xác nhận đã nhận hàng hoặc báo có vấn đề trong vòng 72 giờ.");
            }
            return null;
        });
    }

    private static OrderState lockOrder(Connection conn, long id) throws SQLException {
        try (var ps = conn.prepareStatement("SELECT user_id,order_code,status FROM orders WHERE order_id=? FOR UPDATE")) {
            ps.setLong(1, id);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? new OrderState(rs.getLong(1), rs.getString(2), rs.getString(3)) : null;
            }
        }
    }

    private static void setOrderState(Connection conn, long id, String status, String shipping, boolean completed)
            throws SQLException {
        try (var ps = conn.prepareStatement("""
                UPDATE orders SET status=?,shipping_status=?,completed_at=IF(?,NOW(),completed_at) WHERE order_id=?
                """)) {
            ps.setString(1, status);
            ps.setString(2, shipping);
            ps.setBoolean(3, completed);
            ps.setLong(4, id);
            ps.executeUpdate();
        }
    }

    private static void updateItems(Connection conn, long id, String status) throws SQLException {
        try (var ps = conn.prepareStatement("UPDATE order_items SET status=? WHERE order_id=? AND COALESCE(status,'')<>?")) {
            ps.setString(1, status);
            ps.setLong(2, id);
            ps.setString(3, OrderStatus.CANCELLED);
            ps.executeUpdate();
        }
    }

    private static void addEvent(Connection conn, long orderId, long shipmentId, String code, String title)
            throws SQLException {
        try (var ps = conn.prepareStatement("""
                INSERT INTO shipment_events(shipment_id,order_id,event_code,event_title,occurred_at)
                VALUES(?,?,?,?,NOW())
                """)) {
            ps.setLong(1, shipmentId);
            ps.setLong(2, orderId);
            ps.setString(3, code);
            ps.setString(4, title);
            ps.executeUpdate();
        }
    }

    private static void notifyCustomer(Connection conn, long id, long customer, String code, String title, String body)
            throws SQLException {
        NotificationService.notifyUser(conn, customer, "SHIPMENT_UPDATED", title, body,
                "/order-tracking?code=" + code, "ORDER", id);
    }

    private record OrderState(long customerId, String code, String status) { }
    private interface Transaction<T> { T execute(Connection conn) throws SQLException; }

    private static <T> T transact(Transaction<T> operation) {
        try (var conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                T result = operation.execute(conn);
                conn.commit();
                return result;
            } catch (SQLException | RuntimeException error) {
                try { conn.rollback(); } catch (SQLException rollback) { error.addSuppressed(rollback); }
                throw error;
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }
}
