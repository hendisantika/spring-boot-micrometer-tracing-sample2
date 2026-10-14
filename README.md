# Spring Boot Micrometer Tracing Sample 2

[![Java CI with Gradle](https://github.com/hendisantika/spring-boot-micrometer-tracing-sample2/actions/workflows/gradle.yml/badge.svg)](https://github.com/hendisantika/spring-boot-micrometer-tracing-sample2/actions/workflows/gradle.yml)

A reactive Spring Boot **client** application that shows distributed tracing with
[Micrometer Tracing](https://docs.micrometer.io/tracing/reference/) and the Brave bridge.
It calls a downstream customer service through a declarative HTTP interface (`@GetExchange`)
backed by `WebClient`, and writes the `traceId` and `spanId` into every log line.

## Tech Stack

| Component          | Version |
|--------------------|---------|
| Java (JDK)         | 25      |
| Spring Boot        | 4.1.1   |
| Spring WebFlux     | 7.x     |
| Micrometer Tracing | 1.7.x (Brave bridge) |
| Gradle (wrapper)   | 9.8.1   |

## How It Works

```
curl :8080/all ──► CustomerController ──► CustomerHttpClient (WebClient) ──► http://localhost:9090/customers
                   log: [client, <traceId>, <spanId>]        header: traceparent: 00-<traceId>-<spanId>-01
```

- `CustomerHttpClient` is a declarative HTTP interface created with `HttpServiceProxyFactory` and `WebClientAdapter`.
- Spring Boot auto-configures a Brave `Tracer`. Every incoming request gets a trace, and the trace
  context goes to the downstream service in the W3C `traceparent` header.
- `spring.reactor.context-propagation=auto` copies the trace context into the SLF4J MDC on Reactor
  threads, so the log pattern can print `%X{traceId}` and `%X{spanId}`.
- `management.tracing.sampling.probability=1.0` samples every request (fine for a demo; lower it in production).

## Prerequisites

- JDK 25 (for example Eclipse Temurin 25 or Amazon Corretto 25)
- No local Gradle install needed. Use the bundled wrapper (`./gradlew`).

## Build & Test

```bash
./gradlew clean build
```

The build uses a Gradle Java toolchain pinned to Java 25.

## Run

1. Start a downstream service on port `9090` that serves `GET /customers`, returning
   `[{"id":1,"name":"Alice"}, ...]`. A quick stub for local testing:

   ```bash
   python3 - <<'EOF'
   import http.server, json
   class H(http.server.BaseHTTPRequestHandler):
       def do_GET(self):
           print("traceparent:", self.headers.get("traceparent"))
           body = json.dumps([{"id": 1, "name": "Alice"}, {"id": 2, "name": "Bob"}]).encode()
           self.send_response(200)
           self.send_header("Content-Type", "application/json")
           self.send_header("Content-Length", str(len(body)))
           self.end_headers()
           self.wfile.write(body)
   http.server.HTTPServer(("127.0.0.1", 9090), H).serve_forever()
   EOF
   ```

2. Start the client application (port `8080`):

   ```bash
   ./gradlew bootRun
   # or
   java -jar build/libs/client-0.0.1-SNAPSHOT.jar
   ```

3. Call the endpoint:

   ```bash
   curl http://localhost:8080/all
   # [{"id":1,"name":"Alice"},{"id":2,"name":"Bob"}]
   ```

4. Check the application log. It contains the trace and span IDs:

   ```
   INFO [client, 6ad00ac26e4c533e280ac36c42e43b3c, 280ac36c42e43b3c] ... c.h.client.CustomerController : Fetching customers
   ```

   The downstream stub receives the same trace ID:

   ```
   traceparent: 00-6ad00ac26e4c533e280ac36c42e43b3c-c158213d35c8bc27-01
   ```

## Endpoints

| Method | Path               | Description                                   |
|--------|--------------------|-----------------------------------------------|
| GET    | `/all`             | Fetches customers from the downstream service |
| GET    | `/actuator/health` | Health check                                  |

## Configuration (`application.yml`)

```yaml
spring:
  application:
    name: client
  reactor:
    context-propagation: auto

management:
  tracing:
    sampling:
      probability: 1.0

logging:
  pattern:
    level: "%5p [${spring.application.name:}, %X{traceId:-}, %X{spanId:-}]"
```

## Spring Boot 4 Migration Notes

- **Modular starters:** `WebClient.Builder` auto-configuration now lives in
  `spring-boot-starter-webclient`, and tracing auto-configuration lives in
  `spring-boot-micrometer-tracing-brave`. Both are declared explicitly, along with
  `io.micrometer:micrometer-tracing-bridge-brave`.
- **HTTP interface API:** `WebClientAdapter.forClient(..)` was replaced by `WebClientAdapter.create(..)`,
  and `HttpServiceProxyFactory.Builder#clientAdapter(..)` by `#exchangeAdapter(..)`.

## CI

GitHub Actions (`.github/workflows/gradle.yml`) builds and tests the project on JDK 25 (Temurin)
for every push and pull request to `main`, and submits the dependency graph for Dependabot alerts.

## Author

**Hendi Santika**

- GitHub: [@hendisantika](https://github.com/hendisantika)
