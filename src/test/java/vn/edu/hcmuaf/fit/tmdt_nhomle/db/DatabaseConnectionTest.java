package vn.edu.hcmuaf.fit.tmdt_nhomle.db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Test kết nối THẬT tới MySQL cục bộ — chỉ chạy khi server đang hoạt động.
 * Nếu không kết nối được (môi trường không có MySQL) test được bỏ qua
 * (assumeTrue) — KHÔNG bao giờ tự động chạy SQL schema hay destructive.
 */
class DatabaseConnectionTest {

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void connectionWorksWhenMysqlIsAvailable() {
        assumeTrue(mysqlIsAvailable(), "MySQL không khả dụng — bỏ qua test kết nối thật.");
        try (Connection conn = Database.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT 1")) {
            assertNotNull(conn);
            assertEquals(true, rs.next());
            assertEquals(1, rs.getInt(1));
        } catch (Exception e) {
            throw new AssertionError("MySQL đang chạy nhưng kết nối thất bại.", e);
        }
    }

    private boolean mysqlIsAvailable() {
        try (Connection ignored = Database.getConnection()) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
