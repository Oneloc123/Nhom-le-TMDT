package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeliveryDisputePolicyTest {
    @Test
    void acceptsOnlySupportedCustomerIssueTypes() {
        assertEquals("NOT_RECEIVED", DeliveryDisputePolicy.normalizeIssueType(" not_received "));
        assertEquals("DAMAGED", DeliveryDisputePolicy.normalizeIssueType("damaged"));
        assertThrows(IllegalArgumentException.class,
                () -> DeliveryDisputePolicy.normalizeIssueType("UNKNOWN"));
    }

    @Test
    void mapsAdminActionsToExpectedStatuses() {
        assertEquals("UNDER_REVIEW", DeliveryDisputePolicy.targetStatus("review"));
        assertEquals("RESOLVED", DeliveryDisputePolicy.targetStatus("resolve"));
        assertEquals("REJECTED", DeliveryDisputePolicy.targetStatus("reject"));
        assertThrows(IllegalArgumentException.class,
                () -> DeliveryDisputePolicy.targetStatus("delete"));
    }

    @Test
    void onlyActiveDisputesBlockOrderCompletion() {
        assertTrue(DeliveryDisputePolicy.blocksCompletion("OPEN"));
        assertTrue(DeliveryDisputePolicy.blocksCompletion("UNDER_REVIEW"));
        assertFalse(DeliveryDisputePolicy.blocksCompletion("RESOLVED"));
        assertFalse(DeliveryDisputePolicy.blocksCompletion("REJECTED"));
    }

    @Test
    void terminalDisputesCannotBeReopened() {
        assertTrue(DeliveryDisputePolicy.canTransition("OPEN", "UNDER_REVIEW"));
        assertTrue(DeliveryDisputePolicy.canTransition("UNDER_REVIEW", "RESOLVED"));
        assertFalse(DeliveryDisputePolicy.canTransition("RESOLVED", "UNDER_REVIEW"));
        assertFalse(DeliveryDisputePolicy.canTransition("REJECTED", "RESOLVED"));
    }
}
