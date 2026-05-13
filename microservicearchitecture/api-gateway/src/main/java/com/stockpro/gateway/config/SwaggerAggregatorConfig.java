package com.stockpro.gateway.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregates the Swagger UI from every downstream microservice into the
 * API Gateway's own Swagger UI at http://localhost:8080/swagger-ui.html.
 *
 * Each service must expose its docs at /v3/api-docs (the springdoc default).
 * The gateway routes those paths transparently via the entries added to
 * application.yml (see application.yml.addition in this folder).
 */
@Configuration
public class SwaggerAggregatorConfig {

    private final RouteDefinitionLocator locator;

    public SwaggerAggregatorConfig(@Lazy RouteDefinitionLocator locator) {
        this.locator = locator;
    }

    @Bean
    public List<GroupedOpenApi> apis() {
        List<GroupedOpenApi> groups = new ArrayList<>();
        List<RouteDefinition> definitions = locator.getRouteDefinitions().collectList().block();

        if (definitions != null) {
            definitions.stream()
                    .filter(def -> def.getId().endsWith("-service"))
                    .forEach(def -> {
                        String name = def.getId(); // e.g. "auth-service"
                        groups.add(GroupedOpenApi.builder()
                                .group(name)
                                .pathsToMatch(extractBasePath(name) + "/**")
                                .build());
                    });
        }
        return groups;
    }

    /**
     * Map route IDs like "auth-service" → base path "/auth",
     * "product-service" → "/products", etc.
     */
    private String extractBasePath(String routeId) {
        return switch (routeId) {
            case "auth-service"      -> "/auth";
            case "product-service"   -> "/products";
            case "supplier-service"  -> "/suppliers";
            case "warehouse-service" -> "/warehouses";
            case "movement-service"  -> "/movements";
            case "purchase-service"  -> "/purchase-orders";
            case "report-service"    -> "/reports";
            case "payment-service"   -> "/payments";
            case "alert-service"     -> "/alerts";
            default                  -> "/" + routeId.replace("-service", "s");
        };
    }
}
