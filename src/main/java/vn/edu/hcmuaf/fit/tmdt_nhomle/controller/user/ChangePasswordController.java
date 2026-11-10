package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.user;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.UserDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.BcryptPasswordService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.MailService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.OtpHasher;

import java.io.IOException;

@WebServlet(name = "changePasswordV2", urlPatterns = {"/change-password", "/change-password/otp"})
public class ChangePasswordController extends UserControllerSupport {
    private static final long OTP_TTL_MS = 5 * 60_000L;
    private static final String OTP_HASH = "pendingChangePasswordOtpHash";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = currentUser(req);
        if (user == null) { resp.sendRedirect(req.getContextPath() + "/login"); return; }
        prepare(req);
        if (req.getServletPath().endsWith("/otp")) {
            HttpSession s = req.getSession(false);
            if (!validPending(s, user.getId())) {
                clearPending(s);
                resp.sendRedirect(req.getContextPath() + "/change-password?otpExpired=1");
                return;
            }
            req.setAttribute("email", user.getEmail());
            req.getRequestDispatcher("/WEB-INF/views/user/change-password-otp.jsp").forward(req, resp);
            return;
        }
        req.setAttribute("hasPassword", needsCurrentPassword(user));
        if ("1".equals(req.getParameter("otpExpired"))) req.setAttribute("error", "Mã OTP đã hết hạn. Vui lòng thực hiện lại.");
        if ("1".equals(req.getParameter("otpLocked"))) req.setAttribute("error", "Bạn đã nhập sai OTP quá nhiều lần. Vui lòng thực hiện lại.");
        req.getRequestDispatcher("/WEB-INF/views/user/change-password.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        User user = currentUser(req);
        if (user == null) { resp.sendRedirect(req.getContextPath() + "/login"); return; }
        prepare(req);
        try {
            String next = req.getParameter("newPassword");
            String confirm = req.getParameter("confirmPassword");
            BcryptPasswordService.validate(next);
            if (!next.equals(confirm)) throw new IllegalArgumentException("Mật khẩu xác nhận không khớp.");
            String oldHash = UserDao.findPasswordHashById(user.getId()).orElse(null);
            boolean hasPassword = needsCurrentPassword(user) && oldHash != null && !oldHash.isBlank();
            if (hasPassword) {
                String current = req.getParameter("currentPassword");
                if (current == null || current.isBlank()) throw new IllegalArgumentException("Vui lòng nhập mật khẩu hiện tại.");
                if (!BcryptPasswordService.verify(current, oldHash)) throw new IllegalArgumentException("Mật khẩu hiện tại không đúng.");
                if (BcryptPasswordService.verify(next, oldHash)) throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại.");
            }
            String otp = MailService.randomCode();
            HttpSession s = req.getSession(true);
            s.setAttribute("pendingChangePasswordUserId", user.getId());
            s.setAttribute("pendingChangePasswordHash", BcryptPasswordService.hash(next));
            s.setAttribute(OTP_HASH, OtpHasher.hash(otp));
            s.setAttribute("pendingChangePasswordExpiry", System.currentTimeMillis() + OTP_TTL_MS);
            s.setAttribute("pendingChangePasswordAttempts", 0);
            s.setAttribute("pendingChangePasswordResendAt", System.currentTimeMillis());
            MailService.sendPasswordChangeOtp(user.getEmail(), user.getDisplayName(), otp);
            resp.sendRedirect(req.getContextPath() + "/change-password/otp");
        } catch (RuntimeException ex) {
            req.setAttribute("hasPassword", needsCurrentPassword(user));
            req.setAttribute("error", ex.getMessage() == null ? "Không thể yêu cầu đổi mật khẩu." : ex.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/user/change-password.jsp").forward(req, resp);
        }
    }

    private static boolean needsCurrentPassword(User user) {
        return user.getProvider() == null || user.getProvider().isBlank() || "local".equalsIgnoreCase(user.getProvider());
    }

    static boolean validPending(HttpSession s, int userId) {
        if (s == null || !(s.getAttribute("pendingChangePasswordUserId") instanceof Integer id) || id != userId) return false;
        Object expiry = s.getAttribute("pendingChangePasswordExpiry");
        return s.getAttribute(OTP_HASH) instanceof String && expiry instanceof Number n && System.currentTimeMillis() <= n.longValue();
    }

    static void clearPending(HttpSession s) {
        if (s == null) return;
        s.removeAttribute("pendingChangePasswordUserId");
        s.removeAttribute("pendingChangePasswordHash");
        s.removeAttribute(OTP_HASH);
        s.removeAttribute("pendingChangePasswordExpiry");
        s.removeAttribute("pendingChangePasswordAttempts");
        s.removeAttribute("pendingChangePasswordResendAt");
    }
}
