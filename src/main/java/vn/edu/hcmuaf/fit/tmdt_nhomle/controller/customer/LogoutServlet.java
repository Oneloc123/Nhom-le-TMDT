package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.SecurityEventDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;

import java.io.IOException;
import java.util.Map;

/**
 * Đăng xuất: vô hiệu hoá phiên phía SERVER (invalidate), không chỉ xoá cookie.
 * Nhận POST (ưu tiên, đã có CSRF ở AuthFilter) và GET (tương thích link cũ).
 * Trả JSON cho request AJAX, redirect cho trang thường.
 */
@WebServlet(name = "logoutServlet", value = "/logout")
public class LogoutServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        logout(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        logout(request, response);
    }

    private void logout(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        User user = session == null ? null
                : (session.getAttribute("currentUser") instanceof User u ? u : null);
        if (user != null) {
            SecurityEventDao.record((long) user.getId(), null,
                    SecurityEventDao.EVENT_LOGOUT, "Đăng xuất", request.getRemoteAddr());
        }
        if (session != null) {
            session.invalidate();
        }

        boolean wantsJson = "1".equals(request.getParameter("ajax"))
                || "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"));
        if (wantsJson) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json;charset=UTF-8");
            response.setHeader("Cache-Control", "no-store");
            response.getWriter().write(vn.edu.hcmuaf.fit.tmdt_nhomle.util.Json.stringify(
                    Map.of("success", true,
                            "message", "Đã đăng xuất.",
                            "data", Map.of("redirect", request.getContextPath() + "/login"))));
            return;
        }
        response.sendRedirect(request.getContextPath() + "/login");
    }
}
