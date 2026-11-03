package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.AuthFilter;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.CartException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.CartService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.AjaxResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.Redirects;

import java.io.IOException;

@WebServlet(name = "cartServlet", value = "/cart")
public class CartServlet extends HttpServlet {

    private final CartService cartService = new CartService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        populate(request);
        String noticeCode = request.getParameter("notice");
        String notice = "error".equals(noticeCode)
                ? request.getParameter("message")
                : noticeText(noticeCode);
        request.setAttribute("notice", notice);
        request.getRequestDispatcher("/customer/cart.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        User user = AuthFilter.currentUser(request);
        HttpSession session = request.getSession(true);
        String action = nvl(request.getParameter("action"));
        long shopProductId = parseLong(request.getParameter("shopProductId"), 0);
        long productId = parseLong(request.getParameter("productId"), 0);
        int quantity = (int) parseLong(request.getParameter("quantity"), 0);
        boolean ajax = AjaxResponse.isRequested(request);

        try {
            switch (action) {
                case "add" -> {
                    cartService.add(session, user, shopProductId, productId, quantity);
                    if (ajax) {
                        int total = cartService.totalQuantity(session, user);
                        AjaxResponse.send(response, HttpServletResponse.SC_OK,
                                "{\"success\":true,\"cartTotalQuantity\":" + total
                                        + ",\"message\":"
                                        + AjaxResponse.quote("Đã thêm sản phẩm vào giỏ hàng.") + "}");
                        return;
                    }
                    String next = Redirects.safeLocalPath(request.getParameter("next"));
                    response.sendRedirect(request.getContextPath()
                            + (next == null ? "/cart?notice=added" : next));
                    return;
                }
                case "update" -> {
                    cartService.update(session, user, shopProductId, quantity);
                    if (ajax) {
                        sendCartState(response, session, user, shopProductId,
                                "Đã cập nhật số lượng.");
                        return;
                    }
                    response.sendRedirect(request.getContextPath() + "/cart?notice=updated");
                    return;
                }
                case "remove" -> {
                    cartService.remove(session, user, shopProductId);
                    if (ajax) {
                        sendCartState(response, session, user, null,
                                "Đã xóa sản phẩm khỏi giỏ hàng.");
                        return;
                    }
                    response.sendRedirect(request.getContextPath() + "/cart?notice=removed");
                    return;
                }
                case "clear" -> {
                    cartService.clear(session, user);
                    if (ajax) {
                        sendCartState(response, session, user, null,
                                "Đã xóa toàn bộ giỏ hàng.");
                        return;
                    }
                    response.sendRedirect(request.getContextPath() + "/cart?notice=cleared");
                    return;
                }
                default -> response.sendRedirect(request.getContextPath() + "/cart");
            }
        } catch (CartException error) {
            if (ajax) {
                AjaxResponse.send(response, HttpServletResponse.SC_BAD_REQUEST,
                        "{\"success\":false,\"message\":"
                                + AjaxResponse.quote(error.getMessage()) + "}");
                return;
            }
            response.sendRedirect(request.getContextPath() + "/cart?notice=error&message="
                    + java.net.URLEncoder.encode(error.getMessage(),
                            java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private void populate(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        User user = AuthFilter.currentUser(request);
        request.setAttribute("cartItems", cartService.items(session, user));
        request.setAttribute("cartTotalQuantity", cartService.totalQuantity(session, user));
        request.setAttribute("cartSubtotal", cartService.subtotal(session, user));
    }

    private void sendCartState(HttpServletResponse response, HttpSession session, User user,
                               Long shopProductId, String message) throws IOException {
        var items = cartService.items(session, user);
        int totalQuantity = items.stream().mapToInt(item -> item.getQuantity()).sum();
        var subtotal = items.stream().map(item -> item.getSubtotal())
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        var line = shopProductId == null ? null : items.stream()
                .filter(item -> item.getShopProductId() == shopProductId)
                .findFirst().orElse(null);
        String lineSubtotal = line == null ? "null" : line.getSubtotal().toPlainString();
        AjaxResponse.send(response, HttpServletResponse.SC_OK,
                "{\"success\":true,\"cartTotalQuantity\":" + totalQuantity
                        + ",\"cartSubtotal\":" + subtotal.toPlainString()
                        + ",\"lineSubtotal\":" + lineSubtotal
                        + ",\"message\":" + AjaxResponse.quote(message) + "}");
    }

    private String noticeText(String code) {
        return switch (nvl(code)) {
            case "added" -> "Đã thêm sản phẩm vào giỏ hàng.";
            case "updated" -> "Đã cập nhật số lượng.";
            case "removed" -> "Đã xóa sản phẩm khỏi giỏ hàng.";
            case "cleared" -> "Đã xóa toàn bộ giỏ hàng.";
            case "error" -> null; // giữ nguyên message từ URL
            default -> null;
        };
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    private static long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
