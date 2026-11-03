package vn.edu.hcmuaf.fit.tmdt_nhomle.util;

import java.util.Locale;

public final class VietnamPhoneNumber {
    private VietnamPhoneNumber() { }

    public static boolean isValid(String raw) {
        return raw == null || raw.isBlank() || raw.trim().matches("^(0|\\+84)\\d{9}$");
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String value = raw.trim().replaceAll("[ .()-]", "");
        if (!value.matches("^(0|\\+84)\\d{9}$")) {
            throw new IllegalArgumentException("Số điện thoại Việt Nam phải có dạng 0xxxxxxxxx hoặc +84xxxxxxxxx.");
        }
        return value.startsWith("0") ? "+84" + value.substring(1) : value;
    }

    public static String display(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String value = raw.trim();
        if (value.startsWith("+84") && value.length() == 12) return "0" + value.substring(3);
        return value.toLowerCase(Locale.ROOT).equals("null") ? "" : value;
    }
}
