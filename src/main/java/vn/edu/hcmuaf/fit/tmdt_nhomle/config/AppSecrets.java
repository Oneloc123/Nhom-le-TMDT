package vn.edu.hcmuaf.fit.tmdt_nhomle.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Loads optional local credentials without making them a build requirement. */
public final class AppSecrets {
    private static final Properties CONFIG = loadConfig();

    private AppSecrets() { }

    public static final String GOOGLE_CLIENT_ID = value("google.clientId", "");
    public static final String GOOGLE_CLIENT_SECRET = value("google.clientSecret", "");
    public static final String GOOGLE_REDIRECT_URI = nonBlank("google.redirectUri", "http://localhost:8080/login/google/callback");

    public static final String FACEBOOK_APP_ID = value("facebook.appId", "");
    public static final String FACEBOOK_APP_SECRET = value("facebook.appSecret", "");
    public static final String FACEBOOK_REDIRECT_URI = nonBlank("facebook.redirectUri", "http://localhost:8080/login/facebook/callback");
    public static final String FACEBOOK_GRAPH_VERSION = nonBlank("facebook.graphVersion", "v26.0");

    public static final String MAIL_HOST = nonBlank("mail.smtp.host", "smtp.gmail.com");
    public static final int MAIL_PORT = integer("mail.smtp.port", 587);
    public static final boolean MAIL_STARTTLS = bool("mail.smtp.starttls", true);
    public static final boolean MAIL_SSL_ENABLE = bool("mail.smtp.ssl", false);
    public static final String MAIL_USERNAME = value("mail.smtp.username", "");
    public static final String MAIL_APP_PASSWORD = value("mail.smtp.appPassword", "");
    public static final String MAIL_FROM_NAME = nonBlank("mail.fromName", "PrintKraft 3D");
    public static final String MAIL_FROM = nonBlank("mail.from", MAIL_USERNAME);

    /** Used only when SMTP is not configured; default demo code is 111111. */
    public static final String DEMO_OTP = nonBlank("auth.demoOtp", "111111");

    public static boolean googleConfigured() {
        return notBlank(GOOGLE_CLIENT_ID) && notBlank(GOOGLE_CLIENT_SECRET);
    }

    public static boolean facebookConfigured() {
        return notBlank(FACEBOOK_APP_ID) && notBlank(FACEBOOK_APP_SECRET);
    }

    public static boolean mailConfigured() {
        return notBlank(MAIL_USERNAME) && notBlank(MAIL_APP_PASSWORD);
    }

    private static Properties loadConfig() {
        Properties properties = new Properties();
        String explicit = System.getProperty("printkraft.config");
        if (explicit == null || explicit.isBlank()) explicit = System.getenv("PRINTKRAFT_CONFIG");
        if (explicit != null && !explicit.isBlank()) {
            loadPath(properties, Path.of(explicit.trim()));
            return properties;
        }

        for (String candidate : new String[]{
                "integrations.local.properties",
                "config/integrations.local.properties",
                "resources/integrations.properties",
                "src/main/resources/integrations.properties"
        }) {
            Path path = Path.of(candidate);
            if (Files.isRegularFile(path)) {
                loadPath(properties, path);
                return properties;
            }
        }

        try (InputStream input = AppSecrets.class.getClassLoader().getResourceAsStream("integrations.properties")) {
            if (input != null) properties.load(input);
        } catch (IOException ignored) {
            // Optional configuration: continue with safe defaults and demo OTP.
        }
        return properties;
    }

    private static void loadPath(Properties properties, Path path) {
        if (!Files.isRegularFile(path)) return;
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
        } catch (IOException ignored) {
            // Missing/unreadable local credentials must not prevent application startup.
        }
    }

    private static String value(String key, String fallback) {
        String value = CONFIG.getProperty(key);
        return value == null ? fallback : value.trim();
    }

    private static String nonBlank(String key, String fallback) {
        String value = value(key, fallback);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static int integer(String key, int fallback) {
        try { return Integer.parseInt(value(key, Integer.toString(fallback))); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private static boolean bool(String key, boolean fallback) {
        String value = CONFIG.getProperty(key);
        return value == null ? fallback : Boolean.parseBoolean(value.trim());
    }

    private static boolean notBlank(String value) { return value != null && !value.isBlank(); }
}
