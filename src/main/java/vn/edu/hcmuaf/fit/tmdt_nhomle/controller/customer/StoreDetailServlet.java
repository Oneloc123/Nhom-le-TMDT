package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ReviewDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ShopDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Store;

import java.io.IOException;

public class StoreDetailServlet extends HttpServlet {
    private static final int PAGE_SIZE = 10;
    private static final String ERROR_PAGE = "/customer/store-not-found.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("X-PrintKraft-Supplier-Detail", "2026-11-25-v2");
        request.setAttribute("supplierDetailVersion", "2026-11-25-v2");

        long shopId = parsePositive(request.getParameter("id"));
        if (shopId <= 0) {
            showError(request, response, HttpServletResponse.SC_NOT_FOUND,
                    "Không tìm thấy nhà cung cấp",
                    "Đường dẫn chưa có mã nhà cung cấp hợp lệ. Hãy quay lại danh sách và mở hồ sơ từ một thẻ nhà cung cấp.");
            return;
        }

        try {
            Store store = ShopDao.findById(shopId).orElse(null);
            if (store == null) {
                showError(request, response, HttpServletResponse.SC_NOT_FOUND,
                        "Nhà cung cấp không khả dụng",
                        "Không tìm thấy hồ sơ này hoặc nhà cung cấp chưa được duyệt/đang tạm ngưng. Hãy mở lại danh sách nhà cung cấp để chọn một hồ sơ đang hoạt động.");
                return;
            }

            long totalReviews = ReviewDao.countPublishedForShop(shopId);
            int totalPages = Math.max(1, (int) Math.ceil(totalReviews / (double) PAGE_SIZE));
            int requestedPage = parsePositiveInt(request.getParameter("page"));
            int currentPage = Math.min(requestedPage, totalPages);

            request.setAttribute("store", store);
            request.setAttribute("ratingSummary", ReviewDao.getRatingSummaryForShop(shopId));
            request.setAttribute("reviews", ReviewDao.listPublishedForShop(shopId, currentPage, PAGE_SIZE));
            request.setAttribute("reviewCount", totalReviews);
            request.setAttribute("currentPage", currentPage);
            request.setAttribute("totalPages", totalPages);

            try {
                request.getRequestDispatcher("/customer/supplier-detail.jsp").forward(request, response);
            } catch (ServletException viewError) {
                System.getLogger(StoreDetailServlet.class.getName()).log(
                        System.Logger.Level.ERROR,
                        "JSP nhà cung cấp bị lỗi khi render; shopId=" + shopId
                                + ", view=/customer/supplier-detail.jsp", viewError);
                if (!response.isCommitted()) {
                    response.resetBuffer();
                    showError(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            "Không thể hiển thị hồ sơ nhà cung cấp",
                            "Dữ liệu đã được truy vấn nhưng trang chi tiết gặp lỗi khi render JSP. Kiểm tra log Tomcat để xem nguyên nhân.");
                    return;
                }
                throw viewError;
            }
        } catch (RuntimeException e) {
            System.getLogger(StoreDetailServlet.class.getName()).log(
                    System.Logger.Level.ERROR,
                    "Không thể tải hồ sơ nhà cung cấp id=" + shopId, e);
            showError(request, response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Chưa thể tải hồ sơ nhà cung cấp",
                    "Máy chủ chưa tải được thông tin từ cơ sở dữ liệu. Hãy kiểm tra kết nối database và các bảng print_shops, reviews, products, users, sau đó tải lại trang.");
        }
    }

    private void showError(HttpServletRequest request, HttpServletResponse response,
                           int status, String title, String message)
            throws ServletException, IOException {
        response.setStatus(status);
        request.setAttribute("supplierErrorTitle", title);
        request.setAttribute("supplierErrorMessage", message);
        request.getRequestDispatcher(ERROR_PAGE).forward(request, response);
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
