package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Creates immutable per-order-item financial snapshots after money is actually received.
 * COD is deliberately excluded here; it must pass the platform COD receipt workflow first.
 */
public final class FinancialAllocationService {
    private FinancialAllocationService() {}

    public static void allocatePaidOnlineOrder(Connection conn, long orderId, long paymentId)
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

        String sellerLedger = """
                INSERT INTO financial_ledger
                    (allocation_id, order_id, shop_id, entry_type, credit_amount,
                     currency, description, created_at)
                SELECT a.allocation_id, a.order_id, a.shop_id, 'SELLER_PAYABLE',
                       a.seller_net_amount, a.currency, 'Ghi nhận khoản phải trả xưởng',
                       NOW()
                FROM order_financial_allocations a
                WHERE a.order_id = ? AND a.payment_id = ?
                  AND NOT EXISTS (
                    SELECT 1 FROM financial_ledger l
                    WHERE l.allocation_id = a.allocation_id AND l.entry_type = 'SELLER_PAYABLE'
                  )
                """;
        try (PreparedStatement ps = conn.prepareStatement(sellerLedger)) {
            ps.setLong(1, orderId);
            ps.setLong(2, paymentId);
            ps.executeUpdate();
        }

        String commissionLedger = """
                INSERT INTO financial_ledger
                    (allocation_id, order_id, shop_id, entry_type, credit_amount,
                     currency, description, created_at)
                SELECT a.allocation_id, a.order_id, a.shop_id, 'PLATFORM_COMMISSION',
                       a.commission_amount, a.currency, 'Ghi nhận hoa hồng nền tảng 10%',
                       NOW()
                FROM order_financial_allocations a
                WHERE a.order_id = ? AND a.payment_id = ?
                  AND NOT EXISTS (
                    SELECT 1 FROM financial_ledger l
                    WHERE l.allocation_id = a.allocation_id AND l.entry_type = 'PLATFORM_COMMISSION'
                  )
                """;
        try (PreparedStatement ps = conn.prepareStatement(commissionLedger)) {
            ps.setLong(1, orderId);
            ps.setLong(2, paymentId);
            ps.executeUpdate();
        }
    }
}
