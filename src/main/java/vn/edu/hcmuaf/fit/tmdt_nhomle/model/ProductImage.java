package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

/** Ảnh trong thư viện ảnh của sản phẩm. */
public class ProductImage {
    private long imageId;
    private String imageUrl;
    private String altText;
    private boolean primary;
    private int displayOrder;

    public long getImageId() { return imageId; }
    public void setImageId(long imageId) { this.imageId = imageId; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getAltText() { return altText; }
    public void setAltText(String altText) { this.altText = altText; }

    public boolean isPrimary() { return primary; }
    public void setPrimary(boolean primary) { this.primary = primary; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
}
