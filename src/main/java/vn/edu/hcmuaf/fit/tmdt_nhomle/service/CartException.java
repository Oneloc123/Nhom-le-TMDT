package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

/** Lỗi nghiệp vụ giỏ hàng — thông báo an toàn, hiển thị được cho người dùng. */
public class CartException extends RuntimeException {
    public CartException(String message) {
        super(message);
    }
}
