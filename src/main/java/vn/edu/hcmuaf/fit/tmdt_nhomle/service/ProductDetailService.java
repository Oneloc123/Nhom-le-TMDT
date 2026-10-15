package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ProductDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Product;

import java.util.Optional;

/** Chi tiết sản phẩm từ database (kèm offer của các gian hàng). */
public class ProductDetailService {
    public Optional<Product> findProduct(long productId) {
        return ProductDao.findById(productId);
    }
}
