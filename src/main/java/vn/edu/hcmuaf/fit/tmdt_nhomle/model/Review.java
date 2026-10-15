package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

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

    public long getReviewId() { return reviewId; }
    public void setReviewId(long reviewId) { this.reviewId = reviewId; }

    public long getOrderItemId() { return orderItemId; }
    public void setOrderItemId(long orderItemId) { this.orderItemId = orderItemId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getUserDisplayName() { return userDisplayName; }
    public void setUserDisplayName(String userDisplayName) { this.userDisplayName = userDisplayName; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getReviewText() { return reviewText; }
    public void setReviewText(String reviewText) { this.reviewText = reviewText; }

    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }

    /** Chuỗi sao hiển thị (1–5). */
    public String getStars() {
        return "★★★★★".substring(0, Math.min(Math.max(rating, 0), 5));
    }
}
