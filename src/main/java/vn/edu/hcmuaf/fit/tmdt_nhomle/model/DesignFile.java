package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

import java.sql.Timestamp;
import java.math.BigDecimal;

public class DesignFile {
    private long designFileId;
    private Long userId;
    private Long productId;
    private String originalFilename;
    private String storageUrl;
    private String previewUrl;
    private String status;
    private Timestamp createdAt;
    private String mimeType;
    private Long fileSizeBytes;
    private String sha256Hash;
    private String analysisStatus;
    private String sourceUnit;
    private BigDecimal widthMm;
    private BigDecimal depthMm;
    private BigDecimal heightMm;
    private BigDecimal volumeCm3;
    private BigDecimal surfaceAreaCm2;
    private Long triangleCount;
    private String analysisMessage;

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

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }
    public String getSha256Hash() { return sha256Hash; }
    public void setSha256Hash(String sha256Hash) { this.sha256Hash = sha256Hash; }
    public String getAnalysisStatus() { return analysisStatus; }
    public void setAnalysisStatus(String analysisStatus) { this.analysisStatus = analysisStatus; }
    public String getSourceUnit() { return sourceUnit; }
    public void setSourceUnit(String sourceUnit) { this.sourceUnit = sourceUnit; }
    public BigDecimal getWidthMm() { return widthMm; }
    public void setWidthMm(BigDecimal widthMm) { this.widthMm = widthMm; }
    public BigDecimal getDepthMm() { return depthMm; }
    public void setDepthMm(BigDecimal depthMm) { this.depthMm = depthMm; }
    public BigDecimal getHeightMm() { return heightMm; }
    public void setHeightMm(BigDecimal heightMm) { this.heightMm = heightMm; }
    public BigDecimal getVolumeCm3() { return volumeCm3; }
    public void setVolumeCm3(BigDecimal volumeCm3) { this.volumeCm3 = volumeCm3; }
    public BigDecimal getSurfaceAreaCm2() { return surfaceAreaCm2; }
    public void setSurfaceAreaCm2(BigDecimal surfaceAreaCm2) { this.surfaceAreaCm2 = surfaceAreaCm2; }
    public Long getTriangleCount() { return triangleCount; }
    public void setTriangleCount(Long triangleCount) { this.triangleCount = triangleCount; }
    public String getAnalysisMessage() { return analysisMessage; }
    public void setAnalysisMessage(String analysisMessage) { this.analysisMessage = analysisMessage; }
}
