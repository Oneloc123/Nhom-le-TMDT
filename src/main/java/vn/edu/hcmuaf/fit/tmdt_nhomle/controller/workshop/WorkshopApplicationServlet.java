package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.workshop;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.WorkshopDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.AuthFilter;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet(name = "workshopApplicationServlet", urlPatterns = "/workshop/apply")
public class WorkshopApplicationServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = AuthFilter.currentUser(request);
        if (user != null) {
            List<Map<String, Object>> ownedShops = WorkshopDao.listOwnedShops(user.getId());
            if (WorkshopDao.hasActiveVerifiedShop(user.getId())) {
                response.sendRedirect(request.getContextPath() + "/workshop/dashboard");
                return;
            }
            if (!ownedShops.isEmpty()) request.setAttribute("existingShop", ownedShops.get(0));
        }
        request.setAttribute("csrfToken", Csrf.ensureToken(request));
        request.getRequestDispatcher("/WEB-INF/workshop/apply.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        User user = AuthFilter.currentUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        try {
            WorkshopDao.createApplication(user.getId(), request.getParameter("shopName"),
                    request.getParameter("contactPhone"), request.getParameter("contactEmail"),
                    request.getParameter("province"), request.getParameter("district"),
                    request.getParameter("description"));
            response.sendRedirect(request.getContextPath() + "/workshop/apply?submitted=1");
        } catch (IllegalArgumentException e) {
            request.setAttribute("error", e.getMessage());
            if (WorkshopDao.hasOwnedShop(user.getId())) {
                List<Map<String, Object>> ownedShops = WorkshopDao.listOwnedShops(user.getId());
                if (!ownedShops.isEmpty()) request.setAttribute("existingShop", ownedShops.get(0));
            }
            request.setAttribute("csrfToken", Csrf.ensureToken(request));
            request.getRequestDispatcher("/WEB-INF/workshop/apply.jsp").forward(request, response);
        } catch (DataAccessException e) {
            request.setAttribute("error", "Chưa thể lưu đăng ký xưởng. Vui lòng thử lại sau.");
            request.setAttribute("csrfToken", Csrf.ensureToken(request));
            request.getRequestDispatcher("/WEB-INF/workshop/apply.jsp").forward(request, response);
        }
    }
}
