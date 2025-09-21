package com.benecia.api_gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class GlobalLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(GlobalLoggingFilter.class);


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestId = exchange.getRequest().getId();
        logger.info("Request ID: {}, Path: {}", requestId, exchange.getRequest().getPath());

        // 다음 필터로 요청을 전달
        return chain.filter(exchange)
                .then(Mono.fromRunnable(() -> {
                // 필터의 후처리(Post-Filter) 로직
                logger.info("Response for Request ID: {}", requestId);
        }));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
