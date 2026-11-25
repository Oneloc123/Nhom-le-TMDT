package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.user;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.CustomerAccountService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "customerOrdersV2", urlPatterns = {"/purchase-history", "/order-detail"})
public class CustomerOrdersController extends UserControllerSupport {
    private static final Logger LOGGER = Logger.getLogger(CustomerOrdersController.class.getName());
    private static final Set<String> FILTERS = Set.of(
            "ALL", "Chờ thanh toán", "Đã xác nhận", "Đang xử lý",
            "Đang giao hàng", "Đã hoàn thành", "Đã hủy"
    );

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = currentUser(req);
        prepare(req);
        String path = req.getServletPath();

        if ("/order-detail".equals(path)) {
            String code = value(req.getParameter("code"));
            var order = CustomerAccountService.orderDetail(user.getId(), code);
            if (order.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                req.getRequestDispatcher("/WEB-INF/views/errors/404.jsp").forward(req, resp);
                return;
            }
            req.setAttribute("order", order.get());
            req.getRequestDispatcher("/WEB-INF/views/user/order-detail.jsp").forward(req, resp);
            return;
        }

        String status = value(req.getParameter("status"));
        if (!FILTERS.contains(status)) status = "ALL";
        int page = parsePage(req.getParameter("page"));
        int size = 10;
        long total = CustomerAccountService.countOrders(user.getId(), status);
        int totalPages = (int) Math.max(1, Math.ceil(total / (double) size));
        if (page > totalPages) page = totalPages;

        req.setAttribute("orders", CustomerAccountService.listOrders(user.getId(), status, page, size));
        req.setAttribute("status", status);
        req.setAttribute("currentPage", page);
        req.setAttribute("totalPages", totalPages);
        req.setAttribute("totalOrders", total);
        req.getRequestDispatcher("/WEB-INF/views/user/orders.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = currentUser(req);
        String code = value(req.getParameter("code"));
        String action = value(req.getParameter("action"));

        if (!"cancel".equals(action)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String reason = value(req.getParameter("reason"));
        if (reason.isBlank()) {
            req.setAttribute("error", "Vui lòng nhập lý do hủy đơn hàng trước khi xác nhận.");
        } else if (reason.length() > 500) {
            req.setAttribute("error", "Lý do hủy không được vượt quá 500 ký tự.");
        } else {
            try {
                boolean cancelled = CustomerAccountService.cancelOrder(user.getId(), code, reason);
                if (!cancelled) {
                    req.setAttribute("error",
                            "Đơn hàng không còn đủ điều kiện hủy trực tiếp hoặc không thuộc tài khoản của bạn. Trạng thái đơn có thể đã thay đổi.");
                } else {
                    resp.sendRedirect(req.getContextPath() + "/order-detail?code="
                            + URLEncoder.encode(code, StandardCharsets.UTF_8) + "&cancelled=1");
                    return;
                }
            } catch (IllegalArgumentException e) {
                req.setAttribute("error", e.getMessage());
            } catch (DataAccessException e) {
                LOGGER.log(Level.SEVERE, "Không thể hủy đơn hàng " + code, e);
                req.setAttribute("error",
                        "Hệ thống chưa thể hoàn tất yêu cầu hủy đơn. Vui lòng tải lại trang và thử lại.");
            }
        }

        prepare(req);
        var order = CustomerAccountService.orderDetail(user.getId(), code);
        if (order.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        req.setAttribute("order", order.get());
        req.setAttribute("cancelReasonInput", reason);
        req.getRequestDispatcher("/WEB-INF/views/user/order-detail.jsp").forward(req, resp);
    }

    private int parsePage(String value) {
        try {
            return Math.max(1, Integer.parseInt(value));
        } catch (Exception e) {
            return 1;
        }
    }
}
