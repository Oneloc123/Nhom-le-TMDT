package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.OrderDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ProductDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.UserDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Order;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.OrderStatus;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Product;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.ShopOffer;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class OrderCheckoutIntegrationTest {

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void manualDeliveryCartCheckoutCodAndDuplicateProtectionWorkTogether() throws Exception {
        List<Product> catalog;
        try {
            catalog = ProductDao.search(
                    new ProductDao.SearchCriteria(null, null, null, null, null, null), 1, 1);
        } catch (RuntimeException unavailableDatabase) {
            assumeFalse(true, "Database không khả dụng — bỏ qua checkout integration test.");
            return;
        }
        assumeFalse(catalog.isEmpty(), "Database chưa có catalog — bỏ qua checkout integration test.");

        Product product = ProductDao.findById(catalog.getFirst().getId()).orElseThrow();
        ShopOffer offer = product.getPrimaryOffer();
        assumeFalse(offer == null, "Sản phẩm thử nghiệm không có chào bán khả dụng.");

        User user = UserDao.create("checkout-test-" + System.nanoTime() + "@example.test",
                "temporary-password", "Người nhận kiểm thử", "0901234567",
                User.ROLE_CUSTOMER, User.STATUS_ACTIVE);
        try {
            CartService cartService = new CartService();
            int quantity = Math.max(1, offer.getMinimumQuantity());
            cartService.add(null, user, offer.getShopProductId(), product.getId(), quantity);

            Order created = new OrderService().createOrder(user, null, "0901234567",
                    "TP.HCM", "Quận 1", "12 Đường Kiểm Thử", "Người nhận kiểm thử");
            Order stored = OrderDao.findByCodeForUser(created.getOrderCode(), user.getId())
                    .orElseThrow();
            OrderDao.loadDetails(stored);

            assertEquals("Người nhận kiểm thử", stored.getRecipientName());
            assertEquals("0901234567", stored.getRecipientPhone());
            assertEquals("12 Đường Kiểm Thử, Quận 1, TP.HCM", stored.getShippingAddress());
            assertEquals(quantity, stored.getItemCount());
            assertTrue(cartService.items(null, user).isEmpty());
            assertThrows(IllegalStateException.class, () -> new OrderService().createOrder(
                    user, null, "0901234567", "TP.HCM", "Quận 3", "Địa chỉ khác",
                    "Người nhận kiểm thử"));

            Order paid = new PaymentService().pay(created.getOrderCode(), user.getId(),
                    PaymentService.METHOD_COD);
            assertEquals(OrderStatus.CONFIRMED, paid.getStatus());
            assertEquals(OrderStatus.PAYMENT_COD, paid.getPaymentStatus());
            OrderDao.loadDetails(paid);
            assertTrue(paid.getItems().stream()
                    .allMatch(item -> OrderStatus.CONFIRMED.equals(item.getStatus())));
            assertTrue(paid.getItems().stream().noneMatch(item -> item.isReviewable()));

            OrderDao.updateStatus(paid.getOrderId(), user.getId(), OrderStatus.CANCELLED,
                    null, "Kiểm thử hủy đơn COD");
            Order cancelled = OrderDao.findByCodeForUser(created.getOrderCode(), user.getId())
                    .orElseThrow();
            assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
            assertEquals(OrderStatus.PAYMENT_CANCELLED, cancelled.getPaymentStatus());
            assertEquals("CANCELLED", paymentStatus(cancelled.getOrderId()));
            OrderDao.loadDetails(cancelled);
            assertTrue(cancelled.getItems().stream()
                    .allMatch(item -> OrderStatus.CANCELLED.equals(item.getStatus())));
        } finally {
            cleanupUser(user.getId());
        }
    }

    private void cleanupUser(long userId) throws Exception {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                execute(conn, "DELETE h FROM order_status_history h JOIN orders o"
                        + " ON o.order_id = h.order_id WHERE o.user_id = ?", userId);
                execute(conn, "DELETE p FROM payments p JOIN orders o"
                        + " ON o.order_id = p.order_id WHERE o.user_id = ?", userId);
                execute(conn, "DELETE oi FROM order_items oi JOIN orders o"
                        + " ON o.order_id = oi.order_id WHERE o.user_id = ?", userId);
                execute(conn, "DELETE FROM orders WHERE user_id = ?", userId);
                execute(conn, "DELETE ci FROM cart_items ci JOIN carts c"
                        + " ON c.cart_id = ci.cart_id WHERE c.user_id = ?", userId);
                execute(conn, "DELETE FROM carts WHERE user_id = ?", userId);
                execute(conn, "DELETE FROM users WHERE user_id = ?", userId);
                conn.commit();
            } catch (Exception error) {
                conn.rollback();
                throw error;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private void execute(Connection conn, String sql, long userId) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
    }

    private String paymentStatus(long orderId) throws Exception {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT status FROM payments WHERE order_id = ? ORDER BY payment_id DESC LIMIT 1")) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }
}
