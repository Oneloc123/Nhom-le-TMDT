package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.CategoryDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ProductDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Product;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "homeServlet", urlPatterns = {"/home"})
public class HomeServlet extends HttpServlet {

    private static final int SECTION_SIZE = 4;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        List<Product> bestsellers = ProductDao.getBestsellers(4);
        request.setAttribute("bestsellers", bestsellers);
        List<Product> trending = ProductDao.getTrending(4);
        request.setAttribute("trending", trending);
        request.setAttribute("categories", CategoryDao.findAllActive());
        request.getRequestDispatcher("/customer/home.jsp").forward(request, response);
    }
}
