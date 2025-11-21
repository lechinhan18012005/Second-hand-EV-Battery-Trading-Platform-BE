# ================================
# Stage 1: Build the application
# ================================
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml first for better caching
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B || true

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# Verify build output
RUN echo "=== Build completed. Files in target: ===" && \
    ls -lah /app/target/

# ================================
# Stage 2: Run the application
# ================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the specific JAR file
COPY --from=build /app/target/ev-dealer-management-1.0.0.jar app.jar

# Verify JAR was copied
RUN ls -lah /app/

# Create non-root user
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

USER appuser

EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]