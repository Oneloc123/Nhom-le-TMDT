package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.SuggestedShop;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ShopRecommendationServiceTest {

    private final ShopRecommendationService service = new ShopRecommendationService();

    @Test
    @DisplayName("Gợi ý xưởng SLA/Resin ưu tiên các xưởng có năng lực Resin")
    void testRecommendSlaResin() {
        List<SuggestedShop> shops = service.getSuggestedShops("sla", "resin-std", 50, 40, 60, "stl");
        assertNotNull(shops);
        assertFalse(shops.isEmpty());

        SuggestedShop best = shops.getFirst();
        assertTrue(best.isBestMatch());
        assertNotNull(best.getMatchBadge());
        // Saigon Resin Works chuyên SLA và resin
        assertTrue(best.getCapabilities().toLowerCase().contains("sla")
                || best.getCapabilities().toLowerCase().contains("resin"));
    }

    @Test
    @DisplayName("Gợi ý xưởng FDM/PLA ưu tiên các xưởng in sợi nhựa")
    void testRecommendFdmPla() {
        List<SuggestedShop> shops = service.getSuggestedShops("fdm", "pla", 100, 100, 80, "stl");
        assertNotNull(shops);
        assertFalse(shops.isEmpty());

        SuggestedShop best = shops.getFirst();
        assertTrue(best.isBestMatch());
        assertTrue(best.getCapabilities().toLowerCase().contains("fdm")
                || best.getCapabilities().toLowerCase().contains("pla"));
    }
}
