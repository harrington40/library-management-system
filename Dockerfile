# Build and runtime stage (single stage for exec:java)
FROM maven:3.8.6-eclipse-temurin-17

WORKDIR /app

# Copy only what's needed for Maven
COPY pom.xml .
COPY src ./src

# Download dependencies first (better layer caching)
RUN mvn dependency:go-offline

# Set default command to run with exec:java
CMD ["mvn", "exec:java", "-Dexec.mainClass=your.main.ClassName"]