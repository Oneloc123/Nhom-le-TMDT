package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Order;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public final class OrderStore {
    private static final Map<String, Order> ORDERS = new LinkedHashMap<>();
    private static final AtomicLong SEQUENCE = new AtomicLong(1000);

    private OrderStore() {
    }

    public static synchronized Order save(Order order) {
        ORDERS.put(order.getOrderNumber(), order);
        return order;
    }

    public static synchronized Optional<Order> findByNumber(String orderNumber) {
        return Optional.ofNullable(ORDERS.get(orderNumber));
    }

    public static synchronized List<Order> findAll() {
        List<Order> orders = new ArrayList<>(ORDERS.values());
        Collections.reverse(orders);
        return List.copyOf(orders);
    }

    public static synchronized List<Order> findByCustomerId(int customerId) {
        return ORDERS.values().stream()
                .filter(order -> order.getCustomerId() == customerId)
                .sorted(java.util.Comparator.comparing(Order::getCreatedAt).reversed())
                .toList();
    }

    public static String nextOrderNumber() {
        return "PK-" + SEQUENCE.incrementAndGet();
    }
}
