package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * Nút “Đăng nhập bằng Google” — CHỈ DEMO GIAO DIỆN.
 *
 * <p>Endpoint này KHÔNG gọi Google, KHÔNG chuyển hướng OAuth, KHÔNG tạo tài khoản
 * ảo và KHÔNG đăng nhập phiên “social” giả. Mọi yêu cầu chỉ trả về thông báo
 * OAuth chưa được cấu hình; nút bấm trên trang login/register mở modal demo.</p>
 *
 * <pre>
 * // TODO(OAUTH): Register a Google OAuth application and configure client ID,
 * //   redirect URI, scopes, state, and callback validation before enabling this
 * //   integration. Client secret phải đặt qua biến môi trường — KHÔNG commit.
 * //
 * // Khi kích hoạt, lộ trình chuẩn sẽ là:
 * //   1) GET /login/google → redirect tới
 * //      https://accounts.google.com/o/oauth2/v2/auth
 * //        ?client_id=${GOOGLE_CLIENT_ID}
 * //        &amp;redirect_uri=${GOOGLE_REDIRECT_URI}
 * //        &amp;response_type=code&amp;scope=openid%20email%20profile
 * //        &amp;state=${csrfRandomState}   (verify state khi callback)
 * //   2) Callback: đổi code -&gt; token qua POST https://oauth2.googleapis.com/token
 * //      với ${GOOGLE_CLIENT_SECRET} (lấy từ biến môi trường, không hard-code)
 * //   3) Lấy id_token/email, verify audience + email_verified, rồi MỚI dùng
 * //      user_auth_providers (provider='google', provider_subject=sub) để liên kết
 * //      hoặc tạo tài khoản — KHÔNG bao giờ tin email chưa được verify.
 * //   4) Đặt user_auth_providers.linked_email, không lưu access/refresh token
 * //      vào database và không ghi secret ra log.
 * </pre>
 */
@WebServlet(name = "googleLoginServlet", value = "/login/google")
public class GoogleLoginServlet extends HttpServlet {

    static final String DEMO_MESSAGE =
            "Đăng nhập Google ở chế độ demo: OAuth chưa được cấu hình nên hệ thống "
                    + "chưa kết nối tới Google và không đăng nhập tài khoản nào. "
                    + "Chủ dự án cần cấp client ID/secret để bật tính năng này.";

    // TODO(OAUTH): private static final String CLIENT_ID = env("GOOGLE_CLIENT_ID");
    // TODO(OAUTH): private static final String REDIRECT_URI = env("GOOGLE_REDIRECT_URI");
    // TODO(OAUTH): private static final String CLIENT_SECRET = env("GOOGLE_CLIENT_SECRET");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // KHÔNG gửi redirect tới accounts.google.com khi chưa có credentials.
        // TODO(OAUTH): thay phần thân hàm này bằng luồng authorization code nêu
        //   trong comment phía trên SAU KHI chủ dự án cấp credentials và yêu cầu bật.
        respond(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        respond(request, response);
    }

    private void respond(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        boolean wantsJson = "1".equals(request.getParameter("ajax"))
                || "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))
                || "application/json".equalsIgnoreCase(request.getHeader("Accept"));
        if (wantsJson) {
            response.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json;charset=UTF-8");
            response.setHeader("Cache-Control", "no-store");
            response.getWriter().write(vn.edu.hcmuaf.fit.tmdt_nhomle.util.Json.stringify(
                    Map.of("success", false, "message", DEMO_MESSAGE,
                            "provider", "google", "configured", false)));
            return;
        }
        // Fallback không AJAX: quay lại trang đăng nhập với thông báo demo.
        request.setAttribute("socialDemo", DEMO_MESSAGE);
        request.setAttribute("demoOtpNote",
                vn.edu.hcmuaf.fit.tmdt_nhomle.util.DemoOtp.DEMO_NOTE);
        request.getRequestDispatcher("/customer/login.jsp").forward(request, response);
    }
}
