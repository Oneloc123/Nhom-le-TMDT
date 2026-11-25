package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.user;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.UserSessionRegistry;

import java.io.IOException;

@WebServlet(name = "userLogoutV2", value = "/logout")
public class LogoutController extends UserControllerSupport {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        resp.setHeader("Pragma", "no-cache");

        HttpSession session = req.getSession(false);
        User user = currentUser(req);
        if (session != null) {
            if (user != null) UserSessionRegistry.unregister(user.getId(), session);
            session.invalidate();
        }

        var cookieConfig = req.getServletContext().getSessionCookieConfig();
        String cookieName = cookieConfig.getName();
        if (cookieName == null || cookieName.isBlank()) cookieName = "JSESSIONID";
        String cookiePath = cookieConfig.getPath();
        if (cookiePath == null || cookiePath.isBlank()) {
            cookiePath = req.getContextPath().isEmpty() ? "/" : req.getContextPath();
        }
        Cookie expiredSession = new Cookie(cookieName, "");
        expiredSession.setMaxAge(0);
        expiredSession.setPath(cookiePath);
        expiredSession.setHttpOnly(true);
        expiredSession.setSecure(req.isSecure());
        resp.addCookie(expiredSession);

        resp.sendRedirect(req.getContextPath() + "/login?loggedOut=1");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setHeader("Cache-Control", "no-store");
        resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        resp.setHeader("Allow", "POST");
    }
}
