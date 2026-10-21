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
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.Json;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.PasswordHasher;

import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Đăng ký tài khoản theo luồng OTP DEMO:
 *   Bước 1 — validate form + kiểm tra email chưa tồn tại + phát hành thách thức
 *            OTP (CHƯA tạo tài khoản, chỉ giữ hash PBKDF2 trong phiên).
 *   Bước 2 — xác thực OTP {@code 111111} ở SERVER rồi mới tạo tài khoản.
 *
 * <p>Không gửi email, không gọi SMTP/API mail. Giao diện bắt buộc hiển thị ghi chú
 * DEMO MODE của {@link DemoOtp#DEMO_NOTE}. Hỗ trợ AJAX (trả JSON) và giữ đường
 * fallback submit form thường khi JavaScript tắt.</p>
 */
@WebServlet(name = "registerServlet", value = "/register")
public class RegisterServlet extends HttpServlet {

    static final String SESSION_PENDING = "pendingRegistration";
    static final String SESSION_OTP = DemoOtp.SESSION_ATTR;

    /** Số lần nhập sai OTP tối đa trước khi phải đăng ký lại (demo). */
    static final int MAX_VERIFY_ATTEMPTS = DemoOtp.MAX_ATTEMPTS;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("demoOtpNote", DemoOtp.DEMO_NOTE);
        request.getRequestDispatcher("/customer/register.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        if ("verify".equals(action)) {
            verify(request, response);
        } else {
            register(request, response);
        }
    }

    // ------------------------------------------------------------------
    // Bước 1: validate + phát hành OTP (chưa tạo tài khoản)
    // ------------------------------------------------------------------
    private void register(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String email = DemoOtp.normalizeEmail(request.getParameter("email"));
        String password = nvl(request.getParameter("password"));
        String confirm = nvl(request.getParameter("confirm"));
        String fullName = nvl(request.getParameter("fullName")).trim();
        String phone = nvl(request.getParameter("phone")).trim();
        String address = nvl(request.getParameter("address")).trim();

        String error = null;
        if (email.isEmpty() || password.isEmpty() || fullName.isEmpty()) {
            error = "Vui lòng điền đầy đủ họ tên, email và mật khẩu.";
        } else if (!email.matches("^[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            error = "Định dạng email không hợp lệ.";
        } else if (email.length() > 190) {
            error = "Email tối đa 190 ký tự.";
        } else if (fullName.length() > 120) {
            error = "Họ tên tối đa 120 ký tự.";
        } else if (password.length() < 6) {
            error = "Mật khẩu phải có ít nhất 6 ký tự.";
        } else if (!password.equals(confirm)) {
            error = "Mật khẩu xác nhận không khớp.";
        } else if (!phone.isEmpty() && !phone.matches("\\d{9,12}")) {
            error = "Số điện thoại không hợp lệ (9–12 chữ số).";
        } else if (address.length() > 255) {
            error = "Địa chỉ tối đa 255 ký tự.";
        }

        // Chuẩn hoá email + kiểm tra trùng TRƯỚC khi phát hành OTP.
        if (error == null && UserStore.findByEmail(email).isPresent()) {
            error = "Email đã được đăng ký. Vui lòng dùng email khác hoặc đăng nhập.";
        }

        if (error != null) {
            fail(request, response, error);
            return;
        }

        HttpSession session = request.getSession(true);
        // Chỉ giữ HASH (PBKDF2) trong phiên — không lưu mật khẩu plaintext.
        PendingRegistration pending = new PendingRegistration(
                email, PasswordHasher.hash(password), fullName, phone, address,
                System.currentTimeMillis());
        session.setAttribute(SESSION_PENDING, pending);
        session.setAttribute(SESSION_OTP,
                DemoOtp.Challenge.issue(email, DemoOtp.PURPOSE_REGISTER));

        if (wantsJson(request)) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("email", email);
            data.put("step", "verify");
            data.put("demoOtpNote", DemoOtp.DEMO_NOTE);
            respond(request, response, true,
                    "Đã phát hành OTP demo. Nhập mã 111111 để hoàn tất đăng ký.", data);
            return;
        }
        // Fallback không JavaScript: render bước OTP như trước.
        request.setAttribute("step", "verify");
        request.setAttribute("email", email);
        request.setAttribute("demoOtpNote", DemoOtp.DEMO_NOTE);
        request.getRequestDispatcher("/customer/register.jsp").forward(request, response);
    }

    // ------------------------------------------------------------------
    // Bước 2: xác thực OTP ở server rồi tạo tài khoản
    // ------------------------------------------------------------------
    private void verify(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        Object pendingAttr = session == null ? null : session.getAttribute(SESSION_PENDING);
        DemoOtp.Challenge challenge = session == null ? null
                : (session.getAttribute(SESSION_OTP) instanceof DemoOtp.Challenge c ? c : null);

        if (!(pendingAttr instanceof PendingRegistration pending) || challenge == null) {
            clearPending(session);
            fail(request, response, "Phiên đăng ký đã hết hạn. Vui lòng đăng ký lại.");
            return;
        }
        if (System.currentTimeMillis() - pending.createdAt > DemoOtp.TTL_MILLIS) {
            clearPending(session);
            fail(request, response, "Phiên đăng ký đã hết hạn. Vui lòng đăng ký lại.");
            return;
        }

        DemoOtp.VerifyResult result = challenge.verify(pending.email,
                DemoOtp.PURPOSE_REGISTER, request.getParameter("code"),
                System.currentTimeMillis());

        if (result != DemoOtp.VerifyResult.OK) {
            fail(request, response, otpError(result, "đăng ký"), pending.email);
            return;
        }

        // OTP hợp lệ → chỉ giờ mới tạo tài khoản.
        User user;
        try {
            user = UserStore.createWithHash(pending.email, pending.passwordHash,
                    pending.fullName, pending.phone, pending.address,
                    User.ROLE_CUSTOMER, User.STATUS_ACTIVE);
        } catch (RuntimeException e) {
            // Trùng email do-submit song song hoặc lỗi DB — không lộ chi tiết kỹ thuật.
            fail(request, response, "Không thể tạo tài khoản. Email có thể vừa được đăng ký,"
                    + " vui lòng thử lại.");
            return;
        }
        UserStore.markEmailVerified(user.getId());
        clearPending(session);

        SecurityEventDao.record((long) user.getId(), null, SecurityEventDao.EVENT_REGISTERED,
                "Đăng ký tài khoản thành công qua OTP demo", clientIp(request));

        if (wantsJson(request)) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("email", user.getEmail());
            data.put("step", "done");
            respond(request, response, true,
                    "Đăng ký thành công! Vui lòng đăng nhập bằng email " + user.getEmail(), data);
            return;
        }
        request.setAttribute("registeredOk",
                "Kích hoạt thành công! Vui lòng đăng nhập bằng email " + user.getEmail());
        request.getRequestDispatcher("/customer/login.jsp").forward(request, response);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------
    static String otpError(DemoOtp.VerifyResult result, String flow) {
        return switch (result) {
            case OK -> "";
            case EXPIRED -> "Mã OTP đã hết hạn (5 phút). Vui lòng thử lại từ đầu.";
            case TOO_MANY_ATTEMPTS -> "Bạn đã nhập sai mã quá " + DemoOtp.MAX_ATTEMPTS
                    + " lần. Vui lòng thử lại từ đầu.";
            case ALREADY_USED -> "Mã OTP đã được sử dụng. Vui lòng thử lại từ đầu.";
            case BOUND_TO_OTHER -> "Mã OTP không khớp với yêu cầu " + flow + " này.";
            case MISSING, MISMATCH -> "Mã OTP không đúng.";
        };
    }

    private void fail(HttpServletRequest request, HttpServletResponse response, String message)
            throws ServletException, IOException {
        fail(request, response, message, null);
    }

    private void fail(HttpServletRequest request, HttpServletResponse response, String message,
                      String email) throws ServletException, IOException {
        if (wantsJson(request)) {
            respond(request, response, false, message, null);
            return;
        }
        request.setAttribute("error", message);
        request.setAttribute("demoOtpNote", DemoOtp.DEMO_NOTE);
        if (email != null) {
            request.setAttribute("step", "verify");
            request.setAttribute("email", email);
        }
        request.getRequestDispatcher("/customer/register.jsp").forward(request, response);
    }

    /** Trả JSON khi client gọi AJAX; ngược lại forward về JSP (fallback không JS). */
    static boolean wantsJson(HttpServletRequest request) {
        return "1".equals(request.getParameter("ajax"))
                || "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"));
    }

    static void respond(HttpServletRequest request, HttpServletResponse response,
                        boolean success, String message, Map<String, Object> data)
            throws IOException {
        response.setStatus(success ? HttpServletResponse.SC_OK : HttpServletResponse.SC_BAD_REQUEST);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", success);
        payload.put("message", message);
        if (data != null) {
            payload.put("data", data);
        }
        response.getWriter().write(Json.stringify(payload));
    }

    static void clearPending(HttpSession session) {
        if (session == null) {
            return;
        }
        session.removeAttribute(SESSION_PENDING);
        session.removeAttribute(SESSION_OTP);
    }

    /** So sánh không rò rỉ thời gian (giữ tương thích với kiểm thử hiện hữu). */
    static boolean matchesCode(String input, String expected) {
        return DemoOtp.matches(input, expected);
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    /** Dữ liệu đăng ký chờ OTP — chỉ giữ hash mật khẩu, không lưu plaintext. */
    static final class PendingRegistration implements Serializable {
        private static final long serialVersionUID = 1L;
        final String email;
        final String passwordHash;
        final String fullName;
        final String phone;
        final String address;
        final long createdAt;

        PendingRegistration(String email, String passwordHash, String fullName,
                            String phone, String address, long createdAt) {
            this.email = email;
            this.passwordHash = passwordHash;
            this.fullName = fullName;
            this.phone = phone;
            this.address = address;
            this.createdAt = createdAt;
        }
    }
}
