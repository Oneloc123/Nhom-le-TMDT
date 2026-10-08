package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.UserStore;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;

import java.io.IOException;

/**
 * Xem & cập nhật thông tin cá nhân (họ tên, SĐT, địa chỉ nhận hàng in 3D)
 * và đổi mật khẩu.
 * - action=info (POST): cập nhật thông tin.
 * - action=password (POST): đổi mật khẩu.
 */
@WebServlet(name = "userProfileServlet", value = "/profile")
public class UserProfileServlet extends HttpServlet {

    private User current(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute("currentUser");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = current(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        request.setAttribute("user", user);
        request.getRequestDispatcher("/customer/profile.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = current(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");

        if ("password".equals(action)) {
            changePassword(request, response, user);
        } else {
            updateInfo(request, response, user);
        }
    }

    private void updateInfo(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        String fullName = request.getParameter("fullName");
        String phone = request.getParameter("phone");
        String address = request.getParameter("address");

        if (fullName == null || fullName.trim().isEmpty()) {
            request.setAttribute("user", user);
            request.setAttribute("error", "Họ tên không được để trống.");
            request.getRequestDispatcher("/customer/profile.jsp").forward(request, response);
            return;
        }

        user.setFullName(fullName.trim());
        user.setPhone(phone == null ? "" : phone.trim());
        user.setAddress(address == null ? "" : address.trim());
        UserStore.update(user);

        request.setAttribute("user", user);
        request.setAttribute("message", "Đã cập nhật thông tin cá nhân.");
        request.getRequestDispatcher("/customer/profile.jsp").forward(request, response);
    }

    private void changePassword(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        String oldPass = request.getParameter("oldPassword");
        String newPass = request.getParameter("newPassword");
        String confirm = request.getParameter("confirmPassword");

        if (oldPass == null || !oldPass.equals(user.getPassword())) {
            request.setAttribute("user", user);
            request.setAttribute("error", "Mật khẩu hiện tại không đúng.");
            request.getRequestDispatcher("/customer/profile.jsp").forward(request, response);
            return;
        }
        if (newPass == null || newPass.length() < 6) {
            request.setAttribute("user", user);
            request.setAttribute("error", "Mật khẩu mới phải có ít nhất 6 ký tự.");
            request.getRequestDispatcher("/customer/profile.jsp").forward(request, response);
            return;
        }
        if (!newPass.equals(confirm)) {
            request.setAttribute("user", user);
            request.setAttribute("error", "Mật khẩu xác nhận không khớp.");
            request.getRequestDispatcher("/customer/profile.jsp").forward(request, response);
            return;
        }

        user.setPassword(newPass);
        UserStore.update(user);

        request.setAttribute("user", user);
        request.setAttribute("message", "Đổi mật khẩu thành công.");
        request.getRequestDispatcher("/customer/profile.jsp").forward(request, response);
    }
}
