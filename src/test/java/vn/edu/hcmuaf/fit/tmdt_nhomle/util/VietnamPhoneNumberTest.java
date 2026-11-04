package vn.edu.hcmuaf.fit.tmdt_nhomle.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VietnamPhoneNumberTest {

    @Test
    void normalizesDomesticAndInternationalVietnameseNumbers() {
        assertEquals("+84901234567", VietnamPhoneNumber.normalize("0901234567"));
        assertEquals("+84901234567", VietnamPhoneNumber.normalize("+84901234567"));
        assertEquals("+84901234567", VietnamPhoneNumber.normalize("090 123 4567"));
    }

    @Test
    void rejectsUnsupportedPhoneNumbers() {
        assertThrows(IllegalArgumentException.class,
                () -> VietnamPhoneNumber.normalize("123456789"));
    }
}
