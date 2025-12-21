package com.benecia.api_gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class AuthorizationHeaderFilter extends AbstractGatewayFilterFactory<AuthorizationHeaderFilter.Config> {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationHeaderFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private final Key key;

    public AuthorizationHeaderFilter(@Value("${token.secret}") String secretKey) {
        super(Config.class);
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public static class Config {
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                log.info("Authorization header missing");
                return unauthorized(exchange, "Authorization header missing");
            }

            String authorizationHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
                log.info("Authorization header is not Bearer");
                return unauthorized(exchange, "Authorization header must start with Bearer");
            }

            String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
            if (token.isEmpty()) {
                log.info("Bearer token empty");
                return unauthorized(exchange, "Bearer token is empty");
            }

            Claims claims;
            try {
                claims = Jwts.parserBuilder()
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();
            } catch (ExpiredJwtException e) {
                log.info("Token expired: {}", e.getMessage());
                return unauthorized(exchange, "Token expired");
            } catch (JwtException e) {
                // 서명 불일치, 포맷 오류 등
                log.info("Invalid token: {}", e.getMessage());
                return unauthorized(exchange, "Invalid token");
            } catch (Exception e) {
                log.info("Token parsing error", e);
                return unauthorized(exchange, "Token parsing error");
            }

            String userId = claims.getSubject();
            if (userId == null || userId.isEmpty()) {
                return unauthorized(exchange, "User ID not found in token");
            }

            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Id", userId) // 내부 서비스용 헤더 추가
                    .build();

            // 6. 다음 필터로 진행
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        });
    }

    // 401 응답 처리 메서드 (WebFlux Mono 반환)
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);

        log.info("Unauthorized request: {}", message);

        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);

        return response.writeWith(Flux.just(buffer));
    }
}
