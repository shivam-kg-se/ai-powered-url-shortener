# Stage 1: Build the Spring Boot application
FROM eclipse-temurin:23-jdk AS build

WORKDIR /app

# Copy Maven Wrapper and project configuration
COPY pom.xml ./
COPY mvnw ./
COPY .mvn ./.mvn

# Make Maven Wrapper executable
RUN chmod +x mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy application source code
COPY src ./src

# Build the JAR file
RUN ./mvnw clean package -DskipTests


# Stage 2: Run the application
FROM eclipse-temurin:23-jre

WORKDIR /app

# Copy the JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Application port
EXPOSE 8080

# Start the Spring Boot application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
