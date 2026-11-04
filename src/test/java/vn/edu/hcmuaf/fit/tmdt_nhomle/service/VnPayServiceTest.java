package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import org.junit.jupiter.api.Test;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Order;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VnPayServiceTest {

    private static final VnPayConfig CONFIG = new VnPayConfig(
            "TESTCODE", "test-secret-key",
            "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html", "");
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-11-03T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void createsSignedCheckoutUsingVnPayUnits() {
        Order order = order();
        VnPayService service = new VnPayService(CONFIG, CLOCK);

        VnPayService.Checkout checkout = service.createCheckout(order, "127.0.0.1",
                "https://shop.example/vnpay/return");
        URI uri = URI.create(checkout.checkoutUrl());
        Map<String, String> query = parseQuery(uri.getRawQuery());

        assertEquals("sandbox.vnpayment.vn", uri.getHost());
        assertTrue(checkout.transactionRef().startsWith("PK12"));
        assertEquals(checkout.transactionRef(), query.get("vnp_TxnRef"));
        assertEquals("2.1.0", query.get("vnp_Version"));
        assertEquals("58000000", query.get("vnp_Amount"));
        assertEquals("20261103170000", query.get("vnp_CreateDate"));
        assertEquals("20261103171500", query.get("vnp_ExpireDate"));
        assertEquals("https://shop.example/vnpay/return", query.get("vnp_ReturnUrl"));
        assertNotNull(query.get("vnp_SecureHash"));

        String receivedSignature = query.remove("vnp_SecureHash");
        assertEquals(VnPayService.hmacSha512(CONFIG.hashSecret(),
                VnPayService.canonicalize(query)), receivedSignature);
    }

    @Test
    void acceptsValidSuccessCallbackAndRejectsTampering() {
        VnPayService service = new VnPayService(CONFIG, CLOCK);
        Map<String, String> values = new LinkedHashMap<>();
        values.put("vnp_Amount", "58000000");
        values.put("vnp_BankCode", "NCB");
        values.put("vnp_CardType", "ATM");
        values.put("vnp_PayDate", "20261103170500");
        values.put("vnp_ResponseCode", "00");
        values.put("vnp_TmnCode", "TESTCODE");
        values.put("vnp_TransactionNo", "15000001");
        values.put("vnp_TransactionStatus", "00");
        values.put("vnp_TxnRef", "PK1217621640000001234");
        String signature = VnPayService.hmacSha512(CONFIG.hashSecret(),
                VnPayService.canonicalize(values));

        Map<String, String[]> callbackParameters = asRequestParameters(values);
        callbackParameters.put("vnp_SecureHash", new String[]{signature});
        VnPayService.Callback callback = service.parseCallback(callbackParameters);

        assertTrue(callback.validSignature());
        assertTrue(callback.successful());
        assertEquals(new BigDecimal("580000.00"), callback.amount());
        assertEquals("15000001", callback.providerTransactionNo());

        callbackParameters.put("vnp_Amount", new String[]{"1"});
        VnPayService.Callback tampered = service.parseCallback(callbackParameters);
        assertFalse(tampered.validSignature());
        assertFalse(tampered.successful());
    }

    @Test
    void requiresBothResponseAndTransactionStatusToBeSuccessful() {
        VnPayService service = new VnPayService(CONFIG, CLOCK);
        Map<String, String> values = new LinkedHashMap<>();
        values.put("vnp_Amount", "58000000");
        values.put("vnp_ResponseCode", "00");
        values.put("vnp_TmnCode", "TESTCODE");
        values.put("vnp_TransactionStatus", "01");
        values.put("vnp_TxnRef", "PK12FAILED");
        Map<String, String[]> parameters = asRequestParameters(values);
        parameters.put("vnp_SecureHash", new String[]{VnPayService.hmacSha512(
                CONFIG.hashSecret(), VnPayService.canonicalize(values))});

        VnPayService.Callback callback = service.parseCallback(parameters);

        assertTrue(callback.validSignature());
        assertFalse(callback.successful());
        assertTrue(VnPayService.callbackMessage(callback).contains("trạng thái 01"));
    }

    private static Order order() {
        Order order = new Order();
        order.setOrderId(12L);
        order.setOrderCode("PK-2026-0012");
        order.setTotalAmount(new BigDecimal("580000"));
        return order;
    }

    private static Map<String, String[]> asRequestParameters(Map<String, String> values) {
        Map<String, String[]> result = new LinkedHashMap<>();
        values.forEach((key, value) -> result.put(key, new String[]{value}));
        return result;
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String pair : rawQuery.split("&")) {
            String[] parts = pair.split("=", 2);
            result.put(decode(parts[0]), parts.length > 1 ? decode(parts[1]) : "");
        }
        return result;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
