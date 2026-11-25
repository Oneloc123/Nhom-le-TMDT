package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import vn.edu.hcmuaf.fit.tmdt_nhomle.model.CustomPrintRequest;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.QuoteRequest;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.Json;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Tích hợp ZaloPay OpenAPI v2 (tạo đơn, truy vấn trạng thái, xác minh callback & redirect). */
public final class ZaloPayService {

    @FunctionalInterface
    public interface Transport {
        String postForm(String url, Map<String, String> form) throws IOException, InterruptedException;
    }

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter ZALOPAY_DATE = DateTimeFormatter.ofPattern("yyMMdd");
    private static final HttpClient DEFAULT_HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ZaloPayConfig config;
    private final Clock clock;
    private final Transport transport;
    private final SecureRandom random = new SecureRandom();

    public ZaloPayService(ZaloPayConfig config) {
        this(config, Clock.systemUTC(), defaultTransport());
    }

    public ZaloPayService(ZaloPayConfig config, Clock clock, Transport transport) {
        this.config = config;
        this.clock = clock;
        this.transport = transport;
    }

    public CreateOrderResult createQuoteDepositOrder(QuoteRequest quote, long userId, BigDecimal depositAmount,
                                                     String redirectUrl, String callbackUrl) {
        if (quote == null || depositAmount == null || depositAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Yêu cầu báo giá hoặc số tiền đặt cọc không hợp lệ.");
        }
        String itemName = quote.getProductName() == null
                ? "Dat coc bao gia #" + quote.getQuoteRequestId()
                : quote.getProductName();
        String description = "PrintKraft - Dat coc 30% bao gia #" + quote.getQuoteRequestId();
        return createDepositOrder("Q" + quote.getQuoteRequestId(), userId, depositAmount,
                "quote_" + quote.getQuoteRequestId(), itemName, description, redirectUrl, callbackUrl);
    }

    public CreateOrderResult createCustomPrintDepositOrder(CustomPrintRequest request, long userId,
                                                           BigDecimal depositAmount,
                                                           String redirectUrl, String callbackUrl) {
        if (request == null || depositAmount == null || depositAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Yêu cầu in 3D hoặc số tiền đặt cọc không hợp lệ.");
        }
        String label = request.requestCode() == null || request.requestCode().isBlank()
                ? "#" + request.id()
                : request.requestCode();
        String itemName = request.originalFilename() == null
                ? "Dat coc in 3D " + label
                : "In 3D: " + request.originalFilename();
        String description = "PrintKraft - Dat coc 30% yeu cau in 3D " + label;
        return createDepositOrder("CP" + request.id(), userId, depositAmount,
                "cp_" + request.id(), itemName, description, redirectUrl, callbackUrl);
    }

    private CreateOrderResult createDepositOrder(String refTag, long userId, BigDecimal depositAmount,
                                                 String itemId, String itemName, String description,
                                                 String redirectUrl, String callbackUrl) {
        config.requireConfigured();
        long amountVnd = depositAmount.longValueExact();
        Instant now = clock.instant();
        long appTimeMs = now.toEpochMilli();
        String datePrefix = ZALOPAY_DATE.format(now.atZone(VIETNAM_ZONE));
        String suffix = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        String appTransId = datePrefix + "_" + refTag + "_" + (appTimeMs % 1_000_000_000L) + suffix;
        if (appTransId.length() > 40) {
            appTransId = appTransId.substring(0, 40);
        }

        String appUser = "pk_user_" + userId;
        Map<String, Object> embedMap = new LinkedHashMap<>();
        embedMap.put("redirecturl", redirectUrl);
        String embedData = Json.stringify(embedMap);

        Map<String, Object> itemMap = new LinkedHashMap<>();
        itemMap.put("itemid", itemId);
        itemMap.put("itemname", itemName);
        itemMap.put("itemprice", amountVnd);
        itemMap.put("itemquantity", 1);
        String itemJson = Json.stringify(List.of(itemMap));

        String hmacInput = config.appId() + "|" + appTransId + "|" + appUser + "|"
                + amountVnd + "|" + appTimeMs + "|" + embedData + "|" + itemJson;
        String mac = hmacSha256(config.key1(), hmacInput);

        Map<String, String> form = new LinkedHashMap<>();
        form.put("app_id", config.appId());
        form.put("app_user", appUser);
        form.put("app_time", String.valueOf(appTimeMs));
        form.put("amount", String.valueOf(amountVnd));
        form.put("app_trans_id", appTransId);
        form.put("embed_data", embedData);
        form.put("item", itemJson);
        form.put("description", description);
        form.put("bank_code", "");
        if (callbackUrl != null && !callbackUrl.isBlank()) {
            form.put("callback_url", callbackUrl);
        }
        form.put("mac", mac);

        String responseBody;
        try {
            responseBody = transport.postForm(config.createUrl(), form);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PaymentException("Kết nối ZaloPay Sandbox bị gián đoạn.");
        } catch (IOException e) {
            throw new PaymentException("Không thể kết nối cổng thanh toán ZaloPay Sandbox: " + e.getMessage());
        }

        Map<String, Object> parsed = Json.parseObject(responseBody);
        int returnCode = intField(parsed, "return_code", -1);
        int subReturnCode = intField(parsed, "sub_return_code", 0);
        String returnMessage = stringField(parsed, "return_message");
        String subReturnMessage = stringField(parsed, "sub_return_message");
        String orderUrl = stringField(parsed, "order_url");
        String zpTransToken = stringField(parsed, "zp_trans_token");

        if (returnCode != 1 || orderUrl == null || orderUrl.isBlank()) {
            String detail = (subReturnMessage != null && !subReturnMessage.isBlank())
                    ? subReturnMessage
                    : (returnMessage != null && !returnMessage.isBlank() ? returnMessage : "Mã lỗi " + returnCode);
            throw new PaymentException("ZaloPay từ chối khởi tạo giao dịch: " + detail
                    + " (code=" + returnCode + ", sub=" + subReturnCode + ").");
        }
        return new CreateOrderResult(appTransId, orderUrl, zpTransToken, depositAmount);
    }

    public QueryOrderResult queryOrder(String appTransId) {
        config.requireConfigured();
        if (appTransId == null || appTransId.isBlank()) {
            throw new IllegalArgumentException("Thiếu mã giao dịch app_trans_id.");
        }
        String hmacInput = config.appId() + "|" + appTransId + "|" + config.key1();
        String mac = hmacSha256(config.key1(), hmacInput);

        Map<String, String> form = new LinkedHashMap<>();
        form.put("app_id", config.appId());
        form.put("app_trans_id", appTransId);
        form.put("mac", mac);

        String responseBody;
        try {
            responseBody = transport.postForm(config.queryUrl(), form);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PaymentException("Truy vấn trạng thái ZaloPay bị gián đoạn.");
        } catch (IOException e) {
            throw new PaymentException("Không thể truy vấn trạng thái ZaloPay: " + e.getMessage());
        }

        Map<String, Object> parsed = Json.parseObject(responseBody);
        int returnCode = intField(parsed, "return_code", -1);
        int subReturnCode = intField(parsed, "sub_return_code", 0);
        String returnMessage = stringField(parsed, "return_message");
        boolean isProcessing = boolField(parsed, "is_processing");
        long amount = longField(parsed, "amount", 0L);
        String zpTransId = stringField(parsed, "zp_trans_id");
        return new QueryOrderResult(returnCode, subReturnCode, returnMessage, isProcessing,
                BigDecimal.valueOf(amount), zpTransId);
    }

    public CallbackData parseCallback(String rawJsonBody) {
        config.requireConfigured();
        Map<String, Object> envelope = Json.parseObject(rawJsonBody);
        String dataStr = stringField(envelope, "data");
        String mac = stringField(envelope, "mac");
        if (dataStr == null || mac == null) {
            return new CallbackData(false, null, null, null, null, rawJsonBody);
        }
        String expectedMac = hmacSha256(config.key2(), dataStr);
        boolean validMac = constantTimeEqualsHex(expectedMac, mac);
        if (!validMac) {
            return new CallbackData(false, null, null, null, null, rawJsonBody);
        }
        Map<String, Object> data = Json.parseObject(dataStr);
        String appTransId = stringField(data, "app_trans_id");
        String zpTransId = stringField(data, "zp_trans_id");
        long amountLong = longField(data, "amount", -1L);
        Integer channel = data.get("channel") instanceof Number n ? n.intValue() : null;
        BigDecimal amount = amountLong > 0 ? BigDecimal.valueOf(amountLong) : null;
        return new CallbackData(true, appTransId, zpTransId, amount, channel, rawJsonBody);
    }

    public boolean verifyRedirectChecksum(Map<String, String[]> requestParameters) {
        String appId = param(requestParameters, "appid");
        String appTransId = param(requestParameters, "apptransid");
        String pmcId = param(requestParameters, "pmcid");
        String bankCode = param(requestParameters, "bankcode");
        String amount = param(requestParameters, "amount");
        String discountAmount = param(requestParameters, "discountamount");
        String status = param(requestParameters, "status");
        String checksum = param(requestParameters, "checksum");
        if (appId == null || appTransId == null || checksum == null || !config.appId().equals(appId)) {
            return false;
        }
        String input = appId + "|" + appTransId + "|"
                + nonNull(pmcId) + "|" + nonNull(bankCode) + "|"
                + nonNull(amount) + "|" + nonNull(discountAmount) + "|" + nonNull(status);
        String expected = hmacSha256(config.key2(), input);
        return constantTimeEqualsHex(expected, checksum);
    }

    public static String hmacSha256(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(String.format(Locale.ROOT, "%02x", b));
            }
            return hex.toString();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("JVM không hỗ trợ HMAC-SHA256.", e);
        }
    }

    private static boolean constantTimeEqualsHex(String expected, String actual) {
        if (expected == null || actual == null) return false;
        return MessageDigest.isEqual(
                expected.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII),
                actual.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
    }

    private static Transport defaultTransport() {
        return (url, form) -> {
            StringBuilder body = new StringBuilder();
            for (var entry : form.entrySet()) {
                if (!body.isEmpty()) body.append('&');
                body.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                        .append('=')
                        .append(URLEncoder.encode(entry.getValue() == null ? "" : entry.getValue(),
                                StandardCharsets.UTF_8));
            }
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(15))
                    .build();
            HttpResponse<String> response = DEFAULT_HTTP.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
            }
            return response.body();
        };
    }

    private static String param(Map<String, String[]> map, String key) {
        if (map == null) return null;
        String[] arr = map.get(key);
        return (arr != null && arr.length > 0) ? arr[0] : null;
    }

    private static String nonNull(String s) {
        return s == null ? "" : s;
    }

    private static String stringField(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val == null ? null : String.valueOf(val);
    }

    private static int intField(Map<String, Object> map, String key, int fallback) {
        Object val = map.get(key);
        if (val instanceof Number n) return n.intValue();
        if (val != null) {
            try { return Integer.parseInt(val.toString()); } catch (NumberFormatException ignored) { }
        }
        return fallback;
    }

    private static long longField(Map<String, Object> map, String key, long fallback) {
        Object val = map.get(key);
        if (val instanceof Number n) return n.longValue();
        if (val != null) {
            try { return Long.parseLong(val.toString()); } catch (NumberFormatException ignored) { }
        }
        return fallback;
    }

    private static boolean boolField(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Boolean b) return b;
        return val != null && Boolean.parseBoolean(val.toString());
    }

    public record CreateOrderResult(String appTransId, String orderUrl, String zpTransToken,
                                    BigDecimal amount) { }

    public record QueryOrderResult(int returnCode, int subReturnCode, String returnMessage,
                                   boolean isProcessing, BigDecimal amount, String zpTransId) {
        public boolean paid() {
            return returnCode == 1;
        }
    }

    public record CallbackData(boolean validMac, String appTransId, String zpTransId,
                               BigDecimal amount, Integer channel, String rawBody) { }
}
