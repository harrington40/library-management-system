#!/bin/sh
# Wait for MongoDB to be ready
wait-for-it mongodb:27017 --timeout=30 --strict -- echo "MongoDB is up"

# Start the Spring Boot application
exec java -jar /app/app.jar