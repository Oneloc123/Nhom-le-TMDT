package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public record CustomPrintRequest(
        long id,
        long userId,
        long designFileId,
        String requestCode,
        Long shopId,
        String shopName,
        String workflowType,
        String printingMethod,
        String material,
        Integer infillPercent,
        BigDecimal layerHeightMm,
        Integer quantity,
        String colorOption,
        String postProcessingOption,
        String customerNote,
        BigDecimal estimatedTotal,
        BigDecimal quotedAmount,
        String sellerResponseNote,
        Integer productionDays,
        Timestamp quoteValidUntil,
        String status,
        Long orderId,
        boolean dimensionsConfirmed,
        String originalFilename,
        String storageUrl,
        String mimeType,
        Long fileSizeBytes,
        String sha256Hash,
        String analysisStatus,
        String sourceUnit,
        BigDecimal widthMm,
        BigDecimal depthMm,
        BigDecimal heightMm,
        BigDecimal volumeCm3,
        BigDecimal surfaceAreaCm2,
        Long triangleCount,
        String analysisMessage) {

    // JSP EL resolves JavaBean getters; it does not expose record accessors as properties.
    public long getId() { return id; }
    public String getRequestCode() { return requestCode; }
    public String getShopName() { return shopName; }
    public String getPrintingMethod() { return printingMethod; }
    public String getMaterial() { return material; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getEstimatedTotal() { return estimatedTotal; }
    public BigDecimal getQuotedAmount() { return quotedAmount; }
    public String getSellerResponseNote() { return sellerResponseNote; }
    public Integer getProductionDays() { return productionDays; }
    public Timestamp getQuoteValidUntil() { return quoteValidUntil; }
    public String getStatus() { return status; }
    public long getDesignFileId() { return designFileId; }
    public String getOriginalFilename() { return originalFilename; }
    public String getAnalysisMessage() { return analysisMessage; }
    public String getSourceUnit() { return sourceUnit; }
    public BigDecimal getWidthMm() { return widthMm; }
    public BigDecimal getDepthMm() { return depthMm; }
    public BigDecimal getHeightMm() { return heightMm; }
}
