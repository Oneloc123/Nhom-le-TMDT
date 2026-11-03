package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Product;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Store;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/** Kiểm tra truy vấn thật khi môi trường có database và dữ liệu catalog. */
class ProductDetailDaoIntegrationTest {

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void detailRelatedImagesOffersAndPagedReviewsCanBeLoaded() {
        List<Product> catalog;
        try {
            catalog = ProductDao.search(
                    new ProductDao.SearchCriteria(null, null, null, null, null, null), 1, 1);
        } catch (RuntimeException unavailableDatabase) {
            assumeFalse(true, "Database không khả dụng — bỏ qua integration test.");
            return;
        }
        assumeFalse(catalog.isEmpty(), "Database chưa có sản phẩm — bỏ qua integration test.");

        Product product = ProductDao.findById(catalog.getFirst().getId()).orElseThrow();
        assertNotNull(product.getImages());
        assertNotNull(product.getOffers());

        List<Product> related = ProductDao.findRelated(product.getId(), product.getCategoryId(), 4);
        assertTrue(related.stream().noneMatch(item -> item.getId() == product.getId()));

        long reviewCount = ReviewDao.countPublishedForProduct(product.getId());
        var reviews = ReviewDao.listPublishedForProduct(product.getId(), 1, 10);
        assertTrue(reviews.size() <= 10);
        assertTrue(reviewCount >= reviews.size());
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void filteringByStoreNeverReturnsAnotherStoresOffer() {
        List<Store> stores;
        try {
            stores = ShopDao.search(null);
        } catch (RuntimeException unavailableDatabase) {
            assumeFalse(true, "Database không khả dụng — bỏ qua integration test.");
            return;
        }
        assumeFalse(stores.isEmpty(), "Database chưa có gian hàng — bỏ qua integration test.");

        long shopId = stores.getFirst().getId();
        List<Product> products = ProductDao.search(
                new ProductDao.SearchCriteria(null, null, null, null, shopId, null), 1, 48);
        for (Product product : products) {
            assertTrue(!product.getOffers().isEmpty());
            assertTrue(product.getOffers().stream().allMatch(offer -> offer.getShopId() == shopId),
                    "Bộ lọc gian hàng không được nạp chào bán từ gian hàng khác.");
            assertTrue(product.primaryOffer().getShopId() == shopId);
        }
    }
}
