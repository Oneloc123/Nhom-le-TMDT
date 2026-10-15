package vn.edu.hcmuaf.fit.tmdt_nhomle.db;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

/**
 * Cấu hình kết nối MySQL, đọc theo thứ tự ưu tiên:
 *   1. Biến môi trường PK_DB_HOST / PK_DB_PORT / PK_DB_NAME / PK_DB_USER / PK_DB_PASSWORD
 *   2. File cấu hình ngoài (ngoài repo): chỉ định bằng PK_DB_CONFIG=/duong/dan/db.properties
 *   3. File classpath db.properties (xem db.properties.example) — chỉ chứa host/db/user,
 *      KHÔNG chứa mật khẩu mặc định.
 *   4. Giá trị mặc định an toàn cho môi trường dev cục bộ (localhost:3306/printkraft, user root).
 *
 * Không bao giờ ghi mật khẩu ra log hoặc trả về cho người dùng cuối.
 */
public final class DbConfig {

    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "3306";
    private static final String DEFAULT_NAME = "printkraft";
    private static final String DEFAULT_USER = "root";

    private final String host;
    private final int port;
    private final String database;
    private final String user;
    private final String password;

    private static volatile DbConfig instance;

    private DbConfig() {
        Properties fileProps = loadClasspathProperties();
        String external = System.getenv("PK_DB_CONFIG");
        if (external != null && !external.isBlank()) {
            Properties externalProps = loadFileProperties(Path.of(external.trim()));
            if (externalProps != null) {
                fileProps.putAll(externalProps);
            }
        }

        this.host = envOrProp("PK_DB_HOST", "db.host", fileProps, DEFAULT_HOST);
        this.port = parseIntSafe(envOrProp("PK_DB_PORT", "db.port", fileProps, DEFAULT_PORT), 3306);
        this.database = envOrProp("PK_DB_NAME", "db.name", fileProps, DEFAULT_NAME);
        this.user = envOrProp("PK_DB_USER", "db.user", fileProps, DEFAULT_USER);
        // Mật khẩu KHÔNG có giá trị mặc định trong mã nguồn.
        this.password = envOrProp("PK_DB_PASSWORD", "db.password", fileProps, "");
    }

    public static DbConfig get() {
        if (instance == null) {
            synchronized (DbConfig.class) {
                if (instance == null) {
                    instance = new DbConfig();
                }
            }
        }
        return instance;
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    public String database() {
        return database;
    }

    public String user() {
        return user;
    }

    public String password() {
        return password;
    }

    /** jdbc:mysql://host:port/db?params — không chứa thông tin xác thực. */
    public String jdbcUrl() {
        return "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useUnicode=true&characterEncoding=UTF-8&connectionTimeZone=UTC"
                + "&serverTimezone=UTC&allowMultiQueries=false&sslMode=PREFERRED";
    }

    /** Mô tả an toàn (không có mật khẩu) dùng cho thông báo lỗi. */
    public String describe() {
        return database + "@" + host + ":" + port + " (user=" + user + ")";
    }

    private static String envOrProp(String envKey, String propKey, Properties props, String defaultValue) {
        String env = System.getenv(envKey);
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        String prop = props.getProperty(propKey);
        if (prop != null && !prop.isBlank()) {
            return prop.trim();
        }
        return defaultValue;
    }

    private static Properties loadClasspathProperties() {
        Properties props = new Properties();
        try (InputStream in = DbConfig.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            System.getLogger(DbConfig.class.getName())
                    .log(System.Logger.Level.WARNING, "Không đọc được db.properties từ classpath.", e);
        }
        return props;
    }

    private static Properties loadFileProperties(Path path) {
        Properties props = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            props.load(reader);
        } catch (IOException e) {
            System.getLogger(DbConfig.class.getName())
                    .log(System.Logger.Level.WARNING, "Không đọc được file cấu hình DB: " + path, e);
        }
        return props;
    }

    private static int parseIntSafe(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
