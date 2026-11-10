package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.user;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.UserAuthService;

import java.io.IOException;

@WebServlet(name = "userRegisterV2", value = "/register")
public class RegisterController extends UserControllerSupport {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (currentUser(req) != null) {
            resp.sendRedirect(req.getContextPath() + "/account");
            return;
        }
        prepare(req);
        req.getRequestDispatcher("/WEB-INF/views/user/register.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        prepare(req);
        String name = value(req.getParameter("fullName"));
        String email = value(req.getParameter("email"));
        String phone = value(req.getParameter("phone"));
        String password = req.getParameter("password");
        String confirm = req.getParameter("confirmPassword");
        try {
            if (password == null || !password.equals(confirm))
                throw new IllegalArgumentException("Mật khẩu xác nhận không khớp.");
            var pending = UserAuthService.registerPending(name, email, phone, password);
            HttpSession session = req.getSession(true);
            session.setMaxInactiveInterval(1800);
            session.setAttribute("pendingVerificationEmail", pending.getEmail());
            resp.sendRedirect(req.getContextPath() + "/register/otp");
        } catch (RuntimeException e) {
            req.setAttribute("error", e.getMessage() == null ? "Không thể tạo tài khoản." : e.getMessage());
            req.setAttribute("fullName", name);
            req.setAttribute("email", email);
            req.setAttribute("phone", phone);
            req.getRequestDispatcher("/WEB-INF/views/user/register.jsp").forward(req, resp);
        }
    }
}
