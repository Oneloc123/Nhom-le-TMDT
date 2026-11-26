package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.NotificationService;

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

/** Read and audited actions for Admin's operational queues. */
public final class AdminBusinessQueueDao {
    private static final int MAX_ROWS = 300;

    private AdminBusinessQueueDao() { }

    public static List<Map<String, Object>> listCustomPrintRequests(String rawQuery, String rawStatus) {
        boolean hasReviewEvents = tableExists("admin_custom_print_review_events");
        StringBuilder sql = new StringBuilder(
                "SELECT r.custom_request_id id,r.request_code code,r.status,r.workflow_type workflowType,"
              + "r.technology technology,r.material,r.quantity,r.estimated_total estimatedTotal,"
              + "r.quoted_amount quotedAmount,r.seller_response_note sellerResponseNote,"
              + "r.production_days productionDays,r.quote_valid_until quoteValidUntil,r.order_id orderId,"
              + "f.original_filename fileName,f.analysis_status analysisStatus,"
              + "u.full_name customerName,u.email customerEmail,s.shop_name shopName,"
              + "r.created_at createdAt,r.submitted_at submittedAt,"
              + (hasReviewEvents
                ? "(SELECT e.action FROM admin_custom_print_review_events e WHERE e.custom_request_id=r.custom_request_id"
                  + " ORDER BY e.event_id DESC LIMIT 1) adminReviewAction,"
                  + "(SELECT e.note FROM admin_custom_print_review_events e WHERE e.custom_request_id=r.custom_request_id"
                  + " ORDER BY e.event_id DESC LIMIT 1) adminReviewNote"
                : "NULL adminReviewAction,NULL adminReviewNote")
              + " FROM custom_print_requests r"
              + " LEFT JOIN design_files f ON f.design_file_id=r.design_file_id AND f.user_id=r.user_id"
              + " LEFT JOIN users u ON u.user_id=r.user_id"
              + " LEFT JOIN print_shops s ON s.shop_id=r.shop_id WHERE 1=1");
        List<Object> args = new ArrayList<>();
        String status = filter(rawStatus);
        String query = filter(rawQuery);
        if (status != null) { sql.append(" AND r.status=?"); args.add(status); }
        if (hasReviewEvents && "PENDING_REVIEW".equalsIgnoreCase(status)) {
            sql.append(" AND COALESCE((SELECT e.action FROM admin_custom_print_review_events e"
                    + " WHERE e.custom_request_id=r.custom_request_id ORDER BY e.event_id DESC LIMIT 1),'')<>'REVIEWED'");
        }
        if (query != null) {
            sql.append(" AND (r.request_code LIKE ? OR f.original_filename LIKE ? OR u.full_name LIKE ? OR u.email LIKE ?)");
            String like = "%" + query + "%";
            args.add(like); args.add(like); args.add(like); args.add(like);
        }
        sql.append(" ORDER BY CASE r.status WHEN 'PENDING_REVIEW' THEN 0 WHEN 'READY_TO_ORDER' THEN 1"
                + " WHEN 'DRAFT' THEN 2 WHEN 'QUOTED' THEN 3 ELSE 4 END, r.created_at DESC LIMIT ")
                .append(MAX_ROWS);
        return rows(sql.toString(), args.toArray());
    }

    public static Map<String, Object> customPrintRequestDetail(long requestId) {
        Map<String, Object> item = one(
                "SELECT r.custom_request_id id,r.request_code code,r.user_id userId,r.design_file_id designFileId,"
              + "r.shop_id shopId,s.shop_name shopName,r.workflow_type workflowType,r.technology technology,"
              + "r.material,r.infill_percent infillPercent,r.layer_height_mm layerHeightMm,r.quantity,"
              + "r.color_option colorOption,r.post_processing_option postProcessingOption,r.customer_note customerNote,"
              + "r.dimensions_confirmed dimensionsConfirmed,r.estimated_total estimatedTotal,r.quoted_amount quotedAmount,"
              + "r.seller_response_note sellerResponseNote,r.production_days productionDays,r.quote_valid_until quoteValidUntil,"
              + "r.status,r.order_id orderId,r.submitted_at submittedAt,r.quoted_at quotedAt,r.accepted_at acceptedAt,"
              + "r.declined_at declinedAt,r.created_at createdAt,r.updated_at updatedAt,"
              + "f.original_filename fileName,f.file_size_bytes fileSizeBytes,f.mime_type mimeType,"
              + "f.sha256_hash sha256Hash,f.analysis_status analysisStatus,f.source_unit sourceUnit,"
              + "f.width_mm widthMm,f.depth_mm depthMm,f.height_mm heightMm,f.volume_cm3 volumeCm3,"
              + "f.surface_area_cm2 surfaceAreaCm2,f.triangle_count triangleCount,f.analysis_message analysisMessage,"
              + "u.full_name customerName,u.email customerEmail,u.phone customerPhone"
              + " FROM custom_print_requests r"
              + " LEFT JOIN design_files f ON f.design_file_id=r.design_file_id AND f.user_id=r.user_id"
              + " LEFT JOIN users u ON u.user_id=r.user_id LEFT JOIN print_shops s ON s.shop_id=r.shop_id"
              + " WHERE r.custom_request_id=?", requestId);
        if (item == null) return null;
        item.put("history", rows("SELECT h.old_status oldStatus,h.new_status newStatus,h.note,"
                + "h.created_at createdAt,u.full_name actorName FROM custom_print_request_history h"
                + " LEFT JOIN users u ON u.user_id=h.changed_by_user_id"
                + " WHERE h.custom_request_id=? ORDER BY h.created_at,h.history_id", requestId));
        item.put("adminEvents", tableExists("admin_custom_print_review_events")
                ? rows("SELECT e.event_id id,e.action,e.note,e.created_at createdAt,u.full_name adminName"
                    + " FROM admin_custom_print_review_events e LEFT JOIN users u ON u.user_id=e.admin_user_id"
                    + " WHERE e.custom_request_id=? ORDER BY e.event_id DESC LIMIT 50", requestId)
                : List.of());
        return item;
    }

    public static void reviewCustomPrintRequest(long requestId, long adminId, String rawAction, String rawNote) {
        if (!tableExists("admin_custom_print_review_events")) {
            throw new IllegalArgumentException("Chưa có nhật ký rà soát. Hãy chạy migrate_admin_business_queues.sql trước khi xử lý yêu cầu.");
        }
        String action = upper(rawAction);
        if (!List.of("REVIEWED", "ESCALATED").contains(action)) {
            throw new IllegalArgumentException("Thao tác rà soát yêu cầu in 3D không hợp lệ.");
        }
        String note = requiredNote(rawNote, action.equals("ESCALATED") ? "Lý do chuyển theo dõi" : "Ghi chú rà soát");
        inTransaction(conn -> {
            String currentStatus;
            try (PreparedStatement lock = conn.prepareStatement(
                    "SELECT status FROM custom_print_requests WHERE custom_request_id=? FOR UPDATE")) {
                lock.setLong(1, requestId);
                try (ResultSet rs = lock.executeQuery()) {
                    if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy yêu cầu in 3D.");
                    currentStatus = rs.getString(1);
                }
            }
            if (!"PENDING_REVIEW".equalsIgnoreCase(currentStatus)) {
                throw new IllegalArgumentException("Chỉ có thể rà soát yêu cầu in 3D đang chờ xử lý.");
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO admin_custom_print_review_events(custom_request_id,admin_user_id,action,note,created_at)"
                  + " VALUES(?,?,?,?,NOW())")) {
                ps.setLong(1, requestId); ps.setLong(2, adminId); ps.setString(3, action); ps.setString(4, note);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public static List<Map<String, Object>> listOrderChangeRequests(String rawQuery, String rawStatus) {
        StringBuilder sql = new StringBuilder(
                "SELECT r.change_request_id id,r.order_id orderId,o.order_code orderCode,o.status orderStatus,"
              + "o.payment_status paymentStatus,r.order_item_id orderItemId,oi.product_name_snapshot productName,"
              + "r.customer_user_id customerId,u.full_name customerName,u.email customerEmail,"
              + "r.shop_id shopId,s.shop_name shopName,r.request_type requestType,r.reason,r.payload_json payloadJson,"
              + "r.status,r.shop_response shopResponse,r.price_delta priceDelta,r.cancel_fee cancelFee,"
              + "r.customer_confirmed_at customerConfirmedAt,r.expires_at expiresAt,r.created_at createdAt,r.updated_at updatedAt,"
              + "(SELECT e.action FROM admin_order_change_review_events e WHERE e.change_request_id=r.change_request_id"
              + " ORDER BY e.event_id DESC LIMIT 1) adminReviewAction,"
              + "(SELECT e.note FROM admin_order_change_review_events e WHERE e.change_request_id=r.change_request_id"
              + " ORDER BY e.event_id DESC LIMIT 1) adminReviewNote"
              + " FROM order_change_requests r JOIN orders o ON o.order_id=r.order_id"
              + " LEFT JOIN order_items oi ON oi.order_item_id=r.order_item_id"
              + " LEFT JOIN users u ON u.user_id=r.customer_user_id"
              + " LEFT JOIN print_shops s ON s.shop_id=r.shop_id WHERE 1=1");
        List<Object> args = new ArrayList<>();
        String status = filter(rawStatus);
        String query = filter(rawQuery);
        if (status != null) { sql.append(" AND r.status=?"); args.add(status); }
        if ("PENDING".equalsIgnoreCase(status)) {
            sql.append(" AND COALESCE((SELECT e.action FROM admin_order_change_review_events e"
                    + " WHERE e.change_request_id=r.change_request_id ORDER BY e.event_id DESC LIMIT 1),'')<>'REVIEWED'");
        }
        if (query != null) {
            sql.append(" AND (CAST(r.change_request_id AS CHAR) LIKE ? OR o.order_code LIKE ?"
                    + " OR r.request_type LIKE ? OR r.reason LIKE ? OR u.full_name LIKE ? OR u.email LIKE ?)");
            String like = "%" + query + "%";
            for (int i = 0; i < 6; i++) args.add(like);
        }
        sql.append(" ORDER BY CASE r.status WHEN 'PENDING' THEN 0 WHEN 'ACCEPTED' THEN 1 ELSE 2 END,"
                + " r.created_at DESC LIMIT ").append(MAX_ROWS);
        return rows(sql.toString(), args.toArray());
    }

    public static Map<String, Object> orderChangeRequestDetail(long requestId) {
        Map<String, Object> item = one(
                "SELECT r.change_request_id id,r.order_id orderId,o.order_code orderCode,o.status orderStatus,"
              + "o.payment_status paymentStatus,o.total_amount orderTotal,r.order_item_id orderItemId,"
              + "oi.product_name_snapshot productName,r.customer_user_id customerId,u.full_name customerName,"
              + "u.email customerEmail,r.shop_id shopId,s.shop_name shopName,r.request_type requestType,r.reason,"
              + "r.payload_json payloadJson,r.status,r.shop_response shopResponse,r.price_delta priceDelta,"
              + "r.cancel_fee cancelFee,r.customer_confirmed_at customerConfirmedAt,r.expires_at expiresAt,"
              + "r.created_at createdAt,r.updated_at updatedAt"
              + " FROM order_change_requests r JOIN orders o ON o.order_id=r.order_id"
              + " LEFT JOIN order_items oi ON oi.order_item_id=r.order_item_id"
              + " LEFT JOIN users u ON u.user_id=r.customer_user_id LEFT JOIN print_shops s ON s.shop_id=r.shop_id"
              + " WHERE r.change_request_id=?", requestId);
        if (item == null) return null;
        item.put("adminEvents", rows("SELECT e.event_id id,e.action,e.note,e.created_at createdAt,u.full_name adminName"
                + " FROM admin_order_change_review_events e LEFT JOIN users u ON u.user_id=e.admin_user_id"
                + " WHERE e.change_request_id=? ORDER BY e.event_id DESC LIMIT 50", requestId));
        return item;
    }

    public static void actOnOrderChangeRequest(long requestId, long adminId, String rawAction, String rawNote) {
        String action = upper(rawAction);
        if (!List.of("REVIEWED", "REJECTED").contains(action)) {
            throw new IllegalArgumentException("Thao tác yêu cầu thay đổi đơn không hợp lệ.");
        }
        String note = requiredNote(rawNote, action.equals("REJECTED") ? "Lý do từ chối" : "Ghi chú rà soát");
        inTransaction(conn -> {
            long customerId;
            String status;
            String orderCode;
            try (PreparedStatement lock = conn.prepareStatement(
                    "SELECT r.customer_user_id,r.status,o.order_code FROM order_change_requests r"
                  + " JOIN orders o ON o.order_id=r.order_id WHERE r.change_request_id=? FOR UPDATE")) {
                lock.setLong(1, requestId);
                try (ResultSet rs = lock.executeQuery()) {
                    if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy yêu cầu thay đổi đơn.");
                    customerId = rs.getLong(1); status = rs.getString(2); orderCode = rs.getString(3);
                }
            }
            if (!"PENDING".equalsIgnoreCase(status)) {
                throw new IllegalArgumentException("Chỉ có thể rà soát hoặc từ chối yêu cầu đang chờ xử lý.");
            }
            if ("REJECTED".equals(action)) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE order_change_requests SET status='REJECTED',shop_response=?,expires_at=NULL,updated_at=NOW()"
                      + " WHERE change_request_id=? AND status='PENDING'")) {
                    ps.setString(1, clip("Admin từ chối: " + note, 1000)); ps.setLong(2, requestId);
                    if (ps.executeUpdate() != 1) throw new IllegalArgumentException("Yêu cầu đã được cập nhật bởi người khác.");
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO admin_order_change_review_events(change_request_id,admin_user_id,action,note,created_at)"
                  + " VALUES(?,?,?,?,NOW())")) {
                ps.setLong(1, requestId); ps.setLong(2, adminId); ps.setString(3, action); ps.setString(4, note);
                ps.executeUpdate();
            }
            if ("REJECTED".equals(action) && customerId > 0) {
                NotificationService.notifyUser(conn, customerId, "ORDER_CHANGE_REQUEST",
                        "Yêu cầu thay đổi đơn bị từ chối",
                        "Yêu cầu #" + requestId + " cho đơn " + safe(orderCode) + " đã bị Admin từ chối: " + note,
                        "/purchase-history", "ORDER_CHANGE_REQUEST", requestId);
            }
            return null;
        });
    }

    public static Map<String, Object> listReviews(String rawQuery, String rawStatus, int requestedPage, int requestedSize) {
        String select = "SELECT r.review_id id,r.order_item_id orderItemId,r.user_id userId,u.full_name customerName,"
              + "u.email customerEmail,r.product_id productId,p.product_name productName,r.shop_id shopId,"
              + "s.shop_name shopName,r.rating,r.review_text reviewText,r.is_published isPublished,r.created_at createdAt";
        StringBuilder from = new StringBuilder(" FROM reviews r LEFT JOIN users u ON u.user_id=r.user_id"
              + " LEFT JOIN products p ON p.product_id=r.product_id"
              + " LEFT JOIN print_shops s ON s.shop_id=r.shop_id WHERE 1=1");
        List<Object> args = new ArrayList<>();
        String status = filter(rawStatus);
        String query = filter(rawQuery);
        if ("PUBLISHED".equals(status)) from.append(" AND COALESCE(r.is_published,0)=1");
        else if ("UNPUBLISHED".equals(status)) from.append(" AND COALESCE(r.is_published,0)=0");
        if (query != null) {
            from.append(" AND (CAST(r.review_id AS CHAR) LIKE ? OR p.product_name LIKE ? OR r.review_text LIKE ?"
                    + " OR u.full_name LIKE ? OR u.email LIKE ?)");
            String like = "%" + query + "%";
            for (int i = 0; i < 5; i++) args.add(like);
        }
        Map<String, Object> countRow = one("SELECT COUNT(*) total" + from, args.toArray());
        long total = countRow == null || !(countRow.get("total") instanceof Number n) ? 0 : n.longValue();
        int size = Math.min(Math.max(1, requestedSize), 100);
        int pages = Math.max(1, (int) Math.ceil((double) total / size));
        int page = Math.min(Math.max(1, requestedPage), pages);
        List<Object> dataArgs = new ArrayList<>(args);
        dataArgs.add(size);
        dataArgs.add((long) (page - 1) * size);
        List<Map<String, Object>> items = rows(select + from
                + " ORDER BY COALESCE(r.is_published,0),r.created_at DESC,r.review_id DESC LIMIT ? OFFSET ?",
                dataArgs.toArray());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", size);
        result.put("pages", pages);
        return result;
    }

    public static Map<String, Object> reviewDetail(long reviewId) {
        Map<String, Object> item = one(
                "SELECT r.review_id id,r.order_item_id orderItemId,r.user_id userId,u.full_name customerName,"
              + "u.email customerEmail,r.product_id productId,p.product_name productName,r.shop_id shopId,"
              + "s.shop_name shopName,r.rating,r.review_text reviewText,r.is_published isPublished,r.created_at createdAt"
              + " FROM reviews r LEFT JOIN users u ON u.user_id=r.user_id"
              + " LEFT JOIN products p ON p.product_id=r.product_id"
              + " LEFT JOIN print_shops s ON s.shop_id=r.shop_id WHERE r.review_id=?", reviewId);
        if (item == null) return null;
        item.put("events", rows("SELECT e.event_id id,e.action,e.reason,e.created_at createdAt,u.full_name adminName"
                + " FROM admin_review_moderation_events e LEFT JOIN users u ON u.user_id=e.admin_user_id"
                + " WHERE e.review_id=? ORDER BY e.event_id DESC LIMIT 50", reviewId));
        return item;
    }

    public static void moderateReview(long reviewId, long adminId, String rawAction, String rawReason) {
        String action = upper(rawAction);
        if (!List.of("PUBLISH", "UNPUBLISH").contains(action)) {
            throw new IllegalArgumentException("Thao tác kiểm duyệt đánh giá không hợp lệ.");
        }
        String reason = filter(rawReason);
        if ("UNPUBLISH".equals(action)) reason = requiredNote(reason, "Lý do ẩn đánh giá");
        else if (reason != null && reason.length() > 1000) {
            throw new IllegalArgumentException("Ghi chú tối đa 1000 ký tự.");
        }
        final String finalReason = reason;
        inTransaction(conn -> {
            long userId;
            boolean currentlyPublished;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT user_id,is_published FROM reviews WHERE review_id=? FOR UPDATE")) {
                ps.setLong(1, reviewId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy đánh giá.");
                    long raw = rs.getLong(1); userId = rs.wasNull() ? 0 : raw;
                    currentlyPublished = rs.getBoolean(2);
                }
            }
            boolean publish = "PUBLISH".equals(action);
            if (currentlyPublished == publish) {
                throw new IllegalArgumentException(publish ? "Đánh giá đã được công khai." : "Đánh giá đã bị ẩn.");
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE reviews SET is_published=? WHERE review_id=?")) {
                ps.setBoolean(1, publish); ps.setLong(2, reviewId); ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO admin_review_moderation_events(review_id,admin_user_id,action,reason,created_at)"
                  + " VALUES(?,?,?,?,NOW())")) {
                ps.setLong(1, reviewId); ps.setLong(2, adminId); ps.setString(3, action); ps.setString(4, finalReason);
                ps.executeUpdate();
            }
            if (userId > 0) {
                NotificationService.notifyUser(conn, userId, "REVIEW_MODERATION",
                        publish ? "Đánh giá đã được công khai" : "Đánh giá đã bị ẩn",
                        publish ? "Đánh giá của bạn đã được Admin duyệt và công khai."
                                : "Đánh giá của bạn đã bị ẩn. Lý do: " + safe(finalReason),
                        "/purchase-history", "REVIEW", reviewId);
            }
            return null;
        });
    }

    public static List<Map<String, Object>> listPaymentReconciliation(String rawQuery, String rawStatus) {
        StringBuilder sql = new StringBuilder(paymentSelect());
        List<Object> args = new ArrayList<>();
        String status = rawStatus != null && "ALL".equalsIgnoreCase(rawStatus.trim())
                ? "ALL" : filter(rawStatus);
        String query = filter(rawQuery);
        if (status == null || "OPEN".equalsIgnoreCase(status)) {
            sql.append(" AND (UPPER(COALESCE(p.status,'')) IN ('REVIEW_REQUIRED','REFUND_REQUIRED')"
                    + " OR (UPPER(COALESCE(p.status,'')) IN ('PENDING','SUPERSEDED')"
                    + " AND p.created_at<=DATE_SUB(NOW(),INTERVAL 30 MINUTE))"
                    + " OR (UPPER(COALESCE(o.payment_status,'')) IN ('PAID','SUCCESS','COMPLETED')"
                    + " AND UPPER(COALESCE(p.status,'')) IN ('PENDING','SUPERSEDED','FAILED')))"
                    + " AND COALESCE(last_event.outcome,'OPEN')<>'RECONCILED'");
        } else if (!"ALL".equalsIgnoreCase(status)) {
            sql.append(" AND UPPER(COALESCE(p.status,''))=?");
            args.add(status);
        }
        if (query != null) {
            sql.append(" AND (CAST(p.payment_id AS CHAR) LIKE ? OR o.order_code LIKE ? OR"
                    + " p.provider_transaction_ref LIKE ? OR p.provider_transaction_no LIKE ? OR"
                    + " u.full_name LIKE ? OR u.email LIKE ?)");
            String like = "%" + query + "%";
            for (int i = 0; i < 6; i++) args.add(like);
        }
        sql.append(" ORDER BY CASE WHEN UPPER(COALESCE(p.status,'')) IN ('REVIEW_REQUIRED','REFUND_REQUIRED') THEN 0"
                + " WHEN UPPER(COALESCE(o.payment_status,'')) IN ('PAID','SUCCESS','COMPLETED') THEN 1 ELSE 2 END,"
                + " p.created_at ASC LIMIT ").append(MAX_ROWS);
        return rows(sql.toString(), args.toArray());
    }

    public static Map<String, Object> paymentReconciliationDetail(long paymentId) {
        Map<String, Object> item = one(paymentSelect()
                + " AND p.payment_id=?", paymentId);
        if (item == null) return null;
        item.put("events", rows("SELECT e.event_id id,e.outcome,e.note,e.provider_reference providerReference,"
                + "e.created_at createdAt,u.full_name adminName FROM admin_payment_reconciliation_events e"
                + " LEFT JOIN users u ON u.user_id=e.admin_user_id WHERE e.payment_id=?"
                + " ORDER BY e.event_id DESC LIMIT 100", paymentId));
        return item;
    }

    public static long countOpenPaymentReconciliation() {
        Map<String, Object> result = one("SELECT COUNT(*) total FROM payments p JOIN orders o ON o.order_id=p.order_id"
                + " LEFT JOIN admin_payment_reconciliation_events last_event ON last_event.event_id=("
                + "SELECT MAX(e.event_id) FROM admin_payment_reconciliation_events e WHERE e.payment_id=p.payment_id)"
                + " WHERE (UPPER(COALESCE(p.status,'')) IN ('REVIEW_REQUIRED','REFUND_REQUIRED')"
                + " OR (UPPER(COALESCE(p.status,'')) IN ('PENDING','SUPERSEDED')"
                + " AND p.created_at<=DATE_SUB(NOW(),INTERVAL 30 MINUTE))"
                + " OR (UPPER(COALESCE(o.payment_status,'')) IN ('PAID','SUCCESS','COMPLETED')"
                + " AND UPPER(COALESCE(p.status,'')) IN ('PENDING','SUPERSEDED','FAILED')))"
                + " AND COALESCE(last_event.outcome,'OPEN')<>'RECONCILED'");
        Object total = result == null ? 0 : result.get("total");
        return total instanceof Number n ? n.longValue() : 0L;
    }

    public static void recordPaymentReconciliation(long paymentId, long adminId,
                                                   String rawOutcome, String rawNote, String rawReference) {
        String outcome = upper(rawOutcome);
        if (!List.of("RECONCILED", "FOLLOW_UP").contains(outcome)) {
            throw new IllegalArgumentException("Kết quả đối soát không hợp lệ.");
        }
        String note = requiredNote(rawNote, outcome.equals("RECONCILED") ? "Ghi chú đối soát" : "Lý do cần theo dõi");
        String reference = filter(rawReference);
        if (reference != null && reference.length() > 120) throw new IllegalArgumentException("Mã tham chiếu tối đa 120 ký tự.");
        inTransaction(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("SELECT payment_id,order_id FROM payments WHERE payment_id=? FOR UPDATE")) {
                ps.setLong(1, paymentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy giao dịch thanh toán.");
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO admin_payment_reconciliation_events(payment_id,admin_user_id,outcome,note,provider_reference,created_at)"
                  + " VALUES(?,?,?,?,?,NOW())")) {
                ps.setLong(1, paymentId); ps.setLong(2, adminId); ps.setString(3, outcome); ps.setString(4, note);
                ps.setString(5, reference); ps.executeUpdate();
            }
            return null;
        });
    }

    private static String paymentSelect() {
        return "SELECT p.payment_id id,p.order_id orderId,o.order_code orderCode,o.status orderStatus,"
             + "o.payment_status orderPaymentStatus,o.total_amount orderTotal,o.user_id customerId,"
             + "u.full_name customerName,u.email customerEmail,p.payment_method paymentMethod,p.provider,"
             + "p.provider_transaction_ref providerTransactionRef,p.provider_transaction_no providerTransactionNo,"
             + "p.response_code responseCode,p.bank_code bankCode,p.card_type cardType,p.amount,p.status,"
             + "p.failure_reason failureReason,p.created_at createdAt,p.updated_at updatedAt,p.paid_at paidAt,"
             + "last_event.outcome lastOutcome,last_event.note lastNote,last_event.provider_reference lastReference,"
             + "last_event.created_at lastReviewedAt"
             + " FROM payments p JOIN orders o ON o.order_id=p.order_id"
             + " LEFT JOIN users u ON u.user_id=o.user_id"
             + " LEFT JOIN admin_payment_reconciliation_events last_event ON last_event.event_id=("
             + "SELECT MAX(e.event_id) FROM admin_payment_reconciliation_events e WHERE e.payment_id=p.payment_id)"
             + " WHERE 1=1";
    }

    private interface SqlWork<T> { T run(Connection connection) throws SQLException; }

    private static <T> T inTransaction(SqlWork<T> work) {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                T result = work.run(conn);
                conn.commit();
                return result;
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

    private static List<Map<String, Object>> rows(String sql, Object... args) {
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, args);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        Object value = rs.getObject(i);
                        if (value instanceof Timestamp timestamp) value = timestamp.toLocalDateTime().toString().replace('T', ' ');
                        row.put(meta.getColumnLabel(i), value);
                    }
                    result.add(row);
                }
            }
            return result;
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    private static Map<String, Object> one(String sql, Object... args) {
        List<Map<String, Object>> found = rows(sql, args);
        return found.isEmpty() ? null : found.get(0);
    }

    private static boolean tableExists(String tableName) {
        Map<String, Object> result = one("SELECT COUNT(*) total FROM information_schema.tables"
                + " WHERE table_schema=DATABASE() AND table_name=?", tableName);
        Object total = result == null ? null : result.get("total");
        return total instanceof Number number && number.longValue() > 0;
    }

    private static void bind(PreparedStatement ps, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
    }

    private static String filter(String raw) {
        if (raw == null || raw.isBlank() || "ALL".equalsIgnoreCase(raw.trim())) return null;
        String value = raw.trim();
        if (value.length() > 200) throw new IllegalArgumentException("Từ khóa tối đa 200 ký tự.");
        return value;
    }

    private static String upper(String raw) {
        return raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
    }

    private static String requiredNote(String raw, String label) {
        String value = raw == null ? "" : raw.trim();
        if (value.length() < 3 || value.length() > 1000) {
            throw new IllegalArgumentException(label + " phải có từ 3 đến 1000 ký tự.");
        }
        return value;
    }

    private static String clip(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "không có mã đơn" : value;
    }
}
