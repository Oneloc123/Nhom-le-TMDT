package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Cấu hình ZaloPay Sandbox v2. Đọc từ biến môi trường hoặc Java system property,
 * có fallback thông số Sandbox công khai của ZaloPay để kiểm thử trực tiếp trên localhost.
 */
public final class ZaloPayConfig {

    public static final String SANDBOX_APP_ID = "2554";
    public static final String SANDBOX_KEY1 = "sdngKKJmqEMzvh5QQcdD2A9XBSKUNaYn";
    public static final String SANDBOX_KEY2 = "trMrHtvjo6myautxDUiAcYsVtaeQ8nhf";
    public static final String DEFAULT_CREATE_URL = "https://sb-openapi.zalopay.vn/v2/create";
    public static final String DEFAULT_QUERY_URL = "https://sb-openapi.zalopay.vn/v2/query";

    private static volatile ZaloPayConfig instance;

    private final String appId;
    private final String key1;
    private final String key2;
    private final String createUrl;
    private final String queryUrl;
    private final String callbackUrl;
    private final String redirectUrl;

    private ZaloPayConfig() {
        this(
                valueOrDefault(read("PK_ZALOPAY_APP_ID", "pk.zalopay.appId"), SANDBOX_APP_ID),
                valueOrDefault(read("PK_ZALOPAY_KEY1", "pk.zalopay.key1"), SANDBOX_KEY1),
                valueOrDefault(read("PK_ZALOPAY_KEY2", "pk.zalopay.key2"), SANDBOX_KEY2),
                valueOrDefault(read("PK_ZALOPAY_CREATE_URL", "pk.zalopay.createUrl"), DEFAULT_CREATE_URL),
                valueOrDefault(read("PK_ZALOPAY_QUERY_URL", "pk.zalopay.queryUrl"), DEFAULT_QUERY_URL),
                read("PK_ZALOPAY_CALLBACK_URL", "pk.zalopay.callbackUrl"),
                read("PK_ZALOPAY_REDIRECT_URL", "pk.zalopay.redirectUrl")
        );
    }

    public ZaloPayConfig(String appId, String key1, String key2,
                         String createUrl, String queryUrl,
                         String callbackUrl, String redirectUrl) {
        this.appId = trim(appId);
        this.key1 = trim(key1);
        this.key2 = trim(key2);
        this.createUrl = valueOrDefault(trim(createUrl), DEFAULT_CREATE_URL);
        this.queryUrl = valueOrDefault(trim(queryUrl), DEFAULT_QUERY_URL);
        this.callbackUrl = trim(callbackUrl);
        this.redirectUrl = trim(redirectUrl);
    }

    public static ZaloPayConfig get() {
        if (instance == null) {
            synchronized (ZaloPayConfig.class) {
                if (instance == null) {
                    instance = new ZaloPayConfig();
                }
            }
        }
        return instance;
    }

    public boolean isConfigured() {
        return !appId.isBlank() && !key1.isBlank() && !key2.isBlank();
    }

    public void requireConfigured() {
        if (!isConfigured()) {
            throw new PaymentException("ZaloPay Sandbox chưa được cấu hình đầy đủ (app_id, key1, key2).");
        }
    }

    public String resolveRedirectUrl(HttpServletRequest request) {
        if (isLoopbackHost(request.getServerName())) {
            return requestUrl(request, "/zalopay/return");
        }
        if (!redirectUrl.isBlank()) {
            return redirectUrl;
        }
        return requestUrl(request, "/zalopay/return");
    }

    /**
     * Trả về callback_url gửi lên ZaloPay /v2/create. Trên localhost không gửi URL nội bộ
     * vì server ZaloPay không gọi được; khi deploy domain công khai hoặc cấu hình biến môi trường
     * thì tự động dùng URL callback công khai.
     */
    public String resolveCallbackUrl(HttpServletRequest request) {
        if (!callbackUrl.isBlank()) {
            return callbackUrl;
        }
        if (isLoopbackHost(request.getServerName())) {
            return "";
        }
        return requestUrl(request, "/zalopay/callback");
    }

    private static String requestUrl(HttpServletRequest request, String suffix) {
        String scheme = request.getScheme();
        int port = request.getServerPort();
        boolean defaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443);
        return scheme + "://" + request.getServerName()
                + (defaultPort ? "" : ":" + port)
                + request.getContextPath() + suffix;
    }

    private static boolean isLoopbackHost(String host) {
        if (host == null) return false;
        String value = host.trim();
        return "localhost".equalsIgnoreCase(value)
                || "127.0.0.1".equals(value)
                || "0:0:0:0:0:0:0:1".equals(value)
                || "::1".equals(value);
    }

    public String appId() { return appId; }
    public String key1() { return key1; }
    public String key2() { return key2; }
    public String createUrl() { return createUrl; }
    public String queryUrl() { return queryUrl; }
    public String callbackUrl() { return callbackUrl; }
    public String redirectUrl() { return redirectUrl; }

    private static String read(String envName, String propName) {
        String value = System.getenv(envName);
        if (value == null || value.isBlank()) {
            value = System.getProperty(propName);
        }
        return trim(value);
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
