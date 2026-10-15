package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

import java.math.BigDecimal;

public class OrderItem {
    private final int productId;
    private final String productName;
    private final BigDecimal unitPrice;
    private final int quantity;

    public OrderItem(int productId, String productName, BigDecimal unitPrice, int quantity) {
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public int getProductId() { return productId; }
    public String getProductName() { return productName; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
