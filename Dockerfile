# Build stage
FROM maven:3.8.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Improved JAR handling with wildcards
COPY --from=builder /app/target/*.jar ./app.jar
RUN java -Djarmode=layertools -jar app.jar extract && \
    rm app.jar  # Remove the original JAR after extraction

EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=docker
ENV JAVA_OPTS="-Xms256m -Xmx512m -Djava.security.egd=file:/dev/./urandom"

# Optimized entrypoint for Spring Boot
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.JarLauncher"]