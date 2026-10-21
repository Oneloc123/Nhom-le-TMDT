package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Product;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.ShopOffer;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ProductDao {

    public static final int MAX_PAGE_SIZE = 48;
    public static final int DEFAULT_PAGE_SIZE = 8;

    private static final String SELECT_COLUMNS =
            "SELECT p.product_id, p.category_id, c.category_name, "
                    + "p.product_name, p.slug, p.description,"
                    + " p.brand, p.base_unit, p.is_customizable, p.is_active,"
                    + " (SELECT i.image_url FROM product_images i "
                    + " WHERE i.product_id = p.product_id"
                    + " ORDER BY i.is_primary DESC, i.display_order LIMIT 1) AS image_url,"
                    + " COALESCE(MAX(rv.rating_avg), 0) AS rating_avg,"
                    + " COALESCE(MAX(rv.rating_count), 0) AS review_count,"
                    + " MIN(sp.unit_price) AS min_price"
                    + " FROM products p"
                    + " LEFT JOIN categories c ON c.category_id = p.category_id"
                    + " LEFT JOIN shop_products sp ON sp.product_id = p.product_id"
                    + " AND sp.is_active = 1"
                    + " AND (sp.stock_status IS NULL OR sp.stock_status <> 'OUT_OF_STOCK')"
                    + " AND EXISTS (SELECT 1 FROM print_shops s2"
                    + " WHERE s2.shop_id = sp.shop_id"
                    + " AND s2.verification_status = 'VERIFIED'"
                    + " AND (s2.status IS NULL OR s2.status NOT IN ('INACTIVE','BLOCKED')))"
                    + " LEFT JOIN (SELECT product_id, AVG(rating) AS rating_avg,"
                    + " COUNT(*) AS rating_count FROM reviews"
                    + " WHERE is_published = 1 GROUP BY product_id) rv"
                    + " ON rv.product_id = p.product_id"
                    + " WHERE p.is_active = 1 AND p.moderation_status = 'APPROVED'";

    private ProductDao() {
    }

    public record SearchCriteria(String query, String category, BigDecimal minPrice,
                                 BigDecimal maxPrice, Long shopId, String sort) {
    }

    public static List<Product> search(SearchCriteria criteria, int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, pageSize), MAX_PAGE_SIZE);
        StringBuilder sql = new StringBuilder(SELECT_COLUMNS);
        appendFilters(sql, criteria);
        sql.append(" GROUP BY p.product_id, c.category_name");
        appendHaving(sql, criteria);
        sql.append(" ORDER BY ").append(orderBy(criteria.sort()))
                .append(" LIMIT ? OFFSET ?");
        List<Product> products = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = bindFilters(ps, criteria);
            ps.setInt(idx++, safeSize);
            ps.setInt(idx, (safePage - 1) * safeSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    products.add(mapSummary(rs));
                }
            }
            loadOffers(conn, products);
            return products;
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static long count(SearchCriteria criteria) {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM (SELECT p.product_id, MIN(sp.unit_price) AS min_price"
        ).append(
                SELECT_COLUMNS.substring(SELECT_COLUMNS.indexOf(" FROM products"))
        );

        appendFilters(sql, criteria);
        sql.append(" GROUP BY p.product_id, c.category_name");
        appendHaving(sql, criteria);
        sql.append(") t");

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            bindFilters(ps, criteria);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static Optional<Product> findById(long productId) {
        String sql = SELECT_COLUMNS + " AND p.product_id = ? GROUP BY p.product_id, c.category_name";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, productId);
            Product product = null;
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    product = mapSummary(rs);
                }
            }
            if (product != null) {
                loadOffers(conn, List.of(product));
            }
            return Optional.ofNullable(product);
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    /**
     * Các sản phẩm còn hiển thị và CHO PHÉP TÙY CHỈNH — dùng cho select
     * “Sản phẩm” khi lưu thiết kế custom (giới hạn độ dài danh sách).
     */
    public static List<Product> listCustomizableOptions(int limit) {
        String sql = "SELECT p.product_id, p.product_name FROM products p"
                + " WHERE p.is_active = 1 AND p.moderation_status = 'APPROVED'"
                + " AND p.is_customizable = 1"
                + " ORDER BY p.product_name LIMIT ?";
        List<Product> products = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Math.min(Math.max(1, limit), 200));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Product product = new Product();
                    product.setId(rs.getLong("product_id"));
                    product.setName(rs.getString("product_name"));
                    product.setCustomizable(true);
                    product.setActive(true);
                    products.add(product);
                }
            }
            return products;
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static Optional<ShopOffer> findOffer(long shopProductId) {
        String sql = "SELECT sp.shop_product_id, sp.shop_id, s.shop_name, sp.product_id, sp.sku,"
                + " sp.printing_method, sp.material, sp.unit_price, sp.minimum_quantity,"
                + " sp.production_days, sp.stock_status, sp.is_active"
                + " FROM shop_products sp JOIN print_shops s ON s.shop_id = sp.shop_id"
                + " JOIN products p ON p.product_id = sp.product_id"
                + " WHERE sp.shop_product_id = ? AND sp.is_active = 1"
                + " AND p.is_active = 1 AND p.moderation_status = 'APPROVED'"
                + " AND s.verification_status = 'VERIFIED'"
                + " AND (s.status IS NULL OR s.status NOT IN ('INACTIVE','BLOCKED'))";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, shopProductId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapOffer(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }
// sản phẩm bán chạy nhất dựa trên số lượng
    public static List<Product> getBestsellers(int limit) {
        String sql = SELECT_COLUMNS
                + " GROUP BY p.product_id, c.category_name"
                + " ORDER BY (SELECT COALESCE(SUM(oi.quantity), 0) FROM order_items oi WHERE oi.product_id = p.product_id) DESC, review_count DESC, p.product_id ASC"
                + " LIMIT ?";
        List<Product> products = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    products.add(mapSummary(rs));
                }
            }
            loadOffers(conn, products);
            return products;
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }
//  sản phẩm được xem nhiều nhất
public static List<Product> getTrending(int limit) {
    String sql = SELECT_COLUMNS
            + " GROUP BY p.product_id, c.category_name, p.view_count"
            + " ORDER BY p.view_count DESC, p.product_id ASC"
            + " LIMIT ?";
    List<Product> products = new ArrayList<>();
    try (Connection conn = Database.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, limit);
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                products.add(mapSummary(rs));
            }
        }
        loadOffers(conn, products);
        return products;
    } catch (SQLException e) {
        throw DataAccessException.of(e);
    }
}
    private static void appendFilters(StringBuilder sql, SearchCriteria c) {
        if (c.query() != null && !c.query().isBlank()) {
            sql.append(" AND (LOWER(p.product_name) LIKE ? OR LOWER(p.description) LIKE ?"
                    + " OR LOWER(p.search_keywords) LIKE ?"
                    + " OR EXISTS (SELECT 1 FROM shop_products sp3 JOIN print_shops s3"
                    + "   ON s3.shop_id = sp3.shop_id WHERE sp3.product_id = p.product_id"
                    + "   AND s3.verification_status = 'VERIFIED'"
                    + "   AND (s3.status IS NULL OR s3.status NOT IN ('INACTIVE','BLOCKED'))"
                    + "   AND LOWER(s3.shop_name) LIKE ?))");
        }
        if (c.category() != null && !c.category().isBlank()) {
            sql.append(" AND c.category_name = ?");
        }
        if (c.shopId() != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM shop_products sp4"
                    + " JOIN print_shops s4 ON s4.shop_id = sp4.shop_id"
                    + " WHERE sp4.product_id = p.product_id AND sp4.shop_id = ? AND sp4.is_active = 1"
                    + " AND s4.verification_status = 'VERIFIED'"
                    + " AND (s4.status IS NULL OR s4.status NOT IN ('INACTIVE','BLOCKED')))");
        }
    }

    private static void appendHaving(StringBuilder sql, SearchCriteria c) {
        if (c.minPrice() != null) {
            sql.append(" HAVING min_price >= ?");
            if (c.maxPrice() != null) {
                sql.append(" AND min_price <= ?");
            }
        } else if (c.maxPrice() != null) {
            sql.append(" HAVING min_price <= ?");
        }
    }

    private static int bindFilters(PreparedStatement ps, SearchCriteria c) throws SQLException {
        int i = 1;
        if (c.query() != null && !c.query().isBlank()) {
            String like = "%" + c.query().trim().toLowerCase() + "%";
            ps.setString(i++, like);
            ps.setString(i++, like);
            ps.setString(i++, like);
            ps.setString(i++, like);
        }
        if (c.category() != null && !c.category().isBlank()) {
            ps.setString(i++, c.category().trim());
        }
        if (c.shopId() != null) {
            ps.setLong(i++, c.shopId());
        }
        if (c.minPrice() != null) {
            ps.setBigDecimal(i++, c.minPrice());
        }
        if (c.maxPrice() != null) {
            ps.setBigDecimal(i++, c.maxPrice());
        }
        return i;
    }

    private static String orderBy(String sort) {
        return switch (sort == null ? "" : sort) {
            case "priceAsc" -> "min_price ASC, p.product_id ASC";
            case "priceDesc" -> "min_price DESC, p.product_id ASC";
            case "rating" -> "rating_avg DESC, review_count DESC, p.product_id ASC";
            default -> "review_count DESC, rating_avg DESC, p.product_id ASC";
        };
    }

    private static void loadOffers(Connection conn, List<Product> products) throws SQLException {
        if (products.isEmpty()) {
            return;
        }
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < products.size(); i++) {
            placeholders.append(i == 0 ? "?" : ",?");
        }
        String sql = "SELECT sp.shop_product_id, sp.shop_id, s.shop_name, sp.product_id, sp.sku,"
                + " sp.printing_method, sp.material, sp.unit_price, sp.minimum_quantity,"
                + " sp.production_days, sp.stock_status, sp.is_active"
                + " FROM shop_products sp JOIN print_shops s ON s.shop_id = sp.shop_id"
                + " JOIN products p ON p.product_id = sp.product_id"
                + " WHERE sp.product_id IN (" + placeholders + ") AND sp.is_active = 1"
                + " AND p.is_active = 1 AND p.moderation_status = 'APPROVED'"
                + " AND s.verification_status = 'VERIFIED'"
                + " AND (s.status IS NULL OR s.status NOT IN ('INACTIVE','BLOCKED'))"
                + " ORDER BY sp.unit_price";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < products.size(); i++) {
                ps.setLong(i + 1, products.get(i).getId());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ShopOffer offer = mapOffer(rs);
                    products.stream()
                            .filter(p -> p.getId() == offer.getProductId())
                            .findFirst()
                            .ifPresent(p -> p.getOffers().add(offer));
                }
            }
        }
    }

    private static Product mapSummary(ResultSet rs) throws SQLException {
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
        p.setImageUrl(rs.getString("image_url"));
        p.setRating(Math.round(rs.getDouble("rating_avg") * 10) / 10.0);
        p.setReviewCount(rs.getInt("review_count"));
        return p;
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
