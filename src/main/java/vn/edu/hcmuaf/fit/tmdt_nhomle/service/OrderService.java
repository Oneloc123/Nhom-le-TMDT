package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.AddressDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.DesignFileDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.CartDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.OrderDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ProductDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ShopDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Address;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.CartItem;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.DesignFile;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Order;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.OrderItem;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.ShopOffer;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Store;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class OrderService {

    public static final BigDecimal SHIPPING_FEE = new BigDecimal("30000");
    public static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("2000000");
    private static final String CURRENCY = "VND";
    private static final int MAX_ITEMS = 100;
    private static final int MAX_QUANTITY = 100_000;


    public Order createOrder(User user, Long addressId, String manualPhone,
                             String manualProvince, String manualDistrict,
                             String manualAddressLine, String manualRecipient) {
        if (user == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập trước khi đặt hàng.");
        }

        ShippingDetails shipping = resolveShipping(user, addressId, manualPhone,
                manualProvince, manualDistrict, manualAddressLine, manualRecipient);
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
                Order order = OrderDao.createOrder(conn, user.getId(), shipping.addressId(),
                        shipping.recipientName(), shipping.recipientPhone(),
                        shipping.shippingAddress(), shipping.latitude(), shipping.longitude(), shipping.deliveryNote(), orderItems,
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

    public Order createBuyNowOrder(User user, long shopProductId, int quantity,
                                   Long addressId, String manualPhone,
                                   String manualProvince, String manualDistrict,
                                   String manualAddressLine, String manualRecipient) {
        if (user == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập trước khi đặt hàng.");
        }
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw new CartException("Số lượng đặt mua không hợp lệ.");
        }
        ShippingDetails shipping = resolveShipping(user, addressId, manualPhone,
                manualProvince, manualDistrict, manualAddressLine, manualRecipient);

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ShopOffer offer = CartDao.findOfferForUpdate(conn, shopProductId)
                        .orElseThrow(() -> new CartException(
                                "Sản phẩm không còn tồn tại hoặc đã ngừng bán."));
                if (!offer.isPurchasable()) {
                    throw new CartException("Sản phẩm hiện tạm hết hàng.");
                }
                if (offer.getMinimumQuantity() > 0
                        && quantity < offer.getMinimumQuantity()) {
                    throw new CartException("Sản phẩm yêu cầu tối thiểu "
                            + offer.getMinimumQuantity() + " sản phẩm.");
                }

                String productName = ProductDao.findById(offer.getProductId())
                        .map(product -> product.getName()).orElse("Sản phẩm");
                OrderItem item = new OrderItem();
                item.setProductId(offer.getProductId());
                item.setShopProductId(offer.getShopProductId());
                item.setShopId(offer.getShopId());
                item.setProductName(productName);
                item.setShopName(offer.getShopName());
                item.setQuantity(quantity);
                item.setUnitPrice(offer.getUnitPrice());
                item.setLineTotal(offer.getUnitPrice().multiply(BigDecimal.valueOf(quantity)));

                BigDecimal subtotal = item.getLineTotal();
                BigDecimal shippingFee = calculateShippingFee(subtotal);
                Order order = OrderDao.createOrder(conn, user.getId(), shipping.addressId(),
                        shipping.recipientName(), shipping.recipientPhone(),
                        shipping.shippingAddress(), shipping.latitude(), shipping.longitude(), shipping.deliveryNote(), List.of(item), subtotal, shippingFee, CURRENCY);
                conn.commit();
                return order;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException.of(e);
        }
    }

    public Order createCustomDesignOrder(User user, long shopId, long designFileId,
                                         String customTitle, BigDecimal unitPrice, int quantity,
                                         Long addressId, String manualPhone,
                                         String manualProvince, String manualDistrict,
                                         String manualAddressLine, String manualRecipient) {
        if (user == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập trước khi đặt hàng.");
        }
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw new CartException("Số lượng đặt mua không hợp lệ.");
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0
                || unitPrice.compareTo(new BigDecimal("1000000000.00")) > 0) {
            throw new CartException("Đơn giá không hợp lệ.");
        }
        unitPrice = unitPrice.setScale(2, RoundingMode.HALF_UP);
        DesignFile designFile = DesignFileDao.findById(designFileId)
                .orElseThrow(() -> new CartException("Tệp thiết kế không tồn tại."));
        if (designFile.getUserId() == null || designFile.getUserId().longValue() != user.getId()) {
            throw new CartException("Tệp thiết kế không thuộc tài khoản của bạn.");
        }
        if (designFile.getStatus() == null || !"ACTIVE".equalsIgnoreCase(designFile.getStatus())) {
            throw new CartException("Tệp thiết kế này hiện không thể dùng để đặt in.");
        }
        Store shop = ShopDao.findById(shopId)
                .orElseThrow(() -> new CartException("Xưởng in không tồn tại hoặc đã ngừng hoạt động."));

        ShippingDetails shipping = resolveShipping(user, addressId, manualPhone,
                manualProvince, manualDistrict, manualAddressLine, manualRecipient);

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                OrderItem item = new OrderItem();
                item.setShopId(shop.getId());
                item.setShopName(shop.getName());
                item.setDesignFileId(designFileId);
                item.setProductName(customTitle == null || customTitle.isBlank() ? "In 3D theo yêu cầu" : customTitle);
                item.setQuantity(quantity);
                item.setUnitPrice(unitPrice);
                item.setLineTotal(unitPrice.multiply(BigDecimal.valueOf(quantity)));

                BigDecimal subtotal = item.getLineTotal();
                BigDecimal shippingFee = calculateShippingFee(subtotal);
                Order order = OrderDao.createOrder(conn, user.getId(), shipping.addressId(),
                        shipping.recipientName(), shipping.recipientPhone(),
                        shipping.shippingAddress(), shipping.latitude(), shipping.longitude(), shipping.deliveryNote(), List.of(item), subtotal, shippingFee, CURRENCY);
                conn.commit();
                return order;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException.of(e);
        }
    }

    public static BigDecimal calculateShippingFee(BigDecimal subtotal) {
        BigDecimal safeSubtotal = subtotal == null ? BigDecimal.ZERO : subtotal;
        return safeSubtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0
                ? BigDecimal.ZERO : SHIPPING_FEE;
    }

    private ShippingDetails resolveShipping(User user, Long addressId, String manualPhone,
                                            String manualProvince, String manualDistrict,
                                            String manualAddressLine, String manualRecipient) {
        if (addressId != null && addressId > 0) {
            Address address = AddressDao.findByIdForUser(addressId, user.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Địa chỉ không tồn tại hoặc bạn không có quyền sử dụng."));
            return new ShippingDetails(address.getAddressId(), address.getRecipientName(),
                    address.getRecipientPhone(), address.getFullLine(), address.getLatitude(), address.getLongitude(), address.getDeliveryNote());
        }

        String recipientName = trim(manualRecipient);
        String recipientPhone = vn.edu.hcmuaf.fit.tmdt_nhomle.util.VietnamPhoneNumber.normalize(manualPhone);
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
        if (!recipientPhone.matches("\\+84\\d{9}")) {
            throw new IllegalArgumentException("Số điện thoại nhận hàng không hợp lệ.");
        }
        if (province.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập tỉnh/thành phố.");
        }
        if (addressLine.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập địa chỉ cụ thể.");
        }
        if (province.length() > 100 || district.length() > 100 || addressLine.length() > 255) {
            throw new IllegalArgumentException("Thông tin địa chỉ vượt quá độ dài cho phép.");
        }
        String shippingAddress = java.util.stream.Stream.of(addressLine, district, province)
                .filter(part -> !part.isBlank()).collect(java.util.stream.Collectors.joining(", "));
        return new ShippingDetails(null, recipientName, recipientPhone, shippingAddress, null, null, null);
    }


    private record ShippingDetails(Long addressId, String recipientName,
                                   String recipientPhone, String shippingAddress,
                                   Double latitude, Double longitude, String deliveryNote) {
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
