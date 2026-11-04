package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import vn.edu.hcmuaf.fit.tmdt_nhomle.model.ModelAnalysis;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.PrintPriceEstimate;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.PrintPriceOption;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.PrintQuantityDiscount;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.ShopPrintPriceProfile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

/** Transparent estimate only; it intentionally does not claim slicer accuracy. */
public final class PrintPriceEstimator {
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal MAX_ORDER_AMOUNT = new BigDecimal("9999999999.99");
    private static final String DISCLAIMER = "Giá ước tính theo cấu hình xưởng; không phải kết quả slicer. Xưởng xác nhận giá cuối cùng.";

    public PrintPriceEstimate estimate(ModelAnalysis model, ShopPrintPriceProfile profile,
                                       int infillPercent, BigDecimal layerHeightMm, int quantity,
                                       List<PrintPriceOption> selectedOptions,
                                       List<PrintQuantityDiscount> discounts,
                                       boolean dimensionsConfirmed) {
        if (model == null || (model.status() != ModelAnalysis.Status.READY
                && !(dimensionsConfirmed && model.status() == ModelAnalysis.Status.NEEDS_CONFIRMATION))) {
            throw new IllegalArgumentException("Mô hình chưa được phân tích hoặc cần khách xác nhận kích thước.");
        }
        validateProfile(profile);
        if (quantity < 1 || quantity > 100_000) throw new IllegalArgumentException("Số lượng phải từ 1 đến 100.000.");
        if (infillPercent < profile.minimumInfillPercent() || infillPercent > profile.maximumInfillPercent()) {
            throw new IllegalArgumentException("Tỷ lệ infill nằm ngoài phạm vi xưởng hỗ trợ.");
        }
        BigDecimal layer = layerHeightMm == null ? ZERO : layerHeightMm;
        if (layer.compareTo(profile.minimumLayerHeightMm()) < 0
                || layer.compareTo(profile.maximumLayerHeightMm()) > 0) {
            throw new IllegalArgumentException("Layer height nằm ngoài phạm vi xưởng hỗ trợ.");
        }

        // Surface area (cm²) × wall thickness (mm / 10) approximates shell volume (cm³).
        BigDecimal solidVolume = decimal(model.volumeCm3());
        BigDecimal shellVolume = decimal(model.surfaceAreaCm2())
                .multiply(profile.wallThicknessMm()).divide(BigDecimal.TEN, 8, RoundingMode.HALF_UP)
                .min(solidVolume);
        BigDecimal infillRate = BigDecimal.valueOf(infillPercent).divide(HUNDRED, 8, RoundingMode.HALF_UP)
                .multiply(profile.useInfillFactor() ? profile.infillUsageFactor() : ONE);
        BigDecimal infillVolume = solidVolume.subtract(shellVolume).max(ZERO).multiply(infillRate);
        BigDecimal baseMaterialVolume = shellVolume.add(infillVolume);
        BigDecimal supportVolume = solidVolume.multiply(profile.supportMaterialFactor());
        BigDecimal materialVolume = baseMaterialVolume.add(supportVolume);
        BigDecimal materialGrams = materialVolume.multiply(profile.materialDensityGramsPerCm3())
                .multiply(ONE.add(profile.materialWasteFactor()));

        BigDecimal layerFactor = profile.useLayerHeightFactor()
                ? profile.referenceLayerHeightMm().divide(layer, 8, RoundingMode.HALF_UP) : ONE;
        BigDecimal technologyFactor = profile.useTechnologyFactor() ? profile.technologyFactor() : ONE;
        BigDecimal complexityFactor = profile.useComplexityFactor() ? profile.complexityFactor() : ONE;
        BigDecimal hours = profile.setupHours().add(materialVolume
                .divide(profile.depositionRateCm3PerHour(), 8, RoundingMode.HALF_UP)
                .multiply(layerFactor).multiply(technologyFactor).multiply(complexityFactor));
        BigDecimal materialCost = profile.useMaterialCost() ? materialGrams.multiply(profile.materialPricePerGram()) : ZERO;
        BigDecimal machineCost = profile.useMachineCost() ? hours.multiply(profile.machineCostPerHour()) : ZERO;
        BigDecimal optionFees = !profile.usePostProcessingFee() || selectedOptions == null ? ZERO : selectedOptions.stream()
                .filter(PrintPriceOption::active).map(PrintPriceOption::fee)
                .filter(value -> value != null).reduce(ZERO, BigDecimal::add);
        BigDecimal processingCost = (profile.usePostProcessingFee() ? profile.postProcessingFee() : ZERO).add(optionFees);

        BigDecimal discountRate = !profile.useQuantityDiscount() || discounts == null ? ZERO : discounts.stream()
                .filter(discount -> discount != null && discount.minimumQuantity() <= quantity)
                .max(Comparator.comparingInt(PrintQuantityDiscount::minimumQuantity))
                .map(PrintQuantityDiscount::discountRate).orElse(ZERO);
        if (discountRate.signum() < 0 || discountRate.compareTo(ONE) > 0) {
            throw new IllegalArgumentException("Mức chiết khấu của xưởng không hợp lệ.");
        }
        BigDecimal perPiece = materialCost.add(machineCost).add(processingCost);
        BigDecimal total = perPiece.multiply(BigDecimal.valueOf(quantity))
                .multiply(ONE.subtract(discountRate));
        if (profile.useMinimumPrice()) total = total.max(profile.minimumOrderPrice());
        total = total
                .setScale(2, RoundingMode.HALF_UP);
        if (total.signum() <= 0) {
            throw new IllegalArgumentException("Cấu hình giá của xưởng cho ra tổng tiền bằng 0; hãy bật ít nhất một thành phần chi phí.");
        }
        if (total.compareTo(MAX_ORDER_AMOUNT) > 0) {
            throw new IllegalArgumentException("Giá ước tính vượt giới hạn lưu trữ của đơn hàng; hãy gửi yêu cầu báo giá.");
        }
        return new PrintPriceEstimate(materialGrams.setScale(3, RoundingMode.HALF_UP),
                hours.setScale(3, RoundingMode.HALF_UP), money(materialCost), money(machineCost),
                money(processingCost), discountRate, total, quantity, profile.productionDays(), DISCLAIMER);
    }

    private static void validateProfile(ShopPrintPriceProfile profile) {
        if (profile == null || !profile.active()) throw new IllegalArgumentException("Xưởng chưa cấu hình mức giá cho lựa chọn này.");
        positive(profile.materialPricePerGram(), "Giá vật liệu");
        positive(profile.materialDensityGramsPerCm3(), "Mật độ vật liệu");
        positive(profile.depositionRateCm3PerHour(), "Năng suất máy");
        positive(profile.wallThicknessMm(), "Độ dày vỏ");
        positive(profile.referenceLayerHeightMm(), "Layer height tham chiếu");
        positive(profile.minimumLayerHeightMm(), "Layer height nhỏ nhất");
        positive(profile.maximumLayerHeightMm(), "Layer height lớn nhất");
        nonNegative(profile.materialWasteFactor(), "Hệ số hao hụt");
        nonNegative(profile.machineCostPerHour(), "Chi phí máy");
        nonNegative(profile.supportMaterialFactor(), "Hệ số vật liệu hỗ trợ");
        nonNegative(profile.infillUsageFactor(), "Hệ số infill");
        positive(profile.technologyFactor(), "Hệ số công nghệ");
        positive(profile.complexityFactor(), "Hệ số độ phức tạp");
        nonNegative(profile.setupHours(), "Thời gian chuẩn bị");
        nonNegative(profile.postProcessingFee(), "Phí gia công");
        nonNegative(profile.minimumOrderPrice(), "Giá tối thiểu");
        if (profile.minimumLayerHeightMm().compareTo(profile.maximumLayerHeightMm()) > 0
                || profile.minimumInfillPercent() < 0 || profile.maximumInfillPercent() > 100
                || profile.minimumInfillPercent() > profile.maximumInfillPercent()) {
            throw new IllegalArgumentException("Phạm vi cấu hình giá của xưởng không hợp lệ.");
        }
    }

    private static void positive(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) throw new IllegalArgumentException(field + " của xưởng chưa hợp lệ.");
    }

    private static void nonNegative(BigDecimal value, String field) {
        if (value == null || value.signum() < 0) throw new IllegalArgumentException(field + " của xưởng chưa hợp lệ.");
    }

    private static BigDecimal decimal(double value) {
        if (!Double.isFinite(value) || value <= 0) throw new IllegalArgumentException("Thông số hình học không hợp lệ.");
        return BigDecimal.valueOf(value);
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
