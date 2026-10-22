package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.ProductComment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProductCommentDao {

    private static final int MAX_CONTENT_LENGTH = 2000;
    private static volatile boolean tableInitialized = false;

    private ProductCommentDao() {
    }

    private static void ensureTable(Connection conn) {
        if (tableInitialized) {
            return;
        }
        String sql = "CREATE TABLE IF NOT EXISTS product_comments ("
                + " comment_id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + " product_id BIGINT NOT NULL,"
                + " user_id BIGINT NOT NULL,"
                + " parent_id BIGINT NULL,"
                + " content TEXT NOT NULL,"
                + " is_seller_reply BOOLEAN DEFAULT FALSE,"
                + " created_at DATETIME DEFAULT CURRENT_TIMESTAMP"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
            tableInitialized = true;
        } catch (SQLException ignored) {
            // bảng có thể đã tồn tại
            tableInitialized = true;
        }
    }

    public static List<ProductComment> listForProduct(long productId) {
        String sql = "SELECT c.comment_id, c.product_id, c.user_id, c.parent_id, c.content, c.is_seller_reply,"
                + " DATE_FORMAT(c.created_at, '%d/%m/%Y %H:%i') AS created_at_text,"
                + " COALESCE(NULLIF(TRIM(u.full_name), ''), u.email, 'Khách hàng') AS display_name,"
                + " COALESCE(u.email, '') AS user_email,"
                + " COALESCE(u.role, 'CUSTOMER') AS user_role"
                + " FROM product_comments c"
                + " JOIN users u ON u.user_id = c.user_id"
                + " WHERE c.product_id = ?"
                + " ORDER BY c.created_at ASC, c.comment_id ASC";

        Map<Long, ProductComment> rootsMap = new LinkedHashMap<>();
        List<ProductComment> allComments = new ArrayList<>();

        try (Connection conn = Database.getConnection()) {
            ensureTable(conn);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, productId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        allComments.add(map(rs));
                    }
                }
            }

            // Phân loại câu hỏi gốc và câu trả lời
            for (ProductComment comment : allComments) {
                if (comment.getParentId() == null || comment.getParentId() <= 0) {
                    rootsMap.put(comment.getCommentId(), comment);
                }
            }

            for (ProductComment comment : allComments) {
                if (comment.getParentId() != null && comment.getParentId() > 0) {
                    ProductComment parent = rootsMap.get(comment.getParentId());
                    if (parent != null) {
                        parent.addReply(comment);
                    }
                }
            }

            // Câu hỏi mới nhất đưa lên đầu
            List<ProductComment> roots = new ArrayList<>(rootsMap.values());
            Collections.reverse(roots);
            return roots;
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static int countForProduct(long productId) {
        String sql = "SELECT COUNT(*) FROM product_comments WHERE product_id = ?";
        try (Connection conn = Database.getConnection()) {
            ensureTable(conn);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, productId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static ProductComment create(long productId, long userId, Long parentId, String content, boolean isSellerReply) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.length() < 2) {
            throw new IllegalArgumentException("Nội dung câu hỏi/bình luận cần tối thiểu 2 ký tự.");
        }
        if (trimmed.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("Nội dung tối đa " + MAX_CONTENT_LENGTH + " ký tự.");
        }

        String sql = "INSERT INTO product_comments (product_id, user_id, parent_id, content, is_seller_reply, created_at)"
                + " VALUES (?, ?, ?, ?, ?, NOW())";

        try (Connection conn = Database.getConnection()) {
            ensureTable(conn);
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, productId);
                ps.setLong(2, userId);
                if (parentId == null || parentId <= 0) {
                    ps.setNull(3, java.sql.Types.BIGINT);
                } else {
                    ps.setLong(3, parentId);
                }
                ps.setString(4, trimmed);
                ps.setBoolean(5, isSellerReply);
                ps.executeUpdate();

                long generatedId = -1;
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        generatedId = keys.getLong(1);
                    }
                }

                ProductComment comment = new ProductComment();
                comment.setCommentId(generatedId);
                comment.setProductId(productId);
                comment.setUserId(userId);
                comment.setParentId(parentId);
                comment.setContent(trimmed);
                comment.setSellerReply(isSellerReply);
                return comment;
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static boolean isSellerForProduct(long productId, long userId) {
        String sql = "SELECT COUNT(*) FROM shop_products sp"
                + " JOIN print_shops s ON s.shop_id = sp.shop_id"
                + " WHERE sp.product_id = ? AND s.owner_user_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, productId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public static boolean delete(long commentId, long userId, boolean isAdmin) {
        String deleteRepliesSql = "DELETE FROM product_comments WHERE parent_id = ?";
        String deleteCommentSql = isAdmin
                ? "DELETE FROM product_comments WHERE comment_id = ?"
                : "DELETE FROM product_comments WHERE comment_id = ? AND user_id = ?";

        try (Connection conn = Database.getConnection()) {
            ensureTable(conn);
            // Xóa các câu trả lời con trước
            try (PreparedStatement ps = conn.prepareStatement(deleteRepliesSql)) {
                ps.setLong(1, commentId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(deleteCommentSql)) {
                ps.setLong(1, commentId);
                if (!isAdmin) {
                    ps.setLong(2, userId);
                }
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    private static ProductComment map(ResultSet rs) throws SQLException {
        ProductComment comment = new ProductComment();
        comment.setCommentId(rs.getLong("comment_id"));
        comment.setProductId(rs.getLong("product_id"));
        comment.setUserId(rs.getLong("user_id"));
        long parentId = rs.getLong("parent_id");
        comment.setParentId(rs.wasNull() ? null : parentId);
        comment.setContent(rs.getString("content"));
        comment.setSellerReply(rs.getBoolean("is_seller_reply"));
        comment.setCreatedAtText(rs.getString("created_at_text"));
        comment.setUserDisplayName(rs.getString("display_name"));
        comment.setUserEmail(rs.getString("user_email"));
        comment.setUserRole(rs.getString("user_role"));
        return comment;
    }
}
