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
        insertPaymentReceiptLedger(conn, orderId, paymentId);
    }

    private static void insertPaymentReceiptLedger(Connection conn, long orderId, long paymentId)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO financial_ledger
                    (order_id, entry_type, debit_amount, currency, reference_code, description, created_at)
                SELECT o.order_id, 'CUSTOMER_RECEIPT', p.amount, COALESCE(o.currency, 'VND'),
                       CONCAT('PAYMENT-', p.payment_id), 'Ghi nhận tiền khách hàng đã thanh toán/đối soát COD',
                       NOW()
                FROM payments p
                JOIN orders o ON o.order_id=p.order_id
                WHERE p.payment_id=? AND p.order_id=? AND p.status='PAID'
                  AND NOT EXISTS (
                    SELECT 1 FROM financial_ledger l
                    WHERE l.entry_type='CUSTOMER_RECEIPT'
                      AND l.reference_code=CONCAT('PAYMENT-', p.payment_id)
                  )
                """)) {
            ps.setLong(1, paymentId);
            ps.setLong(2, orderId);
            ps.executeUpdate();
        }

        // Delivery charges are tracked as a clearing liability, not part of platform commission.
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO financial_ledger
                    (order_id, entry_type, credit_amount, currency, reference_code, description, created_at)
                SELECT o.order_id, 'SHIPPING_CLEARING', o.shipping_fee, COALESCE(o.currency, 'VND'),
                       CONCAT('SHIPPING-PAYMENT-', p.payment_id),
                       'Phí vận chuyển tách riêng để đối soát với đơn vị giao hàng', NOW()
                FROM payments p
                JOIN orders o ON o.order_id=p.order_id
                WHERE p.payment_id=? AND p.order_id=? AND p.status='PAID'
                  AND COALESCE(o.shipping_fee,0)>0
                  AND NOT EXISTS (
                    SELECT 1 FROM financial_ledger l
                    WHERE l.entry_type='SHIPPING_CLEARING'
                      AND l.reference_code=CONCAT('SHIPPING-PAYMENT-', p.payment_id)
                  )
                """)) {
            ps.setLong(1, paymentId);
            ps.setLong(2, orderId);
            ps.executeUpdate();
        }
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

        List<BigDecimal> remainingByItem = new ArrayList<>();
        BigDecimal remainingProductTotal = BigDecimal.ZERO;
        for (Allocation allocation : allocations) {
            BigDecimal remaining = remainingAmount(conn, allocation);
            remainingByItem.add(remaining);
            remainingProductTotal = remainingProductTotal.add(remaining);
        }
        if (remainingProductTotal.signum() <= 0) {
            recordShippingRefund(conn, orderId, paymentRefund, sourceType, sourceId, adminUserId);
            return;
        }

        BigDecimal productRefund = paymentRefund.min(remainingProductTotal)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal shippingRefund = paymentRefund.subtract(productRefund).max(BigDecimal.ZERO);
        if (shippingRefund.signum() > 0) {
            recordShippingRefund(conn, orderId, shippingRefund, sourceType, sourceId, adminUserId);
        }
        BigDecimal refundLeft = productRefund;
        BigDecimal capacityLeft = remainingProductTotal;
        for (int i = 0; i < allocations.size(); i++) {
            Allocation allocation = allocations.get(i);
            BigDecimal capacity = remainingByItem.get(i);
            if (capacity.signum() <= 0) continue;

            BigDecimal part = capacity.compareTo(capacityLeft) == 0
                    ? refundLeft
                    : refundLeft.multiply(capacity).divide(capacityLeft, 2, RoundingMode.HALF_UP);
            if (part.compareTo(capacity) > 0) part = capacity;
            if (part.compareTo(refundLeft) > 0) part = refundLeft;

            if (part.signum() > 0) {
                createRefundAdjustment(conn, allocation, part, sourceType, sourceId, adminUserId);
                refundLeft = refundLeft.subtract(part);
            }
            capacityLeft = capacityLeft.subtract(capacity);
            if (refundLeft.signum() <= 0) break;
        }
        if (refundLeft.signum() > 0) {
            throw new IllegalArgumentException("Không thể phân bổ toàn bộ khoản hoàn vào giá trị sản phẩm còn lại.");
        }
    }

    private static void recordShippingRefund(Connection conn, long orderId, BigDecimal refundAmount,
                                              String sourceType, long sourceId, long adminUserId)
            throws SQLException {
        if (!List.of("RETURN_REFUND", "MANUAL_PAYMENT_REFUND").contains(sourceType)) {
            throw new IllegalArgumentException("Nguồn điều chỉnh hoàn phí vận chuyển không hợp lệ.");
        }
        BigDecimal shippingFee;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COALESCE(shipping_fee,0) FROM orders WHERE order_id=? FOR UPDATE")) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy đơn hàng để đối soát phí vận chuyển.");
                shippingFee = rs.getBigDecimal(1);
            }
        }
        BigDecimal previousShippingRefunds;
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT COALESCE(SUM(debit_amount),0)
                FROM financial_ledger
                WHERE order_id=? AND entry_type='REFUND_SHIPPING_FEE'
                """)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                previousShippingRefunds = rs.getBigDecimal(1);
            }
        }
        BigDecimal remainingShippingFee = shippingFee.subtract(
                previousShippingRefunds == null ? BigDecimal.ZERO : previousShippingRefunds);
        if (refundAmount.compareTo(remainingShippingFee) > 0) {
            throw new IllegalArgumentException("Tổng tiền hoàn vượt số phí vận chuyển còn có thể hoàn.");
        }

        String reference = "SHIPPING-REFUND-" + sourceType + "-" + sourceId;
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO financial_ledger
                    (order_id, entry_type, debit_amount, currency, reference_code,
                     description, created_by_user_id, created_at)
                SELECT order_id, 'REFUND_SHIPPING_FEE', ?, COALESCE(currency, 'VND'), ?,
                       'Hoàn lại phí vận chuyển; không ảnh hưởng hoa hồng sản phẩm', ?, NOW()
                FROM orders
                WHERE order_id=?
                  AND NOT EXISTS (
                    SELECT 1 FROM financial_ledger
                    WHERE entry_type='REFUND_SHIPPING_FEE' AND reference_code=?
                  )
                """)) {
            ps.setBigDecimal(1, refundAmount);
            ps.setString(2, reference);
            ps.setLong(3, adminUserId);
            ps.setLong(4, orderId);
            ps.setString(5, reference);
            ps.executeUpdate();
        }
    }

    private static BigDecimal remainingAmount(Connection conn, Allocation allocation) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT COALESCE(SUM(refund_amount),0)
                FROM financial_refund_adjustments WHERE allocation_id=?
                """)) {
            ps.setLong(1, allocation.id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                BigDecimal refunded = rs.getBigDecimal(1);
                return allocation.gross.subtract(refunded == null ? BigDecimal.ZERO : refunded)
                        .max(BigDecimal.ZERO);
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

        // Determine settlement linkage before creating ledger rows; ledger entries are append-only.
        Long linkedSettlementId = null;
        if ("SETTLED".equalsIgnoreCase(allocation.status) && allocation.settlementId != null) {
            String settlementStatus = null;
            BigDecimal batchNetAmount = BigDecimal.ZERO;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT status, seller_net_amount FROM seller_monthly_settlements WHERE settlement_id=? FOR UPDATE")) {
                ps.setLong(1, allocation.settlementId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        settlementStatus = rs.getString("status");
                        batchNetAmount = rs.getBigDecimal("seller_net_amount");
                    }
                }
            }
            if ("READY".equalsIgnoreCase(settlementStatus)) {
                try (PreparedStatement ps = conn.prepareStatement("""
                        UPDATE seller_monthly_settlements
                        SET status=CASE WHEN seller_net_amount<=? THEN 'REVIEW_REQUIRED' ELSE status END,
                            gross_product_amount=gross_product_amount-?,
                            commission_amount=commission_amount-?,
                            seller_net_amount=seller_net_amount-?,
                            refund_amount=refund_amount+?,
                            commission_reversal_amount=commission_reversal_amount+?,
                            seller_recovery_amount=seller_recovery_amount+?
                        WHERE settlement_id=? AND status='READY'
                        """)) {
                    ps.setBigDecimal(1, sellerRecovery);
                    ps.setBigDecimal(2, refundAmount);
                    ps.setBigDecimal(3, commissionReversal);
                    ps.setBigDecimal(4, sellerRecovery);
                    ps.setBigDecimal(5, refundAmount);
                    ps.setBigDecimal(6, commissionReversal);
                    ps.setBigDecimal(7, sellerRecovery);
                    ps.setLong(8, allocation.settlementId);
                    if (ps.executeUpdate() == 1 && batchNetAmount.compareTo(sellerRecovery) > 0) {
                        try (PreparedStatement update = conn.prepareStatement("""
                                UPDATE financial_refund_adjustments
                                SET adjustment_status='SETTLED', settlement_id=?
                                WHERE adjustment_id=? AND adjustment_status='OPEN'
                                """)) {
                            update.setLong(1, allocation.settlementId);
                            update.setLong(2, adjustmentId);
                            update.executeUpdate();
                        }
                        linkedSettlementId = allocation.settlementId;
                    }
                }
            }
        }

        insertRefundLedger(conn, allocation, adjustmentId, linkedSettlementId, adminUserId,
                "REFUND_PLATFORM_COMMISSION", commissionReversal, "Điều chỉnh giảm hoa hồng do hoàn tiền");
        insertRefundLedger(conn, allocation, adjustmentId, linkedSettlementId, adminUserId,
                "REFUND_SELLER_RECOVERY", sellerRecovery, "Khoản thu hồi từ số dư phải trả xưởng do hoàn tiền");
    }

    private static void insertRefundLedger(Connection conn, Allocation allocation, long adjustmentId,
                                           Long settlementId, long adminUserId, String entryType,
                                           BigDecimal amount, String description) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO financial_ledger
                    (allocation_id, settlement_id, order_id, shop_id, entry_type, debit_amount,
                     currency, reference_code, description, created_by_user_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
                """)) {
            ps.setLong(1, allocation.id);
            if (settlementId == null) ps.setNull(2, java.sql.Types.BIGINT); else ps.setLong(2, settlementId);
            ps.setLong(3, allocation.orderId);
            ps.setLong(4, allocation.shopId);
            ps.setString(5, entryType);
            ps.setBigDecimal(6, amount);
            ps.setString(7, allocation.currency);
            ps.setString(8, "FIN-REFUND-" + adjustmentId);
            ps.setString(9, description);
            ps.setLong(10, adminUserId);
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
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT settlement_id FROM seller_monthly_settlements
                WHERE period_start=? AND period_end=? LIMIT 1 FOR UPDATE
                """)) {
            ps.setDate(1, java.sql.Date.valueOf(periodStart));
            ps.setDate(2, java.sql.Date.valueOf(periodEnd));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    throw new IllegalArgumentException(
                            "Kỳ quyết toán này đã được tạo. Các khoản phát sinh sau đó sẽ chuyển sang kỳ tháng kế tiếp.");
                }
            }
        }

        String totalsSql = """
                SELECT scope.shop_id, scope.currency,
                    COALESCE((SELECT SUM(a.gross_product_amount) FROM order_financial_allocations a
                              JOIN orders o ON o.order_id=a.order_id
                              WHERE a.shop_id=scope.shop_id AND a.currency=scope.currency
                                AND a.allocation_status='AVAILABLE'
                                AND o.status='Đã hoàn thành'
                                AND a.created_at<DATE_ADD(?, INTERVAL 1 DAY)),0) gross_sales,
                    COALESCE((SELECT SUM(a.commission_amount) FROM order_financial_allocations a
                              JOIN orders o ON o.order_id=a.order_id
                              WHERE a.shop_id=scope.shop_id AND a.currency=scope.currency
                                AND a.allocation_status='AVAILABLE'
                                AND o.status='Đã hoàn thành'
                                AND a.created_at<DATE_ADD(?, INTERVAL 1 DAY)),0) commission_sales,
                    COALESCE((SELECT SUM(a.seller_net_amount) FROM order_financial_allocations a
                              JOIN orders o ON o.order_id=a.order_id
                              WHERE a.shop_id=scope.shop_id AND a.currency=scope.currency
                                AND a.allocation_status='AVAILABLE'
                                AND o.status='Đã hoàn thành'
                                AND a.created_at<DATE_ADD(?, INTERVAL 1 DAY)),0) seller_sales,
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
                    SELECT a.shop_id, a.currency FROM order_financial_allocations a
                    JOIN orders o ON o.order_id=a.order_id
                    WHERE a.allocation_status='AVAILABLE'
                      AND o.status='Đã hoàn thành'
                      AND a.created_at<DATE_ADD(?, INTERVAL 1 DAY)
                    GROUP BY a.shop_id, a.currency
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
            for (int i = 1; i <= 4; i++) {
                ps.setDate(i, java.sql.Date.valueOf(periodEnd));
            }
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
                              AND created_at<DATE_ADD(?, INTERVAL 1 DAY)
                              AND order_id IN (SELECT order_id FROM orders WHERE status='Đã hoàn thành')
                            """)) {
                        update.setLong(1, settlementId);
                        update.setLong(2, shopId);
                        update.setString(3, currency);
                        update.setDate(4, java.sql.Date.valueOf(periodEnd));
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
