package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ShopDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Store;

import java.util.List;

/** Tìm kiếm gian hàng (print_shops) trong database. */
public class StoreSearchService {
    public List<Store> search(String query) {
        return ShopDao.search(query);
    }
}
