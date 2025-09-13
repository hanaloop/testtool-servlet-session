<%@ page contentType="text/html;charset=UTF-8" language="java" %>
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
    <p>On successful login, the system issues a cookie named <code>el-token</code>, signed with the key specified in the <code>JWT_SECRET</code> environment variable.</p>
    <form action="main" method="get">
        <label for="name">Your name (optional):</label>
        <input type="text" id="name" name="name" placeholder="World" />
        <br/>
        <button type="submit">Say Hello</button>
    </form>

    <p>Or try it directly: <a href="main">/main</a></p>
    <hr/>
    <p><a href="login">Login</a></p>
</div>
</body>
</html>
