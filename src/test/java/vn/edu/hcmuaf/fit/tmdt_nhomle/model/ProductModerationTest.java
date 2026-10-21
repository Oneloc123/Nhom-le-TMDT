package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductModerationTest {
    @Test
    void approvingDoesNotReactivateAnInactiveProduct() {
        ProductModeration.State next = ProductModeration.transition(
                ProductModeration.PENDING_REVIEW, false, "approve", null);
        assertEquals(ProductModeration.APPROVED, next.moderationStatus());
        assertFalse(next.active());
    }

    @Test
    void rejectionRequiresAReasonAndDoesNotChangeActivity() {
        assertThrows(IllegalArgumentException.class, () -> ProductModeration.transition(
                ProductModeration.PENDING_REVIEW, true, "reject", "short"));
        ProductModeration.State next = ProductModeration.transition(
                ProductModeration.PENDING_REVIEW, true, "reject", "Missing product details");
        assertEquals(ProductModeration.REJECTED, next.moderationStatus());
        assertEquals("Missing product details", next.rejectionReason());
        assertTrue(next.active());
    }

    @Test
    void hiddenProductsCanOnlyBeShownByExplicitUnhideAction() {
        assertThrows(IllegalArgumentException.class, () -> ProductModeration.transition(
                ProductModeration.REJECTED, false, "unhide", null));
        ProductModeration.State next = ProductModeration.transition(
                ProductModeration.APPROVED, false, "unhide", null);
        assertTrue(next.active());
    }
}
