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
                    COALESCE(SUM(gross_product_amount),0) gross_amount,
                    COALESCE(SUM(commission_amount),0) commission_amount,
                    COALESCE(SUM(CASE WHEN allocation_status='AVAILABLE' THEN seller_net_amount ELSE 0 END),0) outstanding_seller,
                    COALESCE(SUM(CASE WHEN allocation_status='SETTLED' THEN seller_net_amount ELSE 0 END),0) settled_seller,
                    COUNT(*) allocation_count
                FROM order_financial_allocations
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
                SELECT COALESCE(SUM(commission_amount),0) month_commission,
                       COALESCE(SUM(gross_product_amount),0) month_gross
                FROM order_financial_allocations
                WHERE created_at>=? AND created_at<DATE_ADD(?, INTERVAL 1 DAY)
                """)) {
            ps.setDate(1, java.sql.Date.valueOf(monthStart));
            ps.setDate(2, java.sql.Date.valueOf(today));
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
                       st.seller_net_amount, st.status, st.payout_reference, st.paid_at, st.created_at
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
