package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.AccountSummaryDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.SavedDesignDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.AuthFilter;

import java.io.IOException;
import java.util.Map;

/**
 * Trang tổng quan tài khoản khách (“Tổng quan” trong trung tâm tài khoản).
 * Mọi con số đều lấy trực tiếp từ database của user đang đăng nhập.
 */
@WebServlet(name = "accountServlet", value = "/account")
public class AccountServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = AuthFilter.currentUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        Map<String, Object> overview = AccountSummaryDao.overview(user.getId());
        request.setAttribute("user", user);
        request.setAttribute("overview", overview);
        request.setAttribute("designCount", SavedDesignDao.countByUser(user.getId()));
        request.getRequestDispatcher("/customer/account.jsp").forward(request, response);
    }
}
