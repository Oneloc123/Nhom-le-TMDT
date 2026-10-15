package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ProductStore;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Product;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public class ProductFilterService {
    public List<Product> filter(String query, String category, BigDecimal minPrice,
                                BigDecimal maxPrice, Integer storeId, String sort) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        String normalizedCategory = category == null ? "" : category.trim();

        List<Product> products = ProductStore.findAll().stream()
                .filter(product -> normalizedQuery.isEmpty()
                        || product.getName().toLowerCase().contains(normalizedQuery)
                        || product.getDescription().toLowerCase().contains(normalizedQuery)
                        || product.getStoreName().toLowerCase().contains(normalizedQuery))
                .filter(product -> normalizedCategory.isEmpty()
                        || product.getCategory().equals(normalizedCategory))
                .filter(product -> minPrice == null || product.getPrice().compareTo(minPrice) >= 0)
                .filter(product -> maxPrice == null || product.getPrice().compareTo(maxPrice) <= 0)
                .filter(product -> storeId == null || product.getStoreId() == storeId)
                .toList();

        Comparator<Product> comparator = switch (sort == null ? "" : sort) {
            case "priceAsc" -> Comparator.comparing(Product::getPrice);
            case "priceDesc" -> Comparator.comparing(Product::getPrice).reversed();
            case "rating" -> Comparator.comparing(Product::getRating).reversed();
            default -> Comparator.comparing(Product::getReviewCount).reversed();
        };

        return products.stream().sorted(comparator).toList();
    }
}
