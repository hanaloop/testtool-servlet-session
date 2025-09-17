<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8"/>
    <title>Hanaeco | Servlet Session Test Tool</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/site.css" />
</head>
<body>
<div class="card">
    <h1>Hanaeco Test Tool: Servlet Session</h1>
    <p>This test tool is implemented using a Java Servlet and provides a mock login feature.</p>
    <p>The login process relies on <code>HttpSession</code> (JSESSIONID) for session management.</p>
    <p>On successful login, the system issues a <em>cookie</em> named <code>el-token</code>, signed with the key specified in the <code>JWT_SECRET</code> environment variable.</p>
    <p>If el-token cookie  is not created, verify the environment <code>JWT_SECRET</code>.</p>
    <form action="main" method="get">
        <label for="name">Your name (optional):</label>
        <input type="text" id="name" name="name" placeholder="World" />
        <br/>
        <button type="submit">Say Hello</button>
    </form>

    <p>Or try it directly: <a href="main">/main</a></p>
    <hr/>
    <p>
        <c:choose>
            <c:when test="${not empty sessionScope.authUser}">
                <a href="${pageContext.request.contextPath}/logout?redirUrl=${pageContext.request.contextPath}/">Logout</a>
            </c:when>
            <c:otherwise>
                <a href="login">Login</a>
            </c:otherwise>
        </c:choose>
    </p>
</div>
</body>
</html>
