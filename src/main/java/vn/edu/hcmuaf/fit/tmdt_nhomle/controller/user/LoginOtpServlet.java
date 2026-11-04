package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.user;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.UserDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.UserSessionRegistry;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.OtpHasher;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.MailService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.PortalRedirect;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf;

import java.io.IOException;

/** Bước 2 đăng nhập: xác thực mã OTP gửi qua email. */
@WebServlet(name = "loginOtpV2", value = "/login/otp")
public class LoginOtpServlet extends UserControllerSupport {

    private static final int  MAX_ATTEMPTS       = 5;
    private static final long RESEND_COOLDOWN_MS = 60_000L;

    /* ====================== GET ====================== */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        User current = currentUser(req);
        if (current != null) {
            resp.sendRedirect(req.getContextPath() + PortalRedirect.afterLogin(current));
            return;
        }

        HttpSession session = req.getSession(false);
        Pending pending = Pending.read(session);
        if (pending == null || pending.isExpired()) {
            Pending.clear(session);
            resp.sendRedirect(req.getContextPath() + "/login?otpExpired=1");
            return;
        }

        prepare(req);
        req.setAttribute("email", pending.email);
        req.getRequestDispatcher("/WEB-INF/views/user/login-otp.jsp").forward(req, resp);
    }

    /* ====================== POST ====================== */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        prepare(req);

        HttpSession session = req.getSession(false);
        Pending pending = Pending.read(session);
        if (pending == null || pending.isExpired()) {
            Pending.clear(session);
            resp.sendRedirect(req.getContextPath() + "/login?otpExpired=1");
            return;
        }

        String action = value(req.getParameter("action"));

        /* ---------- Gửi lại OTP ---------- */
        if ("resend".equals(action)) {
            long since = System.currentTimeMillis() - pending.resendAt;
            if (since < RESEND_COOLDOWN_MS) {
                long wait = Math.max(1, (RESEND_COOLDOWN_MS - since) / 1000);
                req.setAttribute("error", "Vui lòng đợi " + wait + " giây trước khi gửi lại mã.");
                req.setAttribute("email", pending.email);
                req.getRequestDispatcher("/WEB-INF/views/user/login-otp.jsp").forward(req, resp);
                return;
            }
            String newOtp = MailService.randomCode();
            try { MailService.sendLoginOtp(pending.email, pending.name, newOtp); }
            catch (RuntimeException ignored) { }

            session.setAttribute("pendingLoginOtpHash", OtpHasher.hash(newOtp));
            session.setAttribute("pendingLoginExpiry", System.currentTimeMillis() + LoginController.OTP_TTL_MS);
            session.setAttribute("pendingLoginResendAt", System.currentTimeMillis());
            session.setAttribute("pendingLoginAttempts", 0);

            req.setAttribute("success", "Đã gửi lại mã đăng nhập. Vui lòng kiểm tra hộp thư.");
            req.setAttribute("email", pending.email);
            req.getRequestDispatcher("/WEB-INF/views/user/login-otp.jsp").forward(req, resp);
            return;
        }

        /* ---------- Xác thực OTP ---------- */
        if (pending.attempts >= MAX_ATTEMPTS) {
            Pending.clear(session);
            resp.sendRedirect(req.getContextPath() + "/login?otpLocked=1");
            return;
        }

        String otp = value(req.getParameter("otp"));
        if (otp.isBlank()) {
            req.setAttribute("error", "Vui lòng nhập mã OTP 6 chữ số.");
            req.setAttribute("email", pending.email);
            req.getRequestDispatcher("/WEB-INF/views/user/login-otp.jsp").forward(req, resp);
            return;
        }

        boolean ok = MailService.isDemoOtp(otp)
                || OtpHasher.verify(otp, pending.otpHash);

        if (!ok) {
            session.setAttribute("pendingLoginAttempts", pending.attempts + 1);
            int left = MAX_ATTEMPTS - pending.attempts - 1;
            req.setAttribute("error", "Mã OTP không đúng. Bạn còn " + left + " lần thử.");
            req.setAttribute("email", pending.email);
            req.getRequestDispatcher("/WEB-INF/views/user/login-otp.jsp").forward(req, resp);
            return;
        }

        /* ---------- OTP đúng — hoàn tất đăng nhập ---------- */
        User user = UserDao.findById(pending.userId).orElse(null);
        if (user == null || !user.isActive()) {
            Pending.clear(session);
            resp.sendRedirect(req.getContextPath() + "/login?error=account");
            return;
        }

        String redirectAttr = pending.redirect;
        String target = safeRedirect(redirectAttr, PortalRedirect.afterLogin(user));

        Pending.clear(session);
        session.invalidate();

        HttpSession fresh = req.getSession(true);
        fresh.setMaxInactiveInterval(30 * 60);
        fresh.setAttribute("currentUser", user);
        fresh.setAttribute("csrfToken", Csrf.ensureToken(req));
        UserSessionRegistry.register(user.getId(), fresh);

        // Gửi mail thông báo đăng nhập (sau khi đã OTP OK)
        try {
            MailService.sendLoginAlert(user.getEmail(), user.getDisplayName(),
                    req.getRemoteAddr(), req.getHeader("User-Agent"));
        } catch (RuntimeException ignored) { }

        resp.sendRedirect(req.getContextPath() + target);
    }

    /* ================================================================
       Helper — đóng gói trạng thái pending trong session
       ================================================================ */
    private record Pending(int userId, String email, String name, String otpHash,
                           long expiry, int attempts, long resendAt, String redirect) {

        boolean isExpired() { return System.currentTimeMillis() > expiry; }

        static Pending read(HttpSession s) {
            if (s == null) return null;
            Object uid = s.getAttribute("pendingLoginUserId");
            Object hash = s.getAttribute("pendingLoginOtpHash");
            if (uid == null || hash == null) return null;
            return new Pending(
                    (Integer) uid,
                    (String) s.getAttribute("pendingLoginEmail"),
                    (String) s.getAttribute("pendingLoginName"),
                    (String) hash,
                    ((Number) s.getAttribute("pendingLoginExpiry")).longValue(),
                    s.getAttribute("pendingLoginAttempts") instanceof Integer i ? i : 0,
                    s.getAttribute("pendingLoginResendAt") instanceof Number n ? n.longValue() : 0L,
                    (String) s.getAttribute("pendingLoginRedirect")
            );
        }

        static void clear(HttpSession s) {
            if (s == null) return;
            s.removeAttribute("pendingLoginUserId");
            s.removeAttribute("pendingLoginEmail");
            s.removeAttribute("pendingLoginName");
            s.removeAttribute("pendingLoginOtpHash");
            s.removeAttribute("pendingLoginExpiry");
            s.removeAttribute("pendingLoginAttempts");
            s.removeAttribute("pendingLoginResendAt");
            s.removeAttribute("pendingLoginRedirect");
        }
    }
}