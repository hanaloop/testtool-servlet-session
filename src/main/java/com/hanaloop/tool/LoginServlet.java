package com.hanaloop.tool;

import com.hanaloop.tool.auth.HanaEcoSessionManager;
import com.hanaloop.tool.auth.User;
import com.hanaloop.tool.auth.UserStore;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * LoginServlet handles login form display and login flow
 */
public class LoginServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Pass through to login form JSP
        LOGGER.fine("GET /login from " + req.getRemoteAddr());
        // If a redirection target is provided, preserve it for the POST
        String redirParam = trim(req.getParameter("redirUrl"));
        if (!redirParam.isEmpty()) {
            req.setAttribute("redirUrl", redirParam);
            LOGGER.fine("Login GET received redirUrl=" + redirParam);
        }
        RequestDispatcher dispatcher = req.getRequestDispatcher("/WEB-INF/jsp/login.jsp");
        dispatcher.forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userId = trim(req.getParameter("userId"));
        String password = trim(req.getParameter("password"));

        LOGGER.info("POST /login attempt for userId='" + userId + "'");

        if (userId.isEmpty() || password.isEmpty()) {
            req.setAttribute("error", "User ID and password are required.");
            LOGGER.fine("Login validation failed: missing userId or password");
            req.setAttribute("userId", userId);
            req.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(req, resp);
            return;
        }

        User matched = UserStore.findByCredentials(userId, password);
        if (matched == null) {
            req.setAttribute("error", "Invalid credentials. Please try again.");
            LOGGER.info("Login failed for userId='" + userId + "'");
            req.setAttribute("userId", userId);
            req.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(req, resp);
            return;
        }

        // Create session for 60 minutes and store user info
        HttpSession session = req.getSession(true);
        session.setMaxInactiveInterval(60 * 60);
        session.setAttribute("authUser", matched);
        try {
            LOGGER.info("Login success for userId='" + userId + "', sessionId=" + session.getId());
        } catch (IllegalStateException e) {
            LOGGER.log(Level.FINE, "Session state error when logging ID", e);
        }

        // Issue JWT cookie "el-token" signed with HS256 using env JWT_SECRET
        String jwtSecret = System.getenv("JWT_SECRET");
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            LOGGER.warning("JWT_SECRET not set; skipping el-token cookie issuance");
        } else {
            try {
                HanaEcoSessionManager sessionManager = new HanaEcoSessionManager(true);
                sessionManager.addCookies(req, resp, userId, jwtSecret);
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Failed to generate/sign el-token JWT", ex);
            }
        }

        // Redirect to main or to a provided redirUrl (app-relative only)
        String redirParam = trim(req.getParameter("redirUrl"));
        String redirectTo;
        if (!redirParam.isEmpty()) {
            // If caller already included contextPath, keep as-is; otherwise prefix it
            if (redirParam.startsWith(req.getContextPath() + "/")) {
                redirectTo = redirParam;
            } else {
                redirectTo = req.getContextPath() + redirParam;
            }
        } else {
            redirectTo = req.getContextPath() + "/main";
        }
        LOGGER.fine("Redirecting authenticated user '" + userId + "' to " + redirectTo);
        resp.sendRedirect(redirectTo);
    }

    private static String trim(String v) {
        return v == null ? "" : v.trim();
    }
}
