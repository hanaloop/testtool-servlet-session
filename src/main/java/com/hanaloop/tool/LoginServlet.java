package com.hanaloop.tool;

import com.hanaloop.tool.auth.User;
import com.hanaloop.tool.auth.UserStore;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class LoginServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Pass through to login form JSP
        RequestDispatcher dispatcher = req.getRequestDispatcher("/WEB-INF/jsp/login.jsp");
        dispatcher.forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userId = trim(req.getParameter("userId"));
        String password = trim(req.getParameter("password"));

        if (userId.isEmpty() || password.isEmpty()) {
            req.setAttribute("error", "User ID and password are required.");
            req.setAttribute("userId", userId);
            req.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(req, resp);
            return;
        }

        User matched = UserStore.findByCredentials(userId, password);
        if (matched == null) {
            req.setAttribute("error", "Invalid credentials. Please try again.");
            req.setAttribute("userId", userId);
            req.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(req, resp);
            return;
        }

        // Create session for 60 minutes and store user info
        HttpSession session = req.getSession(true);
        session.setMaxInactiveInterval(60 * 60);
        session.setAttribute("authUser", matched);

        // Redirect to home or a protected page (here: hello)
        resp.sendRedirect(req.getContextPath() + "/hello");
    }

    private static String trim(String v) {
        return v == null ? "" : v.trim();
    }
}

