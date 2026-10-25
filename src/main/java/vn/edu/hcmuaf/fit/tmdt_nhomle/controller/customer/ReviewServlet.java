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

@WebServlet(name = "reviewServlet", value = "/review")
public class ReviewServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = AuthFilter.currentUser(request);
        ReviewDao.ReviewableItem item = resolveItem(request, user);
        if (item == null) {
            request.setAttribute("message", "Dòng hàng không tồn tại hoặc bạn chưa mua sản phẩm này.");
            request.getRequestDispatcher("/customer/not-found.jsp").forward(request, response);
            return;
        }
        if (!item.paid() || !item.completed()) {
            request.setAttribute("message",
                    "Chỉ đánh giá được sản phẩm sau khi đơn hàng đã hoàn thành.");
            request.getRequestDispatcher("/customer/not-found.jsp").forward(request, response);
            return;
        }
        request.setAttribute("item", item);
        request.setAttribute("isEdit", item.reviewed());
        if (item.reviewed()) {
            ReviewDao.findByOrderItem(item.orderItemId()).ifPresent(r -> {
                request.setAttribute("existingReview", r);
            });
        }
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
            var review = ReviewDao.createOrUpdate(orderItemId, user.getId(), rating, text);
            String redirectUrl = request.getContextPath() + "/product-detail?id="
                    + (review.getProductId() == null ? "" : review.getProductId())
                    + "&reviewed=1#reviews";
            response.sendRedirect(redirectUrl);
        } catch (IllegalArgumentException error) {
            request.setAttribute("error", error.getMessage());
            ReviewDao.ReviewableItem item = resolveItem(request, user);
            request.setAttribute("item", item);
            request.setAttribute("isEdit", item != null && item.reviewed());
            request.setAttribute("selectedRating", rating);
            request.setAttribute("enteredText", text);
            request.getRequestDispatcher("/customer/review-form.jsp").forward(request, response);
        }
    }

    private ReviewDao.ReviewableItem resolveItem(HttpServletRequest request, User user) {
        if (user == null) {
            return null;
        }
        long orderItemId = parseLong(request.getParameter("item"), -1);
        if (orderItemId > 0) {
            return ReviewDao.findReviewableItem(orderItemId, user.getId()).orElse(null);
        }
        long productId = parseLong(request.getParameter("product"), -1);
        if (productId > 0) {
            return ReviewDao.findReviewableItemForProduct(productId, user.getId()).orElse(null);
        }
        return null;
    }

    private long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
