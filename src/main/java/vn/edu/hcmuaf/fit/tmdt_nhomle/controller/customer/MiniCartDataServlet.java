package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.CartItem;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Product;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.AuthFilter;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.CartService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.AjaxResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/** Dữ liệu JSON cho phần giỏ hàng mini ở đầu trang. */
@WebServlet(name = "miniCartDataServlet", value = "/mini-cart-data")
public class MiniCartDataServlet extends HttpServlet {

    private final CartService cartService = new CartService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        try {
            HttpSession session = request.getSession(true);
            User user = AuthFilter.currentUser(request);
            List<CartItem> items = cartService.items(session, user);
            int quantity = items.stream().mapToInt(CartItem::getQuantity).sum();
            BigDecimal subtotal = items.stream().map(CartItem::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            StringBuilder json = new StringBuilder(512);
            json.append("{\"success\":true,\"cartTotalQuantity\":").append(quantity)
                    .append(",\"cartSubtotal\":").append(subtotal.toPlainString())
                    .append(",\"items\":[");

            for (int i = 0; i < items.size(); i++) {
                CartItem item = items.get(i);
                Product product = item.getProduct();
                long productId = product == null ? 0 : product.getId();
                String name = product == null || product.getName() == null
                        ? "Sản phẩm" : product.getName();
                String visualClass = product == null || product.getVisualClass() == null
                        ? "warm" : product.getVisualClass();
                String shopName = item.getShopName() == null ? "Xưởng in" : item.getShopName();
                String detailUrl = request.getContextPath() + "/product-detail?id=" + productId;

                if (i > 0) json.append(',');
                json.append('{')
                        .append("\"shopProductId\":").append(item.getShopProductId()).append(',')
                        .append("\"productId\":").append(productId).append(',')
                        .append("\"name\":").append(AjaxResponse.quote(name)).append(',')
                        .append("\"shopName\":").append(AjaxResponse.quote(shopName)).append(',')
                        .append("\"visualClass\":").append(AjaxResponse.quote(visualClass)).append(',')
                        .append("\"detailUrl\":").append(AjaxResponse.quote(detailUrl)).append(',')
                        .append("\"quantity\":").append(item.getQuantity()).append(',')
                        .append("\"unitPrice\":").append(price(item.getUnitPrice())).append(',')
                        .append("\"subtotal\":").append(price(item.getSubtotal()))
                        .append('}');
            }
            json.append("]}");
            AjaxResponse.send(response, HttpServletResponse.SC_OK, json.toString());
        } catch (RuntimeException error) {
            System.getLogger(MiniCartDataServlet.class.getName()).log(
                    System.Logger.Level.WARNING, "Không thể tải dữ liệu giỏ hàng mini", error);
            AjaxResponse.send(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "{\"success\":false,\"message\":"
                            + AjaxResponse.quote("Hiện chưa thể tải giỏ hàng. Vui lòng thử lại.") + "}");
        }
    }

    private static String price(BigDecimal value) {
        return value == null ? "0" : value.toPlainString();
    }
}
