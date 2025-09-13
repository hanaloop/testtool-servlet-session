<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8"/>
    <title>Hello Servlet JSP</title>
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
    <h1>Java Servlet + JSP</h1>
    <p>Simple Hello World using a Servlet controller and JSP view.</p>
    <form action="hello" method="get">
        <label for="name">Your name (optional):</label>
        <input type="text" id="name" name="name" placeholder="World" />
        <br/>
        <button type="submit">Say Hello</button>
    </form>

    <p>Or try it directly: <a href="hello">/hello</a></p>
</div>
</body>
</html>

