package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ProductStore {
    private static final List<Product> PRODUCTS = List.of(
            product(1, "Cốc sứ trắng 2 mặt", "Cốc & Bình giữ nhiệt",
                    "Cốc sứ trắng phủ bóng, in hình sắc nét ở cả hai mặt.",
                    "In Sublimation", "65000", 1, 4.9, 412, 1,
                    "Mực Tím Studio", "warm", "Bán chạy"),
            product(2, "Áo thun Oversize Cotton", "Áo thun & Hoodie",
                    "Áo cotton 100%, form oversize, phù hợp in DTG từ một chiếc.",
                    "In DTG", "129000", 1, 4.8, 218, 2,
                    "Xưởng Mây In", "primary", "Bán chạy"),
            product(3, "Túi vải canvas dày", "Túi vải & Phụ kiện",
                    "Túi canvas định lượng cao, quai chắc và phù hợp in lụa.",
                    "In lụa", "42000", 50, 4.8, 174, 3,
                    "Canvas Lab", "dark", ""),
            product(4, "Sticker decal cuộn", "Bao bì & Nhãn dán",
                    "Sticker decal chống nước, bế demi theo hình dạng thiết kế.",
                    "In kỹ thuật số", "3500", 100, 4.9, 287, 4,
                    "Nhãn Xinh", "teal", ""),
            product(5, "Hoodie Basic Nỉ Bông", "Áo thun & Hoodie",
                    "Hoodie nỉ bông ấm, nhận in DTF số lượng nhỏ và vừa.",
                    "In DTF", "289000", 10, 4.7, 142, 2,
                    "Xưởng Mây In", "sky", ""),
            product(6, "Bình giữ nhiệt in logo", "Cốc & Bình giữ nhiệt",
                    "Bình inox 500ml, khắc laser logo bền đẹp, giữ nhiệt tốt.",
                    "Khắc laser", "215000", 20, 4.7, 98, 1,
                    "Mực Tím Studio", "pink", ""),
            product(7, "Poster A2 giấy mỹ thuật", "Poster & Canvas",
                    "Poster A2 trên giấy mỹ thuật 300gsm, màu in nổi bật.",
                    "In UV", "89000", 1, 4.7, 96, 5,
                    "Poster House", "cool", ""),
            product(8, "Namecard giấy mỹ thuật 300gsm", "Namecard & Văn phòng phẩm",
                    "Namecard cán mờ, bo góc tùy chọn, nhận từ 100 tấm.",
                    "In offset", "1200", 100, 5.0, 521, 5,
                    "Poster House", "brown", ""),
            product(9, "Canvas treo tường 40x60", "Poster & Canvas",
                    "Tranh canvas căng khung gỗ, phù hợp trang trí văn phòng và nhà ở.",
                    "In canvas", "245000", 1, 4.8, 73, 5,
                    "Poster House", "violet", "Mới"),
            product(10, "Túi tote doanh nghiệp", "Quà tặng doanh nghiệp",
                    "Túi tote may theo nhận diện thương hiệu, hỗ trợ số lượng lớn.",
                    "In lụa", "55000", 50, 4.6, 64, 3,
                    "Canvas Lab", "green", "")
    );

    private ProductStore() {
    }

    private static Product product(int id, String name, String category, String description,
                                   String printMethod, String price, int minimumQuantity,
                                   double rating, int reviewCount, int storeId, String storeName,
                                   String visualClass, String tag) {
        return new Product(id, name, category, description, printMethod,
                new BigDecimal(price), minimumQuantity, rating, reviewCount,
                storeId, storeName, visualClass, tag);
    }

    public static List<Product> findAll() {
        return new ArrayList<>(PRODUCTS);
    }

    public static Optional<Product> findById(int id) {
        return PRODUCTS.stream().filter(product -> product.getId() == id).findFirst();
    }

    public static List<String> categories() {
        return PRODUCTS.stream().map(Product::getCategory).distinct().toList();
    }
}
