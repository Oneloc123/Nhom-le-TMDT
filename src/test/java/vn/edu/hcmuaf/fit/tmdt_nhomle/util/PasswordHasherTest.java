package vn.edu.hcmuaf.fit.tmdt_nhomle.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    @Test
    void hashThenVerifySucceeds() {
        String stored = PasswordHasher.hash("MatKhau123");
        assertTrue(stored.startsWith("pbkdf2$"));
        assertFalse(stored.contains("MatKhau123"), "Không được lưu plaintext");
        assertTrue(PasswordHasher.verify("MatKhau123", stored));
    }

    @Test
    void wrongPasswordFails() {
        String stored = PasswordHasher.hash("MatKhau123");
        assertFalse(PasswordHasher.verify("sai-mat-khau", stored));
        assertFalse(PasswordHasher.verify("", stored));
        assertFalse(PasswordHasher.verify(null, stored));
    }

    @Test
    void samePasswordHashesDifferently() {
        assertNotEquals(PasswordHasher.hash("abc123"), PasswordHasher.hash("abc123"),
                "Mỗi lần băm phải dùng salt ngẫu nhiên");
    }

    @Test
    void malformedStoredValueFails() {
        assertFalse(PasswordHasher.verify("x", "plaintext"));
        assertFalse(PasswordHasher.verify("x", "pbkdf2$abc$bad$bad"));
        assertFalse(PasswordHasher.verify("x", null));
    }

    @Test
    void emptyPasswordRejected() {
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hash(""));
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hash(null));
    }
}
