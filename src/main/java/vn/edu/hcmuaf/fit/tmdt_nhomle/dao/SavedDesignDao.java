package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.SavedDesign;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * DAO cho bảng {@code saved_custom_designs}.
 *
 * <p>NGUYÊN TẮC AN TOÀN: mọi đọc/sửa/xoá/nhân bản đều khoá theo
 * {@code user_id} của phiên đăng nhập — không có phương thức nào cho phép thao tác
 * trên thiết kế của người khác bằng cách đổi id trên URL (chống IDOR).</p>
 */
public final class SavedDesignDao {

    public static final int DEFAULT_PAGE_SIZE = 6;
    public static final int MAX_PAGE_SIZE = 24;

    private static final String SELECT_BASE =
            "SELECT d.design_id designId, d.user_id userId, d.product_id productId,"
                    + " d.design_file_id designFileId, d.design_name designName,"
                    + " d.config_json configJson, d.material, d.color, d.dimensions,"
                    + " d.quantity, d.preview_url previewUrl, d.notes, d.status,"
                    + " d.version, d.created_at createdAt, d.updated_at updatedAt,"
                    + " p.product_name productName, p.is_customizable productCustomizable,"
                    + " p.is_active productActive"
                    + " FROM saved_custom_designs d"
                    + " LEFT JOIN products p ON p.product_id = d.product_id";

    private SavedDesignDao() {
    }

    // ------------------------------------------------------------------
    // Đọc
    // ------------------------------------------------------------------

    /** Trang thiết kế của một khách, có tìm kiếm theo tên (tham số hoá). */
    public static Map<String, Object> listByUser(long userId, String search,
                                                 int page, int pageSize) {
        int size = Math.min(Math.max(1, pageSize), MAX_PAGE_SIZE);
        StringBuilder where = new StringBuilder(" WHERE d.user_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(userId);
        if (search != null && !search.isBlank()) {
            String term = search.trim();
            if (term.length() > 80) {
                throw new IllegalArgumentException("Từ khóa tìm kiếm tối đa 80 ký tự.");
            }
            where.append(" AND d.design_name LIKE ? ESCAPE '!' ");
            args.add("%" + term.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%");
        }

        // Đếm trực tiếp trên bảng, không JOIN — tránh đếm lỗi khi có sản phẩm trùng.
        long total = scalar("SELECT COUNT(*) FROM saved_custom_designs d" + where, args.toArray());

        int pages = Math.max(1, (int) Math.ceil((double) total / size));
        int current = Math.min(Math.max(1, page), pages);

        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add((long) (current - 1) * size);
        List<SavedDesign> items = rows(SELECT_BASE + where
                + " ORDER BY d.updated_at DESC, d.design_id DESC LIMIT ? OFFSET ?",
                pageArgs.toArray());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("total", total);
        result.put("page", current);
        result.put("pageSize", size);
        result.put("pages", pages);
        return result;
    }

    public static long countByUser(long userId) {
        return scalar("SELECT COUNT(*) FROM saved_custom_designs WHERE user_id = ?", userId);
    }

    /** Tìm theo id KÈM điều kiện chủ sở hữu — trả về Optional rỗng nếu không phải của mình. */
    public static Optional<SavedDesign> findByIdForUser(long designId, long userId) {
        List<SavedDesign> items = rows(SELECT_BASE + " WHERE d.design_id = ? AND d.user_id = ?",
                designId, userId);
        return items.isEmpty() ? Optional.empty() : Optional.of(items.getFirst());
    }

    // ------------------------------------------------------------------
    // Ghi (luôn kèm user_id trong WHERE/VALUES)
    // ------------------------------------------------------------------

    public static SavedDesign insert(SavedDesign design) {
        String sql = "INSERT INTO saved_custom_designs (user_id, product_id, design_file_id,"
                + " design_name, config_json, material, color, dimensions, quantity,"
                + " preview_url, notes, status, version, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindWritable(ps, design, 1);
            ps.executeUpdate();
            long id;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Không lấy được design_id vừa tạo.");
                }
                id = keys.getLong(1);
            }
            return findByIdForUser(id, design.getUserId()).orElseThrow();
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    /** Cập nhật — trả về false nếu thiết kế không tồn tại hoặc không thuộc về userId. */
    public static boolean update(SavedDesign design) {
        String sql = "UPDATE saved_custom_designs SET product_id = ?, design_file_id = ?,"
                + " design_name = ?, config_json = ?, material = ?, color = ?, dimensions = ?,"
                + " quantity = ?, preview_url = ?, notes = ?, status = ?,"
                + " version = version + 1, updated_at = NOW()"
                + " WHERE design_id = ? AND user_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            ps.setObject(i++, design.getProductId());
            ps.setObject(i++, design.getDesignFileId());
            ps.setString(i++, design.getDesignName());
            ps.setString(i++, design.getConfigJson());
            ps.setString(i++, design.getMaterial());
            ps.setString(i++, design.getColor());
            ps.setString(i++, design.getDimensions());
            ps.setInt(i++, design.getQuantity());
            ps.setString(i++, design.getPreviewUrl());
            ps.setString(i++, design.getNotes());
            ps.setString(i++, design.getStatus());
            ps.setLong(i++, design.getDesignId());
            ps.setLong(i, design.getUserId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static boolean delete(long designId, long userId) {
        String sql = "DELETE FROM saved_custom_designs WHERE design_id = ? AND user_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, designId);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    /** Nhân bản một thiết kế của chính mình — bản mới luôn là bản nháp DRAFT. */
    public static Optional<SavedDesign> duplicate(long designId, long userId) {
        SavedDesign source = findByIdForUser(designId, userId).orElse(null);
        if (source == null) {
            return Optional.empty();
        }
        SavedDesign copy = new SavedDesign();
        copy.setUserId(userId);
        copy.setProductId(source.getProductId());
        copy.setDesignFileId(source.getDesignFileId());
        copy.setDesignName(truncate(source.getDesignName() + " (bản sao)", 150));
        copy.setConfigJson(source.getConfigJson());
        copy.setMaterial(source.getMaterial());
        copy.setColor(source.getColor());
        copy.setDimensions(source.getDimensions());
        copy.setQuantity(source.getQuantity());
        copy.setPreviewUrl(source.getPreviewUrl());
        copy.setNotes(source.getNotes());
        copy.setStatus(SavedDesign.STATUS_DRAFT);
        copy.setVersion(1);
        return Optional.of(insert(copy));
    }

    // ------------------------------------------------------------------
    private static void bindWritable(PreparedStatement ps, SavedDesign d, int start)
            throws SQLException {
        int i = start;
        ps.setLong(i++, d.getUserId());
        ps.setObject(i++, d.getProductId());
        ps.setObject(i++, d.getDesignFileId());
        ps.setString(i++, d.getDesignName());
        ps.setString(i++, d.getConfigJson());
        ps.setString(i++, d.getMaterial());
        ps.setString(i++, d.getColor());
        ps.setString(i++, d.getDimensions());
        ps.setInt(i++, d.getQuantity());
        ps.setString(i++, d.getPreviewUrl());
        ps.setString(i++, d.getNotes());
        ps.setString(i++, d.getStatus());
        ps.setInt(i, d.getVersion());
    }

    private static List<SavedDesign> rows(String sql, Object... args) {
        List<SavedDesign> items = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(map(rs));
                }
            }
            return items;
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    private static long scalar(String sql, Object... args) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    private static SavedDesign map(ResultSet rs) throws SQLException {
        SavedDesign d = new SavedDesign();
        d.setDesignId(rs.getLong("designId"));
        d.setUserId(rs.getLong("userId"));
        long productId = rs.getLong("productId");
        d.setProductId(rs.wasNull() ? null : productId);
        long fileId = rs.getLong("designFileId");
        d.setDesignFileId(rs.wasNull() ? null : fileId);
        d.setDesignName(rs.getString("designName"));
        d.setConfigJson(rs.getString("configJson"));
        d.setMaterial(rs.getString("material"));
        d.setColor(rs.getString("color"));
        d.setDimensions(rs.getString("dimensions"));
        d.setQuantity(rs.getInt("quantity"));
        d.setPreviewUrl(rs.getString("previewUrl"));
        d.setNotes(rs.getString("notes"));
        d.setStatus(rs.getString("status"));
        d.setVersion(rs.getInt("version"));
        d.setCreatedAt(timestamp(rs.getTimestamp("createdAt")));
        d.setUpdatedAt(timestamp(rs.getTimestamp("updatedAt")));
        d.setProductName(rs.getString("productName"));
        boolean customizable = rs.getBoolean("productCustomizable");
        d.setProductCustomizable(rs.wasNull() ? null : customizable);
        boolean active = rs.getBoolean("productActive");
        d.setProductActive(rs.wasNull() ? null : active);
        return d;
    }

    private static String timestamp(Timestamp value) {
        return value == null ? null : value.toLocalDateTime().toString();
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
