package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Store;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Truy cập bảng print_shops.
 * - location = province/district, specialty = search_keywords (schema không có
 *   cột location/specialty — xem README).
 * - completedOrders = số đơn không bị hủy có dòng hàng của shop (tính động).
 */
public final class ShopDao {

    private static final String COLUMNS =
            "s.shop_id, s.shop_name, s.slug, s.description, s.logo_url, s.province, s.district,"
                    + " s.contact_phone, s.search_keywords, s.average_rating, s.status, s.verification_status,"
                    + " (SELECT COUNT(DISTINCT o.order_id) FROM order_items oi"
                    + "  JOIN orders o ON o.order_id = oi.order_id"
                    + "  WHERE oi.shop_id = s.shop_id AND o.status <> 'Đã hủy') AS completed_orders";

    private ShopDao() {
    }

    public static List<Store> search(String query) {
        StringBuilder sql = new StringBuilder("SELECT ").append(COLUMNS)
                .append(" FROM print_shops s"
                        + " WHERE (s.status IS NULL OR s.status NOT IN ('INACTIVE','BLOCKED'))");
        String normalized = query == null ? "" : query.trim().toLowerCase();
        if (!normalized.isEmpty()) {
            sql.append(" AND (LOWER(s.shop_name) LIKE ? OR LOWER(s.description) LIKE ?"
                    + " OR LOWER(s.search_keywords) LIKE ?"
                    + " OR LOWER(s.province) LIKE ? OR LOWER(s.district) LIKE ?)");
        }
        sql.append(" ORDER BY s.average_rating DESC, s.shop_name");
        List<Store> stores = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            if (!normalized.isEmpty()) {
                String like = "%" + normalized + "%";
                for (int i = 1; i <= 5; i++) {
                    ps.setString(i, like);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    stores.add(map(rs));
                }
            }
            return stores;
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static Optional<Store> findById(long shopId) {
        String sql = "SELECT " + COLUMNS + " FROM print_shops s WHERE s.shop_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, shopId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    private static Store map(ResultSet rs) throws SQLException {
        Store store = new Store();
        store.setId(rs.getLong("shop_id"));
        store.setName(rs.getString("shop_name"));
        store.setSlug(rs.getString("slug"));
        store.setDescription(rs.getString("description"));
        store.setLogoUrl(rs.getString("logo_url"));
        store.setContactPhone(rs.getString("contact_phone"));
        store.setStatus(rs.getString("status"));
        store.setVerificationStatus(rs.getString("verification_status"));
        store.setRating(Math.round(rs.getDouble("average_rating") * 10) / 10.0);
        store.setCompletedOrders(rs.getLong("completed_orders"));

        String specialty = rs.getString("search_keywords");
        store.setSpecialty(specialty == null || specialty.isBlank() ? "Đa dạng" : specialty);

        String province = rs.getString("province");
        String district = rs.getString("district");
        StringBuilder location = new StringBuilder();
        if (district != null && !district.isBlank()) {
            location.append(district.trim());
        }
        if (province != null && !province.isBlank()) {
            if (location.length() > 0) {
                location.append(", ");
            }
            location.append(province.trim());
        }
        store.setLocation(location.length() == 0 ? "Chưa cập nhật" : location.toString());
        return store;
    }
}
