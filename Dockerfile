FROM eclipse-temurin:21-jre-jammy AS base
WORKDIR /app

# Create non-root user for security
RUN groupadd -r bookstore && useradd -r -g bookstore bookstore

FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src

# Build the application (skip tests — tests run separately in CI)
RUN apt-get update && apt-get install -y maven && \
    mvn package -DskipTests -q

FROM base AS final
WORKDIR /app

COPY --from=build /build/target/*.jar app.jar

# Change ownership to non-root user
RUN chown bookstore:bookstore app.jar

USER bookstore

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
