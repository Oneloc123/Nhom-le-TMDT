package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.StoreSearchService;

import java.io.IOException;

@WebServlet(name = "storeSearchServlet", value = "/stores")
public class StoreSearchServlet extends HttpServlet {
    private final StoreSearchService storeSearchService = new StoreSearchService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String query = request.getParameter("q") == null ? "" : request.getParameter("q").trim();
        request.setAttribute("stores", storeSearchService.search(query));
        request.setAttribute("query", query);
        request.getRequestDispatcher("/customer/stores.jsp").forward(request, response);
    }
}
