package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.OrderDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Order;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.OrderStatus;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.AuthFilter;

import java.io.IOException;

/**
 * Chi tiết đơn hàng của chính người dùng:
 * - GET /order-detail?code=PK-...: dòng hàng + lịch sử trạng thái + vận chuyển
 *   + tiến độ sản xuất (mọi truy vấn đều scope theo user của phiên).
 * - POST action=cancel: hủy đơn khi trạng thái hiện tại được phép
 *   (OrderStatus.cancellableByCustomer), ghi lịch sử; không nhận trạng thái
 *   tùy ý từ trình duyệt.
 */
@WebServlet(name = "orderDetailServlet", value = "/order-detail")
public class OrderDetailServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = AuthFilter.currentUser(request);
        Order order = findOwnedOrder(request, user);
        if (order == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            request.setAttribute("message", "Không tìm thấy đơn hàng hoặc bạn không có quyền xem.");
            request.getRequestDispatcher("/customer/not-found.jsp").forward(request, response);
            return;
        }
        OrderDao.loadDetails(order);
        request.setAttribute("order", order);
        request.setAttribute("cancelled", "1".equals(request.getParameter("cancelled")));
        request.getRequestDispatcher("/customer/order-detail.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = AuthFilter.currentUser(request);
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        if (!"cancel".equals(action)) {
            response.sendRedirect(request.getContextPath() + "/order-detail");
            return;
        }
        Order order = findOwnedOrder(request, user);
        if (order == null) {
            response.sendRedirect(request.getContextPath() + "/purchase-history");
            return;
        }
        try {
            OrderDao.updateStatus(order.getOrderId(), user.getId(), OrderStatus.CANCELLED, null,
                    "Khách hàng đã hủy đơn");
            response.sendRedirect(request.getContextPath()
                    + "/order-detail?code=" + java.net.URLEncoder.encode(order.getOrderCode(),
                            java.nio.charset.StandardCharsets.UTF_8) + "&cancelled=1");
        } catch (DataAccessException error) {
            request.setAttribute("error", error.getMessage());
            OrderDao.loadDetails(order);
            request.setAttribute("order", order);
            request.getRequestDispatcher("/customer/order-detail.jsp").forward(request, response);
        }
    }

    private Order findOwnedOrder(HttpServletRequest request, User user) {
        if (user == null) {
            return null;
        }
        String code = request.getParameter("code");
        return code == null ? null : OrderDao.findByCodeForUser(code, user.getId()).orElse(null);
    }
}
