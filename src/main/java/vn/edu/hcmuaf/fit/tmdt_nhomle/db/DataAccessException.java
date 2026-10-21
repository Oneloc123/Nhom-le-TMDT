package vn.edu.hcmuaf.fit.tmdt_nhomle.db;

public class DataAccessException extends RuntimeException {

    private static final System.Logger LOG = System.getLogger(DataAccessException.class.getName());

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Giữ thông báo thân thiện cho người dùng nhưng GHI NGUYÊN NHÂN ra server log
     * (SQLState, vendor code, cause) để không còn "che" lỗi bằng thông báo chung.
     */
    public static DataAccessException of(java.sql.SQLException e) {
        LOG.log(System.Logger.Level.ERROR,
                "Lỗi truy cập dữ liệu: sqlState=" + e.getSQLState()
                        + ", vendorCode=" + e.getErrorCode()
                        + ", cause=" + rootMessage(e), e);
        return new DataAccessException(
                "Không thể truy cập dữ liệu. Vui lòng thử lại sau.", e);
    }

    private static String rootMessage(Throwable e) {
        Throwable current = e;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null ? current.getClass().getSimpleName() : message;
    }
}
