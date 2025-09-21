package com.benecia.api_gateway.config;

import com.benecia.api_gateway.filter.RegionalFilterFactory;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    // 실습용 지역 필터
    private final RegionalFilterFactory regionalFilterFactory;

    public GatewayConfig(RegionalFilterFactory regionalFilterFactory) {
        this.regionalFilterFactory = regionalFilterFactory;
    }

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user-service-route", r -> r.path("/user-service/**")
                        .filters(f -> f.stripPrefix(1)
                                // 헤더 추가 지역 필터 적용
                                .filters(regionalFilterFactory.apply(new RegionalFilterFactory.Config())))
                        .uri("lb://user-service"))
                .route("order-service-route", r -> r.path("/order-service/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://order-service"))
                .build();
    }
}
