# Use official OpenJDK base image
FROM eclipse-temurin:17-jdk-jammy

# Set working directory
WORKDIR /app

# Copy the built JAR file (assuming it's in target/)
COPY target/library-cli-1.0-SNAPSHOT.jar app.jar

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]