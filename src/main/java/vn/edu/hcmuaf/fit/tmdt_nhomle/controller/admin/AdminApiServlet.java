package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.AdminDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.AuthFilter;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.Json;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@WebServlet(name = "adminApiServlet", urlPatterns = "/admin/api/*")
public class AdminApiServlet extends HttpServlet {
    private static final System.Logger LOG = System.getLogger(AdminApiServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String[] path = path(request);
            Object data;
            if (path.length == 1 && "session".equals(path[0])) {
                User current = AuthFilter.currentUser(request);
                data = Map.of("id", current.getId(), "name", Optional.ofNullable(current.getFullName()).orElse("Admin"),
                        "role", current.getRole());
            } else if (path.length == 1 && "dashboard".equals(path[0])) {
                data = AdminDao.dashboard();
            } else if (path.length == 1 && "users".equals(path[0])) {
                data = AdminDao.listUsers(request.getParameter("q"), request.getParameter("role"),
                        request.getParameter("status"), request.getParameter("verified"),
                        request.getParameter("deleted"), request.getParameter("from"),
                        request.getParameter("to"), request.getParameter("sort"),
                        intParam(request, "page", 1), intParam(request, "size", 20));
            } else if (path.length == 2 && "users".equals(path[0])) {
                data = AdminDao.userDetail(id(path[1])).orElseThrow(() -> new NotFound("Không tìm thấy người dùng."));
            } else if (path.length == 1 && "sellers".equals(path[0])) {
                data = AdminDao.listSellers(request.getParameter("q"), request.getParameter("status"),
                        request.getParameter("verification"), intParam(request, "page", 1), intParam(request, "size", 20));
            } else if (path.length == 2 && "sellers".equals(path[0])) {
                data = AdminDao.sellerDetail(id(path[1])).orElseThrow(() -> new NotFound("Không tìm thấy xưởng in."));
            } else if (path.length == 1 && "products".equals(path[0])) {
                data = AdminDao.listProducts(request.getParameter("q"), request.getParameter("status"),
                        request.getParameter("category"), request.getParameter("shop"),
                        intParam(request, "page", 1), intParam(request, "size", 20));
            } else if (path.length == 2 && "products".equals(path[0])) {
                data = AdminDao.productDetail(id(path[1])).orElseThrow(() -> new NotFound("Không tìm thấy sản phẩm."));
            } else if (path.length == 1 && "orders".equals(path[0])) {
                data = AdminDao.listOrders(request.getParameter("q"), request.getParameter("status"),
                        request.getParameter("payment"), intParam(request, "page", 1), intParam(request, "size", 20));
            } else if (path.length == 2 && "orders".equals(path[0])) {
                data = AdminDao.orderDetail(id(path[1])).orElseThrow(() -> new NotFound("Không tìm thấy đơn hàng."));
            } else if (path.length == 1 && "quotes".equals(path[0])) {
                data = AdminDao.listQuotes(request.getParameter("q"), request.getParameter("status"),
                        intParam(request, "page", 1), intParam(request, "size", 20));
            } else {
                throw new NotFound("Đường dẫn Admin API không tồn tại.");
            }
            send(response, HttpServletResponse.SC_OK, Map.of("data", data));
        } catch (NotFound e) {
            send(response, HttpServletResponse.SC_NOT_FOUND, Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            send(response, HttpServletResponse.SC_BAD_REQUEST, Map.of("error", safeMessage(e)));
        } catch (DataAccessException e) {
            LOG.log(System.Logger.Level.ERROR, "Admin API read failed", e);
            send(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    Map.of("error", "Không thể đọc dữ liệu quản trị. Vui lòng thử lại sau."));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        try {
            String[] path = path(request);
            User actor = AuthFilter.currentUser(request);
            if (path.length == 3 && "users".equals(path[0]) && "profile".equals(path[2])) {
                AdminDao.updateUserProfile(id(path[1]), request.getParameter("fullName"), request.getParameter("phone"));
            } else if (path.length == 3 && "users".equals(path[0]) && "status".equals(path[2])) {
                AdminDao.changeUserStatus(id(path[1]), actor.getId(), request.getParameter("status"));
            } else if (path.length == 3 && "users".equals(path[0]) && "role".equals(path[2])) {
                AdminDao.changeUserRole(id(path[1]), actor.getId(), request.getParameter("role"));
            } else if (path.length == 3 && "users".equals(path[0]) && "delete".equals(path[2])) {
                // Xoá MỀM — dữ liệu đơn hàng/đánh giá được giữ nguyên.
                AdminDao.softDeleteUser(id(path[1]), actor.getId());
            } else if (path.length == 3 && "users".equals(path[0]) && "restore".equals(path[2])) {
                AdminDao.restoreUser(id(path[1]), actor.getId());
            } else if (path.length == 3 && "sellers".equals(path[0]) && "status".equals(path[2])) {
                AdminDao.changeSellerStatus(id(path[1]), request.getParameter("status"));
            } else if (path.length == 3 && "sellers".equals(path[0]) && "verification".equals(path[2])) {
                if (!"verify".equals(request.getParameter("action"))) throw new IllegalArgumentException("Thao tác xác minh không hợp lệ.");
                AdminDao.verifySeller(id(path[1]));
            } else if (path.length == 3 && "products".equals(path[0]) && "moderation".equals(path[2])) {
                AdminDao.moderateProduct(id(path[1]), actor.getId(), request.getParameter("action"), request.getParameter("reason"));
            } else if (path.length == 3 && "orders".equals(path[0]) && "status".equals(path[2])) {
                AdminDao.changeOrderStatus(id(path[1]), actor.getId(), request.getParameter("status"), request.getParameter("note"));
            } else {
                throw new NotFound("Đường dẫn Admin API không tồn tại.");
            }
            send(response, HttpServletResponse.SC_OK, Map.of("data", Map.of("message", "Đã lưu thay đổi.")));
        } catch (NotFound e) {
            send(response, HttpServletResponse.SC_NOT_FOUND, Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            send(response, HttpServletResponse.SC_BAD_REQUEST, Map.of("error", safeMessage(e)));
        } catch (DataAccessException e) {
            LOG.log(System.Logger.Level.ERROR, "Admin API update failed", e);
            send(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    Map.of("error", "Không thể lưu dữ liệu. Vui lòng kiểm tra kết nối cơ sở dữ liệu rồi thử lại."));
        }
    }

    private static String[] path(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path == null || path.isBlank() || "/".equals(path)) return new String[0];
        return java.util.Arrays.stream(path.split("/"))
                .filter(part -> !part.isBlank()).toArray(String[]::new);
    }

    private static long id(String raw) {
        try {
            long value = Long.parseLong(raw);
            if (value > 0) return value;
        } catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException("Mã bản ghi không hợp lệ.");
    }

    private static int intParam(HttpServletRequest request, String key, int fallback) {
        String value = request.getParameter(key);
        if (value == null || value.isBlank()) return fallback;
        try { return Math.max(1, Integer.parseInt(value)); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Tham số phân trang không hợp lệ."); }
    }

    private static String safeMessage(IllegalArgumentException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "Dữ liệu gửi lên không hợp lệ." : message;
    }

    private static void send(HttpServletResponse response, int status, Object payload) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(Json.stringify(payload));
    }

    private static final class NotFound extends RuntimeException {
        private NotFound(String message) { super(message); }
    }
}
