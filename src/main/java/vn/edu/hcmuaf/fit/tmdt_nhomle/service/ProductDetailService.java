package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ProductDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Product;

import java.util.List;
import java.util.Optional;

public class ProductDetailService {
    private final ProductRecommendationService recommendationService = new ProductRecommendationService();

    public Optional<Product> findProduct(long productId) {
        return ProductDao.findById(productId);
    }

    public List<Product> findRelatedProducts(Product product, int limit) {
        return findRelatedProducts(product, null, null, limit);
    }

    /** Nâng cấp sản phẩm liên quan theo nội dung tương đồng, độ phổ biến và sở thích khách hàng. */
    public List<Product> findRelatedProducts(Product product, Long userId, int limit) {
        return findRelatedProducts(product, userId, null, limit);
    }

    /** Nâng cấp sản phẩm liên quan theo nội dung tương đồng, độ phổ biến và sở thích theo user/session. */
    public List<Product> findRelatedProducts(Product product, Long userId, String sessionKey, int limit) {
        if (product == null) {
            return List.of();
        }
        return recommendationService.recommendRelated(product, userId, sessionKey, limit);
    }
}
