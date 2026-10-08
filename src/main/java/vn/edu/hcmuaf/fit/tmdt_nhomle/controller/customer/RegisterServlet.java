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

/**
 * Đăng ký tài khoản + gửi email xác thực (kích hoạt).
 * - action=register (POST): tạo tài khoản trạng thái PENDING, gửi mã xác thực.
 * - action=verify (POST): kích hoạt tài khoản bằng mã xác thực.
 */
@WebServlet(name = "registerServlet", value = "/register")
public class RegisterServlet extends HttpServlet {

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

        if (email.isEmpty() || password.isEmpty() || fullName.isEmpty()) {
            fail(request, response, "Vui lòng điền đầy đủ họ tên, email và mật khẩu.");
            return;
        }
        if (!email.matches("^[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            fail(request, response, "Định dạng email không hợp lệ.");
            return;
        }
        if (password.length() < 6) {
            fail(request, response, "Mật khẩu phải có ít nhất 6 ký tự.");
            return;
        }
        if (!password.equals(confirm)) {
            fail(request, response, "Mật khẩu xác nhận không khớp.");
            return;
        }
        if (UserStore.findByEmail(email).isPresent()) {
            fail(request, response, "Email đã được đăng ký: " + email);
            return;
        }

        User user = UserStore.create(email, password, fullName, phone, address,
                User.ROLE_CUSTOMER, User.STATUS_PENDING, "local");
        String code = MailService.sendVerificationCode(user.getEmail(), user.getFullName());

        HttpSession session = request.getSession(true);
        session.setAttribute("pendingVerifyEmail", user.getEmail());
        session.setAttribute("pendingVerifyCode", code);

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
        if (code == null || !code.equals(session.getAttribute("pendingVerifyCode"))) {
            request.setAttribute("step", "verify");
            request.setAttribute("email", email);
            request.setAttribute("error", "Mã xác thực không đúng.");
            request.getRequestDispatcher("/customer/register.jsp").forward(request, response);
            return;
        }

        user.setStatus(User.STATUS_ACTIVE);
        UserStore.update(user);
        session.removeAttribute("pendingVerifyEmail");
        session.removeAttribute("pendingVerifyCode");

        request.setAttribute("registeredOk",
                "Kích hoạt thành công! Vui lòng đăng nhập bằng email " + user.getEmail());
        request.getRequestDispatcher("/customer/login.jsp").forward(request, response);
    }

    private void fail(HttpServletRequest request, HttpServletResponse response, String message)
            throws ServletException, IOException {
        request.setAttribute("error", message);
        request.getRequestDispatcher("/customer/register.jsp").forward(request, response);
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
