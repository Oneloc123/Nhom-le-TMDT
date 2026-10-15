package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.OrderStore;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Order;

import java.util.List;

public class PurchaseHistoryService {
    public List<Order> findByCustomer(int customerId) {
        return OrderStore.findByCustomerId(customerId);
    }
}
