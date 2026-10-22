package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Store;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public final class ShopDao {

    /** Giới hạn số cửa hàng trả về một lần để không tải vô hạn dữ liệu. */
    public static final int MAX_RESULTS = 200;

    private ShopDao() {
    }

    public static List<Store> search(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase();
        List<Store> stores = new ArrayList<>();
        try (Connection conn = Database.getConnection()) {
            StringBuilder sql = new StringBuilder("SELECT ").append(columns(conn))
                    .append(" FROM print_shops s"
                            + " WHERE (s.verification_status = 'VERIFIED' OR s.verification_status IS NULL)"
                            + " AND (s.status IS NULL OR s.status NOT IN ('INACTIVE','BLOCKED'))");
            if (!normalized.isEmpty()) {
                sql.append(" AND (LOWER(s.shop_name) LIKE ? OR LOWER(s.description) LIKE ?"
                        + " OR LOWER(s.search_keywords) LIKE ?"
                        + " OR LOWER(s.province) LIKE ? OR LOWER(s.district) LIKE ?)");
            }
            sql.append(" ORDER BY s.average_rating DESC, s.shop_name LIMIT ").append(MAX_RESULTS);
            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
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
            }
            return stores;
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static Optional<Store> findById(long shopId) {
        try (Connection conn = Database.getConnection()) {
            String sql = "SELECT " + columns(conn) + " FROM print_shops s"
                    + " WHERE s.shop_id = ? AND (s.verification_status = 'VERIFIED' OR s.verification_status IS NULL)"
                    + " AND (s.status IS NULL OR s.status NOT IN ('INACTIVE','BLOCKED'))";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, shopId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(map(rs)) : Optional.empty();
                }
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    private static String columns(Connection conn) throws SQLException {
        Set<String> available = new HashSet<>();
        DatabaseMetaData metadata = conn.getMetaData();
        try (ResultSet rs = metadata.getColumns(conn.getCatalog(), null, "print_shops", null)) {
            while (rs.next()) available.add(rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
        }
        StringBuilder select = new StringBuilder("s.shop_id, s.shop_name, s.slug, s.description, s.logo_url,"
                + " s.province, s.district, s.contact_phone, s.search_keywords, s.average_rating,"
                + " s.status, s.verification_status,");
        appendOptionalColumn(select, available, "contact_email");
        appendOptionalColumn(select, available, "address_line");
        appendOptionalColumn(select, available, "capabilities");
        appendOptionalColumn(select, available, "accepted_file_formats");
        appendOptionalColumn(select, available, "max_build_dimensions");
        appendOptionalColumn(select, available, "operating_hours");
        appendOptionalColumn(select, available, "estimated_lead_days");
        appendOptionalColumn(select, available, "service_notes");
        select.append(" (SELECT COUNT(DISTINCT o.order_id) FROM order_items oi"
                + " JOIN orders o ON o.order_id = oi.order_id"
                + " WHERE oi.shop_id = s.shop_id AND o.status = 'Đã hoàn thành') AS completed_orders");
        return select.toString();
    }

    private static void appendOptionalColumn(StringBuilder select, Set<String> available, String column) {
        select.append(available.contains(column) ? " s." + column : " NULL")
                .append(" AS ").append(column).append(',');
    }

    private static Store map(ResultSet rs) throws SQLException {
        Store store = new Store();
        store.setId(rs.getLong("shop_id"));
        store.setName(rs.getString("shop_name"));
        store.setSlug(rs.getString("slug"));
        store.setDescription(rs.getString("description"));
        store.setLogoUrl(rs.getString("logo_url"));
        store.setContactPhone(rs.getString("contact_phone"));
        store.setContactEmail(rs.getString("contact_email"));
        store.setAddressLine(rs.getString("address_line"));
        store.setCapabilities(rs.getString("capabilities"));
        store.setAcceptedFileFormats(rs.getString("accepted_file_formats"));
        store.setMaxBuildDimensions(rs.getString("max_build_dimensions"));
        store.setOperatingHours(rs.getString("operating_hours"));
        int leadDays = rs.getInt("estimated_lead_days");
        store.setEstimatedLeadDays(rs.wasNull() ? null : leadDays);
        store.setServiceNotes(rs.getString("service_notes"));
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
