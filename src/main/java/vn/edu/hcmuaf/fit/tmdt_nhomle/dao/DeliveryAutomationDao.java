package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.OrderStatus;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.GhnShippingService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.GhnTrackingPolicy;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.NotificationService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.ShipmentPolicy;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** GHN webhook ingestion, polling fallback, reminder delivery and 72-hour receipt automation. */
public final class DeliveryAutomationDao {
    private static final System.Logger LOG = System.getLogger(DeliveryAutomationDao.class.getName());
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int POLL_BATCH_SIZE = 80;
    private static final int AUTOMATION_BATCH_SIZE = 200;

    private DeliveryAutomationDao() { }

    public enum WebhookResult { PROCESSED, DUPLICATE, UNMATCHED, IGNORED }

    public static WebhookResult processGhnEvent(String rawOrderCode, String rawType, String rawStatus,
                                                 String rawTime, String rawReason, String rawPodUrl,
                                                 String rawPayload) {
        String orderCode = required(rawOrderCode, 120, "Mã vận đơn GHN");
        String type = required(rawType, 40, "Loại sự kiện GHN").toLowerCase(Locale.ROOT);
        String status = rawStatus == null ? "" : rawStatus.trim().toLowerCase(Locale.ROOT);
        String eventTimeText = required(rawTime, 80, "Thời điểm sự kiện GHN");
        LocalDateTime eventTime = parseEventTime(eventTimeText);
        String reason = bounded(rawReason, 500);
        String podUrl = safePodUrl(rawPodUrl);
        String payload = sanitizePayload(rawPayload);
        if (payload.length() > 65536) throw new IllegalArgumentException("Webhook GHN vượt quá giới hạn dữ liệu.");

        String eventKey = sha256(orderCode + "|" + type + "|" + eventTimeText);
        String nextStatus = ("switch_status".equals(type) || "create".equals(type))
                ? GhnTrackingPolicy.mapStatus(status) : null;

        return inTransaction(conn -> {
            long eventId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO carrier_webhook_events(provider,event_key,provider_order_code,event_type,"
                    + "external_status,event_time,processing_status,payload_json,received_at)"
                    + " VALUES('GHN',?,?,?,?,?,'RECEIVED',?,NOW())",
                    PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, eventKey);
                ps.setString(2, orderCode);
                ps.setString(3, type);
                ps.setString(4, status.isEmpty() ? null : status);
                ps.setTimestamp(5, Timestamp.valueOf(eventTime));
                ps.setString(6, payload);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("Không tạo được nhật ký webhook GHN.");
                    eventId = keys.getLong(1);
                }
            } catch (SQLException error) {
                if (error.getErrorCode() == 1062) return WebhookResult.DUPLICATE;
                throw error;
            }

            if (!GhnTrackingPolicy.shouldTrackEventType(type)) {
                markEvent(conn, eventId, null, "IGNORED", "Loại sự kiện GHN chưa được hỗ trợ.");
                return WebhookResult.IGNORED;
            }
            Long shipmentId = findShipmentId(conn, orderCode);
            if (shipmentId == null) {
                markEvent(conn, eventId, null, "IGNORED", "Không tìm thấy vận đơn PrintKraft có provider GHN và mã tương ứng.");
                return WebhookResult.UNMATCHED;
            }
            if (nextStatus == null && ("switch_status".equals(type) || "create".equals(type))) {
                markEvent(conn, eventId, shipmentId, "IGNORED", "Mã trạng thái GHN chưa được ánh xạ: " + status);
                return WebhookResult.IGNORED;
            }
            if (nextStatus == null) {
                // COD/fee/weight callbacks are linked to the shipment for Phase 3 reconciliation,
                // but no financial ledger entry is created in this phase.
                markEvent(conn, eventId, shipmentId, "PROCESSED", null);
                return WebhookResult.PROCESSED;
            }

            applyGhnStatus(conn, eventId, shipmentId, orderCode, type, status,
                    eventTime, reason, podUrl, "WEBHOOK");
            return eventOutcome(conn, eventId);
        });
    }

    /** Polls a bounded batch of active GHN shipments. Called by the container scheduler. */
    public static void pollGhnShipments() {
        List<PollTarget> targets = listPollTargets();
        for (PollTarget target : targets) {
            markPollAttempt(target.shipmentId());
            try {
                GhnShippingService.GhnTrackingStatus status =
                        GhnShippingService.fetchTrackingStatus(target.trackingNumber());
                if (status.updatedAt() == null || status.updatedAt().isBlank()) continue;
                String syntheticPayload = "{\"OrderCode\":\"" + jsonEscape(status.orderCode())
                        + "\",\"Type\":\"switch_status\",\"Status\":\"" + jsonEscape(status.status())
                        + "\",\"Time\":\"" + jsonEscape(status.updatedAt()) + "\",\"Source\":\"POLL\"}";
                processGhnEvent(status.orderCode(), "switch_status", status.status(),
                        status.updatedAt(), status.reason(), null, syntheticPayload);
            } catch (RuntimeException error) {
                LOG.log(System.Logger.Level.WARNING,
                        "GHN fallback tracking lookup failed for shipment #" + target.shipmentId()
                                + " (tracking code masked)", error);
            }
        }
    }

    /** Sends one reminder near the 48-hour point and auto-confirms only at/after 72 hours. */
    public static void processReceiptAutomation() {
        for (Long shipmentId : listReceiptRemindersDue()) {
            try { sendReceiptReminder(shipmentId); }
            catch (RuntimeException error) {
                LOG.log(System.Logger.Level.WARNING, "Receipt reminder failed for shipment #" + shipmentId, error);
            }
        }
        for (Long shipmentId : listAutoConfirmationsDue()) {
            try { autoConfirmShipment(shipmentId); }
            catch (RuntimeException error) {
                LOG.log(System.Logger.Level.WARNING, "Automatic receipt confirmation failed for shipment #" + shipmentId, error);
            }
        }
    }

    private static void sendReceiptReminder(long shipmentId) {
        inTransaction(conn -> {
            OrderContext order = lockOrderForShipment(conn, shipmentId);
            if (order == null) return null;
            String orderCode;
            LocalDateTime deadline;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT receipt_status,receipt_deadline_at,receipt_reminder_sent_at,"
                    + "carrier_delivery_verified_at FROM shipments WHERE shipment_id=? AND order_id=? FOR UPDATE")) {
                ps.setLong(1, shipmentId);
                ps.setLong(2, order.orderId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || !"WAITING".equalsIgnoreCase(rs.getString("receipt_status"))
                            || rs.getTimestamp("receipt_reminder_sent_at") != null
                            || rs.getTimestamp("carrier_delivery_verified_at") == null) return null;
                    Timestamp time = rs.getTimestamp("receipt_deadline_at");
                    if (time == null) return null;
                    deadline = time.toLocalDateTime();
                    if (!deadline.isAfter(LocalDateTime.now(APP_ZONE))
                            || deadline.isAfter(LocalDateTime.now(APP_ZONE).plusHours(24))) return null;
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE shipments SET receipt_reminder_sent_at=NOW()"
                    + " WHERE shipment_id=? AND receipt_status='WAITING' AND receipt_reminder_sent_at IS NULL")) {
                ps.setLong(1, shipmentId);
                if (ps.executeUpdate() != 1) return null;
            }
            orderCode = order.orderCode();
            NotificationService.notifyUser(conn, order.customerId(), "RECEIPT_CONFIRMATION_REMINDER",
                    "Bạn vui lòng xác nhận đã nhận hàng",
                    "Đơn " + orderCode + " đã được hãng vận chuyển báo giao. Vui lòng kiểm tra kiện hàng và xác nhận trước "
                            + deadline.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                            + ". Nếu có vấn đề, hãy gửi khiếu nại ngay trên trang theo dõi đơn.",
                    "/order-tracking?code=" + orderCode, "SHIPMENT", shipmentId);
            return null;
        });
    }

    private static void autoConfirmShipment(long shipmentId) {
        inTransaction(conn -> {
            OrderContext order = lockOrderForShipment(conn, shipmentId);
            if (order == null || !OrderStatus.canTransition(order.orderStatus(), OrderStatus.COMPLETED)) return null;
            String receiptStatus;
            LocalDateTime deadline;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT receipt_status,receipt_deadline_at,carrier_delivery_verified_at"
                    + " FROM shipments WHERE shipment_id=? AND order_id=? FOR UPDATE")) {
                ps.setLong(1, shipmentId);
                ps.setLong(2, order.orderId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;
                    receiptStatus = rs.getString("receipt_status");
                    Timestamp value = rs.getTimestamp("receipt_deadline_at");
                    deadline = value == null ? null : value.toLocalDateTime();
                    if (!"WAITING".equalsIgnoreCase(receiptStatus) || deadline == null
                            || deadline.isAfter(LocalDateTime.now(APP_ZONE))
                            || rs.getTimestamp("carrier_delivery_verified_at") == null) return null;
                }
            }
            if (DeliveryDisputeDao.hasActiveDispute(conn, order.orderId())) return null;
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE shipments SET receipt_status='CONFIRMED',receipt_confirmed_at=NOW(),"
                    + "receipt_auto_confirmed_at=NOW(),receipt_confirmed_by_user_id=NULL"
                    + " WHERE shipment_id=? AND receipt_status='WAITING' AND receipt_deadline_at<=NOW()"
                    + " AND carrier_delivery_verified_at IS NOT NULL")) {
                ps.setLong(1, shipmentId);
                if (ps.executeUpdate() != 1) return null;
            }
            insertShipmentEvent(conn, order.orderId(), shipmentId,
                    "Hệ thống tự động xác nhận nhận hàng sau 72 giờ",
                    "GHN đã xác nhận giao thành công; khách không gửi khiếu nại và không xác nhận trong thời hạn 72 giờ.");
            NotificationService.notifyUser(conn, order.customerId(), "RECEIPT_AUTO_CONFIRMED",
                    "Đơn hàng được tự động xác nhận hoàn tất",
                    "Đơn " + order.orderCode() + " đã được tự động xác nhận nhận hàng sau 72 giờ kể từ khi GHN báo giao thành công. Nếu cần hỗ trợ sau giao hàng, bạn vẫn có thể gửi yêu cầu theo chính sách đổi trả.",
                    "/purchase-history", "SHIPMENT", shipmentId);

            if (DeliveryDisputeDao.allShipmentsReceiptConfirmed(conn, order.orderId())
                    && !DeliveryDisputeDao.hasActiveDispute(conn, order.orderId())
                    && OrderStatus.canTransition(order.orderStatus(), OrderStatus.COMPLETED)) {
                String completionNote = "Hệ thống tự động hoàn tất sau khi toàn bộ kiện hàng có bằng chứng GHN giao thành công, hết thời hạn 72 giờ và không có khiếu nại.";
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE orders SET status=?,completed_at=NOW() WHERE order_id=? AND status=?")) {
                    ps.setString(1, OrderStatus.COMPLETED);
                    ps.setLong(2, order.orderId());
                    ps.setString(3, order.orderStatus());
                    if (ps.executeUpdate() == 1) {
                        try (PreparedStatement items = conn.prepareStatement(
                                "UPDATE order_items SET status=? WHERE order_id=?")) {
                            items.setString(1, OrderStatus.COMPLETED);
                            items.setLong(2, order.orderId());
                            items.executeUpdate();
                        }
                        // changed_by_user_id is deliberately NULL: the actor is the automation job, not the customer.
                        try (PreparedStatement history = conn.prepareStatement(
                                "INSERT INTO order_status_history(order_id,changed_by_user_id,old_status,new_status,note,changed_at)"
                                + " VALUES(?,NULL,?,?,?,NOW())")) {
                            history.setLong(1, order.orderId());
                            history.setString(2, order.orderStatus());
                            history.setString(3, OrderStatus.COMPLETED);
                            history.setString(4, completionNote);
                            history.executeUpdate();
                        }
                        NotificationService.notifyUser(conn, order.customerId(), "ORDER_COMPLETED",
                                "Đơn hàng đã hoàn tất",
                                "Đơn " + order.orderCode() + " đã được tự động hoàn tất sau khi tất cả kiện hàng được xác nhận và không có khiếu nại.",
                                "/purchase-history", "ORDER", order.orderId());
                    }
                }
            }
            return null;
        });
    }

    private static List<Long> listReceiptRemindersDue() {
        return queryIds("SELECT shipment_id FROM shipments WHERE carrier_provider='GHN'"
                + " AND receipt_status='WAITING' AND carrier_delivery_verified_at IS NOT NULL"
                + " AND receipt_reminder_sent_at IS NULL AND receipt_deadline_at>NOW()"
                + " AND receipt_deadline_at<=DATE_ADD(NOW(),INTERVAL 24 HOUR)"
                + " ORDER BY receipt_deadline_at LIMIT " + AUTOMATION_BATCH_SIZE);
    }

    private static List<Long> listAutoConfirmationsDue() {
        return queryIds("SELECT shipment_id FROM shipments WHERE carrier_provider='GHN'"
                + " AND receipt_status='WAITING' AND carrier_delivery_verified_at IS NOT NULL"
                + " AND receipt_deadline_at<=NOW() ORDER BY receipt_deadline_at LIMIT " + AUTOMATION_BATCH_SIZE);
    }

    private static List<PollTarget> listPollTargets() {
        List<PollTarget> result = new ArrayList<>();
        String sql = "SELECT shipment_id,tracking_number FROM shipments"
                + " WHERE carrier_provider='GHN' AND tracking_number IS NOT NULL AND tracking_number<>''"
                + " AND UPPER(TRIM(COALESCE(status,''))) NOT IN ('DELIVERED','COMPLETED','ĐÃ GIAO','RETURNED','ĐÃ TRẢ','ĐÃ HOÀN')"
                + " AND (last_carrier_sync_at IS NULL OR last_carrier_sync_at<=DATE_SUB(NOW(),INTERVAL 15 MINUTE))"
                + " ORDER BY COALESCE(last_carrier_sync_at,'2000-01-01') LIMIT " + POLL_BATCH_SIZE;
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(new PollTarget(rs.getLong("shipment_id"), rs.getString("tracking_number")));
        } catch (SQLException error) { throw DataAccessException.of(error); }
        return result;
    }

    private static void markPollAttempt(long shipmentId) {
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(
                "UPDATE shipments SET last_carrier_sync_at=NOW() WHERE shipment_id=? AND carrier_provider='GHN'")) {
            ps.setLong(1, shipmentId);
            ps.executeUpdate();
        } catch (SQLException error) { throw DataAccessException.of(error); }
    }

    private static Long findShipmentId(Connection conn, String orderCode) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT shipment_id FROM shipments WHERE carrier_provider='GHN' AND tracking_number=?"
                + " ORDER BY shipment_id LIMIT 1")) {
            ps.setString(1, orderCode);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getLong(1) : null; }
        }
    }

    private static void applyGhnStatus(Connection conn, long eventId, long shipmentId, String orderCode,
                                       String eventType, String rawStatus, LocalDateTime eventTime,
                                       String reason, String podUrl, String source) throws SQLException {
        long orderId;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT order_id FROM shipments WHERE shipment_id=? AND carrier_provider='GHN' AND tracking_number=?")) {
            ps.setLong(1, shipmentId);
            ps.setString(2, orderCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    markEvent(conn, eventId, null, "IGNORED", "Vận đơn không còn khớp mã GHN.");
                    return;
                }
                orderId = rs.getLong(1);
            }
        }
        OrderContext order = lockOrder(conn, orderId);
        String currentStatus;
        String currentCarrierStatus;
        LocalDateTime lastStatusAt;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT status,carrier_status_code,carrier_status_updated_at FROM shipments WHERE shipment_id=? AND order_id=? FOR UPDATE")) {
            ps.setLong(1, shipmentId);
            ps.setLong(2, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    markEvent(conn, eventId, shipmentId, "IGNORED", "Vận đơn không còn tồn tại.");
                    return;
                }
                currentStatus = ShipmentPolicy.normalize(rs.getString("status"));
                currentCarrierStatus = rs.getString("carrier_status_code");
                Timestamp value = rs.getTimestamp("carrier_status_updated_at");
                lastStatusAt = value == null ? null : value.toLocalDateTime();
            }
        }
        if (lastStatusAt != null && !eventTime.isAfter(lastStatusAt)) {
            markEvent(conn, eventId, shipmentId, "IGNORED", "Bỏ qua sự kiện GHN cũ hơn hoặc trùng thời điểm đã đồng bộ.");
            return;
        }
        String nextStatus = GhnTrackingPolicy.mapStatus(rawStatus);
        if (GhnTrackingPolicy.isFinalRawStatus(currentCarrierStatus)
                && !currentCarrierStatus.equalsIgnoreCase(rawStatus)) {
            markEvent(conn, eventId, shipmentId, "IGNORED",
                    "GHN đã ghi nhận trạng thái kết thúc " + currentCarrierStatus + "; không nhận trạng thái mở lại.");
            return;
        }
        if (nextStatus == null || !GhnTrackingPolicy.canApplyCarrierStatus(currentStatus, nextStatus)) {
            markEvent(conn, eventId, shipmentId, "IGNORED",
                    "Không áp dụng chuyển trạng thái vận đơn " + currentStatus + " → " + nextStatus + ".");
            return;
        }
        // Allow authenticated GHN proof to enrich a manually declared DELIVERED shipment,
        // but never reopen terminal shipments or reset a customer's CONFIRMED receipt.
        if (GhnTrackingPolicy.isTerminal(currentStatus)
                && !("DELIVERED".equals(currentStatus) && "DELIVERED".equals(nextStatus))) {
            markEvent(conn, eventId, shipmentId, "IGNORED", "Vận đơn đã ở trạng thái kết thúc.");
            return;
        }

        boolean delivered = "DELIVERED".equals(nextStatus);
        LocalDateTime deadline = delivered ? eventTime.plus(GhnTrackingPolicy.CUSTOMER_CONFIRMATION_WINDOW) : null;
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE shipments SET status=?,carrier_status_code=?,carrier_status_updated_at=?,last_carrier_sync_at=NOW(),"
                + "shipped_at=CASE WHEN ? IN ('SHIPPED','IN_TRANSIT','DELIVERED') THEN COALESCE(shipped_at,?) ELSE shipped_at END,"
                + "delivered_at=CASE WHEN ?='DELIVERED' THEN COALESCE(delivered_at,?) ELSE delivered_at END,"
                + "receipt_status=CASE WHEN ?='DELIVERED' AND COALESCE(receipt_status,'NOT_READY') IN ('NOT_READY','WAITING') THEN 'WAITING' ELSE receipt_status END,"
                + "carrier_delivery_verified_at=CASE WHEN ?='DELIVERED' THEN COALESCE(carrier_delivery_verified_at,?) ELSE carrier_delivery_verified_at END,"
                + "receipt_deadline_at=CASE WHEN ?='DELIVERED' AND COALESCE(receipt_status,'NOT_READY') IN ('NOT_READY','WAITING') THEN COALESCE(receipt_deadline_at,?) ELSE receipt_deadline_at END,"
                + "carrier_pod_url=CASE WHEN ?='DELIVERED' AND ? IS NOT NULL THEN ? ELSE carrier_pod_url END"
                + " WHERE shipment_id=?")) {
            int i = 1;
            ps.setString(i++, nextStatus);
            ps.setString(i++, rawStatus);
            ps.setTimestamp(i++, Timestamp.valueOf(eventTime));
            ps.setString(i++, nextStatus); ps.setTimestamp(i++, Timestamp.valueOf(eventTime));
            ps.setString(i++, nextStatus); ps.setTimestamp(i++, Timestamp.valueOf(eventTime));
            ps.setString(i++, nextStatus);
            ps.setString(i++, nextStatus); ps.setTimestamp(i++, Timestamp.valueOf(eventTime));
            ps.setString(i++, nextStatus); ps.setTimestamp(i++, deadline == null ? null : Timestamp.valueOf(deadline));
            ps.setString(i++, nextStatus); ps.setString(i++, podUrl);
            ps.setString(i++, podUrl);
            ps.setLong(i, shipmentId);
            if (ps.executeUpdate() != 1) throw new IllegalArgumentException("Không cập nhật được trạng thái GHN.");
        }
        updateOrderShippingStatus(conn, orderId);
        String title = delivered
                ? "GHN xác nhận giao thành công — chờ khách xác nhận"
                : "GHN cập nhật vận đơn: " + rawStatus;
        String description = "Nguồn: GHN " + source + " · Trạng thái: " + rawStatus
                + (reason == null || reason.isBlank() ? "" : " · Lý do: " + reason)
                + (delivered ? " · Hạn xác nhận nhận hàng: " + deadline : "");
        insertShipmentEvent(conn, orderId, shipmentId, title, description, eventTime);
        if (order != null && (currentStatus != null && !currentStatus.equals(nextStatus))) {
            NotificationService.notifyUser(conn, order.customerId(), "SHIPMENT_UPDATED",
                    delivered ? "Hãng vận chuyển báo đã giao hàng" : "Hãng vận chuyển cập nhật đơn hàng",
                    delivered
                            ? "GHN đã xác nhận giao kiện hàng của đơn " + order.orderCode()
                                    + ". Vui lòng kiểm tra hàng và xác nhận trong 72 giờ; nếu có vấn đề hãy gửi khiếu nại."
                            : "Vận đơn " + orderCode + " của đơn " + order.orderCode()
                                    + " vừa cập nhật: " + rawStatus
                                    + (reason == null || reason.isBlank() ? "." : " · " + reason),
                    "/order-tracking?code=" + order.orderCode(), "SHIPMENT", shipmentId);
        }
        markEvent(conn, eventId, shipmentId, "PROCESSED", null);
    }

    private static void updateOrderShippingStatus(Connection conn, long orderId) throws SQLException {
        long total;
        long delivered;
        long failed;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) total,"
                + "COALESCE(SUM(CASE WHEN UPPER(TRIM(COALESCE(status,''))) IN ('DELIVERED','COMPLETED','ĐÃ GIAO') THEN 1 ELSE 0 END),0) delivered,"
                + "COALESCE(SUM(CASE WHEN UPPER(TRIM(COALESCE(status,''))) IN ('FAILED','GIAO THẤT BẠI') THEN 1 ELSE 0 END),0) failed"
                + " FROM shipments WHERE order_id=?")) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                total = rs.getLong("total");
                delivered = rs.getLong("delivered");
                failed = rs.getLong("failed");
            }
        }
        long returned;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM shipments WHERE order_id=?"
                + " AND UPPER(TRIM(COALESCE(status,''))) IN ('RETURNED','ĐÃ TRẢ','ĐÃ HOÀN')")) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) { returned = rs.next() ? rs.getLong(1) : 0; }
        }
        String shipping = total > 0 && delivered == total ? "DELIVERED"
                : returned > 0 ? "RETURNED"
                : failed > 0 ? "FAILED"
                : "IN_TRANSIT";
        try (PreparedStatement ps = conn.prepareStatement("UPDATE orders SET shipping_status=? WHERE order_id=?")) {
            ps.setString(1, shipping); ps.setLong(2, orderId); ps.executeUpdate();
        }
    }

    private static OrderContext lockOrderForShipment(Connection conn, long shipmentId) throws SQLException {
        long orderId;
        try (PreparedStatement ps = conn.prepareStatement("SELECT order_id FROM shipments WHERE shipment_id=?")) {
            ps.setLong(1, shipmentId);
            try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) return null; orderId = rs.getLong(1); }
        }
        return lockOrder(conn, orderId);
    }

    private static OrderContext lockOrder(Connection conn, long orderId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT user_id,order_code,status FROM orders WHERE order_id=? FOR UPDATE")) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new OrderContext(orderId, rs.getLong("user_id"),
                        rs.getString("order_code"), rs.getString("status"));
            }
        }
    }

    private static void insertShipmentEvent(Connection conn, long orderId, long shipmentId,
                                             String title, String description, LocalDateTime when) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO shipment_events(order_id,shipment_id,event_title,description,occurred_at)"
                + " VALUES(?,?,?,?,?)")) {
            ps.setLong(1, orderId);
            ps.setLong(2, shipmentId);
            ps.setString(3, clip(title, 180));
            ps.setString(4, clip(description, 1000));
            ps.setTimestamp(5, Timestamp.valueOf(when));
            ps.executeUpdate();
        }
    }

    private static void markEvent(Connection conn, long eventId, Long shipmentId,
                                  String status, String error) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE carrier_webhook_events SET shipment_id=?,processing_status=?,error_message=?,processed_at=NOW()"
                + " WHERE event_id=?")) {
            if (shipmentId == null) ps.setNull(1, java.sql.Types.BIGINT); else ps.setLong(1, shipmentId);
            ps.setString(2, status);
            ps.setString(3, clip(error, 500));
            ps.setLong(4, eventId);
            ps.executeUpdate();
        }
    }

    private static WebhookResult eventOutcome(Connection conn, long eventId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT processing_status FROM carrier_webhook_events WHERE event_id=?")) {
            ps.setLong(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return WebhookResult.IGNORED;
                return "PROCESSED".equals(rs.getString(1)) ? WebhookResult.PROCESSED : WebhookResult.IGNORED;
            }
        }
    }

    private static List<Long> queryIds(String sql) {
        List<Long> ids = new ArrayList<>();
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) ids.add(rs.getLong(1));
            return ids;
        } catch (SQLException error) { throw DataAccessException.of(error); }
    }

    private static <T> T inTransaction(SqlWork<T> work) {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                T value = work.run(conn);
                conn.commit();
                return value;
            } catch (SQLException | RuntimeException error) {
                try { conn.rollback(); } catch (SQLException rollback) { error.addSuppressed(rollback); }
                throw error;
            } finally { conn.setAutoCommit(true); }
        } catch (SQLException error) { throw DataAccessException.of(error); }
    }

    private static LocalDateTime parseEventTime(String raw) {
        try { return OffsetDateTime.parse(raw).atZoneSameInstant(APP_ZONE).toLocalDateTime(); }
        catch (DateTimeParseException ignored) {
            try { return Instant.parse(raw).atZone(APP_ZONE).toLocalDateTime(); }
            catch (DateTimeParseException second) {
                try { return LocalDateTime.parse(raw); }
                catch (DateTimeParseException third) {
                    throw new IllegalArgumentException("Thời điểm webhook GHN không đúng ISO-8601.");
                }
            }
        }
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) { throw new IllegalStateException("SHA-256 unavailable", error); }
    }

    private static String safePodUrl(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) return null;
        if (value.length() > 1000) return null;
        try {
            java.net.URI uri = java.net.URI.create(value);
            String scheme = uri.getScheme();
            return ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)) && uri.getHost() != null
                    ? value : null;
        } catch (IllegalArgumentException ignored) { return null; }
    }

    private static String required(String raw, int max, String label) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty() || value.length() > max) throw new IllegalArgumentException(label + " không hợp lệ.");
        return value;
    }

    private static String bounded(String raw, int max) {
        if (raw == null) return null;
        String value = raw.trim();
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static String clip(String raw, int max) {
        if (raw == null) return null;
        return raw.length() <= max ? raw : raw.substring(0, max);
    }

    /** Keep operational fields needed for reconciliation; do not persist recipient contact/address data. */
    private static String sanitizePayload(String rawPayload) {
        if (rawPayload == null || rawPayload.isBlank()) return "{}";
        try {
            JsonObject source = JsonParser.parseString(rawPayload).getAsJsonObject();
            JsonObject safe = new JsonObject();
            for (String key : List.of("ShopID", "Time", "OrderCode", "ClientOrderCode", "Type",
                    "Description", "Status", "Reason", "ReasonCode", "CODAmount", "CODTransferDate",
                    "IsPartialReturn", "PartialReturnCode", "PaymentType", "TotalFee", "Fee", "PodURL")) {
                JsonElement value = source.get(key);
                if (value != null && !value.isJsonNull()) safe.add(key, value.deepCopy());
            }
            return safe.toString();
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("Webhook GHN không chứa JSON object hợp lệ.");
        }
    }

    private static String jsonEscape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", " ").replace("\r", " ");
    }

    private record PollTarget(long shipmentId, String trackingNumber) { }
    private record OrderContext(long orderId, long customerId, String orderCode, String orderStatus) { }

    @FunctionalInterface
    private interface SqlWork<T> { T run(Connection conn) throws SQLException; }
}
