package vn.edu.hcmuaf.fit.tmdt_nhomle;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Set;

@WebServlet(name = "workshopRouterServlet", urlPatterns = "/workshop/*")
public class WorkshopRouterServlet extends HttpServlet {
    private static final Set<String> PAGES =
            Set.of("/", "/dashboard", "/profile", "/products", "/orders", "/quotes", "/reviews");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getPathInfo();
        if (path == null || path.isBlank()) path = "/";
        if (!PAGES.contains(path)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.setHeader("Cache-Control", "no-store");
        if ("/".equals(path)) {
            response.sendRedirect(request.getContextPath() + "/workshop/dashboard");
            return;
        }
        request.getRequestDispatcher("/WEB-INF/workshop/index.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
}
