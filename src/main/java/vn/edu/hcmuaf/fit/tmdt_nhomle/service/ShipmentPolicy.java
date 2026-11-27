package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Canonical Admin shipment states; accepts legacy Vietnamese labels during migration. */
public final class ShipmentPolicy {
    public static final Set<String> STATES = Set.of(
            "PREPARING", "SHIPPED", "IN_TRANSIT", "DELIVERED", "FAILED", "RETURNED");
    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("READY", "PREPARING"),
            Map.entry("PENDING", "PREPARING"),
            Map.entry("PREPARING", "PREPARING"),
            Map.entry("ĐANG CHUẨN BỊ", "PREPARING"),
            Map.entry("CHỜ LẤY HÀNG", "PREPARING"),
            Map.entry("SHIPPED", "SHIPPED"),
            Map.entry("ĐÃ GỬI", "SHIPPED"),
            Map.entry("ĐÃ LẤY HÀNG", "SHIPPED"),
            Map.entry("IN_TRANSIT", "IN_TRANSIT"),
            Map.entry("ĐANG GIAO", "IN_TRANSIT"),
            Map.entry("ĐANG VẬN CHUYỂN", "IN_TRANSIT"),
            Map.entry("DELIVERED", "DELIVERED"),
            Map.entry("COMPLETED", "DELIVERED"),
            Map.entry("ĐÃ GIAO", "DELIVERED"),
            Map.entry("FAILED", "FAILED"),
            Map.entry("DELIVERY_FAILED", "FAILED"),
            Map.entry("GIAO THẤT BẠI", "FAILED"),
            Map.entry("RETURNED", "RETURNED"),
            Map.entry("CANCELLED", "RETURNED"),
            Map.entry("ĐÃ HOÀN", "RETURNED"),
            Map.entry("ĐÃ TRẢ", "RETURNED"),
            Map.entry("ĐÃ HỦY", "RETURNED")
    );

    private ShipmentPolicy() { }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) return "PREPARING";
        String key = raw.trim().toUpperCase(Locale.ROOT);
        String state = ALIASES.get(key);
        if (state == null) throw new IllegalArgumentException("Trạng thái vận đơn không hợp lệ.");
        return state;
    }

    public static boolean canTransition(String rawFrom, String rawTo) {
        final String from;
        final String to;
        try {
            from = normalize(rawFrom);
            to = normalize(rawTo);
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (from.equals(to)) return true; // permit correcting carrier/tracking metadata without reopening status.
        return switch (from) {
            case "PREPARING" -> Set.of("SHIPPED", "IN_TRANSIT", "FAILED", "RETURNED").contains(to);
            case "SHIPPED" -> Set.of("IN_TRANSIT", "DELIVERED", "FAILED", "RETURNED").contains(to);
            case "IN_TRANSIT" -> Set.of("DELIVERED", "FAILED", "RETURNED").contains(to);
            case "FAILED" -> Set.of("SHIPPED", "IN_TRANSIT", "RETURNED").contains(to);
            case "DELIVERED", "RETURNED" -> false;
            default -> false;
        };
    }

    public static boolean requiresTracking(String status) {
        return Set.of("SHIPPED", "IN_TRANSIT", "DELIVERED").contains(normalize(status));
    }

    public static boolean isTerminal(String status) {
        return Set.of("DELIVERED", "RETURNED").contains(normalize(status));
    }
}
