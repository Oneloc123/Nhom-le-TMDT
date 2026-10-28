package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ProductCommentDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.AuthFilter;

import java.io.IOException;

@WebServlet(name = "productCommentServlet", value = "/product-comment")
public class ProductCommentServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        long productId = parseLong(request.getParameter("productId"), -1);
        if (productId <= 0) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        User user = AuthFilter.currentUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login?redirect="
                    + java.net.URLEncoder.encode(request.getContextPath() + "/product-detail?id=" + productId + "#comments", java.nio.charset.StandardCharsets.UTF_8));
            return;
        }

        String action = request.getParameter("action");
        if ("delete".equalsIgnoreCase(action)) {
            long commentId = parseLong(request.getParameter("commentId"), -1);
            if (commentId > 0) {
                ProductCommentDao.delete(commentId, user.getId(), user.isAdmin());
            }
            response.sendRedirect(request.getContextPath() + "/product-detail?id=" + productId + "#comments");
            return;
        }

        Long parentId = parseLongOrNull(request.getParameter("parentId"));
        String content = request.getParameter("content");

        boolean isSeller = ProductCommentDao.isSellerForProduct(productId, user.getId())
                || user.isAdmin()
                || "SELLER".equalsIgnoreCase(user.getRole());

        try {
            ProductCommentDao.create(productId, user.getId(), parentId, content, isSeller);
            response.sendRedirect(request.getContextPath() + "/product-detail?id=" + productId + "&commentSuccess=1#comments");
        } catch (IllegalArgumentException e) {
            request.getSession().setAttribute("commentError", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/product-detail?id=" + productId + "#new-comment-box");
        }
    }

    private long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private Long parseLongOrNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            long parsed = Long.parseLong(value.trim());
            return parsed > 0 ? parsed : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
