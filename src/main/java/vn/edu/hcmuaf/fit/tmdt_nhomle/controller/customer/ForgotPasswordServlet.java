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
 * Khôi phục mật khẩu qua email (OTP).
 * - action=send (POST): gửi OTP đến email đã đăng ký.
 * - action=reset (POST): xác thực OTP và đặt lại mật khẩu.
 */
@WebServlet(name = "forgotPasswordServlet", value = "/forgot-password")
public class ForgotPasswordServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/customer/forgot-password.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        if ("reset".equals(action)) {
            reset(request, response);
        } else {
            sendOtp(request, response);
        }
    }

    private void sendOtp(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String email = request.getParameter("email");
        User user = email == null ? null : UserStore.findByEmail(email.trim()).orElse(null);

        if (user == null) {
            request.setAttribute("error", "Email chưa được đăng ký trong hệ thống.");
            request.getRequestDispatcher("/customer/forgot-password.jsp").forward(request, response);
            return;
        }

        String otp = MailService.sendPasswordReset(user.getEmail(), user.getFullName());
        HttpSession session = request.getSession(true);
        session.setAttribute("resetEmail", user.getEmail());
        session.setAttribute("resetOtp", otp);

        request.setAttribute("step", "otp");
        request.setAttribute("email", user.getEmail());
        request.getRequestDispatcher("/customer/forgot-password.jsp").forward(request, response);
    }

    private void reset(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        String email = session == null ? null : (String) session.getAttribute("resetEmail");
        String otp = request.getParameter("otp");
        String password = request.getParameter("password");
        String confirm = request.getParameter("confirm");

        User user = email == null ? null : UserStore.findByEmail(email).orElse(null);
        if (user == null) {
            request.setAttribute("error", "Phiên khôi phục đã hết hạn. Vui lòng thử lại.");
            request.getRequestDispatcher("/customer/forgot-password.jsp").forward(request, response);
            return;
        }
        if (otp == null || !otp.equals(session.getAttribute("resetOtp"))) {
            request.setAttribute("step", "otp");
            request.setAttribute("email", email);
            request.setAttribute("error", "OTP không đúng.");
            request.getRequestDispatcher("/customer/forgot-password.jsp").forward(request, response);
            return;
        }
        if (password == null || password.length() < 6) {
            request.setAttribute("step", "otp");
            request.setAttribute("email", email);
            request.setAttribute("error", "Mật khẩu mới phải có ít nhất 6 ký tự.");
            request.getRequestDispatcher("/customer/forgot-password.jsp").forward(request, response);
            return;
        }
        if (!password.equals(confirm)) {
            request.setAttribute("step", "otp");
            request.setAttribute("email", email);
            request.setAttribute("error", "Mật khẩu xác nhận không khớp.");
            request.getRequestDispatcher("/customer/forgot-password.jsp").forward(request, response);
            return;
        }

        user.setPassword(password);
        UserStore.update(user);
        session.removeAttribute("resetEmail");
        session.removeAttribute("resetOtp");

        request.setAttribute("resetOk", "Đặt lại mật khẩu thành công. Vui lòng đăng nhập.");
        request.getRequestDispatcher("/customer/login.jsp").forward(request, response);
    }
}
