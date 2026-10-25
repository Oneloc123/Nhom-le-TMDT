package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.OrderStatus;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.QuoteRequest;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.WorkshopWorkflow;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Database operations for an authenticated owner of a print_shops row. */
public final class WorkshopDao {
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> OFFER_STOCK_STATES = Set.of("IN_STOCK", "OUT_OF_STOCK", "PREORDER");
    private static final Set<String> ORDER_FILTERS = Set.of(
            OrderStatus.PENDING_PAYMENT, OrderStatus.CONFIRMED, OrderStatus.PROCESSING,
            OrderStatus.SHIPPED, OrderStatus.COMPLETED, OrderStatus.CANCELLED);
    private static final Set<String> QUOTE_FILTERS = Set.of(
            QuoteRequest.STATUS_PENDING, QuoteRequest.STATUS_QUOTED, QuoteRequest.STATUS_CLOSED);

    private WorkshopDao() { }

    public static boolean hasOwnedShop(long ownerUserId) {
        return scalar("SELECT COUNT(*) FROM print_shops WHERE owner_user_id = ?", ownerUserId) > 0;
    }

    public static List<Map<String, Object>> listOwnedShops(long ownerUserId) {
        return rows("SELECT shop_id id, shop_name name, status, verification_status verificationStatus"
                + " FROM print_shops WHERE owner_user_id = ? ORDER BY created_at, shop_id", ownerUserId);
    }

    /** New seller applications are ordinary active customer accounts awaiting Admin verification. */
    public static long createApplication(long ownerUserId, String shopName, String contactPhone,
                                         String contactEmail, String province, String district,
                                         String description) {
        String name = required(shopName, 180, "Tên xưởng");
        String phone = optional(contactPhone, 25, "Số điện thoại");
        if (!phone.isEmpty() && !phone.matches("[0-9+(). -]{7,25}")) {
            throw new IllegalArgumentException("Số điện thoại xưởng không hợp lệ.");
        }
        String email = optional(contactEmail, 190, "Email liên hệ");
        if (!email.isEmpty() && !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalArgumentException("Email liên hệ không hợp lệ.");
        }
        String region = optional(province, 100, "Tỉnh/thành phố");
        String localArea = optional(district, 100, "Quận/huyện");
        String details = optional(description, 4000, "Mô tả");

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String ownerEmail;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT email, role, status FROM users WHERE user_id = ? FOR UPDATE")) {
                    ps.setLong(1, ownerUserId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next() || !User.STATUS_ACTIVE.equals(rs.getString("status"))) {
                            throw new IllegalArgumentException("Tài khoản chưa đủ điều kiện đăng ký xưởng.");
                        }
                        if (User.ROLE_ADMIN.equals(rs.getString("role"))) {
                            throw new IllegalArgumentException("Tài khoản Admin không thể đăng ký xưởng.");
                        }
                        ownerEmail = rs.getString("email");
                    }
                }
                if (scalar(conn, "SELECT COUNT(*) FROM print_shops WHERE owner_user_id = ?", ownerUserId) > 0) {
                    throw new IllegalArgumentException("Tài khoản này đã liên kết với xưởng.");
                }
                String sql = "INSERT INTO print_shops (owner_user_id, shop_name, slug, description,"
                        + " contact_phone, contact_email, province, district, search_keywords,"
                        + " verification_status, status, created_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', 'ACTIVE', NOW())";
                long shopId;
                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, ownerUserId);
                    ps.setString(2, name);
                    ps.setString(3, slug(name));
                    ps.setString(4, details);
                    setNullable(ps, 5, phone);
                    setNullable(ps, 6, email.isEmpty() ? ownerEmail : email);
                    setNullable(ps, 7, region);
                    setNullable(ps, 8, localArea);
                    String keywords = String.join(" ", name, details, region, localArea).trim();
                    ps.setString(9, keywords.length() > 500 ? keywords.substring(0, 500) : keywords);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Không nhận được mã xưởng mới.");
                        shopId = keys.getLong(1);
                    }
                }
                conn.commit();
                return shopId;
            } catch (SQLException | RuntimeException e) {
                rollback(conn, e);
                if (e instanceof SQLException sqlException && sqlException.getErrorCode() == 1062) {
                    throw new IllegalArgumentException("Tên hoặc đường dẫn xưởng đã tồn tại, hãy thử tên khác.");
                }
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static Map<String, Object> dashboard(long ownerUserId, long shopId) {
        try (Connection conn = Database.getConnection()) {
            requireOwnedShop(conn, ownerUserId, shopId);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("shop", one(conn, "SELECT shop_id id, shop_name name, status,"
                    + " verification_status verificationStatus, average_rating rating"
                    + " FROM print_shops WHERE shop_id = ?", shopId));
            result.put("metrics", one(conn, "SELECT"
                    + " (SELECT COUNT(*) FROM quote_requests q WHERE q.preferred_shop_id = ? AND q.status = ?) newQuotes,"
                    + " (SELECT COUNT(DISTINCT o.order_id) FROM order_items oi JOIN orders o ON o.order_id = oi.order_id"
                    + " WHERE oi.shop_id = ? AND o.status IN (?, ?, ?)) activeOrders,"
                    + " (SELECT COUNT(DISTINCT o.order_id) FROM order_items oi JOIN orders o ON o.order_id = oi.order_id"
                    + " WHERE oi.shop_id = ? AND o.status = ?) completedOrders,"
                    + " (SELECT COUNT(DISTINCT o.order_id) FROM order_items oi JOIN orders o ON o.order_id = oi.order_id"
                    + " WHERE oi.shop_id = ? AND o.status = ?) cancelledOrders,"
                    + " (SELECT COUNT(*) FROM shop_products sp WHERE sp.shop_id = ?) offers,"
                    + " (SELECT COUNT(*) FROM shop_products sp JOIN products p ON p.product_id = sp.product_id"
                    + " WHERE sp.shop_id = ? AND sp.is_active = 1 AND p.is_active = 1"
                    + " AND p.moderation_status = 'APPROVED') publishedProducts,"
                    + " (SELECT COUNT(DISTINCT p.product_id) FROM shop_products sp JOIN products p ON p.product_id = sp.product_id"
                    + " WHERE sp.shop_id = ? AND p.moderation_status = 'PENDING_REVIEW') pendingReviewProducts",
                    shopId, QuoteRequest.STATUS_PENDING, shopId,
                    OrderStatus.CONFIRMED, OrderStatus.PROCESSING, OrderStatus.SHIPPED,
                    shopId, OrderStatus.COMPLETED, shopId, OrderStatus.CANCELLED,
                    shopId, shopId, shopId));
            result.put("recentOrders", rows(conn, "SELECT oi.order_item_id id, o.order_code code,"
                    + " oi.product_name_snapshot productName, oi.quantity, oi.status itemStatus,"
                    + " o.status orderStatus, o.payment_status paymentStatus, o.placed_at placedAt"
                    + " FROM order_items oi JOIN orders o ON o.order_id = oi.order_id"
                    + " WHERE oi.shop_id = ? ORDER BY o.placed_at DESC, oi.order_item_id DESC LIMIT 8", shopId));
            result.put("recentQuotes", rows(conn, "SELECT q.quote_request_id id, q.company_name companyName,"
                    + " q.requested_quantity quantity, q.required_by requiredBy, q.status,"
                    + " p.product_name productName FROM quote_requests q"
                    + " LEFT JOIN products p ON p.product_id = q.product_id"
                    + " WHERE q.preferred_shop_id = ? ORDER BY q.quote_request_id DESC LIMIT 8", shopId));
            return result;
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static Map<String, Object> profile(long ownerUserId, long shopId) {
        try (Connection conn = Database.getConnection()) {
            requireOwnedShop(conn, ownerUserId, shopId);
            return one(conn, "SELECT s.shop_id id, s.shop_name name, s.slug, s.description, s.logo_url logoUrl,"
                    + " s.contact_phone contactPhone, COALESCE(NULLIF(s.contact_email, ''), u.email) contactEmail,"
                    + " s.address_line addressLine, s.province, s.district, s.search_keywords searchKeywords,"
                    + " s.capabilities, s.accepted_file_formats acceptedFileFormats,"
                    + " s.max_build_dimensions maxBuildDimensions, s.operating_hours operatingHours,"
                    + " s.estimated_lead_days estimatedLeadDays, s.service_notes serviceNotes,"
                    + " s.average_rating rating, s.verification_status verificationStatus, s.status,"
                    + " u.full_name ownerName, u.email ownerEmail"
                    + " FROM print_shops s LEFT JOIN users u ON u.user_id = s.owner_user_id"
                    + " WHERE s.shop_id = ?", shopId);
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static void updateProfile(long ownerUserId, long shopId, Map<String, String> input) {
        String name = required(input.get("name"), 180, "Tên xưởng");
        String phone = optional(input.get("contactPhone"), 25, "Số điện thoại");
        if (!phone.isEmpty() && !phone.matches("[0-9+(). -]{7,25}")) {
            throw new IllegalArgumentException("Số điện thoại xưởng không hợp lệ.");
        }
        String email = optional(input.get("contactEmail"), 190, "Email liên hệ");
        if (!email.isEmpty() && !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalArgumentException("Email liên hệ không hợp lệ.");
        }
        String address = optional(input.get("addressLine"), 255, "Địa chỉ");
        String province = optional(input.get("province"), 100, "Tỉnh/thành phố");
        String district = optional(input.get("district"), 100, "Quận/huyện");
        String description = optional(input.get("description"), 4000, "Mô tả");
        String capabilities = optional(input.get("capabilities"), 4000, "Năng lực và dịch vụ");
        String formats = optional(input.get("acceptedFileFormats"), 255, "Định dạng tệp");
        String dimensions = optional(input.get("maxBuildDimensions"), 120, "Kích thước in tối đa");
        String hours = optional(input.get("operatingHours"), 255, "Thời gian hoạt động");
        String notes = optional(input.get("serviceNotes"), 4000, "Ghi chú dịch vụ");
        int leadDays = intValue(input.get("estimatedLeadDays"), 0, 365, "Thời gian xử lý dự kiến");
        String logoUrl = validateImageUrl(input.get("logoUrl"));
        String keywords = String.join(" ", name, description, capabilities, formats, dimensions,
                province, district).trim();
        if (keywords.length() > 500) keywords = keywords.substring(0, 500);

        String sql = "UPDATE print_shops SET shop_name = ?, description = ?, logo_url = ?,"
                + " contact_phone = ?, contact_email = ?, address_line = ?, province = ?, district = ?,"
                + " search_keywords = ?, capabilities = ?, accepted_file_formats = ?,"
                + " max_build_dimensions = ?, operating_hours = ?, estimated_lead_days = ?, service_notes = ?"
                + " WHERE shop_id = ? AND owner_user_id = ?";
        try (Connection conn = Database.getConnection()) {
            requireOwnedShop(conn, ownerUserId, shopId);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, name); setNullable(ps, 2, description); setNullable(ps, 3, logoUrl);
                setNullable(ps, 4, phone); setNullable(ps, 5, email); setNullable(ps, 6, address);
                setNullable(ps, 7, province); setNullable(ps, 8, district); ps.setString(9, keywords);
                setNullable(ps, 10, capabilities); setNullable(ps, 11, formats); setNullable(ps, 12, dimensions);
                setNullable(ps, 13, hours); if (leadDays == 0) ps.setNull(14, java.sql.Types.SMALLINT);
                else ps.setInt(14, leadDays);
                setNullable(ps, 15, notes); ps.setLong(16, shopId); ps.setLong(17, ownerUserId);
                if (ps.executeUpdate() != 1) throw new IllegalArgumentException("Không tìm thấy xưởng thuộc tài khoản này.");
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static List<Map<String, Object>> categories() {
        return rows("SELECT category_id id, category_name name FROM categories"
                + " WHERE is_active = 1 ORDER BY display_order, category_name");
    }

    public static Map<String, Object> listOffers(long ownerUserId, long shopId, String query,
                                                  String status, Long categoryId, int page, int pageSize) {
        StringBuilder where = new StringBuilder(" WHERE sp.shop_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(shopId);
        appendSearch(where, args, query, "CAST(p.product_id AS CHAR)", "p.product_name", "p.description",
                "p.brand", "sp.sku", "sp.material", "sp.printing_method");
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            switch (status) {
                case "ACTIVE" -> where.append(" AND sp.is_active = 1");
                case "INACTIVE" -> where.append(" AND sp.is_active = 0");
                case "PENDING_REVIEW", "APPROVED", "REJECTED" -> { where.append(" AND p.moderation_status = ?"); args.add(status); }
                default -> throw new IllegalArgumentException("Bộ lọc sản phẩm không hợp lệ.");
            }
        }
        if (categoryId != null) { where.append(" AND p.category_id = ?"); args.add(categoryId); }
        String from = " FROM shop_products sp JOIN products p ON p.product_id = sp.product_id"
                + " LEFT JOIN categories c ON c.category_id = p.category_id";
        try (Connection conn = Database.getConnection()) {
            requireOwnedShop(conn, ownerUserId, shopId);
            return page(conn, "SELECT sp.shop_product_id id, sp.product_id productId, sp.sku,"
                    + " p.product_name name, p.slug, p.description, p.brand, p.is_active productActive,"
                    + " p.category_id categoryId, p.is_customizable customizable,"
                    + " p.moderation_status moderationStatus, p.rejection_reason rejectionReason,"
                    + " (SELECT pi.image_url FROM product_images pi WHERE pi.product_id = p.product_id"
                    + " ORDER BY pi.is_primary DESC, pi.display_order, pi.image_id LIMIT 1) imageUrl,"
                    + " c.category_name category, sp.printing_method printingMethod, sp.material,"
                    + " sp.unit_price unitPrice, sp.minimum_quantity minimumQuantity,"
                    + " sp.production_days productionDays, sp.stock_status stockStatus, sp.is_active offerActive,"
                    + " (SELECT COUNT(DISTINCT other.shop_id) FROM shop_products other"
                    + " WHERE other.product_id = p.product_id) = 1 canEditContent"
                    + from + where + " ORDER BY sp.updated_at DESC, sp.shop_product_id DESC",
                    "SELECT COUNT(*)" + from + where, args, page, pageSize,
                    "SELECT category_id id, category_name name FROM categories WHERE is_active = 1"
                            + " ORDER BY display_order, category_name");
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static long createProductAndOffer(long ownerUserId, long shopId, Map<String, String> input) {
        String name = required(input.get("name"), 180, "Tên sản phẩm");
        String description = optional(input.get("description"), 8000, "Mô tả");
        String brand = optional(input.get("brand"), 120, "Thương hiệu");
        long categoryId = positiveLong(input.get("categoryId"), "Danh mục");
        String method = required(input.get("printingMethod"), 100, "Công nghệ in");
        String material = required(input.get("material"), 120, "Vật liệu");
        BigDecimal price = price(input.get("unitPrice"));
        int minimumQuantity = intValue(input.get("minimumQuantity"), 1, 1_000_000, "Số lượng tối thiểu");
        int productionDays = intValue(input.get("productionDays"), 0, 365, "Thời gian sản xuất");
        String sku = optional(input.get("sku"), 80, "SKU");
        String stockStatus = stockStatus(input.get("stockStatus"));
        String imageUrl = validateImageUrl(input.get("imageUrl"));
        boolean customizable = "true".equalsIgnoreCase(input.get("customizable"));

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                requireOwnedShop(conn, ownerUserId, shopId);
                if (scalar(conn, "SELECT COUNT(*) FROM categories WHERE category_id = ? AND is_active = 1", categoryId) != 1) {
                    throw new IllegalArgumentException("Danh mục không tồn tại hoặc đã ngừng hoạt động.");
                }
                String productSql = "INSERT INTO products (category_id, product_name, slug, description, brand,"
                        + " base_unit, is_customizable, is_active, moderation_status, created_at, updated_at)"
                        + " VALUES (?, ?, ?, ?, ?, 'unit', ?, 1, 'PENDING_REVIEW', NOW(), NOW())";
                long productId;
                try (PreparedStatement ps = conn.prepareStatement(productSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, categoryId); ps.setString(2, name); ps.setString(3, slug(name));
                    setNullable(ps, 4, description); setNullable(ps, 5, brand); ps.setBoolean(6, customizable);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Không nhận được mã sản phẩm mới.");
                        productId = keys.getLong(1);
                    }
                }
                String offerSql = "INSERT INTO shop_products (shop_id, product_id, sku, printing_method,"
                        + " material, unit_price, minimum_quantity, production_days, stock_status, is_active, updated_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, NOW())";
                try (PreparedStatement ps = conn.prepareStatement(offerSql)) {
                    ps.setLong(1, shopId); ps.setLong(2, productId); setNullable(ps, 3, sku);
                    ps.setString(4, method); ps.setString(5, material); ps.setBigDecimal(6, price);
                    ps.setInt(7, minimumQuantity); ps.setInt(8, productionDays); ps.setString(9, stockStatus);
                    ps.executeUpdate();
                }
                savePrimaryImage(conn, productId, imageUrl, name);
                conn.commit();
                return productId;
            } catch (SQLException | RuntimeException e) {
                rollback(conn, e);
                if (e instanceof SQLException sqlException && sqlException.getErrorCode() == 1062) {
                    throw new IllegalArgumentException("SKU hoặc đường dẫn sản phẩm đã được sử dụng.");
                }
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static void updateProductContent(long ownerUserId, long shopId, long productId,
                                            Map<String, String> input) {
        String name = required(input.get("name"), 180, "Tên sản phẩm");
        String description = optional(input.get("description"), 8000, "Mô tả");
        String brand = optional(input.get("brand"), 120, "Thương hiệu");
        String imageUrl = validateImageUrl(input.get("imageUrl"));
        long categoryId = positiveLong(input.get("categoryId"), "Danh mục");
        boolean customizable = "true".equalsIgnoreCase(input.get("customizable"));
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                requireOwnedShop(conn, ownerUserId, shopId);
                try (PreparedStatement lock = conn.prepareStatement(
                        "SELECT product_id FROM products WHERE product_id = ? FOR UPDATE")) {
                    lock.setLong(1, productId);
                    try (ResultSet rs = lock.executeQuery()) {
                        if (!rs.next()) throw new NotFoundException("Không tìm thấy sản phẩm.");
                    }
                }
                long offers = scalar(conn, "SELECT COUNT(DISTINCT shop_id) FROM shop_products WHERE product_id = ?", productId);
                if (offers != 1 || scalar(conn, "SELECT COUNT(*) FROM shop_products WHERE product_id = ? AND shop_id = ?",
                        productId, shopId) == 0) {
                    throw new IllegalArgumentException("Sản phẩm đang được nhiều xưởng cùng chào bán; chỉ có thể sửa bảng giá riêng của xưởng.");
                }
                if (scalar(conn, "SELECT COUNT(*) FROM categories WHERE category_id = ? AND is_active = 1", categoryId) != 1) {
                    throw new IllegalArgumentException("Danh mục không tồn tại hoặc đã ngừng hoạt động.");
                }
                String sql = "UPDATE products SET category_id = ?, product_name = ?, slug = ?, description = ?,"
                        + " brand = ?, is_customizable = ?, moderation_status = 'PENDING_REVIEW',"
                        + " rejection_reason = NULL, reviewed_by_user_id = NULL, reviewed_at = NULL, updated_at = NOW()"
                        + " WHERE product_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setLong(1, categoryId); ps.setString(2, name); ps.setString(3, slug(name));
                    setNullable(ps, 4, description); setNullable(ps, 5, brand); ps.setBoolean(6, customizable);
                    ps.setLong(7, productId);
                    ps.executeUpdate();
                }
                savePrimaryImage(conn, productId, imageUrl, name);
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                rollback(conn, e);
                if (e instanceof SQLException sqlException && sqlException.getErrorCode() == 1062) {
                    throw new IllegalArgumentException("Tên hoặc đường dẫn sản phẩm đã tồn tại.");
                }
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static void updateOffer(long ownerUserId, long shopId, long offerId, Map<String, String> input) {
        String sku = optional(input.get("sku"), 80, "SKU");
        String method = required(input.get("printingMethod"), 100, "Công nghệ in");
        String material = required(input.get("material"), 120, "Vật liệu");
        BigDecimal price = price(input.get("unitPrice"));
        int minimumQuantity = intValue(input.get("minimumQuantity"), 1, 1_000_000, "Số lượng tối thiểu");
        int productionDays = intValue(input.get("productionDays"), 0, 365, "Thời gian sản xuất");
        String stockStatus = stockStatus(input.get("stockStatus"));
        String sql = "UPDATE shop_products SET sku = ?, printing_method = ?, material = ?, unit_price = ?,"
                + " minimum_quantity = ?, production_days = ?, stock_status = ?, updated_at = NOW()"
                + " WHERE shop_product_id = ? AND shop_id = ?";
        try (Connection conn = Database.getConnection()) {
            requireOwnedShop(conn, ownerUserId, shopId);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                setNullable(ps, 1, sku); ps.setString(2, method); ps.setString(3, material); ps.setBigDecimal(4, price);
                ps.setInt(5, minimumQuantity); ps.setInt(6, productionDays); ps.setString(7, stockStatus);
                ps.setLong(8, offerId); ps.setLong(9, shopId);
                if (ps.executeUpdate() != 1) throw new IllegalArgumentException("Không tìm thấy chào bán thuộc xưởng này.");
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) throw new IllegalArgumentException("SKU này đã được xưởng khác sử dụng.");
            throw DataAccessException.of(e);
        }
    }

    public static void setOfferActive(long ownerUserId, long shopId, long offerId, boolean active) {
        try (Connection conn = Database.getConnection()) {
            requireOwnedShop(conn, ownerUserId, shopId);
            if (active && scalar(conn, "SELECT COUNT(*) FROM shop_products WHERE shop_product_id = ?"
                    + " AND shop_id = ? AND stock_status = 'OUT_OF_STOCK'", offerId, shopId) > 0) {
                throw new IllegalArgumentException("Hãy cập nhật tình trạng kho trước khi mở lại chào bán.");
            }
            try (PreparedStatement ps = conn.prepareStatement("UPDATE shop_products SET is_active = ?, updated_at = NOW()"
                    + " WHERE shop_product_id = ? AND shop_id = ?")) {
                ps.setBoolean(1, active); ps.setLong(2, offerId); ps.setLong(3, shopId);
                if (ps.executeUpdate() != 1) throw new IllegalArgumentException("Không tìm thấy chào bán thuộc xưởng này.");
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static Map<String, Object> listOrders(long ownerUserId, long shopId, String query,
                                                  String status, int page, int pageSize) {
        StringBuilder where = new StringBuilder(" WHERE oi.shop_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(shopId);
        appendOrderSearch(where, args, query);
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            if (!ORDER_FILTERS.contains(status)) throw new IllegalArgumentException("Trạng thái đơn không hợp lệ.");
            where.append(" AND o.status = ?"); args.add(status);
        }
        String from = " FROM order_items oi JOIN orders o ON o.order_id = oi.order_id"
                + " LEFT JOIN users u ON u.user_id = o.user_id"
                + " LEFT JOIN products p ON p.product_id = oi.product_id";
        try (Connection conn = Database.getConnection()) {
            requireOwnedShop(conn, ownerUserId, shopId);
            return page(conn, "SELECT oi.order_item_id id, oi.order_id orderId, o.order_code code,"
                    + " o.status orderStatus, o.payment_status paymentStatus, o.placed_at placedAt,"
                    + " oi.product_name_snapshot productName, oi.quantity, oi.unit_price unitPrice,"
                    + " oi.line_total lineTotal, oi.status itemStatus,"
                    + " CASE WHEN o.status IN ('" + OrderStatus.CONFIRMED + "', '"
                    + OrderStatus.PROCESSING + "', '" + OrderStatus.SHIPPED
                    + "') THEN u.full_name ELSE NULL END customerName,"
                    + " (SELECT pu.stage FROM production_updates pu WHERE pu.order_item_id = oi.order_item_id"
                    + " AND pu.shop_id = oi.shop_id ORDER BY pu.created_at DESC, pu.production_update_id DESC LIMIT 1) latestStage,"
                    + " (SELECT pu.progress_percent FROM production_updates pu WHERE pu.order_item_id = oi.order_item_id"
                    + " AND pu.shop_id = oi.shop_id ORDER BY pu.created_at DESC, pu.production_update_id DESC LIMIT 1) latestProgress"
                    + from + where + " ORDER BY o.placed_at DESC, oi.order_item_id DESC",
                    "SELECT COUNT(*)" + from + where, args, page, pageSize,
                    "SELECT DISTINCT status value FROM orders WHERE status IS NOT NULL ORDER BY status");
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static Optional<Map<String, Object>> orderItemDetail(long ownerUserId, long shopId, long itemId) {
        String sql = "SELECT oi.order_item_id id, oi.order_id orderId, o.order_code code,"
                + " o.status orderStatus, o.payment_status paymentStatus, o.shipping_status shippingStatus,"
                + " o.placed_at placedAt, oi.product_id productId, oi.shop_product_id shopProductId,"
                + " oi.product_name_snapshot productName, oi.shop_name_snapshot shopName, oi.quantity,"
                + " oi.unit_price unitPrice, oi.line_total lineTotal, oi.status itemStatus,"
                + " CASE WHEN o.status IN (?, ?, ?) THEN u.full_name ELSE NULL END customerName,"
                + " CASE WHEN o.status IN (?, ?, ?) THEN u.email ELSE NULL END customerEmail,"
                + " CASE WHEN o.status IN (?, ?, ?) THEN COALESCE(a.recipient_name, u.full_name) ELSE NULL END recipientName,"
                + " CASE WHEN o.status IN (?, ?, ?) THEN a.recipient_phone ELSE NULL END recipientPhone,"
                + " CASE WHEN o.status IN (?, ?, ?) THEN CONCAT(COALESCE(a.address_line, ''),"
                + " CASE WHEN a.district IS NULL OR a.district = '' THEN '' ELSE ', ' END, COALESCE(a.district, ''),"
                + " CASE WHEN a.province IS NULL OR a.province = '' THEN '' ELSE ', ' END, COALESCE(a.province, ''))"
                + " ELSE NULL END shippingAddress, df.original_filename designFileName"
                + " FROM order_items oi JOIN orders o ON o.order_id = oi.order_id"
                + " LEFT JOIN users u ON u.user_id = o.user_id"
                + " LEFT JOIN addresses a ON a.address_id = o.address_id AND a.user_id = o.user_id"
                + " LEFT JOIN design_files df ON df.design_file_id = oi.design_file_id AND df.user_id = o.user_id"
                + " WHERE oi.order_item_id = ? AND oi.shop_id = ?";
        List<Object> args = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            args.add(OrderStatus.CONFIRMED); args.add(OrderStatus.PROCESSING); args.add(OrderStatus.SHIPPED);
        }
        args.add(itemId); args.add(shopId);
        try (Connection conn = Database.getConnection()) {
            requireOwnedShop(conn, ownerUserId, shopId);
            Optional<Map<String, Object>> detail = findOne(conn, sql, args.toArray());
            if (detail.isPresent()) {
                Map<String, Object> row = detail.get();
                row.put("productionUpdates", rows(conn,
                        "SELECT pu.production_update_id id, pu.stage, pu.progress_percent progressPercent,"
                                + " pu.message, pu.created_at createdAt, u.full_name createdBy"
                                + " FROM production_updates pu LEFT JOIN users u ON u.user_id = pu.created_by_user_id"
                                + " WHERE pu.shop_id = ? AND pu.order_item_id = ?"
                                + " ORDER BY pu.created_at, pu.production_update_id", shopId, itemId));
                long orderId = ((Number) row.get("orderId")).longValue();
                row.put("orderStatusHistory", rows(conn,
                        "SELECT old_status oldStatus, new_status newStatus, note, changed_at changedAt"
                                + " FROM order_status_history WHERE order_id = ?"
                                + " ORDER BY changed_at, history_id", orderId));
            }
            return detail;
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static void addProductionUpdate(long ownerUserId, long shopId, long itemId, long actorUserId,
                                           String stage, int progress, String message) {
        String normalizedMessage = optional(message, 500, "Ghi chú tiến độ");
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                requireOwnedShop(conn, ownerUserId, shopId);
                String orderStatus;
                String paymentStatus;
                try (PreparedStatement ps = conn.prepareStatement("SELECT o.status, o.payment_status"
                        + " FROM order_items oi JOIN orders o ON o.order_id = oi.order_id"
                        + " WHERE oi.order_item_id = ? AND oi.shop_id = ? FOR UPDATE")) {
                    ps.setLong(1, itemId); ps.setLong(2, shopId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new NotFoundException("Không tìm thấy dòng đơn hàng thuộc xưởng này.");
                        orderStatus = rs.getString("status"); paymentStatus = rs.getString("payment_status");
                    }
                }
                String previousStage = null;
                int previousProgress = 0;
                try (PreparedStatement ps = conn.prepareStatement("SELECT stage, progress_percent"
                        + " FROM production_updates WHERE shop_id = ? AND order_item_id = ?"
                        + " ORDER BY created_at DESC, production_update_id DESC LIMIT 1 FOR UPDATE")) {
                    ps.setLong(1, shopId); ps.setLong(2, itemId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) { previousStage = rs.getString("stage"); previousProgress = rs.getInt("progress_percent"); }
                    }
                }
                WorkshopWorkflow.requireValidUpdate(orderStatus, paymentStatus, previousStage,
                        previousProgress, stage, progress);
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO production_updates"
                        + " (order_item_id, shop_id, created_by_user_id, stage, progress_percent, message, created_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?, NOW())")) {
                    ps.setLong(1, itemId); ps.setLong(2, shopId); ps.setLong(3, actorUserId);
                    ps.setString(4, stage); ps.setInt(5, progress); setNullable(ps, 6, normalizedMessage);
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                rollback(conn, e); throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static Map<String, Object> listQuotes(long ownerUserId, long shopId, String query,
                                                  String status, int page, int pageSize) {
        StringBuilder where = new StringBuilder(" WHERE q.preferred_shop_id = ?");
        List<Object> args = new ArrayList<>(); args.add(shopId);
        appendSearch(where, args, query, "CAST(q.quote_request_id AS CHAR)", "q.company_name",
                "p.product_name", "u.full_name", "u.email");
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            if (!QUOTE_FILTERS.contains(status)) throw new IllegalArgumentException("Trạng thái báo giá không hợp lệ.");
            where.append(" AND q.status = ?"); args.add(status);
        }
        String from = " FROM quote_requests q LEFT JOIN products p ON p.product_id = q.product_id"
                + " LEFT JOIN users u ON u.user_id = q.user_id";
        try (Connection conn = Database.getConnection()) {
            requireOwnedShop(conn, ownerUserId, shopId);
            return page(conn, "SELECT q.quote_request_id id, q.company_name companyName,"
                    + " q.requested_quantity quantity, q.required_by requiredBy, q.quoted_amount quotedAmount,"
                    + " q.seller_response_note responseNote, q.status, p.product_name productName,"
                    + " u.full_name customerName, u.email customerEmail"
                    + from + where + " ORDER BY q.quote_request_id DESC",
                    "SELECT COUNT(*)" + from + where, args, page, pageSize,
                    "SELECT DISTINCT status value FROM quote_requests WHERE status IS NOT NULL ORDER BY status");
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static void respondToQuote(long ownerUserId, long shopId, long quoteId,
                                      String action, String amountText, String reason) {
        String normalizedAction = action == null ? "" : action.trim().toUpperCase(Locale.ROOT);
        BigDecimal amount = null;
        String note = optional(reason, 500, "Phản hồi");
        if ("QUOTE".equals(normalizedAction)) amount = price(amountText);
        else if ("CLOSE".equals(normalizedAction)) {
            if (note.length() < 8) throw new IllegalArgumentException("Lý do từ chối cần ít nhất 8 ký tự.");
        } else throw new IllegalArgumentException("Thao tác yêu cầu báo giá không hợp lệ.");

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                requireOwnedShop(conn, ownerUserId, shopId);
                String current;
                try (PreparedStatement ps = conn.prepareStatement("SELECT status FROM quote_requests"
                        + " WHERE quote_request_id = ? AND preferred_shop_id = ? FOR UPDATE")) {
                    ps.setLong(1, quoteId); ps.setLong(2, shopId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new NotFoundException("Không tìm thấy yêu cầu báo giá của xưởng này.");
                        current = rs.getString("status");
                    }
                }
                if (!QuoteRequest.STATUS_PENDING.equals(current)) {
                    throw new IllegalArgumentException("Chỉ yêu cầu đang chờ mới được phản hồi.");
                }
                String sql = "UPDATE quote_requests SET quoted_amount = ?, seller_response_note = ?, status = ?"
                        + " WHERE quote_request_id = ? AND preferred_shop_id = ? AND status = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    if (amount == null) ps.setNull(1, java.sql.Types.DECIMAL); else ps.setBigDecimal(1, amount);
                    setNullable(ps, 2, note.isEmpty() ? null : note);
                    ps.setString(3, "QUOTE".equals(normalizedAction)
                            ? QuoteRequest.STATUS_QUOTED : QuoteRequest.STATUS_CLOSED);
                    ps.setLong(4, quoteId); ps.setLong(5, shopId); ps.setString(6, QuoteRequest.STATUS_PENDING);
                    if (ps.executeUpdate() != 1) throw new IllegalArgumentException("Yêu cầu báo giá đã được xử lý.");
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                rollback(conn, e); throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static List<Map<String, Object>> reviews(long ownerUserId, long shopId) {
        try (Connection conn = Database.getConnection()) {
            requireOwnedShop(conn, ownerUserId, shopId);
            return rows(conn, "SELECT r.review_id id, r.rating, r.review_text reviewText,"
                    + " u.full_name customerName, p.product_name productName, o.order_code orderCode"
                    + " FROM reviews r LEFT JOIN users u ON u.user_id = r.user_id"
                    + " LEFT JOIN products p ON p.product_id = r.product_id"
                    + " LEFT JOIN order_items oi ON oi.order_item_id = r.order_item_id"
                    + " LEFT JOIN orders o ON o.order_id = oi.order_id"
                    + " WHERE r.shop_id = ? AND r.is_published = 1"
                    + " ORDER BY r.review_id DESC LIMIT 50", shopId);
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    private static Map<String, Object> page(Connection conn, String select, String count,
                                            List<Object> args, int requestedPage, int requestedSize,
                                            String... optionQueries) throws SQLException {
        int size = Math.min(Math.max(1, requestedSize), MAX_PAGE_SIZE);
        long total = scalar(conn, count, args.toArray());
        int pages = Math.max(1, (int) Math.ceil((double) total / size));
        int current = Math.min(Math.max(1, requestedPage), pages);
        List<Object> dataArgs = new ArrayList<>(args);
        dataArgs.add(size); dataArgs.add((long) (current - 1) * size);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", rows(conn, select + " LIMIT ? OFFSET ?", dataArgs.toArray()));
        result.put("total", total); result.put("page", current); result.put("pageSize", size); result.put("pages", pages);
        if (optionQueries.length > 0) {
            List<Object> options = new ArrayList<>();
            for (String query : optionQueries) options.add(rows(conn, query));
            result.put("options", options);
        }
        return result;
    }

    private static void requireOwnedShop(Connection conn, long ownerUserId, long shopId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM print_shops WHERE shop_id = ? AND owner_user_id = ?")) {
            ps.setLong(1, shopId); ps.setLong(2, ownerUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new NotFoundException("Không tìm thấy xưởng thuộc tài khoản này.");
            }
        }
    }

    private static Map<String, Object> one(Connection conn, String sql, Object... args) throws SQLException {
        List<Map<String, Object>> result = rows(conn, sql, args);
        return result.isEmpty() ? new LinkedHashMap<>() : result.getFirst();
    }

    private static Optional<Map<String, Object>> findOne(Connection conn, String sql, Object... args) throws SQLException {
        List<Map<String, Object>> result = rows(conn, sql, args);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.getFirst());
    }

    private static List<Map<String, Object>> rows(Connection conn, String sql, Object... args) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, args);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                        row.put(rs.getMetaData().getColumnLabel(i), jsonValue(rs.getObject(i)));
                    }
                    result.add(row);
                }
            }
        }
        return result;
    }

    private static List<Map<String, Object>> rows(String sql, Object... args) {
        try (Connection conn = Database.getConnection()) { return rows(conn, sql, args); }
        catch (SQLException e) { throw DataAccessException.of(e); }
    }

    private static long scalar(String sql, Object... args) {
        try (Connection conn = Database.getConnection()) { return scalar(conn, sql, args); }
        catch (SQLException e) { throw DataAccessException.of(e); }
    }

    private static long scalar(Connection conn, String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, args);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getLong(1) : 0; }
        }
    }

    private static void bind(PreparedStatement ps, Object[] args) throws SQLException {
        for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
    }

    private static Object jsonValue(Object value) {
        if (value instanceof Timestamp timestamp) return timestamp.toLocalDateTime().toString();
        if (value instanceof java.sql.Date date) return date.toLocalDate().toString();
        if (value instanceof LocalDateTime || value instanceof LocalDate) return value.toString();
        return value;
    }

    private static void appendSearch(StringBuilder where, List<Object> args, String raw, String... columns) {
        String query = raw == null ? "" : raw.trim();
        if (query.isEmpty()) return;
        if (query.length() > 120) throw new IllegalArgumentException("Từ khóa tìm kiếm tối đa 120 ký tự.");
        where.append(" AND (");
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) where.append(" OR ");
            where.append(columns[i]).append(" LIKE ? ESCAPE '!' ");
            args.add("%" + query.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%");
        }
        where.append(')');
    }

    private static void appendOrderSearch(StringBuilder where, List<Object> args, String raw) {
        String query = raw == null ? "" : raw.trim();
        if (query.isEmpty()) return;
        if (query.length() > 120) throw new IllegalArgumentException("Từ khóa tìm kiếm tối đa 120 ký tự.");
        String pattern = "%" + query.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        where.append(" AND (o.order_code LIKE ? ESCAPE '!'"
                + " OR CAST(o.order_id AS CHAR) LIKE ? ESCAPE '!'"
                + " OR p.product_name LIKE ? ESCAPE '!'"
                + " OR oi.product_name_snapshot LIKE ? ESCAPE '!'"
                + " OR (o.status IN ('" + OrderStatus.CONFIRMED + "', '" + OrderStatus.PROCESSING
                + "', '" + OrderStatus.SHIPPED + "') AND (u.full_name LIKE ? ESCAPE '!'"
                + " OR u.email LIKE ? ESCAPE '!'))) ");
        for (int i = 0; i < 6; i++) args.add(pattern);
    }

    private static String required(String raw, int max, String label) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty() || value.length() > max) throw new IllegalArgumentException(label + " không hợp lệ.");
        return value;
    }

    private static String optional(String raw, int max, String label) {
        String value = raw == null ? "" : raw.trim();
        if (value.length() > max) throw new IllegalArgumentException(label + " tối đa " + max + " ký tự.");
        return value;
    }

    private static String validateImageUrl(String raw) {
        String value = optional(raw, 500, "Đường dẫn hình ảnh");
        if (value.isEmpty()) return "";
        try {
            URI uri = URI.create(value);
            if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null) {
                throw new IllegalArgumentException("Đường dẫn hình ảnh phải là URL HTTP hoặc HTTPS hợp lệ.");
            }
            return value;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Đường dẫn hình ảnh phải là URL HTTP hoặc HTTPS hợp lệ.");
        }
    }

    private static BigDecimal price(String raw) {
        try {
            BigDecimal value = new BigDecimal(raw == null ? "" : raw.trim()).setScale(2, RoundingMode.UNNECESSARY);
            if (value.signum() <= 0 || value.precision() > 12) throw new NumberFormatException();
            return value;
        } catch (ArithmeticException | NumberFormatException e) {
            throw new IllegalArgumentException("Đơn giá phải lớn hơn 0 và có tối đa 2 chữ số thập phân.");
        }
    }

    private static int intValue(String raw, int min, int max, String label) {
        if (raw == null || raw.isBlank()) {
            if (min == 0) return 0;
            throw new IllegalArgumentException(label + " không hợp lệ.");
        }
        try {
            int value = Integer.parseInt(raw.trim());
            if (value < min || value > max) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " phải từ " + min + " đến " + max + ".");
        }
    }

    private static long positiveLong(String raw, String label) {
        try {
            long value = Long.parseLong(raw == null ? "" : raw.trim());
            if (value > 0) return value;
        } catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException(label + " không hợp lệ.");
    }

    private static String stockStatus(String raw) {
        String value = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (!OFFER_STOCK_STATES.contains(value)) throw new IllegalArgumentException("Tình trạng kho không hợp lệ.");
        return value;
    }

    private static String slug(String source) {
        String normalized = Normalizer.normalize(source, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        if (normalized.length() > 160) normalized = normalized.substring(0, 160).replaceAll("-$", "");
        return normalized + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static void setNullable(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) ps.setNull(index, java.sql.Types.VARCHAR);
        else ps.setString(index, value);
    }

    private static void savePrimaryImage(Connection conn, long productId, String imageUrl, String altText)
            throws SQLException {
        if (imageUrl == null || imageUrl.isBlank()) return;
        boolean hasPrimary;
        try (PreparedStatement ps = conn.prepareStatement("SELECT image_id FROM product_images"
                + " WHERE product_id = ? AND is_primary = 1 ORDER BY display_order, image_id LIMIT 1 FOR UPDATE")) {
            ps.setLong(1, productId);
            try (ResultSet rs = ps.executeQuery()) { hasPrimary = rs.next(); }
        }
        if (hasPrimary) {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE product_images"
                    + " SET image_url = ?, alt_text = ? WHERE product_id = ? AND is_primary = 1")) {
                ps.setString(1, imageUrl); ps.setString(2, altText); ps.setLong(3, productId);
                ps.executeUpdate();
            }
            return;
        }
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO product_images"
                + " (product_id, image_url, alt_text, is_primary, display_order) VALUES (?, ?, ?, 1, 0)")) {
            ps.setLong(1, productId); ps.setString(2, imageUrl); ps.setString(3, altText);
            ps.executeUpdate();
        }
    }

    private static void rollback(Connection conn, Throwable original) {
        try { conn.rollback(); } catch (SQLException rollbackError) { original.addSuppressed(rollbackError); }
    }

    public static final class NotFoundException extends RuntimeException {
        public NotFoundException(String message) { super(message); }
    }
}
