package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductDetailBehaviorTest {

    @Test
    void priceAndMinimumQuantityComeFromTheSameCheapestOffer() {
        Product product = new Product();
        ShopOffer expensiveSmallBatch = offer(1, "129000", 1, "IN_STOCK", true);
        ShopOffer cheapestLargeBatch = offer(2, "99000", 5, "IN_STOCK", true);
        product.getOffers().add(expensiveSmallBatch);
        product.getOffers().add(cheapestLargeBatch);

        assertSame(cheapestLargeBatch, product.getPrimaryOffer());
        assertEquals(new BigDecimal("99000"), product.getPrice());
        assertEquals(5, product.getMinimumQuantity());
        assertTrue(product.isAvailable());
    }

    @Test
    void productIsUnavailableWhenEveryOfferIsOutOfStockOrInactive() {
        Product product = new Product();
        product.getOffers().add(offer(1, "10000", 1, "OUT_OF_STOCK", true));
        product.getOffers().add(offer(2, "9000", 1, "IN_STOCK", false));

        assertFalse(product.isAvailable());
        assertEquals(BigDecimal.ZERO, product.getPrice());
        assertEquals(1, product.getMinimumQuantity());
    }

    private ShopOffer offer(long id, String price, int minimum, String stock, boolean active) {
        ShopOffer offer = new ShopOffer();
        offer.setShopProductId(id);
        offer.setUnitPrice(new BigDecimal(price));
        offer.setMinimumQuantity(minimum);
        offer.setStockStatus(stock);
        offer.setActive(active);
        return offer;
    }
}
