package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

import java.math.BigDecimal;

public record PrintPriceOption(long optionId, long profileId, String type, String value,
                               String label, BigDecimal fee, boolean active) { }
