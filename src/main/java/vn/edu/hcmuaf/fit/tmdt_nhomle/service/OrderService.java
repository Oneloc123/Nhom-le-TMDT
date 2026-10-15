package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.AddressDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.CartDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.OrderDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Address;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.CartItem;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Order;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.OrderItem;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.ShopOffer;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * Đặt hàng:
 * - Tải lại giỏ và GIÁ HIỆN TẠI từ shop_products (không tin giá/tổng từ client).
 * - Đơn được tạo trong MỘT transaction (orders + order_items + lịch sử).
 * - Giỏ hàng chỉ xóa SAU khi transaction thành công.
 * - Phí vận chuyển: quy định hiện tại của dự án chưa có — áp dụng phí cố định
 *   30.000đ, miễn phí cho đơn từ 2.000.000đ (định nghĩa tập trung tại đây).
 */
public class OrderService {

    public static final BigDecimal SHIPPING_FEE = new BigDecimal("30000");
    public static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("2000000");
    private static final String CURRENCY = "VND";
    private static final int MAX_ITEMS = 100;

    private final CartService cartService = new CartService();

    /**
     * Tạo đơn từ giỏ hàng của user.
     * @param addressId địa chỉ đã lưu (kiểm tra sở hữu) — hoặc null nếu nhập tay.
     */
    public Order createOrder(User user, HttpSession session, Long addressId,
                             String manualPhone, String manualAddress, String manualRecipient) {
        List<CartItem> cartItems = cartService.items(session, user);
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Giỏ hàng đang trống.");
        }
        if (cartItems.size() > MAX_ITEMS) {
            throw new IllegalArgumentException("Đơn hàng vượt số dòng sản phẩm cho phép.");
        }

        // 1. Xác nhận địa chỉ nhận hàng (sở hữu hoặc nhập tay hợp lệ).
        String recipientName;
        String recipientPhone;
        String shippingAddress;
        Long resolvedAddressId = null;
        if (addressId != null && addressId > 0) {
            Address address = AddressDao.findByIdForUser(addressId, user.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Địa chỉ không tồn tại hoặc bạn không có quyền sử dụng."));
            recipientName = address.getRecipientName();
            recipientPhone = address.getRecipientPhone();
            shippingAddress = address.getFullLine();
            resolvedAddressId = address.getAddressId();
        } else {
            recipientName = user.getFullName();
            recipientPhone = manualPhone == null ? "" : manualPhone.trim();
            shippingAddress = manualAddress == null ? "" : manualAddress.trim();
            if (recipientPhone.isEmpty()) {
                throw new IllegalArgumentException("Vui lòng nhập số điện thoại nhận hàng.");
            }
            if (!recipientPhone.matches("\\d{9,12}")) {
                throw new IllegalArgumentException("Số điện thoại nhận hàng không hợp lệ.");
            }
            if (shippingAddress.isEmpty()) {
                throw new IllegalArgumentException("Vui lòng nhập địa chỉ nhận hàng.");
            }
        }

        // 2. Đọc lại giá & kiểm tra điều kiện chào bán NGAY TRONG transaction.
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long cartId = CartDao.getOrCreateCartId(conn, user.getId());
                List<CartItem> freshItems = CartDao.listItems(conn, cartId);
                if (freshItems.isEmpty()) {
                    throw new IllegalStateException("Giỏ hàng đang trống.");
                }
                for (CartItem line : freshItems) {
                    ShopOffer offer = CartDao.findOffer(conn, line.getShopProductId())
                            .orElseThrow(() -> new CartException(
                                    "Chào bán \"" + line.getProduct().getName()
                                            + "\" không còn tồn tại hoặc đã ngừng bán."));
                    if (!offer.isPurchasable()) {
                        throw new CartException("Chào bán \"" + line.getProduct().getName()
                                + "\" hiện tạm hết hàng.");
                    }
                    if (offer.getMinimumQuantity() > 0
                            && line.getQuantity() < offer.getMinimumQuantity()) {
                        throw new CartException("Chào bán \"" + line.getProduct().getName()
                                + "\" yêu cầu tối thiểu " + offer.getMinimumQuantity() + " sản phẩm.");
                    }
                    OrderItem item = new OrderItem();
                    item.setProductId(offer.getProductId());
                    item.setShopProductId(offer.getShopProductId());
                    item.setShopId(offer.getShopId());
                    item.setDesignFileId(line.getDesignFileId());
                    item.setProductName(line.getProduct().getName());
                    item.setShopName(offer.getShopName());
                    item.setQuantity(line.getQuantity());
                    item.setUnitPrice(offer.getUnitPrice()); // giá hiện tại từ DB
                    item.setLineTotal(offer.getUnitPrice()
                            .multiply(BigDecimal.valueOf(line.getQuantity())));
                    subtotal = subtotal.add(item.getLineTotal());
                    orderItems.add(item);
                }
                // rollback transaction đọc — transaction tạo đơn riêng bên dưới.
                conn.rollback();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (java.sql.SQLException e) {
            throw vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException.of(e);
        }

        BigDecimal shippingFee = subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0
                ? BigDecimal.ZERO : SHIPPING_FEE;

        // 3. Tạo đơn (OrderDao chạy transaction riêng: orders + items + history).
        Order order = OrderDao.createOrder(user.getId(), resolvedAddressId, recipientName,
                recipientPhone, shippingAddress, orderItems, subtotal, shippingFee, CURRENCY);

        // 4. Xóa giỏ hàng SAU khi đã tạo đơn thành công.
        cartService.clear(session, user);
        return order;
    }
}
