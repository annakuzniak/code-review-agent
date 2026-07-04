#!/bin/bash
echo "🐳 Starting pgvector..."
docker start pgvector

echo "⏳ Waiting for database to be ready..."
sleep 2

echo "🚀 Starting code-review-agent..."
#I'm not sure if this is needed, but it's a good idea to limit the memory usage of the maven process
MAVEN_OPTS="-Xmx512m" ./mvnw spring-boot:run -Dspring-boot.run.profiles=local