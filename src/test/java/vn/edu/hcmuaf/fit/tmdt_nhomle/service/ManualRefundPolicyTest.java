package vn.edu.hcmuaf.fit.tmdt_nhomle.service;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
class ManualRefundPolicyTest {
 @Test void validatesSuccessfulRefundInputs() {
  var r=ManualRefundPolicy.validateCompletion("BANK_TRANSFER","TXN-12345","Đã đối chiếu xác nhận chuyển tiền",new BigDecimal("123456.78"));
  assertEquals("BANK_TRANSFER",r.method()); assertEquals("TXN-12345",r.reference());
  assertEquals(new BigDecimal("123456.78"),r.amount());
 }
 @Test void rejectsInvalidMethodReferenceNoteAmountAndFailureNote() {
  assertThrows(IllegalArgumentException.class,()->ManualRefundPolicy.validateCompletion("UNKNOWN","TXN-1","Ghi chú",new BigDecimal("10")));
  assertThrows(IllegalArgumentException.class,()->ManualRefundPolicy.validateCompletion("BANK_TRANSFER","","Ghi chú",new BigDecimal("10")));
  assertThrows(IllegalArgumentException.class,()->ManualRefundPolicy.validateCompletion("BANK_TRANSFER","TXN-1","x",new BigDecimal("10")));
  assertThrows(IllegalArgumentException.class,()->ManualRefundPolicy.validateCompletion("BANK_TRANSFER","TXN-1","Ghi chú",BigDecimal.ZERO));
  assertThrows(IllegalArgumentException.class,()->ManualRefundPolicy.validateFailureNote(" "));
 }
 @Test void onlyRefundRequiredCanBeMarkedRefunded() {
  assertTrue(ManualRefundPolicy.shouldMarkPaymentRefunded("REFUND_REQUIRED"));
  assertFalse(ManualRefundPolicy.shouldMarkPaymentRefunded("PAID"));
  assertFalse(ManualRefundPolicy.shouldMarkPaymentRefunded("REFUNDED"));
 }}