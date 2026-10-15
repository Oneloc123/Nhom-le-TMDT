package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ProductStore;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Product;

import java.util.Optional;

public class ProductDetailService {
    public Optional<Product> findProduct(int productId) {
        return ProductStore.findById(productId);
    }
}
