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
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.UserDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.Json;

import java.util.List;
import java.util.Set;
import java.util.Map;

@WebFilter(filterName = "authFilter", urlPatterns = "/*")
public class AuthFilter implements Filter {

    private static final List<String> LOGIN_REQUIRED = List.of(
            "/profile", "/addresses", "/order", "/order-detail", "/payment",
            "/purchase-history", "/favorites", "/quotes", "/review");

    private static final Set<String> CSRF_REQUIRED_POST = Set.of(
            "/cart", "/order", "/payment", "/profile", "/addresses",
            "/favorites", "/review", "/quotes", "/admin/users",
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

        if (isAdminPath(path)) {
            User sessionUser = currentUser(req);
            if (sessionUser == null) {
                unauthenticated(req, res);
                return;
            }

            User liveUser;
            try {
                liveUser = UserDao.findById(sessionUser.getId()).orElse(null);
            } catch (DataAccessException e) {
                System.getLogger(AuthFilter.class.getName()).log(System.Logger.Level.ERROR,
                        "Admin access check could not verify the current account", e);
                if (isAdminApi(path)) {
                    res.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write(Json.stringify(Map.of("error", "Không thể xác minh tài khoản quản trị lúc này.")));
                } else {
                    res.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                            "Không thể xác minh tài khoản quản trị lúc này.");
                }
                return;
            }

            if (liveUser == null || !liveUser.isActive()) {
                HttpSession session = req.getSession(false);
                if (session != null) session.invalidate();
                unauthenticated(req, res);
                return;
            }
            if (!liveUser.isAdmin()) {
                if (isAdminApi(path)) {
                    res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write(Json.stringify(Map.of("error", "Tài khoản không có quyền Admin.")));
                } else {
                    res.sendError(HttpServletResponse.SC_FORBIDDEN, "Tài khoản không có quyền Admin.");
                }
                return;
            }
            req.getSession(false).setAttribute("currentUser", liveUser);
        }

        if ("POST".equals(method) && requiresCsrf(path) && !Csrf.validate(req)) {
            if (isAdminApi(path)) {
                res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                res.setCharacterEncoding("UTF-8");
                res.setContentType("application/json");
                res.getWriter().write(Json.stringify(Map.of("error", "Phiên làm việc không hợp lệ. Hãy tải lại trang rồi thử lại.")));
            } else {
                res.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Phiên làm việc không hợp lệ (CSRF). Vui lòng tải lại trang và thử lại.");
            }
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
        return isAdminPath(path) || CSRF_REQUIRED_POST.contains(path);
    }

    private static boolean isAdminPath(String path) {
        return "/admin".equals(path) || path.startsWith("/admin/");
    }

    private static boolean isAdminApi(String path) {
        return path.startsWith("/admin/api/");
    }

    private static void unauthenticated(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if (isAdminApi(pathAfterContext(request))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(Json.stringify(Map.of("error", "Phiên đăng nhập đã hết hạn.")));
        } else {
            response.sendRedirect(request.getContextPath() + "/login");
        }
    }

    public static User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute("currentUser");
        return value instanceof User user ? user : null;
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
