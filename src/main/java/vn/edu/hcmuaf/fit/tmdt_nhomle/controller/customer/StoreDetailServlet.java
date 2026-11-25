package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ReviewDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ShopDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Review;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Store;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class StoreDetailServlet extends HttpServlet {
    private static final int PAGE_SIZE = 10;
    private static final String VIEW = "/customer/supplier-detail.jsp";
    private static final String VERSION = "2026-11-25-v3";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("X-PrintKraft-Supplier-Detail", VERSION);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");

        long shopId = parsePositiveLong(request.getParameter("id"));
        if (shopId <= 0) {
            renderError(response, HttpServletResponse.SC_NOT_FOUND,
                    "Không tìm thấy nhà cung cấp",
                    "Đường dẫn chưa có mã nhà cung cấp hợp lệ. Hãy quay lại danh sách và chọn một nhà cung cấp.");
            return;
        }

        final Store store;
        final int currentPage;
        final int totalPages;

        try {
            store = ShopDao.findById(shopId).orElse(null);
            if (store == null) {
                renderError(response, HttpServletResponse.SC_NOT_FOUND,
                        "Nhà cung cấp không khả dụng",
                        "Không tìm thấy hồ sơ này hoặc nhà cung cấp chưa được duyệt/đang tạm ngưng.");
                return;
            }

            ReviewDao.RatingSummary summary = ReviewDao.getRatingSummaryForShop(shopId);
            long reviewCount = summary.totalReviews();
            totalPages = Math.max(1, (int) Math.ceil(reviewCount / (double) PAGE_SIZE));
            currentPage = Math.min(parsePositiveInt(request.getParameter("page")), totalPages);
            List<Review> reviews = ReviewDao.listPublishedForShop(shopId, currentPage, PAGE_SIZE);

            request.setAttribute("store", store);
            request.setAttribute("reviews", reviews);
            request.setAttribute("reviewCount", reviewCount);
            request.setAttribute("ratingAverageText",
                    String.format(Locale.US, "%.1f", summary.averageRating()));
            request.setAttribute("ratingStars", starsFor(summary.averageRating()));
            request.setAttribute("star5Count", summary.star5Count());
            request.setAttribute("star4Count", summary.star4Count());
            request.setAttribute("star3Count", summary.star3Count());
            request.setAttribute("star2Count", summary.star2Count());
            request.setAttribute("star1Count", summary.star1Count());
            request.setAttribute("currentPage", currentPage);
            request.setAttribute("totalPages", totalPages);
            request.setAttribute("hasPreviousPage", currentPage > 1);
            request.setAttribute("hasNextPage", currentPage < totalPages);
            request.setAttribute("previousPage", Math.max(1, currentPage - 1));
            request.setAttribute("nextPage", Math.min(totalPages, currentPage + 1));
            request.setAttribute("supplierInitial", firstCharacter(store.getName()));
            request.setAttribute("supplierLogoSrc", resolveLogoUrl(store.getLogoUrl(), request.getContextPath()));
            request.setAttribute("supplierPhoneLink", normalizePhone(store.getContactPhone()));
            request.setAttribute("supplierDetailVersion", VERSION);
        } catch (RuntimeException e) {
            System.getLogger(StoreDetailServlet.class.getName()).log(
                    System.Logger.Level.ERROR,
                    "Không thể tải dữ liệu hồ sơ nhà cung cấp; shopId=" + shopId, e);
            renderError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Chưa thể tải hồ sơ nhà cung cấp",
                    "Máy chủ không đọc được dữ liệu nhà cung cấp. Kiểm tra log Tomcat và kết nối database rồi tải lại trang.");
            return;
        }

        try {
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (ServletException viewError) {
            System.getLogger(StoreDetailServlet.class.getName()).log(
                    System.Logger.Level.ERROR,
                    "JSP nhà cung cấp bị lỗi khi render; shopId=" + shopId + ", view=" + VIEW, viewError);
            if (response.isCommitted()) {
                throw viewError;
            }
            renderError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Không thể hiển thị hồ sơ nhà cung cấp",
                    "Dữ liệu đã được truy vấn nhưng trang chi tiết gặp lỗi khi render JSP. Xem log Tomcat tại dòng 'JSP nhà cung cấp bị lỗi khi render' để biết lỗi gốc.");
        }
    }

    private static long parsePositiveLong(String raw) {
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

    private static String firstCharacter(String value) {
        if (value == null || value.isBlank()) return "P";
        return value.trim().substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private static String starsFor(double average) {
        int filled = Math.min(5, Math.max(0, (int) Math.round(average)));
        return "★".repeat(filled) + "☆".repeat(5 - filled);
    }

    private static String normalizePhone(String phone) {
        if (phone == null) return "";
        return phone.replaceAll("[^0-9+]", "");
    }

    private static String resolveLogoUrl(String rawUrl, String contextPath) {
        if (rawUrl == null || rawUrl.isBlank()) return "";
        String value = rawUrl.trim();
        if (value.startsWith("https://") || value.startsWith("http://") || value.startsWith("//")) {
            return value;
        }
        if (value.startsWith("/")) {
            return contextPath + value;
        }
        return contextPath + "/" + value;
    }

    private static void renderError(HttpServletResponse response, int status, String title, String message)
            throws IOException {
        if (response.isCommitted()) return;
        response.resetBuffer();
        response.setStatus(status);
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>PrintKraft - Không thể mở nhà cung cấp</title>
                    <style>
                        *{box-sizing:border-box}body{margin:0;padding:24px;background:#f5f6fc;color:#20243a;font:15px/1.6 Arial,sans-serif}
                        main{max-width:620px;margin:8vh auto;padding:32px;border:1px solid #e4e6f4;border-radius:20px;background:#fff;box-shadow:0 16px 42px #252b5210}
                        h1{font-size:25px;line-height:1.3;margin:0 0 12px}p{color:#68718a;margin:0 0 24px}
                        a{display:inline-block;padding:10px 15px;border-radius:10px;background:#6c5ce7;color:#fff;text-decoration:none;font-weight:700}
                    </style>
                </head>
                <body><main><h1>""" + escapeHtml(title) + """
                </h1><p>""" + escapeHtml(message) + """
                </p><a href="""" + escapeHtml(response.encodeURL("stores")) + """">Quay lại nhà cung cấp</a></main></body></html>
                """);
    }

    private static String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
