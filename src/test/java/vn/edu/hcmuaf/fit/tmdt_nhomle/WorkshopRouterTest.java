package vn.edu.hcmuaf.fit.tmdt_nhomle;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import vn.edu.hcmuaf.fit.tmdt_nhomle.controller.workshop.WorkshopApiServlet;
import vn.edu.hcmuaf.fit.tmdt_nhomle.controller.workshop.WorkshopApplicationServlet;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkshopRouterTest {
    @Test
    void workshopPageApiAndApplicationHaveSpecificMappings() {
        assertArrayEquals(new String[]{"/workshop/*"},
                WorkshopRouterServlet.class.getAnnotation(WebServlet.class).urlPatterns());
        assertArrayEquals(new String[]{"/workshop/api/*"},
                WorkshopApiServlet.class.getAnnotation(WebServlet.class).urlPatterns());
        assertArrayEquals(new String[]{"/workshop/apply"},
                WorkshopApplicationServlet.class.getAnnotation(WebServlet.class).urlPatterns());
    }

    @Test
    void directWorkshopPageReloadForwardsToSharedShell() throws Exception {
        AtomicReference<String> forwarded = new AtomicReference<>();
        RequestDispatcher dispatcher = proxy(RequestDispatcher.class, (proxy, method, args) -> {
            if ("forward".equals(method.getName())) forwarded.set("shell");
            return defaultValue(method.getReturnType());
        });
        HttpServletRequest request = proxy(HttpServletRequest.class, (proxy, method, args) -> switch (method.getName()) {
            case "getPathInfo" -> "/orders";
            case "getRequestDispatcher" -> dispatcher;
            default -> defaultValue(method.getReturnType());
        });
        HttpServletResponse response = proxy(HttpServletResponse.class,
                (proxy, method, args) -> defaultValue(method.getReturnType()));

        new WorkshopRouterServlet().doGet(request, response);

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
