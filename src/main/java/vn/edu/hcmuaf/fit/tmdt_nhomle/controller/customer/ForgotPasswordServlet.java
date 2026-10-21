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
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.DemoOtp;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.PasswordHasher;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Quên mật khẩu / đặt lại mật khẩu bằng OTP DEMO ({@code 111111}).
 *
 * <p>Bước 1 — phản hồi LUÔN giống nhau cho mọi email (không tiết lộ email nào
 * đã đăng ký), hiển thị ghi chú DEMO MODE kèm mã kiểm thử thay vì gửi email thật.
 * Thách thức OTP vẫn được phát hành và gắn với email + mục đích RESET_PASSWORD.</p>
 * <p>Bước 2 — xác thực OTP ở server, kiểm tra mật khẩu mới, cập nhật hash rồi
 * xoá thách thức (dùng một lần). TODO(EMAIL): khi có SMTP, thay phần hiển thị mã
 * demo bằng lần gọi API mail thật; giữ nguyên validate phía server.</p>
 */
@WebServlet(name = "forgotPasswordServlet", value = "/forgot-password")
public class ForgotPasswordServlet extends HttpServlet {

    static final String SESSION_EMAIL = "resetEmail";
    static final String SESSION_OTP = DemoOtp.SESSION_ATTR;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("demoOtpNote", DemoOtp.DEMO_NOTE);
        request.getRequestDispatcher("/customer/forgot-password.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        if ("reset".equals(action)) {
            reset(request, response);
        } else if ("cancel".equals(action)) {
            clear(request.getSession(false));
            if (RegisterServlet.wantsJson(request)) {
                respond(request, response, true, "Đã huỷ yêu cầu khôi phục mật khẩu.", null);
            } else {
                response.sendRedirect(request.getContextPath() + "/forgot-password");
            }
        } else {
            sendOtp(request, response);
        }
    }

    // ------------------------------------------------------------------
    // Bước 1: yêu cầu OTP (phản hồi chung, không lộ tài khoản tồn tại)
    // ------------------------------------------------------------------
    private void sendOtp(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String email = DemoOtp.normalizeEmail(request.getParameter("email"));
        if (email.isEmpty() || !email.matches("^[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            fail(request, response, "Định dạng email không hợp lệ.");
            return;
        }

        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_EMAIL, email);
        // Thách thức phát hành cho BẤT KỲ email nào → kết quả các bước sau
        // không phân biệt được email có tồn tại hay không.
        session.setAttribute(SESSION_OTP,
                DemoOtp.Challenge.issue(email, DemoOtp.PURPOSE_RESET_PASSWORD));

        // TODO(EMAIL): với môi trường thật, chỉ gọi API gửi mail khi email tồn tại
        //   và KHÔNG trả khác biệt về UI: sendMailAsync(email, generateRandomOtp());
        //   DEMO hiện tại hiển thị mã 111111 trực tiếp, KHÔNG gửi email nào.

        if (RegisterServlet.wantsJson(request)) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("step", "otp");
            data.put("email", email);
            data.put("demoOtpNote", DemoOtp.DEMO_NOTE);
            data.put("demoOtp", DemoOtp.CODE);
            respond(request, response, true,
                    "Nếu email đã đăng ký, mã OTP khôi phục sẽ có hiệu lực trong 5 phút.",
                    data);
            return;
        }
        // Fallback không JavaScript: render bước nhập OTP (ghi chú DEMO kèm mã).
        request.setAttribute("step", "otp");
        request.setAttribute("email", email);
        request.setAttribute("demoOtpNote", DemoOtp.DEMO_NOTE);
        request.setAttribute("demoOtpCode", DemoOtp.CODE);
        request.getRequestDispatcher("/customer/forgot-password.jsp").forward(request, response);
    }

    // ------------------------------------------------------------------
    // Bước 2: xác thực OTP + đặt mật khẩu mới
    // ------------------------------------------------------------------
    private void reset(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        String email = session == null ? null : (String) session.getAttribute(SESSION_EMAIL);
        DemoOtp.Challenge challenge = session == null ? null
                : (session.getAttribute(SESSION_OTP) instanceof DemoOtp.Challenge c ? c : null);
        String password = request.getParameter("password");
        String confirm = request.getParameter("confirm");

        if (email == null || challenge == null) {
            clear(session);
            fail(request, response, "Phiên khôi phục đã hết hạn. Vui lòng yêu cầu mã mới.");
            return;
        }

        DemoOtp.VerifyResult result = challenge.verify(email,
                DemoOtp.PURPOSE_RESET_PASSWORD, request.getParameter("otp"),
                System.currentTimeMillis());
        if (result != DemoOtp.VerifyResult.OK) {
            // Thông báo lỗi OTP chung — không phân biệt email có tồn tại.
            fail(request, response, RegisterServlet.otpError(result, "khôi phục mật khẩu"),
                    true);
            return;
        }

        User user = UserStore.findByEmail(email).orElse(null);
        if (user == null || user.isDeleted() || User.STATUS_LOCKED.equals(user.getStatus())) {
            // Không dùng lại thách thức đã xác thực cho tài khoản không hợp lệ.
            clear(session);
            fail(request, response, "Không thể khôi phục mật khẩu cho yêu cầu này.",
                    true);
            return;
        }
        if (password == null || password.length() < 6) {
            fail(request, response, "Mật khẩu mới phải có ít nhất 6 ký tự.", true);
            return;
        }
        if (!password.equals(confirm)) {
            fail(request, response, "Mật khẩu xác nhận không khớp.", true);
            return;
        }
        if (PasswordHasher.verify(password, user.getPassword())) {
            fail(request, response, "Mật khẩu mới không được trùng mật khẩu hiện tại.",
                    true);
            return;
        }

        // Cập nhật hash PBKDF2 (không bao giờ lưu/ghi plaintext), tiêu thụ OTP.
        UserStore.updatePassword(user.getId(), password);
        UserStore.clearLoginLock(user.getId());
        clear(session);

        SecurityEventDao.record((long) user.getId(), null,
                SecurityEventDao.EVENT_PASSWORD_RESET,
                "Đặt lại mật khẩu thành công qua OTP demo", request.getRemoteAddr());
        // TODO(EMAIL): sau khi đặt lại mật khẩu, gửi mail thông báo bằng API mail thật.

        if (RegisterServlet.wantsJson(request)) {
            respond(request, response, true,
                    "Đặt lại mật khẩu thành công. Vui lòng đăng nhập bằng mật khẩu mới.",
                    Map.of("redirect", request.getContextPath() + "/login"));
            return;
        }
        request.setAttribute("resetOk", "Đặt lại mật khẩu thành công. Vui lòng đăng nhập.");
        request.getRequestDispatcher("/customer/login.jsp").forward(request, response);
    }

    // ------------------------------------------------------------------
    private void fail(HttpServletRequest request, HttpServletResponse response, String message)
            throws ServletException, IOException {
        fail(request, response, message, false);
    }

    private void fail(HttpServletRequest request, HttpServletResponse response, String message,
                      boolean otpStep) throws ServletException, IOException {
        if (RegisterServlet.wantsJson(request)) {
            respond(request, response, false, message, null);
            return;
        }
        request.setAttribute("error", message);
        request.setAttribute("demoOtpNote", DemoOtp.DEMO_NOTE);
        if (otpStep) {
            request.setAttribute("step", "otp");
            request.setAttribute("email", request.getSession(false) == null ? null
                    : request.getSession(false).getAttribute(SESSION_EMAIL));
        }
        request.getRequestDispatcher("/customer/forgot-password.jsp").forward(request, response);
    }

    private void respond(HttpServletRequest request, HttpServletResponse response,
                         boolean success, String message, Map<String, Object> data)
            throws IOException {
        response.setStatus(success ? HttpServletResponse.SC_OK
                : HttpServletResponse.SC_BAD_REQUEST);
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

    private static void clear(HttpSession session) {
        if (session == null) {
            return;
        }
        session.removeAttribute(SESSION_EMAIL);
        session.removeAttribute(SESSION_OTP);
    }
}
