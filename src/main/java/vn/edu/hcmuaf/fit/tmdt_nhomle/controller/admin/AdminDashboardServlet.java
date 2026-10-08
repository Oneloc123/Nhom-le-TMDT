package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.PrintOrderStore;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.UserStore;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.PrintOrder;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Điều hướng + chuẩn bị dữ liệu cho trang tổng quan (Dashboard) của Admin:
 * số đơn đặt in 3D, doanh thu, số người dùng, đơn theo công nghệ in.
 */
@WebServlet(name = "adminDashboardServlet", value = "/admin/dashboard")
public class AdminDashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User current = (session == null) ? null : (User) session.getAttribute("currentUser");
        if (current == null || !current.isAdmin()) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // Thống kê tổng quan
        request.setAttribute("totalOrders", PrintOrderStore.count());
        request.setAttribute("revenueVnd", PrintOrderStore.revenue());
        request.setAttribute("totalUsers", UserStore.findAll().size());
        request.setAttribute("customerCount", UserStore.countByRole(User.ROLE_CUSTOMER));
        request.setAttribute("adminCount", UserStore.countByRole(User.ROLE_ADMIN));
        request.setAttribute("lockedCount", UserStore.countByStatus(User.STATUS_LOCKED));
        request.setAttribute("pendingCount", UserStore.countByStatus(User.STATUS_PENDING));

        request.setAttribute("newOrders", PrintOrderStore.countByStatus(PrintOrder.STATUS_NEW));
        request.setAttribute("printingOrders", PrintOrderStore.countByStatus(PrintOrder.STATUS_PRINTING));
        request.setAttribute("doneOrders", PrintOrderStore.countByStatus(PrintOrder.STATUS_DONE));
        request.setAttribute("shippedOrders", PrintOrderStore.countByStatus(PrintOrder.STATUS_SHIPPED));

        // Đơn theo công nghệ in (FDM / SLA) cho biểu đồ
        Map<String, Long> byTech = new LinkedHashMap<>();
        byTech.put("FDM", 0L);
        byTech.put("SLA", 0L);
        for (PrintOrder o : PrintOrderStore.findAll()) {
            byTech.merge(o.getTechnology(), 1L, Long::sum);
        }
        request.setAttribute("ordersByTech", byTech);

        // Đơn theo chất liệu (PLA / ABS / Resin)
        Map<String, Long> byMaterial = new LinkedHashMap<>();
        byMaterial.put("PLA", 0L);
        byMaterial.put("ABS", 0L);
        byMaterial.put("Resin", 0L);
        for (PrintOrder o : PrintOrderStore.findAll()) {
            byMaterial.merge(o.getMaterial(), 1L, Long::sum);
        }
        request.setAttribute("ordersByMaterial", byMaterial);

        request.setAttribute("recentOrders", PrintOrderStore.findRecent(5));

        request.getRequestDispatcher("/admin/dashboard.jsp").forward(request, response);
    }
}
