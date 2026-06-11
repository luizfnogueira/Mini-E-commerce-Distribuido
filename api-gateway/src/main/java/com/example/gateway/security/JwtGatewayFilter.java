package com.example.gateway.security;

import com.example.gateway.health.ServiceHealthRegistry;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {
    private final SecretKey jwtKey;
    private final ServiceHealthRegistry healthRegistry;

    public JwtGatewayFilter(@Value("${jwt.secret}") String secret, ServiceHealthRegistry healthRegistry) {
        this.jwtKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.healthRegistry = healthRegistry;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String service = serviceForPath(path);

        if (service != null && !healthRegistry.isAvailable(service)) {
            exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE, "application/json");
            byte[] body = ("{\"error\":\"Servico " + service + " indisponivel\"}").getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        }

        String method = exchange.getRequest().getMethod().name();
        if (isPublicRoute(path, method)) {
            return chain.filter(exchange);
        }

        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(jwtKey)
                    .build()
                    .parseSignedClaims(authorization.substring(7))
                    .getPayload();

            String role = claims.get("role", String.class);
            if (path.equals("/products") && "POST".equals(method) && !"admin".equals(role)) {
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", String.valueOf(claims.get("userId")))
                    .header("X-User-Email", claims.get("email", String.class))
                    .header("X-User-Role", role)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (Exception error) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private boolean isPublicRoute(String path, String method) {
        return path.equals("/users/register")
                || path.equals("/users/login")
                || ("GET".equals(method) && (path.equals("/products") || path.startsWith("/products/")));
    }

    private String serviceForPath(String path) {
        if (path.startsWith("/users/")) {
            return "users";
        }
        if (path.startsWith("/products")) {
            return "products";
        }
        if (path.startsWith("/orders")) {
            return "orders";
        }
        return null;
    }
}
