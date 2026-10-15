package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.FavoriteDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ReviewDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.OrderStatus;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Product;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.AuthFilter;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.ProductDetailService;

import java.io.IOException;

/**
 * Chi tiết sản phẩm từ database theo id.
 * ID không hợp lệ hoặc sản phẩm không còn bán → trang "Không tìm thấy" thân thiện (404).
 */
@WebServlet(name = "productDetailServlet", value = "/product-detail")
public class ProductDetailServlet extends HttpServlet {
    private final ProductDetailService productDetailService = new ProductDetailService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long productId = parseLong(request.getParameter("id"), -1);
        Product product = productId < 0 ? null : productDetailService.findProduct(productId).orElse(null);
        if (product == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            request.setAttribute("message", "Sản phẩm không tồn tại hoặc đã ngừng bán.");
            request.getRequestDispatcher("/customer/not-found.jsp").forward(request, response);
            return;
        }
        request.setAttribute("product", product);
        // Đánh giá đã đăng (public) + trạng thái yêu thích của người dùng hiện tại.
        request.setAttribute("reviews", ReviewDao.listPublishedForProduct(product.getId()));
        User user = AuthFilter.currentUser(request);
        request.setAttribute("favorite",
                user != null && FavoriteDao.exists(user.getId(), product.getId()));
        request.getRequestDispatcher("/customer/product-detail.jsp").forward(request, response);
    }

    private long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
