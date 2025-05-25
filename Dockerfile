# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml first to leverage Docker cache
COPY pom.xml .
# Download dependencies (will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Package the application
RUN mvn package -DskipTests

# Stage 2: Create the runtime image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/target/transport-0.0.1-SNAPSHOT.jar app.jar

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=prod

# Expose the port the application runs on
EXPOSE 8080

# Run the application with dynamic port assignment
# Environment variables (MONGODB_URI, JWT_SECRET) should be passed at runtime
ENTRYPOINT ["java", "-jar", "/app/app.jar", "--server.port=${PORT:8080}"]