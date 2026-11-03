package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Tắt luồng Google giả lập cũ: chỉ bật lại sau khi cấu hình và xác minh OAuth2 ID token thật. */
@WebServlet(name = "googleLoginServlet", value = "/login/google")
public class GoogleLoginServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Không đăng nhập tài khoản demo chỉ vì callback có query parameter "code".
        response.sendRedirect(request.getContextPath() + "/login?oauth=unavailable");
    }
}
