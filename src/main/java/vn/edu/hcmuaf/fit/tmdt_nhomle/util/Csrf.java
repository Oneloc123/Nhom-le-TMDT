package vn.edu.hcmuaf.fit.tmdt_nhomle.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.security.SecureRandom;

/**
 * CSRF token đơn giản lưu trong session.
 * JSP nhúng token bằng: <%= Csrf.ensureToken(request) %>
 * (xem include customer/_csrf.jsp), Servlet kiểm tra trong AuthFilter.
 */
public final class Csrf {

    public static final String SESSION_ATTR = "csrfToken";
    public static final String PARAM = "csrfToken";
    private static final SecureRandom RANDOM = new SecureRandom();

    private Csrf() {
    }

    /** Trả về token hiện có hoặc tạo mới trong session. */
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

    /** Kiểm tra token gửi lên có khớp token trong session. */
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
