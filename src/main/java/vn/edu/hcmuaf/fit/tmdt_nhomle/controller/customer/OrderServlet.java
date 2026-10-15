package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.CartService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.OrderService;

import java.io.IOException;

@WebServlet(name = "orderServlet", value = "/order")
public class OrderServlet extends HttpServlet {
    private final CartService cartService = new CartService();
    private final OrderService orderService = new OrderService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = currentUser(request.getSession(false));
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        if (cartService.items(request.getSession(true)).isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/cart?error=empty");
            return;
        }
        populate(request, user);
        request.getRequestDispatcher("/customer/order.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = currentUser(request.getSession(false));
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        request.setCharacterEncoding("UTF-8");
        String phone = request.getParameter("phone");
        String address = request.getParameter("address");
        try {
            var order = orderService.createOrder(user, phone, address, request.getSession(true));
            response.sendRedirect(request.getContextPath()
                    + "/payment?orderNumber=" + order.getOrderNumber());
        } catch (RuntimeException error) {
            request.setAttribute("error", error.getMessage());
            populate(request, user);
            request.getRequestDispatcher("/customer/order.jsp").forward(request, response);
        }
    }

    private void populate(HttpServletRequest request, User user) {
        HttpSession session = request.getSession(true);
        request.setAttribute("user", user);
        request.setAttribute("cartItems", cartService.items(session));
        request.setAttribute("cartSubtotal", cartService.subtotal(session));
    }

    private User currentUser(HttpSession session) {
        return session == null ? null : (User) session.getAttribute("currentUser");
    }
}
