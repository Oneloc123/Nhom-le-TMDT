package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.QuoteRequest;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class QuoteDao {

    private static final String SELECT_COLUMNS =
            "SELECT q.quote_request_id, q.user_id, q.product_id, q.preferred_shop_id,"
                    + " q.company_name, q.requested_quantity, q.required_by, q.quoted_amount,"
                    + " q.deposit_amount, q.deposit_paid_at, q.production_started_at,"
                    + " q.completed_at, q.production_note, q.order_id, o.order_code,"
                    + " q.seller_response_note, q.status,"
                    + " p.product_name, s.shop_name"
                    + " FROM quote_requests q"
                    + " LEFT JOIN products p ON p.product_id = q.product_id"
                    + " LEFT JOIN print_shops s ON s.shop_id = q.preferred_shop_id"
                    + " LEFT JOIN orders o ON o.order_id = q.order_id";

    private QuoteDao() {
    }

    public static List<QuoteRequest> listByUser(long userId) {
        List<QuoteRequest> list = new ArrayList<>();
        try (Connection conn = Database.getConnection()) {
            QuotePaymentDao.ensureSchema(conn);
            String sql = SELECT_COLUMNS + " WHERE q.user_id = ? ORDER BY q.quote_request_id DESC";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(map(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static Optional<QuoteRequest> findByIdForUser(long quoteRequestId, long userId) {
        try (Connection conn = Database.getConnection()) {
            QuotePaymentDao.ensureSchema(conn);
            String sql = SELECT_COLUMNS + " WHERE q.quote_request_id = ? AND q.user_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, quoteRequestId);
                ps.setLong(2, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(map(rs)) : Optional.empty();
                }
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static String lockStatusForUser(Connection conn, long quoteRequestId, long userId) throws SQLException {
        QuotePaymentDao.ensureSchema(conn);
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT status FROM quote_requests WHERE quote_request_id = ? AND user_id = ? FOR UPDATE")) {
            ps.setLong(1, quoteRequestId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("status") : null;
            }
        }
    }

    public static void markDeposited(Connection conn, long quoteRequestId, long userId,
                                     BigDecimal depositAmount) throws SQLException {
        QuotePaymentDao.ensureSchema(conn);
        String sql = "UPDATE quote_requests"
                + " SET status = ?, deposit_amount = ?, deposit_paid_at = NOW()"
                + " WHERE quote_request_id = ? AND user_id = ? AND status = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, QuoteRequest.STATUS_DEPOSITED);
            ps.setBigDecimal(2, depositAmount);
            ps.setLong(3, quoteRequestId);
            ps.setLong(4, userId);
            ps.setString(5, QuoteRequest.STATUS_QUOTED);
            if (ps.executeUpdate() != 1) {
                throw new IllegalArgumentException("Yêu cầu báo giá không còn ở trạng thái Đã báo giá.");
            }
        }
    }

    public static QuoteRequest insert(long userId, long productId, Long preferredShopId,
                                      String companyName, int quantity, Timestamp requiredBy) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Số lượng yêu cầu phải lớn hơn 0.");
        }
        String checkProduct = "SELECT COUNT(*) FROM products WHERE product_id = ?"
                + " AND is_active = 1 AND moderation_status = 'APPROVED'";
        try (Connection conn = Database.getConnection()) {
            QuotePaymentDao.ensureSchema(conn);
            try (PreparedStatement ps = conn.prepareStatement(checkProduct)) {
                ps.setLong(1, productId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || rs.getLong(1) == 0) {
                        throw new IllegalArgumentException("Sản phẩm không tồn tại hoặc ngừng bán.");
                    }
                }
            }
            if (preferredShopId != null) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT COUNT(*) FROM print_shops WHERE shop_id = ?"
                                + " AND verification_status = 'VERIFIED'"
                                + " AND (status IS NULL OR status NOT IN ('INACTIVE','BLOCKED'))")) {
                    ps.setLong(1, preferredShopId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next() || rs.getLong(1) == 0) {
                            throw new IllegalArgumentException("Gian hàng ưu tiên không tồn tại.");
                        }
                    }
                }
            }
            String sql = "INSERT INTO quote_requests (user_id, product_id, preferred_shop_id,"
                    + " company_name, requested_quantity, required_by, quoted_amount, status)"
                    + " VALUES (?, ?, ?, ?, ?, ?, NULL, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, userId);
                ps.setLong(2, productId);
                if (preferredShopId == null) {
                    ps.setNull(3, java.sql.Types.BIGINT);
                } else {
                    ps.setLong(3, preferredShopId);
                }
                ps.setString(4, companyName);
                ps.setInt(5, quantity);
                ps.setTimestamp(6, requiredBy);
                ps.setString(7, QuoteRequest.STATUS_PENDING);
                ps.executeUpdate();
                long id;
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    id = keys.next() ? keys.getLong(1) : -1;
                }
                return findByIdForUser(id, userId).orElseThrow();
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    private static QuoteRequest map(ResultSet rs) throws SQLException {
        QuoteRequest q = new QuoteRequest();
        q.setQuoteRequestId(rs.getLong("quote_request_id"));
        q.setUserId(rs.getLong("user_id"));
        long productId = rs.getLong("product_id");
        q.setProductId(rs.wasNull() ? null : productId);
        long shopId = rs.getLong("preferred_shop_id");
        q.setPreferredShopId(rs.wasNull() ? null : shopId);
        q.setCompanyName(rs.getString("company_name"));
        q.setRequestedQuantity(rs.getInt("requested_quantity"));
        Timestamp requiredBy = rs.getTimestamp("required_by");
        q.setRequiredBy(requiredBy == null ? null : requiredBy.toLocalDateTime().toLocalDate());
        q.setQuotedAmount(rs.getBigDecimal("quoted_amount"));
        q.setDepositAmount(rs.getBigDecimal("deposit_amount"));
        Timestamp depositPaidAt = rs.getTimestamp("deposit_paid_at");
        q.setDepositPaidAt(depositPaidAt == null ? null : depositPaidAt.toLocalDateTime());
        Timestamp productionStartedAt = rs.getTimestamp("production_started_at");
        q.setProductionStartedAt(productionStartedAt == null ? null : productionStartedAt.toLocalDateTime());
        Timestamp completedAt = rs.getTimestamp("completed_at");
        q.setCompletedAt(completedAt == null ? null : completedAt.toLocalDateTime());
        q.setProductionNote(rs.getString("production_note"));
        long orderId = rs.getLong("order_id");
        q.setOrderId(rs.wasNull() ? null : orderId);
        q.setOrderCode(rs.getString("order_code"));
        q.setSellerResponseNote(rs.getString("seller_response_note"));
        q.setStatus(rs.getString("status"));
        q.setProductName(rs.getString("product_name"));
        q.setPreferredShopName(rs.getString("shop_name"));
        return q;
    }
}
