# Backend Dockerfile for Logistics Analytics Dashboard
# Based on PRD section 12.1
# Using Maven image for building to avoid maven wrapper issues

FROM maven:3.8-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml first for better layer caching
COPY pom.xml .

# Download dependencies (using resolve instead of go-offline for better reliability)
RUN mvn dependency:resolve -B

# Copy source code
COPY src src

# Build the application
RUN mvn clean package -DskipTests && ls -la /app/target/*.jar

# Runtime stage
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /app/target/logistics-analytics-dashboard-*.jar app.jar
RUN chmod +x app.jar

# Create a non-root user to run the application
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "/app.jar"]