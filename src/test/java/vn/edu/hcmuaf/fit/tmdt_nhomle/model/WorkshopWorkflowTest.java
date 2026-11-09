package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkshopWorkflowTest {
    @Test
    void allowsProductionOnlyForConfirmedPaidOrCodOrders() {
        assertTrue(WorkshopWorkflow.canStart(OrderStatus.CONFIRMED, OrderStatus.PAYMENT_PAID));
        assertTrue(WorkshopWorkflow.canStart(OrderStatus.PROCESSING, OrderStatus.PAYMENT_COD));
        assertFalse(WorkshopWorkflow.canStart(OrderStatus.PENDING_PAYMENT, OrderStatus.PAYMENT_UNPAID));
    }

    @Test
    void enforcesProductionStageOrderAndProgress() {
        assertTrue(WorkshopWorkflow.isValidUpdate(null, 0, WorkshopWorkflow.ACCEPTED, 0));
        assertTrue(WorkshopWorkflow.isValidUpdate(WorkshopWorkflow.ACCEPTED, 0, WorkshopWorkflow.PRINTING, 20));
        assertTrue(WorkshopWorkflow.isValidUpdate(WorkshopWorkflow.PRINTING, 80,
                WorkshopWorkflow.POST_PROCESSING, 85));
        assertTrue(WorkshopWorkflow.isValidUpdate(WorkshopWorkflow.POST_PROCESSING, 95,
                WorkshopWorkflow.READY_FOR_SHIPPING, 100));
        assertFalse(WorkshopWorkflow.isValidUpdate(null, 0, WorkshopWorkflow.PRINTING, 20));
        assertFalse(WorkshopWorkflow.isValidUpdate(WorkshopWorkflow.PRINTING, 60,
                WorkshopWorkflow.PRINTING, 50));
        assertFalse(WorkshopWorkflow.isValidUpdate(WorkshopWorkflow.POST_PROCESSING, 95,
                WorkshopWorkflow.READY_FOR_SHIPPING, 99));
    }
}
