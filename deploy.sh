#!/bin/bash
set -e

echo "Building Docker image..."
mvn clean package -DskipTests
docker build -t job-processing-scheduler:latest .

echo "Tagging for DigitalOcean Registry..."
REGISTRY=${DIGITALOCEAN_REGISTRY:-registry.digitalocean.com/}
TAG="${REGISTRY}job-processing-scheduler:latest"
docker tag job-processing-scheduler:latest "${TAG}"

echo "Pushing to DigitalOcean Registry..."
docker push "${TAG}"

echo "Deployment manifest created. Use DigitalOcean App Platform to deploy."
echo "Tag: ${TAG}"
