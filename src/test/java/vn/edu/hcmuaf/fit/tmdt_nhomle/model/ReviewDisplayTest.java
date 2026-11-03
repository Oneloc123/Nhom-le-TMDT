package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReviewDisplayTest {

    @Test
    void starStringsReflectTheActualRating() {
        Review review = new Review();
        review.setRating(3);

        assertEquals("★★★", review.getStars());
        assertEquals("☆☆", review.getEmptyStars());
    }

    @Test
    void createdAtUsesVietnameseDayFirstDisplay() {
        Review review = new Review();
        review.setCreatedAt(LocalDateTime.of(2026, 10, 24, 21, 15));

        assertEquals("24/10/2026 21:15", review.getCreatedAtText());
    }
}
