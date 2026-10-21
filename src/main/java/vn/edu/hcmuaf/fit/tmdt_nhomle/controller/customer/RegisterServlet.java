package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.UserStore;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.MailService;

import java.io.IOException;

@WebServlet(name = "registerServlet", value = "/register")
public class RegisterServlet extends HttpServlet {

    /** Mã xác thực có hiệu lực 15 phút (khớp nội dung email). */
    static final long CODE_TTL_MILLIS = 15 * 60 * 1000L;
    /** Số lần nhập sai tối đa trước khi phải đăng ký lại. */
    static final int MAX_VERIFY_ATTEMPTS = 5;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
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

    private void register(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String email = trim(request.getParameter("email"));
        String password = request.getParameter("password");
        String confirm = request.getParameter("confirm");
        String fullName = trim(request.getParameter("fullName"));
        String phone = trim(request.getParameter("phone"));
        String address = trim(request.getParameter("address"));

        if (email.isEmpty() || password == null || password.isEmpty() || fullName.isEmpty()) {
            fail(request, response, "Vui lòng điền đầy đủ họ tên, email và mật khẩu.");
            return;
        }
        if (!email.matches("^[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            fail(request, response, "Định dạng email không hợp lệ.");
            return;
        }
        if (password.length() < 8) {
            fail(request, response, "Mật khẩu phải có ít nhất 8 ký tự.");
            return;
        }
        if (!password.equals(confirm)) {
            fail(request, response, "Mật khẩu xác nhận không khớp.");
            return;
        }
        if (UserStore.findByEmail(email).isPresent()) {
            fail(request, response, "Email đã được đăng ký. Vui lòng dùng email khác.");
            return;
        }

        // Lưu ý: schema users không có cột address — tham số address chỉ giữ
        // trong phiên model; mật khẩu được băm (PBKDF2) bên trong UserStore.create.
        User user = UserStore.create(email, password, fullName, phone, address,
                User.ROLE_CUSTOMER, User.STATUS_PENDING, "local");
        String code = MailService.sendVerificationCode(user.getEmail(), user.getFullName());

        HttpSession session = request.getSession(true);
        session.setAttribute("pendingVerifyEmail", user.getEmail());
        session.setAttribute("pendingVerifyCode", code);
        session.setAttribute("pendingVerifyExpiresAt", System.currentTimeMillis() + CODE_TTL_MILLIS);
        session.setAttribute("pendingVerifyAttempts", 0);

        request.setAttribute("step", "verify");
        request.setAttribute("email", user.getEmail());
        request.getRequestDispatcher("/customer/register.jsp").forward(request, response);
    }

    private void verify(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        String email = session == null ? null : (String) session.getAttribute("pendingVerifyEmail");
        String code = request.getParameter("code");

        User user = email == null ? null : UserStore.findByEmail(email).orElse(null);
        if (user == null) {
            fail(request, response, "Phiên xác thực đã hết hạn. Vui lòng đăng ký lại.");
            return;
        }

        long expiresAt = asLong(session.getAttribute("pendingVerifyExpiresAt"));
        if (expiresAt > 0 && System.currentTimeMillis() > expiresAt) {
            clearPending(session);
            fail(request, response, "Mã xác thực đã hết hạn (15 phút). Vui lòng đăng ký lại.");
            return;
        }
        int attempts = (int) asLong(session.getAttribute("pendingVerifyAttempts"));
        if (attempts >= MAX_VERIFY_ATTEMPTS) {
            clearPending(session);
            fail(request, response,
                    "Bạn đã nhập sai mã quá " + MAX_VERIFY_ATTEMPTS + " lần. Vui lòng đăng ký lại.");
            return;
        }

        String expected = (String) session.getAttribute("pendingVerifyCode");
        if (!matchesCode(code, expected)) {
            session.setAttribute("pendingVerifyAttempts", attempts + 1);
            request.setAttribute("step", "verify");
            request.setAttribute("email", email);
            request.setAttribute("error", "Mã xác thực không đúng.");
            request.getRequestDispatcher("/customer/register.jsp").forward(request, response);
            return;
        }

        user.setStatus(User.STATUS_ACTIVE);
        UserStore.update(user);
        clearPending(session);

        request.setAttribute("registeredOk",
                "Kích hoạt thành công! Vui lòng đăng nhập bằng email " + user.getEmail());
        request.getRequestDispatcher("/customer/login.jsp").forward(request, response);
    }    private static void clearPending(HttpSession session) {
        session.removeAttribute("pendingVerifyEmail");
        session.removeAttribute("pendingVerifyCode");
        session.removeAttribute("pendingVerifyExpiresAt");
        session.removeAttribute("pendingVerifyAttempts");
    }

    /** So sánh không rò rỉ thời gian giữa mã người dùng nhập và mã lưu trong phiên. */
    static boolean matchesCode(String input, String expected) {
        if (input == null || expected == null || input.length() != expected.length()) {
            return false;
        }
        return java.security.MessageDigest.isEqual(
                input.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                expected.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private void fail(HttpServletRequest request, HttpServletResponse response, String message) throws ServletException, IOException {
        request.setAttribute("error", message);
        request.getRequestDispatcher("/customer/register.jsp").forward(request, response);
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
