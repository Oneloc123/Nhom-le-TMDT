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

@WebServlet(name = "facebookLoginServlet", value = "/login/facebook")
public class FacebookLoginServlet extends HttpServlet {
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!AppSecrets.facebookConfigured()) {
            resp.sendRedirect(req.getContextPath() + "/login?oauth=unavailable");
            return;
        }
        byte[] buf = new byte[24]; RANDOM.nextBytes(buf);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(buf);

        HttpSession session = req.getSession(true);
        session.setAttribute("oauth_state_facebook", state);
        String redirect = req.getParameter("redirect");
        if (redirect != null && !redirect.isBlank())
            session.setAttribute("oauth_redirect_facebook", redirect);

        String url = "https://www.facebook.com/" + AppSecrets.FACEBOOK_GRAPH_VERSION + "/dialog/oauth"
                + "?client_id=" + OAuthHttp.urlEnc(AppSecrets.FACEBOOK_APP_ID)
                + "&redirect_uri=" + OAuthHttp.urlEnc(AppSecrets.FACEBOOK_REDIRECT_URI)
                + "&state=" + OAuthHttp.urlEnc(state)
                + "&response_type=code"
                + "&scope=" + OAuthHttp.urlEnc("email,public_profile");
        resp.sendRedirect(url);
    }
}