package vn.edu.hcmuaf.fit.tmdt_nhomle.db;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Wrapper cho một kết nối trong pool.
 * Người dùng nhận về một {@code Connection} proxy: {@code close()} trả kết nối
 * về pool thay vì đóng thật; nếu phát sinh lỗi kết nối (SQLState class 08)
 * kết nối bị đánh dấu hỏng và sẽ bị đóng thật thay vì tái sử dụng.
 */
final class PooledConnection implements InvocationHandler {

    private final Connection raw;
    private final Connection proxy;
    private volatile boolean dead;

    private PooledConnection(Connection raw) {
        this.raw = raw;
        this.proxy = (Connection) java.lang.reflect.Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                this);
    }

    static PooledConnection wrap(Connection raw) {
        return new PooledConnection(raw);
    }

    Connection connection() {
        return proxy;
    }

    Connection raw() {
        return raw;
    }

    boolean isDead() {
        return dead;
    }

    void markDead() {
        dead = true;
    }

    void closeRaw() throws SQLException {
        raw.close();
    }

    @Override
    public Object invoke(Object proxyInstance, Method method, Object[] args) throws Throwable {
        switch (method.getName()) {
            case "close" -> {
                Database.release(this);
                return null;
            }
            case "isClosed" -> {
                return dead || raw.isClosed();
            }
            case "equals" -> {
                return proxyInstance == args[0];
            }
            case "hashCode" -> {
                return System.identityHashCode(proxyInstance);
            }
            case "toString" -> {
                return "PooledConnection[" + DbConfig.get().describe() + "]";
            }
            case "unwrap" -> {
                @SuppressWarnings("unchecked")
                Class<?> iface = (Class<?>) args[0];
                if (iface.isInstance(proxyInstance)) {
                    return proxyInstance;
                }
                return raw.unwrap(iface);
            }
            case "isWrapperFor" -> {
                @SuppressWarnings("unchecked")
                Class<?> iface = (Class<?>) args[0];
                return iface.isInstance(proxyInstance) || raw.isWrapperFor(iface);
            }
            default -> {
                try {
                    return method.invoke(raw, args);
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof SQLException sql && isConnectionException(sql)) {
                        dead = true;
                    }
                    throw cause;
                }
            }
        }
    }

    private static boolean isConnectionException(SQLException e) {
        String state = e.getSQLState();
        return state != null && state.startsWith("08");
    }
}
