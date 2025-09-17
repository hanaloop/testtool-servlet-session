package com.hanaloop.tool.auth;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWTClaimsSet;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Object of this class manages
 */
public class HanaEcoSessionManager {
    private static final Logger LOGGER = Logger.getLogger(HanaEcoSessionManager.class.getName());

    private static final String COOKIE_EL_TOKEN = "el-token";
    private static final String COOKIE_NEXT_AUTH = "next-auth.session-token";
    private static final String COOKIE_NEXT_AUTH_SECURE = "__Secure-next-auth.session-token";
    private static final String COOKIE_JSESSIONID = "JSESSIONID";
    private static final int COOKIE_MAX_AGE = 60 * 60; // 60 minutes
    private static final int NEXTAUTH_KEY_LENGTH = 32;
    private static final int HKDF_HASH_LEN = 32;
    private static final String NEXTAUTH_SALT = "";
    private static final String NEXTAUTH_INFO_PREFIX = "NextAuth.js Generated Encryption Key";
    private static final String SESSION_AUDIENCE = "local";
    private static final String SESSION_ISSUER = "local";

    public String generateNextAuthToken(User user,
            String accessToken,
            String secret,
            long issuedAt,
            long expiresAt) throws Exception {
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .subject(user.getUserId()) // or user.getUserUid() if you add it
                .issuer(SESSION_ISSUER)
                .audience(SESSION_AUDIENCE)
                .issueTime(new Date(issuedAt * 1000))
                .expirationTime(new Date(expiresAt * 1000))
                .jwtID(UUID.randomUUID().toString())
                .claim("name", user.getName())
                .claim("email", user.getEmail())
                .claim("accessToken", accessToken);

        JWEHeader header = new JWEHeader.Builder(JWEAlgorithm.DIR, EncryptionMethod.A256GCM)
                .build();

        EncryptedJWT jwt = new EncryptedJWT(header, claims.build());

        byte[] encryptionKey = deriveNextAuthEncryptionKey(secret, NEXTAUTH_SALT);
        DirectEncrypter encrypter = new DirectEncrypter(encryptionKey);

        jwt.encrypt(encrypter);

        return jwt.serialize();
    }

    private boolean handleJsession = false;

    public HanaEcoSessionManager(boolean useJsession) {
        this.handleJsession = useJsession;
    }

    /**
     * Issues cookies for session authentication.
     */
    public void addCookies(HttpServletRequest req, HttpServletResponse resp, User user, String secret)
            throws Exception {
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }

        long issuedAt = System.currentTimeMillis() / 1000L;
        long expiresAt = issuedAt + COOKIE_MAX_AGE;

        // Generate access token for el-token cookie
        String accessToken = JwtUtil.generateElToken(
                user.getUserId(),
                issuedAt,
                expiresAt,
                SESSION_AUDIENCE,
                SESSION_ISSUER,
                secret);

        // Generate NextAuth session token
        String nextAuthToken = generateNextAuthToken(user, accessToken, secret, issuedAt, expiresAt);

        boolean isSecure = req.isSecure();
        String nextAuthCookieName = isSecure ? COOKIE_NEXT_AUTH_SECURE : COOKIE_NEXT_AUTH;
        resp.addCookie(createCookie(nextAuthCookieName, nextAuthToken, isSecure, COOKIE_MAX_AGE));

        // Always issue the el-token cookie
        resp.addCookie(createCookie(COOKIE_EL_TOKEN, accessToken, isSecure, COOKIE_MAX_AGE));

        LOGGER.fine(() -> String.format(
                "Issued session cookies for userId='%s' exp=%d (secure=%s)",
                user.getUserId(), expiresAt, isSecure));
    }

    /**
     * Removes authentication-related cookies.
     */
    public void destroyCookies(HttpServletRequest req, HttpServletResponse resp) {
        boolean isSecure = req.isSecure();

        // Remove issued cookies. Cookie removal is setting cookie with immediate expiration.
        resp.addCookie(removeCookie(COOKIE_EL_TOKEN, isSecure));
        resp.addCookie(removeCookie(COOKIE_NEXT_AUTH, isSecure));
        resp.addCookie(removeCookie(COOKIE_NEXT_AUTH_SECURE, isSecure));
        LOGGER.fine("Destroyed authentication cookies");

        if (this.handleJsession) {
            // Best-effort removal of JSESSIONID
            String sessionPath = req.getContextPath().isEmpty() ? "/" : req.getContextPath();
            resp.addCookie(removeCookie(COOKIE_JSESSIONID, isSecure, sessionPath));
            LOGGER.fine("Destroyed JSESSION cookie");
        }

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

    private static byte[] deriveNextAuthEncryptionKey(String secret, String salt) throws Exception {
        String info = NEXTAUTH_INFO_PREFIX + (isBlank(salt) ? "" : " (" + salt + ")");
        byte[] saltBytes = isBlank(salt)
                ? new byte[HKDF_HASH_LEN]
                : salt.getBytes(StandardCharsets.UTF_8);
        byte[] ikm = secret.getBytes(StandardCharsets.UTF_8);
        byte[] infoBytes = info.getBytes(StandardCharsets.UTF_8);
        return hkdfSha256(ikm, saltBytes, infoBytes, NEXTAUTH_KEY_LENGTH);
    }

    private static byte[] hkdfSha256(byte[] ikm, byte[] salt, byte[] info, int outputLength) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(salt, "HmacSHA256"));
        byte[] prk = mac.doFinal(ikm);

        byte[] okm = new byte[outputLength];
        byte[] previous = new byte[0];
        int generated = 0;
        int counter = 1;

        while (generated < outputLength) {
            Mac loopMac = Mac.getInstance("HmacSHA256");
            loopMac.init(new SecretKeySpec(prk, "HmacSHA256"));
            loopMac.update(previous);
            loopMac.update(info);
            loopMac.update((byte) counter);
            previous = loopMac.doFinal();

            int bytesToCopy = Math.min(previous.length, outputLength - generated);
            System.arraycopy(previous, 0, okm, generated, bytesToCopy);
            generated += bytesToCopy;
            counter++;
        }

        return okm;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

}
