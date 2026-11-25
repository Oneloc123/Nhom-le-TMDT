package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.NotificationService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.ReturnRefundPolicy;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.FinancialAllocationService;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ReturnRefundDao {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Set<String> NON_RELEASABLE_STATUSES = Set.of(
            "SUBMITTED", "UNDER_REVIEW", "AWAITING_RETURN_SHIPMENT", "RETURN_RECEIVED",
            "REFUND_PROCESSING", "REFUND_FAILED", "REFUNDED");
    private ReturnRefundDao() { }

    public record EvidenceUpload(String storageKey, String originalFilename, String contentType,
                                 long fileSize, String sha256) { }

    /** Customer can request return/refund only for a delivered item within the configured policy window. */
    public static List<Map<String, Object>> listEligibleItems(long userId) {
        String sql = "SELECT o.order_id,o.order_code,o.status order_status,o.shipping_status,"
                + "oi.order_item_id,oi.product_name_snapshot,oi.quantity purchased_quantity,"
                + "oi.unit_price,oi.line_total,oi.design_file_id,COALESCE(p.is_customizable,0) custom_made,"
                + "COALESCE((SELECT MAX(s.delivered_at) FROM shipments s WHERE s.order_id=o.order_id AND oi.shop_id IS NOT NULL AND s.shop_id=oi.shop_id AND s.delivered_at IS NOT NULL),o.completed_at) delivered_at,"
                + "(oi.quantity-COALESCE((SELECT SUM(r.requested_quantity) FROM return_refund_requests r "
                + "WHERE r.order_item_id=oi.order_item_id AND r.status NOT IN ('REJECTED','WITHDRAWN')),0)) available_quantity,"
                + "EXISTS(SELECT 1 FROM shipments sd WHERE sd.order_id=o.order_id AND oi.shop_id IS NOT NULL AND sd.shop_id=oi.shop_id AND sd.delivered_at IS NOT NULL) has_delivery_event "
                + "FROM orders o JOIN order_items oi ON oi.order_id=o.order_id "
                + "LEFT JOIN products p ON p.product_id=oi.product_id "
                + "WHERE o.user_id=? AND (o.status IN ('Đã hoàn thành','COMPLETED') "
                + "OR o.shipping_status IN ('Đã giao','DELIVERED') "
                + "OR EXISTS(SELECT 1 FROM shipments sx WHERE sx.order_id=o.order_id AND oi.shop_id IS NOT NULL AND sx.shop_id=oi.shop_id AND sx.delivered_at IS NOT NULL)) "
                + "ORDER BY COALESCE((SELECT MAX(s2.delivered_at) FROM shipments s2 WHERE s2.order_id=o.order_id AND s2.delivered_at IS NOT NULL),o.completed_at) DESC,oi.order_item_id DESC";
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, userId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    LocalDateTime deliveredAt = dateTime(r.getTimestamp("delivered_at"));
                    int available = r.getInt("available_quantity");
                    if (available <= 0 || !ReturnRefundPolicy.isWithinReturnWindow(deliveredAt, LocalDateTime.now())) continue;
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("orderId", r.getLong("order_id"));
                    row.put("orderCode", r.getString("order_code"));
                    row.put("orderItemId", r.getLong("order_item_id"));
                    row.put("productName", r.getString("product_name_snapshot"));
                    row.put("purchasedQuantity", r.getInt("purchased_quantity"));
                    row.put("availableQuantity", available);
                    row.put("unitPrice", r.getBigDecimal("unit_price"));
                    row.put("lineTotal", r.getBigDecimal("line_total"));
                    row.put("customMade", r.getObject("design_file_id") != null);
                    row.put("deliveredAt", format(deliveredAt));
                    row.put("deadline", format(deliveredAt.plusDays(ReturnRefundPolicy.RETURN_WINDOW_DAYS)));
                    rows.add(row);
                }
            }
            return rows;
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static long createRequest(long userId, long orderItemId, String requestType,
                                     String reasonCode, int quantity, String customerNote,
                                     List<EvidenceUpload> evidence) {
        if (!ReturnRefundPolicy.isValidRequestType(requestType)) throw new IllegalArgumentException("Loại yêu cầu không hợp lệ.");
        if (!ReturnRefundPolicy.isValidReason(reasonCode)) throw new IllegalArgumentException("Lý do đổi trả/hoàn tiền không hợp lệ.");
        String note = clean(customerNote, 2000);
        if (note.length() < 10) throw new IllegalArgumentException("Vui lòng mô tả tình trạng với ít nhất 10 ký tự.");
        List<EvidenceUpload> uploads = evidence == null ? List.of() : List.copyOf(evidence);
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                PurchaseItem item = findPurchaseItem(c, userId, orderItemId, true);
                validateEligibleItem(item, requestType, reasonCode, quantity, uploads.size());
                int available = availableQuantity(c, orderItemId, item.purchasedQuantity());
                if (!ReturnRefundPolicy.isQuantityAvailable(quantity, available)) {
                    throw new IllegalArgumentException("Số lượng có thể yêu cầu hiện còn " + available + " sản phẩm.");
                }
                BigDecimal amount = ReturnRefundPolicy.requestedAmount(item.unitPrice(), quantity);
                String code = null;
                long id = -1;
                for (int attempt = 0; attempt < 5 && id < 0; attempt++) {
                    code = "RR-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
                            + String.format(Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
                    String sql = "INSERT INTO return_refund_requests "
                            + "(request_code,user_id,order_id,order_item_id,request_type,reason_code,requested_quantity,"
                            + "requested_amount,currency,status,customer_note,policy_version,return_deadline_at,submitted_at,updated_at) "
                            + "VALUES (?,?,?,?,?,?,?,?,?,'SUBMITTED',?,?,?,NOW(),NOW())";
                    try (PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                        p.setString(1, code); p.setLong(2, userId); p.setLong(3, item.orderId()); p.setLong(4, orderItemId);
                        p.setString(5, requestType); p.setString(6, reasonCode); p.setInt(7, quantity);
                        p.setBigDecimal(8, amount); p.setString(9, "VND"); p.setString(10, note);
                        p.setString(11, ReturnRefundPolicy.POLICY_VERSION);
                        p.setTimestamp(12, Timestamp.valueOf(item.deliveredAt().plusDays(ReturnRefundPolicy.RETURN_WINDOW_DAYS)));
                        p.executeUpdate();
                        try (ResultSet keys = p.getGeneratedKeys()) { if (keys.next()) id = keys.getLong(1); }
                    } catch (SQLException e) {
                        if (e.getErrorCode() != 1062) throw e;
                    }
                }
                if (id < 0) throw new SQLException("Không tạo được mã yêu cầu đổi trả duy nhất.");
                for (EvidenceUpload upload : uploads) insertEvidence(c, id, userId, upload);
                Long supportTicketId = null;
                java.sql.Savepoint ticketSavepoint = c.setSavepoint();
                try {
                    long createdTicketId = insertLinkedSupportTicket(c, userId, item, code, requestType, reasonCode, note);
                    try (PreparedStatement p = c.prepareStatement(
                            "UPDATE return_refund_requests SET support_ticket_id=? WHERE request_id=?")) {
                        p.setLong(1, createdTicketId);
                        p.setLong(2, id);
                        p.executeUpdate();
                    }
                    supportTicketId = createdTicketId;
                } catch (SQLException | RuntimeException ticketError) {
                    // The return/refund case remains the primary workflow even if support integration is unavailable.
                    c.rollback(ticketSavepoint);
                }
                String eventNote = supportTicketId == null
                        ? "Khách hàng gửi yêu cầu đổi trả/hoàn tiền. Hồ sơ vẫn được theo dõi độc lập trong mục Đổi trả / Hoàn tiền."
                        : "Khách hàng gửi yêu cầu đổi trả/hoàn tiền. Ticket trao đổi: #" + supportTicketId + ".";
                insertEvent(c, id, userId, null, "SUBMITTED", eventNote);
                c.commit();
                if (supportTicketId != null) {
                    try {
                        SupportDao.notifyAdmins("Yêu cầu đổi trả/hoàn tiền " + code, supportTicketId, code);
                    } catch (RuntimeException ignored) { }
                }
                return id;
            } catch (SQLException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static void addEvidenceForUser(long requestId, long userId, List<EvidenceUpload> evidence) {
        List<EvidenceUpload> uploads = evidence == null ? List.of() : List.copyOf(evidence);
        if (uploads.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn ít nhất một ảnh bằng chứng.");
        if (uploads.size() > 4) throw new IllegalArgumentException("Mỗi lần chỉ được tải tối đa 4 ảnh.");
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                String status;
                try (PreparedStatement p = c.prepareStatement("SELECT status FROM return_refund_requests WHERE request_id=? AND user_id=? FOR UPDATE")) {
                    p.setLong(1, requestId); p.setLong(2, userId);
                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next()) throw new IllegalArgumentException("Không tìm thấy yêu cầu đổi trả/hoàn tiền của bạn.");
                        status = r.getString(1);
                    }
                }
                if (!Set.of("SUBMITTED", "UNDER_REVIEW").contains(status)) {
                    throw new IllegalArgumentException("Chỉ bổ sung bằng chứng khi yêu cầu đang được tiếp nhận hoặc xem xét.");
                }
                int count = countEvidence(c, requestId);
                if (count + uploads.size() > 8) throw new IllegalArgumentException("Mỗi yêu cầu được đính kèm tối đa 8 ảnh.");
                for (EvidenceUpload upload : uploads) insertEvidence(c, requestId, userId, upload);
                insertEvent(c, requestId, userId, status, status, "Khách hàng bổ sung bằng chứng.");
                c.commit();
            } catch (SQLException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            } finally { c.setAutoCommit(true); }
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static void withdraw(long requestId, long userId) {
        transitionAsCustomer(requestId, userId, "WITHDRAWN", "Khách hàng chủ động rút yêu cầu.");
    }

    public static void markReturnShipped(long requestId, long userId, String carrier, String trackingNumber) {
        String safeCarrier = clean(carrier, 100);
        String safeTracking = clean(trackingNumber, 120);
        requireText(safeCarrier, "Vui lòng nhập đơn vị vận chuyển hoặc hình thức gửi trả.");
        requireText(safeTracking, "Vui lòng nhập mã vận đơn hoặc mã biên nhận trả hàng.");
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                String oldStatus;
                try (PreparedStatement p = c.prepareStatement(
                        "SELECT status FROM return_refund_requests WHERE request_id=? AND user_id=? FOR UPDATE")) {
                    p.setLong(1, requestId); p.setLong(2, userId);
                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next()) throw new IllegalArgumentException("Không tìm thấy yêu cầu đổi trả/hoàn tiền của bạn.");
                        oldStatus = r.getString(1);
                    }
                }
                if (!"AWAITING_RETURN_SHIPMENT".equals(oldStatus)) {
                    throw new IllegalArgumentException("Yêu cầu chưa được duyệt để gửi trả hoặc đã qua bước gửi trả.");
                }
                try (PreparedStatement p = c.prepareStatement(
                        "UPDATE return_refund_requests SET return_carrier=?,return_tracking_number=?,return_shipped_at=NOW(),updated_at=NOW() WHERE request_id=?")) {
                    p.setString(1, safeCarrier); p.setString(2, safeTracking); p.setLong(3, requestId);
                    if (p.executeUpdate() != 1) throw new IllegalArgumentException("Không cập nhật được thông tin trả hàng.");
                }
                updateStatus(c, requestId, "RETURN_SHIPPED", null);
                insertEvent(c, requestId, userId, oldStatus, "RETURN_SHIPPED",
                        "Khách hàng đã gửi hàng trả. Đơn vị vận chuyển: " + safeCarrier + "; mã: " + safeTracking);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            } finally { c.setAutoCommit(true); }
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    private static void transitionAsCustomer(long requestId, long userId, String nextStatus, String note) {
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                String oldStatus;
                try (PreparedStatement p = c.prepareStatement("SELECT status FROM return_refund_requests WHERE request_id=? AND user_id=? FOR UPDATE")) {
                    p.setLong(1, requestId); p.setLong(2, userId);
                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next()) throw new IllegalArgumentException("Không tìm thấy yêu cầu đổi trả/hoàn tiền của bạn.");
                        oldStatus = r.getString(1);
                    }
                }
                if (!ReturnRefundPolicy.canTransition(oldStatus, nextStatus)) {
                    throw new IllegalArgumentException("Yêu cầu đã được xử lý hoặc không còn có thể rút.");
                }
                updateStatus(c, requestId, nextStatus, null);
                insertEvent(c, requestId, userId, oldStatus, nextStatus, note);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            } finally { c.setAutoCommit(true); }
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static List<Map<String, Object>> listForUser(long userId) {
        String sql = "SELECT r.request_id,r.request_code,r.order_id,o.order_code,r.order_item_id,"
                + "oi.product_name_snapshot,r.request_type,r.reason_code,r.requested_quantity,r.requested_amount,"
                + "r.approved_amount,r.currency,r.status,r.customer_note,r.decision_note,r.resolution_method,"
                + "r.refund_reference,r.submitted_at,r.updated_at,r.decided_at,r.resolved_at,"
                + "(SELECT COUNT(*) FROM return_refund_evidence e WHERE e.request_id=r.request_id) evidence_count "
                + "FROM return_refund_requests r JOIN orders o ON o.order_id=r.order_id "
                + "JOIN order_items oi ON oi.order_item_id=r.order_item_id "
                + "WHERE r.user_id=? ORDER BY r.submitted_at DESC,r.request_id DESC LIMIT 100";
        List<Map<String, Object>> rows = queryMaps(sql, p -> p.setLong(1, userId));
        for (Map<String, Object> row : rows) {
            row.put("statusLabel", ReturnRefundPolicy.label(String.valueOf(row.get("status"))));
        }
        return rows;
    }

    public static Map<String, Object> detailForUser(long requestId, long userId) {
        Map<String, Object> m = queryOne("SELECT r.*,o.order_code,oi.product_name_snapshot,oi.quantity purchased_quantity "
                + "FROM return_refund_requests r JOIN orders o ON o.order_id=r.order_id "
                + "JOIN order_items oi ON oi.order_item_id=r.order_item_id WHERE r.request_id=? AND r.user_id=?",
                p -> { p.setLong(1, requestId); p.setLong(2, userId); });
        if (m != null) m.put("statusLabel", ReturnRefundPolicy.label(String.valueOf(m.get("status"))));
        return m;
    }

    public static List<Map<String, Object>> eventsForUser(long requestId, long userId) {
        if (detailForUser(requestId, userId) == null) return List.of();
        return listEvents(requestId);
    }

    public static Map<String, Object> evidenceForUser(long evidenceId, long userId) {
        return queryOne("SELECT e.evidence_id,e.request_id,e.original_filename,e.storage_key,e.content_type,e.file_size,e.sha256 "
                + "FROM return_refund_evidence e JOIN return_refund_requests r ON r.request_id=e.request_id "
                + "WHERE e.evidence_id=? AND r.user_id=?",
                p -> { p.setLong(1, evidenceId); p.setLong(2, userId); });
    }

    public static List<Map<String, Object>> evidenceListForUser(long requestId, long userId) {
        if (detailForUser(requestId, userId) == null) return List.of();
        return listEvidence(requestId);
    }

    public static List<Map<String, Object>> listForAdmin(String status, String query) {
        Set<String> statuses = Set.of("SUBMITTED", "UNDER_REVIEW", "AWAITING_RETURN_SHIPMENT",
                "RETURN_RECEIVED", "REFUND_PROCESSING", "REFUND_FAILED", "REFUNDED", "REJECTED", "WITHDRAWN");
        String s = status == null || status.isBlank() || "ALL".equalsIgnoreCase(status) ? null : status.trim().toUpperCase(Locale.ROOT);
        if (s != null && !statuses.contains(s)) throw new IllegalArgumentException("Bộ lọc trạng thái không hợp lệ.");
        String q = query == null || query.isBlank() ? null : query.trim().substring(0, Math.min(120, query.trim().length()));
        String sql = "SELECT r.request_id,r.request_code,r.user_id,u.full_name customer_name,u.email,"
                + "o.order_code,oi.product_name_snapshot,r.request_type,r.reason_code,r.requested_quantity,"
                + "r.requested_amount,r.approved_amount,r.status,r.submitted_at,r.updated_at "
                + "FROM return_refund_requests r JOIN users u ON u.user_id=r.user_id "
                + "JOIN orders o ON o.order_id=r.order_id JOIN order_items oi ON oi.order_item_id=r.order_item_id "
                + "WHERE (? IS NULL OR r.status=?) AND (? IS NULL OR r.request_code LIKE ? OR o.order_code LIKE ? "
                + "OR u.email LIKE ? OR oi.product_name_snapshot LIKE ?) ORDER BY r.submitted_at DESC LIMIT 200";
        return queryMaps(sql, p -> {
            p.setString(1, s); p.setString(2, s); p.setString(3, q);
            p.setString(4, q == null ? null : "%" + q + "%");
            p.setString(5, q == null ? null : "%" + q + "%");
            p.setString(6, q == null ? null : "%" + q + "%");
            p.setString(7, q == null ? null : "%" + q + "%");
        });
    }

    public static Map<String, Object> detailForAdmin(long requestId) {
        return queryOne("SELECT r.*,u.full_name customer_name,u.email customer_email,o.order_code,"
                + "oi.product_name_snapshot,oi.quantity purchased_quantity FROM return_refund_requests r "
                + "JOIN users u ON u.user_id=r.user_id JOIN orders o ON o.order_id=r.order_id "
                + "JOIN order_items oi ON oi.order_item_id=r.order_item_id WHERE r.request_id=?",
                p -> p.setLong(1, requestId));
    }

    public static List<Map<String, Object>> evidenceListForAdmin(long requestId) {
        return listEvidence(requestId);
    }

    public static Map<String, Object> evidenceForAdmin(long evidenceId) {
        return queryOne("SELECT evidence_id,request_id,original_filename,storage_key,content_type,file_size,sha256 "
                + "FROM return_refund_evidence WHERE evidence_id=?", p -> p.setLong(1, evidenceId));
    }

    public static List<Map<String, Object>> eventsForAdmin(long requestId) {
        return listEvents(requestId);
    }

    public static List<Map<String, Object>> refundTransactionsForAdmin(long requestId) {
        return queryMaps("SELECT t.transaction_id,t.request_id,t.amount,t.method,t.reference,t.status,t.note,t.recorded_at,"
                + "u.full_name recorded_by_name FROM return_refund_transactions t "
                + "JOIN users u ON u.user_id=t.recorded_by_user_id WHERE t.request_id=? "
                + "ORDER BY t.recorded_at DESC,t.transaction_id DESC", p -> p.setLong(1, requestId));
    }

    /** Admin decision point; actual external bank/gateway transfer is performed outside this application. */
    public static void actAsAdmin(long requestId, long adminId, String action, String note,
                                  BigDecimal approvedAmount, String refundMethod, String refundReference) {
        String safeNote = clean(note, 2000);
        String actionName = action == null ? "" : action.trim().toLowerCase(Locale.ROOT);
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            long customerId;
            long orderItemId;
            String oldStatus;
            String requestType;
            BigDecimal requestedAmount;
            BigDecimal currentApprovedAmount;
            try {
                try (PreparedStatement p = c.prepareStatement("SELECT user_id,order_item_id,status,request_type,requested_amount,approved_amount "
                        + "FROM return_refund_requests WHERE request_id=? FOR UPDATE")) {
                    p.setLong(1, requestId);
                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next()) throw new IllegalArgumentException("Không tìm thấy yêu cầu.");
                        customerId = r.getLong("user_id"); orderItemId = r.getLong("order_item_id"); oldStatus = r.getString("status");
                        requestType = r.getString("request_type"); requestedAmount = r.getBigDecimal("requested_amount");
                        currentApprovedAmount = r.getBigDecimal("approved_amount");
                    }
                }
                String nextStatus;
                switch (actionName) {
                    case "review" -> nextStatus = "UNDER_REVIEW";
                    case "approve" -> nextStatus = "RETURN_AND_REFUND".equals(requestType)
                            ? "AWAITING_RETURN_SHIPMENT" : "REFUND_PROCESSING";
                    case "reject" -> {
                        requireText(safeNote, "Vui lòng ghi rõ lý do từ chối.");
                        nextStatus = "REJECTED";
                    }
                    case "mark_received" -> nextStatus = "RETURN_RECEIVED";
                    case "start_refund" -> {
                        if (currentApprovedAmount == null || currentApprovedAmount.signum() <= 0) {
                            throw new IllegalArgumentException("Vui lòng duyệt số tiền trước khi bắt đầu hoàn tiền.");
                        }
                        nextStatus = "REFUND_PROCESSING";
                    }
                    case "refund_complete" -> {
                        if (refundMethod == null || !ReturnRefundPolicy.REFUND_METHODS.contains(refundMethod))
                            throw new IllegalArgumentException("Phương thức hoàn tiền không hợp lệ.");
                        requireText(refundReference, "Vui lòng nhập mã tham chiếu/kết quả giao dịch hoàn tiền.");
                        if (refundReference.trim().length() > 120) throw new IllegalArgumentException("Mã hoàn tiền tối đa 120 ký tự.");
                        nextStatus = "REFUNDED";
                    }
                    case "refund_failed" -> {
                        requireText(safeNote, "Vui lòng ghi rõ lý do hoàn tiền thất bại.");
                        nextStatus = "REFUND_FAILED";
                    }
                    case "retry_refund" -> nextStatus = "REFUND_PROCESSING";
                    default -> throw new IllegalArgumentException("Thao tác xử lý yêu cầu không hợp lệ.");
                }
                if (!ReturnRefundPolicy.canTransition(oldStatus, nextStatus)) {
                    throw new IllegalArgumentException("Không thể chuyển yêu cầu từ " + oldStatus + " sang " + nextStatus + ".");
                }
                if ("approve".equals(actionName)) {
                    BigDecimal amount = approvedAmount == null ? requestedAmount : approvedAmount;
                    if (amount == null || amount.signum() <= 0 || amount.compareTo(requestedAmount) > 0) {
                        throw new IllegalArgumentException("Số tiền duyệt phải lớn hơn 0 và không vượt số tiền khách yêu cầu.");
                    }
                    try (PreparedStatement p = c.prepareStatement("UPDATE return_refund_requests SET approved_amount=?,"
                            + "decided_by_user_id=?,decided_at=NOW(),decision_note=?,updated_at=NOW() WHERE request_id=?")) {
                        p.setBigDecimal(1, amount); p.setLong(2, adminId); p.setString(3, emptyToNull(safeNote)); p.setLong(4, requestId); p.executeUpdate();
                    }
                } else if ("reject".equals(actionName)) {
                    try (PreparedStatement p = c.prepareStatement("UPDATE return_refund_requests SET decided_by_user_id=?,"
                            + "decided_at=NOW(),decision_note=?,updated_at=NOW() WHERE request_id=?")) {
                        p.setLong(1, adminId); p.setString(2, safeNote); p.setLong(3, requestId); p.executeUpdate();
                    }
                }
                if ("refund_complete".equals(actionName)) {
                    try (PreparedStatement p = c.prepareStatement("UPDATE return_refund_requests SET resolution_method=?,"
                            + "refund_reference=?,resolved_at=NOW(),decided_by_user_id=?,updated_at=NOW() WHERE request_id=?")) {
                        p.setString(1, refundMethod); p.setString(2, refundReference.trim()); p.setLong(3, adminId); p.setLong(4, requestId); p.executeUpdate();
                    }
                }
                if (Set.of("mark_received", "refund_failed", "retry_refund").contains(actionName) && !safeNote.isBlank()) {
                    try (PreparedStatement p = c.prepareStatement("UPDATE return_refund_requests SET decision_note=?,"
                            + "decided_by_user_id=?,decided_at=NOW(),updated_at=NOW() WHERE request_id=?")) {
                        p.setString(1, safeNote); p.setLong(2, adminId); p.setLong(3, requestId); p.executeUpdate();
                    }
                }
                if ("refund_complete".equals(actionName) || "refund_failed".equals(actionName)) {
                    BigDecimal transactionAmount = currentApprovedAmount == null ? requestedAmount : currentApprovedAmount;
                    if (transactionAmount == null || transactionAmount.signum() <= 0) {
                        throw new IllegalArgumentException("Yêu cầu chưa có số tiền hoàn trả hợp lệ.");
                    }
                    String transactionMethod = refundMethod != null && ReturnRefundPolicy.REFUND_METHODS.contains(refundMethod)
                            ? refundMethod : "OTHER";
                    String transactionReference = emptyToNull(refundReference == null ? null : refundReference.trim());
                    try (PreparedStatement p = c.prepareStatement(
                            "INSERT INTO return_refund_transactions "
                                    + "(request_id,recorded_by_user_id,amount,method,reference,status,note,recorded_at) "
                                    + "VALUES (?,?,?,?,?,?,?,NOW())")) {
                        p.setLong(1, requestId); p.setLong(2, adminId); p.setBigDecimal(3, transactionAmount);
                        p.setString(4, transactionMethod); p.setString(5, transactionReference);
                        p.setString(6, "refund_complete".equals(actionName) ? "SUCCESS" : "FAILED");
                        p.setString(7, emptyToNull(safeNote)); p.executeUpdate();
                    }
                    if ("refund_complete".equals(actionName)) {
                        FinancialAllocationService.recordItemRefund(c, orderItemId, transactionAmount,
                                "RETURN_REFUND", requestId, adminId);
                    }
                }
                updateStatus(c, requestId, nextStatus, null);
                insertEvent(c, requestId, adminId, oldStatus, nextStatus,
                        safeNote.isBlank() ? defaultEventNote(actionName, nextStatus) : safeNote);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            } finally { c.setAutoCommit(true); }
            try {
                NotificationService.notifyUser(customerId, "RETURN_REFUND_UPDATED",
                        "Cập nhật yêu cầu đổi trả/hoàn tiền",
                        "Yêu cầu " + requestCode(requestId) + " đã chuyển sang trạng thái: "
                                + ReturnRefundPolicy.label(nextStatusFor(actionName, requestType)) + ".",
                        "/return-refunds?id=" + requestId, "RETURN_REFUND", requestId);
            } catch (RuntimeException ignored) { }
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    private static String nextStatusFor(String action, String requestType) {
        return switch (action == null ? "" : action.toLowerCase(Locale.ROOT)) {
            case "review" -> "UNDER_REVIEW";
            case "approve" -> "RETURN_AND_REFUND".equals(requestType) ? "AWAITING_RETURN_SHIPMENT" : "REFUND_PROCESSING";
            case "reject" -> "REJECTED";
            case "mark_received" -> "RETURN_RECEIVED";
            case "start_refund" -> "REFUND_PROCESSING";
            case "refund_complete" -> "REFUNDED";
            case "refund_failed" -> "REFUND_FAILED";
            case "retry_refund" -> "REFUND_PROCESSING";
            default -> "SUBMITTED";
        };
    }

    private static String requestCode(long id) {
        Map<String, Object> m = queryOne("SELECT request_code FROM return_refund_requests WHERE request_id=?", p -> p.setLong(1, id));
        return m == null ? "#" + id : String.valueOf(m.get("requestCode"));
    }

    private static String defaultEventNote(String action, String status) {
        return switch (action) {
            case "review" -> "Admin bắt đầu xem xét yêu cầu.";
            case "approve" -> "Yêu cầu được duyệt; chuyển sang bước tiếp theo.";
            case "reject" -> "Yêu cầu bị từ chối.";
            case "mark_received" -> "Đã xác nhận nhận hàng hoàn trả.";
            case "start_refund" -> "Yêu cầu chuyển sang bước thực hiện hoàn tiền.";
            case "refund_complete" -> "Admin xác nhận việc hoàn tiền đã hoàn tất ở hệ thống thanh toán bên ngoài.";
            case "refund_failed" -> "Giao dịch hoàn tiền chưa thành công.";
            case "retry_refund" -> "Yêu cầu được đưa lại vào quy trình hoàn tiền.";
            default -> "Cập nhật trạng thái: " + status;
        };
    }

    private static long insertLinkedSupportTicket(Connection c, long userId, PurchaseItem item,
                                                    String requestCode, String requestType,
                                                    String reasonCode, String customerNote) throws SQLException {
        String subject = "Đổi trả / hoàn tiền - " + requestCode;
        String priority = ReturnRefundPolicy.EVIDENCE_REQUIRED_REASONS.contains(reasonCode) ? "HIGH" : "NORMAL";
        String sql = "INSERT INTO support_tickets (ticket_code,user_id,order_id,shop_id,category,subject,priority,status,created_at,updated_at) "
                + "VALUES (?,?,?,?,? ,? ,? ,'OPEN',NOW(),NOW())";
        long ticketId = -1;
        String ticketCode = null;
        for (int attempt = 0; attempt < 5 && ticketId < 0; attempt++) {
            ticketCode = "TK-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                    + "-" + String.format(Locale.ROOT, "%04d", RANDOM.nextInt(10000));
            try (PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                p.setString(1, ticketCode);
                p.setLong(2, userId);
                p.setLong(3, item.orderId());
                if (item.shopId() == null) p.setNull(4, java.sql.Types.BIGINT); else p.setLong(4, item.shopId());
                p.setString(5, "REFUND");
                p.setString(6, subject.length() > 180 ? subject.substring(0, 180) : subject);
                p.setString(7, priority);
                p.executeUpdate();
                try (ResultSet keys = p.getGeneratedKeys()) {
                    if (keys.next()) ticketId = keys.getLong(1);
                }
            } catch (SQLException e) {
                if (e.getErrorCode() != 1062) throw e;
            }
        }
        if (ticketId < 0) throw new SQLException("Không thể tạo ticket trao đổi cho yêu cầu đổi trả/hoàn tiền.");
        String message = "Yêu cầu " + requestCode + " được tạo. Loại: " + requestType
                + "; lý do: " + reasonCode + ". Nội dung khách hàng: " + customerNote;
        try (PreparedStatement p = c.prepareStatement(
                "INSERT INTO support_ticket_messages(ticket_id,sender_user_id,message_body,is_internal,created_at) VALUES(?,?,?,0,NOW())")) {
            p.setLong(1, ticketId);
            p.setLong(2, userId);
            p.setString(3, message);
            p.executeUpdate();
        }
        return ticketId;
    }

    private static PurchaseItem findPurchaseItem(Connection c, long userId, long orderItemId, boolean forUpdate)
            throws SQLException {
        String sql = "SELECT o.order_id,o.order_code,o.status order_status,o.shipping_status,o.payment_status,"
                + "oi.order_item_id,oi.quantity purchased_quantity,oi.unit_price,oi.line_total,"
                + "oi.product_name_snapshot,oi.design_file_id,oi.shop_id,COALESCE(p.is_customizable,0) custom_made,"
                + "COALESCE((SELECT MAX(s.delivered_at) FROM shipments s WHERE s.order_id=o.order_id AND oi.shop_id IS NOT NULL AND s.shop_id=oi.shop_id AND s.delivered_at IS NOT NULL),o.completed_at) delivered_at,"
                + "EXISTS(SELECT 1 FROM shipments sd WHERE sd.order_id=o.order_id AND oi.shop_id IS NOT NULL AND sd.shop_id=oi.shop_id AND sd.delivered_at IS NOT NULL) has_delivery_event "
                + "FROM orders o JOIN order_items oi ON oi.order_id=o.order_id "
                + "LEFT JOIN products p ON p.product_id=oi.product_id WHERE o.user_id=? AND oi.order_item_id=?"
                + (forUpdate ? " FOR UPDATE" : "");
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, userId); p.setLong(2, orderItemId);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) throw new IllegalArgumentException("Sản phẩm không thuộc đơn hàng của tài khoản này.");
                LocalDateTime deliveredAt = dateTime(r.getTimestamp("delivered_at"));
                boolean hasDeliveredShipment = r.getBoolean("has_delivery_event");
                return new PurchaseItem(r.getLong("order_id"), r.getString("order_code"),
                        r.getString("order_status"), r.getString("shipping_status"),
                        r.getString("payment_status"), r.getLong("order_item_id"),
                        r.getInt("purchased_quantity"), r.getBigDecimal("unit_price"),
                        r.getBigDecimal("line_total"), r.getString("product_name_snapshot"),
                        r.getObject("design_file_id") != null,
                        (Long) r.getObject("shop_id"), deliveredAt, hasDeliveredShipment);
            }
        }
    }

    private static void validateEligibleItem(PurchaseItem item, String requestType, String reasonCode,
                                             int quantity, int evidenceCount) {
        if ("Đã hủy".equals(item.orderStatus()) || "CANCELLED".equalsIgnoreCase(item.orderStatus())) {
            throw new IllegalArgumentException("Đơn hàng đã hủy không đủ điều kiện đổi trả.");
        }
        boolean delivered = "Đã hoàn thành".equals(item.orderStatus()) || "COMPLETED".equalsIgnoreCase(item.orderStatus())
                || "Đã giao".equals(item.shippingStatus()) || "DELIVERED".equalsIgnoreCase(item.shippingStatus())
                || item.hasDeliveredShipment();
        if (!delivered || !ReturnRefundPolicy.isWithinReturnWindow(item.deliveredAt(), LocalDateTime.now())) {
            throw new IllegalArgumentException("Chỉ có thể yêu cầu trong vòng " + ReturnRefundPolicy.RETURN_WINDOW_DAYS
                    + " ngày kể từ khi đơn được giao thành công.");
        }
        if (!ReturnRefundPolicy.isQuantityAvailable(quantity, item.purchasedQuantity())) {
            throw new IllegalArgumentException("Số lượng yêu cầu không hợp lệ.");
        }
        if (item.customMade() && !ReturnRefundPolicy.isReasonAllowedForCustomItem(reasonCode)) {
            throw new IllegalArgumentException("Sản phẩm cá nhân hóa/in theo yêu cầu chỉ được đổi trả khi lỗi, hư hỏng, giao sai hoặc thiếu hàng.");
        }
        if ("REFUND_ONLY".equals(requestType) && "CHANGED_MIND".equals(reasonCode)) {
            throw new IllegalArgumentException("Lý do đổi ý cần gửi trả sản phẩm; vui lòng chọn loại Trả hàng và hoàn tiền.");
        }
        if (ReturnRefundPolicy.requiresEvidence(reasonCode, item.customMade()) && evidenceCount < 1) {
            throw new IllegalArgumentException("Lý do này cần tối thiểu một ảnh bằng chứng.");
        }
        if (evidenceCount > 4) throw new IllegalArgumentException("Mỗi yêu cầu chỉ nhận tối đa 4 ảnh ban đầu.");
    }

    private static int availableQuantity(Connection c, long orderItemId, int purchased) throws SQLException {
        String sql = "SELECT COALESCE(SUM(requested_quantity),0) FROM return_refund_requests "
                + "WHERE order_item_id=? AND status NOT IN ('REJECTED','WITHDRAWN')";
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, orderItemId);
            try (ResultSet r = p.executeQuery()) {
                r.next();
                return Math.max(0, purchased - r.getInt(1));
            }
        }
    }

    private static void insertEvidence(Connection c, long requestId, long userId, EvidenceUpload e) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("INSERT INTO return_refund_evidence "
                + "(request_id,uploaded_by_user_id,original_filename,storage_key,content_type,file_size,sha256,uploaded_at) "
                + "VALUES (?,?,?,?,?,?,?,NOW())")) {
            p.setLong(1, requestId); p.setLong(2, userId); p.setString(3, clean(e.originalFilename(), 180));
            p.setString(4, e.storageKey()); p.setString(5, e.contentType()); p.setLong(6, e.fileSize());
            p.setString(7, e.sha256()); p.executeUpdate();
        }
    }

    private static int countEvidence(Connection c, long requestId) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT COUNT(*) FROM return_refund_evidence WHERE request_id=?")) {
            p.setLong(1, requestId); try (ResultSet r = p.executeQuery()) { r.next(); return r.getInt(1); }
        }
    }

    private static void updateStatus(Connection c, long requestId, String status, String unused) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("UPDATE return_refund_requests SET status=?,updated_at=NOW() WHERE request_id=?")) {
            p.setString(1, status); p.setLong(2, requestId);
            if (p.executeUpdate() != 1) throw new IllegalArgumentException("Yêu cầu không còn tồn tại.");
        }
    }

    private static void insertEvent(Connection c, long requestId, long actorId, String from, String to, String note)
            throws SQLException {
        try (PreparedStatement p = c.prepareStatement("INSERT INTO return_refund_request_events "
                + "(request_id,actor_user_id,from_status,to_status,note,created_at) VALUES (?,?,?,?,?,NOW())")) {
            p.setLong(1, requestId); p.setLong(2, actorId); p.setString(3, from); p.setString(4, to);
            p.setString(5, clean(note, 2000)); p.executeUpdate();
        }
    }

    private static List<Map<String, Object>> listEvidence(long requestId) {
        return queryMaps("SELECT evidence_id,request_id,original_filename,storage_key,content_type,file_size,sha256,uploaded_at "
                + "FROM return_refund_evidence WHERE request_id=? ORDER BY uploaded_at,evidence_id",
                p -> p.setLong(1, requestId));
    }

    private static List<Map<String, Object>> listEvents(long requestId) {
        List<Map<String, Object>> events = queryMaps(
                "SELECT e.event_id,e.actor_user_id,e.from_status,e.to_status,e.note,e.created_at,u.full_name actor_name "
                        + "FROM return_refund_request_events e LEFT JOIN users u ON u.user_id=e.actor_user_id "
                        + "WHERE e.request_id=? ORDER BY e.created_at,e.event_id",
                p -> p.setLong(1, requestId));
        for (Map<String, Object> event : events) {
            event.put("toStatusLabel", ReturnRefundPolicy.label(String.valueOf(event.get("toStatus"))));
        }
        return events;
    }

    @FunctionalInterface private interface Binder { void bind(PreparedStatement p) throws SQLException; }

    private static List<Map<String, Object>> queryMaps(String sql, Binder binder) {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            binder.bind(p);
            try (ResultSet r = p.executeQuery()) {
                var md = r.getMetaData();
                while (r.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    for (int i = 1; i <= md.getColumnCount(); i++) {
                        Object value = r.getObject(i);
                        String key = md.getColumnLabel(i);
                        if (value instanceof Timestamp t) value = t.toLocalDateTime().toString().replace('T', ' ');
                        m.put(key, value);
                    }
                    normalizeKeys(m);
                    rows.add(m);
                }
            }
            return rows;
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    private static Map<String, Object> queryOne(String sql, Binder binder) {
        List<Map<String, Object>> rows = queryMaps(sql, binder);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private static void normalizeKeys(Map<String, Object> m) {
        // Preserve snake_case aliases for SQL clients while exposing JSP/Java-friendly camelCase.
        List<Map.Entry<String, Object>> entries = new ArrayList<>(m.entrySet());
        for (Map.Entry<String, Object> e : entries) {
            String key = e.getKey();
            String camel = snakeToCamel(key);
            if (!camel.equals(key)) m.putIfAbsent(camel, e.getValue());
        }
        Object from = m.get("fromStatus");
        Object to = m.get("toStatus");
        if (m.containsKey("from_status")) m.put("fromStatus", from);
        if (m.containsKey("to_status")) m.put("toStatus", to);
    }

    private static String snakeToCamel(String value) {
        StringBuilder out = new StringBuilder(value.length());
        boolean uppercaseNext = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '_') { uppercaseNext = true; continue; }
            out.append(uppercaseNext ? Character.toUpperCase(ch) : ch);
            uppercaseNext = false;
        }
        return out.toString();
    }

    private static LocalDateTime dateTime(Timestamp t) { return t == null ? null : t.toLocalDateTime(); }
    private static String format(LocalDateTime t) { return t == null ? "" : t.toString().replace('T', ' '); }
    private static String clean(String s, int max) {
        String value = s == null ? "" : s.trim();
        if (value.length() > max) throw new IllegalArgumentException("Nội dung vượt quá " + max + " ký tự.");
        return value;
    }
    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
    }
    private static String emptyToNull(String s) { return s == null || s.isBlank() ? null : s; }

    private record PurchaseItem(long orderId, String orderCode, String orderStatus, String shippingStatus,
                                String paymentStatus, long orderItemId, int purchasedQuantity,
                                BigDecimal unitPrice, BigDecimal lineTotal, String productName,
                                boolean customMade, Long shopId, LocalDateTime deliveredAt, boolean hasDeliveredShipment) { }
}
