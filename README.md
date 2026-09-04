# ordered-gateway

The single public entry point for [ordered-system](https://github.com/ordered-system) — a reactive Spring Cloud Gateway sitting in front of four independent business services. Every external request goes through here first.

## What it does

- **Routes** requests by path to the right downstream service, resolved dynamically through Eureka (`lb://order-service`, `lb://product-service`, `lb://user-service`, `lb://engagement-service`) instead of hardcoded URLs.
- **Verifies JWTs at the edge.** `JwtAuthenticationGlobalFilter` validates the token's signature (HS256, shared secret pulled from `ordered-config-server`) before a request is allowed past the gateway, so downstream services can trust the identity headers they receive instead of re-verifying signatures themselves. A configurable allowlist (`ordered.gateway.security.public-paths`) exempts login/register, actuator, public product browsing, and Swagger from this check.
- **Aggregates API docs.** Each service exposes its own OpenAPI spec; the gateway rewrites and re-exposes them (e.g. `/order-service/v3/api-docs` → the service's `/v3/api-docs`) so a single Swagger UI at the gateway shows the whole system's API surface.

## Routing map

| Path prefix | Routed to |
|---|---|
| `/api/v1/orders/**`, `/api/v1/payments/**` | order-service |
| `/api/v1/products/**`, `/api/v1/cart/**` | product-service |
| `/api/v1/users/**`, `/api/v1/auth/**`, `/api/v1/addresses/**` | user-service |
| `/api/v1/reviews/**`, `/api/v1/browsing-history/**` | engagement-service |

## Stack

Java 21 · Spring Boot 4.1.0 · Spring Cloud Gateway (WebFlux, reactive) · Spring Cloud Config Client · Eureka Client · JJWT (JWT verification)

## Running it

Needs [`ordered-eureka`](https://github.com/ordered-system/ordered-eureka) and [`ordered-config-server`](https://github.com/ordered-system/ordered-config-server) running first (the gateway won't start without a JWT secret to verify against).

```bash
./mvnw spring-boot:run
```

Runs on **port 8080**. Swagger UI: `http://localhost:8080/swagger-ui.html`

### Docker

```bash
docker build -t ordered-gateway .
docker run -p 8080:8080 \
  -e EUREKA_URL=http://host.docker.internal:8761/eureka/ \
  -e CONFIG_SERVER_URL=http://host.docker.internal:8888 \
  ordered-gateway
```

## Testing

```bash
./mvnw test
```

Covers `JwtAuthenticationGlobalFilter` — valid/expired/malformed tokens, and requests to public vs. protected paths.

## Where this fits

Part of the [ordered-system](https://github.com/ordered-system) organization. See [ordered-infra](https://github.com/ordered-system/ordered-infra) to run the entire platform (this service included) with one command, and [ordered-backend](https://github.com/ordered-system/ordered-backend) for the monolith this system was decomposed from.

## License

MIT — see [LICENSE](LICENSE).
