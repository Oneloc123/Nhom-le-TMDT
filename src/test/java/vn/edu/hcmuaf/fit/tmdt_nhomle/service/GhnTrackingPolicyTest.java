package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class GhnTrackingPolicyTest {
    @Test
    void mapsGhnStatusCodesToCanonicalShipmentStates() {
        assertEquals("PREPARING", GhnTrackingPolicy.mapStatus("ready_to_pick"));
        assertEquals("SHIPPED", GhnTrackingPolicy.mapStatus("picked"));
        assertEquals("IN_TRANSIT", GhnTrackingPolicy.mapStatus("transporting"));
        assertEquals("IN_TRANSIT", GhnTrackingPolicy.mapStatus("delivering"));
        assertEquals("DELIVERED", GhnTrackingPolicy.mapStatus("delivered"));
        assertEquals("FAILED", GhnTrackingPolicy.mapStatus("delivery_fail"));
        assertEquals("RETURNED", GhnTrackingPolicy.mapStatus("returned"));
        assertNull(GhnTrackingPolicy.mapStatus("unknown_status"));
    }

    @Test
    void allowsCarrierToSkipIntermediateStatusesButNeverReopenTerminals() {
        assertTrue(GhnTrackingPolicy.canApplyCarrierStatus("PREPARING", "DELIVERED"));
        assertTrue(GhnTrackingPolicy.canApplyCarrierStatus("FAILED", "IN_TRANSIT"));
        assertTrue(GhnTrackingPolicy.canApplyCarrierStatus("IN_TRANSIT", "DELIVERED"));
        assertFalse(GhnTrackingPolicy.canApplyCarrierStatus("DELIVERED", "IN_TRANSIT"));
        assertFalse(GhnTrackingPolicy.canApplyCarrierStatus("RETURNED", "DELIVERED"));
    }

    @Test
    void usesThreeDayReceiptWindowAndReminderOneDayBefore() {
        assertEquals(Duration.ofHours(72), GhnTrackingPolicy.CUSTOMER_CONFIRMATION_WINDOW);
        assertEquals(Duration.ofHours(24), GhnTrackingPolicy.CUSTOMER_REMINDER_BEFORE_DEADLINE);
    }

    @Test
    void recognizesCarrierFinalStatesThatMustNeverReopen() {
        assertTrue(GhnTrackingPolicy.isFinalRawStatus("delivered"));
        assertTrue(GhnTrackingPolicy.isFinalRawStatus("returned"));
        assertTrue(GhnTrackingPolicy.isFinalRawStatus("exception"));
        assertTrue(GhnTrackingPolicy.isFinalRawStatus("lost"));
        assertTrue(GhnTrackingPolicy.isFinalRawStatus("damage"));
        assertTrue(GhnTrackingPolicy.isFinalRawStatus("scrap"));
        assertFalse(GhnTrackingPolicy.isFinalRawStatus("delivery_fail"));
        assertFalse(GhnTrackingPolicy.isFinalRawStatus("transporting"));
    }

    @Test
    void identifiesGhnCarrierLabelsWithoutTreatingOtherCarriersAsGhn() {
        assertEquals("GHN", GhnTrackingPolicy.providerForCarrierLabel("GHN"));
        assertEquals("GHN", GhnTrackingPolicy.providerForCarrierLabel("Giao Hàng Nhanh"));
        assertEquals("MANUAL", GhnTrackingPolicy.providerForCarrierLabel("Viettel Post"));
        assertEquals("MANUAL", GhnTrackingPolicy.providerForCarrierLabel(null));
    }

    @Test
    void recognizesOnlyDocumentedGhnWebhookTypes() {
        assertTrue(GhnTrackingPolicy.shouldTrackEventType("switch_status"));
        assertTrue(GhnTrackingPolicy.shouldTrackEventType("cod"));
        assertFalse(GhnTrackingPolicy.shouldTrackEventType("arbitrary"));
    }
}
