package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ReviewDaoTest {

    private static boolean mysqlAvailable() {
        try (var ignored = vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database.getConnection()) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void ratingSummaryCalculation() {
        ReviewDao.RatingSummary summary = new ReviewDao.RatingSummary(
                4.5, 10, 6, 3, 1, 0, 0
        );
        assertEquals(4.5, summary.averageRating());
        assertEquals("4.5", summary.formattedAverage());
        assertEquals(10, summary.totalReviews());
        assertEquals(60, summary.percent(summary.star5Count()));
        assertEquals(30, summary.percent(summary.star4Count()));
        assertEquals(10, summary.percent(summary.star3Count()));
        assertEquals(0, summary.percent(summary.star2Count()));
        assertEquals(0, summary.percent(summary.star1Count()));
    }

    @Test
    void ratingValidationRejectsInvalidValues() {
        assertThrows(IllegalArgumentException.class, () ->
                ReviewDao.createOrUpdate(1L, 1L, 0, "Đánh giá tốt"));
        assertThrows(IllegalArgumentException.class, () ->
                ReviewDao.createOrUpdate(1L, 1L, 6, "Đánh giá tốt"));
        assertThrows(IllegalArgumentException.class, () ->
                ReviewDao.createOrUpdate(1L, 1L, 5, ""));
        assertThrows(IllegalArgumentException.class, () ->
                ReviewDao.createOrUpdate(1L, 1L, 5, "   "));
        assertThrows(IllegalArgumentException.class, () ->
                ReviewDao.createOrUpdate(1L, 1L, 5, "ab")); // dưới 3 ký tự
        assertThrows(IllegalArgumentException.class, () ->
                ReviewDao.createOrUpdate(1L, 1L, 5, "a".repeat(2001))); // vượt 2000 ký tự

        assertThrows(IllegalArgumentException.class, () ->
                ReviewDao.postReviewOrComment(1L, 1L, 0, "Bình luận tốt"));
        assertThrows(IllegalArgumentException.class, () ->
                ReviewDao.postReviewOrComment(1L, 1L, 6, "Bình luận tốt"));
        assertThrows(IllegalArgumentException.class, () ->
                ReviewDao.postReviewOrComment(1L, 1L, 5, ""));
        assertThrows(IllegalArgumentException.class, () ->
                ReviewDao.postReviewOrComment(1L, 1L, 5, "a"));
    }

    @Test
    void daoQueriesRunSafelyWhenDbConnected() {
        assumeTrue(mysqlAvailable(), "Bỏ qua khi MySQL không khả dụng.");
        assertDoesNotThrow(() -> ReviewDao.listPublishedForProduct(1L));
        assertDoesNotThrow(() -> ReviewDao.getRatingSummary(1L));
        assertDoesNotThrow(() -> ReviewDao.findReviewableItem(-1L, -1L));
        assertDoesNotThrow(() -> ReviewDao.findReviewableItemForProduct(1L, -1L));
        assertDoesNotThrow(() -> ReviewDao.findUserReviewForProduct(1L, -1L));
    }
}
