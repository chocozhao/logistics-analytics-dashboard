# Backend Dockerfile for Logistics Analytics Dashboard
# Based on PRD section 12.1

FROM eclipse-temurin:17-jdk as builder

WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build the application
RUN ./mvnw clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Create a non-root user to run the application
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "/app.jar"]