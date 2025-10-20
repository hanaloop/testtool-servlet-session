package com.hanaloop.tool.auth;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWTClaimsSet;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Connects the servlet login flow with NextAuth by generating the same cookies
 * it normally sets.
 * NextAuth encrypts its session token as a JWE using "dir/A256GCM".
 * That key is derived from {@code NEXTAUTH_SECRET} with HKDF (a key-derivation
 * function). To work with Nextauth, we must reproduce that derivation exactly;
 * otherwise it rejects
 * and clears the cookie.
 */
public class HanaEcoSessionManager {
    private static final Logger LOGGER = Logger.getLogger(HanaEcoSessionManager.class.getName());

    // Constants for el-token setting
    private static final String COOKIE_EL_TOKEN = "el-token";
    private static final String COOKIE_JSESSIONID = "JSESSIONID";

    private static final String SESSION_AUDIENCE = "local";
    private static final String SESSION_ISSUER = "local";

    // Constants for NextAuth cookie setting
    private static final String COOKIE_NEXT_AUTH = "next-auth.session-token";
    private static final String COOKIE_NEXT_AUTH_SECURE = "__Secure-next-auth.session-token";
    private static final int NEXTAUTH_KEY_LENGTH = 32;
    private static final int HKDF_HASH_LEN = 32;
    private static final String NEXTAUTH_SALT = "";
    private static final String NEXTAUTH_INFO_PREFIX = "NextAuth.js Generated Encryption Key"; //This is actually needed and if changed, it will not work

    private String secret;
    private boolean handleJsession = false;

    public HanaEcoSessionManager(String secret, boolean handleJsession) {
        this.secret = secret;
        this.handleJsession = handleJsession;
    }
    

    /**
     * Issues both the Hanaeco's {@code el-token} JWT and NextAuth session cookie (encrypted JWE).
     * The method aligns with NextAuth cookie semantics: secure prefix when the request is HTTPS, 
     * and a 1 hour lifetime to match the surrounding servlet session.
     */
    public void addCookies(HttpServletRequest req, HttpServletResponse resp, User user, int cookieDurationSeconds)
            throws Exception {
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }

        boolean isSecure = req.isSecure();
        long issuedAt = System.currentTimeMillis() / 1000L;
        long expiresAt = issuedAt + cookieDurationSeconds;

        String companyCd = "fta01";
        // Generate access token for el-token cookie
        String accessToken = JwtUtil.generateElToken(
                user.getUserId(),
                issuedAt,
                expiresAt,
                SESSION_AUDIENCE,
                SESSION_ISSUER,
                "fta",
                companyCd,
                this.secret);

        // Add the el-token cookie
        resp.addCookie(createCookie(COOKIE_EL_TOKEN, accessToken, isSecure, cookieDurationSeconds));

        // Generate NextAuth session token
        String nextAuthToken = generateNextAuthToken(user, accessToken, issuedAt, expiresAt);

        // NextAuth uses different name depending on http/s
        String nextAuthCookieName = isSecure ? COOKIE_NEXT_AUTH_SECURE : COOKIE_NEXT_AUTH;
        resp.addCookie(createCookie(nextAuthCookieName, nextAuthToken, isSecure, cookieDurationSeconds));

        LOGGER.fine(() -> String.format(
                "Issued session cookies for userId='%s' exp=%d (secure=%s)",
                user.getUserId(), expiresAt, isSecure));
    }

    /**
     * Removes authentication-related cookies.
     */
    public void destroyCookies(HttpServletRequest req, HttpServletResponse resp) {
        boolean isSecure = req.isSecure();

        // Remove issued cookies. Cookie removal is setting cookie with immediate
        // expiration.
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

    /**
     * Generates NextAuth compliant token
     */
    private String generateNextAuthToken(User user,
            String accessToken,
            long issuedAt,
            long expiresAt) throws Exception {
        // NextAuth stores the user payload inside an encrypted JWT. Claims (pieces of
        // info) below mirror what a standard Nextauth credentials provider provides by default
        // (sub/email/name/etc.).
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

        // The derived key must be byte-for-byte identical to Nextauth' hkdf("sha256") output.
        byte[] encryptionKey = deriveNextAuthEncryptionKey( NEXTAUTH_SALT);

        // Make new encrypter using the encryption key generated above with oru secret + salt
        DirectEncrypter encrypter = new DirectEncrypter(encryptionKey);

        // use encrypter to encrypt jwt
        jwt.encrypt(encrypter);

        return jwt.serialize();
    }

    /**
     * Derives the symmetric encryption key used by Nextauth. The framework takes
     * {@code secret},
     * runs HKDF-SHA256 with a fixed info string, and uses the first 32 bytes of output. 
     * Matching this process exactly ensures interoperability even when NextAuth rotates
     * secrets.
     */
    private byte[] deriveNextAuthEncryptionKey(String salt) {
        String info = NEXTAUTH_INFO_PREFIX + (isBlank(salt) ? "" : " (" + salt + ")");
        byte[] saltBytes = isBlank(salt)
                ? new byte[HKDF_HASH_LEN] // 32 bytes of zeros
                : salt.getBytes(StandardCharsets.UTF_8);
        
        byte[] ikm = this.secret.getBytes(StandardCharsets.UTF_8); //Get number of bytes
        byte[] infoBytes = info.getBytes(StandardCharsets.UTF_8);

        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA256Digest());
        hkdf.init(new HKDFParameters(ikm, saltBytes, infoBytes));

        byte[] okm = new byte[NEXTAUTH_KEY_LENGTH];
        hkdf.generateBytes(okm, 0, NEXTAUTH_KEY_LENGTH);

        return okm;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

}
