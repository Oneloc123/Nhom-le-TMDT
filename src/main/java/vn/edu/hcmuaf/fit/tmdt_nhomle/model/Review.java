package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

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

    private String reviewDateText;
    private String productName;
    private String shopName;

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
        return userDisplayName.substring(0, 1).toUpperCase();
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
}
