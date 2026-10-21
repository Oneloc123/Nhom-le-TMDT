package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

public class User {
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_CUSTOMER = "CUSTOMER";

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_LOCKED = "LOCKED";
    public static final String STATUS_PENDING = "PENDING"; // chưa xác thực email

    private int id;
    private String email;
    private String password;
    private String fullName;
    private String phone;
    private String address; // địa chỉ nhận hàng in 3D
    private String role;
    private String status;
    private String provider; // local | google | facebook
    private String createdAt; // yyyy-MM-dd
    private String updatedAt; // yyyy-MM-dd HH:mm:ss (null nếu chưa cập nhật)
    private boolean emailVerified;
    private String lastLoginAt; // yyyy-MM-dd HH:mm:ss
    private int failedLoginAttempts;
    private String lockedUntil; // yyyy-MM-dd HH:mm:ss, null = không khóa tạm
    private String deletedAt; // yyyy-MM-dd HH:mm:ss, null = chưa bị xoá mềm

    public User() {
    }

    public User(int id, String email, String password, String fullName, String phone,
                String address, String role, String status, String provider, String createdAt) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.phone = phone;
        this.address = address;
        this.role = role;
        this.status = status;
        this.provider = provider;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public String getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(String lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public String getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(String lockedUntil) { this.lockedUntil = lockedUntil; }

    public String getDeletedAt() { return deletedAt; }
    public void setDeletedAt(String deletedAt) { this.deletedAt = deletedAt; }

    public boolean isAdmin() { return ROLE_ADMIN.equals(role); }
    public boolean isActive() { return STATUS_ACTIVE.equals(status); }

    /** Tài khoản đã bị xoá mềm (admin khôi phục được, không xoá lịch sử đơn). */
    public boolean isDeleted() { return deletedAt != null && !deletedAt.isBlank(); }

    /** Đang bị khóa tạm do nhập sai mật khẩu quá nhiều lần. */
    public boolean isTemporarilyLocked() {
        if (lockedUntil == null || lockedUntil.isBlank()) {
            return false;
        }
        try {
            return java.time.LocalDateTime.now().isBefore(java.time.LocalDateTime.parse(lockedUntil));
        } catch (RuntimeException e) {
            return false;
        }
    }
}
