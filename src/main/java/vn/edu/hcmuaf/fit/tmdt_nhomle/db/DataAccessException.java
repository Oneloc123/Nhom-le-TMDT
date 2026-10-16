package vn.edu.hcmuaf.fit.tmdt_nhomle.db;

public class DataAccessException extends RuntimeException {

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public static DataAccessException of(java.sql.SQLException e) {
        return new DataAccessException(
                "Không thể truy cập dữ liệu. Vui lòng thử lại sau.", e);
    }
}
