# Stage 1: Build with Maven
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app
COPY . .
RUN mvn clean package spring-boot:repackage

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install su-exec for user switching
RUN apk add --no-cache su-exec

# Create a non-root user and group for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the fat JAR from the builder stage
COPY --from=builder /app/target/abada-engine-*.jar app.jar

# Copy entrypoint script
COPY docker-entrypoint.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

# Create logs directory and log file
RUN mkdir -p /app/logs && \
    touch /app/logs/abada-engine.logs && \
    chown -R appuser:appgroup /app/logs

# Create data directory for dev profile
RUN mkdir -p /app/data && \
    chown -R appuser:appgroup /app/data

# Change ownership to the new user
RUN chown appuser:appgroup app.jar

# Note: We stay as root so entrypoint can fix volume permissions
# The entrypoint script will switch to appuser before running the app

EXPOSE 5601

# Run the application via entrypoint script
ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh", "java", "-jar", "app.jar"]
