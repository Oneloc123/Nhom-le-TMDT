package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Product;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.ProductImage;
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
                    + " AND (sp.stock_status IS NULL OR UPPER(sp.stock_status) = 'IN_STOCK')"
                    + " AND EXISTS (SELECT 1 FROM print_shops s2"
                    + " WHERE s2.shop_id = sp.shop_id"
                    + " AND (s2.verification_status = 'VERIFIED' OR s2.verification_status IS NULL)"
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
            loadOffers(conn, products, criteria.shopId());
            return products;
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static long count(SearchCriteria criteria) {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM (SELECT p.product_id,"
                        + " MIN(sp.unit_price) AS min_price"
        ).append(
                SELECT_COLUMNS.substring(SELECT_COLUMNS.indexOf(" FROM products p"))
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

    /** Sản phẩm bán chạy, xếp theo tổng số lượng đã xuất hiện trong đơn hàng. */
    public static List<Product> getBestsellers(int limit) {
        int safeLimit = Math.min(Math.max(1, limit), MAX_PAGE_SIZE);
        String sql = SELECT_COLUMNS
                + " GROUP BY p.product_id, c.category_name"
                + " HAVING min_price IS NOT NULL"
                + " ORDER BY (SELECT COALESCE(SUM(oi.quantity), 0)"
                + " FROM order_items oi WHERE oi.product_id = p.product_id) DESC,"
                + " review_count DESC, p.product_id ASC LIMIT ?";
        return listFeatured(sql, safeLimit);
    }

    /** Sản phẩm nổi bật, xếp theo điểm và số lượng đánh giá. */
    public static List<Product> getTrending(int limit) {
        int safeLimit = Math.min(Math.max(1, limit), MAX_PAGE_SIZE);
        String sql = SELECT_COLUMNS
                + " GROUP BY p.product_id, c.category_name"
                + " HAVING min_price IS NOT NULL"
                + " ORDER BY rating_avg DESC, review_count DESC, p.product_id DESC LIMIT ?";
        return listFeatured(sql, safeLimit);
    }

    /** Sản phẩm được xem nhiều nhất, xếp theo bộ đếm lượt mở trang chi tiết. */
    public static List<Product> getMostViewed(int limit) {
        int safeLimit = Math.min(Math.max(1, limit), MAX_PAGE_SIZE);
        String sql = SELECT_COLUMNS
                + " GROUP BY p.product_id, c.category_name, p.view_count"
                + " HAVING min_price IS NOT NULL"
                + " ORDER BY p.view_count DESC, p.product_id ASC LIMIT ?";
        return listFeatured(sql, safeLimit);
    }

    private static List<Product> listFeatured(String sql, int limit) {
        List<Product> products = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    products.add(mapSummary(rs));
                }
            }
            loadOffers(conn, products, null);
            return products;
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
                loadOffers(conn, List.of(product), null);
                loadImages(conn, product);
            }
            return Optional.ofNullable(product);
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
                + " AND (s.verification_status = 'VERIFIED' OR s.verification_status IS NULL)"
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

    /** Sản phẩm cùng danh mục, ưu tiên nhiều đánh giá và điểm cao. */
    public static List<Product> findRelated(long productId, long categoryId, int limit) {
        int safeLimit = Math.min(Math.max(1, limit), 12);
        String sql = SELECT_COLUMNS
                + " AND p.product_id <> ? AND p.category_id = ?"
                + " GROUP BY p.product_id"
                + " HAVING min_price IS NOT NULL"
                + " ORDER BY review_count DESC, rating_avg DESC, p.product_id DESC LIMIT ?";
        List<Product> products = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, productId);
            ps.setLong(2, categoryId);
            ps.setInt(3, safeLimit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    products.add(mapSummary(rs));
                }
            }
            loadOffers(conn, products, null);
            return products;
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    // ---------------------------------------------------------------- internal

    private static void appendFilters(StringBuilder sql, SearchCriteria c) {
        if (c.query() != null && !c.query().isBlank()) {
            sql.append(" AND (LOWER(p.product_name) LIKE ? OR LOWER(p.description) LIKE ?"
                    + " OR LOWER(p.search_keywords) LIKE ?"
                    + " OR EXISTS (SELECT 1 FROM shop_products sp3 JOIN print_shops s3"
                    + "   ON s3.shop_id = sp3.shop_id WHERE sp3.product_id = p.product_id"
                    + "   AND (s3.verification_status = 'VERIFIED' OR s3.verification_status IS NULL)"
                    + "   AND (s3.status IS NULL OR s3.status NOT IN ('INACTIVE','BLOCKED'))"
                    + "   AND LOWER(s3.shop_name) LIKE ?))");
        }
        if (c.category() != null && !c.category().isBlank()) {
            sql.append(" AND c.category_name = ?");
        }
        if (c.shopId() != null) {
            // Ràng buộc trực tiếp alias sp để MIN(price), sắp xếp, MOQ và offer
            // hiển thị đều thuộc đúng gian hàng người dùng đã chọn.
            sql.append(" AND sp.shop_id = ?");
        }
    }

    private static void appendHaving(StringBuilder sql, SearchCriteria c) {
        sql.append(" HAVING min_price IS NOT NULL");
        if (c.minPrice() != null) {
            sql.append(" AND min_price >= ?");
            if (c.maxPrice() != null) {
                sql.append(" AND min_price <= ?");
            }
        } else if (c.maxPrice() != null) {
            sql.append(" AND min_price <= ?");
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

    private static void loadOffers(Connection conn, List<Product> products, Long shopId)
            throws SQLException {
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
                + " AND (s.verification_status = 'VERIFIED' OR s.verification_status IS NULL)"
                + " AND (s.status IS NULL OR s.status NOT IN ('INACTIVE','BLOCKED'))"
                + (shopId == null ? "" : " AND sp.shop_id = ?")
                + " ORDER BY sp.unit_price";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < products.size(); i++) {
                ps.setLong(i + 1, products.get(i).getId());
            }
            if (shopId != null) {
                ps.setLong(products.size() + 1, shopId);
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

    private static void loadImages(Connection conn, Product product) throws SQLException {
        String sql = "SELECT image_id, image_url, alt_text, is_primary, display_order"
                + " FROM product_images WHERE product_id = ?"
                + " ORDER BY is_primary DESC, display_order, image_id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, product.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String url = rs.getString("image_url");
                    if (url == null || url.isBlank()) {
                        continue;
                    }
                    ProductImage image = new ProductImage();
                    image.setImageId(rs.getLong("image_id"));
                    image.setImageUrl(url);
                    image.setAltText(rs.getString("alt_text"));
                    image.setPrimary(rs.getBoolean("is_primary"));
                    image.setDisplayOrder(rs.getInt("display_order"));
                    product.getImages().add(image);
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
