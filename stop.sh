#!/bin/bash -e

DOCKER_REPOSITORY="inferno"

POM_VERSION=$(xpath -q -e '/project/version/text()' pom.xml)

# Pre-Build
docker rm -f testDatabase testRealm testWorld || echo "No containers running"
docker rmi ${DOCKER_REPOSITORY}/test-executor:${POM_VERSION} ${DOCKER_REPOSITORY}/realm:${POM_VERSION} ${DOCKER_REPOSITORY}/world:${POM_VERSION} || echo "No images found"

# Build :: Build Artifacts
mvn clean