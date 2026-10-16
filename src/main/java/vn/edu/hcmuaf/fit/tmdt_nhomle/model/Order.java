package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Order {

    private long orderId;
    private String orderCode;
    private long userId;
    private Long addressId;
    private String customerName;
    private String recipientName;
    private String recipientPhone;
    private String shippingAddress;
    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal shippingFee = BigDecimal.ZERO;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private String currency = "VND";
    private String status = OrderStatus.PENDING_PAYMENT;
    private String paymentStatus = OrderStatus.PAYMENT_UNPAID;
    private String paymentMethod;
    private String shippingStatus = OrderStatus.SHIPPING_NONE;
    private LocalDateTime placedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private final List<OrderItem> items = new ArrayList<>();
    private final List<StatusHistory> timeline = new ArrayList<>();
    private final List<Shipment> shipments = new ArrayList<>();
    private final List<ProductionUpdate> productionUpdates = new ArrayList<>();

    public long getOrderId() { return orderId; }
    public void setOrderId(long orderId) { this.orderId = orderId; }

    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }

    public String getOrderNumber() { return orderCode; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public Long getAddressId() { return addressId; }
    public void setAddressId(Long addressId) { this.addressId = addressId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

    public String getRecipientPhone() { return recipientPhone; }
    public void setRecipientPhone(String recipientPhone) { this.recipientPhone = recipientPhone; }

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getShippingFee() { return shippingFee; }
    public void setShippingFee(BigDecimal shippingFee) { this.shippingFee = shippingFee; }

    public BigDecimal getTotal() { return totalAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getShippingStatus() { return shippingStatus; }
    public void setShippingStatus(String shippingStatus) { this.shippingStatus = shippingStatus; }

    public LocalDateTime getPlacedAt() { return placedAt; }
    public void setPlacedAt(LocalDateTime placedAt) { this.placedAt = placedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

    public List<OrderItem> getItems() { return items; }
    public List<StatusHistory> getTimeline() { return timeline; }
    public List<Shipment> getShipments() { return shipments; }
    public List<ProductionUpdate> getProductionUpdates() { return productionUpdates; }

    public int getItemCount() {
        return items.stream().mapToInt(OrderItem::getQuantity).sum();
    }

    public String getCreatedAt() {
        LocalDateTime at = placedAt;
        return at == null ? "" : at.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    public boolean isCancellable() {
        return OrderStatus.cancellableByCustomer(status);
    }

    public boolean isPaid() {
        return OrderStatus.PAYMENT_PAID.equals(paymentStatus);
    }

    public boolean isPaidOrCod() {
        return isPaid() || OrderStatus.PAYMENT_COD.equals(paymentStatus);
    }
}
