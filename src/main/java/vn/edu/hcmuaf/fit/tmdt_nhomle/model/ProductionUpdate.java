package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

import java.time.LocalDateTime;

public class ProductionUpdate {
    private long productionUpdateId;
    private long orderItemId;
    private Long shopId;
    private String shopName;
    private String stage;
    private int progressPercent;
    private String message;
    private LocalDateTime createdAt;

    public long getProductionUpdateId() { return productionUpdateId; }
    public void setProductionUpdateId(long productionUpdateId) { this.productionUpdateId = productionUpdateId; }

    public long getOrderItemId() { return orderItemId; }
    public void setOrderItemId(long orderItemId) { this.orderItemId = orderItemId; }

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }

    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }

    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }

    public int getProgressPercent() { return progressPercent; }
    public void setProgressPercent(int progressPercent) { this.progressPercent = progressPercent; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCreatedAtText() {
        return createdAt == null ? "" : createdAt.format(java.time.format.DateTimeFormatter
                .ofPattern("dd/MM/yyyy HH:mm"));
    }
}
