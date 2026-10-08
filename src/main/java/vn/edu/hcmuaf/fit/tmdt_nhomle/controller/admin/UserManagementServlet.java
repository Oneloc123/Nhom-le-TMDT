package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.UserStore;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;

import java.io.IOException;
import java.util.List;

/**
 * CRUD người dùng cho Admin: xem danh sách, thêm, sửa, khóa/mở khóa.
 * Params: action = list | add | edit | toggle, id, email, fullName, phone, address, role
 */
@WebServlet(name = "userManagementServlet", value = "/admin/users")
public class UserManagementServlet extends HttpServlet {

    private boolean isAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User current = (session == null) ? null : (User) session.getAttribute("currentUser");
        return current != null && current.isAdmin();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAdmin(request)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        String action = nvl(request.getParameter("action"), "list");

        if ("edit".equals(action)) {
            User u = UserStore.findById(parseInt(request.getParameter("id"), -1)).orElse(null);
            if (u == null) {
                request.setAttribute("error", "Không tìm thấy người dùng.");
            } else {
                request.setAttribute("editUser", u);
            }
        } else if ("toggle".equals(action)) {
            UserStore.findById(parseInt(request.getParameter("id"), -1)).ifPresent(u -> {
                u.setStatus(User.STATUS_ACTIVE.equals(u.getStatus())
                        ? User.STATUS_LOCKED : User.STATUS_ACTIVE);
                UserStore.update(u);
            });
            request.setAttribute("message", "Đã cập nhật trạng thái tài khoản.");
        }

        List<User> users = UserStore.findAll();
        request.setAttribute("users", users);
        request.setAttribute("totalUsers", users.size());
        request.setAttribute("adminCount", UserStore.countByRole(User.ROLE_ADMIN));
        request.setAttribute("customerCount", UserStore.countByRole(User.ROLE_CUSTOMER));
        request.setAttribute("lockedCount", UserStore.countByStatus(User.STATUS_LOCKED));

        request.getRequestDispatcher("/admin/user-management.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAdmin(request)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        request.setCharacterEncoding("UTF-8");
        String action = nvl(request.getParameter("action"), "add");

        if ("add".equals(action)) {
            String email = nvl(request.getParameter("email"), "").trim();
            String password = nvl(request.getParameter("password"), "");
            if (email.isEmpty() || password.isEmpty()) {
                request.setAttribute("error", "Email và mật khẩu là bắt buộc.");
            } else if (UserStore.findByEmail(email).isPresent()) {
                request.setAttribute("error", "Email đã tồn tại: " + email);
            } else {
                UserStore.create(email, password,
                        nvl(request.getParameter("fullName"), "Người dùng mới"),
                        nvl(request.getParameter("phone"), ""),
                        nvl(request.getParameter("address"), ""),
                        nvl(request.getParameter("role"), User.ROLE_CUSTOMER),
                        User.STATUS_ACTIVE, "local");
                request.setAttribute("message", "Đã thêm người dùng: " + email);
            }
        } else if ("edit".equals(action)) {
            int id = parseInt(request.getParameter("id"), -1);
            User u = UserStore.findById(id).orElse(null);
            if (u == null) {
                request.setAttribute("error", "Không tìm thấy người dùng.");
            } else {
                u.setFullName(nvl(request.getParameter("fullName"), u.getFullName()));
                u.setPhone(nvl(request.getParameter("phone"), u.getPhone()));
                u.setAddress(nvl(request.getParameter("address"), u.getAddress()));
                String role = request.getParameter("role");
                if (role != null && (User.ROLE_ADMIN.equals(role) || User.ROLE_CUSTOMER.equals(role))) {
                    u.setRole(role);
                }
                UserStore.update(u);
                request.setAttribute("message", "Đã lưu thay đổi cho " + u.getEmail());
            }
        }

        List<User> users = UserStore.findAll();
        request.setAttribute("users", users);
        request.setAttribute("totalUsers", users.size());
        request.setAttribute("adminCount", UserStore.countByRole(User.ROLE_ADMIN));
        request.setAttribute("customerCount", UserStore.countByRole(User.ROLE_CUSTOMER));
        request.setAttribute("lockedCount", UserStore.countByStatus(User.STATUS_LOCKED));
        request.getRequestDispatcher("/admin/user-management.jsp").forward(request, response);
    }

    private static String nvl(String s, String def) {
        return s == null ? def : s;
    }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }
}
