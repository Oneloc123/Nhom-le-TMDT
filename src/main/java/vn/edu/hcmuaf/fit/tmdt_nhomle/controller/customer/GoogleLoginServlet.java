package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.tmdt_nhomle.config.AppSecrets;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.OAuthHttp;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

/** Bước 1: redirect trình duyệt sang Google OAuth consent. */
@WebServlet(name = "googleLoginServlet", value = "/login/google")
public class GoogleLoginServlet extends HttpServlet {
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!AppSecrets.googleConfigured()) {
            resp.sendRedirect(req.getContextPath() + "/login?oauth=unavailable");
            return;
        }
        byte[] buf = new byte[24]; RANDOM.nextBytes(buf);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(buf);

        HttpSession session = req.getSession(true);
        session.setAttribute("oauth_state_google", state);
        String redirect = req.getParameter("redirect");
        if (redirect != null && !redirect.isBlank())
            session.setAttribute("oauth_redirect_google", redirect);

        String url = "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + OAuthHttp.urlEnc(AppSecrets.GOOGLE_CLIENT_ID)
                + "&redirect_uri=" + OAuthHttp.urlEnc(AppSecrets.GOOGLE_REDIRECT_URI)
                + "&response_type=code"
                + "&scope=" + OAuthHttp.urlEnc("openid email profile")
                + "&state=" + OAuthHttp.urlEnc(state)
                + "&access_type=online&prompt=select_account";
        resp.sendRedirect(url);
    }
}