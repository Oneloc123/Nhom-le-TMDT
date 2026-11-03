package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import java.util.Locale;

public final class ChangeRequestPolicy {
    public enum Decision { AUTO_APPLY, SHOP_APPROVAL, SHOP_DECIDES, BLOCKED }

    private ChangeRequestPolicy() { }

    public static Decision decide(boolean hasProductionUpdate, String stage, int progressPercent) {
        if (!hasProductionUpdate) return Decision.AUTO_APPLY;
        String normalized = stage == null ? "" : stage.trim().toUpperCase(Locale.ROOT);
        if (normalized.equals("READY_FOR_SHIPPING") || normalized.equals("SHIPPED")
                || normalized.equals("DELIVERED") || normalized.equals("COMPLETED")) {
            return Decision.BLOCKED;
        }
        if (normalized.equals("POST_PROCESSING") || (normalized.equals("PRINTING") && progressPercent >= 20)) {
            return Decision.SHOP_DECIDES;
        }
        if (normalized.equals("ACCEPTED") || normalized.equals("PRINTING")) return Decision.SHOP_APPROVAL;
        return Decision.SHOP_APPROVAL;
    }
}
