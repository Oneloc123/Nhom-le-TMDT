package vn.edu.hcmuaf.fit.tmdt_nhomle;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;

import java.io.IOException;

/** Serves the Admin UI shell for every /admin route, including direct deep links. */
@WebServlet(urlPatterns = "/admin/*")
public class AdminRouterServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Tạm bỏ kiểm tra session để test giao diện Admin; bật lại sau khi test.
        /*
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute("currentUser");
        if (value instanceof User user && user.isAdmin() && user.isActive()) {
            request.getRequestDispatcher("/WEB-INF/admin/index.jsp").forward(request, response);
            return;
        }
        if (value instanceof User user && !user.isAdmin()) {
            response.sendRedirect(request.getContextPath() + "/customer/home.jsp");
            return;
        }
        if (session != null && value instanceof User) {
            session.invalidate();
        }
        if (value == null || value instanceof User) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        response.sendRedirect(request.getContextPath() + "/login");
        */
        request.getRequestDispatcher("/WEB-INF/admin/index.jsp").forward(request, response);
    }
}
