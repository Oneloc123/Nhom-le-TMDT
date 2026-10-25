package vn.edu.hcmuaf.fit.tmdt_nhomle.db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class OrderSchemaIntegrationTest {

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void ordersKeepAnImmutableDeliverySnapshot() throws Exception {
        Connection conn;
        try {
            conn = Database.getConnection();
        } catch (Exception unavailable) {
            assumeTrue(false, "MySQL không khả dụng — bỏ qua kiểm tra schema đơn hàng.");
            return;
        }
        try (conn) {
            DatabaseMetaData metadata = conn.getMetaData();
            Set<String> columns = new HashSet<>();
            try (ResultSet rs = metadata.getColumns(conn.getCatalog(), null, "orders", null)) {
                while (rs.next()) {
                    columns.add(rs.getString("COLUMN_NAME").toLowerCase());
                }
            }
            assertTrue(columns.contains("recipient_name_snapshot"));
            assertTrue(columns.contains("recipient_phone_snapshot"));
            assertTrue(columns.contains("shipping_address_snapshot"));
        }
    }
}
