package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class OrderService {

    public static final BigDecimal SHIPPING_FEE = new BigDecimal("30000");
    public static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("2000000");
    private static final String CURRENCY = "VND";
    private static final int MAX_ITEMS = 100;

    /**
     * Tạo đơn từ giỏ hàng của user.
     * @param addressId địa chỉ đã lưu (kiểm tra sở hữu) — hoặc null nếu nhập tay.
     */
    public Order createOrder(User user, Long addressId, String manualPhone,
                             String manualProvince, String manualDistrict,
                             String manualAddressLine, String manualRecipient) {
        if (user == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập trước khi đặt hàng.");
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
            recipientName = manualRecipient == null ? "" : manualRecipient.trim();
            recipientPhone = manualPhone == null ? "" : manualPhone.trim();
            String province = trim(manualProvince);
            String district = trim(manualDistrict);
            String addressLine = trim(manualAddressLine);
            if (recipientName.isEmpty()) {
                throw new IllegalArgumentException("Vui lòng nhập tên người nhận.");
            }
            if (recipientName.length() > 120) {
                throw new IllegalArgumentException("Tên người nhận không được vượt quá 120 ký tự.");
            }
            if (recipientPhone.isEmpty()) {
                throw new IllegalArgumentException("Vui lòng nhập số điện thoại nhận hàng.");
            }
            if (!recipientPhone.matches("\\d{9,12}")) {
                throw new IllegalArgumentException("Số điện thoại nhận hàng không hợp lệ.");
            }
            if (province.isEmpty()) {
                throw new IllegalArgumentException("Vui lòng nhập tỉnh/thành phố.");
            }
            if (district.isEmpty()) {
                throw new IllegalArgumentException("Vui lòng nhập quận/huyện.");
            }
            if (addressLine.isEmpty()) {
                throw new IllegalArgumentException("Vui lòng nhập địa chỉ cụ thể.");
            }
            if (province.length() > 100 || district.length() > 100 || addressLine.length() > 255) {
                throw new IllegalArgumentException("Thông tin địa chỉ vượt quá độ dài cho phép.");
            }
            shippingAddress = addressLine + ", " + district + ", " + province;
        }

        // 2. Khóa giỏ, chốt giá, tạo đơn và xóa giỏ trong CÙNG một transaction.
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long cartId = CartDao.getOrCreateCartIdForUpdate(conn, user.getId());
                List<CartItem> freshItems = CartDao.listItems(conn, cartId);
                if (freshItems.isEmpty()) {
                    throw new IllegalStateException("Giỏ hàng đang trống.");
                }
                if (freshItems.size() > MAX_ITEMS) {
                    throw new IllegalArgumentException("Đơn hàng vượt số dòng sản phẩm cho phép.");
                }

                List<OrderItem> orderItems = new ArrayList<>();
                BigDecimal subtotal = BigDecimal.ZERO;
                for (CartItem line : freshItems) {
                    ShopOffer offer = CartDao.findOfferForUpdate(conn, line.getShopProductId())
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

                BigDecimal shippingFee = calculateShippingFee(subtotal);
                Order order = OrderDao.createOrder(conn, user.getId(), resolvedAddressId,
                        recipientName, recipientPhone, shippingAddress, orderItems,
                        subtotal, shippingFee, CURRENCY);
                CartDao.clearItems(conn, cartId);
                CartDao.refreshCartTotals(conn, cartId);
                conn.commit();
                return order;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (java.sql.SQLException e) {
            throw vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException.of(e);
        }

    }

    public static BigDecimal calculateShippingFee(BigDecimal subtotal) {
        BigDecimal safeSubtotal = subtotal == null ? BigDecimal.ZERO : subtotal;
        return safeSubtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0
                ? BigDecimal.ZERO : SHIPPING_FEE;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
