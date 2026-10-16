package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

import java.time.LocalDateTime;

public class Shipment {
    private long shipmentId;
    private long orderId;
    private Long shopId;
    private String shopName;
    private String carrier;
    private String trackingNumber;
    private String status;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;

    public long getShipmentId() { return shipmentId; }
    public void setShipmentId(long shipmentId) { this.shipmentId = shipmentId; }

    public long getOrderId() { return orderId; }
    public void setOrderId(long orderId) { this.orderId = orderId; }

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }

    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }

    public String getCarrier() { return carrier; }
    public void setCarrier(String carrier) { this.carrier = carrier; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getShippedAt() { return shippedAt; }
    public void setShippedAt(LocalDateTime shippedAt) { this.shippedAt = shippedAt; }

    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }

    public String getShippedAtText() {
        return shippedAt == null ? "—" : shippedAt.format(java.time.format.DateTimeFormatter
                .ofPattern("dd/MM/yyyy HH:mm"));
    }

    public String getDeliveredAtText() {
        return deliveredAt == null ? "—" : deliveredAt.format(java.time.format.DateTimeFormatter
                .ofPattern("dd/MM/yyyy HH:mm"));
    }
}
