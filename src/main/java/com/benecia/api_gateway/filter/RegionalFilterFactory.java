package com.benecia.api_gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

@Component
public class RegionalFilterFactory extends AbstractGatewayFilterFactory<RegionalFilterFactory.Config> {

    private static final Logger logger = LoggerFactory.getLogger(RegionalFilterFactory.class);

    public RegionalFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // 1. 기존 요청 객체 get
            ServerHttpRequest request = exchange.getRequest();

            // 2. 새로운 헤더 추가하여 요청 객체 복제(mutate)
            ServerHttpRequest newRequest = request.mutate()
                    .header("X-Custom-Header", "MyCustomValue")
                    .build();

            logger.info("Added custom header to request: {}", request.getId());

            // 3. 새로 만들어진 요청으로 다음 필터 체인 실행
            return chain.filter(exchange.mutate().request(newRequest).build());
        };
    }

    public static class Config {}
}
