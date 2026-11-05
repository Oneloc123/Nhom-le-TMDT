package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.AddressDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.CustomPrintPricingDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.CustomPrintWorkflowDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.CartDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.OrderDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ProductDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ShopDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Address;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.CartItem;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Order;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.OrderItem;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.CustomPrintRequest;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.ModelAnalysis;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.PrintPriceOption;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.PrintPriceEstimate;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.ShopPricingConfig;
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
    private final PrintPriceEstimator printPriceEstimator = new PrintPriceEstimator();

    public PrintPriceEstimate previewCustomPrint(CustomPrintRequest request) {
        if (request == null || request.shopId() == null || request.quantity() == null) {
            throw new IllegalArgumentException("Thiếu cấu hình đặt in.");
        }
        ShopPricingConfig pricing = CustomPrintPricingDao.findActive(request.shopId(), request.printingMethod(), request.material())
                .orElseThrow(() -> new IllegalArgumentException("Xưởng chưa có bảng giá cho cấu hình này."));
        return estimate(request, pricing);
    }

    /** Reprices direct requests from the current shop profile and converts accepted quotes atomically. */
    public Order createCustomPrintOrder(User user, long requestId, Long addressId, String manualPhone,
                                        String manualProvince, String manualDistrict,
                                        String manualAddressLine, String manualRecipient) {
        if (user == null) throw new IllegalArgumentException("Vui lòng đăng nhập trước khi đặt hàng.");
        ShippingDetails shipping = resolveShipping(user, addressId, manualPhone, manualProvince,
                manualDistrict, manualAddressLine, manualRecipient);
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                CustomPrintRequest request = CustomPrintWorkflowDao.findByIdForUser(conn, requestId, user.getId(), true)
                        .orElseThrow(() -> new IllegalArgumentException("Hồ sơ in 3D không tồn tại."));
                if (request.orderId() != null) {
                    try (var ps=conn.prepareStatement("SELECT order_code FROM orders WHERE order_id=? AND user_id=?")) {
                        ps.setLong(1,request.orderId());ps.setLong(2,user.getId());
                        try(var rs=ps.executeQuery()){
                            if(!rs.next())throw new IllegalArgumentException("Đơn hàng liên kết không thuộc tài khoản này.");
                            Order existing=new Order();existing.setOrderId(request.orderId());existing.setOrderCode(rs.getString(1));existing.setUserId(user.getId());
                            conn.rollback();return existing;
                        }
                    }
                }
                boolean direct = "DIRECT".equals(request.workflowType()) && "READY_TO_ORDER".equals(request.status());
                boolean quoted = "QUOTE".equals(request.workflowType()) && "QUOTED".equals(request.status());
                if (!direct && !quoted) throw new IllegalArgumentException("Hồ sơ chưa sẵn sàng để đặt hàng.");
                if (quoted && !CustomPrintWorkflowDao.quoteIsStillValid(conn, request.id())) {
                    throw new IllegalArgumentException("Báo giá đã hết hạn. Hãy gửi yêu cầu báo giá mới.");
                }
                requireActiveDesignFile(conn, request, user.getId());
                String shopName = shopNameForOrder(conn, request.shopId());
                BigDecimal totalPrice;
                int estimatedProductionDays;
                if (direct) {
                    CustomPrintWorkflowDao.validateDirectShopForOrder(conn,request);
                    ShopPricingConfig pricing = CustomPrintPricingDao.findActive(conn, request.shopId(),
                            request.printingMethod(), request.material()).orElseThrow(
                            () -> new IllegalArgumentException("Bảng giá xưởng không còn khả dụng."));
                    PrintPriceEstimate estimate = estimate(request, pricing);
                    totalPrice = estimate.totalEstimate();
                    estimatedProductionDays = estimate.productionDays();
                } else {
                    if (request.quotedAmount() == null || request.quotedAmount().signum() <= 0) {
                        throw new IllegalArgumentException("Báo giá của xưởng chưa hợp lệ.");
                    }
                    totalPrice = request.quotedAmount().setScale(2, RoundingMode.HALF_UP);
                    estimatedProductionDays = request.productionDays() == null ? 3 : request.productionDays();
                    insertCustomHistory(conn, request.id(), user.getId(), "QUOTED", "ACCEPTED", "Khách hàng chấp nhận báo giá.");
                }

                OrderItem item = new OrderItem();
                item.setShopId(request.shopId()); item.setShopName(shopName); item.setDesignFileId(request.designFileId());
                item.setProductName(limit("In 3D: " + request.originalFilename(), 180));
                item.setQuantity(request.quantity());
                item.setUnitPrice(totalPrice.divide(BigDecimal.valueOf(request.quantity()), 2, RoundingMode.HALF_UP));
                item.setLineTotal(totalPrice);
                BigDecimal subtotal = totalPrice;
                Order order = OrderDao.createOrder(conn, user.getId(), shipping.addressId(), shipping.recipientName(),
                        shipping.recipientPhone(), shipping.shippingAddress(), shipping.latitude(), shipping.longitude(),
                        shipping.deliveryNote(), List.of(item), subtotal, calculateShippingFee(subtotal), CURRENCY,
                        request.id());
                try (var ps = conn.prepareStatement("UPDATE orders SET estimated_delivery_at=TIMESTAMPADD(DAY,?,NOW())"
                        + " WHERE order_id=? AND user_id=?")) {
                    ps.setInt(1, estimatedProductionDays); ps.setLong(2, order.getOrderId()); ps.setLong(3, user.getId());
                    if (ps.executeUpdate() != 1) throw new SQLException("Không lưu được hạn giao dự kiến của đơn in 3D.");
                }
                try (var ps = conn.prepareStatement("UPDATE custom_print_requests SET order_id=?,status='ORDER_CREATED',"
                        + "accepted_at=IF(workflow_type='QUOTE',NOW(),accepted_at),updated_at=NOW()"
                        + " WHERE custom_request_id=? AND user_id=? AND order_id IS NULL AND status=?")) {
                    ps.setLong(1, order.getOrderId()); ps.setLong(2, request.id()); ps.setLong(3, user.getId()); ps.setString(4, request.status());
                    if (ps.executeUpdate() != 1) throw new IllegalArgumentException("Hồ sơ đã được xử lý ở nơi khác.");
                }
                insertCustomHistory(conn, request.id(), user.getId(), quoted ? "ACCEPTED" : request.status(), "ORDER_CREATED",
                        quoted ? "Báo giá được chấp nhận và chuyển thành đơn hàng." : "Đơn đặt in trực tiếp được tạo.");
                conn.commit();
                return order;
            } catch (SQLException | RuntimeException e) {
                conn.rollback(); throw e;
            } finally { conn.setAutoCommit(true); }
        } catch (SQLException e) { throw vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException.of(e); }
    }

    private PrintPriceEstimate estimate(CustomPrintRequest request, ShopPricingConfig pricing) {
        ModelAnalysis.Status status;
        try { status = ModelAnalysis.Status.valueOf(request.analysisStatus()); }
        catch (RuntimeException e) { throw new IllegalArgumentException("Mô hình chưa thể ước tính tự động."); }
        ModelAnalysis model = new ModelAnalysis(status, "obj", request.sourceUnit(), value(request.widthMm()),
                value(request.depthMm()), value(request.heightMm()), value(request.volumeCm3()),
                value(request.surfaceAreaCm2()), request.triangleCount() == null ? 0 : request.triangleCount(), request.analysisMessage());
        List<PrintPriceOption> selected = pricing.options().stream().filter(option ->
                ("COLOR".equals(option.type()) && option.value().equals(request.colorOption()))
                        || ("FINISH".equals(option.type()) && option.value().equals(request.postProcessingOption()))).toList();
        if (request.colorOption() != null && selected.stream().noneMatch(option -> "COLOR".equals(option.type()))) {
            throw new IllegalArgumentException("Màu đã chọn không còn được xưởng hỗ trợ.");
        }
        if (request.postProcessingOption() != null && selected.stream().noneMatch(option -> "FINISH".equals(option.type()))) {
            throw new IllegalArgumentException("Gia công đã chọn không còn được xưởng hỗ trợ.");
        }
        return printPriceEstimator.estimate(model, pricing.profile(), request.infillPercent(), request.layerHeightMm(),
                request.quantity(), selected, pricing.discounts(), request.dimensionsConfirmed());
    }

    private static double value(BigDecimal n) { return n == null ? 0 : n.doubleValue(); }
    private static void requireActiveDesignFile(Connection c, CustomPrintRequest r, long userId) throws SQLException {
        try (var p = c.prepareStatement("SELECT 1 FROM design_files WHERE design_file_id=? AND user_id=? AND status='ACTIVE' AND deleted_at IS NULL FOR UPDATE")) {
            p.setLong(1, r.designFileId()); p.setLong(2, userId); try (var rs = p.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Tệp mô hình không còn khả dụng.");
            }
        }
    }
    private static String shopNameForOrder(Connection c, Long shopId) throws SQLException {
        if (shopId == null) throw new IllegalArgumentException("Chưa chọn xưởng.");
        try (var p = c.prepareStatement("SELECT shop_name FROM print_shops WHERE shop_id=? AND verification_status='VERIFIED' AND (status IS NULL OR status='ACTIVE') FOR UPDATE")) {
            p.setLong(1, shopId); try (var rs = p.executeQuery()) { if (!rs.next()) throw new IllegalArgumentException("Xưởng không còn nhận đơn."); return rs.getString(1); }
        }
    }
    private static void insertCustomHistory(Connection c,long id,long actor,String oldStatus,String next,String note)throws SQLException {
        try(var p=c.prepareStatement("INSERT INTO custom_print_request_history(custom_request_id,changed_by_user_id,old_status,new_status,note,created_at) VALUES(?,?,?,?,?,NOW())")){
            p.setLong(1,id);p.setLong(2,actor);p.setString(3,oldStatus);p.setString(4,next);p.setString(5,note);p.executeUpdate();
        }
    }
    private static String limit(String s,int max){return s.length()<=max?s:s.substring(0,max);}


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
