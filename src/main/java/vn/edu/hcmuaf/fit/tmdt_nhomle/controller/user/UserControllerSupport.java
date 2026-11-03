package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.user;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.AuthFilter;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf;

abstract class UserControllerSupport extends HttpServlet {
    protected User currentUser(HttpServletRequest request) { return AuthFilter.currentUser(request); }
    protected void prepare(HttpServletRequest request) {
        request.setAttribute("csrfToken", Csrf.ensureToken(request));
        User user = currentUser(request);
        if (user != null) request.setAttribute("accountUser", user);
    }
    protected String safeRedirect(String path, String fallback) {
        if (path == null || !path.startsWith("/") || path.startsWith("//") || path.contains("\\") || path.contains("\r") || path.contains("\n")) return fallback;
        if (path.startsWith("/login") || path.startsWith("/logout") || path.startsWith("/register")) return fallback;
        return path;
    }
    protected String value(String v) { return v == null ? "" : v.trim(); }
}
