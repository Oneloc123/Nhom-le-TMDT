package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

import java.math.BigDecimal;

public class Product {
    private final int id;
    private final String name;
    private final String category;
    private final String description;
    private final String printMethod;
    private final BigDecimal price;
    private final int minimumQuantity;
    private final double rating;
    private final int reviewCount;
    private final int storeId;
    private final String storeName;
    private final String visualClass;
    private final String tag;

    public Product(int id, String name, String category, String description,
                   String printMethod, BigDecimal price, int minimumQuantity,
                   double rating, int reviewCount, int storeId, String storeName,
                   String visualClass, String tag) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.description = description;
        this.printMethod = printMethod;
        this.price = price;
        this.minimumQuantity = minimumQuantity;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.storeId = storeId;
        this.storeName = storeName;
        this.visualClass = visualClass;
        this.tag = tag;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getPrintMethod() { return printMethod; }
    public BigDecimal getPrice() { return price; }
    public int getMinimumQuantity() { return minimumQuantity; }
    public double getRating() { return rating; }
    public int getReviewCount() { return reviewCount; }
    public int getStoreId() { return storeId; }
    public String getStoreName() { return storeName; }
    public String getVisualClass() { return visualClass; }
    public String getTag() { return tag; }
}
