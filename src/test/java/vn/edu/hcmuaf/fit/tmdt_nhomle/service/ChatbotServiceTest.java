package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatbotServiceTest {

    @Test
    void normalizesVietnameseAccentsAndPunctuation() {
        assertEquals("toi muon theo doi don hang",
                ChatbotService.normalize("Tôi muốn theo dõi đơn hàng!"));
        assertEquals("dang ky tai khoan",
                ChatbotService.normalize("Đăng ký tài khoản."));
    }

    @Test
    void identifiesOrderTrackingQuestionsWithAndWithoutAccents() {
        ChatbotService.Answer accented = ChatbotService.answer("Tôi muốn theo dõi đơn hàng");
        ChatbotService.Answer unaccented = ChatbotService.answer("toi muon theo doi don hang");

        assertEquals("order_tracking", accented.intent());
        assertEquals(accented.intent(), unaccented.intent());
        assertEquals("/order-tracking", accented.actions().get(0).path());
        assertFalse(accented.fallback());
    }

    @Test
    void directsRefundRequestsToSupportWithoutClaimingRefundWasCompleted() {
        ChatbotService.Answer answer = ChatbotService.answer("Tôi muốn hoàn tiền");

        assertEquals("return_refund", answer.intent());
        assertTrue(answer.text().contains("không thể tự xác nhận"));
        assertEquals("/support", answer.actions().get(0).path());
    }

    @Test
    void returnsFallbackForUnsupportedQuestions() {
        ChatbotService.Answer answer = ChatbotService.answer("câu hỏi không có trong kho tri thức");

        assertEquals("fallback", answer.intent());
        assertTrue(answer.fallback());
        assertFalse(answer.actions().isEmpty());
    }

    @Test
    void rejectsBlankQuestionWithFriendlyResponse() {
        ChatbotService.Answer answer = ChatbotService.answer("   ");

        assertEquals("fallback", answer.intent());
        assertTrue(answer.text().contains("nhập câu hỏi"));
    }
}
