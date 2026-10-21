package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.AddressDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.UserStore;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.UserDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.PasswordHasher;

import java.io.IOException;

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
        request.setAttribute("addresses", AddressDao.findByUser(user.getId()));
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

        if (fullName == null || fullName.trim().isEmpty()) {
            render(request, response, user, "Họ tên không được để trống.", null);
            return;
        }
        if (fullName.trim().length() > 120) {
            render(request, response, user, "Họ tên không được vượt quá 120 ký tự.", null);
            return;
        }
        if (phone != null && !phone.trim().isEmpty()
                && !phone.trim().matches("\\d{9,12}")) {
            render(request, response, user, "Số điện thoại không hợp lệ (9–12 chữ số).", null);
            return;
        }

        user.setFullName(fullName.trim());
        user.setPhone(phone == null ? "" : phone.trim());
        // Chỉ cập nhật các trường hồ sơ; không ghi đè role/status từ dữ liệu session cũ.
        UserDao.updateProfile(user);

        // Tải lại bản ghi để giao diện và session phản ánh đúng dữ liệu vừa lưu.
        User saved = UserStore.findById(user.getId()).orElse(user);
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.setAttribute("currentUser", saved);
        }
        render(request, response, saved, null, "Đã cập nhật thông tin cá nhân.");
    }

    private void changePassword(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        String oldPass = request.getParameter("oldPassword");
        String newPass = request.getParameter("newPassword");
        String confirm = request.getParameter("confirmPassword");

        // Xác minh bằng bản ghi hiện tại, không dùng hash có thể đã cũ trong session.
        User persisted = UserStore.findById(user.getId()).orElse(user);
        if (!PasswordHasher.verify(oldPass, persisted.getPassword())) {
            render(request, response, persisted, "Mật khẩu hiện tại không đúng.", null);
            return;
        }
        if (newPass == null || newPass.length() < 8) {
            render(request, response, persisted, "Mật khẩu mới phải có ít nhất 8 ký tự.", null);
            return;
        }
        if (!newPass.equals(confirm)) {
            render(request, response, persisted, "Mật khẩu xác nhận không khớp.", null);
            return;
        }
        if (PasswordHasher.verify(newPass, persisted.getPassword())) {
            render(request, response, persisted, "Mật khẩu mới phải khác mật khẩu hiện tại.", null);
            return;
        }

        // UserStore mã hóa mật khẩu bằng PBKDF2 trước khi lưu xuống database.
        UserStore.updatePassword(user.getId(), newPass);
        User saved = UserStore.findById(user.getId()).orElse(persisted);
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.setAttribute("currentUser", saved);
        }
        render(request, response, saved, null, "Đổi mật khẩu thành công.");
    }

    private void render(HttpServletRequest request, HttpServletResponse response, User user,
                        String error, String message) throws ServletException, IOException {
        request.setAttribute("user", user);
        request.setAttribute("addresses", AddressDao.findByUser(user.getId()));
        if (error != null) {
            request.setAttribute("error", error);
        }
        if (message != null) {
            request.setAttribute("message", message);
        }
        request.getRequestDispatcher("/customer/profile.jsp").forward(request, response);
    }
}
