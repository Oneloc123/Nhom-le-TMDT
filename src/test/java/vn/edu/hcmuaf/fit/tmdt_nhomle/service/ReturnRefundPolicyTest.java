package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ReturnRefundPolicyTest {
    @Test
    void deliveryWindowIncludesDeadlineButRejectsEarlierAndLaterOrMissingDelivery() {
        LocalDateTime delivered = LocalDateTime.of(2026, 11, 1, 10, 0);
        assertTrue(ReturnRefundPolicy.isWithinReturnWindow(delivered, delivered));
        assertTrue(ReturnRefundPolicy.isWithinReturnWindow(delivered, delivered.plusDays(7)));
        assertFalse(ReturnRefundPolicy.isWithinReturnWindow(delivered, delivered.plusDays(7).plusNanos(1)));
        assertFalse(ReturnRefundPolicy.isWithinReturnWindow(null, delivered));
        assertFalse(ReturnRefundPolicy.isWithinReturnWindow(delivered.plusMinutes(1), delivered));
    }

    @Test
    void onlyRecognizedRequestTypesAndReasonsAreAccepted() {
        assertTrue(ReturnRefundPolicy.isValidRequestType("RETURN_AND_REFUND"));
        assertTrue(ReturnRefundPolicy.isValidRequestType("REFUND_ONLY"));
        assertFalse(ReturnRefundPolicy.isValidRequestType("DELETE_ORDER"));
        assertTrue(ReturnRefundPolicy.isValidReason("DAMAGED"));
        assertFalse(ReturnRefundPolicy.isValidReason("arbitrary"));
    }

    @Test
    void evidenceIsRequiredForClaimsAndCustomMadeItems() {
        assertTrue(ReturnRefundPolicy.requiresEvidence("DEFECTIVE", false));
        assertTrue(ReturnRefundPolicy.requiresEvidence("CHANGED_MIND", true));
        assertFalse(ReturnRefundPolicy.requiresEvidence("CHANGED_MIND", false));
        assertFalse(ReturnRefundPolicy.isReasonAllowedForCustomItem("CHANGED_MIND"));
        assertTrue(ReturnRefundPolicy.isReasonAllowedForCustomItem("WRONG_ITEM"));
    }

    @Test
    void acceptedQuantityCannotExceedAvailableQuantity() {
        assertTrue(ReturnRefundPolicy.isQuantityAvailable(1, 2));
        assertTrue(ReturnRefundPolicy.isQuantityAvailable(2, 2));
        assertFalse(ReturnRefundPolicy.isQuantityAvailable(3, 2));
        assertFalse(ReturnRefundPolicy.isQuantityAvailable(0, 2));
    }

    @Test
    void amountUsesPurchasedUnitPriceAndTwoDecimalPlaces() {
        assertEquals(new BigDecimal("12500.50"),
                ReturnRefundPolicy.requestedAmount(new BigDecimal("6250.25"), 2));
        assertThrows(IllegalArgumentException.class,
                () -> ReturnRefundPolicy.requestedAmount(new BigDecimal("-1"), 1));
    }

    @Test
    void stateTransitionsAreExplicitAndTerminalStatesCannotReopen() {
        assertTrue(ReturnRefundPolicy.canTransition("SUBMITTED", "UNDER_REVIEW"));
        assertTrue(ReturnRefundPolicy.canTransition("UNDER_REVIEW", "AWAITING_RETURN_SHIPMENT"));
        assertTrue(ReturnRefundPolicy.canTransition("AWAITING_RETURN_SHIPMENT", "RETURN_SHIPPED"));
        assertTrue(ReturnRefundPolicy.canTransition("RETURN_SHIPPED", "RETURN_RECEIVED"));
        assertTrue(ReturnRefundPolicy.canTransition("RETURN_RECEIVED", "REFUND_PROCESSING"));
        assertTrue(ReturnRefundPolicy.canTransition("REFUND_PROCESSING", "REFUND_FAILED"));
        assertTrue(ReturnRefundPolicy.canTransition("REFUND_FAILED", "REFUND_PROCESSING"));
        assertFalse(ReturnRefundPolicy.canTransition("REFUNDED", "REFUND_PROCESSING"));
        assertFalse(ReturnRefundPolicy.canTransition("REJECTED", "UNDER_REVIEW"));
    }
}
