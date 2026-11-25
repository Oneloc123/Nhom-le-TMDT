package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuoteRequestTest {

    @Test
    void calculates30PercentDepositAndRemainingAmount() {
        QuoteRequest quote = new QuoteRequest();
        quote.setQuotedAmount(new BigDecimal("10000000"));
        quote.setStatus(QuoteRequest.STATUS_QUOTED);

        assertEquals(new BigDecimal("3000000"), quote.getRequiredDepositAmount());
        assertEquals(new BigDecimal("7000000"), quote.getRemainingAmount());
        assertTrue(quote.isDepositable());
    }

    @Test
    void isDepositableOnlyWhenQuotedWithPositiveAmount() {
        QuoteRequest quote = new QuoteRequest();
        quote.setQuotedAmount(new BigDecimal("10000000"));
        quote.setStatus(QuoteRequest.STATUS_DEPOSITED);
        assertFalse(quote.isDepositable());

        quote.setStatus(QuoteRequest.STATUS_PENDING);
        assertFalse(quote.isDepositable());

        quote.setStatus(QuoteRequest.STATUS_QUOTED);
        quote.setQuotedAmount(null);
        assertNull(quote.getRequiredDepositAmount());
        assertFalse(quote.isDepositable());
    }
}
