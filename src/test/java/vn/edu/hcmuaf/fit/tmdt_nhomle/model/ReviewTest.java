package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReviewTest {

    @Test
    void starsFormatting() {
        Review r = new Review();
        r.setRating(5);
        assertEquals("★★★★★", r.getStars());
        assertEquals("", r.getEmptyStars());
        assertEquals("Tuyệt vời", r.getRatingLabel());

        r.setRating(4);
        assertEquals("★★★★", r.getStars());
        assertEquals("☆", r.getEmptyStars());
        assertEquals("Hài lòng", r.getRatingLabel());

        r.setRating(3);
        assertEquals("★★★", r.getStars());
        assertEquals("☆☆", r.getEmptyStars());
        assertEquals("Bình thường", r.getRatingLabel());

        r.setRating(2);
        assertEquals("★★", r.getStars());
        assertEquals("☆☆☆", r.getEmptyStars());
        assertEquals("Chưa hài lòng", r.getRatingLabel());

        r.setRating(1);
        assertEquals("★", r.getStars());
        assertEquals("☆☆☆☆", r.getEmptyStars());
        assertEquals("Rất tệ", r.getRatingLabel());
    }

    @Test
    void userInitialExtraction() {
        Review r = new Review();
        r.setUserDisplayName("Nguyễn Văn A");
        assertEquals("N", r.getUserInitial());

        r.setUserDisplayName("linh***");
        assertEquals("L", r.getUserInitial());

        r.setUserDisplayName(null);
        assertEquals("K", r.getUserInitial());

        r.setUserDisplayName("");
        assertEquals("K", r.getUserInitial());
    }

    @Test
    void verifiedBuyerStatus() {
        Review r = new Review();
        r.setOrderItemId(0);
        org.junit.jupiter.api.Assertions.assertFalse(r.isVerifiedBuyer());

        r.setOrderItemId(101);
        org.junit.jupiter.api.Assertions.assertTrue(r.isVerifiedBuyer());
    }
}
