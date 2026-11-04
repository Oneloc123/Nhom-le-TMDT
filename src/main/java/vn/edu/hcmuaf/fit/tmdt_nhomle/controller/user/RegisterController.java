package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.user;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.UserAuthService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.UserDao;
import java.io.IOException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.MailService;

/** Đăng ký và xác minh email; mã OTP không được lưu dạng rõ trong session hoặc DB. */
@WebServlet(name = "userRegisterV2", value = "/register")
public class RegisterController extends UserControllerSupport {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (currentUser(req) != null) {
            resp.sendRedirect(req.getContextPath() + "/account");
            return;
        }
        prepare(req);
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("pendingVerificationEmail") instanceof String email) {
            req.setAttribute("step", "verify");
            req.setAttribute("email", email);
        }
        req.getRequestDispatcher("/WEB-INF/views/user/register.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        prepare(req);
        String action = value(req.getParameter("action"));
        if ("verify".equals(action)) {
            verify(req, resp);
        } else if ("resend".equals(action)) {
            resend(req, resp);
        } else {
            register(req, resp);
        }
    }

    private void register(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String name = value(req.getParameter("fullName"));
        String email = value(req.getParameter("email"));
        String phone = value(req.getParameter("phone"));
        String password = req.getParameter("password");
        String confirm = req.getParameter("confirmPassword");
        try {
            if (password == null || !password.equals(confirm)) {
                throw new IllegalArgumentException("Mật khẩu xác nhận không khớp.");
            }
            var pending = UserAuthService.registerPending(name, email, phone, password);
            HttpSession session = req.getSession(true);
            session.setMaxInactiveInterval(1800);
            session.setAttribute("pendingVerificationEmail", pending.getEmail());
            req.setAttribute("step", "verify");
            req.setAttribute("email", pending.getEmail());
            req.setAttribute("success", "Mã xác thực đã được gửi. Vui lòng kiểm tra email và nhập mã 6 chữ số.");
        } catch (RuntimeException e) {
            req.setAttribute("error", friendly(e, "Không thể tạo tài khoản."));
            req.setAttribute("fullName", name);
            req.setAttribute("email", email);
            req.setAttribute("phone", phone);
        }
        req.getRequestDispatcher("/WEB-INF/views/user/register.jsp").forward(req, resp);
    }

    private void verify(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        String pendingEmail = session == null ? null : (String) session.getAttribute("pendingVerificationEmail");
        String submittedEmail = value(req.getParameter("email"));
        if (pendingEmail == null || !pendingEmail.equalsIgnoreCase(submittedEmail)) {
            req.setAttribute("error", "Phiên xác thực không còn hợp lệ. Vui lòng đăng ký hoặc gửi lại mã.");
            req.getRequestDispatcher("/WEB-INF/views/user/register.jsp").forward(req, resp);
            return;
        }
        if (UserAuthService.verifyRegistration(pendingEmail, value(req.getParameter("code")))) {
            session.removeAttribute("pendingVerificationEmail");
            try {
                UserDao.findByEmail(pendingEmail).ifPresent(u ->
                        MailService.sendWelcome(u.getEmail(), u.getFullName()));
            } catch (RuntimeException ignored) { }
            resp.sendRedirect(req.getContextPath() + "/login?registered=1");
            return;
        }
        req.setAttribute("step", "verify");
        req.setAttribute("email", pendingEmail);
        req.setAttribute("error", "Mã không đúng, đã hết hạn hoặc vượt quá số lần thử. Mã có hiệu lực 15 phút và tối đa 5 lần nhập.");
        req.getRequestDispatcher("/WEB-INF/views/user/register.jsp").forward(req, resp);
    }

    private void resend(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        String pendingEmail = session == null ? null : (String) session.getAttribute("pendingVerificationEmail");
        if (pendingEmail == null || !pendingEmail.equalsIgnoreCase(value(req.getParameter("email")))) {
            req.setAttribute("error", "Phiên xác thực không còn hợp lệ. Vui lòng đăng ký lại.");
            req.getRequestDispatcher("/WEB-INF/views/user/register.jsp").forward(req, resp);
            return;
        }
        boolean sent = UserAuthService.resendRegistrationCode(pendingEmail);
        req.setAttribute("step", "verify");
        req.setAttribute("email", pendingEmail);
        req.setAttribute(sent ? "success" : "error", sent
                ? "Mã xác thực mới đã được gửi. Hãy kiểm tra hộp thư."
                : "Bạn chỉ có thể yêu cầu gửi lại mã sau 60 giây hoặc tài khoản không còn chờ xác minh.");
        req.getRequestDispatcher("/WEB-INF/views/user/register.jsp").forward(req, resp);
    }

    private static String friendly(RuntimeException error, String fallback) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? fallback : message;
    }
}
