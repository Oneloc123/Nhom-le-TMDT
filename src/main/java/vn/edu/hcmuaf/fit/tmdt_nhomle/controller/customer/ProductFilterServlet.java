package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.CategoryDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ShopDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Category;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.ProductFilterService;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@WebServlet(name = "productFilterServlet", value = "/products")
public class ProductFilterServlet extends HttpServlet {

    private static final int PAGE_SIZE = 8;

    private final ProductFilterService productFilterService = new ProductFilterService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String query = trim(request.getParameter("q"));
        String category = trim(request.getParameter("category"));
        BigDecimal minPrice = parsePrice(request.getParameter("minPrice"));
        BigDecimal maxPrice = parsePrice(request.getParameter("maxPrice"));
        Long shopId = parseLong(request.getParameter("storeId"));
        String sort = trim(request.getParameter("sort"));
        int page = Math.max(1, parseInt(request.getParameter("page"), 1));

        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            request.setAttribute("filterError", "Giá từ không được lớn hơn giá đến.");
        }

        ProductFilterService.Page result = productFilterService.search(
                query, category, minPrice, maxPrice, shopId, sort, page, PAGE_SIZE);

        List<String> categoryNames = CategoryDao.findAllActive().stream()
                .map(Category::getName).toList();

        request.setAttribute("products", result.items());
        request.setAttribute("categories", categoryNames);
        request.setAttribute("query", query);
        request.setAttribute("category", category);
        request.setAttribute("minPrice", request.getParameter("minPrice"));
        request.setAttribute("maxPrice", request.getParameter("maxPrice"));
        request.setAttribute("storeId", shopId);
        request.setAttribute("storeName", shopId == null ? null
                : ShopDao.findById(shopId).map(store -> store.getName()).orElse(null));
        request.setAttribute("sort", sort);
        request.setAttribute("currentPage", result.pageNumber());
        request.setAttribute("totalPages", result.totalPages());
        request.setAttribute("totalItems", result.totalItems());
        request.getRequestDispatcher("/customer/products.jsp").forward(request, response);
    }

    private BigDecimal parsePrice(String value) {
        try {
            return value == null || value.isBlank() ? null : new BigDecimal(value).max(BigDecimal.ZERO);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Long parseLong(String value) {
        try {
            return value == null || value.isBlank() ? null : Long.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
