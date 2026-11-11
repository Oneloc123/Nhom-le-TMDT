package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.workshop;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkshopApiServletRoutingTest {
    @Test
    void unknownApiRouteReturnsNotFoundJson() throws Exception {
        ResponseCapture response = new ResponseCapture();

        new WorkshopApiServlet().doGet(
                request("/unknown", Map.of("shopId", "11")),
                response.proxy()
        );

        assertEquals(HttpServletResponse.SC_NOT_FOUND, response.status);
        assertTrue(response.body().contains("Workshop API không tồn tại."));
    }

    @Test
    void invalidShopIdReturnsBadRequestJson() throws Exception {
        ResponseCapture response = new ResponseCapture();

        new WorkshopApiServlet().doGet(
                request("/orders", Map.of("shopId", "not-a-number")),
                response.proxy()
        );

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.status);
        assertTrue(response.body().contains("Mã xưởng hoặc bản ghi không hợp lệ."));
    }

    @Test
    void unknownPostRouteReturnsNotFoundJson() throws Exception {
        ResponseCapture response = new ResponseCapture();

        new WorkshopApiServlet().doPost(
                request("/not-a-mutation", Map.of("shopId", "11")),
                response.proxy()
        );

        assertEquals(HttpServletResponse.SC_NOT_FOUND, response.status);
        assertTrue(response.body().contains("Workshop API không tồn tại."));
    }

    private static HttpServletRequest request(String pathInfo, Map<String, String> parameters) {
        HttpSession session = (HttpSession) Proxy.newProxyInstance(
                HttpSession.class.getClassLoader(),
                new Class<?>[]{HttpSession.class},
                (proxy, method, args) -> {
                    if ("getAttribute".equals(method.getName())) return null;
                    return defaultValue(method.getReturnType());
                }
        );

        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getPathInfo" -> pathInfo;
                    case "getParameter" -> parameters.get((String) args[0]);
                    case "getSession" -> session;
                    case "setCharacterEncoding" -> null;
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive() || type == void.class) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        return null;
    }

    private static final class ResponseCapture {
        private int status = HttpServletResponse.SC_OK;
        private final StringWriter output = new StringWriter();
        private final PrintWriter writer = new PrintWriter(output);

        HttpServletResponse proxy() {
            return (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(),
                    new Class<?>[]{HttpServletResponse.class},
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "setStatus" -> status = (int) args[0];
                            case "sendError" -> status = (int) args[0];
                            case "getWriter" -> { return writer; }
                            default -> { }
                        }
                        return defaultValue(method.getReturnType());
                    }
            );
        }

        String body() {
            writer.flush();
            return output.toString();
        }
    }
}
