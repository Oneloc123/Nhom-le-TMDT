package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.OrderStore;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.CartItem;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Order;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.OrderItem;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;

import java.util.List;

public class OrderService {
    private final CartService cartService = new CartService();

    public Order createOrder(User customer, String phone, String address, HttpSession session) {
        List<CartItem> cartItems = cartService.items(session);
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Giỏ hàng đang trống.");
        }
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập số điện thoại nhận hàng.");
        }
        if (address == null || address.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập địa chỉ nhận hàng.");
        }

        List<OrderItem> items = cartItems.stream()
                .map(item -> new OrderItem(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getProduct().getPrice(),
                        item.getQuantity()))
                .toList();

        Order order = new Order(
                OrderStore.nextOrderNumber(),
                customer.getId(),
                customer.getFullName(),
                phone.trim(),
                address.trim(),
                items,
                cartService.subtotal(session));
        OrderStore.save(order);
        cartService.clear(session);
        return order;
    }
}
