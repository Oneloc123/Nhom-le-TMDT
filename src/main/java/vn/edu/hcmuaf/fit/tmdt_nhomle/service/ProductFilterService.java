package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ProductDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Product;

import java.math.BigDecimal;
import java.util.List;

public class ProductFilterService {

    public record Page(List<Product> items, long totalItems, int totalPages, int pageNumber) {
    }

    public List<Product> filter(String query, String category, BigDecimal minPrice,
                                BigDecimal maxPrice, Long shopId, String sort) {
        return search(query, category, minPrice, maxPrice, shopId, sort, 1,
                ProductDao.MAX_PAGE_SIZE).items();
    }

    public Page search(String query, String category, BigDecimal minPrice, BigDecimal maxPrice,
                       Long shopId, String sort, int page, int pageSize) {
        ProductDao.SearchCriteria criteria =
                new ProductDao.SearchCriteria(query, category, minPrice, maxPrice, shopId, sort);
        long total = ProductDao.count(criteria);
        int totalPages = (int) Math.max(1, (total + pageSize - 1) / pageSize);
        int safePage = Math.min(Math.max(1, page), totalPages);
        List<Product> items = ProductDao.search(criteria, safePage, pageSize);
        return new Page(items, total, totalPages, safePage);
    }
}
