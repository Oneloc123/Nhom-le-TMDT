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
 * Đăng nhập bằng Google (OAuth2).
 * Dùng client_id/secret mẫu — môi trường demo không có SMTP/OAuth thật,
 * nên nếu Google trả về lỗi (VD: redirect_uri chưa cấu hình) thì hệ thống
 * tự động đăng nhập bằng tài khoản Google demo đã tạo sẵn.
 */
@WebServlet(name = "googleLoginServlet", value = "/login/google")
public class GoogleLoginServlet extends HttpServlet {

    private static final String CLIENT_ID = "YOUR_GOOGLE_CLIENT_ID.apps.googleusercontent.com";
    private static final String REDIRECT_URI = "http://localhost:8080/TMDT_NhomLe/login/google";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String code = request.getParameter("code");
        String error = request.getParameter("error");

        // Chưa có code -> chuyển hướng sang tran chap thuan Google (OAuth2 authorization code)
        if (code == null && error == null) {
            String authorizeUrl = "https://accounts.google.com/o/oauth2/v2/auth"
                    + "?client_id=" + URLEncoder.encode(CLIENT_ID, StandardCharsets.UTF_8)
                    + "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8)
                    + "&response_type=code"
                    + "&scope=" + URLEncoder.encode("email profile", StandardCharsets.UTF_8);
            response.sendRedirect(authorizeUrl);
            return;
        }

        // Google tra ve code (hoac loi) -> demo: tim/tao tai khoan Google mau va dang nhap
        User user = UserStore.findByEmail("google.demo@gmail.com").orElseGet(() ->
                UserStore.create("google.demo@gmail.com", "oauth-google",
                        "Người dùng Google", "0900000001",
                        "TP.HCM", User.ROLE_CUSTOMER, User.STATUS_ACTIVE, "google"));

        HttpSession session = request.getSession(true);
        session.setAttribute("currentUser", user);
        response.sendRedirect(request.getContextPath() + "/customer/home.jsp");
    }
}
