package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiểm thử quy tắc xác thực tài khoản sau đăng ký (mã 6 số, so sánh không rò rỉ
 * thời gian, hiệu lực 15 phút, giới hạn số lần thử).
 */
class RegisterVerificationTest {

    @Test
    void matchingCodeIsAccepted() {
        assertTrue(RegisterServlet.matchesCode("123456", "123456"));
    }

    @Test
    void wrongLengthOrNullCodeIsRejected() {
        assertFalse(RegisterServlet.matchesCode("12345", "123456"));
        assertFalse(RegisterServlet.matchesCode("1234567", "123456"));
        assertFalse(RegisterServlet.matchesCode(null, "123456"));
        assertFalse(RegisterServlet.matchesCode("123456", null));
    }

    @Test
    void differentCodeIsRejected() {
        assertFalse(RegisterServlet.matchesCode("000000", "999999"));
    }

    @Test
    void codeLifetimeAndAttemptLimitAreBounded() {
        assertEquals(15 * 60 * 1000L, RegisterServlet.CODE_TTL_MILLIS,
                "Mã xác thực phải hết hạn sau 15 phút như nội dung email.");
        assertEquals(5, RegisterServlet.MAX_VERIFY_ATTEMPTS,
                "Phải giới hạn số lần nhập sai để chống dò mã.");
        assertTrue(RegisterServlet.MAX_VERIFY_ATTEMPTS <= 10);
    }
}
