package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

public class SuggestedShop {
    private long shopId;
    private String shopName;
    private String slug;
    private String location;
    private double rating;
    private long completedOrders;
    private Integer estimatedLeadDays;
    private String capabilities;
    private String maxBuildDimensions;
    private int matchScore;
    private String matchBadge;
    private String matchReason;
    private boolean bestMatch;

    public long getShopId() {
        return shopId;
    }

    public void setShopId(long shopId) {
        this.shopId = shopId;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public long getCompletedOrders() {
        return completedOrders;
    }

    public void setCompletedOrders(long completedOrders) {
        this.completedOrders = completedOrders;
    }

    public Integer getEstimatedLeadDays() {
        return estimatedLeadDays;
    }

    public void setEstimatedLeadDays(Integer estimatedLeadDays) {
        this.estimatedLeadDays = estimatedLeadDays;
    }

    public String getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(String capabilities) {
        this.capabilities = capabilities;
    }

    public String getMaxBuildDimensions() {
        return maxBuildDimensions;
    }

    public void setMaxBuildDimensions(String maxBuildDimensions) {
        this.maxBuildDimensions = maxBuildDimensions;
    }

    public int getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(int matchScore) {
        this.matchScore = matchScore;
    }

    public String getMatchBadge() {
        return matchBadge;
    }

    public void setMatchBadge(String matchBadge) {
        this.matchBadge = matchBadge;
    }

    public String getMatchReason() {
        return matchReason;
    }

    public void setMatchReason(String matchReason) {
        this.matchReason = matchReason;
    }

    public boolean isBestMatch() {
        return bestMatch;
    }

    public void setBestMatch(boolean bestMatch) {
        this.bestMatch = bestMatch;
    }
}
