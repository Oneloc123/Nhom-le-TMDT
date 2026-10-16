package vn.edu.hcmuaf.fit.tmdt_nhomle;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;

import java.io.IOException;

@WebServlet(urlPatterns = "/admin/*")
public class AdminRouterServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Chặn phía server: không cho người chưa đăng nhập / không phải admin
        // mở giao diện quản trị chỉ bằng cách gõ URL.
        HttpSession session = request.getSession(false);
        User current = (session == null) ? null : (User) session.getAttribute("currentUser");
        if (current == null || !current.isAdmin()) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        request.getRequestDispatcher("/WEB-INF/admin/index.jsp").forward(request, response);
    }
}
