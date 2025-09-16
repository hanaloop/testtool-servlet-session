package com.hanaloop.tool.auth;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Logger;

public class HanaEcoSessionManager {
    private static final Logger LOGGER = Logger.getLogger(HanaEcoSessionManager.class.getName());

    private static final String COOKIE_EL_TOKEN = "el-token";
    private static final String COOKIE_NEXT_AUTH = "next-auth.session-token";
    private static final String COOKIE_NEXT_AUTH_SECURE = "__Secure-next-auth.session-token";
    private static final String COOKIE_JSESSIONID = "JSESSIONID";
    private static final int COOKIE_MAX_AGE = 60 * 60; // 60 minutes

    /**
     * Issues cookies for session authentication.
     */
    public void addCookies(HttpServletRequest req, HttpServletResponse resp, String userId, String secret) throws Exception {
        long issuedAt = System.currentTimeMillis() / 1000L;
        long expiresAt = issuedAt + COOKIE_MAX_AGE;

        String token = JwtUtil.generateElToken(userId, issuedAt, expiresAt, "local", "local", secret);
        boolean isSecure = req.isSecure();

        // Always issue the el-token cookie
        resp.addCookie(createCookie(COOKIE_EL_TOKEN, token, isSecure, COOKIE_MAX_AGE));

        // Use different cookie name depending on secure
        String nextAuthCookieName = isSecure ? COOKIE_NEXT_AUTH_SECURE : COOKIE_NEXT_AUTH;
        resp.addCookie(createCookie(nextAuthCookieName, token, isSecure, COOKIE_MAX_AGE));

        LOGGER.fine(() -> String.format("Issued session cookies for userId='%s' exp=%d (secure=%s)",
                userId, expiresAt, isSecure));
    }

    /**
     * Removes authentication-related cookies.
     */
    public void destroyCookies(HttpServletRequest req, HttpServletResponse resp) {
        boolean isSecure = req.isSecure();

        // Remove issued cookies
        resp.addCookie(removeCookie(COOKIE_EL_TOKEN, isSecure));
        resp.addCookie(removeCookie(COOKIE_NEXT_AUTH, isSecure));
        resp.addCookie(removeCookie(COOKIE_NEXT_AUTH_SECURE, isSecure));

        // Best-effort removal of JSESSIONID
        String sessionPath = req.getContextPath().isEmpty() ? "/" : req.getContextPath();
        resp.addCookie(removeCookie(COOKIE_JSESSIONID, isSecure, sessionPath));

        LOGGER.fine("Destroyed authentication cookies");
    }

    /**
     * Creates a cookie with consistent defaults.
     */
    private Cookie createCookie(String name, String value, boolean secure, int maxAgeSeconds) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeSeconds);
        if (secure) {
            cookie.setSecure(true);
        }
        return cookie;
    }

    /**
     * Removes a cookie by setting max age to 0.
     */
    private Cookie removeCookie(String name, boolean secure) {
        return removeCookie(name, secure, "/");
    }

    private Cookie removeCookie(String name, boolean secure, String path) {
        Cookie cookie = new Cookie(name, "");
        cookie.setHttpOnly(true);
        cookie.setPath(path);
        cookie.setMaxAge(0);
        if (secure) {
            cookie.setSecure(true);
        }
        return cookie;
    }
}
