package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import org.mindrot.jbcrypt.BCrypt;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.PasswordHasher;

/** Quản lý hash BCrypt mới và đọc tương thích hash PBKDF2 đã có trong DB. */
public final class BcryptPasswordService {
    private static final int COST = 12;

    private BcryptPasswordService() { }

    public static String hash(String raw) {
        validate(raw);
        return BCrypt.hashpw(raw, BCrypt.gensalt(COST));
    }

    public static boolean verify(String raw, String encoded) {
        if (raw == null || encoded == null || encoded.isBlank()) return false;
        try {
            if (encoded.startsWith("$2a$") || encoded.startsWith("$2b$") || encoded.startsWith("$2y$")) {
                return BCrypt.checkpw(raw, encoded);
            }
            // Tương thích tài khoản cũ được tạo bằng PasswordHasher (PBKDF2).
            return PasswordHasher.verify(raw, encoded);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public static boolean needsUpgrade(String encoded) {
        return encoded == null || !encoded.startsWith("$2");
    }

    public static void validate(String raw) {
        if (raw == null || raw.length() < 8 || raw.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 8 ký tự và không vượt quá 72 byte UTF-8.");
        }
    }
}
