# Hanaeco Test Tool: Servlet Session

Hanaeco Test Tool Servlet Session, using Jetty, `web.xml`, and Maven.

## Requirements

- Java 11+
- Maven 3.8+

## Run (dev)

```bash
mvn -q clean package
mvn -q jetty:run
```

Then open:

- http://localhost:8080/
- http://localhost:8080/hello

You can pass a name: `http://localhost:8080/hello?name=Hanaloop`.

## Project Layout

- `pom.xml` — Maven config with Jetty plugin
- `src/main/webapp/WEB-INF/web.xml` — Servlet routing (maps `/hello`)
- `src/main/java/com/hanaloop/tool/HelloServlet.java` — Controller sets model and forwards to JSP
- `src/main/webapp/WEB-INF/jsp/hello.jsp` — JSP view (not directly accessible)
- `src/main/webapp/index.jsp` — Home page + form

