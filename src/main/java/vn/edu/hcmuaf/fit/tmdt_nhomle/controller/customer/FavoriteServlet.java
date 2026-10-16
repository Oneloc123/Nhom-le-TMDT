package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.FavoriteDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ProductDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Product;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.AuthFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "favoriteServlet", value = "/favorites")
public class FavoriteServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = AuthFilter.currentUser(request);
        List<Product> products = new ArrayList<>();
        for (Long productId : FavoriteDao.listProductIds(user.getId())) {
            ProductDao.findById(productId).ifPresent(products::add);
        }
        request.setAttribute("products", products);
        request.getRequestDispatcher("/customer/favorites.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        User user = AuthFilter.currentUser(request);
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        long productId = parseLong(request.getParameter("productId"), -1);
        if (productId > 0) {
            if ("remove".equals(action)) {
                FavoriteDao.remove(user.getId(), productId);
            } else if ("add".equals(action)) {
                FavoriteDao.add(user.getId(), productId);
            }
        }
        response.sendRedirect(request.getContextPath() + "/favorites");
    }

    private long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
