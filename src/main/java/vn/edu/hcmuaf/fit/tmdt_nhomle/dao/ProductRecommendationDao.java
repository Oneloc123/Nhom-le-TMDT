package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.OrderStatus;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Product;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.ShopOffer;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Truy vấn dữ liệu phục vụ gợi ý sản phẩm:
 * - Tập sản phẩm ứng viên đã duyệt, đang bán và có chào bán hợp lệ từ xưởng hoạt động.
 * - Tín hiệu hành vi & nội dung từ product_view_events, favorites, carts/cart_items, orders/order_items và reviews.
 */
public final class ProductRecommendationDao {

    private static final int MAX_CANDIDATES = 120;
    private static final int VIEW_DEDUP_MINUTES = 10;

    private ProductRecommendationDao() {
    }

    /** Sản phẩm ứng viên kèm số lượng bán ròng (đã loại đơn hủy/chưa thanh toán và trừ hàng hoàn). */
    public record CandidateProduct(Product product, long soldQuantity) {
    }

    /** Một tín hiệu hành vi của khách hàng gắn với sản phẩm và thông số chào bán liên quan. */
    public record UserSignal(
            long productId,
            long categoryId,
            String productName,
            String searchKeywords,
            String description,
            String material,
            String printingMethod,
            BigDecimal unitPrice,
            String source,
            int rating,
            int quantity,
            double hoursAgo,
            boolean returned) {

        /** Constructor tiện lợi cho tín hiệu không gắn thời gian xem/hoàn trả. */
        public UserSignal(
                long productId,
                long categoryId,
                String productName,
                String searchKeywords,
                String description,
                String material,
                String printingMethod,
                BigDecimal unitPrice,
                String source,
                int rating,
                int quantity) {
            this(productId, categoryId, productName, searchKeywords, description,
                    material, printingMethod, unitPrice, source, rating, quantity, 0.0, false);
        }
    }

    /** Ghi nhận lượt xem sản phẩm theo người dùng hoặc session_key (chống trùng lặp trong 10 phút). */
    public static void recordProductView(Long userId, String sessionKey, long productId) {
        if (productId <= 0 || ((userId == null || userId <= 0) && (sessionKey == null || sessionKey.isBlank()))) {
            return;
        }
        String safeSession = sessionKey == null || sessionKey.isBlank()
                ? null
                : sessionKey.trim().substring(0, Math.min(128, sessionKey.trim().length()));
        try (Connection conn = Database.getConnection()) {
            Long recentViewId = findRecentViewId(conn, userId, safeSession, productId);
            if (recentViewId != null) {
                String updateSql = "UPDATE product_view_events SET viewed_at = NOW(),"
                        + " user_id = COALESCE(user_id, ?) WHERE view_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    if (userId != null && userId > 0) {
                        ps.setLong(1, userId);
                    } else {
                        ps.setNull(1, Types.BIGINT);
                    }
                    ps.setLong(2, recentViewId);
                    ps.executeUpdate();
                }
                return;
            }
            String insertSql = "INSERT INTO product_view_events (user_id, session_key, product_id, viewed_at)"
                    + " VALUES (?, ?, ?, NOW())";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                if (userId != null && userId > 0) {
                    ps.setLong(1, userId);
                } else {
                    ps.setNull(1, Types.BIGINT);
                }
                if (safeSession != null) {
                    ps.setString(2, safeSession);
                } else {
                    ps.setNull(2, Types.VARCHAR);
                }
                ps.setLong(3, productId);
                ps.executeUpdate();
            }
        } catch (SQLException ignored) {
            // Không làm gián đoạn trang chi tiết nếu bảng lịch sử xem chưa được tạo.
        }
    }

    private static Long findRecentViewId(Connection conn, Long userId, String sessionKey, long productId)
            throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT view_id FROM product_view_events WHERE product_id = ?"
                        + " AND viewed_at >= DATE_SUB(NOW(), INTERVAL ? MINUTE) AND (");
        boolean hasUser = userId != null && userId > 0;
        boolean hasSession = sessionKey != null;
        if (hasUser && hasSession) {
            sql.append("user_id = ? OR session_key = ?");
        } else if (hasUser) {
            sql.append("user_id = ?");
        } else {
            sql.append("session_key = ?");
        }
        sql.append(") ORDER BY viewed_at DESC LIMIT 1");
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int i = 1;
            ps.setLong(i++, productId);
            ps.setInt(i++, VIEW_DEDUP_MINUTES);
            if (hasUser) {
                ps.setLong(i++, userId);
            }
            if (hasSession) {
                ps.setString(i, sessionKey);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        }
    }

    /** Nạp các sản phẩm đã duyệt, đang hoạt động và có ít nhất một offer hợp lệ còn hàng. */
    public static List<CandidateProduct> loadApprovedCandidates() {
        String sql = "SELECT p.product_id, p.category_id, c.category_name,"
                + " p.product_name, p.slug, p.description, p.brand, p.base_unit,"
                + " p.is_customizable, p.is_active, p.search_keywords, p.view_count,"
                + " (SELECT i.image_url FROM product_images i"
                + "  WHERE i.product_id = p.product_id"
                + "  AND i.image_url IS NOT NULL AND TRIM(i.image_url) <> ''"
                + "  ORDER BY i.is_primary DESC, i.display_order, i.image_id LIMIT 1) AS image_url,"
                + " COALESCE(rv.rating_avg, 0) AS rating_avg,"
                + " COALESCE(rv.rating_count, 0) AS review_count,"
                + " COALESCE(ord.sold_qty, 0) AS sold_qty,"
                + " MIN(sp.unit_price) AS min_price"
                + " FROM products p"
                + " LEFT JOIN categories c ON c.category_id = p.category_id"
                + " JOIN shop_products sp ON sp.product_id = p.product_id"
                + "  AND sp.is_active = 1"
                + "  AND (sp.stock_status IS NULL OR UPPER(sp.stock_status) = 'IN_STOCK')"
                + " JOIN print_shops s ON s.shop_id = sp.shop_id"
                + "  AND (s.verification_status = 'VERIFIED' OR s.verification_status IS NULL)"
                + "  AND (s.status IS NULL OR s.status NOT IN ('INACTIVE','BLOCKED'))"
                + " LEFT JOIN (SELECT product_id, AVG(rating) AS rating_avg, COUNT(*) AS rating_count"
                + "  FROM reviews WHERE is_published = 1 GROUP BY product_id) rv ON rv.product_id = p.product_id"
                + " LEFT JOIN ("
                + "  SELECT oi.product_id,"
                + "   SUM(GREATEST(0, oi.quantity - COALESCE(("
                + "     SELECT SUM(rrr.requested_quantity) FROM return_refund_requests rrr"
                + "     WHERE rrr.order_item_id = oi.order_item_id"
                + "     AND rrr.status IN ('AWAITING_RETURN_SHIPMENT','RETURN_SHIPPED','RETURN_RECEIVED','REFUND_PROCESSING','REFUNDED')"
                + "   ), 0))) AS sold_qty"
                + "  FROM order_items oi JOIN orders o ON o.order_id = oi.order_id"
                + "  WHERE o.status IS NOT NULL AND o.status NOT IN (?, ?)"
                + "  GROUP BY oi.product_id"
                + " ) ord ON ord.product_id = p.product_id"
                + " WHERE p.is_active = 1 AND p.moderation_status = 'APPROVED'"
                + " GROUP BY p.product_id, c.category_name, rv.rating_avg, rv.rating_count, ord.sold_qty"
                + " HAVING min_price IS NOT NULL"
                + " ORDER BY ord.sold_qty DESC, p.view_count DESC, p.product_id DESC"
                + " LIMIT ?";

        List<CandidateProduct> candidates = new ArrayList<>();
        Map<Long, Product> byId = new LinkedHashMap<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, OrderStatus.CANCELLED);
            ps.setString(2, OrderStatus.PENDING_PAYMENT);
            ps.setInt(3, MAX_CANDIDATES);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Product product = mapCandidateProduct(rs);
                    candidates.add(new CandidateProduct(product, rs.getLong("sold_qty")));
                    byId.put(product.getId(), product);
                }
            }
            loadValidOffers(conn, byId);
            return candidates;
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    /** Nạp tín hiệu sở thích theo userId (nếu đã đăng nhập) và sessionKey (lịch sử xem). */
    public static List<UserSignal> loadUserSignals(long userId) {
        return loadUserSignals(userId > 0 ? userId : null, null);
    }

    /** Nạp tín hiệu sở thích của người dùng hoặc phiên truy cập khách. */
    public static List<UserSignal> loadUserSignals(Long userId, String sessionKey) {
        boolean hasUser = userId != null && userId > 0;
        String safeSession = sessionKey == null || sessionKey.isBlank() ? null : sessionKey.trim();
        if (!hasUser && safeSession == null) {
            return List.of();
        }
        List<UserSignal> signals = new ArrayList<>();
        try (Connection conn = Database.getConnection()) {
            loadViewSignals(conn, hasUser ? userId : null, safeSession, signals);
            if (hasUser) {
                loadFavoriteSignals(conn, userId, signals);
                loadCartSignals(conn, userId, signals);
                loadOrderAndReviewSignals(conn, userId, signals);
            }
            return signals;
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    private static void loadViewSignals(Connection conn, Long userId, String sessionKey, List<UserSignal> out) {
        if (userId == null && sessionKey == null) {
            return;
        }
        StringBuilder sql = new StringBuilder(
                "SELECT p.product_id, p.category_id, p.product_name, p.search_keywords, p.description,"
                        + " (SELECT sp.material FROM shop_products sp WHERE sp.product_id = p.product_id AND sp.is_active = 1 ORDER BY sp.unit_price LIMIT 1) AS material,"
                        + " (SELECT sp.printing_method FROM shop_products sp WHERE sp.product_id = p.product_id AND sp.is_active = 1 ORDER BY sp.unit_price LIMIT 1) AS printing_method,"
                        + " (SELECT MIN(sp.unit_price) FROM shop_products sp WHERE sp.product_id = p.product_id AND sp.is_active = 1) AS unit_price,"
                        + " GREATEST(0, TIMESTAMPDIFF(HOUR, pve.viewed_at, NOW())) AS hours_ago"
                        + " FROM product_view_events pve"
                        + " JOIN products p ON p.product_id = pve.product_id"
                        + " WHERE pve.viewed_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) AND (");
        if (userId != null && sessionKey != null) {
            sql.append("pve.user_id = ? OR pve.session_key = ?");
        } else if (userId != null) {
            sql.append("pve.user_id = ?");
        } else {
            sql.append("pve.session_key = ?");
        }
        sql.append(") ORDER BY pve.viewed_at DESC LIMIT 30");
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int i = 1;
            if (userId != null) {
                ps.setLong(i++, userId);
            }
            if (sessionKey != null) {
                ps.setString(i, sessionKey);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new UserSignal(
                            rs.getLong("product_id"),
                            rs.getLong("category_id"),
                            rs.getString("product_name"),
                            rs.getString("search_keywords"),
                            rs.getString("description"),
                            rs.getString("material"),
                            rs.getString("printing_method"),
                            rs.getBigDecimal("unit_price"),
                            "VIEW",
                            0,
                            1,
                            rs.getDouble("hours_ago"),
                            false));
                }
            }
        } catch (SQLException ignored) {
            // Không chặn gợi ý nếu bảng product_view_events chưa tồn tại.
        }
    }

    private static void loadFavoriteSignals(Connection conn, long userId, List<UserSignal> out)
            throws SQLException {
        String sql = "SELECT p.product_id, p.category_id, p.product_name, p.search_keywords, p.description,"
                + " (SELECT sp.material FROM shop_products sp WHERE sp.product_id = p.product_id AND sp.is_active = 1 ORDER BY sp.unit_price LIMIT 1) AS material,"
                + " (SELECT sp.printing_method FROM shop_products sp WHERE sp.product_id = p.product_id AND sp.is_active = 1 ORDER BY sp.unit_price LIMIT 1) AS printing_method,"
                + " (SELECT MIN(sp.unit_price) FROM shop_products sp WHERE sp.product_id = p.product_id AND sp.is_active = 1) AS unit_price"
                + " FROM favorites f"
                + " JOIN products p ON p.product_id = f.product_id"
                + " WHERE f.user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(mapSignal(rs, "FAVORITE", 0, 1, false));
                }
            }
        }
    }

    private static void loadCartSignals(Connection conn, long userId, List<UserSignal> out)
            throws SQLException {
        String sql = "SELECT p.product_id, p.category_id, p.product_name, p.search_keywords, p.description,"
                + " sp.material, sp.printing_method, sp.unit_price, ci.quantity"
                + " FROM carts c"
                + " JOIN cart_items ci ON ci.cart_id = c.cart_id"
                + " JOIN shop_products sp ON sp.shop_product_id = ci.shop_product_id"
                + " JOIN products p ON p.product_id = sp.product_id"
                + " WHERE c.user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(mapSignal(rs, "CART", 0, Math.max(1, rs.getInt("quantity")), false));
                }
            }
        }
    }

    private static void loadOrderAndReviewSignals(Connection conn, long userId, List<UserSignal> out)
            throws SQLException {
        String sql = "SELECT p.product_id, p.category_id, p.product_name, p.search_keywords, p.description,"
                + " COALESCE(sp.material, '') AS material,"
                + " COALESCE(sp.printing_method, '') AS printing_method,"
                + " COALESCE(sp.unit_price, oi.unit_price) AS unit_price,"
                + " oi.quantity, COALESCE(rv.rating, 0) AS rating,"
                + " EXISTS (SELECT 1 FROM return_refund_requests rrr"
                + "   WHERE rrr.order_item_id = oi.order_item_id"
                + "   AND rrr.status IN ('AWAITING_RETURN_SHIPMENT','RETURN_SHIPPED','RETURN_RECEIVED','REFUND_PROCESSING','REFUNDED')) AS is_returned"
                + " FROM orders o"
                + " JOIN order_items oi ON oi.order_id = o.order_id"
                + " JOIN products p ON p.product_id = oi.product_id"
                + " LEFT JOIN shop_products sp ON sp.shop_product_id = oi.shop_product_id"
                + " LEFT JOIN reviews rv ON rv.order_item_id = oi.order_item_id AND rv.user_id = o.user_id"
                + " WHERE o.user_id = ? AND o.status IS NOT NULL AND o.status NOT IN (?, ?)"
                + " ORDER BY o.placed_at DESC, oi.order_item_id DESC LIMIT 60";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, OrderStatus.CANCELLED);
            ps.setString(3, OrderStatus.PENDING_PAYMENT);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(mapSignal(
                            rs,
                            "ORDER",
                            rs.getInt("rating"),
                            Math.max(1, rs.getInt("quantity")),
                            rs.getBoolean("is_returned")));
                }
            }
        }
    }

    private static void loadValidOffers(Connection conn, Map<Long, Product> byId) throws SQLException {
        if (byId.isEmpty()) {
            return;
        }
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < byId.size(); i++) {
            placeholders.append(i == 0 ? "?" : ",?");
        }
        String sql = "SELECT sp.shop_product_id, sp.shop_id, s.shop_name, sp.product_id, sp.sku,"
                + " sp.printing_method, sp.material, sp.unit_price, sp.minimum_quantity,"
                + " sp.production_days, sp.stock_status, sp.is_active"
                + " FROM shop_products sp"
                + " JOIN print_shops s ON s.shop_id = sp.shop_id"
                + " WHERE sp.product_id IN (" + placeholders + ") AND sp.is_active = 1"
                + " AND (sp.stock_status IS NULL OR UPPER(sp.stock_status) = 'IN_STOCK')"
                + " AND (s.verification_status = 'VERIFIED' OR s.verification_status IS NULL)"
                + " AND (s.status IS NULL OR s.status NOT IN ('INACTIVE','BLOCKED'))"
                + " ORDER BY sp.unit_price ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            for (Long productId : byId.keySet()) {
                ps.setLong(idx++, productId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ShopOffer offer = new ShopOffer();
                    offer.setShopProductId(rs.getLong("shop_product_id"));
                    offer.setShopId(rs.getLong("shop_id"));
                    offer.setShopName(rs.getString("shop_name"));
                    offer.setProductId(rs.getLong("product_id"));
                    offer.setSku(rs.getString("sku"));
                    offer.setPrintingMethod(rs.getString("printing_method"));
                    offer.setMaterial(rs.getString("material"));
                    offer.setUnitPrice(rs.getBigDecimal("unit_price"));
                    offer.setMinimumQuantity(rs.getInt("minimum_quantity"));
                    offer.setProductionDays(rs.getInt("production_days"));
                    offer.setStockStatus(rs.getString("stock_status"));
                    offer.setActive(rs.getBoolean("is_active"));
                    Product owner = byId.get(offer.getProductId());
                    if (owner != null) {
                        owner.getOffers().add(offer);
                    }
                }
            }
        }
    }

    private static Product mapCandidateProduct(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getLong("product_id"));
        p.setCategoryId(rs.getLong("category_id"));
        p.setCategoryName(rs.getString("category_name"));
        p.setName(rs.getString("product_name"));
        p.setSlug(rs.getString("slug"));
        p.setDescription(rs.getString("description"));
        p.setBrand(rs.getString("brand"));
        p.setBaseUnit(rs.getString("base_unit"));
        p.setCustomizable(rs.getBoolean("is_customizable"));
        p.setActive(rs.getBoolean("is_active"));
        p.setSearchKeywords(rs.getString("search_keywords"));
        p.setViewCount(rs.getInt("view_count"));
        p.setImageUrl(rs.getString("image_url"));
        p.setRating(Math.round(rs.getDouble("rating_avg") * 10) / 10.0);
        p.setReviewCount(rs.getInt("review_count"));
        return p;
    }

    private static UserSignal mapSignal(
            ResultSet rs, String source, int rating, int quantity, boolean returned) throws SQLException {
        return new UserSignal(
                rs.getLong("product_id"),
                rs.getLong("category_id"),
                rs.getString("product_name"),
                rs.getString("search_keywords"),
                rs.getString("description"),
                rs.getString("material"),
                rs.getString("printing_method"),
                rs.getBigDecimal("unit_price"),
                source,
                rating,
                quantity,
                0.0,
                returned);
    }
}
