package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Đánh giá sau khi mua (bảng reviews), gắn với order_item_id cụ thể. */
public class Review {

    private long reviewId;
    private long orderItemId;
    private long userId;
    private String userDisplayName;
    private Long productId;
    private Long shopId;
    private int rating;
    private String reviewText;
    private boolean published;
    private LocalDateTime createdAt;

    private String userEmail;
    private String reviewDateText;
    private String productName;
    private String shopName;

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public boolean isVerifiedBuyer() {
        return orderItemId > 0;
    }

    public long getReviewId() {
        return reviewId;
    }

    public void setReviewId(long reviewId) {
        this.reviewId = reviewId;
    }

    public long getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(long orderItemId) {
        this.orderItemId = orderItemId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getUserDisplayName() {
        return userDisplayName;
    }

    public void setUserDisplayName(String userDisplayName) {
        this.userDisplayName = userDisplayName;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getReviewText() {
        return reviewText;
    }

    public void setReviewText(String reviewText) {
        this.reviewText = reviewText;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public String getReviewDateText() {
        return reviewDateText;
    }

    public void setReviewDateText(String reviewDateText) {
        this.reviewDateText = reviewDateText;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCreatedAtText() {
        return createdAt == null ? "" : createdAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    /** Chuỗi sao hiển thị (1–5). */
    public String getStars() {
        int safe = Math.min(Math.max(rating, 0), 5);
        return "★★★★★".substring(0, safe);
    }

    public String getEmptyStars() {
        int safe = Math.min(Math.max(rating, 0), 5);
        return "☆☆☆☆☆".substring(0, 5 - safe);
    }

    public String getUserInitial() {
        if (userDisplayName == null || userDisplayName.isBlank()) {
            return "K";
        }
        return userDisplayName.trim().substring(0, 1).toUpperCase();
    }

    public String getRatingLabel() {
        return switch (rating) {
            case 5 -> "Tuyệt vời";
            case 4 -> "Hài lòng";
            case 3 -> "Bình thường";
            case 2 -> "Chưa hài lòng";
            case 1 -> "Rất tệ";
            default -> "Đánh giá";
        };
    }

    public String getEmptyStars() {
        return "☆☆☆☆☆".substring(0, 5 - Math.min(Math.max(rating, 0), 5));
    }
}
