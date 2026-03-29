FROM maven:3.8.1-openjdk-21 AS builder

WORKDIR /build

# Copy pom files
COPY java-platform-v2/pom.xml .
COPY java-platform-v2/backend/pom.xml ./backend/

# Download dependencies
RUN mvn -B dependency:resolve

# Copy source code
COPY java-platform-v2/backend/src ./backend/src

# Build application
RUN mvn -B clean package -DskipTests

# Runtime stage
FROM openjdk:21-slim

WORKDIR /app

# Copy built jar
COPY --from=builder /build/backend/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD java -cp app.jar com.javaplatform.health.HealthCheck || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
