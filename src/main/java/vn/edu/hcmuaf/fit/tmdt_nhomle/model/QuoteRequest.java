package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class QuoteRequest {

    public static final String STATUS_PENDING = "Chờ xử lý";
    public static final String STATUS_QUOTED = "Đã báo giá";
    public static final String STATUS_DEPOSITED = "Đã đặt cọc";
    public static final String STATUS_PRINTING = "Đang in";
    public static final String STATUS_COMPLETED = "Hoàn thành";
    public static final String STATUS_CLOSED = "Đã đóng";

    public static final BigDecimal DEPOSIT_RATE = new BigDecimal("0.30");
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private long quoteRequestId;
    private long userId;
    private Long productId;
    private String productName;
    private Long preferredShopId;
    private String preferredShopName;
    private String companyName;
    private int requestedQuantity;
    private LocalDate requiredBy;
    private BigDecimal quotedAmount;
    private BigDecimal depositAmount;
    private LocalDateTime depositPaidAt;
    private LocalDateTime productionStartedAt;
    private LocalDateTime completedAt;
    private String productionNote;
    private Long orderId;
    private String orderCode;
    private String sellerResponseNote;
    private String status = STATUS_PENDING;

    /** Computes the 30% deposit rounded to whole VND. */
    public static BigDecimal calculateDepositAmount(BigDecimal quotedAmount) {
        if (quotedAmount == null || quotedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return quotedAmount.multiply(DEPOSIT_RATE).setScale(0, RoundingMode.HALF_UP);
    }

    public long getQuoteRequestId() { return quoteRequestId; }
    public void setQuoteRequestId(long quoteRequestId) { this.quoteRequestId = quoteRequestId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Long getPreferredShopId() { return preferredShopId; }
    public void setPreferredShopId(Long preferredShopId) { this.preferredShopId = preferredShopId; }

    public String getPreferredShopName() { return preferredShopName; }
    public void setPreferredShopName(String preferredShopName) { this.preferredShopName = preferredShopName; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public int getRequestedQuantity() { return requestedQuantity; }
    public void setRequestedQuantity(int requestedQuantity) { this.requestedQuantity = requestedQuantity; }

    public LocalDate getRequiredBy() { return requiredBy; }
    public void setRequiredBy(LocalDate requiredBy) { this.requiredBy = requiredBy; }

    public String getRequiredByText() {
        return requiredBy == null ? "—" : requiredBy.toString();
    }

    public BigDecimal getQuotedAmount() { return quotedAmount; }
    public void setQuotedAmount(BigDecimal quotedAmount) { this.quotedAmount = quotedAmount; }

    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }

    public BigDecimal getRequiredDepositAmount() {
        if (depositAmount != null && depositAmount.compareTo(BigDecimal.ZERO) > 0) {
            return depositAmount;
        }
        return calculateDepositAmount(quotedAmount);
    }

    public BigDecimal getRemainingAmount() {
        BigDecimal requiredDeposit = getRequiredDepositAmount();
        if (quotedAmount == null || requiredDeposit == null) {
            return null;
        }
        BigDecimal remaining = quotedAmount.subtract(requiredDeposit);
        return remaining.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remaining;
    }

    public LocalDateTime getDepositPaidAt() { return depositPaidAt; }
    public void setDepositPaidAt(LocalDateTime depositPaidAt) { this.depositPaidAt = depositPaidAt; }

    public String getDepositPaidAtText() {
        return depositPaidAt == null ? "—" : DISPLAY_TIME.format(depositPaidAt);
    }

    public LocalDateTime getProductionStartedAt() { return productionStartedAt; }
    public void setProductionStartedAt(LocalDateTime productionStartedAt) { this.productionStartedAt = productionStartedAt; }

    public String getProductionStartedAtText() {
        return productionStartedAt == null ? "—" : DISPLAY_TIME.format(productionStartedAt);
    }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public String getCompletedAtText() {
        return completedAt == null ? "—" : DISPLAY_TIME.format(completedAt);
    }

    public String getProductionNote() { return productionNote; }
    public void setProductionNote(String productionNote) { this.productionNote = productionNote; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }

    public String getSellerResponseNote() { return sellerResponseNote; }
    public void setSellerResponseNote(String sellerResponseNote) { this.sellerResponseNote = sellerResponseNote; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isDepositable() {
        return STATUS_QUOTED.equals(status)
                && quotedAmount != null
                && quotedAmount.compareTo(BigDecimal.ZERO) > 0;
    }
}
