package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.AddressDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.CustomPrintWorkflowDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ShopDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.CartItem;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Order;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Product;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Store;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.AuthFilter;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.CartService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.OrderService;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * Đặt hàng:
 * - GET: hiển thị giỏ, hoặc đúng một sản phẩm của luồng "Đặt ngay" hoặc đơn in 3D theo yêu cầu.
 * - POST: tạo đơn (địa chỉ đã lưu được kiểm tra sở hữu, hoặc nhập tay hợp lệ),
 *   giá/tổng tính lại từ database, đơn tạo trong transaction (OrderService).
 * - PRG: chuyển sang trang thanh toán bằng order_code.
 */
@WebServlet(name = "orderServlet", value = "/order")
public class OrderServlet extends HttpServlet {

    private final CartService cartService = new CartService();
    private final OrderService orderService = new OrderService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = AuthFilter.currentUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        if (isLegacyCustomDesign(request)) {
            response.sendRedirect(request.getContextPath() + "/design");
            return;
        }
        if (!isBuyNow(request) && !isCustomPrint(request) && cartService.items(request.getSession(false), user).isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/cart?error=empty");
            return;
        }
        try {
            populate(request, user);
            request.getRequestDispatcher("/customer/order.jsp").forward(request, response);
        } catch (RuntimeException error) {
            request.setAttribute("message", error.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.getRequestDispatcher("/customer/not-found.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = AuthFilter.currentUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        request.setCharacterEncoding("UTF-8");
        if (isLegacyCustomDesign(request)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Luồng in cũ đã đóng. Hãy tải mô hình lên trang đặt in 3D để máy chủ xác thực giá.");
            return;
        }

        Long addressId = parseLong(request.getParameter("addressId"));
        String phone = request.getParameter("phone");
        try {
            Order order;
            if (isCustomPrint(request)) {
                order = orderService.createCustomPrintOrder(user,
                        requiredLong(request.getParameter("requestId"), "Hồ sơ in 3D không hợp lệ."),
                        addressId, phone, request.getParameter("province"), request.getParameter("district"),
                        request.getParameter("addressLine"), request.getParameter("recipientName"));
            } else if (isBuyNow(request)) {
                order = orderService.createBuyNowOrder(user,
                        requiredLong(request.getParameter("shopProductId"), "Sản phẩm không hợp lệ."),
                        requiredInt(request.getParameter("quantity"), "Số lượng không hợp lệ."),
                        addressId, phone, request.getParameter("province"),
                        request.getParameter("district"), request.getParameter("addressLine"),
                        request.getParameter("recipientName"));
            } else {
                order = orderService.createOrder(user, addressId, phone,
                        request.getParameter("province"), request.getParameter("district"),
                        request.getParameter("addressLine"), request.getParameter("recipientName"));
            }
            response.sendRedirect(request.getContextPath()
                    + "/payment?orderNumber=" + java.net.URLEncoder.encode(order.getOrderCode(),
                            java.nio.charset.StandardCharsets.UTF_8));
        } catch (RuntimeException error) {
            request.setAttribute("error", error.getMessage());
            populate(request, user);
            request.getRequestDispatcher("/customer/order.jsp").forward(request, response);
        }
    }

    private void populate(HttpServletRequest request, User user) {
        HttpSession session = request.getSession(false);
        boolean buyNow = isBuyNow(request);
        boolean customPrint = isCustomPrint(request);
        List<CartItem> items;
        if (customPrint) {
            long requestId=requiredLong(request.getParameter("requestId"),"Hồ sơ in 3D không hợp lệ.");
            var custom=CustomPrintWorkflowDao.findByIdForUser(requestId,user.getId())
                    .orElseThrow(()->new IllegalArgumentException("Không tìm thấy hồ sơ in 3D của bạn."));
            if(custom.orderId()!=null)throw new IllegalArgumentException("Hồ sơ này đã được chuyển thành đơn hàng.");
            boolean direct="DIRECT".equals(custom.workflowType())&&"READY_TO_ORDER".equals(custom.status());
            boolean quote="QUOTE".equals(custom.workflowType())&&"QUOTED".equals(custom.status());
            if(!direct&&!quote)throw new IllegalArgumentException("Báo giá hoặc yêu cầu chưa sẵn sàng để đặt hàng.");
            if(quote&&!CustomPrintWorkflowDao.quoteIsStillValid(requestId))throw new IllegalArgumentException("Báo giá đã hết hạn. Hãy gửi yêu cầu báo giá mới.");
            BigDecimal total=quote?custom.quotedAmount():orderService.previewCustomPrint(custom).totalEstimate();
            if(total==null||total.signum()<=0)throw new IllegalArgumentException("Không xác định được tổng tiền đặt in.");
            Store shop=ShopDao.findById(custom.shopId()).orElseThrow(()->new IllegalArgumentException("Xưởng không còn tồn tại."));
            int quantity=custom.quantity()==null?1:custom.quantity();
            CartItem customItem=new CartItem();customItem.setShopProductId(0);customItem.setShopName(shop.getName());
            customItem.setQuantity(quantity);customItem.setUnitPrice(total.divide(BigDecimal.valueOf(quantity),2,java.math.RoundingMode.HALF_UP));
            customItem.setDesignFileId(custom.designFileId());
            Product product=new Product();product.setId(0);product.setName("In 3D: "+custom.originalFilename());customItem.setProduct(product);
            items=List.of(customItem);request.setAttribute("checkoutMode","customPrint");
            request.setAttribute("checkoutCustomPrintRequestId",requestId);request.setAttribute("customPrintWorkflow",custom.workflowType());
            request.setAttribute("customPrintEstimate",total);request.setAttribute("customPrintStatus",custom.status());
        } else if (buyNow) {
            long shopProductId = requiredLong(request.getParameter("shopProductId"),
                    "Sản phẩm không hợp lệ.");
            int quantity = requiredInt(request.getParameter("quantity"),
                    "Số lượng không hợp lệ.");
            items = List.of(cartService.checkoutItem(shopProductId, quantity));
            request.setAttribute("checkoutMode", "buyNow");
            request.setAttribute("checkoutShopProductId", shopProductId);
            request.setAttribute("checkoutQuantity", quantity);
            request.setAttribute("checkoutProductId", items.getFirst().getProduct().getId());
        } else {
            items = cartService.items(session, user);
        }
        BigDecimal subtotal = items.stream().map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (customPrint) subtotal=(BigDecimal)request.getAttribute("customPrintEstimate");
        var shippingFee = OrderService.calculateShippingFee(subtotal);
        request.setAttribute("user", user);
        request.setAttribute("cartItems", items);
        request.setAttribute("cartSubtotal", subtotal);
        request.setAttribute("shippingFee", shippingFee);
        request.setAttribute("orderTotal", subtotal.add(shippingFee));
        request.setAttribute("addresses", AddressDao.findByUser(user.getId()));
    }

    private Long parseLong(String value) {
        try {
            return value == null || value.isBlank() ? null : Long.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean isBuyNow(HttpServletRequest request) {
        return "buyNow".equals(request.getParameter("mode"));
    }

    private boolean isLegacyCustomDesign(HttpServletRequest request) {
        return "customDesign".equals(request.getParameter("mode"));
    }

    private boolean isCustomPrint(HttpServletRequest request) {
        return "customPrint".equals(request.getParameter("mode"));
    }

    private long requiredLong(String value, String message) {
        Long parsed = parseLong(value);
        if (parsed == null || parsed <= 0) {
            throw new IllegalArgumentException(message);
        }
        return parsed;
    }

    private int requiredInt(String value, String message) {
        long parsed = requiredLong(value, message);
        if (parsed > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(message);
        }
        return (int) parsed;
    }
}
