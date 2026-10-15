package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.OrderStore;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Order;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.PaymentService;

import java.io.IOException;

@WebServlet(name = "paymentServlet", value = "/payment")
public class PaymentServlet extends HttpServlet {
    private final PaymentService paymentService = new PaymentService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = currentUser(request.getSession(false));
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        Order order = findCustomerOrder(request.getParameter("orderNumber"), user.getId());
        if (order == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy đơn hàng.");
            return;
        }
        request.setAttribute("order", order);
        request.getRequestDispatcher("/customer/payment.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = currentUser(request.getSession(false));
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        try {
            paymentService.pay(request.getParameter("orderNumber"), user.getId(),
                    request.getParameter("method"));
            response.sendRedirect(request.getContextPath()
                    + "/purchase-history?paid=1");
        } catch (RuntimeException error) {
            Order order = findCustomerOrder(request.getParameter("orderNumber"), user.getId());
            request.setAttribute("order", order);
            request.setAttribute("error", error.getMessage());
            request.getRequestDispatcher("/customer/payment.jsp").forward(request, response);
        }
    }

    private Order findCustomerOrder(String orderNumber, int customerId) {
        return orderNumber == null ? null : OrderStore.findByNumber(orderNumber)
                .filter(order -> order.getCustomerId() == customerId)
                .orElse(null);
    }

    private User currentUser(HttpSession session) {
        return session == null ? null : (User) session.getAttribute("currentUser");
    }
}
