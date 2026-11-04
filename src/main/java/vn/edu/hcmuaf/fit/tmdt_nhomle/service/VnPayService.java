package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Order;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Tạo URL và xác minh callback VNPAY Payment Gateway API 2.1.0. */
public final class VnPayService {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNPAY_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Duration CHECKOUT_TTL = Duration.ofMinutes(15);

    private final VnPayConfig config;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public VnPayService(VnPayConfig config) {
        this(config, Clock.systemUTC());
    }

    public VnPayService(VnPayConfig config, Clock clock) {
        this.config = config;
        this.clock = clock;
    }

    public Checkout createCheckout(Order order, String clientIp, String returnUrl) {
        config.requireConfigured();
        if (order == null || order.getTotalAmount() == null
                || order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Đơn hàng hoặc số tiền thanh toán không hợp lệ.");
        }

        Instant now = clock.instant();
        String transactionRef = "PK" + order.getOrderId() + now.toEpochMilli()
                + String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        String createDate = VNPAY_TIME.format(now.atZone(VIETNAM_ZONE));
        String expireDate = VNPAY_TIME.format(now.plus(CHECKOUT_TTL).atZone(VIETNAM_ZONE));

        Map<String, String> parameters = new TreeMap<>();
        parameters.put("vnp_Version", "2.1.0");
        parameters.put("vnp_Command", "pay");
        parameters.put("vnp_TmnCode", config.tmnCode());
        parameters.put("vnp_Amount", toVnPayAmount(order.getTotalAmount()));
        parameters.put("vnp_CreateDate", createDate);
        parameters.put("vnp_CurrCode", "VND");
        parameters.put("vnp_IpAddr", normalizeIp(clientIp));
        parameters.put("vnp_Locale", "vn");
        parameters.put("vnp_OrderInfo", asciiOrderInfo(order.getOrderCode()));
        parameters.put("vnp_OrderType", "other");
        parameters.put("vnp_ReturnUrl", returnUrl);
        parameters.put("vnp_ExpireDate", expireDate);
        parameters.put("vnp_TxnRef", transactionRef);

        String query = canonicalize(parameters);
        String signature = hmacSha512(config.hashSecret(), query);
        return new Checkout(transactionRef,
                config.paymentUrl() + "?" + query + "&vnp_SecureHash=" + signature);
    }

    public Callback parseCallback(Map<String, String[]> requestParameters) {
        Map<String, String> values = new LinkedHashMap<>();
        requestParameters.forEach((key, raw) -> {
            if (key != null && key.startsWith("vnp_") && raw != null && raw.length > 0
                    && raw[0] != null && !raw[0].isBlank()) {
                values.put(key, raw[0]);
            }
        });

        String receivedHash = values.remove("vnp_SecureHash");
        values.remove("vnp_SecureHashType");
        String expectedHash = hmacSha512(config.hashSecret(), canonicalize(values));
        boolean validSignature = receivedHash != null && MessageDigest.isEqual(
                expectedHash.getBytes(StandardCharsets.US_ASCII),
                receivedHash.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
        boolean validMerchant = config.tmnCode().equals(values.get("vnp_TmnCode"));

        BigDecimal amount = parseAmount(values.get("vnp_Amount"));
        return new Callback(validSignature && validMerchant,
                values.get("vnp_TxnRef"), amount,
                values.get("vnp_ResponseCode"), values.get("vnp_TransactionStatus"),
                values.get("vnp_TransactionNo"), values.get("vnp_BankCode"),
                values.get("vnp_CardType"), values.get("vnp_PayDate"));
    }

    public static String responseMessage(String responseCode) {
        if (responseCode == null) return "Không nhận được mã phản hồi từ VNPAY.";
        return switch (responseCode) {
            case "00" -> "Giao dịch thành công.";
            case "07" -> "Giao dịch bị nghi ngờ bất thường và cần được kiểm tra.";
            case "09" -> "Thẻ hoặc tài khoản chưa đăng ký Internet Banking.";
            case "10" -> "Thông tin xác thực không đúng quá số lần cho phép.";
            case "11" -> "Giao dịch đã hết thời gian chờ thanh toán.";
            case "12" -> "Thẻ hoặc tài khoản đang bị khóa.";
            case "13" -> "Mã OTP không chính xác.";
            case "24" -> "Bạn đã hủy giao dịch thanh toán.";
            case "51" -> "Tài khoản không đủ số dư.";
            case "65" -> "Tài khoản đã vượt hạn mức giao dịch trong ngày.";
            case "75" -> "Ngân hàng thanh toán đang bảo trì.";
            case "79" -> "Bạn đã nhập sai mật khẩu thanh toán quá số lần quy định.";
            default -> "Giao dịch không thành công (mã " + responseCode + ").";
        };
    }

    public static String callbackMessage(Callback callback) {
        if (callback == null) return "Không nhận được kết quả từ VNPAY.";
        if (!"00".equals(callback.responseCode())) {
            return responseMessage(callback.responseCode());
        }
        if (!"00".equals(callback.transactionStatus())) {
            String status = callback.transactionStatus();
            return "VNPAY chưa xác nhận giao dịch thành công"
                    + (status == null ? "." : " (trạng thái " + status + ").");
        }
        return responseMessage("00");
    }

    static String canonicalize(Map<String, String> parameters) {
        StringBuilder result = new StringBuilder();
        new TreeMap<>(parameters).forEach((key, value) -> {
            if (value == null || value.isBlank()) return;
            if (!result.isEmpty()) result.append('&');
            result.append(encode(key)).append('=').append(encode(value));
        });
        return result.toString();
    }

    static String hmacSha512(String secret, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            hmac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) hex.append(String.format(Locale.ROOT, "%02x", value));
            return hex.toString();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("JVM không hỗ trợ HMAC-SHA512.", e);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String toVnPayAmount(BigDecimal amount) {
        try {
            return amount.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).toPlainString();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Số tiền VND không hợp lệ.", e);
        }
    }

    private static BigDecimal parseAmount(String raw) {
        try {
            return raw == null ? null : new BigDecimal(raw).movePointLeft(2);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String asciiOrderInfo(String orderCode) {
        String safeCode = orderCode == null ? "" : orderCode.replaceAll("[^A-Za-z0-9]", "");
        return "Thanh toan don hang " + safeCode;
    }

    private static String normalizeIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) return "127.0.0.1";
        String value = clientIp.trim();
        return value.length() <= 45 ? value : value.substring(0, 45);
    }

    public record Checkout(String transactionRef, String checkoutUrl) {
    }

    public record Callback(boolean validSignature, String transactionRef, BigDecimal amount,
                           String responseCode, String transactionStatus,
                           String providerTransactionNo, String bankCode,
                           String cardType, String payDate) {
        public boolean successful() {
            return validSignature && "00".equals(responseCode) && "00".equals(transactionStatus);
        }
    }
}
