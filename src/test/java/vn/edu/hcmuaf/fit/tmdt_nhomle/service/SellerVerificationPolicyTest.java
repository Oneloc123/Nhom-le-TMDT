package vn.edu.hcmuaf.fit.tmdt_nhomle.service;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SellerVerificationPolicyTest {
 @Test void decisionsNeedReasonsAndMapToExpectedStates() {
  assertEquals("VERIFIED", SellerVerificationPolicy.decide("PENDING","APPROVE","Đã kiểm tra hồ sơ").targetStatus());
  assertEquals("REJECTED", SellerVerificationPolicy.decide("PENDING","REJECT","Thiếu giấy tờ").targetStatus());
  assertEquals("PENDING", SellerVerificationPolicy.decide("PENDING","REQUEST_INFO","Bổ sung hồ sơ").targetStatus());
  assertEquals("ACTIVE", SellerVerificationPolicy.shopStatusFor("APPROVE"));
  assertEquals("INACTIVE", SellerVerificationPolicy.shopStatusFor("REJECT"));
  assertEquals("INACTIVE", SellerVerificationPolicy.shopStatusFor("REQUEST_INFO"));
 }
 @Test void rejectsBlankLongUnsupportedOrRepeatedDecisions() {
  assertThrows(IllegalArgumentException.class, () -> SellerVerificationPolicy.decide("PENDING","REJECT"," "));
  assertThrows(IllegalArgumentException.class, () -> SellerVerificationPolicy.decide("PENDING","DELETE","Có lý do"));
  assertThrows(IllegalArgumentException.class, () -> SellerVerificationPolicy.decide("VERIFIED","REJECT","Cần từ chối"));
  assertThrows(IllegalArgumentException.class, () -> SellerVerificationPolicy.decide("PENDING","APPROVE","x".repeat(1001)));
  assertThrows(IllegalArgumentException.class, () -> SellerVerificationPolicy.shopStatusFor("DELETE"));
 }}