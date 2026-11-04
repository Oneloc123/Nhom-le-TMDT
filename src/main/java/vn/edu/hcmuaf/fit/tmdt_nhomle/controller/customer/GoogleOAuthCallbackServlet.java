package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.tmdt_nhomle.config.AppSecrets;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.UserSessionRegistry;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.CartService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.OAuthHttp;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.OAuthUserService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.PortalRedirect;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf;

import java.io.IOException;
import java.util.Map;

/** Bước 2: Google trả code → đổi token → lấy userinfo → đăng nhập. */
@WebServlet(name = "googleOAuthCallback", value = "/login/google/callback")
public class GoogleOAuthCallbackServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        try {
            if (!AppSecrets.googleConfigured()) throw new IllegalStateException("Google OAuth chưa được cấu hình.");
            String code  = req.getParameter("code");
            String state = req.getParameter("state");
            String expected = session == null ? null : (String) session.getAttribute("oauth_state_google");
            if (code == null || state == null || expected == null || !expected.equals(state))
                throw new IllegalStateException("Phiên OAuth không hợp lệ, vui lòng thử lại.");
            session.removeAttribute("oauth_state_google");

            // Đổi code -> access_token
            String tokenJson = OAuthHttp.postForm("https://oauth2.googleapis.com/token", Map.of(
                    "code", code,
                    "client_id", AppSecrets.GOOGLE_CLIENT_ID,
                    "client_secret", AppSecrets.GOOGLE_CLIENT_SECRET,
                    "redirect_uri", AppSecrets.GOOGLE_REDIRECT_URI,
                    "grant_type", "authorization_code"));
            String accessToken = OAuthHttp.jsonString(tokenJson, "access_token");
            if (accessToken == null) throw new IllegalStateException("Google không trả access token.");

            // Lấy userinfo
            String info = OAuthHttp.get("https://www.googleapis.com/oauth2/v2/userinfo", accessToken);
            String email = OAuthHttp.jsonString(info, "email");
            String name  = OAuthHttp.jsonString(info, "name");
            String pid   = OAuthHttp.jsonString(info, "id");
            if (pid == null) pid = email;

            User user = OAuthUserService.findOrCreate("google", pid, email, name);
            finishLogin(req, resp, session, user, "oauth_redirect_google");
        } catch (Exception e) {
            String msg = e.getMessage() == null ? "Đăng nhập Google thất bại." : e.getMessage();
            resp.sendRedirect(req.getContextPath() + "/login?oauth=failed&msg=" + OAuthHttp.urlEnc(msg));
        }
    }

    /** Dùng chung cho Google + Facebook — invalidate session cũ, tạo session mới, merge cart. */
    static void finishLogin(HttpServletRequest req, HttpServletResponse resp,
                            HttpSession old, User user, String redirectAttr) throws IOException {
        try { new CartService().mergeGuestCart(old, user); }
        catch (RuntimeException ignored) { }
        String redirect = old == null ? null : (String) old.getAttribute(redirectAttr);
        if (old != null) old.invalidate();
        HttpSession fresh = req.getSession(true);
        fresh.setMaxInactiveInterval(30 * 60);
        fresh.setAttribute("currentUser", user);
        fresh.setAttribute("csrfToken", Csrf.ensureToken(req));
        UserSessionRegistry.register(user.getId(), fresh);
        String target = (redirect != null && redirect.startsWith("/"))
                ? redirect : PortalRedirect.afterLogin(user);
        resp.sendRedirect(req.getContextPath() + target);
    }
}