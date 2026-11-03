package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ShopDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Store;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.SuggestedShop;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShopRecommendationService {

    private static final Pattern DIM_PATTERN = Pattern.compile("(\\d+)\\s*[xX*×]\\s*(\\d+)\\s*[xX*×]\\s*(\\d+)");

    public List<SuggestedShop> getSuggestedShops(String tech, String material,
                                                 double sizeX, double sizeY, double sizeZ,
                                                 String fileExt) {
        List<Store> allStores = ShopDao.search("");
        List<SuggestedShop> results = new ArrayList<>();

        String normTech = tech == null ? "fdm" : tech.trim().toLowerCase(Locale.ROOT);
        String normMat = material == null ? "pla" : material.trim().toLowerCase(Locale.ROOT);
        String normExt = fileExt == null ? "stl" : fileExt.trim().toLowerCase(Locale.ROOT).replace(".", "");

        for (Store store : allStores) {
            SuggestedShop suggested = new SuggestedShop();
            suggested.setShopId(store.getId());
            suggested.setShopName(store.getName());
            suggested.setSlug(store.getSlug());
            suggested.setLocation(store.getLocation());
            suggested.setRating(store.getRating());
            suggested.setCompletedOrders(store.getCompletedOrders());
            suggested.setEstimatedLeadDays(store.getEstimatedLeadDays() != null ? store.getEstimatedLeadDays() : 3);
            suggested.setCapabilities(store.getCapabilities() != null ? store.getCapabilities() : store.getSpecialty());
            suggested.setMaxBuildDimensions(store.getMaxBuildDimensions());

            int score = 0;
            List<String> reasons = new ArrayList<>();

            String caps = (store.getCapabilities() + " " + store.getSpecialty() + " " + store.getDescription())
                    .toLowerCase(Locale.ROOT);

            if (caps.contains(normTech)) {
                score += 45;
                reasons.add("Hỗ trợ in " + normTech.toUpperCase(Locale.ROOT));
            }

            String[] matKeywords = getMaterialKeywords(normMat);
            boolean matMatched = false;
            for (String kw : matKeywords) {
                if (caps.contains(kw)) {
                    matMatched = true;
                    break;
                }
            }
            if (matMatched) {
                score += 35;
                reasons.add("Chuyên vật liệu " + normMat.toUpperCase(Locale.ROOT));
            }

            String formats = store.getAcceptedFileFormats() == null ? "stl, obj"
                    : store.getAcceptedFileFormats().toLowerCase(Locale.ROOT);
            if (formats.contains(normExt)) {
                score += 10;
            }

            if (sizeX > 0 && sizeY > 0 && sizeZ > 0 && store.getMaxBuildDimensions() != null) {
                Matcher m = DIM_PATTERN.matcher(store.getMaxBuildDimensions());
                if (m.find()) {
                    try {
                        double bx = Double.parseDouble(m.group(1));
                        double by = Double.parseDouble(m.group(2));
                        double bz = Double.parseDouble(m.group(3));
                        if (sizeX <= bx && sizeY <= by && sizeZ <= bz) {
                            score += 15;
                            reasons.add("Bàn in " + (int)bx + "×" + (int)by + "×" + (int)bz + "mm vừa kích thước");
                        } else {
                            score -= 20;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            score += (int) Math.round(store.getRating() * 2);
            score += Math.min((int) store.getCompletedOrders(), 10);

            suggested.setMatchScore(score);
            if (reasons.isEmpty()) {
                suggested.setMatchReason("Xưởng in uy tín trên sàn PrintKraft");
            } else {
                suggested.setMatchReason(String.join(", ", reasons));
            }

            results.add(suggested);
        }

        results.sort(Comparator.comparingInt(SuggestedShop::getMatchScore).reversed());

        if (!results.isEmpty()) {
            SuggestedShop best = results.getFirst();
            best.setBestMatch(true);
            best.setMatchBadge("Phù hợp nhất");

            SuggestedShop fastest = null;
            for (int i = 1; i < results.size(); i++) {
                SuggestedShop s = results.get(i);
                if (fastest == null || s.getEstimatedLeadDays() < fastest.getEstimatedLeadDays()) {
                    fastest = s;
                }
            }
            if (fastest != null && fastest.getEstimatedLeadDays() <= 3) {
                fastest.setMatchBadge("Giao nhanh");
            }

            for (int i = 1; i < results.size(); i++) {
                SuggestedShop s = results.get(i);
                if (s != fastest && s.getRating() >= 4.8) {
                    s.setMatchBadge("Đánh giá cao");
                    break;
                }
            }
        }

        return results;
    }

    private String[] getMaterialKeywords(String mat) {
        if (mat.contains("resin")) {
            return new String[]{"resin", "sla"};
        } else if (mat.contains("pla")) {
            return new String[]{"pla", "fdm"};
        } else if (mat.contains("abs")) {
            return new String[]{"abs", "fdm"};
        } else if (mat.contains("petg")) {
            return new String[]{"petg", "fdm"};
        } else if (mat.contains("tpu")) {
            return new String[]{"tpu", "dẻo", "flex"};
        } else if (mat.contains("nylon")) {
            return new String[]{"nylon", "pa12", "sls"};
        }
        return new String[]{mat};
    }
}
