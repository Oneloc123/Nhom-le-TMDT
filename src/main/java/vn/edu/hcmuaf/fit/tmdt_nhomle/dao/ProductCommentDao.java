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
            try {
                st.execute("ALTER TABLE product_comments ADD COLUMN is_seller_reply BOOLEAN DEFAULT FALSE");
            } catch (SQLException ignored) {
                // column already exists
            }
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
                + " COALESCE(u.role, 'CUSTOMER') AS user_role,"
                + " (SELECT s.shop_name FROM print_shops s WHERE s.owner_user_id = c.user_id LIMIT 1) AS shop_name"
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
        try {
            comment.setShopName(rs.getString("shop_name"));
        } catch (SQLException ignored) {
            // Cột shop_name có thể không có ở một số câu truy vấn khác
        }
        return comment;
    }

    public static int countUnansweredForWorkshop(long ownerUserId, long shopId) {
        String sql = "SELECT COUNT(DISTINCT c.comment_id) FROM product_comments c"
                + " JOIN shop_products sp ON sp.product_id = c.product_id"
                + " JOIN print_shops ps ON ps.shop_id = sp.shop_id"
                + " WHERE sp.shop_id = ? AND ps.owner_user_id = ?"
                + " AND (c.parent_id IS NULL OR c.parent_id = 0)"
                + " AND NOT EXISTS ("
                + "   SELECT 1 FROM product_comments r"
                + "   WHERE r.parent_id = c.comment_id AND r.is_seller_reply = TRUE"
                + " )";
        try (Connection conn = Database.getConnection()) {
            ensureTable(conn);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, shopId);
                ps.setLong(2, ownerUserId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }
        } catch (SQLException e) {
            return 0;
        }
    }

    public static List<Map<String, Object>> listForWorkshop(long ownerUserId, long shopId, String status, String q) {
        try (Connection conn = Database.getConnection()) {
            ensureTable(conn);

            // 1. Kiểm tra xưởng thuộc ownerUserId
            String checkShopSql = "SELECT shop_name FROM print_shops WHERE shop_id = ? AND owner_user_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(checkShopSql)) {
                ps.setLong(1, shopId);
                ps.setLong(2, ownerUserId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new WorkshopDao.NotFoundException("Không tìm thấy xưởng thuộc tài khoản này.");
                    }
                }
            }

            // 2. Tìm các câu hỏi gốc của các sản phẩm thuộc xưởng
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT DISTINCT c.comment_id, c.product_id, c.user_id, c.content, c.created_at,")
              .append(" DATE_FORMAT(c.created_at, '%d/%m/%Y %H:%i') AS created_at_text,")
              .append(" COALESCE(NULLIF(TRIM(u.full_name), ''), u.email, 'Khách hàng') AS customer_name,")
              .append(" COALESCE(u.email, '') AS customer_email,")
              .append(" p.product_name,")
              .append(" (SELECT pi.image_url FROM product_images pi WHERE pi.product_id = c.product_id ORDER BY pi.is_primary DESC, pi.display_order ASC, pi.image_id ASC LIMIT 1) AS image_url,")
              .append(" EXISTS (SELECT 1 FROM product_comments r WHERE r.parent_id = c.comment_id AND r.is_seller_reply = TRUE) AS is_answered")
              .append(" FROM product_comments c")
              .append(" JOIN shop_products sp ON sp.product_id = c.product_id")
              .append(" JOIN products p ON p.product_id = c.product_id")
              .append(" LEFT JOIN users u ON u.user_id = c.user_id")
              .append(" WHERE sp.shop_id = ? AND (c.parent_id IS NULL OR c.parent_id = 0)");

            List<Object> params = new ArrayList<>();
            params.add(shopId);

            String normStatus = status == null ? "" : status.trim().toUpperCase(java.util.Locale.ROOT);
            if ("UNANSWERED".equals(normStatus)) {
                sb.append(" AND NOT EXISTS (SELECT 1 FROM product_comments r WHERE r.parent_id = c.comment_id AND r.is_seller_reply = TRUE)");
            } else if ("ANSWERED".equals(normStatus)) {
                sb.append(" AND EXISTS (SELECT 1 FROM product_comments r WHERE r.parent_id = c.comment_id AND r.is_seller_reply = TRUE)");
            }

            if (q != null && !q.trim().isEmpty()) {
                String keyword = "%" + q.trim() + "%";
                sb.append(" AND (p.product_name LIKE ? OR c.content LIKE ? OR u.full_name LIKE ? OR u.email LIKE ?)");
                params.add(keyword);
                params.add(keyword);
                params.add(keyword);
                params.add(keyword);
            }

            sb.append(" ORDER BY c.created_at DESC, c.comment_id DESC LIMIT 100");

            List<Map<String, Object>> questions = new ArrayList<>();
            List<Long> questionIds = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(sb.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        long commentId = rs.getLong("comment_id");
                        questionIds.add(commentId);
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("id", commentId);
                        item.put("productId", rs.getLong("product_id"));
                        item.put("productName", rs.getString("product_name"));
                        item.put("productImage", rs.getString("image_url"));
                        item.put("customerName", rs.getString("customer_name"));
                        item.put("customerEmail", rs.getString("customer_email"));
                        item.put("content", rs.getString("content"));
                        item.put("createdAt", rs.getString("created_at_text"));
                        boolean answered = rs.getBoolean("is_answered");
                        item.put("answered", answered);
                        item.put("status", answered ? "ANSWERED" : "UNANSWERED");
                        item.put("replies", new ArrayList<Map<String, Object>>());
                        questions.add(item);
                    }
                }
            }

            // 3. Tải danh sách phản hồi cho các câu hỏi
            if (!questionIds.isEmpty()) {
                String repliesSql = "SELECT r.comment_id, r.product_id, r.user_id, r.parent_id, r.content, r.is_seller_reply,"
                        + " DATE_FORMAT(r.created_at, '%d/%m/%Y %H:%i') AS created_at_text,"
                        + " COALESCE(NULLIF(TRIM(ru.full_name), ''), ru.email, 'Người dùng') AS author_name,"
                        + " (SELECT s.shop_name FROM print_shops s WHERE s.owner_user_id = r.user_id LIMIT 1) AS shop_name"
                        + " FROM product_comments r"
                        + " LEFT JOIN users ru ON ru.user_id = r.user_id"
                        + " WHERE r.parent_id IN (" + String.join(",", Collections.nCopies(questionIds.size(), "?")) + ")"
                        + " ORDER BY r.created_at ASC, r.comment_id ASC";
                Map<Long, List<Map<String, Object>>> repliesByParent = new LinkedHashMap<>();
                try (PreparedStatement ps = conn.prepareStatement(repliesSql)) {
                    for (int i = 0; i < questionIds.size(); i++) {
                        ps.setLong(i + 1, questionIds.get(i));
                    }
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            long parentId = rs.getLong("parent_id");
                            Map<String, Object> reply = new LinkedHashMap<>();
                            reply.put("id", rs.getLong("comment_id"));
                            reply.put("parentId", parentId);
                            reply.put("authorName", rs.getString("author_name"));
                            reply.put("shopName", rs.getString("shop_name"));
                            reply.put("content", rs.getString("content"));
                            reply.put("isSellerReply", rs.getBoolean("is_seller_reply"));
                            reply.put("createdAt", rs.getString("created_at_text"));
                            repliesByParent.computeIfAbsent(parentId, k -> new ArrayList<>()).add(reply);
                        }
                    }
                }
                for (Map<String, Object> qItem : questions) {
                    long qId = (Long) qItem.get("id");
                    List<Map<String, Object>> rList = repliesByParent.get(qId);
                    if (rList != null) {
                        qItem.put("replies", rList);
                    }
                }
            }
            return questions;
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static Map<String, Object> replyByWorkshop(long ownerUserId, long shopId, long commentId, String content) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.length() < 2) {
            throw new IllegalArgumentException("Nội dung phản hồi cần tối thiểu 2 ký tự.");
        }
        if (trimmed.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("Nội dung tối đa " + MAX_CONTENT_LENGTH + " ký tự.");
        }

        try (Connection conn = Database.getConnection()) {
            ensureTable(conn);

            // 1. Kiểm tra xưởng thuộc ownerUserId
            String checkShopSql = "SELECT shop_name FROM print_shops WHERE shop_id = ? AND owner_user_id = ?";
            String shopName;
            try (PreparedStatement ps = conn.prepareStatement(checkShopSql)) {
                ps.setLong(1, shopId);
                ps.setLong(2, ownerUserId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new WorkshopDao.NotFoundException("Không tìm thấy xưởng thuộc tài khoản này.");
                    }
                    shopName = rs.getString("shop_name");
                }
            }

            // 2. Kiểm tra câu hỏi gốc thuộc sản phẩm của xưởng
            long productId;
            String checkCommentSql = "SELECT c.product_id FROM product_comments c"
                    + " JOIN shop_products sp ON sp.product_id = c.product_id"
                    + " WHERE c.comment_id = ? AND sp.shop_id = ? AND (c.parent_id IS NULL OR c.parent_id = 0)";
            try (PreparedStatement ps = conn.prepareStatement(checkCommentSql)) {
                ps.setLong(1, commentId);
                ps.setLong(2, shopId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new WorkshopDao.NotFoundException("Không tìm thấy câu hỏi thuộc sản phẩm của xưởng này.");
                    }
                    productId = rs.getLong("product_id");
                }
            }

            // 3. Thêm câu trả lời từ xưởng
            String insertSql = "INSERT INTO product_comments (product_id, user_id, parent_id, content, is_seller_reply, created_at)"
                    + " VALUES (?, ?, ?, ?, TRUE, NOW())";
            long replyId = -1;
            try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, productId);
                ps.setLong(2, ownerUserId);
                ps.setLong(3, commentId);
                ps.setString(4, trimmed);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        replyId = keys.getLong(1);
                    }
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("replyId", replyId);
            result.put("commentId", commentId);
            result.put("productId", productId);
            result.put("shopName", shopName);
            result.put("content", trimmed);
            result.put("isSellerReply", true);
            return result;
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }
}
