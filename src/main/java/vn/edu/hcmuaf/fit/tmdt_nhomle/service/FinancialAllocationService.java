package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Financial recognition and settlement. Methods participate in the caller's DB transaction.
 */
public final class FinancialAllocationService {
    private FinancialAllocationService() { }

    public static void allocatePaidOrder(Connection conn, long orderId, long paymentId)
            throws SQLException {
        String sql = """
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
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
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
            if (ps.executeUpdate() != 1) throw new IllegalArgumentException("Khoản COD vừa được xử lý ở nơi khác.");
        }
        try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE orders SET payment_status='Đã thanh toán'
                WHERE order_id=? AND payment_status='COD (thanh toán khi nhận hàng)'
                """)) {
            ps.setLong(1, orderId);
            if (ps.executeUpdate() != 1) throw new IllegalArgumentException("Không thể cập nhật trạng thái thu COD.");
        }
        allocatePaidOrder(conn, orderId, paymentId);
    }

    /** Record a successful refund for one order item, using its commission snapshot. */
    public static void recordItemRefund(Connection conn, long orderItemId, BigDecimal refundAmount,
                                        String sourceType, long sourceId, long adminUserId)
            throws SQLException {
        if (refundAmount == null || refundAmount.signum() <= 0) {
            throw new IllegalArgumentException("Số tiền hoàn phải lớn hơn 0.");
        }
        Allocation allocation = findAllocation(conn, orderItemId);
        if (allocation == null) return; // Legacy order: it has no financial allocation snapshot.
        createRefundAdjustment(conn, allocation, refundAmount, sourceType, sourceId, adminUserId);
    }

    /**
     * Records a full payment refund. Shipping is not part of the commission basis, so only
     * the product-subtotal portion is distributed across the order's item allocations.
     */
    public static void recordOrderRefund(Connection conn, long orderId, BigDecimal paymentRefund,
                                         String sourceType, long sourceId, long adminUserId)
            throws SQLException {
        if (paymentRefund == null || paymentRefund.signum() <= 0) {
            throw new IllegalArgumentException("Số tiền hoàn phải lớn hơn 0.");
        }
        List<Allocation> allocations = findAllocationsForOrder(conn, orderId);
        if (allocations.isEmpty()) return;
        BigDecimal productTotal = allocations.stream().map(a -> a.gross).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal productRefund = paymentRefund.min(productTotal).setScale(2, RoundingMode.HALF_UP);
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < allocations.size(); i++) {
            Allocation allocation = allocations.get(i);
            BigDecimal part;
            if (i == allocations.size() - 1) {
                part = productRefund.subtract(allocated);
            } else {
                part = productRefund.multiply(allocation.gross)
                        .divide(productTotal, 2, RoundingMode.HALF_UP);
                allocated = allocated.add(part);
            }
            if (part.signum() > 0) {
                createRefundAdjustment(conn, allocation, part, sourceType, sourceId, adminUserId);
            }
        }
    }

    private static Allocation findAllocation(Connection conn, long orderItemId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT allocation_id, order_id, order_item_id, shop_id, currency,
                       gross_product_amount, commission_rate_snapshot, seller_net_amount,
                       allocation_status, settlement_id
                FROM order_financial_allocations WHERE order_item_id=? FOR UPDATE
                """)) {
            ps.setLong(1, orderItemId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? mapAllocation(rs) : null; }
        }
    }

    private static List<Allocation> findAllocationsForOrder(Connection conn, long orderId) throws SQLException {
        List<Allocation> rows = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT allocation_id, order_id, order_item_id, shop_id, currency,
                       gross_product_amount, commission_rate_snapshot, seller_net_amount,
                       allocation_status, settlement_id
                FROM order_financial_allocations WHERE order_id=? ORDER BY order_item_id FOR UPDATE
                """)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) rows.add(mapAllocation(rs)); }
        }
        return rows;
    }

    private static Allocation mapAllocation(ResultSet rs) throws SQLException {
        long settlementId = rs.getLong("settlement_id");
        Long settlement = rs.wasNull() ? null : settlementId;
        return new Allocation(rs.getLong("allocation_id"), rs.getLong("order_id"),
                rs.getLong("order_item_id"), rs.getLong("shop_id"), rs.getString("currency"),
                rs.getBigDecimal("gross_product_amount"), rs.getBigDecimal("commission_rate_snapshot"),
                rs.getBigDecimal("seller_net_amount"), rs.getString("allocation_status"), settlement);
    }

    private static void createRefundAdjustment(Connection conn, Allocation allocation, BigDecimal refundAmount,
                                               String sourceType, long sourceId, long adminUserId)
            throws SQLException {
        if (!List.of("RETURN_REFUND", "MANUAL_PAYMENT_REFUND").contains(sourceType)) {
            throw new IllegalArgumentException("Nguồn điều chỉnh hoàn tiền không hợp lệ.");
        }
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT adjustment_id FROM financial_refund_adjustments
                WHERE source_type=? AND source_id=? AND allocation_id=?
                """)) {
            ps.setString(1, sourceType);
            ps.setLong(2, sourceId);
            ps.setLong(3, allocation.id);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return; }
        }

        BigDecimal priorRefunds;
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT COALESCE(SUM(refund_amount),0)
                FROM financial_refund_adjustments
                WHERE allocation_id=?
                """)) {
            ps.setLong(1, allocation.id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                priorRefunds = rs.getBigDecimal(1);
            }
        }
        BigDecimal remaining = allocation.gross.subtract(priorRefunds == null ? BigDecimal.ZERO : priorRefunds);
        if (refundAmount.compareTo(remaining) > 0) {
            throw new IllegalArgumentException("Tổng tiền hoàn vượt giá trị sản phẩm chưa được hoàn của đơn.");
        }
        BigDecimal commissionReversal = refundAmount.multiply(allocation.commissionRate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal sellerRecovery = refundAmount.subtract(commissionReversal);
        long adjustmentId;
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO financial_refund_adjustments
                    (allocation_id, source_type, source_id, refund_amount, commission_reversal,
                     seller_recovery, adjustment_status, created_by_user_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, 'OPEN', ?, NOW())
                """, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, allocation.id);
            ps.setString(2, sourceType);
            ps.setLong(3, sourceId);
            ps.setBigDecimal(4, refundAmount);
            ps.setBigDecimal(5, commissionReversal);
            ps.setBigDecimal(6, sellerRecovery);
            ps.setLong(7, adminUserId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Không lấy được mã điều chỉnh hoàn tiền.");
                adjustmentId = keys.getLong(1);
            }
        }

        insertRefundLedger(conn, allocation, adjustmentId, "REFUND_PLATFORM_COMMISSION",
                commissionReversal, "Điều chỉnh giảm hoa hồng do hoàn tiền");
        insertRefundLedger(conn, allocation, adjustmentId, "REFUND_SELLER_RECOVERY",
                sellerRecovery, "Khoản thu hồi từ số dư phải trả xưởng do hoàn tiền");

        // If this seller's batch is created but not paid yet, adjust that batch immediately.
        if ("SETTLED".equalsIgnoreCase(allocation.status) && allocation.settlementId != null) {
            String settlementStatus = null;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT status FROM seller_monthly_settlements WHERE settlement_id=? FOR UPDATE")) {
                ps.setLong(1, allocation.settlementId);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) settlementStatus = rs.getString(1); }
            }
            if ("READY".equalsIgnoreCase(settlementStatus)) {
                try (PreparedStatement ps = conn.prepareStatement("""
                        UPDATE seller_monthly_settlements
                        SET gross_product_amount=gross_product_amount-?,
                            commission_amount=commission_amount-?,
                            seller_net_amount=seller_net_amount-?,
                            refund_amount=refund_amount+?,
                            commission_reversal_amount=commission_reversal_amount+?,
                            seller_recovery_amount=seller_recovery_amount+?
                        WHERE settlement_id=? AND status='READY'
                        """)) {
                    ps.setBigDecimal(1, refundAmount);
                    ps.setBigDecimal(2, commissionReversal);
                    ps.setBigDecimal(3, sellerRecovery);
                    ps.setBigDecimal(4, refundAmount);
                    ps.setBigDecimal(5, commissionReversal);
                    ps.setBigDecimal(6, sellerRecovery);
                    ps.setLong(7, allocation.settlementId);
                    if (ps.executeUpdate() == 1) {
                        try (PreparedStatement update = conn.prepareStatement("""
                                UPDATE financial_refund_adjustments
                                SET adjustment_status='SETTLED', settlement_id=?
                                WHERE adjustment_id=? AND adjustment_status='OPEN'
                                """)) {
                            update.setLong(1, allocation.settlementId);
                            update.setLong(2, adjustmentId);
                            update.executeUpdate();
                        }
                    }
                }
            }
        }
    }

    private static void insertRefundLedger(Connection conn, Allocation allocation, long adjustmentId,
                                           String entryType, BigDecimal amount, String description)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO financial_ledger
                    (allocation_id, order_id, shop_id, entry_type, debit_amount, currency,
                     reference_code, description, created_by_user_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, NULL, NOW())
                """)) {
            ps.setLong(1, allocation.id);
            ps.setLong(2, allocation.orderId);
            ps.setLong(3, allocation.shopId);
            ps.setString(4, entryType);
            ps.setBigDecimal(5, amount);
            ps.setString(6, allocation.currency);
            ps.setString(7, "FIN-REFUND-" + adjustmentId);
            ps.setString(8, description);
            ps.executeUpdate();
        }
    }

    /** Close a completed calendar month. Open refund deductions carry into the next payable month. */
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
                SELECT scope.shop_id, scope.currency,
                    COALESCE((SELECT SUM(a.gross_product_amount) FROM order_financial_allocations a
                              WHERE a.shop_id=scope.shop_id AND a.currency=scope.currency
                                AND a.allocation_status='AVAILABLE'
                                AND a.created_at>=? AND a.created_at<DATE_ADD(?, INTERVAL 1 DAY)),0) gross_sales,
                    COALESCE((SELECT SUM(a.commission_amount) FROM order_financial_allocations a
                              WHERE a.shop_id=scope.shop_id AND a.currency=scope.currency
                                AND a.allocation_status='AVAILABLE'
                                AND a.created_at>=? AND a.created_at<DATE_ADD(?, INTERVAL 1 DAY)),0) commission_sales,
                    COALESCE((SELECT SUM(a.seller_net_amount) FROM order_financial_allocations a
                              WHERE a.shop_id=scope.shop_id AND a.currency=scope.currency
                                AND a.allocation_status='AVAILABLE'
                                AND a.created_at>=? AND a.created_at<DATE_ADD(?, INTERVAL 1 DAY)),0) seller_sales,
                    COALESCE((SELECT SUM(r.refund_amount) FROM financial_refund_adjustments r
                              JOIN order_financial_allocations a ON a.allocation_id=r.allocation_id
                              WHERE a.shop_id=scope.shop_id AND a.currency=scope.currency
                                AND r.adjustment_status='OPEN'),0) refund_amount,
                    COALESCE((SELECT SUM(r.commission_reversal) FROM financial_refund_adjustments r
                              JOIN order_financial_allocations a ON a.allocation_id=r.allocation_id
                              WHERE a.shop_id=scope.shop_id AND a.currency=scope.currency
                                AND r.adjustment_status='OPEN'),0) commission_reversal,
                    COALESCE((SELECT SUM(r.seller_recovery) FROM financial_refund_adjustments r
                              JOIN order_financial_allocations a ON a.allocation_id=r.allocation_id
                              WHERE a.shop_id=scope.shop_id AND a.currency=scope.currency
                                AND r.adjustment_status='OPEN'),0) seller_recovery
                FROM (
                    SELECT shop_id, currency FROM order_financial_allocations
                    WHERE allocation_status='AVAILABLE'
                      AND created_at>=? AND created_at<DATE_ADD(?, INTERVAL 1 DAY)
                    GROUP BY shop_id, currency
                    UNION
                    SELECT a.shop_id, a.currency FROM financial_refund_adjustments r
                    JOIN order_financial_allocations a ON a.allocation_id=r.allocation_id
                    WHERE r.adjustment_status='OPEN'
                    GROUP BY a.shop_id, a.currency
                ) scope
                ORDER BY scope.shop_id, scope.currency
                """;
        int settledAllocations = 0;
        try (PreparedStatement ps = conn.prepareStatement(totalsSql)) {
            int i = 1;
            for (int pair = 0; pair < 4; pair++) {
                ps.setDate(i++, java.sql.Date.valueOf(periodStart));
                ps.setDate(i++, java.sql.Date.valueOf(periodEnd));
            }
            ps.setDate(i++, java.sql.Date.valueOf(periodStart));
            ps.setDate(i++, java.sql.Date.valueOf(periodEnd));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long shopId = rs.getLong("shop_id");
                    String currency = rs.getString("currency");
                    BigDecimal gross = rs.getBigDecimal("gross_sales").subtract(rs.getBigDecimal("refund_amount"));
                    BigDecimal commission = rs.getBigDecimal("commission_sales").subtract(rs.getBigDecimal("commission_reversal"));
                    BigDecimal net = rs.getBigDecimal("seller_sales").subtract(rs.getBigDecimal("seller_recovery"));

                    // A refund debt can exceed this month's proceeds. Keep all open deductions
                    // for the next month rather than producing a negative transfer.
                    if (net.signum() <= 0) continue;

                    long settlementId;
                    try (PreparedStatement insert = conn.prepareStatement("""
                            INSERT INTO seller_monthly_settlements
                                (shop_id, period_start, period_end, currency, gross_product_amount,
                                 commission_amount, seller_net_amount, refund_amount,
                                 commission_reversal_amount, seller_recovery_amount, status, created_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'READY', NOW())
                            ON DUPLICATE KEY UPDATE settlement_id=LAST_INSERT_ID(settlement_id)
                            """)) {
                        insert.setLong(1, shopId);
                        insert.setDate(2, java.sql.Date.valueOf(periodStart));
                        insert.setDate(3, java.sql.Date.valueOf(periodEnd));
                        insert.setString(4, currency);
                        insert.setBigDecimal(5, gross);
                        insert.setBigDecimal(6, commission);
                        insert.setBigDecimal(7, net);
                        insert.setBigDecimal(8, rs.getBigDecimal("refund_amount"));
                        insert.setBigDecimal(9, rs.getBigDecimal("commission_reversal"));
                        insert.setBigDecimal(10, rs.getBigDecimal("seller_recovery"));
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
                        settledAllocations += update.executeUpdate();
                    }
                    try (PreparedStatement update = conn.prepareStatement("""
                            UPDATE financial_refund_adjustments r
                            JOIN order_financial_allocations a ON a.allocation_id=r.allocation_id
                            SET r.adjustment_status='SETTLED', r.settlement_id=?
                            WHERE a.shop_id=? AND a.currency=? AND r.adjustment_status='OPEN'
                            """)) {
                        update.setLong(1, settlementId);
                        update.setLong(2, shopId);
                        update.setString(3, currency);
                        update.executeUpdate();
                    }
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
                }
            }
        }
        return settledAllocations;
    }

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
                if (netAmount == null || netAmount.signum() <= 0) {
                    throw new IllegalArgumentException("Số tiền phải trả không dương; cần đối soát số dư trước khi chi.");
                }
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

    private record Allocation(long id, long orderId, long orderItemId, long shopId, String currency,
                              BigDecimal gross, BigDecimal commissionRate, BigDecimal sellerNet,
                              String status, Long settlementId) { }
}
