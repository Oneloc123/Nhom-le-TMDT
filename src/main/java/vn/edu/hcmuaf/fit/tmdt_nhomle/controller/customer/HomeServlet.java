package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.CategoryDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ProductDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Product;

import java.io.IOException;
import java.util.List;

/**
 * Trang chủ động: nạp danh mục + sản phẩm nổi bật / gợi ý
 * trực tiếp từ database (thay cho HTML cứng như trước).
 */
@WebServlet(name = "homeServlet", value = "/home")
public class HomeServlet extends HttpServlet {

    private static final int SECTION_SIZE = 4;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Bán chạy: ưu tiên tổng số lượng đã bán.
        List<Product> bestsellers = ProductDao.getBestsellers(SECTION_SIZE);
        // Xem nhiều: ưu tiên bộ đếm lượt mở trang chi tiết.
        List<Product> mostViewed = ProductDao.getMostViewed(SECTION_SIZE);

        request.setAttribute("bestsellers", bestsellers);
        request.setAttribute("mostViewed", mostViewed);
        request.setAttribute("categories", CategoryDao.findAllActive());
        request.getRequestDispatcher("/customer/home.jsp").forward(request, response);
    }
}
