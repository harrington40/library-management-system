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

# Spring Boot layered JAR optimization
COPY --from=builder /app/target/*.jar  /app/
RUN java -Djarmode=layertools -jar app.jar extract

EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=docker
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]