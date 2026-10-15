package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

/** Lỗi thanh toán — thông báo hiển thị được cho người dùng. */
public class PaymentException extends RuntimeException {
    public PaymentException(String message) {
        super(message);
    }
}
