package vn.edu.hcmuaf.fit.tmdt_nhomle.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import java.io.IOException;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.AjaxResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf;

import java.util.List;
import java.util.Set;

/**
 * Filter tập trung cho khu vực khách hàng:
 *  1. Yêu cầu đăng nhập cho các route cần tài khoản (redirect về /login).
 *  2. Kiểm tra CSRF token trên mọi POST tới các route thay đổi trạng thái
 *     của khách hàng (giỏ hàng, đặt hàng, thanh toán, hồ sơ, địa chỉ...).
 *
 * Khu vực Admin giữ nguyên cơ chế cũ (kiểm tra bên trong từng Servlet Admin)
 * và không nằm trong phạm vi nâng cấp này.
 */
@WebFilter(filterName = "authFilter", urlPatterns = "/*")
public class AuthFilter implements Filter {

    /** Route yêu cầu đã đăng nhập (prefix match trên path sau context path). */
    private static final List<String> LOGIN_REQUIRED = List.of(
            "/profile", "/addresses", "/order", "/order-detail", "/payment",
            "/purchase-history", "/favorites", "/quotes", "/review");

    /** POST route bắt buộc có CSRF token hợp lệ. */
    private static final Set<String> CSRF_REQUIRED_POST = Set.of(
            "/cart", "/order", "/order-detail", "/payment", "/profile", "/addresses",
            "/favorites", "/review", "/quotes",
            "/login", "/register", "/forgot-password");

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = pathAfterContext(req);
        String method = req.getMethod();

        if ("POST".equals(method) && requiresCsrf(path) && !Csrf.validate(req)) {
            if (AjaxResponse.isRequested(req)) {
                AjaxResponse.send(res, HttpServletResponse.SC_FORBIDDEN,
                        "{\"success\":false,\"message\":"
                                + AjaxResponse.quote("Phiên làm việc đã hết hạn. Vui lòng tải lại trang và thử lại.")
                                + "}");
                return;
            }
            res.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Phiên làm việc không hợp lệ (CSRF). Vui lòng tải lại trang và thử lại.");
            return;
        }

        if (requiresLogin(path) && currentUser(req) == null) {
            if (AjaxResponse.isRequested(req)) {
                AjaxResponse.send(res, HttpServletResponse.SC_UNAUTHORIZED,
                        "{\"success\":false,\"loginRequired\":true,\"loginUrl\":"
                                + AjaxResponse.quote(req.getContextPath() + "/login") + "}");
                return;
            }
            String destination = path;
            if (req.getQueryString() != null && !req.getQueryString().isBlank()) {
                destination += "?" + req.getQueryString();
            }
            res.sendRedirect(req.getContextPath() + "/login?redirect="
                    + java.net.URLEncoder.encode(destination,
                    java.nio.charset.StandardCharsets.UTF_8));
            return;
        }

        chain.doFilter(request, response);
    }

    private static boolean requiresLogin(String path) {
        return LOGIN_REQUIRED.stream().anyMatch(path::startsWith);
    }

    private static boolean requiresCsrf(String path) {
        return CSRF_REQUIRED_POST.contains(path);
    }

    public static User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute("currentUser");
    }

    private static String pathAfterContext(HttpServletRequest req) {
        String uri = req.getRequestURI();
        String context = req.getContextPath();
        if (context != null && !context.isEmpty() && uri.startsWith(context)) {
            return uri.substring(context.length());
        }
        return uri;
    }
}
