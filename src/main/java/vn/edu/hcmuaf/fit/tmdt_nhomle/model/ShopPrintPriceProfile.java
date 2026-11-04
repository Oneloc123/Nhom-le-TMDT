package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

import java.math.BigDecimal;

/** Per-shop rates for one printing technology and material combination. */
public record ShopPrintPriceProfile(
        long profileId,
        long shopId,
        String printingMethod,
        String material,
        BigDecimal materialPricePerGram,
        BigDecimal materialDensityGramsPerCm3,
        BigDecimal materialWasteFactor,
        BigDecimal machineCostPerHour,
        BigDecimal depositionRateCm3PerHour,
        BigDecimal wallThicknessMm,
        BigDecimal supportMaterialFactor,
        BigDecimal infillUsageFactor,
        BigDecimal referenceLayerHeightMm,
        BigDecimal technologyFactor,
        BigDecimal complexityFactor,
        BigDecimal setupHours,
        BigDecimal postProcessingFee,
        BigDecimal minimumOrderPrice,
        BigDecimal minimumLayerHeightMm,
        BigDecimal maximumLayerHeightMm,
        int minimumInfillPercent,
        int maximumInfillPercent,
        int productionDays,
        boolean useMaterialCost,
        boolean useMachineCost,
        boolean useInfillFactor,
        boolean useLayerHeightFactor,
        boolean useTechnologyFactor,
        boolean useComplexityFactor,
        boolean usePostProcessingFee,
        boolean useQuantityDiscount,
        boolean useMinimumPrice,
        boolean active) {
    public ShopPrintPriceProfile(long profileId,long shopId,String printingMethod,String material,
                                 BigDecimal materialPricePerGram,BigDecimal materialDensityGramsPerCm3,
                                 BigDecimal materialWasteFactor,BigDecimal machineCostPerHour,
                                 BigDecimal depositionRateCm3PerHour,BigDecimal wallThicknessMm,
                                 BigDecimal supportMaterialFactor,BigDecimal infillUsageFactor,
                                 BigDecimal referenceLayerHeightMm,BigDecimal technologyFactor,
                                 BigDecimal complexityFactor,BigDecimal setupHours,BigDecimal postProcessingFee,
                                 BigDecimal minimumOrderPrice,BigDecimal minimumLayerHeightMm,
                                 BigDecimal maximumLayerHeightMm,int minimumInfillPercent,int maximumInfillPercent,
                                 int productionDays,boolean active) {
        this(profileId,shopId,printingMethod,material,materialPricePerGram,materialDensityGramsPerCm3,
                materialWasteFactor,machineCostPerHour,depositionRateCm3PerHour,wallThicknessMm,
                supportMaterialFactor,infillUsageFactor,referenceLayerHeightMm,technologyFactor,
                complexityFactor,setupHours,postProcessingFee,minimumOrderPrice,minimumLayerHeightMm,
                maximumLayerHeightMm,minimumInfillPercent,maximumInfillPercent,productionDays,
                true,true,true,true,true,true,true,true,true,active);
    }
}
