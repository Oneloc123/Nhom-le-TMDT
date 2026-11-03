package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogoutServletTest {
    @Test
    void postInvalidatesSessionAndRedirectsToLogin() throws Exception {
        AtomicBoolean invalidated = new AtomicBoolean();
        AtomicReference<String> redirect = new AtomicReference<>();
        HttpSession session = proxy(HttpSession.class, (proxy, method, args) -> {
            if ("invalidate".equals(method.getName())) invalidated.set(true);
            return defaultValue(method.getReturnType());
        });
        HttpServletRequest request = proxy(HttpServletRequest.class, (proxy, method, args) -> switch (method.getName()) {
            case "getSession" -> session;
            case "getContextPath" -> "/printkraft";
            default -> defaultValue(method.getReturnType());
        });
        HttpServletResponse response = proxy(HttpServletResponse.class, (proxy, method, args) -> {
            if ("sendRedirect".equals(method.getName())) redirect.set((String) args[0]);
            return defaultValue(method.getReturnType());
        });

        new LogoutServlet().doPost(request, response);

        assertTrue(invalidated.get());
        assertEquals("/printkraft/login?loggedOut=1", redirect.get());
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
