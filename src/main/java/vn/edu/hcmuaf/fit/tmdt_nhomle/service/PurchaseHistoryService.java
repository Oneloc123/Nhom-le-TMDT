package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.OrderDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Order;

import java.util.List;

public class PurchaseHistoryService {
    public record Page(List<Order> items, long totalItems, int currentPage, int totalPages) {
    }

    public List<Order> findByCustomer(long customerId) {
        return OrderDao.listByUser(customerId);
    }

    public Page findByCustomer(long customerId, int page, int pageSize) {
        long total = OrderDao.countByUser(customerId);
        int pages = (int) Math.max(1, (total + pageSize - 1) / pageSize);
        int safePage = Math.min(Math.max(1, page), pages);
        return new Page(OrderDao.listByUser(customerId, safePage, pageSize),
                total, safePage, pages);
    }
}
