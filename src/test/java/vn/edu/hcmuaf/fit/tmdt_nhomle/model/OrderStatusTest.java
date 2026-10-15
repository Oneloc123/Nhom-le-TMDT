package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStatusTest {

    @Test
    void customerCanCancelPendingAndConfirmedOnly() {
        assertTrue(OrderStatus.cancellableByCustomer(OrderStatus.PENDING_PAYMENT));
        assertTrue(OrderStatus.cancellableByCustomer(OrderStatus.CONFIRMED));
        assertFalse(OrderStatus.cancellableByCustomer(OrderStatus.PROCESSING));
        assertFalse(OrderStatus.cancellableByCustomer(OrderStatus.SHIPPED));
        assertFalse(OrderStatus.cancellableByCustomer(OrderStatus.COMPLETED));
        assertFalse(OrderStatus.cancellableByCustomer(OrderStatus.CANCELLED));
    }

    @Test
    void illegalTransitionsRejected() {
        // Không cho "nhảy cóc" trạng thái.
        assertFalse(OrderStatus.canTransition(OrderStatus.PENDING_PAYMENT, OrderStatus.SHIPPED));
        assertFalse(OrderStatus.canTransition(OrderStatus.PENDING_PAYMENT, OrderStatus.COMPLETED));
        assertFalse(OrderStatus.canTransition(OrderStatus.CANCELLED, OrderStatus.CONFIRMED));
        assertFalse(OrderStatus.canTransition(OrderStatus.COMPLETED, OrderStatus.CANCELLED));
        assertFalse(OrderStatus.canTransition(null, OrderStatus.CONFIRMED));
        assertFalse(OrderStatus.canTransition(OrderStatus.CONFIRMED, null));
    }

    @Test
    void legalTransitionsAllowed() {
        assertTrue(OrderStatus.canTransition(OrderStatus.PENDING_PAYMENT, OrderStatus.CONFIRMED));
        assertTrue(OrderStatus.canTransition(OrderStatus.PENDING_PAYMENT, OrderStatus.CANCELLED));
        assertTrue(OrderStatus.canTransition(OrderStatus.CONFIRMED, OrderStatus.PROCESSING));
        assertTrue(OrderStatus.canTransition(OrderStatus.PROCESSING, OrderStatus.SHIPPED));
        assertTrue(OrderStatus.canTransition(OrderStatus.SHIPPED, OrderStatus.COMPLETED));
    }
}
