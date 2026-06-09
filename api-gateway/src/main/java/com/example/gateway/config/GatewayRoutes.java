package com.example.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutes {
    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("users-service", route -> route.path("/users/**").uri("http://localhost:5001"))
                .route("products-service", route -> route.path("/products/**").uri("http://localhost:5002"))
                .route("orders-service", route -> route.path("/orders/**").uri("http://localhost:5003"))
                .build();
    }
}
