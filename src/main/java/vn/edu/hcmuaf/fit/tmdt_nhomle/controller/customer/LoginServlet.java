package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.SecurityEventDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.UserStore;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.CartService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.PortalRedirect;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.DemoOtp;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.PasswordHasher;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Đăng nhập bằng mật khẩu và đăng nhập bằng OTP DEMO.
 *
 * <p>Các {@code action}: {@code password} (mặc định), {@code otp_request},
 * {@code otp_verify}. Mọi xác thực (mật khẩu, OTP {@code 111111}, trạng thái
 * tài khoản) đều chạy ở server. AJAX trả JSON {@code {success, message, data}},
 * không phải AJAX vẫn forward về JSP như trước.</p>
 */
@WebServlet(name = "loginServlet", value = "/login")
public class LoginServlet extends HttpServlet {

    /** Số lần nhập sai mật khẩu trước khi bị khóa tạm 15 phút. */
    static final int LOCK_THRESHOLD = 10;
    static final int LOCK_MINUTES = 15;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User current = session == null ? null : (User) session.getAttribute("currentUser");
        if (current != null) {
            response.sendRedirect(request.getContextPath() + PortalRedirect.afterLogin(current));
            return;
        }
        request.setAttribute("demoOtpNote", DemoOtp.DEMO_NOTE);
        request.getRequestDispatcher("/customer/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        if ("otp_request".equals(action)) {
            otpRequest(request, response);
        } else if ("otp_verify".equals(action)) {
            otpVerify(request, response);
        } else {
            passwordLogin(request, response);
        }
    }

    // ------------------------------------------------------------------
    // Đăng nhập mật khẩu
    // ------------------------------------------------------------------
    private void passwordLogin(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String email = DemoOtp.normalizeEmail(request.getParameter("email"));
        String password = request.getParameter("password");

        User user = email.isEmpty() ? null : UserStore.findByEmail(email).orElse(null);

        // Thông báo chung chung — không tiết lộ email có tồn tại hay không.
        if (user == null || !PasswordHasher.verify(password, user.getPassword())) {
            if (user != null) {
                recordFailure(user);
            }
            fail(request, response, "Email hoặc mật khẩu không đúng.");
            return;
        }
        String blocked = blockedReason(user);
        if (blocked != null) {
            fail(request, response, blocked);
            return;
        }

        establishSession(request, response, user, SecurityEventDao.EVENT_LOGIN_SUCCESS,
                "Đăng nhập bằng mật khẩu");
    }

    // ------------------------------------------------------------------
    // Đăng nhập OTP demo: phát hành mã 111111
    // ------------------------------------------------------------------
    private void otpRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String email = DemoOtp.normalizeEmail(request.getParameter("email"));
        if (email.isEmpty()) {
            fail(request, response, "Vui lòng nhập email.");
            return;
        }

        HttpSession session = request.getSession(true);
        session.setAttribute(DemoOtp.SESSION_ATTR,
                DemoOtp.Challenge.issue(email, DemoOtp.PURPOSE_LOGIN));

        if (RegisterServlet.wantsJson(request)) {
            // Không tiết lộ tài khoản có tồn tại hay không ở bước yêu cầu mã.
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("email", email);
            data.put("step", "otp");
            data.put("demoOtpNote", DemoOtp.DEMO_NOTE);
            respond(request, response, true,
                    "Đã phát hành OTP demo. Nhập mã 111111 để đăng nhập.", data);
            return;
        }
        request.setAttribute("step", "otp");
        request.setAttribute("email", email);
        request.setAttribute("demoOtpNote", DemoOtp.DEMO_NOTE);
        request.getRequestDispatcher("/customer/login.jsp").forward(request, response);
    }

    // ------------------------------------------------------------------
    // Đăng nhập OTP demo: xác thực rồi đăng nhập TÀI KHOẢN CÓ SẴN
    // ------------------------------------------------------------------
    private void otpVerify(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        DemoOtp.Challenge challenge = session == null ? null
                : (session.getAttribute(DemoOtp.SESSION_ATTR) instanceof DemoOtp.Challenge c
                        ? c : null);
        String email = DemoOtp.normalizeEmail(request.getParameter("email"));

        if (challenge == null) {
            fail(request, response, "Phiên OTP đã hết hạn. Vui lòng yêu cầu mã mới.");
            return;
        }

        DemoOtp.VerifyResult result = challenge.verify(email, DemoOtp.PURPOSE_LOGIN,
                request.getParameter("code"), System.currentTimeMillis());
        if (result != DemoOtp.VerifyResult.OK) {
            fail(request, response, RegisterServlet.otpError(result, "đăng nhập"));
            return;
        }

        // OTP đúng nhưng CHỈ đăng nhập được tài khoản tồn tại + đủ điều kiện.
        User user = UserStore.findByEmail(challenge.email()).orElse(null);
        if (user == null) {
            fail(request, response, "Tài khoản không tồn tại hoặc chưa đủ điều kiện đăng nhập.");
            return;
        }
        String blocked = blockedReason(user);
        if (blocked != null) {
            fail(request, response, blocked);
            return;
        }

        session.removeAttribute(DemoOtp.SESSION_ATTR);
        establishSession(request, response, user, SecurityEventDao.EVENT_LOGIN_OTP,
                "Đăng nhập bằng OTP demo");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Lý do không được phép đăng nhập (null = được phép). */
    private static String blockedReason(User user) {
        if (user.isDeleted()) {
            return "Tài khoản đã bị xoá. Vui lòng liên hệ quản trị viên.";
        }
        if (User.STATUS_LOCKED.equals(user.getStatus())) {
            return "Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.";
        }
        if (User.STATUS_PENDING.equals(user.getStatus())) {
            return "Tài khoản chưa được kích hoạt. Vui lòng xác thực email trước.";
        }
        if (user.isTemporarilyLocked()) {
            return "Bạn đã nhập sai mật khẩu quá nhiều lần. Vui lòng thử lại sau "
                    + LOCK_MINUTES + " phút.";
        }
        return null;
    }

    /** Ghi nhận đăng nhập sai + khóa tạm khi vượt ngưỡng (không ghi mật khẩu). */
    private void recordFailure(User user) {
        try {
            int attempts = UserStore.recordFailedLogin(user.getId(),
                    user.getFailedLoginAttempts(), LOCK_THRESHOLD,
                    LocalDateTime.now().plusMinutes(LOCK_MINUTES));
            SecurityEventDao.record((long) user.getId(), null,
                    SecurityEventDao.EVENT_LOGIN_FAILED,
                    attempts >= LOCK_THRESHOLD
                            ? "Sai mật khẩu quá " + LOCK_THRESHOLD + " lần — khóa tạm "
                                    + LOCK_MINUTES + " phút"
                            : "Đăng nhập sai mật khẩu (lần " + attempts + ")");
        } catch (RuntimeException e) {
            System.getLogger(LoginServlet.class.getName()).log(System.Logger.Level.WARNING,
                    "Không thể ghi nhận đăng nhập sai", e);
        }
    }

    private void establishSession(HttpServletRequest request, HttpServletResponse response,
                                  User user, String eventType, String eventDescription)
            throws ServletException, IOException {
        // Chống session fixation: hủy phiên cũ, gộp giỏ hàng khách, tạo phiên mới.
        HttpSession old = request.getSession(false);
        new CartService().mergeGuestCart(old, user);
        if (old != null) {
            old.invalidate();
        }
        HttpSession session = request.getSession(true);
        session.setAttribute("currentUser", user);
        try {
            UserStore.touchLastLogin(user.getId());
            user.setLastLoginAt(LocalDateTime.now().toString());
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
        } catch (RuntimeException e) {
            System.getLogger(LoginServlet.class.getName()).log(System.Logger.Level.WARNING,
                    "Không thể cập nhật last_login_at", e);
        }
        SecurityEventDao.record((long) user.getId(), null, eventType, eventDescription);

        String target = safeNext(request, PortalRedirect.afterLogin(user));
        if (RegisterServlet.wantsJson(request)) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("redirect", request.getContextPath() + target);
            data.put("role", user.getRole());
            respond(request, response, true, "Đăng nhập thành công.", data);
            return;
        }
        response.sendRedirect(request.getContextPath() + target);
    }

    /**
     * Cho phép chuyển hướng nội bộ tới {@code fallback} hoặc tham số {@code next}
     * CHỈ khi là đường dẫn tuyệt đối nội bộ (bắt đầu bằng {@code /}, không phải
     * {@code //}) — chặn open redirect ra domain ngoài.
     */
    static String safeNext(HttpServletRequest request, String fallback) {
        String next = request.getParameter("next");
        if (next != null && next.startsWith("/") && !next.startsWith("//")
                && !next.startsWith("/\\") && !next.contains(":\\")) {
            return next;
        }
        return fallback;
    }

    private void fail(HttpServletRequest request, HttpServletResponse response, String message)
            throws ServletException, IOException {
        if (RegisterServlet.wantsJson(request)) {
            respond(request, response, false, message, null);
            return;
        }
        request.setAttribute("error", message);
        request.setAttribute("demoOtpNote", DemoOtp.DEMO_NOTE);
        request.getRequestDispatcher("/customer/login.jsp").forward(request, response);
    }

    private void respond(HttpServletRequest request, HttpServletResponse response,
                         boolean success, String message, Map<String, Object> data)
            throws IOException {
        response.setStatus(success ? HttpServletResponse.SC_OK
                : HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", success);
        payload.put("message", message);
        if (data != null) {
            payload.put("data", data);
        }
        response.getWriter().write(vn.edu.hcmuaf.fit.tmdt_nhomle.util.Json.stringify(payload));
    }
}
