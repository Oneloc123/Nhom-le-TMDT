package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.user;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.UserDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.MailService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.UserAuthService;

import java.io.IOException;

@WebServlet(name = "registerOtpV2", value = "/register/otp")
public class RegisterOtpServlet extends UserControllerSupport {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        String email = session == null ? null : (String) session.getAttribute("pendingVerificationEmail");
        if (email == null || email.isBlank()) {
            resp.sendRedirect(req.getContextPath() + "/register");
            return;
        }
        prepare(req);
        req.setAttribute("email", email);
        req.getRequestDispatcher("/WEB-INF/views/user/register-otp.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        prepare(req);
        HttpSession session = req.getSession(false);
        String email = session == null ? null : (String) session.getAttribute("pendingVerificationEmail");
        if (email == null || email.isBlank()) {
            resp.sendRedirect(req.getContextPath() + "/register");
            return;
        }
        String action = value(req.getParameter("action"));
        if ("resend".equals(action)) {
            boolean sent = UserAuthService.resendRegistrationCode(email);
            req.setAttribute(sent ? "success" : "error", sent
                    ? "Nếu tài khoản còn chờ xác thực, mã mới đã được gửi."
                    : "Vui lòng đợi 60 giây trước khi gửi lại mã hoặc đăng ký lại.");
        } else if (UserAuthService.verifyRegistration(email, value(req.getParameter("code")))) {
            session.removeAttribute("pendingVerificationEmail");
            try { UserDao.findByEmail(email).ifPresent(user -> MailService.sendWelcome(user.getEmail(), user.getDisplayName())); }
            catch (RuntimeException ignored) { }
            resp.sendRedirect(req.getContextPath() + "/login?registered=1");
            return;
        } else {
            req.setAttribute("error", "Mã OTP không đúng, đã hết hạn hoặc vượt số lần thử.");
        }
        req.setAttribute("email", email);
        req.getRequestDispatcher("/WEB-INF/views/user/register-otp.jsp").forward(req, resp);
    }
}
