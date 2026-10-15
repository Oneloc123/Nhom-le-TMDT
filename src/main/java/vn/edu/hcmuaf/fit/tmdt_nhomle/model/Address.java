package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

/** Địa chỉ nhận hàng đã lưu của khách hàng (bảng addresses). */
public class Address {
    private long addressId;
    private long userId;
    private String recipientName;
    private String recipientPhone;
    private String province;
    private String district;
    private String addressLine;
    private boolean defaultAddress;

    public Address() {
    }

    public long getAddressId() { return addressId; }
    public void setAddressId(long addressId) { this.addressId = addressId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

    public String getRecipientPhone() { return recipientPhone; }
    public void setRecipientPhone(String recipientPhone) { this.recipientPhone = recipientPhone; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getAddressLine() { return addressLine; }
    public void setAddressLine(String addressLine) { this.addressLine = addressLine; }

    public boolean isDefaultAddress() { return defaultAddress; }
    public void setDefaultAddress(boolean defaultAddress) { this.defaultAddress = defaultAddress; }

    /** Dòng địa chỉ đầy đủ dùng khi hiển thị / tạo đơn. */
    public String getFullLine() {
        StringBuilder sb = new StringBuilder();
        if (addressLine != null && !addressLine.isBlank()) {
            sb.append(addressLine.trim());
        }
        append(sb, district);
        append(sb, province);
        return sb.toString();
    }

    private static void append(StringBuilder sb, String part) {
        if (part != null && !part.isBlank()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(part.trim());
        }
    }
}
