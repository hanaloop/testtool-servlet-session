<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8"/>
    <title>Login</title>
    <style>
        body { font-family: system-ui, -apple-system, Segoe UI, Roboto, Helvetica, Arial, sans-serif; margin: 2rem; }
        .card { border: 1px solid #ddd; border-radius: 8px; padding: 1.25rem; max-width: 520px; }
        label { display: block; margin-top: 0.5rem; }
        input[type=text], input[type=password] { padding: 0.5rem; width: 100%; max-width: 320px; }
        button { margin-top: 0.75rem; padding: 0.5rem 0.75rem; }
        .error { color: #b00020; margin-top: 0.5rem; }
        a { text-decoration: none; color: #0b5fff; }
    </style>
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <meta http-equiv="Content-Security-Policy" content="default-src 'self' 'unsafe-inline' data:; img-src 'self' data:;" />
    <link rel="icon" href="data:,"/>
    <meta name="robots" content="noindex" />
    <meta name="referrer" content="no-referrer" />
    <meta name="description" content="Login form" />
    <meta name="generator" content="Codex CLI" />
    <meta name="color-scheme" content="light dark" />
    <meta name="supported-color-schemes" content="light dark" />
    <meta name="apple-mobile-web-app-capable" content="yes" />
    <meta name="mobile-web-app-capable" content="yes" />
</head>
<body>
<div class="card">
    <h1>Login</h1>
    <form action="${pageContext.request.contextPath}/login" method="post">
        <label for="userId">User ID:</label>
        <input type="text" id="userId" name="userId" value="${fn:escapeXml(param.userId != null ? param.userId : (requestScope.userId != null ? requestScope.userId : ''))}" />

        <label for="password">Password:</label>
        <input type="password" id="password" name="password" />

        <button type="submit">Sign In</button>
        <p class="error">${error}</p>
    </form>
    <p><a href="${pageContext.request.contextPath}/">Back to Home</a></p>
    <p style="color:#666; font-size:0.9rem;">Demo users are in users.db.yml</p>
</div>
</body>
</html>
