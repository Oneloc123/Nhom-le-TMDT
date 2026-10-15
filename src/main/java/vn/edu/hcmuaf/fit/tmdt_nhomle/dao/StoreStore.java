package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Store;

import java.util.List;

public final class StoreStore {
    private static final List<Store> STORES = List.of(
            new Store(1, "Mực Tím Studio",
                    "Chuyên cốc sứ, bình giữ nhiệt và quà tặng cá nhân hóa.",
                    "Cốc, bình giữ nhiệt, quà tặng", "TP. Hồ Chí Minh", 4.9, 1280),
            new Store(2, "Xưởng Mây In",
                    "Xưởng may và in áo thun, hoodie theo yêu cầu.",
                    "Áo thun, hoodie, đồng phục", "Hà Nội", 4.8, 956),
            new Store(3, "Canvas Lab",
                    "Sản xuất túi canvas và quà tặng doanh nghiệp số lượng lớn.",
                    "Túi vải, quà tặng doanh nghiệp", "Đà Nẵng", 4.7, 734),
            new Store(4, "Nhãn Xinh",
                    "In sticker, tem nhãn và bao bì nhỏ lẻ giao nhanh.",
                    "Sticker, decal, tem nhãn", "TP. Hồ Chí Minh", 4.9, 1422),
            new Store(5, "Poster House",
                    "In poster, canvas, namecard và văn phòng phẩm mỹ thuật.",
                    "Poster, canvas, namecard", "Bình Dương", 4.8, 611)
    );

    private StoreStore() {
    }

    public static List<Store> findAll() {
        return STORES;
    }
}
