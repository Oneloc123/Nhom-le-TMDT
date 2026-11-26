package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import org.junit.jupiter.api.Test;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ProductRecommendationDao.CandidateProduct;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ProductRecommendationDao.UserSignal;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Product;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.ShopOffer;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductRecommendationServiceTest {

    private final ProductRecommendationService service = new ProductRecommendationService();

    @Test
    void coldStartUsesPopularityAndFiltersIneligibleCandidates() {
        Product popular = product(1, 10, "Mô hình Dragon PLA", "dragon pla 3d", "PLA", "FDM", 180000, 4.8, 12, 500, true, true);
        Product niche = product(2, 10, "Kệ tai nghe", "stand headphone", "PETG", "FDM", 150000, 4.2, 2, 40, true, true);
        Product inactive = product(3, 10, "Sản phẩm ngừng bán", "inactive", "PLA", "FDM", 90000, 5.0, 99, 999, false, true);
        Product noOffer = product(4, 10, "Chưa có giá", "out of stock", "PLA", "FDM", 90000, 5.0, 99, 999, true, false);

        List<Product> ranked = service.rankForUser(
                List.of(
                        new CandidateProduct(niche, 1),
                        new CandidateProduct(inactive, 200),
                        new CandidateProduct(noOffer, 200),
                        new CandidateProduct(popular, 45)),
                List.of(),
                4);

        assertEquals(2, ranked.size());
        assertEquals(1L, ranked.get(0).getId());
        assertEquals(2L, ranked.get(1).getId());
    }

    @Test
    void personalizedRankingMatchesCategoryMaterialMethodPriceAndKeywords() {
        Product resinFigure = product(10, 2, "Tượng chiến binh Resin 8K", "figure resin miniature", "Resin 8K", "SLA", 260000, 4.6, 5, 60, true, true);
        Product popularShirt = product(11, 5, "Áo thun Cotton in DTG", "ao thun cotton", "Cotton 100%", "DTG", 190000, 4.9, 30, 300, true, true);
        Product resinBust = product(12, 2, "Tượng bán thân hiệp sĩ Resin", "figure miniature sla", "Resin 8K", "SLA", 250000, 4.5, 4, 50, true, true);

        List<UserSignal> signals = List.of(
                new UserSignal(12, 2, "Tượng bán thân hiệp sĩ Resin", "figure miniature sla", "",
                        "Resin 8K", "SLA", BigDecimal.valueOf(250000), "FAVORITE", 0, 1),
                new UserSignal(99, 2, "Phụ kiện mô hình Resin", "miniature resin", "",
                        "Resin 8K", "SLA", BigDecimal.valueOf(240000), "CART", 0, 1));

        List<Product> ranked = service.rankForUser(
                List.of(
                        new CandidateProduct(popularShirt, 60),
                        new CandidateProduct(resinBust, 8),
                        new CandidateProduct(resinFigure, 10)),
                signals,
                3);

        assertEquals(10L, ranked.get(0).getId());
        assertFalse(ranked.stream().limit(2).map(Product::getId).anyMatch(id -> id == 11L));
    }

    @Test
    void negativeReviewDemotesDislikedProduct() {
        Product disliked = product(21, 3, "Sticker Decal mờ", "sticker decal", "Decal", "UV", 35000, 4.5, 10, 100, true, true);
        Product preferred = product(22, 4, "Cốc sứ in nhiệt", "mug ceramic", "Sứ", "Sublimation", 95000, 4.7, 8, 90, true, true);

        List<UserSignal> signals = List.of(
                new UserSignal(21, 3, "Sticker Decal mờ", "sticker decal", "",
                        "Decal", "UV", BigDecimal.valueOf(35000), "ORDER", 1, 1),
                new UserSignal(22, 4, "Cốc sứ quà tặng", "mug ceramic", "",
                        "Sứ", "Sublimation", BigDecimal.valueOf(90000), "FAVORITE", 0, 1));

        List<Product> ranked = service.rankForUser(
                List.of(new CandidateProduct(disliked, 50), new CandidateProduct(preferred, 5)),
                signals,
                2);

        assertEquals(22L, ranked.get(0).getId());
    }

    @Test
    void relatedRankingPrioritizesContentSimilarityAndExcludesTarget() {
        Product target = product(30, 7, "Hộp đựng linh kiện PLA", "box organizer", "PLA", "FDM", 120000, 4.5, 5, 80, true, true);
        Product sameCatSimilar = product(31, 7, "Khay chia linh kiện PLA", "tray organizer", "PLA", "FDM", 130000, 4.4, 3, 40, true, true);
        Product crossCatSameMaterial = product(32, 8, "Giá đỡ bút PLA", "pen holder", "PLA", "FDM", 110000, 4.8, 12, 120, true, true);
        Product unrelated = product(33, 9, "Poster A2 bóng", "poster gloss", "Giấy Couche", "Offset", 45000, 4.9, 50, 500, true, true);

        List<Product> related = service.rankRelated(
                target,
                List.of(
                        new CandidateProduct(target, 100),
                        new CandidateProduct(unrelated, 90),
                        new CandidateProduct(crossCatSameMaterial, 30),
                        new CandidateProduct(sameCatSimilar, 10)),
                List.of(),
                3);

        assertEquals(3, related.size());
        assertFalse(related.stream().anyMatch(p -> p.getId() == 30L));
        assertEquals(31L, related.get(0).getId());
        assertEquals(32L, related.get(1).getId());
        assertTrue(related.stream().anyMatch(p -> p.getId() == 33L));
    }

    @Test
    void recentViewHistoryDrivesRecommendationsAndDemotesReturnedOrders() {
        Product petgMount = product(40, 6, "Giá treo phụ kiện PETG", "mount petg holder", "PETG", "FDM", 145000, 4.6, 4, 30, true, true);
        Product petgBracket = product(41, 6, "Ngàm gắn tường PETG", "bracket petg", "PETG", "FDM", 140000, 4.5, 2, 20, true, true);
        Product returnedItem = product(42, 9, "Poster A3", "poster", "Giấy", "Offset", 50000, 4.9, 40, 300, true, true);

        List<UserSignal> signals = List.of(
                new UserSignal(41, 6, "Ngàm gắn tường PETG", "bracket petg", "",
                        "PETG", "FDM", BigDecimal.valueOf(140000), "VIEW", 0, 1, 2.0, false),
                new UserSignal(42, 9, "Poster A3", "poster", "",
                        "Giấy", "Offset", BigDecimal.valueOf(50000), "ORDER", 0, 1, 0.0, true));

        List<Product> ranked = service.rankForUser(
                List.of(
                        new CandidateProduct(returnedItem, 100),
                        new CandidateProduct(petgMount, 5),
                        new CandidateProduct(petgBracket, 4)),
                signals,
                3);

        assertEquals(40L, ranked.get(0).getId());
        assertEquals(42L, ranked.get(ranked.size() - 1).getId());
    }

    private static Product product(
            long id,
            long categoryId,
            String name,
            String keywords,
            String material,
            String method,
            int price,
            double rating,
            int reviewCount,
            int viewCount,
            boolean active,
            boolean hasValidOffer) {
        Product p = new Product();
        p.setId(id);
        p.setCategoryId(categoryId);
        p.setCategoryName("Danh mục " + categoryId);
        p.setName(name);
        p.setSearchKeywords(keywords);
        p.setDescription(keywords);
        p.setRating(rating);
        p.setReviewCount(reviewCount);
        p.setViewCount(viewCount);
        p.setActive(active);

        if (hasValidOffer) {
            ShopOffer offer = new ShopOffer();
            offer.setShopProductId(id * 10);
            offer.setShopId(1L);
            offer.setShopName("PrintKraft Shop");
            offer.setProductId(id);
            offer.setMaterial(material);
            offer.setPrintingMethod(method);
            offer.setUnitPrice(BigDecimal.valueOf(price));
            offer.setMinimumQuantity(1);
            offer.setStockStatus("IN_STOCK");
            offer.setActive(true);
            p.getOffers().add(offer);
        }
        return p;
    }
}
