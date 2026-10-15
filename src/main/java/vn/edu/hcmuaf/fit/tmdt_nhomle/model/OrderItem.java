package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

import java.math.BigDecimal;

/**
 * Dòng hàng trong đơn (bảng order_items) — giữ snapshot tên sản phẩm/tên gian
 * hàng/giá tại thời điểm đặt. JSP cũ dùng productName/quantity/subtotal.
 */
public class OrderItem {

    private long orderItemId;
    private long orderId;
    private Long productId;
    private Long shopProductId;
    private Long shopId;
    private Long designFileId;
    private String productName;
    private String shopName;
    private int quantity;
    private BigDecimal unitPrice = BigDecimal.ZERO;
    private BigDecimal lineTotal = BigDecimal.ZERO;
    private String status;
    private boolean reviewed;
    private boolean orderPaid;

    public long getOrderItemId() { return orderItemId; }
    public void setOrderItemId(long orderItemId) { this.orderItemId = orderItemId; }

    public long getOrderId() { return orderId; }
    public void setOrderId(long orderId) { this.orderId = orderId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getShopProductId() { return shopProductId; }
    public void setShopProductId(Long shopProductId) { this.shopProductId = shopProductId; }

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }

    public Long getDesignFileId() { return designFileId; }
    public void setDesignFileId(Long designFileId) { this.designFileId = designFileId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }

    /** Tên cũ của lineTotal — JSP lịch sử mua hàng dùng. */
    public BigDecimal getSubtotal() { return lineTotal; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    /** Dòng hàng đã có đánh giá chưa (order_item_id UNIQUE trong reviews). */
    public boolean isReviewed() { return reviewed; }
    public void setReviewed(boolean reviewed) { this.reviewed = reviewed; }

    public boolean isOrderPaid() { return orderPaid; }
    public void setOrderPaid(boolean orderPaid) { this.orderPaid = orderPaid; }

    /** Khách được đánh giá khi đơn đã thanh toán và dòng chưa có đánh giá. */
    public boolean isReviewable() {
        return orderPaid && !reviewed;
    }
}
