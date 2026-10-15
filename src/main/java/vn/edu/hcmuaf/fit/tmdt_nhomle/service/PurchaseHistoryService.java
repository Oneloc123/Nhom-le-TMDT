package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.OrderDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Order;

import java.util.List;

/** Lịch sử mua hàng từ database — luôn theo user của phiên. */
public class PurchaseHistoryService {
    public List<Order> findByCustomer(long customerId) {
        return OrderDao.listByUser(customerId);
    }
}
