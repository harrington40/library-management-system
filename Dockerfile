FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
COPY target/library-management-system-1.0.0.jar app.jar

# Wait for MongoDB to be ready (install wait-for-it)
RUN apt-get update && apt-get install -y wait-for-it

# Use an entrypoint script to handle dependencies
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]