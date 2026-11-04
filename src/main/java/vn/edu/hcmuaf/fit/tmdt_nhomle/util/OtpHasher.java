package vn.edu.hcmuaf.fit.tmdt_nhomle.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Hash OTP 6 chữ số bằng SHA-256 + salt.
 * KHÔNG dùng BcryptPasswordService — lớp đó áp đặt rule "mật khẩu ≥ 8 ký tự"
 * không phù hợp với OTP ngắn hạn (5–15 phút).
 *
 * An toàn cho use-case OTP vì:
 *  - Có rate-limit (5 lần / OTP) + TTL (5 phút) → brute-force qua HTTP bất khả thi.
 *  - Nếu lộ DB: attacker phải brute-force 1M giá trị trong cửa sổ TTL ngắn.
 */
public final class OtpHasher {
    private static final SecureRandom RANDOM = new SecureRandom();

    private OtpHasher() { }

    /** Trả về chuỗi "saltBase64.digestBase64". */
    public static String hash(String otp) {
        if (otp == null || otp.isBlank())
            throw new IllegalArgumentException("OTP không được để trống.");
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] digest = sha256(otp.trim(), salt);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(salt)
                + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    /** So khớp constant-time. */
    public static boolean verify(String otp, String stored) {
        if (otp == null || stored == null) return false;
        int dot = stored.indexOf('.');
        if (dot < 0) return false;
        try {
            byte[] salt     = Base64.getUrlDecoder().decode(stored.substring(0, dot));
            byte[] expected = Base64.getUrlDecoder().decode(stored.substring(dot + 1));
            byte[] actual   = sha256(otp.trim(), salt);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static byte[] sha256(String text, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            md.update(text.getBytes(StandardCharsets.UTF_8));
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 không khả dụng.", e);
        }
    }
}