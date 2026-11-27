package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.integration;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.DeliveryAutomationDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.Json;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/** Public GHN callback endpoint. GHN custom-header token authenticates the callback. */
@WebServlet(name = "ghnWebhookServlet", urlPatterns = "/webhooks/ghn")
public final class GhnWebhookServlet extends HttpServlet {
    private static final int MAX_BODY_BYTES = 64 * 1024;
    private static final String SECRET_HEADER = "X-PrintKraft-GHN-Webhook-Token";
    private static final System.Logger LOG = System.getLogger(GhnWebhookServlet.class.getName());

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");

        String expectedSecret = setting("GHN_WEBHOOK_SECRET");
        String actualSecret = request.getHeader(SECRET_HEADER);
        if (expectedSecret.isBlank()) {
            send(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    Map.of("received", false, "message", "GHN webhook chưa được cấu hình bí mật xác thực."));
            return;
        }
        if (!constantTimeEquals(expectedSecret, actualSecret)) {
            send(response, HttpServletResponse.SC_UNAUTHORIZED, Map.of("received", false));
            return;
        }

        String expectedShopId = setting("GHN_SHOP_ID");
        if (expectedShopId.isBlank()) {
            send(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    Map.of("received", false, "message", "Thiếu cấu hình GHN_SHOP_ID."));
            return;
        }

        byte[] body = request.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
        if (body.length == 0 || body.length > MAX_BODY_BYTES) {
            send(response, HttpServletResponse.SC_BAD_REQUEST,
                    Map.of("received", false, "message", "Nội dung webhook trống hoặc vượt quá 64 KB."));
            return;
        }

        final JsonObject payload;
        try {
            payload = JsonParser.parseString(new String(body, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (RuntimeException error) {
            send(response, HttpServletResponse.SC_BAD_REQUEST,
                    Map.of("received", false, "message", "Webhook GHN không phải JSON object hợp lệ."));
            return;
        }

        String shopId = text(payload, "ShopID");
        if (!expectedShopId.equals(shopId)) {
            LOG.log(System.Logger.Level.WARNING, "Rejected GHN webhook with unexpected ShopID.");
            send(response, HttpServletResponse.SC_FORBIDDEN, Map.of("received", false));
            return;
        }

        try {
            DeliveryAutomationDao.WebhookResult result = DeliveryAutomationDao.processGhnEvent(
                    text(payload, "OrderCode"), text(payload, "Type"), text(payload, "Status"),
                    text(payload, "Time"), text(payload, "Reason"), text(payload, "PodURL"),
                    new String(body, StandardCharsets.UTF_8));
            if (result == DeliveryAutomationDao.WebhookResult.UNMATCHED) {
                LOG.log(System.Logger.Level.WARNING, "GHN webhook received for an unlinked tracking code.");
            }
            // 2xx acknowledgement is required for GHN; duplicate and unmatched events are audited
            // and should not be retried indefinitely.
            send(response, HttpServletResponse.SC_OK,
                    Map.of("received", true, "result", result.name()));
        } catch (IllegalArgumentException error) {
            send(response, HttpServletResponse.SC_BAD_REQUEST,
                    Map.of("received", false, "message", safeMessage(error)));
        } catch (RuntimeException error) {
            LOG.log(System.Logger.Level.ERROR, "GHN webhook processing failed.", error);
            send(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    Map.of("received", false, "message", "Chưa xử lý được webhook. GHN có thể thử gửi lại."));
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    private static String text(JsonObject payload, String key) {
        if (payload == null || payload.get(key) == null || payload.get(key).isJsonNull()) return "";
        try { return payload.get(key).getAsString().trim(); }
        catch (RuntimeException ignored) { return ""; }
    }

    private static String setting(String name) {
        String value = System.getenv(name);
        return value == null ? "" : value.trim();
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        if (actual == null || actual.isBlank()) return false;
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                actual.trim().getBytes(StandardCharsets.UTF_8));
    }

    private static String safeMessage(IllegalArgumentException error) {
        String value = error.getMessage();
        return value == null || value.isBlank() ? "Webhook GHN không hợp lệ." : value;
    }

    private static void send(HttpServletResponse response, int status, Object data) throws IOException {
        response.setStatus(status);
        response.getWriter().write(Json.stringify(data));
    }
}
