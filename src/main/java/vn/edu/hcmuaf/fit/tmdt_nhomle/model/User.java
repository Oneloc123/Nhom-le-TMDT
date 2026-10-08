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

    public boolean isAdmin() { return ROLE_ADMIN.equals(role); }
    public boolean isActive() { return STATUS_ACTIVE.equals(status); }
}
