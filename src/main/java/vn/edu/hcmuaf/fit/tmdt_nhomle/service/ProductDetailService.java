package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ProductDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Product;

import java.util.List;
import java.util.Optional;

public class ProductDetailService {
    public Optional<Product> findProduct(long productId) {
        return ProductDao.findById(productId);
    }

    public List<Product> findRelatedProducts(Product product, int limit) {
        if (product == null || product.getCategoryId() <= 0) {
            return List.of();
        }
        return ProductDao.findRelated(product.getId(), product.getCategoryId(), limit);
    }
}
