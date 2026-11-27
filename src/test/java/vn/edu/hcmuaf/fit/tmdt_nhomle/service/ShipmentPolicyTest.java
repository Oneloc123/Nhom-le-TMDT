package vn.edu.hcmuaf.fit.tmdt_nhomle.service;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ShipmentPolicyTest {
 @Test void normalizesStatusesAndRejectsUnknownValues() {
  assertEquals("DELIVERED", ShipmentPolicy.normalize("Đã giao"));
  assertEquals("IN_TRANSIT", ShipmentPolicy.normalize("IN_TRANSIT"));
  assertEquals("PREPARING", ShipmentPolicy.normalize(null));
  assertThrows(IllegalArgumentException.class, () -> ShipmentPolicy.normalize("UNKNOWN"));
 }
 @Test void validatesTransitionsAndTerminalStates() {
  assertTrue(ShipmentPolicy.canTransition("PREPARING","SHIPPED"));
  assertTrue(ShipmentPolicy.canTransition("SHIPPED","IN_TRANSIT"));
  assertTrue(ShipmentPolicy.canTransition("IN_TRANSIT","DELIVERED"));
  assertTrue(ShipmentPolicy.canTransition("FAILED","SHIPPED"));
  assertFalse(ShipmentPolicy.canTransition("PREPARING","DELIVERED"));
  assertFalse(ShipmentPolicy.canTransition("DELIVERED","IN_TRANSIT"));
  assertFalse(ShipmentPolicy.canTransition("RETURNED","SHIPPED"));
 }
 @Test void requiresTrackingForDispatchAndDelivery() {
  assertFalse(ShipmentPolicy.requiresTracking("PREPARING"));
  assertTrue(ShipmentPolicy.requiresTracking("SHIPPED"));
  assertTrue(ShipmentPolicy.requiresTracking("DELIVERED"));
  assertTrue(ShipmentPolicy.isTerminal("DELIVERED"));
  assertTrue(ShipmentPolicy.isTerminal("RETURNED"));
 }
}