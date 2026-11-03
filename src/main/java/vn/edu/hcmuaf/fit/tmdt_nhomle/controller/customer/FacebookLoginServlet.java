package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Tắt luồng Facebook giả lập cũ cho tới khi triển khai xác minh OAuth2 thật. */
@WebServlet(name = "facebookLoginServlet", value = "/login/facebook")
public class FacebookLoginServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Không tạo phiên đăng nhập dựa trên callback chưa xác thực từ trình duyệt.
        response.sendRedirect(request.getContextPath() + "/login?oauth=unavailable");
    }
}
