package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import org.junit.jupiter.api.Test;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.ModelAnalysis;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.PrintPriceEstimate;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.PrintPriceOption;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.PrintQuantityDiscount;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.ShopPrintPriceProfile;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrintPriceEstimatorTest {
    private final PrintPriceEstimator estimator = new PrintPriceEstimator();
    private final ModelAnalysis model = new ModelAnalysis(ModelAnalysis.Status.READY, "obj", "mm",
            10, 10, 10, 1, 6, 12, "Ready");

    @Test
    void calculatesMaterialMachineMinimumAndQuantityDiscount() {
        PrintPriceEstimate result = estimator.estimate(model, profile(), 20,
                new BigDecimal("0.2"), 5, List.of(), List.of(
                        new PrintQuantityDiscount(5, new BigDecimal("0.10"))), false);

        assertEquals(new BigDecimal("0.10"), result.discountRate());
        assertEquals(5, result.quantity());
        assertTrue(result.totalEstimate().compareTo(new BigDecimal("25000.00")) >= 0);
        assertTrue(result.disclaimer().contains("không phải kết quả slicer"));
    }

    @Test
    void refusesUnconfirmedGeometryAndUnsupportedProfileRanges() {
        ModelAnalysis unusual = new ModelAnalysis(ModelAnalysis.Status.NEEDS_CONFIRMATION, "obj", "mm",
                0.01, 1, 1, 0.00001, 0.1, 12, "confirm");
        assertThrows(IllegalArgumentException.class, () -> estimator.estimate(unusual, profile(), 20,
                new BigDecimal("0.2"), 1, List.of(), List.of(), false));
        assertThrows(IllegalArgumentException.class, () -> estimator.estimate(model, profile(), 101,
                new BigDecimal("0.2"), 1, List.of(), List.of(), false));
    }

    @Test
    void refusesEstimateOutsideOrderStorageRange() {
        ModelAnalysis large = new ModelAnalysis(ModelAnalysis.Status.READY, "obj", "mm",
                1000, 1000, 1000, 1_000_000, 60_000, 50_000, "large");
        ShopPrintPriceProfile expensive = new ShopPrintPriceProfile(1, 2, "FDM", "NYLON",
                new BigDecimal("1000000"), new BigDecimal("100"), new BigDecimal("5"),
                new BigDecimal("100000000"), new BigDecimal("0.001"), new BigDecimal("50"),
                new BigDecimal("5"), new BigDecimal("10"), new BigDecimal("10"),
                new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("1000"),
                new BigDecimal("100000000"), BigDecimal.ZERO, new BigDecimal("0.01"),
                new BigDecimal("10"), 0, 100, 365, true);

        assertThrows(IllegalArgumentException.class, () -> estimator.estimate(large, expensive, 100,
                new BigDecimal("0.01"), 100_000, List.of(), List.of(), false));
    }

    @Test
    void allowsEstimateAfterConfirmingUnusualButValidObjDimensions() {
        ModelAnalysis unusual = new ModelAnalysis(ModelAnalysis.Status.NEEDS_CONFIRMATION, "obj", "mm",
                0.01, 1, 1, 0.00001, 0.1, 12, "confirm");
        PrintPriceEstimate result = estimator.estimate(unusual, profile(), 20,
                new BigDecimal("0.2"), 1, List.of(), List.of(), true);
        assertTrue(result.totalEstimate().signum() > 0);
    }

    @Test
    void rejectsInvalidPricingOptionsAndDiscountTiersBeforeCalculating() {
        PrintPriceOption invalidOption = new PrintPriceOption(1, 1, "COLOR", "red", "Red", new BigDecimal("-1"), true);
        PrintQuantityDiscount invalidDiscount = new PrintQuantityDiscount(2, new BigDecimal("0.95"));
        assertThrows(IllegalArgumentException.class, () -> estimator.estimate(model, profile(), 20,
                new BigDecimal("0.2"), 1, List.of(invalidOption), List.of(), false));
        assertThrows(IllegalArgumentException.class, () -> estimator.estimate(model, profile(), 20,
                new BigDecimal("0.2"), 1, List.of(), List.of(invalidDiscount), false));
    }

    @Test
    void refusesNonObjAnalysisResults() {
        ModelAnalysis stl = new ModelAnalysis(ModelAnalysis.Status.READY, "stl", "mm",
                10, 10, 10, 1, 6, 12, "legacy");
        assertThrows(IllegalArgumentException.class, () -> estimator.estimate(stl, profile(), 20,
                new BigDecimal("0.2"), 1, List.of(), List.of(), false));
    }

    private ShopPrintPriceProfile profile() {
        return new ShopPrintPriceProfile(1, 2, "FDM", "PLA",
                new BigDecimal("10"), new BigDecimal("1.24"), new BigDecimal("0.05"),
                new BigDecimal("50000"), new BigDecimal("10"), new BigDecimal("1.2"),
                new BigDecimal("0.05"), BigDecimal.ONE, new BigDecimal("0.2"),
                BigDecimal.ONE, BigDecimal.ONE, new BigDecimal("0.05"),
                BigDecimal.ZERO, new BigDecimal("25000"), new BigDecimal("0.08"),
                new BigDecimal("0.4"), 5, 100, 3, true);
    }
}
