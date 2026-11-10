package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.user;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.UserAuthService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.BcryptPasswordService;

import java.io.IOException;

@WebServlet(name = "userForgotPasswordV2", urlPatterns = {"/forgot-password", "/forgot-password/otp"})
public class ForgotPasswordController extends UserControllerSupport {
    private static final String GENERIC = "Nếu email tồn tại và đang hoạt động, mã xác thực đã được gửi. Vui lòng kiểm tra hộp thư.";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        prepare(req);
        if (req.getServletPath().endsWith("/otp")) {
            HttpSession session = req.getSession(false);
            String email = session == null ? null : (String) session.getAttribute("pendingResetEmail");
            if (email == null || email.isBlank()) {
                resp.sendRedirect(req.getContextPath() + "/forgot-password");
                return;
            }
            req.setAttribute("email", email);
            req.setAttribute("info", GENERIC);
            req.getRequestDispatcher("/WEB-INF/views/user/forgot-password-otp.jsp").forward(req, resp);
            return;
        }
        req.getRequestDispatcher("/WEB-INF/views/user/forgot-password.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        prepare(req);
        if (!req.getServletPath().endsWith("/otp")) {
            String email = value(req.getParameter("email")).toLowerCase();
            if (email.isBlank() || email.length() > 190) {
                req.setAttribute("error", "Vui lòng nhập email hợp lệ.");
                req.setAttribute("email", email);
                req.getRequestDispatcher("/WEB-INF/views/user/forgot-password.jsp").forward(req, resp);
                return;
            }
            UserAuthService.requestPasswordReset(email);
            req.getSession(true).setAttribute("pendingResetEmail", email);
            resp.sendRedirect(req.getContextPath() + "/forgot-password/otp");
            return;
        }

        HttpSession session = req.getSession(false);
        String email = session == null ? null : (String) session.getAttribute("pendingResetEmail");
        if (email == null || email.isBlank()) {
            resp.sendRedirect(req.getContextPath() + "/forgot-password");
            return;
        }
        String action = value(req.getParameter("action"));
        if ("resend".equals(action)) {
            UserAuthService.requestPasswordReset(email);
            req.setAttribute("info", GENERIC + " Nếu đủ thời gian gửi lại, mã mới sẽ được gửi.");
        } else {
            String password = req.getParameter("password");
            try {
                BcryptPasswordService.validate(password);
                if (!password.equals(req.getParameter("confirmPassword"))) {
                    req.setAttribute("error", "Mật khẩu xác nhận không khớp.");
                } else if (UserAuthService.resetPassword(email, value(req.getParameter("otp")), password)) {
                    session.removeAttribute("pendingResetEmail");
                    req.setAttribute("resetOk", "Đặt lại mật khẩu thành công. Hãy đăng nhập bằng mật khẩu mới.");
                    req.getRequestDispatcher("/WEB-INF/views/user/login.jsp").forward(req, resp);
                    return;
                } else {
                    req.setAttribute("error", "Mã OTP không đúng, đã hết hạn hoặc đã vượt số lần thử.");
                }
            } catch (IllegalArgumentException invalidPassword) {
                req.setAttribute("error", invalidPassword.getMessage());
            }
        }
        req.setAttribute("email", email);
        req.getRequestDispatcher("/WEB-INF/views/user/forgot-password-otp.jsp").forward(req, resp);
    }
}
