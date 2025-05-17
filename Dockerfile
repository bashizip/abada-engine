# Stage 1: Build with Maven
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the fat JAR from the builder stage
COPY --from=builder /app/target/abada-engine-*.jar app.jar

# Expose the default Spring Boot port
EXPOSE 5601

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
