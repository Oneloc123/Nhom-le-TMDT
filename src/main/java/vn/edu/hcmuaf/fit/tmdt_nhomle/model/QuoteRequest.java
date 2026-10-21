package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class QuoteRequest {

    public static final String STATUS_PENDING = "Chờ xử lý";
    public static final String STATUS_QUOTED = "Đã báo giá";
    public static final String STATUS_CLOSED = "Đã đóng";

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
    private String sellerResponseNote;
    private String status = STATUS_PENDING;

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

    public String getSellerResponseNote() { return sellerResponseNote; }
    public void setSellerResponseNote(String sellerResponseNote) { this.sellerResponseNote = sellerResponseNote; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
