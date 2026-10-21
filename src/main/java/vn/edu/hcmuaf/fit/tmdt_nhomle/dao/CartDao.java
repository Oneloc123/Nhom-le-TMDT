package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.CartItem;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Product;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.ShopOffer;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CartDao {

    private static final String OFFER_COLUMNS =
            "sp.shop_product_id, sp.shop_id, s.shop_name, sp.product_id, sp.sku,"
                    + " sp.printing_method, sp.material, sp.unit_price, sp.minimum_quantity,"
                    + " sp.production_days, sp.stock_status, sp.is_active";

    private CartDao() {
    }

    public static long getOrCreateCartId(Connection conn, long userId) throws SQLException {
        String select = "SELECT cart_id FROM carts WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(select)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        String insert = "INSERT INTO carts (user_id, status, currency, subtotal, created_at, updated_at)"
                + " VALUES (?, 'ACTIVE', 'VND', 0, NOW(), NOW())";
        try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, userId);
            try {
                ps.executeUpdate();
            } catch (SQLException duplicate) {
                // User khác request vừa tạo — đọc lại (user_id UNIQUE).
                try (PreparedStatement again = conn.prepareStatement(select)) {
                    again.setLong(1, userId);
                    try (ResultSet rs = again.executeQuery()) {
                        if (rs.next()) {
                            return rs.getLong(1);
                        }
                    }
                }
                throw duplicate;
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Không tạo được giỏ hàng cho người dùng " + userId);
    }

    public static Optional<ShopOffer> findOffer(Connection conn, long shopProductId) throws SQLException {
        String sql = "SELECT " + OFFER_COLUMNS + " FROM shop_products sp"
                + " JOIN print_shops s ON s.shop_id = sp.shop_id"
                + " JOIN products p ON p.product_id = sp.product_id"
                + " WHERE sp.shop_product_id = ?"
                + " AND sp.is_active = 1 AND p.is_active = 1 AND p.moderation_status = 'APPROVED'"
                + " AND (s.status IS NULL OR s.status NOT IN ('INACTIVE','BLOCKED'))";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, shopProductId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapOffer(rs)) : Optional.empty();
            }
        }
    }

    public static List<CartItem> listItems(Connection conn, long cartId) throws SQLException {
        String sql = "SELECT ci.cart_item_id, ci.shop_product_id, ci.quantity,"
                + " ci.unit_price_snapshot, ci.design_file_id,"
                + " " + OFFER_COLUMNS + ", p.product_name, p.slug"
                + " FROM cart_items ci"
                + " JOIN shop_products sp ON sp.shop_product_id = ci.shop_product_id"
                + " JOIN print_shops s ON s.shop_id = sp.shop_id"
                + " JOIN products p ON p.product_id = sp.product_id"
                + " WHERE ci.cart_id = ?"
                + " ORDER BY ci.cart_item_id";
        List<CartItem> items = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, cartId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapItem(rs));
                }
            }
        }
        return items;
    }

    public static void upsertItem(Connection conn, long cartId, long shopProductId,
                                  int quantity, Long designFileId) throws SQLException {
        String select = "SELECT cart_item_id, quantity FROM cart_items"
                + " WHERE cart_id = ? AND shop_product_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(select)) {
            ps.setLong(1, cartId);
            ps.setLong(2, shopProductId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long cartItemId = rs.getLong(1);
                    int newQuantity = rs.getInt(2) + quantity;
                    updateQuantity(conn, cartId, shopProductId, newQuantity);
                    return;
                }
            }
        }
        String insert = "INSERT INTO cart_items (cart_id, shop_product_id, design_file_id, quantity,"
                + " selected_options, unit_price_snapshot, line_total, created_at)"
                + " VALUES (?, ?, ?, ?, NULL,"
                + " (SELECT unit_price FROM shop_products WHERE shop_product_id = ?),"
                + " ? * (SELECT unit_price FROM shop_products WHERE shop_product_id = ?), NOW())";
        try (PreparedStatement ps = conn.prepareStatement(insert)) {
            ps.setLong(1, cartId);
            ps.setLong(2, shopProductId);
            if (designFileId == null) {
                ps.setNull(3, java.sql.Types.BIGINT);
            } else {
                ps.setLong(3, designFileId);
            }
            ps.setInt(4, quantity);
            ps.setLong(5, shopProductId);
            ps.setInt(6, quantity);
            ps.setLong(7, shopProductId);
            ps.executeUpdate();
        }
    }

    public static boolean updateQuantity(Connection conn, long cartId, long shopProductId,
                                         int quantity) throws SQLException {
        if (quantity <= 0) {
            return deleteItem(conn, cartId, shopProductId);
        }
        String sql = "UPDATE cart_items SET quantity = ?,"
                + " unit_price_snapshot = (SELECT unit_price FROM shop_products"
                + "   WHERE shop_product_id = ?),"
                + " line_total = ? * (SELECT unit_price FROM shop_products WHERE shop_product_id = ?)"
                + " WHERE cart_id = ? AND shop_product_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setLong(2, shopProductId);
            ps.setInt(3, quantity);
            ps.setLong(4, shopProductId);
            ps.setLong(5, cartId);
            ps.setLong(6, shopProductId);
            return ps.executeUpdate() > 0;
        }
    }

    public static boolean deleteItem(Connection conn, long cartId, long shopProductId) throws SQLException {
        String sql = "DELETE FROM cart_items WHERE cart_id = ? AND shop_product_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, cartId);
            ps.setLong(2, shopProductId);
            return ps.executeUpdate() > 0;
        }
    }

    public static void clearItems(Connection conn, long cartId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM cart_items WHERE cart_id = ?")) {
            ps.setLong(1, cartId);
            ps.executeUpdate();
        }
    }

    public static void refreshCartTotals(Connection conn, long cartId) throws SQLException {
        String sql = "UPDATE carts c SET subtotal = ("
                + " SELECT COALESCE(SUM(ci.line_total), 0) FROM cart_items ci WHERE ci.cart_id = c.cart_id"
                + "), updated_at = NOW() WHERE c.cart_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, cartId);
            ps.executeUpdate();
        }
    }

    // ---------------------------------------------------------------- internal

    private static CartItem mapItem(ResultSet rs) throws SQLException {
        CartItem item = new CartItem();
        item.setCartItemId(rs.getLong("cart_item_id"));
        item.setShopProductId(rs.getLong("shop_product_id"));
        item.setQuantity(rs.getInt("quantity"));
        BigDecimal snapshot = rs.getBigDecimal("unit_price_snapshot");
        item.setUnitPrice(snapshot != null ? snapshot : rs.getBigDecimal("unit_price"));
        long designFileId = rs.getLong("design_file_id");
        item.setDesignFileId(rs.wasNull() ? null : designFileId);

        ShopOffer offer = mapOffer(rs);
        item.setShopName(offer.getShopName());
        item.setMaterial(offer.getMaterial());
        item.setMinimumQuantity(offer.getMinimumQuantity());

        Product product = new Product();
        product.setId(offer.getProductId());
        product.setName(rs.getString("product_name"));
        product.setSlug(rs.getString("slug"));
        product.getOffers().add(offer);
        item.setProduct(product);
        return item;
    }

    private static ShopOffer mapOffer(ResultSet rs) throws SQLException {
        ShopOffer o = new ShopOffer();
        o.setShopProductId(rs.getLong("shop_product_id"));
        o.setShopId(rs.getLong("shop_id"));
        o.setShopName(rs.getString("shop_name"));
        o.setProductId(rs.getLong("product_id"));
        o.setSku(rs.getString("sku"));
        o.setPrintingMethod(rs.getString("printing_method"));
        o.setMaterial(rs.getString("material"));
        o.setUnitPrice(rs.getBigDecimal("unit_price"));
        o.setMinimumQuantity(rs.getInt("minimum_quantity"));
        o.setProductionDays(rs.getInt("production_days"));
        o.setStockStatus(rs.getString("stock_status"));
        o.setActive(rs.getBoolean("is_active"));
        return o;
    }
}
