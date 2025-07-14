#!/bin/bash

if [[ $# -lt 2 ]]; then
    echo "Error: No args provided."
    echo "Usage: $0 <Dockerfile> <Docker Registry> <docker-compose file>"
    exit 1
fi

DOCKERFILE="$1"
DOCKER_REGISTRY="$2"

echo "Compiling..."
rm -rf target

mvn clean package

mvn clean install

echo "Creating runnable directory"
mkdir -p target/runnable

echo "Copying bindist/lib to runnable/lib"
cp -r target/bindist/lib target/runnable/lib

echo "Copying classes to runnable/classes"
cp -r target/classes target/runnable/classes

echo "Copying scripts to runnable"
cp -r target/classes/scripts/. target/runnable

echo "Remove files and folders"
rm -rf target/runnable/classes/scripts/tini target/runnable/classes/scripts/web

echo "Building Docker image"
docker build -f "$DOCKERFILE" -t browxy_satellite .

echo "Tag docker image"
docker build -f "$DOCKERFILE" -t "${DOCKER_REGISTRY}"/browxy_satellite:1.0 .

echo "Push Docker image"
sudo docker push "${DOCKER_REGISTRY}"/browxy_satellite:1.0

echo "Done"
