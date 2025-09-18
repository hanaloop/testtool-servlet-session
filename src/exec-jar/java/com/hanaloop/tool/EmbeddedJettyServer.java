package com.hanaloop.tool;

import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AllowSymLinkAliasChecker;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;

import java.net.URL;
import java.nio.file.Files;
import java.io.File;

public class EmbeddedJettyServer {
    public static void main(String[] args) throws Exception {
        int port = getPort();

        Server server = new Server(port);

        URL webAppUrl = EmbeddedJettyServer.class.getClassLoader().getResource("webapp");
        if (webAppUrl == null) {
            throw new IllegalStateException("Could not locate webapp resources on classpath at /webapp");
        }

        // Extract webapp from the JAR to a temp directory so Jetty can read WEB-INF/web.xml and JSPs
        File extracted = Files.createTempDirectory("webapp-").toFile();
        extracted.deleteOnExit();
        Resource webRes = Resource.newResource(webAppUrl);
        webRes.copyTo(extracted);

        // Detect if resources were copied directly (WEB-INF at root) or under a 'webapp' subfolder
        File webRoot = extracted;
        File candidateRoot = new File(extracted, "WEB-INF/web.xml");
        if (!candidateRoot.exists()) {
            File nested = new File(extracted, "webapp/WEB-INF/web.xml");
            if (nested.exists()) {
                webRoot = new File(extracted, "webapp");
            }
        }

        // Debug info to help trace issues
        System.out.println("[EmbeddedJetty] Using web root: " + webRoot.getAbsolutePath());
        System.out.println("[EmbeddedJetty] Has index.jsp: " + new File(webRoot, "index.jsp").exists());
        System.out.println("[EmbeddedJetty] Has /WEB-INF/jsp/login.jsp: " + new File(webRoot, "WEB-INF/jsp/login.jsp").exists());
        System.out.println("[EmbeddedJetty] Has /WEB-INF/web.xml: " + new File(webRoot, "WEB-INF/web.xml").exists());

        WebAppContext context = new WebAppContext();
        context.setContextPath("/");
        context.setWar(webRoot.getAbsolutePath());
        context.setDescriptor(new File(webRoot, "WEB-INF/web.xml").getAbsolutePath());
        context.setParentLoaderPriority(true);
        // Permit alias/symlink access for extracted resources (temp dirs)
        context.addAliasCheck(new AllowSymLinkAliasChecker());
        context.addAliasCheck(new ContextHandler.ApproveAliases());

        context.setConfigurationDiscovered(true);
        // Use the standard Jetty configuration stack so web.xml is parsed
        context.setConfigurations(new Configuration[] {
                new org.eclipse.jetty.webapp.WebInfConfiguration(),
                new org.eclipse.jetty.webapp.WebXmlConfiguration(),
                new org.eclipse.jetty.webapp.MetaInfConfiguration(),
                new org.eclipse.jetty.webapp.FragmentConfiguration(),
                new org.eclipse.jetty.plus.webapp.EnvConfiguration(),
                new org.eclipse.jetty.plus.webapp.PlusConfiguration(),
                new AnnotationConfiguration(),
                new org.eclipse.jetty.webapp.JettyWebXmlConfiguration()
        });

        context.setAttribute(
                "org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
                ".*/[^/]*taglibs.*\\.jar$|.*/javax\\.servlet\\.jsp\\.jstl.*\\.jar$|.*/[^/]*jsp.*\\.jar$|.*/servlet-api.*\\.jar$|.*/javax\\.el.*\\.jar$"
        );

        // Point resource base directly at detected web root
        context.setBaseResource(Resource.newResource(webRoot));

        server.setHandler(context);

        try {
            server.start();
            System.out.println("Embedded Jetty started on port " + port);
            server.join();
        } finally {
            server.destroy();
        }
    }

    private static int getPort() {
        String fromEnv = System.getenv("PORT");
        if (fromEnv != null) {
            try { return Integer.parseInt(fromEnv); } catch (NumberFormatException ignore) {}
        }
        String fromProp = System.getProperty("port");
        if (fromProp != null) {
            try { return Integer.parseInt(fromProp); } catch (NumberFormatException ignore) {}
        }
        return 8080;
    }
}
