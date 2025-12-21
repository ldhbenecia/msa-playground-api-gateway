package com.benecia.api_gateway.config;

import com.benecia.api_gateway.filter.AuthorizationHeaderFilter;
import com.benecia.api_gateway.filter.RegionalFilterFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    private final RegionalFilterFactory regionalFilterFactory;
    private final AuthorizationHeaderFilter authorizationHeaderFilter;

    @Value("${msa.client.user-url}")
    private String userServiceUrl;

    @Value("${msa.client.order-url}")
    private String orderServiceUrl;

    @Value("${msa.client.product-url}")
    private String productServiceUrl;

    public GatewayConfig(RegionalFilterFactory regionalFilterFactory,
                         AuthorizationHeaderFilter authorizationHeaderFilter) {
        this.regionalFilterFactory = regionalFilterFactory;
        this.authorizationHeaderFilter = authorizationHeaderFilter;
    }

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user-service-route", r -> r.path("/user-service/**")
                        .filters(f -> f.stripPrefix(1)
                                .filter(regionalFilterFactory.apply(new RegionalFilterFactory.Config())))
                        .uri(userServiceUrl))

                .route("order-service-route", r -> r.path("/order-service/**")
                        .filters(f -> f.stripPrefix(1)
                                .filter(authorizationHeaderFilter.apply(new AuthorizationHeaderFilter.Config())))
                        .uri(orderServiceUrl))

                .route("product-service-route", r -> r.path("/product-service/**")
                        .filters(f -> f.stripPrefix(1)
                                .filter(authorizationHeaderFilter.apply(new AuthorizationHeaderFilter.Config())))
                        .uri(productServiceUrl))
                .build();
    }
}
