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
    private static final int PAGE_SIZE = 10;
    private final PurchaseHistoryService purchaseHistoryService = new PurchaseHistoryService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = currentUser(request.getSession(false));
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        int requestedPage = parsePage(request.getParameter("page"));
        PurchaseHistoryService.Page result = purchaseHistoryService.findByCustomer(
                user.getId(), requestedPage, PAGE_SIZE);
        request.setAttribute("orders", result.items());
        request.setAttribute("currentPage", result.currentPage());
        request.setAttribute("totalPages", result.totalPages());

        HttpSession session = request.getSession(false);
        if (session != null) {
            Object notice = session.getAttribute("orderNotice");
            if (notice != null) {
                request.setAttribute("notice", notice.toString());
                session.removeAttribute("orderNotice");
            }
        }
        request.getRequestDispatcher("/customer/purchase-history.jsp").forward(request, response);
    }

    private int parsePage(String value) {
        try {
            return Math.max(1, Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private User currentUser(HttpSession session) {
        return session == null ? null : (User) session.getAttribute("currentUser");
    }
}
