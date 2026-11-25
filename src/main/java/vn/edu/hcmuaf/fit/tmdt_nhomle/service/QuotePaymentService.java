package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.QuoteDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.QuotePaymentDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.QuotePaymentDao.QuotePaymentAttempt;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.QuoteRequest;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/** Xử lý luồng thanh toán tiền cọc 30% qua ZaloPay Sandbox cho yêu cầu báo giá. */
public class QuotePaymentService {

    private static final System.Logger LOGGER = System.getLogger(QuotePaymentService.class.getName());

    private final ZaloPayConfig config;
    private final ZaloPayService zaloPayService;

    public QuotePaymentService() {
        this(ZaloPayConfig.get());
    }

    public QuotePaymentService(ZaloPayConfig config) {
        this(config, new ZaloPayService(config));
    }

    QuotePaymentService(ZaloPayConfig config, ZaloPayService zaloPayService) {
        this.config = config;
        this.zaloPayService = zaloPayService;
    }

    public StartCheckout startZaloPayDeposit(long quoteRequestId, User user,
                                             String redirectUrl, String callbackUrl) {
        config.requireConfigured();
        if (user == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập để đặt cọc.");
        }
        QuoteRequest quote = QuoteDao.findByIdForUser(quoteRequestId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu báo giá."));
        if (!QuoteRequest.STATUS_QUOTED.equals(quote.getStatus())) {
            throw new IllegalArgumentException("Chỉ yêu cầu đang ở trạng thái Đã báo giá mới có thể đặt cọc.");
        }
        BigDecimal depositAmount = QuoteRequest.calculateDepositAmount(quote.getQuotedAmount());
        if (depositAmount == null || depositAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Báo giá chưa có số tiền hợp lệ để đặt cọc.");
        }

        ZaloPayService.CreateOrderResult created = zaloPayService.createQuoteDepositOrder(
                quote, user.getId(), depositAmount, redirectUrl, callbackUrl);

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String currentStatus = QuoteDao.lockStatusForUser(conn, quoteRequestId, user.getId());
                if (!QuoteRequest.STATUS_QUOTED.equals(currentStatus)) {
                    throw new IllegalArgumentException("Yêu cầu báo giá này không còn ở trạng thái chờ đặt cọc.");
                }
                QuotePaymentDao.createPendingZaloPay(conn, quoteRequestId, user.getId(),
                        created.appTransId(), created.zpTransToken(), created.orderUrl(), depositAmount);
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
        return new StartCheckout(created.appTransId(), created.orderUrl());
    }

    public String reconcileOne(String appTransId) {
        if (appTransId == null || appTransId.isBlank()) return "ERROR";
        QuotePaymentAttempt attempt = QuotePaymentDao.findByAppTransId(appTransId).orElse(null);
        if (attempt == null) return "NOT_FOUND";
        if ("PAID".equals(attempt.status()) || isAlreadyDepositedOrBeyond(attempt.quoteStatus())) {
            return "PAID";
        }
        if ("FAILED".equals(attempt.status())) {
            return "FAILED";
        }
        try {
            ZaloPayService.QueryOrderResult query = zaloPayService.queryOrder(appTransId);
            if (query.paid()) {
                if (query.amount() != null && attempt.amount().compareTo(query.amount()) == 0) {
                    confirmDeposit(appTransId, query.zpTransId(), query.amount(), null, null);
                    return "PAID";
                }
                return "ERROR";
            }
            if (query.isProcessing() || query.returnCode() == 3) {
                return "PENDING";
            }
            markFailedIfPending(appTransId, query.returnCode(),
                    query.returnMessage() == null ? "Giao dịch chưa hoàn tất hoặc đã hủy." : query.returnMessage());
            return "FAILED";
        } catch (RuntimeException e) {
            LOGGER.log(System.Logger.Level.WARNING, "Không truy vấn được ZaloPay cho " + appTransId, e);
            return "PENDING";
        }
    }

    public void reconcilePendingForUser(long userId) {
        try {
            for (String appTransId : QuotePaymentDao.listRecentPendingAppTransIds(userId)) {
                reconcileOne(appTransId);
            }
        } catch (RuntimeException e) {
            LOGGER.log(System.Logger.Level.WARNING, "Không đồng bộ được giao dịch ZaloPay pending cho user " + userId, e);
        }
    }

    public CallbackResult processCallback(String rawJsonBody) {
        ZaloPayService.CallbackData callback = zaloPayService.parseCallback(rawJsonBody);
        if (!callback.validMac()) {
            return new CallbackResult(-1, "mac not equal");
        }
        if (callback.appTransId() == null || callback.amount() == null) {
            return new CallbackResult(0, "invalid payload");
        }
        QuotePaymentAttempt located = QuotePaymentDao.findByAppTransId(callback.appTransId()).orElse(null);
        if (located == null) {
            return new CallbackResult(0, "transaction not found");
        }
        if (located.amount() == null || located.amount().compareTo(callback.amount()) != 0) {
            return new CallbackResult(0, "invalid amount");
        }
        confirmDeposit(callback.appTransId(), callback.zpTransId(), callback.amount(),
                callback.channel(), rawJsonBody);
        return new CallbackResult(1, "success");
    }

    public ReturnResult processReturn(Map<String, String[]> requestParameters) {
        String appTransId = param(requestParameters, "apptransid");
        String redirectStatus = param(requestParameters, "status");
        String amountParam = param(requestParameters, "amount");

        if (appTransId == null || appTransId.isBlank()) {
            return new ReturnResult("error", "Thiếu mã giao dịch ZaloPay.", null, null);
        }

        QuotePaymentAttempt attempt = QuotePaymentDao.findByAppTransId(appTransId).orElse(null);
        if (attempt == null) {
            return new ReturnResult("error", "Không tìm thấy giao dịch đặt cọc trong hệ thống.", null, null);
        }

        if ("PAID".equals(attempt.status())
                || isAlreadyDepositedOrBeyond(attempt.quoteStatus())) {
            return new ReturnResult("success",
                    "Thanh toán đặt cọc qua ZaloPay thành công. Xưởng sẽ sớm bắt đầu in.",
                    attempt, attempt.zpTransId());
        }

        try {
            ZaloPayService.QueryOrderResult query = zaloPayService.queryOrder(appTransId);
            if (query.paid()) {
                if (query.amount() == null || attempt.amount().compareTo(query.amount()) != 0) {
                    return new ReturnResult("error",
                            "Số tiền xác nhận từ ZaloPay không khớp với tiền cọc yêu cầu.",
                            attempt, query.zpTransId());
                }
                confirmDeposit(appTransId, query.zpTransId(), query.amount(), null, null);
                attempt = QuotePaymentDao.findByAppTransId(appTransId).orElse(attempt);
                return new ReturnResult("success",
                        "Đặt cọc 30% qua ZaloPay thành công. Yêu cầu đã chuyển sang trạng thái Đã đặt cọc.",
                        attempt, query.zpTransId());
            }
            if (query.isProcessing() || query.returnCode() == 3) {
                return new ReturnResult("pending",
                        "Giao dịch ZaloPay đang được xử lý. Vui lòng kiểm tra lại sau ít phút.",
                        attempt, query.zpTransId());
            }
            markFailedIfPending(appTransId, query.returnCode(),
                    query.returnMessage() == null ? "Giao dịch chưa hoàn tất hoặc đã hủy." : query.returnMessage());
            return new ReturnResult("failed",
                    "Giao dịch đặt cọc ZaloPay chưa hoàn tất hoặc đã bị hủy.",
                    attempt, query.zpTransId());
        } catch (PaymentException networkOrQueryError) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "Không truy vấn được ZaloPay /v2/query cho " + appTransId, networkOrQueryError);
            boolean validChecksum = zaloPayService.verifyRedirectChecksum(requestParameters);
            BigDecimal returnedAmount = parseDecimal(amountParam);
            if (validChecksum && "1".equals(redirectStatus)
                    && returnedAmount != null && attempt.amount().compareTo(returnedAmount) == 0) {
                confirmDeposit(appTransId, null, returnedAmount, null, null);
                attempt = QuotePaymentDao.findByAppTransId(appTransId).orElse(attempt);
                return new ReturnResult("success",
                        "Đặt cọc 30% qua ZaloPay thành công. Yêu cầu đã chuyển sang trạng thái Đã đặt cọc.",
                        attempt, attempt.zpTransId());
            }
            if (redirectStatus != null && !"1".equals(redirectStatus)) {
                markFailedIfPending(appTransId, -1, "Khách hàng hủy hoặc giao dịch thất bại (status=" + redirectStatus + ").");
                return new ReturnResult("failed",
                        "Giao dịch đặt cọc ZaloPay chưa thành công hoặc đã bị hủy.",
                        attempt, null);
            }
            return new ReturnResult("pending",
                    "Chưa thể xác minh trạng thái giao dịch từ ZaloPay ngay lúc này. Vui lòng kiểm tra lại tại mục Yêu cầu báo giá.",
                    attempt, null);
        }
    }

    private void confirmDeposit(String appTransId, String zpTransId, BigDecimal amount,
                                Integer channel, String rawCallback) {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                QuotePaymentAttempt located = QuotePaymentDao.findByAppTransId(appTransId).orElse(null);
                if (located == null) {
                    conn.rollback();
                    return;
                }
                String lockedQuoteStatus = QuoteDao.lockStatusForUser(
                        conn, located.quoteRequestId(), located.userId());
                QuotePaymentAttempt attempt = QuotePaymentDao.findForUpdate(conn, appTransId).orElse(null);
                if (attempt == null || lockedQuoteStatus == null) {
                    conn.rollback();
                    return;
                }
                if ("PAID".equals(attempt.status()) || isAlreadyDepositedOrBeyond(lockedQuoteStatus)) {
                    conn.rollback();
                    return;
                }
                if (!QuoteRequest.STATUS_QUOTED.equals(lockedQuoteStatus)) {
                    conn.rollback();
                    return;
                }
                QuoteDao.markDeposited(conn, attempt.quoteRequestId(), attempt.userId(), attempt.amount());
                QuotePaymentDao.markPaid(conn, attempt.quotePaymentId(), zpTransId, channel, rawCallback);
                sendDepositNotifications(conn, attempt);
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    private void markFailedIfPending(String appTransId, int returnCode, String message) {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                QuotePaymentAttempt attempt = QuotePaymentDao.findForUpdate(conn, appTransId).orElse(null);
                if (attempt != null && "PENDING".equals(attempt.status())) {
                    QuotePaymentDao.markFailed(conn, attempt.quotePaymentId(), returnCode, message);
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LOGGER.log(System.Logger.Level.WARNING, "Không cập nhật được trạng thái FAILED cho " + appTransId, e);
        }
    }

    private void sendDepositNotifications(Connection conn, QuotePaymentAttempt attempt) {
        try {
            NotificationService.notifyUser(conn, attempt.userId(), "QUOTE_DEPOSITED",
                    "Đặt cọc báo giá #" + attempt.quoteRequestId() + " thành công",
                    "Bạn đã đặt cọc " + attempt.amount().toPlainString() + "đ qua ZaloPay. Xưởng sẽ sớm bắt đầu in.",
                    "/quotes", "QUOTE_REQUEST", attempt.quoteRequestId());
            if (attempt.preferredShopId() != null) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT owner_user_id FROM print_shops WHERE shop_id = ?")) {
                    ps.setLong(1, attempt.preferredShopId());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            long ownerId = rs.getLong("owner_user_id");
                            NotificationService.notifyUser(conn, ownerId, "QUOTE_DEPOSITED",
                                    "Khách đã đặt cọc báo giá #" + attempt.quoteRequestId(),
                                    "Yêu cầu báo giá #" + attempt.quoteRequestId()
                                            + " đã thanh toán tiền cọc qua ZaloPay. Xưởng có thể bắt đầu in.",
                                    "/workshop/quotes?shopId=" + attempt.preferredShopId(),
                                    "QUOTE_REQUEST", attempt.quoteRequestId());
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "Không gửi được thông báo đặt cọc cho yêu cầu #" + attempt.quoteRequestId(), e);
        }
    }

    private static boolean isAlreadyDepositedOrBeyond(String status) {
        return QuoteRequest.STATUS_DEPOSITED.equals(status)
                || QuoteRequest.STATUS_PRINTING.equals(status)
                || QuoteRequest.STATUS_COMPLETED.equals(status);
    }

    private static String param(Map<String, String[]> map, String key) {
        if (map == null) return null;
        String[] values = map.get(key);
        return (values != null && values.length > 0) ? values[0] : null;
    }

    private static BigDecimal parseDecimal(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public record StartCheckout(String appTransId, String orderUrl) { }

    public record CallbackResult(int returnCode, String returnMessage) { }

    public record ReturnResult(String status, String message, QuotePaymentAttempt attempt, String zpTransId) { }
}
