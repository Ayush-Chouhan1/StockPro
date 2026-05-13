package com.stockpro.gateway;

import com.stockpro.gateway.filter.JwtAuthFilter;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthFilterTest {

    private static final String SECRET = "01234567890123456789012345678901";

    private JwtAuthFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter();
        ReflectionTestUtils.setField(filter, "secret", SECRET);
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
    }

    @Test
    void filter_OpenPath_SkipsJwtValidation() {
        ServerWebExchange exchange = exchange("GET", "/auth/login", null);

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        assertNull(exchange.getResponse().getStatusCode());
    }

    @Test
    void filter_OptionsRequest_SkipsJwtValidation() {
        ServerWebExchange exchange = exchange("OPTIONS", "/products", null);

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        assertNull(exchange.getResponse().getStatusCode());
    }

    @Test
    void filter_MissingAuthorization_ReturnsUnauthorized() {
        ServerWebExchange exchange = exchange("GET", "/products", null);

        filter.filter(exchange, chain).block();

        assertEquals(401, exchange.getResponse().getStatusCode().value());
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_InvalidToken_ReturnsUnauthorized() {
        ServerWebExchange exchange = exchange("GET", "/products", "Bearer invalid-token");

        filter.filter(exchange, chain).block();

        assertEquals(401, exchange.getResponse().getStatusCode().value());
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_ValidToken_AddsUserHeadersAndContinues() {
        ServerWebExchange exchange = exchange("GET", "/products", "Bearer " + token("user@test.com", "WAREHOUSE_STAFF"));

        filter.filter(exchange, chain).block();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        ServerWebExchange forwarded = captor.getValue();
        assertEquals("user@test.com", forwarded.getRequest().getHeaders().getFirst("X-User-Email"));
        assertEquals("WAREHOUSE_STAFF", forwarded.getRequest().getHeaders().getFirst("X-User-Role"));
    }

    @Test
    void filter_StaffPaymentRequest_ReturnsForbidden() {
        ServerWebExchange exchange = exchange("GET", "/payments", "Bearer " + token("user@test.com", "WAREHOUSE_STAFF"));

        filter.filter(exchange, chain).block();

        assertEquals(403, exchange.getResponse().getStatusCode().value());
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_ManagerPaymentRequest_Continues() {
        ServerWebExchange exchange = exchange("GET", "/payments", "Bearer " + token("manager@test.com", "INVENTORY_MANAGER"));

        filter.filter(exchange, chain).block();

        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    void filter_StaffDeleteRequest_ReturnsForbidden() {
        ServerWebExchange exchange = exchange("DELETE", "/products/1", "Bearer " + token("user@test.com", "WAREHOUSE_STAFF"));

        filter.filter(exchange, chain).block();

        assertEquals(403, exchange.getResponse().getStatusCode().value());
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_PurchaseOfficerProductCreate_ReturnsForbidden() {
        ServerWebExchange exchange = exchange("POST", "/products", "Bearer " + token("buyer@test.com", "PURCHASE_OFFICER"));

        filter.filter(exchange, chain).block();

        assertEquals(403, exchange.getResponse().getStatusCode().value());
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_InventoryManagerProductCreate_Continues() {
        ServerWebExchange exchange = exchange("POST", "/products", "Bearer " + token("manager@test.com", "INVENTORY_MANAGER"));

        filter.filter(exchange, chain).block();

        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    void filter_PurchaseOfficerSupplierCreate_Continues() {
        ServerWebExchange exchange = exchange("POST", "/suppliers", "Bearer " + token("buyer@test.com", "PURCHASE_OFFICER"));

        filter.filter(exchange, chain).block();

        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    void filter_WarehouseStaffSupplierCreate_ReturnsForbidden() {
        ServerWebExchange exchange = exchange("POST", "/suppliers", "Bearer " + token("staff@test.com", "WAREHOUSE_STAFF"));

        filter.filter(exchange, chain).block();

        assertEquals(403, exchange.getResponse().getStatusCode().value());
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_PurchaseOfficerApprovePurchaseOrder_ReturnsForbidden() {
        ServerWebExchange exchange = exchange("PUT", "/purchase-orders/10/approve", "Bearer " + token("buyer@test.com", "PURCHASE_OFFICER"));

        filter.filter(exchange, chain).block();

        assertEquals(403, exchange.getResponse().getStatusCode().value());
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_InventoryManagerApprovePurchaseOrder_Continues() {
        ServerWebExchange exchange = exchange("PUT", "/purchase-orders/10/approve", "Bearer " + token("manager@test.com", "INVENTORY_MANAGER"));

        filter.filter(exchange, chain).block();

        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    void filter_WarehouseStaffReceiveGoods_Continues() {
        ServerWebExchange exchange = exchange("POST", "/purchase-orders/10/receive-goods", "Bearer " + token("staff@test.com", "WAREHOUSE_STAFF"));

        filter.filter(exchange, chain).block();

        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    void filter_PurchaseOfficerReportsRequest_ReturnsForbidden() {
        ServerWebExchange exchange = exchange("GET", "/reports/valuation/total", "Bearer " + token("buyer@test.com", "PURCHASE_OFFICER"));

        filter.filter(exchange, chain).block();

        assertEquals(403, exchange.getResponse().getStatusCode().value());
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_LegacyManagerRoleMapsToInventoryManager() {
        ServerWebExchange exchange = exchange("GET", "/reports/valuation/total", "Bearer " + token("manager@test.com", "MANAGER"));

        filter.filter(exchange, chain).block();

        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    void getOrder_RunsBeforeRouteFilters() {
        assertEquals(-1, filter.getOrder());
    }

    private ServerWebExchange exchange(String method, String path, String authorization) {
        MockServerHttpRequest.BaseBuilder<?> request = MockServerHttpRequest
                .method(HttpMethod.valueOf(method), path);
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        return MockServerWebExchange.from(request);
    }

    private String token(String subject, String role) {
        return Jwts.builder()
                .setSubject(subject)
                .claim("role", role)
                .setIssuedAt(new Date())
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256)
                .compact();
    }
}
