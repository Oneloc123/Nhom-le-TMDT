package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

/** Response SLA: HIGH=4h, NORMAL=24h, LOW=72h; waiting on the customer pauses SLA. */
public final class SupportSlaPolicy {
    public static final int DUE_SOON_HOURS = 4;
    private static final Set<String> STATUSES = Set.of("OPEN", "IN_PROGRESS", "WAITING_CUSTOMER", "RESOLVED", "CLOSED");

    private SupportSlaPolicy() { }

    public static int responseWindowHours(String priority) {
        String value = priority == null ? "" : priority.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "HIGH" -> 4;
            case "NORMAL" -> 24;
            case "LOW" -> 72;
            default -> throw new IllegalArgumentException("Mức ưu tiên ticket không hợp lệ.");
        };
    }

    public static boolean isValidStatus(String status) {
        return status != null && STATUSES.contains(status.trim().toUpperCase(Locale.ROOT));
    }

    public static boolean canTransition(String from, String to) {
        String source = from == null ? "" : from.trim().toUpperCase(Locale.ROOT);
        String target = to == null ? "" : to.trim().toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(source) || !STATUSES.contains(target)) return false;
        if (source.equals(target)) return true;
        return switch (source) {
            case "OPEN", "IN_PROGRESS", "WAITING_CUSTOMER" ->
                    Set.of("OPEN", "IN_PROGRESS", "WAITING_CUSTOMER", "RESOLVED", "CLOSED").contains(target);
            case "RESOLVED" -> Set.of("OPEN", "IN_PROGRESS", "CLOSED").contains(target);
            case "CLOSED" -> Set.of("OPEN").contains(target);
            default -> false;
        };
    }

    public static boolean isClockRunning(String status) {
        return "OPEN".equalsIgnoreCase(status) || "IN_PROGRESS".equalsIgnoreCase(status);
    }

    public static boolean isPaused(String status) {
        return "WAITING_CUSTOMER".equalsIgnoreCase(status);
    }

    public static boolean isOverdue(String status, LocalDateTime dueAt, LocalDateTime now) {
        return isClockRunning(status) && dueAt != null && now != null && dueAt.isBefore(now);
    }

    public static boolean isDueSoon(String status, LocalDateTime dueAt, LocalDateTime now) {
        return isClockRunning(status) && dueAt != null && now != null
                && !dueAt.isBefore(now) && !dueAt.isAfter(now.plusHours(DUE_SOON_HOURS));
    }
}
