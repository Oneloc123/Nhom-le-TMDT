package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.UserStore;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.CartService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.PasswordHasher;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.Redirects;

import java.io.IOException;

/**
 * Đăng nhập thường (email + mật khẩu).
 * - So sánh mật khẩu đã băm PBKDF2 (không plaintext).
 * - Chống session fixation: hủy phiên cũ, tạo phiên mới sau khi xác thực.
 */
@WebServlet(name = "loginServlet", value = "/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User current = (session == null) ? null : (User) session.getAttribute("currentUser");
        String redirect = Redirects.safeLocalPath(request.getParameter("redirect"));
        if (current != null) {
            response.sendRedirect(request.getContextPath()
                    + (current.isAdmin() ? "/admin/dashboard"
                    : redirect == null ? "/home" : redirect));
            return;
        }
        request.setAttribute("redirect", redirect);
        request.getRequestDispatcher("/customer/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String redirect = Redirects.safeLocalPath(request.getParameter("redirect"));
        request.setAttribute("redirect", redirect);

        User user = email == null ? null
                : UserStore.findByEmail(email.trim()).orElse(null);

        // Thông báo lỗi chung chung, không tiết lộ email có tồn tại hay không.
        if (user == null || !PasswordHasher.verify(password, user.getPassword())) {
            request.setAttribute("error", "Email hoặc mật khẩu không đúng.");
            request.getRequestDispatcher("/customer/login.jsp").forward(request, response);
            return;
        }
        if (User.STATUS_LOCKED.equals(user.getStatus())) {
            request.setAttribute("error", "Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.");
            request.getRequestDispatcher("/customer/login.jsp").forward(request, response);
            return;
        }
        if (User.STATUS_PENDING.equals(user.getStatus())) {
            request.setAttribute("error", "Tài khoản chưa được kích hoạt. Vui lòng kiểm tra email để xác thực.");
            request.getRequestDispatcher("/customer/login.jsp").forward(request, response);
            return;
        }

        // Chống session fixation: hủy phiên đăng nhập cũ, tạo phiên mới.
        HttpSession old = request.getSession(false);
        // Gộp giỏ hàng khách (session) vào giỏ đã lưu của tài khoản.
        try {
            new CartService().mergeGuestCart(old, user);
        } catch (RuntimeException mergeError) {
            request.setAttribute("error",
                    "Chưa thể đồng bộ giỏ hàng. Sản phẩm của bạn vẫn được giữ; vui lòng thử lại.");
            request.getRequestDispatcher("/customer/login.jsp").forward(request, response);
            return;
        }
        if (old != null) {
            old.invalidate();
        }
        HttpSession session = request.getSession(true);
        session.setAttribute("currentUser", user);
        response.sendRedirect(request.getContextPath()
                + (user.isAdmin() ? "/admin/dashboard"
                : redirect == null ? "/home" : redirect));
    }
}
