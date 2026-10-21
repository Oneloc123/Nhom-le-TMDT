package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.FavoriteDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ProductCommentDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ReviewDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.OrderStatus;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Product;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.AuthFilter;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.ProductDetailService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database; // Bổ sung import Database

import java.io.IOException;
import java.sql.Connection; // Bổ sung import Connection
import java.sql.PreparedStatement; // Bổ sung import PreparedStatement
import java.sql.SQLException; // Bổ sung import SQLException

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

//       số lượt xem
        String updateViewSql = "UPDATE products SET view_count = view_count + 1 WHERE product_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(updateViewSql)) {
            ps.setLong(1, productId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        request.setAttribute("product", product);
        request.setAttribute("reviews", ReviewDao.listPublishedForProduct(product.getId()));
        request.setAttribute("ratingSummary", ReviewDao.getRatingSummary(product.getId()));
        request.setAttribute("reviewSuccess", "1".equals(request.getParameter("reviewed")));

        // Tải danh sách bình luận / hỏi đáp về sản phẩm
        request.setAttribute("comments", ProductCommentDao.listForProduct(product.getId()));
        request.setAttribute("commentsCount", ProductCommentDao.countForProduct(product.getId()));
        request.setAttribute("commentSuccess", "1".equals(request.getParameter("commentSuccess")));

        Object commentError = request.getSession().getAttribute("commentError");
        if (commentError != null) {
            request.setAttribute("commentError", commentError);
            request.getSession().removeAttribute("commentError");
        }

        Object reviewError = request.getSession().getAttribute("reviewError");
        if (reviewError != null) {
            request.setAttribute("reviewError", reviewError);
            request.getSession().removeAttribute("reviewError");
        }

        User user = AuthFilter.currentUser(request);
        request.setAttribute("favorite",
                user != null && FavoriteDao.exists(user.getId(), product.getId()));

        boolean isProductSeller = user != null && (ProductCommentDao.isSellerForProduct(product.getId(), user.getId())
                || user.isAdmin()
                || "SELLER".equalsIgnoreCase(user.getRole()));
        request.setAttribute("isProductSeller", isProductSeller);

        if (user != null) {
            ReviewDao.findReviewableItemForProduct(product.getId(), user.getId())
                    .ifPresent(item -> request.setAttribute("userReviewItem", item));
            ReviewDao.findUserReviewForProduct(product.getId(), user.getId())
                    .ifPresent(rev -> request.setAttribute("currentUserReview", rev));
        }

        request.getRequestDispatcher("/customer/product-detail.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        long productId = parseLong(request.getParameter("productId"), -1);
        if (productId <= 0) {
            productId = parseLong(request.getParameter("id"), -1);
        }

        User user = AuthFilter.currentUser(request);
        if (user == null) {
            String redirectUrl = request.getContextPath() + "/product-detail?id=" + productId + "#reviews";
            response.sendRedirect(request.getContextPath() + "/login?redirect="
                    + java.net.URLEncoder.encode(redirectUrl, java.nio.charset.StandardCharsets.UTF_8));
            return;
        }

        String action = request.getParameter("action");
        if ("delete".equalsIgnoreCase(action)) {
            long reviewId = parseLong(request.getParameter("reviewId"), -1);
            if (reviewId > 0) {
                ReviewDao.deleteReview(reviewId, user.getId());
            }
            response.sendRedirect(request.getContextPath() + "/product-detail?id=" + productId + "#reviews");
            return;
        }

        int rating = (int) parseLong(request.getParameter("rating"), 5);
        String text = request.getParameter("reviewText");
        try {
            ReviewDao.postReviewOrComment(productId, user.getId(), rating, text);
            response.sendRedirect(request.getContextPath() + "/product-detail?id=" + productId + "&reviewed=1#reviews");
        } catch (IllegalArgumentException e) {
            request.getSession().setAttribute("reviewError", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/product-detail?id=" + productId + "#review-form-box");
        }
    }

    private long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}