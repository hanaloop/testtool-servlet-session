package com.hanaloop.tool;

import com.hanaloop.tool.auth.JwtUtil;
import com.hanaloop.tool.auth.User;
import com.hanaloop.tool.auth.UserStore;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SessionValidatorServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(SessionValidatorServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handle(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handle(req, resp);
    }

    private void handle(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");

        // 1) Check HttpSession
        HttpSession session = req.getSession(false);
        if (session != null) {
            Object u = session.getAttribute("authUser");
            if (u instanceof User) {
                writeUserJson(resp, (User) u, true, "session");
                return;
            }
        }

        // 2) Check Authorization: Bearer <token>
        String token = null;
        String auth = req.getHeader("Authorization");
        if (auth != null && auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = auth.substring(7).trim();
        }

        // 3) Or fallback to el-token cookie
        if (token == null || token.isEmpty()) {
            Cookie[] cookies = req.getCookies();
            if (cookies != null) {
                for (Cookie c : cookies) {
                    if ("el-token".equals(c.getName())) {
                        token = c.getValue();
                        break;
                    }
                }
            }
        }

        if (token != null && !token.isEmpty()) {
            String secret = System.getenv("JWT_SECRET");
            if (secret == null || secret.isEmpty()) {
                LOGGER.warning("JWT_SECRET not set; cannot validate bearer token");
            } else {
                try {
                    JwtUtil.Claims claims = JwtUtil.verifyAndDecode(token, secret);
                    if (claims != null && claims.sub != null) {
                        long now = System.currentTimeMillis() / 1000L;
                        if (claims.exp != 0 && now > claims.exp) {
                            writeUnauth(resp, "token_expired");
                            return;
                        }
                        // Optional: enforce expected aud/iss
                        if ((claims.iss != null && !"local".equals(claims.iss)) || (claims.aud != null && !"local".equals(claims.aud))) {
                            writeUnauth(resp, "token_invalid_issuer_audience");
                            return;
                        }
                        // Enrich from user store if available
                        User found = null;
                        List<User> all = UserStore.loadUsers();
                        if (all != null) {
                            for (User uu : all) {
                                if (claims.sub.equals(uu.getUserId())) { found = uu; break; }
                            }
                        }
                        if (found != null) {
                            writeUserJson(resp, found, true, "token");
                        } else {
                            // Minimal user from claims
                            User minimal = new User();
                            minimal.setUserId(claims.sub);
                            minimal.setName(claims.sub);
                            minimal.setRole("UNKNOWN");
                            writeUserJson(resp, minimal, true, "token");
                        }
                        return;
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.FINE, "Token validation error", ex);
                }
            }
        }

        writeUnauth(resp, "not_authenticated");
    }

    private void writeUserJson(HttpServletResponse resp, User user, boolean authenticated, String source) throws IOException {
        String json = "{" +
                "\"authenticated\":" + authenticated + "," +
                "\"source\":\"" + escape(userString(source)) + "\"," +
                "\"user\":{" +
                "\"userId\":\"" + escape(user.getUserId()) + "\"," +
                "\"name\":\"" + escape(user.getName()) + "\"," +
                "\"role\":\"" + escape(user.getRole()) + "\"}" +
                "}";
        PrintWriter out = resp.getWriter();
        out.write(json);
        out.flush();
    }

    private void writeUnauth(HttpServletResponse resp, String reason) throws IOException {
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        String json = "{" +
                "\"authenticated\":false," +
                "\"reason\":\"" + escape(userString(reason)) + "\"}";
        PrintWriter out = resp.getWriter();
        out.write(json);
        out.flush();
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String userString(String s) {
        return s == null ? "" : s;
    }
}

