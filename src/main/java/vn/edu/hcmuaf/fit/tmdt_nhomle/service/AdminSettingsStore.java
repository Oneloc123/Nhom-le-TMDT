package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Server-side settings for the admin workspace; currently held in memory. */
public final class AdminSettingsStore {
    private static final Map<String, Object> SETTINGS = new LinkedHashMap<>();

    static {
        SETTINGS.put("platformName", "Print3D Studio");
        SETTINGS.put("currency", "VND");
        SETTINGS.put("timezone", "Asia/Ho_Chi_Minh");
        SETTINGS.put("requiredDeposit", 8_000_000L);
        SETTINGS.put("requireProductReview", true);
        SETTINGS.put("commissionRate", new BigDecimal("10.0"));
        SETTINGS.put("printingTechnologies", new ArrayList<>(List.of("FDM", "SLA", "SLS", "MJF")));
        SETTINGS.put("materials", new ArrayList<>(List.of("PLA", "PETG", "ABS", "Resin tiêu chuẩn", "Nylon PA12")));
        SETTINGS.put("surfaceQualities", new ArrayList<>(List.of("Tiêu chuẩn", "Mịn", "Siêu mịn")));
    }

    private AdminSettingsStore() { }

    public static synchronized Map<String, Object> get() {
        Map<String, Object> copy = new LinkedHashMap<>(SETTINGS);
        for (String key : List.of("printingTechnologies", "materials", "surfaceQualities")) {
            copy.put(key, new ArrayList<>((List<?>) SETTINGS.get(key)));
        }
        return copy;
    }

    public static synchronized Map<String, Object> update(Map<String, Object> values) {
        for (String key : List.of("platformName", "currency", "timezone")) {
            if (values.containsKey(key)) SETTINGS.put(key, values.get(key));
        }
        if (values.containsKey("requiredDeposit")) SETTINGS.put("requiredDeposit", values.get("requiredDeposit"));
        if (values.containsKey("requireProductReview")) SETTINGS.put("requireProductReview", values.get("requireProductReview"));
        if (values.containsKey("commissionRate")) SETTINGS.put("commissionRate", values.get("commissionRate"));
        for (String key : List.of("printingTechnologies", "materials", "surfaceQualities")) {
            if (values.containsKey(key)) SETTINGS.put(key, new ArrayList<>((List<?>) values.get(key)));
        }
        return get();
    }
}
