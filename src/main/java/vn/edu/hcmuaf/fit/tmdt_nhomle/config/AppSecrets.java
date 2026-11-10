package vn.edu.hcmuaf.fit.tmdt_nhomle.config;

/**
 * Cấu hình tích hợp của PrintKraft.
 *
 * Điền thông tin thật trực tiếp vào các hằng bên dưới.
 * Class này không đọc integrations.local hoặc integrations.properties.
 * Không commit các thông tin bí mật này lên Git.
 */
public final class AppSecrets {
    private AppSecrets() { }

    // =========================
    // GOOGLE OAUTH 2.0
    // Google Cloud Console -> APIs & Services -> Credentials
    // =========================
    public static final String GOOGLE_CLIENT_ID = "";
    public static final String GOOGLE_CLIENT_SECRET = "";
    public static final String GOOGLE_REDIRECT_URI =
            "http://localhost:8080/login/google/callback";

    // =========================
    // FACEBOOK OAUTH
    // Meta for Developers -> App settings -> Basic
    // =========================
    public static final String FACEBOOK_APP_ID = "";
    public static final String FACEBOOK_APP_SECRET = "";
    public static final String FACEBOOK_REDIRECT_URI =
            "http://localhost:8080/login/facebook/callback";
    public static final String FACEBOOK_GRAPH_VERSION = "v26.0";

    // =========================
    // GMAIL SMTP / OTP EMAIL
    // MAIL_APP_PASSWORD cần là thông tin xác thực SMTP hợp lệ.
    // Để trống thì hệ thống hiện tại sẽ dùng OTP demo thay vì gửi email thật.
    // =========================
    public static final String MAIL_HOST = "smtp.gmail.com";
    public static final int MAIL_PORT = 587;
    public static final boolean MAIL_STARTTLS = true;
    public static final boolean MAIL_SSL_ENABLE = false;
    public static final String MAIL_USERNAME = "oneloc123@gmail.com";
    public static final String MAIL_APP_PASSWORD = "";
    public static final String MAIL_FROM_NAME = "PrintKraft 3D";
    public static final String MAIL_FROM = MAIL_USERNAME;

    // Chỉ dùng khi SMTP chưa được cấu hình.
    public static final String DEMO_OTP = "111111";

    public static boolean googleConfigured() {
        return notBlank(GOOGLE_CLIENT_ID) && notBlank(GOOGLE_CLIENT_SECRET);
    }

    public static boolean facebookConfigured() {
        return notBlank(FACEBOOK_APP_ID) && notBlank(FACEBOOK_APP_SECRET);
    }

    public static boolean mailConfigured() {
        return notBlank(MAIL_USERNAME) && notBlank(MAIL_APP_PASSWORD);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
