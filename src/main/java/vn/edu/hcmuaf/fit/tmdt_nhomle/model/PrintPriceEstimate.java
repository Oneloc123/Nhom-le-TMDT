package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

import java.math.BigDecimal;

public record PrintPriceEstimate(
        BigDecimal estimatedMaterialGrams,
        BigDecimal estimatedMachineHours,
        BigDecimal materialCost,
        BigDecimal machineCost,
        BigDecimal processingCost,
        BigDecimal discountRate,
        BigDecimal totalEstimate,
        int quantity,
        int productionDays,
        String disclaimer) { }
