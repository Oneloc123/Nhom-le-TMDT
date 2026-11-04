package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.user;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.CartService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.MailService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.PortalRedirect;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.UserAuthService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.OtpHasher;

import java.io.IOException;

@WebServlet(name = "userLoginV2", value = "/login")
public class LoginController extends UserControllerSupport {

    public static final long OTP_TTL_MS = 5 * 60_000L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (currentUser(request) != null) {
            response.sendRedirect(request.getContextPath()
                    + PortalRedirect.afterLogin(currentUser(request)));
            return;
        }
        prepare(request);
        request.getRequestDispatcher("/WEB-INF/views/user/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        prepare(request);

        String email = value(request.getParameter("email"));
        String password = request.getParameter("password");
        String redirectParam = request.getParameter("redirect");

        try {
            // 1) Xác thực email + mật khẩu (BCrypt – password thật)
            User user = UserAuthService.authenticate(email, password,
                    request.getRemoteAddr(), request.getHeader("User-Agent"));

            // 2) Gộp giỏ khách TRƯỚC khi invalidate session cũ
            HttpSession old = request.getSession(false);
            try { new CartService().mergeGuestCart(old, user); }
            catch (RuntimeException e) {
                System.getLogger(LoginController.class.getName())
                        .log(System.Logger.Level.WARNING,
                                "Không thể gộp giỏ khách vào tài khoản.", e);
            }

            // 3) Sinh OTP + gửi mail
            String otp = MailService.randomCode();
            try {
                MailService.sendLoginOtp(user.getEmail(), user.getFullName(), otp);
            } catch (RuntimeException e) {
                System.getLogger(LoginController.class.getName())
                        .log(System.Logger.Level.WARNING,
                                "Không gửi được email OTP — vẫn cho nhập DEMO_OTP nếu bật.", e);
            }

            // 4) Rotate session
            if (old != null) old.invalidate();
            HttpSession session = request.getSession(true);
            session.setMaxInactiveInterval(10 * 60);

            session.setAttribute("pendingLoginUserId", user.getId());
            session.setAttribute("pendingLoginEmail", user.getEmail());
            session.setAttribute("pendingLoginName", user.getFullName());
            // ★ OTP hash bằng OtpHasher (SHA-256 + salt), KHÔNG phải BCrypt
            session.setAttribute("pendingLoginOtpHash", OtpHasher.hash(otp));
            session.setAttribute("pendingLoginExpiry", System.currentTimeMillis() + OTP_TTL_MS);
            session.setAttribute("pendingLoginAttempts", 0);
            session.setAttribute("pendingLoginResendAt", System.currentTimeMillis());

            if (redirectParam != null && !redirectParam.isBlank())
                session.setAttribute("pendingLoginRedirect", redirectParam);

            session.setAttribute("csrfToken", Csrf.ensureToken(request));

            response.sendRedirect(request.getContextPath() + "/login/otp");

        } catch (RuntimeException e) {
            request.setAttribute("error",
                    e.getMessage() == null ? "Không thể đăng nhập lúc này." : e.getMessage());
            request.setAttribute("email", email);
            request.getRequestDispatcher("/WEB-INF/views/user/login.jsp")
                    .forward(request, response);
        }
    }
}