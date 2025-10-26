package com.hanaloop.tool;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MockResponseServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(MockResponseServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String baseFolder = System.getenv("MOCK_API_FOLDER");
        if (baseFolder == null || baseFolder.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write("MOCK_API_FOLDER environment variable is not set");
            return;
        }

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || "/".equals(pathInfo) || pathInfo.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write("Filename must be provided");
            return;
        }

        String requestedFile = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        if (requestedFile.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write("Filename must be provided");
            return;
        }

        Path basePath = Paths.get(baseFolder).toAbsolutePath().normalize();
        Path targetPath = basePath.resolve(requestedFile).normalize();

        if (!targetPath.startsWith(basePath)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write("Invalid filename");
            return;
        }

        if (!Files.exists(targetPath) || !Files.isRegularFile(targetPath)) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write("File not found");
            return;
        }

        String contentType = null;
        try {
            contentType = Files.probeContentType(targetPath);
        } catch (IOException ex) {
            LOGGER.log(Level.FINE, "Unable to determine content type for {0}", targetPath);
        }
        if (contentType == null || contentType.isEmpty()) {
            contentType = "application/octet-stream";
        }
        resp.setContentType(contentType);
        resp.setStatus(HttpServletResponse.SC_OK);

        try (OutputStream out = resp.getOutputStream()) {
            Files.copy(targetPath, out);
        }
    }
}
