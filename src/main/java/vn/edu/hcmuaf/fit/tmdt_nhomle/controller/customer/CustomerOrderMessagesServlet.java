package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.OrderMessageDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.AuthFilter;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet(name = "customerOrderMessagesServlet", value = "/messages")
public class CustomerOrderMessagesServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = AuthFilter.currentUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        long orderId = parsePositive(request.getParameter("orderId"));
        long shopId = parsePositive(request.getParameter("shopId"));
        if ((orderId > 0) != (shopId > 0)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Liên kết hội thoại không hợp lệ.");
            return;
        }
        if (orderId > 0 && !OrderMessageDao.ownsThread(orderId, shopId, user.getId())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        render(request, response, user, orderId, shopId, null);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        User user = AuthFilter.currentUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        long orderId = parsePositive(request.getParameter("orderId"));
        long shopId = parsePositive(request.getParameter("shopId"));
        String message = request.getParameter("message");
        if (orderId <= 0 || shopId <= 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Hội thoại không hợp lệ.");
            return;
        }
        try {
            if (message == null || message.trim().isEmpty() || message.trim().length() > 2000) {
                throw new IllegalArgumentException("Tin nhắn cần từ 1 đến 2.000 ký tự.");
            }
            OrderMessageDao.send(orderId, shopId, user.getId(), message.trim());
            response.sendRedirect(request.getContextPath() + "/messages?orderId=" + orderId
                    + "&shopId=" + shopId + "&sent=1");
        } catch (IllegalArgumentException e) {
            if (!OrderMessageDao.ownsThread(orderId, shopId, user.getId())) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            render(request, response, user, orderId, shopId, e.getMessage());
        } catch (RuntimeException e) {
            System.getLogger(CustomerOrderMessagesServlet.class.getName()).log(
                    System.Logger.Level.ERROR, "Không thể tải hoặc gửi tin nhắn hội thoại.", e);
            render(request, response, user, orderId, shopId,
                    "Không thể gửi tin nhắn lúc này. Vui lòng thử lại sau.");
        }
    }

    private void render(HttpServletRequest request, HttpServletResponse response, User user,
                        long orderId, long shopId, String error)
            throws ServletException, IOException {
        try {
            request.setAttribute("csrfToken", Csrf.ensureToken(request));
            List<Map<String, Object>> threads = OrderMessageDao.threadsForUser(user.getId());
            request.setAttribute("threads", threads);
            long unreadMessages = 0;
            for (Map<String, Object> thread : threads) {
                Object unread = thread.get("unread");
                if (unread instanceof Number number) unreadMessages += Math.max(0, number.longValue());
            }
            request.setAttribute("unreadMessages", unreadMessages);
            if (orderId > 0 && shopId > 0) {
                List<Map<String, Object>> messages = OrderMessageDao.threadForUser(orderId, shopId, user.getId());
                if (messages == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                request.setAttribute("threadOrderId", orderId);
                request.setAttribute("threadShopId", shopId);
                request.setAttribute("threadMessages", messages);
            }
            if (error != null && !error.isBlank()) request.setAttribute("error", error);
            request.getRequestDispatcher("/customer/messages.jsp").forward(request, response);
        } catch (RuntimeException e) {
            System.getLogger(CustomerOrderMessagesServlet.class.getName()).log(
                    System.Logger.Level.ERROR, "Không thể tải hộp thư của khách hàng.", e);
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Chưa thể tải tin nhắn xưởng lúc này. Vui lòng thử lại sau.");
        }
    }

    private static long parsePositive(String raw) {
        if (raw == null || raw.isBlank()) return 0;
        try {
            long value = Long.parseLong(raw.trim());
            return value > 0 ? value : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}