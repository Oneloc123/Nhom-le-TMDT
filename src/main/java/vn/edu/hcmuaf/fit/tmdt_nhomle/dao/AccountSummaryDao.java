package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Số liệu tổng quan tài khoản khách — ĐƯỢC TÍNH TỪ DATABASE theo user đang đăng
 * nhập, không dùng số cố định hay dữ liệu mẫu.
 */
public final class AccountSummaryDao {

    private AccountSummaryDao() {
    }

    public static Map<String, Object> overview(long userId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("addressCount", scalar(
                "SELECT COUNT(*) FROM addresses WHERE user_id = ?", userId));
        result.put("favoriteCount", scalar(
                "SELECT COUNT(*) FROM favorites WHERE user_id = ?", userId));
        result.put("designCount", scalar(
                "SELECT COUNT(*) FROM saved_custom_designs WHERE user_id = ?", userId));
        result.put("orderCount", scalar(
                "SELECT COUNT(*) FROM orders WHERE user_id = ?", userId));
        result.put("orderStatusCounts", orderStatusCounts(userId));
        result.put("recentOrders", recentOrders(userId));
        return result;
    }

    /** Đếm đơn theo từng trạng thái (badge “Chờ xác nhận / Đang giao / …”). */
    public static List<Map<String, Object>> orderStatusCounts(long userId) {
        String sql = "SELECT status, COUNT(*) count FROM orders WHERE user_id = ?"
                + " GROUP BY status ORDER BY status";
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("status", rs.getString("status"));
                    row.put("count", rs.getLong("count"));
                    rows.add(row);
                }
            }
            return rows;
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    /** 3 đơn gần nhất — chỉ trường an toàn để hiển thị (không có ghi chú nội bộ). */
    public static List<Map<String, Object>> recentOrders(long userId) {
        String sql = "SELECT order_id orderId, order_code orderCode, status,"
                + " payment_status paymentStatus, total_amount totalAmount, currency,"
                + " placed_at placedAt FROM orders WHERE user_id = ?"
                + " ORDER BY placed_at DESC, order_id DESC LIMIT 3";
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("orderId", rs.getLong("orderId"));
                    row.put("orderCode", rs.getString("orderCode"));
                    row.put("status", rs.getString("status"));
                    row.put("paymentStatus", rs.getString("paymentStatus"));
                    row.put("totalAmount", rs.getBigDecimal("totalAmount"));
                    row.put("currency", rs.getString("currency"));
                    Timestamp placedAt = rs.getTimestamp("placedAt");
                    row.put("placedAt", placedAt == null ? null : placedAt.toString());
                    rows.add(row);
                }
            }
            return rows;
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    private static long scalar(String sql, long userId) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }
}
