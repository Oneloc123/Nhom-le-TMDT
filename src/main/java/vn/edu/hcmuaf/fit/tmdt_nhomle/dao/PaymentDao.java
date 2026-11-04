package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Order;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.PaymentAttempt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public final class PaymentDao {

    private static final String ATTEMPT_COLUMNS =
            "p.payment_id, p.order_id, o.user_id, o.order_code, p.amount, p.status,"
                    + " p.provider_transaction_ref, o.status order_status,"
                    + " o.payment_status order_payment_status";

    private PaymentDao() {
    }

    public static void createPendingVnPay(Connection conn, Order order, String transactionRef,
                                           String checkoutUrl) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE payments SET status = 'SUPERSEDED', updated_at = NOW()"
                        + " WHERE order_id = ? AND provider = 'VNPAY' AND status = 'PENDING'")) {
            ps.setLong(1, order.getOrderId());
            ps.executeUpdate();
        }

        String sql = "INSERT INTO payments (order_id, payment_method, provider,"
                + " provider_transaction_ref, checkout_url, amount, status, failure_reason,"
                + " paid_at, created_at, updated_at)"
                + " VALUES (?, 'VNPay', 'VNPAY', ?, ?, ?, 'PENDING', NULL, NULL, NOW(), NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, order.getOrderId());
            ps.setString(2, transactionRef);
            ps.setString(3, checkoutUrl);
            ps.setBigDecimal(4, order.getTotalAmount());
            ps.executeUpdate();
        }
    }

    public static Optional<PaymentAttempt> findByTransactionRef(String transactionRef) {
        if (transactionRef == null || transactionRef.isBlank()) return Optional.empty();
        try (Connection conn = Database.getConnection()) {
            return find(conn, transactionRef, false);
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static Optional<PaymentAttempt> findForUpdate(Connection conn, String transactionRef)
            throws SQLException {
        if (transactionRef == null || transactionRef.isBlank()) return Optional.empty();
        return find(conn, transactionRef, true);
    }

    public static void markPaid(Connection conn, long paymentId,
                                String providerTransactionNo, String responseCode,
                                String bankCode, String cardType) throws SQLException {
        updateResult(conn, paymentId, "PAID", providerTransactionNo, responseCode,
                bankCode, cardType, null, true);
    }

    public static void markFailed(Connection conn, long paymentId,
                                  String providerTransactionNo, String responseCode,
                                  String bankCode, String cardType, String reason) throws SQLException {
        updateResult(conn, paymentId, "FAILED", providerTransactionNo, responseCode,
                bankCode, cardType, reason, false);
    }

    public static void markReviewRequired(Connection conn, long paymentId,
                                          String providerTransactionNo, String responseCode,
                                          String bankCode, String cardType, String reason) throws SQLException {
        updateResult(conn, paymentId, "REVIEW_REQUIRED", providerTransactionNo, responseCode,
                bankCode, cardType, reason, false);
    }

    private static Optional<PaymentAttempt> find(Connection conn, String transactionRef,
                                                  boolean forUpdate) throws SQLException {
        String sql = "SELECT " + ATTEMPT_COLUMNS + " FROM payments p"
                + " JOIN orders o ON o.order_id = p.order_id"
                + " WHERE p.provider = 'VNPAY' AND p.provider_transaction_ref = ?"
                + (forUpdate ? " FOR UPDATE" : "");
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, transactionRef);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new PaymentAttempt(
                        rs.getLong("payment_id"), rs.getLong("order_id"), rs.getLong("user_id"),
                        rs.getString("order_code"), rs.getBigDecimal("amount"),
                        rs.getString("status"), rs.getString("provider_transaction_ref"),
                        rs.getString("order_status"), rs.getString("order_payment_status")));
            }
        }
    }

    private static void updateResult(Connection conn, long paymentId, String status,
                                     String providerTransactionNo, String responseCode,
                                     String bankCode, String cardType, String failureReason,
                                     boolean paid) throws SQLException {
        String sql = "UPDATE payments SET status = ?, provider_transaction_no = ?,"
                + " response_code = ?, bank_code = ?, card_type = ?, failure_reason = ?,"
                + (paid ? " paid_at = NOW()," : "")
                + " updated_at = NOW() WHERE payment_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, providerTransactionNo);
            ps.setString(3, responseCode);
            ps.setString(4, bankCode);
            ps.setString(5, cardType);
            ps.setString(6, failureReason);
            ps.setLong(7, paymentId);
            ps.executeUpdate();
        }
    }
}
