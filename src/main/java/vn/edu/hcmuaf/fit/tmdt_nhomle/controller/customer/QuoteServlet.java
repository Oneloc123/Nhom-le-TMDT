package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.CategoryDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ProductDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.QuoteDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ShopDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.QuoteRequest;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.AuthFilter;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDate;

/**
 * Yêu cầu báo giá (doanh nghiệp / số lượng lớn) — bảng quote_requests.
 * - GET: danh sách yêu cầu của chính user + form tạo mới.
 * - POST: validate (sản phẩm, số lượng, ngày cần nhận) rồi tạo; PRG.
 */
@WebServlet(name = "quoteServlet", value = "/quotes")
public class QuoteServlet extends HttpServlet {

    private static final int MAX_QUANTITY = 1_000_000;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = AuthFilter.currentUser(request);
        request.setAttribute("quotes", QuoteDao.listByUser(user.getId()));
        request.setAttribute("products", ProductDao.search(
                new ProductDao.SearchCriteria(null, null, null, null, null, null), 1, 100));
        request.setAttribute("shops", ShopDao.search(""));
        request.setAttribute("categories", CategoryDao.findAllActive());
        request.setAttribute("selectedProductId",
                parseLong(request.getParameter("productId"), -1));
        request.getRequestDispatcher("/customer/quotes.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = AuthFilter.currentUser(request);
        request.setCharacterEncoding("UTF-8");
        try {
            long productId = parseLong(request.getParameter("productId"), -1);
            long shopId = parseLong(request.getParameter("preferredShopId"), 0);
            int quantity = (int) parseLong(request.getParameter("quantity"), 0);
            String companyName = trim(request.getParameter("companyName"));
            String requiredByRaw = trim(request.getParameter("requiredBy"));

            if (productId <= 0) {
                throw new IllegalArgumentException("Vui lòng chọn sản phẩm cần báo giá.");
            }
            if (quantity < 1 || quantity > MAX_QUANTITY) {
                throw new IllegalArgumentException(
                        "Số lượng phải từ 1 đến " + MAX_QUANTITY + ".");
            }
            if (companyName.isEmpty()) {
                throw new IllegalArgumentException("Vui lòng nhập tên doanh nghiệp.");
            }
            LocalDate requiredBy = null;
            if (!requiredByRaw.isEmpty()) {
                requiredBy = LocalDate.parse(requiredByRaw);
                if (requiredBy.isBefore(LocalDate.now())) {
                    throw new IllegalArgumentException("Ngày cần nhận phải từ hôm nay trở đi.");
                }
            }
            QuoteRequest created = QuoteDao.insert(user.getId(), productId,
                    shopId > 0 ? shopId : null, companyName, quantity,
                    requiredBy == null ? null : Timestamp.valueOf(requiredBy.atStartOfDay()));
            response.sendRedirect(request.getContextPath() + "/quotes?created="
                    + created.getQuoteRequestId());
        } catch (RuntimeException error) {
            request.setAttribute("error", error.getMessage());
            request.setAttribute("quotes", QuoteDao.listByUser(user.getId()));
            request.setAttribute("products", ProductDao.search(
                    new ProductDao.SearchCriteria(null, null, null, null, null, null), 1, 100));
            request.setAttribute("shops", ShopDao.search(""));
            request.getRequestDispatcher("/customer/quotes.jsp").forward(request, response);
        }
    }

    private long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
