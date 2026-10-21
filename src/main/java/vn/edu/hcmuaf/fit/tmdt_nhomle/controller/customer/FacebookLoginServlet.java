package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * Nút “Đăng nhập bằng Facebook” — CHỈ DEMO GIAO DIỆN.
 *
 * <p>Endpoint này KHÔNG gọi Facebook, KHÔNG chuyển hướng OAuth, KHÔNG tạo tài khoản
 * ảo và KHÔNG đăng nhập phiên “social” giả. Mọi yêu cầu chỉ trả về thông báo
 * OAuth chưa được cấu hình; nút bấm trên trang login/register mở modal demo.</p>
 *
 * <pre>
 * // TODO(OAUTH): Register a Facebook Login application and configure app ID,
 * //   redirect URI, scopes, state, and callback validation before enabling this
 * //   integration. App secret phải đặt qua biến môi trường — KHÔNG commit.
 * //
 * // Khi kích hoạt, lộ trình chuẩn sẽ là:
 * //   1) GET /login/facebook → redirect tới
 * //      https://www.facebook.com/v19.0/dialog/oauth
 * //        ?client_id=${FACEBOOK_APP_ID}
 * //        &amp;redirect_uri=${FACEBOOK_REDIRECT_URI}
 * //        &amp;scope=email&amp;state=${csrfRandomState}   (verify state khi callback)
 * //   2) Callback: đổi code -&gt; token qua graph.facebook.com/oauth/access_token
 * //      với ${FACEBOOK_APP_SECRET} (biến môi trường, không hard-code)
 * //   3) Đọc /me?fields=id,email (verify EAA... token) rồi MỚI dùng
 * //      user_auth_providers (provider='facebook', provider_subject=id) để liên kết
 * //      hoặc tạo tài khoản — không tin email chưa xác thực từ provider.
 * //   4) Không lưu access token vào database, không ghi secret ra log.
 * </pre>
 */
@WebServlet(name = "facebookLoginServlet", value = "/login/facebook")
public class FacebookLoginServlet extends HttpServlet {

    static final String DEMO_MESSAGE =
            "Đăng nhập Facebook ở chế độ demo: OAuth chưa được cấu hình nên hệ thống "
                    + "chưa kết nối tới Facebook và không đăng nhập tài khoản nào. "
                    + "Chủ dự án cần cấp app ID/secret để bật tính năng này.";

    // TODO(OAUTH): private static final String APP_ID = env("FACEBOOK_APP_ID");
    // TODO(OAUTH): private static final String REDIRECT_URI = env("FACEBOOK_REDIRECT_URI");
    // TODO(OAUTH): private static final String APP_SECRET = env("FACEBOOK_APP_SECRET");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // KHÔNG gửi redirect tới facebook.com khi chưa có credentials.
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
                            "provider", "facebook", "configured", false)));
            return;
        }
        request.setAttribute("socialDemo", DEMO_MESSAGE);
        request.setAttribute("demoOtpNote",
                vn.edu.hcmuaf.fit.tmdt_nhomle.util.DemoOtp.DEMO_NOTE);
        request.getRequestDispatcher("/customer/login.jsp").forward(request, response);
    }
}
