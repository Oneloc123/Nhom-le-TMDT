package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.user;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.UserDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.UserSessionRegistry;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.MailService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.OtpHasher;

import java.io.IOException;

@WebServlet(name = "changePasswordOtpV2", value = "/change-password/otp/confirm")
public class ChangePasswordOtpServlet extends UserControllerSupport {
    private static final long RESEND_MS = 60_000L;
    private static final int MAX_ATTEMPTS = 5;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.sendRedirect(req.getContextPath() + "/change-password/otp");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        prepare(req);
        User current = currentUser(req);
        HttpSession session = req.getSession(false);
        if (current == null) { resp.sendRedirect(req.getContextPath() + "/login"); return; }
        if (!ChangePasswordController.validPending(session, current.getId())) {
            ChangePasswordController.clearPending(session);
            resp.sendRedirect(req.getContextPath() + "/change-password?otpExpired=1");
            return;
        }
        String email = current.getEmail();
        String action = value(req.getParameter("action"));
        if ("resend".equals(action)) {
            long sentAt = session.getAttribute("pendingChangePasswordResendAt") instanceof Number n ? n.longValue() : 0;
            long wait = RESEND_MS - (System.currentTimeMillis() - sentAt);
            if (wait > 0) {
                req.setAttribute("error", "Vui lòng đợi " + Math.max(1, wait / 1000) + " giây trước khi gửi mã mới.");
            } else {
                String otp = MailService.randomCode();
                session.setAttribute("pendingChangePasswordOtpHash", OtpHasher.hash(otp));
                session.setAttribute("pendingChangePasswordExpiry", System.currentTimeMillis() + 5 * 60_000L);
                session.setAttribute("pendingChangePasswordAttempts", 0);
                session.setAttribute("pendingChangePasswordResendAt", System.currentTimeMillis());
                MailService.sendPasswordChangeOtp(email, current.getDisplayName(), otp);
                req.setAttribute("success", "Đã yêu cầu gửi mã mới. Nếu SMTP đang cấu hình, hãy kiểm tra email.");
            }
            req.setAttribute("email", email);
            req.getRequestDispatcher("/WEB-INF/views/user/change-password-otp.jsp").forward(req, resp);
            return;
        }
        int attempts = session.getAttribute("pendingChangePasswordAttempts") instanceof Number n ? n.intValue() : 0;
        if (attempts >= MAX_ATTEMPTS) {
            ChangePasswordController.clearPending(session);
            resp.sendRedirect(req.getContextPath() + "/change-password?otpLocked=1");
            return;
        }
        String code = value(req.getParameter("otp"));
        String hash = (String) session.getAttribute("pendingChangePasswordOtpHash");
        if (code.isBlank() || !(MailService.isDemoOtp(code) || OtpHasher.verify(code, hash))) {
            attempts++;
            session.setAttribute("pendingChangePasswordAttempts", attempts);
            req.setAttribute("email", email);
            req.setAttribute("error", attempts >= MAX_ATTEMPTS ? "Bạn đã nhập sai quá nhiều lần. Hãy bắt đầu lại." : "Mã OTP không đúng.");
            if (attempts >= MAX_ATTEMPTS) {
                ChangePasswordController.clearPending(session);
                resp.sendRedirect(req.getContextPath() + "/change-password?otpLocked=1");
                return;
            }
            req.getRequestDispatcher("/WEB-INF/views/user/change-password-otp.jsp").forward(req, resp);
            return;
        }
        int id = (Integer) session.getAttribute("pendingChangePasswordUserId");
        String newHash = (String) session.getAttribute("pendingChangePasswordHash");
        User user = UserDao.findById(id).orElse(null);
        if (user == null || !user.isActive() || newHash == null) {
            ChangePasswordController.clearPending(session);
            resp.sendRedirect(req.getContextPath() + "/profile");
            return;
        }
        UserDao.updatePasswordHash(id, newHash);
        UserDao.recordSecurityEvent(id, "PASSWORD_CHANGED", "Đổi mật khẩu sau khi xác minh OTP email.");
        UserSessionRegistry.invalidateOthers(id, session);
        ChangePasswordController.clearPending(session);
        MailService.sendPasswordChangedAlert(user.getEmail(), user.getDisplayName());
        resp.sendRedirect(req.getContextPath() + "/profile?passwordChanged=1");
    }
}
