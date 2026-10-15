package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Order {
    public static final String STATUS_PENDING_PAYMENT = "Chờ thanh toán";
    public static final String STATUS_CONFIRMED = "Đã xác nhận";
    public static final String PAYMENT_UNPAID = "Chưa thanh toán";
    public static final String PAYMENT_PAID = "Đã thanh toán";

    private final String orderNumber;
    private final int customerId;
    private final String customerName;
    private final String phone;
    private final String address;
    private final List<OrderItem> items;
    private final BigDecimal total;
    private final String createdAt;
    private String status;
    private String paymentStatus;
    private String paymentMethod;

    public Order(String orderNumber, int customerId, String customerName,
                 String phone, String address, List<OrderItem> items,
                 BigDecimal total) {
        this.orderNumber = orderNumber;
        this.customerId = customerId;
        this.customerName = customerName;
        this.phone = phone;
        this.address = address;
        this.items = List.copyOf(items);
        this.total = total;
        this.createdAt = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        this.status = STATUS_PENDING_PAYMENT;
        this.paymentStatus = PAYMENT_UNPAID;
    }

    public String getOrderNumber() { return orderNumber; }
    public int getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public List<OrderItem> getItems() { return items; }
    public BigDecimal getTotal() { return total; }
    public String getCreatedAt() { return createdAt; }
    public String getStatus() { return status; }
    public String getPaymentStatus() { return paymentStatus; }
    public String getPaymentMethod() { return paymentMethod; }
    public int getItemCount() {
        return items.stream().mapToInt(OrderItem::getQuantity).sum();
    }

    public void markPaid(String paymentMethod) {
        this.paymentMethod = paymentMethod;
        this.paymentStatus = PAYMENT_PAID;
        this.status = STATUS_CONFIRMED;
    }
}
