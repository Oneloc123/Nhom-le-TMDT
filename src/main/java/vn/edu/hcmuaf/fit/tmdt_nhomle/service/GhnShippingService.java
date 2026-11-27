package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.Json;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GhnShippingService {

    private static final String PREVIEW_PATH = "/shiip/public-api/v2/shipping-order/preview";
    private static final String STAGING = "https://dev-online-gateway.ghn.vn";
    private static final String PRODUCTION = "https://online-gateway.ghn.vn";
    private static final String UNAVAILABLE = "GHN chưa thể tính phí vận chuyển lúc này. Vui lòng thử lại.";
    private static final String UNSUPPORTED = "GHN chưa báo được giá cho địa chỉ này. Vui lòng kiểm tra địa chỉ hoặc chọn địa chỉ khác.";
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3)).build();

    BigDecimal quote(String name, String phone, String address, String province, String district, String ward) {
        if (province == null || province.isBlank() || ward == null || ward.isBlank()) {
            throw new IllegalArgumentException("Địa chỉ cần có tỉnh/thành phố và phường/xã để tính phí GHN.");
        }
        String token = setting("GHN_TOKEN");
        String shopId = setting("GHN_SHOP_ID");
        if (token.isBlank() || !shopId.matches("[1-9][0-9]*")) {
            throw new ShippingUnavailable(UNAVAILABLE);
        }
        String environment = setting("GHN_ENV");
        String host = switch (environment.toLowerCase(java.util.Locale.ROOT)) {
            case "", "staging" -> STAGING;
            case "production" -> PRODUCTION;
            default -> throw new ShippingUnavailable(UNAVAILABLE);
        };
        boolean twoLevelAddress = district == null || district.isBlank();
        Map<String, Object> parcel = new LinkedHashMap<>();
        parcel.put("from_address", "Đại học Nông Lâm, Linh Xuân, Thủ Đức, TP. Hồ Chí Minh");
        parcel.put("from_province_name", "Hồ Chí Minh");
        parcel.put("from_district_name", "Thành Phố Thủ Đức");
        parcel.put("from_ward_name", "Phường Linh Xuân");
        parcel.put("to_name", name);
        parcel.put("to_phone", phone != null && phone.startsWith("+84") ? "0" + phone.substring(3) : phone);
        parcel.put("to_address", address);
        parcel.put("to_province_name", province);
        parcel.put("to_ward_name", ward);
        parcel.put("is_new_to_address", twoLevelAddress);
        if (!twoLevelAddress) parcel.put("to_district_name", district);
        parcel.put("content", "Đơn hàng PrintKraft");
        parcel.put("weight", 500);
        parcel.put("length", 20);
        parcel.put("width", 15);
        parcel.put("height", 10);
        parcel.put("service_type_id", 2);
        parcel.put("payment_type_id", 2);
        parcel.put("required_note", "KHONGCHOXEMHANG");

        HttpRequest request = HttpRequest.newBuilder(URI.create(host + PREVIEW_PATH))
                .timeout(Duration.ofSeconds(6))
                .header("Content-Type", "application/json")
                .header("Token", token)
                .header("ShopId", shopId)
                .POST(HttpRequest.BodyPublishers.ofString(Json.stringify(parcel))).build();
        try {
            var response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 400 || response.statusCode() == 422) {
                throw new IllegalArgumentException(UNSUPPORTED);
            }
            if (response.statusCode() != 200) throw new ShippingUnavailable(UNAVAILABLE);
            return readFee(response.body());
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new ShippingUnavailable(UNAVAILABLE);
        } catch (IOException error) {
            throw new ShippingUnavailable(UNAVAILABLE);
        }
    }

    /** Reads the latest GHN carrier status for webhook-reconciliation fallback polling. */
    public static GhnTrackingStatus fetchTrackingStatus(String orderCode) {
        String code = orderCode == null ? "" : orderCode.trim();
        if (code.isEmpty() || code.length() > 120) throw new IllegalArgumentException("Mã vận đơn GHN không hợp lệ.");
        String token = setting("GHN_TOKEN");
        String shopId = setting("GHN_SHOP_ID");
        if (token.isBlank() || !shopId.matches("[1-9][0-9]*")) {
            throw new ShippingUnavailable(UNAVAILABLE);
        }
        String environment = setting("GHN_ENV");
        String host = switch (environment.toLowerCase(java.util.Locale.ROOT)) {
            case "", "staging" -> STAGING;
            case "production" -> PRODUCTION;
            default -> throw new ShippingUnavailable(UNAVAILABLE);
        };
        String encoded = java.net.URLEncoder.encode(code, java.nio.charset.StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(host
                        + "/shiip/public-api/v2/shipping-order/detail?order_code=" + encoded))
                .timeout(Duration.ofSeconds(8))
                .header("Token", token)
                .header("ShopId", shopId)
                .GET().build();
        try {
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new ShippingUnavailable(UNAVAILABLE);
            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            if (root.get("code") == null || root.get("code").getAsInt() != 200
                    || root.get("data") == null || !root.get("data").isJsonObject()) {
                throw new ShippingUnavailable(UNAVAILABLE);
            }
            JsonObject data = root.getAsJsonObject("data");
            String returnedCode = jsonString(data, "order_code");
            String status = jsonString(data, "status");
            String updatedAt = jsonString(data, "updated_date");
            String reason = jsonString(data, "note");
            if (returnedCode.isBlank() || !code.equalsIgnoreCase(returnedCode) || status.isBlank()) {
                throw new ShippingUnavailable(UNAVAILABLE);
            }
            return new GhnTrackingStatus(returnedCode, status, updatedAt, reason);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new ShippingUnavailable(UNAVAILABLE);
        } catch (IOException | com.google.gson.JsonParseException | IllegalStateException error) {
            throw new ShippingUnavailable(UNAVAILABLE);
        }
    }

    private static String jsonString(JsonObject object, String key) {
        if (object == null || object.get(key) == null || object.get(key).isJsonNull()) return "";
        try { return object.get(key).getAsString().trim(); }
        catch (RuntimeException ignored) { return ""; }
    }

    public record GhnTrackingStatus(String orderCode, String status, String updatedAt, String reason) { }

    static BigDecimal readFee(String body) {
        try {
            JsonObject result = JsonParser.parseString(body).getAsJsonObject();
            if (result.get("code").getAsInt() != 200) throw new ShippingUnavailable(UNAVAILABLE);
            var total = result.getAsJsonObject("data").get("total_fee").getAsJsonPrimitive();
            if (total.isNumber()) {
                int amount = total.getAsBigDecimal().intValueExact();
                if (amount >= 0) return BigDecimal.valueOf(amount);
            }
        } catch (com.google.gson.JsonParseException | ArithmeticException | IllegalStateException |
                 NullPointerException | ClassCastException error) {
            throw new ShippingUnavailable(UNAVAILABLE);
        }
        throw new ShippingUnavailable(UNAVAILABLE);
    }

    private static String setting(String name) {
        String value = System.getenv(name);
        return value == null ? "" : value.strip();
    }

    static final class ShippingUnavailable extends RuntimeException {
        ShippingUnavailable(String message) {
            super(message);
        }
    }
}
