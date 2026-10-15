package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.CartService;

import java.io.IOException;

@WebServlet(name = "cartServlet", value = "/cart")
public class CartServlet extends HttpServlet {
    private final CartService cartService = new CartService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        populate(request);
        request.getRequestDispatcher("/customer/cart.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        int productId = parseInt(request.getParameter("productId"), -1);

        try {
            if ("add".equals(action)) {
                cartService.add(request.getSession(true), productId,
                        Math.max(1, parseInt(request.getParameter("quantity"), 1)));
            } else if ("update".equals(action)) {
                cartService.update(request.getSession(true), productId,
                        parseInt(request.getParameter("quantity"), 0));
            } else if ("remove".equals(action)) {
                cartService.remove(request.getSession(true), productId);
            } else if ("clear".equals(action)) {
                cartService.clear(request.getSession(true));
            }
        } catch (RuntimeException ignored) {
            // Dữ liệu nhập sai không làm hỏng phiên mua hàng hiện tại.
        }
        response.sendRedirect(request.getContextPath() + "/cart");
    }

    private void populate(HttpServletRequest request) {
        var session = request.getSession(true);
        request.setAttribute("cartItems", cartService.items(session));
        request.setAttribute("cartTotalQuantity", cartService.totalQuantity(session));
        request.setAttribute("cartSubtotal", cartService.subtotal(session));
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
