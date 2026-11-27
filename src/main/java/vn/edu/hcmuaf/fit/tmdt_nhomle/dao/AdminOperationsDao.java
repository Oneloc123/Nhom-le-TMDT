package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.ManualRefundPolicy;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.FinancialAllocationService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.NotificationService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.SellerVerificationPolicy;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.ShipmentPolicy;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.GhnTrackingPolicy;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Admin operational updates that must be committed with their audit trail and notifications. */
public final class AdminOperationsDao {
    private static final int MAX_ROWS = 300;

    private AdminOperationsDao() { }

    public static List<Map<String, Object>> listShipments(String rawQuery, String rawStatus) {
        StringBuilder sql = new StringBuilder(
                "SELECT sh.shipment_id id,sh.order_id orderId,o.order_code orderCode,o.user_id customerId,"
              + "u.full_name customerName,u.email customerEmail,sh.shop_id shopId,s.shop_name shopName,"
              + "sh.carrier,sh.carrier_provider carrierProvider,sh.carrier_status_code carrierStatusCode,sh.tracking_number trackingNumber,sh.status storedStatus,"
              + "sh.carrier_status_updated_at carrierStatusUpdatedAt,sh.receipt_deadline_at receiptDeadlineAt,"
              + "sh.receipt_auto_confirmed_at receiptAutoConfirmedAt,"
              + "sh.shipped_at shippedAt,sh.delivered_at deliveredAt,sh.receipt_status receiptStatus,o.shipping_status orderShippingStatus,"
              + "sh.status lastStatus"
              + " FROM shipments sh JOIN orders o ON o.order_id=sh.order_id"
              + " LEFT JOIN users u ON u.user_id=o.user_id LEFT JOIN print_shops s ON s.shop_id=sh.shop_id WHERE 1=1");
        List<Object> args = new ArrayList<>();
        String status = rawStatus == null ? "" : rawStatus.trim().toUpperCase(Locale.ROOT);
        if (status.equals("OPEN")) {
            sql.append(" AND UPPER(COALESCE(sh.status,'')) NOT IN"
                    + " ('DELIVERED','COMPLETED','CANCELLED','RETURNED','ĐÃ GIAO','ĐÃ HỦY','ĐÃ TRẢ','ĐÃ HOÀN')");
        } else if (!status.isBlank() && !status.equals("ALL")) {
            Set<String> values = storedShipmentStatuses(status);
            if (values.isEmpty()) throw new IllegalArgumentException("Trạng thái vận đơn không hợp lệ.");
            sql.append(" AND UPPER(COALESCE(sh.status,'')) IN (")
                    .append(String.join(",", java.util.Collections.nCopies(values.size(), "?"))).append(")");
            args.addAll(values);
        }
        String query = boundedFilter(rawQuery, 160, "Từ khóa");
        if (query != null) {
            String like = "%" + query + "%";
            sql.append(" AND (CAST(sh.shipment_id AS CHAR) LIKE ? OR o.order_code LIKE ?"
                    + " OR sh.tracking_number LIKE ? OR sh.carrier LIKE ? OR u.full_name LIKE ?"
                    + " OR u.email LIKE ? OR s.shop_name LIKE ?)");
            for (int i = 0; i < 7; i++) args.add(like);
        }
        sql.append(" ORDER BY CASE WHEN UPPER(COALESCE(sh.status,'')) IN ('REFUND_REQUIRED','FAILED') THEN 0"
                + " WHEN sh.tracking_number IS NULL OR sh.tracking_number='' THEN 1 ELSE 2 END,"
                + " sh.shipped_at IS NULL,sh.shipped_at DESC,sh.shipment_id DESC LIMIT ").append(MAX_ROWS);
        List<Map<String,Object>> rows = rows(sql.toString(), args.toArray());
        for (Map<String,Object> row : rows) normalizeShipmentRow(row);
        return rows;
    }

    public static Map<String,Object> shipmentDetail(long shipmentId) {
        Map<String,Object> item = one(
                "SELECT sh.shipment_id id,sh.order_id orderId,o.order_code orderCode,o.user_id customerId,"
              + "u.full_name customerName,u.email customerEmail,sh.shop_id shopId,s.shop_name shopName,"
              + "sh.carrier,sh.carrier_provider carrierProvider,sh.carrier_status_code carrierStatusCode,sh.tracking_number trackingNumber,sh.status storedStatus,"
              + "sh.shipped_at shippedAt,sh.delivered_at deliveredAt,sh.receipt_status receiptStatus,o.shipping_status orderShippingStatus"
              + " FROM shipments sh JOIN orders o ON o.order_id=sh.order_id"
              + " LEFT JOIN users u ON u.user_id=o.user_id LEFT JOIN print_shops s ON s.shop_id=sh.shop_id"
              + " WHERE sh.shipment_id=?", shipmentId);
        if (item == null) return null;
        normalizeShipmentRow(item);
        item.put("events", rows("SELECT shipment_event_id id,event_title title,description note,occurred_at createdAt"
                + " FROM shipment_events WHERE order_id=? ORDER BY occurred_at,shipment_event_id LIMIT 100",
                item.get("orderId")));
        return item;
    }

    public static long countOpenShipments() {
        Map<String,Object> result = one("SELECT COUNT(*) total FROM shipments WHERE"
                + " UPPER(COALESCE(status,'')) NOT IN ('DELIVERED','COMPLETED','CANCELLED','RETURNED',"
                + "'ĐÃ GIAO','ĐÃ HỦY','ĐÃ TRẢ','ĐÃ HOÀN')");
        Object value = result == null ? 0L : result.get("total");
        return value instanceof Number n ? n.longValue() : 0L;
    }

    public static void updateShipment(long shipmentId, long adminId, String rawCarrier, String rawTracking,
                                      String rawStatus, String rawNote) {
        String carrier = boundedRequired(rawCarrier, 100, "Hãng vận chuyển");
        String tracking = boundedFilter(rawTracking, 120, "Mã vận đơn");
        String status = ShipmentPolicy.normalize(rawStatus);
        String note = boundedFilter(rawNote, 500, "Ghi chú");
        if (ShipmentPolicy.requiresTracking(status) && (carrier == null || tracking == null)) {
            throw new IllegalArgumentException("Cần nhập hãng vận chuyển và mã vận đơn trước khi giao/đang giao/đã giao.");
        }

        inTransaction(conn -> {
            long orderId;
            try (PreparedStatement ps = conn.prepareStatement("SELECT order_id FROM shipments WHERE shipment_id=?")) {
                ps.setLong(1, shipmentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy vận đơn.");
                    orderId = rs.getLong(1);
                }
            }
            long customerId;
            String orderCode;
            try (PreparedStatement ps = conn.prepareStatement("SELECT user_id,order_code FROM orders WHERE order_id=? FOR UPDATE")) {
                ps.setLong(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy đơn hàng của vận đơn.");
                    customerId = rs.getLong("user_id");
                    orderCode = rs.getString("order_code");
                }
            }
            String oldStoredStatus;
            String oldCarrier;
            String oldTracking;
            String oldProvider;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT status,carrier,tracking_number,carrier_provider FROM shipments WHERE shipment_id=? AND order_id=? FOR UPDATE")) {
                ps.setLong(1, shipmentId); ps.setLong(2, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new IllegalArgumentException("Vận đơn đã thay đổi hoặc không còn tồn tại.");
                    oldStoredStatus = rs.getString("status");
                    oldCarrier = rs.getString("carrier");
                    oldTracking = rs.getString("tracking_number");
                    oldProvider = rs.getString("carrier_provider");
                }
            }
            String oldStatus = ShipmentPolicy.normalize(oldStoredStatus);
            String provider = GhnTrackingPolicy.providerForCarrierLabel(carrier);
            if (!ShipmentPolicy.canTransition(oldStatus, status)) {
                throw new IllegalArgumentException("Không thể chuyển vận đơn từ " + oldStatus + " sang " + status + ".");
            }
            if (oldStatus.equals(status)
                    && java.util.Objects.equals(blankToNull(oldCarrier), carrier)
                    && java.util.Objects.equals(blankToNull(oldTracking), tracking)
                    && java.util.Objects.equals(oldProvider, provider)
                    && (note == null || note.isBlank())) {
                throw new IllegalArgumentException("Không có thay đổi nào để lưu.");
            }

            
            boolean resetCarrierState = !java.util.Objects.equals(oldProvider, provider)
                    || !java.util.Objects.equals(blankToNull(oldTracking), tracking);
            if (ShipmentPolicy.isTerminal(oldStatus) && resetCarrierState) {
                throw new IllegalArgumentException("Không thể thay đổi hãng hoặc mã vận đơn sau khi vận đơn đã kết thúc.");
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE shipments SET carrier=?,tracking_number=?,status=?,"
                  + "carrier_status_code=CASE WHEN ?=1 THEN NULL ELSE carrier_status_code END,"
                  + "carrier_status_updated_at=CASE WHEN ?=1 THEN NULL ELSE carrier_status_updated_at END,"
                  + "last_carrier_sync_at=CASE WHEN ?=1 THEN NULL ELSE last_carrier_sync_at END,"
                  + "carrier_delivery_verified_at=CASE WHEN ?=1 THEN NULL ELSE carrier_delivery_verified_at END,"
                  + "carrier_pod_url=CASE WHEN ?=1 THEN NULL ELSE carrier_pod_url END,"
                  + "receipt_deadline_at=CASE WHEN ?=1 THEN NULL ELSE receipt_deadline_at END,"
                  + "receipt_reminder_sent_at=CASE WHEN ?=1 THEN NULL ELSE receipt_reminder_sent_at END,"
                  + "carrier_provider=?,"
                  + "shipped_at=CASE WHEN ? IN ('SHIPPED','IN_TRANSIT','DELIVERED') THEN COALESCE(shipped_at,NOW()) ELSE shipped_at END,"
                  + "delivered_at=CASE WHEN ?='DELIVERED' THEN COALESCE(delivered_at,NOW())"
                  + " WHEN ? IN ('PREPARING','SHIPPED','IN_TRANSIT','FAILED') THEN NULL ELSE delivered_at END,"
                  + "receipt_status=CASE WHEN ?='DELIVERED' AND ?<>'DELIVERED' THEN 'WAITING' ELSE receipt_status END"
                  + " WHERE shipment_id=?")) {
                ps.setString(1, carrier);
                if (tracking == null) ps.setNull(2, java.sql.Types.VARCHAR); else ps.setString(2, tracking);
                ps.setString(3, status);
                for (int i = 4; i <= 10; i++) ps.setInt(i, resetCarrierState ? 1 : 0);
                ps.setString(11, provider);
                ps.setString(12, status);
                ps.setString(13, status);
                ps.setString(14, status);
                ps.setString(15, status);
                ps.setString(16, oldStatus);
                ps.setLong(17, shipmentId);
                if (ps.executeUpdate() != 1) throw new IllegalArgumentException("Vận đơn đã thay đổi ở nơi khác.");
            }

            List<String> statuses = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement("SELECT status FROM shipments WHERE order_id=?")) {
                ps.setLong(1, orderId);
                try (ResultSet rs = ps.executeQuery()) { while (rs.next()) statuses.add(ShipmentPolicy.normalize(rs.getString(1))); }
            }
            String orderShippingStatus = aggregateShippingStatus(statuses);
            try (PreparedStatement ps = conn.prepareStatement("UPDATE orders SET shipping_status=? WHERE order_id=?")) {
                ps.setString(1, orderShippingStatus); ps.setLong(2, orderId); ps.executeUpdate();
            }

            String description = "Trạng thái: " + oldStatus + " → " + status
                    + "; hãng: " + safe(carrier) + "; mã vận đơn: " + safe(tracking)
                    + (note == null ? "" : "; ghi chú: " + note);
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO shipment_events(order_id,shipment_id,event_title,description,occurred_at)"
                  + " VALUES(?,?,?,?,NOW())")) {
                ps.setLong(1, orderId); ps.setLong(2, shipmentId);
                ps.setString(3, "Cập nhật vận đơn #" + shipmentId + " — " + status);
                ps.setString(4, clip(description, 1000)); ps.executeUpdate();
            }
            if (customerId > 0) {
                NotificationService.notifyUser(conn, customerId, "SHIPMENT_UPDATED",
                        "Cập nhật vận đơn",
                        "Đơn " + safe(orderCode) + " có cập nhật vận chuyển: " + status
                                + (tracking == null ? "" : " · Mã vận đơn " + tracking) + ".",
                        "/purchase-history", "SHIPMENT", shipmentId);
            }
            return null;
        });
    }

    public static void decideSellerVerification(long shopId, long adminId, String rawAction, String rawReason) {
        inTransaction(conn -> {
            String current;
            Long ownerId;
            String shopName;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT verification_status,owner_user_id,shop_name FROM print_shops WHERE shop_id=? FOR UPDATE")) {
                ps.setLong(1, shopId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy xưởng in.");
                    current = rs.getString("verification_status");
                    long rawOwner = rs.getLong("owner_user_id");
                    ownerId = rs.wasNull() ? null : rawOwner;
                    shopName = rs.getString("shop_name");
                }
            }
            SellerVerificationPolicy.Decision decision =
                    SellerVerificationPolicy.decide(current, rawAction, rawReason);
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO seller_verification_events(shop_id,admin_user_id,action,from_status,to_status,reason,created_at)"
                  + " VALUES(?,?,?,?,?,?,NOW())")) {
                ps.setLong(1, shopId); ps.setLong(2, adminId);
                ps.setString(3, decision.action()); ps.setString(4, current);
                ps.setString(5, decision.targetStatus()); ps.setString(6, decision.reason()); ps.executeUpdate();
            }
            String shopStatus = SellerVerificationPolicy.shopStatusFor(decision.action());
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE print_shops SET verification_status=?, status=?"
                  + " WHERE shop_id=? AND verification_status='PENDING'")) {
                ps.setString(1, decision.targetStatus());
                ps.setString(2, shopStatus);
                ps.setLong(3, shopId);
                if (ps.executeUpdate() != 1) {
                    throw new IllegalArgumentException("Trạng thái xác minh đã thay đổi; hãy tải lại trang.");
                }
            }
            if (ownerId != null) {
                String title = switch (decision.action()) {
                    case "APPROVE" -> "Xưởng đã được xác minh";
                    case "REJECT" -> "Hồ sơ xưởng cần được cập nhật";
                    default -> "Cần bổ sung thông tin xác minh";
                };
                String message = "Xưởng " + safe(shopName) + ": " + decision.reason();
                NotificationService.notifyUser(conn, ownerId, "SELLER_VERIFICATION", title, message,
                        "/workshop/profile", "SELLER_VERIFICATION", shopId);
            }
            return null;
        });
    }

    public static List<Map<String,Object>> sellerVerificationHistory(long shopId) {
        if (!tableExists("seller_verification_events")) return List.of();
        return rows("SELECT e.event_id id,e.action,e.from_status fromStatus,e.to_status toStatus,e.reason note,"
                + "e.created_at createdAt,u.full_name adminName FROM seller_verification_events e"
                + " LEFT JOIN users u ON u.user_id=e.admin_user_id WHERE e.shop_id=?"
                + " ORDER BY e.created_at DESC,e.event_id DESC LIMIT 50", shopId);
    }

    /** Records a failed external attempt but leaves the refund obligation open for retry. */
    public static void recordManualRefundFailure(long paymentId, long adminId, String rawMethod,
                                                 String rawReference, String rawNote) {
        String method = rawMethod == null ? "" : rawMethod.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("ORIGINAL_PAYMENT","BANK_TRANSFER","COD_MANUAL","CASH","OTHER").contains(method)) {
            throw new IllegalArgumentException("Phương thức hoàn tiền thủ công không hợp lệ.");
        }
        String reference = boundedFilter(rawReference, 120, "Mã tham chiếu");
        String note = boundedRequired(rawNote, 1000, "Lý do hoàn tiền thất bại");
        if (note.length() < 3) throw new IllegalArgumentException("Lý do hoàn tiền thất bại phải có ít nhất 3 ký tự.");
        inTransaction(conn -> {
            long orderId;
            long customerId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT p.order_id,o.user_id FROM payments p JOIN orders o ON o.order_id=p.order_id WHERE p.payment_id=?")) {
                ps.setLong(1, paymentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy giao dịch thanh toán.");
                    orderId = rs.getLong("order_id"); customerId = rs.getLong("user_id");
                }
            }
            try (PreparedStatement ps = conn.prepareStatement("SELECT order_id FROM orders WHERE order_id=? FOR UPDATE")) {
                ps.setLong(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy đơn hàng của giao dịch.");
                }
            }
            String orderCode;
            String paymentStatus;
            BigDecimal amount;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT p.status,p.amount,o.order_code FROM payments p"
                  + " JOIN orders o ON o.order_id=p.order_id WHERE p.payment_id=? FOR UPDATE")) {
                ps.setLong(1, paymentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new IllegalArgumentException("Giao dịch đã thay đổi hoặc không còn tồn tại.");
                    orderCode = rs.getString("order_code"); paymentStatus = rs.getString("status");
                    amount = rs.getBigDecimal("amount");
                }
            }
            if (!ManualRefundPolicy.shouldMarkPaymentRefunded(paymentStatus)) {
                throw new IllegalArgumentException("Chỉ giao dịch có trạng thái REFUND_REQUIRED mới được ghi nhận lần thử hoàn tiền.");
            }
            if (amount == null || amount.signum() <= 0) {
                throw new IllegalArgumentException("Giao dịch không có số tiền hợp lệ để hoàn.");
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO admin_manual_refunds(payment_id,order_id,admin_user_id,amount,method,reference,note,status,recorded_at)"
                  + " VALUES(?,?,?,?,?,?,?,'FAILED',NOW())")) {
                ps.setLong(1, paymentId); ps.setLong(2, orderId); ps.setLong(3, adminId);
                ps.setBigDecimal(4, amount); ps.setString(5, method);
                if (reference == null) ps.setNull(6, java.sql.Types.VARCHAR); else ps.setString(6, reference);
                ps.setString(7, note); ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO admin_payment_reconciliation_events(payment_id,admin_user_id,outcome,note,provider_reference,created_at)"
                  + " VALUES(?,?,'FOLLOW_UP',?,?,NOW())")) {
                ps.setLong(1, paymentId); ps.setLong(2, adminId); ps.setString(3, note);
                ps.setString(4, reference); ps.executeUpdate();
            }
            if (customerId > 0) {
                NotificationService.notifyUser(conn, customerId, "MANUAL_REFUND_FAILED",
                        "Hoàn tiền chưa hoàn tất",
                        "Lần thử hoàn tiền cho đơn " + safe(orderCode) + " chưa thành công. Chúng tôi sẽ tiếp tục xử lý.",
                        "/purchase-history", "PAYMENT", paymentId);
            }
            return null;
        });
    }

    public static List<Map<String,Object>> manualRefundsForPayment(long paymentId) {
        if (!tableExists("admin_manual_refunds")) return List.of();
        return rows("SELECT r.refund_id id,r.amount,r.method,r.reference,r.note,r.status,r.recorded_at recordedAt,"
                + "u.full_name adminName FROM admin_manual_refunds r LEFT JOIN users u ON u.user_id=r.admin_user_id"
                + " WHERE r.payment_id=? ORDER BY r.recorded_at DESC,r.refund_id DESC LIMIT 50", paymentId);
    }

    /** Records a full manual refund only after the operator confirms the external transfer succeeded. */
    public static void completeManualRefund(long paymentId, long adminId, String method,
                                            String reference, String note) {
        // The amount is deliberately loaded and validated while the payment row is locked.
        inTransaction(conn -> {
            long orderId;
            long customerId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT p.order_id,o.user_id FROM payments p JOIN orders o ON o.order_id=p.order_id WHERE p.payment_id=?")) {
                ps.setLong(1, paymentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy giao dịch thanh toán.");
                    orderId = rs.getLong("order_id"); customerId = rs.getLong("user_id");
                }
            }
            try (PreparedStatement ps = conn.prepareStatement("SELECT order_id FROM orders WHERE order_id=? FOR UPDATE")) {
                ps.setLong(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy đơn hàng của giao dịch.");
                }
            }
            String orderCode;
            String paymentStatus;
            BigDecimal amount;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT p.status,p.amount,o.order_code FROM payments p"
                  + " JOIN orders o ON o.order_id=p.order_id WHERE p.payment_id=? FOR UPDATE")) {
                ps.setLong(1, paymentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new IllegalArgumentException("Giao dịch đã thay đổi hoặc không còn tồn tại.");
                    orderCode = rs.getString("order_code"); paymentStatus = rs.getString("status");
                    amount = rs.getBigDecimal("amount");
                }
            }
            if (!ManualRefundPolicy.shouldMarkPaymentRefunded(paymentStatus)) {
                throw new IllegalArgumentException("Chỉ giao dịch có trạng thái REFUND_REQUIRED mới được hoàn tiền thủ công.");
            }
            ManualRefundPolicy.Completion completion =
                    ManualRefundPolicy.validateCompletion(method, reference, note, amount);
            if (oneInTransaction(conn,
                    "SELECT refund_id FROM admin_manual_refunds WHERE payment_id=? AND status='SUCCESS' LIMIT 1",
                    paymentId) != null) {
                throw new IllegalArgumentException("Giao dịch này đã được ghi nhận hoàn tiền thành công.");
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO admin_manual_refunds(payment_id,order_id,admin_user_id,amount,method,reference,note,status,recorded_at)"
                  + " VALUES(?,?,?,?,?,?,?,'SUCCESS',NOW())")) {
                ps.setLong(1, paymentId); ps.setLong(2, orderId); ps.setLong(3, adminId);
                ps.setBigDecimal(4, completion.amount()); ps.setString(5, completion.method());
                ps.setString(6, completion.reference()); ps.setString(7, completion.note()); ps.executeUpdate();
            }
            FinancialAllocationService.recordOrderRefund(conn, orderId, completion.amount(),
                    "MANUAL_PAYMENT_REFUND", paymentId, adminId);
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE payments SET status='REFUNDED',failure_reason=NULL,updated_at=NOW()"
                  + " WHERE payment_id=? AND status='REFUND_REQUIRED'")) {
                ps.setLong(1, paymentId);
                if (ps.executeUpdate() != 1) throw new IllegalArgumentException("Giao dịch đã thay đổi; không thể ghi nhận hoàn tiền.");
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE orders o SET payment_status=CASE"
                  + " WHEN o.status='CANCELLED' AND NOT EXISTS (SELECT 1 FROM payments p2 WHERE p2.order_id=o.order_id"
                  + " AND UPPER(COALESCE(p2.status,'')) NOT IN ('REFUNDED','CANCELLED','FAILED','SUPERSEDED')) THEN 'REFUNDED'"
                  + " ELSE o.payment_status END WHERE o.order_id=?")) {
                ps.setLong(1, orderId); ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO admin_payment_reconciliation_events(payment_id,admin_user_id,outcome,note,provider_reference,created_at)"
                  + " VALUES(?,?,'RECONCILED',?,?,NOW())")) {
                ps.setLong(1, paymentId); ps.setLong(2, adminId);
                ps.setString(3, "Đã xác nhận chuyển hoàn thủ công thành công. " + completion.note());
                ps.setString(4, completion.reference()); ps.executeUpdate();
            }
            if (customerId > 0) {
                NotificationService.notifyUser(conn, customerId, "MANUAL_REFUND_COMPLETED",
                        "Đã ghi nhận hoàn tiền",
                        "Khoản hoàn tiền cho đơn " + safe(orderCode) + " đã được xác nhận hoàn tất.",
                        "/purchase-history", "PAYMENT", paymentId);
            }
            return null;
        });
    }

    private static Set<String> storedShipmentStatuses(String normalized) {
        return switch (normalized) {
            case "PREPARING" -> Set.of("PREPARING", "READY", "PENDING", "ĐANG CHUẨN BỊ", "CHỜ LẤY HÀNG");
            case "SHIPPED" -> Set.of("SHIPPED", "ĐÃ GỬI", "ĐÃ LẤY HÀNG");
            case "IN_TRANSIT" -> Set.of("IN_TRANSIT", "ĐANG GIAO", "ĐANG VẬN CHUYỂN");
            case "DELIVERED" -> Set.of("DELIVERED", "COMPLETED", "ĐÃ GIAO");
            case "FAILED" -> Set.of("FAILED", "DELIVERY_FAILED", "GIAO THẤT BẠI");
            case "RETURNED" -> Set.of("RETURNED", "CANCELLED", "ĐÃ HOÀN", "ĐÃ TRẢ", "ĐÃ HỦY");
            default -> Set.of();
        };
    }

    private static void normalizeShipmentRow(Map<String,Object> row) {
        Object raw = row.remove("storedStatus");
        if (raw == null) raw = row.get("status");
        try {
            row.put("status", ShipmentPolicy.normalize(raw == null ? null : String.valueOf(raw)));
        } catch (IllegalArgumentException legacyUnknown) {
            row.put("status", "UNKNOWN");
        }
    }

    private static String aggregateShippingStatus(List<String> statuses) {
        if (statuses.isEmpty()) return "PREPARING";
        if (statuses.stream().allMatch("DELIVERED"::equals)) return "DELIVERED";
        if (statuses.stream().anyMatch("RETURNED"::equals)) return "RETURNED";
        if (statuses.stream().anyMatch("FAILED"::equals)) return "FAILED";
        if (statuses.stream().anyMatch(s -> "SHIPPED".equals(s) || "IN_TRANSIT".equals(s))) return "IN_TRANSIT";
        return "PREPARING";
    }

    private static String boundedRequired(String raw, int max, String label) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty() || value.length() > max) throw new IllegalArgumentException(label + " là bắt buộc, tối đa " + max + " ký tự.");
        return value;
    }

    private static String boundedFilter(String raw, int max, String label) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim();
        if (value.length() > max) throw new IllegalArgumentException(label + " tối đa " + max + " ký tự.");
        return value;
    }

    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private static String safe(String value) { return value == null || value.isBlank() ? "không có dữ liệu" : value; }
    private static String clip(String value, int max) { return value == null ? "" : value.length() <= max ? value : value.substring(0, max); }

    private interface SqlWork<T> { T run(Connection connection) throws SQLException; }

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
            } finally { conn.setAutoCommit(true); }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    private static List<Map<String,Object>> rows(String sql, Object... args) {
        List<Map<String,Object>> data = new ArrayList<>();
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps,args);
            try (ResultSet rs=ps.executeQuery()) {
                ResultSetMetaData meta=rs.getMetaData();
                while(rs.next()) {
                    Map<String,Object> row=new LinkedHashMap<>();
                    for(int i=1;i<=meta.getColumnCount();i++) {
                        Object value=rs.getObject(i);
                        if(value instanceof Timestamp timestamp) value=timestamp.toLocalDateTime().toString().replace('T',' ');
                        row.put(meta.getColumnLabel(i),value);
                    }
                    data.add(row);
                }
            }
            return data;
        } catch(SQLException e) { throw DataAccessException.of(e); }
    }

    private static Map<String,Object> one(String sql,Object...args) {
        List<Map<String,Object>> data=rows(sql,args);
        return data.isEmpty()?null:data.get(0);
    }

    private static Map<String,Object> oneInTransaction(Connection conn,String sql,Object...args)throws SQLException {
        try(PreparedStatement ps=conn.prepareStatement(sql)) {
            bind(ps,args);
            try(ResultSet rs=ps.executeQuery()) {
                if(!rs.next()) return null;
                Map<String,Object> row=new LinkedHashMap<>();
                ResultSetMetaData meta=rs.getMetaData();
                for(int i=1;i<=meta.getColumnCount();i++) row.put(meta.getColumnLabel(i),rs.getObject(i));
                return row;
            }
        }
    }

    private static void bind(PreparedStatement ps,Object...args)throws SQLException {
        for(int i=0;i<args.length;i++) ps.setObject(i+1,args[i]);
    }

    private static boolean tableExists(String tableName) {
        try (Connection conn = Database.getConnection()) {
            try (ResultSet rs = conn.getMetaData().getTables(conn.getCatalog(), null, tableName, new String[]{"TABLE"})) {
                if (rs.next()) return true;
            }
            try (ResultSet rs = conn.getMetaData().getTables(conn.getCatalog(), null,
                    tableName.toUpperCase(Locale.ROOT), new String[]{"TABLE"})) {
                return rs.next();
            }
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }
}
