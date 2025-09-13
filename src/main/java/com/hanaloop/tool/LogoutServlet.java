package com.hanaloop.tool;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.logging.Logger;

public class LogoutServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(LogoutServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleLogout(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleLogout(req, resp);
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Invalidate session if present
        HttpSession session = req.getSession(false);
        if (session != null) {
            try {
                session.invalidate();
            } catch (IllegalStateException ignore) {
            }
        }

        // Remove el-token cookie
        Cookie removed = new Cookie("el-token", "");
        removed.setPath("/");
        removed.setMaxAge(0);
        removed.setHttpOnly(true);
        if (req.isSecure()) {
            removed.setSecure(true);
        }
        resp.addCookie(removed);

        // Optionally also try to remove JSESSIONID (best-effort; container-managed)
        Cookie killSessionId = new Cookie("JSESSIONID", "");
        killSessionId.setPath(req.getContextPath().isEmpty() ? "/" : req.getContextPath());
        killSessionId.setMaxAge(0);
        killSessionId.setHttpOnly(true);
        if (req.isSecure()) {
            killSessionId.setSecure(true);
        }
        resp.addCookie(killSessionId);

        // Determine safe redirect URL
        String redir = req.getParameter("redirUrl");
        if (redir == null || redir.isBlank() || !redir.startsWith("/")) {
            redir = req.getContextPath() + "/";
        }
        LOGGER.fine("Logout redirecting to: " + redir);
        resp.sendRedirect(redir);
    }
}

