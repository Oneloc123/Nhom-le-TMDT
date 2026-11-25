package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GhnShippingServiceTest {

    @Test
    void usesFullFeeInsteadOfOnlyTheBaseServiceFee() {
        assertEquals(BigDecimal.valueOf(25_900), GhnShippingService.readFee("""
                {"code":200,"data":{"fee":{"main_service":20900},"total_fee":25900}}
                """));
    }

    @Test
    void doesNotTreatMissingFeeAsFreeShipping() {
        assertThrows(GhnShippingService.ShippingUnavailable.class,
                () -> GhnShippingService.readFee("{\"code\":200,\"data\":{}}"));
        assertThrows(GhnShippingService.ShippingUnavailable.class,
                () -> GhnShippingService.readFee("{\"code\":400,\"data\":null}"));
    }
}
