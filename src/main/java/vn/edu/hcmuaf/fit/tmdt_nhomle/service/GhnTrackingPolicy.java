package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Canonical mapping for GHN callbacks and periodic order-info polling. */
public final class GhnTrackingPolicy {
    public static final Duration CUSTOMER_CONFIRMATION_WINDOW = Duration.ofHours(72);
    public static final Duration CUSTOMER_REMINDER_BEFORE_DEADLINE = Duration.ofHours(24);
    private static final Map<String, String> STATUS_MAP = Map.ofEntries(
            Map.entry("ready_to_pick", "PREPARING"),
            Map.entry("picking", "PREPARING"),
            Map.entry("money_collect_picking", "PREPARING"),
            Map.entry("picked", "SHIPPED"),
            Map.entry("storing", "IN_TRANSIT"),
            Map.entry("sorting", "IN_TRANSIT"),
            Map.entry("transporting", "IN_TRANSIT"),
            Map.entry("delivering", "IN_TRANSIT"),
            Map.entry("money_collect_delivering", "IN_TRANSIT"),
            Map.entry("delivered", "DELIVERED"),
            Map.entry("delivery_fail", "FAILED"),
            Map.entry("waiting_to_return", "FAILED"),
            Map.entry("return", "FAILED"),
            Map.entry("return_transporting", "FAILED"),
            Map.entry("return_sorting", "FAILED"),
            Map.entry("returning", "FAILED"),
            Map.entry("return_fail", "FAILED"),
            Map.entry("returned", "RETURNED"),
            Map.entry("cancel", "RETURNED"),
            Map.entry("exception", "FAILED"),
            Map.entry("lost", "FAILED"),
            Map.entry("damage", "FAILED"),
            Map.entry("scrap", "FAILED")
    );

    private GhnTrackingPolicy() { }

    public static String normalizeRawStatus(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    public static String mapStatus(String raw) {
        return STATUS_MAP.get(normalizeRawStatus(raw));
    }

    /** Allows skipped intermediate carrier events while never reopening a terminal shipment. */
    public static boolean canApplyCarrierStatus(String current, String next) {
        if (current == null || next == null) return false;
        String from = current.trim().toUpperCase(Locale.ROOT);
        String to = next.trim().toUpperCase(Locale.ROOT);
        if (from.equals(to)) return true;
        return switch (from) {
            case "PREPARING" -> Set.of("SHIPPED", "IN_TRANSIT", "DELIVERED", "FAILED", "RETURNED").contains(to);
            case "SHIPPED" -> Set.of("IN_TRANSIT", "DELIVERED", "FAILED", "RETURNED").contains(to);
            case "IN_TRANSIT" -> Set.of("DELIVERED", "FAILED", "RETURNED").contains(to);
            case "FAILED" -> Set.of("SHIPPED", "IN_TRANSIT", "DELIVERED", "RETURNED").contains(to);
            case "DELIVERED", "RETURNED" -> false;
            default -> false;
        };
    }

    public static boolean isTerminal(String status) {
        return "DELIVERED".equals(status) || "RETURNED".equals(status);
    }

    public static String providerForCarrierLabel(String rawCarrier) {
        String value = rawCarrier == null ? "" : rawCarrier.trim().toUpperCase(Locale.ROOT);
        if (value.equals("GHN") || value.contains("GIAO HÀNG NHANH")
                || value.contains("GIAO HANG NHANH") || value.startsWith("GHN ")) return "GHN";
        return "MANUAL";
    }

    public static boolean shouldTrackEventType(String rawType) {
        String type = rawType == null ? "" : rawType.trim().toLowerCase(Locale.ROOT);
        return Set.of("create", "switch_status", "update_weight", "update_cod",
                "update_fee", "update_payment_type", "cod", "update_partial_return").contains(type);
    }
}
