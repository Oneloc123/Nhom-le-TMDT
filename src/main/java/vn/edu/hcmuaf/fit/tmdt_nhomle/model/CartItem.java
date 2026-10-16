package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

import java.math.BigDecimal;

public class CartItem {

    private long cartItemId;
    private long shopProductId;
    private Product product;
    private String shopName;
    private String material;
    private int minimumQuantity;
    private BigDecimal unitPrice;
    private int quantity;
    private Long designFileId;

    public long getCartItemId() { return cartItemId; }
    public void setCartItemId(long cartItemId) { this.cartItemId = cartItemId; }

    public long getShopProductId() { return shopProductId; }
    public void setShopProductId(long shopProductId) { this.shopProductId = shopProductId; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }

    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }

    public int getMinimumQuantity() { return minimumQuantity; }
    public void setMinimumQuantity(int minimumQuantity) { this.minimumQuantity = minimumQuantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public Long getDesignFileId() { return designFileId; }
    public void setDesignFileId(Long designFileId) { this.designFileId = designFileId; }

    public BigDecimal getSubtotal() {
        if (unitPrice == null) {
            return BigDecimal.ZERO;
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
