package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

/** Central deployment configuration for custom-print quote validity. */
public final class CustomPrintConfig {
    private static final int DEFAULT_QUOTE_VALIDITY_DAYS = 3;
    private CustomPrintConfig() { }

    public static int quoteValidityDays() {
        String configured = System.getenv("PK_CUSTOM_PRINT_QUOTE_VALID_DAYS");
        if (configured == null || configured.isBlank()) return DEFAULT_QUOTE_VALIDITY_DAYS;
        try {
            int days = Integer.parseInt(configured.trim());
            return days >= 1 && days <= 30 ? days : DEFAULT_QUOTE_VALIDITY_DAYS;
        } catch (NumberFormatException e) {
            return DEFAULT_QUOTE_VALIDITY_DAYS;
        }
    }
}
