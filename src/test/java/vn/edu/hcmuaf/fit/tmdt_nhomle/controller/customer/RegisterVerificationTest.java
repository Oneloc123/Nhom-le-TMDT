package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import org.junit.jupiter.api.Test;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.DemoOtp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiểm thử quy tắc xác thực tài khoản sau đăng ký theo luồng OTP DEMO:
 * mã 6 số cố định 111111, so sánh không rò rỉ thời gian, hiệu lực 5 phút,
 * giới hạn số lần thử (chi tiết hành vi nằm ở {@code DemoOtpTest}).
 */
class RegisterVerificationTest {

    @Test
    void matchingCodeIsAccepted() {
        assertTrue(RegisterServlet.matchesCode("111111", "111111"));
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
        assertFalse(RegisterServlet.matchesCode("000000", "111111"));
    }

    @Test
    void codeLifetimeAndAttemptLimitAreBounded() {
        assertEquals(5 * 60 * 1000L, DemoOtp.TTL_MILLIS,
                "OTP demo phải hết hạn sau 5 phút như ghi chú trên giao diện.");
        assertEquals(5, RegisterServlet.MAX_VERIFY_ATTEMPTS,
                "Phải giới hạn số lần nhập sai để chống dò mã.");
        assertTrue(RegisterServlet.MAX_VERIFY_ATTEMPTS <= 10);
        assertEquals(DemoOtp.MAX_ATTEMPTS, RegisterServlet.MAX_VERIFY_ATTEMPTS,
                "Giới hạn của servlet phải khớp với thách thức OTP.");
    }

    @Test
    void demoOtpCodeIsTheFixedTestCode() {
        assertEquals("111111", DemoOtp.CODE);
    }
}
