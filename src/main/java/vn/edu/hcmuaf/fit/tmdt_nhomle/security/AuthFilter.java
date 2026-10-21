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
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf;

import java.util.List;
import java.util.Set;

@WebFilter(filterName = "authFilter", urlPatterns = "/*")
public class AuthFilter implements Filter {

    private static final List<String> LOGIN_REQUIRED = List.of(
            "/profile", "/addresses", "/order", "/order-detail", "/payment",
            "/purchase-history", "/favorites", "/quotes", "/review", "/product-comment");

    private static final Set<String> CSRF_REQUIRED_POST = Set.of(
            "/cart", "/order", "/payment", "/profile", "/addresses",
            "/favorites", "/review", "/quotes", "/admin/users",
            "/login", "/register", "/forgot-password", "/product-detail", "/product-comment");

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
            res.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Phiên làm việc không hợp lệ (CSRF). Vui lòng tải lại trang và thử lại.");
            return;
        }

        if (requiresLogin(path) && currentUser(req) == null) {
            res.sendRedirect(req.getContextPath() + "/login");
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
