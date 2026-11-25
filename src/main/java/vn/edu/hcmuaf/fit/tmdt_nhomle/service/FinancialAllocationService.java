package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Financial recognition for verified receipts. Every amount is snapshotted per order item.
 * This service must be invoked inside the caller's transaction.
 */
public final class FinancialAllocationService {
    private FinancialAllocationService() { }

    public static void allocatePaidOrder(Connection conn, long orderId, long paymentId)
            throws SQLException {
        String allocationSql = """
                INSERT INTO order_financial_allocations
                    (order_id, order_item_id, shop_id, payment_id, currency,
                     gross_product_amount, commission_rate_snapshot, commission_amount,
                     seller_net_amount, allocation_status, created_at)
                SELECT oi.order_id, oi.order_item_id, oi.shop_id, p.payment_id,
                       COALESCE(o.currency, 'VND'), oi.line_total, s.commission_rate,
                       ROUND(oi.line_total * s.commission_rate / 100, 2),
                       oi.line_total - ROUND(oi.line_total * s.commission_rate / 100, 2),
                       'AVAILABLE', NOW()
                FROM order_items oi
                JOIN orders o ON o.order_id = oi.order_id
                JOIN payments p ON p.payment_id = ?
                JOIN platform_finance_settings s ON s.setting_id = 1
                WHERE oi.order_id = ?
                  AND p.order_id = oi.order_id
                  AND p.status = 'PAID'
                  AND o.payment_status = 'Đã thanh toán'
                  AND oi.shop_id IS NOT NULL
                  AND oi.line_total > 0
                ON DUPLICATE KEY UPDATE order_item_id = VALUES(order_item_id)
                """;
        try (PreparedStatement ps = conn.prepareStatement(allocationSql)) {
            ps.setLong(1, paymentId);
            ps.setLong(2, orderId);
            ps.executeUpdate();
        }

        insertAllocationLedger(conn, orderId, paymentId, "SELLER_PAYABLE",
                "seller_net_amount", "Ghi nhận khoản phải trả xưởng");
        insertAllocationLedger(conn, orderId, paymentId, "PLATFORM_COMMISSION",
                "commission_amount", "Ghi nhận hoa hồng nền tảng");
    }

    private static void insertAllocationLedger(Connection conn, long orderId, long paymentId,
                                               String entryType, String amountColumn, String description)
            throws SQLException {
        if (!"seller_net_amount".equals(amountColumn) && !"commission_amount".equals(amountColumn)) {
            throw new IllegalArgumentException("Cột tài chính không hợp lệ.");
        }
        String sql = """
                INSERT INTO financial_ledger
                    (allocation_id, order_id, shop_id, entry_type, credit_amount,
                     currency, description, created_at)
                SELECT a.allocation_id, a.order_id, a.shop_id, ?, a.%s,
                       a.currency, ?, NOW()
                FROM order_financial_allocations a
                WHERE a.order_id = ? AND a.payment_id = ?
                  AND NOT EXISTS (
                    SELECT 1 FROM financial_ledger l
                    WHERE l.allocation_id = a.allocation_id AND l.entry_type = ?
                  )
                """.formatted(amountColumn);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entryType);
            ps.setString(2, description);
            ps.setLong(3, orderId);
            ps.setLong(4, paymentId);
            ps.setString(5, entryType);
            ps.executeUpdate();
        }
    }

    /**
     * Records COD money only after an Admin has verified the carrier's actual remittance.
     * Caller must wrap this method in a database transaction.
     */
    public static void recordPlatformCodReceipt(Connection conn, long orderId, String carrierName,
                                                String carrierReference, long adminUserId, String note)
            throws SQLException {
        String carrier = trimRequired(carrierName, 120, "Cần tên đơn vị vận chuyển hợp lệ.");
        String reference = trimRequired(carrierReference, 120, "Cần mã tham chiếu COD hợp lệ.");

        long paymentId;
        BigDecimal paymentAmount;
        String currency;
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT o.total_amount, o.currency, o.payment_status, p.payment_id,
                       p.amount, p.status, p.payment_method
                FROM orders o
                JOIN payments p ON p.order_id = o.order_id
                WHERE o.order_id = ? AND p.provider = 'COD'
                ORDER BY p.created_at DESC, p.payment_id DESC
                LIMIT 1 FOR UPDATE
                """)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy khoản COD của đơn hàng.");
                if (!"COD (thanh toán khi nhận hàng)".equals(rs.getString("payment_status"))
                        || !"PENDING".equalsIgnoreCase(rs.getString("status"))
                        || !"COD".equalsIgnoreCase(rs.getString("payment_method"))) {
                    throw new IllegalArgumentException("Đơn COD không ở trạng thái chờ đối soát.");
                }
                paymentId = rs.getLong("payment_id");
                paymentAmount = rs.getBigDecimal("amount");
                currency = rs.getString("currency");
                BigDecimal orderTotal = rs.getBigDecimal("total_amount");
                if (paymentAmount == null || orderTotal == null || paymentAmount.compareTo(orderTotal) != 0) {
                    throw new IllegalArgumentException("Số tiền COD không khớp tổng tiền đơn hàng.");
                }
            }
        }

        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO financial_cod_receipts
                    (order_id, payment_id, collected_amount, currency, carrier_name,
                     carrier_reference, received_by_user_id, received_at, note, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), ?, NOW())
                """)) {
            ps.setLong(1, orderId);
            ps.setLong(2, paymentId);
            ps.setBigDecimal(3, paymentAmount);
            ps.setString(4, currency == null ? "VND" : currency);
            ps.setString(5, carrier);
            ps.setString(6, reference);
            ps.setLong(7, adminUserId);
            ps.setString(8, note == null || note.isBlank() ? null : note.trim());
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE payments SET status='PAID', paid_at=NOW(), updated_at=NOW()
                WHERE payment_id=? AND status='PENDING'
                """)) {
            ps.setLong(1, paymentId);
            if (ps.executeUpdate() != 1) {
                throw new IllegalArgumentException("Khoản COD vừa được xử lý ở nơi khác.");
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE orders SET payment_status='Đã thanh toán'
                WHERE order_id=? AND payment_status='COD (thanh toán khi nhận hàng)'
                """)) {
            ps.setLong(1, orderId);
            if (ps.executeUpdate() != 1) {
                throw new IllegalArgumentException("Không thể cập nhật trạng thái thu COD.");
            }
        }
        allocatePaidOrder(conn, orderId, paymentId);
    }

    /**
     * Closes a complete, past calendar month. It is intentionally not allowed to close
     * the current month because late receipts could otherwise be omitted from a frozen batch.
     */
    public static int createMonthlySettlements(Connection conn, LocalDate periodStart,
                                               LocalDate periodEnd, long adminUserId)
            throws SQLException {
        if (periodStart == null || periodEnd == null
                || periodStart.getDayOfMonth() != 1
                || !periodStart.withDayOfMonth(1).plusMonths(1).minusDays(1).equals(periodEnd)) {
            throw new IllegalArgumentException("Kỳ quyết toán phải là một tháng dương lịch đầy đủ.");
        }
        if (!periodEnd.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Chỉ được quyết toán tháng đã kết thúc.");
        }

        String totalsSql = """
                SELECT shop_id, currency, SUM(gross_product_amount) gross_amount,
                       SUM(commission_amount) commission_amount, SUM(seller_net_amount) net_amount
                FROM order_financial_allocations
                WHERE allocation_status='AVAILABLE'
                  AND created_at >= ? AND created_at < DATE_ADD(?, INTERVAL 1 DAY)
                GROUP BY shop_id, currency
                ORDER BY shop_id, currency
                """;
        int settledAllocations = 0;
        try (PreparedStatement ps = conn.prepareStatement(totalsSql)) {
            ps.setDate(1, java.sql.Date.valueOf(periodStart));
            ps.setDate(2, java.sql.Date.valueOf(periodEnd));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long shopId = rs.getLong("shop_id");
                    String currency = rs.getString("currency");
                    BigDecimal gross = rs.getBigDecimal("gross_amount");
                    BigDecimal commission = rs.getBigDecimal("commission_amount");
                    BigDecimal net = rs.getBigDecimal("net_amount");
                    long settlementId;

                    try (PreparedStatement insert = conn.prepareStatement("""
                            INSERT INTO seller_monthly_settlements
                                (shop_id, period_start, period_end, currency, gross_product_amount,
                                 commission_amount, seller_net_amount, status, created_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, 'READY', NOW())
                            ON DUPLICATE KEY UPDATE settlement_id=LAST_INSERT_ID(settlement_id)
                            """)) {
                        insert.setLong(1, shopId);
                        insert.setDate(2, java.sql.Date.valueOf(periodStart));
                        insert.setDate(3, java.sql.Date.valueOf(periodEnd));
                        insert.setString(4, currency);
                        insert.setBigDecimal(5, gross);
                        insert.setBigDecimal(6, commission);
                        insert.setBigDecimal(7, net);
                        insert.executeUpdate();
                        try (ResultSet keys = insert.getGeneratedKeys()) {
                            if (!keys.next()) throw new SQLException("Không lấy được mã kỳ quyết toán.");
                            settlementId = keys.getLong(1);
                        }
                    }

                    try (PreparedStatement update = conn.prepareStatement("""
                            UPDATE order_financial_allocations
                            SET allocation_status='SETTLED', settlement_id=?, settled_at=NOW()
                            WHERE shop_id=? AND currency=? AND allocation_status='AVAILABLE'
                              AND created_at>=? AND created_at<DATE_ADD(?, INTERVAL 1 DAY)
                            """)) {
                        update.setLong(1, settlementId);
                        update.setLong(2, shopId);
                        update.setString(3, currency);
                        update.setDate(4, java.sql.Date.valueOf(periodStart));
                        update.setDate(5, java.sql.Date.valueOf(periodEnd));
                        int changed = update.executeUpdate();
                        if (changed > 0) {
                            try (PreparedStatement ledger = conn.prepareStatement("""
                                    INSERT INTO financial_ledger
                                        (settlement_id, shop_id, entry_type, debit_amount, currency,
                                         reference_code, description, created_by_user_id, created_at)
                                    VALUES (?, ?, 'SELLER_SETTLEMENT', ?, ?, ?, ?, ?, NOW())
                                    """)) {
                                ledger.setLong(1, settlementId);
                                ledger.setLong(2, shopId);
                                ledger.setBigDecimal(3, net);
                                ledger.setString(4, currency);
                                ledger.setString(5, "SETTLEMENT-" + settlementId);
                                ledger.setString(6, "Khóa khoản phải trả xưởng cho kỳ " + periodStart + " đến " + periodEnd);
                                ledger.setLong(7, adminUserId);
                                ledger.executeUpdate();
                            }
                            settledAllocations += changed;
                        }
                    }
                }
            }
        }
        return settledAllocations;
    }

    /** Marks a settlement paid only after the actual transfer reference is supplied. */
    public static void markSettlementPaid(Connection conn, long settlementId, long adminUserId,
                                          String payoutReference, String note) throws SQLException {
        String reference = trimRequired(payoutReference, 120, "Cần mã tham chiếu chuyển khoản hợp lệ.");
        long shopId;
        String currency;
        BigDecimal netAmount;
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT shop_id, currency, seller_net_amount, status
                FROM seller_monthly_settlements WHERE settlement_id=? FOR UPDATE
                """)) {
            ps.setLong(1, settlementId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy kỳ quyết toán.");
                if (!"READY".equalsIgnoreCase(rs.getString("status"))) {
                    throw new IllegalArgumentException("Kỳ quyết toán không còn ở trạng thái chờ chi.");
                }
                shopId = rs.getLong("shop_id");
                currency = rs.getString("currency");
                netAmount = rs.getBigDecimal("seller_net_amount");
            }
        }

        try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE seller_monthly_settlements
                SET status='PAID', payout_reference=?, paid_by_user_id=?, paid_at=NOW(), note=?
                WHERE settlement_id=? AND status='READY'
                """)) {
            ps.setString(1, reference);
            ps.setLong(2, adminUserId);
            ps.setString(3, note == null || note.isBlank() ? null : note.trim());
            ps.setLong(4, settlementId);
            if (ps.executeUpdate() != 1) throw new IllegalArgumentException("Kỳ quyết toán vừa được xử lý ở nơi khác.");
        }

        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO financial_ledger
                    (settlement_id, shop_id, entry_type, debit_amount, currency,
                     reference_code, description, created_by_user_id, created_at)
                VALUES (?, ?, 'SELLER_PAYOUT', ?, ?, ?, ?, ?, NOW())
                """)) {
            ps.setLong(1, settlementId);
            ps.setLong(2, shopId);
            ps.setBigDecimal(3, netAmount);
            ps.setString(4, currency);
            ps.setString(5, reference);
            ps.setString(6, "Xác nhận đã chuyển tiền quyết toán cho xưởng");
            ps.setLong(7, adminUserId);
            ps.executeUpdate();
        }
    }

    private static String trimRequired(String value, int max, String message) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty() || normalized.length() > max) throw new IllegalArgumentException(message);
        return normalized;
    }
}
