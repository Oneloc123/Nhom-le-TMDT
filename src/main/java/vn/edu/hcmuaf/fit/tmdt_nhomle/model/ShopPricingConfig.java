package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

import java.util.List;

public record ShopPricingConfig(ShopPrintPriceProfile profile,
                                List<PrintPriceOption> options,
                                List<PrintQuantityDiscount> discounts) { }
