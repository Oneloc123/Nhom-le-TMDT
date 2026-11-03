package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.OrderStatus;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Review;

import java.math.BigDecimal;
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

    public record RatingSummary(
            double averageRating,
            int totalReviews,
            int star5Count,
            int star4Count,
            int star3Count,
            int star2Count,
            int star1Count) {
        public double getAverageRating() {
            return averageRating;
        }

        public int getTotalReviews() {
            return totalReviews;
        }

        public int getStar5Count() {
            return star5Count;
        }

        public int getStar4Count() {
            return star4Count;
        }

        public int getStar3Count() {
            return star3Count;
        }

        public int getStar2Count() {
            return star2Count;
        }

        public int getStar1Count() {
            return star1Count;
        }

        public int getPercent5() {
            return percent(star5Count);
        }

        public int getPercent4() {
            return percent(star4Count);
        }

        public int getPercent3() {
            return percent(star3Count);
        }

        public int getPercent2() {
            return percent(star2Count);
        }

        public int getPercent1() {
            return percent(star1Count);
        }

        public int percent(int starCount) {
            return totalReviews <= 0 ? 0 : (int) Math.round((starCount * 100.0) / totalReviews);
        }

        public String getFormattedAverage() {
            return String.format(java.util.Locale.US, "%.1f", averageRating);
        }

        public String formattedAverage() {
            return getFormattedAverage();
        }
    }

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

    public static RatingSummary getRatingSummary(long productId) {
        String sql = "SELECT "
                + " COALESCE(AVG(rating), 0) AS avg_rating,"
                + " COUNT(*) AS total_count,"
                + " COALESCE(SUM(CASE WHEN rating = 5 THEN 1 ELSE 0 END), 0) AS c5,"
                + " COALESCE(SUM(CASE WHEN rating = 4 THEN 1 ELSE 0 END), 0) AS c4,"
                + " COALESCE(SUM(CASE WHEN rating = 3 THEN 1 ELSE 0 END), 0) AS c3,"
                + " COALESCE(SUM(CASE WHEN rating = 2 THEN 1 ELSE 0 END), 0) AS c2,"
                + " COALESCE(SUM(CASE WHEN rating = 1 THEN 1 ELSE 0 END), 0) AS c1"
                + " FROM reviews WHERE product_id = ? AND is_published = 1";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double avg = Math.round(rs.getDouble("avg_rating") * 10.0) / 10.0;
                    return new RatingSummary(
                            avg,
                            rs.getInt("total_count"),
                            rs.getInt("c5"),
                            rs.getInt("c4"),
                            rs.getInt("c3"),
                            rs.getInt("c2"),
                            rs.getInt("c1"));
                }
            }
            return new RatingSummary(0.0, 0, 0, 0, 0, 0, 0);
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

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

    /** Tìm đánh giá/bình luận gần nhất của một người dùng cho sản phẩm. */
    public static Optional<Review> findUserReviewForProduct(long productId, long userId) {
        try (Connection conn = Database.getConnection()) {
            boolean hasCreatedAt = hasColumn(conn, "reviews", "created_at");
            String sql = "SELECT r.review_id, r.order_item_id, r.user_id, r.product_id, r.shop_id,"
                    + " r.rating, r.review_text, r.is_published,"
                    + (hasCreatedAt ? " r.created_at," : " NULL AS created_at,")
                    + " COALESCE(NULLIF(TRIM(u.full_name), ''), u.email, 'Khách hàng') AS display_name"
                    + " FROM reviews r LEFT JOIN users u ON u.user_id = r.user_id"
                    + " WHERE r.product_id = ? AND r.user_id = ?"
                    + " ORDER BY (r.order_item_id IS NOT NULL) DESC, r.review_id DESC LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, productId);
                ps.setLong(2, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(map(rs)) : Optional.empty();
                }
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    /** Thông tin dòng hàng để form đánh giá hiển thị + kiểm tra điều kiện. */
    public record ReviewableItem(
            long orderItemId,
            long orderId,
            String orderCode,
            long userId,
            long productId,
            Long shopId,
            String productName,
            String shopName,
            BigDecimal unitPrice,
            int quantity,
            String imageUrl,
            String placedAtText,
            boolean paid,
            boolean completed,
            boolean reviewed,
            Integer existingRating,
            String existingReviewText) {
        public boolean reviewable() {
            return paid && completed && !reviewed;
        }

        public long getOrderItemId() {
            return orderItemId;
        }

        public long getOrderId() {
            return orderId;
        }

        public String getOrderCode() {
            return orderCode;
        }

        public long getUserId() {
            return userId;
        }

        public long getProductId() {
            return productId;
        }

        public Long getShopId() {
            return shopId;
        }

        public String getProductName() {
            return productName;
        }

        public String getShopName() {
            return shopName;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public int getQuantity() {
            return quantity;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public String getPlacedAtText() {
            return placedAtText;
        }

        public boolean isPaid() {
            return paid;
        }

        public boolean isCompleted() {
            return completed;
        }

        public boolean isReviewed() {
            return reviewed;
        }

        public Integer getExistingRating() {
            return existingRating;
        }

        public String getExistingReviewText() {
            return existingReviewText;
        }
    }

    public static Optional<ReviewableItem> findReviewableItem(long orderItemId, long viewerUserId) {
        String sql = "SELECT oi.order_item_id, oi.order_id, o.order_code, o.user_id,"
                + " oi.product_id, oi.shop_id, oi.product_name_snapshot, oi.shop_name_snapshot,"
                + " oi.unit_price, oi.quantity, o.payment_status, o.status AS order_status,"
                + " DATE_FORMAT(o.placed_at, '%d/%m/%Y %H:%i') AS placed_at_text,"
                + " (SELECT i.image_url FROM product_images i"
                + "  WHERE i.product_id = oi.product_id"
                + "  ORDER BY i.is_primary DESC, i.display_order LIMIT 1) AS image_url,"
                + " rv.review_id, rv.rating AS existing_rating, rv.review_text AS existing_text"
                + " FROM order_items oi JOIN orders o ON o.order_id = oi.order_id"
                + " LEFT JOIN reviews rv ON rv.order_item_id = oi.order_item_id"
                + " WHERE oi.order_item_id = ? AND o.user_id = ?";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderItemId);
            ps.setLong(2, viewerUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                String paymentStatus = rs.getString("payment_status");
                String orderStatus = rs.getString("order_status");
                boolean notCancelled = !OrderStatus.CANCELLED.equals(orderStatus);
                boolean paid = notCancelled && (OrderStatus.PAYMENT_PAID.equals(paymentStatus)
                        || OrderStatus.PAYMENT_COD.equals(paymentStatus)
                        || OrderStatus.COMPLETED.equals(orderStatus));
                long reviewId = rs.getLong("review_id");
                boolean reviewed = !rs.wasNull() && reviewId > 0;
                int ratingVal = rs.getInt("existing_rating");
                Integer existingRating = rs.wasNull() ? null : ratingVal;
                String existingText = rs.getString("existing_text");
                long shopIdValue = rs.getLong("shop_id");
                Long shopId = rs.wasNull() ? null : shopIdValue;

                return Optional.of(new ReviewableItem(
                        rs.getLong("order_item_id"),
                        rs.getLong("order_id"),
                        rs.getString("order_code"),
                        rs.getLong("user_id"),
                        rs.getLong("product_id"),
                        shopId,
                        rs.getString("product_name_snapshot"),
                        rs.getString("shop_name_snapshot"),
                        rs.getBigDecimal("unit_price"),
                        rs.getInt("quantity"),
                        rs.getString("image_url"),
                        rs.getString("placed_at_text"),
                        paid,
                        OrderStatus.COMPLETED.equals(rs.getString("order_status")),
                        reviewed,
                        existingRating,
                        existingText));
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static Optional<ReviewableItem> findReviewableItemForProduct(long productId, long viewerUserId) {
        String sql = "SELECT oi.order_item_id"
                + " FROM order_items oi"
                + " JOIN orders o ON o.order_id = oi.order_id"
                + " LEFT JOIN reviews rv ON rv.order_item_id = oi.order_item_id"
                + " WHERE oi.product_id = ? AND o.user_id = ? AND o.status <> ?"
                + " ORDER BY (rv.review_id IS NULL) DESC, oi.order_item_id DESC"
                + " LIMIT 1";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, productId);
            ps.setLong(2, viewerUserId);
            ps.setString(3, OrderStatus.CANCELLED);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long orderItemId = rs.getLong("order_item_id");
                    return findReviewableItem(orderItemId, viewerUserId);
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static Review createOrUpdate(long orderItemId, long userId, int rating, String text) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Điểm đánh giá phải từ 1 đến 5 sao.");
        }
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Nội dung đánh giá không được để trống.");
        }
        if (trimmed.length() < 3) {
            throw new IllegalArgumentException("Nội dung đánh giá cần tối thiểu 3 ký tự.");
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
            String updateSql = "UPDATE reviews SET rating = ?, review_text = ? WHERE order_item_id = ? AND user_id = ?";
            try (Connection conn = Database.getConnection();
                    PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setInt(1, rating);
                ps.setString(2, trimmed);
                ps.setLong(3, orderItemId);
                ps.setLong(4, userId);
                ps.executeUpdate();
                return findByOrderItem(orderItemId).orElseThrow();
            } catch (SQLException e) {
                throw DataAccessException.of(e);
            }
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

    public static Review create(long orderItemId, long userId, int rating, String text) {
        return createOrUpdate(orderItemId, userId, rating, text);
    }

    public static Review postReviewOrComment(long productId, long userId, int rating, String text) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Điểm đánh giá phải từ 1 đến 5 sao.");
        }
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Nội dung bình luận không được để trống.");
        }
        if (trimmed.length() < 3) {
            throw new IllegalArgumentException("Nội dung bình luận cần tối thiểu 3 ký tự.");
        }
        if (trimmed.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException("Nội dung bình luận tối đa "
                    + MAX_TEXT_LENGTH + " ký tự.");
        }

        // 1. Nếu khách đã mua sản phẩm và đơn hợp lệ, ưu tiên gắn vào dòng hàng
        Optional<ReviewableItem> eligibleItem = findReviewableItemForProduct(productId, userId);
        if (eligibleItem.isPresent() && eligibleItem.get().paid()) {
            return createOrUpdate(eligibleItem.get().orderItemId(), userId, rating, trimmed);
        }

        // 2. Nếu khách chưa mua hoặc không có dòng hàng, lưu bình luận/đánh giá chung của tài khoản
        Optional<Review> existing = findUserReviewForProduct(productId, userId);
        if (existing.isPresent()) {
            Review rev = existing.get();
            String updateSql = "UPDATE reviews SET rating = ?, review_text = ? WHERE review_id = ? AND user_id = ?";
            try (Connection conn = Database.getConnection();
                    PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setInt(1, rating);
                ps.setString(2, trimmed);
                ps.setLong(3, rev.getReviewId());
                ps.setLong(4, userId);
                ps.executeUpdate();
                return findUserReviewForProduct(productId, userId).orElse(rev);
            } catch (SQLException e) {
                throw DataAccessException.of(e);
            }
        }

        String insertSql = "INSERT INTO reviews (order_item_id, user_id, product_id, shop_id,"
                + " rating, review_text, is_published)"
                + " VALUES (NULL, ?, ?, (SELECT shop_id FROM products WHERE product_id = ? LIMIT 1), ?, ?, 1)";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, userId);
            ps.setLong(2, productId);
            ps.setLong(3, productId);
            ps.setInt(4, rating);
            ps.setString(5, trimmed);
            ps.executeUpdate();
            return findUserReviewForProduct(productId, userId).orElseThrow();
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static boolean deleteReview(long reviewId, long userId) {
        String sql = "DELETE FROM reviews WHERE review_id = ? AND user_id = ?";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, reviewId);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
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
