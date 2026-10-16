package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Product {

    private static final String[] VISUAL_CLASSES =
            {"warm", "primary", "dark", "teal", "sky", "pink", "cool", "brown", "violet", "green"};

    private long id;
    private long categoryId;
    private String categoryName;
    private String name;
    private String slug;
    private String description;
    private String brand;
    private String baseUnit;
    private boolean customizable;
    private boolean active;
    private String searchKeywords;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int viewCount;

    private String imageUrl;
    private String visualClass;
    private String tag = "";
    private double rating;
    private int reviewCount;

    private final List<ShopOffer> offers = new ArrayList<>();


    public long getId() { return id; }
    public void setId(long id) {
        this.id = id;
        this.visualClass = VISUAL_CLASSES[(int) (Math.abs(id) % VISUAL_CLASSES.length)];
    }

    public long getCategoryId() { return categoryId; }
    public void setCategoryId(long categoryId) { this.categoryId = categoryId; }

    public String getCategory() { return categoryName; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getBaseUnit() { return baseUnit; }
    public void setBaseUnit(String baseUnit) { this.baseUnit = baseUnit; }

    public boolean isCustomizable() { return customizable; }
    public void setCustomizable(boolean customizable) { this.customizable = customizable; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getSearchKeywords() { return searchKeywords; }
    public void setSearchKeywords(String searchKeywords) { this.searchKeywords = searchKeywords; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getVisualClass() { return visualClass; }
    public void setVisualClass(String visualClass) { this.visualClass = visualClass; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag == null ? "" : tag; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

    public List<ShopOffer> getOffers() { return offers; }

    public ShopOffer primaryOffer() {
        return offers.stream()
                .filter(ShopOffer::isPurchasable)
                .min((a, b) -> a.getUnitPrice().compareTo(b.getUnitPrice()))
                .orElse(null);
    }

    public BigDecimal getPrice() {
        ShopOffer offer = primaryOffer();
        return offer == null ? BigDecimal.ZERO : offer.getUnitPrice();
    }

    public String getPrintMethod() {
        ShopOffer offer = primaryOffer();
        return offer == null ? "" : offer.getPrintingMethod();
    }

    public int getMinimumQuantity() {
        return offers.stream()
                .filter(ShopOffer::isPurchasable)
                .mapToInt(ShopOffer::getMinimumQuantity)
                .min()
                .orElse(1);
    }

    public long getStoreId() {
        ShopOffer offer = primaryOffer();
        return offer == null ? 0 : offer.getShopId();
    }

    public String getStoreName() {
        ShopOffer offer = primaryOffer();
        return offer == null ? "" : offer.getShopName();
    }
}