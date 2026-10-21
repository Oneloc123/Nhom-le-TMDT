package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Order;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.OrderItem;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.OrderStatus;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.ProductionUpdate;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Shipment;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.StatusHistory;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class OrderDao {

    private static final SecureRandom RANDOM = new SecureRandom();

    private OrderDao() {
    }

    public static Order createOrder(long userId, Long addressId, String recipientName,
                                    String recipientPhone, String shippingAddress,
                                    List<OrderItem> items, BigDecimal subtotal,
                                    BigDecimal shippingFee, String currency) {
        BigDecimal total = subtotal.add(shippingFee);
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long orderId;
                String orderCode;
                int attempts = 0;
                while (true) {
                    orderCode = generateOrderCode();
                    orderId = insertOrder(conn, userId, addressId, orderCode, recipientName,
                            recipientPhone, shippingAddress, subtotal, shippingFee, total, currency);
                    if (orderId > 0) {
                        break;
                    }
                    if (++attempts > 3) {
                        throw new SQLException("Không tạo được mã đơn hàng duy nhất.");
                    }
                }
                for (OrderItem item : items) {
                    insertItem(conn, orderId, item);
                }
                insertHistory(conn, orderId, userId, null, OrderStatus.PENDING_PAYMENT,
                        "Khách hàng đã đặt hàng");
                conn.commit();
                return findByIdForUser(orderId, userId).orElseThrow();
            } catch (SQLException | DataAccessException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static Optional<Order> findByCodeForUser(String orderCode, long userId) {
        if (orderCode == null || orderCode.isBlank()) {
            return Optional.empty();
        }
        String sql = "SELECT " + ORDER_COLUMNS + " FROM orders o"
                + " LEFT JOIN addresses a ON a.address_id = o.address_id"
                + " LEFT JOIN users u ON u.user_id = o.user_id"
                + " WHERE o.order_code = ? AND o.user_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderCode.trim());
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapOrder(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static Optional<Order> findByIdForUser(long orderId, long userId) {
        String sql = "SELECT " + ORDER_COLUMNS + " FROM orders o"
                + " LEFT JOIN addresses a ON a.address_id = o.address_id"
                + " LEFT JOIN users u ON u.user_id = o.user_id"
                + " WHERE o.order_id = ? AND o.user_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapOrder(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static List<Order> listByUser(long userId) {
        String sql = "SELECT " + ORDER_COLUMNS + " FROM orders o"
                + " LEFT JOIN addresses a ON a.address_id = o.address_id"
                + " LEFT JOIN users u ON u.user_id = o.user_id"
                + " WHERE o.user_id = ? ORDER BY o.placed_at DESC, o.order_id DESC";
        List<Order> orders = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapOrder(rs));
                }
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
        for (Order order : orders) {
            order.getItems().addAll(listItemsForOrder(order.getOrderId()));
        }
        return orders;
    }

    /**
     * Lịch sử đơn hàng của MỘT khách: tìm theo mã đơn, lọc theo trạng thái
     * (allow-list), phân trang phía server. Mọi điều kiện đều tham số hoá và
     * luôn kèm {@code o.user_id = ?} nên không thể đọc đơn của người khác.
     */
    public static Map<String, Object> searchByUser(long userId, String orderCode,
                                                   String status, int page, int pageSize) {
        int size = Math.min(Math.max(1, pageSize), 50);
        StringBuilder where = new StringBuilder(" WHERE o.user_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(userId);

        if (orderCode != null && !orderCode.isBlank()) {
            String term = orderCode.trim();
            if (term.length() > 40) {
                throw new IllegalArgumentException("Mã đơn tìm kiếm tối đa 40 ký tự.");
            }
            where.append(" AND o.order_code LIKE ? ESCAPE '!' ");
            args.add("%" + term.replace("!", "!!").replace("%", "!%")
                    .replace("_", "!_") + "%");
        }
        if (status != null && !status.isBlank() && !"All".equalsIgnoreCase(status)) {
            List<String> allowed = List.of(OrderStatus.PENDING_PAYMENT, OrderStatus.CONFIRMED,
                    OrderStatus.PROCESSING, OrderStatus.SHIPPED, OrderStatus.COMPLETED,
                    OrderStatus.CANCELLED);
            if (!allowed.contains(status)) {
                throw new IllegalArgumentException("Trạng thái đơn hàng không hợp lệ.");
            }
            where.append(" AND o.status = ?");
            args.add(status);
        }

        long total;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM orders o" + where)) {
            for (int i = 0; i < args.size(); i++) {
                ps.setObject(i + 1, args.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                total = rs.next() ? rs.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }

        int pages = Math.max(1, (int) Math.ceil((double) total / size));
        int current = Math.min(Math.max(1, page), pages);

        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add((long) (current - 1) * size);
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT " + ORDER_COLUMNS + " FROM orders o"
                + " LEFT JOIN addresses a ON a.address_id = o.address_id"
                + " LEFT JOIN users u ON u.user_id = o.user_id"
                + where + " ORDER BY o.placed_at DESC, o.order_id DESC LIMIT ? OFFSET ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < pageArgs.size(); i++) {
                ps.setObject(i + 1, pageArgs.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapOrder(rs));
                }
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
        for (Order order : orders) {
            order.getItems().addAll(listItemsForOrder(order.getOrderId()));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", orders);
        result.put("total", total);
        result.put("page", current);
        result.put("pageSize", size);
        result.put("pages", pages);
        return result;
    }

    public static void loadDetails(Order order) {
        List<OrderItem> items = listItemsForOrder(order.getOrderId());
        order.getItems().clear();
        order.getItems().addAll(items);
        order.getTimeline().clear();
        order.getTimeline().addAll(listHistory(order.getOrderId()));
        order.getShipments().clear();
        order.getShipments().addAll(listShipments(order.getOrderId()));
        order.getProductionUpdates().clear();
        order.getProductionUpdates().addAll(listProductionUpdates(order.getOrderId()));
    }

    public static List<OrderItem> listItemsForOrder(long orderId) {
        try (Connection conn = Database.getConnection()) {
            return listItems(conn, orderId);
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static List<StatusHistory> listHistory(long orderId) {
        String sql = "SELECT history_id, order_id, changed_by_user_id, old_status, new_status,"
                + " note, changed_at FROM order_status_history WHERE order_id = ?"
                + " ORDER BY changed_at, history_id";
        List<StatusHistory> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StatusHistory h = new StatusHistory();
                    h.setHistoryId(rs.getLong("history_id"));
                    h.setOrderId(rs.getLong("order_id"));
                    long by = rs.getLong("changed_by_user_id");
                    h.setChangedByUserId(rs.wasNull() ? null : by);
                    h.setOldStatus(rs.getString("old_status"));
                    h.setNewStatus(rs.getString("new_status"));
                    h.setNote(rs.getString("note"));
                    h.setChangedAt(toLocalDateTime(rs.getTimestamp("changed_at")));
                    list.add(h);
                }
            }
            return list;
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static List<Shipment> listShipments(long orderId) {
        String sql = "SELECT sh.shipment_id, sh.order_id, sh.shop_id, s.shop_name, sh.carrier,"
                + " sh.tracking_number, sh.status, sh.shipped_at, sh.delivered_at"
                + " FROM shipments sh LEFT JOIN print_shops s ON s.shop_id = sh.shop_id"
                + " WHERE sh.order_id = ? ORDER BY sh.shipment_id";
        List<Shipment> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Shipment sh = new Shipment();
                    sh.setShipmentId(rs.getLong("shipment_id"));
                    sh.setOrderId(rs.getLong("order_id"));
                    long shopId = rs.getLong("shop_id");
                    sh.setShopId(rs.wasNull() ? null : shopId);
                    sh.setShopName(rs.getString("shop_name"));
                    sh.setCarrier(rs.getString("carrier"));
                    sh.setTrackingNumber(rs.getString("tracking_number"));
                    sh.setStatus(rs.getString("status"));
                    sh.setShippedAt(toLocalDateTime(rs.getTimestamp("shipped_at")));
                    sh.setDeliveredAt(toLocalDateTime(rs.getTimestamp("delivered_at")));
                    list.add(sh);
                }
            }
            return list;
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static List<ProductionUpdate> listProductionUpdates(long orderId) {
        String sql = "SELECT pu.production_update_id, pu.order_item_id, pu.shop_id, s.shop_name,"
                + " pu.stage, pu.progress_percent, pu.message, pu.created_at"
                + " FROM production_updates pu"
                + " LEFT JOIN print_shops s ON s.shop_id = pu.shop_id"
                + " JOIN order_items oi ON oi.order_item_id = pu.order_item_id"
                + " WHERE oi.order_id = ? ORDER BY pu.created_at DESC";
        List<ProductionUpdate> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ProductionUpdate pu = new ProductionUpdate();
                    pu.setProductionUpdateId(rs.getLong("production_update_id"));
                    pu.setOrderItemId(rs.getLong("order_item_id"));
                    long shopId = rs.getLong("shop_id");
                    pu.setShopId(rs.wasNull() ? null : shopId);
                    pu.setShopName(rs.getString("shop_name"));
                    pu.setStage(rs.getString("stage"));
                    pu.setProgressPercent(rs.getInt("progress_percent"));
                    pu.setMessage(rs.getString("message"));
                    pu.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
                    list.add(pu);
                }
            }
            return list;
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static void updateStatus(long orderId, long userId, String newStatus,
                                    String paymentStatusOrNull, String note) {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                applyStatusChange(conn, orderId, userId, newStatus, paymentStatusOrNull, note);
                conn.commit();
            } catch (SQLException | DataAccessException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static void applyStatusChange(Connection conn, long orderId, long userId,
                                         String newStatus, String paymentStatusOrNull, String note)
            throws SQLException {
        String current = lockStatus(conn, orderId, userId);
        if (current == null) {
            throw new DataAccessException("Đơn hàng không tồn tại hoặc bạn không có quyền.");
        }
        if (!OrderStatus.canTransition(current, newStatus)) {
            throw new DataAccessException("Không thể chuyển trạng thái từ \""
                    + current + "\" sang \"" + newStatus + "\".");
        }
        String sql = "UPDATE orders SET status = ?"
                + (paymentStatusOrNull == null ? "" : ", payment_status = ?")
                + (OrderStatus.CANCELLED.equals(newStatus) ? ", cancelled_at = NOW()" : "")
                + (OrderStatus.COMPLETED.equals(newStatus) ? ", completed_at = NOW()" : "")
                + " WHERE order_id = ? AND user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, newStatus);
            if (paymentStatusOrNull != null) {
                ps.setString(i++, paymentStatusOrNull);
            }
            ps.setLong(i++, orderId);
            ps.setLong(i, userId);
            ps.executeUpdate();
        }
        insertHistory(conn, orderId, userId, current, newStatus, note);
    }

    public static void insertPayment(Connection conn, long orderId, String paymentMethod,
                                     String provider, BigDecimal amount, String status,
                                     String failureReason) throws SQLException {
        String sql = "INSERT INTO payments (order_id, payment_method, provider,"
                + " provider_transaction_ref, checkout_url, amount, status, failure_reason,"
                + " paid_at, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, NULL, ?, ?, ?, NULL, NOW(), NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            ps.setString(2, paymentMethod);
            ps.setString(3, provider);
            ps.setString(4, "TX-" + System.currentTimeMillis() + "-" + RANDOM.nextInt(1000));
            ps.setBigDecimal(5, amount);
            ps.setString(6, status);
            ps.setString(7, failureReason);
            ps.executeUpdate();
        }
    }

    public static String lockStatus(Connection conn, long orderId, long userId) throws SQLException {
        String sql = "SELECT status FROM orders WHERE order_id = ? AND user_id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    public static void insertHistory(Connection conn, long orderId, long userId,
                                     String oldStatus, String newStatus, String note)
            throws SQLException {
        String sql = "INSERT INTO order_status_history (order_id, changed_by_user_id,"
                + " old_status, new_status, note, changed_at) VALUES (?, ?, ?, ?, ?, NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            ps.setLong(2, userId);
            ps.setString(3, oldStatus);
            ps.setString(4, newStatus);
            ps.setString(5, note);
            ps.executeUpdate();
        }
    }

    // ---------------------------------------------------------------- internal

    private static final String ORDER_COLUMNS =
            "o.order_id, o.order_code, o.user_id, o.address_id, o.subtotal, o.shipping_fee,"
                    + " o.total_amount, o.currency, o.status, o.payment_status, o.shipping_status,"
                    + " o.placed_at, o.completed_at, o.cancelled_at,"
                    + " u.full_name AS customer_name,"
                    + " a.recipient_name, a.recipient_phone,"
                    + " CONCAT(COALESCE(a.address_line, ''),"
                    + "   CASE WHEN a.district IS NULL OR a.district = '' THEN '' ELSE ', ' END,"
                    + "   COALESCE(a.district, ''),"
                    + "   CASE WHEN a.province IS NULL OR a.province = '' THEN '' ELSE ', ' END,"
                    + "   COALESCE(a.province, '')) AS shipping_address";

    private static long insertOrder(Connection conn, long userId, Long addressId, String orderCode,
                                    String recipientName, String recipientPhone,
                                    String shippingAddress, BigDecimal subtotal,
                                    BigDecimal shippingFee, BigDecimal total, String currency)
            throws SQLException {
        String sql = "INSERT INTO orders (order_code, user_id, address_id, subtotal, shipping_fee,"
                + " total_amount, currency, status, payment_status, shipping_status, placed_at)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, orderCode);
            ps.setLong(2, userId);
            if (addressId == null) {
                ps.setNull(3, java.sql.Types.BIGINT);
            } else {
                ps.setLong(3, addressId);
            }
            ps.setBigDecimal(4, subtotal);
            ps.setBigDecimal(5, shippingFee);
            ps.setBigDecimal(6, total);
            ps.setString(7, currency);
            ps.setString(8, OrderStatus.PENDING_PAYMENT);
            ps.setString(9, OrderStatus.PAYMENT_UNPAID);
            ps.setString(10, OrderStatus.SHIPPING_NONE);
            try {
                ps.executeUpdate();
            } catch (SQLException duplicate) {
                if (duplicate.getErrorCode() == 1062) {
                    return -1; // order_code trùng — thử lại
                }
                throw duplicate;
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : -1;
            }
        }
    }

    private static void insertItem(Connection conn, long orderId, OrderItem item) throws SQLException {
        String sql = "INSERT INTO order_items (order_id, product_id, shop_product_id, shop_id,"
                + " design_file_id, product_name_snapshot, shop_name_snapshot, quantity,"
                + " unit_price, line_total, status)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, orderId);
            setLongOrNull(ps, 2, item.getProductId());
            setLongOrNull(ps, 3, item.getShopProductId());
            setLongOrNull(ps, 4, item.getShopId());
            setLongOrNull(ps, 5, item.getDesignFileId());
            ps.setString(6, item.getProductName());
            ps.setString(7, item.getShopName());
            ps.setInt(8, item.getQuantity());
            ps.setBigDecimal(9, item.getUnitPrice());
            ps.setBigDecimal(10, item.getLineTotal());
            ps.setString(11, OrderStatus.PENDING_PAYMENT);
            ps.executeUpdate();
        }
    }

    private static List<OrderItem> listItems(Connection conn, long orderId) throws SQLException {
        String sql = "SELECT oi.order_item_id, oi.order_id, oi.product_id, oi.shop_product_id,"
                + " oi.shop_id, oi.design_file_id, oi.product_name_snapshot, oi.shop_name_snapshot,"
                + " oi.quantity, oi.unit_price, oi.line_total, oi.status,"
                + " o.payment_status, o.status AS order_status,"
                + " (SELECT COUNT(*) FROM reviews rv WHERE rv.order_item_id = oi.order_item_id)"
                + "   AS review_count"
                + " FROM order_items oi JOIN orders o ON o.order_id = oi.order_id"
                + " WHERE oi.order_id = ? ORDER BY oi.order_item_id";
        List<OrderItem> items = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderItem item = new OrderItem();
                    item.setOrderItemId(rs.getLong("order_item_id"));
                    item.setOrderId(rs.getLong("order_id"));
                    long productId = rs.getLong("product_id");
                    item.setProductId(rs.wasNull() ? null : productId);
                    long shopProductId = rs.getLong("shop_product_id");
                    item.setShopProductId(rs.wasNull() ? null : shopProductId);
                    long shopId = rs.getLong("shop_id");
                    item.setShopId(rs.wasNull() ? null : shopId);
                    long designFileId = rs.getLong("design_file_id");
                    item.setDesignFileId(rs.wasNull() ? null : designFileId);
                    item.setProductName(rs.getString("product_name_snapshot"));
                    item.setShopName(rs.getString("shop_name_snapshot"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setUnitPrice(rs.getBigDecimal("unit_price"));
                    item.setLineTotal(rs.getBigDecimal("line_total"));
                    item.setStatus(rs.getString("status"));
                    item.setReviewed(rs.getInt("review_count") > 0);
                    String paymentStatus = rs.getString("payment_status");
                    item.setOrderPaid(OrderStatus.PAYMENT_PAID.equals(paymentStatus)
                            || OrderStatus.PAYMENT_COD.equals(paymentStatus));
                    items.add(item);
                }
            }
        }
        return items;
    }

    private static Order mapOrder(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setOrderId(rs.getLong("order_id"));
        order.setOrderCode(rs.getString("order_code"));
        order.setUserId(rs.getLong("user_id"));
        long addressId = rs.getLong("address_id");
        order.setAddressId(rs.wasNull() ? null : addressId);
        order.setSubtotal(rs.getBigDecimal("subtotal"));
        order.setShippingFee(rs.getBigDecimal("shipping_fee"));
        order.setTotalAmount(rs.getBigDecimal("total_amount"));
        order.setCurrency(rs.getString("currency"));
        order.setStatus(rs.getString("status"));
        order.setPaymentStatus(rs.getString("payment_status"));
        order.setShippingStatus(rs.getString("shipping_status"));
        order.setPlacedAt(toLocalDateTime(rs.getTimestamp("placed_at")));
        order.setCompletedAt(toLocalDateTime(rs.getTimestamp("completed_at")));
        order.setCancelledAt(toLocalDateTime(rs.getTimestamp("cancelled_at")));
        order.setCustomerName(rs.getString("customer_name"));
        order.setRecipientName(rs.getString("recipient_name"));
        order.setRecipientPhone(rs.getString("recipient_phone"));
        String shippingAddress = rs.getString("shipping_address");
        order.setShippingAddress(shippingAddress == null ? "" : shippingAddress.trim());
        return order;
    }

    private static void setLongOrNull(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.BIGINT);
        } else {
            ps.setLong(index, value);
        }
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private static String generateOrderCode() {
        long now = System.currentTimeMillis() % 100_000_000L;
        int random = RANDOM.nextInt(1_000_000);
        return "PK-%08d-%06d".formatted(now, random);
    }
}
