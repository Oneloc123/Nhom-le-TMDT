package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.fake.FakeNotificationApi;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Notification;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf;

import java.io.IOException;
import java.util.List;

@WebServlet("/notifications")
public class NotificationsServlet extends HttpServlet {

    private static final int PAGE_SIZE = 6;
    private final FakeNotificationApi api = FakeNotificationApi.getInstance();

    // ============================ GET ============================
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Long userId = resolveUserId(req, resp);
        if (userId == null) return;   // đã redirect /login

        int page = parsePositiveInt(req.getParameter("page"), 1);

        List<Notification> items = api.list(userId, page, PAGE_SIZE);
        int total = api.total(userId);
        int unread = api.unreadCount(userId);
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));

        req.setAttribute("notifications", items);
        req.setAttribute("unreadNotifications", unread);
        req.setAttribute("page", Math.min(page, totalPages));
        req.setAttribute("totalPages", totalPages);
        req.setAttribute("unreadMessages", unread);   // sidebar dùng chung

        req.getRequestDispatcher("/customer/notifications.jsp").forward(req, resp);
    }

    // ============================ POST ===========================
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Long userId = resolveUserId(req, resp);
        if (userId == null) return;

        // CSRF: so khớp token gửi lên với token trong session
        String submitted = req.getParameter("csrfToken");
        String expected = Csrf.ensureToken(req);
        if (submitted == null || !submitted.equals(expected)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "CSRF token không hợp lệ");
            return;
        }

        String action = req.getParameter("action");
        if ("markAll".equals(action)) {
            api.markAllRead(userId);
        } else if ("markOne".equals(action)) {
            long id = parsePositiveInt(req.getParameter("id"), -1);
            if (id > 0) api.markRead(userId, id);
        }

        // PRG (Post-Redirect-Get) tránh F5 resubmit
        resp.sendRedirect(req.getContextPath() + "/notifications?read=1");
    }

    // ============================ Helpers ========================
    private Long resolveUserId(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        Object u = req.getSession().getAttribute("currentUser");
        if (!(u instanceof User user)) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return null;
        }
        // Nếu User không có getId(), thay bằng: user.getEmail().hashCode() & 0xffffffffL
        return (long)user.getId();
    }

    private int parsePositiveInt(String s, int def) {
        if (s == null || s.isBlank()) return def;
        try {
            int v = Integer.parseInt(s.trim());
            return v > 0 ? v : def;
        } catch (NumberFormatException e) {
            return def;
        }
    }
}