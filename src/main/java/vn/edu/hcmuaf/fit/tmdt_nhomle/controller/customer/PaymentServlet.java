package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.OrderDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Order;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.OrderStatus;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.AuthFilter;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.PaymentService;

import java.io.IOException;

/**
 * Thanh toán đơn hàng.
 * - GET: chỉ hiển thị đơn thuộc về người dùng đang đăng nhập.
 * - POST: COD được xử lý thật (payments + xác nhận đơn, transaction).
 *   Cổng VNPay/MoMo/thẻ chưa cấu hình → thông báo rõ, không giả lập thành công.
 */
@WebServlet(name = "paymentServlet", value = "/payment")
public class PaymentServlet extends HttpServlet {

    private final PaymentService paymentService = new PaymentService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = AuthFilter.currentUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        Order order = findCustomerOrder(request.getParameter("orderNumber"), user);
        if (order == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            request.setAttribute("message", "Không tìm thấy đơn hàng.");
            request.getRequestDispatcher("/customer/not-found.jsp").forward(request, response);
            return;
        }
        if (!OrderStatus.PENDING_PAYMENT.equals(order.getStatus())) {
            // Đơn đã xử lý/hủy: quay về lịch sử, không báo thanh toán thành công sai.
            response.sendRedirect(request.getContextPath() + "/purchase-history");
            return;
        }
        OrderDao.loadDetails(order);
        request.setAttribute("order", order);
        request.getRequestDispatcher("/customer/payment.jsp").forward(request, response);
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
        try {
            paymentService.pay(request.getParameter("orderNumber"), user.getId(),
                    request.getParameter("method"));
            request.getSession(true).setAttribute("orderNotice",
                    "Đơn hàng của bạn đã được xác nhận.");
            response.sendRedirect(request.getContextPath() + "/purchase-history");
        } catch (RuntimeException error) {
            Order order = findCustomerOrder(request.getParameter("orderNumber"), user);
            if (order == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                request.setAttribute("message", "Không tìm thấy đơn hàng.");
                request.getRequestDispatcher("/customer/not-found.jsp").forward(request, response);
                return;
            }
            OrderDao.loadDetails(order);
            request.setAttribute("order", order);
            request.setAttribute("error", error.getMessage());
            request.getRequestDispatcher("/customer/payment.jsp").forward(request, response);
        }
    }

    private Order findCustomerOrder(String orderCode, User user) {
        return orderCode == null ? null
                : OrderDao.findByCodeForUser(orderCode, user.getId()).orElse(null);
    }
}
