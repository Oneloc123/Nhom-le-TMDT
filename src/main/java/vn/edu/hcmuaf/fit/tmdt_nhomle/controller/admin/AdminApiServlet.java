package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.AdminDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.AdminBusinessQueueDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.AdminOperationsDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.AdminStatisticsDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ReturnRefundDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.FinancialAdminDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.ReturnRefundEvidenceStore;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.AuthFilter;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.Json;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@WebServlet(name = "adminApiServlet", urlPatterns = "/admin/api/*")
public class AdminApiServlet extends HttpServlet {
    private static final System.Logger LOG = System.getLogger(AdminApiServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String[] path = path(request);
            if (path.length == 2 && "return-refund-evidence".equals(path[0])) {
                streamReturnRefundEvidence(request, response, id(path[1]));
                return;
            }
            Object data;
            if (path.length == 1 && "session".equals(path[0])) {
                User current = AuthFilter.currentUser(request);
                data = Map.of("id", current.getId(), "name", Optional.ofNullable(current.getFullName()).orElse("Admin"),
                        "role", current.getRole());
            } else if (path.length == 1 && "notification-summary".equals(path[0])) {
                data = AdminDao.notificationSummary();
            } else if (path.length == 1 && "dashboard".equals(path[0])) {
                data = AdminDao.dashboard();
            } else if (path.length == 1 && "finance".equals(path[0])) {
                data = FinancialAdminDao.dashboard();
            } else if (path.length == 1 && "statistics".equals(path[0])) {
                LocalDate today = LocalDate.now();
                LocalDate to = dateParam(request.getParameter("to"), today, "Ngày kết thúc");
                LocalDate from = dateParam(request.getParameter("from"), to.withDayOfMonth(1), "Ngày bắt đầu");
                data = AdminStatisticsDao.load(from, to, request.getParameter("groupBy"));
            } else if (path.length == 1 && "users".equals(path[0])) {
                data = AdminDao.listUsers(request.getParameter("q"), request.getParameter("role"),
                        request.getParameter("status"), intParam(request, "page", 1), intParam(request, "size", 20));
            } else if (path.length == 2 && "users".equals(path[0])) {
                data = AdminDao.userDetail(id(path[1])).orElseThrow(() -> new NotFound("Không tìm thấy người dùng."));
            } else if (path.length == 1 && "sellers".equals(path[0])) {
                data = AdminDao.listSellers(request.getParameter("q"), request.getParameter("status"),
                        request.getParameter("verification"), intParam(request, "page", 1), intParam(request, "size", 20));
            } else if (path.length == 2 && "sellers".equals(path[0])) {
                data = AdminDao.sellerDetail(id(path[1])).orElseThrow(() -> new NotFound("Không tìm thấy xưởng in."));
            } else if (path.length == 1 && "products".equals(path[0])) {
                data = AdminDao.listProducts(request.getParameter("q"), request.getParameter("status"),
                        request.getParameter("category"), request.getParameter("shop"),
                        intParam(request, "page", 1), intParam(request, "size", 20));
            } else if (path.length == 2 && "products".equals(path[0])) {
                data = AdminDao.productDetail(id(path[1])).orElseThrow(() -> new NotFound("Không tìm thấy sản phẩm."));
            } else if (path.length == 1 && "orders".equals(path[0])) {
                data = AdminDao.listOrders(request.getParameter("q"), request.getParameter("status"),
                        request.getParameter("payment"), intParam(request, "page", 1), intParam(request, "size", 20));
            } else if (path.length == 2 && "orders".equals(path[0])) {
                data = AdminDao.orderDetail(id(path[1])).orElseThrow(() -> new NotFound("Không tìm thấy đơn hàng."));
            } else if (path.length == 1 && "quotes".equals(path[0])) {
                data = AdminDao.listQuotes(request.getParameter("q"), request.getParameter("status"),
                        intParam(request, "page", 1), intParam(request, "size", 20));
            } else if (path.length == 1 && "custom-prints".equals(path[0])) {
                data = AdminBusinessQueueDao.listCustomPrintRequests(request.getParameter("q"), request.getParameter("status"));
            } else if (path.length == 2 && "custom-prints".equals(path[0])) {
                data = AdminBusinessQueueDao.customPrintRequestDetail(id(path[1]));
                if (data == null) throw new NotFound("Không tìm thấy yêu cầu in 3D.");
            } else if (path.length == 1 && "order-changes".equals(path[0])) {
                data = AdminBusinessQueueDao.listOrderChangeRequests(request.getParameter("q"), request.getParameter("status"));
            } else if (path.length == 2 && "order-changes".equals(path[0])) {
                data = AdminBusinessQueueDao.orderChangeRequestDetail(id(path[1]));
                if (data == null) throw new NotFound("Không tìm thấy yêu cầu thay đổi đơn.");
            } else if (path.length == 1 && "reviews".equals(path[0])) {
                data = AdminBusinessQueueDao.listReviews(request.getParameter("q"), request.getParameter("status"),
                        intParam(request, "page", 1), intParam(request, "size", 20));
            } else if (path.length == 2 && "reviews".equals(path[0])) {
                data = AdminBusinessQueueDao.reviewDetail(id(path[1]));
                if (data == null) throw new NotFound("Không tìm thấy đánh giá.");
            } else if (path.length == 1 && "reconciliation".equals(path[0])) {
                data = AdminBusinessQueueDao.listPaymentReconciliation(request.getParameter("q"), request.getParameter("status"));
            } else if (path.length == 2 && "reconciliation".equals(path[0])) {
                data = AdminBusinessQueueDao.paymentReconciliationDetail(id(path[1]));
                if (data == null) throw new NotFound("Không tìm thấy giao dịch thanh toán.");
            } else if (path.length == 1 && "return-refunds".equals(path[0])) {
                data = ReturnRefundDao.listForAdmin(request.getParameter("status"), request.getParameter("q"));
            } else if (path.length == 2 && "return-refunds".equals(path[0])) {
                Map<String, Object> detail = ReturnRefundDao.detailForAdmin(id(path[1]));
                if (detail == null) throw new NotFound("Không tìm thấy yêu cầu đổi trả/hoàn tiền.");
                detail.put("evidence", ReturnRefundDao.evidenceListForAdmin(id(path[1])));
                detail.put("events", ReturnRefundDao.eventsForAdmin(id(path[1])));
                detail.put("refundTransactions", ReturnRefundDao.refundTransactionsForAdmin(id(path[1])));
                detail.put("manualRefunds", AdminOperationsDao.manualRefundsForPayment(id(path[1])));
                data = detail;
            } else {
                throw new NotFound("Đường dẫn Admin API không tồn tại.");
            }
            send(response, HttpServletResponse.SC_OK, Map.of("data", data));
        } catch (NotFound e) {
            send(response, HttpServletResponse.SC_NOT_FOUND, Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            send(response, HttpServletResponse.SC_BAD_REQUEST, Map.of("error", safeMessage(e)));
        } catch (DataAccessException e) {
            LOG.log(System.Logger.Level.ERROR, "Admin API read failed", e);
            send(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    Map.of("error", "Không thể đọc dữ liệu quản trị. Vui lòng thử lại sau."));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        try {
            String[] path = path(request);
            User actor = AuthFilter.currentUser(request);
            if (path.length == 1 && "users".equals(path[0])) {
                long createdId = AdminDao.createUser(request.getParameter("email"), request.getParameter("password"),
                        request.getParameter("fullName"), request.getParameter("phone"), request.getParameter("role"));
                send(response, HttpServletResponse.SC_CREATED,
                        Map.of("data", Map.of("id", Long.toString(createdId), "message", "Đã tạo tài khoản.")));
                return;
            } else if (path.length == 3 && "users".equals(path[0]) && "profile".equals(path[2])) {
                AdminDao.updateUserProfile(id(path[1]), request.getParameter("fullName"), request.getParameter("phone"),
                        request.getParameter("role"), actor.getId());
            } else if (path.length == 3 && "users".equals(path[0]) && "status".equals(path[2])) {
                AdminDao.changeUserStatus(id(path[1]), actor.getId(), request.getParameter("status"));
            } else if (path.length == 3 && "sellers".equals(path[0]) && "status".equals(path[2])) {
                AdminDao.changeSellerStatus(id(path[1]), request.getParameter("status"));
            } else if (path.length == 3 && "sellers".equals(path[0]) && "verification".equals(path[2])) {
                String action = request.getParameter("action");
                if ("verify".equalsIgnoreCase(action)) action = "APPROVE";
                AdminOperationsDao.decideSellerVerification(id(path[1]), actor.getId(),
                        action, request.getParameter("reason"));
            } else if (path.length == 3 && "products".equals(path[0]) && "moderation".equals(path[2])) {
                AdminDao.moderateProduct(id(path[1]), actor.getId(), request.getParameter("action"), request.getParameter("reason"));
            } else if (path.length == 3 && "orders".equals(path[0]) && "status".equals(path[2])) {
                AdminDao.changeOrderStatus(id(path[1]), actor.getId(), request.getParameter("status"), request.getParameter("note"));
            } else if (path.length == 3 && "reconciliation".equals(path[0]) && "refund".equals(path[2])) {
                AdminOperationsDao.completeManualRefund(id(path[1]), actor.getId(),
                        request.getParameter("method"), request.getParameter("reference"), request.getParameter("note"));
            } else if (path.length == 3 && "reconciliation".equals(path[0]) && "refund-failed".equals(path[2])) {
                AdminOperationsDao.recordManualRefundFailure(id(path[1]), actor.getId(),
                        request.getParameter("method"), request.getParameter("reference"), request.getParameter("note"));
            } else if (path.length == 3 && "custom-prints".equals(path[0]) && "review".equals(path[2])) {
                AdminBusinessQueueDao.reviewCustomPrintRequest(id(path[1]), actor.getId(),
                        request.getParameter("action"), request.getParameter("note"));
            } else if (path.length == 3 && "order-changes".equals(path[0]) && "action".equals(path[2])) {
                AdminBusinessQueueDao.actOnOrderChangeRequest(id(path[1]), actor.getId(),
                        request.getParameter("action"), request.getParameter("note"));
            } else if (path.length == 3 && "reviews".equals(path[0]) && "moderation".equals(path[2])) {
                AdminBusinessQueueDao.moderateReview(id(path[1]), actor.getId(),
                        request.getParameter("action"), request.getParameter("reason"));
            } else if (path.length == 3 && "reconciliation".equals(path[0]) && "review".equals(path[2])) {
                AdminBusinessQueueDao.recordPaymentReconciliation(id(path[1]), actor.getId(),
                        request.getParameter("outcome"), request.getParameter("note"),
                        request.getParameter("reference"));
            } else if (path.length == 4 && "finance".equals(path[0]) && "cod".equals(path[1]) && "receive".equals(path[3])) {
                FinancialAdminDao.recordCodReceipt(id(path[2]), request.getParameter("carrierName"),
                        request.getParameter("carrierReference"), actor.getId(), request.getParameter("note"));
            } else if (path.length == 3 && "finance".equals(path[0]) && "settlements".equals(path[1]) && "create".equals(path[2])) {
                LocalDate periodStart = dateParam(request.getParameter("periodStart"), null, "Ngày bắt đầu kỳ");
                LocalDate periodEnd = dateParam(request.getParameter("periodEnd"), null, "Ngày kết thúc kỳ");
                int settled = FinancialAdminDao.createMonthlySettlement(periodStart, periodEnd, actor.getId());
                send(response, HttpServletResponse.SC_OK, Map.of("data", Map.of("message", "Đã tạo kỳ quyết toán.", "settledAllocations", settled)));
                return;
            } else if (path.length == 4 && "finance".equals(path[0]) && "settlements".equals(path[1]) && "paid".equals(path[3])) {
                FinancialAdminDao.markSettlementPaid(id(path[2]), actor.getId(),
                        request.getParameter("payoutReference"), request.getParameter("note"));
            } else if (path.length == 3 && "return-refunds".equals(path[0]) && "action".equals(path[2])) {
                ReturnRefundDao.actAsAdmin(id(path[1]), actor.getId(), request.getParameter("action"),
                        request.getParameter("note"), optionalAmount(request.getParameter("approvedAmount")),
                        request.getParameter("refundMethod"), request.getParameter("refundReference"));
            } else {
                throw new NotFound("Đường dẫn Admin API không tồn tại.");
            }
            send(response, HttpServletResponse.SC_OK, Map.of("data", Map.of("message", "Đã lưu thay đổi.")));
        } catch (NotFound e) {
            send(response, HttpServletResponse.SC_NOT_FOUND, Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            send(response, HttpServletResponse.SC_BAD_REQUEST, Map.of("error", safeMessage(e)));
        } catch (DataAccessException e) {
            LOG.log(System.Logger.Level.ERROR, "Admin API update failed", e);
            send(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    Map.of("error", "Không thể lưu dữ liệu. Vui lòng kiểm tra kết nối cơ sở dữ liệu rồi thử lại."));
        }
    }

    private static String[] path(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path == null || path.isBlank() || "/".equals(path)) return new String[0];
        return java.util.Arrays.stream(path.split("/"))
                .filter(part -> !part.isBlank()).toArray(String[]::new);
    }

    private static long id(String raw) {
        try {
            long value = Long.parseLong(raw);
            if (value > 0) return value;
        } catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException("Mã bản ghi không hợp lệ.");
    }

    private static int intParam(HttpServletRequest request, String key, int fallback) {
        String value = request.getParameter(key);
        if (value == null || value.isBlank()) return fallback;
        try { return Math.max(1, Integer.parseInt(value)); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Tham số phân trang không hợp lệ."); }
    }

    private BigDecimal optionalAmount(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            BigDecimal amount = new BigDecimal(raw.trim());
            if (amount.scale() > 2 || amount.precision() > 12 || amount.signum() <= 0) {
                throw new IllegalArgumentException("Số tiền duyệt phải là số dương tối đa 12 chữ số và 2 chữ số thập phân.");
            }
            return amount;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Số tiền duyệt không hợp lệ.");
        }
    }

    private void streamReturnRefundEvidence(HttpServletRequest request, HttpServletResponse response, long evidenceId)
            throws IOException {
        Map<String, Object> evidence = ReturnRefundDao.evidenceForAdmin(evidenceId);
        if (evidence == null) { response.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
        Path file;
        try {
            file = ReturnRefundEvidenceStore.resolve(String.valueOf(evidence.get("storageKey")), getServletContext());
        } catch (IllegalArgumentException | IllegalStateException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(file)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String type = String.valueOf(evidence.get("contentType"));
        if (!"image/jpeg".equals(type) && !"image/png".equals(type)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.setContentType(type);
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Cache-Control", "private, no-store");
        response.setHeader("Content-Disposition", "inline; filename=\"return-evidence\"");
        response.setContentLengthLong(Files.size(file));
        try (var out = response.getOutputStream()) { Files.copy(file, out); }
    }

    private static LocalDate dateParam(String value, LocalDate fallback, String label) {
        if (value == null || value.isBlank()) return fallback;
        try { return LocalDate.parse(value.trim()); }
        catch (DateTimeParseException e) { throw new IllegalArgumentException(label + " không hợp lệ. Hãy dùng định dạng YYYY-MM-DD."); }
    }

    private static String safeMessage(IllegalArgumentException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "Dữ liệu gửi lên không hợp lệ." : message;
    }

    private static void send(HttpServletResponse response, int status, Object payload) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(Json.stringify(payload));
    }

    private static final class NotFound extends RuntimeException {
        private NotFound(String message) { super(message); }
    }
}
