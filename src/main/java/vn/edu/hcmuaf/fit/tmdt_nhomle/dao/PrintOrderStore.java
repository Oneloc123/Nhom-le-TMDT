package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.model.PrintOrder;

import java.util.ArrayList;
import java.util.List;

/**
 * Kho đơn đặt in 3D in-memory (mock) phục vụ Dashboard.
 * Báo giá theo: 1.500đ/g vật liệu + 8.000đ/giờ thời gian in.
 */
public class PrintOrderStore {

    private static final List<PrintOrder> ORDERS = new ArrayList<>();

    static {
        add(new PrintOrder(1, "Nguyen Minh Tuan", "nhan_tpsl_v2.stl", "FDM", "PLA",
                42, 5.5, 113000, PrintOrder.STATUS_DONE, "2025-09-02"));
        add(new PrintOrder(2, "Pham Thi Trang", "op_dien_thoai_slA.obj", "SLA", "Resin",
                18, 3.0, 51000, PrintOrder.STATUS_SHIPPED, "2025-09-05"));
        add(new PrintOrder(3, "Vu Quynh Anh", "maket_nha_3_tang.stl", "FDM", "ABS",
                310, 26.0, 673000, PrintOrder.STATUS_PRINTING, "2025-09-09"));
        add(new PrintOrder(4, "Do Viet Hoang", "ban_do_ranh_gioic.obj", "SLA", "Resin",
                65, 8.5, 164000, PrintOrder.STATUS_NEW, "2025-09-12"));
        add(new PrintOrder(5, "Le Hoi Nam", "khay_cong_nghe.stl", "FDM", "PLA",
                120, 9.0, 250000, PrintOrder.STATUS_DONE, "2025-09-15"));
        add(new PrintOrder(6, "Nguyen Minh Tuan", "mat_na_hoa_si.stl", "FDM", "PLA",
                88, 7.5, 194000, PrintOrder.STATUS_PRINTING, "2025-09-18"));
        add(new PrintOrder(7, "Tran Bao Long", "noi_that_mini_01.obj", "SLA", "Resin",
                34, 4.0, 77000, PrintOrder.STATUS_DONE, "2025-09-21"));
        add(new PrintOrder(8, "Pham Thi Trang", "co_tich_500ml.stl", "FDM", "ABS",
                96, 6.0, 194000, PrintOrder.STATUS_SHIPPED, "2025-09-25"));
    }

    private PrintOrderStore() {
    }

    private static void add(PrintOrder o) {
        ORDERS.add(o);
    }

    public static List<PrintOrder> findAll() {
        return new ArrayList<>(ORDERS);
    }

    public static List<PrintOrder> findRecent(int limit) {
        int from = Math.max(0, ORDERS.size() - limit);
        return new ArrayList<>(ORDERS.subList(from, ORDERS.size()));
    }

    public static long count() {
        return ORDERS.size();
    }

    public static long revenue() {
        return ORDERS.stream()
                .filter(o -> PrintOrder.STATUS_DONE.equals(o.getStatus())
                        || PrintOrder.STATUS_SHIPPED.equals(o.getStatus()))
                .mapToLong(PrintOrder::getPriceVnd)
                .sum();
    }

    public static long countByStatus(String status) {
        return ORDERS.stream().filter(o -> o.getStatus().equals(status)).count();
    }
}
