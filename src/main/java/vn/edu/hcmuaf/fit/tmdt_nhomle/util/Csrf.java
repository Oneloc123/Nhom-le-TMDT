package vn.edu.hcmuaf.fit.tmdt_nhomle.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.security.SecureRandom;

public final class Csrf {

    public static final String SESSION_ATTR = "csrfToken";
    public static final String PARAM = "csrfToken";
    private static final SecureRandom RANDOM = new SecureRandom();

    private Csrf() {
    }

    public static String ensureToken(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        Object value = session.getAttribute(SESSION_ATTR);
        if (value instanceof String token && !token.isEmpty()) {
            return token;
        }
        byte[] raw = new byte[24];
        RANDOM.nextBytes(raw);
        String token = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        session.setAttribute(SESSION_ATTR, token);
        return token;
    }

    public static boolean validate(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        Object expected = session.getAttribute(SESSION_ATTR);
        String actual = request.getParameter(PARAM);
        return expected instanceof String token && actual != null
                && java.security.MessageDigest.isEqual(
                        token.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        actual.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
