package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import vn.edu.hcmuaf.fit.tmdt_nhomle.controller.admin.AdminJson;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

/** Admin repository backed by the application's relational business tables. */
public final class BusinessAdminStore {
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/printkraft"
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8";
    private static final List<String> COLORS = List.of("lilac", "mint", "peach", "sky", "lemon", "rose");

    private BusinessAdminStore() { }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(env("PRINTKRAFT_DB_URL", DEFAULT_URL),
                env("PRINTKRAFT_DB_USER", "root"), env("PRINTKRAFT_DB_PASSWORD", ""));
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    public static List<Map<String, Object>> findAll(String type) {
        try (Connection connection = connection()) {
            return findAll(connection, type);
        } catch (SQLException e) {
            throw databaseError(e);
        }
    }

    private static List<Map<String, Object>> findAll(Connection connection, String type) throws SQLException {
        return switch (type) {
            case "users" -> users(connection);
            case "sellers" -> sellers(connection);
            case "products" -> products(connection);
            case "orders" -> orders(connection);
            case "orderItems" -> orderItems(connection);
            case "transactions" -> transactions(connection);
            case "depositTransactions" -> List.of(); // No deposit ledger exists in the business schema.
            case "commissionRules" -> commissionRules(connection);
            case "reviews" -> reviews(connection);
            default -> throw new IllegalArgumentException("Unknown admin record type: " + type);
        };
    }

    public static Optional<Map<String, Object>> find(String type, String id) {
        return findAll(type).stream().filter(row -> id.equals(String.valueOf(row.get("id")))).findFirst();
    }

    public static void save(String type, String id, Map<String, Object> payload) {
        try (Connection connection = connection()) {
            switch (type) {
                case "commissionRules" -> saveCommissionRule(connection, id, payload);
                case "users" -> createUser(connection, id, payload);
                default -> throw new IllegalArgumentException("Records of type " + type + " cannot be created.");
            }
        } catch (SQLException e) {
            throw databaseError(e);
        }
    }

    public static Optional<Map<String, Object>> update(String type, String id,
                                                        UnaryOperator<Map<String, Object>> updater) {
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            try {
                Optional<Map<String, Object>> found = findAll(connection, type).stream()
                        .filter(row -> id.equals(String.valueOf(row.get("id")))).findFirst();
                if (found.isEmpty()) {
                    connection.rollback();
                    return Optional.empty();
                }
                Map<String, Object> changed = new LinkedHashMap<>(found.get());
                updater.apply(changed);
                persistUpdate(connection, type, changed);
                Map<String, Object> saved = findAll(connection, type).stream()
                        .filter(row -> id.equals(String.valueOf(row.get("id")))).findFirst().orElse(changed);
                connection.commit();
                return Optional.of(saved);
            } catch (RuntimeException | SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw databaseError(e);
        }
    }

    public static Map<String, Object> getSettings(String key) {
        String sql = "SELECT setting_value FROM platform_settings WHERE setting_key = ?";
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key);
            try (ResultSet rows = statement.executeQuery()) {
                if (rows.next()) return AdminJson.parseObject(rows.getString(1));
                return "marketplace".equals(key) ? defaultSettings() : new LinkedHashMap<>();
            }
        } catch (SQLException e) {
            throw databaseError(e);
        }
    }

    public static void saveSettings(String key, Map<String, Object> value) {
        String sql = "INSERT INTO platform_settings (setting_key, setting_value) VALUES (?, CAST(? AS JSON)) "
                + "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), updated_at = CURRENT_TIMESTAMP";
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key);
            statement.setString(2, AdminJson.stringify(value));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw databaseError(e);
        }
    }

    public static Map<String, Object> bootstrap() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String type : List.of("users", "sellers", "products", "orders", "transactions",
                "orderItems", "depositTransactions", "commissionRules", "reviews")) {
            result.put(type, findAll(type));
        }
        result.put("settings", getSettings("marketplace"));
        result.put("dashboardSeries", dashboardSeries());
        return result;
    }

    private static List<Map<String, Object>> users(Connection c) throws SQLException {
        String sql = "SELECT u.user_id, u.full_name, u.email, u.phone, u.status, u.created_at, "
                + "(SELECT COUNT(*) FROM orders o WHERE o.user_id=u.user_id) AS order_count, "
                + "(SELECT COALESCE(SUM(o.total_amount),0) FROM orders o WHERE o.user_id=u.user_id "
                + "AND UPPER(o.payment_status) IN ('PAID','SETTLED')) AS total_spent "
                + "FROM users u WHERE UPPER(COALESCE(u.role,''))='CUSTOMER' ORDER BY u.created_at DESC, u.user_id";
        List<Map<String, Object>> result = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long userId = rs.getLong("user_id");
                String name = rs.getString("full_name");
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", customerId(userId)); row.put("userId", userId);
                row.put("name", empty(name)); row.put("email", empty(rs.getString("email")));
                row.put("phone", empty(rs.getString("phone")));
                row.put("orders", rs.getLong("order_count"));
                row.put("totalSpent", amount(rs, "total_spent"));
                row.put("joined", date(rs.getTimestamp("created_at")));
                row.put("status", userStatus(rs.getString("status")));
                row.put("initials", initials(name)); row.put("color", color(userId));
                result.add(row);
            }
        }
        return result;
    }

    private static List<Map<String, Object>> sellers(Connection c) throws SQLException {
        String sql = "SELECT s.shop_id,s.owner_user_id,s.shop_name,s.description,s.contact_phone,s.province,s.district,"
                + "s.average_rating,s.verification_status,s.status,s.created_at,u.full_name,u.email,"
                + "(SELECT COUNT(*) FROM shop_products sp WHERE sp.shop_id=s.shop_id) product_count,"
                + "(SELECT COUNT(DISTINCT oi.order_id) FROM order_items oi WHERE oi.shop_id=s.shop_id) order_count,"
                + "(SELECT COALESCE(SUM(oi.line_total),0) FROM order_items oi JOIN orders o ON o.order_id=oi.order_id "
                + "WHERE oi.shop_id=s.shop_id AND UPPER(o.payment_status) IN ('PAID','SETTLED')) revenue,"
                + "(SELECT COUNT(*) FROM reviews r WHERE r.shop_id=s.shop_id) review_count,"
                + "(SELECT GROUP_CONCAT(DISTINCT sp.printing_method ORDER BY sp.printing_method SEPARATOR ',') "
                + "FROM shop_products sp WHERE sp.shop_id=s.shop_id) technologies "
                + "FROM print_shops s LEFT JOIN users u ON u.user_id=s.owner_user_id ORDER BY s.created_at DESC,s.shop_id";
        List<Map<String, Object>> result = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long shopId = rs.getLong("shop_id");
                String district = empty(rs.getString("district"));
                String province = empty(rs.getString("province"));
                String location = district.isBlank() ? province : district + ", " + province;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", sellerId(shopId)); row.put("shopId", shopId);
                row.put("ownerUserId", rs.getLong("owner_user_id"));
                row.put("ownerName", empty(rs.getString("full_name")));
                row.put("name", empty(rs.getString("shop_name")));
                row.put("location", location); row.put("city", province); row.put("address", location);
                row.put("phone", empty(rs.getString("contact_phone")));
                row.put("email", empty(rs.getString("email")));
                row.put("products", rs.getLong("product_count")); row.put("orders", rs.getLong("order_count"));
                row.put("revenue", amount(rs, "revenue")); row.put("rating", rs.getBigDecimal("average_rating") == null ? 0 : rs.getBigDecimal("average_rating").doubleValue());
                row.put("reviews", rs.getLong("review_count"));
                row.put("status", sellerStatus(rs.getString("status")));
                row.put("joined", date(rs.getTimestamp("created_at")));
                row.put("technology", split(rs.getString("technologies")));
                row.put("coords", List.of()); row.put("initials", initials(rs.getString("shop_name")));
                row.put("color", color(shopId)); row.put("verificationStatus", empty(rs.getString("verification_status")));
                result.add(row);
            }
        }
        return result;
    }

    private static List<Map<String, Object>> products(Connection c) throws SQLException {
        String sql = "SELECT sp.shop_product_id,sp.shop_id,sp.product_id,sp.printing_method,sp.material,sp.unit_price,"
                + "sp.stock_status,sp.is_active listing_active,p.product_name,p.description,p.is_customizable,"
                + "p.moderation_status,p.moderation_reason,p.created_at,c.category_name,s.shop_name,"
                + "COALESCE((SELECT AVG(r.rating) FROM reviews r WHERE r.product_id=p.product_id),0) rating,"
                + "(SELECT COUNT(*) FROM reviews r WHERE r.product_id=p.product_id) review_count "
                + "FROM shop_products sp JOIN products p ON p.product_id=sp.product_id "
                + "LEFT JOIN categories c ON c.category_id=p.category_id LEFT JOIN print_shops s ON s.shop_id=sp.shop_id "
                + "ORDER BY p.created_at DESC,sp.shop_product_id";
        List<Map<String, Object>> result = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long listingId = rs.getLong("shop_product_id");
                long productId = rs.getLong("product_id");
                String moderation = upper(rs.getString("moderation_status"));
                boolean listingActive = rs.getBoolean("listing_active");
                String status = switch (moderation) {
                    case "PENDING_REVIEW", "PENDING" -> "Pending Review";
                    case "REJECTED" -> "Rejected";
                    case "DRAFT" -> "Draft";
                    default -> listingActive ? "Published" : "Hidden";
                };
                String name = rs.getString("product_name");
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", listingId(listingId)); row.put("shopProductId", listingId);
                row.put("productId", productId); row.put("sellerId", sellerId(rs.getLong("shop_id")));
                row.put("sellerName", empty(rs.getString("shop_name")));
                row.put("name", empty(name)); row.put("category", empty(rs.getString("category_name")));
                row.put("type", rs.getBoolean("is_customizable") ? "CUSTOM PRINTING SERVICE" : "READY-MADE");
                row.put("price", amount(rs, "unit_price")); row.put("status", status);
                row.put("created", date(rs.getTimestamp("created_at")));
                row.put("description", empty(rs.getString("description")));
                row.put("stock", empty(rs.getString("stock_status")));
                row.put("technologies", split(rs.getString("printing_method")));
                row.put("materials", split(rs.getString("material")));
                row.put("rating", rs.getDouble("rating")); row.put("reviews", rs.getLong("review_count"));
                row.put("rejectionReason", empty(rs.getString("moderation_reason")));
                row.put("initials", initials(name)); row.put("color", color(listingId));
                result.add(row);
            }
        }
        return result;
    }

    private static List<Map<String, Object>> orders(Connection c) throws SQLException {
        String sql = "SELECT o.order_id,o.order_code,o.user_id,o.subtotal,o.shipping_fee,o.total_amount,o.currency,"
                + "o.status,o.payment_status,o.shipping_status,o.placed_at,u.full_name,"
                + "(SELECT oi.shop_id FROM order_items oi WHERE oi.order_id=o.order_id ORDER BY oi.order_item_id LIMIT 1) shop_id,"
                + "(SELECT oi.shop_product_id FROM order_items oi WHERE oi.order_id=o.order_id ORDER BY oi.order_item_id LIMIT 1) shop_product_id,"
                + "(SELECT p.status FROM payments p WHERE p.order_id=o.order_id ORDER BY p.created_at DESC,p.payment_id DESC LIMIT 1) latest_payment "
                + "FROM orders o LEFT JOIN users u ON u.user_id=o.user_id ORDER BY o.placed_at DESC,o.order_id DESC";
        List<Map<String, Object>> result = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long orderId = rs.getLong("order_id");
                Map<String, Object> row = new LinkedHashMap<>();
                String orderCode = empty(rs.getString("order_code"));
                row.put("id", orderCode.isBlank() ? "ORD-" + orderId : orderCode);
                row.put("orderId", orderId); row.put("customerId", customerId(rs.getLong("user_id")));
                long shopId = rs.getLong("shop_id");
                row.put("sellerId", rs.wasNull() ? "" : sellerId(shopId));
                long listingId = rs.getLong("shop_product_id");
                row.put("productId", rs.wasNull() ? "" : listingId(listingId));
                row.put("customerName", empty(rs.getString("full_name")));
                row.put("amount", amount(rs, "total_amount"));
                row.put("payment", paymentLabel(rs.getString("payment_status")));
                row.put("status", title(rs.getString("status")));
                row.put("created", dateTime(rs.getTimestamp("placed_at")));
                long subtotal = amount(rs, "subtotal");
                long shipping = amount(rs, "shipping_fee");
                long total = amount(rs, "total_amount");
                row.put("breakdown", Map.of("subtotal", subtotal, "shipping", shipping,
                        "discount", Math.max(0L, subtotal + shipping - total), "total", total));
                row.put("items", orderItems(c, orderId));
                row.put("timeline", orderTimeline(c, orderId, rs.getString("status")));
                row.put("shippingInfo", shippingInfo(c, orderId, rs.getString("shipping_status")));
                row.put("currency", empty(rs.getString("currency")));
                result.add(row);
            }
        }
        return result;
    }

    private static List<Map<String, Object>> orderItems(Connection c) throws SQLException {
        String sql = "SELECT oi.order_item_id,oi.order_code,oi.order_id,o.order_code order_number,oi.product_id,"
                + "oi.shop_product_id,oi.shop_id,oi.product_name_snapshot,oi.shop_name_snapshot,oi.quantity,"
                + "oi.unit_price,oi.line_total,oi.status FROM order_items oi LEFT JOIN orders o ON o.order_id=oi.order_id "
                + "ORDER BY oi.order_item_id DESC";
        List<Map<String, Object>> result = new ArrayList<>();
        // order_items has no order_code column in the application schema; use the joined order code.
        sql = "SELECT oi.order_item_id,oi.order_id,o.order_code order_number,oi.product_id,oi.shop_product_id,oi.shop_id,"
                + "oi.product_name_snapshot,oi.shop_name_snapshot,oi.quantity,oi.unit_price,oi.line_total,oi.status "
                + "FROM order_items oi LEFT JOIN orders o ON o.order_id=oi.order_id ORDER BY oi.order_item_id DESC";
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", "ITM-" + rs.getLong("order_item_id"));
                row.put("orderId", empty(rs.getString("order_number")));
                row.put("productId", listingId(rs.getLong("shop_product_id")));
                row.put("name", empty(rs.getString("product_name_snapshot")));
                row.put("sellerName", empty(rs.getString("shop_name_snapshot")));
                row.put("quantity", rs.getInt("quantity")); row.put("unitPrice", amount(rs, "unit_price"));
                row.put("lineTotal", amount(rs, "line_total")); row.put("status", title(rs.getString("status")));
                result.add(row);
            }
        }
        return result;
    }

    private static List<Map<String, Object>> orderItems(Connection c, long orderId) throws SQLException {
        String sql = "SELECT order_item_id,shop_product_id,product_name_snapshot,quantity,unit_price,line_total "
                + "FROM order_items WHERE order_id=? ORDER BY order_item_id";
        List<Map<String, Object>> result = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", "ITM-" + rs.getLong("order_item_id"));
                    item.put("productId", listingId(rs.getLong("shop_product_id")));
                    item.put("name", empty(rs.getString("product_name_snapshot")));
                    item.put("quantity", rs.getInt("quantity")); item.put("unitPrice", amount(rs, "unit_price"));
                    item.put("lineTotal", amount(rs, "line_total")); result.add(item);
                }
            }
        }
        return result;
    }

    private static List<String> orderTimeline(Connection c, long orderId, String current) throws SQLException {
        String sql = "SELECT new_status FROM order_status_history WHERE order_id=? ORDER BY changed_at,history_id";
        List<String> result = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) result.add(title(rs.getString(1))); }
        }
        if (result.isEmpty()) {
            result.add("Đặt hàng");
            if (current != null && !current.isBlank()) result.add(title(current));
        }
        return result;
    }

    private static String shippingInfo(Connection c, long orderId, String fallback) throws SQLException {
        String sql = "SELECT carrier,tracking_number,status,delivered_at FROM shipments WHERE order_id=? "
                + "ORDER BY shipment_id LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String carrier = empty(rs.getString("carrier"));
                    String tracking = empty(rs.getString("tracking_number"));
                    String status = title(rs.getString("status"));
                    String delivered = date(rs.getTimestamp("delivered_at"));
                    return String.join(" · ", List.of(carrier, tracking, status, delivered).stream().filter(s -> !s.isBlank()).toList());
                }
            }
        }
        return fallback == null || fallback.isBlank() ? "Chưa có thông tin vận chuyển" : title(fallback);
    }

    private static List<Map<String, Object>> transactions(Connection c) throws SQLException {
        String sql = "SELECT p.payment_id,p.order_id,p.amount,p.status,p.payment_method,p.provider,"
                + "p.created_at,p.paid_at,o.order_code,o.user_id,"
                + "(SELECT oi.shop_id FROM order_items oi WHERE oi.order_id=o.order_id ORDER BY oi.order_item_id LIMIT 1) shop_id "
                + "FROM payments p JOIN orders o ON o.order_id=p.order_id ORDER BY COALESCE(p.paid_at,p.created_at) DESC,p.payment_id DESC";
        long rate = decimal(getSettings("marketplace").get("commissionRate"), 10L);
        List<Map<String, Object>> result = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long paymentId = rs.getLong("payment_id"); long gross = amount(rs, "amount");
                String statusRaw = upper(rs.getString("status"));
                boolean paid = List.of("PAID", "SUCCESS", "SETTLED", "COMPLETED").contains(statusRaw);
                long commission = paid ? Math.round(gross * rate / 100.0) : 0;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", "TXN-" + paymentId); row.put("paymentId", paymentId);
                row.put("orderId", empty(rs.getString("order_code")));
                row.put("customerId", customerId(rs.getLong("user_id")));
                long shop = rs.getLong("shop_id"); row.put("sellerId", rs.wasNull() ? "" : sellerId(shop));
                row.put("gross", gross); row.put("commission", commission);
                row.put("sellerRevenue", paid ? gross - commission : 0);
                row.put("refund", "REFUNDED".equals(statusRaw) ? gross : 0);
                row.put("date", date(rs.getTimestamp("paid_at") == null ? rs.getTimestamp("created_at") : rs.getTimestamp("paid_at")));
                row.put("status", paymentStatus(statusRaw));
                row.put("method", empty(rs.getString("payment_method"))); row.put("provider", empty(rs.getString("provider")));
                result.add(row);
            }
        }
        return result;
    }

    private static List<Map<String, Object>> commissionRules(Connection c) throws SQLException {
        String sql = "SELECT rule_id,rule_name,seller_scope,shop_id,product_type,commission_rate,is_enabled "
                + "FROM commission_rules ORDER BY rule_id";
        List<Map<String, Object>> result = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getString("rule_id")); row.put("name", empty(rs.getString("rule_name")));
                long shopId = rs.getLong("shop_id");
                row.put("scope", rs.wasNull() ? empty(rs.getString("seller_scope")) : sellerId(shopId));
                row.put("productType", productTypeLabel(rs.getString("product_type")));
                row.put("rate", rs.getBigDecimal("commission_rate").doubleValue());
                row.put("enabled", rs.getBoolean("is_enabled")); result.add(row);
            }
        }
        return result;
    }

    private static List<Map<String, Object>> reviews(Connection c) throws SQLException {
        String sql = "SELECT r.review_id,r.user_id,r.shop_id,r.product_id,r.rating,r.review_text,r.is_published,"
                + "u.full_name,p.product_name,s.shop_name FROM reviews r LEFT JOIN users u ON u.user_id=r.user_id "
                + "LEFT JOIN products p ON p.product_id=r.product_id LEFT JOIN print_shops s ON s.shop_id=r.shop_id "
                + "ORDER BY r.review_id DESC";
        List<Map<String, Object>> result = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", "REV-" + rs.getLong("review_id"));
                row.put("userId", customerId(rs.getLong("user_id")));
                row.put("userName", empty(rs.getString("full_name")));
                row.put("sellerId", sellerId(rs.getLong("shop_id")));
                row.put("sellerName", empty(rs.getString("shop_name")));
                row.put("productId", "PRD-" + rs.getLong("product_id"));
                row.put("productName", empty(rs.getString("product_name")));
                row.put("rating", rs.getInt("rating")); row.put("text", empty(rs.getString("review_text")));
                row.put("visible", rs.getBoolean("is_published"));
                row.put("status", rs.getBoolean("is_published") ? "Published" : "Hidden");
                result.add(row);
            }
        }
        return result;
    }

    private static void persistUpdate(Connection c, String type, Map<String, Object> row) throws SQLException {
        switch (type) {
            case "users" -> {
                String sql = "UPDATE users SET full_name=?,email=?,phone=?,status=? WHERE user_id=?";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setString(1, string(row.get("name"))); ps.setString(2, string(row.get("email")));
                    ps.setString(3, string(row.get("phone"))); ps.setString(4, userStatusDb(row.get("status")));
                    ps.setLong(5, parseId(row.get("id"), "CUS-")); requireUpdated(ps.executeUpdate());
                }
            }
            case "sellers" -> {
                String sql = "UPDATE print_shops SET status=? WHERE shop_id=?";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setString(1, sellerStatusDb(row.get("status")));
                    ps.setLong(2, parseId(row.get("id"), "SEL-")); requireUpdated(ps.executeUpdate());
                }
            }
            case "products" -> {
                long listingId = parseId(row.get("id"), "SP-");
                long productId;
                try (PreparedStatement ps = c.prepareStatement("SELECT product_id FROM shop_products WHERE shop_product_id=?")) {
                    ps.setLong(1, listingId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Product listing not found: " + row.get("id"));
                        productId = rs.getLong(1);
                    }
                }
                String status = string(row.get("status"));
                String moderation = switch (status) {
                    case "Pending Review" -> "PENDING_REVIEW";
                    case "Rejected" -> "REJECTED";
                    case "Draft" -> "DRAFT";
                    case "Published", "Hidden" -> "APPROVED";
                    default -> throw new IllegalArgumentException("Invalid product status: " + status);
                };
                boolean active = "Published".equals(status);
                String reason = "Rejected".equals(status) ? string(row.get("rejectionReason")) : null;
                try (PreparedStatement ps = c.prepareStatement("UPDATE products SET moderation_status=?,moderation_reason=?,is_active=? WHERE product_id=?")) {
                    ps.setString(1, moderation); ps.setString(2, reason); ps.setBoolean(3, active); ps.setLong(4, productId);
                    requireUpdated(ps.executeUpdate());
                }
                try (PreparedStatement ps = c.prepareStatement("UPDATE shop_products SET is_active=? WHERE shop_product_id=?")) {
                    ps.setBoolean(1, active); ps.setLong(2, listingId); requireUpdated(ps.executeUpdate());
                }
            }
            case "commissionRules" -> {
                String sql = "UPDATE commission_rules SET rule_name=?,seller_scope=?,product_type=?,commission_rate=?,is_enabled=?,updated_at=CURRENT_TIMESTAMP WHERE rule_id=?";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    bindRule(ps, row, false); ps.setString(6, string(row.get("id"))); requireUpdated(ps.executeUpdate());
                }
            }
            default -> throw new IllegalArgumentException("Records of type " + type + " cannot be updated.");
        }
    }

    private static void createUser(Connection c, String id, Map<String, Object> row) throws SQLException {
        long explicitId = parseIdOrZero(id, "CUS-");
        String sql = explicitId > 0
                ? "INSERT INTO users(user_id,full_name,email,phone,role,status,created_at) VALUES(?,?,?,?,'CUSTOMER',?,CURRENT_TIMESTAMP)"
                : "INSERT INTO users(full_name,email,phone,role,status,created_at) VALUES(?,?,?,'CUSTOMER',?,CURRENT_TIMESTAMP)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int index = 1;
            if (explicitId > 0) ps.setLong(index++, explicitId);
            ps.setString(index++, string(row.get("name"))); ps.setString(index++, string(row.get("email")));
            ps.setString(index++, string(row.get("phone"))); ps.setString(index, userStatusDb(row.get("status")));
            ps.executeUpdate();
            if (explicitId == 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) row.put("id", customerId(keys.getLong(1)));
                }
            }
        }
    }

    private static void saveCommissionRule(Connection c, String id, Map<String, Object> row) throws SQLException {
        String sql = "INSERT INTO commission_rules(rule_id,rule_name,seller_scope,shop_id,product_type,commission_rate,is_enabled) "
                + "VALUES(?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE rule_name=VALUES(rule_name),seller_scope=VALUES(seller_scope),"
                + "shop_id=VALUES(shop_id),product_type=VALUES(product_type),commission_rate=VALUES(commission_rate),"
                + "is_enabled=VALUES(is_enabled),updated_at=CURRENT_TIMESTAMP";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id); ps.setString(2, string(row.getOrDefault("name", id)));
            ps.setString(3, string(row.getOrDefault("scope", "All sellers")));
            Long shopId = sellerScopeId(row.get("scope"));
            if (shopId == null) ps.setNull(4, java.sql.Types.BIGINT); else ps.setLong(4, shopId);
            ps.setString(5, productTypeDb(row.get("productType")));
            ps.setBigDecimal(6, new java.math.BigDecimal(String.valueOf(row.getOrDefault("rate", 0))));
            ps.setBoolean(7, Boolean.parseBoolean(String.valueOf(row.getOrDefault("enabled", false))));
            ps.executeUpdate();
        }
    }

    private static void bindRule(PreparedStatement ps, Map<String, Object> row, boolean includeShop) throws SQLException {
        ps.setString(1, string(row.getOrDefault("name", row.get("id"))));
        ps.setString(2, string(row.getOrDefault("scope", "All sellers")));
        ps.setString(3, productTypeDb(row.get("productType")));
        ps.setBigDecimal(4, new java.math.BigDecimal(String.valueOf(row.getOrDefault("rate", 0))));
        ps.setBoolean(5, Boolean.parseBoolean(String.valueOf(row.getOrDefault("enabled", false))));
    }

    private static Map<String, Object> dashboardSeries() {
        Map<String, Object> result = new LinkedHashMap<>();
        try (Connection c = connection()) {
            String sql = "SELECT DATE(COALESCE(p.paid_at,p.created_at)) paid_date,SUM(p.amount) gross FROM payments p "
                    + "WHERE UPPER(p.status) IN ('PAID','SUCCESS','SETTLED','COMPLETED') "
                    + "AND COALESCE(p.paid_at,p.created_at)>=DATE_SUB(CURRENT_DATE,INTERVAL 1 YEAR) "
                    + "GROUP BY DATE(COALESCE(p.paid_at,p.created_at)) ORDER BY paid_date";
            Map<LocalDate, Long> byDate = new LinkedHashMap<>();
            try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) byDate.put(rs.getDate("paid_date").toLocalDate(), amount(rs, "gross"));
            }
            result.put("7 days", series(byDate, 7, 1)); result.put("30 days", series(byDate, 12, 3));
            result.put("3 months", series(byDate, 12, 7)); result.put("6 months", series(byDate, 12, 14));
            result.put("1 year", series(byDate, 12, 30));
            return result;
        } catch (SQLException e) {
            throw databaseError(e);
        }
    }

    private static List<Long> series(Map<LocalDate, Long> values, int points, int daysPerBucket) {
        List<Long> result = new ArrayList<>();
        LocalDate end = LocalDate.now();
        for (int i = points - 1; i >= 0; i--) {
            LocalDate from = end.minusDays((long) (i + 1) * daysPerBucket - 1);
            LocalDate to = end.minusDays((long) i * daysPerBucket);
            long total = 0;
            for (Map.Entry<LocalDate, Long> entry : values.entrySet()) {
                if (!entry.getKey().isBefore(from) && !entry.getKey().isAfter(to)) total += entry.getValue();
            }
            result.add(total);
        }
        return result;
    }

    private static Map<String, Object> defaultSettings() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("platformName", "PrintKraft"); result.put("currency", "VND · Việt Nam Đồng");
        result.put("timezone", "Asia/Ho_Chi_Minh (UTC+7)"); result.put("requiredDeposit", 0);
        result.put("commissionRate", 10); result.put("requireProductReview", true);
        result.put("printingTechnologies", List.of("FDM", "SLA", "SLS", "MJF"));
        result.put("materials", List.of("PLA", "PETG", "ABS", "Resin tiêu chuẩn", "Nylon PA12"));
        result.put("surfaceQualities", List.of("Tiêu chuẩn", "Mịn", "Siêu mịn"));
        return result;
    }

    private static void requireUpdated(int rows) {
        if (rows == 0) throw new IllegalArgumentException("Business record was not updated.");
    }

    private static String userStatus(String status) {
        return switch (upper(status)) {
            case "LOCKED", "SUSPENDED", "BLOCKED" -> "Suspended";
            case "PENDING", "INACTIVE", "UNVERIFIED" -> "Inactive";
            default -> "Active";
        };
    }
    private static String sellerStatus(String status) { return switch (upper(status)) {
        case "SUSPENDED", "LOCKED", "BLOCKED" -> "Suspended";
        case "INACTIVE", "DISABLED", "PENDING" -> "Inactive";
        default -> "Active";
    }; }
    private static String userStatusDb(Object status) { return switch (string(status)) {
        case "Suspended" -> "LOCKED"; case "Inactive" -> "PENDING"; default -> "ACTIVE";
    }; }
    private static String sellerStatusDb(Object status) { return switch (string(status)) {
        case "Suspended" -> "SUSPENDED"; case "Inactive" -> "INACTIVE"; default -> "ACTIVE";
    }; }
    private static String paymentLabel(String status) { return switch (upper(status)) {
        case "PAID", "SUCCESS", "SETTLED", "COMPLETED" -> "Đã thanh toán";
        case "REFUNDED" -> "Đã hoàn tiền"; case "PARTIALLY_REFUNDED" -> "Hoàn tiền một phần";
        case "FAILED" -> "Thanh toán thất bại"; default -> "Chưa thanh toán";
    }; }
    private static String paymentStatus(String status) { return switch (status) {
        case "PAID", "SUCCESS", "SETTLED", "COMPLETED" -> "Settled";
        case "REFUNDED", "PARTIALLY_REFUNDED" -> "Refunded"; case "FAILED" -> "Failed"; default -> "Pending";
    }; }
    private static String productTypeDb(Object value) {
        String type = string(value);
        if (type.isBlank() || "All products".equalsIgnoreCase(type)) return "ALL";
        return type;
    }
    private static String productTypeLabel(String value) { return "ALL".equalsIgnoreCase(value) ? "All products" : value; }
    private static Long sellerScopeId(Object scope) {
        String value = string(scope);
        return value.startsWith("SEL-") ? parseIdOrZero(value, "SEL-") : null;
    }
    private static String customerId(long id) { return String.format(Locale.ROOT, "CUS-%05d", id); }
    private static String sellerId(long id) { return String.format(Locale.ROOT, "SEL-%04d", id); }
    private static String listingId(long id) { return "SP-" + id; }
    private static long parseId(Object value, String prefix) {
        long id = parseIdOrZero(string(value), prefix);
        if (id <= 0) throw new IllegalArgumentException("Invalid business id: " + value);
        return id;
    }
    private static long parseIdOrZero(String value, String prefix) {
        String digits = value != null && value.startsWith(prefix) ? value.substring(prefix.length()) : value;
        try { return Long.parseLong(digits); } catch (Exception ignored) { return 0; }
    }
    private static long amount(ResultSet rs, String column) throws SQLException {
        java.math.BigDecimal value = rs.getBigDecimal(column);
        return value == null ? 0 : value.longValue();
    }
    private static String date(Timestamp timestamp) { return timestamp == null ? "" : timestamp.toLocalDateTime().toLocalDate().toString(); }
    private static String dateTime(Timestamp timestamp) { return timestamp == null ? "" : timestamp.toLocalDateTime().toString(); }
    private static String empty(String value) { return value == null ? "" : value; }
    private static String string(Object value) { return value == null ? "" : String.valueOf(value); }
    private static String upper(String value) { return value == null ? "" : value.trim().toUpperCase(Locale.ROOT); }
    private static List<String> split(String value) {
        if (value == null || value.isBlank()) return List.of();
        return java.util.Arrays.stream(value.split("[,|]" )).map(String::trim).filter(s -> !s.isBlank()).toList();
    }
    private static String initials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] words = name.trim().split("\\s+");
        return words.length == 1 ? words[0].substring(0, Math.min(2, words[0].length())).toUpperCase(Locale.ROOT)
                : (words[0].substring(0, 1) + words[words.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }
    private static String color(long id) { return COLORS.get((int) Math.floorMod(id, COLORS.size())); }
    private static String title(String value) {
        if (value == null || value.isBlank()) return "Unknown";
        String[] words = value.replace('_', ' ').trim().toLowerCase(Locale.ROOT).split("\\s+");
        List<String> result = new ArrayList<>();
        for (String word : words) result.add(word.isEmpty() ? word : word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1));
        return String.join(" ", result);
    }
    private static long decimal(Object value, long fallback) {
        try { return new java.math.BigDecimal(String.valueOf(value)).longValue(); } catch (Exception ignored) { return fallback; }
    }

    private static AdminDatabaseException databaseError(SQLException error) {
        return new AdminDatabaseException("Không đọc/ghi được các bảng nghiệp vụ PrintKraft. "
                + "Kiểm tra cấu hình PRINTKRAFT_DB_URL/PRINTKRAFT_DB_USER/PRINTKRAFT_DB_PASSWORD "
                + "và chạy printkraft_admin_seed.sql để cài migration/cấu hình Admin.", error);
    }
}
