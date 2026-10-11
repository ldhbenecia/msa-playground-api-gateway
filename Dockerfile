# ---- 빌드 스테이지 ----
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# 의존성 먼저 복사 → 소스만 바뀌면 캐시 재사용
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# 소스는 나중에
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# jar를 레이어로 분리 (스프링 layertools)
RUN java -Djarmode=layertools -jar build/libs/*.jar extract --destination extracted

# ---- 런타임 스테이지 ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app

# 변경 빈도 낮은 것부터 (캐시 최적화)
COPY --from=builder --chown=app:app /app/extracted/dependencies/ ./
COPY --from=builder --chown=app:app /app/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=app:app /app/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=app:app /app/extracted/application/ ./

USER app
EXPOSE 8000
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "org.springframework.boot.loader.launch.JarLauncher"]
