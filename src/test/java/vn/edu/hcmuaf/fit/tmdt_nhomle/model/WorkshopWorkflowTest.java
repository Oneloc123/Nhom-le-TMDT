package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        assertTrue(WorkshopWorkflow.isValidUpdate(null, 0, WorkshopWorkflow.ACCEPTED));
        assertTrue(WorkshopWorkflow.isValidUpdate(WorkshopWorkflow.ACCEPTED, 0, WorkshopWorkflow.PRINTING));
        assertTrue(WorkshopWorkflow.isValidUpdate(WorkshopWorkflow.PRINTING, 55, WorkshopWorkflow.POST_PROCESSING));
        assertTrue(WorkshopWorkflow.isValidUpdate(WorkshopWorkflow.POST_PROCESSING, 85, WorkshopWorkflow.READY_FOR_SHIPPING));
        assertFalse(WorkshopWorkflow.isValidUpdate(null, 0, WorkshopWorkflow.PRINTING));
        assertFalse(WorkshopWorkflow.isValidUpdate(WorkshopWorkflow.PRINTING, 55, WorkshopWorkflow.PRINTING));
        assertFalse(WorkshopWorkflow.isValidUpdate(WorkshopWorkflow.POST_PROCESSING, 85, WorkshopWorkflow.ACCEPTED));
    }
    @Test
    void assignsProgressMilestonesOnTheServer() {
        assertEquals(0, WorkshopWorkflow.progressForStage(WorkshopWorkflow.ACCEPTED));
        assertEquals(55, WorkshopWorkflow.progressForStage(WorkshopWorkflow.PRINTING));
        assertEquals(85, WorkshopWorkflow.progressForStage(WorkshopWorkflow.POST_PROCESSING));
        assertEquals(100, WorkshopWorkflow.progressForStage(WorkshopWorkflow.READY_FOR_SHIPPING));
        assertEquals(55, WorkshopWorkflow.progressForStage(" printing "));
        assertTrue(WorkshopWorkflow.isValidUpdate(" accepted ", 0, " PRINTING "));
        assertThrows(IllegalArgumentException.class, () -> WorkshopWorkflow.progressForStage("UNKNOWN"));
    }
}

