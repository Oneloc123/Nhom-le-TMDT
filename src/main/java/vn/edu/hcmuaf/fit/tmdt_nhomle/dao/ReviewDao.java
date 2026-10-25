package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.OrderStatus;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Review;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Truy cập bảng reviews.
 * - Hiển thị công khai CHỈ các đánh giá is_published = 1; tên người hiển thị
 *   được rút gọn (chữ đầu + ***), không lộ email.
 * - Tạo đánh giá: kiểm tra dòng hàng thuộc đơn của user, đơn đã hoàn thành,
 *   điểm 1–5, và dòng hàng CHƯA có đánh giá (order_item_id UNIQUE).
 */
public final class ReviewDao {

    private static final int MAX_TEXT_LENGTH = 2000;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 50;

    private ReviewDao() {
    }

    /** Đánh giá đã đăng của một sản phẩm (công khai). */
    public static List<Review> listPublishedForProduct(long productId) {
        return listPublishedForProduct(productId, 1, DEFAULT_PAGE_SIZE);
    }

    /** Một trang đánh giá công khai; giới hạn kích thước để tránh quét bảng quá lớn. */
    public static List<Review> listPublishedForProduct(long productId, int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, pageSize), MAX_PAGE_SIZE);
        List<Review> reviews = new ArrayList<>();
        try (Connection conn = Database.getConnection()) {
            boolean hasCreatedAt = hasColumn(conn, "reviews", "created_at");
            String sql = "SELECT r.review_id, r.order_item_id, r.user_id, r.product_id, r.shop_id,"
                    + " r.rating, r.review_text, r.is_published,"
                    + (hasCreatedAt ? " r.created_at," : " NULL AS created_at,")
                    + " CONCAT(LEFT(COALESCE(u.full_name, 'Khách'), 1), '***') AS display_name"
                    + " FROM reviews r LEFT JOIN users u ON u.user_id = r.user_id"
                    + " WHERE r.product_id = ? AND r.is_published = 1"
                    + " ORDER BY " + (hasCreatedAt ? "r.created_at DESC, " : "")
                    + "r.review_id DESC LIMIT ? OFFSET ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, productId);
                ps.setInt(2, safeSize);
                ps.setInt(3, (safePage - 1) * safeSize);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        reviews.add(map(rs));
                    }
                }
            }
            return reviews;
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static long countPublishedForProduct(long productId) {
        String sql = "SELECT COUNT(*) FROM reviews WHERE product_id = ? AND is_published = 1";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    /** Đánh giá theo dòng hàng (0 hoặc 1 phần tử — order_item_id UNIQUE). */
    public static Optional<Review> findByOrderItem(long orderItemId) {
        try (Connection conn = Database.getConnection()) {
            boolean hasCreatedAt = hasColumn(conn, "reviews", "created_at");
            String sql = "SELECT r.review_id, r.order_item_id, r.user_id, r.product_id, r.shop_id,"
                    + " r.rating, r.review_text, r.is_published,"
                    + (hasCreatedAt ? " r.created_at," : " NULL AS created_at,")
                    + " CONCAT(LEFT(COALESCE(u.full_name, 'Khách'), 1), '***') AS display_name"
                    + " FROM reviews r LEFT JOIN users u ON u.user_id = r.user_id"
                    + " WHERE r.order_item_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, orderItemId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(map(rs)) : Optional.empty();
                }
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    /** Thông tin dòng hàng để form đánh giá hiển thị + kiểm tra điều kiện. */
    public record ReviewableItem(long orderItemId, long orderId, long userId, long productId,
                                 Long shopId, String productName, boolean paid,
                                 boolean completed, boolean reviewed) {
        public boolean reviewable() {
            return paid && completed && !reviewed;
        }
    }

    public static Optional<ReviewableItem> findReviewableItem(long orderItemId, long viewerUserId) {
        String sql = "SELECT oi.order_item_id, oi.order_id, o.user_id, oi.product_id, oi.shop_id,"
                + " oi.product_name_snapshot, o.payment_status, o.status AS order_status,"
                + " (SELECT COUNT(*) FROM reviews rv WHERE rv.order_item_id = oi.order_item_id)"
                + "   AS review_count"
                + " FROM order_items oi JOIN orders o ON o.order_id = oi.order_id"
                + " WHERE oi.order_item_id = ? AND o.user_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderItemId);
            ps.setLong(2, viewerUserId); // ownership: dòng hàng thuộc đơn của user đang xem
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                String paymentStatus = rs.getString("payment_status");
                boolean paid = OrderStatus.PAYMENT_PAID.equals(paymentStatus)
                        || OrderStatus.PAYMENT_COD.equals(paymentStatus);
                return Optional.of(new ReviewableItem(
                        rs.getLong("order_item_id"),
                        rs.getLong("order_id"),
                        rs.getLong("user_id"),
                        rs.getLong("product_id"),
                        rs.getLong("shop_id"),
                        rs.getString("product_name_snapshot"),
                        paid,
                        OrderStatus.COMPLETED.equals(rs.getString("order_status")),
                        rs.getInt("review_count") > 0));
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    /** Tạo đánh giá hợp lệ (validate lại toàn bộ điều kiện trước khi ghi). */
    public static Review create(long orderItemId, long userId, int rating, String text) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Điểm đánh giá phải từ 1 đến 5.");
        }
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Nội dung đánh giá không được để trống.");
        }
        if (trimmed.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException("Nội dung đánh giá tối đa "
                    + MAX_TEXT_LENGTH + " ký tự.");
        }
        ReviewableItem item = findReviewableItem(orderItemId, userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Dòng hàng không tồn tại hoặc bạn không có quyền đánh giá."));
        if (!item.paid() || !item.completed()) {
            throw new IllegalArgumentException(
                    "Chỉ đánh giá được sản phẩm sau khi đơn hàng đã hoàn thành.");
        }
        if (item.reviewed()) {
            throw new IllegalArgumentException("Dòng hàng này đã được đánh giá.");
        }
        try (Connection conn = Database.getConnection()) {
            boolean hasCreatedAt = hasColumn(conn, "reviews", "created_at");
            String sql = "INSERT INTO reviews (order_item_id, user_id, product_id, shop_id,"
                    + " rating, review_text, is_published"
                    + (hasCreatedAt ? ", created_at" : "")
                    + ") VALUES (?, ?, ?, ?, ?, ?, 1" + (hasCreatedAt ? ", NOW()" : "") + ")";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, orderItemId);
                ps.setLong(2, userId);
                ps.setLong(3, item.productId());
                if (item.shopId() == null) {
                    ps.setNull(4, java.sql.Types.BIGINT);
                } else {
                    ps.setLong(4, item.shopId());
                }
                ps.setInt(5, rating);
                ps.setString(6, trimmed);
                try {
                    ps.executeUpdate();
                } catch (SQLException duplicate) {
                    if (duplicate.getErrorCode() == 1062) {
                        throw new IllegalArgumentException("Dòng hàng này đã được đánh giá.");
                    }
                    throw duplicate;
                }
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    long id = keys.next() ? keys.getLong(1) : -1;
                    return findByOrderItem(orderItemId).orElseGet(() -> {
                        Review review = new Review();
                        review.setReviewId(id);
                        review.setOrderItemId(orderItemId);
                        review.setUserId(userId);
                        review.setProductId(item.productId());
                        review.setShopId(item.shopId());
                        review.setRating(rating);
                        review.setReviewText(trimmed);
                        return review;
                    });
                }
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    private static Review map(ResultSet rs) throws SQLException {
        Review review = new Review();
        review.setReviewId(rs.getLong("review_id"));
        review.setOrderItemId(rs.getLong("order_item_id"));
        review.setUserId(rs.getLong("user_id"));
        long productId = rs.getLong("product_id");
        review.setProductId(rs.wasNull() ? null : productId);
        long shopId = rs.getLong("shop_id");
        review.setShopId(rs.wasNull() ? null : shopId);
        review.setRating(rs.getInt("rating"));
        review.setReviewText(rs.getString("review_text"));
        review.setPublished(rs.getBoolean("is_published"));
        review.setUserDisplayName(rs.getString("display_name"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        review.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        return review;
    }

    private static boolean hasColumn(Connection conn, String table, String column) throws SQLException {
        try (ResultSet columns = conn.getMetaData().getColumns(conn.getCatalog(), null, table, column)) {
            if (columns.next()) {
                return true;
            }
        }
        try (ResultSet columns = conn.getMetaData().getColumns(conn.getCatalog(), null,
                table.toUpperCase(), column.toUpperCase())) {
            return columns.next();
        }
    }
}
