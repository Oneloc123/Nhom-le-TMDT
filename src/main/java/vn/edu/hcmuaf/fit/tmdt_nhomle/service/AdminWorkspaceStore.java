package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import vn.edu.hcmuaf.fit.tmdt_nhomle.controller.admin.AdminJson;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

/** JDBC repository for the seeded admin workspace records and settings. */
public final class AdminWorkspaceStore {
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/printkraft"
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8";

    private AdminWorkspaceStore() { }

    private static Connection connection() throws SQLException {
        String url = env("PRINTKRAFT_DB_URL", DEFAULT_URL);
        String user = env("PRINTKRAFT_DB_USER", "root");
        String password = env("PRINTKRAFT_DB_PASSWORD", "");
        return DriverManager.getConnection(url, user, password);
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    public static List<Map<String, Object>> findAll(String type) {
        String sql = "SELECT payload FROM admin_workspace_records WHERE record_type = ? ORDER BY sort_order, record_id";
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, type);
            try (ResultSet rows = statement.executeQuery()) {
                List<Map<String, Object>> result = new ArrayList<>();
                while (rows.next()) result.add(AdminJson.parseObject(rows.getString(1)));
                return result;
            }
        } catch (SQLException e) {
            throw databaseError(e);
        }
    }

    public static Optional<Map<String, Object>> find(String type, String id) {
        String sql = "SELECT payload FROM admin_workspace_records WHERE record_type = ? AND record_id = ?";
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, type);
            statement.setString(2, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(AdminJson.parseObject(rows.getString(1))) : Optional.empty();
            }
        } catch (SQLException e) {
            throw databaseError(e);
        }
    }

    public static void save(String type, String id, Map<String, Object> payload) {
        String sql = "INSERT INTO admin_workspace_records (record_type, record_id, sort_order, payload) "
                + "VALUES (?, ?, 99999, CAST(? AS JSON)) "
                + "ON DUPLICATE KEY UPDATE payload = VALUES(payload), updated_at = CURRENT_TIMESTAMP";
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, type);
            statement.setString(2, id);
            statement.setString(3, AdminJson.stringify(payload));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw databaseError(e);
        }
    }

    public static Optional<Map<String, Object>> update(String type, String id,
                                                        UnaryOperator<Map<String, Object>> updater) {
        String select = "SELECT payload FROM admin_workspace_records WHERE record_type = ? AND record_id = ? FOR UPDATE";
        String update = "UPDATE admin_workspace_records SET payload = CAST(? AS JSON), updated_at = CURRENT_TIMESTAMP "
                + "WHERE record_type = ? AND record_id = ?";
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(select)) {
                statement.setString(1, type);
                statement.setString(2, id);
                Map<String, Object> payload;
                try (ResultSet rows = statement.executeQuery()) {
                    if (!rows.next()) {
                        connection.rollback();
                        return Optional.empty();
                    }
                    payload = new LinkedHashMap<>(AdminJson.parseObject(rows.getString(1)));
                }
                Map<String, Object> changed = updater.apply(payload);
                try (PreparedStatement save = connection.prepareStatement(update)) {
                    save.setString(1, AdminJson.stringify(changed));
                    save.setString(2, type);
                    save.setString(3, id);
                    save.executeUpdate();
                }
                connection.commit();
                return Optional.of(changed);
            } catch (RuntimeException | SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw databaseError(e);
        }
    }

    public static Map<String, Object> getSettings(String key) {
        String sql = "SELECT setting_value FROM admin_workspace_settings WHERE setting_key = ?";
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? AdminJson.parseObject(rows.getString(1)) : new LinkedHashMap<>();
            }
        } catch (SQLException e) {
            throw databaseError(e);
        }
    }

    public static void saveSettings(String key, Map<String, Object> value) {
        String sql = "INSERT INTO admin_workspace_settings (setting_key, setting_value) VALUES (?, CAST(? AS JSON)) "
                + "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), updated_at = CURRENT_TIMESTAMP";
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key);
            statement.setString(2, AdminJson.stringify(value));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw databaseError(e);
        }
    }

    public static Map<String, Object> bootstrap() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String type : List.of("users", "sellers", "products", "orders", "transactions",
                "orderItems", "depositTransactions", "commissionRules", "reviews")) {
            result.put(type, findAll(type));
        }
        result.put("settings", getSettings("marketplace"));
        result.put("dashboardSeries", getSettings("dashboardSeries"));
        return result;
    }

    private static AdminDatabaseException databaseError(SQLException error) {
        return new AdminDatabaseException("Không kết nối được MySQL admin workspace. "
                + "Kiểm tra PRINTKRAFT_DB_URL/PRINTKRAFT_DB_USER/PRINTKRAFT_DB_PASSWORD và đã nạp SQL seed chưa.", error);
    }
}
