package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChangeRequestPolicyTest {
    @Test
    void allowsAutomaticHandlingBeforeProductionStarts() {
        assertEquals(ChangeRequestPolicy.Decision.AUTO_APPLY,
                ChangeRequestPolicy.decide(false, null, 0));
    }

    @Test
    void requiresWorkshopApprovalAtTheBeginningOfProduction() {
        assertEquals(ChangeRequestPolicy.Decision.SHOP_APPROVAL,
                ChangeRequestPolicy.decide(true, "ACCEPTED", 0));
        assertEquals(ChangeRequestPolicy.Decision.SHOP_APPROVAL,
                ChangeRequestPolicy.decide(true, "PRINTING", 19));
    }

    @Test
    void requiresWorkshopDecisionAfterPrintingHasProgressed() {
        assertEquals(ChangeRequestPolicy.Decision.SHOP_DECIDES,
                ChangeRequestPolicy.decide(true, "PRINTING", 20));
        assertEquals(ChangeRequestPolicy.Decision.SHOP_DECIDES,
                ChangeRequestPolicy.decide(true, " POST_PROCESSING ", 90));
    }

    @Test
    void blocksChangesOnceTheOrderIsReadyForShippingOrBeyond() {
        assertEquals(ChangeRequestPolicy.Decision.BLOCKED,
                ChangeRequestPolicy.decide(true, "READY_FOR_SHIPPING", 100));
        assertEquals(ChangeRequestPolicy.Decision.BLOCKED,
                ChangeRequestPolicy.decide(true, "SHIPPED", 100));
        assertEquals(ChangeRequestPolicy.Decision.BLOCKED,
                ChangeRequestPolicy.decide(true, "COMPLETED", 100));
    }
}
