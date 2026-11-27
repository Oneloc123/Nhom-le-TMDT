package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

enum GhnDeliveryStage {
    WAITING("PREPARING", "Chờ GHN lấy hàng", false, false),
    PICKING("PREPARING", "GHN đang đến lấy hàng", false, false),
    PICKED("SHIPPED", "GHN đã lấy hàng", true, false),
    IN_TRANSIT("IN_TRANSIT", "Đang vận chuyển", true, false),
    DELIVERING("IN_TRANSIT", "Đang giao đến người nhận", true, false),
    DELIVERED("DELIVERED", "Đã giao – chờ xác nhận", true, true),
    FAILED("FAILED", "Giao không thành công", true, false),
    RETURNING("IN_TRANSIT", "GHN đang hoàn hàng", true, false),
    RETURN_FAILED("FAILED", "GHN hoàn hàng không thành công", true, false),
    RETURNED("RETURNED", "GHN đã hoàn hàng", true, true),
    CANCELLED("RETURNED", "Vận đơn GHN đã hủy", false, true),
    EXCEPTION("FAILED", "Vận đơn GHN gặp sự cố", false, true);

    private final String shipmentStatus;
    private final String title;
    private final boolean inCarrierCustody;
    private final boolean terminal;

    GhnDeliveryStage(String shipmentStatus, String title, boolean inCarrierCustody, boolean terminal) {
        this.shipmentStatus = shipmentStatus;
        this.title = title;
        this.inCarrierCustody = inCarrierCustody;
        this.terminal = terminal;
    }

    String shipmentStatus() { return shipmentStatus; }
    String title() { return title; }
    boolean inCarrierCustody() { return inCarrierCustody; }
    boolean terminal() { return terminal; }
    boolean delivered() { return this == DELIVERED; }

    static GhnDeliveryStage from(String status) {
        if (status == null) return null;
        return switch (status) {
            case "ready_to_pick" -> WAITING;
            case "picking", "money_collect_picking" -> PICKING;
            case "picked" -> PICKED;
            case "storing", "sorting", "transporting" -> IN_TRANSIT;
            case "delivering", "money_collect_delivering" -> DELIVERING;
            case "delivered" -> DELIVERED;
            case "delivery_fail" -> FAILED;
            case "waiting_to_return", "return", "return_transporting", "return_sorting", "returning" -> RETURNING;
            case "return_fail" -> RETURN_FAILED;
            case "returned" -> RETURNED;
            case "cancel" -> CANCELLED;
            case "exception", "lost", "damage", "scrap" -> EXCEPTION;
            default -> null;
        };
    }
}
