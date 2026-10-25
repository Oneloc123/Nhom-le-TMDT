package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ReviewDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.AuthFilter;

import java.io.IOException;

/**
 * Đánh giá sản phẩm đã mua, gắn với order_item_id cụ thể.
 * - GET  /review?item=<orderItemId>: form đánh giá — chỉ khi dòng hàng thuộc
 *   đơn của user, đơn đã thanh toán và chưa từng được đánh giá.
 * - POST: validate điểm 1–5 + nội dung, ghi reviews (order_item_id UNIQUE).
 * PRG: quay về trang sản phẩm, mục đánh giá.
 */
@WebServlet(name = "reviewServlet", value = "/review")
public class ReviewServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = AuthFilter.currentUser(request);
        ReviewDao.ReviewableItem item = resolveItem(request, user);
        if (item == null) {
            request.setAttribute("message", "Dòng hàng không tồn tại hoặc bạn không có quyền đánh giá.");
            request.getRequestDispatcher("/customer/not-found.jsp").forward(request, response);
            return;
        }
        if (!item.paid() || !item.completed()) {
            request.setAttribute("message",
                    "Chỉ đánh giá được sản phẩm sau khi đơn hàng đã hoàn thành.");
            request.getRequestDispatcher("/customer/not-found.jsp").forward(request, response);
            return;
        }
        if (item.reviewed()) {
            response.sendRedirect(request.getContextPath() + "/product-detail?id=" + item.productId());
            return;
        }
        request.setAttribute("item", item);
        request.getRequestDispatcher("/customer/review-form.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = AuthFilter.currentUser(request);
        request.setCharacterEncoding("UTF-8");
        long orderItemId = parseLong(request.getParameter("item"), -1);
        int rating = (int) parseLong(request.getParameter("rating"), 0);
        String text = request.getParameter("reviewText");
        try {
            var review = ReviewDao.create(orderItemId, user.getId(), rating, text);
            response.sendRedirect(request.getContextPath() + "/product-detail?id="
                    + (review.getProductId() == null ? "" : review.getProductId()) + "#reviews");
        } catch (IllegalArgumentException error) {
            request.setAttribute("error", error.getMessage());
            ReviewDao.ReviewableItem item = resolveItem(request, user);
            request.setAttribute("item", item);
            request.getRequestDispatcher("/customer/review-form.jsp").forward(request, response);
        }
    }

    private ReviewDao.ReviewableItem resolveItem(HttpServletRequest request, User user) {
        long orderItemId = parseLong(request.getParameter("item"), -1);
        if (orderItemId <= 0 || user == null) {
            return null;
        }
        return ReviewDao.findReviewableItem(orderItemId, user.getId()).orElse(null);
    }

    private long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
