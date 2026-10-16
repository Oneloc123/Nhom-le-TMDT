package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Product;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Store;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Kiểm tra câu SQL của các DAO CHẠY THẬT trên schema hiện có (chỉ SELECT, không ghi).
 * Bộ test này từng bắt được lỗi lệch schema (products.view_count thiếu trong script)
 * vốn gây HTTP 500 "Không thể truy cập dữ liệu" trên trang chủ.
 *
 * <p>Tự bỏ qua khi MySQL không khả dụng — không bao giờ trỏ tới database Production.</p>
 */
class DaoQuerySmokeTest {

    private static boolean mysqlAvailable() {
        try (var ignored = vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database.getConnection()) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void bestsellerAndTrendingQueriesMatchSchema() {
        assumeTrue(mysqlAvailable(), "MySQL không khả dụng — bỏ qua smoke test DAO.");
        assertDoesNotThrow(() -> ProductDao.getBestsellers(4));
        assertDoesNotThrow(() -> ProductDao.getTrending(4));
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void productSearchSupportsFilterCombinationAndSorts() {
        assumeTrue(mysqlAvailable(), "MySQL không khả dụng — bỏ qua smoke test DAO.");
        ProductDao.SearchCriteria empty = new ProductDao.SearchCriteria("", "", null, null, null, null);
        assertDoesNotThrow(() -> ProductDao.count(empty));
        assertDoesNotThrow(() -> ProductDao.search(empty, 1, 8));

        ProductDao.SearchCriteria combined = new ProductDao.SearchCriteria(
                "in", null, new BigDecimal("0"), new BigDecimal("100000000"), null, "priceAsc");
        assertDoesNotThrow(() -> ProductDao.search(combined, 1, 8));

        assertDoesNotThrow(() -> ProductDao.search(
                new ProductDao.SearchCriteria("abc%", null, null, null, null, "rating"), 2, 8),
                "Ký tự đặc biệt trong từ khóa phải được parameter hóa, không được làm vỡ SQL.");
        assertDoesNotThrow(() -> ProductDao.search(
                new ProductDao.SearchCriteria(null, null, null, null, 99999999L, "priceDesc"), 1, 8),
                "shopId không tồn tại phải trả về danh sách rỗng thay vì lỗi.");
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void productDetailLookupHandlesMissingId() {
        assumeTrue(mysqlAvailable(), "MySQL không khả dụng — bỏ qua smoke test DAO.");
        assertDoesNotThrow(() -> ProductDao.findById(-1L));
        List<Product> bestsellers = ProductDao.getBestsellers(4);
        if (!bestsellers.isEmpty()) {
            assertDoesNotThrow(() -> ProductDao.findById(bestsellers.get(0).getId()));
        }
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shopSearchHandlesVietnameseAndEmptyQuery() {
        assumeTrue(mysqlAvailable(), "MySQL không khả dụng — bỏ qua smoke test DAO.");
        assertDoesNotThrow(() -> ShopDao.search(""));
        assertDoesNotThrow(() -> ShopDao.search("xưởng in"));
        assertDoesNotThrow(() -> ShopDao.search("' OR 1=1 --"),
                "Ký tự đặc biệt phải được parameter hóa.");
        List<Store> stores = ShopDao.search("");
        assertTrue(stores.size() <= ShopDao.MAX_RESULTS, "Số cửa hàng trả về phải trong giới hạn");
        if (!stores.isEmpty()) {
            assertDoesNotThrow(() -> ShopDao.findById(stores.get(0).getId()));
        }
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void orderQueriesScopeToUnknownUser() {
        assumeTrue(mysqlAvailable(), "MySQL không khả dụng — bỏ qua smoke test DAO.");
        long unknownUserId = -1L;
        assertDoesNotThrow(() -> OrderDao.listByUser(unknownUserId));
        assertTrue(OrderDao.listByUser(unknownUserId).isEmpty(), "User lạ không có đơn nào");
        assertDoesNotThrow(() -> OrderDao.findByCodeForUser("PK-00000000-000000", unknownUserId));
        assertEquals(java.util.Optional.empty(),
                OrderDao.findByCodeForUser("PK-00000000-000000", unknownUserId));
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void userDaoLookupIsSafeForUnknownEmail() {
        assumeTrue(mysqlAvailable(), "MySQL không khả dụng — bỏ qua smoke test DAO.");
        assertTrue(UserDao.findByEmail("khong-ton-tai@example.invalid").isEmpty());
        assertTrue(UserDao.findByEmail("").isEmpty());
    }
}
