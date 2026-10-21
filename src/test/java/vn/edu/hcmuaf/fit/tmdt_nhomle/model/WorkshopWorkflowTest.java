package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorkshopWorkflowTest {
    @Test
    void productionUpdatesRequireConfirmedOrderAndPaidOrCodPayment() {
        assertTrue(WorkshopWorkflow.canStart(OrderStatus.CONFIRMED, OrderStatus.PAYMENT_PAID));
        assertTrue(WorkshopWorkflow.canStart(OrderStatus.PROCESSING, OrderStatus.PAYMENT_COD));
        assertFalse(WorkshopWorkflow.canStart(OrderStatus.PENDING_PAYMENT, OrderStatus.PAYMENT_PAID));
        assertFalse(WorkshopWorkflow.canStart(OrderStatus.CONFIRMED, OrderStatus.PAYMENT_UNPAID));
    }

    @Test
    void productionStagesFollowOneForwardSequence() {
        assertTrue(WorkshopWorkflow.isValidUpdate(null, 0, WorkshopWorkflow.ACCEPTED, 0));
        assertTrue(WorkshopWorkflow.isValidUpdate(WorkshopWorkflow.ACCEPTED, 0,
                WorkshopWorkflow.PRINTING, 10));
        assertTrue(WorkshopWorkflow.isValidUpdate(WorkshopWorkflow.PRINTING, 60,
                WorkshopWorkflow.POST_PROCESSING, 70));
        assertTrue(WorkshopWorkflow.isValidUpdate(WorkshopWorkflow.POST_PROCESSING, 90,
                WorkshopWorkflow.READY_FOR_SHIPPING, 100));
        assertFalse(WorkshopWorkflow.isValidUpdate(null, 0, WorkshopWorkflow.PRINTING, 10));
        assertFalse(WorkshopWorkflow.isValidUpdate(WorkshopWorkflow.PRINTING, 60,
                WorkshopWorkflow.ACCEPTED, 0));
        assertFalse(WorkshopWorkflow.isValidUpdate(WorkshopWorkflow.POST_PROCESSING, 90,
                WorkshopWorkflow.READY_FOR_SHIPPING, 99));
        assertFalse(WorkshopWorkflow.isValidUpdate(WorkshopWorkflow.PRINTING, 60,
                WorkshopWorkflow.PRINTING, 60));
    }
}
