package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.FavoriteDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ProductDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Product;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.AuthFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "favoriteServlet", value = "/favorites")
public class FavoriteServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = AuthFilter.currentUser(request);
        List<Product> products = new ArrayList<>();
        for (Long productId : FavoriteDao.listProductIds(user.getId())) {
            ProductDao.findById(productId).ifPresent(products::add);
        }
        request.setAttribute("products", products);
        request.getRequestDispatcher("/customer/favorites.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        User user = AuthFilter.currentUser(request);
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        long productId = parseLong(request.getParameter("productId"), -1);
        boolean ajax = RegisterServlet.wantsJson(request);

        if (productId <= 0 || (!"add".equals(action) && !"remove".equals(action))) {
            respond(request, response, ajax, false, "Yêu cầu không hợp lệ.");
            return;
        }

        if ("remove".equals(action)) {
            boolean removed = FavoriteDao.remove(user.getId(), productId);
            respond(request, response, ajax, true,
                    removed ? "Đã bỏ khỏi danh sách yêu thích."
                            : "Sản phẩm không có trong danh sách yêu thích.");
            return;
        }

        // “add”: kiểm tra sản phẩm còn hiển thị/được duyệt ở SERVER,
        // chống trùng bằng check + unique index (user_id, product_id).
        if (ProductDao.findById(productId).isEmpty()) {
            respond(request, response, ajax, false,
                    "Sản phẩm không tồn tại hoặc đã ngừng hiển thị.");
            return;
        }
        boolean added = FavoriteDao.add(user.getId(), productId);
        respond(request, response, ajax, true,
                added ? "Đã thêm vào danh sách yêu thích."
                        : "Sản phẩm đã có trong danh sách yêu thích.");
    }

    /** Trả JSON cho AJAX; redirect cho submit form thường (không JS). */
    private void respond(HttpServletRequest request, HttpServletResponse response,
                         boolean ajax, boolean success, String message) throws IOException {
        if (ajax) {
            response.setStatus(success ? HttpServletResponse.SC_OK
                    : HttpServletResponse.SC_BAD_REQUEST);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json;charset=UTF-8");
            response.setHeader("Cache-Control", "no-store");
            response.getWriter().write(vn.edu.hcmuaf.fit.tmdt_nhomle.util.Json.stringify(
                    java.util.Map.of("success", success, "message", message)));
            return;
        }
        response.sendRedirect(request.getContextPath() + "/favorites"
                + (success ? "" : "?error=" + java.net.URLEncoder.encode(message,
                        java.nio.charset.StandardCharsets.UTF_8)));
    }

    private long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
