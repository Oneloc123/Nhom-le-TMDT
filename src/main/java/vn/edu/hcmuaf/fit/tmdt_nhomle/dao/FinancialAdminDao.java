package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.FinancialAllocationService;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Read/write boundary for the PrintKraft platform-finance Admin module. */
public final class FinancialAdminDao {
    private FinancialAdminDao() { }

    public static Map<String, Object> dashboard() {
        try (Connection conn = Database.getConnection()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("policy", readPolicy(conn));
            result.put("summary", readSummary(conn));
            result.put("pendingCod", readPendingCod(conn));
            result.put("settlements", readSettlements(conn));
            result.put("allocations", readRecentAllocations(conn));
            result.put("refundAdjustments", readRecentRefundAdjustments(conn));
            result.put("ledger", readRecentLedger(conn));
            return result;
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    private static Map<String, Object> readPolicy(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT commission_rate, commission_basis, settlement_cycle, cod_collection_mode, currency
                FROM platform_finance_settings WHERE setting_id=1
                """); ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new DataAccessException("Chưa khởi tạo cấu hình tài chính PrintKraft.");
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("commissionRate", rs.getBigDecimal("commission_rate"));
            map.put("commissionBasis", rs.getString("commission_basis"));
            map.put("settlementCycle", rs.getString("settlement_cycle"));
            map.put("codCollectionMode", rs.getString("cod_collection_mode"));
            map.put("currency", rs.getString("currency"));
            return map;
        }
    }

    private static Map<String, Object> readSummary(Connection conn) throws SQLException {
        Map<String, Object> map = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT
                    COALESCE(SUM(a.gross_product_amount),0)
                      - COALESCE((SELECT SUM(r.refund_amount) FROM financial_refund_adjustments r),0) gross_amount,
                    COALESCE(SUM(a.commission_amount),0)
                      - COALESCE((SELECT SUM(r.commission_reversal) FROM financial_refund_adjustments r),0) commission_amount,
                    COALESCE(SUM(CASE WHEN a.allocation_status='AVAILABLE' THEN a.seller_net_amount ELSE 0 END),0)
                      + COALESCE((SELECT SUM(st.seller_net_amount) FROM seller_monthly_settlements st WHERE st.status='READY'),0)
                      - COALESCE((SELECT SUM(r.seller_recovery) FROM financial_refund_adjustments r WHERE r.adjustment_status='OPEN'),0) outstanding_seller,
                    COALESCE((SELECT SUM(st.seller_net_amount) FROM seller_monthly_settlements st WHERE st.status='PAID'),0) settled_seller,
                    COUNT(*) allocation_count
                FROM order_financial_allocations a
                """); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                map.put("grossProductAmount", rs.getBigDecimal("gross_amount"));
                map.put("commissionAmount", rs.getBigDecimal("commission_amount"));
                map.put("outstandingSellerAmount", rs.getBigDecimal("outstanding_seller"));
                map.put("settledSellerAmount", rs.getBigDecimal("settled_seller"));
                map.put("allocationCount", rs.getLong("allocation_count"));
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT COUNT(*) cod_count, COALESCE(SUM(p.amount),0) cod_amount
                FROM payments p
                JOIN orders o ON o.order_id=p.order_id
                WHERE p.provider='COD' AND p.status='PENDING'
                  AND o.payment_status='COD (thanh toán khi nhận hàng)'
                """); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                map.put("pendingCodCount", rs.getLong("cod_count"));
                map.put("pendingCodAmount", rs.getBigDecimal("cod_amount"));
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT COUNT(*) ready_count, COALESCE(SUM(seller_net_amount),0) ready_amount
                FROM seller_monthly_settlements WHERE status='READY'
                """); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                map.put("readySettlementCount", rs.getLong("ready_count"));
                map.put("readySettlementAmount", rs.getBigDecimal("ready_amount"));
            }
        }
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT
                    COALESCE(SUM(a.commission_amount),0)
                      - COALESCE((SELECT SUM(r.commission_reversal) FROM financial_refund_adjustments r
                                  WHERE r.created_at>=? AND r.created_at<DATE_ADD(?, INTERVAL 1 DAY)),0) month_commission,
                    COALESCE(SUM(a.gross_product_amount),0)
                      - COALESCE((SELECT SUM(r.refund_amount) FROM financial_refund_adjustments r
                                  WHERE r.created_at>=? AND r.created_at<DATE_ADD(?, INTERVAL 1 DAY)),0) month_gross
                FROM order_financial_allocations a
                WHERE a.created_at>=? AND a.created_at<DATE_ADD(?, INTERVAL 1 DAY)
                """)) {
            ps.setDate(1, java.sql.Date.valueOf(monthStart));
            ps.setDate(2, java.sql.Date.valueOf(today));
            ps.setDate(3, java.sql.Date.valueOf(monthStart));
            ps.setDate(4, java.sql.Date.valueOf(today));
            ps.setDate(5, java.sql.Date.valueOf(monthStart));
            ps.setDate(6, java.sql.Date.valueOf(today));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    map.put("currentMonthCommission", rs.getBigDecimal("month_commission"));
                    map.put("currentMonthGross", rs.getBigDecimal("month_gross"));
                }
            }
        }
        return map;
    }

    private static List<Map<String, Object>> readPendingCod(Connection conn) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT o.order_id, o.order_code, p.payment_id, p.amount, p.created_at,
                       o.total_amount, o.currency,
                       GROUP_CONCAT(DISTINCT sh.shop_name ORDER BY sh.shop_name SEPARATOR ', ') shop_names
                FROM orders o
                JOIN payments p ON p.order_id=o.order_id
                LEFT JOIN order_items oi ON oi.order_id=o.order_id
                LEFT JOIN print_shops sh ON sh.shop_id=oi.shop_id
                WHERE p.provider='COD' AND p.status='PENDING'
                  AND o.payment_status='COD (thanh toán khi nhận hàng)'
                GROUP BY o.order_id, o.order_code, p.payment_id, p.amount, p.created_at,
                         o.total_amount, o.currency
                ORDER BY p.created_at ASC
                LIMIT 100
                """); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("orderId", rs.getLong("order_id"));
                row.put("orderCode", rs.getString("order_code"));
                row.put("paymentId", rs.getLong("payment_id"));
                row.put("amount", rs.getBigDecimal("amount"));
                row.put("totalAmount", rs.getBigDecimal("total_amount"));
                row.put("currency", rs.getString("currency"));
                row.put("createdAt", rs.getTimestamp("created_at"));
                row.put("shops", rs.getString("shop_names"));
                rows.add(row);
            }
        }
        return rows;
    }

    private static List<Map<String, Object>> readSettlements(Connection conn) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT st.settlement_id, st.shop_id, sh.shop_name, st.period_start, st.period_end,
                       st.currency, st.gross_product_amount, st.commission_amount,
                       st.seller_net_amount, st.refund_amount, st.commission_reversal_amount,
                       st.seller_recovery_amount, st.status, st.payout_reference, st.paid_at, st.created_at
                FROM seller_monthly_settlements st
                JOIN print_shops sh ON sh.shop_id=st.shop_id
                ORDER BY st.period_start DESC, st.created_at DESC, st.settlement_id DESC
                LIMIT 100
                """); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("settlementId", rs.getLong("settlement_id"));
                row.put("shopId", rs.getLong("shop_id"));
                row.put("shopName", rs.getString("shop_name"));
                row.put("periodStart", rs.getDate("period_start"));
                row.put("periodEnd", rs.getDate("period_end"));
                row.put("currency", rs.getString("currency"));
                row.put("grossProductAmount", rs.getBigDecimal("gross_product_amount"));
                row.put("commissionAmount", rs.getBigDecimal("commission_amount"));
                row.put("sellerNetAmount", rs.getBigDecimal("seller_net_amount"));
                row.put("refundAmount", rs.getBigDecimal("refund_amount"));
                row.put("commissionReversalAmount", rs.getBigDecimal("commission_reversal_amount"));
                row.put("sellerRecoveryAmount", rs.getBigDecimal("seller_recovery_amount"));
                row.put("status", rs.getString("status"));
                row.put("payoutReference", rs.getString("payout_reference"));
                row.put("paidAt", rs.getTimestamp("paid_at"));
                row.put("createdAt", rs.getTimestamp("created_at"));
                rows.add(row);
            }
        }
        return rows;
    }

    private static List<Map<String, Object>> readRecentAllocations(Connection conn) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT a.allocation_id, a.order_id, o.order_code, a.order_item_id,
                       oi.product_name_snapshot, a.shop_id, sh.shop_name, a.currency,
                       a.gross_product_amount, a.commission_rate_snapshot,
                       a.commission_amount, a.seller_net_amount, a.allocation_status,
                       a.created_at
                FROM order_financial_allocations a
                JOIN orders o ON o.order_id=a.order_id
                JOIN order_items oi ON oi.order_item_id=a.order_item_id
                JOIN print_shops sh ON sh.shop_id=a.shop_id
                ORDER BY a.created_at DESC, a.allocation_id DESC
                LIMIT 30
                """); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("allocationId", rs.getLong("allocation_id"));
                row.put("orderId", rs.getLong("order_id"));
                row.put("orderCode", rs.getString("order_code"));
                row.put("orderItemId", rs.getLong("order_item_id"));
                row.put("productName", rs.getString("product_name_snapshot"));
                row.put("shopId", rs.getLong("shop_id"));
                row.put("shopName", rs.getString("shop_name"));
                row.put("currency", rs.getString("currency"));
                row.put("grossProductAmount", rs.getBigDecimal("gross_product_amount"));
                row.put("commissionRate", rs.getBigDecimal("commission_rate_snapshot"));
                row.put("commissionAmount", rs.getBigDecimal("commission_amount"));
                row.put("sellerNetAmount", rs.getBigDecimal("seller_net_amount"));
                row.put("status", rs.getString("allocation_status"));
                row.put("createdAt", rs.getTimestamp("created_at"));
                rows.add(row);
            }
        }
        return rows;
    }

    private static List<Map<String, Object>> readRecentRefundAdjustments(Connection conn) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT r.adjustment_id, r.source_type, r.source_id, r.refund_amount,
                       r.commission_reversal, r.seller_recovery, r.adjustment_status, r.created_at,
                       o.order_code, oi.product_name_snapshot, sh.shop_name
                FROM financial_refund_adjustments r
                JOIN order_financial_allocations a ON a.allocation_id=r.allocation_id
                JOIN orders o ON o.order_id=a.order_id
                JOIN order_items oi ON oi.order_item_id=a.order_item_id
                JOIN print_shops sh ON sh.shop_id=a.shop_id
                ORDER BY r.created_at DESC, r.adjustment_id DESC
                LIMIT 50
                """); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("adjustmentId", rs.getLong("adjustment_id"));
                row.put("sourceType", rs.getString("source_type"));
                row.put("sourceId", rs.getLong("source_id"));
                row.put("refundAmount", rs.getBigDecimal("refund_amount"));
                row.put("commissionReversal", rs.getBigDecimal("commission_reversal"));
                row.put("sellerRecovery", rs.getBigDecimal("seller_recovery"));
                row.put("status", rs.getString("adjustment_status"));
                row.put("createdAt", rs.getTimestamp("created_at"));
                row.put("orderCode", rs.getString("order_code"));
                row.put("productName", rs.getString("product_name_snapshot"));
                row.put("shopName", rs.getString("shop_name"));
                rows.add(row);
            }
        }
        return rows;
    }

    private static List<Map<String, Object>> readRecentLedger(Connection conn) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT l.ledger_id, l.entry_type, l.debit_amount, l.credit_amount,
                       l.currency, l.reference_code, l.description, l.created_at,
                       o.order_code, sh.shop_name
                FROM financial_ledger l
                LEFT JOIN orders o ON o.order_id=l.order_id
                LEFT JOIN print_shops sh ON sh.shop_id=l.shop_id
                ORDER BY l.created_at DESC, l.ledger_id DESC
                LIMIT 100
                """); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("ledgerId", rs.getLong("ledger_id"));
                row.put("entryType", rs.getString("entry_type"));
                row.put("debitAmount", rs.getBigDecimal("debit_amount"));
                row.put("creditAmount", rs.getBigDecimal("credit_amount"));
                row.put("currency", rs.getString("currency"));
                row.put("referenceCode", rs.getString("reference_code"));
                row.put("description", rs.getString("description"));
                row.put("createdAt", rs.getTimestamp("created_at"));
                row.put("orderCode", rs.getString("order_code"));
                row.put("shopName", rs.getString("shop_name"));
                rows.add(row);
            }
        }
        return rows;
    }

    public static void recordCodReceipt(long orderId, String carrierName, String carrierReference,
                                        long adminUserId, String note) {
        inTransaction(conn -> {
            FinancialAllocationService.recordPlatformCodReceipt(
                    conn, orderId, carrierName, carrierReference, adminUserId, note);
            return null;
        });
    }

    public static int createMonthlySettlement(LocalDate periodStart, LocalDate periodEnd, long adminUserId) {
        return inTransaction(conn ->
                FinancialAllocationService.createMonthlySettlements(conn, periodStart, periodEnd, adminUserId));
    }

    public static void markSettlementPaid(long settlementId, long adminUserId,
                                          String payoutReference, String note) {
        inTransaction(conn -> {
            FinancialAllocationService.markSettlementPaid(
                    conn, settlementId, adminUserId, payoutReference, note);
            return null;
        });
    }

    private static <T> T inTransaction(SqlWork<T> work) {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                T result = work.execute(conn);
                conn.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                try { conn.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    @FunctionalInterface
    private interface SqlWork<T> {
        T execute(Connection connection) throws SQLException;
    }
}
