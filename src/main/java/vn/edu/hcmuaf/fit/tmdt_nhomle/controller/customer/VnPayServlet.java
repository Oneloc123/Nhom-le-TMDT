package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.PaymentAttempt;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.OrderStatus;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.PaymentService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.VnPayConfig;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.VnPayService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.Json;

import java.io.IOException;
import java.util.Map;

/** Endpoint công khai để VNPAY trả khách hàng về và gửi IPN server-to-server. */
@WebServlet(name = "vnPayServlet", value = "/vnpay/*")
public class VnPayServlet extends HttpServlet {

    private static final System.Logger LOGGER = System.getLogger(VnPayServlet.class.getName());
    private final PaymentService paymentService = new PaymentService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getPathInfo();
        if ("/ipn".equals(path)) {
            handleIpn(request, response);
            return;
        }
        if ("/return".equals(path)) {
            handleReturn(request, response);
            return;
        }
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void handleIpn(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");

        PaymentService.IpnResult result;
        try {
            if (!VnPayConfig.get().isConfigured()) {
                result = new PaymentService.IpnResult("99", "Merchant not configured", null, false);
            } else {
                VnPayService.Callback callback = paymentService.parseVnPayCallback(request.getParameterMap());
                result = paymentService.processVnPayIpn(callback);
            }
        } catch (RuntimeException error) {
            LOGGER.log(System.Logger.Level.ERROR, "Không xử lý được VNPAY IPN", error);
            result = new PaymentService.IpnResult("99", "Unknown error", null, false);
        }

        response.getWriter().write(Json.stringify(Map.of(
                "RspCode", result.responseCode(),
                "Message", result.message())));
    }

    private void handleReturn(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        String resultStatus = "error";
        String resultMessage = "Không thể xác minh kết quả thanh toán.";
        VnPayService.Callback callback = null;
        PaymentAttempt attempt = null;

        try {
            if (!VnPayConfig.get().isConfigured()) {
                resultMessage = "Cổng VNPAY chưa được cấu hình trên hệ thống.";
            } else {
                callback = paymentService.parseVnPayCallback(request.getParameterMap());
                if (!callback.validSignature()) {
                    resultMessage = "Chữ ký kết quả trả về từ VNPAY không hợp lệ.";
                } else {
                    attempt = paymentService.findVnPayAttempt(callback.transactionRef());
                    if (attempt == null) {
                        resultMessage = "Không tìm thấy giao dịch tương ứng trong hệ thống.";
                    } else if (callback.amount() == null
                            || attempt.amount().compareTo(callback.amount()) != 0) {
                        resultMessage = "Số tiền VNPAY trả về không khớp với đơn hàng.";
                    } else if ("PAID".equals(attempt.status())
                            || OrderStatus.PAYMENT_PAID.equals(attempt.orderPaymentStatus())) {
                        resultStatus = "success";
                        resultMessage = "Thanh toán VNPAY thành công.";
                    } else if (!callback.successful()) {
                        resultStatus = "failed";
                        resultMessage = VnPayService.callbackMessage(callback);
                    } else if ("FAILED".equals(attempt.status())) {
                        resultStatus = "failed";
                        resultMessage = "Giao dịch đã được hệ thống ghi nhận là không thành công.";
                    } else if ("REVIEW_REQUIRED".equals(attempt.status())) {
                        resultStatus = "pending";
                        resultMessage = "Giao dịch cần được kiểm tra trước khi xác nhận đơn hàng.";
                    } else {
                        resultStatus = "pending";
                        resultMessage = "VNPAY đã trả kết quả thành công; hệ thống đang chờ IPN xác nhận.";
                    }
                }
            }
        } catch (RuntimeException error) {
            LOGGER.log(System.Logger.Level.ERROR, "Không hiển thị được VNPAY return", error);
            resultMessage = "Hệ thống chưa thể kiểm tra giao dịch. Vui lòng xem lại lịch sử mua hàng.";
        }

        request.setAttribute("resultStatus", resultStatus);
        request.setAttribute("resultMessage", resultMessage);
        if (attempt != null) {
            request.setAttribute("orderCode", attempt.orderCode());
            request.setAttribute("amount", attempt.amount());
        }
        if (callback != null && callback.validSignature()) {
            request.setAttribute("transactionNo", callback.providerTransactionNo());
            request.setAttribute("bankCode", callback.bankCode());
            request.setAttribute("responseCode", callback.responseCode());
        }
        request.getRequestDispatcher("/customer/payment-result.jsp").forward(request, response);
    }
}
