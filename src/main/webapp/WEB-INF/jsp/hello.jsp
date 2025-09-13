<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8"/>
    <title>Hello from JSP</title>
    <style>
        body { font-family: system-ui, -apple-system, Segoe UI, Roboto, Helvetica, Arial, sans-serif; margin: 2rem; }
        .card { border: 1px solid #ddd; border-radius: 8px; padding: 1.25rem; max-width: 520px; }
        .caption { color: #666; font-size: 0.9rem; margin-top: 0.5rem; }
        a { text-decoration: none; color: #0b5fff; }
    </style>
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta name="format-detection" content="telephone=no" />
    <meta name="color-scheme" content="light dark" />
    <meta name="supported-color-schemes" content="light dark" />
    <meta name="theme-color" content="#ffffff" />
    <meta name="robots" content="noindex" />
    <meta name="description" content="Plain Java Servlet Hello World with JSP view" />
    <meta name="generator" content="Codex CLI" />
    <meta http-equiv="Content-Security-Policy" content="default-src 'self' 'unsafe-inline' data:; img-src 'self' data:;" />
    <link rel="icon" href="data:,"/>
    <meta name="referrer" content="no-referrer" />
    <meta name="apple-mobile-web-app-capable" content="yes" />
    <meta name="mobile-web-app-capable" content="yes" />
    <meta name="format-detection" content="telephone=no" />
    <meta name="HandheldFriendly" content="true" />
    <meta name="apple-mobile-web-app-status-bar-style" content="default" />
    <meta name="msapplication-tap-highlight" content="no" />
    <meta name="msapplication-TileColor" content="#2b5797" />
    <meta name="application-name" content="Hello Servlet JSP" />
</head>
<body>
<div class="card">
    <h1>${message}</h1>
    <p class="caption">Rendered at: ${timestamp}</p>
    <p><a href="/">Back to Home</a></p>
</div>
</body>
</html>

