package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CategoryDao {

    private CategoryDao() {
    }

    public static List<Category> findAllActive() {
        String sql = "SELECT category_id, parent_id, category_name, slug, is_active, display_order"
                + " FROM categories WHERE is_active = 1 ORDER BY display_order, category_name";
        List<Category> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static Optional<Category> findById(long categoryId) {
        String sql = "SELECT category_id, parent_id, category_name, slug, is_active, display_order"
                + " FROM categories WHERE category_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    private static Category map(ResultSet rs) throws SQLException {
        Category c = new Category();
        c.setCategoryId(rs.getLong("category_id"));
        long parent = rs.getLong("parent_id");
        c.setParentId(rs.wasNull() ? null : parent);
        c.setName(rs.getString("category_name"));
        c.setSlug(rs.getString("slug"));
        c.setActive(rs.getBoolean("is_active"));
        c.setDisplayOrder(rs.getInt("display_order"));
        return c;
    }
}
