package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ReviewDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ShopDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Store;

import java.io.IOException;

@WebServlet(name = "storeDetailServlet", value = "/store-detail")
public class StoreDetailServlet extends HttpServlet {
    private static final int PAGE_SIZE = 10;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long shopId = parsePositive(request.getParameter("id"));
        if (shopId <= 0) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Store store = ShopDao.findById(shopId).orElse(null);
        if (store == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        long totalReviews = ReviewDao.countPublishedForShop(shopId);
        int totalPages = Math.max(1, (int) Math.ceil(totalReviews / (double) PAGE_SIZE));
        int requestedPage = Math.max(1, parsePositiveInt(request.getParameter("page")));
        int currentPage = Math.min(requestedPage, totalPages);

        request.setAttribute("store", store);
        request.setAttribute("ratingSummary", ReviewDao.getRatingSummaryForShop(shopId));
        request.setAttribute("reviews", ReviewDao.listPublishedForShop(shopId, currentPage, PAGE_SIZE));
        request.setAttribute("reviewCount", totalReviews);
        request.setAttribute("currentPage", currentPage);
        request.setAttribute("totalPages", totalPages);
        request.getRequestDispatcher("/customer/store-detail.jsp").forward(request, response);
    }

    private static long parsePositive(String raw) {
        if (raw == null || raw.isBlank()) return 0;
        try {
            long value = Long.parseLong(raw.trim());
            return value > 0 ? value : 0;
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static int parsePositiveInt(String raw) {
        if (raw == null || raw.isBlank()) return 1;
        try {
            int value = Integer.parseInt(raw.trim());
            return value > 0 ? value : 1;
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }
}
