package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.tmdt_nhomle.config.AppSecrets;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.OAuthHttp;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.OAuthUserService;

import java.io.IOException;
import java.util.Map;

@WebServlet(name = "facebookOAuthCallback", value = "/login/facebook/callback")
public class FacebookOAuthCallbackServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        try {
            if (!AppSecrets.facebookConfigured()) throw new IllegalStateException("Facebook OAuth chưa được cấu hình.");
            String code  = req.getParameter("code");
            String state = req.getParameter("state");
            String expected = session == null ? null : (String) session.getAttribute("oauth_state_facebook");
            if (code == null || state == null || expected == null || !expected.equals(state))
                throw new IllegalStateException("Phiên OAuth không hợp lệ, vui lòng thử lại.");
            session.removeAttribute("oauth_state_facebook");

            // Đổi code -> access_token (Facebook dùng GET với query params)
            String tokenUrl = "https://graph.facebook.com/" + AppSecrets.FACEBOOK_GRAPH_VERSION + "/oauth/access_token"
                    + "?client_id="     + OAuthHttp.urlEnc(AppSecrets.FACEBOOK_APP_ID)
                    + "&client_secret=" + OAuthHttp.urlEnc(AppSecrets.FACEBOOK_APP_SECRET)
                    + "&redirect_uri="  + OAuthHttp.urlEnc(AppSecrets.FACEBOOK_REDIRECT_URI)
                    + "&code="          + OAuthHttp.urlEnc(code);
            String tokenJson = OAuthHttp.get(tokenUrl, null);
            String accessToken = OAuthHttp.jsonString(tokenJson, "access_token");
            if (accessToken == null) throw new IllegalStateException("Facebook không trả access token.");

            // Lấy userinfo
            String info = OAuthHttp.get(
                    "https://graph.facebook.com/" + AppSecrets.FACEBOOK_GRAPH_VERSION + "/me?fields=id,name,email&access_token="
                            + OAuthHttp.urlEnc(accessToken), null);
            String pid   = OAuthHttp.jsonString(info, "id");
            String email = OAuthHttp.jsonString(info, "email");
            String name  = OAuthHttp.jsonString(info, "name");
            if (email == null || email.isBlank())
                throw new IllegalStateException(
                        "Tài khoản Facebook không chia sẻ email — hãy đăng nhập bằng email/mật khẩu.");

            User user = OAuthUserService.findOrCreate("facebook", pid, email, name);
            GoogleOAuthCallbackServlet.finishLogin(req, resp, session, user, "oauth_redirect_facebook");
        } catch (Exception e) {
            String msg = e.getMessage() == null ? "Đăng nhập Facebook thất bại." : e.getMessage();
            resp.sendRedirect(req.getContextPath() + "/login?oauth=failed&msg=" + OAuthHttp.urlEnc(msg));
        }
    }
}