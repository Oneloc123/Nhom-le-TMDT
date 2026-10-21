package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

/**
 * Bản cấu hình custom sản phẩm in 3D đã lưu của khách hàng.
 *
 * <p>Khác với {@code design_files} (file gốc khách tải lên), bản ghi này giữ
 * THÔNG SỐ cấu hình: sản phẩm tham chiếu, tên thiết kế, JSON cấu hình, vật liệu,
 * màu, kích thước, số lượng, preview và trạng thái — để mở lại đúng như đã lưu.</p>
 */
public class SavedDesign {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_FINAL = "FINAL";

    private long designId;
    private long userId;
    private Long productId;
    private Long designFileId;
    private String designName;
    private String configJson;
    private String material;
    private String color;
    private String dimensions;
    private int quantity = 1;
    private String previewUrl;
    private String notes;
    private String status = STATUS_DRAFT;
    private int version = 1;
    private String createdAt;
    private String updatedAt;

    // Chỉ đọc — đi kèm từ JOIN products để hiển thị.
    private String productName;
    private Boolean productCustomizable;
    private Boolean productActive;

    public long getDesignId() { return designId; }
    public void setDesignId(long designId) { this.designId = designId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getDesignFileId() { return designFileId; }
    public void setDesignFileId(Long designFileId) { this.designFileId = designFileId; }

    public String getDesignName() { return designName; }
    public void setDesignName(String designName) { this.designName = designName; }

    public String getConfigJson() { return configJson; }
    public void setConfigJson(String configJson) { this.configJson = configJson; }

    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getDimensions() { return dimensions; }
    public void setDimensions(String dimensions) { this.dimensions = dimensions; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getPreviewUrl() { return previewUrl; }
    public void setPreviewUrl(String previewUrl) { this.previewUrl = previewUrl; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Boolean getProductCustomizable() { return productCustomizable; }
    public void setProductCustomizable(Boolean productCustomizable) {
        this.productCustomizable = productCustomizable;
    }

    public Boolean getProductActive() { return productActive; }
    public void setProductActive(Boolean productActive) { this.productActive = productActive; }
}
