package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ProductRecommendationDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ProductRecommendationDao.CandidateProduct;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ProductRecommendationDao.UserSignal;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Product;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.ShopOffer;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Dịch vụ gợi ý sản phẩm kết hợp:
 * - Lọc dựa trên nội dung (danh mục, vật liệu, phương pháp in, mức giá, từ khóa/tên/mô tả).
 * - Tín hiệu hành vi cá nhân (yêu thích, giỏ hàng, đơn hàng, đánh giá tích cực/tiêu cực).
 * - Độ phổ biến (số lượng đã bán, lượt xem, điểm &amp; số đánh giá) cho khách vãng lai hoặc cold-start.
 */
public class ProductRecommendationService {

    private static final Set<String> STOP_WORDS = Set.of(
            "san", "pham", "in", "theo", "yeu", "cau", "va", "cho", "cua", "mau", "loai", "cao", "cap", "gia");

    /** Hồ sơ sở thích được tổng hợp từ các tín hiệu hành vi của người dùng. */
    static final class UserProfile {
        final Map<Long, Double> categoryWeights = new HashMap<>();
        final Map<String, Double> materialWeights = new HashMap<>();
        final Map<String, Double> methodWeights = new HashMap<>();
        final Map<String, Double> keywordWeights = new HashMap<>();
        final Set<Long> viewedProductIds = new HashSet<>();
        final Set<Long> favoriteProductIds = new HashSet<>();
        final Set<Long> cartOrPurchasedIds = new HashSet<>();
        final Set<Long> dislikedProductIds = new HashSet<>();
        double targetPrice;
        boolean hasPositiveSignals;

        static UserProfile fromSignals(List<UserSignal> signals) {
            UserProfile profile = new UserProfile();
            if (signals == null || signals.isEmpty()) {
                return profile;
            }
            double priceSum = 0;
            double priceWeight = 0;
            for (UserSignal signal : signals) {
                double w = signalWeight(signal);
                if ("VIEW".equals(signal.source())) {
                    profile.viewedProductIds.add(signal.productId());
                } else if ("FAVORITE".equals(signal.source())) {
                    profile.favoriteProductIds.add(signal.productId());
                } else if ("CART".equals(signal.source()) || "ORDER".equals(signal.source())) {
                    profile.cartOrPurchasedIds.add(signal.productId());
                }
                if (signal.returned() || ("ORDER".equals(signal.source()) && signal.rating() > 0 && signal.rating() <= 2)) {
                    profile.dislikedProductIds.add(signal.productId());
                }
                if (w > 0) {
                    profile.hasPositiveSignals = true;
                }
                if (signal.categoryId() > 0) {
                    profile.categoryWeights.merge(signal.categoryId(), w, Double::sum);
                }
                addNormalizedWeight(profile.materialWeights, signal.material(), w);
                addNormalizedWeight(profile.methodWeights, signal.printingMethod(), w);
                if (w > 0) {
                    for (String token : extractTokens(signal.productName(), signal.searchKeywords(), signal.description())) {
                        profile.keywordWeights.merge(token, w, Double::sum);
                    }
                    if (signal.unitPrice() != null && signal.unitPrice().doubleValue() > 0) {
                        priceSum += signal.unitPrice().doubleValue() * w;
                        priceWeight += w;
                    }
                }
            }
            if (priceWeight > 0) {
                profile.targetPrice = priceSum / priceWeight;
            }
            return profile;
        }
    }

    /** Gợi ý "Gợi ý dành cho bạn" cho trang chủ; khách chưa có lịch sử nhận danh sách phổ biến. */
    public List<Product> recommendForUser(Long userId, int limit) {
        return recommendForUser(userId, null, limit);
    }

    /** Gợi ý "Gợi ý dành cho bạn" theo tài khoản và lịch sử xem phiên truy cập. */
    public List<Product> recommendForUser(Long userId, String sessionKey, int limit) {
        int safeLimit = clampLimit(limit, 48);
        List<CandidateProduct> candidates = ProductRecommendationDao.loadApprovedCandidates();
        List<UserSignal> signals = ProductRecommendationDao.loadUserSignals(userId, sessionKey);
        return rankForUser(candidates, signals, safeLimit);
    }

    /** Kiểm tra người dùng hoặc phiên truy cập có tín hiệu sở thích tích cực hay không. */
    public boolean hasPersonalSignals(Long userId) {
        return hasPersonalSignals(userId, null);
    }

    /** Kiểm tra người dùng hoặc phiên truy cập có tín hiệu sở thích tích cực hay không. */
    public boolean hasPersonalSignals(Long userId, String sessionKey) {
        return UserProfile.fromSignals(ProductRecommendationDao.loadUserSignals(userId, sessionKey)).hasPositiveSignals;
    }

    /** Gợi ý sản phẩm liên quan trên trang chi tiết bằng nội dung + độ phổ biến + sở thích cá nhân. */
    public List<Product> recommendRelated(Product target, Long userId, int limit) {
        return recommendRelated(target, userId, null, limit);
    }

    /** Gợi ý sản phẩm liên quan trên trang chi tiết theo cả userId và lịch sử xem phiên truy cập. */
    public List<Product> recommendRelated(Product target, Long userId, String sessionKey, int limit) {
        if (target == null) {
            return List.of();
        }
        int safeLimit = clampLimit(limit, 12);
        List<CandidateProduct> candidates = ProductRecommendationDao.loadApprovedCandidates();
        List<UserSignal> signals = ProductRecommendationDao.loadUserSignals(userId, sessionKey);
        return rankRelated(target, candidates, signals, safeLimit);
    }

    /** Chấm điểm và xếp hạng danh sách ứng viên cho trang chủ (thuần bộ nhớ, dễ kiểm thử đơn vị). */
    List<Product> rankForUser(List<CandidateProduct> candidates, List<UserSignal> signals, int limit) {
        if (candidates == null || candidates.isEmpty() || limit <= 0) {
            return List.of();
        }
        UserProfile profile = UserProfile.fromSignals(signals);
        double maxCatWeight = maxPositiveValue(profile.categoryWeights);
        double maxMatWeight = maxPositiveValue(profile.materialWeights);
        double maxMethodWeight = maxPositiveValue(profile.methodWeights);
        double maxKeywordScore = maxPositiveValue(profile.keywordWeights) * 3.0;

        List<ScoredProduct> scored = new ArrayList<>();
        for (CandidateProduct candidate : candidates) {
            Product product = candidate.product();
            if (!isEligibleProduct(product)) {
                continue;
            }
            double score = popularityScore(product, candidate.soldQuantity());
            if (profile.hasPositiveSignals) {
                score += personalAffinityScore(
                        product, profile, maxCatWeight, maxMatWeight, maxMethodWeight, maxKeywordScore);
                if (profile.dislikedProductIds.contains(product.getId())) {
                    score -= 100.0;
                } else if (profile.cartOrPurchasedIds.contains(product.getId())) {
                    score -= 18.0;
                } else if (profile.favoriteProductIds.contains(product.getId())) {
                    score -= 8.0;
                } else if (profile.viewedProductIds.contains(product.getId())) {
                    score -= 12.0;
                }
            }
            scored.add(new ScoredProduct(product, score, candidate.soldQuantity()));
        }

        scored.sort((a, b) -> {
            int cmp = Double.compare(b.score(), a.score());
            if (cmp != 0) return cmp;
            cmp = Long.compare(b.soldQuantity(), a.soldQuantity());
            if (cmp != 0) return cmp;
            cmp = Integer.compare(b.product().getViewCount(), a.product().getViewCount());
            if (cmp != 0) return cmp;
            return Long.compare(a.product().getId(), b.product().getId());
        });

        List<Product> result = new ArrayList<>(Math.min(limit, scored.size()));
        for (int i = 0; i < scored.size() && result.size() < limit; i++) {
            result.add(scored.get(i).product());
        }
        return result;
    }

    /** Chấm điểm sản phẩm liên quan theo nội dung tương đồng với sản phẩm đang xem. */
    List<Product> rankRelated(Product target, List<CandidateProduct> candidates, List<UserSignal> signals, int limit) {
        if (target == null || candidates == null || candidates.isEmpty() || limit <= 0) {
            return List.of();
        }
        UserProfile profile = UserProfile.fromSignals(signals);
        double maxCatWeight = maxPositiveValue(profile.categoryWeights);
        double maxMatWeight = maxPositiveValue(profile.materialWeights);
        double maxMethodWeight = maxPositiveValue(profile.methodWeights);

        Set<String> targetMaterials = normalizedMaterials(target);
        Set<String> targetMethods = normalizedMethods(target);
        Set<String> targetTokens = extractTokens(target.getName(), target.getSearchKeywords(), target.getDescription());
        double targetPrice = target.getPrice() != null ? target.getPrice().doubleValue() : 0.0;

        List<ScoredProduct> scored = new ArrayList<>();
        for (CandidateProduct candidate : candidates) {
            Product product = candidate.product();
            if (!isEligibleProduct(product) || product.getId() == target.getId()) {
                continue;
            }
            double score = 0.0;
            if (target.getCategoryId() > 0 && product.getCategoryId() == target.getCategoryId()) {
                score += 40.0;
            }
            if (sharesAny(targetMaterials, normalizedMaterials(product))) {
                score += 20.0;
            }
            if (sharesAny(targetMethods, normalizedMethods(product))) {
                score += 18.0;
            }
            score += 12.0 * priceProximity(targetPrice, product.getPrice() != null ? product.getPrice().doubleValue() : 0.0);
            score += 15.0 * jaccard(targetTokens, extractTokens(product.getName(), product.getSearchKeywords(), product.getDescription()));
            score += popularityScore(product, candidate.soldQuantity()) * 0.5;
            if (profile.hasPositiveSignals) {
                score += personalAffinityScore(product, profile, maxCatWeight, maxMatWeight, maxMethodWeight, 0.0) * 0.15;
                if (profile.dislikedProductIds.contains(product.getId())) {
                    score -= 60.0;
                }
            }
            scored.add(new ScoredProduct(product, score, candidate.soldQuantity()));
        }

        scored.sort((a, b) -> {
            int cmp = Double.compare(b.score(), a.score());
            if (cmp != 0) return cmp;
            cmp = Long.compare(b.soldQuantity(), a.soldQuantity());
            if (cmp != 0) return cmp;
            return Long.compare(a.product().getId(), b.product().getId());
        });

        List<Product> result = new ArrayList<>(Math.min(limit, scored.size()));
        for (int i = 0; i < scored.size() && result.size() < limit; i++) {
            result.add(scored.get(i).product());
        }
        return result;
    }

    private static double personalAffinityScore(
            Product product,
            UserProfile profile,
            double maxCatWeight,
            double maxMatWeight,
            double maxMethodWeight,
            double maxKeywordScore) {
        double score = 0.0;
        double catW = profile.categoryWeights.getOrDefault(product.getCategoryId(), 0.0);
        if (catW > 0 && maxCatWeight > 0) {
            score += 35.0 * (catW / maxCatWeight);
        } else if (catW < 0) {
            score += Math.max(-20.0, catW * 5.0);
        }

        double bestMat = bestOfferWeight(product, profile.materialWeights, true);
        if (bestMat > 0 && maxMatWeight > 0) {
            score += 22.0 * (bestMat / maxMatWeight);
        }

        double bestMethod = bestOfferWeight(product, profile.methodWeights, false);
        if (bestMethod > 0 && maxMethodWeight > 0) {
            score += 18.0 * (bestMethod / maxMethodWeight);
        }

        double candidatePrice = product.getPrice() != null ? product.getPrice().doubleValue() : 0.0;
        score += 12.0 * priceProximity(profile.targetPrice, candidatePrice);

        if (maxKeywordScore > 0 && !profile.keywordWeights.isEmpty()) {
            Set<String> candidateTokens = extractTokens(
                    product.getName(), product.getSearchKeywords(), product.getDescription());
            double kwSum = 0.0;
            for (String token : candidateTokens) {
                kwSum += profile.keywordWeights.getOrDefault(token, 0.0);
            }
            score += 15.0 * Math.min(1.0, kwSum / maxKeywordScore);
        }
        return score;
    }

    private static double popularityScore(Product product, long soldQuantity) {
        double ratingWeight = product.getReviewCount() > 0 ? product.getRating() : product.getRating() * 0.5;
        return ratingWeight
                + Math.log1p(Math.max(0, soldQuantity)) * 2.5
                + Math.log1p(Math.max(0, product.getViewCount())) * 1.5
                + Math.log1p(Math.max(0, product.getReviewCount())) * 1.5;
    }

    private static double signalWeight(UserSignal signal) {
        return switch (signal.source()) {
            case "CART" -> 4.0 + Math.min(2.0, Math.max(0, signal.quantity() - 1) * 0.5);
            case "FAVORITE" -> 3.0;
            case "VIEW" -> {
                double days = Math.max(0.0, signal.hoursAgo() / 24.0);
                // Lượt xem mới (trong 24h) có trọng số cao hơn và giảm dần trong 30 ngày.
                yield Math.max(0.8, 2.4 * Math.exp(-days / 10.0));
            }
            case "ORDER" -> {
                if (signal.returned()) yield -2.0;
                if (signal.rating() >= 5) yield 3.5;
                if (signal.rating() == 4) yield 2.5;
                if (signal.rating() == 3) yield 0.5;
                if (signal.rating() > 0) yield signal.rating() == 1 ? -2.5 : -1.5;
                yield 2.0 + Math.min(1.0, Math.max(0, signal.quantity() - 1) * 0.25);
            }
            default -> 1.0;
        };
    }

    private static boolean isEligibleProduct(Product product) {
        return product != null && product.isActive() && product.isAvailable();
    }

    private static double bestOfferWeight(Product product, Map<String, Double> weights, boolean useMaterial) {
        double best = 0.0;
        for (ShopOffer offer : product.getOffers()) {
            if (!offer.isPurchasable()) {
                continue;
            }
            String key = normalizeText(useMaterial ? offer.getMaterial() : offer.getPrintingMethod());
            if (!key.isEmpty()) {
                best = Math.max(best, weights.getOrDefault(key, 0.0));
            }
        }
        return best;
    }

    private static Set<String> normalizedMaterials(Product product) {
        Set<String> set = new HashSet<>();
        for (ShopOffer offer : product.getOffers()) {
            if (offer.isPurchasable()) {
                String norm = normalizeText(offer.getMaterial());
                if (!norm.isEmpty()) {
                    set.add(norm);
                }
            }
        }
        return set;
    }

    private static Set<String> normalizedMethods(Product product) {
        Set<String> set = new HashSet<>();
        for (ShopOffer offer : product.getOffers()) {
            if (offer.isPurchasable()) {
                String norm = normalizeText(offer.getPrintingMethod());
                if (!norm.isEmpty()) {
                    set.add(norm);
                }
            }
        }
        return set;
    }

    private static boolean sharesAny(Set<String> left, Set<String> right) {
        for (String item : left) {
            if (right.contains(item)) {
                return true;
            }
        }
        return false;
    }

    private static double priceProximity(double targetPrice, double candidatePrice) {
        if (targetPrice <= 0 || candidatePrice <= 0) {
            return 0.0;
        }
        double max = Math.max(targetPrice, candidatePrice);
        double diffRatio = Math.abs(candidatePrice - targetPrice) / max;
        return Math.max(0.0, 1.0 - diffRatio);
    }

    private static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        int intersection = 0;
        for (String token : a) {
            if (b.contains(token)) {
                intersection++;
            }
        }
        if (intersection == 0) {
            return 0.0;
        }
        return intersection / (double) (a.size() + b.size() - intersection);
    }

    private static Set<String> extractTokens(String... parts) {
        Set<String> tokens = new HashSet<>();
        for (String part : parts) {
            String norm = normalizeText(part);
            if (norm.isEmpty()) {
                continue;
            }
            for (String token : norm.split("\\s+")) {
                if (token.length() >= 2 && !STOP_WORDS.contains(token)) {
                    tokens.add(token);
                }
            }
        }
        return tokens;
    }

    private static void addNormalizedWeight(Map<String, Double> map, String rawKey, double weight) {
        String key = normalizeText(rawKey);
        if (!key.isEmpty()) {
            map.merge(key, weight, Double::sum);
        }
    }

    private static double maxPositiveValue(Map<?, Double> map) {
        double max = 0.0;
        for (Double value : map.values()) {
            if (value != null && value > max) {
                max = value;
            }
        }
        return max;
    }

    private static String normalizeText(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String decomposed = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'd')
                .toLowerCase(Locale.ROOT);
        return decomposed.replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static int clampLimit(int limit, int max) {
        return Math.min(Math.max(1, limit), max);
    }

    private record ScoredProduct(Product product, double score, long soldQuantity) {
    }
}
