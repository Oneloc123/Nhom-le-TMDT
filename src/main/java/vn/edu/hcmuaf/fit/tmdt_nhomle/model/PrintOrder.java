package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

/**
 * Đơn đặt in 3D — mock data cho Dashboard.
 */
public class PrintOrder {
    public static final String STATUS_NEW = "Moi";
    public static final String STATUS_PRINTING = "Dang in";
    public static final String STATUS_DONE = "Hoan tat";
    public static final String STATUS_SHIPPED = "Da giao";

    private int id;
    private String customerName;
    private String fileName;   // van ban .stl / .obj
    private String technology;  // FDM / SLA
    private String material;    // PLA / ABS / Resin
    private double weightGram;  // trong luong (g)
    private double printHours;  // thoi gian in (gio)
    private long priceVnd;      // bao gia
    private String status;
    private String createdAt;   // yyyy-MM-dd

    public PrintOrder() {
    }

    public PrintOrder(int id, String customerName, String fileName, String technology,
                      String material, double weightGram, double printHours,
                      long priceVnd, String status, String createdAt) {
        this.id = id;
        this.customerName = customerName;
        this.fileName = fileName;
        this.technology = technology;
        this.material = material;
        this.weightGram = weightGram;
        this.printHours = printHours;
        this.priceVnd = priceVnd;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getTechnology() { return technology; }
    public void setTechnology(String technology) { this.technology = technology; }

    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }

    public double getWeightGram() { return weightGram; }
    public void setWeightGram(double weightGram) { this.weightGram = weightGram; }

    public double getPrintHours() { return printHours; }
    public void setPrintHours(double printHours) { this.printHours = printHours; }

    public long getPriceVnd() { return priceVnd; }
    public void setPriceVnd(long priceVnd) { this.priceVnd = priceVnd; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
