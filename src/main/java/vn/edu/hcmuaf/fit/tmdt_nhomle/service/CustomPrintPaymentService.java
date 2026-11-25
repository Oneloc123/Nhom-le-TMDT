package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.CustomPrintPaymentDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.CustomPrintPaymentDao.CustomPrintPaymentAttempt;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.CustomPrintWorkflowDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.CustomPrintRequest;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/** Xử lý thanh toán tiền cọc 30% qua ZaloPay Sandbox cho yêu cầu in 3D theo yêu cầu. */
public class CustomPrintPaymentService {

    private static final System.Logger LOGGER = System.getLogger(CustomPrintPaymentService.class.getName());

    private final ZaloPayConfig config;
    private final ZaloPayService zaloPayService;

    public CustomPrintPaymentService() {
        this(ZaloPayConfig.get());
    }

    public CustomPrintPaymentService(ZaloPayConfig config) {
        this(config, new ZaloPayService(config));
    }

    CustomPrintPaymentService(ZaloPayConfig config, ZaloPayService zaloPayService) {
        this.config = config;
        this.zaloPayService = zaloPayService;
    }

    public QuotePaymentService.StartCheckout startZaloPayDeposit(long customRequestId, User user,
                                                                 String redirectUrl, String callbackUrl) {
        config.requireConfigured();
        if (user == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập để đặt cọc.");
        }
        CustomPrintRequest req = CustomPrintWorkflowDao.findByIdForUser(customRequestId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu in 3D."));
        if (!"QUOTE".equals(req.workflowType()) || !"QUOTED".equals(req.status()) || req.orderId() != null) {
            throw new IllegalArgumentException("Chỉ yêu cầu đang ở trạng thái Đã báo giá mới có thể đặt cọc.");
        }
        if (req.quoteValidUntil() != null && !CustomPrintWorkflowDao.quoteIsStillValid(customRequestId)) {
            throw new IllegalArgumentException("Báo giá đã hết hạn. Hãy gửi yêu cầu báo giá mới.");
        }
        BigDecimal depositAmount = CustomPrintRequest.calculateDepositAmount(req.quotedAmount());
        if (depositAmount == null || depositAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Báo giá chưa có số tiền hợp lệ để đặt cọc.");
        }

        ZaloPayService.CreateOrderResult created = zaloPayService.createCustomPrintDepositOrder(
                req, user.getId(), depositAmount, redirectUrl, callbackUrl);

        try (Connection conn = Database.getConnection()) {
            CustomPrintPaymentDao.ensureSchema(conn);
            conn.setAutoCommit(false);
            try {
                String currentStatus = lockStatus(conn, customRequestId, user.getId());
                if (!"QUOTED".equals(currentStatus)) {
                    throw new IllegalArgumentException("Yêu cầu in 3D này không còn ở trạng thái chờ đặt cọc.");
                }
                CustomPrintPaymentDao.createPendingZaloPay(conn, customRequestId, user.getId(),
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
        return new QuotePaymentService.StartCheckout(created.appTransId(), created.orderUrl());
    }

    public String reconcileOne(String appTransId) {
        if (appTransId == null || appTransId.isBlank()) return "ERROR";
        CustomPrintPaymentAttempt attempt = CustomPrintPaymentDao.findByAppTransId(appTransId).orElse(null);
        if (attempt == null) return "NOT_FOUND";
        if ("PAID".equals(attempt.status()) || isAlreadyDepositedOrBeyond(attempt.requestStatus())) {
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
            for (String appTransId : CustomPrintPaymentDao.listRecentPendingAppTransIds(userId)) {
                reconcileOne(appTransId);
            }
        } catch (RuntimeException e) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "Không đồng bộ được giao dịch ZaloPay pending in 3D cho user " + userId, e);
        }
    }

    public QuotePaymentService.CallbackResult processCallback(String rawJsonBody) {
        ZaloPayService.CallbackData callback = zaloPayService.parseCallback(rawJsonBody);
        if (!callback.validMac()) {
            return new QuotePaymentService.CallbackResult(-1, "mac not equal");
        }
        if (callback.appTransId() == null || callback.amount() == null) {
            return new QuotePaymentService.CallbackResult(0, "invalid payload");
        }
        CustomPrintPaymentAttempt located = CustomPrintPaymentDao.findByAppTransId(callback.appTransId()).orElse(null);
        if (located == null) {
            return new QuotePaymentService.CallbackResult(0, "transaction not found");
        }
        if (located.amount() == null || located.amount().compareTo(callback.amount()) != 0) {
            return new QuotePaymentService.CallbackResult(0, "invalid amount");
        }
        confirmDeposit(callback.appTransId(), callback.zpTransId(), callback.amount(),
                callback.channel(), rawJsonBody);
        return new QuotePaymentService.CallbackResult(1, "success");
    }

    public ReturnResult processReturn(Map<String, String[]> requestParameters) {
        String appTransId = param(requestParameters, "apptransid");
        String redirectStatus = param(requestParameters, "status");
        String amountParam = param(requestParameters, "amount");

        if (appTransId == null || appTransId.isBlank()) {
            return new ReturnResult("error", "Thiếu mã giao dịch ZaloPay.", null, null);
        }

        CustomPrintPaymentAttempt attempt = CustomPrintPaymentDao.findByAppTransId(appTransId).orElse(null);
        if (attempt == null) {
            return new ReturnResult("error", "Không tìm thấy giao dịch đặt cọc in 3D trong hệ thống.", null, null);
        }

        if ("PAID".equals(attempt.status()) || isAlreadyDepositedOrBeyond(attempt.requestStatus())) {
            return new ReturnResult("success",
                    "Đặt cọc 30% qua ZaloPay thành công. Xưởng sẽ sớm bắt đầu in.",
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
                attempt = CustomPrintPaymentDao.findByAppTransId(appTransId).orElse(attempt);
                return new ReturnResult("success",
                        "Đặt cọc 30% qua ZaloPay thành công. Yêu cầu in 3D đã chuyển sang trạng thái Đã đặt cọc.",
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
                attempt = CustomPrintPaymentDao.findByAppTransId(appTransId).orElse(attempt);
                return new ReturnResult("success",
                        "Đặt cọc 30% qua ZaloPay thành công. Yêu cầu in 3D đã chuyển sang trạng thái Đã đặt cọc.",
                        attempt, attempt.zpTransId());
            }
            if (redirectStatus != null && !"1".equals(redirectStatus)) {
                markFailedIfPending(appTransId, -1, "Khách hàng hủy hoặc giao dịch thất bại (status=" + redirectStatus + ").");
                return new ReturnResult("failed",
                        "Giao dịch đặt cọc ZaloPay chưa thành công hoặc đã bị hủy.",
                        attempt, null);
            }
            return new ReturnResult("pending",
                    "Chưa thể xác minh trạng thái giao dịch từ ZaloPay ngay lúc này. Vui lòng kiểm tra lại tại mục Yêu cầu in 3D.",
                    attempt, null);
        }
    }

    private void confirmDeposit(String appTransId, String zpTransId, BigDecimal amount,
                                Integer channel, String rawCallback) {
        try (Connection conn = Database.getConnection()) {
            CustomPrintPaymentDao.ensureSchema(conn);
            conn.setAutoCommit(false);
            try {
                CustomPrintPaymentAttempt located = CustomPrintPaymentDao.findByAppTransId(appTransId).orElse(null);
                if (located == null) {
                    conn.rollback();
                    return;
                }
                String lockedStatus = lockStatus(conn, located.customRequestId(), located.userId());
                CustomPrintPaymentAttempt attempt = CustomPrintPaymentDao.findForUpdate(conn, appTransId).orElse(null);
                if (attempt == null || lockedStatus == null) {
                    conn.rollback();
                    return;
                }
                if ("PAID".equals(attempt.status()) || isAlreadyDepositedOrBeyond(lockedStatus)) {
                    conn.rollback();
                    return;
                }
                if (!"QUOTED".equals(lockedStatus)) {
                    conn.rollback();
                    return;
                }
                CustomPrintWorkflowDao.markDeposited(conn, attempt.customRequestId(), attempt.userId(), attempt.amount());
                CustomPrintPaymentDao.markPaid(conn, attempt.customPaymentId(), zpTransId, channel, rawCallback);
                sendNotifications(conn, attempt);
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
            CustomPrintPaymentDao.ensureSchema(conn);
            conn.setAutoCommit(false);
            try {
                CustomPrintPaymentAttempt attempt = CustomPrintPaymentDao.findForUpdate(conn, appTransId).orElse(null);
                if (attempt != null && "PENDING".equals(attempt.status())) {
                    CustomPrintPaymentDao.markFailed(conn, attempt.customPaymentId(), returnCode, message);
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

    private void sendNotifications(Connection conn, CustomPrintPaymentAttempt attempt) {
        String code = attempt.requestCode() == null ? "#" + attempt.customRequestId() : attempt.requestCode();
        try {
            NotificationService.notifyUser(conn, attempt.userId(), "CUSTOM_PRINT_DEPOSITED",
                    "Đặt cọc yêu cầu in 3D " + code + " thành công",
                    "Bạn đã đặt cọc " + attempt.amount().toPlainString() + "đ qua ZaloPay. Xưởng sẽ sớm bắt đầu in.",
                    "/my-designs", "CUSTOM_PRINT_REQUEST", attempt.customRequestId());
            if (attempt.shopId() != null) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT owner_user_id FROM print_shops WHERE shop_id = ?")) {
                    ps.setLong(1, attempt.shopId());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            long ownerId = rs.getLong("owner_user_id");
                            NotificationService.notifyUser(conn, ownerId, "CUSTOM_PRINT_DEPOSITED",
                                    "Khách đã đặt cọc yêu cầu in 3D " + code,
                                    "Yêu cầu in 3D " + code + " đã đặt cọc 30% qua ZaloPay. Xưởng có thể bắt đầu in.",
                                    "/workshop/custom-print-requests/" + attempt.customRequestId()
                                            + "?shopId=" + attempt.shopId(),
                                    "CUSTOM_PRINT_REQUEST", attempt.customRequestId());
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "Không gửi được thông báo đặt cọc in 3D cho #" + attempt.customRequestId(), e);
        }
    }

    private static String lockStatus(Connection conn, long customRequestId, long userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT status FROM custom_print_requests WHERE custom_request_id = ? AND user_id = ? FOR UPDATE")) {
            ps.setLong(1, customRequestId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("status") : null;
            }
        }
    }

    private static boolean isAlreadyDepositedOrBeyond(String status) {
        return "DEPOSITED".equals(status) || "PRINTING".equals(status) || "COMPLETED".equals(status);
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

    public record ReturnResult(String status, String message, CustomPrintPaymentAttempt attempt, String zpTransId) { }
}
