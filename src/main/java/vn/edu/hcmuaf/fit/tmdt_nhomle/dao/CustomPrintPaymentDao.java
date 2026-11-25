package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public final class CustomPrintPaymentDao {

    private static volatile boolean schemaEnsured;

    private CustomPrintPaymentDao() { }

    public static void ensureSchema(Connection conn) throws SQLException {
        if (schemaEnsured) {
            return;
        }
        synchronized (CustomPrintPaymentDao.class) {
            if (schemaEnsured) {
                return;
            }
            addColumnIfMissing(conn, "custom_print_requests", "quoted_at", "DATETIME NULL AFTER `submitted_at`");
            addColumnIfMissing(conn, "custom_print_requests", "accepted_at", "DATETIME NULL AFTER `quoted_at`");
            addColumnIfMissing(conn, "custom_print_requests", "declined_at", "DATETIME NULL AFTER `accepted_at`");
            addColumnIfMissing(conn, "custom_print_requests", "deposit_amount", "DECIMAL(12,2) NULL");
            addColumnIfMissing(conn, "custom_print_requests", "deposit_paid_at", "DATETIME NULL");
            addColumnIfMissing(conn, "custom_print_requests", "production_started_at", "DATETIME NULL");
            addColumnIfMissing(conn, "custom_print_requests", "completed_at", "DATETIME NULL");
            addColumnIfMissing(conn, "custom_print_requests", "production_note", "VARCHAR(500) NULL");

            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS custom_print_payments ("
                        + "custom_payment_id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                        + "custom_request_id BIGINT NOT NULL, "
                        + "user_id BIGINT NOT NULL, "
                        + "provider VARCHAR(40) NOT NULL DEFAULT 'ZALOPAY', "
                        + "app_trans_id VARCHAR(64) NOT NULL UNIQUE, "
                        + "zp_trans_token VARCHAR(255) NULL, "
                        + "zp_trans_id VARCHAR(100) NULL, "
                        + "order_url VARCHAR(1000) NULL, "
                        + "amount DECIMAL(12,2) NOT NULL, "
                        + "status VARCHAR(40) NOT NULL DEFAULT 'PENDING', "
                        + "return_code INT NULL, "
                        + "return_message VARCHAR(500) NULL, "
                        + "channel INT NULL, "
                        + "raw_callback TEXT NULL, "
                        + "paid_at DATETIME NULL, "
                        + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                        + "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
                        + "KEY idx_custom_print_payments_req (custom_request_id, status)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
            }
            schemaEnsured = true;
        }
    }

    private static void addColumnIfMissing(Connection conn, String table, String column, String definition)
            throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, table, column)) {
            if (!rs.next()) {
                try (Statement st = conn.createStatement()) {
                    st.execute("ALTER TABLE `" + table + "` ADD COLUMN `" + column + "` " + definition);
                }
            }
        }
    }

    public static void createPendingZaloPay(Connection conn, long customRequestId, long userId,
                                            String appTransId, String zpTransToken,
                                            String orderUrl, BigDecimal amount) throws SQLException {
        ensureSchema(conn);
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE custom_print_payments SET status = 'SUPERSEDED', updated_at = NOW()"
                        + " WHERE custom_request_id = ? AND provider = 'ZALOPAY' AND status = 'PENDING'")) {
            ps.setLong(1, customRequestId);
            ps.executeUpdate();
        }
        String sql = "INSERT INTO custom_print_payments"
                + " (custom_request_id, user_id, provider, app_trans_id, zp_trans_token, order_url, amount, status, created_at, updated_at)"
                + " VALUES (?, ?, 'ZALOPAY', ?, ?, ?, ?, 'PENDING', NOW(), NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, customRequestId);
            ps.setLong(2, userId);
            ps.setString(3, appTransId);
            ps.setString(4, zpTransToken);
            ps.setString(5, orderUrl);
            ps.setBigDecimal(6, amount);
            ps.executeUpdate();
        }
    }

    public static java.util.List<String> listRecentPendingAppTransIds(long userId) {
        java.util.List<String> ids = new java.util.ArrayList<>();
        try (Connection conn = Database.getConnection()) {
            ensureSchema(conn);
            String sql = "SELECT app_trans_id FROM custom_print_payments"
                    + " WHERE user_id = ? AND status = 'PENDING'"
                    + " AND created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)"
                    + " ORDER BY custom_payment_id DESC LIMIT 5";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ids.add(rs.getString("app_trans_id"));
                    }
                }
            }
            return ids;
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static Optional<String> findOrderUrlForUser(String appTransId, long userId) {
        if (appTransId == null || appTransId.isBlank()) return Optional.empty();
        try (Connection conn = Database.getConnection()) {
            ensureSchema(conn);
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT order_url FROM custom_print_payments WHERE app_trans_id = ? AND user_id = ?")) {
                ps.setString(1, appTransId.trim());
                ps.setLong(2, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.ofNullable(rs.getString("order_url")) : Optional.empty();
                }
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static Optional<CustomPrintPaymentAttempt> findByAppTransId(String appTransId) {
        if (appTransId == null || appTransId.isBlank()) return Optional.empty();
        try (Connection conn = Database.getConnection()) {
            ensureSchema(conn);
            return find(conn, appTransId, false);
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static Optional<CustomPrintPaymentAttempt> findForUpdate(Connection conn, String appTransId)
            throws SQLException {
        if (appTransId == null || appTransId.isBlank()) return Optional.empty();
        ensureSchema(conn);
        return find(conn, appTransId, true);
    }

    public static void markPaid(Connection conn, long customPaymentId, String zpTransId,
                                Integer channel, String rawCallback) throws SQLException {
        String sql = "UPDATE custom_print_payments SET status = 'PAID', zp_trans_id = ?, return_code = 1,"
                + " return_message = 'Thanh toán thành công', channel = ?, raw_callback = ?,"
                + " paid_at = NOW(), updated_at = NOW() WHERE custom_payment_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, zpTransId);
            if (channel == null) ps.setNull(2, java.sql.Types.INTEGER);
            else ps.setInt(2, channel);
            ps.setString(3, rawCallback);
            ps.setLong(4, customPaymentId);
            ps.executeUpdate();
        }
    }

    public static void markFailed(Connection conn, long customPaymentId, int returnCode,
                                  String message) throws SQLException {
        String sql = "UPDATE custom_print_payments SET status = 'FAILED', return_code = ?, return_message = ?,"
                + " updated_at = NOW() WHERE custom_payment_id = ? AND status = 'PENDING'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, returnCode);
            ps.setString(2, message);
            ps.setLong(3, customPaymentId);
            ps.executeUpdate();
        }
    }

    private static Optional<CustomPrintPaymentAttempt> find(Connection conn, String appTransId,
                                                            boolean forUpdate) throws SQLException {
        String sql = "SELECT cp.custom_payment_id, cp.custom_request_id, cp.user_id, cp.app_trans_id,"
                + " cp.zp_trans_id, cp.amount, cp.status, r.status request_status,"
                + " r.request_code, r.quoted_amount, r.shop_id, df.original_filename"
                + " FROM custom_print_payments cp"
                + " JOIN custom_print_requests r ON r.custom_request_id = cp.custom_request_id"
                + " LEFT JOIN design_files df ON df.design_file_id = r.design_file_id"
                + " WHERE cp.app_trans_id = ?"
                + (forUpdate ? " FOR UPDATE" : "");
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, appTransId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                long shopIdVal = rs.getLong("shop_id");
                Long shopId = rs.wasNull() ? null : shopIdVal;
                return Optional.of(new CustomPrintPaymentAttempt(
                        rs.getLong("custom_payment_id"),
                        rs.getLong("custom_request_id"),
                        rs.getLong("user_id"),
                        rs.getString("app_trans_id"),
                        rs.getString("zp_trans_id"),
                        rs.getBigDecimal("amount"),
                        rs.getString("status"),
                        rs.getString("request_status"),
                        rs.getString("request_code"),
                        rs.getBigDecimal("quoted_amount"),
                        shopId,
                        rs.getString("original_filename")));
            }
        }
    }

    public record CustomPrintPaymentAttempt(
            long customPaymentId,
            long customRequestId,
            long userId,
            String appTransId,
            String zpTransId,
            BigDecimal amount,
            String status,
            String requestStatus,
            String requestCode,
            BigDecimal quotedAmount,
            Long shopId,
            String originalFilename) { }
}
