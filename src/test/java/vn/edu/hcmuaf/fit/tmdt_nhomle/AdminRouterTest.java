package vn.edu.hcmuaf.fit.tmdt_nhomle;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import vn.edu.hcmuaf.fit.tmdt_nhomle.controller.admin.AdminApiServlet;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminRouterTest {
    @Test
    void adminShellAndApiUseSpecificConsistentMappings() {
        assertArrayEquals(new String[]{"/admin/*"}, AdminRouterServlet.class.getAnnotation(WebServlet.class).urlPatterns());
        assertArrayEquals(new String[]{"/admin/api/*"}, AdminApiServlet.class.getAnnotation(WebServlet.class).urlPatterns());
    }

    @Test
    void legacyDashboardAliasRedirectsToCanonicalDashboard() throws Exception {
        AtomicReference<String> redirect = new AtomicReference<>();
        HttpServletRequest request = proxy(HttpServletRequest.class, (proxy, method, args) -> switch (method.getName()) {
            case "getPathInfo" -> "/dashboard";
            case "getContextPath" -> "/market";
            default -> defaultValue(method.getReturnType());
        });
        HttpServletResponse response = proxy(HttpServletResponse.class, (proxy, method, args) -> {
            if ("sendRedirect".equals(method.getName())) redirect.set((String) args[0]);
            return defaultValue(method.getReturnType());
        });

        new AdminRouterServlet().doGet(request, response);

        assertEquals("/market/admin", redirect.get());
    }

    @Test
    void reloadableAdminModulesForwardToTheSameShell() throws Exception {
        AtomicReference<String> forwarded = new AtomicReference<>();
        RequestDispatcher dispatcher = proxy(RequestDispatcher.class, (proxy, method, args) -> {
            if ("forward".equals(method.getName())) forwarded.set("shell");
            return defaultValue(method.getReturnType());
        });
        HttpServletRequest request = proxy(HttpServletRequest.class, (proxy, method, args) ->
                "getPathInfo".equals(method.getName()) ? "/users" : "getRequestDispatcher".equals(method.getName())
                        ? dispatcher : defaultValue(method.getReturnType()));
        HttpServletResponse response = proxy(HttpServletResponse.class, (proxy, method, args) -> defaultValue(method.getReturnType()));

        new AdminRouterServlet().doGet(request, response);

        assertEquals("shell", forwarded.get());
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
