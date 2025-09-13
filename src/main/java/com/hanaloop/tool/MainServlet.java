package com.hanaloop.tool;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
import com.hanaloop.tool.auth.User;

public class MainServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(MainServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final String paramName = req.getParameter("name");
        String name = paramName;

        // If logged in, prefer the user's name
        HttpSession session = req.getSession(false);
        if (session != null) {
            Object u = session.getAttribute("authUser");
            if (u instanceof User) {
                name = ((User) u).getName();
                LOGGER.fine("Main page viewed by logged-in user: " + ((User) u).getUserId());
            }
        }
        if (name == null || name.isBlank()) {
            name = "World";
        }

        LOGGER.fine("Rendering main for name='" + name + "'");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        req.setAttribute("message", "Hello, " + name + "!");
        req.setAttribute("timestamp", timestamp);

        RequestDispatcher dispatcher = req.getRequestDispatcher("/WEB-INF/jsp/main.jsp");
        dispatcher.forward(req, resp);
    }
}
