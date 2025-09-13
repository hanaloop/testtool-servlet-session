<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8"/>
    <title>Hanaeco | Servlet Session Test Tool</title>
    <style>
        body { font-family: system-ui, -apple-system, Segoe UI, Roboto, Helvetica, Arial, sans-serif; margin: 2rem; }
        .card { border: 1px solid #ddd; border-radius: 8px; padding: 1.25rem; max-width: 520px; }
        label { display: block; margin-top: 0.5rem; }
        input[type=text] { padding: 0.5rem; width: 100%; max-width: 320px; }
        button { margin-top: 0.75rem; padding: 0.5rem 0.75rem; }
        a { text-decoration: none; color: #0b5fff; }
    </style>
</head>
<body>
<div class="card">
    <h1>Hanaeco Test Tool: Servlet Session</h1>
    <p>This test tool is implemented using a Java Servlet and provides a mock login feature.</p>
    <p>The login process relies on <code>HttpSession</code> (JSESSIONID) for session management.</p>
    <p>On successful login, the system issues a cookie named <code>el-token</code>, signed with the key specified in the <code>JWT_SECRET</code> environment variable.</p>
    <form action="hello" method="get">
        <label for="name">Your name (optional):</label>
        <input type="text" id="name" name="name" placeholder="World" />
        <br/>
        <button type="submit">Say Hello</button>
    </form>

    <p>Or try it directly: <a href="hello">/hello</a></p>
    <hr/>
    <p><a href="login">Login</a></p>
</div>
</body>
</html>
