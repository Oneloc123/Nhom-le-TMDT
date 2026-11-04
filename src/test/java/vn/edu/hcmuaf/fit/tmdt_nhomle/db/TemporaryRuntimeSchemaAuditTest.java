package vn.edu.hcmuaf.fit.tmdt_nhomle.db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

// This opt-in audit opens a JDBC connection; keep the normal unit-test run offline.
@EnabledIfSystemProperty(named = "pk.runtimeSchemaAudit", matches = "true")
class TemporaryRuntimeSchemaAuditTest {
    @Test
    void printRelevantSchemaMetadataWithoutChangingDatabase() throws Exception {
        List<String> tables = List.of("users", "design_files", "print_shops", "orders", "order_items",
                "reviews", "quote_requests", "custom_print_requests", "custom_print_request_history",
                "shop_print_price_profiles", "shop_print_price_options", "shop_print_quantity_discount_tiers");
        DbConfig config = DbConfig.get();
        String serverUrl = "jdbc:mysql://" + config.host() + ":" + config.port()
                + "/?useUnicode=true&characterEncoding=UTF-8&connectionTimeZone=UTC&serverTimezone=UTC"
                + "&allowMultiQueries=false&sslMode=PREFERRED&allowPublicKeyRetrieval=true";
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("JDBC driver unavailable", e);
        }
        try (Connection connection = DriverManager.getConnection(serverUrl, config.user(), config.password())) {
            var metadata = connection.getMetaData();
            List<String> available = new ArrayList<>();
            try (var statement = connection.prepareStatement(
                    "SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME IN (?,?)")) {
                statement.setString(1, config.database());
                statement.setString(2, "printkraft");
                try (var result = statement.executeQuery()) {
                    while (result.next()) available.add(result.getString(1));
                }
            }
            String catalog = available.contains(config.database()) ? config.database()
                    : available.contains("printkraft") ? "printkraft" : null;
            if (catalog == null) {
                System.out.println("Neither the configured schema nor the repository schema exists on this server.");
                return;
            }
            connection.setCatalog(catalog);
            System.out.println("Inspected project schema: " + catalog);
            System.out.println("Runtime schema metadata (names only):");
            for (String table : tables) {
                List<String> columns = new ArrayList<>();
                try (var result = metadata.getColumns(catalog, null, table, null)) {
                    while (result.next()) {
                        String column = result.getString("COLUMN_NAME");
                        String details = result.getString("TYPE_NAME") + (result.getString("COLUMN_SIZE") == null
                                ? "" : "(" + result.getString("COLUMN_SIZE") + ")")
                                + ("YES".equals(result.getString("IS_NULLABLE")) ? " NULL" : " NOT NULL");
                        columns.add(column + " " + details);
                    }
                }
                System.out.println(table + ": " + (columns.isEmpty() ? "<missing>" : String.join(",", columns)));
            }
            try (var statement = connection.prepareStatement(
                    "SELECT TABLE_NAME,COLUMN_NAME,REFERENCED_TABLE_NAME,REFERENCED_COLUMN_NAME "
                            + "FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=? "
                            + "AND TABLE_NAME IN ('design_files','print_shops','orders','order_items','custom_print_requests') "
                            + "AND REFERENCED_TABLE_NAME IS NOT NULL ORDER BY TABLE_NAME,ORDINAL_POSITION")) {
                statement.setString(1, catalog);
                try (var result = statement.executeQuery()) {
                    System.out.println("Relevant foreign keys:");
                    while (result.next()) System.out.println(result.getString(1) + "." + result.getString(2)
                            + " -> " + result.getString(3) + "." + result.getString(4));
                }
            }
            try (var statement = connection.prepareStatement(
                    "SELECT TABLE_NAME,INDEX_NAME,COLUMN_NAME,NON_UNIQUE FROM information_schema.STATISTICS "
                            + "WHERE TABLE_SCHEMA=? AND TABLE_NAME IN ('custom_print_requests','custom_print_request_history') "
                            + "ORDER BY TABLE_NAME,INDEX_NAME,SEQ_IN_INDEX")) {
                statement.setString(1, catalog);
                try (var result = statement.executeQuery()) {
                    System.out.println("Existing custom-print indexes:");
                    while (result.next()) System.out.println(result.getString(1) + "." + result.getString(2)
                            + "(" + result.getString(3) + ") unique=" + (result.getInt(4) == 0));
                }
            }
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery("SELECT status,COUNT(*) FROM custom_print_requests GROUP BY status")) {
                System.out.println("Existing custom-print request counts by status:");
                while (result.next()) System.out.println(String.valueOf(result.getObject(1)) + "=" + result.getLong(2));
            }
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery("SELECT COUNT(*) FROM custom_print_request_history")) {
                if (result.next()) System.out.println("Existing custom-print history rows: " + result.getLong(1));
            }
        }
    }
}
