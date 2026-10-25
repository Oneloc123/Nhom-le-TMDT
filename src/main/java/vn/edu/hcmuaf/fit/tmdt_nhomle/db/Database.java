package vn.edu.hcmuaf.fit.tmdt_nhomle.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class Database {

    private static final int POOL_SIZE = 10;
    private static final long BORROW_TIMEOUT_SECONDS = 10;

    private static final BlockingQueue<PooledConnection> POOL = new ArrayBlockingQueue<>(POOL_SIZE);
    private static final AtomicInteger OPEN_CONNECTIONS = new AtomicInteger();
    private static volatile boolean driverLoaded;

    private Database() {
    }

    public static Connection getConnection() throws SQLException {
        ensureDriverLoaded();
        PooledConnection pooled = acquire();
        return pooled.connection();
    }

    private static PooledConnection acquire() throws SQLException {
        PooledConnection pooled = POOL.poll();
        if (pooled != null && !isUsable(pooled)) {
            discard(pooled);
            pooled = null;
        }
        if (pooled == null) {
            if (OPEN_CONNECTIONS.get() < POOL_SIZE) {
                pooled = openNew();
            } else {
                try {
                    pooled = POOL.poll(BORROW_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("Đã hủy chờ kết nối cơ sở dữ liệu.", e);
                }
                if (pooled == null || !isUsable(pooled)) {
                    if (pooled != null) {
                        discard(pooled);
                    }
                    throw new SQLException("Hết thời gian chờ kết nối cơ sở dữ liệu.");
                }
            }
        }
        return pooled;
    }

    static void release(PooledConnection pooled) {
        try {
            Connection raw = pooled.raw();
            if (!raw.getAutoCommit()) {
                raw.rollback();
                raw.setAutoCommit(true);
            }
        } catch (SQLException e) {
            discard(pooled);
            return;
        }
        if (pooled.isDead() || !POOL.offer(pooled)) {
            discard(pooled);
        }
    }

    private static PooledConnection openNew() throws SQLException {
        DbConfig cfg = DbConfig.get();
        try {
            Connection raw = DriverManager.getConnection(cfg.jdbcUrl(), cfg.user(), cfg.password());
            OPEN_CONNECTIONS.incrementAndGet();
            return PooledConnection.wrap(raw);
        } catch (SQLException e) {
            // Log an toàn: không kèm mật khẩu.
            System.getLogger(Database.class.getName()).log(System.Logger.Level.ERROR,
                    "Không kết nối được cơ sở dữ liệu " + cfg.describe()
                            + " (sqlState=" + e.getSQLState() + ", vendorCode=" + e.getErrorCode() + ")"
                            + ". Kiểm tra MySQL đã chạy, tên database, user/mật khẩu qua "
                            + "biến môi trường PK_DB_* hoặc file trỏ bởi PK_DB_CONFIG. "
                            + "Không đặt mật khẩu trong source (xem db.properties.example). "
                            + "Chi tiết lỗi: " + e.getMessage(), e);
            throw e;
        }
    }

    private static void discard(PooledConnection pooled) {
        OPEN_CONNECTIONS.decrementAndGet();
        try {
            pooled.closeRaw();
        } catch (SQLException ignored) {
            // đã đóng
        }
    }

    private static boolean isUsable(PooledConnection pooled) {
        try {
            return !pooled.isDead() && !pooled.raw().isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    private static void ensureDriverLoaded() throws SQLException {
        if (driverLoaded) {
            return;
        }
        synchronized (Database.class) {
            if (!driverLoaded) {
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                } catch (ClassNotFoundException e) {
                    throw new SQLException(
                            "Thiếu driver MySQL trong classpath (mysql-connector-j).", e);
                }
                driverLoaded = true;
            }
        }
    }
}
