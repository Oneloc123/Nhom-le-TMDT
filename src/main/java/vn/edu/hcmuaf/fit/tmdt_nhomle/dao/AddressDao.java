package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Address;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class AddressDao {
    public static final int MAX_ADDRESSES_PER_USER = 20;
    private static final String COLUMNS = "address_id, user_id, recipient_name, recipient_phone, province, district, ward, "
            + "address_line, postal_code, label, province_code, district_code, ward_code, latitude, longitude, "
            + "place_id, formatted_address, map_label, geo_source, delivery_note, is_default";

    private AddressDao() { }

    public static List<Address> findByUser(long userId) {
        String sql = "SELECT " + COLUMNS + " FROM addresses WHERE user_id = ? AND is_deleted = 0 "
                + "ORDER BY is_default DESC, address_id DESC";
        List<Address> list = new ArrayList<>();
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
            return list;
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static Optional<Address> findByIdForUser(long addressId, long userId) {
        String sql = "SELECT " + COLUMNS + " FROM addresses WHERE address_id = ? AND user_id = ? AND is_deleted = 0";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, addressId); ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(map(rs)) : Optional.empty(); }
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static Address insert(Address address, boolean makeDefault) {
        String sql = "INSERT INTO addresses (user_id, recipient_name, recipient_phone, province, district, ward, address_line, "
                + "postal_code, label, province_code, district_code, ward_code, latitude, longitude, place_id, formatted_address, "
                + "map_label, geo_source, delivery_note, is_default, is_deleted, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE, 0, NOW(), NOW())";
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                lockUser(conn, address.getUserId());
                if (countActive(conn, address.getUserId()) >= MAX_ADDRESSES_PER_USER) {
                    throw new DataAccessException("Mỗi tài khoản được lưu tối đa " + MAX_ADDRESSES_PER_USER + " địa chỉ.");
                }
                assertNotDuplicate(conn, address, 0);
                long id;
                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    bindAddress(ps, address);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Không lấy được address_id vừa tạo.");
                        id = keys.getLong(1);
                    }
                }
                if (makeDefault || countActive(conn, address.getUserId()) == 1) {
                    clearDefault(conn, address.getUserId());
                    setDefaultFlag(conn, id, address.getUserId(), true);
                }
                long defaultId = normalizeDefault(conn, address.getUserId());
                conn.commit();
                address.setAddressId(id);
                address.setDefaultAddress(defaultId == id);
                return address;
            } catch (SQLException | RuntimeException e) { rollback(conn, e); throw e; }
            finally { conn.setAutoCommit(true); }
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static void update(Address address, boolean makeDefault) {
        String sql = "UPDATE addresses SET recipient_name=?, recipient_phone=?, province=?, district=?, ward=?, address_line=?, "
                + "postal_code=?, label=?, province_code=?, district_code=?, ward_code=?, latitude=?, longitude=?, place_id=?, "
                + "formatted_address=?, map_label=?, geo_source=?, delivery_note=?, updated_at=NOW() "
                + "WHERE address_id=? AND user_id=? AND is_deleted=0";
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                lockUser(conn, address.getUserId());
                if (!addressBelongsToUser(conn, address.getAddressId(), address.getUserId()))
                    throw new DataAccessException("Địa chỉ không tồn tại hoặc bạn không có quyền sửa.");
                assertNotDuplicate(conn, address, address.getAddressId());
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    int i = bindAddressFields(ps, address, 1);
                    ps.setLong(i++, address.getAddressId()); ps.setLong(i, address.getUserId());
                    if (ps.executeUpdate() != 1) throw new DataAccessException("Không thể cập nhật địa chỉ. Vui lòng tải lại trang.");
                }
                if (makeDefault) { clearDefault(conn, address.getUserId()); setDefaultFlag(conn, address.getAddressId(), address.getUserId(), true); }
                normalizeDefault(conn, address.getUserId());
                conn.commit();
            } catch (SQLException | RuntimeException e) { rollback(conn, e); throw e; }
            finally { conn.setAutoCommit(true); }
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static boolean delete(long addressId, long userId) {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                lockUser(conn, userId);
                if (!addressBelongsToUser(conn, addressId, userId)) { conn.rollback(); return false; }
                try (PreparedStatement ps = conn.prepareStatement("UPDATE addresses SET is_deleted=1, is_default=0, updated_at=NOW() WHERE address_id=? AND user_id=? AND is_deleted=0")) {
                    ps.setLong(1, addressId); ps.setLong(2, userId);
                    if (ps.executeUpdate() != 1) { conn.rollback(); return false; }
                }
                normalizeDefault(conn, userId);
                conn.commit();
                return true;
            } catch (SQLException | RuntimeException e) { rollback(conn, e); throw e; }
            finally { conn.setAutoCommit(true); }
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static void setDefault(long addressId, long userId) {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                lockUser(conn, userId);
                if (!addressBelongsToUser(conn, addressId, userId)) throw new DataAccessException("Địa chỉ không tồn tại hoặc bạn không có quyền sửa.");
                clearDefault(conn, userId); setDefaultFlag(conn, addressId, userId, true); normalizeDefault(conn, userId); conn.commit();
            } catch (SQLException | RuntimeException e) { rollback(conn, e); throw e; }
            finally { conn.setAutoCommit(true); }
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    private static void bindAddress(PreparedStatement ps, Address a) throws SQLException {
        ps.setLong(1, a.getUserId()); bindAddressFields(ps, a, 2);
    }

    private static int bindAddressFields(PreparedStatement ps, Address a, int i) throws SQLException {
        ps.setString(i++, a.getRecipientName()); ps.setString(i++, a.getRecipientPhone()); ps.setString(i++, a.getProvince());
        ps.setString(i++, nullable(a.getDistrict())); ps.setString(i++, nullable(a.getWard())); ps.setString(i++, a.getAddressLine());
        ps.setString(i++, nullable(a.getPostalCode())); ps.setString(i++, label(a.getLabel()));
        ps.setString(i++, nullable(a.getProvinceCode())); ps.setString(i++, nullable(a.getDistrictCode())); ps.setString(i++, nullable(a.getWardCode()));
        setNullableDouble(ps, i++, a.getLatitude()); setNullableDouble(ps, i++, a.getLongitude());
        ps.setString(i++, nullable(a.getPlaceId())); ps.setString(i++, nullable(a.getFormattedAddress()));
        ps.setString(i++, nullable(a.getMapLabel())); ps.setString(i++, nullable(a.getGeoSource())); ps.setString(i++, nullable(a.getDeliveryNote()));
        return i;
    }

    private static void lockUser(Connection conn, long userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT user_id FROM users WHERE user_id=? FOR UPDATE")) {
            ps.setLong(1, userId); try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) throw new DataAccessException("Tài khoản không còn tồn tại."); }
        }
    }

    private static int countActive(Connection conn, long userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM addresses WHERE user_id=? AND is_deleted=0")) {
            ps.setLong(1, userId); try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    private static void assertNotDuplicate(Connection conn, Address address, long excludeId) throws SQLException {
        String sql = "SELECT address_id FROM addresses WHERE user_id=? AND is_deleted=0 AND address_id<>? "
                + "AND LOWER(TRIM(province))=? AND LOWER(TRIM(COALESCE(district,'')))=? "
                + "AND LOWER(TRIM(COALESCE(ward,'')))=? AND LOWER(TRIM(address_line))=? LIMIT 1 FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, address.getUserId()); ps.setLong(2, excludeId);
            ps.setString(3, normalize(address.getProvince())); ps.setString(4, normalize(address.getDistrict()));
            ps.setString(5, normalize(address.getWard())); ps.setString(6, normalize(address.getAddressLine()));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) throw new DataAccessException("Địa chỉ này gần như trùng với địa chỉ bạn đã lưu trước đó.");
            }
        }
    }

    private static String normalize(String value) {
        String raw = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return Normalizer.normalize(raw.replaceAll("\\s+", " "), Normalizer.Form.NFKC);
    }

    private static boolean addressBelongsToUser(Connection conn, long addressId, long userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT address_id FROM addresses WHERE address_id=? AND user_id=? AND is_deleted=0 FOR UPDATE")) {
            ps.setLong(1, addressId); ps.setLong(2, userId); try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private static void clearDefault(Connection conn, long userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE addresses SET is_default=FALSE WHERE user_id=? AND is_deleted=0")) { ps.setLong(1, userId); ps.executeUpdate(); }
    }

    private static void setDefaultFlag(Connection conn, long addressId, long userId, boolean value) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE addresses SET is_default=?, updated_at=NOW() WHERE address_id=? AND user_id=? AND is_deleted=0")) {
            ps.setBoolean(1, value); ps.setLong(2, addressId); ps.setLong(3, userId); ps.executeUpdate();
        }
    }

    private static long normalizeDefault(Connection conn, long userId) throws SQLException {
        Long selected = selectAddressId(conn, "SELECT address_id FROM addresses WHERE user_id=? AND is_deleted=0 AND is_default=TRUE ORDER BY address_id DESC LIMIT 1 FOR UPDATE", userId);
        if (selected == null) selected = selectAddressId(conn, "SELECT address_id FROM addresses WHERE user_id=? AND is_deleted=0 ORDER BY address_id DESC LIMIT 1 FOR UPDATE", userId);
        clearDefault(conn, userId); if (selected != null) setDefaultFlag(conn, selected, userId, true);
        return selected == null ? -1 : selected;
    }

    private static Long selectAddressId(Connection conn, String sql, long userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.setLong(1, userId); try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getLong(1) : null; } }
    }

    private static void setNullableDouble(PreparedStatement ps, int index, Double value) throws SQLException {
        if (value == null) ps.setNull(index, Types.DECIMAL); else ps.setBigDecimal(index, BigDecimal.valueOf(value));
    }

    private static String nullable(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private static String label(String value) {
        if (value == null) return "Nhà riêng";
        return switch (value.trim()) { case "Nhà riêng", "Văn phòng", "Xưởng", "Khác" -> value.trim(); default -> "Khác"; };
    }
    private static void rollback(Connection conn, Throwable original) { try { conn.rollback(); } catch (SQLException e) { original.addSuppressed(e); } }

    private static Address map(ResultSet rs) throws SQLException {
        Address a = new Address(); a.setAddressId(rs.getLong("address_id")); a.setUserId(rs.getLong("user_id"));
        a.setRecipientName(rs.getString("recipient_name")); a.setRecipientPhone(rs.getString("recipient_phone"));
        a.setProvince(rs.getString("province")); a.setDistrict(rs.getString("district")); a.setWard(rs.getString("ward"));
        a.setAddressLine(rs.getString("address_line")); a.setPostalCode(rs.getString("postal_code")); a.setLabel(rs.getString("label"));
        a.setProvinceCode(rs.getString("province_code")); a.setDistrictCode(rs.getString("district_code")); a.setWardCode(rs.getString("ward_code"));
        double lat = rs.getDouble("latitude"); if (!rs.wasNull()) a.setLatitude(lat);
        double lng = rs.getDouble("longitude"); if (!rs.wasNull()) a.setLongitude(lng);
        a.setPlaceId(rs.getString("place_id")); a.setFormattedAddress(rs.getString("formatted_address"));
        a.setMapLabel(rs.getString("map_label")); a.setGeoSource(rs.getString("geo_source")); a.setDeliveryNote(rs.getString("delivery_note"));
        a.setDefaultAddress(rs.getBoolean("is_default")); return a;
    }
}
