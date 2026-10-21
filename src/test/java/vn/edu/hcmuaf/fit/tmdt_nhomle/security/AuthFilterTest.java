package vn.edu.hcmuaf.fit.tmdt_nhomle.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthFilterTest {
    @Test
    void redirectsUnauthenticatedAdminPageToLogin() throws Exception {
        AtomicReference<String> redirect = new AtomicReference<>();
        AtomicBoolean chainCalled = new AtomicBoolean();
        HttpServletRequest request = request("/app/admin/users", "/app", "GET", null, null);
        HttpServletResponse response = proxy(HttpServletResponse.class, (proxy, method, args) -> {
            if ("sendRedirect".equals(method.getName())) redirect.set((String) args[0]);
            return defaultValue(method.getReturnType());
        });
        FilterChain chain = chain(chainCalled);

        new AuthFilter().doFilter(request, response, chain);

        assertEquals("/app/login", redirect.get());
        assertFalse(chainCalled.get());
    }

    @Test
    void doesNotReturnAdminApiDataToAnUnauthenticatedSession() throws Exception {
        AtomicInteger status = new AtomicInteger();
        AtomicBoolean chainCalled = new AtomicBoolean();
        StringWriter body = new StringWriter();
        HttpServletRequest request = request("/app/admin/api/users", "/app", "GET", null, null);
        HttpServletResponse response = proxy(HttpServletResponse.class, (proxy, method, args) -> {
            if ("setStatus".equals(method.getName())) status.set((Integer) args[0]);
            if ("getWriter".equals(method.getName())) return new PrintWriter(body);
            return defaultValue(method.getReturnType());
        });

        new AuthFilter().doFilter(request, response, chain(chainCalled));

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, status.get());
        assertTrue(body.toString().contains("Phiên đăng nhập đã hết hạn"));
        assertFalse(chainCalled.get());
    }

    @Test
    void redirectsUnauthenticatedWorkshopPageToLogin() throws Exception {
        AtomicReference<String> redirect = new AtomicReference<>();
        AtomicBoolean chainCalled = new AtomicBoolean();
        HttpServletRequest request = request("/app/workshop/dashboard", "/app", "GET", null, null);
        HttpServletResponse response = proxy(HttpServletResponse.class, (proxy, method, args) -> {
            if ("sendRedirect".equals(method.getName())) redirect.set((String) args[0]);
            return defaultValue(method.getReturnType());
        });

        new AuthFilter().doFilter(request, response, chain(chainCalled));

        assertEquals("/app/login", redirect.get());
        assertFalse(chainCalled.get());
    }

    @Test
    void workshopApiDoesNotReturnDataWithoutSession() throws Exception {
        AtomicInteger status = new AtomicInteger();
        AtomicBoolean chainCalled = new AtomicBoolean();
        StringWriter body = new StringWriter();
        HttpServletRequest request = request("/app/workshop/api/dashboard", "/app", "GET", null, null);
        HttpServletResponse response = proxy(HttpServletResponse.class, (proxy, method, args) -> {
            if ("setStatus".equals(method.getName())) status.set((Integer) args[0]);
            if ("getWriter".equals(method.getName())) return new PrintWriter(body);
            return defaultValue(method.getReturnType());
        });

        new AuthFilter().doFilter(request, response, chain(chainCalled));

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, status.get());
        assertTrue(body.toString().contains("Phiên đăng nhập đã hết hạn"));
        assertFalse(chainCalled.get());
    }

    @Test
    void acceptsCsrfHeaderAndRejectsMissingToken() {
        HttpSession session = proxy(HttpSession.class, (proxy, method, args) ->
                "getAttribute".equals(method.getName()) && Csrf.SESSION_ATTR.equals(args[0]) ? "known-token" : defaultValue(method.getReturnType()));
        assertTrue(Csrf.validate(request("/admin/api/users/1/status", "", "POST", session, "known-token")));
        assertFalse(Csrf.validate(request("/admin/api/users/1/status", "", "POST", session, null)));
    }

    private static HttpServletRequest request(String uri, String context, String method,
                                             HttpSession session, String csrfHeader) {
        return proxy(HttpServletRequest.class, (proxy, called, args) -> switch (called.getName()) {
            case "getRequestURI" -> uri;
            case "getContextPath" -> context;
            case "getMethod" -> method;
            case "getSession" -> session;
            case "getHeader" -> Csrf.HEADER.equals(args[0]) ? csrfHeader : null;
            case "getParameter" -> null;
            default -> defaultValue(called.getReturnType());
        });
    }

    private static FilterChain chain(AtomicBoolean invoked) {
        return proxy(FilterChain.class, (proxy, method, args) -> {
            if ("doFilter".equals(method.getName())) invoked.set(true);
            return defaultValue(method.getReturnType());
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive() || type == void.class) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0f;
        if (type == double.class) return 0d;
        return null;
    }
}
