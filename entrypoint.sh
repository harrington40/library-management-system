#!/bin/sh
# Wait for Spring Boot server to be ready
while ! nc -z server 8080; do 
  echo "Waiting for server to start..."
  sleep 2
done

# Start the client application
exec java -jar /app/app.jar