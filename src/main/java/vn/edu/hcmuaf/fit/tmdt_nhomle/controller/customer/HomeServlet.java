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
        // Bán chạy: sắp xếp mặc định (nhiều đánh giá nhất).
        List<Product> bestsellers = ProductDao.search(
                new ProductDao.SearchCriteria(null, null, null, null, null, null),
                1, SECTION_SIZE);
        // Gợi ý: ưu tiên điểm đánh giá và số lượt đánh giá.
        List<Product> trending = ProductDao.search(
                new ProductDao.SearchCriteria(null, null, null, null, null, "rating"),
                1, SECTION_SIZE);

        request.setAttribute("bestsellers", bestsellers);
        List<Product> trending = ProductDao.getTrending(4);
        request.setAttribute("trending", trending);
        request.setAttribute("categories", CategoryDao.findAllActive());
        request.getRequestDispatcher("/customer/home.jsp").forward(request, response);
    }
}
