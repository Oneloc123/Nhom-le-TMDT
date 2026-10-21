package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.OrderStatus;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.ProductModeration;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.AdminAccountPolicy;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Database-backed read and write operations used only by the Admin MVP. */
public final class AdminDao {
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    private static final List<String> MODERATION_STATES =
            List.of("PENDING_REVIEW", "APPROVED", "REJECTED", "HIDDEN");
    /** Allow-list vai trò mà admin có thể gán (không nhận role tự do từ request). */
    private static final List<String> ASSIGNABLE_ROLES =
            List.of(User.ROLE_ADMIN, User.ROLE_CUSTOMER);

    private AdminDao() { }

    public static Map<String, Object> dashboard() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("users", one("SELECT COUNT(*) total,"
                + " SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END) active"
                + " FROM users"));
        result.put("sellers", one("SELECT COUNT(*) total,"
                + " SUM(CASE WHEN verification_status = 'PENDING' THEN 1 ELSE 0 END) pendingVerification,"
                + " SUM(CASE WHEN status IN ('INACTIVE','BLOCKED') THEN 1 ELSE 0 END) restricted"
                + " FROM print_shops"));
        result.put("pendingSellers", rows("SELECT s.shop_id id, s.shop_name name, s.province, s.district,"
                + " u.full_name ownerName, u.email ownerEmail, s.created_at createdAt"
                + " FROM print_shops s LEFT JOIN users u ON u.user_id = s.owner_user_id"
                + " WHERE s.verification_status = 'PENDING'"
                + " ORDER BY s.created_at, s.shop_id LIMIT 8"));
        result.put("products", one("SELECT COUNT(*) total,"
                + " SUM(CASE WHEN moderation_status = 'PENDING_REVIEW' THEN 1 ELSE 0 END) pendingReview"
                + " FROM products"));
        result.put("orders", one("SELECT COUNT(*) total FROM orders"));
        result.put("orderStatuses", rows("SELECT status, COUNT(*) count FROM orders"
                + " GROUP BY status ORDER BY status"));
        result.put("recentOrders", rows("SELECT o.order_id id, o.order_code code, o.status,"
                + " o.payment_status paymentStatus, o.total_amount totalAmount, o.currency,"
                + " o.placed_at placedAt, u.full_name customerName, u.email customerEmail"
                + " FROM orders o LEFT JOIN users u ON u.user_id = o.user_id"
                + " ORDER BY o.placed_at DESC, o.order_id DESC LIMIT 8"));
        result.put("pendingProducts", rows("SELECT p.product_id id, p.product_name name,"
                + " c.category_name category, p.created_at createdAt,"
                + " MIN(sp.unit_price) minPrice, GROUP_CONCAT(DISTINCT s.shop_name"
                + " ORDER BY s.shop_name SEPARATOR ', ') shops"
                + " FROM products p LEFT JOIN categories c ON c.category_id = p.category_id"
                + " LEFT JOIN shop_products sp ON sp.product_id = p.product_id"
                + " LEFT JOIN print_shops s ON s.shop_id = sp.shop_id"
                + " WHERE p.moderation_status = 'PENDING_REVIEW'"
                + " GROUP BY p.product_id, c.category_name, p.product_name, p.created_at"
                + " ORDER BY p.created_at, p.product_id LIMIT 8"));
        result.put("pendingQuotes", rows(quoteSql() + " WHERE q.status = ?"
                + " ORDER BY q.quote_request_id DESC LIMIT 8", "Chờ xử lý"));
        return result;
    }

    /**
     * Danh sách người dùng cho admin: tìm kiếm (id/họ tên/email/SĐT), lọc theo
     * vai trò, trạng thái, xác thực email, trạng thái xoá mềm, khoảng ngày đăng ký
     * và sắp xếp với ALLOW-LIST cột (không nối chuỗi người dùng vào ORDER BY).
     * Chỉ trả các cột an toàn — không bao giờ đọc password_hash/OTP/token.
     */
    public static Map<String, Object> listUsers(String query, String role, String status,
                                                 String verified, String deleted,
                                                 String createdFrom, String createdTo,
                                                 String sort, int page, int pageSize) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        appendSearch(where, args, query, "CAST(u.user_id AS CHAR)", "u.full_name", "u.email", "u.phone");
        if (notAll(role)) {
            requireOneOf(role, List.of("ADMIN", "CUSTOMER"), "Vai trò không hợp lệ.");
            where.append(" AND u.role = ?"); args.add(role);
        }
        if (notAll(status)) {
            requireOneOf(status, List.of("ACTIVE", "LOCKED", "PENDING"), "Trạng thái người dùng không hợp lệ.");
            where.append(" AND u.status = ?"); args.add(status);
        }
        if (notAll(verified)) {
            requireOneOf(verified, List.of("1", "0"), "Bộ lọc xác thực email không hợp lệ.");
            where.append(" AND u.email_verified = ?"); args.add(Integer.parseInt(verified));
        }
        if (notAll(deleted)) {
            if ("yes".equalsIgnoreCase(deleted)) where.append(" AND u.deleted_at IS NOT NULL");
            else if ("no".equalsIgnoreCase(deleted)) where.append(" AND u.deleted_at IS NULL");
            else throw new IllegalArgumentException("Bộ lọc tài khoản đã xoá không hợp lệ.");
        }
        if (notAll(createdFrom)) {
            where.append(" AND u.created_at >= ?");
            args.add(parseDate(createdFrom, "Ngày bắt đầu"));
        }
        if (notAll(createdTo)) {
            where.append(" AND u.created_at < DATE_ADD(?, INTERVAL 1 DAY)");
            args.add(parseDate(createdTo, "Ngày kết thúc"));
        }
        // ALLOW-LIST sắp xếp: chỉ nhận các khoá đã định nghĩa, không nối input thô.
        String orderBy = switch (sort == null ? "" : sort.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "", "id", "newest" -> "u.user_id DESC";
            case "oldest" -> "u.user_id ASC";
            case "name" -> "u.full_name ASC, u.user_id DESC";
            case "email" -> "u.email ASC, u.user_id DESC";
            case "lastlogin" -> "u.last_login_at DESC, u.user_id DESC";
            default -> throw new IllegalArgumentException("Trường sắp xếp không hợp lệ.");
        };
        String from = " FROM users u";
        String select = "SELECT u.user_id id, u.full_name fullName, u.email, u.phone,"
                + " u.role, u.status, u.created_at createdAt, u.updated_at updatedAt,"
                + " u.provider provider, u.email_verified emailVerified,"
                + " u.last_login_at lastLoginAt, u.deleted_at deletedAt,"
                + " (SELECT COUNT(*) FROM orders o WHERE o.user_id = u.user_id) orderCount,"
                + " (SELECT COUNT(*) FROM addresses a WHERE a.user_id = u.user_id) addressCount";
        return page(select + from + where + " ORDER BY " + orderBy,
                "SELECT COUNT(*)" + from + where, args, page, pageSize);
    }

    /**
     * Chi tiết người dùng cho admin: hồ sơ (cột an toàn), địa chỉ, đếm
     * đơn/yêu thích/thiết kế, và lịch sử sự kiện bảo mật gần nhất.
     * KHÔNG trả password hash, OTP, reset token hay session id.
     */
    public static Optional<Map<String, Object>> userDetail(long id) {
        Optional<Map<String, Object>> user = findOne("SELECT u.user_id id, u.full_name fullName,"
                + " u.email, u.phone, u.role, u.status, u.created_at createdAt,"
                + " u.updated_at updatedAt, u.provider provider,"
                + " u.email_verified emailVerified, u.last_login_at lastLoginAt,"
                + " u.deleted_at deletedAt, u.failed_login_attempts failedLoginAttempts"
                + " FROM users u WHERE u.user_id = ?", id);
        user.ifPresent(row -> {
            row.put("recentOrders", rows("SELECT order_id id, order_code code,"
                    + " status, payment_status paymentStatus, total_amount totalAmount, currency, placed_at placedAt"
                    + " FROM orders WHERE user_id = ? ORDER BY placed_at DESC, order_id DESC LIMIT 10", id));
            row.put("addresses", rows("SELECT address_id id, recipient_name recipientName,"
                    + " recipient_phone recipientPhone, province, district, address_line addressLine,"
                    + " is_default isDefault FROM addresses WHERE user_id = ?"
                    + " ORDER BY is_default DESC, address_id", id));
            Map<String, Object> counts = new LinkedHashMap<>();
            counts.put("orders", scalar("SELECT COUNT(*) FROM orders WHERE user_id = ?", id));
            counts.put("favorites", scalar("SELECT COUNT(*) FROM favorites WHERE user_id = ?", id));
            counts.put("savedDesigns",
                    scalar("SELECT COUNT(*) FROM saved_custom_designs WHERE user_id = ?", id));
            counts.put("addresses", scalar("SELECT COUNT(*) FROM addresses WHERE user_id = ?", id));
            row.put("counts", counts);
            // Nhật ký bảo mật đã được lọc an toàn trong SecurityEventDao.
            row.put("securityEvents", SecurityEventDao.recentForUser(id, 20));
        });
        return user;
    }

    public static void updateUserProfile(long id, String fullName, String phone) {
        String name = normalized(fullName, 120, "Họ tên");
        String normalizedPhone = phone == null ? "" : phone.trim();
        if (normalizedPhone.length() > 25 || (!normalizedPhone.isEmpty()
                && !normalizedPhone.matches("[+0-9() .-]{7,25}"))) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ.");
        }
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE users SET full_name = ?, phone = ? WHERE user_id = ?")) {
            ps.setString(1, name); ps.setString(2, normalizedPhone); ps.setLong(3, id);
            if (ps.executeUpdate() != 1) throw new IllegalArgumentException("Không tìm thấy người dùng.");
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static void changeUserStatus(long targetId, long actorId, String nextStatus) {
        requireOneOf(nextStatus, List.of(User.STATUS_ACTIVE, User.STATUS_LOCKED), "Trạng thái người dùng không hợp lệ.");
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String role;
                String currentStatus;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT role, status FROM users WHERE user_id = ? FOR UPDATE")) {
                    ps.setLong(1, targetId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy người dùng.");
                        role = rs.getString("role"); currentStatus = rs.getString("status");
                    }
                }
                if (nextStatus.equals(currentStatus)) throw new IllegalArgumentException("Tài khoản đã ở trạng thái này.");
                if (User.ROLE_ADMIN.equals(role) && User.STATUS_LOCKED.equals(nextStatus)) {
                    int activeAdmins = 2;
                    if (User.STATUS_ACTIVE.equals(currentStatus)) {
                        activeAdmins = 0;
                        try (PreparedStatement ps = conn.prepareStatement(
                                "SELECT user_id FROM users WHERE role = 'ADMIN' AND status = 'ACTIVE' FOR UPDATE");
                             ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) activeAdmins++;
                        }
                    }
                    AdminAccountPolicy.assertCanDisable(targetId, actorId, activeAdmins);
                }
                try (PreparedStatement ps = conn.prepareStatement("UPDATE users SET status = ? WHERE user_id = ?")) {
                    ps.setString(1, nextStatus); ps.setLong(2, targetId);
                    if (ps.executeUpdate() != 1) throw new IllegalArgumentException("Không tìm thấy người dùng.");
                }
                if (User.STATUS_ACTIVE.equals(nextStatus)) {
                    // Mở khóa đồng nghĩa xoá cờ khóa tạm / bộ đếm đăng nhập sai.
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE users SET failed_login_attempts = 0, locked_until = NULL"
                                    + " WHERE user_id = ?")) {
                        ps.setLong(1, targetId); ps.executeUpdate();
                    }
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                rollback(conn, e); throw e;
            } finally { conn.setAutoCommit(true); }
        } catch (SQLException e) { throw DataAccessException.of(e); }
        // Audit: ghi hành động + đối tượng, KHÔNG ghi mật khẩu/OTP/token.
        SecurityEventDao.record(targetId, actorId,
                SecurityEventDao.EVENT_ADMIN_STATUS_CHANGED,
                "Admin đổi trạng thái tài khoản sang " + nextStatus);
    }

    /**
     * Đổi vai trò theo ALLOW-LIST (ADMIN/CUSTOMER) trong transaction, có bảo vệ
     * “không hạ quyền admin cuối cùng đang hoạt động” và ghi audit.
     */
    public static void changeUserRole(long targetId, long actorId, String nextRole) {
        requireOneOf(nextRole, ASSIGNABLE_ROLES, "Vai trò không hợp lệ.");
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String currentRole;
                String currentStatus;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT role, status FROM users WHERE user_id = ? FOR UPDATE")) {
                    ps.setLong(1, targetId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy người dùng.");
                        currentRole = rs.getString("role");
                        currentStatus = rs.getString("status");
                    }
                }
                if (nextRole.equals(currentRole)) {
                    throw new IllegalArgumentException("Người dùng đã ở vai trò này.");
                }
                boolean demotingActiveAdmin = User.ROLE_ADMIN.equals(currentRole)
                        && User.STATUS_ACTIVE.equals(currentStatus);
                if (demotingActiveAdmin) {
                    int otherActiveAdmins = 0;
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT user_id FROM users WHERE role = 'ADMIN' AND status = 'ACTIVE'"
                                    + " AND deleted_at IS NULL FOR UPDATE");
                         ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            if (rs.getLong("user_id") != targetId) otherActiveAdmins++;
                        }
                    }
                    if (otherActiveAdmins == 0) {
                        throw new IllegalArgumentException(
                                "Không thể hạ quyền quản trị viên cuối cùng đang hoạt động.");
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE users SET role = ?, updated_at = NOW() WHERE user_id = ?")) {
                    ps.setString(1, nextRole); ps.setLong(2, targetId); ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                rollback(conn, e); throw e;
            } finally { conn.setAutoCommit(true); }
        } catch (SQLException e) { throw DataAccessException.of(e); }
        SecurityEventDao.record(targetId, actorId, SecurityEventDao.EVENT_ADMIN_ROLE_CHANGED,
                "Admin đổi vai trò tài khoản sang " + nextRole);
    }

    /**
     * Xoá MỀM tài khoản (đặt deleted_at) — giữ nguyên mọi dữ liệu đơn hàng,
     * thanh toán, đánh giá và lịch sử. Không cho tự xoá mình hay xoá admin
     * cuối cùng. Có thể đảo ngược bằng {@link #restoreUser}.
     */
    public static void softDeleteUser(long targetId, long actorId) {
        if (targetId == actorId) {
            throw new IllegalArgumentException("Bạn không thể xoá tài khoản đang đăng nhập.");
        }
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String role;
                java.sql.Timestamp deletedAt;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT role, deleted_at FROM users WHERE user_id = ? FOR UPDATE")) {
                    ps.setLong(1, targetId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy người dùng.");
                        role = rs.getString("role");
                        deletedAt = rs.getTimestamp("deleted_at");
                    }
                }
                if (deletedAt != null) throw new IllegalArgumentException("Tài khoản đã bị xoá.");
                if (User.ROLE_ADMIN.equals(role)) {
                    int otherActiveAdmins = 0;
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT user_id FROM users WHERE role = 'ADMIN' AND status = 'ACTIVE'"
                                    + " AND deleted_at IS NULL FOR UPDATE");
                         ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            if (rs.getLong("user_id") != targetId) otherActiveAdmins++;
                        }
                    }
                    if (otherActiveAdmins == 0) {
                        throw new IllegalArgumentException(
                                "Không thể xoá quản trị viên cuối cùng đang hoạt động.");
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE users SET deleted_at = NOW(), updated_at = NOW() WHERE user_id = ?")) {
                    ps.setLong(1, targetId); ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                rollback(conn, e); throw e;
            } finally { conn.setAutoCommit(true); }
        } catch (SQLException e) { throw DataAccessException.of(e); }
        SecurityEventDao.record(targetId, actorId, SecurityEventDao.EVENT_ADMIN_SOFT_DELETE,
                "Admin xoá mềm tài khoản (dữ liệu đơn hàng được giữ nguyên)");
    }

    /** Khôi phục tài khoản đã xoá mềm — đặt lại deleted_at = NULL. */
    public static void restoreUser(long targetId, long actorId) {
        int updated;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE users SET deleted_at = NULL, updated_at = NOW()"
                             + " WHERE user_id = ? AND deleted_at IS NOT NULL")) {
            ps.setLong(1, targetId);
            updated = ps.executeUpdate();
        } catch (SQLException e) { throw DataAccessException.of(e); }
        if (updated != 1) {
            throw new IllegalArgumentException("Tài khoản chưa bị xoá hoặc không tồn tại.");
        }
        SecurityEventDao.record(targetId, actorId, SecurityEventDao.EVENT_ADMIN_RESTORE,
                "Admin khôi phục tài khoản đã xoá mềm");
    }

    public static Map<String, Object> listSellers(String query, String status, String verification,
                                                   int page, int pageSize) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        appendSearch(where, args, query, "CAST(s.shop_id AS CHAR)", "s.shop_name", "u.full_name",
                "u.email", "s.province", "s.district");
        if (notAll(status)) { requireOneOf(status, List.of("ACTIVE", "INACTIVE", "BLOCKED"), "Trạng thái xưởng không hợp lệ."); where.append(" AND s.status = ?"); args.add(status); }
        if (notAll(verification)) { requireOneOf(verification, List.of("PENDING", "VERIFIED", "REJECTED"), "Trạng thái xác minh không hợp lệ."); where.append(" AND s.verification_status = ?"); args.add(verification); }
        String from = " FROM print_shops s LEFT JOIN users u ON u.user_id = s.owner_user_id";
        String select = "SELECT s.shop_id id, s.owner_user_id ownerUserId, s.shop_name name,"
                + " u.full_name ownerName, u.email ownerEmail, u.phone ownerPhone,"
                + " s.contact_phone contactPhone, s.province, s.district, s.description, s.search_keywords specialties,"
                + " s.average_rating rating, s.verification_status verificationStatus, s.status, s.created_at createdAt,"
                + " (SELECT COUNT(DISTINCT sp.product_id) FROM shop_products sp WHERE sp.shop_id = s.shop_id) productCount,"
                + " (SELECT COUNT(DISTINCT oi.order_id) FROM order_items oi WHERE oi.shop_id = s.shop_id) orderCount";
        return page(select + from + where + " ORDER BY s.shop_id DESC",
                "SELECT COUNT(*)" + from + where, args, page, pageSize);
    }

    public static Optional<Map<String, Object>> sellerDetail(long id) {
        Optional<Map<String, Object>> seller = findOne("SELECT s.shop_id id, s.owner_user_id ownerUserId,"
                + " s.shop_name name, s.slug, s.description, s.logo_url logoUrl,"
                + " s.contact_phone contactPhone, s.province, s.district, s.search_keywords specialties,"
                + " s.average_rating rating, s.verification_status verificationStatus, s.status, s.created_at createdAt,"
                + " u.full_name ownerName, u.email ownerEmail, u.phone ownerPhone, u.status ownerStatus"
                + " FROM print_shops s LEFT JOIN users u ON u.user_id = s.owner_user_id WHERE s.shop_id = ?", id);
        seller.ifPresent(row -> {
            row.put("products", rows("SELECT p.product_id id, p.product_name name, p.moderation_status moderationStatus,"
                    + " p.is_active active, sp.unit_price price, sp.material, sp.printing_method printingMethod"
                    + " FROM shop_products sp JOIN products p ON p.product_id = sp.product_id"
                    + " WHERE sp.shop_id = ? ORDER BY p.product_name, sp.shop_product_id LIMIT 50", id));
            row.put("productCount", scalar("SELECT COUNT(DISTINCT product_id) FROM shop_products WHERE shop_id = ?", id));
            row.put("orderCount", scalar("SELECT COUNT(DISTINCT order_id) FROM order_items WHERE shop_id = ?", id));
        });
        return seller;
    }

    public static void changeSellerStatus(long shopId, String nextStatus) {
        requireOneOf(nextStatus, List.of("ACTIVE", "INACTIVE", "BLOCKED"), "Trạng thái xưởng không hợp lệ.");
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String current; String verification;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT status, verification_status FROM print_shops WHERE shop_id = ? FOR UPDATE")) {
                    ps.setLong(1, shopId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy xưởng in.");
                        current = rs.getString("status"); verification = rs.getString("verification_status");
                    }
                }
                if (nextStatus.equals(current)) throw new IllegalArgumentException("Xưởng đã ở trạng thái này.");
                if ("ACTIVE".equals(nextStatus) && !"VERIFIED".equals(verification)) {
                    throw new IllegalArgumentException("Chỉ xưởng đã xác minh mới có thể hoạt động.");
                }
                try (PreparedStatement ps = conn.prepareStatement("UPDATE print_shops SET status = ? WHERE shop_id = ?")) {
                    ps.setString(1, nextStatus); ps.setLong(2, shopId); ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) { rollback(conn, e); throw e; }
            finally { conn.setAutoCommit(true); }
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static void verifySeller(long shopId) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE print_shops SET verification_status = 'VERIFIED'"
                     + " WHERE shop_id = ? AND verification_status = 'PENDING'")) {
            ps.setLong(1, shopId);
            if (ps.executeUpdate() != 1) throw new IllegalArgumentException("Chỉ xưởng đang chờ xác minh mới được duyệt.");
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static Map<String, Object> listProducts(String query, String moderation, String category,
                                                    String shop, int page, int pageSize) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        appendSearch(where, args, query, "CAST(p.product_id AS CHAR)", "p.product_name", "p.description", "p.brand");
        if (notAll(moderation)) {
            requireOneOf(moderation, MODERATION_STATES, "Trạng thái kiểm duyệt không hợp lệ.");
            if ("HIDDEN".equals(moderation)) where.append(" AND p.moderation_status = 'APPROVED' AND p.is_active = 0");
            else { where.append(" AND p.moderation_status = ?"); args.add(moderation); }
        }
        if (notAll(category)) { where.append(" AND p.category_id = ?"); args.add(parsePositiveId(category, "Danh mục")); }
        if (notAll(shop)) { where.append(" AND s.shop_id = ?"); args.add(parsePositiveId(shop, "Xưởng")); }
        String joins = " FROM products p LEFT JOIN categories c ON c.category_id = p.category_id"
                + " LEFT JOIN shop_products sp ON sp.product_id = p.product_id"
                + " LEFT JOIN print_shops s ON s.shop_id = sp.shop_id";
        String select = "SELECT p.product_id id, p.product_name name, p.description, p.brand,"
                + " p.category_id categoryId, c.category_name category, p.is_customizable customizable,"
                + " p.is_active active, p.moderation_status moderationStatus, p.rejection_reason rejectionReason,"
                + " p.reviewed_by_user_id reviewedBy, p.reviewed_at reviewedAt, p.created_at createdAt,"
                + " MIN(sp.unit_price) minPrice, GROUP_CONCAT(DISTINCT s.shop_name ORDER BY s.shop_name SEPARATOR ', ') shops";
        return page(select + joins + where + " GROUP BY p.product_id, c.category_name ORDER BY p.created_at DESC, p.product_id DESC",
                "SELECT COUNT(DISTINCT p.product_id)" + joins + where, args, page, pageSize,
                "SELECT category_id id, category_name name FROM categories ORDER BY display_order, category_name",
                "SELECT shop_id id, shop_name name FROM print_shops ORDER BY shop_name");
    }

    public static Optional<Map<String, Object>> productDetail(long id) {
        Optional<Map<String, Object>> product = findOne("SELECT p.product_id id, p.product_name name, p.slug,"
                + " p.description, p.brand, p.base_unit baseUnit, p.is_customizable customizable,"
                + " p.is_active active, p.category_id categoryId, c.category_name category,"
                + " p.moderation_status moderationStatus, p.rejection_reason rejectionReason,"
                + " p.reviewed_by_user_id reviewedBy, reviewer.full_name reviewerName, p.reviewed_at reviewedAt,"
                + " p.created_at createdAt, p.updated_at updatedAt"
                + " FROM products p LEFT JOIN categories c ON c.category_id = p.category_id"
                + " LEFT JOIN users reviewer ON reviewer.user_id = p.reviewed_by_user_id"
                + " WHERE p.product_id = ?", id);
        product.ifPresent(row -> {
            row.put("offers", rows("SELECT sp.shop_product_id id, sp.shop_id shopId, s.shop_name shopName,"
                    + " s.owner_user_id ownerUserId, owner.full_name ownerName, s.status shopStatus,"
                    + " sp.unit_price price, sp.material, sp.printing_method printingMethod,"
                    + " sp.minimum_quantity minimumQuantity, sp.production_days productionDays,"
                    + " sp.stock_status stockStatus, sp.is_active active, sp.status"
                    + " FROM shop_products sp JOIN print_shops s ON s.shop_id = sp.shop_id"
                    + " LEFT JOIN users owner ON owner.user_id = s.owner_user_id"
                    + " WHERE sp.product_id = ? ORDER BY s.shop_name, sp.shop_product_id", id));
            row.put("images", rows("SELECT image_url imageUrl, alt_text altText, is_primary primaryImage"
                    + " FROM product_images WHERE product_id = ? ORDER BY is_primary DESC, display_order", id));
        });
        return product;
    }

    public static void moderateProduct(long id, long adminId, String action, String reason) {
        if (!List.of("approve", "reject", "hide", "unhide").contains(action)) {
            throw new IllegalArgumentException("Thao tác kiểm duyệt không hợp lệ.");
        }
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String moderation; boolean active;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT moderation_status, is_active FROM products WHERE product_id = ? FOR UPDATE")) {
                    ps.setLong(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy sản phẩm.");
                        moderation = rs.getString("moderation_status"); active = rs.getBoolean("is_active");
                    }
                }
                ProductModeration.State next = ProductModeration.transition(moderation, active, action, reason);
                if ("approve".equals(action) || "reject".equals(action)) {
                    String sql = "UPDATE products SET moderation_status = ?, rejection_reason = ?,"
                            + " reviewed_by_user_id = ?, reviewed_at = NOW() WHERE product_id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, next.moderationStatus());
                        ps.setString(2, next.rejectionReason()); ps.setLong(3, adminId); ps.setLong(4, id); ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = conn.prepareStatement("UPDATE products SET is_active = ? WHERE product_id = ?")) {
                        ps.setBoolean(1, next.active()); ps.setLong(2, id); ps.executeUpdate();
                    }
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) { rollback(conn, e); throw e; }
            finally { conn.setAutoCommit(true); }
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static Map<String, Object> listOrders(String query, String status, String paymentStatus,
                                                  int page, int pageSize) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        appendSearch(where, args, query, "CAST(o.order_id AS CHAR)", "o.order_code", "u.full_name", "u.email", "u.phone");
        if (notAll(status)) { args.add(filterValue(status, "Trạng thái đơn hàng")); where.append(" AND o.status = ?"); }
        if (notAll(paymentStatus)) { args.add(filterValue(paymentStatus, "Trạng thái thanh toán")); where.append(" AND o.payment_status = ?"); }
        String from = " FROM orders o LEFT JOIN users u ON u.user_id = o.user_id";
        String select = "SELECT o.order_id id, o.order_code code, o.user_id userId, u.full_name customerName,"
                + " u.email customerEmail, u.phone customerPhone, o.subtotal, o.shipping_fee shippingFee,"
                + " o.total_amount totalAmount, o.currency, o.status, o.payment_status paymentStatus,"
                + " o.shipping_status shippingStatus, o.placed_at placedAt,"
                + " (SELECT COUNT(*) FROM order_items oi WHERE oi.order_id = o.order_id) itemCount";
        return page(select + from + where + " ORDER BY o.placed_at DESC, o.order_id DESC",
                "SELECT COUNT(*)" + from + where, args, page, pageSize,
                "SELECT DISTINCT status value FROM orders WHERE status IS NOT NULL ORDER BY status",
                "SELECT DISTINCT payment_status value FROM orders WHERE payment_status IS NOT NULL ORDER BY payment_status");
    }

    public static Optional<Map<String, Object>> orderDetail(long id) {
        Optional<Map<String, Object>> order = findOne("SELECT o.order_id id, o.order_code code, o.user_id userId,"
                + " u.full_name customerName, u.email customerEmail, u.phone customerPhone,"
                + " a.recipient_name recipientName, a.recipient_phone recipientPhone,"
                + " CONCAT_WS(', ', NULLIF(a.address_line,''), NULLIF(a.district,''), NULLIF(a.province,'')) shippingAddress,"
                + " o.subtotal, o.shipping_fee shippingFee, o.total_amount totalAmount, o.currency,"
                + " o.status, o.payment_status paymentStatus, o.shipping_status shippingStatus,"
                + " o.placed_at placedAt, o.completed_at completedAt, o.cancelled_at cancelledAt"
                + " FROM orders o LEFT JOIN users u ON u.user_id = o.user_id"
                + " LEFT JOIN addresses a ON a.address_id = o.address_id WHERE o.order_id = ?", id);
        order.ifPresent(row -> {
            row.put("items", rows("SELECT oi.order_item_id id, oi.product_id productId, oi.shop_product_id shopProductId,"
                    + " oi.shop_id shopId, oi.product_name_snapshot productName, oi.shop_name_snapshot shopName,"
                    + " oi.quantity, oi.unit_price unitPrice, oi.line_total lineTotal, oi.status"
                    + " FROM order_items oi WHERE oi.order_id = ? ORDER BY oi.order_item_id", id));
            row.put("payments", rows("SELECT payment_id id, payment_method method, provider, amount, status,"
                    + " failure_reason failureReason, paid_at paidAt, created_at createdAt"
                    + " FROM payments WHERE order_id = ? ORDER BY created_at, payment_id", id));
            row.put("shipments", rows("SELECT sh.shipment_id id, sh.shop_id shopId, s.shop_name shopName,"
                    + " sh.carrier, sh.tracking_number trackingNumber, sh.status, sh.shipped_at shippedAt,"
                    + " sh.delivered_at deliveredAt FROM shipments sh LEFT JOIN print_shops s ON s.shop_id = sh.shop_id"
                    + " WHERE sh.order_id = ? ORDER BY sh.shipment_id", id));
            row.put("productionUpdates", rows("SELECT pu.production_update_id id, pu.order_item_id orderItemId,"
                    + " s.shop_name shopName, pu.stage, pu.progress_percent progressPercent, pu.message, pu.created_at createdAt"
                    + " FROM production_updates pu JOIN order_items oi ON oi.order_item_id = pu.order_item_id"
                    + " LEFT JOIN print_shops s ON s.shop_id = pu.shop_id WHERE oi.order_id = ?"
                    + " ORDER BY pu.created_at DESC, pu.production_update_id DESC", id));
            row.put("history", rows("SELECT h.history_id id, h.changed_by_user_id changedByUserId,"
                    + " actor.full_name changedBy, h.old_status oldStatus, h.new_status newStatus, h.note, h.changed_at changedAt"
                    + " FROM order_status_history h LEFT JOIN users actor ON actor.user_id = h.changed_by_user_id"
                    + " WHERE h.order_id = ? ORDER BY h.changed_at, h.history_id", id));
            row.put("allowedTransitions", allowedTransitions((String) row.get("status")));
        });
        return order;
    }

    public static void changeOrderStatus(long orderId, long adminId, String newStatus, String note) {
        if (!List.of(OrderStatus.PENDING_PAYMENT, OrderStatus.CONFIRMED, OrderStatus.PROCESSING,
                OrderStatus.SHIPPED, OrderStatus.COMPLETED, OrderStatus.CANCELLED).contains(newStatus)) {
            throw new IllegalArgumentException("Trạng thái đơn hàng không hợp lệ.");
        }
        String normalizedNote = note == null ? "" : note.trim();
        if (normalizedNote.length() > 500) throw new IllegalArgumentException("Ghi chú tối đa 500 ký tự.");
        if (normalizedNote.isEmpty()) normalizedNote = "Admin cập nhật trạng thái đơn hàng";
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String current;
                try (PreparedStatement ps = conn.prepareStatement("SELECT status FROM orders WHERE order_id = ? FOR UPDATE")) {
                    ps.setLong(1, orderId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy đơn hàng.");
                        current = rs.getString(1);
                    }
                }
                if (!OrderStatus.canTransition(current, newStatus)) {
                    throw new IllegalArgumentException("Không thể chuyển trạng thái đơn từ “" + current + "” sang “" + newStatus + "”.");
                }
                StringBuilder sql = new StringBuilder("UPDATE orders SET status = ?");
                if (OrderStatus.CANCELLED.equals(newStatus)) sql.append(", cancelled_at = NOW()");
                if (OrderStatus.COMPLETED.equals(newStatus)) sql.append(", completed_at = NOW()");
                sql.append(" WHERE order_id = ?");
                try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                    ps.setString(1, newStatus); ps.setLong(2, orderId); ps.executeUpdate();
                }
                OrderDao.insertHistory(conn, orderId, adminId, current, newStatus, normalizedNote);
                conn.commit();
            } catch (SQLException | RuntimeException e) { rollback(conn, e); throw e; }
            finally { conn.setAutoCommit(true); }
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static Map<String, Object> listQuotes(String query, String status, int page, int pageSize) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        appendSearch(where, args, query, "CAST(q.quote_request_id AS CHAR)", "u.full_name", "u.email",
                "q.company_name", "p.product_name", "s.shop_name");
        if (notAll(status)) { where.append(" AND q.status = ?"); args.add(filterValue(status, "Trạng thái yêu cầu")); }
        String from = " FROM quote_requests q LEFT JOIN users u ON u.user_id = q.user_id"
                + " LEFT JOIN products p ON p.product_id = q.product_id"
                + " LEFT JOIN print_shops s ON s.shop_id = q.preferred_shop_id";
        return page(quoteSql() + where + " ORDER BY q.quote_request_id DESC",
                "SELECT COUNT(*)" + from + where, args, page, pageSize,
                "SELECT DISTINCT status value FROM quote_requests WHERE status IS NOT NULL ORDER BY status");
    }

    private static String quoteSql() {
        return "SELECT q.quote_request_id id, q.user_id userId, u.full_name customerName, u.email customerEmail,"
                + " q.product_id productId, p.product_name productName, q.preferred_shop_id shopId, s.shop_name shopName,"
                + " q.company_name companyName, q.requested_quantity quantity, q.required_by requiredBy,"
                + " q.quoted_amount quotedAmount, q.status FROM quote_requests q"
                + " LEFT JOIN users u ON u.user_id = q.user_id"
                + " LEFT JOIN products p ON p.product_id = q.product_id"
                + " LEFT JOIN print_shops s ON s.shop_id = q.preferred_shop_id";
    }

    private static List<String> allowedTransitions(String status) {
        List<String> candidates = List.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED, OrderStatus.PROCESSING,
                OrderStatus.SHIPPED, OrderStatus.COMPLETED);
        return candidates.stream().filter(next -> OrderStatus.canTransition(status, next)).toList();
    }

    private static Map<String, Object> page(String select, String count, List<Object> args,
                                             int requestedPage, int requestedSize, String... optionQueries) {
        int size = safePageSize(requestedSize);
        long total = scalar(count, args.toArray());
        int pages = Math.max(1, (int) Math.ceil((double) total / size));
        int current = Math.min(Math.max(1, requestedPage), pages);
        List<Object> dataArgs = new ArrayList<>(args);
        dataArgs.add(size); dataArgs.add((long) (current - 1) * size);
        List<Map<String, Object>> items = rows(select + " LIMIT ? OFFSET ?", dataArgs.toArray());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items); result.put("total", total); result.put("page", current);
        result.put("pageSize", size); result.put("pages", pages);
        if (optionQueries.length > 0) result.put("options", optionResults(optionQueries));
        return result;
    }

    private static Map<String, Object> optionResults(String... queries) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < queries.length; i++) result.put("set" + i, rows(queries[i]));
        return result;
    }

    private static Optional<Map<String, Object>> findOne(String sql, Object... args) {
        List<Map<String, Object>> result = rows(sql, args);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.getFirst());
    }

    private static Map<String, Object> one(String sql, Object... args) {
        List<Map<String, Object>> result = rows(sql, args);
        return result.isEmpty() ? new LinkedHashMap<>() : result.getFirst();
    }

    private static List<Map<String, Object>> rows(String sql, Object... args) {
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, args);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                        String label = rs.getMetaData().getColumnLabel(i);
                        item.put(label, jsonValue(label, rs.getObject(i)));
                    }
                    result.add(item);
                }
            }
            return result;
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    private static long scalar(String sql, Object... args) {
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, args);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getLong(1) : 0; }
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    private static void bind(PreparedStatement ps, Object[] args) throws SQLException {
        for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
    }

    private static Object jsonValue(String label, Object value) {
        if (value instanceof Number && label.toLowerCase(java.util.Locale.ROOT).endsWith("id")) {
            return value.toString();
        }
        if (value instanceof Timestamp timestamp) return timestamp.toLocalDateTime().toString();
        if (value instanceof java.sql.Date date) return date.toLocalDate().toString();
        if (value instanceof LocalDateTime || value instanceof LocalDate) return value.toString();
        return value;
    }

    private static void appendSearch(StringBuilder where, List<Object> args, String raw, String... columns) {
        String query = raw == null ? "" : raw.trim();
        if (query.isEmpty()) return;
        if (query.length() > 120) throw new IllegalArgumentException("Từ khóa tìm kiếm tối đa 120 ký tự.");
        where.append(" AND (");
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) where.append(" OR ");
            where.append(columns[i]).append(" LIKE ? ESCAPE '!' ");
            args.add("%" + query.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%");
        }
        where.append(')');
    }

    private static boolean notAll(String value) { return value != null && !value.isBlank() && !"All".equalsIgnoreCase(value); }
    /** Nhận ngày dạng yyyy-MM-dd; trả về java.sql.Date để so sánh với cột DATE/DATETIME. */
    private static java.sql.Date parseDate(String raw, String label) {
        String value = raw.trim();
        if (!value.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new IllegalArgumentException(label + " phải có dạng yyyy-MM-dd.");
        }
        try {
            return java.sql.Date.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(label + " không hợp lệ.");
        }
    }
    private static int safePageSize(int size) { return Math.min(Math.max(1, size), MAX_PAGE_SIZE); }
    private static String filterValue(String value, String label) {
        String normalized = value.trim();
        if (normalized.length() > 50) throw new IllegalArgumentException(label + " không hợp lệ.");
        return normalized;
    }
    private static void requireOneOf(String value, List<String> allowed, String message) {
        if (value == null || !allowed.contains(value)) throw new IllegalArgumentException(message);
    }
    private static long parsePositiveId(String value, String label) {
        try { long id = Long.parseLong(value); if (id > 0) return id; } catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException(label + " không hợp lệ.");
    }
    private static String normalized(String value, int maxLength, String label) {
        String result = value == null ? "" : value.trim();
        if (result.isEmpty() || result.length() > maxLength) throw new IllegalArgumentException(label + " không hợp lệ.");
        return result;
    }
    private static void rollback(Connection conn, Throwable original) {
        try { conn.rollback(); } catch (SQLException rollbackError) { original.addSuppressed(rollbackError); }
    }
}
