package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

import java.math.BigDecimal;

public class ShopOffer {
    private long shopProductId;
    private long shopId;
    private String shopName;
    private long productId;
    private String sku;
    private String printingMethod;
    private String material;
    private BigDecimal unitPrice;
    private int minimumQuantity;
    private int productionDays;
    private String stockStatus;
    private boolean active;

    public long getShopProductId() { return shopProductId; }
    public void setShopProductId(long shopProductId) { this.shopProductId = shopProductId; }

    public long getShopId() { return shopId; }
    public void setShopId(long shopId) { this.shopId = shopId; }

    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }

    public long getProductId() { return productId; }
    public void setProductId(long productId) { this.productId = productId; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getPrintingMethod() { return printingMethod; }
    public void setPrintingMethod(String printingMethod) { this.printingMethod = printingMethod; }

    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public int getMinimumQuantity() { return minimumQuantity; }
    public void setMinimumQuantity(int minimumQuantity) { this.minimumQuantity = minimumQuantity; }

    public int getProductionDays() { return productionDays; }
    public void setProductionDays(int productionDays) { this.productionDays = productionDays; }

    public String getStockStatus() { return stockStatus; }
    public void setStockStatus(String stockStatus) { this.stockStatus = stockStatus; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isPurchasable() {
        return active && unitPrice != null
                && (stockStatus == null || "IN_STOCK".equalsIgnoreCase(stockStatus));
    }
}
