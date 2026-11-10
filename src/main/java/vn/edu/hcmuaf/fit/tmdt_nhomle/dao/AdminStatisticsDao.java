package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Database-backed analytics for the PrintKraft administrator workspace. */
public final class AdminStatisticsDao {
    private static final String PAID_ORDER = "(UPPER(COALESCE(o.payment_status,'')) IN ('PAID','SUCCESS','COMPLETED')"
            + " OR EXISTS (SELECT 1 FROM payments paid_p WHERE paid_p.order_id=o.order_id"
            + " AND UPPER(COALESCE(paid_p.status,'')) IN ('PAID','SUCCESS','COMPLETED')))";

    private AdminStatisticsDao() { }

    public static Map<String, Object> load(LocalDate from, LocalDate to, String groupBy) {
        if (from == null || to == null) throw new IllegalArgumentException("Vui lòng chọn ngày bắt đầu và ngày kết thúc.");
        if (from.isAfter(to)) throw new IllegalArgumentException("Ngày bắt đầu không được sau ngày kết thúc.");
        String grouping = groupBy == null ? "month" : groupBy.trim().toLowerCase(java.util.Locale.ROOT);
        if (!List.of("day", "month", "year").contains(grouping)) {
            throw new IllegalArgumentException("Kiểu thống kê phải là ngày, tháng hoặc năm.");
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if ("day".equals(grouping) && days > 366) {
            throw new IllegalArgumentException("Biểu đồ theo ngày chỉ hỗ trợ tối đa 366 ngày. Hãy chọn theo tháng hoặc năm.");
        }
        if ("month".equals(grouping) && days > 3660) {
            throw new IllegalArgumentException("Biểu đồ theo tháng chỉ hỗ trợ tối đa 10 năm. Hãy chọn theo năm.");
        }
        if ("year".equals(grouping) && days > 36525) {
            throw new IllegalArgumentException("Khoảng thống kê theo năm tối đa 100 năm.");
        }

        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("from", from.toString());
        result.put("to", to.toString());
        result.put("groupBy", grouping);
        result.put("summary", summary(start, end));

        long previousDays = days;
        LocalDate previousTo = from.minusDays(1);
        LocalDate previousFrom = previousTo.minusDays(previousDays - 1);
        result.put("previous", summary(previousFrom.atStartOfDay(), previousTo.plusDays(1).atStartOfDay()));

        String orderPeriod = periodExpression("o.placed_at", grouping);
        String userPeriod = periodExpression("u.created_at", grouping);
        String productPeriod = periodExpression("p.created_at", grouping);
        String shopPeriod = periodExpression("s.created_at", grouping);
        String customPeriod = periodExpression("r.created_at", grouping);
        Map<String, Object> trends = new LinkedHashMap<>();
        trends.put("revenue", rows("SELECT " + orderPeriod + " period, COALESCE(SUM(CASE WHEN " + PAID_ORDER
                + " THEN o.total_amount ELSE 0 END),0) value FROM orders o"
                + " WHERE o.placed_at >= ? AND o.placed_at < ? GROUP BY period ORDER BY period", start, end));
        trends.put("orders", rows("SELECT " + orderPeriod + " period, COUNT(*) value FROM orders o"
                + " WHERE o.placed_at >= ? AND o.placed_at < ? GROUP BY period ORDER BY period", start, end));
        trends.put("customers", rows("SELECT " + userPeriod + " period, COUNT(*) value FROM users u"
                + " WHERE UPPER(u.role)='CUSTOMER' AND u.created_at >= ? AND u.created_at < ?"
                + " GROUP BY period ORDER BY period", start, end));
        trends.put("products", rows("SELECT " + productPeriod + " period, COUNT(*) value FROM products p"
                + " WHERE p.created_at >= ? AND p.created_at < ? GROUP BY period ORDER BY period", start, end));
        trends.put("shops", rows("SELECT " + shopPeriod + " period, COUNT(*) value FROM print_shops s"
                + " WHERE s.created_at >= ? AND s.created_at < ? GROUP BY period ORDER BY period", start, end));
        trends.put("customRequests", rows("SELECT " + customPeriod + " period, COUNT(*) value FROM custom_print_requests r"
                + " WHERE r.created_at >= ? AND r.created_at < ? GROUP BY period ORDER BY period", start, end));
        result.put("trends", trends);

        result.put("orderStatuses", rows("SELECT COALESCE(NULLIF(o.status,''),'UNKNOWN') label, COUNT(*) value"
                + " FROM orders o WHERE o.placed_at >= ? AND o.placed_at < ?"
                + " GROUP BY COALESCE(NULLIF(o.status,''),'UNKNOWN') ORDER BY value DESC", start, end));
        result.put("paymentMethods", rows("SELECT COALESCE(NULLIF(p.payment_method,''),'Không xác định') label,"
                + " COUNT(*) value, COALESCE(SUM(p.amount),0) amount"
                + " FROM payments p WHERE UPPER(COALESCE(p.status,'')) IN ('PAID','SUCCESS','COMPLETED')"
                + " AND COALESCE(p.paid_at,p.created_at) >= ? AND COALESCE(p.paid_at,p.created_at) < ?"
                + " GROUP BY COALESCE(NULLIF(p.payment_method,''),'Không xác định') ORDER BY amount DESC", start, end));
        result.put("categorySales", rows("SELECT COALESCE(NULLIF(c.category_name,''),'Chưa phân loại') label,"
                + " COALESCE(SUM(oi.quantity),0) value, COALESCE(SUM(oi.line_total),0) amount"
                + " FROM orders o JOIN order_items oi ON oi.order_id=o.order_id"
                + " LEFT JOIN products p ON p.product_id=oi.product_id"
                + " LEFT JOIN categories c ON c.category_id=p.category_id"
                + " WHERE o.placed_at >= ? AND o.placed_at < ? AND " + PAID_ORDER
                + " GROUP BY c.category_id,c.category_name ORDER BY value DESC LIMIT 8", start, end));
        result.put("topProducts", rows("SELECT COALESCE(NULLIF(oi.product_name_snapshot,''),MAX(p.product_name),'Sản phẩm') name,"
                + " COALESCE(SUM(oi.quantity),0) quantity, COALESCE(SUM(oi.line_total),0) revenue"
                + " FROM orders o JOIN order_items oi ON oi.order_id=o.order_id"
                + " LEFT JOIN products p ON p.product_id=oi.product_id"
                + " WHERE o.placed_at >= ? AND o.placed_at < ? AND " + PAID_ORDER
                + " GROUP BY oi.product_id,oi.product_name_snapshot ORDER BY revenue DESC LIMIT 8", start, end));

        Map<String, Object> operations = new LinkedHashMap<>();
        operations.put("pendingOrders", scalarLong("SELECT COUNT(*) FROM orders WHERE UPPER(COALESCE(status,'')) IN ('PENDING','PENDING_PAYMENT','CONFIRMED','PROCESSING')"));
        operations.put("pendingShipments", scalarLong("SELECT COUNT(*) FROM shipments WHERE UPPER(COALESCE(status,'')) NOT IN ('DELIVERED','COMPLETED','CANCELLED','RETURNED')"));
        operations.put("openSupportTickets", scalarLong("SELECT COUNT(*) FROM support_tickets WHERE UPPER(COALESCE(status,'')) IN ('OPEN','IN_PROGRESS','WAITING_CUSTOMER')"));
        operations.put("pendingCustomRequests", scalarLong("SELECT COUNT(*) FROM custom_print_requests WHERE UPPER(COALESCE(status,''))='PENDING_REVIEW'"));
        operations.put("pendingQuotes", scalarLong("SELECT COUNT(*) FROM quote_requests WHERE status IN ('Chờ xử lý','PENDING','PENDING_REVIEW')"));
        operations.put("pendingProducts", scalarLong("SELECT COUNT(*) FROM products WHERE UPPER(COALESCE(moderation_status,''))='PENDING_REVIEW'"));
        operations.put("pendingSellerVerifications", scalarLong("SELECT COUNT(*) FROM print_shops WHERE UPPER(COALESCE(verification_status,''))='PENDING'"));
        operations.put("pendingChangeRequests", scalarLong("SELECT COUNT(*) FROM order_change_requests WHERE UPPER(COALESCE(status,''))='PENDING'"));
        operations.put("refundRequired", scalarLong("SELECT COUNT(*) FROM payments WHERE UPPER(COALESCE(status,''))='REFUND_REQUIRED'"));
        operations.put("unpublishedReviews", scalarLong("SELECT COUNT(*) FROM reviews WHERE COALESCE(is_published,0)=0"));
        result.put("operations", operations);
        result.put("reviewSummary", one("SELECT COUNT(*) reviewCount, COALESCE(AVG(rating),0) averageRating FROM reviews"));
        return result;
    }

    private static Map<String, Object> summary(LocalDateTime start, LocalDateTime end) {
        Map<String, Object> result = one("SELECT COUNT(*) orderCount,"
                + " COALESCE(SUM(CASE WHEN " + PAID_ORDER + " THEN 1 ELSE 0 END),0) paidOrderCount,"
                + " COALESCE(SUM(CASE WHEN " + PAID_ORDER + " THEN o.total_amount ELSE 0 END),0) revenue,"
                + " COALESCE(AVG(CASE WHEN " + PAID_ORDER + " THEN o.total_amount ELSE NULL END),0) averageOrderValue,"
                + " COALESCE(SUM(CASE WHEN UPPER(COALESCE(o.status,''))='CANCELLED' THEN 1 ELSE 0 END),0) cancelledOrderCount"
                + " FROM orders o WHERE o.placed_at >= ? AND o.placed_at < ?", start, end);
        result.put("newCustomers", scalarLong("SELECT COUNT(*) FROM users WHERE UPPER(role)='CUSTOMER' AND created_at >= ? AND created_at < ?", start, end));
        result.put("newProducts", scalarLong("SELECT COUNT(*) FROM products WHERE created_at >= ? AND created_at < ?", start, end));
        result.put("newShops", scalarLong("SELECT COUNT(*) FROM print_shops WHERE created_at >= ? AND created_at < ?", start, end));
        result.put("customRequests", scalarLong("SELECT COUNT(*) FROM custom_print_requests WHERE created_at >= ? AND created_at < ?", start, end));
        return result;
    }

    private static String periodExpression(String column, String groupBy) {
        return switch (groupBy) {
            case "day" -> "DATE_FORMAT(" + column + ", '%Y-%m-%d')";
            case "year" -> "DATE_FORMAT(" + column + ", '%Y')";
            default -> "DATE_FORMAT(" + column + ", '%Y-%m')";
        };
    }

    private static Map<String, Object> one(String sql, Object... args) {
        List<Map<String, Object>> rows = rows(sql, args);
        if (!rows.isEmpty()) return rows.get(0);
        return new LinkedHashMap<>();
    }

    private static List<Map<String, Object>> rows(String sql, Object... args) {
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, args);
            try (ResultSet rs = statement.executeQuery()) {
                int columns = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columns; i++) {
                        row.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i));
                    }
                    result.add(row);
                }
            }
            return result;
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    private static long scalarLong(String sql, Object... args) {
        List<Map<String, Object>> result = rows(sql, args);
        if (result.isEmpty() || result.get(0).isEmpty()) return 0L;
        Object value = result.get(0).values().iterator().next();
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private static void bind(PreparedStatement statement, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) {
            Object value = args[i];
            int index = i + 1;
            if (value instanceof LocalDateTime dateTime) statement.setTimestamp(index, Timestamp.valueOf(dateTime));
            else if (value instanceof LocalDate date) statement.setDate(index, java.sql.Date.valueOf(date));
            else statement.setObject(index, value);
        }
    }
}
