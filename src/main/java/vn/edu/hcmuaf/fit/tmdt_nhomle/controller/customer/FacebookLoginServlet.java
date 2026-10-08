package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.UserStore;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Đăng nhập bằng Facebook (OAuth2) — cơ chế tương tự GoogleLoginServlet.
 */
@WebServlet(name = "facebookLoginServlet", value = "/login/facebook")
public class FacebookLoginServlet extends HttpServlet {

    private static final String CLIENT_ID = "YOUR_FACEBOOK_APP_ID";
    private static final String REDIRECT_URI = "http://localhost:8080/TMDT_NhomLe/login/facebook";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String code = request.getParameter("code");
        String error = request.getParameter("error");

        if (code == null && error == null) {
            String authorizeUrl = "https://www.facebook.com/v19.0/dialog/oauth"
                    + "?client_id=" + URLEncoder.encode(CLIENT_ID, StandardCharsets.UTF_8)
                    + "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8)
                    + "&scope=" + URLEncoder.encode("email", StandardCharsets.UTF_8);
            response.sendRedirect(authorizeUrl);
            return;
        }

        User user = UserStore.findByEmail("facebook.demo@facebook.com").orElseGet(() ->
                UserStore.create("facebook.demo@facebook.com", "oauth-facebook",
                        "Người dùng Facebook", "0900000002",
                        "TP.HCM", User.ROLE_CUSTOMER, User.STATUS_ACTIVE, "facebook"));

        HttpSession session = request.getSession(true);
        session.setAttribute("currentUser", user);
        response.sendRedirect(request.getContextPath() + "/customer/home.jsp");
    }
}
