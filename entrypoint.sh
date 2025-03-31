#!/bin/sh

# Wait for MongoDB to be healthy
echo "Waiting for MongoDB to be ready..."
while ! curl -s http://mongodb:27017/librarydb --max-time 2 >/dev/null; do
  sleep 2
done

# Additional wait to ensure MongoDB is fully initialized
sleep 5

# Start the Spring Boot application
exec java -jar /app/app.jar