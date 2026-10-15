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
        request.setAttribute("orders", purchaseHistoryService.findByCustomer(user.getId()));
        request.setAttribute("paid", "1".equals(request.getParameter("paid")));
        request.getRequestDispatcher("/customer/purchase-history.jsp").forward(request, response);
    }

    private User currentUser(HttpSession session) {
        return session == null ? null : (User) session.getAttribute("currentUser");
    }
}
