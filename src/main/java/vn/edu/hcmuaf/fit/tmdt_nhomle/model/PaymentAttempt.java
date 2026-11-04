package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

import java.math.BigDecimal;

public record PaymentAttempt(long paymentId, long orderId, long userId, String orderCode,
                             BigDecimal amount, String status, String transactionRef,
                             String orderStatus, String orderPaymentStatus) {
}
