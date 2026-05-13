package com.stockpro.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.util.List;

@Component
@Slf4j
public class JwtAuthFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String secret;

    private final List<String> OPEN_PATHS = List.of(
            "/auth/register",
            "/auth/login",
            "/auth/google",
            "/auth/forgot-password",
            "/auth/reset-password",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/webjars/**",
            "/v3/api-docs/**",
            "/alert-service/v3/api-docs",
            "/auth-service/v3/api-docs",
            "/product-service/v3/api-docs",
            "/supplier-service/v3/api-docs",
            "/warehouse-service/v3/api-docs",
            "/movement-service/v3/api-docs",
            "/purchase-service/v3/api-docs",
            "/report-service/v3/api-docs",
            "/payment-service/v3/api-docs"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        log.debug("Gateway request: {} {}", request.getMethod(), path);

        // Skip JWT check for open paths
        if (isOpenPath(path)) {
            log.debug("Open path — skipping JWT: {}", path);
            return chain.filter(exchange);
        }

        // Handle CORS preflight
        if (request.getMethod().name().equals("OPTIONS")) {
            return chain.filter(exchange);
        }

        // Check Authorization header
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for: {}", path);
            return unauthorized(exchange, "Missing Authorization header");
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = parseToken(token);
            String email = claims.getSubject();
            String role = claims.get("role", String.class);

            log.debug("JWT valid for user: {}, role: {}", email, role);

            if (!isAuthorized(path, request.getMethod(), role)) {
                log.warn("Forbidden request for role {}: {} {}", role,
                        request.getMethod(), path);
                return forbidden(exchange,
                        "You do not have permission to access this resource");
            }

            // Forward user info to downstream services via headers
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Email", email)
                    .header("X-User-Role", role != null ? role : "")
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            log.warn("JWT validation failed for path {}: {}", path, e.getMessage());
            return unauthorized(exchange, "Invalid or expired token");
        }
    }

    private boolean isOpenPath(String path) {
        return OPEN_PATHS.stream().anyMatch(path::startsWith);
    }

    private Claims parseToken(String token) {
        Key key = Keys.hmacShaKeyFor(secret.getBytes());
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private boolean isAuthorized(String path, HttpMethod method, String role) {
        if (role == null || role.isBlank()) {
            return false;
        }

        String normalizedRole = normalizeRole(role);

        if (path.startsWith("/auth/users") || path.startsWith("/auth/user/")) {
            return isAdmin(normalizedRole);
        }

        if (path.startsWith("/products")) {
            if (HttpMethod.GET.equals(method)) {
                return isInternalUser(normalizedRole);
            }
            return hasAnyRole(normalizedRole, "ADMIN", "INVENTORY_MANAGER");
        }

        if (path.startsWith("/warehouses")) {
            if (HttpMethod.GET.equals(method)) {
                return isInternalUser(normalizedRole);
            }
            if (path.matches("^/warehouses/\\d+/manager/\\d+$")) {
                return isAdmin(normalizedRole);
            }
            return hasAnyRole(normalizedRole, "ADMIN", "INVENTORY_MANAGER");
        }

        if (path.startsWith("/stock")) {
            if (HttpMethod.GET.equals(method)) {
                return isInternalUser(normalizedRole);
            }
            return hasAnyRole(normalizedRole, "ADMIN", "INVENTORY_MANAGER", "WAREHOUSE_STAFF");
        }

        if (path.startsWith("/suppliers")) {
            if (HttpMethod.GET.equals(method)) {
                return hasAnyRole(normalizedRole, "ADMIN", "PURCHASE_OFFICER", "INVENTORY_MANAGER");
            }
            return hasAnyRole(normalizedRole, "ADMIN", "PURCHASE_OFFICER");
        }

        if (path.startsWith("/purchase-orders")) {
            if (path.matches("^/purchase-orders/\\d+/(approve|reject)$")) {
                return hasAnyRole(normalizedRole, "ADMIN", "INVENTORY_MANAGER");
            }
            if (path.matches("^/purchase-orders/\\d+/receive-goods$")) {
                return hasAnyRole(normalizedRole, "ADMIN", "WAREHOUSE_STAFF", "PURCHASE_OFFICER");
            }
            if (HttpMethod.GET.equals(method)) {
                return hasAnyRole(normalizedRole, "ADMIN", "INVENTORY_MANAGER", "PURCHASE_OFFICER", "WAREHOUSE_STAFF");
            }
            return hasAnyRole(normalizedRole, "ADMIN", "PURCHASE_OFFICER");
        }

        if (path.startsWith("/movements")) {
            if (HttpMethod.GET.equals(method)) {
                return isInternalUser(normalizedRole);
            }
            return hasAnyRole(normalizedRole, "ADMIN", "INVENTORY_MANAGER", "WAREHOUSE_STAFF");
        }

        if (path.startsWith("/reports")) {
            if (HttpMethod.GET.equals(method)) {
                return hasAnyRole(normalizedRole, "ADMIN", "INVENTORY_MANAGER");
            }
            return isAdmin(normalizedRole);
        }

        if (path.startsWith("/alerts")) {
            if (HttpMethod.GET.equals(method) || HttpMethod.PUT.equals(method)) {
                return isInternalUser(normalizedRole);
            }
            return hasAnyRole(normalizedRole, "ADMIN", "INVENTORY_MANAGER");
        }

        if (path.startsWith("/payments")) {
            return hasAnyRole(normalizedRole, "ADMIN", "INVENTORY_MANAGER", "PURCHASE_OFFICER");
        }

        return true;
    }

    private String normalizeRole(String role) {
        return switch (role) {
            case "STAFF" -> "WAREHOUSE_STAFF";
            case "MANAGER" -> "INVENTORY_MANAGER";
            case "OFFICER" -> "PURCHASE_OFFICER";
            default -> role;
        };
    }

    private boolean isInternalUser(String role) {
        return hasAnyRole(role, "ADMIN", "INVENTORY_MANAGER", "PURCHASE_OFFICER", "WAREHOUSE_STAFF");
    }

    private boolean isAdmin(String role) {
        return "ADMIN".equals(role);
    }

    private boolean hasAnyRole(String role, String... allowedRoles) {
        for (String allowedRole : allowedRoles) {
            if (allowedRole.equals(role)) {
                return true;
            }
        }
        return false;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        byte[] bytes = ("{\"error\":\"" + message + "\"}").getBytes();
        var buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    private Mono<Void> forbidden(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().add("Content-Type", "application/json");
        byte[] bytes = ("{\"error\":\"" + message + "\"}").getBytes();
        var buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1; // Run before all other filters
    }
}
