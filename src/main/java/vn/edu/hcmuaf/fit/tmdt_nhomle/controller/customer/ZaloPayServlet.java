package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.CustomPrintPaymentDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.PaymentDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.QuotePaymentDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.PaymentAttempt;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.AuthFilter;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.CustomPrintPaymentService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.PaymentService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.QuotePaymentService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.ZaloPayConfig;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.ZaloPayService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.Json;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Endpoint nhận callback server-to-server, trang chờ thanh toán và redirect từ ZaloPay Sandbox. */
@WebServlet(name = "zaloPayServlet", value = "/zalopay/*")
public class ZaloPayServlet extends HttpServlet {

    private static final System.Logger LOGGER = System.getLogger(ZaloPayServlet.class.getName());
    private final QuotePaymentService quotePaymentService = new QuotePaymentService();
    private final CustomPrintPaymentService customPrintPaymentService = new CustomPrintPaymentService();
    private final PaymentService paymentService = new PaymentService();
    private final ZaloPayService zaloPayService = new ZaloPayService(ZaloPayConfig.get());

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String path = request.getPathInfo();
        if (!"/callback".equals(path)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");

        QuotePaymentService.CallbackResult result;
        try {
            if (!ZaloPayConfig.get().isConfigured()) {
                result = new QuotePaymentService.CallbackResult(0, "ZaloPay not configured");
            } else {
                String rawBody = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                ZaloPayService.CallbackData parsed = zaloPayService.parseCallback(rawBody);
                if (!parsed.validMac()) {
                    result = new QuotePaymentService.CallbackResult(-1, "mac not equal");
                } else if (PaymentDao.findByZaloPayAppTransId(parsed.appTransId()).isPresent()) {
                    PaymentService.ZaloPayCallbackResult paymentResult =
                            paymentService.processZaloPayCallback(rawBody);
                    result = new QuotePaymentService.CallbackResult(
                            paymentResult.returnCode(), paymentResult.returnMessage());
                } else if (isCustomPrintTransaction(parsed.appTransId())) {
                    result = customPrintPaymentService.processCallback(rawBody);
                } else {
                    result = quotePaymentService.processCallback(rawBody);
                }
            }
        } catch (RuntimeException error) {
            LOGGER.log(System.Logger.Level.ERROR, "Không xử lý được callback ZaloPay", error);
            result = new QuotePaymentService.CallbackResult(0, error.getMessage());
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("return_code", result.returnCode());
        payload.put("return_message", result.returnMessage());
        response.getWriter().write(Json.stringify(payload));
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getPathInfo();
        if ("/checkout".equals(path)) {
            handleCheckoutPage(request, response);
            return;
        }
        if ("/status".equals(path)) {
            handleStatusCheck(request, response);
            return;
        }
        if ("/return".equals(path)) {
            handleReturn(request, response);
            return;
        }
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void handleCheckoutPage(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = AuthFilter.currentUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        String appTransId = request.getParameter("appTransId");
        if (appTransId == null || appTransId.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/my-designs");
            return;
        }
        response.setHeader("Cache-Control", "no-store");

        if (isCustomPrintTransaction(appTransId)) {
            var attempt = CustomPrintPaymentDao.findByAppTransId(appTransId).orElse(null);
            String orderUrl = CustomPrintPaymentDao.findOrderUrlForUser(appTransId, user.getId()).orElse(null);
            if (attempt == null || attempt.userId() != user.getId() || orderUrl == null) {
                response.sendRedirect(request.getContextPath() + "/my-designs");
                return;
            }
            if ("PAID".equals(customPrintPaymentService.reconcileOne(appTransId))) {
                response.sendRedirect(returnRedirectUrl(request, appTransId));
                return;
            }
            request.setAttribute("appTransId", appTransId);
            request.setAttribute("orderUrl", orderUrl);
            request.setAttribute("amount", attempt.amount());
            request.setAttribute("quotedAmount", attempt.quotedAmount());
            request.setAttribute("requestCode", attempt.requestCode());
            request.setAttribute("productName", attempt.originalFilename());
            request.setAttribute("backUrl", "/my-designs");
            request.setAttribute("backLabel", "Quay lại Yêu cầu in 3D");
        } else {
            var attempt = QuotePaymentDao.findByAppTransId(appTransId).orElse(null);
            String orderUrl = QuotePaymentDao.findOrderUrlForUser(appTransId, user.getId()).orElse(null);
            if (attempt == null || attempt.userId() != user.getId() || orderUrl == null) {
                response.sendRedirect(request.getContextPath() + "/quotes");
                return;
            }
            if ("PAID".equals(quotePaymentService.reconcileOne(appTransId))) {
                response.sendRedirect(returnRedirectUrl(request, appTransId));
                return;
            }
            request.setAttribute("appTransId", appTransId);
            request.setAttribute("orderUrl", orderUrl);
            request.setAttribute("amount", attempt.amount());
            request.setAttribute("quotedAmount", attempt.quotedAmount());
            request.setAttribute("quoteRequestId", attempt.quoteRequestId());
            request.setAttribute("productName", attempt.productName());
            request.setAttribute("backUrl", "/quotes");
            request.setAttribute("backLabel", "Quay lại Yêu cầu báo giá");
        }
        request.getRequestDispatcher("/customer/zalopay-checkout.jsp").forward(request, response);
    }

    private void handleStatusCheck(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");

        User user = AuthFilter.currentUser(request);
        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(Json.stringify(Map.of("status", "UNAUTHORIZED")));
            return;
        }
        String appTransId = request.getParameter("appTransId");
        String status;
        PaymentAttempt orderAttempt = PaymentDao.findByZaloPayAppTransId(appTransId).orElse(null);
        if (orderAttempt != null) {
            if (orderAttempt.userId() != user.getId()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write(Json.stringify(Map.of("status", "NOT_FOUND")));
                return;
            }
            status = paymentService.reconcileZaloPay(appTransId);
        } else if (isCustomPrintTransaction(appTransId)) {
            var attempt = CustomPrintPaymentDao.findByAppTransId(appTransId).orElse(null);
            if (attempt == null || attempt.userId() != user.getId()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write(Json.stringify(Map.of("status", "NOT_FOUND")));
                return;
            }
            status = customPrintPaymentService.reconcileOne(appTransId);
        } else {
            var attempt = QuotePaymentDao.findByAppTransId(appTransId).orElse(null);
            if (attempt == null || attempt.userId() != user.getId()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write(Json.stringify(Map.of("status", "NOT_FOUND")));
                return;
            }
            status = quotePaymentService.reconcileOne(appTransId);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", status);
        payload.put("returnUrl", returnRedirectUrl(request, appTransId));
        response.getWriter().write(Json.stringify(payload));
    }

    private void handleReturn(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        String appTransId = request.getParameter("apptransid");

        PaymentAttempt orderAttempt = PaymentDao.findByZaloPayAppTransId(appTransId).orElse(null);
        if (orderAttempt != null) {
            handleOrderReturn(request, response);
            return;
        }

        try {
            if (isCustomPrintTransaction(appTransId)) {
                CustomPrintPaymentService.ReturnResult result =
                        customPrintPaymentService.processReturn(request.getParameterMap());
                request.setAttribute("resultStatus", result.status());
                request.setAttribute("resultMessage", result.message());
                request.setAttribute("backUrl", "/my-designs");
                request.setAttribute("backLabel", "Quay lại Yêu cầu in 3D");
                if (result.attempt() != null) {
                    request.setAttribute("requestCode", result.attempt().requestCode());
                    request.setAttribute("appTransId", result.attempt().appTransId());
                    request.setAttribute("amount", result.attempt().amount());
                    request.setAttribute("quotedAmount", result.attempt().quotedAmount());
                    request.setAttribute("productName", result.attempt().originalFilename());
                }
                if (result.zpTransId() != null) {
                    request.setAttribute("zpTransId", result.zpTransId());
                }
            } else {
                QuotePaymentService.ReturnResult result =
                        quotePaymentService.processReturn(request.getParameterMap());
                request.setAttribute("resultStatus", result.status());
                request.setAttribute("resultMessage", result.message());
                request.setAttribute("backUrl", "/quotes");
                request.setAttribute("backLabel", "Quay lại Yêu cầu báo giá");
                if (result.attempt() != null) {
                    request.setAttribute("quoteRequestId", result.attempt().quoteRequestId());
                    request.setAttribute("appTransId", result.attempt().appTransId());
                    request.setAttribute("amount", result.attempt().amount());
                    request.setAttribute("quotedAmount", result.attempt().quotedAmount());
                    request.setAttribute("productName", result.attempt().productName());
                }
                if (result.zpTransId() != null) {
                    request.setAttribute("zpTransId", result.zpTransId());
                }
            }
        } catch (RuntimeException error) {
            LOGGER.log(System.Logger.Level.ERROR, "Không xử lý được redirect ZaloPay", error);
            request.setAttribute("resultStatus", "error");
            request.setAttribute("resultMessage",
                    "Hệ thống chưa thể xác minh giao dịch ZaloPay. Vui lòng kiểm tra lại trong tài khoản.");
            request.setAttribute("backUrl", "/my-designs");
            request.setAttribute("backLabel", "Quay lại Yêu cầu in 3D");
        }

        request.getRequestDispatcher("/customer/quote-payment-result.jsp").forward(request, response);
    }

    private void handleOrderReturn(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            PaymentService.ZaloPayReturnResult result =
                    paymentService.processZaloPayReturn(request.getParameterMap());
            request.setAttribute("resultStatus", result.status());
            request.setAttribute("resultMessage", result.message());
            request.setAttribute("paymentProvider", "ZaloPay");
            request.setAttribute("backUrl", "/purchase-history");
            request.setAttribute("backLabel", "Xem lịch sử mua hàng");
            if (result.attempt() != null) {
                request.setAttribute("orderCode", result.attempt().orderCode());
                request.setAttribute("amount", result.attempt().amount());
            }
            if (result.zpTransId() != null) {
                request.setAttribute("transactionNo", result.zpTransId());
            }
        } catch (RuntimeException error) {
            LOGGER.log(System.Logger.Level.ERROR, "Không xử lý được kết quả thanh toán đơn hàng ZaloPay", error);
            request.setAttribute("resultStatus", "error");
            request.setAttribute("resultMessage",
                    "Hệ thống chưa thể xác minh giao dịch ZaloPay. Vui lòng kiểm tra lịch sử mua hàng.");
            request.setAttribute("paymentProvider", "ZaloPay");
            request.setAttribute("backUrl", "/purchase-history");
            request.setAttribute("backLabel", "Xem lịch sử mua hàng");
        }
        request.getRequestDispatcher("/customer/payment-result.jsp").forward(request, response);
    }

    private static String returnRedirectUrl(HttpServletRequest request, String appTransId) {
        return request.getContextPath() + "/zalopay/return?apptransid="
                + URLEncoder.encode(appTransId, StandardCharsets.UTF_8);
    }

    private static boolean isCustomPrintTransaction(String appTransId) {
        if (appTransId == null || appTransId.isBlank()) {
            return false;
        }
        return appTransId.contains("_CP")
                || CustomPrintPaymentDao.findByAppTransId(appTransId).isPresent();
    }
}
