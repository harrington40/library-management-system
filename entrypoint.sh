#!/bin/sh

# Wait for MongoDB to be fully ready
echo "Waiting for MongoDB to be ready..."
while ! mongosh --host mongodb --eval "db.runCommand('ping').ok" --quiet; do
  sleep 2
done

# Additional safety wait
sleep 5

# Start the Spring Boot application
exec java -jar /app/app.jar