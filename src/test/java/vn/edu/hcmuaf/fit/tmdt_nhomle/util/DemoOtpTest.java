package vn.edu.hcmuaf.fit.tmdt_nhomle.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiểm thử thách thức OTP demo {@code 111111}: gắn email/mục đích, hiệu lực
 * 5 phút, giới hạn số lần sai, dùng một lần và thông điệp ghi chú DEMO.
 */
class DemoOtpTest {

    private static final long NOW = System.currentTimeMillis();

    @Test
    void demoCodeAndNoteAreExplicit() {
        assertEquals("111111", DemoOtp.CODE, "Mã OTP demo phải đúng 111111.");
        assertTrue(DemoOtp.DEMO_NOTE.contains("111111"));
        assertTrue(DemoOtp.DEMO_NOTE.contains("demo"),
                "Ghi chú phải nói rõ đây là chế độ demo, không gửi email thật.");
    }

    @Test
    void correctCodeForBoundEmailAndPurposePassesOnce() {
        DemoOtp.Challenge challenge =
                DemoOtp.Challenge.issue("Khach@Gmail.com", DemoOtp.PURPOSE_REGISTER);
        assertEquals("khach@gmail.com", challenge.email(), "Email phải được chuẩn hóa.");

        assertEquals(DemoOtp.VerifyResult.OK,
                challenge.verify("khach@gmail.com", DemoOtp.PURPOSE_REGISTER, "111111", NOW));
        assertTrue(challenge.used(), "OTP phải bị tiêu thụ sau khi xác thực thành công.");
        assertEquals(DemoOtp.VerifyResult.ALREADY_USED,
                challenge.verify("khach@gmail.com", DemoOtp.PURPOSE_REGISTER, "111111", NOW),
                "Không được dùng lại mã đã dùng.");
    }

    @Test
    void wrongCodeIsRejectedAndAttemptLimitApplies() {
        DemoOtp.Challenge challenge =
                DemoOtp.Challenge.issue("a@b.com", DemoOtp.PURPOSE_LOGIN);
        // 4 lần sai đầu trả MISMATCH; lần sai thứ 5 (đúng bằng MAX_ATTEMPTS) trả
        // TOO_MANY_ATTEMPTS vì người dùng đã dùng hết lượt — mã đúng ở lần 5
        // vẫn được chấp nhận (xem lần thử bên dưới với challenge khác).
        for (int i = 1; i < DemoOtp.MAX_ATTEMPTS; i++) {
            DemoOtp.VerifyResult result =
                    challenge.verify("a@b.com", DemoOtp.PURPOSE_LOGIN, "000000", NOW);
            assertEquals(DemoOtp.VerifyResult.MISMATCH, result, "Lần thử " + i);
        }
        assertEquals(DemoOtp.VerifyResult.TOO_MANY_ATTEMPTS,
                challenge.verify("a@b.com", DemoOtp.PURPOSE_LOGIN, "000000", NOW),
                "Lần sai thứ MAX_ATTEMPTS phải báo hết lượt.");
        assertEquals(DemoOtp.VerifyResult.TOO_MANY_ATTEMPTS,
                challenge.verify("a@b.com", DemoOtp.PURPOSE_LOGIN, "111111", NOW),
                "Sau khi hết lượt thì mã đúng cũng phải bị chặn.");
        assertFalse(challenge.used());
    }

    @Test
    void expiredChallengeIsRejected() {
        DemoOtp.Challenge challenge =
                DemoOtp.Challenge.issue("a@b.com", DemoOtp.PURPOSE_RESET_PASSWORD);
        long afterExpiry = challenge.issuedAt() + DemoOtp.TTL_MILLIS + 1;
        assertEquals(DemoOtp.VerifyResult.EXPIRED,
                challenge.verify("a@b.com", DemoOtp.PURPOSE_RESET_PASSWORD, "111111",
                        afterExpiry),
                "Mã đúng nhưng đã quá 5 phút phải bị từ chối.");
        // Trước khi hết hạn vẫn chấp nhận được.
        assertEquals(DemoOtp.VerifyResult.OK,
                challenge.verify("a@b.com", DemoOtp.PURPOSE_RESET_PASSWORD, "111111",
                        challenge.issuedAt() + 1000));
    }

    @Test
    void challengeIsBoundToEmailAndPurpose() {
        DemoOtp.Challenge challenge =
                DemoOtp.Challenge.issue("a@b.com", DemoOtp.PURPOSE_REGISTER);

        assertEquals(DemoOtp.VerifyResult.BOUND_TO_OTHER,
                challenge.verify("other@b.com", DemoOtp.PURPOSE_REGISTER, "111111", NOW),
                "Mã OTP không được dùng cho email khác.");
        assertEquals(DemoOtp.VerifyResult.BOUND_TO_OTHER,
                challenge.verify("a@b.com", DemoOtp.PURPOSE_RESET_PASSWORD, "111111", NOW),
                "Mã OTP đăng ký không được dùng cho khôi phục mật khẩu.");
        assertFalse(challenge.used(), "Sai email/mục đích không được tiêu thụ mã.");
    }

    @Test
    void ttlAndAttemptLimitsAreBounded() {
        assertEquals(5 * 60 * 1000L, DemoOtp.TTL_MILLIS,
                "Thách thức OTP demo phải hết hạn sau 5 phút.");
        assertEquals(5, DemoOtp.MAX_ATTEMPTS);
        assertTrue(DemoOtp.MAX_ATTEMPTS <= 10, "Chặn dò mã: tối đa 10 lần thử.");
    }

    @Test
    void invalidPurposeOrEmailIsRejectedAtIssueTime() {
        assertThrows(IllegalArgumentException.class,
                () -> DemoOtp.Challenge.issue("", DemoOtp.PURPOSE_LOGIN));
        assertThrows(IllegalArgumentException.class,
                () -> DemoOtp.Challenge.issue(null, DemoOtp.PURPOSE_LOGIN));
        assertThrows(IllegalArgumentException.class,
                () -> DemoOtp.Challenge.issue("a@b.com", "ADMIN"));
    }

    @Test
    void purposeValuesAreTheExpectedThree() {
        assertEquals("REGISTER", DemoOtp.PURPOSE_REGISTER);
        assertEquals("LOGIN", DemoOtp.PURPOSE_LOGIN);
        assertEquals("RESET_PASSWORD", DemoOtp.PURPOSE_RESET_PASSWORD);
        assertNotEquals(DemoOtp.PURPOSE_REGISTER, DemoOtp.PURPOSE_LOGIN);
    }

    @Test
    void constantTimeCompareRejectsMismatchedInput() {
        assertTrue(DemoOtp.matches("111111", "111111"));
        assertFalse(DemoOtp.matches("111110", "111111"));
        assertFalse(DemoOtp.matches(null, "111111"));
        assertFalse(DemoOtp.matches("111111", null));
        assertFalse(DemoOtp.matches("11111", "111111"));
    }
}
