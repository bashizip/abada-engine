# Stage 1: Build with Maven
FROM maven:3.9.6-eclipse-temurin-21 AS builder

# Build argument to control whether to use local jar or build from source
ARG USE_LOCAL_JAR=false

WORKDIR /app

# Copy only pom.xml first to leverage Docker layer caching for dependencies
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Download dependencies (this layer will be cached unless pom.xml changes)
RUN if [ "$USE_LOCAL_JAR" = "false" ]; then \
    ./mvnw dependency:go-offline -B; \
    fi

# Copy source code
COPY src ./src

# Build the application (skip if using local jar)
RUN if [ "$USE_LOCAL_JAR" = "false" ]; then \
    ./mvnw clean package spring-boot:repackage -DskipTests; \
    else \
    mkdir -p target; \
    fi

# Copy local jar if USE_LOCAL_JAR is true (will be in context if .dockerignore allows it)
COPY target/ ./target/

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
