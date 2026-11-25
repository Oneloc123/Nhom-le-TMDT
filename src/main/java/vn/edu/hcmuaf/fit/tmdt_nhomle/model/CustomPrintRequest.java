package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

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
        String analysisMessage,
        BigDecimal depositAmount,
        Timestamp depositPaidAt,
        Timestamp productionStartedAt,
        Timestamp completedAt,
        String productionNote,
        String orderCode) {

    public static final BigDecimal DEPOSIT_RATE = new BigDecimal("0.30");

    public static BigDecimal calculateDepositAmount(BigDecimal quotedAmount) {
        if (quotedAmount == null || quotedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return quotedAmount.multiply(DEPOSIT_RATE).setScale(0, RoundingMode.HALF_UP);
    }

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
    public BigDecimal getDepositAmount() { return depositAmount; }
    public Timestamp getDepositPaidAt() { return depositPaidAt; }
    public Timestamp getProductionStartedAt() { return productionStartedAt; }
    public Timestamp getCompletedAt() { return completedAt; }
    public String getProductionNote() { return productionNote; }
    public Long getOrderId() { return orderId; }
    public String getOrderCode() { return orderCode; }

    public BigDecimal getRequiredDepositAmount() {
        if (depositAmount != null && depositAmount.compareTo(BigDecimal.ZERO) > 0) {
            return depositAmount;
        }
        return calculateDepositAmount(quotedAmount);
    }

    public BigDecimal getRemainingAmount() {
        BigDecimal dep = getRequiredDepositAmount();
        if (quotedAmount == null || dep == null) {
            return null;
        }
        BigDecimal rem = quotedAmount.subtract(dep);
        return rem.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : rem;
    }

    public boolean isDepositable() {
        return "QUOTE".equals(workflowType)
                && "QUOTED".equals(status)
                && orderId == null
                && quotedAmount != null
                && quotedAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    public String getStatusLabel() {
        if (status == null) return "—";
        return switch (status) {
            case "PENDING_REVIEW" -> "Chờ xưởng kiểm tra";
            case "QUOTED" -> "Đã báo giá";
            case "DEPOSITED" -> "Đã đặt cọc";
            case "PRINTING" -> "Đang in";
            case "COMPLETED" -> "Hoàn thành";
            case "CUSTOMER_DECLINED" -> "Khách từ chối";
            case "EXPIRED" -> "Hết hạn báo giá";
            case "ORDER_CREATED" -> "Đã tạo đơn";
            case "CLOSED" -> "Đã đóng";
            case "READY_TO_ORDER" -> "Sẵn sàng đặt trực tiếp";
            case "DRAFT" -> "Bản nháp";
            default -> status;
        };
    }

    public String getDepositPaidAtText() {
        return formatTs(depositPaidAt);
    }

    public String getProductionStartedAtText() {
        return formatTs(productionStartedAt);
    }

    public String getCompletedAtText() {
        return formatTs(completedAt);
    }

    private static String formatTs(Timestamp ts) {
        if (ts == null) return "—";
        return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(ts);
    }
}
