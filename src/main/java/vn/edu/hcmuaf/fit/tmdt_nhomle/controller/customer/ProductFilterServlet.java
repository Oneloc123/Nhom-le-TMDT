package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ProductStore;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.ProductFilterService;

import java.io.IOException;
import java.math.BigDecimal;

@WebServlet(name = "productFilterServlet", value = "/products")
public class ProductFilterServlet extends HttpServlet {
    private final ProductFilterService productFilterService = new ProductFilterService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String query = trim(request.getParameter("q"));
        String category = trim(request.getParameter("category"));
        BigDecimal minPrice = parsePrice(request.getParameter("minPrice"));
        BigDecimal maxPrice = parsePrice(request.getParameter("maxPrice"));
        Integer storeId = parseInteger(request.getParameter("storeId"));
        String sort = trim(request.getParameter("sort"));

        request.setAttribute("products", productFilterService.filter(
                query, category, minPrice, maxPrice, storeId, sort));
        request.setAttribute("categories", ProductStore.categories());
        request.setAttribute("query", query);
        request.setAttribute("category", category);
        request.setAttribute("minPrice", request.getParameter("minPrice"));
        request.setAttribute("maxPrice", request.getParameter("maxPrice"));
        request.setAttribute("storeId", storeId);
        request.setAttribute("sort", sort);
        request.getRequestDispatcher("/customer/products.jsp").forward(request, response);
    }

    private BigDecimal parsePrice(String value) {
        try {
            return value == null || value.isBlank() ? null : new BigDecimal(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Integer parseInteger(String value) {
        try {
            return value == null || value.isBlank() ? null : Integer.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
