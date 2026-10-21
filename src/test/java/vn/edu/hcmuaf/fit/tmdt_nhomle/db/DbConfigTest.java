package vn.edu.hcmuaf.fit.tmdt_nhomle.db;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DbConfigTest {

    @Test
    void jdbcUrlContainsDatabaseAndNoCredentials() {
        DbConfig cfg = DbConfig.get();
        String url = cfg.jdbcUrl();
        assertTrue(url.startsWith("jdbc:mysql://localhost:3306/printkraft?"));
        assertTrue(url.contains("/" + cfg.database()));
        assertTrue(url.contains("characterEncoding=UTF-8"));
        assertTrue(url.contains("useSSL=false"));
        assertTrue(url.contains("allowPublicKeyRetrieval=true"));
        if (!cfg.password().isEmpty()) {
            assertFalse(url.contains(cfg.password()), "URL không được chứa mật khẩu");
        }
        assertFalse(url.contains("password"), "URL không được chứa tham số mật khẩu");
    }

    @Test
    void defaultValuesAreLocalDev() {
        DbConfig cfg = DbConfig.get();
        assertEquals(3306, cfg.port());
        assertEquals("printkraft", cfg.database());
        assertTrue(cfg.port() > 0 && cfg.port() <= 65535);
    }

    @Test
    void describeNeverLeaksPassword() {
        DbConfig cfg = DbConfig.get();
        String described = cfg.describe();
        assertFalse(described.contains(cfg.password()) && !cfg.password().isEmpty(),
                "describe() không được lộ mật khẩu");
        assertTrue(described.contains(cfg.database()));
    }
}
