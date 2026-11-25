package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import org.junit.jupiter.api.Test;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.CustomPrintRequest;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.QuoteRequest;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.Json;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZaloPayServiceTest {

    private static final ZaloPayConfig CONFIG = new ZaloPayConfig(
            "2554",
            "sdngKKJmqEMzvh5QQcdD2A9XBSKUNaYn",
            "trMrHtvjo6myautxDUiAcYsVtaeQ8nhf",
            "https://sb-openapi.zalopay.vn/v2/create",
            "https://sb-openapi.zalopay.vn/v2/query",
            "",
            "");

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-11-25T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void createsSignedDepositOrderWith30PercentAmountAndEmbedRedirectUrl() {
        QuoteRequest quote = new QuoteRequest();
        quote.setQuoteRequestId(5L);
        quote.setProductName("Đế điện thoại mô-đun");
        quote.setQuotedAmount(new BigDecimal("10000000"));
        BigDecimal deposit = QuoteRequest.calculateDepositAmount(quote.getQuotedAmount());
        assertEquals(new BigDecimal("3000000"), deposit);

        Map<String, String> capturedForm = new LinkedHashMap<>();
        ZaloPayService service = new ZaloPayService(CONFIG, FIXED_CLOCK, (url, form) -> {
            assertEquals("https://sb-openapi.zalopay.vn/v2/create", url);
            capturedForm.putAll(form);
            return "{\"return_code\":1,\"return_message\":\"Giao dịch thành công\","
                    + "\"sub_return_code\":1,\"sub_return_message\":\"\","
                    + "\"zp_trans_token\":\"token-abc\",\"order_url\":\"https://sbgateway.zalopay.vn/openinapp?order=abc\"}";
        });

        ZaloPayService.CreateOrderResult result = service.createQuoteDepositOrder(
                quote, 42L, deposit,
                "http://localhost:8080/zalopay/return",
                "");

        assertEquals("https://sbgateway.zalopay.vn/openinapp?order=abc", result.orderUrl());
        assertEquals("token-abc", result.zpTransToken());
        assertTrue(result.appTransId().startsWith("261125_Q5_"));
        assertEquals("2554", capturedForm.get("app_id"));
        assertEquals("3000000", capturedForm.get("amount"));
        assertFalse(capturedForm.containsKey("callback_url"));

        String expectedMacInput = capturedForm.get("app_id") + "|" + capturedForm.get("app_trans_id")
                + "|" + capturedForm.get("app_user") + "|" + capturedForm.get("amount")
                + "|" + capturedForm.get("app_time") + "|" + capturedForm.get("embed_data")
                + "|" + capturedForm.get("item");
        assertEquals(ZaloPayService.hmacSha256(CONFIG.key1(), expectedMacInput), capturedForm.get("mac"));

        Map<String, Object> embed = Json.parseObject(capturedForm.get("embed_data"));
        assertEquals("http://localhost:8080/zalopay/return", embed.get("redirecturl"));
    }

    @Test
    void verifiesCallbackMacWithKey2AndRejectsTampering() {
        ZaloPayService service = new ZaloPayService(CONFIG, FIXED_CLOCK, (u, f) -> "{}");
        String dataJson = "{\"app_id\":2554,\"app_trans_id\":\"261125_Q5_123456\","
                + "\"zp_trans_id\":261125000000001,\"amount\":3000000,\"channel\":38}";
        String mac = ZaloPayService.hmacSha256(CONFIG.key2(), dataJson);
        String body = Json.stringify(Map.of("data", dataJson, "mac", mac, "type", 1));

        ZaloPayService.CallbackData parsed = service.parseCallback(body);
        assertTrue(parsed.validMac());
        assertEquals("261125_Q5_123456", parsed.appTransId());
        assertEquals("261125000000001", parsed.zpTransId());
        assertEquals(new BigDecimal("3000000"), parsed.amount());
        assertEquals(38, parsed.channel());

        String tamperedBody = Json.stringify(Map.of("data", dataJson.replace("3000000", "1000"), "mac", mac));
        assertFalse(service.parseCallback(tamperedBody).validMac());
    }

    @Test
    void queriesOrderWithKey1SignatureAndParsesPaidResult() {
        Map<String, String> capturedForm = new LinkedHashMap<>();
        ZaloPayService service = new ZaloPayService(CONFIG, FIXED_CLOCK, (url, form) -> {
            assertEquals("https://sb-openapi.zalopay.vn/v2/query", url);
            capturedForm.putAll(form);
            return "{\"return_code\":1,\"return_message\":\"Giao dịch thành công\","
                    + "\"sub_return_code\":1,\"is_processing\":false,"
                    + "\"amount\":3000000,\"zp_trans_id\":261125000000002}";
        });

        ZaloPayService.QueryOrderResult query = service.queryOrder("261125_Q5_123456");
        assertTrue(query.paid());
        assertFalse(query.isProcessing());
        assertEquals(new BigDecimal("3000000"), query.amount());
        assertEquals("261125000000002", query.zpTransId());
        assertEquals(
                ZaloPayService.hmacSha256(CONFIG.key1(), "2554|261125_Q5_123456|" + CONFIG.key1()),
                capturedForm.get("mac"));
    }

    @Test
    void verifiesRedirectChecksumUsingKey2() {
        ZaloPayService service = new ZaloPayService(CONFIG, FIXED_CLOCK, (u, f) -> "{}");
        String input = "2554|261125_Q5_123456|38||3000000|0|1";
        String checksum = ZaloPayService.hmacSha256(CONFIG.key2(), input);

        Map<String, String[]> params = new LinkedHashMap<>();
        params.put("appid", new String[]{"2554"});
        params.put("apptransid", new String[]{"261125_Q5_123456"});
        params.put("pmcid", new String[]{"38"});
        params.put("bankcode", new String[]{""});
        params.put("amount", new String[]{"3000000"});
        params.put("discountamount", new String[]{"0"});
        params.put("status", new String[]{"1"});
        params.put("checksum", new String[]{checksum});

        assertTrue(service.verifyRedirectChecksum(params));
        params.put("amount", new String[]{"100"});
        assertFalse(service.verifyRedirectChecksum(params));
    }

    @Test
    void throwsPaymentExceptionWhenCreateReturnsError() {
        QuoteRequest quote = new QuoteRequest();
        quote.setQuoteRequestId(5L);
        quote.setQuotedAmount(new BigDecimal("10000000"));
        ZaloPayService service = new ZaloPayService(CONFIG, FIXED_CLOCK,
                (u, f) -> "{\"return_code\":2,\"sub_return_code\":-68,\"return_message\":\"MAC invalid\"}");

        PaymentException ex = assertThrows(PaymentException.class, () ->
                service.createQuoteDepositOrder(quote, 1L, new BigDecimal("3000000"),
                        "http://localhost:8080/zalopay/return", ""));
        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("MAC invalid"));
    }

    @Test
    void createsSignedDepositOrderForCustomPrintRequest() {
        CustomPrintRequest cp = new CustomPrintRequest(
                7L, 15L, 2L, "PK3DF0855DF5692D", 6L, "Hanoi Layer Lab",
                "QUOTE", "FDM", "PLA", 20, new BigDecimal("0.2000"), 1,
                null, null, null, null, new BigDecimal("2000000"), "ok", 3, null,
                "QUOTED", null, true, "test_3d_torus.obj", "designs/abc.obj",
                "model/obj", 1024L, "hash", "READY", "mm",
                new BigDecimal("2.7"), new BigDecimal("2.7"), new BigDecimal("0.7"),
                new BigDecimal("1.0"), new BigDecimal("2.0"), 100L, null,
                null, null, null, null, null, null);
        assertTrue(cp.isDepositable());
        BigDecimal deposit = cp.getRequiredDepositAmount();
        assertEquals(new BigDecimal("600000"), deposit);
        assertEquals(new BigDecimal("1400000"), cp.getRemainingAmount());

        Map<String, String> capturedForm = new LinkedHashMap<>();
        ZaloPayService service = new ZaloPayService(CONFIG, FIXED_CLOCK, (url, form) -> {
            capturedForm.putAll(form);
            return "{\"return_code\":1,\"return_message\":\"OK\",\"zp_trans_token\":\"tk\",\"order_url\":\"https://sbgateway.zalopay.vn/pay?id=7\"}";
        });

        ZaloPayService.CreateOrderResult result = service.createCustomPrintDepositOrder(
                cp, 15L, deposit, "http://localhost:8080/zalopay/return", "");
        assertTrue(result.appTransId().startsWith("261125_CP7_"));
        assertEquals("600000", capturedForm.get("amount"));
        assertEquals("https://sbgateway.zalopay.vn/pay?id=7", result.orderUrl());
    }
}
