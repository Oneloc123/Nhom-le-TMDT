package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class FavoriteDao {

    private FavoriteDao() {
    }

    public static List<Long> listProductIds(long userId) {
        String sql = "SELECT f.product_id FROM favorites f"
                + " JOIN products p ON p.product_id = f.product_id AND p.is_active = 1"
                + " WHERE f.user_id = ? ORDER BY f.created_at DESC, f.favorite_id DESC";
        List<Long> ids = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong(1));
                }
            }
            return ids;
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static boolean exists(long userId, long productId) {
        String sql = "SELECT COUNT(*) FROM favorites WHERE user_id = ? AND product_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, productId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getLong(1) > 0;
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static boolean add(long userId, long productId) {
        if (exists(userId, productId)) {
            return false;
        }
        String sql = "INSERT INTO favorites (user_id, product_id, created_at) VALUES (?, ?, NOW())";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, userId);
            ps.setLong(2, productId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                return false; // đã có (phòng hở ràng buộc unique)
            }
            throw DataAccessException.of(e);
        }
    }

    public static boolean remove(long userId, long productId) {
        String sql = "DELETE FROM favorites WHERE user_id = ? AND product_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, productId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }
}
