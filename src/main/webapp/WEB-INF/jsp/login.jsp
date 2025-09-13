<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8"/>
    <title>Login</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/site.css" />
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
        <c:set var="redirVal" value="${empty param.redirUrl ? requestScope.redirUrl : param.redirUrl}" />
        <c:if test="${not empty redirVal}">
            <input type="hidden" name="redirUrl" value="${fn:escapeXml(redirVal)}" />
        </c:if>
        <label for="userId">User ID:</label>
        <input type="text" id="userId" name="userId" value="${fn:escapeXml(param.userId != null ? param.userId : (requestScope.userId != null ? requestScope.userId : ''))}" />

        <label for="password">Password:</label>
        <input type="password" id="password" name="password" />

        <button type="submit">Sign In</button>
        <p class="error">${error}</p>
    </form>
    <p><a href="${pageContext.request.contextPath}/">Back to Home</a></p>
    <p class="note">Demo users are in users.db.yml</p>
</div>
</body>
</html>
