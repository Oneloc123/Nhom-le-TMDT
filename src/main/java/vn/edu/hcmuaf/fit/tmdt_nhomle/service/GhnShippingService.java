package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.Json;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GhnShippingService {

    private static final String PATH = "/shiip/public-api/v2/shipping-order/";
    private static final String STAGING = "https://dev-online-gateway.ghn.vn";
    private static final String PRODUCTION = "https://online-gateway.ghn.vn";
    private static final String UNAVAILABLE = "GHN chưa thể tính phí vận chuyển lúc này. Vui lòng thử lại.";
    private static final String UNSUPPORTED = "GHN chưa báo được giá cho địa chỉ này. Vui lòng kiểm tra địa chỉ hoặc chọn địa chỉ khác.";
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3)).build();

    BigDecimal quote(String name, String phone, String address, String province, String district, String ward) {
        return readFee(send("preview", parcel(name, phone, address, province, district, ward)));
    }

    public String createShipment(String orderCode, String name, String phone, String address,
                                 String province, String district, String ward, int codAmount) {
        Map<String, Object> data = parcel(name, phone, address, province, district, ward);
        data.put("client_order_code", orderCode);
        data.put("cod_amount", codAmount);
        // Checkout already includes the shipping charge. The carrier collects one total for COD.
        data.put("payment_type_id", 1);
        data.put("return_address", data.get("from_address"));
        data.put("return_ward_name", data.get("from_ward_name"));
        data.put("return_district_name", data.get("from_district_name"));
        data.put("return_province_name", data.get("from_province_name"));
        String code = requiredText(readData(send("create", data)), "order_code");
        if (!code.matches("[A-Za-z0-9-]{1,40}")) throw new ShippingUnavailable(UNAVAILABLE);
        return code;
    }

    public DeliveryStatus detail(String code) {
        JsonObject data = readData(send("detail?order_code=" + URLEncoder.encode(code, StandardCharsets.UTF_8), null));
        if (!code.equals(requiredText(data, "order_code"))) throw new ShippingUnavailable(UNAVAILABLE);
        String status = requiredText(data, "status");
        try {
            return new DeliveryStatus(status, Instant.parse(requiredText(data, "updated_date")).toEpochMilli());
        } catch (java.time.format.DateTimeParseException | ArithmeticException error) {
            throw new ShippingUnavailable(UNAVAILABLE);
        }
    }

    public record DeliveryStatus(String status, long version) { }

    private static Map<String, Object> parcel(String name, String phone, String address,
                                               String province, String district, String ward) {
        if (province == null || province.isBlank() || ward == null || ward.isBlank()) {
            throw new IllegalArgumentException("Địa chỉ cần có tỉnh/thành phố và phường/xã để tính phí GHN.");
        }
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
        return parcel;
    }

    private static String send(String endpoint, Map<String, Object> body) {
        String token = setting("GHN_TOKEN");
        String shopId = setting("GHN_SHOP_ID");
        if (token.isBlank() || !shopId.matches("[1-9][0-9]*")) throw new ShippingUnavailable(UNAVAILABLE);
        String host = switch (setting("GHN_ENV").toLowerCase(java.util.Locale.ROOT)) {
            case "", "staging" -> STAGING;
            case "production" -> PRODUCTION;
            default -> throw new ShippingUnavailable(UNAVAILABLE);
        };
        var request = HttpRequest.newBuilder(URI.create(host + PATH + endpoint))
                .timeout(Duration.ofSeconds(6))
                .header("Content-Type", "application/json")
                .header("Token", token)
                .header("ShopId", shopId);
        if (body != null) request.POST(HttpRequest.BodyPublishers.ofString(Json.stringify(body)));
        try {
            var response = CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 400 || response.statusCode() == 422) {
                throw new IllegalArgumentException("preview".equals(endpoint) ? UNSUPPORTED
                        : "GHN từ chối yêu cầu vận đơn. Kiểm tra cấu hình shop và thông tin giao nhận.");
            }
            if (response.statusCode() != 200) throw new ShippingUnavailable(UNAVAILABLE);
            return response.body();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new ShippingUnavailable(UNAVAILABLE);
        } catch (IOException error) {
            throw new ShippingUnavailable(UNAVAILABLE);
        }
    }

    static BigDecimal readFee(String body) {
        try {
            var total = readData(body).get("total_fee").getAsJsonPrimitive();
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

    private static JsonObject readData(String body) {
        try {
            JsonObject result = JsonParser.parseString(body).getAsJsonObject();
            if (result.get("code").getAsInt() == 200) return result.getAsJsonObject("data");
        } catch (com.google.gson.JsonParseException | IllegalStateException | NullPointerException |
                 ClassCastException | NumberFormatException ignored) {
        }
        throw new ShippingUnavailable(UNAVAILABLE);
    }

    private static String requiredText(JsonObject data, String key) {
        if (data != null && data.has(key) && data.get(key).isJsonPrimitive()) {
            String value = data.get(key).getAsString();
            if (!value.isBlank()) return value;
        }
        throw new ShippingUnavailable(UNAVAILABLE);
    }

    private static String setting(String name) {
        String value = System.getenv(name);
        return value == null ? "" : value.strip();
    }

    public static final class ShippingUnavailable extends RuntimeException {
        ShippingUnavailable(String message) {
            super(message);
        }
    }
}
