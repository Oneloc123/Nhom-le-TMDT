package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import org.junit.jupiter.api.Test;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.OrderStatus;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class GhnDeliveryWorkflowTest {
    @Test
    void onlyDeliveredStatusEnablesReceiptConfirmation() {
        assertTrue(GhnDeliveryStage.from("delivered").delivered());
        assertFalse(GhnDeliveryStage.from("ready_to_pick").inCarrierCustody());
        assertFalse(GhnDeliveryStage.from("delivery_fail").terminal());
        assertEquals(GhnDeliveryStage.DELIVERING, GhnDeliveryStage.from("delivering"));
        assertFalse(GhnDeliveryStage.from("returned").delivered());
        assertTrue(GhnDeliveryStage.from("returned").terminal());
        assertNull(GhnDeliveryStage.from("unknown"));
    }

    @Test
    void codUsesCheckoutTotalAndPaidOrdersAreNotCollectedAgain() {
        var total = new BigDecimal("120900.00");
        assertEquals(120900, GhnDeliveryDao.codAmount(OrderStatus.PAYMENT_COD, total));
        assertEquals(0, GhnDeliveryDao.codAmount(OrderStatus.PAYMENT_PAID, total));
        assertThrows(IllegalArgumentException.class,
                () -> GhnDeliveryDao.codAmount(OrderStatus.PAYMENT_UNPAID, total));
        assertThrows(IllegalArgumentException.class,
                () -> GhnDeliveryDao.codAmount(OrderStatus.PAYMENT_COD, new BigDecimal("50000001")));
    }
}
