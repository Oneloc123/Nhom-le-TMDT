package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

import java.sql.Timestamp;

public class DesignFile {
    private long designFileId;
    private Long userId;
    private Long productId;
    private String originalFilename;
    private String storageUrl;
    private String previewUrl;
    private String status;
    private Timestamp createdAt;

    public long getDesignFileId() {
        return designFileId;
    }

    public void setDesignFileId(long designFileId) {
        this.designFileId = designFileId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getStorageUrl() {
        return storageUrl;
    }

    public void setStorageUrl(String storageUrl) {
        this.storageUrl = storageUrl;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
