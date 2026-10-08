package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.UserStore;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;

import java.io.IOException;

/**
 * Đăng nhập thường (email + mật khẩu).
 */
@WebServlet(name = "loginServlet", value = "/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User current = (session == null) ? null : (User) session.getAttribute("currentUser");
        if (current != null) {
            response.sendRedirect(request.getContextPath()
                    + (current.isAdmin() ? "/admin/dashboard" : "/customer/home.jsp"));
            return;
        }
        request.getRequestDispatcher("/customer/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        User user = UserStore.findByEmail(email).orElse(null);

        if (user == null || user.getPassword() == null || !user.getPassword().equals(password)) {
            request.setAttribute("error", "Email hoặc mật khẩu không đúng.");
            request.getRequestDispatcher("/customer/login.jsp").forward(request, response);
            return;
        }
        if (User.STATUS_LOCKED.equals(user.getStatus())) {
            request.setAttribute("error", "Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.");
            request.getRequestDispatcher("/customer/login.jsp").forward(request, response);
            return;
        }
        if (User.STATUS_PENDING.equals(user.getStatus())) {
            request.setAttribute("error", "Tài khoản chưa được kích hoạt. Vui lòng kiểm tra email để xác thực.");
            request.getRequestDispatcher("/customer/login.jsp").forward(request, response);
            return;
        }

        HttpSession session = request.getSession(true);
        session.setAttribute("currentUser", user);
        response.sendRedirect(request.getContextPath()
                + (user.isAdmin() ? "/admin/dashboard" : "/customer/home.jsp"));
    }
}
