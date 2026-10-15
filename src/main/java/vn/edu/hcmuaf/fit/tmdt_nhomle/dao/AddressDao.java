package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Address;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Truy cập bảng addresses. Mọi thao tác đọc/sửa/xóa đều kèm điều kiện
 * user_id để một tài khoản không thể chạm tới địa chỉ của người khác
 * (không có foreign key trong schema nên DAO tự đảm bảo quyền sở hữu).
 */
public final class AddressDao {

    private static final String COLUMNS =
            "address_id, user_id, recipient_name, recipient_phone, province, district, address_line, is_default";

    private AddressDao() {
    }

    public static List<Address> findByUser(long userId) {
        String sql = "SELECT " + COLUMNS + " FROM addresses WHERE user_id = ?"
                + " ORDER BY is_default DESC, address_id";
        List<Address> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static Optional<Address> findByIdForUser(long addressId, long userId) {
        String sql = "SELECT " + COLUMNS + " FROM addresses WHERE address_id = ? AND user_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, addressId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    /** Thêm địa chỉ mới; makeDefault=true sẽ bỏ mặc định của các địa chỉ khác (transaction). */
    public static Address insert(Address address, boolean makeDefault) {
        String sql = "INSERT INTO addresses (user_id, recipient_name, recipient_phone,"
                + " province, district, address_line, is_default) VALUES (?, ?, ?, ?, ?, ?, FALSE)";
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long id;
                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, address.getUserId());
                    ps.setString(2, address.getRecipientName());
                    ps.setString(3, address.getRecipientPhone());
                    ps.setString(4, address.getProvince());
                    ps.setString(5, address.getDistrict());
                    ps.setString(6, address.getAddressLine());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Không lấy được address_id vừa tạo.");
                        }
                        id = keys.getLong(1);
                    }
                }
                if (makeDefault) {
                    clearDefault(conn, address.getUserId());
                    setDefaultFlag(conn, id, address.getUserId(), true);
                } else if (!hasAny(conn, address.getUserId())) {
                    // Địa chỉ đầu tiên luôn là mặc định.
                    setDefaultFlag(conn, id, address.getUserId(), true);
                }
                conn.commit();
                return findByIdForUser(id, address.getUserId()).orElseThrow();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    /** Cập nhật địa chỉ; kiểm tra quyền sở hữu qua user_id. */
    public static void update(Address address, boolean makeDefault) {
        String sql = "UPDATE addresses SET recipient_name = ?, recipient_phone = ?, province = ?,"
                + " district = ?, address_line = ? WHERE address_id = ? AND user_id = ?";
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int updated;
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, address.getRecipientName());
                    ps.setString(2, address.getRecipientPhone());
                    ps.setString(3, address.getProvince());
                    ps.setString(4, address.getDistrict());
                    ps.setString(5, address.getAddressLine());
                    ps.setLong(6, address.getAddressId());
                    ps.setLong(7, address.getUserId());
                    updated = ps.executeUpdate();
                }
                if (updated == 0) {
                    throw new DataAccessException("Địa chỉ không tồn tại hoặc bạn không có quyền sửa.");
                }
                if (makeDefault) {
                    clearDefault(conn, address.getUserId());
                    setDefaultFlag(conn, address.getAddressId(), address.getUserId(), true);
                }
                conn.commit();
            } catch (SQLException | DataAccessException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    /** Xóa địa chỉ; trả về true nếu có dòng bị ảnh hưởng (kiểm tra quyền sở hữu). */
    public static boolean delete(long addressId, long userId) {
        String sql = "DELETE FROM addresses WHERE address_id = ? AND user_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, addressId);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static void setDefault(long addressId, long userId) {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (!setIdDefault(conn, addressId, userId)) {
                    throw new DataAccessException("Địa chỉ không tồn tại hoặc bạn không có quyền sửa.");
                }
                clearDefault(conn, userId);
                setDefaultFlag(conn, addressId, userId, true);
                conn.commit();
            } catch (SQLException | DataAccessException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    private static boolean setIdDefault(Connection conn, long addressId, long userId) throws SQLException {
        String check = "SELECT COUNT(*) FROM addresses WHERE address_id = ? AND user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(check)) {
            ps.setLong(1, addressId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getLong(1) > 0;
            }
        }
    }

    private static void clearDefault(Connection conn, long userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE addresses SET is_default = FALSE WHERE user_id = ?")) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
    }

    private static void setDefaultFlag(Connection conn, long addressId, long userId, boolean value)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE addresses SET is_default = ? WHERE address_id = ? AND user_id = ?")) {
            ps.setBoolean(1, value);
            ps.setLong(2, addressId);
            ps.setLong(3, userId);
            ps.executeUpdate();
        }
    }

    private static boolean hasAny(Connection conn, long userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM addresses WHERE user_id = ?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getLong(1) > 0;
            }
        }
    }

    private static Address map(ResultSet rs) throws SQLException {
        Address a = new Address();
        a.setAddressId(rs.getLong("address_id"));
        a.setUserId(rs.getLong("user_id"));
        a.setRecipientName(rs.getString("recipient_name"));
        a.setRecipientPhone(rs.getString("recipient_phone"));
        a.setProvince(rs.getString("province"));
        a.setDistrict(rs.getString("district"));
        a.setAddressLine(rs.getString("address_line"));
        a.setDefaultAddress(rs.getBoolean("is_default"));
        return a;
    }
}
