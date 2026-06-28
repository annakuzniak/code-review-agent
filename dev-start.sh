#!/bin/bash
echo "🐳 Starting pgvector..."
docker start pgvector

echo "⏳ Waiting for database to be ready..."
sleep 2

echo "🚀 Starting code-review-agent..."
./mvnw spring-boot:run -Dspring-boot.run.profiles=local