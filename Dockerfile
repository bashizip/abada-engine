# Stage 1: Build with Maven
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create a non-root user and group for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the fat JAR from the builder stage
COPY --from=builder /app/target/abada-engine-*.jar app.jar

# Change ownership to the new user
RUN chown appuser:appgroup app.jar

# Switch to the non-root user
USER appuser

EXPOSE 5601

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
