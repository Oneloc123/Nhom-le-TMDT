package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Cấu hình VNPAY. Khóa bí mật chỉ được đọc ở server từ biến môi trường hoặc
 * Java system property, không đặt trong source hay gửi xuống trình duyệt.
 */
public final class VnPayConfig {

    public static final String DEFAULT_PAYMENT_URL =
            "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";

    private static volatile VnPayConfig instance;

    private final String tmnCode;
    private final String hashSecret;
    private final String paymentUrl;
    private final String returnUrl;

    private VnPayConfig() {
        this(read("PK_VNPAY_TMN_CODE", "pk.vnpay.tmnCode"),
                read("PK_VNPAY_HASH_SECRET", "pk.vnpay.hashSecret"),
                valueOrDefault(read("PK_VNPAY_PAYMENT_URL", "pk.vnpay.paymentUrl"),
                        DEFAULT_PAYMENT_URL),
                read("PK_VNPAY_RETURN_URL", "pk.vnpay.returnUrl"));
    }

    public VnPayConfig(String tmnCode, String hashSecret, String paymentUrl, String returnUrl) {
        this.tmnCode = trim(tmnCode);
        this.hashSecret = trim(hashSecret);
        this.paymentUrl = valueOrDefault(trim(paymentUrl), DEFAULT_PAYMENT_URL);
        this.returnUrl = trim(returnUrl);
    }

    public static VnPayConfig get() {
        if (instance == null) {
            synchronized (VnPayConfig.class) {
                if (instance == null) instance = new VnPayConfig();
            }
        }
        return instance;
    }

    public boolean isConfigured() {
        return !tmnCode.isBlank() && !hashSecret.isBlank();
    }

    public void requireConfigured() {
        if (!isConfigured()) {
            throw new PaymentException("VNPAY chưa được cấu hình. Hãy đặt PK_VNPAY_TMN_CODE "
                    + "và PK_VNPAY_HASH_SECRET rồi khởi động lại Tomcat.");
        }
    }

    public String resolveReturnUrl(HttpServletRequest request) {
        if (!returnUrl.isBlank()) return returnUrl;
        String scheme = request.getScheme();
        int port = request.getServerPort();
        boolean defaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443);
        return scheme + "://" + request.getServerName()
                + (defaultPort ? "" : ":" + port)
                + request.getContextPath() + "/vnpay/return";
    }

    public String tmnCode() {
        return tmnCode;
    }

    public String hashSecret() {
        return hashSecret;
    }

    public String paymentUrl() {
        return paymentUrl;
    }

    private static String read(String environmentName, String propertyName) {
        String value = System.getenv(environmentName);
        if (value == null || value.isBlank()) value = System.getProperty(propertyName);
        return trim(value);
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
