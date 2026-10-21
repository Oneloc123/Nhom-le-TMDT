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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ReviewDao {

    private static final int MAX_TEXT_LENGTH = 2000;
    private static final int MAX_VISIBLE_REVIEWS = 50;

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
        String sql = "SELECT r.review_id, r.order_item_id, r.user_id, r.product_id, r.shop_id,"
                + " r.rating, r.review_text, r.is_published,"
                + " CONCAT(LEFT(COALESCE(u.full_name, 'Khách'), 1), '***') AS display_name,"
                + " COALESCE(oi.product_name_snapshot, '') AS product_name,"
                + " COALESCE(oi.shop_name_snapshot, '') AS shop_name,"
                + " DATE_FORMAT(o.placed_at, '%d/%m/%Y') AS review_date"
                + " FROM reviews r"
                + " LEFT JOIN users u ON u.user_id = r.user_id"
                + " LEFT JOIN order_items oi ON oi.order_item_id = r.order_item_id"
                + " LEFT JOIN orders o ON o.order_id = oi.order_id"
                + " WHERE r.product_id = ? AND r.is_published = 1"
                + " ORDER BY r.review_id DESC LIMIT " + MAX_VISIBLE_REVIEWS;
        List<Review> reviews = new ArrayList<>();
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reviews.add(map(rs));
                }
            }
            return reviews;
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
        String sql = "SELECT r.review_id, r.order_item_id, r.user_id, r.product_id, r.shop_id,"
                + " r.rating, r.review_text, r.is_published,"
                + " CONCAT(LEFT(COALESCE(u.full_name, 'Khách'), 1), '***') AS display_name,"
                + " COALESCE(oi.product_name_snapshot, '') AS product_name,"
                + " COALESCE(oi.shop_name_snapshot, '') AS shop_name,"
                + " DATE_FORMAT(o.placed_at, '%d/%m/%Y') AS review_date"
                + " FROM reviews r"
                + " LEFT JOIN users u ON u.user_id = r.user_id"
                + " LEFT JOIN order_items oi ON oi.order_item_id = r.order_item_id"
                + " LEFT JOIN orders o ON o.order_id = oi.order_id"
                + " WHERE r.order_item_id = ?";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderItemId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

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
            boolean reviewed,
            Integer existingRating,
            String existingReviewText) {
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
        String sql = "SELECT oi.order_item_id, oi.order_id, o.order_code, o.user_id, oi.product_id, oi.shop_id,"
                + " oi.product_name_snapshot, oi.shop_name_snapshot, oi.unit_price, oi.quantity,"
                + " o.payment_status, o.status AS order_status,"
                + " DATE_FORMAT(o.placed_at, '%d/%m/%Y %H:%i') AS placed_at_text,"
                + " (SELECT i.image_url FROM product_images i"
                + "  WHERE i.product_id = oi.product_id"
                + "  ORDER BY i.is_primary DESC, i.display_order LIMIT 1) AS image_url,"
                + " rv.review_id, rv.rating AS existing_rating, rv.review_text AS existing_text"
                + " FROM order_items oi"
                + " JOIN orders o ON o.order_id = oi.order_id"
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

                return Optional.of(new ReviewableItem(
                        rs.getLong("order_item_id"),
                        rs.getLong("order_id"),
                        rs.getString("order_code"),
                        rs.getLong("user_id"),
                        rs.getLong("product_id"),
                        rs.getLong("shop_id"),
                        rs.getString("product_name_snapshot"),
                        rs.getString("shop_name_snapshot"),
                        rs.getBigDecimal("unit_price"),
                        rs.getInt("quantity"),
                        rs.getString("image_url"),
                        rs.getString("placed_at_text"),
                        paid,
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
        if (!item.paid()) {
            throw new IllegalArgumentException(
                    "Chỉ đánh giá được đơn hàng hợp lệ đã thanh toán hoặc đặt COD thành công.");
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

        String sql = "INSERT INTO reviews (order_item_id, user_id, product_id, shop_id,"
                + " rating, review_text, is_published) VALUES (?, ?, ?, ?, ?, ?, 1)";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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
                    return createOrUpdate(orderItemId, userId, rating, trimmed);
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
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static Review create(long orderItemId, long userId, int rating, String text) {
        return createOrUpdate(orderItemId, userId, rating, text);
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
        try {
            review.setProductName(rs.getString("product_name"));
        } catch (SQLException ignored) {
        }
        try {
            review.setShopName(rs.getString("shop_name"));
        } catch (SQLException ignored) {
        }
        try {
            review.setReviewDateText(rs.getString("review_date"));
        } catch (SQLException ignored) {
        }
        return review;
    }
}
