package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.PurchaseHistoryService;

import java.io.IOException;
import java.util.Map;

@WebServlet(name = "purchaseHistoryServlet", value = "/purchase-history")
public class PurchaseHistoryServlet extends HttpServlet {
    private final PurchaseHistoryService purchaseHistoryService = new PurchaseHistoryService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = currentUser(request.getSession(false));
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        String q = request.getParameter("q");
        String status = request.getParameter("status");
        int page = parseInt(request.getParameter("page"), 1);
        try {
            Map<String, Object> result = vn.edu.hcmuaf.fit.tmdt_nhomle.dao.OrderDao
                    .searchByUser(user.getId(), q, status, page, 5);
            request.setAttribute("orders", result.get("items"));
            request.setAttribute("total", result.get("total"));
            request.setAttribute("page", result.get("page"));
            request.setAttribute("pages", result.get("pages"));
        } catch (IllegalArgumentException e) {
            request.setAttribute("error", e.getMessage());
            request.setAttribute("orders", purchaseHistoryService.findByCustomer(user.getId()));
        }
        request.setAttribute("q", q);
        request.setAttribute("status", status);
        request.setAttribute("statuses", java.util.List.of(
                vn.edu.hcmuaf.fit.tmdt_nhomle.model.OrderStatus.PENDING_PAYMENT,
                vn.edu.hcmuaf.fit.tmdt_nhomle.model.OrderStatus.CONFIRMED,
                vn.edu.hcmuaf.fit.tmdt_nhomle.model.OrderStatus.PROCESSING,
                vn.edu.hcmuaf.fit.tmdt_nhomle.model.OrderStatus.SHIPPED,
                vn.edu.hcmuaf.fit.tmdt_nhomle.model.OrderStatus.COMPLETED,
                vn.edu.hcmuaf.fit.tmdt_nhomle.model.OrderStatus.CANCELLED));
        request.setAttribute("paid", "1".equals(request.getParameter("paid")));
        request.getRequestDispatcher("/customer/purchase-history.jsp").forward(request, response);
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private User currentUser(HttpSession session) {
        return session == null ? null : (User) session.getAttribute("currentUser");
    }
}
