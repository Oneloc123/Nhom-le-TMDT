package vn.edu.hcmuaf.fit.tmdt_nhomle.service;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
class SupportSlaPolicyTest {
 @Test void responseWindowsDependOnPriority() {
  assertEquals(4,SupportSlaPolicy.responseWindowHours("HIGH"));
  assertEquals(24,SupportSlaPolicy.responseWindowHours("NORMAL"));
  assertEquals(72,SupportSlaPolicy.responseWindowHours("LOW"));
  assertThrows(IllegalArgumentException.class,()->SupportSlaPolicy.responseWindowHours("URGENT"));
 }
 @Test void waitingCustomerPausesClockAndClosedCanOnlyReopenExplicitly() {
  assertTrue(SupportSlaPolicy.isPaused("WAITING_CUSTOMER"));
  assertFalse(SupportSlaPolicy.isClockRunning("WAITING_CUSTOMER"));
  assertTrue(SupportSlaPolicy.canTransition("IN_PROGRESS","WAITING_CUSTOMER"));
  assertTrue(SupportSlaPolicy.canTransition("CLOSED","OPEN"));
  assertFalse(SupportSlaPolicy.canTransition("CLOSED","RESOLVED"));
  assertFalse(SupportSlaPolicy.canTransition("RESOLVED","WAITING_CUSTOMER"));
 }
 @Test void checksOverdueAndDueSoonDeadlines() {
  LocalDateTime now=LocalDateTime.of(2026,11,17,10,0);
  assertTrue(SupportSlaPolicy.isOverdue("OPEN",now.minusSeconds(1),now));
  assertFalse(SupportSlaPolicy.isOverdue("RESOLVED",now.minusHours(10),now));
  assertTrue(SupportSlaPolicy.isDueSoon("IN_PROGRESS",now.plusHours(4),now));
  assertFalse(SupportSlaPolicy.isDueSoon("IN_PROGRESS",now.plusHours(5),now));
  assertFalse(SupportSlaPolicy.isDueSoon("WAITING_CUSTOMER",now.plusHours(1),now));
 }}