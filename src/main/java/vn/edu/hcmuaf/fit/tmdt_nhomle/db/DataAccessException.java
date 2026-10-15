package vn.edu.hcmuaf.fit.tmdt_nhomle.db;

/**
 * Lỗi truy cập dữ liệu ở mức dịch vụ — thông báo đã được kiểm soát
 * để không lộ chi tiết nội bộ (SQL, mật khẩu, stack trace) cho người dùng.
 * Nguyên nhân gốc vẫn được giữ để ghi log server.
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Bọc một SQLException bằng thông báo an toàn, giữ nguyên nguyên nhân để log. */
    public static DataAccessException of(java.sql.SQLException e) {
        return new DataAccessException(
                "Không thể truy cập dữ liệu. Vui lòng thử lại sau.", e);
    }
}
