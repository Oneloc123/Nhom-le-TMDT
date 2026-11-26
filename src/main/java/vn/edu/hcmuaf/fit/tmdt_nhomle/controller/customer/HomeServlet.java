package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.CategoryDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ProductDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Product;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.AuthFilter;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.ProductRecommendationService;

import java.io.IOException;
import java.util.List;

/**
 * Trang chủ động: nạp danh mục + sản phẩm nổi bật / gợi ý
 * trực tiếp từ database (thay cho HTML cứng như trước).
 */
@WebServlet(name = "homeServlet", value = "/home")
public class HomeServlet extends HttpServlet {

    private static final int SECTION_SIZE = 4;
    private final ProductRecommendationService recommendationService = new ProductRecommendationService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User currentUser = AuthFilter.currentUser(request);
        Long userId = currentUser == null ? null : Long.valueOf(currentUser.getId());
        HttpSession session = request.getSession(false);
        String sessionKey = session == null ? null : session.getId();

        // Bán chạy: ưu tiên tổng số lượng ròng từ đơn hợp lệ (đã loại hủy và trừ hàng hoàn).
        List<Product> bestsellers = ProductDao.getBestsellers(SECTION_SIZE);
        // Gợi ý dành cho bạn: lọc dựa trên nội dung + hành vi cá nhân (kể cả lịch sử xem) + độ phổ biến.
        List<Product> recommendedProducts = recommendationService.recommendForUser(userId, sessionKey, SECTION_SIZE);
        // Xem nhiều: ưu tiên bộ đếm lượt mở trang chi tiết.
        List<Product> mostViewed = ProductDao.getMostViewed(SECTION_SIZE);

        request.setAttribute("bestsellers", bestsellers);
        request.setAttribute("recommendedProducts", recommendedProducts);
        request.setAttribute("hasPersonalRecommendations",
                recommendationService.hasPersonalSignals(userId, sessionKey));
        request.setAttribute("mostViewed", mostViewed);
        request.setAttribute("categories", CategoryDao.findAllActive());
        request.getRequestDispatcher("/customer/home.jsp").forward(request, response);
    }
}
