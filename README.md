# Hanaeco Test Tool: Servlet Session

Simple servlet/JSP app demonstrating session-based login plus a signed JWT cookie, using Jetty, `web.xml`, and Maven.

## Features

- Login form (`/login`) backed by YAML user store (`users.db.yml`).
- Credential validation creates a 60-minute `HttpSession` with `authUser`.
- Issues HS256 JWT cookie `el-token` (HttpOnly, `Secure` on HTTPS) signed with `JWT_SECRET`.
- Optional `callbackUrl` to control post-login redirect (app-relative only).
- Logout endpoint (`/logout`) invalidates the session, clears cookies, and redirects to `callbackUrl`.
- Session validator API (`/api/session`) returns current user from session or JWT bearer/cookie.

## Requirements

- Java 11+
- Maven 3.8+

## Build & Run

### Build
```sh
# Package will create a jar file within ./target folder
mvn -Pexec-jar clean package
```

### Configure: Set a signing secret (for JWT) and run Jetty:

```sh
export JWT_SECRET='dev-secret-change-me'
```


### Run
```sh
# To run the jar
java -jar ./target/hanaeco-session-servlet-test-tool-1.0.0-SNAPSHOT-exec.jar
```

> In, intellij it can be run as war on top of jetty `mvn jetty:run`

### Test

Open:
- Home: http://localhost:8080/
- Main page: http://localhost:8080/main
- Login: http://localhost:8080/login
- Logout: http://localhost:8080/logout?callbackUrl=/
- Session API: http://localhost:8080/api/session

Sample users live in `src/main/resources/users.db.yml` (userId/password):

- `admin` / `admin123`
- `alice` / `wonderland`
- `bob` / `builder`

## Usage Notes

- Login success redirect:
  - Defaults to `/main`.
  - If `callbackUrl` is provided (e.g., `/feature/x`), login redirects there.
  - External URLs are ignored for safety; only app-relative (`/...`) are allowed.
- JWT cookie `el-token` claims: `sub`, `iat`, `exp` (+60m), `aud=local`, `iss=local`.
- The YAML loader accepts `users.db.yam`, `users.db.yml`, or `users.db.yaml` on the classpath.

## API Examples

Validate session via cookie or Authorization header:

```bash
# Using browser cookies (session or el-token) — just curl the endpoint when authenticated
curl -i http://localhost:8080/api/session

# Using bearer token
curl -i -H "Authorization: Bearer <your-jwt>" http://localhost:8080/api/session
```

Successful response:

```json
{ "authenticated": true, "source": "session", "user": { "userId": "alice", "name": "Alice Liddell", "role": "USER" } }
```

Unauthenticated response:

```json
{ "authenticated": false, "reason": "not_authenticated" }
```

## Project Layout

- `pom.xml` — Maven config and dependencies
- `src/main/webapp/WEB-INF/web.xml` — Servlet routing
- Servlets:
  - `src/main/java/com/hanaloop/tool/MainServlet.java` — Renders main view
  - `src/main/java/com/hanaloop/tool/LoginServlet.java` — Login flow + JWT issuance
  - `src/main/java/com/hanaloop/tool/LogoutServlet.java` — Logout + redirect
  - `src/main/java/com/hanaloop/tool/SessionValidatorServlet.java` — `/api/session`
- Auth utils and store:
  - `src/main/java/com/hanaloop/tool/auth/JwtUtil.java`
  - `src/main/java/com/hanaloop/tool/auth/UserStore.java`
  - `src/main/java/com/hanaloop/tool/auth/User.java`
- Views:
  - `src/main/webapp/index.jsp` — Home
  - `src/main/webapp/WEB-INF/jsp/main.jsp` — Main page
  - `src/main/webapp/WEB-INF/jsp/login.jsp` — Login form
- Static assets:
  - `src/main/webapp/css/site.css`

