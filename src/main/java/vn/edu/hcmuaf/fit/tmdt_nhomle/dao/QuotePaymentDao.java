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

public final class QuotePaymentDao {

    private static volatile boolean schemaEnsured;

    private QuotePaymentDao() { }

    public static void ensureSchema(Connection conn) throws SQLException {
        if (schemaEnsured) {
            return;
        }
        synchronized (QuotePaymentDao.class) {
            if (schemaEnsured) {
                return;
            }
            addColumnIfMissing(conn, "quote_requests", "seller_response_note", "VARCHAR(500) NULL");
            addColumnIfMissing(conn, "quote_requests", "deposit_amount", "DECIMAL(12,2) NULL");
            addColumnIfMissing(conn, "quote_requests", "deposit_paid_at", "DATETIME NULL");
            addColumnIfMissing(conn, "quote_requests", "production_started_at", "DATETIME NULL");
            addColumnIfMissing(conn, "quote_requests", "completed_at", "DATETIME NULL");
            addColumnIfMissing(conn, "quote_requests", "production_note", "VARCHAR(500) NULL");
            addColumnIfMissing(conn, "quote_requests", "order_id", "BIGINT NULL");

            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS quote_payments ("
                        + "quote_payment_id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                        + "quote_request_id BIGINT NOT NULL, "
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
                        + "KEY idx_quote_payments_quote (quote_request_id, status)"
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

    public static void createPendingZaloPay(Connection conn, long quoteRequestId, long userId,
                                            String appTransId, String zpTransToken,
                                            String orderUrl, BigDecimal amount) throws SQLException {
        ensureSchema(conn);
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE quote_payments SET status = 'SUPERSEDED', updated_at = NOW()"
                        + " WHERE quote_request_id = ? AND provider = 'ZALOPAY' AND status = 'PENDING'")) {
            ps.setLong(1, quoteRequestId);
            ps.executeUpdate();
        }
        String sql = "INSERT INTO quote_payments"
                + " (quote_request_id, user_id, provider, app_trans_id, zp_trans_token, order_url, amount, status, created_at, updated_at)"
                + " VALUES (?, ?, 'ZALOPAY', ?, ?, ?, ?, 'PENDING', NOW(), NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, quoteRequestId);
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
            String sql = "SELECT app_trans_id FROM quote_payments"
                    + " WHERE user_id = ? AND status = 'PENDING'"
                    + " AND created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)"
                    + " ORDER BY quote_payment_id DESC LIMIT 5";
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
                    "SELECT order_url FROM quote_payments WHERE app_trans_id = ? AND user_id = ?")) {
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

    public static Optional<QuotePaymentAttempt> findByAppTransId(String appTransId) {
        if (appTransId == null || appTransId.isBlank()) return Optional.empty();
        try (Connection conn = Database.getConnection()) {
            ensureSchema(conn);
            return find(conn, appTransId, false);
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static Optional<QuotePaymentAttempt> findForUpdate(Connection conn, String appTransId)
            throws SQLException {
        if (appTransId == null || appTransId.isBlank()) return Optional.empty();
        ensureSchema(conn);
        return find(conn, appTransId, true);
    }

    public static void markPaid(Connection conn, long quotePaymentId, String zpTransId,
                                Integer channel, String rawCallback) throws SQLException {
        String sql = "UPDATE quote_payments SET status = 'PAID', zp_trans_id = ?, return_code = 1,"
                + " return_message = 'Thanh toán thành công', channel = ?, raw_callback = ?,"
                + " paid_at = NOW(), updated_at = NOW() WHERE quote_payment_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, zpTransId);
            if (channel == null) ps.setNull(2, java.sql.Types.INTEGER);
            else ps.setInt(2, channel);
            ps.setString(3, rawCallback);
            ps.setLong(4, quotePaymentId);
            ps.executeUpdate();
        }
    }

    public static void markFailed(Connection conn, long quotePaymentId, int returnCode,
                                  String message) throws SQLException {
        String sql = "UPDATE quote_payments SET status = 'FAILED', return_code = ?, return_message = ?,"
                + " updated_at = NOW() WHERE quote_payment_id = ? AND status = 'PENDING'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, returnCode);
            ps.setString(2, message);
            ps.setLong(3, quotePaymentId);
            ps.executeUpdate();
        }
    }

    private static Optional<QuotePaymentAttempt> find(Connection conn, String appTransId,
                                                      boolean forUpdate) throws SQLException {
        String sql = "SELECT qp.quote_payment_id, qp.quote_request_id, qp.user_id, qp.app_trans_id,"
                + " qp.zp_trans_id, qp.amount, qp.status, q.status quote_status,"
                + " q.quoted_amount, q.preferred_shop_id, p.product_name"
                + " FROM quote_payments qp"
                + " JOIN quote_requests q ON q.quote_request_id = qp.quote_request_id"
                + " LEFT JOIN products p ON p.product_id = q.product_id"
                + " WHERE qp.app_trans_id = ?"
                + (forUpdate ? " FOR UPDATE" : "");
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, appTransId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                long shopId = rs.getLong("preferred_shop_id");
                Long preferredShopId = rs.wasNull() ? null : shopId;
                return Optional.of(new QuotePaymentAttempt(
                        rs.getLong("quote_payment_id"),
                        rs.getLong("quote_request_id"),
                        rs.getLong("user_id"),
                        rs.getString("app_trans_id"),
                        rs.getString("zp_trans_id"),
                        rs.getBigDecimal("amount"),
                        rs.getString("status"),
                        rs.getString("quote_status"),
                        rs.getBigDecimal("quoted_amount"),
                        preferredShopId,
                        rs.getString("product_name")));
            }
        }
    }

    public record QuotePaymentAttempt(
            long quotePaymentId,
            long quoteRequestId,
            long userId,
            String appTransId,
            String zpTransId,
            BigDecimal amount,
            String status,
            String quoteStatus,
            BigDecimal quotedAmount,
            Long preferredShopId,
            String productName) { }
}
