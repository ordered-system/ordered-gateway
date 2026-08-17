package pl.dybcio.ordered.gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationGlobalFilterTest {

    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hs256-signing";

    private JwtAuthenticationGlobalFilter filter;
    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        GatewaySecurityProperties properties = new GatewaySecurityProperties(
                SECRET,
                List.of("/api/v1/auth/**", "/actuator/**")
        );
        filter = new JwtAuthenticationGlobalFilter(properties);
        signingKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void allowsPublicPathWithoutToken() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/auth/login").build());
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.filter(exchange, (ex) -> {
            chainCalled.set(true);
            return reactor.core.publisher.Mono.empty();
        }).block();

        assertThat(chainCalled).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void rejectsProtectedPathWithoutAuthorizationHeader() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders").build());
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.filter(exchange, (ex) -> {
            chainCalled.set(true);
            return reactor.core.publisher.Mono.empty();
        }).block();

        assertThat(chainCalled).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsExpiredToken() {
        String expiredToken = Jwts.builder()
                .subject("adam")
                .issuedAt(new Date(System.currentTimeMillis() - 60_000))
                .expiration(new Date(System.currentTimeMillis() - 30_000))
                .signWith(signingKey)
                .compact();

        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders")
                        .header("Authorization", "Bearer " + expiredToken)
                        .build());
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.filter(exchange, (ex) -> {
            chainCalled.set(true);
            return reactor.core.publisher.Mono.empty();
        }).block();

        assertThat(chainCalled).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void allowsValidSignedToken() {
        String validToken = Jwts.builder()
                .subject("adam")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(signingKey)
                .compact();

        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders")
                        .header("Authorization", "Bearer " + validToken)
                        .build());
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.filter(exchange, (ex) -> {
            chainCalled.set(true);
            return reactor.core.publisher.Mono.empty();
        }).block();

        assertThat(chainCalled).isTrue();
    }
}