package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;

/** Validation for a manually executed refund; this class never moves money. */
public final class ManualRefundPolicy {
    public record Completion(String method, String reference, String note, BigDecimal amount) { }

    private ManualRefundPolicy() { }

    public static Completion validateCompletion(String rawMethod, String rawReference,
                                                String rawNote, BigDecimal approvedAmount) {
        String method = rawMethod == null ? "" : rawMethod.trim().toUpperCase(Locale.ROOT);
        if (!ReturnRefundPolicy.REFUND_METHODS.contains(method)) {
            throw new IllegalArgumentException("Phương thức hoàn tiền thủ công không hợp lệ.");
        }
        String reference = rawReference == null ? "" : rawReference.trim();
        if (reference.length() < 3 || reference.length() > 120) {
            throw new IllegalArgumentException("Mã tham chiếu hoàn tiền phải có từ 3 đến 120 ký tự.");
        }
        String note = rawNote == null ? "" : rawNote.trim();
        if (note.length() < 3 || note.length() > 1000) {
            throw new IllegalArgumentException("Ghi chú xác nhận hoàn tiền phải có từ 3 đến 1000 ký tự.");
        }
        if (approvedAmount == null || approvedAmount.signum() <= 0
                || approvedAmount.scale() > 2 || approvedAmount.precision() > 12) {
            throw new IllegalArgumentException("Số tiền hoàn phải là số dương, tối đa 12 chữ số và 2 chữ số thập phân.");
        }
        return new Completion(method, reference, note, approvedAmount);
    }

    public static String validateFailureNote(String rawNote) {
        String note = rawNote == null ? "" : rawNote.trim();
        if (note.length() < 3 || note.length() > 1000) {
            throw new IllegalArgumentException("Cần ghi lý do hoàn tiền thất bại từ 3 đến 1000 ký tự.");
        }
        return note;
    }

    public static boolean shouldMarkPaymentRefunded(String paymentStatus) {
        return paymentStatus != null && "REFUND_REQUIRED".equals(paymentStatus.trim().toUpperCase(Locale.ROOT));
    }
}
