package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import org.junit.jupiter.api.Test;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Order;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.OrderItem;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderCheckoutBehaviorTest {

    @Test
    void shippingFeeUsesTheConfiguredThreshold() {
        assertEquals(OrderService.SHIPPING_FEE,
                OrderService.calculateShippingFee(new BigDecimal("1999999")));
        assertEquals(BigDecimal.ZERO,
                OrderService.calculateShippingFee(new BigDecimal("2000000")));
        assertEquals(OrderService.SHIPPING_FEE, OrderService.calculateShippingFee(null));
    }

    @Test
    void paymentItemCountSumsQuantitiesInsteadOfCountingLines() {
        Order order = new Order();
        OrderItem first = new OrderItem();
        first.setQuantity(2);
        OrderItem second = new OrderItem();
        second.setQuantity(5);
        order.getItems().add(first);
        order.getItems().add(second);

        assertEquals(7, order.getItemCount());
    }
}
